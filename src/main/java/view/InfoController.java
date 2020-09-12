package view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class InfoController implements Controller{


    @FXML
    public Label infoContainer;

    @FXML
    public Button confirmBtn;
    private Stage mOwner;

    @Override
    public void init(Stage owner) {
        mOwner = owner;
    }

    public void setMessage(String message) {
        infoContainer.setText(message);
    }

    @FXML
    public void closeMe() {
        mOwner.close();
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)){
            closeMe();
        }
    }
}
