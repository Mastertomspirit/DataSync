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

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import de.spiritscorp.datasync.gui.BgView;
import de.spiritscorp.datasync.gui.Gui;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.model.BgModel;
import de.spiritscorp.datasync.model.Model;

/**
 * Central orchestration engine handling asynchronous background file synchronization routines.
 * <p>
 * The {@code BgController} manages the application's daemon lifecycle. It leverages a dedicated
 * two-tier concurrent executor architecture to decouple continuous time-threshold monitoring from
 * high-overhead disk I/O operations. This design prevents resource starvation and avoids system
 * UI freezes by offloading execution workloads to isolated worker threads.
 * <p>
 * System state integration is maintained via an operating system {@link SystemTray} proxy interface,
 * allowing the core UI application framework to seamlessly minimize into background execution lanes.
 * <p>
 *
 * @author Tom Spirit
 * @version 2.1.0
 */
public class BgController {

	/** Default initial delay in milliseconds before the background scheduler activates. */
	static final long INITIAL_DELAY = 15L * 1_000;
	/** Extended delay in milliseconds applied during system boot execution to prevent early resource contention. */
	static final long BOOT_START_DELAY = 10L * 60 * 1_000;

	/** Operating system tray integration proxy for minimizing the application frame. */
	private final SystemTray sysTray;
	/** The core dataset of synchronization job contexts monitored and orchestrated by this engine. */
	private final ObservableList<SyncJobContext> jobList;
	/** The central logger instance for tracking daemon lifecycles and background routine states. */
	private final Logger logger;
	/** Primary view controller handling UI flow control and state transitions. */
	private final ViewController controller;
	/** The visual representation of the background menu and tray interface context. */
	private BgView bgView;
	/** Reference to the primary graphical user interface application framework. */
	private final Gui gui;
	/** Formatter utility responsible for converting raw sync metrics into human-readable UI logs. */
	private final LogFormatter logFormatter;

	/** First-tier executor dedicated solely to low-overhead, periodic time-threshold heartbeat monitoring. */
	private ScheduledExecutorService scheduler;
	/** Second-tier decoupled thread pool isolated for high-overhead file system and disk I/O execution workloads. */
	private ExecutorService workerQueue;

	/** Test interface: Allows accelerating intervals inside JUnit execution tasks */
	private double timeMultiplier = 1.0;

	/**
	 * Constructs a fully operational background engine attached to the primary interface layers.
	 * <p>
	 * The initialization phase maps structural JavaFX core properties, bindings, and multi-job tracking
	 * contexts. It automatically registers native {@link SystemTray} hardware capacity parameters to bind
	 * the decoupled visual notification framework shell.
	 * <p>
	 *
	 * @param gui        The visual primary graphical user interface facade wrapper
	 * @param controller The central master view controller orchestrating active window transitions
	 * @param jobList    The reactive data backing list containing operational task metrics and execution state tokens
	 * @param logger     The standardized system logging framework interface
	 */
	BgController( final Gui gui, final ViewController controller, final ObservableList<SyncJobContext> jobList, final Logger logger ) {
		this.gui = gui;
		this.controller = controller;
		this.jobList = jobList;
		this.logger = logger;
		this.logFormatter = new LogFormatter();
		this.sysTray = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
		setEnvironment( timeMultiplier, new BgView( this ), Executors.newSingleThreadScheduledExecutor(), Executors.newSingleThreadExecutor() );
	}

	/**
	 * Initiates a global application termination sequence triggered from the background context.
	 * <p>
	 * This method acts as the bridge for the {@code BgView} (SystemTray) to command a full system exit.
	 * It systematically deallocates and dismantles internal concurrent tracking structures using a
	 * standardized background grace period before delegating downstream lifecycle teardown protocols
	 * to the central application controller.
	 * <p>
	 *
	 * @see #shutdownExecutors(long)
	 */
	public void requestApplicationShutdown() {
		// Disassemble concurrent tracking frameworks before global window exit procedures trigger
		shutdownExecutors( MainViewController.BG_TIMEOUT );
		controller.handleApplicationShutdown();
	}

	/**
	 * Interrupts the active background execution cycle and restores the primary user interface.
	 * <p>
	 * This dual-purpose lifecycle hook is invoked by both the primary workspace ({@code MainView})
	 * and the system notification shell ({@code BgView}). It enforces an immediate visibility state
	 * transition on the main window stage and guarantees a deterministic, timed collapse of all
	 * active thread pool frames.
	 * <p>
	 *
	 * @param timeout The maximum allocation window in milliseconds granted to active
	 *                worker threads to complete processing cycles before a hard
	 *                interruption signal is enforced.
	 * @see #shutdownExecutors(long)
	 */
	public void interruptBgJob( final long timeout ) {
		if( Platform.isFxApplicationThread() ) {
			gui.getWindowStage().show();
		}
		shutdownExecutors( timeout );
		Debug.printDebug( "[Bg Controller] Background routine interrupted" );
	}

	/**
	 * Initiates the continuous background daemon monitoring pipeline and minimizes the user interface.
	 * <p>
	 * Activating this boot phase suppresses the primary desktop window frame and binds the visual notifications
	 * infrastructure into the native operating system taskbar environment. It dynamically analyzes user scheduling
	 * rules to compute an optimal, non-blocking check frequency tick rate.
	 * <p>
	 * Once configurations are parsed, an initial delay configuration is selected—differentiating between fresh
	 * application boots ({@code BOOT_START_DELAY}) and quick UI toggle states ({@code INITIAL_DELAY}). The continuous
	 * tracking routine is then permanently registered inside the internal {@link ScheduledExecutorService} core thread framework.
	 * <p>
	 *
	 * @param bootDelay Enforces an extended cold-boot initialization timeout buffer if set to {@code true};
	 *                  allocates a standard near-instant scheduling offset if set to {@code false}.
	 */
	void startBgJob( final boolean bootDelay ) {
		gui.getWindowStage().hide();
		if( sysTray != null && bgView.getTrayIcon() != null ) {
			try {
				sysTray.add( bgView.getTrayIcon() );
			}catch( final AWTException exception ) {
				Debug.printError( "[Bg Controller] Failed to register TrayIcon context." );
				Debug.printException( getClass(), exception );
				gui.getWindowStage().show();
				return;
			}
		}

		Debug.printDebug( "[Bg Controller] Multi-Job Background-Daemon initialization started." );

		// Dynamically determine the optimal check interval based on active jobs
		final long calculatedTick = determineOptimalCheckTime();
		final long tickInterval = (long) ( calculatedTick * timeMultiplier );
		final long initialDelay = (long) ( ( bootDelay ? BOOT_START_DELAY : INITIAL_DELAY ) * timeMultiplier );
		Debug.printDebug( "[Bg Controller] Heartbeat configured to tick every %d ms based on job preferences.", calculatedTick );
		jobList.stream()
				.filter( job -> job.getPreference()
						.isBgSync() )
				.forEach( job -> Debug.printDebug( "[Bg Controller] Executing background routine is activated for task: %s", job.getJobName() ) );
		// Begin tracking task list rules loops
		this.scheduler.scheduleAtFixedRate( this::checkAndQueueJobs, initialDelay, tickInterval, TimeUnit.MILLISECONDS );
	}

	/**
	 * Determines the smallest defined checkTime among all active background jobs.
	 * Falls back to a default interval (10 sec) if no matching jobs are active.
	 */
	private long determineOptimalCheckTime() {
		long minCheckTime = 10_000; // Default fallback: 10 seconds
		boolean foundActiveJob = false;

		for( final SyncJobContext job : jobList ) {
			final Preference pref = job.getPreference();
			if( pref != null && pref.isBgSync() && pref.getBgTime() != null ) {
				final long currentCheck = pref.getBgTime().getCheckTime();
				if( !foundActiveJob || currentCheck < minCheckTime ) {
					minCheckTime = currentCheck;
					foundActiveJob = true;
				}
			}
		}
		return minCheckTime;
	}

	/**
	 * Evaluates temporal boundaries across registered task configurations to schedule overdue synchronization pipelines.
	 * <p>
	 * This core evaluation loop acts as the engine's processing heartbeat. It scans all configured
	 * synchronization definitions, applies an accelerated time scaling calculation using the {@code timeMultiplier},
	 * and determines if an individual task context has surpassed its requested execution interval threshold.
	 * <p>
	 * Overdue jobs are safely flag-locked to guarantee execution idempotency. The payload runnable is subsequently
	 * dispatched into a dedicated single-threaded sequential worker pool ({@code workerQueue}). This strict serialization
	 * strategy isolates concurrent I/O access and actively prevents multiple background tasks from triggering destructive
	 * physical disk drive thrashing.
	 * <p>
	 * To ensure resilient remote cancellation capabilities, the executing worker thread frame is explicitly mapped
	 * directly back to the target {@link SyncJobContext} token inside the processing boundary.
	 * <p>
	 */
	private void checkAndQueueJobs() {
		for( final SyncJobContext job : jobList ) {
			// Skip tasks if they are actively running or already waiting inside the execution queue lane
			if( job.isRunning() ) continue;

			final Preference pref = job.getPreference();
			// Only process if background execution is explicitly requested for this task context
			if( pref != null && pref.isBgSync() ) {
				final long timeDelta = System.currentTimeMillis() - pref.getLastScanTime();
				final long targetInterval = (long) ( pref.getBgTime().getTime() * timeMultiplier );
				Debug.printDebug( "[Bg Controller] time since last check (%s): %s", job.getJobName(),
						logFormatter.getTimeFormatted( ( System.currentTimeMillis() - pref.getLastScanTime() ) * 1_000_000 ) );
				Path destPath = pref.getDestPaths().getFirst();
				if( !Files.exists( destPath, LinkOption.NOFOLLOW_LINKS ) ) {
					Debug.printDebug( "[Bg Controller] Destination Path is offline: %s", destPath.toString() );
				}else if( timeDelta > targetInterval ) {
					Debug.printDebug( "[Bg Controller] Polling threshold triggered for task: %s. Queueing worker task.", job.getJobName() );
					job.setRunning( true );
					// Dispatch into the dedicated loop queue lane (prevents hardware disk I/O thrashing)
					workerQueue.execute( () -> {
						try {
							final BgModel bgModel = new BgModel( pref, logger, Model.createMap(), Model.createMap() );

							// Map active thread to the context token to let external shutdown requests throw interrupts
							job.setActiveWorkerThread( Thread.currentThread() );
							Debug.printDebug( "[Bg Controller] Executing background routine for task: %s", job.getJobName() );
							bgModel.runBgJob();
						}catch( final RuntimeException exception ) {
							Debug.printDebug( "[Bg Controller Error] Critical fault captured inside background thread execution pipeline for: %s", job.getJobName() );
							Debug.printException( this.getClass(), exception );
						}finally {
							job.setRunning( false );
							job.setActiveWorkerThread( null );
							Debug.printDebug( "[Bg Controller] Finished executing background routine for task: %s", job.getJobName() );
						}
					} );
				}
			}
		}
	}

	/**
	 * Deallocates the dual-tier concurrent execution infrastructure and dissolves operational states.
	 * <p>
	 * This structural shutdown hook safely liquidates asynchronous runtimes by issuing immediate
	 * cancellation signals via {@link ExecutorService#shutdownNow()} to both the high-frequency tick scheduler
	 * and the sequential data transfer queue. Active backup threads executing file system operations
	 * are granted a strict temporal grace window to cooperatively wind down file handles.
	 * <p>
	 * Upon pool expiration, all underlying job context data models are purged of volatile execution parameters
	 * and the associated hardware {@link TrayIcon} is stripped from the operating system shell to ensure
	 * zero resource leaks.
	 * <p>
	 *
	 * @param timeout The maximum synchronization epoch in milliseconds granted to active
	 *                I/O operations to complete task evaluation loops before
	 *                the lifecycle boundary is forcibly closed.
	 */
	private void shutdownExecutors( final long timeout ) {
		Debug.printDebug( "[Bg Controller] Dissolving executor pools and cleaning up task contexts." );

		if( scheduler != null ) {
			scheduler.shutdownNow();
		}

		if( workerQueue != null ) {
			// Drops instant interrupt signals down to the thread executing the active copy sequence
			workerQueue.shutdownNow();
			try {
				if( !workerQueue.awaitTermination( timeout, TimeUnit.MILLISECONDS ) ) {
					Debug.printDebug( "[Bg Controller] Worker queue termination delayed. Enforcing lifecycle exit." );
				}
			}catch( InterruptedException _ ) {
				Thread.currentThread().interrupt();
			}
		}

		// Clean up framework tracking tokens across the execution stack
		for( final SyncJobContext job : jobList ) {
			job.setRunning( false );
			job.setActiveWorkerThread( null );
		}

		// Remove indicator shell icons
		if( sysTray != null && bgView != null && bgView.getTrayIcon() != null ) {
			sysTray.remove( bgView.getTrayIcon() );
		}

		Debug.printDebug( "[Bg Controller] Background-Daemon terminated cleanly." );
	}

	/**
	 * Configures the internal asynchronous execution environment for isolation testing.
	 * <p>
	 * This configuration interface swaps out production thread pools with deterministic
	 * mock implementations and scales execution time windows. It guarantees atomic evaluation
	 * boundaries without leaking OS threads during unit test runs.
	 * </p>
	 *
	 * @param multiplier  The scaling factor applied to time calculations (e.g., fractional values)
	 * @param bgView      The background view for ui interactions.
	 * @param scheduler   The scheduled executor tracking the heartbeat loops
	 * @param workerQueue The sequential worker queue processing pending sync transfers
	 */
	private void setEnvironment( final double multiplier, final BgView bgView, final ScheduledExecutorService scheduler, final ExecutorService workerQueue ) {
		if( multiplier > 0.0 ) this.timeMultiplier = multiplier;
		if( bgView != null ) this.bgView = bgView;
		if( scheduler != null ) this.scheduler = scheduler;
		if( workerQueue != null ) this.workerQueue = workerQueue;
	}
}