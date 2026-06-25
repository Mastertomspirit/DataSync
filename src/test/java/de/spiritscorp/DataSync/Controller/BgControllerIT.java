package de.spiritscorp.DataSync.Controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

/**
 * Integration test suite for {@link BgController}.
 * <p>
 * Unlike isolated unit tests, this suite validates the concurrent collaboration
 * between the controller's background daemon, actual live thread executor pools,
 * and structural state synchronization across active sync tasks.
 * </p>
 *
 * @author Tom Spirit
 * @version 1.0.0
 * @see BgController
 */
@DisplayName("BgController Integration Test Suite")
class BgControllerIT {

	private Gui mockGui;
	private Stage mockStage;
	private ViewController mockViewController;
	private Logger mockLogger;
	private BgView mockBgView;
	private TrayIcon mockTrayIcon;
	private ObservableList<SyncJobContext> integrationJobList;

	private MockedStatic<Debug> mockedDebug;
	private MockedStatic<SystemTray> mockedSystemTray;
	private SystemTray mockSystemTray;

	private BgController controller;

	/**
	 * Configures the integration environment before each test case execution.
	 * <p>
	 * This setup initializes environmental subsystem mocks while preparing
	 * the controller to spin up authentic, live production thread pools instead
	 * of intercepted mock queues.
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		mockedDebug = Mockito.mockStatic(Debug.class);
		mockedSystemTray = Mockito.mockStatic(SystemTray.class);

		mockGui = mock(Gui.class);
		mockStage = mock(Stage.class);
		mockViewController = mock(ViewController.class);
		mockLogger = mock(Logger.class);
		mockBgView = mock(BgView.class);
		mockTrayIcon = mock(TrayIcon.class);

		when(mockGui.getWindowStage()).thenReturn(mockStage);
		when(mockBgView.getTrayIcon()).thenReturn(mockTrayIcon);

		mockedSystemTray.when(SystemTray::isSupported).thenReturn(true);
		mockSystemTray = mock(SystemTray.class);
		mockedSystemTray.when(SystemTray::getSystemTray).thenReturn(mockSystemTray);

		integrationJobList = FXCollections.observableArrayList();
	}

	/**
	 * Cleans up concurrent infrastructure and environmental registrations upon test completion.
	 * <p>
	 * Ensures that all background thread executors spun up during integration testing
	 * are explicitly shut down to avoid trailing daemon threads leaking into subsequent builds.
	 * </p>
	 */
	@AfterEach
	void tearDown() {
		if (controller != null) {
			try {
				controller.requestApplicationShutdown();
			} catch (final Exception ignored) {
				// Prevent teardown faults from masking compilation errors
			}
		}
		mockedDebug.close();
		mockedSystemTray.close();
	}

	/**
	 * Test 01 - Verifies the complete end-to-end integration lifecycle under real scheduling conditions.
	 * <p>
	 * This test uses a fractional time multiplier to accelerate the background clock, allowing a real
	 * heartbeat tick to fire on production threads, triggering state modifications and spawning sub-tasks.
	 * </p>
	 */
	@Test
	@DisplayName("01: End-to-End Execution - Live scheduler loop triggers overdue job and transitions states")
	void testEndToEndSchedulerTriggersJob() throws Exception {
		// Arrange: Create an overdue task using an accelerated execution multiplier
		final long overdueTimestamp = System.currentTimeMillis() - BgTime.MIN_30.getTime();
		final SyncJobContext liveJob = createIntegrationJob("Live-Integration-Task", true, BgTime.MIN_30, overdueTimestamp);
		final CountDownLatch jobStartedLatch = new CountDownLatch(1);

		// Intercept the execution hook on the live job to signal our test thread when it running state changes
		doAnswer(invocation -> {
			final Boolean isRunning = invocation.getArgument(0);
			if (isRunning) {
				jobStartedLatch.countDown();
			}
			return null;
		}).when(liveJob).setRunning(anyBoolean());
		integrationJobList.add(liveJob);

		controller = new BgController(mockGui, mockViewController, integrationJobList, mockLogger);
		// Accelerate time bounds via reflection: Scale interval down dramatically for rapid ticking
		injectMockExecutors(controller, 0.0001);

		// Act: Start the engine
		controller.startBgJob(false);

		// Assert: Verify that the background thread engine actually processed the job within a safe timeout
		final boolean executedSuccessfully = jobStartedLatch.await(2000, TimeUnit.MILLISECONDS);
		assertTrue(executedSuccessfully, "The asynchronous integration pipeline failed to execute the task queue loop.");
	}

	/**
	 * Test 02 - Validates system coordination during a complete shutdown chain.
	 * <p>
	 * Assures that active, live thread executors are terminated cleanly and that tracking metrics
	 * across all pending contexts are reset back to neutral.
	 * </p>
	 */
	@Test
	@DisplayName("02: System Teardown Integration - Real executor pools terminate and flush tracking states upon shutdown")
	void testLiveExecutorPoolShutdownChain() {
		// Arrange
		final SyncJobContext activeJob = createIntegrationJob("Active-Teardown-Task", true, BgTime.HOURLY, System.currentTimeMillis());
		integrationJobList.add(activeJob);

		controller = new BgController(mockGui, mockViewController, integrationJobList, mockLogger);
		controller.startBgJob(false);

		// Act: Execute the complete architectural shutdown sequence on live thread-pools
		assertDoesNotThrow(() -> controller.requestApplicationShutdown());
		// Assert: Verify state metrics are flushed and downstream components notified
		verify(activeJob, times(1)).setRunning(false);
		verify(activeJob, times(1)).setActiveWorkerThread(null);
		verify(mockViewController, times(1)).handleApplicationShutdown();
	}

	/**
	 * Test 03 - Validates the structural teardown of the live scheduler pool.
	 * <p>
	 * Assures that invoking the application shutdown sequence successfully transitions
	 * the real ScheduledExecutorService into a terminated lifecycle state, preventing
	 * any further background heartbeat ticks from being fired.
	 * </p>
	 */
	@Test
	@DisplayName("03: Scheduler Lifecycle Integration - Real scheduler pool completely terminates upon application shutdown")
	void testLiveSchedulerPoolTerminatesCleanly() throws Exception {
		// Arrange: Setup controller with a standard task to initialize executors natively
		final SyncJobContext activeJob = createIntegrationJob("Scheduler-Teardown-Task", true, BgTime.MIN_30, System.currentTimeMillis());
		integrationJobList.add(activeJob);

		controller = new BgController(mockGui, mockViewController, integrationJobList, mockLogger);

		// Start the engine -> instantiates real ScheduledExecutorService
		controller.startBgJob(false);

		// Extract the real scheduler instance via reflection to inspect its runtime lifecycle state
		final Field schedulerField = BgController.class.getDeclaredField("scheduler");
		schedulerField.setAccessible(true);
		final ScheduledExecutorService realScheduler = (ScheduledExecutorService) schedulerField.get(controller);

		assertNotNull(realScheduler, "The live scheduler pool was not properly initialized by the controller.");
		assertFalse(realScheduler.isShutdown(), "The live scheduler pool was pre-terminated before the shutdown test sequence began.");
		// Act: Trigger the core application teardown
		assertDoesNotThrow(() -> controller.requestApplicationShutdown());
		// Assert: Verify the real JDK executor pool is dead
		assertTrue(realScheduler.isShutdown(), "The live scheduler executor pool failed to enter a shutdown state.");
	}

	/**
	 * Test 04 - Validates the structural teardown and resource release of the live worker pool.
	 * <p>
	 * Verifies that the dedicated single-threaded worker queue processing the actual data sync payloads
	 * is fully collapsed and terminated when a user explicitly interrupts the background routine.
	 * </p>
	 */
	@Test
	@DisplayName("04: Worker Pool Lifecycle Integration - Real worker queue completely terminates upon user interruption")
	void testLiveWorkerQueueTerminatesOnInterrupt() throws Exception {
		// Arrange
		final SyncJobContext activeJob = createIntegrationJob("Worker-Teardown-Task", true, BgTime.HOURLY, System.currentTimeMillis());
		integrationJobList.add(activeJob);

		controller = new BgController(mockGui, mockViewController, integrationJobList, mockLogger);
		controller.startBgJob(false);

		// Extract the real workerQueue instance via reflection to inspect its runtime lifecycle state
		final Field workerQueueField = BgController.class.getDeclaredField("workerQueue");
		workerQueueField.setAccessible(true);
		final ExecutorService realWorkerQueue = (ExecutorService) workerQueueField.get(controller);

		assertNotNull(realWorkerQueue, "The live worker executor queue was not properly initialized by the controller.");
		assertFalse(realWorkerQueue.isShutdown(), "The live worker queue was pre-terminated before the interrupt test sequence began.");

		// Act: Trigger an explicit user interrupt sequence (e.g., maximizing the GUI back from tray)
		assertDoesNotThrow(() -> controller.interruptBgJob(Main.BACKGROUND_THREAD_TIMEOUT));

		// Assert: Verify the real JDK worker thread pool is dead
		assertTrue(realWorkerQueue.isShutdown(), "The live worker executor pool failed to enter a shutdown state after an interrupt signal.");
	}

	/**
	 * Test 05 - Validates thread recovery behavior during repeated manual user interactions.
	 * <p>
	 * Simulates a scenario where a user repeatedly toggles the background daemon via the UI button.
	 * Since each invocation instantiates a fresh {@link BgController}, this test guarantees that
	 * continuous allocation and teardown cycles cleanly release JDK thread pools without creating orphans.
	 * </p>
	 */
	@Test
	@DisplayName("05: Concurrency Multi-Boot Integration - Repeated start and stop cycles cleanly recycle thread pools without leaks")
	void testMultiBootLifecycleIdempotency() throws Exception {
		// Arrange: Prepare the task payload parameters
		final SyncJobContext cycleJob = createIntegrationJob("Cycle-Task", true, BgTime.MIN_30, System.currentTimeMillis());
		integrationJobList.add(cycleJob);

		// Extract target field definitions once prior to entering the execution loop
		final Field schedulerField = BgController.class.getDeclaredField("scheduler");
		schedulerField.setAccessible(true);
		final Field workerQueueField = BgController.class.getDeclaredField("workerQueue");
		workerQueueField.setAccessible(true);

		// Act & Assert: Simulate 3 consecutive UI button click/toggle lifecycles
		for (int generation = 1; generation <= 3; generation++) {
			// Mimic the exact behavior of runInBackground(): Instantiate a completely fresh controller layer
			controller = new BgController(mockGui, mockViewController, integrationJobList, mockLogger);
			controller.startBgJob(false);

			final ScheduledExecutorService schedulerGen = (ScheduledExecutorService) schedulerField.get(controller);
			final ExecutorService workerGen = (ExecutorService) workerQueueField.get(controller);

			// Verify that the new generation allocates active operational resource frames
			assertFalse(schedulerGen.isShutdown(), "Scheduler generation " + generation + " died prematurely.");
			assertFalse(workerGen.isShutdown(), "Worker queue generation " + generation + " died prematurely.");

			// Dismantle this generation (simulates user maximizing the GUI or closing the context)
			controller.interruptBgJob(Main.BACKGROUND_THREAD_TIMEOUT);

			// Guarantee that the native OS thread pool frames were successfully dissolved
			assertTrue(schedulerGen.isShutdown(), "Scheduler generation " + generation + " failed to shutdown.");
			assertTrue(workerGen.isShutdown(), "Worker queue generation " + generation + " failed to shutdown.");
		}
	}

	/**
	 * Factory helper to assemble operational sync jobs using real configuration models.
	 */
	private SyncJobContext createIntegrationJob(String name, boolean bgSync, BgTime bgTime, long lastScan) {
		final SyncJobContext job = mock(SyncJobContext.class);
		final Preference pref = mock(Preference.class);

		when(job.getJobName()).thenReturn(name);
		when(job.getPreference()).thenReturn(pref);
		when(pref.isBgSync()).thenReturn(bgSync);
		when(pref.getBgTime()).thenReturn(bgTime);
		when(pref.getLastScanTime()).thenReturn(lastScan);

		return job;
	}

	/**
	 * Internal reflection abstraction utility to swap default instances with custom testing mocks.
	 */
	private void injectMockExecutors(BgController controller, double multiplier) {
		try {
			final Method setEnvironment = BgController.class.getDeclaredMethod("setEnvironment", double.class, BgView.class, ScheduledExecutorService.class, ExecutorService.class);
			setEnvironment.setAccessible(true);
			setEnvironment.invoke(controller, multiplier, mockBgView, null, null);
		} catch (IllegalAccessException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Failed to inject architectural test values via reflection.", e);
		} catch (final InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke the methode via reflection.", e);
		}
	}
}