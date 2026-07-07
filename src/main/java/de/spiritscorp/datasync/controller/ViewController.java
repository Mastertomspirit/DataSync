package de.spiritscorp.datasync.controller;

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

import de.spiritscorp.datasync.gui.Gui;
import de.spiritscorp.datasync.theme.AppTheme;

/**
 * Core architectural interface decoupling user interface interactions from business logic orchestration.
 * Acts as the primary controller boundary for all GUI-driven events.
 * * @author Tom Spirit
 */
public interface ViewController {

	/**
	 * Registers a native host operating system runtime shutdown hook to capture external termination signals.
	 */
	void registerNativeShutdownHook();

	/**
	 * Handles switching between different primary view layers within the main viewport.
	 *
	 * @param state The target structural navigation layer.
	 */
	void handleNavigate( Gui.ViewState state );

	/**
	 * Requests termination of the entire application environment, safely stopping background hooks.
	 */
	void handleApplicationShutdown();

	/**
	 * Handles the creation and append workflow for a new managed task synchronization context instance.
	 */
	void handleCreateNewJob();

	/**
	 * Triggers the specialized configuration context dialog to change a job instance identification label.
	 *
	 * @param job The source configuration instance payload.
	 */
	void handleRenameJob( SyncJobContext job );

	/**
	 * Creates an independent copy of the currently selected task parameters profile mapping.
	 *
	 * @param job The source configuration instance payload.
	 */
	void handleDuplicateJob( SyncJobContext job );

	/**
	 * Permanently removes a task entity context from the global orchestrator matrix tracking layer.
	 *
	 * @param job The target configuration instance to wipe.
	 */
	void handleDeleteJob( SyncJobContext job );

	/**
	 * Initiates execution of processing operations based on active configuration parameters.
	 *
	 * @param job The active task configuration processing target.
	 */
	void handleExecuteTask( SyncJobContext job );

	/**
	 * Stop execution of processing operations based on active configuration parameters.
	 *
	 * @param job The active task configuration processing target.
	 */
	void handleStopTask( SyncJobContext job );

	/**
	 * Commits altered orchestration state variables using the encapsulated properties entity carrier.
	 *
	 * @param targetTheme Visual presentation theme strategy selection.
	 */
	void handleSaveSettings( AppTheme targetTheme );

	/**
	 * Asynchronously dispatches the orchestration engine onto a background worker thread.
	 * Optionally defers the initial execution sequence to accommodate bootstrap stabilization,
	 * dependency initialization, or throttling requirements during application startup.
	 *
	 * @param bootDelay true to enforce an initial structural delay prior to thread execution;
	 *                  false for immediate background execution.
	 */
	void runInBackground( boolean bootDelay );

	/**
	 * Triggers a destructive purging routine to eliminate identified duplicate entities
	 * within the scope of the given synchronization task context.
	 * Mutates the active memory allocation state and immediately persists structural changes.
	 *
	 * @param job The specific synchronization runtime context targeting duplicate remediation.
	 */
	void deleteSelectedDuplicates( SyncJobContext job );

	/**
	 * Coordinates and processes drag-and-drop reordering requests originating from the job list.
	 * Acts as the bridge to pass index mutations from the view architecture back to the
	 * underlying sequential model registries.
	 *
	 * @param thisIdx    The destination target index where the dragged element is dropped.
	 * @param draggedIdx The original source index where the drag gesture was initiated.
	 */
	void handleDragJob( int thisIdx, int draggedIdx );
}
