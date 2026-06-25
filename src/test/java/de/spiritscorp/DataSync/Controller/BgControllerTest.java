package de.spiritscorp.DataSync.Controller;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.BgModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

/**
 * Enterprise-grade lifecycle and concurrency test suite for {@link BgController}.
 * <p>
 * This suite orchestrates complex multi-threading setups, OS interaction layers (SystemTray),
 * JavaFX window stages, and deep verification of transient worker states during execution cycles.
 * <p>
 *
 * @author Tom Spirit
 * @version 1.2.0
 * @see BgController
 */
@DisplayName( "Background Controller Engine Test Suite" )
class BgControllerTest {

	private Gui mockGui;
	private Stage mockStage;
	private ViewController mockViewController;
	private Logger mockLogger;
	private BgView mockBgView;
	private TrayIcon mockTrayIcon;
	private ObservableList<SyncJobContext> testJobList;

	private ScheduledExecutorService mockScheduler;
	private ExecutorService mockWorkerQueue;

	private MockedStatic<Debug> mockedDebug;
	private MockedStatic<SystemTray> mockedSystemTray;
	private SystemTray spySystemTray;

	/**
	 * Prepares the structural execution context prior to each test case execution.
	 * <p>
	 * This initialization hook configures virtual test doubles for JavaFX components
	 * ({@link Gui}, {@link Stage}) and foundational logging interfaces ({@link Logger}).
	 * Additionally, it bypasses native OS desktop resource locks by mocking the static
	 * {@link SystemTray} API to return a safe test proxy, preventing {@link NullPointerException}
	 * failures inside non-GUI test environments.
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		mockedDebug = Mockito.mockStatic( Debug.class );
		mockedSystemTray = Mockito.mockStatic( SystemTray.class );

		mockGui = mock( Gui.class );
		mockStage = mock( Stage.class );
		mockViewController = mock( ViewController.class );
		mockLogger = mock( Logger.class );
		mockBgView = mock( BgView.class );
		mockTrayIcon = mock( TrayIcon.class );

		mockScheduler = mock( ScheduledExecutorService.class );
		mockWorkerQueue = mock( ExecutorService.class );

		when( mockGui.getWindowStage() ).thenReturn( mockStage );
		when( mockBgView.getTrayIcon() ).thenReturn( mockTrayIcon );

		// Abstract underlying OS desktop ecosystem capabilities
		mockedSystemTray.when( SystemTray::isSupported ).thenReturn( true );

		spySystemTray = mock( SystemTray.class );
		mockedSystemTray.when( SystemTray::getSystemTray ).thenReturn( spySystemTray );

		testJobList = FXCollections.observableArrayList();
	}

	/**
	 * Cleans up static mock registrations upon test completion.
	 * <p>
	 * This teardown sequence releases global hooks attached to the JVM classloader by closing
	 * the {@code mockedDebug} and {@code mockedSystemTray} context boundaries. Ensuring a clean
	 * state reset prevents internal framework leaks and side effects from distorting the execution
	 * results of subsequent evaluation pipelines inside the test suite.
	 * </p>
	 */
	@AfterEach
	void tearDown() {
		mockedDebug.close();
		mockedSystemTray.close();
	}

	/**
	 * Test 01 - Verifies the internal mathematical interval resolution engine.
	 * Target: {@code determineOptimalCheckTime()}
	 */
	@Test
	@DisplayName( "01: Private Reflection - determineOptimalCheckTime should resolve the shortest interval configuration" )
	void testPrivateDetermineOptimalCheckTimeViaReflection() {
		testJobList.add( createMockJob( "Job-Disabled", false, BgTime.MIN_1, System.currentTimeMillis() ) );
		testJobList.add( createMockJob( "Job-30Min", true, BgTime.MIN_30, System.currentTimeMillis() ) );
		testJobList.add( createMockJob( "Job-Hourly", true, BgTime.HOURLY, System.currentTimeMillis() ) );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		final Method targetMethod = ReflectionSupport.findMethod( BgController.class, "determineOptimalCheckTime" )
				.orElseThrow( () -> new AssertionError( "Private method target identifier not found." ) );
		final Long minimumCheckInterval = (Long) ReflectionSupport.invokeMethod( targetMethod, controller );

		// Assumes BgTime.MIN_30.getCheckTime() maps to its respective time metrics
		assertEquals( BgTime.MIN_30.getCheckTime(), minimumCheckInterval.longValue(),
				"The internal interval resolver failed to identify the minimum interval configuration." );
	}

	/**
	 * Test 02 - Verifies standard execution tick scheduling parameter mapping.
	 * Target: {@code startBgJob(false)}
	 */
	@Test
	@DisplayName( "02: Executor Ticking Verification - startBgJob maps interval math perfectly into the executor core parameters" )
	void testStartBgJobConfiguresExecutorTickRates() {
		final BgTime time = BgTime.MIN_30;
		testJobList.add( createMockJob( "Job-30Min", true, time, System.currentTimeMillis() ) );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		// Break encapsulation to override default initialization states
		injectMockExecutors( controller );
		controller.startBgJob( false );

		// Verify accurate scheduler registration metrics instead of scraping stdout logs
		verify( mockScheduler, times( 1 ) ).scheduleAtFixedRate(
				any( Runnable.class ),
				eq( BgController.INITIAL_DELAY ),
				eq( time.getCheckTime() ), // tickInterval (calculatedTick * 1.0)
				eq( TimeUnit.MILLISECONDS ) );
	}

	/**
	 * Test 03 - Verifies initial delay shifts when processing cold application boot states.
	 * Target: {@code startBgJob(true)}
	 */
	@Test
	@DisplayName( "03: Executor Boot Verification - startBgJob maps interval into initial_delay after system boot" )
	void testStartBgJobConfiguresExecutorBootDelay() {
		final BgTime time = BgTime.MIN_30;
		testJobList.add( createMockJob( "Job-30Min", true, time, System.currentTimeMillis() ) );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		// Break encapsulation to override default initialization states
		injectMockExecutors( controller );
		controller.startBgJob( true );

		// Verify accurate scheduler registration metrics instead of scraping stdout logs
		verify( mockScheduler, times( 1 ) ).scheduleAtFixedRate(
				any( Runnable.class ),
				eq( BgController.BOOT_START_DELAY ),
				eq( time.getCheckTime() ), // tickInterval (calculatedTick * 1.0)
				eq( TimeUnit.MILLISECONDS ) );
	}

	/**
	 * Test 04 - Evaluates target operating system shell bindings and view transitions.
	 * Target: {@code startBgJob(boolean)} UI Hooks
	 */
	@Test
	@DisplayName( "04: UI & SystemTray Lifecycle - Verify strict orchestration of UI stage hiding and tray mounting" )
	void testStartBgJobHidesGuiAndMountsTray() throws Exception {
		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		controller.startBgJob( false );

		verify( mockStage, times( 1 ) ).hide();
		verify( spySystemTray, times( 1 ) ).add( mockTrayIcon );
	}

	/**
	 * Test 05 - Exercises the critical polling pipeline and task transition rules synchronizing states.
	 * Target: {@code checkAndQueueJobs()} Deterministic Thread Execution
	 */
	@Test
	@DisplayName( "05: Asynchronous Execution Lifecycle - Tracking job state mutation hooks, thread injection, and Model execution loops" )
	void testCheckAndQueueJobsStateOrchestration() {
		final long expiredTimeDelta = System.currentTimeMillis() - BgTime.HOURLY.getTime();
		final SyncJobContext overdueJob = createMockJob( "Overdue-Process", true, BgTime.MIN_30, expiredTimeDelta );

		when( overdueJob.isRunning() ).thenReturn( false );
		testJobList.add( overdueJob );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		final Method checkMethod = ReflectionSupport.findMethod( BgController.class, "checkAndQueueJobs" )
				.orElseThrow( () -> new AssertionError( "Private method context identifier not found." ) );

		// Trigger polling logic inside current execution context
		ReflectionSupport.invokeMethod( checkMethod, controller );
		// Part 1: Verify the job transition state maps to true prior to thread worker handshakes
		verify( overdueJob, times( 1 ) ).setRunning( true );

		// Part 2: Extract and execute the worker context sent to the execution thread pool lane
		final ArgumentCaptor<Runnable> workerCaptor = ArgumentCaptor.forClass( Runnable.class );
		verify( mockWorkerQueue, times( 1 ) ).execute( workerCaptor.capture() );

		// Intercept inline object creation of BgModel to prevent filesystem thrashing
		try( MockedConstruction<BgModel> mockedModelConstruction = mockConstruction( BgModel.class ) ) {

			// Execute task payload synchronously
			workerCaptor.getValue().run();

			// Part 3: Verify the transient worker registration hooks fired correctly
			verify( overdueJob, times( 1 ) ).setActiveWorkerThread( Thread.currentThread() );
			// Part 4: Assure core processing loop was executed
			final BgModel constructedModel = mockedModelConstruction.constructed().get( 0 );
			verify( constructedModel, times( 1 ) ).runBgJob();
			// Part 5: Verify automatic resource and context cleanups inside finally blocks
			verify( overdueJob, times( 1 ) ).setRunning( false );
			verify( overdueJob, times( 1 ) ).setActiveWorkerThread( null );
		}
	}

	/**
	 * Test 06 - Assures total fallback isolation when individual background tasks experience runtime errors.
	 * Target: {@code checkAndQueueJobs()} Robust Exception Isolation
	 */
	@Test
	@DisplayName( "06: Interrupt & Exception Resilience - Worker thread failures or abort signals must fully roll back active execution states" )
	void testCheckAndQueueJobsResilienceUnderInterrupts() {
		final long expiredTimeDelta = System.currentTimeMillis() - ( BgTime.MIN_30.getTime() + 5000 );
		final SyncJobContext faultyJob = createMockJob( "Faulty-Process", true, BgTime.MIN_30, expiredTimeDelta );

		when( faultyJob.isRunning() ).thenReturn( false );
		testJobList.add( faultyJob );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		final Method checkMethod = ReflectionSupport.findMethod( BgController.class, "checkAndQueueJobs" ).orElseThrow();
		ReflectionSupport.invokeMethod( checkMethod, controller );
		final ArgumentCaptor<Runnable> workerCaptor = ArgumentCaptor.forClass( Runnable.class );
		verify( mockWorkerQueue ).execute( workerCaptor.capture() );

		// Force a runtime exception down the pipeline execution lane
		try( MockedConstruction<BgModel> mockedModelConstruction = mockConstruction( BgModel.class, ( mock, context ) -> {
			doThrow( new RuntimeException( "Simulated Thread Interruption Fault" ) ).when( mock ).runBgJob();
		} ) ) {
			assertDoesNotThrow( () -> workerCaptor.getValue().run() );
			// Assure state switches rolled back flawlessly inside finally segments despite severe crashes
			verify( faultyJob, times( 1 ) ).setRunning( false );
			verify( faultyJob, times( 1 ) ).setActiveWorkerThread( null );
		}
	}

	/**
	 * Test 07 - Validates clean runtime terminations, pool teardowns, and surface display restoration.
	 * Target: {@code interruptBgJob()}
	 */
	@Test
	@DisplayName( "07: Teardown Coordination - Decoupling thread frames, unmounting hardware notification shells, and exposing UI surfaces" )
	void testInterruptBgJobCompleteTeardownSequence() throws Exception {
		final SyncJobContext activeJob = createMockJob( "Aborted-Process", true, BgTime.MIN_30, System.currentTimeMillis() );
		testJobList.add( activeJob );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		controller.startBgJob( false );

		// Simulate successful framework feedback metrics
		when( mockWorkerQueue.awaitTermination( anyLong(), any( TimeUnit.class ) ) ).thenReturn( true );

		// Command daemon termination sequence
		assertDoesNotThrow( () -> controller.interruptBgJob( Main.BACKGROUND_THREAD_TIMEOUT ) );
		// Part 1: Verify window environment visibility is brought back immediately
		verify( mockStage, times( 1 ) ).show();
		// Part 2: Verify thread destruction routines are systematically passed down
		verify( mockScheduler, times( 1 ) ).shutdownNow();
		verify( mockWorkerQueue, times( 1 ) ).shutdownNow();
		// Part 3: Verify context parameters are flushed completely
		verify( activeJob, times( 1 ) ).setRunning( false );
		verify( activeJob, times( 1 ) ).setActiveWorkerThread( null );
		// Part 4: Verify the active OS Shell context indicator was completely scrubbed
		verify( spySystemTray, times( 1 ) ).remove( mockTrayIcon );
	}

	/**
	 * Test 08 - Verifies immediate initialization abort stress boundaries.
	 * Target: {@code handleApplicationShutdown()} Race Condition Resistance
	 */
	@Test
	@DisplayName( "08: Stress Case - Fast application shutdown directly after background routine initialization" )
	void testImmediateShutdownResilience() {
		testJobList.add( createMockJob( "Stress-Job", true, BgTime.WEEKLY, System.currentTimeMillis() ) );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		assertDoesNotThrow( () -> {
			controller.startBgJob( false );
			// Instantly teardown concurrent workers right after starting up
			controller.requestApplicationShutdown();
		}, "Concurrency pipeline threw an unexpected unhandled exception during immediate system cleanup." );
		verify( mockViewController, times( 1 ) ).handleApplicationShutdown();
	}

	/**
	 * Test 09 - Assures explicit user background preference flags are respected.
	 * Target: {@code checkAndQueueJobs()} Polling Skips
	 */
	@Test
	@DisplayName( "09: Skip Conditions - Polling routine must ignore jobs where background synchronization is disabled" )
	void testCheckAndQueueJobsSkipsWhenBgSyncIsDisabled() {
		// Even if the job is heavily overdue, bgSync = false must prevent any queueing actions
		final long overdueTimestamp = System.currentTimeMillis();
		final SyncJobContext disabledJobMIN30 = createMockJob( "Disabled-Min30", false, BgTime.MIN_30, overdueTimestamp );
		final SyncJobContext disabledJobHourly = createMockJob( "Disabled-Hourly", false, BgTime.HOURLY, overdueTimestamp );

		testJobList.add( disabledJobMIN30 );
		testJobList.add( disabledJobHourly );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );
		final Method checkMethod = ReflectionSupport.findMethod( BgController.class, "checkAndQueueJobs" ).orElseThrow();
		assertDoesNotThrow( () -> ReflectionSupport.invokeMethod( checkMethod, controller ) );

		// Assert that the workers were never touched or flagged as running
		verify( disabledJobMIN30, never() ).setRunning( true );
		verify( disabledJobHourly, never() ).setRunning( true );
		verify( mockWorkerQueue, never() ).execute( any( Runnable.class ) );
	}

	/**
	 * Test 10 - Tests absolute time limit boundaries to avoid double scheduling tasks.
	 * Target: {@code checkAndQueueJobs()} Threshold Fencing
	 */
	@Test
	@DisplayName( "10: Boundary Verification - Polling routine must skip executions when intervals are within valid time limits" )
	void testCheckAndQueueJobsSkipsWhenIntervalHasNotElapsed() {
		// Simulate that all jobs have just been scanned 5 seconds ago (well within any interval limit)
		final long recentScanTimestamp = System.currentTimeMillis() - 5000;

		final SyncJobContext disabledJobMin1 = createMockJob( "Fresh-Min30", false, BgTime.MIN_1, recentScanTimestamp );
		final SyncJobContext freshJobMin30 = createMockJob( "Fresh-Min30", true, BgTime.MIN_30, recentScanTimestamp );
		final SyncJobContext freshJobHourly = createMockJob( "Fresh-Hourly", true, BgTime.HOURLY, recentScanTimestamp );
		final SyncJobContext freshJobDaily = createMockJob( "Fresh-Daily", true, BgTime.DAYLY, recentScanTimestamp );

		testJobList.add( disabledJobMin1 );
		testJobList.add( freshJobMin30 );
		testJobList.add( freshJobHourly );
		testJobList.add( freshJobDaily );

		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );
		final Method checkMethod = ReflectionSupport.findMethod( BgController.class, "checkAndQueueJobs" ).orElseThrow();
		assertDoesNotThrow( () -> ReflectionSupport.invokeMethod( checkMethod, controller ) );

		// Guarantee that no state mutations or task dispatches were triggered
		verify( disabledJobMin1, never() ).setRunning( true );
		verify( freshJobMin30, never() ).setRunning( true );
		verify( freshJobHourly, never() ).setRunning( true );
		verify( freshJobDaily, never() ).setRunning( true );
		verify( mockWorkerQueue, never() ).execute( any( Runnable.class ) );
	}

	/**
	 * Test 11 - Validates OS-level error recovery and immediate GUI fallback state management.
	 * Target: {@code startBgJob(boolean)} Error Interception Fallback
	 */
	@Test
	@DisplayName( "11: Exception Fallback - Should restore GUI visibility and abort execution if TrayIcon registration fails" )
	void testStartBgJobFallbackOnAwtException() throws Exception {
		final BgController controller = new BgController( mockGui, mockViewController, testJobList, mockLogger );
		injectMockExecutors( controller );

		// Enforce an AWTException when the controller tries to add the TrayIcon to the OS SystemTray
		doThrow( new java.awt.AWTException( "Simulated OS Tray Capacity Exhaustion" ) )
				.when( spySystemTray ).add( any( TrayIcon.class ) );
		assertDoesNotThrow( () -> controller.startBgJob( false ) );
		// Part 1: Verify the lifecycle protocol hid the stage initially
		verify( mockStage, times( 1 ) ).hide();
		// Part 2: Verify the error fallback immediately forced the stage back to visible
		verify( mockStage, times( 1 ) ).show();
		// Part 3: Verify the scheduler registration was completely skipped due to the early return
		verify( mockScheduler, never() ).scheduleAtFixedRate( any( Runnable.class ), anyLong(), anyLong(), any( TimeUnit.class ) );
	}

	/**
	 * Helper to assemble a standardized mock environment for internal sync jobs.
	 */
	private SyncJobContext createMockJob( String name, boolean bgSync, BgTime bgTime, long lastScan ) {
		final SyncJobContext mockJob = mock( SyncJobContext.class );
		final var mockPref = mock( Preference.class );

		when( mockJob.getJobName() ).thenReturn( name );
		when( mockJob.getPreference() ).thenReturn( mockPref );
		when( mockPref.isBgSync() ).thenReturn( bgSync );
		when( mockPref.getBgTime() ).thenReturn( bgTime );
		when( mockPref.getLastScanTime() ).thenReturn( lastScan );

		return mockJob;
	}

	/**
	 * Internal reflection abstraction utility to swap default instances with custom testing mocks.
	 */
	private void injectMockExecutors( BgController controller ) {
		try {
			final Method setEnvironment = BgController.class.getDeclaredMethod( "setEnvironment", double.class, BgView.class, ScheduledExecutorService.class, ExecutorService.class );
			setEnvironment.setAccessible( true );
			setEnvironment.invoke( controller, 1.0, mockBgView, mockScheduler, mockWorkerQueue );
		}catch( IllegalAccessException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( "Failed to inject architectural test values via reflection.", e );
		}catch( final InvocationTargetException e ) {
			throw new RuntimeException( "Failed to invoke the methode via reflection.", e );
		}
	}
}