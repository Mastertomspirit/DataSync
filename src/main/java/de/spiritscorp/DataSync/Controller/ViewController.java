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
package de.spiritscorp.DataSync.Controller;

import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Theme.AppTheme;
import javafx.scene.control.ListCell;

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
	void handleNavigate(Gui.ViewState state);

	/**
	 * Requests termination of the entire application environment, safely stopping background hooks.
	 */
	void handleApplicationShutdown();

	/**
	 * Load the whole jobList from the Preferences.
	 */
	void loadInitialJobList();

	/**
	 * Handles the creation and append workflow for a new managed task synchronization context instance.
	 */
	void handleCreateNewJob();

	/**
	 * Triggers the specialized configuration context dialog to change a job instance identification label.
	 *
	 * @param cell The graphical ListCell context container hosting the target model entity.
	 */
	void handleRenameJob(ListCell<SyncJobContext> cell);

	/**
	 * Creates an independent copy of the currently selected task parameters profile mapping.
	 *
	 * @param job The source configuration instance payload.
	 */
	void handleDuplicateJob(SyncJobContext job);

	/**
	 * Permanently removes a task entity context from the global orchestrator matrix tracking layer.
	 *
	 * @param job The target configuration instance to wipe.
	 */
	void handleDeleteJob(SyncJobContext job);

	/**
	 * Initiates execution of processing operations based on active configuration parameters.
	 *
	 * @param job The active task configuration processing target.
	 */
	void handleExecuteTask(SyncJobContext job);

	/**
	 * Stop execution of processing operations based on active configuration parameters.
	 *
	 * @param job The active task configuration processing target.
	 */
	void handleStopTask(SyncJobContext job);

	/**
	 * Commits altered orchestration state variables using the encapsulated properties entity carrier.
	 *
	 * @param localPreferences Altered context data parameters container reference.
	 * @param targetTheme      Visual presentation theme strategy selection.
	 */
	void handleSaveSettings(Preference localPreferences, AppTheme targetTheme);

	void runInBackground(boolean firstStart);

}
