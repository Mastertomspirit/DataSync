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

import java.util.Arrays;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.controller.ViewController;
import de.spiritscorp.datasync.io.Preference;

/**
 * Display workspace panel hosting the interactive operational consoles,
 * data lists, detailed execution metadata bars and the dynamic target settings configurations grid.
 *
 * @author Tom Spirit
 */
final class WorkspaceView extends VBox {

	/** The central user interface anchor context managing stage overlays. */
	private final Gui mainGui;
	/** The action routing controller handling state transitions and business logic. */
	private final ViewController controller;
	/** The configuration layout coordinator assembling parameter option controls. */
	private final SettingsGrid settingsGrid;
	/** The visual heading label indicating the active workspace scope. */
	private final Label wrkspcHeaderLabel;
	/** The contextual metadata label displaying active task descriptions. */
	private final Label contextInfoLabel;
	/** The top layout container holding execution toolbar action elements. */
	private final HBox controlToolbar;
	/** The primary content viewport switching between execution outputs. */
	private final StackPane centerViewport;

	/** The scrollable container framing the text-based console output. */
	private final ScrollPane consoleViewNode;
	/** The textual log stream output region rendering live terminal logs. */
	private final TextArea consoleTextArea;
	/** The structural layout pane wrapping the duplicate file assessment grid. */
	private final VBox duplicateViewNode;
	/** The tabular data viewer presenting matching duplicate path records. */
	private TableView<SyncJobContext.FileRow> duplicateTable;

	/** The execution trigger button initiating chosen synchronization flows. */
	private final Button actionButton;
	/** The termination trigger button requesting active task cancellations. */
	private final Button cancelButton;
	/** The destructive cleanup button executing selected file pruning routines. */
	private Button deleteButton;
	/** The quantitative progress bar tracking transaction completion ratios. */
	private final ProgressBar progressBar;
	/** The status message label summarizing system operations in real-time. */
	private final Label statusLabel;

	/**
	 * Prepares layouts and maps operations targets onto implementation controller.
	 *
	 * @param mainGui    Configuration context core coordinator link.
	 * @param controller Strategy abstraction dealing with interface state management mutations.
	 */
	WorkspaceView( final Gui mainGui, final ViewController controller ) {
		this(
				mainGui,
				controller,
				new SettingsGrid( controller, mainGui.getWindowStage(), new ContextPathRenderer() ) );
	}

	/**
	 * For TESTING
	 * <br>
	 * Prepares layouts and maps operations targets onto implementation controller.
	 *
	 * @param mainGui      Configuration context core coordinator link.
	 * @param controller   Strategy abstraction dealing with interface state management mutations.
	 * @param settingsGrid The layout factory engine responsible for parameter control rendering.
	 */
	WorkspaceView( final Gui mainGui, final ViewController controller, final SettingsGrid settingsGrid ) {
		super();
		this.mainGui = mainGui;
		this.controller = controller;
		this.settingsGrid = settingsGrid;
		this.setPadding( new Insets( 24 ) );
		this.setSpacing( 12 );

		wrkspcHeaderLabel = new Label( "Kein Task aktiv" );
		wrkspcHeaderLabel.getStyleClass().addAll( "workspace-header-label" );

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

		this.getChildren().addAll( wrkspcHeaderLabel, contextInfoLabel, controlToolbar, centerViewport, statusFooter );
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
	 *
	 * @param state The target navigation ViewState.
	 * @param job   The selected target sync context model instance.
	 */
	void refreshView( final Gui.ViewState state, final SyncJobContext job ) {

		centerViewport.getChildren().clear();

		if( state == Gui.ViewState.INFO ) {
			wrkspcHeaderLabel.setText( "About" );
			contextInfoLabel.setTooltip( new Tooltip( "" ) );
			contextInfoLabel.setText( "Backup Software" );
			controlToolbar.setVisible( false );
			return;
		}else if( job == null ) { return; }

		String finalTip = "";
		if( state == Gui.ViewState.MONITOR ) {
			wrkspcHeaderLabel.setText( "Task-Monitor: " + job.getJobName() );
			controlToolbar.setVisible( true );

			// Build informative context metadata bar metrics string
			final Preference pref = job.getPreference();
			final String src = pref.getSourcePaths() != null ? Arrays.toString( pref.getSourcePaths().toArray() ) : "Keine Quelle";
			final String dest = pref.getDestPaths() != null && !pref.getDestPaths().isEmpty() ? pref.getDestPaths().toString() : "Kein Ziel";
			final String srcTip = src.replace( '[', ' ' ).replace( ']', ' ' ).replace( ',', '\n' );

			if( ScanType.DUBLICATE_SCAN == job.getSelectedMode() ) {
				finalTip = String.format( """
						Quelle:
						%s
						""", srcTip );
				contextInfoLabel.setText( String.format( "Modus: %s  |  Verzeichnisse: %s", job.getSelectedMode().getDescription(), src ) );
				centerViewport.getChildren().add( duplicateViewNode );
			}else {
				finalTip = String.format( """
						Quelle:
						%s
						Ziel:
						%s
						""", srcTip, dest.replace( '[', ' ' ).replace( ']', ' ' ) );
				contextInfoLabel.setText( String.format( "Modus: %s  |  Quelle: %s  |  Ziel: %s", job.getSelectedMode().getDescription(), src, dest ) );
				centerViewport.getChildren().add( consoleViewNode );
			}
		}else if( state == Gui.ViewState.SETTINGS ) {
			wrkspcHeaderLabel.setText( "Einstellungen für: " + job.getJobName() );
			contextInfoLabel.setText( "Konfiguration der task-spezifischen Ablaufparameter, Dateiattribute und Verzeichnisstrukturen." );
			controlToolbar.setVisible( false );
			displayCustomViewNode( settingsGrid.buildSettingsGridTab( this, job, mainGui.getAvailableThemes() ) );
		}
		contextInfoLabel.setTooltip( new Tooltip( finalTip ) );
	}

	/**
	 * Swaps out current content layouts for custom visual configurations nodes.
	 *
	 * @param content Visual layout UI node element.
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
	 *
	 * @param job Selected pipeline source.
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
		final String origContextText = contextInfoLabel.getText();
		final String originalStyle = contextInfoLabel.getStyle();
		// 1. Clean up any existing status style classes to prevent collision states
		contextInfoLabel.getStyleClass().removeAll( "status-success", "status-error", "status-warning" );

		// 2. Programmatic structural fallback: Set a default color via inline styles
		// This acts as a safety net if the active CSS theme completely lacks the targeted class definition.
		// CHECKSTYLE:OFF its ok without a default
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
					contextInfoLabel.setText( origContextText );
					contextInfoLabel.getStyleClass().remove( cssNotifyStatus.getCssClass() );
					contextInfoLabel.setStyle( originalStyle );
				} ) );

		fallbackTimeline.setCycleCount( 1 );
		fallbackTimeline.play();
	}
}