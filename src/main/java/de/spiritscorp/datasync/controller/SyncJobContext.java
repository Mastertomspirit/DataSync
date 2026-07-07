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

import java.nio.file.Path;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.model.FileAttributes;

/**
 * Manages the reactive runtime context for an individual synchronization or backup task.
 * Holds task-specific properties, isolated file tables, and active worker thread references.
 * * @author Tom Spirit
 */
public class SyncJobContext {

	private final StringProperty jobName = new SimpleStringProperty();
	private final BooleanProperty running = new SimpleBooleanProperty( false );
	private final StringProperty statusMessage = new SimpleStringProperty( "Bereit" );
	private final StringProperty logOutput = new SimpleStringProperty( "" );
	private final StringProperty selectedMode = new SimpleStringProperty( ScanType.SYNCHRONIZE.getDescription() );

	private final Preference taskPreference;
	private Thread activeWorkerThread;

	private final ObservableList<FileRow> duplicateFiles = FXCollections.observableArrayList();

	/**
	 * Creates a new isolated synchronization job environment with its own preference clone.
	 * * @param name The identification name for the user interface sidebar
	 *
	 * @param taskPreference The template preference instance to derive task-specific settings from
	 */
	public SyncJobContext( final String name, final Preference taskPreference ) {
		this.jobName.set( name );
		this.taskPreference = taskPreference;
	}

	/**
	 * Assigns the thread processing file modifications to allow secure termination handles.
	 * * @param thread The execution context running background tasks
	 */
	void setActiveWorkerThread( final Thread thread ) { this.activeWorkerThread = thread; }

	/**
	 * Signals the underlying worker thread to terminate via standard interruption flags.
	 * If a timeout greater than zero is specified, this method blocks the invoking context
	 * to await a graceful structural thread finalization.
	 *
	 * @param timeoutMs Maximum duration in milliseconds to await thread join; 0 executes asynchronously.
	 */
	public synchronized void cancelRunningTask( final long timeoutMs ) {
		if( activeWorkerThread != null && activeWorkerThread.isAlive() ) {
			Debug.printDebug( "[Info] Sending interruption signal to worker thread for job: %s", getJobName() );
			activeWorkerThread.interrupt();

			if( timeoutMs > 0 ) {
				try {
					// Gracefully await the thread to flush buffers and exit its iteration loops
					activeWorkerThread.join( timeoutMs );
					if( activeWorkerThread.isAlive() ) {
						Debug.printDebug( "[Warn] Warning: Worker thread for job '%s' breached timeout matrix.", getJobName() );
					}
				}catch( InterruptedException _ ) {
					Debug.printDebug( "[Info] Thread joining sequence was interrupted for job: %s", getJobName() );
					Thread.currentThread().interrupt();
				}
			}

			// If it was cleared or joined successfully, adjust states safely
			if( !activeWorkerThread.isAlive() ) {
				setRunning( false );
				setStatusMessage( "Aktion erfolgreich beendet." );
				appendLog( "-> Vorgang sauber beendet." );
			}else {
				setRunning( false );
				setStatusMessage( "Aktion vom Benutzer abgebrochen (Forced)." );
				appendLog( "-> Vorgang erzwungen abgebrochen." );
			}
		}else {
			updateUIAndLog( "Keine aktive Aktion.", "-> Nichts zu beenden gefunden." );
		}
	}

	/**
	 * Helper method to safely update UI properties and internal log feeds across thread boundaries.
	 */
	private void updateUIAndLog( final String status, final String logEntry ) {
		if( Platform.isFxApplicationThread() ) {
			setStatusMessage( status );
			appendLog( logEntry );
		}else {
			try {
				Platform.runLater( () -> {
					setStatusMessage( status );
					appendLog( logEntry );
				} );
			}catch( IllegalStateException _ ) {
				// Caught if the JavaFX toolkit is already dead during a hard native OS shutdown.
				// We log the text purely to the background core debug stream.
				Debug.printDebug( "[Info] GUI framework offline. Suppressed state update: %s (%s)", status, logEntry );
			}
		}
	}

	public void appendLog( final String line ) {
		this.logOutput.set( this.logOutput.get() + line + System.lineSeparator() );
	}

	void clearLog() {
		this.logOutput.set( "" );
	}

	public String getJobName() { return jobName.get(); }

	void setJobName( final String newTaskName ) {
		this.jobName.set( newTaskName );
	}

	public StringProperty jobNameProperty() {
		return jobName;
	}

	public boolean isRunning() { return running.get(); }

	public BooleanProperty runningProperty() {
		return running;
	}

	void setRunning( final boolean value ) {
		this.running.set( value );
	}

	public String getStatusMessage() { return statusMessage.get(); }

	public StringProperty statusMessageProperty() {
		return statusMessage;
	}

	public void setStatusMessage( final String message ) {
		this.statusMessage.set( message );
	}

	public String getLogOutput() { return logOutput.get(); }

	public StringProperty logOutputProperty() {
		return logOutput;
	}

	public ScanType getSelectedMode() { return ScanType.get( selectedMode.get() ); }

	public StringProperty selectedModeProperty() {
		return selectedMode;
	}

	public void setSelectedMode( final String mode ) {
		this.selectedMode.set( mode );
	}

	public Preference getPreference() { return taskPreference; }

	public ObservableList<FileRow> getDuplicateFiles() { return duplicateFiles; }

	/**
	 * Wraps file characteristics inside property types suited for dynamic UI grids.
	 */
	public static class FileRow {
		private final BooleanProperty selected = new SimpleBooleanProperty( false );
		private final StringProperty fileName = new SimpleStringProperty();
		private final StringProperty size = new SimpleStringProperty();
		private final StringProperty hash = new SimpleStringProperty();
		private final StringProperty path = new SimpleStringProperty();
		private final Path fileSystemPath;

		public FileRow( final Path path, final FileAttributes attr, final String readableSize ) {
			this.fileSystemPath = path;
			this.fileName.set( attr.getFileName() );
			this.size.set( readableSize );
			this.hash.set( attr.getFileHash() );
			this.path.set( path.toString() );
		}

		public BooleanProperty selectedProperty() {
			return selected;
		}

		public boolean isSelected() { return selected.get(); }

		public void setSelected( final boolean val ) {
			this.selected.set( val );
		}

		public StringProperty fileNameProperty() {
			return fileName;
		}

		public StringProperty sizeProperty() {
			return size;
		}

		public StringProperty hashProperty() {
			return hash;
		}

		public StringProperty pathProperty() {
			return path;
		}

		public Path getFileSystemPath() { return fileSystemPath; }
	}
}
