/*
        DataSync Application
        
        @author Tom Spirit
        
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation; either version 3 of the License, or
        (at your option) any later version.
        
        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.
        
        You should have received a copy of the GNU General Public License
        along with this program; if not, write to the Free Software Foundation,
        Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package de.spiritscorp.DataSync.Gui;

import de.spiritscorp.DataSync.Job.SyncJobContext;
import de.spiritscorp.DataSync.Controller.ControllerHelper;
import de.spiritscorp.DataSync.IO.Preference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

/**
 * Enterprise production-grade JavaFX Management Interface.
 * Implements code-driven Material Design metrics, dynamic multi-job context configurations,
 * exclusive mode switching via ComboBox, and explicit window hide hooks.
 * * @author Tom Spirit
 */
public class MainView extends Application {

    private final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();
    private final ControllerHelper helper = new ControllerHelper();
    
    private ListView<SyncJobContext> sidebarListView;
    private Label jobTitleLabel;
    private TableView<SyncJobContext.FileRow> duplicateTable;
    private TextArea consoleTextArea;
    private TabPane detailTabPane;
    
    private ComboBox<String> modeComboBox;
    private Button actionButton;
    private Button cancelButton;
    private Button deleteButton;
    private Button exitButton;
    private ProgressBar progressBar;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DataSync Advanced Management Platform");

        // Intercept close requests to hide to the system tray environment instead of shutting down
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
            System.out.println("[Lifecycle] Window hidden. Application processing stays active in background.");
        });

        BorderPane mainLayout = new BorderPane();
        mainLayout.setLeft(buildSidebar());
        mainLayout.setCenter(buildMainWorkArea());

        Scene scene = new Scene(mainLayout, 1200, 750);
        applyMaterialSkin(scene);
        
        primaryStage.setScene(scene);
        primaryStage.show();

        loadInitialJobConfigurations();
    }

    private void applyMaterialSkin(Scene scene) {
        scene.getRoot().setStyle(
            "-fx-font-family: 'Segoe UI', Arial, sans-serif;" +
            "-fx-background-color: #f8f9fa;"
        );
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: #2c3e50;");

        Label sidebarHeader = new Label("VERWALTETE TASK-INSTANZEN");
        sidebarHeader.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        sidebarListView = new ListView<>(jobList);
        sidebarListView.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        sidebarListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SyncJobContext item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    textProperty().bind(item.jobNameProperty());
                    FontIcon itemIcon = new FontIcon("mdal-folder");
                    itemIcon.setStyle("-fx-icon-color: #3498db;");
                    setGraphic(itemIcon);
                    setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px; -fx-padding: 10px 6px;");
                }
            }
        });

        sidebarListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                bindJobToInterfaceContext(newVal);
            }
        });

        Button addJobButton = new Button("Task hinzufügen");
        addJobButton.setGraphic(new FontIcon("mdal-add"));
        addJobButton.setMaxWidth(Double.MAX_VALUE);
        addJobButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        addJobButton.setOnAction(e -> jobList.add(new SyncJobContext("Sync Job " + (jobList.size() + 1), Preference.getInstance())));

        exitButton = new Button("Programm beenden");
        FontIcon exitIcon = new FontIcon("mdal-power");
        exitIcon.setStyle("-fx-icon-color: white;");
        exitButton.setGraphic(exitIcon);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        exitButton.setOnAction(e -> triggerApplicationShutdown());

        sidebar.getChildren().addAll(sidebarHeader, sidebarListView, addJobButton, exitButton);
        VBox.setVgrow(sidebarListView, Priority.ALWAYS);
        return sidebar;
    }

    private Node buildMainWorkArea() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));

        jobTitleLabel = new Label("Kein Task aktiv");
        jobTitleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox actionToolbar = new HBox(12);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Label modeLabel = new Label("Modus:");
        modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");

        modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll("Synchronisieren", "Backup erstellen", "Duplikate scannen");
        modeComboBox.setStyle("-fx-padding: 6px; -fx-pref-width: 200px;");

        actionButton = new Button("Ausführen", new FontIcon("mdal-play_arrow"));
        actionButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");

        cancelButton = new Button("Abbrechen", new FontIcon("mdal-stop"));
        cancelButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 16px;");

        deleteButton = new Button("Duplikate löschen", new FontIcon("mdal-delete"));
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 16px;");

        actionToolbar.getChildren().addAll(modeLabel, modeComboBox, actionButton, cancelButton, deleteButton);

        detailTabPane = new TabPane();
        detailTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        consoleTextArea = new TextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

        detailTabPane.getTabs().addAll(
            new Tab("Protokoll-Konsole", consoleTextArea),
            new Tab("Duplikat-Manager", assembleDuplicateTableView()),
            new Tab("Erweiterte Parameter", new StackPane())
        );

        HBox statusFooter = new HBox(12);
        statusFooter.setAlignment(Pos.CENTER_LEFT);
        progressBar = new ProgressBar(0);
        statusLabel = new Label("Bereit");
        statusFooter.getChildren().addAll(progressBar, statusLabel);

        container.getChildren().addAll(jobTitleLabel, actionToolbar, detailTabPane, statusFooter);
        VBox.setVgrow(detailTabPane, Priority.ALWAYS);

        return container;
    }

    private Node assembleDuplicateTableView() {
        duplicateTable = new TableView<>();
        duplicateTable.setEditable(true);

        TableColumn<SyncJobContext.FileRow, Boolean> selectionColumn = new TableColumn<>("Auswahl");
        selectionColumn.setCellValueFactory(data -> data.getValue().selectedProperty());
        selectionColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectionColumn));
        selectionColumn.setEditable(true);
        selectionColumn.setPrefWidth(70);

        TableColumn<SyncJobContext.FileRow, String> nameColumn = new TableColumn<>("Dateiname");
        nameColumn.setCellValueFactory(data -> data.getValue().fileNameProperty());
        nameColumn.setPrefWidth(200);

        TableColumn<SyncJobContext.FileRow, String> sizeColumn = new TableColumn<>("Größe");
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        sizeColumn.setPrefWidth(90);

        TableColumn<SyncJobContext.FileRow, String> pathColumn = new TableColumn<>("Absoluter Pfad");
        pathColumn.setCellValueFactory(data -> data.getValue().pathProperty());
        pathColumn.setPrefWidth(450);

        duplicateTable.getColumns().addAll(selectionColumn, nameColumn, sizeColumn, pathColumn);
        return duplicateTable;
    }

    private void bindJobToInterfaceContext(SyncJobContext job) {
        jobTitleLabel.textProperty().unbind();
        statusLabel.textProperty().unbind();
        consoleTextArea.textProperty().unbind();
        cancelButton.disableProperty().unbind();
        modeComboBox.valueProperty().unbindBidirectional(job.selectedModeProperty());

        jobTitleLabel.textProperty().bind(job.jobNameProperty());
        statusLabel.textProperty().bind(job.statusMessageProperty());
        consoleTextArea.textProperty().bind(job.logOutputProperty());
        
        // Dynamic bi-directional coupling for mode choices
        modeComboBox.valueProperty().bindBidirectional(job.selectedModeProperty());
        
        cancelButton.disableProperty().bind(job.runningProperty().not());
        actionButton.disableProperty().bind(job.runningProperty());
        modeComboBox.disableProperty().bind(job.runningProperty());

        duplicateTable.setItems(job.getDuplicateFiles());

        // Single action button mapping the execution routing logic
        actionButton.setOnAction(e -> {
            String selected = modeComboBox.getValue();
            if ("Synchronisieren".equals(selected)) {
                helper.startSynchronize(job);
            } else if ("Backup erstellen".equals(selected)) {
                helper.startBackup(job);
            } else if ("Duplikate scannen".equals(selected)) {
                helper.startDuplicateScan(job);
            }
        });

        cancelButton.setOnAction(e -> job.cancelRunningTask());
        deleteButton.setOnAction(e -> helper.deleteSelectedDuplicates(job));

        // Dynamically compile and hook isolated configurations tab
        Tab settingsTab = buildSettingsGridTab(job.getPreference());
        detailTabPane.getTabs().set(2, settingsTab);
    }

    private Tab buildSettingsGridTab(Preference pref) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        CheckBox subDirCheck = new CheckBox("Unterordner einbeziehen (SubDir)");
        subDirCheck.setSelected(pref.isSubDir());
        subDirCheck.setOnAction(e -> pref.setSubDir(subDirCheck.isSelected()));

        CheckBox trashbinCheck = new CheckBox("Papierkorb verwenden (Trashbin)");
        trashbinCheck.setSelected(pref.isTrashbin());
        trashbinCheck.setOnAction(e -> pref.setTrashbin(trashbinCheck.isSelected()));

        CheckBox autoDelCheck = new CheckBox("Automatisches Löschen erlauben (AutoDel)");
        autoDelCheck.setSelected(pref.isAutoDel());
        autoDelCheck.setOnAction(e -> pref.setAutoDel(autoDelCheck.isSelected()));

        CheckBox logOnCheck = new CheckBox("Protokollierung aktivieren (LogOn)");
        logOnCheck.setSelected(pref.isLogOn());
        logOnCheck.setOnAction(e -> pref.setLogOn(logOnCheck.isSelected()));

        CheckBox bgSyncCheck = new CheckBox("Hintergrund-Synchronisation aktiv");
        bgSyncCheck.setSelected(pref.isBgSync());
        bgSyncCheck.setOnAction(e -> {
            pref.setBgSync(bgSyncCheck.isSelected());
            helper.setOSAutostart(bgSyncCheck.isSelected());
        });

        grid.add(subDirCheck, 0, 0);
        grid.add(trashbinCheck, 0, 1);
        grid.add(autoDelCheck, 0, 2);
        grid.add(logOnCheck, 0, 3);
        grid.add(bgSyncCheck, 0, 4);

        return new Tab("Erweiterte Parameter", grid);
    }

    private void triggerApplicationShutdown() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Programm beenden");
        confirmation.setHeaderText("Möchten Sie DataSync wirklich schließen?");
        confirmation.setContentText("Aktive Hintergrunddienste werden unwiderruflich beendet.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("[Lifecycle] Complete system teardown triggered manually.");
            System.exit(0);
        }
    }

    private void loadInitialJobConfigurations() {
        jobList.add(new SyncJobContext("NAS Dokumenten-Spiegel", Preference.getInstance()));
        jobList.add(new SyncJobContext("Lokales Code-Workspace Backup", Preference.getInstance()));
        if (!jobList.isEmpty()) {
            sidebarListView.getSelectionModel().select(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
