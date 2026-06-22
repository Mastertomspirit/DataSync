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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.Model.BgModel;
import de.spiritscorp.DataSync.Model.Model;
import javafx.collections.ObservableList;

public class BgController {

	private final SystemTray sysTray;
	private final ObservableList<SyncJobContext> jobList;
	private final Logger logger;
	private final ViewController controller;
	private BgView bgView;
	private final Gui gui;

	// Modern thread-pool architecture for accurate execution timing and disk I/O optimization
	private ScheduledExecutorService scheduler;
	private ExecutorService workerQueue;

	// Test interface: Allows accelerating intervals inside JUnit execution tasks
	private double timeMultiplier = 1.0;

	/**
	 * Background Controller
	 *
	 * @param bgView  The background view
	 * @param jobList The settings for all jobs to be used
	 * @param logger  The system log interface
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
	 * Sets the time multiplier for unit tests (e.g., 0.001 to scale minutes into milliseconds).
	 */
	void setTimeMultiplierForTesting(double multiplier) { this.timeMultiplier = multiplier; }

	/**
	 * Starts the background daemon worker routine.
	 *
	 * @param firstStart If true, the scheduler starts checking after an initial delay
	 */
	void startBgJob(final boolean firstStart) {
		gui.getWindowStage().hide();
		if (sysTray != null && bgView.getTrayIcon() != null) {
			try {
				sysTray.add(bgView.getTrayIcon());
			} catch (final Exception e) {
				Debug.printError("[DataSync Daemon] Failed to register TrayIcon context.");
				Debug.printException(getClass(), e);
			}
		}

		Debug.printDebug("[DataSync Daemon] Multi-Job Background-Daemon initialization started.");

		// Initialize execution frameworks: Scheduler ticks periodically, worker processes sequentially
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.workerQueue = Executors.newSingleThreadExecutor();

		// Dynamically determine the optimal check interval based on active jobs
		final long calculatedTick = determineOptimalCheckTime();
		final long tickInterval = (long) (calculatedTick * timeMultiplier);
		final long initialDelay = (long) ((firstStart ? 30000 : 1000) * timeMultiplier);
		Debug.printDebug("[DataSync Daemon] Heartbeat configured to tick every %d ms based on job preferences.", calculatedTick);
		jobList.stream()
				.filter((job) -> job.getPreference()
						.isBgSync())
				.forEach((job) -> Debug.printDebug("[DataSync Daemon] Executing background routine is deactivated for task: %s", job.getJobName()));
		// Begin tracking task list rules loops
		this.scheduler.scheduleAtFixedRate(this::checkAndQueueJobs, initialDelay, tickInterval, TimeUnit.MILLISECONDS);
	}

	/**
	 * Determines the smallest defined checkTime among all active background jobs.
	 * Falls back to a default interval (10 sec) if no matching jobs are active.
	 */
	private long determineOptimalCheckTime() {
		long minCheckTime = 10000; // Default fallback: 10 seconds
		boolean foundActiveJob = false;

		for (final SyncJobContext job : jobList) {
			final var pref = job.getPreference();
			if (pref != null && pref.isBgSync() && pref.getBgTime() != null) {
				final long currentCheck = pref.getBgTime().getCheckTime();
				if (!foundActiveJob || currentCheck < minCheckTime) {
					minCheckTime = currentCheck;
					foundActiveJob = true;
				}
			}
		}
		return minCheckTime;
	}

	/**
	 * Core polling rule: Evaluates each job dynamically to verify individual time limit thresholds.
	 */
	private void checkAndQueueJobs() {
		for (final SyncJobContext job : jobList) {
			// Skip tasks if they are actively running or already waiting inside the execution queue lane
			if (job.isRunning()) {
				continue;
			}

			final var pref = job.getPreference();

			// Only process if background execution is explicitly requested for this task context
			if (pref != null && pref.isBgSync()) {

				final long timeDelta = System.currentTimeMillis() - pref.getLastScanTime();
				final long targetInterval = (long) (pref.getBgTime().getTime() * timeMultiplier);

				if (timeDelta > targetInterval) {
					Debug.printDebug("[DataSync Daemon] Polling threshold triggered for task: %s. Queueing worker task.", job.getJobName());

					job.setRunning(true);

					// Dispatch into the dedicated loop queue lane (prevents hardware disk I/O thrashing)
					workerQueue.execute(() -> {
						try {
							final BgModel bgModel = new BgModel(pref, logger, Model.createMap(), Model.createMap());

							// Map active thread to the context token to let external shutdown requests throw interrupts
							job.setActiveWorkerThread(Thread.currentThread());

							Debug.printDebug("[DataSync Daemon] Executing background routine for task: %s", job.getJobName());
							bgModel.runBgJob();

						} catch (final Exception e) {
							Debug.printDebug("[Error DataSync Daemon] Critical fault captured inside background thread execution pipeline for: %s", job.getJobName());
							Debug.printException(this.getClass(), e);
						} finally {
							job.setRunning(false);
							job.setActiveWorkerThread(null);
							Debug.printDebug("[DataSync Daemon] Finished executing background routine for task: %s", job.getJobName());
						}
					});
				}
			}
		}
	}

	public void handleApplicationShutdown() {
		// Disassemble concurrent tracking frameworks before global window exit procedures trigger
		shutdownExecutors();
		controller.handleApplicationShutdown();
	}

	public void interruptBgJob() {
		gui.getWindowStage().show();
		shutdownExecutors();
		Debug.printDebug("[DataSync Daemon] Background routine interrupted");
	}

	/**
	 * Helper sequence to cleanly dismantle thread execution systems and safely unregister tray indicators.
	 */
	private void shutdownExecutors() {
		Debug.printDebug("[DataSync Daemon] Dissolving executor pools and cleaning up task contexts.");

		if (scheduler != null) {
			scheduler.shutdownNow();
		}

		if (workerQueue != null) {
			// Drops instant interrupt signals down to the thread executing the active copy sequence
			workerQueue.shutdownNow();
			try {
				if (!workerQueue.awaitTermination(1500, TimeUnit.MILLISECONDS)) {
					Debug.printDebug("[DataSync Daemon] Worker queue termination delayed. Enforcing lifecycle exit.");
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Clean up framework tracking tokens across the execution stack
		for (final SyncJobContext job : jobList) {
			job.setRunning(false);
			job.setActiveWorkerThread(null);
		}

		// Remove indicator shell icons
		if (sysTray != null && bgView != null && bgView.getTrayIcon() != null) {
			sysTray.remove(bgView.getTrayIcon());
		}

		Debug.printDebug("[DataSync Daemon] Background-Daemon terminated cleanly.");
	}
}