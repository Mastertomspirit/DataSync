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

import java.nio.file.Path;
import java.util.Map;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.Controller.ControllerHelper;
import de.spiritscorp.DataSync.Controller.MainViewController;
import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.Controller.ViewController;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;
import de.spiritscorp.DataSync.Theme.AppTheme;
import de.spiritscorp.DataSync.Theme.DarkSlateTheme;
import de.spiritscorp.DataSync.Theme.MatrixTerminalTheme;
import de.spiritscorp.DataSync.Theme.NordicLightTheme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main Entry Point Orchestrator managing operational state transactions switcher channels,
 * initialization parameters, and global view configuration lifecycle processes.
 * * @author Tom Spirit
 */
public class Gui extends Application {

	private final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();
	private final Map<Path, FileAttributes> sourceMap = Model.createMap();
	private final Map<Path, FileAttributes> destMap = Model.createMap();
	private final Model model = new Model(new Logger(), sourceMap, destMap);
	private final ControllerHelper helper = new ControllerHelper(model, Preference.getInstance(), sourceMap, destMap);
	private final ViewController controller = new MainViewController(this);

	private AppTheme currentTheme = new DarkSlateTheme();
	private final ObservableList<AppTheme> availableThemes = FXCollections.observableArrayList(
			new DarkSlateTheme(),
			new MatrixTerminalTheme(),
			new NordicLightTheme());
	private Scene mainScene;

	private SidebarView sidebarView;
	private WorkspaceView workspaceView;
	private SyncJobContext currentActiveJob;
	private Stage windowStage;

	/**
	 * Enumeration tracking the structural visibility layer of the main Viewport container.
	 */
	public enum ViewState {
		MONITOR,
		SETTINGS,
		INFO
	}

	private ViewState currentViewState = ViewState.MONITOR;

	/**
	 * Utility method allocating custom font vector metrics icons definitions graphics layouts.
	 * * @param ikon Selected base vector item index.
	 *
	 * @return Prepared graphic FontIcon instance node.
	 */
	public static FontIcon createIcon(Ikon ikon) {
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

		sidebarView = new SidebarView(this, controller);
		workspaceView = new WorkspaceView(this, controller);

		final BorderPane mainLayout = new BorderPane();
		mainLayout.setLeft(sidebarView);
		mainLayout.setCenter(workspaceView);

		// Szene zuweisen und Theme anwenden
		mainScene = new Scene(mainLayout, 1350, 800);
		currentTheme.apply(mainScene);

		primaryStage.setScene(mainScene);
		primaryStage.show();

		loadInitialJobConfigurations();
	}

	/**
	 * Updates global active tracking routes navigation indexes updating workspace render cycles.
	 * * @param state Target destination navigation path selection layer.
	 */
	public void setViewState(ViewState state) {
		this.currentViewState = state;
		if (state == ViewState.INFO) {
			workspaceView.displayCustomViewNode(buildAboutInfoNode());
		} else {
			workspaceView.refreshView(currentViewState, currentActiveJob);
		}
	}

	/**
	 * Updates central contextual execution active jobs binding structures hooks.
	 * * @param job Active core source entity context.
	 */
	public void setCurrentActiveJob(SyncJobContext job) {
		this.currentActiveJob = job;
		workspaceView.bindJob(job);
		workspaceView.refreshView(currentViewState, job);
	}

	/**
	 * Changes the runtime theme context and triggers immediate scene redraw.
	 *
	 * @param newTheme The target AppTheme strategy implementation.
	 */
	public void changeTheme(AppTheme newTheme) {
		if (newTheme != null && mainScene != null) {
			this.currentTheme = newTheme;
			// Clear previous runtime stylesheets to avoid collision matrix
			mainScene.getStylesheets().clear();
			this.currentTheme.apply(mainScene);
		}
	}

	// --- Standard Java-Bean Property Accessors APIs layer ---
	public ObservableList<SyncJobContext> getJobList() { return jobList; }

	public Stage getWindowStage() { return windowStage; }

	public ControllerHelper getHelper() { return helper; }

	public ObservableList<AppTheme> getAvailableThemes() { return availableThemes; }

	public AppTheme getCurrentTheme() { return currentTheme; }

	/**
	 * Builds standard software information metrics description panels nodes.
	 */
	private Node buildAboutInfoNode() {
		final VBox infoBox = new VBox(10);
		infoBox.setStyle("-fx-padding: 15px;");
		final Label appTitle = new Label("DataSync Core Engine");
		appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
		final Label version = new Label("Programmversion: " + Main.VERSION);
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

	/**
	 * Populates active runtime instances entries grids tracing mock startup properties tracking.
	 */
	private void loadInitialJobConfigurations() {
		jobList.add(new SyncJobContext("NAS Dokumenten-Spiegel", Preference.getInstance()));
		jobList.add(new SyncJobContext("Lokales Code-Workspace Backup", Preference.getInstance()));
		if (!jobList.isEmpty()) {
			sidebarView.getSidebarListView().getSelectionModel().select(0);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}