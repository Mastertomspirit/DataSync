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

import java.awt.SystemTray;

import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.Model.BgModel;
import de.spiritscorp.DataSync.Model.Model;
import javafx.collections.ObservableList;

public class BgController {

	private Thread thread;
	private final SystemTray sysTray;
	private final ObservableList<SyncJobContext> jobList;
	private final Logger logger;
	private final ViewController controller;
	private BgView bgView;
	private final Gui gui;

	// Test-Schnittstelle: Ermöglicht das Beschleunigen von Timeouts im JUnit-Test
	private double timeMultiplier = 1.0;
	private volatile boolean bgRun = true;

	/**
	 * Background Controller
	 *
	 * @param bgView  The background view
	 * @param jobList The settings for all jobs to be used
	 * @param logger
	 */
	BgController(final Gui gui, ViewController controller, final ObservableList<SyncJobContext> jobList, final Logger logger) {
		this.gui = gui;
		this.controller = controller;
		this.jobList = jobList;
		this.logger = logger;
		this.sysTray = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
	}

	void setBgView(BgView bgView) { this.bgView = bgView; }

	/**
	 * Setzt den Zeitmultiplikator für Unit-Tests (z.B. 0.001 um Minuten in Millisekunden zu wandeln).
	 */
	void setTimeMultiplierForTesting(double multiplier) { this.timeMultiplier = multiplier; }

	/**
	 *
	 * Run the Background Job
	 *
	 * @param view       The view
	 * @param helper
	 * @param firstStart If true, the thread wait 5 minutes
	 */
	void startBgJob(final boolean firstStart) {
		gui.getWindowStage().hide();
		if (sysTray != null && bgView.getTrayIcon() != null) {
			try {
				sysTray.add(bgView.getTrayIcon());
			} catch (final Exception e) {
				Debug.printDebug("[DataSync Daemon] TrayIcon konnte nicht registriert werden.");
			}
		}

		thread = new Thread(() -> {
			Debug.printDebug("DataSync Multi-Job Background-Daemon started.");

			// Initialer Start-Delay (Für Tests beschleunigt)
			try {
				final long initialSleep = (long) ((firstStart ? 30000 : 1000) * timeMultiplier);
				if (initialSleep > 0) Thread.sleep(initialSleep);
			} catch (final InterruptedException e) {
				bgRun = false;
			}

			while (bgRun) {
				// Iteriere über alle vorhandenen Jobs in der reaktiven Liste
				for (final SyncJobContext job : jobList) {
					if (!bgRun) break;

					final var pref = job.getPreference();

					// Nur ausführen, wenn für diesen spezifischen Job die Hintergrund-Synchronisation aktiv ist
					if (pref != null && pref.isBgSync()) {
						Debug.printDebug("Executing background routine for task: " + job.getJobName());

						// Erstelle das Arbeitsmodell passend für die Kriterien des jeweiligen Jobs
						final Thread th = new Thread(() -> {
							final BgModel bgModel = new BgModel(pref, logger, Model.createMap(), Model.createMap());
							job.setRunning(true);
// TODO TEST							bgModel.runBgJob();
							job.setRunning(false);
							Debug.PRINT_DEBUG("Finished executing background routine for task: " + job.getJobName());
						});
						job.setActiveWorkerThread(th);
						th.start();
					}
				}

//	TODO global oder per thread			 Pausiere den Thread basierend auf dem globalen/kleinsten Intervall
				try {
					// Standard-Fallback: 30 Min, falls keine Jobs da sind oder kein Intervall greift
					long checkTime = 1800000;
					if (!jobList.isEmpty() && jobList.get(0).getPreference().getBgTime() != null) {
						checkTime = jobList.get(0).getPreference().getBgTime().getCheckTime();
					}

					Thread.sleep((long) (checkTime * timeMultiplier));
				} catch (final InterruptedException e) {
					bgRun = false; // Flag bremst den Loop sauber aus
				}
			}

			// Cleanup beim Beenden des Threads
			if (sysTray != null && bgView.getTrayIcon() != null) {
				sysTray.remove(bgView.getTrayIcon());
			}
			Debug.printDebug("DataSync Background-Daemon terminated cleanly.");
		});

		thread.start();
	}

	public void handleApplicationShutdown() {
		controller.handleApplicationShutdown();
	}

	public void interruptBgJob() {
		this.bgRun = false;
		gui.getWindowStage().show();
		if (this.thread != null) {
			this.thread.interrupt();
			Debug.printException("Background routine interrupted");
		}
	}
}