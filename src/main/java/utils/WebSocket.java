package utils;

import observer.EventManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebSocket {

    private static WebSocket instance;
    private WebSocketClient mWs;
    public EventManager events;
    private boolean mWaitForStartPumpConfirm = false;
    private boolean mWaitForStopPumpConfirm = false;

    private boolean savedInputState = false;

    public static WebSocket getInstance() {
        if (instance == null) {
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
        this.events = new EventManager("message", "pump_status", "water_status");
        // установлю подключение
        createWebSocket();
        //open websocket
        mWs.connect();
        events.notify("message",Misc.getCurrentTime() + "Инициализирую подключение сокета");
    }

    private void createWebSocket() throws URISyntaxException {

        mWs = new WebSocketClient(new URI("ws://127.0.0.1:8002/")) {
            @Override
            public void onMessage(String message) {
                if (message != null) {
                    // если получен ответ об успешной аутентификации- запрошу текущий статус устройства
                    JSONObject response = new JSONObject(message);
                    // проверю команду
                    if (response.has("cmd")) {
                        String cmd = response.getString("cmd");
                        switch (cmd) {
                            case "auth_resp": {
                                // получу статус
                                boolean status = response.getBoolean("status");
                                if (status) {
                                    events.notify("message", Misc.getCurrentTime() + "Успешная аутентификация");
                                    // получу последние данные
                                    requestLastReaderData();
                                }
                                break;
                            }
                            case "get_data_resp": {
                                System.out.println("Получен ответ на запрос");
                                /*try {
                                    PrintWriter out = new PrintWriter("received_data.json");
                                    out.println(response.toString());
                                    out.close();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }*/
                                // получены данные о счётчике
                                // проверю, что считыватель тот
                                if (response.getString("devEui").equals("3735333773386805")) {
                                    JSONArray info = response.getJSONArray("data_list");
                                    JSONObject answerItem = (JSONObject) info.get(0);
                                    String data = answerItem.getString("data");
                                    if (data.startsWith("01")) {
                                        System.out.println("find data " + data);
                                        String status = data.substring(32, 40);
                                        if (Integer.parseInt(status) > 0) {
                                            events.notify("water_status", Misc.getCurrentTime() + "Вход замкнут");
                                            savedInputState = true;
                                        } else {
                                            events.notify("water_status", Misc.getCurrentTime() + "Вход разомкнут");
                                            savedInputState = false;
                                        }
                                    } else if (data.startsWith("05")) {
                                        // изменилось состояние насоса
                                        handlePumpStatusChange(data);
                                    }
                                }
                                break;
                            }
                            case "rx": {
                                // произошло событие
                                if (response.getString("devEui").equals("3735333773386805")) {
                                    String data = response.getString("data");
                                    if (data.startsWith("02")) {
                                        events.notify("message",Misc.getCurrentTime() +  "Произошло действие входного контакта");
                                        String status = data.substring(6, 8);
                                        if (status.equals("03")) {
                                            // наш вход
                                            status = data.substring(32, 40);
                                            if (Integer.parseInt(status) > 0) {
                                                events.notify("water_status",Misc.getCurrentTime() +  "Входной контакт замкнут");
                                                // проверю, не дублируется ли событие
                                                if (savedInputState = false) {
                                                    savedInputState = true;
                                                    startPump();
                                                    writeWaterLog("Входной контакт замкнут");
                                                }
                                            } else {
                                                // проверю, не дублируется ли событие
                                                if (savedInputState = true) {
                                                    savedInputState = false;
                                                    events.notify("water_status",Misc.getCurrentTime() +  "Входной контакт разомкнут");
                                                    writeWaterLog("Входной контакт разомкнут");
                                                    stopPump();
                                                }
                                            }
                                        }
                                    } else if (data.startsWith("05")) {
                                        // изменилось состояние насоса
                                        handlePumpStatusChange(data);
                                    }
                                }
                            }
                            break;
                            case "send_data_resp": {
                                boolean status = response.getBoolean("status");
                                if (mWaitForStartPumpConfirm) {
                                    mWaitForStartPumpConfirm = false;
                                    if (status) {
                                        events.notify("message",Misc.getCurrentTime() +  "Команда на запуск насоса передана");
                                    } else {
                                        events.notify("message",Misc.getCurrentTime() +  "Не удалось передать команду на запуск насоса");
                                    }
                                } else if (mWaitForStopPumpConfirm) {
                                    mWaitForStopPumpConfirm = false;
                                    if (status) {
                                        events.notify("message",Misc.getCurrentTime() +  "Команда на остановку насоса передана");
                                    } else {
                                        events.notify("message",Misc.getCurrentTime() +  "Не удалось передать команду на остановку насоса");
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("opened connection");
                events.notify("message", "Соединение установлено");
                // аутентифицируюсь
                JSONObject obj = new JSONObject();
                obj.put("cmd", "auth_req");
                obj.put("login", "root");
                obj.put("password", "5PDMtu24mzSRXvew");
                String message = obj.toString();
                mWs.send(message);
                events.notify("message", "Посылаю запрос на аутентификацию");
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("closed connection");
                events.notify("message", "Соединение с сервером закрыто... Инициализирую перезапуск");
                // через 5 секунд попробую перезапустить сервер
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("restarting connection");
                                events.notify("message", "Перезапуск соединения с сервером");
                                WebSocket.getInstance().restartConnection();
                            }
                        },
                        5000
                );
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        events.notify("message", "Сокет создан");
    }

    private void writeWaterLog(String s) {
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(
                    new File("water_log.log"),
                    true));
            out.println(Misc.getCurrentTime() + s);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handlePumpStatusChange(String data) {
        String status = data.substring(8, 10);
        if (Integer.parseInt(status) > 0) {
            events.notify("pump_status",Misc.getCurrentTime() +  "Насос запущен");
            writePumpLog("Насос запущен");
        } else {
            events.notify("pump_status",Misc.getCurrentTime() +  "Насос остановлен");
            writePumpLog("Насос остановлен");
        }
    }

    private void writePumpLog(String s) {
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(
                    new File("pump_log.log"),
                    true));
            out.println(Misc.getCurrentTime() + s);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
        if (mWs.isOpen()) {
            System.out.println("check water status");
            requestLastReaderData();
        }
    }

    private void requestLastReaderData() {
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

    public void startPump() {
        // запущу насос
        JSONObject obj = new JSONObject();
        obj.put("cmd", "send_data_req");
        JSONArray arr = new JSONArray();
        JSONObject innerObj = new JSONObject();
        innerObj.put("ack", "true");
        innerObj.put("data", "0301ff");
        innerObj.put("devEui", "3735333773386805");
        innerObj.put("port", 2);
        arr.put(innerObj);
        obj.put("data_list", arr);
        String message = obj.toString();
        mWs.send(message);
        events.notify("pump_status",Misc.getCurrentTime() +  "Отправлена команда на включение насоса");
        mWaitForStartPumpConfirm = true;
    }

    public void stopPump() {
        // остановлю насос
        JSONObject obj = new JSONObject();
        obj.put("cmd", "send_data_req");
        JSONArray arr = new JSONArray();
        JSONObject innerObj = new JSONObject();
        innerObj.put("ack", "true");
        innerObj.put("data", "030111");
        innerObj.put("devEui", "3735333773386805");
        innerObj.put("port", 2);
        arr.put(innerObj);
        obj.put("data_list", arr);
        String message = obj.toString();
        mWs.send(message);
        events.notify("pump_status",Misc.getCurrentTime() +  "Отправлена команда на выключение насоса");
        mWaitForStopPumpConfirm = true;
    }
}
