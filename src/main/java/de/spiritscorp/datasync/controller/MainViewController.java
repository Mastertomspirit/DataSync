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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import de.spiritscorp.datasync.gui.DialogService;
import de.spiritscorp.datasync.gui.Gui;
import de.spiritscorp.datasync.gui.NotifyStatus;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.theme.AppTheme;

/**
 * Central controller implementation executing operational state translations and business action flows.<br>
 *
 * Acting as the primary orchestrator (Mediator pattern), this component decouples the reactive
 * JavaFX user interface layer from the transactional backend service domains and persistence engines.
 * It intercepts user-driven view interactions, coordinates lifecycle mutations of background synchronization
 * tasks, and dispatches state-change signals across the active runtime workspace.
 *
 * @author Tom Spirit
 * @since 1.2.0
 */
public class MainViewController implements ViewController {

	/** The timeout limit in milliseconds for asynchronous background processes (e.g., automated file or task scans). */
	public static final int BG_TIMEOUT = 20_000;
	/** The timeout limit in milliseconds for regular, worker threads before a forced termination is triggered. */
	public static final int EXIT_TIMEOUT = 10_000;

	/** The primary user interface orchestration shell managing view states, layouts, and volatile notifications. */
	private final Gui gui;
	/** The centralized dialog orchestration service managing modal view lifecycles and user confirmation flows. */
	private final DialogService dialogService;
	/** The core service layer processing execution requests and lifecycle validations for synchronization tasks. */
	private final SyncJobService helper;
	/** The central preference manager coordinating serialization, persistence, and registration of global and job-specific profiles. */
	private final PreferenceManager manager;
	/** The execution controller overseeing scheduled background worker threads and daemon interval routines. */
	private BgController bgController;

	/**
	 * Constructs a primary controller instance, automatically provisioning the underlying dialog subsystem using the active window stage hook.
	 *
	 * @param gui The primary user interface orchestration shell managing view states and layouts.
	 */
	public MainViewController( final Gui gui ) {
		this(
				gui,
				new DialogService( gui.getWindowStage() ) );
	}

	/**
	 * Allocates a new controller instance tied directly to the display engine layer hook, bootstrapping the intermediate execution and configuration service layers.
	 *
	 * @param gui           The global display manager orchestrator application shell instance.
	 * @param dialogService The centralized dialog orchestration service managing modal view lifecycles.
	 */
	MainViewController( final Gui gui, final DialogService dialogService ) {
		this(
				gui,
				new SyncJobService( dialogService, new LogFormatter() ),
				PreferenceManager.getInstance(),
				dialogService );
		loadInitialJobList();
	}

	/**
	 * For TESTING
	 * <br>
	 * Constructs a new central view controller fully decoupled and initialized with its core
	 * architectural dependencies for isolated execution tracking.
	 *
	 * @param gui           The visual layout shell managing view hierarchies and user interaction states.
	 * @param helper        The core service layer orchestrating synchronization job execution routines.
	 * @param manager       The central preference authority handling profile configuration persistence.
	 * @param dialogService The centralized dialog orchestration service managing modal view lifecycles.
	 */
	MainViewController( final Gui gui, final SyncJobService helper, final PreferenceManager manager, final DialogService dialogService ) {
		this.dialogService = dialogService;
		this.gui = gui;
		this.helper = helper;
		this.manager = manager;
	}

	@SuppressWarnings( { "java:S106" } )
	@Override
	public void registerNativeShutdownHook() {
		Runtime.getRuntime().addShutdownHook( new Thread( () -> {
			// This block executes automatically if Windows/Linux sends a SIGTERM or shutdown signal
			Debug.printDebug( "[Exit] Host operating system shutdown signal intercepted via native runtime hook." );
			// Enforce rapid execution with small timeouts since the OS will forcefully kill us shortly
			executeShutdown( false );
			Debug.printDebug( "[Exit] BYE, BYE" );
			System.out.flush();
			System.err.flush();
		}, "DataSync-OS-Shutdown-Hook-Thread" ) );
		Debug.printDebug( "[Info] Native OS runtime shutdown hook successfully registered." );
	}

	@Override
	public void handleApplicationShutdown() {
		if( dialogService.promptOkChancel( "Programm beenden", "Möchten sie DataSync wirklich schließen?", "Aktive Hintergrunddienste werden wenn möglich sauber beendet." ) ) {
			Debug.printDebug( "[Exit] Complete system teardown triggered manually via user confirmation." );

			// Tear down the JavaFX UI framework layer immediately so the window closes for the user
			Platform.exit();
			// Execute executeCoreShutdownSequence in the System.exit() hook
			Debug.printDebug( "[Exit] Manual graceful teardown completed. Evicting core JVM runtime context loop." );
			System.exit( 0 ); // NOPMD Kill all
		}
	}

	@Override
	public void runInBackground( final boolean firstStart ) {
		bgController = new BgController( gui, this, gui.getJobList(), new Logger() );
		bgController.startBgJob( firstStart );
	}

	@Override
	public void handleAutostart( final boolean autostart ) {
		manager.setGlobalAutoStart( autostart );
		if( helper.setOSAutostart( autostart ) && manager.saveAllPreferences() ) {
			Debug.printDebug( "[Settings] OS desktop autostart hooks successfully synchronized to state: " + autostart );
		}else {
			Debug.printDebug( "[Settings] Warning: Failed to apply host operating system autostart modifications." );
		}
	}

	@Override
	public void handleNavigate( final Gui.ViewState state ) {
		gui.setViewState( state );
		if( !gui.getWindowStage().isShowing() ) {
			gui.getWindowStage().show();
			gui.getWindowStage().toFront();
		}
	}

	@Override
	public void handleCreateNewJob() {
		final String name = "Sync Job " + ( gui.getJobList().size() + 1 );
		gui.getJobList().add( new SyncJobContext( name, manager.createProfile( name, true ) ) );
		gui.showStatusNotification( name + " wurde erstellt und gespeichert", NotifyStatus.SUCCESS, Gui.INFO_DELAY );
	}

	@Override
	public void handleRenameJob( final SyncJobContext job ) {
		if( job != null ) {
			final String oldName = job.getJobName();
			final String newName = dialogService.promptTextInput( "Task umbenennen: " + oldName, "Geben Sie einen neuen Namen für den Task ein", "Neuer Name:" );
//		Check if not the same and don`t exists
			if( !newName.isBlank() && !newName.equals( oldName ) && manager.getProfile( newName ) == null ) {
				manager.renameProfile( oldName, newName, job.getPreference() );
				job.setJobName( newName );
				gui.showStatusNotification( oldName + " wurde ersetzt und gespeichert durch " + newName, NotifyStatus.SUCCESS, Gui.INFO_DELAY );
			}else {
				gui.showStatusNotification( oldName + " wurde nicht ersetzt", NotifyStatus.WARNING, Gui.INFO_DELAY );
			}
		}
	}

	@Override
	public void handleDuplicateJob( final SyncJobContext job ) {
		if( job != null ) {
			final String newName = job.getJobName() + " (Kopie)";
			final Preference pref = job.getPreference();
			gui.getJobList().addLast( new SyncJobContext( newName, manager.setNewProfile( newName, pref ) ) );
			gui.showStatusNotification( newName + " wurde erstellt und gespeichert", NotifyStatus.SUCCESS, Gui.INFO_DELAY );
		}else {
			gui.showStatusNotification( "Fehler: Job ist unbekannt", NotifyStatus.ERROR, Gui.INFO_DELAY );
		}
	}

	@Override
	public void deleteSelectedDuplicates( final SyncJobContext jobContext ) {
		helper.deleteSelectedDuplicates( jobContext );
	}

	@Override
	public void handleDeleteJob( final SyncJobContext job ) {
		if( job != null ) {
			final String jobName = job.getJobName();
			if( dialogService.promptYesNo( "Task entfernen", null, "Task '" + job.getJobName() + "' wirklich unwiderruflich löschen?" ) ) {
				gui.getJobList().remove( job );
				manager.removeProfile( jobName );
				gui.showStatusNotification( jobName + " wurde erfolgreich gelöscht", NotifyStatus.SUCCESS, Gui.INFO_DELAY );
			}else {
				gui.showStatusNotification( jobName + " wurde nicht gelöscht", NotifyStatus.WARNING, Gui.INFO_DELAY );
			}
		}
	}

	@Override
	public void handleDragJob( final int newIdx, final int draggedIdx ) {
		final SyncJobContext item = gui.getJobList().remove( draggedIdx );
		if( item != null ) {
			gui.getJobList().add( newIdx, item );
			manager.moveProfile( newIdx, draggedIdx, item.getPreference() );
		}
	}

	@Override
	public void handleExecuteTask( final SyncJobContext job ) {
		if( job != null ) {
			switch( job.getSelectedMode() ) { // NOPMD default is here ok
				case SYNCHRONIZE -> helper.startSynchronize( job );
				case DUBLICATE_SCAN -> helper.startDuplicateScan( job );
				case DEEP_SCAN, FLAT_SCAN -> helper.startBackup( job );
				default -> throw new IllegalArgumentException( "Unexpected value: " + job.getSelectedMode() );
			}
		}
	}

	@Override
	public void handleStopTask( final SyncJobContext job ) {
		job.cancelRunningTask( EXIT_TIMEOUT );
	}

	@Override
	public void handleSaveSettings( final AppTheme targetTheme ) {
		gui.changeTheme( targetTheme );
		gui.setViewState( Gui.ViewState.SETTINGS );

		// Persist structural configuration states securely to disk
		if( manager.saveAllPreferences() ) {
			gui.showStatusNotification( "Die Einstellungen wurden erfolgreich in der Konfiguration gespeichert.", NotifyStatus.SUCCESS, Gui.INFO_DELAY );
			Debug.printDebug( "[Settings] Configuration profile assets successfully serialized to disk." );
		}else {
			gui.showStatusNotification( "Fehler: Die Konfigurationsdaten konnten nicht in 'conf.json' geschrieben werden.", NotifyStatus.ERROR, Gui.INFO_DELAY );
			Debug.printDebug( "[Settings Error] Critical: Failed to persist configuration profile assets." );
		}
	}

	/**
	 * Orchestrates the application teardown sequence by gracefully processing or aborting
	 * active tasks based on the execution context boundaries.
	 *
	 * @param gracePeriod If true, allocates an extended time buffer per thread;
	 *                    if false (OS shutdown), enforces tight, rapid deadlines.
	 */
	private void executeShutdown( final boolean gracePeriod ) {
		final long timeout = gracePeriod ? BG_TIMEOUT : EXIT_TIMEOUT;
		Debug.printDebug( "[Exit] Internal system teardown invoked. Dynamic grace mode: %b -> %d ms", gracePeriod, timeout );
		// Delegate termination orchestration directly to the individual task contexts securely
		for( final SyncJobContext job : gui.getJobList() ) {
			job.cancelRunningTask( timeout );
		}
		if( !gui.isShowing() && bgController != null ) bgController.interruptBgJob( timeout );
		Debug.printDebug( "[Exit] Core teardown protocol finalized. Flushing runtime buffers." );
	}

	/**
	 * Bootstraps the primary synchronization job registry during application startup.
	 * <br>
	 * This method attempts to load and deserialize all previously persisted task profiles
	 * from the underlying configuration storage. If existing preferences are successfully recovered,
	 * they are mapped into operational context instances. If no data is found or initialization
	 * fails (e.g., during the first application launch), a pre-configured set of standard fallback
	 * profiles is generated to guarantee a seamless out-of-the-box user experience.
	 *
	 */
	private void loadInitialJobList() {
		final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();

		if( manager.loadAllPreferences() ) {
			manager.getLoadedProfiles().stream()
					.map( entry -> {
						final SyncJobContext ctx = new SyncJobContext( entry.getJobName(), entry );
						ctx.setSelectedMode( entry.getScanMode().getDescription() );
						return ctx;
					} )
					.forEach( jobList::add );
		}else {
			jobList.add( new SyncJobContext( "NAS Dokumente", manager.createProfile( "NAS  Dokumente", false ) ) );
			jobList.add( new SyncJobContext( "Lokales Workspace Backup", manager.createProfile( "Lokales Workspace Backup", false ) ) );
		}
		gui.setInitialJobConfigurations( jobList );
	}
}