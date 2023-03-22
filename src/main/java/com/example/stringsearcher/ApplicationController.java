package com.example.stringsearcher;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class ApplicationController implements Initializable {

    @FXML
    private Label labelAppRunning;
    @FXML
    private Button btnStartSearch;
    @FXML
    private Button btnStopSearch;
    @FXML
    private Spinner<Integer> spinnerNoThreads;
    @FXML
    private Label labelRuntime;
    @FXML
    private TextField tfSearchString;
    @FXML
    private TextField tfScanFile;
    @FXML
    private AnchorPane parentPane;
    @FXML
    private ListView<String> resultList;

    private final ApplicationModel model;

    public ApplicationController(){
        this.model = new ApplicationModel();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Bind listener to searchRunning boolean, that updates the UI
        model.searchRunningProperty().addListener((observableValue, oldValue, newValue) -> {
            if(newValue){
                updateUiSearchStarted();
            }
            else{
                updateUiSearchStopped();
            }
        });

        // Start Button Click Listener
        btnStartSearch.setOnAction(evt -> {
            if(inputIsValid()){
                model.startSearch();
            }
            else{
                showAlert("Please check your input!", "Error", Alert.AlertType.ERROR);
            }
        });

        // Stop Button Click Listener
        btnStopSearch.setOnAction(evt -> {
            if(model.searchRunningProperty().get()){
                model.stopSearch();
            }
        });

        // Number of Threads Spinner
        // Create a new Integer Spinner and set minimum, maximum and step size
        spinnerNoThreads.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,Runtime.getRuntime().availableProcessors(), 1, 1));
        var fac = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinnerNoThreads.getValueFactory();
        fac.setMin(1);
        fac.setMax(Runtime.getRuntime().availableProcessors());
        fac.setAmountToStepBy(1);
        fac.valueProperty().bindBidirectional(model.threadCountProperty().asObject());

        // Bind to searchRuntime long property
        // Update UI by formatting long to a duration
        model.searchRuntimeProperty().addListener((observableValue, oldValue, newValue) -> {
            Duration duration = Duration.of(newValue.longValue(), ChronoUnit.SECONDS);
            labelRuntime.setText("%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart()));
        });

        // Update the text field input when a new file has been selected as input
        model.scanFilePropertyProperty().addListener(
                (observableValue, oldValue, newValue) -> tfScanFile.setText(newValue.getAbsolutePath())
        );

        // Open a FileChooser when text field for file input is clicked
        // Validate the choosen file (file is null, when canceled by user)
        tfScanFile.setOnMouseClicked(evt -> {
            File f = openFileChooser();
            if(f != null){
                // If f is not null, something has been selected
                if(!f.exists() || !f.isFile()){
                    // An invalid selection has been made, display error
                    showAlert("Selected file does not exist or is not a file", "Error", Alert.AlertType.ERROR);
                }
                else{
                    // Update file in Model
                    model.scanFilePropertyProperty().setValue(f);
                }
            }
        });

        model.searchTextPropertyProperty().bindBidirectional(tfSearchString.textProperty());
        model.searchResultPropertyProperty().bindBidirectional(resultList.itemsProperty());
        model.runstatusProperty().bindBidirectional(labelAppRunning.textProperty());
    }


    /**
     * Checks if the user inputs are valid
     * @return true is valid, false otherwise
     */
    private boolean inputIsValid() {
        if(model.scanFilePropertyProperty().getValue() == null){
            return false;
        }
        if(model.searchTextPropertyProperty().getValue().isBlank()){
            return false;
        }
        return true;
    }


    /**
     * Updates the UI in a stage when the search has been started
     */
    private void updateUiSearchStarted(){
        labelAppRunning.setTextFill(Color.DARKGREEN);
        btnStartSearch.setDisable(true);
        btnStopSearch.setDisable(false);
        tfSearchString.setDisable(true);
        tfScanFile.setDisable(true);
        spinnerNoThreads.setDisable(true);
    }


    /**
     * Updates the UI in a stage where the search has been stopped
     */
    private void updateUiSearchStopped(){
        labelAppRunning.setTextFill(Color.RED);
        btnStopSearch.setDisable(true);
        btnStartSearch.setDisable(false);
        tfSearchString.setDisable(false);
        tfScanFile.setDisable(false);
        spinnerNoThreads.setDisable(false);
    }


    /**
     * Opens a FileChooser and returns the selected file
     * @return File which has been choosen
     */
    private File openFileChooser(){
        FileChooser fc = new FileChooser();
        fc.setTitle("Select file to be searched");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        return fc.showOpenDialog(parentPane.getScene().getWindow());
    }


    /**
     * Shows an alert
     * @param text Alert message
     * @param title Alert title text
     * @param alertType Alert type
     */
    private void showAlert(final String text, final String title, final Alert.AlertType alertType){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
