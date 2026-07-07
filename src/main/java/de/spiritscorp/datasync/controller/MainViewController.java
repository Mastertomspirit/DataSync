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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import de.spiritscorp.datasync.Main;
import de.spiritscorp.datasync.gui.DialogService;
import de.spiritscorp.datasync.gui.Gui;
import de.spiritscorp.datasync.gui.NotifyStatus;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.theme.AppTheme;

/**
 * Central controller implementation executing operational state translations and business action flows.
 * * @author Tom Spirit
 */
public class MainViewController implements ViewController {

	private final Gui gui;
	private final SyncJobService helper;
	private final PreferenceManager manager;
	private BgController activeBgController;

	/**
	 * Allocates a new controller instance tied directly to the display engine layer hook.
	 *
	 * @param gui The global display manager orchestrator application shell instance.
	 */
	public MainViewController( final Gui gui ) {
		this(
				gui,
				new SyncJobService( new DialogService( gui.getWindowStage() ), new UiLogFormatter() ),
				PreferenceManager.getInstance() );
		loadInitialJobList();
	}

	MainViewController( final Gui gui, final SyncJobService helper, final PreferenceManager manager ) {
		this.gui = gui;
		this.helper = helper;
		this.manager = manager;
	}

	@Override
	public void registerNativeShutdownHook() {
		Runtime.getRuntime().addShutdownHook( new Thread( () -> {
			// This block executes automatically if Windows/Linux sends a SIGTERM or shutdown signal
			Debug.printDebug( "[Exit] Host operating system shutdown signal intercepted via native runtime hook." );
			// Enforce rapid execution with small timeouts since the OS will forcefully kill us shortly
			executeCoreShutdownSequence( false );
			Debug.printDebug( "[Exit] BYE, BYE" );
		}, "DataSync-OS-Shutdown-Hook-Thread" ) );
		Debug.printDebug( "[Info] Native OS runtime shutdown hook successfully registered." );
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
	public void handleApplicationShutdown() {
		final Alert confirmation = new Alert( Alert.AlertType.CONFIRMATION, "Hintergrunddienste werden beendet.", ButtonType.OK, ButtonType.CANCEL );
		confirmation.setTitle( "Programm beenden" );
		confirmation.setHeaderText( "Möchten Sie DataSync wirklich schließen?" );
		confirmation.setContentText( "Aktive Hintergrunddienste werden wenn möglich sauber beendet." );
		confirmation.initOwner( gui.getWindowStage() );

		if( confirmation.showAndWait().orElse( ButtonType.CANCEL ) == ButtonType.OK ) {
			Debug.printDebug( "[Exit] Complete system teardown triggered manually via user confirmation." );

			// Tear down the JavaFX UI framework layer immediately so the window closes for the user
			Platform.exit();
			// Execute executeCoreShutdownSequence in the System.exit() hook
			Debug.printDebug( "[Exit] Manual graceful teardown completed. Evicting core JVM runtime context loop." );
			System.exit( 0 );
		}
	}

	private void loadInitialJobList() {
		final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();

		if( manager.loadAllPreferences() ) {
			for( final Preference entry : manager.getLoadedProfiles() ) {
				final SyncJobContext ctx = new SyncJobContext( entry.getJobName(), entry );
				ctx.setSelectedMode( entry.getScanMode().getDescription() );
				jobList.add( ctx );
			}
		}else {
			jobList.add( new SyncJobContext( "NAS Dokumenten-Spiegel", manager.createProfile( "NAS Dokus" ) ) );
			jobList.add( new SyncJobContext( "Lokales Code-Workspace Backup", manager.createProfile( "WorkspaceRepo" ) ) );
		}
		gui.setInitialJobConfigurations( jobList );
	}

	@Override
	public void handleCreateNewJob() {
		final String name = "Sync Job " + ( gui.getJobList().size() + 1 );
		gui.getJobList().add( new SyncJobContext( name, manager.createProfile( name ) ) );
		gui.showStatusNotification( name + " wurde erstellt und gespeichert", NotifyStatus.SUCCESS, Main.INFO_DELAY );
	}

	@Override
	public void handleRenameJob( final SyncJobContext job ) {
		if( job != null ) {
			final String oldName = job.getJobName();
			final TextInputDialog dialog = new TextInputDialog( oldName );
			dialog.setTitle( "Task umbenennen" );
			dialog.setHeaderText( "Geben Sie einen neuen Namen für den Task ein:" );
			dialog.setContentText( "Name:" );
			dialog.initOwner( gui.getWindowStage() );
			dialog.showAndWait().ifPresent( newName -> {
				final String trimmedName = newName.trim();
//		Check if not the same and don`t exists
				if( !trimmedName.isEmpty() && !trimmedName.equals( oldName ) && manager.getProfile( trimmedName ) == null ) {
					manager.renameProfile( oldName, trimmedName, job.getPreference() );
					job.setJobName( trimmedName );
					gui.showStatusNotification( oldName + " wurde ersetzt und gespeichert durch" + newName, NotifyStatus.SUCCESS, Main.INFO_DELAY );
				}else {
					gui.showStatusNotification( oldName + " wurde nicht ersetzt", NotifyStatus.WARNING, Main.INFO_DELAY );
				}
			} );
		}
	}

	@Override
	public void handleDuplicateJob( final SyncJobContext job ) {
		if( job != null ) {
			String newName = job.getJobName() + " (Kopie)";
			Preference pref = job.getPreference();
			gui.getJobList().addLast( new SyncJobContext( newName, manager.setNewProfile( newName, pref ) ) );
			gui.showStatusNotification( newName + " wurde erstellt und gespeichert", NotifyStatus.SUCCESS, Main.INFO_DELAY );
		}else {
			gui.showStatusNotification( "Fehler: Job ist unbekannt", NotifyStatus.ERROR, Main.INFO_DELAY );
		}
	}

	@Override
	public void handleDeleteJob( final SyncJobContext job ) {
		if( job != null ) {
			final Alert alert = new Alert( Alert.AlertType.CONFIRMATION, "Task '" + job.getJobName() + "' wirklich unwiderruflich löschen?", ButtonType.YES, ButtonType.NO );
			alert.setTitle( "Task entfernen" );
			alert.setHeaderText( null );
			alert.initOwner( gui.getWindowStage() );
			alert.showAndWait().ifPresent( response -> {
				String jobName = job.getJobName();
				if( response == ButtonType.YES ) {
					gui.getJobList().remove( job );
					manager.removeProfile( jobName );
					gui.showStatusNotification( jobName + " wurde erfolgreich gelöscht", NotifyStatus.SUCCESS, Main.INFO_DELAY );
				}else {
					gui.showStatusNotification( jobName + " wurde nicht gelöscht", NotifyStatus.WARNING, Main.INFO_DELAY );
				}
			} );
		}
	}

	@Override
	public void handleDragJob( final int newIdx, final int draggedIdx ) {
		SyncJobContext item = gui.getJobList().remove( draggedIdx );
		if( item != null ) {
			gui.getJobList().add( newIdx, item );
			manager.moveProfile( newIdx, draggedIdx, item.getPreference() );
		}
	}

	@Override
	public void handleExecuteTask( final SyncJobContext job ) {
		if( job != null ) {
			switch( job.getSelectedMode() ) {
				case SYNCHRONIZE -> helper.startSynchronize( job );
				case DUBLICATE_SCAN -> helper.startDuplicateScan( job );
				case DEEP_SCAN, FLAT_SCAN -> helper.startBackup( job );
				default -> throw new IllegalArgumentException( "Unexpected value: " + job.getSelectedMode() );
			}
		}
	}

	@Override
	public void handleStopTask( final SyncJobContext job ) {
		job.cancelRunningTask( Main.EXIT_THREAD_TIMEOUT );
	}

	@Override
	public void handleSaveSettings( final AppTheme targetTheme ) {

		// Persist structural configuration states securely to disk
		final boolean prefsSaved = manager.saveAllPreferences();
		if( prefsSaved ) {
			Debug.printDebug( "[Settings] Configuration profile assets successfully serialized to disk." );
		}else {
			Debug.printDebug( "[Settings Error] Critical: Failed to persist configuration profile assets." );
		}

		// Adjust underlying host operating system autostart integration context matrix
		final boolean autostartTargetState = manager.isGlobalAutoStart();
		final boolean autostartSaved = helper.setOSAutostart( autostartTargetState );
		if( autostartSaved ) {
			Debug.printDebug( "[Settings] OS desktop autostart hooks successfully synchronized to state: " + autostartTargetState );
		}else {
			Debug.printDebug( "[Settings] Warning: Failed to apply host operating system autostart modifications." );
		}

		gui.changeTheme( targetTheme );
		gui.setViewState( Gui.ViewState.SETTINGS );

		// Trigger visual feedback via the global GUI proxy method using theme classes
		if( prefsSaved && autostartSaved ) {
			gui.showStatusNotification( "Settings successfully persisted to the configuration registry.", NotifyStatus.SUCCESS, Main.INFO_DELAY );
		}else if( !prefsSaved ) {
			gui.showStatusNotification( "Error: Failed to write configuration payload to 'conf.json'.", NotifyStatus.ERROR, Main.INFO_DELAY );
		}else {
			gui.showStatusNotification( "Warning: Profiles saved, but host operating system autostart configuration failed.", NotifyStatus.WARNING, Main.INFO_DELAY );
		}
	}

	@Override
	public void runInBackground( final boolean firstStart ) {
		activeBgController = new BgController( gui, this, gui.getJobList(), new Logger() );
		activeBgController.startBgJob( firstStart );
	}

	@Override
	public void deleteSelectedDuplicates( final SyncJobContext jobContext ) {
		helper.deleteSelectedDuplicates( jobContext );
	}

	/**
	 * Orchestrates the application teardown sequence by gracefully processing or aborting
	 * active tasks based on the execution context boundaries.
	 *
	 * @param dynamicGracePeriod If true, allocates an extended time buffer per thread;
	 *                           if false (OS shutdown), enforces tight, rapid deadlines.
	 */
	private void executeCoreShutdownSequence( final boolean dynamicGracePeriod ) {
		final long timeoutPerThreadMs = dynamicGracePeriod ? Main.BACKGROUND_THREAD_TIMEOUT : Main.EXIT_THREAD_TIMEOUT;
		Debug.printDebug( "[Exit] Internal system teardown invoked. Dynamic grace mode: %b -> %d ms", dynamicGracePeriod, timeoutPerThreadMs );
		// Delegate termination orchestration directly to the individual task contexts securely
		for( final SyncJobContext job : gui.getJobList() ) {
			job.cancelRunningTask( timeoutPerThreadMs );
		}
		if( !gui.isShowing() && activeBgController != null ) activeBgController.interruptBgJob( timeoutPerThreadMs );
		Debug.printDebug( "[Exit] Core teardown protocol finalized. Flushing runtime buffers." );
	}
}