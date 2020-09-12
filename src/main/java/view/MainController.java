package view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import observer.EventListener;
import utils.WebSocket;
import view.view_models.ControllerModel;


public class MainController implements Controller, EventListener {
    public Label WaterControlTextLabel;
    public Label PumpControlTextLabel;
    public Label LastMessageTextLabel;
    public Button startWaterButton;
    public Button stopWaterButton;
    private WebSocket mWs;
    private ControllerModel mMyModel;
    private Stage mStage;

    public void startPump(ActionEvent actionEvent) {
        System.out.println("activate start pump");
        startWaterButton.setDisable(true);
        stopWaterButton.setDisable(false);
        WebSocket.getInstance().startPump();
    }

    public void stopPump(ActionEvent actionEvent) {
        startWaterButton.setDisable(false);
        stopWaterButton.setDisable(true);
        System.out.println("activate stop pump");
        WebSocket.getInstance().stopPump();
    }

    public void init(Stage primaryStage) {
        mStage = primaryStage;
        // добавлю модель
        mMyModel = new ControllerModel();
        // запущу сокет
        try {
            mWs = WebSocket.getInstance();
            LastMessageTextLabel.setText("Соединение с сервером установлено");
        } catch (Exception e) {
            e.printStackTrace();
            LastMessageTextLabel.setText("Ой-ой, не смог установить соединение, перезапускайте меня");
        }
        /*// каждые 5 секунд буду проверять статус устройства
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(20),
                        event -> {
                            WebSocket.getInstance().checkWaterStatus();
                        }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();*/
        // подпишусь на уведомления
        mWs.events.subscribe("message", this);
        mWs.events.subscribe("pump_status", this);
        mWs.events.subscribe("water_status", this);
    }

    @Override
    public void update(String eventType, String message) {
        switch (eventType){
            case "message":{
                Platform.runLater(() -> LastMessageTextLabel.setText(message));
                if (message.equals("Успешная аутентификация")) {
                    Platform.runLater(() -> {
                        startWaterButton.setDisable(false);
                        stopWaterButton.setDisable(false);
                    });
                } else if (message.equals("Соединение с сервером закрыто... Инициализирую перезапуск")) {
                    Platform.runLater(() -> {
                        startWaterButton.setDisable(true);
                        stopWaterButton.setDisable(true);
                    });
                }
            }
            break;
            case "pump_status":{
                Platform.runLater(() -> PumpControlTextLabel.setText(message));
            }
            break;
            case "water_status":{
                Platform.runLater(() -> WaterControlTextLabel.setText(message));
            }
            break;
        }
    }

}
