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

import java.io.IOException;


public class MainController implements Controller, EventListener {
    public Label errorTextLabel;
    public Button startWaterButton;
    public Button stopWaterButton;
    private WebSocket mWs;
    private ControllerModel mMyModel;
    private Stage mStage;

    public void startPump(ActionEvent actionEvent) {
        System.out.println("activate start pump");
        startWaterButton.setDisable(true);
        stopWaterButton.setDisable(false);
        try {
            mMyModel.createInfoWindow("Запускаю насос", mStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPump(ActionEvent actionEvent) {
        startWaterButton.setDisable(false);
        stopWaterButton.setDisable(true);
        System.out.println("activate stop pump");
        try {
            mMyModel.createInfoWindow("Останавливаю насос", mStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(Stage primaryStage) {
        mStage = primaryStage;
        // добавлю модель
        mMyModel = new ControllerModel();
        // запущу сокет
        try {
            mWs = WebSocket.getInstance();
            errorTextLabel.setText("Соединение с сервером установлено");
        } catch (Exception e) {
            e.printStackTrace();
            errorTextLabel.setText("Ой-ой, не смог установить соединение, перезапускайте меня");
        }
        // каждые 5 секунд буду проверять статус устройства
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(5),
                        event -> {
                            WebSocket.getInstance().checkWaterStatus();
                        }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();
        // подпишусь на уведомления
        mWs.events.subscribe("test", this);
    }

    @Override
    public void update(String eventType, String message) {
        Platform.runLater(() -> errorTextLabel.setText(message));
        if(message.equals("Успешная аутентификация")){
            Platform.runLater(() -> {
                startWaterButton.setDisable(false);
                stopWaterButton.setDisable(false);
            });
        }
        else if(message.equals("Соединение с сервером закрыто... Инициализирую перезапуск")){
            Platform.runLater(() -> {
                startWaterButton.setDisable(true);
                stopWaterButton.setDisable(true);
            });
        }
    }
}
