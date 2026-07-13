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

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import de.spiritscorp.datasync.BgTime;
import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.controller.ViewController;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.theme.AppTheme;

/**
 * Layout factory responsible for assembling the unified task and system configuration grid.
 * <p>
 * This component builds the visual controls required to mutate parameters associated with
 * synchronization jobs, operational scheduling routines, and application-wide preferences.
 * Due to the dense concentration of UI control definitions, standard non-comment source
 * statements (NCSS) and variable length restrictions are intentionally bypassed.
 * </p>
 *
 * @author Tom Spirit
 * @since 1.0.0
 */
@SuppressWarnings( { "PMD.LongVariable", "PMD.NcssCount" } )
class SettingsGrid {

	/** The central UI controller routing finalized state commits to the processing backend. */
	private final ViewController controller;
	/** The window frame context utilized as the anchor owner for modal directory choosing sub-menus. */
	private final Stage primaryStage;
	/** Render delegate dynamically rewriting path configuration containers based on operational modes. */
	private final ContextPathRenderer renderer;

	/** The directory flattening layout configuration checkbox */
	private final CheckBox subDirCheck;
	/** The deleted file backup management structure checkbox */
	private final CheckBox trashbinCheck;
	/** The target structure pruning allowance checkbox */
	private final CheckBox autoDelCheck;
	/** The sequential scan automation copy process checkbox */
	private final CheckBox autoSyncCheck;
	/** The background system daemon interval execution checkbox */
	private final CheckBox bgSyncCheck;

	/**
	 * Constructs a new configuration layout coordinator backed by required UI dependencies.
	 *
	 * @param controller   The main application view action routing hub
	 * @param primaryStage The root stage window managing modal overlays
	 * @param renderer     The path renderer manipulating context-sensitive folder controls
	 */
	SettingsGrid( final ViewController controller, final Stage primaryStage, final ContextPathRenderer renderer ) {
		this.controller = controller;
		this.primaryStage = primaryStage;
		this.renderer = renderer;
		this.subDirCheck = new CheckBox( "Unterordner einbeziehen (SubDir)" );
		this.trashbinCheck = new CheckBox( "Papierkorb verwenden (Trashbin)" );
		this.autoDelCheck = new CheckBox( "Automatisches Löschen erlauben (AutoDel)" );
		this.autoSyncCheck = new CheckBox( "Automatisches kopieren erlauben (AutoSync)" );
		this.bgSyncCheck = new CheckBox( "Hintergrund-Synchronisation aktiv" );
	}

	/**
	 * Assembles all parameter configuration fields structured nicely within grid metrics elements.
	 *
	 * @param workspaceView   The visual layout shell providing volatile overlay notification services
	 * @param job             The context model holding currently evaluated task parameters
	 * @param availableThemes The observable catalog of registered UI layout skins
	 * @return A constructed {@link Node} containing the complete scrollable dashboard layout
	 */
	Node buildSettingsGridTab( final WorkspaceView workspaceView, final SyncJobContext job, final ObservableList<AppTheme> availableThemes ) {
		final Preference pref = job.getPreference();

		// Initialize the base grid layout matrix
		final GridPane settingsGrid = new GridPane();
		settingsGrid.setHgap( 24 );
		settingsGrid.setVgap( 16 );
		settingsGrid.setPadding( new Insets( 10 ) );
		settingsGrid.getStyleClass().addAll( "settings-grid" );

		// ---------------------------------------------------------------------
		// SECTION 1: Scan Type Selection
		// ---------------------------------------------------------------------
		final Label modeLabel = new Label( "Ausführungsmodus:" );
		modeLabel.getStyleClass().addAll( "mode-label" );

		// Scan Type Combo Box
		final ComboBox<String> taskModeComboBox = new ComboBox<>();
		taskModeComboBox.setTooltip( new Tooltip( "Listet die möglichen Betriebsmodis auf" ) );
		taskModeComboBox.getItems().addAll( ScanType.getAllDescriptions() );

		// Bind selection bidirectional to preserve model state shifts immediately
		job.selectedModeProperty().set( pref.getScanMode() != null ? pref.getScanMode().getDescription() : ScanType.FLAT_SCAN.getDescription() ); // NOPMD LawOfDemeter
		taskModeComboBox.valueProperty().bindBidirectional( job.selectedModeProperty() );
		taskModeComboBox.setPrefWidth( 260 );

		settingsGrid.add( modeLabel, 0, 0 );
		settingsGrid.add( taskModeComboBox, 1, 0 );

		// ---------------------------------------------------------------------
		// SECTION 2: Context Paths Configurations Box
		// ---------------------------------------------------------------------
		final VBox dynamicPathsContainer = new VBox( 12 );
		settingsGrid.add( dynamicPathsContainer, 0, 1, 2, 1 );

		// Establish listener to dynamically rebuild path boxes on operational changes
		taskModeComboBox.valueProperty().addListener( ( _, _, n ) -> {
			renderer.renderContextPaths( ScanType.get( n ), dynamicPathsContainer, pref, primaryStage );
			applyModeRestrictions( ScanType.get( n ), pref );
		} );
		renderer.renderContextPaths( job.getSelectedMode(), dynamicPathsContainer, pref, primaryStage );

		// ---------------------------------------------------------------------
		// SECTION 3: Task Parameter Flags Options
		// ---------------------------------------------------------------------
		final Label paramsTitleLabel = new Label( "Erweiterte Ablaufparameter" );
		paramsTitleLabel.getStyleClass().addAll( "params-title-label" );

		subDirCheck.setSelected( pref.isSubDir() );
		final String subDirText = """
				Aktiviert: Kopiert nur die nackten Dateien und Unterordner DIREKT in das Zielverzeichnis
				(ideal, um mehrere Quellen in einem einzigen Zielordner zusammenzuführen).
				Deaktiviert: Erstellt für jeden Quellpfad einen eigenen Hauptordner im Zielverzeichnis, um die Quellen sauber voneinander zu trennen.
				""";
		subDirCheck.setTooltip( new Tooltip( subDirText ) );

		trashbinCheck.setSelected( pref.isTrashbin() );
		trashbinCheck.setTooltip( new Tooltip( "Verschiebt modifizierte/gelöschte Dateien temporär in Sicherungsstrukturen" ) );

		autoDelCheck.setSelected( pref.isAutoDel() );
		final String autoDelText = """
				Erlaubt dem System, verwaiste Dateien im Zielordner restlos zu bereinigen
				( Nötig für das hintergrund Backup )
				""";
		autoDelCheck.setTooltip( new Tooltip( autoDelText ) );

		autoSyncCheck.setSelected( pref.isAutoSync() );
		autoSyncCheck.setTooltip( new Tooltip( "Erlaubt dem System, nach einem Scan alle nötigen Dateien zu kopieren." ) );

		final CheckBox logOnCheck = new CheckBox( "Protokollierung aktivieren (LogOn)" );
		logOnCheck.setSelected( pref.isLogOn() );
		logOnCheck.setTooltip( new Tooltip( "Schreibt detaillierte Transaktionsprotokolle in das System-Logverzeichnis" ) );

		bgSyncCheck.setSelected( pref.isBgSync() );
		bgSyncCheck.setTooltip( new Tooltip( "Setzt den Autostart und aktiviert die Hintergrundsyncronisierung im nachfolgenden Intervall" ) );

		// Background Scan Time Combo Box
		final Label bgTimeLabel = new Label( "Hintergrund Scan-Intervall:" );
		final ComboBox<String> bgTimeComboBox = new ComboBox<>();
		bgTimeComboBox.setTooltip( new Tooltip( "Listet die möglichen Job Intervalle auf" ) );
		bgTimeComboBox.getItems().addAll( BgTime.getNames() );
		bgTimeComboBox.getSelectionModel().select( pref.getBgTime() != null ? pref.getBgTime().getName() : BgTime.MIN_30.getName() );// NOPMD LawOfDemeter

		// Disable interval settings if the background processing flag is unchecked
		bgTimeComboBox.disableProperty().bind( bgSyncCheck.selectedProperty().not() );

		final VBox optionsBox = new VBox( 12, paramsTitleLabel, subDirCheck, trashbinCheck, autoDelCheck, autoSyncCheck, logOnCheck, bgSyncCheck, new HBox( 8, bgTimeLabel, bgTimeComboBox ) );
		settingsGrid.add( optionsBox, 0, 2, 2, 1 );

		// ---------------------------------------------------------------------
		// SECTION 4: Global Parameters Stack
		// ---------------------------------------------------------------------
		final Label globalTitleLabel = new Label( "Globale System-Konfiguration" );
		globalTitleLabel.getStyleClass().addAll( "global-title-label" );

		final CheckBox globalAutostartCheck = new CheckBox( "DataSync beim Systemstart minimiert laden (Autostart OS)" );
		globalAutostartCheck.setSelected( PreferenceManager.getInstance().isGlobalAutoStart() ); // Bind status fallback trace
		globalAutostartCheck.setOnAction( _ -> controller.handleAutostart( globalAutostartCheck.isSelected() ) );

		// Theme Layout Combo Box
		final Label themeLabel = new Label( "Visuelles Anwendungs-Theme:" );
		final ComboBox<AppTheme> themeComboBox = new ComboBox<>( availableThemes );
		themeComboBox.setTooltip( new Tooltip( "Listet alle möglichen Themes auf" ) );

		// Apply safe custom cell factories to render strategy model labels cleanly
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
		themeComboBox.getSelectionModel().select( PreferenceManager.getInstance().getTheme() );

		final VBox globalBox = new VBox( 10, globalTitleLabel, globalAutostartCheck, new HBox( 8, themeLabel, themeComboBox ) );
		applyModeRestrictions( pref.getScanMode(), pref );
		settingsGrid.add( globalBox, 0, 3, 2, 1 );

		// ---------------------------------------------------------------------
		// SECTION 5: Commit Action Triggers
		// ---------------------------------------------------------------------
		final Button saveButton = new Button( "Einstellungen speichern", Gui.createIcon( MaterialDesignD.DISC ) );
		saveButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		saveButton.getStyleClass().addAll( "save-button" );
		saveButton.setTooltip( new Tooltip( "Übernimmt alle geänderten Zustandsparameter permanent in die JSON-Konfigurationsdatei" ) );
		saveButton.setOnAction( _ -> {
			final ScanType scanType = ScanType.get( taskModeComboBox.getValue() );

			// Structural sanity checklist before executing persistent data dump
			if( scanType == ScanType.DUBLICATE_SCAN && pref.getSourcePaths().isEmpty() ) {
				workspaceView.displayTemporaryStatus( "Fehlender Scan Pfad! Einstellungen nicht gespeichert!", NotifyStatus.WARNING, Gui.INFO_DELAY );
				return;
			}else if( pref.getSourcePaths().isEmpty() && pref.getDestPaths().isEmpty() ) {
				workspaceView.displayTemporaryStatus( "Fehlende Pfad! Einstellungen nicht gespeichert!", NotifyStatus.WARNING, Gui.INFO_DELAY );
				return;
			}

			// Commit application global environment parameters
			PreferenceManager.getInstance().setTheme( themeComboBox.getValue() );

			// Commit isolated job specific parameter values
			final Preference jobPref = job.getPreference();
			jobPref.setSubDir( subDirCheck.isSelected() );
			jobPref.setTrashbin( trashbinCheck.isSelected() );
			jobPref.setAutoDel( autoDelCheck.isSelected() );
			jobPref.setLogOn( logOnCheck.isSelected() );
			jobPref.setBgSync( bgSyncCheck.isSelected() );
			jobPref.setBgTime( BgTime.get( bgTimeComboBox.getValue() ) );
			jobPref.setAutoSync( autoSyncCheck.isSelected() );
			jobPref.setScanMode( scanType );

			// Delegate layout update processing onwards to core view thread orchestrator
			controller.handleSaveSettings( themeComboBox.getValue() );
		} );

		final HBox buttonRow = new HBox( saveButton );
		buttonRow.setAlignment( Pos.CENTER_RIGHT );
		settingsGrid.add( buttonRow, 1, 4 );

		return settingsGrid;
	}

	/**
	 * Evaluates and modifies the accessibility and selection states of option checkboxes
	 * according to the active functional {@link ScanType} guidelines.
	 *
	 * @param type The target operational mode currently active in the UI combo selection
	 * @param pref The backing config configuration holding safe fallback default values
	 */
	private void applyModeRestrictions( final ScanType type, final Preference pref ) {
		// Always ensure trashbin remains modifiable and matches user specs
		trashbinCheck.setSelected( pref.isTrashbin() );
//CHECKSTYLE:OFF
		switch( type ) {
			case DUBLICATE_SCAN -> {
				// Forced uncheck and full disablement rules
				subDirCheck.setDisable( true );
				subDirCheck.setSelected( false );

				autoDelCheck.setDisable( true );
				autoDelCheck.setSelected( false );

				autoSyncCheck.setDisable( true );
				autoSyncCheck.setSelected( false );

				bgSyncCheck.setDisable( true );
				bgSyncCheck.setSelected( false );
			}
			case SYNCHRONIZE -> {
				// Access block only - keep native property settings representation alive
				subDirCheck.setDisable( true );
				subDirCheck.setSelected( pref.isSubDir() );

				autoDelCheck.setDisable( false );
				autoDelCheck.setSelected( pref.isAutoDel() );

				autoSyncCheck.setDisable( true );
				autoSyncCheck.setSelected( pref.isAutoSync() );

				bgSyncCheck.setDisable( false );
				bgSyncCheck.setSelected( pref.isBgSync() );
			}
			case FLAT_SCAN, DEEP_SCAN -> {
				// Full evaluation processing release - reload tracking references safely
				subDirCheck.setDisable( false );
				subDirCheck.setSelected( pref.isSubDir() );

				autoDelCheck.setDisable( false );
				autoDelCheck.setSelected( pref.isAutoDel() );

				autoSyncCheck.setDisable( false );
				autoSyncCheck.setSelected( pref.isAutoSync() );

				bgSyncCheck.setDisable( false );
				bgSyncCheck.setSelected( pref.isBgSync() );
			}
		}
		// CHECKSTYLE:ON
	}
}
