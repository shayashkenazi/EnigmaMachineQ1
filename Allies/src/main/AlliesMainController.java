package main;

import DTOs.*;
import com.google.gson.reflect.TypeToken;
import http.HttpClientUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.Pair;
import login.LoginController;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import utils.Constants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static utils.Constants.GSON_INSTANCE;

public class AlliesMainController {

    private LoginController loginComponentController;
    private Node rootNode;
    private final StringProperty userName;
    @FXML private Button btn_ready;
    @FXML private ComboBox<String> cb_battlefieldNames;
    @FXML private ScrollPane sp_mainPage;
    @FXML private TextField tf_taskSize;
    @FXML private TextArea ta_teamsAgentsData, ta_contestsData, ta_contestData, ta_contestTeams, ta_teamsAgentsAndProgress, ta_teamCandidates, ta_progress;
    BooleanProperty  isBattlefieldSelected, isTaskSizeSelected,isReady, isBattleOn, moreThanOneAgent;
    Set<Pair<String,String>> uboatBattlefieldSet;
    private TimerTask agentsDetailsRefresher,readyRefresher, contestTeamsRefresher
            ,finishRefresher,resultRefresher,contestsDataRefresher,currentContestDataRefresher,agentTasksDetailsRefresher;
    private Timer timerAgentsDetails, timerContestTeam,timerResult,timerReadyRefresher,timerFinishRefresher,timerContestsData,timerCurrentContestData,timerAgentTasksDetails;
    private Thread createTaskDMThread;

    @FXML void initialize() {
        isBattlefieldSelected = new SimpleBooleanProperty(false);
        isTaskSizeSelected = new SimpleBooleanProperty(false);
        isBattleOn = new SimpleBooleanProperty(false);
        moreThanOneAgent = new SimpleBooleanProperty(false);
        rootNode = sp_mainPage.getContent();
        cb_battlefieldNames.valueProperty().addListener(observable -> {
            isBattlefieldSelected.set(cb_battlefieldNames.getValue() != null);
        });

        btn_ready.disableProperty().bind(isBattlefieldSelected.not().or(isTaskSizeSelected.not().or(moreThanOneAgent.not()).or(isReady)));

        // Always keep Task Size Text-Field valid
        tf_taskSize.textProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue.equals("")) {
                isTaskSizeSelected.set(false);
                return;
            }

            if (!newValue.matches("^[0-9]*[1-9][0-9]*$")) // Invalid number
                tf_taskSize.setText(oldValue);
            else
                isTaskSizeSelected.set(true);
        });
        isBattleOn.addListener((observable, oldValue, newValue) -> {
            //TODO new Thread RUN THIS SHIT
            if(newValue){
                createTasksDM();
                checkFinishedRefresher();
                refresherCurrentContestDataDetails();
                refresherAgentTasksDetails();
                refresherContestTeamDetails();
            }
            else {
                resultRefresher.cancel();
                timerResult.cancel();
                currentContestDataRefresher.cancel();
                timerCurrentContestData.cancel();
                agentTasksDetailsRefresher.cancel();
                timerAgentTasksDetails.cancel();
                finishRefresher.cancel();
                timerFinishRefresher.cancel();
                contestTeamsRefresher.cancel();
                timerContestTeam.cancel();

            }

        });
        isReady.addListener((observable, oldValue, newValue) -> {
            if(newValue){
                checkReadyRefresher();
            }

        });
    }


    public AlliesMainController() {
        userName = new SimpleStringProperty("Anonymous");
        isReady = new SimpleBooleanProperty(false);
    }

    public void setLoginController(LoginController loginController) {
        this.loginComponentController = loginController;
        loginController.setMainController(this);
    }

    public void setContentScene() {
        sp_mainPage.setContent(loginComponentController.getLoginPage());
    }

    public void setUserName(String userName) {
        this.userName.set(userName);
    }

    public void switchToMainPanel() {
        sp_mainPage.setContent(rootNode);
    }

    @FXML
    void readyBtnClick(ActionEvent event) {
        updateHierarchy();
        //createDM();
        //updateReadyManager();
        //createTasksDM();// TODO ITS NOT HEREEE ONLY WHEN ALL IS READY
        //checkReadyRefresher();
    }
    private void updateReadyManager(){
        String uBoatName = getUboatNameByBattlefieldName(cb_battlefieldNames.getValue());
        String finalUrl = HttpUrl
                .parse(Constants.READY)
                .newBuilder()
                .addQueryParameter(Constants.BATTLEFIELD_NAME,cb_battlefieldNames.getValue())
                .addQueryParameter(Constants.UBOAT_NAME, uBoatName)
                .addQueryParameter(Constants.CLASS_TYPE,Constants.ALLIES_CLASS)
                .addQueryParameter(Constants.TASK_SIZE,tf_taskSize.getText())
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String ignoreLeak = response.body().string();
                if(response.code() == 200) {
                    isReady.set(true);
                }

            }
        });
    }
    private void createDM() {
        String uBoatName = getUboatNameByBattlefieldName(cb_battlefieldNames.getValue());
        String finalUrl = HttpUrl
                .parse(Constants.ALLY_DM)
                .newBuilder()
                .addQueryParameter(Constants.TASK_SIZE, tf_taskSize.getText())
                .addQueryParameter(Constants.UBOAT_NAME, uBoatName)
                .addQueryParameter(Constants.BATTLEFIELD_NAME,cb_battlefieldNames.getValue())
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String text = response.body().string();  // this is decode msg
                updateReadyManager();
            }
        });

    }
    private String getUboatNameByBattlefieldName(String battlefieldName){
        for(Pair<String, String> pair :uboatBattlefieldSet){
            if(pair.getValue().equals(battlefieldName))
                return pair.getKey();
        }
        return null;
    }
    private void updateHierarchy(){
        String BattlefieldSelected = cb_battlefieldNames.getValue();
        String uBoatNameSelected = "unknown Name";
        for(Pair<String, String> pair :uboatBattlefieldSet){
            if(pair.getValue().equals(BattlefieldSelected))
                uBoatNameSelected = pair.getKey();
        }
        //noinspection ConstantConditions
        String finalUrl = HttpUrl
                .parse(Constants.HIERARCHY)
                .newBuilder()
                .addQueryParameter(Constants.USERNAME, userName.getValue())
                .addQueryParameter(Constants.UBOAT_NAME, uBoatNameSelected)
                .addQueryParameter(Constants.TASK_SIZE,tf_taskSize.getText())
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() != 200) {
                    String responseBody = response.body().string();
                }
                else {
                    if(response.code() == 200){
                        createDM();
                        //isReady.set(true);
                    }
                }
            }
        });

    }
/*    public void setUboatBattlefieldSet(Set<Pair<String,String>> uboatBattlefieldSet){
        this.uboatBattlefieldSet = uboatBattlefieldSet;
    }*/
    public void initializeCB(Set<Pair<String,String>> uboatBattlefieldSet){
        this.uboatBattlefieldSet = uboatBattlefieldSet;
        for(Pair<String, String> pair :uboatBattlefieldSet){
            cb_battlefieldNames.getItems().add(pair.getValue());
        }

    }

    public void checkReadyRefresher() {
        readyRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.CHECK_READY_BATTLE)
                        .newBuilder()
                        .addQueryParameter(Constants.CLASS_TYPE,Constants.ALLIES_CLASS)
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String ignoreLeak = response.body().string();
                        if (response.code() == 200) {
                            isBattleOn.set(true);
                            if (readyRefresher != null && timerReadyRefresher != null) {
                                readyRefresher.cancel();
                                timerReadyRefresher.cancel();
                            }
                        }
                        else
                        {

                        }
                        //System.out.println("hey im in ready servlet res");
                        //isReady.set(true);
                    }
                });
            }
        };
        timerReadyRefresher = new Timer();
        timerReadyRefresher.schedule(readyRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
    private void checkFinishedRefresher() {
        finishRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.CHECK_READY_BATTLE)
                        .newBuilder()
                        .addQueryParameter(Constants.CLASS_TYPE,Constants.ALLIES_CLASS)
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String ignoreLeak = response.body().string();
                        if (response.code() == 204 ) {
                            isBattleOn.set(false);
                            isReady.set(false);
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {

                            }
                            resetAlly();
                        }
                    }
                });
            }
        };
        timerFinishRefresher = new Timer();
        timerFinishRefresher.schedule(finishRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
    private void resetAlly(){
        String uBoatName = getUboatNameByBattlefieldName(cb_battlefieldNames.getValue());
        String finalUrl = HttpUrl
                .parse(Constants.RESET_ALLY)
                .newBuilder()
                .addQueryParameter(Constants.UBOAT_NAME,uBoatName )
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String ignoreLeak = response.body().string();
                if (response.code() == 200) {

                }

            }
        });
    }

    private void createTasksDM() {

        //Runnable workerThread = () -> {
        String uBoatName = getUboatNameByBattlefieldName(cb_battlefieldNames.getValue());
        String finalUrl = HttpUrl
                .parse(Constants.CREATE_TASKS)
                .newBuilder()
                .addQueryParameter(Constants.UBOAT_NAME, uBoatName)
                .build()
                .toString();
        HttpClientUtil.runAsync(finalUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String ignoreLeak = response.body().string();
                if (response.code() == 200) {
                    refresherResult();
                }
                //System.out.println("hey im in ready servlet res");
                //isReady.set(true);
            }
        });
    }
    private void refresherResult(){
        resultRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.RESULT)
                        .newBuilder()
                        .addQueryParameter(Constants.CLASS_TYPE,Constants.ALLIES_CLASS)
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() == 200) {
                            showCandidates(response);
                        }

                    }
                });
            }
        };
        timerResult = new Timer();
        timerResult.schedule(resultRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
    private void showCandidates(@NotNull Response response) throws IOException{
        String json_candidates = response.body().string();
        Type setCandidatesType = new TypeToken<Set<DTO_CandidateResult>>() { }.getType(); // TODO: FIX !!!
        Set<DTO_CandidateResult> setCandidates = GSON_INSTANCE.fromJson(json_candidates, setCandidatesType);
        Platform.runLater(() -> {
            ta_teamCandidates.clear();
        });
        for(DTO_CandidateResult dto_candidateResult : setCandidates){
            Platform.runLater(() -> {
                ta_teamCandidates.appendText("-----------------------------------------\n");
                ta_teamCandidates.appendText(dto_candidateResult.getPrintedFormat());
                ta_teamCandidates.appendText("-----------------------------------------\n");
            });
        }
    }
    public void refresherTeamsAgentDetails(){
        agentsDetailsRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.TEAMS_AGENT_DETAILS)
                        .newBuilder()
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() == 200) {
                            showTeamsAgent(response);
                        }

                    }
                });
            }
        };
        timerAgentsDetails = new Timer();
        timerAgentsDetails.schedule(agentsDetailsRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
    private void showTeamsAgent(@NotNull Response response) throws IOException{

        String json_teamsAgentDetails = response.body().string();
        Type listAgentsDetails = new TypeToken< List<DTO_AgentDetails>>() { }.getType();
        List<DTO_AgentDetails> dto_agentDetailsList = GSON_INSTANCE.fromJson(json_teamsAgentDetails, listAgentsDetails);
        Platform.runLater(() -> {
                    ta_teamsAgentsData.clear();
                });
        moreThanOneAgent.set(dto_agentDetailsList.size() > 0);
        for(DTO_AgentDetails dto_agentDetails : dto_agentDetailsList){
            Platform.runLater(() -> {
                ta_teamsAgentsData.appendText("-----------------------------------------\n");
                ta_teamsAgentsData.appendText(dto_agentDetails.printDetailsAgent());
                ta_teamsAgentsData.appendText("-----------------------------------------\n");
            });
        }
    }

    public void refresherContestsDataDetails(){
        contestsDataRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.CONTEST_DATA)
                        .newBuilder()
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.code() == 200) {
                            showContestData(response);
                        }


                    }
                });
            }
        };
        timerContestsData = new Timer();
        timerContestsData.schedule(contestsDataRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
    private void showContestData(@NotNull Response response) throws IOException{

        String json_contestData = response.body().string();
        Type listAgentsDetails = new TypeToken< List<DTO_ContestData>>() { }.getType();
        List<DTO_ContestData> dto_contestDataList = GSON_INSTANCE.fromJson(json_contestData, listAgentsDetails);
        Platform.runLater(() -> {
            ta_contestsData.clear();
        });
        for(DTO_ContestData dto_contestData : dto_contestDataList){
            Platform.runLater(() -> {
                ta_contestsData.appendText("-----------------------------------------\n");
                ta_contestsData.appendText(dto_contestData.printDetailsContestData());
                ta_contestsData.appendText("-----------------------------------------\n");
            });
        }
    }

    public void refresherContestTeamDetails() {
        contestTeamsRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.TEAMS_DETAILS)
                        .newBuilder()
                        .addQueryParameter(Constants.CLASS_TYPE, Constants.ALLIES_CLASS)
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                        if (response.code() == 200) {
                            String json_candidates = response.body().string();
                            Type setCandidatesType = new TypeToken<List<DTO_AllyDetails>>() {
                            }.getType();
                            List<DTO_AllyDetails> dto_allyDetailsList = GSON_INSTANCE.fromJson(json_candidates, setCandidatesType);
                            Platform.runLater(() -> {
                                ta_contestTeams.clear();
                            });

                            for (DTO_AllyDetails dto_allyDetails : dto_allyDetailsList) {
                                Platform.runLater(() -> {
                                    ta_contestTeams.appendText("-----------------------------------------\n");
                                    ta_contestTeams.appendText(dto_allyDetails.getDetailsFormat());
                                    ta_contestTeams.appendText("-----------------------------------------\n");
                                });
                            }
                        }

                    }
                });
            }
        };
        timerContestTeam = new Timer();
        timerContestTeam.schedule(contestTeamsRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }

    public void refresherCurrentContestDataDetails() {
        currentContestDataRefresher = new TimerTask() {
            @Override
            public void run() {
                String finalUrl = HttpUrl
                        .parse(Constants.CONTEST_DATA)
                        .newBuilder()
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                        if (response.code() == 200) {
                            String json_contestData = response.body().string();
                            Type listAgentsDetails = new TypeToken< List<DTO_ContestData>>() { }.getType();
                            List<DTO_ContestData> dto_contestDataList = GSON_INSTANCE.fromJson(json_contestData, listAgentsDetails);
                            Platform.runLater(() -> {
                                ta_contestData.clear();
                            });
                            for(DTO_ContestData dto_contestData : dto_contestDataList){
                                if(dto_contestData.getBattlefieldName().equals(cb_battlefieldNames.getValue())) {
                                    Platform.runLater(() -> {
                                        ta_contestData.appendText("-----------------------------------------\n");
                                        ta_contestData.appendText(dto_contestData.printDetailsContestData());
                                        ta_contestData.appendText("-----------------------------------------\n");
                                    });
                                break;
                                }
                            }
                        }

                    }
                });
            }
        };
        timerCurrentContestData = new Timer();
        timerCurrentContestData.schedule(currentContestDataRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }

    public void refresherAgentTasksDetails() {
        agentTasksDetailsRefresher= new TimerTask() {
            @Override
            public void run() {
                String uBoatName = getUboatNameByBattlefieldName(cb_battlefieldNames.getValue());
                String finalUrl = HttpUrl
                        .parse(Constants.AGENT_TASKS_DETAILS)
                        .newBuilder()
                        .addQueryParameter(Constants.UBOAT_NAME, uBoatName)
                        .build()
                        .toString();
                HttpClientUtil.runAsync(finalUrl, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String json_contestData = response.body().string();
                        if (response.code() == 200) {
                            Type AgentsTasksDetails = new TypeToken< DTO_ProgressDM>() { }.getType();
                            DTO_ProgressDM dto_progressDM = GSON_INSTANCE.fromJson(json_contestData, AgentsTasksDetails);
                            Set<DTO_AgentTasksDetails> dto_agentTasksDetails = dto_progressDM.getDto_AgentTasksDetailsSet();
                            Platform.runLater(() -> {
                                ta_teamsAgentsAndProgress.clear();
                                ta_progress.clear();
                            });
                            for(DTO_AgentTasksDetails dtoAgentTasksDetails : dto_agentTasksDetails) {
                                Platform.runLater(() -> {
                                    ta_teamsAgentsAndProgress.appendText("-----------------------------------------\n");
                                    ta_teamsAgentsAndProgress.appendText(dtoAgentTasksDetails.printDetailsForAgentsTask());
                                    ta_teamsAgentsAndProgress.appendText("-----------------------------------------\n");
                                });
                                break;
                            }
                            Platform.runLater(() -> {
                                ta_progress.setText(dto_progressDM.printDetails());
                            });


                        }

                    }
                });
            }
        };
         timerAgentTasksDetails = new Timer();
        timerAgentTasksDetails.schedule(agentTasksDetailsRefresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }
}
