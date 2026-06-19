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
import java.util.List;

import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.Controller.ViewController;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Theme.AppTheme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Display workspace panel hosting the interactive operational consoles,
 * data lists, detailed execution metadata bars and the dynamic target settings configurations grid.
 * * @author Tom Spirit
 */
public class WorkspaceView extends VBox {

	private final Gui mainGui;
	private final ViewController controller;
	private final Label workspaceHeaderLabel;
	private final Label contextInfoLabel;
	private final HBox controlToolbar;
	private final StackPane centerViewport;

	private final ScrollPane consoleViewNode;
	private final TextArea consoleTextArea;
	private final VBox duplicateViewNode;
	private TableView<SyncJobContext.FileRow> duplicateTable;

	private final Button actionButton;
	private final Button cancelButton;
	private Button deleteButton;
	private final ProgressBar progressBar;
	private final Label statusLabel;

	/**
	 * Prepares layouts and maps operations targets onto implementation controller.
	 *
	 * @param mainGui    Configuration context core coordinator link.
	 * @param controller Strategy abstraction dealing with interface state management mutations.
	 */
	public WorkspaceView(Gui mainGui, ViewController controller) {
		this.mainGui = mainGui;
		this.controller = controller;
		this.setPadding(new Insets(24));
		this.setSpacing(12);

		workspaceHeaderLabel = new Label("Kein Task aktiv");
		workspaceHeaderLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

		// Subtitle dynamic information bar containing directories context mapping
		contextInfoLabel = new Label("");
		contextInfoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 0 0 8px 0;");

		controlToolbar = new HBox(12);
		controlToolbar.setAlignment(Pos.CENTER_LEFT);

		actionButton = new Button("Ausführen", Gui.createIcon(MaterialDesignP.PLAY));
		actionButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");

		cancelButton = new Button("Abbrechen", Gui.createIcon(MaterialDesignS.STOP));
		cancelButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 16px;");
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

		this.getChildren().addAll(workspaceHeaderLabel, contextInfoLabel, controlToolbar, centerViewport, statusFooter);
		VBox.setVgrow(centerViewport, Priority.ALWAYS);
	}

	/**
	 * Builds standard layout configuration frame for processing double files arrays.
	 */
	private VBox assembleDuplicateTableView() {
		duplicateTable = new TableView<>();
		duplicateTable.setEditable(true);

		final TableColumn<SyncJobContext.FileRow, Boolean> selCol = new TableColumn<>("Auswahl");
		selCol.setCellValueFactory(d -> d.getValue().selectedProperty());
		selCol.setCellFactory(CheckBoxTableCell.forTableColumn(selCol));
		selCol.setPrefWidth(70);

		final TableColumn<SyncJobContext.FileRow, String> nameCol = new TableColumn<>("Dateiname");
		nameCol.setCellValueFactory(d -> d.getValue().fileNameProperty());
		nameCol.setPrefWidth(200);

		final TableColumn<SyncJobContext.FileRow, String> pathCol = new TableColumn<>("Pfad");
		pathCol.setCellValueFactory(d -> d.getValue().pathProperty());
		pathCol.setPrefWidth(400);

		duplicateTable.getColumns().addAll(List.of(selCol, nameCol, pathCol));
		deleteButton = new Button("Duplikate löschen", Gui.createIcon(MaterialDesignD.DELETE));
		deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

		final VBox frame = new VBox(8, duplicateTable, deleteButton);
		VBox.setVgrow(duplicateTable, Priority.ALWAYS);
		return frame;
	}

	/**
	 * Redraws visible frame items based on routing navigation instructions and current job payload state.
	 * * @param state The target navigation ViewState.
	 *
	 * @param job The selected target sync context model instance.
	 */
	public void refreshView(Gui.ViewState state, SyncJobContext job) {
		if (job == null) return;

		centerViewport.getChildren().clear();

		if (state == Gui.ViewState.MONITOR) {
			workspaceHeaderLabel.setText("Task-Monitor: " + job.getJobName());
			controlToolbar.setVisible(true);

			// Build informative context metadata bar metrics string
			final Preference p = job.getPreference();
			final String src = p.getSourcePath() != null ? Arrays.toString(p.getSourcePath().toArray()) : "Keine Quelle";
			final String dest = p.getDestPath() != null && !p.getDestPath().isEmpty() ? p.getDestPath().toString() : "Kein Ziel";
			contextInfoLabel.setText(String.format("Modus: %s  |  Quelle: %s  |  Ziel: %s", job.getSelectedMode().getDescription(), src, dest));

			if (ScanType.DUBLICATE_SCAN.equals(job.getSelectedMode())) {
				centerViewport.getChildren().add(duplicateViewNode);
			} else {
				centerViewport.getChildren().add(consoleViewNode);
			}
		} else if (state == Gui.ViewState.SETTINGS) {
			workspaceHeaderLabel.setText("Einstellungen für: " + job.getJobName());
			contextInfoLabel.setText("Konfiguration der task-spezifischen Ablaufparameter, Dateiattribute und Verzeichnisstrukturen.");
			controlToolbar.setVisible(false);
			displayCustomViewNode(buildSettingsGridTab(job));
		}
	}

	/**
	 * Swaps out current content layouts for custom visual configurations nodes.
	 * * @param content Visual layout UI node element.
	 */
	public void displayCustomViewNode(Node content) {
		centerViewport.getChildren().clear();
		final ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		scroll.setPadding(new Insets(12));
		scroll.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");
		centerViewport.getChildren().add(scroll);
	}

	/**
	 * Rebinds background parameters changes metrics values directly onto visual output listeners text nodes.
	 * * @param job Selected pipeline source.
	 */
	public void bindJob(SyncJobContext job) {
		statusLabel.textProperty().unbind();
		consoleTextArea.textProperty().unbind();
		statusLabel.textProperty().bind(job.statusMessageProperty());
		consoleTextArea.textProperty().bind(job.logOutputProperty());
		duplicateTable.setItems(job.getDuplicateFiles());

		cancelButton.disableProperty().unbind();
		actionButton.disableProperty().unbind();
		cancelButton.disableProperty().bind(job.runningProperty().not());
		actionButton.disableProperty().bind(job.runningProperty());

		cancelButton.setOnAction(e -> job.cancelRunningTask());
		actionButton.setOnAction(e -> controller.handleExecuteTask(job));
	}

	/**
	 * Assembles all parameter configurations fields structured nicely within grid metrics elements.
	 */
	private Node buildSettingsGridTab(SyncJobContext job) {
		final Preference pref = job.getPreference();
		final GridPane grid = new GridPane();
		grid.setHgap(24);
		grid.setVgap(16);
		grid.setPadding(new Insets(10));

		// --- Section 1: Execution Mode Selection ---
		final Label modeTitle = new Label("Ausführungsmodus:");
		modeTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
		final ComboBox<String> taskModeComboBox = new ComboBox<>();
		taskModeComboBox.getItems().addAll(ScanType.getAllDescriptions());
		taskModeComboBox.valueProperty().bindBidirectional(job.selectedModeProperty());
		taskModeComboBox.setPrefWidth(260);
		grid.add(modeTitle, 0, 0);
		grid.add(taskModeComboBox, 1, 0);

		// --- Section 2: Context Paths Configurations Box ---
		final VBox dynamicPathsContainer = new VBox(12);
		grid.add(dynamicPathsContainer, 0, 1, 2, 1);

		taskModeComboBox.valueProperty().addListener((obs, o, n) -> renderContextPaths(ScanType.get(n), dynamicPathsContainer, pref));
		renderContextPaths(job.getSelectedMode(), dynamicPathsContainer, pref);

		// --- Section 3: Task Parameter Flags Options ---
		final Label paramsTitle = new Label("Erweiterte Ablaufparameter");
		paramsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

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
		bgTimeComboBox.getSelectionModel().select(pref.getBgTime() != null ? pref.getBgTime().getName() : BgTime.MIN_30.getName());
		bgTimeComboBox.disableProperty().bind(bgSyncCheck.selectedProperty().not());

		final VBox optionsBox = new VBox(12, paramsTitle, subDirCheck, trashbinCheck, autoDelCheck, logOnCheck, bgSyncCheck, new HBox(8, bgTimeLabel, bgTimeComboBox));
		grid.add(optionsBox, 0, 2, 2, 1);

		// --- Section 4: Global Parameters Stack ---
		final Label globalTitle = new Label("Globale System-Konfiguration");
		globalTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

		final CheckBox globalAutostartCheck = new CheckBox("DataSync beim Systemstart minimiert laden (Autostart OS)");
		globalAutostartCheck.setSelected(pref.isBgSync()); // Bind status fallback trace

		// NEW: Theme Changer Layout Elements Configuration
		final Label themeLabel = new Label("Visuelles Anwendungs-Theme:");
		final ComboBox<AppTheme> themeComboBox = new ComboBox<>(mainGui.getAvailableThemes());

		// Custom cell rendering to display the specific Strategy names cleanly
		themeComboBox.setCellFactory(lv -> new ListCell<>() {
			@Override
			protected void updateItem(AppTheme item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item.getName());
			}
		});
		themeComboBox.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(AppTheme item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item.getName());
			}
		});
		themeComboBox.getSelectionModel().select(mainGui.getCurrentTheme());

		final VBox globalBox = new VBox(10, globalTitle, globalAutostartCheck, new HBox(8, themeLabel, themeComboBox));
		grid.add(globalBox, 0, 3, 2, 1);

		// --- Commit Action Triggers ---
		final Button saveButton = new Button("Einstellungen speichern", Gui.createIcon(MaterialDesignD.DISC));
		saveButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 24px;");
		saveButton.setOnAction(e -> controller.handleSaveSettings(
				job,
				subDirCheck.isSelected(),
				trashbinCheck.isSelected(),
				autoDelCheck.isSelected(),
				logOnCheck.isSelected(),
				bgSyncCheck.isSelected(),
				bgTimeComboBox.getValue(),
				themeComboBox.getValue(),
				globalAutostartCheck.isSelected()));

		final HBox buttonRow = new HBox(saveButton);
		buttonRow.setAlignment(Pos.CENTER_RIGHT);
		grid.add(buttonRow, 1, 4);

		return grid;
	}

	/**
	 * Morph layouts rendering dynamically mapped on target selected Action mode definitions.
	 */
	private void renderContextPaths(ScanType type, VBox container, Preference pref) {
		container.getChildren().clear();
		final GridPane pathsGrid = new GridPane();
		pathsGrid.setHgap(12);
		pathsGrid.setVgap(10);

		final Label title = new Label("Verzeichnis-Konfiguration (" + type.getDescription() + ")");
		title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
		container.getChildren().add(title);

		if (ScanType.SYNCHRONIZE.equals(type) || ScanType.DUBLICATE_SCAN.equals(type)) {
			final String initialSrc = (pref.getSourcePath() != null && !pref.getSourcePath().isEmpty()) ? pref.getSourcePath().get(0).toString() : "";
			final TextField srcField = new TextField(initialSrc);
			srcField.setPrefWidth(400);
			final Button srcBtn = new Button("Durchsuchen...");
			srcBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(mainGui.getWindowStage());
				if (f != null) {
					srcField.setText(f.getAbsolutePath());
					final ArrayList<Path> paths = new ArrayList<>();
					paths.add(Paths.get(f.getAbsolutePath()));
					pref.setSourcePath(paths);
				}
			});
			pathsGrid.add(new Label(ScanType.DUBLICATE_SCAN.equals(type) ? "Scanverzeichnis:" : "Quellverzeichnis:"), 0, 0);
			pathsGrid.add(new HBox(8, srcField, srcBtn), 1, 0);
		}

		if (ScanType.SYNCHRONIZE.equals(type)) {
			final String initialDest = (pref.getDestPath() != null && !pref.getDestPath().isEmpty()) ? pref.getDestPath().get(0).toString() : "";
			final TextField destField = new TextField(initialDest);
			destField.setPrefWidth(400);
			final Button destBtn = new Button("Durchsuchen...");
			destBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(mainGui.getWindowStage());
				if (f != null) {
					destField.setText(f.getAbsolutePath());
					final ArrayList<Path> paths = new ArrayList<>();
					paths.add(Paths.get(f.getAbsolutePath()));
					pref.setDestPath(paths);
				}
			});
			pathsGrid.add(new Label("Zielverzeichnis:"), 0, 1);
			pathsGrid.add(new HBox(8, destField, destBtn), 1, 1);
		}

		if (ScanType.FLAT_SCAN.equals(type) || ScanType.DEEP_SCAN.equals(type)) {
			final VBox multiSrcBox = new VBox(6);
			final Label multiLabel = new Label("Quellverzeichnisse (Multi-Source Pathing):");
			multiLabel.setStyle("-fx-font-weight: bold;");

			final ObservableList<String> backupPaths = FXCollections.observableArrayList();
			if (pref.getSourcePath() != null) {
				for (final Path p : pref.getSourcePath())
					backupPaths.add(p.toString());
			}

			final ListView<String> pathsListView = new ListView<>(backupPaths);
			pathsListView.setPrefHeight(100);
			final Button add = new Button("Ordner hinzufügen", Gui.createIcon(MaterialDesignP.PLUS));
			add.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(mainGui.getWindowStage());
				if (f != null && !backupPaths.contains(f.getAbsolutePath())) {
					backupPaths.add(f.getAbsolutePath());
					updatePreferencesMultiPaths(pref, backupPaths);
				}
			});
			final Button rem = new Button("Entfernen", Gui.createIcon(MaterialDesignD.DELETE));
			rem.setOnAction(e -> {
				final String sel = pathsListView.getSelectionModel().getSelectedItem();
				if (sel != null) {
					backupPaths.remove(sel);
					updatePreferencesMultiPaths(pref, backupPaths);
				}
			});

			multiSrcBox.getChildren().addAll(multiLabel, pathsListView, new HBox(8, add, rem));
			pathsGrid.add(multiSrcBox, 0, 0, 2, 1);

			final GridPane destGrid = new GridPane();
			destGrid.setHgap(12);
			final String initialDest = (pref.getDestPath() != null && !pref.getDestPath().isEmpty()) ? pref.getDestPath().get(0).toString() : "";
			final TextField destField = new TextField(initialDest);
			destField.setPrefWidth(400);
			final Button destBtn = new Button("Durchsuchen...");
			destBtn.setOnAction(e -> {
				final File f = new DirectoryChooser().showDialog(mainGui.getWindowStage());
				if (f != null) {
					destField.setText(f.getAbsolutePath());
					final ArrayList<Path> paths = new ArrayList<>();
					paths.add(Paths.get(f.getAbsolutePath()));
					pref.setDestPath(paths);
				}
			});
			destGrid.add(new Label("Zielverzeichnis:"), 0, 0);
			destGrid.add(new HBox(8, destField, destBtn), 1, 0);
			pathsGrid.add(new VBox(10, new Separator(), destGrid), 0, 1, 2, 1);
		}

		container.getChildren().add(pathsGrid);
	}

	/**
	 * Converts structural UI lists updates payload entries directly back onto underlying multi-source files maps.
	 */
	private void updatePreferencesMultiPaths(Preference pref, ObservableList<String> uiList) {
		final ArrayList<Path> updatedPaths = new ArrayList<>();
		for (final String s : uiList)
			updatedPaths.add(Paths.get(s));
		pref.setSourcePath(updatedPaths);
	}
}