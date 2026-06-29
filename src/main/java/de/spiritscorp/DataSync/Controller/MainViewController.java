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

import java.util.Map;

import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Gui.Gui;
import de.spiritscorp.DataSync.Gui.WorkspaceView.NotifyStatus;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.IO.PreferenceManager;
import de.spiritscorp.DataSync.Theme.AppTheme;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
	private final ContHelper helper;
	private final PreferenceManager manager;
	private BgController activeBgController;

	/**
	 * Allocates a new controller instance tied directly to the display engine layer hook.
	 *
	 * @param gui The global display manager orchestrator application shell instance.
	 */
	public MainViewController( Gui gui ) {
		this(
				gui,
				new ContHelper(),
				PreferenceManager.getInstance() );
		loadInitialJobList();
	}

	MainViewController( Gui gui, ContHelper helper, PreferenceManager manager ) {
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
			executeCoreShutdownSequence( true );
			Debug.printDebug( "[Exit] BYE, BYE" );
		}, "DataSync-OS-Shutdown-Hook-Thread" ) );
		Debug.printDebug( "[Info] Native OS runtime shutdown hook successfully registered." );
	}

	@Override
	public void handleNavigate( Gui.ViewState state ) {
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

//	@Override
	private void loadInitialJobList() {
		final ObservableList<SyncJobContext> jobList = FXCollections.observableArrayList();

		if( manager.loadAllPreferences() ) {
			for( final Map.Entry<String, Preference> entry : manager.getLoadedProfiles().entrySet() ) {
				final SyncJobContext ctx = new SyncJobContext( entry.getKey(), entry.getValue() );
				ctx.setSelectedMode( entry.getValue().getScanMode().getDescription() );
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
	}

	@Override
	public void handleRenameJob( ListCell<SyncJobContext> cell ) {
		final SyncJobContext selectedJob = cell.getItem();
		if( selectedJob != null ) {
			final String oldName = selectedJob.getJobName();
			final TextInputDialog dialog = new TextInputDialog( selectedJob.getJobName() );
			dialog.setTitle( "Task umbenennen" );
			dialog.setHeaderText( "Geben Sie einen neuen Namen für den Task ein:" );
			dialog.setContentText( "Name:" );
			dialog.initOwner( gui.getWindowStage() );
			dialog.showAndWait().ifPresent( newName -> {
				final String trimmedName = newName.trim();
				if( !trimmedName.isEmpty() && !trimmedName.equals( oldName ) ) {
					selectedJob.setJobName( trimmedName );
					gui.showStatusNotification( oldName + " wurde ersetzt und gespeichert durch" + newName, NotifyStatus.SUCESS, Main.INFO_DELAY );
				}else {
					gui.showStatusNotification( oldName + " wurde nicht ersetzt", NotifyStatus.WARNING, Main.INFO_DELAY );
				}
			} );
		}
	}

	@Override
	public void handleDuplicateJob( SyncJobContext job ) {
		if( job != null ) {
			gui.getJobList().add( new SyncJobContext( job.getJobName() + " (Kopie)", job.getPreference() ) );
		}
	}

	@Override
	public void handleDeleteJob( SyncJobContext job ) {
		if( job != null ) {
			final Alert alert = new Alert( Alert.AlertType.CONFIRMATION, "Task '" + job.getJobName() + "' wirklich unwiderruflich löschen?", ButtonType.YES, ButtonType.NO );
			alert.setTitle( "Task entfernen" );
			alert.setHeaderText( null );
			alert.initOwner( gui.getWindowStage() );
			alert.showAndWait().ifPresent( response -> {
				if( response == ButtonType.YES ) {
					gui.getJobList().remove( job );
					PreferenceManager.getInstance().removeProfile( job );
					gui.showStatusNotification( job.getJobName() + " wurde erfolgreich gelöscht", NotifyStatus.SUCESS, Main.INFO_DELAY );
				}else {
					gui.showStatusNotification( job.getJobName() + " wurde nicht gelöscht", NotifyStatus.WARNING, Main.INFO_DELAY );
				}
			} );
		}
	}

	@Override
	public void handleExecuteTask( SyncJobContext job ) {
		if( job != null ) {
			switch( job.getSelectedMode() ) {
			case ScanType.SYNCHRONIZE -> helper.startSynchronize( job );
			case ScanType.DUBLICATE_SCAN -> helper.startDuplicateScan( job );
			case DEEP_SCAN, FLAT_SCAN -> helper.startBackup( job );
			default -> throw new IllegalArgumentException( "Unexpected value: " + job.getSelectedMode() );
			}
		}
	}

	@Override
	public void handleStopTask( SyncJobContext job ) {
		job.cancelRunningTask( Main.THREAD_TIMEOUT );
	}

	@Override
	public void handleSaveSettings( Preference localPreferences, AppTheme targetTheme ) {
		if( localPreferences == null ) return;

		// Persist structural configuration states securely to disk
		final boolean prefsSaved = PreferenceManager.getInstance().saveAllPreferences();
		if( prefsSaved ) {
			Debug.printDebug( "[Settings] Configuration profile assets successfully serialized to disk." );
		}else {
			Debug.printDebug( "[Settings] Critical: Failed to persist configuration profile assets." );
		}

		// Adjust underlying host operating system autostart integration context matrix
		final boolean autostartTargetState = PreferenceManager.getInstance().isGlobalAutoStart();
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
			gui.showStatusNotification( "Settings successfully persisted to the configuration registry.", NotifyStatus.SUCESS, Main.INFO_DELAY );
		}else if( !prefsSaved ) {
			gui.showStatusNotification( "Error: Failed to write configuration payload to 'conf.json'.", NotifyStatus.ERROR, Main.INFO_DELAY );
		}else {
			gui.showStatusNotification( "Warning: Profiles saved, but host operating system autostart configuration failed.", NotifyStatus.WARNING, Main.INFO_DELAY );
		}
	}

	@Override
	public void runInBackground( boolean firstStart ) {
		activeBgController = new BgController( gui, this, gui.getJobList(), new Logger() );
		activeBgController.startBgJob( firstStart );
	}

	/**
	 * Orchestrates the application teardown sequence by gracefully processing or aborting
	 * active tasks based on the execution context boundaries.
	 *
	 * @param dynamicGracePeriod If true, allocates an extended time buffer per thread;
	 *                           if false (OS shutdown), enforces tight, rapid deadlines.
	 */
	private void executeCoreShutdownSequence( boolean dynamicGracePeriod ) {
		final long timeoutPerThreadMs = dynamicGracePeriod ? Main.BACKGROUND_THREAD_TIMEOUT : Main.THREAD_TIMEOUT;
		Debug.printDebug( "[Exit] Internal system teardown invoked. Dynamic grace mode: %b -> %d ms", dynamicGracePeriod, timeoutPerThreadMs );
		// Delegate termination orchestration directly to the individual task contexts securely
		for( final SyncJobContext job : gui.getJobList() ) {
			job.cancelRunningTask( timeoutPerThreadMs );
		}
		if( activeBgController != null ) activeBgController.interruptBgJob( timeoutPerThreadMs );
		Debug.printDebug( "[Exit] Core teardown protocol finalized. Flushing runtime buffers." );
	}
}