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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.controller.ViewController;

/**
 * Sidebar Navigation panel hosting the main execution links, managed tasks instances, and dynamic manipulation actions via context menus.
 *
 * @author Tom Spirit
 */
final class SidebarView extends VBox {

	private final ListView<SyncJobContext> sidebarListView;
	private final Gui mainGui;
	private final ViewController controller;

	private static final String CSS_MENU_ICON = "menu-icon";
	private static final String CSS_JOB_ICON = "job-icon";
	private static final String CSS_DRAG = "drag-over-target";

	private boolean dragAndDropEnabled = true;

	/**
	 * Constructs the graphical container and links action events directly into specified controllers interfaces.
	 *
	 * @param mainGui    Application root shell container.
	 * @param controller Associated decoupled interaction pipeline boundary coordinator.
	 */
	SidebarView( final Gui mainGui, final ViewController controller ) {
		this.mainGui = mainGui;
		this.controller = controller;
		this.setSpacing( 16 );
		this.setPadding( new Insets( 16, 16, 24, 16 ) );
		this.setPrefWidth( 300 );

		// Initialize application navigation switcher menu

		final MenuButton hamburgerMenu = new MenuButton( "Navigation", Gui.createIcon( MaterialDesignH.HAMBURGER ) );
		hamburgerMenu.getGraphic().getStyleClass().addAll( CSS_MENU_ICON );
		hamburgerMenu.setMaxWidth( Double.MAX_VALUE );

		final MenuItem taskViewItem = new MenuItem( "Aktiver Task-Monitor", Gui.createIcon( MaterialDesignF.FOLDER ) );
		taskViewItem.getGraphic().getStyleClass().addAll( CSS_MENU_ICON );
		taskViewItem.setOnAction( _ -> controller.handleNavigate( Gui.ViewState.MONITOR ) );

		final MenuItem settingsItem = new MenuItem( "Erweiterte Parameter", Gui.createIcon( MaterialDesignS.STORE_SETTINGS ) );
		settingsItem.getGraphic().getStyleClass().addAll( CSS_MENU_ICON );
		settingsItem.setOnAction( _ -> controller.handleNavigate( Gui.ViewState.SETTINGS ) );

		final MenuItem infoItem = new MenuItem( "System-Informationen", Gui.createIcon( MaterialDesignI.INFORMATION ) );
		infoItem.getGraphic().getStyleClass().addAll( CSS_MENU_ICON );
		infoItem.setOnAction( _ -> controller.handleNavigate( Gui.ViewState.INFO ) );

		hamburgerMenu.getItems().addAll( taskViewItem, settingsItem, new SeparatorMenuItem(), infoItem );

		// Section header label
		final Label sidebarTitleLabel = new Label( "VERWALTETE TASK-INSTANZEN" );
		sidebarTitleLabel.getStyleClass().addAll( "sidebar-title-label" );

		// Main ListView layout for tasks mapping
		sidebarListView = new ListView<>( mainGui.getJobList() );
		setupCellFactory();

		// Control operation triggers
		final Button addJobButton = new Button( "Task hinzufügen", Gui.createIcon( MaterialDesignP.PLUS ) );
		addJobButton.setMaxWidth( Double.MAX_VALUE );
		addJobButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		addJobButton.getStyleClass().addAll( "add-job-button" );
		addJobButton.setOnAction( _ -> controller.handleCreateNewJob() );

		final Button exitButton = new Button( "Programm beenden", Gui.createIcon( MaterialDesignP.POWER_STANDBY ) );
		exitButton.setMaxWidth( Double.MAX_VALUE );
		exitButton.getGraphic().getStyleClass().addAll( Gui.CSS_BUTTON_ICON );
		exitButton.getStyleClass().addAll( "exit-button" );
		exitButton.setOnAction( _ -> controller.handleApplicationShutdown() );

		this.getChildren().addAll( hamburgerMenu, sidebarTitleLabel, sidebarListView, addJobButton, exitButton );
		setVgrow( sidebarListView, Priority.ALWAYS );
	}

	/**
	 * Builds and styles custom Cell rendering including interactive management items.
	 */
	private void setupCellFactory() {
		sidebarListView.setCellFactory( _ -> {
			@SuppressWarnings( { "PMD.CommentRequired" } )
			final ListCell<SyncJobContext> cell = new ListCell<>() {
				private final FontIcon itemIcon = Gui.createIcon( MaterialDesignF.FOLDER );
				private final Tooltip toolTip = new Tooltip();

				@Override
				protected void updateItem( final SyncJobContext item, final boolean empty ) {
					super.updateItem( item, empty );
					textProperty().unbind();
					toolTip.textProperty().unbind();
					if( empty || item == null ) {
						setText( "" );
						setGraphic( null );
						setTooltip( null );
					}else {
						textProperty().bind( item.jobNameProperty() );
						itemIcon.getStyleClass().addAll( CSS_JOB_ICON );
						setGraphic( itemIcon );
						toolTip.textProperty().bind( item.jobNameProperty() );
						setTooltip( toolTip );
					}
				}
			};

			final ContextMenu contextMenu = new ContextMenu();

			final MenuItem renameItem = new MenuItem( "Task umbenennen", Gui.createIcon( MaterialDesignS.SWAP_HORIZONTAL ) );
			renameItem.setOnAction( _ -> controller.handleRenameJob( cell.getItem() ) );

			final MenuItem duplicateItem = new MenuItem( "Task duplizieren", Gui.createIcon( MaterialDesignC.CONTENT_DUPLICATE ) );
			duplicateItem.setOnAction( _ -> controller.handleDuplicateJob( cell.getItem() ) );

			final MenuItem deleteItem = new MenuItem( "Task löschen", Gui.createIcon( MaterialDesignD.DELETE ) );
			deleteItem.setOnAction( _ -> controller.handleDeleteJob( cell.getItem() ) );

			contextMenu.getItems().addAll( renameItem, duplicateItem, new SeparatorMenuItem(), deleteItem );

			cell.setOnContextMenuRequested( event -> {
				// Guard clause: Block popup triggers on empty rows or missing models
				if( cell.isEmpty() || cell.getItem() == null ) {
					event.consume();
					return;
				}

				// Explicitly verify if the mouse coordinates hit the bounded layout area of the graphic/text
				// This prevents triggers on empty spaces inside extended high-width list cells
				contextMenu.show( cell, event.getScreenX(), event.getScreenY() );
				event.consume();
			} );

			// ==========================================
			// DRAG & DROP IMPLEMENTATION START
			// ==========================================
			cell.setOnDragDetected( event -> {
				// Guard clause: Block gesture if disabled or triggered on an empty row
				if( cell.isEmpty() || !dragAndDropEnabled ) { return; }

				// Initiate move transfer mode and store the source index inside the dragboard
				final Dragboard db = cell.startDragAndDrop( TransferMode.MOVE );
				final ClipboardContent content = new ClipboardContent();
				content.putString( String.valueOf( cell.getIndex() ) );
				db.setContent( content );

				event.consume();
			} );

			cell.setOnDragOver( event -> {
				// Only accept the move if functionality is enabled and data type matches
				if( dragAndDropEnabled && event.getDragboard().hasString() ) {
					event.acceptTransferModes( TransferMode.MOVE );
				}
				event.consume();
			} );

			cell.setOnDragEntered( event -> {
				// Apply visual feedback style when dragging an item over a valid cell
				if( dragAndDropEnabled && !cell.isEmpty() && event.getDragboard().hasString() ) {
					cell.getStyleClass().add( CSS_DRAG );
				}
				event.consume();
			} );

			cell.setOnDragExited( event -> {
				// Clear visual feedback style immediately when leaving the cell area
				cell.getStyleClass().remove( CSS_DRAG );
				event.consume();
			} );

			cell.setOnDragDropped( event -> {
				// Ensure style cleanup before processing data logic
				cell.getStyleClass().remove( CSS_DRAG );

				if( !dragAndDropEnabled ) { return; }

				final Dragboard db = event.getDragboard();
				if( db.hasString() ) {
					final int draggedIdx = Integer.parseInt( db.getString() );

					// Fall back to last list index if item dropped onto an empty trailing cell
					int thisIdx = cell.isEmpty() ? sidebarListView.getItems().size() - 1 : cell.getIndex();
					if( thisIdx < 0 ) thisIdx = 0;
					if( draggedIdx < thisIdx ) thisIdx -= 1;
					if( draggedIdx != thisIdx && draggedIdx >= 0 ) {
						controller.handleDragJob( thisIdx, draggedIdx );
						// Immediately restore focus and selection onto the moved element
						sidebarListView.getSelectionModel().select( thisIdx );
						event.setDropCompleted( true );
					}
				}
				event.consume();
			} );
			// ==========================================
			// DRAG & DROP IMPLEMENTATION END
			// ==========================================

			return cell;
		} );

		sidebarListView.getSelectionModel().selectedItemProperty().addListener( ( _, oldVal, newVal ) -> {
			if( newVal != null && !newVal.equals( oldVal ) ) mainGui.setCurrentActiveJob( newVal );
		} );
	}

	/**
	 * @return The underlying task selection view platform.
	 */
	ListView<SyncJobContext> getSidebarListView() { return sidebarListView; }
}