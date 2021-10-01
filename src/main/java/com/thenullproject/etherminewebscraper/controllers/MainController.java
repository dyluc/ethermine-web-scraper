package com.thenullproject.etherminewebscraper.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import com.thenullproject.etherminewebscraper.entities.Entry;
import com.thenullproject.etherminewebscraper.dao.FileHandler;
import com.thenullproject.etherminewebscraper.service.TaskHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MainController implements Initializable{

    // Handles threads for concurrent page scraping and sending of emails
    private TaskHandler taskHandler;

    @FXML
    private TableView<Entry> table;

    @FXML
    private TableColumn<Entry, String> entryColumn;

    @FXML
    private TableColumn<Entry, String> workerColumn;

    @FXML
    private TableColumn<Entry, String> currentHRColumn;

    @FXML
    private TableColumn<Entry, String> reportedHRColumn;

    @FXML
    private TableColumn<Entry, String> emailColumn;

    @FXML
    private TableColumn<Entry, String> httpColumn;

    @FXML
    private TableColumn<Entry, String> saveColumn;

    @FXML
    private TableColumn<Entry, String> deleteColumn;

    @FXML
    private Label logLabel;
    private FadeTransition fadeOut;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        entryColumn.setCellValueFactory(p -> new ReadOnlyObjectWrapper(table.getItems().indexOf(p.getValue()) + 1));
        entryColumn.setSortable(false);

        workerColumn.setCellValueFactory(new PropertyValueFactory<>("worker"));
        currentHRColumn.setCellValueFactory(new PropertyValueFactory<>("currentHR"));
        reportedHRColumn.setCellValueFactory(new PropertyValueFactory<>("reportedHR"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        httpColumn.setCellValueFactory(new PropertyValueFactory<>("http"));

        workerColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        currentHRColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        reportedHRColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        emailColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        httpColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        fadeOut = new FadeTransition(Duration.seconds(2f), logLabel);
        fadeOut.setFromValue(10);
        fadeOut.setToValue(0);

        saveColumn.setCellFactory(initButtonCellFactory("Save", this::saveEntry));
        deleteColumn.setCellFactory(initButtonCellFactory("Delete", this::deleteEntry));

        // load data from config file
        List<Entry> loadedEntries = FileHandler.loadEntries();
        if(loadedEntries != null) {
            if(loadedEntries.isEmpty())
                log('I', "No past entries found");
            else
                log('I', "Successfully loaded past entries");
            table.getItems().setAll(loadedEntries);
        } else {
            loadedEntries = new ArrayList<>();
            log('E', "IOException or ParseException occurred loading entries file");
        }

        // load sender email details from properties file
        Properties senderDetails = FileHandler.loadEmailDetails();
        if(senderDetails != null) {
            taskHandler = new TaskHandler(loadedEntries, senderDetails.getProperty("sender.email"), senderDetails.getProperty("sender.password"));
        } else {
            log('E', "Problem occurred loading sender_email_details.properties");
        }
    }

    private Callback<TableColumn<Entry, String>, TableCell<Entry, String>> initButtonCellFactory(String buttonText, Consumer<Entry> onActionFunction) {
        return new Callback<>() {
            @Override
            public TableCell call(final TableColumn<Entry, String> param) {
                final TableCell<Entry, String> cell = new TableCell<>() {
                    final Button btn = new Button(buttonText);

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            btn.setOnAction(event -> {
                                Entry entry = getTableView().getItems().get(getIndex());
                                onActionFunction.accept(entry);
                            });
                            setGraphic(btn);
                        }
                        setText(null);
                    }
                };
                return cell;
            }
        };
    }

    private void log(char type, String message) {
        switch (type) {
            case 'E' -> { // error
                logLabel.setTextFill(Color.color(239 / 255f, 134 / 255f, 54 / 255f));
                logLabel.setText(message);
                //fadeOut.playFromStart();
            }
            case 'M' -> { // modification
                logLabel.setTextFill(Color.color(81 / 255f, 158 / 255f, 62 / 255f));
                logLabel.setText(message);
                fadeOut.playFromStart();
            }
            case 'I' -> { // info
                logLabel.setTextFill(Color.color(59 / 255f, 117 / 255f, 175 / 255f));
                logLabel.setText(message);
            }
        }
    }

    @FXML
    void addNewEntry(ActionEvent event) {
        table.getItems().add(new Entry("New Worker", 0.0f, 0.0f,"example@email.com", "https://ethermine.org/miners/[xxx]/dashboard"));
    }

    private void saveEntry(Entry entry) {
        entry.commitTempValues();
        if(taskHandler != null) {
            String prompt;
            if(taskHandler.containsEntry(entry)) {
                taskHandler.updateEntry(entry);
                prompt = "Saved modified";
            } else {
                taskHandler.addNewEntry(entry);
                prompt = "Saved new";
            }

            if(FileHandler.updateEntries(taskHandler.getAllEntries())) {
                log('M', prompt + String.format(" entry %s:%s:%s", entry.getWorker(), entry.getReportedHR(), entry.getCurrentHR()));
            } else {
                log('E', "IOException occurred updating entries file");
            }
        }

    }

    private void deleteEntry(Entry entry) {
        table.getItems().remove(entry);

        if(taskHandler != null) {
            taskHandler.deleteEntry(entry);

            if(FileHandler.updateEntries(taskHandler.getAllEntries())) {
                log('M', String.format("Deleted entry %s:%s:%s", entry.getWorker(), entry.getReportedHR(), entry.getCurrentHR()));
            } else {
                log('E', "IOException occurred updating entries file");
            }
        }

    }

    public void updateWorker(TableColumn.CellEditEvent<Entry, String> entryStringCellEditEvent) {
        table.getSelectionModel().getSelectedItem().setTempValue(0, entryStringCellEditEvent.getNewValue());
    }

    public void updateCurrentHR(TableColumn.CellEditEvent<Entry, String> entryStringCellEditEvent) {
        try {
            float newCurrentHR = Float.parseFloat(entryStringCellEditEvent.getNewValue());
            table.getSelectionModel().getSelectedItem().setTempValue(1, newCurrentHR);
        } catch(NumberFormatException e) {
            System.err.println("NUMBER FORMAT EXCEPTION");
            log('E', "Current hash rate is not a number!");
        }
    }

    public void updateReportedHR(TableColumn.CellEditEvent<Entry, String> entryStringCellEditEvent) {
        try {
            float newReportedHR = Float.parseFloat(entryStringCellEditEvent.getNewValue());
            table.getSelectionModel().getSelectedItem().setTempValue(2, newReportedHR);
        } catch(NumberFormatException e) {
            System.err.println("NUMBER FORMAT EXCEPTION");
            log('E', "Reported hash rate is not a number!");
        }
    }

    public void updateEmail(TableColumn.CellEditEvent<Entry, String> entryStringCellEditEvent) {
        table.getSelectionModel().getSelectedItem().setTempValue(3, entryStringCellEditEvent.getNewValue());
    }

    public void updateHttp(TableColumn.CellEditEvent<Entry, String> entryStringCellEditEvent) {
        table.getSelectionModel().getSelectedItem().setTempValue(4, entryStringCellEditEvent.getNewValue());
    }

    public void shutdown() {
        if(taskHandler != null)
            taskHandler.shutdown();
    }
}
