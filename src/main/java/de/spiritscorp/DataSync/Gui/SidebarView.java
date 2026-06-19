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

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.Controller.ViewController;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Sidebar Navigation panel hosting the main execution links,
 * managed tasks instances, and dynamic manipulation actions via context menus.
 * * @author Tom Spirit
 */
public class SidebarView extends VBox {

	private final ListView<SyncJobContext> sidebarListView;
	private final Gui mainGui;
	private final ViewController controller;

	/**
	 * Constructs the graphical container and links action events directly into specified controllers interfaces.
	 *
	 * @param mainGui    Application root shell container.
	 * @param controller Associated decoupled interaction pipeline boundary coordinator.
	 */
	public SidebarView(Gui mainGui, ViewController controller) {
		this.mainGui = mainGui;
		this.controller = controller;
		this.setSpacing(16);
		this.setPadding(new Insets(16, 16, 24, 16));
		this.setPrefWidth(300);
		this.setStyle("-fx-background-color: #1a252f;"); // Deep slate custom navy container

		// Initialize application navigation switcher menu
		final MenuButton hamburgerMenu = new MenuButton("Navigation", Gui.createIcon(MaterialDesignH.HAMBURGER));
		hamburgerMenu.setMaxWidth(Double.MAX_VALUE);

		final MenuItem taskViewItem = new MenuItem("Aktiver Task-Monitor", Gui.createIcon(MaterialDesignF.FOLDER));
		taskViewItem.setOnAction(e -> controller.handleNavigate(Gui.ViewState.MONITOR));

		final MenuItem settingsItem = new MenuItem("Erweiterte Parameter", Gui.createIcon(MaterialDesignS.STORE_SETTINGS));
		settingsItem.setOnAction(e -> controller.handleNavigate(Gui.ViewState.SETTINGS));

		final MenuItem infoItem = new MenuItem("System-Informationen", Gui.createIcon(MaterialDesignI.INFORMATION));
		infoItem.setOnAction(e -> controller.handleNavigate(Gui.ViewState.INFO));

		hamburgerMenu.getItems().addAll(taskViewItem, settingsItem, new SeparatorMenuItem(), infoItem);

		// Section header label
		final Label sidebarHeader = new Label("VERWALTETE TASK-INSTANZEN");
		sidebarHeader.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.8px;");

		// Main ListView layout for tasks mapping
		sidebarListView = new ListView<>(mainGui.getJobList());
		setupCellFactory();

		// Control operation triggers
		final Button addJobButton = new Button("Task hinzufügen", Gui.createIcon(MaterialDesignP.PLUS));
		addJobButton.setMaxWidth(Double.MAX_VALUE);
		addJobButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11px;");
		addJobButton.setOnAction(e -> controller.handleCreateNewJob());

		final Button exitButton = new Button("Programm beenden", Gui.createIcon(MaterialDesignP.POWER));
		exitButton.getGraphic().setStyle("-fx-icon-color: white;");
		exitButton.setMaxWidth(Double.MAX_VALUE);
		exitButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11px;");
		exitButton.setOnAction(e -> controller.handleApplicationShutdown());

		this.getChildren().addAll(hamburgerMenu, sidebarHeader, sidebarListView, addJobButton, exitButton);
		VBox.setVgrow(sidebarListView, Priority.ALWAYS);
	}

	/**
	 * Builds and styles custom Cell rendering including interactive management items.
	 */
	private void setupCellFactory() {
		sidebarListView.setCellFactory(lv -> {
			final ListCell<SyncJobContext> cell = new ListCell<>() {
				@Override
				protected void updateItem(SyncJobContext item, boolean empty) {
					super.updateItem(item, empty);
					textProperty().unbind();
					if (empty || item == null) {
						setText("");
						setGraphic(null);
					} else {
						textProperty().bind(item.jobNameProperty());
						final FontIcon itemIcon = Gui.createIcon(MaterialDesignF.FOLDER);
						itemIcon.setStyle("-fx-icon-color: #3498db;");
						setGraphic(itemIcon);
					}
				}
			};

			final ContextMenu contextMenu = new ContextMenu();

			// Explicit lambda fix avoiding functional inference signature conflicts
			final MenuItem renameItem = new MenuItem("Task umbenennen", Gui.createIcon(MaterialDesignS.SWAP_HORIZONTAL));
			renameItem.setOnAction(event -> controller.handleRenameJob(cell));

			final MenuItem duplicateItem = new MenuItem("Task duplizieren", Gui.createIcon(MaterialDesignC.CONTENT_DUPLICATE));
			duplicateItem.setOnAction(event -> controller.handleDuplicateJob(cell.getItem()));

			final MenuItem deleteItem = new MenuItem("Task löschen", Gui.createIcon(MaterialDesignD.DELETE));
			deleteItem.setStyle("-fx-text-fill: #c0392b;");
			deleteItem.setOnAction(event -> controller.handleDeleteJob(cell.getItem()));

			contextMenu.getItems().addAll(renameItem, duplicateItem, new SeparatorMenuItem(), deleteItem);
			cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> cell.setContextMenu(isNowEmpty ? null : contextMenu));
			return cell;
		});

		sidebarListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) mainGui.setCurrentActiveJob(newVal);
		});
	}

	/**
	 * @return The underlying task selection view platform.
	 */
	public ListView<SyncJobContext> getSidebarListView() { return sidebarListView; }
}