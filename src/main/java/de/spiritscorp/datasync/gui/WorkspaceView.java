package de.spiritscorp.datasync.gui;

/*-
 * 		Data Sync
 *
 * 		Copyright ©   2022    The Spirit
 * 		@email                        thespirit@spiritscorp.network
 *
 * 		This program is free software; you can redistribute it and/or modify
 * 		it under the terms of the GNU General Public License as published by
 * 		the Free Software Foundation; either version 3 of the License, or
 * 		(at your option) any later version.
 *
 * 		This program is distributed in the hope that it will be useful,
 * 		but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 		See the GNU General Public License for more details.
 *
 * 		You should have received a copy of the GNU General Public License
 * 		along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.datasync.BgTime;
import de.spiritscorp.datasync.Main;
import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.controller.ViewController;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.theme.AppTheme;

/**
 * Display workspace panel hosting the interactive operational consoles,
 * data lists, detailed execution metadata bars and the dynamic target settings configurations grid.
 * * @author Tom Spirit
 */
final class WorkspaceView extends VBox {

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
	WorkspaceView( final Gui mainGui, final ViewController controller ) {
		this.mainGui = mainGui;
		this.controller = controller;
		this.setPadding( new Insets( 24 ) );
		this.setSpacing( 12 );

		workspaceHeaderLabel = new Label( "Kein Task aktiv" );
		workspaceHeaderLabel.getStyleClass().addAll( "workspace-header-label" );

		// Subtitle dynamic information bar containing directories context mapping
		contextInfoLabel = new Label( "" );
		contextInfoLabel.getStyleClass().addAll( "context-info-label" );

		controlToolbar = new HBox( 12 );
		controlToolbar.setAlignment( Pos.CENTER_LEFT );

		actionButton = new Button( "Ausführen", Gui.createIcon( MaterialDesignP.PLAY ) );
		actionButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		actionButton.getStyleClass().addAll( "action-button" );
		actionButton.setTooltip( new Tooltip( "Starte Job" ) );
		cancelButton = new Button( "Abbrechen", Gui.createIcon( MaterialDesignS.STOP ) );
		cancelButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		cancelButton.getStyleClass().addAll( "cancel-button" );
		cancelButton.setTooltip( new Tooltip( "Stoppe Job" ) );

		controlToolbar.getChildren().addAll( actionButton, cancelButton );

		consoleTextArea = new TextArea();
		consoleTextArea.setEditable( false );
		consoleTextArea.getStyleClass().addAll( "console-text-area" );
		consoleViewNode = new ScrollPane( consoleTextArea );
		consoleViewNode.setFitToWidth( true );
		consoleViewNode.setFitToHeight( true );

		duplicateViewNode = assembleDuplicateTableView();
		centerViewport = new StackPane( consoleViewNode );

		final HBox statusFooter = new HBox( 12 );
		statusFooter.setAlignment( Pos.CENTER_LEFT );
		progressBar = new ProgressBar( 0 );
		progressBar.setTooltip( new Tooltip( "Aktueller Fortschritt" ) );
		statusLabel = new Label( "Bereit" );
		statusLabel.setTooltip( new Tooltip( "Aktueller Status" ) );
		statusFooter.getChildren().addAll( progressBar, statusLabel );

		this.getChildren().addAll( workspaceHeaderLabel, contextInfoLabel, controlToolbar, centerViewport, statusFooter );
		setVgrow( centerViewport, Priority.ALWAYS );
	}

	/**
	 * Builds standard layout configuration frame for processing double files arrays.
	 */
	private VBox assembleDuplicateTableView() {
		duplicateTable = new TableView<>();
		duplicateTable.setEditable( true );
		duplicateTable.setFixedCellSize( 24.0 );

		final TableColumn<SyncJobContext.FileRow, Boolean> selCol = new TableColumn<>( "Auswahl" );
		selCol.setCellValueFactory( d -> d.getValue().selectedProperty() );
		selCol.setCellFactory( CheckBoxTableCell.forTableColumn( selCol ) );
		selCol.setPrefWidth( 100 );

		final TableColumn<SyncJobContext.FileRow, String> nameCol = new TableColumn<>( "Dateiname" );
		nameCol.setCellValueFactory( d -> d.getValue().fileNameProperty() );
		nameCol.setPrefWidth( 250 );

		final TableColumn<SyncJobContext.FileRow, String> sizeCol = new TableColumn<>( "Größe" );
		sizeCol.setCellValueFactory( d -> d.getValue().sizeProperty() );
		sizeCol.setPrefWidth( 250 );

		final TableColumn<SyncJobContext.FileRow, String> pathCol = new TableColumn<>( "Pfad" );
		pathCol.setCellValueFactory( d -> d.getValue().pathProperty() );
		pathCol.setPrefWidth( 600 );

		final TableColumn<SyncJobContext.FileRow, String> hashCol = new TableColumn<>( "Hash" );
		hashCol.setCellValueFactory( d -> d.getValue().hashProperty() );
		hashCol.setPrefWidth( 250 );

		duplicateTable.getColumns().addAll( List.of( selCol, nameCol, sizeCol, hashCol, pathCol ) );
		deleteButton = new Button( "Duplikate löschen", Gui.createIcon( MaterialDesignD.DELETE ) );
		deleteButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		deleteButton.getStyleClass().addAll( "delete-button" );
		deleteButton.setTooltip( new Tooltip( "Ausgewählte Dateien werden gelöscht" ) );

		final VBox frame = new VBox( 8, duplicateTable, deleteButton );
		setVgrow( duplicateTable, Priority.ALWAYS );
		return frame;
	}

	/**
	 * Redraws visible frame items based on routing navigation instructions and current job payload state.
	 * * @param state The target navigation ViewState.
	 *
	 * @param job The selected target sync context model instance.
	 */
	void refreshView( final Gui.ViewState state, final SyncJobContext job ) {

		centerViewport.getChildren().clear();

		if( state == Gui.ViewState.MONITOR ) {
			if( job == null ) return;
			workspaceHeaderLabel.setText( "Task-Monitor: " + job.getJobName() );
			controlToolbar.setVisible( true );

			// Build informative context metadata bar metrics string
			final Preference p = job.getPreference();
			final String src = p.getSourcePath() != null ? Arrays.toString( p.getSourcePath().toArray() ) : "Keine Quelle";
			final String dest = p.getDestPath() != null && !p.getDestPath().isEmpty() ? p.getDestPath().toString() : "Kein Ziel";

			if( ScanType.DUBLICATE_SCAN.equals( job.getSelectedMode() ) ) {
				contextInfoLabel.setText( String.format( "Modus: %s  |  Ziel: %s", job.getSelectedMode().getDescription(), src ) );
				centerViewport.getChildren().add( duplicateViewNode );
			}else {
				contextInfoLabel.setText( String.format( "Modus: %s  |  Quelle: %s  |  Ziel: %s", job.getSelectedMode().getDescription(), src, dest ) );
				centerViewport.getChildren().add( consoleViewNode );
			}
		}else if( state == Gui.ViewState.SETTINGS ) {
			if( job == null ) return;
			workspaceHeaderLabel.setText( "Einstellungen für: " + job.getJobName() );
			contextInfoLabel.setText( "Konfiguration der task-spezifischen Ablaufparameter, Dateiattribute und Verzeichnisstrukturen." );
			controlToolbar.setVisible( false );
			displayCustomViewNode( buildSettingsGridTab( job ) );
		}else if( state == Gui.ViewState.INFO ) {
			workspaceHeaderLabel.setText( "About" );
			contextInfoLabel.setText( "Backup Software" );
			controlToolbar.setVisible( false );
		}
	}

	/**
	 * Swaps out current content layouts for custom visual configurations nodes.
	 * * @param content Visual layout UI node element.
	 */
	void displayCustomViewNode( final Node content ) {
		centerViewport.getChildren().clear();
		final ScrollPane scroll = new ScrollPane( content );
		scroll.setFitToWidth( true );
		scroll.setPadding( new Insets( 12 ) );
		centerViewport.getChildren().add( scroll );
	}

	/**
	 * Rebinds background parameters changes metrics values directly onto visual output listeners text nodes.
	 * * @param job Selected pipeline source.
	 */
	void bindJob( final SyncJobContext job ) {
		statusLabel.textProperty().unbind();
		consoleTextArea.textProperty().unbind();
		statusLabel.textProperty().bind( job.statusMessageProperty() );
		consoleTextArea.textProperty().bind( job.logOutputProperty() );
		duplicateTable.setItems( job.getDuplicateFiles() );

		cancelButton.disableProperty().unbind();
		actionButton.disableProperty().unbind();
		cancelButton.disableProperty().bind( job.runningProperty().not() );
		actionButton.disableProperty().bind( job.runningProperty() );

		cancelButton.setOnAction( _ -> controller.handleStopTask( job ) );
		actionButton.setOnAction( _ -> controller.handleExecuteTask( job ) );
		deleteButton.setOnAction( _ -> controller.deleteSelectedDuplicates( job ) );
	}

	/**
	 * Displays a temporary status message within the context info banner.
	 * Automatically reverts to the default baseline description after a set duration.
	 * Includes programmatic safety fallbacks if the active theme lacks CSS class declarations.
	 *
	 * @param message         The localized text string to display.
	 * @param cssNotifyStatus The status for the notification..
	 * @param durationSec     The visibility duration in seconds before auto-reverting.
	 */
	void displayTemporaryStatus( final String message, final NotifyStatus cssNotifyStatus, final int durationSec ) {
		final String originalContextText = contextInfoLabel.getText();
		final String originalStyle = contextInfoLabel.getStyle();
		// 1. Clean up any existing status style classes to prevent collision states
		contextInfoLabel.getStyleClass().removeAll( "status-success", "status-error", "status-warning" );

		// 2. Programmatic structural fallback: Set a default color via inline styles
		// This acts as a safety net if the active CSS theme completely lacks the targeted class definition.
		// CHECKSTYLE:OFF
		switch( cssNotifyStatus ) {
			case SUCCESS -> {
				contextInfoLabel.setStyle( "-fx-text-fill: #22aa22; -fx-font-weight: bold;" );
				contextInfoLabel.setText( "✔ " + message );
			}
			case ERROR -> {
				contextInfoLabel.setStyle( "-fx-text-fill: #ff3333; -fx-font-weight: bold;" );
				contextInfoLabel.setText( "❌ " + message );
			}
			case WARNING -> {
				contextInfoLabel.setStyle( "-fx-text-fill: #ffaa00; -fx-font-weight: bold;" );
				contextInfoLabel.setText( "⚠ " + message );
			}
		}
		// CHECKSTYLE:ON

		// 3. Inject the theme's class rule.
		// If the theme defines this class, the stylesheet will cleanly override our inline fallback style.
		contextInfoLabel.getStyleClass().addFirst( cssNotifyStatus.getCssClass() );

		// Initialize asynchronous fade-out/revert timer
		final Timeline fallbackTimeline = new Timeline( new KeyFrame(
				Duration.seconds( durationSec ),
				_ -> {
					contextInfoLabel.setText( originalContextText );
					contextInfoLabel.getStyleClass().remove( cssNotifyStatus.getCssClass() );
					contextInfoLabel.setStyle( originalStyle );
				} ) );

		fallbackTimeline.setCycleCount( 1 );
		fallbackTimeline.play();
	}

	/**
	 * Assembles all parameter configurations fields structured nicely within grid metrics elements.
	 */
	private Node buildSettingsGridTab( final SyncJobContext job ) {
		final Preference pref = job.getPreference();
		final GridPane settingsGrid = new GridPane();
		settingsGrid.setHgap( 24 );
		settingsGrid.setVgap( 16 );
		settingsGrid.setPadding( new Insets( 10 ) );
		settingsGrid.getStyleClass().addAll( "settings-grid" );

		// --- Section 1: Execution Mode Selection ---
		final Label modeLabel = new Label( "Ausführungsmodus:" );
		modeLabel.getStyleClass().addAll( "mode-label" );
		final ComboBox<String> taskModeComboBox = new ComboBox<>();
		taskModeComboBox.setTooltip( new Tooltip( "Listet die möglichen Betriebsmodis auf" ) );
		taskModeComboBox.getItems().addAll( ScanType.getAllDescriptions() );
		job.selectedModeProperty().set( pref.getScanMode() != null ? pref.getScanMode().getDescription() : ScanType.FLAT_SCAN.getDescription() );
		taskModeComboBox.valueProperty().bindBidirectional( job.selectedModeProperty() );
		taskModeComboBox.setPrefWidth( 260 );
		settingsGrid.add( modeLabel, 0, 0 );
		settingsGrid.add( taskModeComboBox, 1, 0 );

		// --- Section 2: Context Paths Configurations Box ---
		final VBox dynamicPathsContainer = new VBox( 12 );
		settingsGrid.add( dynamicPathsContainer, 0, 1, 2, 1 );

		final PathContext pathCtx = new PathContext();
		if( pref.getSourcePath() != null ) {
			pathCtx.sources.addAll( pref.getSourcePath() );
		}
		if( pref.getDestPath() != null ) {
			pathCtx.destinations.addAll( pref.getDestPath() );
		}

		taskModeComboBox.valueProperty().addListener( ( obs, o, n ) -> renderContextPaths( ScanType.get( n ), dynamicPathsContainer, pref, pathCtx ) );
		renderContextPaths( job.getSelectedMode(), dynamicPathsContainer, pref, pathCtx );

		// --- Section 3: Task Parameter Flags Options ---
		final Label paramsTitleLabel = new Label( "Erweiterte Ablaufparameter" );
		paramsTitleLabel.getStyleClass().addAll( "params-title-label" );

		final CheckBox subDirCheck = new CheckBox( "Unterordner einbeziehen (SubDir)" );
		subDirCheck.setSelected( pref.isSubDir() );
		final String subDirText = """
				Aktiviert: Kopiert nur die nackten Dateien und Unterordner DIREKT in das Zielverzeichnis
				(ideal, um mehrere Quellen in einem einzigen Zielordner zusammenzuführen).
				Deaktiviert: Erstellt für jeden Quellpfad einen eigenen Hauptordner im Zielverzeichnis, um die Quellen sauber voneinander zu trennen.
				""";
		subDirCheck.setTooltip( new Tooltip( subDirText ) );
		final CheckBox trashbinCheck = new CheckBox( "Papierkorb verwenden (Trashbin)" );
		trashbinCheck.setSelected( pref.isTrashbin() );
		trashbinCheck.setTooltip( new Tooltip( "Verschiebt modifizierte/gelöschte Dateien temporär in Sicherungsstrukturen" ) );
		final CheckBox autoDelCheck = new CheckBox( "Automatisches Löschen erlauben (AutoDel)" );
		autoDelCheck.setSelected( pref.isAutoDel() );
		String autoDelText = """
				Erlaubt dem System, verwaiste Dateien im Zielordner restlos zu bereinigen
				( Nötig für das hintergrund Backup )
				""";
		autoDelCheck.setTooltip( new Tooltip( autoDelText ) );
		final CheckBox autoSyncCheck = new CheckBox( "Automatisches kopieren erlauben (AutoSync)" );
		autoSyncCheck.setSelected( pref.isAutoSync() );
		autoSyncCheck.setTooltip( new Tooltip( "Erlaubt dem System, nach einem Scan alle nötigen Dateien zu kopieren." ) );
		final CheckBox logOnCheck = new CheckBox( "Protokollierung aktivieren (LogOn)" );
		logOnCheck.setSelected( pref.isLogOn() );
		logOnCheck.setTooltip( new Tooltip( "Schreibt detaillierte Transaktionsprotokolle in das System-Logverzeichnis" ) );
		final CheckBox bgSyncCheck = new CheckBox( "Hintergrund-Synchronisation aktiv" );
		bgSyncCheck.setSelected( pref.isBgSync() );
		bgSyncCheck.setTooltip( new Tooltip( "Setzt den Autostart und aktiviert die Hintergrundsyncronisierung im nachfolgenden Intervall" ) );

		final Label bgTimeLabel = new Label( "Hintergrund Scan-Intervall:" );
		final ComboBox<String> bgTimeComboBox = new ComboBox<>();
		bgTimeComboBox.setTooltip( new Tooltip( "Listet die möglichen Job Intervalle auf" ) );
		bgTimeComboBox.getItems().addAll( BgTime.getNames() );
		bgTimeComboBox.getSelectionModel().select( pref.getBgTime() != null ? pref.getBgTime().getName() : BgTime.MIN_30.getName() );
		bgTimeComboBox.disableProperty().bind( bgSyncCheck.selectedProperty().not() );

		final VBox optionsBox = new VBox( 12, paramsTitleLabel, subDirCheck, trashbinCheck, autoDelCheck, autoSyncCheck, logOnCheck, bgSyncCheck, new HBox( 8, bgTimeLabel, bgTimeComboBox ) );
		settingsGrid.add( optionsBox, 0, 2, 2, 1 );

		// --- Section 4: Global Parameters Stack ---
		final Label globalTitleLabel = new Label( "Globale System-Konfiguration" );
		globalTitleLabel.getStyleClass().addAll( "global-title-label" );

		final CheckBox globalAutostartCheck = new CheckBox( "DataSync beim Systemstart minimiert laden (Autostart OS)" );
		globalAutostartCheck.setSelected( PreferenceManager.getInstance().isGlobalAutoStart() ); // Bind status fallback trace

		// NEW: Theme Changer Layout Elements Configuration
		final Label themeLabel = new Label( "Visuelles Anwendungs-Theme:" );
		final ComboBox<AppTheme> themeComboBox = new ComboBox<>( mainGui.getAvailableThemes() );
		themeComboBox.setTooltip( new Tooltip( "Listet alle möglichen Themes auf" ) );
		// Custom cell rendering to display the specific Strategy names cleanly
		themeComboBox.setCellFactory( _ -> new ListCell<>() {
			@Override
			protected void updateItem( final AppTheme item, final boolean empty ) {
				super.updateItem( item, empty );
				setText( empty || item == null ? "" : item.getName() );
			}
		} );
		themeComboBox.setButtonCell( new ListCell<>() {
			@Override
			protected void updateItem( final AppTheme item, final boolean empty ) {
				super.updateItem( item, empty );
				setText( empty || item == null ? "" : item.getName() );
			}
		} );
		themeComboBox.getSelectionModel().select( mainGui.getCurrentTheme() );

		final VBox globalBox = new VBox( 10, globalTitleLabel, globalAutostartCheck, new HBox( 8, themeLabel, themeComboBox ) );
		settingsGrid.add( globalBox, 0, 3, 2, 1 );

		// --- Commit Action Triggers ---
		final Button saveButton = new Button( "Einstellungen speichern", Gui.createIcon( MaterialDesignD.DISC ) );
		saveButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		saveButton.getStyleClass().addAll( "save-button" );
		saveButton.setTooltip( new Tooltip( "Übernimmt alle geänderten Zustandsparameter permanent in die JSON-Konfigurationsdatei" ) );
		saveButton.setOnAction( _ -> {
			PreferenceManager.getInstance().setGlobalAutoStart( globalAutostartCheck.isSelected() );
			PreferenceManager.getInstance().setTheme( themeComboBox.getValue() );
			final Preference jobPref = job.getPreference();
			final ScanType scanType = ScanType.get( taskModeComboBox.getValue() );
			jobPref.setSubDir( subDirCheck.isSelected() );
			jobPref.setTrashbin( trashbinCheck.isSelected() );
			jobPref.setAutoDel( autoDelCheck.isSelected() );
			jobPref.setLogOn( logOnCheck.isSelected() );
			jobPref.setBgSync( bgSyncCheck.isSelected() );
			jobPref.setBgTime( BgTime.get( bgTimeComboBox.getValue() ) );
			jobPref.setAutoSync( autoSyncCheck.isSelected() );
			jobPref.setScanMode( scanType );

			jobPref.setSourcePath( new ArrayList<>( pathCtx.sources ) );
			jobPref.setDestPath( new ArrayList<>( pathCtx.destinations ) );

			if( scanType == ScanType.DUBLICATE_SCAN && !pathCtx.sources.isEmpty() ) {
				jobPref.setStartSourcePath( pathCtx.sources.get( 0 ) );
				jobPref.setStartDestPath( Paths.get( "" ) );
			}else if( !pathCtx.sources.isEmpty() && !pathCtx.destinations.isEmpty() ) {
				jobPref.setStartSourcePath( pathCtx.sources.get( 0 ) );
				jobPref.setStartDestPath( pathCtx.destinations.get( 0 ) );
			}else {
				displayTemporaryStatus( "Fehlender Pfad! Einstellungen nicht gespeichert!", NotifyStatus.WARNING, Main.INFO_DELAY );
				return;
			}
			controller.handleSaveSettings( themeComboBox.getValue() );
		} );

		final HBox buttonRow = new HBox( saveButton );
		buttonRow.setAlignment( Pos.CENTER_RIGHT );
		settingsGrid.add( buttonRow, 1, 4 );

		return settingsGrid;
	}

	/**
	 * Morph layouts rendering dynamically mapped on target selected Action mode definitions.
	 */
	private void renderContextPaths( final ScanType type, final VBox container, final Preference pref, final PathContext pathCtx ) {
		container.getChildren().clear();
		final GridPane pathsGrid = new GridPane();
		pathsGrid.setHgap( 12 );
		pathsGrid.setVgap( 10 );

		final Label dirTitleLabel = new Label( "Verzeichnis-Konfiguration (" + type.getDescription() + ")" );
		dirTitleLabel.getStyleClass().addAll( "dir-title-label" );
		container.getChildren().add( dirTitleLabel );

		if( ScanType.SYNCHRONIZE.equals( type ) || ScanType.DUBLICATE_SCAN.equals( type ) ) {
			final Path initialSrc = ( pref.getSourcePath() != null && !pref.getSourcePath().isEmpty() ) ? pref.getSourcePath().get( 0 ) : null;
			final TextField srcTextField = new TextField( initialSrc.toString() );
			srcTextField.setPrefWidth( 400 );
			final Button srcBtn = new Button( "Durchsuchen..." );
			srcBtn.setOnAction( _ -> {
				final File f = chooseDirectory( initialSrc.toFile(), "Quellverzeichnis für " + pref.getScanMode().getDescription() );
				if( f != null ) {
					srcTextField.setText( f.getAbsolutePath() );
					pathCtx.sources.clear();
					pathCtx.sources.add( Paths.get( f.getAbsolutePath() ) );
				}
			} );
			pathsGrid.add( new Label( ScanType.DUBLICATE_SCAN.equals( type ) ? "Scanverzeichnis:" : "Quellverzeichnis:" ), 0, 0 );
			pathsGrid.add( new HBox( 8, srcTextField, srcBtn ), 1, 0 );
		}

		if( ScanType.SYNCHRONIZE.equals( type ) ) {
			final Path initialDest = ( pref.getDestPath() != null && !pref.getDestPath().isEmpty() ) ? pref.getDestPath().get( 0 ) : null;
			final TextField destField = new TextField( initialDest.toString() );
			destField.setPrefWidth( 400 );
			final Button destBtn = new Button( "Durchsuchen..." );
			destBtn.setOnAction( _ -> {
				final File f = chooseDirectory( initialDest.toFile(), "Zielverzeichnis für " + pref.getScanMode().getDescription() );
				if( f != null ) {
					destField.setText( f.getAbsolutePath() );
					pathCtx.destinations.clear();
					pathCtx.destinations.add( Paths.get( f.getAbsolutePath() ) );
				}
			} );
			pathsGrid.add( new Label( "Zielverzeichnis:" ), 0, 1 );
			pathsGrid.add( new HBox( 8, destField, destBtn ), 1, 1 );
		}

		if( ScanType.FLAT_SCAN.equals( type ) || ScanType.DEEP_SCAN.equals( type ) ) {
			final VBox multiSrcBox = new VBox( 6 );
			final Label multiSrcLabel = new Label( "Quellverzeichnisse (Multi-Source Pathing):" );
			multiSrcLabel.getStyleClass().addAll( "multi-src-label" );

			final ObservableList<String> backupPaths = FXCollections.observableArrayList();
			for( final Path p : pathCtx.sources ) {
				backupPaths.add( p.toString() );
			}

			final ListView<String> pathsListView = new ListView<>( backupPaths );
			pathsListView.setPrefHeight( 100 );
			final Button add = new Button( "Verzeichnis hinzufügen", Gui.createIcon( MaterialDesignP.PLUS ) );
			add.setOnAction( _ -> {
				final File f = chooseDirectory( pathCtx.sources.getLast().toFile(), "Quellverzeichnis für " + pref.getScanMode().getDescription() );
				if( f != null && !backupPaths.contains( f.getAbsolutePath() ) ) {
					backupPaths.add( f.getAbsolutePath() );
					pathCtx.sources.add( Paths.get( f.getAbsolutePath() ) );
				}
			} );
			final Button rem = new Button( "Entfernen", Gui.createIcon( MaterialDesignD.DELETE ) );
			rem.setOnAction( _ -> {
				final String sel = pathsListView.getSelectionModel().getSelectedItem();
				if( sel != null ) {
					backupPaths.remove( sel );
					pathCtx.sources.remove( Paths.get( sel ) );
				}
			} );

			multiSrcBox.getChildren().addAll( multiSrcLabel, pathsListView, new HBox( 8, add, rem ) );
			pathsGrid.add( multiSrcBox, 0, 0, 2, 1 );

			final GridPane destGrid = new GridPane();
			destGrid.setHgap( 12 );
			final String initialDest = ( pref.getDestPath() != null && !pref.getDestPath().isEmpty() ) ? pref.getDestPath().get( 0 ).toString() : "";
			final TextField destField = new TextField( initialDest );
			destField.setPrefWidth( 400 );
			final Button destBtn = new Button( "Durchsuchen..." );
			destBtn.setOnAction( _ -> {
				final File f = chooseDirectory( pathCtx.destinations.getFirst().toFile(), "Ziielverzeichnis für " + pref.getScanMode().getDescription() );
				if( f != null ) {
					destField.setText( f.getAbsolutePath() );
					pathCtx.destinations.clear();
					pathCtx.destinations.add( Paths.get( f.getAbsolutePath() ) );
				}
			} );
			destGrid.add( new Label( "Zielverzeichnis:" ), 0, 0 );
			destGrid.add( new HBox( 8, destField, destBtn ), 1, 0 );
			pathsGrid.add( new VBox( 10, new Separator(), destGrid ), 0, 1, 2, 1 );
		}

		container.getChildren().add( pathsGrid );
	}

	private File chooseDirectory( final File initialDir, final String title ) {
		final DirectoryChooser chooser = new DirectoryChooser();
		if( initialDir != null && initialDir.exists() ) chooser.setInitialDirectory( initialDir );
		chooser.setTitle( title );
		return chooser.showDialog( mainGui.getWindowStage() );
	}

	/**
	 * Simple structural model container mirroring active paths modifications
	 * decoupled from underlying live Node hierarchies.
	 */
	private static final class PathContext {
		private final ArrayList<Path> sources = new ArrayList<>();
		private final ArrayList<Path> destinations = new ArrayList<>();
	}
}