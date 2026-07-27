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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import de.spiritscorp.datasync.Main;
import de.spiritscorp.datasync.controller.MainViewController;
import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.controller.ViewController;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.theme.AppTheme;
import de.spiritscorp.datasync.theme.DarkSlateTheme;
import de.spiritscorp.datasync.theme.MatrixTerminalTheme;
import de.spiritscorp.datasync.theme.NordicLightTheme;

/**
 * Main Entry Point Orchestrator managing operational state transactions switcher channels,
 * initialization parameters, and global view configuration lifecycle processes.
 *
 * @author Tom Spirit
 */
public class Gui extends Application {

	/** The delay time in seconds used for displaying or fading out status and informational messages within the GUI. */
	public static final int INFO_DELAY = 4;
	static final String CSS_BUTTON_ICON = "button-icon";
	private static final int ICON_SIZE = 20;

	private final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();
	private ViewController controller;

	private final ObservableList<AppTheme> availableThemes = FXCollections.observableArrayList(
			new DarkSlateTheme(),
			new MatrixTerminalTheme(),
			new NordicLightTheme() );
	private AppTheme currentTheme = availableThemes.getFirst();

	private Scene mainScene;
	private SidebarView sidebarView;
	private WorkspaceView workspaceView;
	private SyncJobContext currentActiveJob;
	private Stage windowStage;

	/**
	 * Represents the structural visibility layers and active UI states of the main Viewport container.
	 */
	public enum ViewState {
		/**
		 * The main monitoring interface showing active background synchronizations and statistics.
		 */
		MONITOR,

		/**
		 * The configuration interface for application-wide rules and job setups.
		 */
		SETTINGS,

		/**
		 * The application info, versioning, and about page layer.
		 */
		INFO
	}

	private ViewState currentViewState = ViewState.MONITOR;

	/**
	 * Utility method allocating custom font vector metrics icons definitions graphics layouts.
	 *
	 * @param ikon Selected base vector item index.
	 *
	 * @return Prepared graphic FontIcon instance node.
	 */
	public static FontIcon createIcon( final Ikon ikon ) {
		final FontIcon icon = new FontIcon( ikon );
		icon.setIconSize( ICON_SIZE );
		return icon;
	}

	@Override
	public void start( final Stage primaryStage ) {
		this.windowStage = primaryStage;
		this.controller = new MainViewController( this );
		this.controller.registerNativeShutdownHook();

		PreferenceManager prefMan = PreferenceManager.getInstance();
		AppTheme theme = prefMan.getTheme();
		if( theme instanceof DarkSlateTheme ) {
			this.currentTheme = availableThemes.get( 0 );
		}else if( theme instanceof MatrixTerminalTheme ) {
			this.currentTheme = availableThemes.get( 1 );
		}else if( theme instanceof NordicLightTheme ) {
			this.currentTheme = availableThemes.get( 2 );
		}
		prefMan.setTheme( currentTheme );

		primaryStage.setTitle( "DataSync Advanced Management Platform" );
		primaryStage.getIcons().add( new Image( getClass().getResourceAsStream( "/icons/16x16.png" ) ) );
		Platform.setImplicitExit( false );
		primaryStage.setOnCloseRequest( _ -> {
			Debug.printDebug( "[Info] Window hidden. Application processing stays active in background." );
			controller.runInBackground( false );
		} );
		sidebarView = new SidebarView( this, controller );
		workspaceView = new WorkspaceView( this, controller );

		final BorderPane mainLayout = new BorderPane();
		mainLayout.setLeft( sidebarView );
		mainLayout.setCenter( workspaceView );

		mainScene = new Scene( mainLayout, 1350, 800 );
		if( !getJobList().isEmpty() ) {
			sidebarView.getSidebarListView().getSelectionModel().select( 0 );
		}
		currentTheme.apply( mainScene );

		primaryStage.setScene( mainScene );
		if( Main.isFirstStart() ) {
			controller.runInBackground( Main.isFirstStart() );
		}else {
			primaryStage.show();
		}
	}

	/**
	 * Updates global active tracking routes navigation indexes updating workspace render cycles.
	 *
	 * @param state Target destination navigation path selection layer.
	 */
	public void setViewState( final ViewState state ) {
		this.currentViewState = state;
		if( state == ViewState.INFO ) {
			workspaceView.refreshView( state, null );
			workspaceView.displayCustomViewNode( buildAboutInfoNode() );
		}else {
			workspaceView.refreshView( currentViewState, currentActiveJob );
		}
	}

	/**
	 * Updates central contextual execution active jobs binding structures hooks.
	 *
	 * @param job Active core source entity context.
	 */
	public void setCurrentActiveJob( final SyncJobContext job ) {
		this.currentActiveJob = job;
		workspaceView.bindJob( job );
		if( this.currentViewState == ViewState.INFO ) {
			workspaceView.displayCustomViewNode( buildAboutInfoNode() );
		}else {
			workspaceView.refreshView( currentViewState, job );
		}
	}

	/**
	 * Changes the runtime theme context and triggers immediate scene redraw.
	 *
	 * @param newTheme The target AppTheme strategy implementation.
	 */
	public void changeTheme( final AppTheme newTheme ) {
		if( newTheme != null && mainScene != null ) {
			this.currentTheme = newTheme;
			// Clear previous runtime stylesheets to avoid collision matrix
			mainScene.getStylesheets().clear();
			this.currentTheme.apply( mainScene );
		}
	}

	/**
	 * Proxy method to delegate temporary status messages to the active workspace view boundary.
	 *
	 * @param message      The localized text string to display.
	 * @param notifyStatus The theme-defined CSS class for contextual coloring.
	 * @param durationSec  The visibility lifespan of the message in seconds.
	 */
	public void showStatusNotification( final String message, final NotifyStatus notifyStatus, final int durationSec ) {
		if( workspaceView != null ) {
			workspaceView.displayTemporaryStatus( message, notifyStatus, durationSec );
		}
	}

	/**
	 * Initializes saved job configurations within the runtime context.
	 * This resets the current tracking list and populates it with the provided synchronization jobs.
	 *
	 * @param jobList the observable list of {@link SyncJobContext} instances to set
	 */
	public void setInitialJobConfigurations( final ObservableList<SyncJobContext> jobList ) {
		this.jobList.clear();
		this.jobList.addAll( jobList );
	}

	/**
	 * Returns the observable list of currently tracked synchronization jobs.
	 *
	 * @return the observable list of {@link SyncJobContext} instances
	 */
	public ObservableList<SyncJobContext> getJobList() { return jobList; }

	/**
	 * Retrieves the primary JavaFX Stage window context associated with this manager.
	 *
	 * @return the current {@link Stage} instance
	 */
	public Stage getWindowStage() { return windowStage; }

	/**
	 * Returns the list of all application themes available for selection.
	 *
	 * @return an observable list of {@link AppTheme} options
	 */
	public ObservableList<AppTheme> getAvailableThemes() { return availableThemes; }

	/**
	 * Retrieves the currently active application theme configuration.
	 *
	 * @return the currently applied {@link AppTheme}
	 */
	public AppTheme getCurrentTheme() { return currentTheme; }

	public boolean isShowing() { return windowStage.isShowing(); }

	/**
	 * Builds standard software information metrics description panels nodes.
	 */
	private Node buildAboutInfoNode() {
		final VBox infoBox = new VBox( 10 );
		infoBox.getStyleClass().addAll( "info-box" );
		final Label appTitle = new Label( "DataSync Core Engine" );
		appTitle.getStyleClass().addAll( "app-title-label" );
		final Label version = new Label( "Programmversion: " + Main.VERSION );
		final Label vendor = new Label( "Lizenznehmer / Entwickler: Tom Spirit" );
		final Label copyright = new Label( "Copyright: Licensed under GNU GPL v3.0 Copyleft System." );
		final Separator sep = new Separator();
		final TextArea legalText = new TextArea(
				"""
						This program is free software; you can redistribute it and/or modify
						it under the terms of the GNU General Public License as published by
						the Free Software Foundation; either version 3 of the License.

						This program is distributed in the hope that it will be useful, without any warranty.
						""" );
		legalText.setEditable( false );
		legalText.setPrefHeight( 150 );
		legalText.getStyleClass().addAll( "legalText" );

		infoBox.getChildren().addAll( appTitle, version, vendor, copyright, sep, legalText );
		return infoBox;
	}
}