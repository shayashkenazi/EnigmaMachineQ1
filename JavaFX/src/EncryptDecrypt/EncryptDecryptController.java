package EncryptDecrypt;

import DTOs.DTO_CodeDescription;
import DTOs.DTO_MachineInfo;
import Interfaces.SubController;
import MainApp.AppController;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.Pair;

import java.net.URL;
import java.util.*;

public class EncryptDecryptController implements SubController {

    private AppController appController;
    private Map<Character, Button> keyboardMap;

    @FXML private Button btn_clear, btn_proccess, btn_done;
    @FXML private TextField tf_input, tf_output;
    @FXML private TextArea ta_statistics;
    @FXML private TextArea ta_codeConfiguration;
    @FXML private FlowPane fp_keyboard;
    @FXML private Button btn_reset;

    //----------------------------------------- FXML Methods -----------------------------------------
    @FXML void clearBtnClick(ActionEvent event) {
        tf_input.setText("");
        tf_output.setText("");
    }

    @FXML void proccessBtnClick(ActionEvent event) {
        appController.encryptDecryptController_proccessBtnClick();
    }

    @FXML void doneBtnClick(ActionEvent event) {

    }

    @FXML void resetBtnClick(ActionEvent event) {

    }
    //------------------------------------------------------------------------------------------------

    public void initializeTab() {

        DTO_MachineInfo dto_machineInfo = appController.getDto_machineInfo();

        btn_proccess.setDisable(true);
        btn_done.setDisable(true);

        //ta_codeConfiguration.setText(appController.createCodeConfigurationFormat());

        initializeKeyboard(dto_machineInfo);

        tf_input.textProperty().addListener((observable, oldValue, newValue) -> {
            btn_proccess.setDisable(newValue.equals(""));
        });

        tf_output.textProperty().addListener((observable, oldValue, newValue) -> {
            btn_done.setDisable(newValue.equals(""));
        });
        /*btn_proccess.setOnAction(event -> {
            ta_codeConfiguration.setText(appController.createCodeConfigurationFormat());
        });*/
        // Code Configuration
/*        btn_proccess.setOnAction(event -> {
            ta_codeConfiguration.setText(appController.createCodeConfigurationFormat());
        });

        btn_done.setOnAction(event -> {
            ta_codeConfiguration.setText(appController.createCodeConfigurationFormat());
        });*/
    }

    @Override
    public void setMainController(AppController mainController) {
        appController = mainController;
    }

    private void initializeKeyboard(DTO_MachineInfo dto_machineInfo) {

        keyboardMap = new HashMap<>();
        for (int i = 0; i < dto_machineInfo.getABC().length(); i++) {

            Character currChar = dto_machineInfo.getABC().charAt(i);
            Button btn = new Button(String.valueOf(currChar));

/*            btn.setOnAction(()->{
                btn.getText().charAt(0);
            });*/

            btn.setOnAction(new EventHandler<ActionEvent>() { // TODO: do this better
                @Override
                public void handle(ActionEvent event) {
                    Character resChar = appController.encryptDecryptController_keyboardBtnClick(btn.getText().charAt(0));
                    Button b = keyboardMap.get(resChar);
                    // TODO: add animation
                    makeButtonAnimation(b);
                    tf_input.appendText(String.valueOf(btn.getText().charAt(0)));
                    tf_output.appendText(String.valueOf(resChar));
                    ta_codeConfiguration.setText(appController.createDescriptionFormat());
                }
            });

            keyboardMap.put(currChar, btn);
            fp_keyboard.getChildren().add(btn);
        }
    }

    private void makeButtonAnimation (Button button) {

        Background originalBackground = button.getBackground();
        final Animation animation = new Transition() {

            {
                setCycleDuration(Duration.millis(1000));
                setInterpolator(Interpolator.EASE_OUT);
            }
            @Override
            protected void interpolate(double frac) {
                Color vColor = new Color(0, 0, 1, 1 - frac);
                button.setBackground(new Background(new BackgroundFill(vColor, CornerRadii.EMPTY, Insets.EMPTY)));
            }
        };
        animation.play();
    }

    public TextField getTf_input() { return tf_input; }

    public TextField getTf_output() { return tf_output; }

    public TextArea getTa_statistics() { return ta_statistics; }

    public FlowPane getFp_keyboard() { return fp_keyboard; }
    public TextArea getTa_codeConfiguration() { return ta_codeConfiguration; }

}