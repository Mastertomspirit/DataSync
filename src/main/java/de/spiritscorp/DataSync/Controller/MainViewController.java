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

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Theme.AppTheme;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;

/**
 * Central controller implementation executing operational state translations and business action flows.
 * * @author Tom Spirit
 */
public class MainViewController implements ViewController {

	private final Gui gui;

	/**
	 * Allocates a new controller instance tied directly to the display engine layer hook.
	 *
	 * @param gui The global display manager orchestrator application shell instance.
	 */
	public MainViewController(Gui gui) {
		this.gui = gui;
	}

	@Override
	public void handleNavigate(Gui.ViewState state) {
		gui.setViewState(state);
	}

	@Override
	public void handleCreateNewJob() {
		gui.getJobList().add(new SyncJobContext("Sync Job " + (gui.getJobList().size() + 1), Preference.getInstance()));
	}

	@Override
	public void handleApplicationShutdown() {
		final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Hintergrunddienste werden beendet.", ButtonType.OK, ButtonType.CANCEL);
		confirmation.setTitle("Programm beenden");
		confirmation.setHeaderText("Möchten Sie DataSync wirklich schließen?");
		confirmation.setContentText("Aktive Hintergrunddienste werden unwiderruflich beendet.");
		confirmation.initOwner(gui.getWindowStage());
		if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			System.out.println("[Lifecycle] Complete system teardown triggered manually.");
			System.exit(0);
		}
	}

	@Override
	public void handleRenameJob(ListCell<SyncJobContext> cell) {
		final SyncJobContext selectedJob = cell.getItem();
		if (selectedJob != null) {
			final TextInputDialog dialog = new TextInputDialog(selectedJob.getJobName());
			dialog.setTitle("Task umbenennen");
			dialog.setHeaderText("Geben Sie einen neuen Namen für den Task ein:");
			dialog.setContentText("Name:");
			dialog.initOwner(gui.getWindowStage());
			dialog.showAndWait().ifPresent(newName -> selectedJob.setJobName(newName));
		}
	}

	@Override
	public void handleDuplicateJob(SyncJobContext job) {
		if (job != null) {
			gui.getJobList().add(new SyncJobContext(job.getJobName() + " (Kopie)", job.getPreference()));
		}
	}

	@Override
	public void handleDeleteJob(SyncJobContext job) {
		if (job != null) {
			final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Task '" + job.getJobName() + "' wirklich unwiderruflich löschen?", ButtonType.YES, ButtonType.NO);
			alert.setTitle("Task entfernen");
			alert.setHeaderText(null);
			alert.initOwner(gui.getWindowStage());
			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) gui.getJobList().remove(job);
			});
		}
	}

	@Override
	public void handleExecuteTask(SyncJobContext job) {
		if (job != null) {
//            gui.getHelper().executeJobBasedOnMode(job);
		}
	}

	@Override
	public void handleSaveSettings(SyncJobContext job, boolean subDir, boolean trashbin, boolean autoDel,
			boolean logOn, boolean bgSync, String interval, AppTheme targetTheme, boolean autostart) {
		if (job == null) return;
		final Preference pref = job.getPreference();

		pref.setSubDir(subDir);
		pref.setTrashbin(trashbin);
		pref.setAutoDel(autoDel);
		pref.setLogOn(logOn);
		pref.setBgSync(bgSync);
		if (bgSync) {
			pref.setBgTime(BgTime.get(interval));
		}
		gui.getHelper().setOSAutostart(autostart);
		gui.changeTheme(targetTheme);

		gui.setViewState(Gui.ViewState.SETTINGS);
	}
}