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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Controller.ControllerHelper;
import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Advanced Production JavaFX Interface with Sticky Viewport Context State.
 * Maintains settings/monitor view during task switching, handles enum bindings (BgTime/ScanTime),
 * and dynamically morphs configuration layouts based on target action modes.
 * * @author Tom Spirit
 */
public class Gui_old extends Application {

	private final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();
	private final Map<Path, FileAttributes> sourceMap = Model.createMap();
	private final Map<Path, FileAttributes> destMap = Model.createMap();
	private final Model model = new Model(new Logger(), sourceMap, destMap);
	private final ControllerHelper helper = new ControllerHelper(model, Preference.getInstance(), sourceMap, destMap);

	private ListView<SyncJobContext> sidebarListView;
	private Label workspaceHeaderLabel;
	private TableView<SyncJobContext.FileRow> duplicateTable;
	private TextArea consoleTextArea;

	private StackPane centerViewport;
	private ScrollPane consoleViewNode;
	private VBox duplicateViewNode;
	private HBox controlToolbar;

	private Button actionButton;
	private Button cancelButton;
	private Button deleteButton;
	private Button exitButton;
	private ProgressBar progressBar;
	private Label statusLabel;

	private SyncJobContext currentActiveJob;
	private Stage windowStage;

	// Sticky Navigation State Tracker
	private enum ViewState {
		MONITOR,
		SETTINGS,
		INFO
	}

	private ViewState currentViewState = ViewState.MONITOR;

	private static FontIcon createIcon(Ikon ikon) {
		final FontIcon icon = new FontIcon(ikon);
		icon.setIconSize(20);
		return icon;
	}

	@Override
	public void start(Stage primaryStage) {
		this.windowStage = primaryStage;
		primaryStage.setTitle("DataSync Advanced Management Platform");

		Platform.setImplicitExit(false);
		primaryStage.setOnCloseRequest(event -> {
			event.consume();
			primaryStage.hide();
			System.out.println("[Lifecycle] Window hidden. Application processing stays active in background.");
		});

		final BorderPane mainLayout = new BorderPane();
		mainLayout.setLeft(buildSidebar());
		mainLayout.setCenter(buildMainWorkArea());

		final Scene scene = new Scene(mainLayout, 1300, 850);
		applyMaterialSkin(scene);

		primaryStage.setScene(scene);
		primaryStage.show();

		loadInitialJobConfigurations();
	}

	private void applyMaterialSkin(Scene scene) {
		scene.getRoot().setStyle(
				"-fx-font-family: 'Segoe UI', Arial, sans-serif;" +
						"-fx-background-color: #f8f9fa;");
	}

	private Node buildSidebar() {
		final VBox sidebar = new VBox(16);
		sidebar.setPadding(new Insets(16, 16, 24, 16));
		sidebar.setPrefWidth(290);
		sidebar.setStyle("-fx-background-color: #2c3e50;");

		final MenuButton hamburgerMenu = new MenuButton("Navigation", createIcon(MaterialDesignH.HAMBURGER));
		hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
		hamburgerMenu.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px;");

		final MenuItem taskViewItem = new MenuItem("Aktiver Task-Monitor", createIcon(MaterialDesignF.FOLDER));
		taskViewItem.setOnAction(e -> {
			currentViewState = ViewState.MONITOR;
			switchToTaskContextView();
		});

		final MenuItem settingsItem = new MenuItem("Erweiterte Parameter", createIcon(MaterialDesignS.STORE_SETTINGS));
		settingsItem.setOnAction(e -> {
			currentViewState = ViewState.SETTINGS;
			switchToSettingsContextView();
		});

		final MenuItem infoItem = new MenuItem("System-Informationen", createIcon(MaterialDesignI.INFORMATION));
		infoItem.setOnAction(e -> {
			currentViewState = ViewState.INFO;
			workspaceHeaderLabel.setText("System-Informationen & Lizenzmatrix");
			controlToolbar.setVisible(false);
			displayCustomViewNode(buildAboutInfoNode());
		});

		hamburgerMenu.getItems().addAll(taskViewItem, settingsItem, new SeparatorMenuItem(), infoItem);

		final Label sidebarHeader = new Label("VERWALTETE TASK-INSTANZEN");
		sidebarHeader.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

		sidebarListView = new ListView<>(jobList);
		sidebarListView.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
		sidebarListView.setCellFactory(lv -> {
			final ListCell<SyncJobContext> cell = new ListCell<>() {
				@Override
				protected void updateItem(SyncJobContext item, boolean empty) {
					super.updateItem(item, empty);
					textProperty().unbind();
					if (empty || item == null) {
						setText("");
						setGraphic(null);
						setStyle("-fx-background-color: transparent;");
					} else {
						textProperty().bind(item.jobNameProperty());
						final FontIcon itemIcon = createIcon(MaterialDesignF.FOLDER);
						itemIcon.setStyle("-fx-icon-color: #3498db;");
						setGraphic(itemIcon);
						setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px; -fx-padding: 10px 6px;");
					}
				}
			};

			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem duplicateItem = new MenuItem("Task duplizieren", createIcon(MaterialDesignC.CONTENT_DUPLICATE));
			duplicateItem.setOnAction(event -> {
				final SyncJobContext selectedJob = cell.getItem();
				if (selectedJob != null) {
					final SyncJobContext copy = new SyncJobContext(selectedJob.getJobName() + " (Kopie)", selectedJob.getPreference());
					jobList.add(copy);
				}
			});

			final MenuItem deleteItem = new MenuItem("Task löschen", createIcon(MaterialDesignD.DELETE));
			deleteItem.setStyle("-fx-text-fill: #c0392b;");
			deleteItem.setOnAction(event -> {
				final SyncJobContext selectedJob = cell.getItem();
				if (selectedJob != null) {
					final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Task '" + selectedJob.getJobName() + "' wirklich unwiderruflich löschen?", ButtonType.YES, ButtonType.NO);
					alert.setTitle("Task entfernen");
					alert.setHeaderText(null);
					alert.showAndWait().ifPresent(response -> {
						if (response == ButtonType.YES) {
							jobList.remove(selectedJob);
						}
					});
				}
			});

			contextMenu.getItems().addAll(duplicateItem, deleteItem);
			cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
				if (isNowEmpty)
					cell.setContextMenu(null);
				else
					cell.setContextMenu(contextMenu);
			});
			return cell;
		});

		sidebarListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				currentActiveJob = newVal;
				bindJobToInterfaceContext(newVal);

				// Sticky State Hook: Wechsle basierend auf dem aktuellen ViewState
				if (currentViewState == ViewState.SETTINGS) {
					switchToSettingsContextView();
				} else if (currentViewState == ViewState.INFO) {
					// Bleibe im Info-Tab, falls global ausgewählt
				} else {
					switchToTaskContextView();
				}
			}
		});

		final Button addJobButton = new Button("Task hinzufügen", createIcon(MaterialDesignP.PLUS));
		addJobButton.setMaxWidth(Double.MAX_VALUE);
		addJobButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
		addJobButton.setOnAction(e -> jobList.add(new SyncJobContext("Sync Job " + (jobList.size() + 1), Preference.getInstance())));

		exitButton = new Button("Programm beenden", createIcon(MaterialDesignP.POWER));
		final FontIcon exitIcon = (FontIcon) exitButton.getGraphic();
		exitIcon.setStyle("-fx-icon-color: white;");
		exitButton.setMaxWidth(Double.MAX_VALUE);
		exitButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
		exitButton.setOnAction(e -> triggerApplicationShutdown());

		sidebar.getChildren().addAll(hamburgerMenu, sidebarHeader, sidebarListView, addJobButton, exitButton);
		VBox.setVgrow(sidebarListView, Priority.ALWAYS);
		return sidebar;
	}

	private Node buildMainWorkArea() {
		final VBox container = new VBox(20);
		container.setPadding(new Insets(24));

		workspaceHeaderLabel = new Label("Kein Task aktiv");
		workspaceHeaderLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

		controlToolbar = new HBox(12);
		controlToolbar.setAlignment(Pos.CENTER_LEFT);

		actionButton = new Button("Ausführen", createIcon(MaterialDesignP.PLAY));
		actionButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");

		cancelButton = new Button("Abbrechen", createIcon(MaterialDesignS.STOP));
		cancelButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 16px;");

		controlToolbar.getChildren().addAll(actionButton, cancelButton);

		consoleTextArea = new TextArea();
		consoleTextArea.setEditable(false);
		consoleTextArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

		consoleViewNode = new ScrollPane(consoleTextArea);
		consoleViewNode.setFitToWidth(true);
		consoleViewNode.setFitToHeight(true);

		duplicateViewNode = assembleDuplicateTableView();
		centerViewport = new StackPane(consoleViewNode);

		final HBox statusFooter = new HBox(12);
		statusFooter.setAlignment(Pos.CENTER_LEFT);
		progressBar = new ProgressBar(0);
		statusLabel = new Label("Bereit");
		statusFooter.getChildren().addAll(progressBar, statusLabel);

		container.getChildren().addAll(workspaceHeaderLabel, controlToolbar, centerViewport, statusFooter);
		VBox.setVgrow(centerViewport, Priority.ALWAYS);

		return container;
	}

	private VBox assembleDuplicateTableView() {
		duplicateTable = new TableView<>();
		duplicateTable.setEditable(true);

		final TableColumn<SyncJobContext.FileRow, Boolean> selectionColumn = new TableColumn<>("Auswahl");
		selectionColumn.setCellValueFactory(data -> data.getValue().selectedProperty());
		selectionColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectionColumn));
		selectionColumn.setEditable(true);
		selectionColumn.setPrefWidth(70);

		final TableColumn<SyncJobContext.FileRow, String> nameColumn = new TableColumn<>("Dateiname");
		nameColumn.setCellValueFactory(data -> data.getValue().fileNameProperty());
		nameColumn.setPrefWidth(220);

		final TableColumn<SyncJobContext.FileRow, String> sizeColumn = new TableColumn<>("Größe");
		sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
		sizeColumn.setPrefWidth(95);

		final TableColumn<SyncJobContext.FileRow, String> pathColumn = new TableColumn<>("Absoluter Pfad");
		pathColumn.setCellValueFactory(data -> data.getValue().pathProperty());
		pathColumn.setPrefWidth(480);

		duplicateTable.getColumns().addAll(selectionColumn, nameColumn, sizeColumn, pathColumn);

		deleteButton = new Button("Ausgewählte Duplikate unwiderruflich löschen", createIcon(MaterialDesignD.DELETE));
		deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 16px;");

		final VBox frame = new VBox(12, duplicateTable, deleteButton);
		VBox.setVgrow(duplicateTable, Priority.ALWAYS);
		return frame;
	}

	private void adjustViewportForSelectedMode(String mode) {
		centerViewport.getChildren().clear();
		if ("Duplikate scannen".equals(mode)) {
			centerViewport.getChildren().add(duplicateViewNode);
		} else {
			centerViewport.getChildren().add(consoleViewNode);
		}
	}

	private void switchToTaskContextView() {
		if (currentActiveJob != null) {
			workspaceHeaderLabel.setText("Task-Monitor: " + currentActiveJob.getJobName());
			controlToolbar.setVisible(true);
			adjustViewportForSelectedMode(currentActiveJob.selectedModeProperty().get());
		}
	}

	private void switchToSettingsContextView() {
		if (currentActiveJob != null) {
			workspaceHeaderLabel.setText("Einstellungen für: " + currentActiveJob.getJobName());
			controlToolbar.setVisible(false);
			displayCustomViewNode(buildSettingsGridTab(currentActiveJob));
		}
	}

	private void displayCustomViewNode(Node UIContent) {
		centerViewport.getChildren().clear();
		final ScrollPane scrollFrame = new ScrollPane(UIContent);
		scrollFrame.setFitToWidth(true);
		scrollFrame.setPadding(new Insets(12));
		scrollFrame.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");
		centerViewport.getChildren().add(scrollFrame);
	}

	private void bindJobToInterfaceContext(SyncJobContext job) {
		statusLabel.textProperty().unbind();
		consoleTextArea.textProperty().unbind();
		cancelButton.disableProperty().unbind();

		statusLabel.textProperty().bind(job.statusMessageProperty());
		consoleTextArea.textProperty().bind(job.logOutputProperty());

		cancelButton.disableProperty().bind(job.runningProperty().not());
		actionButton.disableProperty().bind(job.runningProperty());

		duplicateTable.setItems(job.getDuplicateFiles());

		actionButton.setOnAction(e -> {
			final String selected = job.selectedModeProperty().get();
//			if ("Synchronisieren".equals(selected)) helper.startSynchronize(job);
//			else if ("Backup erstellen".equals(selected)) helper.startBackup(job);
//			else if ("Duplikate scannen".equals(selected)) helper.startDuplicateScan(job);
		});

		cancelButton.setOnAction(e -> job.cancelRunningTask());
//		deleteButton.setOnAction(e -> helper.deleteSelectedDuplicates(job));
	}

	private Node buildSettingsGridTab(SyncJobContext job) {
		final Preference pref = job.getPreference();
		final GridPane grid = new GridPane();
		grid.setHgap(24);
		grid.setVgap(16);
		grid.setPadding(new Insets(10));

		// --- Section 1: Job Meta & Modus ---
		final Label modeTitle = new Label("Ausführungsmodus für diesen Task");
		modeTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
		final ComboBox<String> taskModeComboBox = new ComboBox<>();
		taskModeComboBox.getItems().addAll(ScanType.getAllDescriptions());
		taskModeComboBox.valueProperty().bindBidirectional(job.selectedModeProperty());
		taskModeComboBox.setStyle("-fx-padding: 4px; -fx-pref-width: 250px;");

		grid.add(modeTitle, 0, 0);
		grid.add(taskModeComboBox, 1, 0);

		// --- Section 2: Dateipfade (Dynamischer Container basierend auf Modus) ---
		final VBox dynamicPathsContainer = new VBox(12);
		grid.add(dynamicPathsContainer, 0, 1, 2, 1);

		// Listener sorgt für ein Morphing der Layouts bei Modusänderungen direkt im Einstellungsfenster
		taskModeComboBox.valueProperty().addListener((obs, oldMode, newMode) -> {
			renderContextSpecificPathFields(ScanType.get(newMode), dynamicPathsContainer, pref);
		});
		// Erstinitialisierung für den aktuellen Zustand
		renderContextSpecificPathFields(job.getSelectedMode(), dynamicPathsContainer, pref);

		// --- Section 3: Taskspezifische Parameter ---
		final Label paramsTitle = new Label("Erweiterte Ablaufparameter");
		paramsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
		grid.add(paramsTitle, 0, 2, 2, 1);

		final CheckBox subDirCheck = new CheckBox("Unterordner einbeziehen (SubDir)");
		subDirCheck.setSelected(pref.isSubDir());

		final CheckBox trashbinCheck = new CheckBox("Papierkorb verwenden (Trashbin)");
		trashbinCheck.setSelected(pref.isTrashbin());

		final CheckBox autoDelCheck = new CheckBox("Automatisches Löschen erlauben (AutoDel)");
		autoDelCheck.setSelected(pref.isAutoDel());

		final CheckBox logOnCheck = new CheckBox("Protokollierung aktivieren (LogOn)");
		logOnCheck.setSelected(pref.isLogOn());

		final CheckBox bgSyncCheck = new CheckBox("Hintergrund-Synchronisation aktiv");
		bgSyncCheck.setSelected(pref.isBgSync());

		final Label bgTimeLabel = new Label("Hintergrund Scan-Intervall:");
		final ComboBox<String> bgTimeComboBox = new ComboBox<>();
		bgTimeComboBox.getItems().addAll(BgTime.getNames());
		bgTimeComboBox.getSelectionModel().select(BgTime.MIN_30.getName());
		bgTimeComboBox.disableProperty().bind(bgSyncCheck.selectedProperty().not());

		final VBox optionsBox = new VBox(12, subDirCheck, trashbinCheck, autoDelCheck, logOnCheck, bgSyncCheck, new HBox(8, bgTimeLabel, bgTimeComboBox));
		grid.add(optionsBox, 0, 3, 2, 1);

		// --- Section 4: Globale Parameter (ScanTime & Systemstart) ---
		final Label globalTitle = new Label("Globale System-Konfiguration");
		globalTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
		grid.add(globalTitle, 0, 4, 2, 1);

		/*		final Label scanTimeLabel = new Label("Standard Parallel-Scan Limit:");
				final ComboBox<String> scanTimeComboBox = new ComboBox<>();
				scanTimeComboBox.getItems().addAll(ScanType.getAllDescriptions());
				scanTimeComboBox.getSelectionModel().select(ScanType.FLAT_SCAN.getDescription()); // Standardfall binden
		*/
		final CheckBox globalAutostartCheck = new CheckBox("DataSync beim Systemstart minimiert laden (Autostart OS)");
		globalAutostartCheck.setSelected(pref.isBgSync());

//		grid.add(new VBox(10, new HBox(8, scanTimeLabel, scanTimeComboBox), globalAutostartCheck), 0, 5, 2, 1);
		grid.add(new VBox(10, globalAutostartCheck), 0, 5, 2, 1);

		// --- Save Button Action Triggers ---
		final Button saveButton = new Button("Einstellungen speichern", createIcon(MaterialDesignD.DISC));
		saveButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 24px;");

		// Event-Handler injiziert Daten-Dump zurück in die State Engine
		saveButton.setOnAction(e -> {
			pref.setSubDir(subDirCheck.isSelected());
			pref.setTrashbin(trashbinCheck.isSelected());
			pref.setAutoDel(autoDelCheck.isSelected());
			pref.setLogOn(logOnCheck.isSelected());
			pref.setBgSync(bgSyncCheck.isSelected());

			// Enum Rücktransformation über den ausgewählten Namen
			if (bgSyncCheck.isSelected()) {
				pref.setBgTime(BgTime.get(bgTimeComboBox.getValue()));
			}

			helper.setOSAutostart(globalAutostartCheck.isSelected());
			System.out.println("[Config] Settings persistently committed for: " + job.getJobName());

			// Kehre nach dem Speichern zurück in die Monitor-Ansicht
			currentViewState = ViewState.MONITOR;
			switchToTaskContextView();
		});

		final HBox buttonRow = new HBox(saveButton);
		buttonRow.setAlignment(Pos.CENTER_RIGHT);
		grid.add(buttonRow, 1, 6);

		return grid;
	}

	/**
	 * Renders context-specific inputs depending on the active operational action mode.
	 */
	private void renderContextSpecificPathFields(ScanType type, VBox container, Preference pref) {
		container.getChildren().clear();

		final Label title = new Label("Verzeichnis-Konfiguration (" + type.getDescription() + ")");
		title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
		container.getChildren().add(title);

		if (ScanType.SYNCHRONIZE.equals(type)) {
			// 1 Quellordner, 1 Zielordner
			final GridPane pathsGrid = new GridPane();
			pathsGrid.setHgap(12);
			pathsGrid.setVgap(10);

			final TextField srcField = new TextField(pref.getSourcePath() != null ? Arrays.toString(pref.getSourcePath().toArray()) : "");
			srcField.setPrefWidth(400);
			final Button srcBtn = new Button("Durchsuchen...");
			srcBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(windowStage);
				final ArrayList<Path> p = new ArrayList<>();
				p.add(Paths.get(f.getAbsolutePath()));
				if (f != null) {
					f.getAbsolutePath();
					srcField.setText(f.getAbsolutePath());
					pref.setSourcePath(p);
				}
			});
			pathsGrid.add(new Label("Quellverzeichnis:"), 0, 0);
			pathsGrid.add(new HBox(8, srcField, srcBtn), 1, 0);

			final TextField destField = new TextField(pref.getDestPath() != null ? pref.getDestPath().toString() : "");
			destField.setPrefWidth(400);
			final Button destBtn = new Button("Durchsuchen...");
			destBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(windowStage);
				final ArrayList<Path> p = new ArrayList<>();
				p.add(Paths.get(f.getAbsolutePath()));
				if (f != null) {
					destField.setText(f.getAbsolutePath());
					pref.setDestPath(p);
				}
			});
			pathsGrid.add(new Label("Zielverzeichnis:"), 0, 1);
			pathsGrid.add(new HBox(8, destField, destBtn), 1, 1);

			container.getChildren().add(pathsGrid);

		} else if (ScanType.FLAT_SCAN.equals(type) || ScanType.DEEP_SCAN.equals(type)) {
			// Multi-Quellordner Stack, 1 Zielordner
			final VBox multiSrcBox = new VBox(6);
			final Label multiLabel = new Label("Quellverzeichnisse (Multi-Source Pathing):");
			multiLabel.setStyle("-fx-font-weight: bold;");

			final ObservableList<String> backupPaths = FXCollections.observableArrayList();
			if (pref.getSourcePath() != null) backupPaths.add(Arrays.toString(pref.getSourcePath().toArray())); // Fallback Mapping

			final ListView<String> pathsListView = new ListView<>(backupPaths);
			pathsListView.setPrefHeight(100);

			final Button addPathBtn = new Button("Ordner hinzufügen", createIcon(MaterialDesignP.PLUS));
			addPathBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(windowStage);
				if (f != null && !backupPaths.contains(f.getAbsolutePath())) backupPaths.add(f.getAbsolutePath());
			});
			final Button remPathBtn = new Button("Entfernen", createIcon(MaterialDesignD.DELETE));
			remPathBtn.setOnAction(e -> {
				final String sel = pathsListView.getSelectionModel().getSelectedItem();
				if (sel != null) backupPaths.remove(sel);
			});

			final HBox listActions = new HBox(8, addPathBtn, remPathBtn);
			multiSrcBox.getChildren().addAll(multiLabel, pathsListView, listActions);

			final GridPane destGrid = new GridPane();
			destGrid.setHgap(12);
			final TextField destField = new TextField(pref.getDestPath() != null ? pref.getDestPath().toString() : "");
			destField.setPrefWidth(400);
			final Button destBtn = new Button("Durchsuchen...");
			destBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(windowStage);
				final ArrayList<Path> p = new ArrayList<>();
				p.add(Paths.get(f.getAbsolutePath()));
				if (f != null) {
					destField.setText(f.getAbsolutePath());
					pref.setDestPath(p);
				}
			});
			destGrid.add(new Label("Zielverzeichnis:"), 0, 0);
			destGrid.add(new HBox(8, destField, destBtn), 1, 0);

			container.getChildren().addAll(multiSrcBox, new Separator(), destGrid);

		} else if (ScanType.DUBLICATE_SCAN.equals(type)) {
			// Nur 1 Quellordner, kein Zielordner
			final GridPane pathsGrid = new GridPane();
			pathsGrid.setHgap(12);

			final TextField srcField = new TextField(pref.getSourcePath() != null ? Arrays.toString(pref.getSourcePath().toArray()) : "");
			srcField.setPrefWidth(400);
			final Button srcBtn = new Button("Durchsuchen...");
			srcBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(windowStage);
				final ArrayList<Path> p = new ArrayList<>();
				p.add(Paths.get(f.getAbsolutePath()));
				if (f != null) {
					srcField.setText(f.getAbsolutePath());
					pref.setSourcePath(p);
				}
			});
			pathsGrid.add(new Label("Scanverzeichnis:"), 0, 0);
			pathsGrid.add(new HBox(8, srcField, srcBtn), 1, 0);

			container.getChildren().add(pathsGrid);
		}
	}

	private Node buildAboutInfoNode() {
		final VBox infoBox = new VBox(12);
		infoBox.setPadding(new Insets(10));

		final Label appTitle = new Label("DataSync Core Engine");
		appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

		final Label version = new Label("Programmversion: " + System.getProperty("java.runtime.version", "21.x-Custom"));
		final Label vendor = new Label("Lizenznehmer / Entwickler: Tom Spirit");
		final Label copyright = new Label("Copyright: Licensed under GNU GPL v3.0 Copyleft System.");

		final Separator sep = new Separator();

		final TextArea legalText = new TextArea(
				"""
						This program is free software; you can redistribute it and/or modify
						it under the terms of the GNU General Public License as published by
						the Free Software Foundation; either version 3 of the License.

						This program is distributed in the hope that it will be useful, without any warranty.""");
		legalText.setEditable(false);
		legalText.setPrefHeight(150);
		legalText.setStyle("-fx-font-family: monospace;");

		infoBox.getChildren().addAll(appTitle, version, vendor, copyright, sep, legalText);
		return infoBox;
	}

	private void triggerApplicationShutdown() {
		final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
		confirmation.setTitle("Programm beenden");
		confirmation.setHeaderText("Möchten Sie DataSync wirklich schließen?");
		confirmation.setContentText("Aktive Hintergrunddienste werden unwiderruflich beendet.");

		final Optional<ButtonType> result = confirmation.showAndWait();
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