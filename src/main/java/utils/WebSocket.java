package utils;

import observer.EventManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocket {

    private static WebSocket instance;
    private WebSocketClient mWs;
    public EventManager events;

    public static WebSocket getInstance(){
    if(instance == null){
        try {
            instance = new WebSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    return instance;
}

    private WebSocket() throws URISyntaxException {
        // register observers
        this.events = new EventManager("test");
        // установлю подключение
        createWebSocket();
        //open websocket
        mWs.connect();
        events.notify("test", "Инициализирую подключение сокета");
    }

    private void createWebSocket() throws URISyntaxException {

        mWs = new WebSocketClient( new URI( "ws://127.0.0.1:8002/" ))
        {
            @Override
            public void onMessage( String message ) {
                if(message != null){
                    // если получен ответ об успешной аутентификации- запрошу текущий статус устройства
                    JSONObject response = new JSONObject(message);
                    // проверю команду
                    if(response.has("cmd")){
                        String cmd = response.getString("cmd");
                        if(cmd.equals("auth_resp")){
                            // получу статус
                            boolean status = response.getBoolean("status");
                            if(status){
                                events.notify("test", "Успешная аутентификация");
                                // получу последние данные
                                requestLastReaderData();
                            }
                        }
                        else if(cmd.equals("get_data_resp")){
                            // получены данные о счётчике
                            JSONArray info = response.getJSONArray("data_list");
                            JSONObject answerItem = (JSONObject) info.get(0);
                            String data = answerItem.getString("data");
                            String status = data.substring(32, 40);
                            if(Integer.parseInt(status) > 0){
                                events.notify("test", "Вход замкнут");
                            }
                            else{
                                events.notify("test", "Вход разомкнут");
                            }
                        }
                    }
                }
            }

            @Override
            public void onOpen( ServerHandshake handshake ) {
                System.out.println( "opened connection" );
                events.notify("test", "Соединение установлено");
                // аутентифицируюсь
                JSONObject obj = new JSONObject();
                obj.put("cmd", "auth_req");
                obj.put("login", "root");
                obj.put("password", "5PDMtu24mzSRXvew");
                String message = obj.toString();
                mWs.send(message);
                events.notify("test", "Посылаю запрос на аутентификацию");
            }

            @Override
            public void onClose( int code, String reason, boolean remote ) {
                System.out.println( "closed connection" );
                events.notify("test", "Соединение с сервером закрыто... Инициализирую перезапуск");
                // через 5 секунд попробую перезапустить сервер
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("restarting connection");
                                events.notify("test", "Перезапуск соединения с сервером");
                                WebSocket.getInstance().restartConnection();
                            }
                        },
                        5000
                );
            }

            @Override
            public void onError( Exception ex ) {
                ex.printStackTrace();
            }
        };
        events.notify("test", "Сокет создан");
    }

    private void restartConnection() {
        // установлю подключение
        try {
            createWebSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //open websocket
        mWs.connect();
    }

    public void checkWaterStatus() {
        if(mWs.isOpen()){
            requestLastReaderData();
        }
    }

    private void requestLastReaderData(){
        // получу последние данные
        JSONObject obj = new JSONObject();
        obj.put("cmd", "get_data_req");
        obj.put("devEui", "3735333773386805");
        JSONObject select = new JSONObject();
        select.put("port", 2);
        select.put("direction", "UPLINK");
        select.put("begin_index", 1);
        select.put("limit", 1);
        obj.put("select", select);
        String request = obj.toString();
        mWs.send(request);
    }
}
