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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.application.Platform;

import de.spiritscorp.datasync.Main;
import de.spiritscorp.datasync.gui.DialogService;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;
import de.spiritscorp.datasync.model.FileAttributes;
import de.spiritscorp.datasync.model.Model;

/**
 * Orchestrates background thread processing for synchronization, backup,
 * and duplicate detection, utilizing task-isolated configuration states.
 *
 * @author Tom Spirit
 */
public class SyncJobService {

	/**
	 * Service used to display modal dialogs and confirmation prompts to the user.
	 */
	private final DialogService dialogService;

	/**
	 * Formatter utility responsible for converting raw sync metrics into human-readable UI logs.
	 */
	private final UiLogFormatter uiLog;

	/**
	 * Constructs a new {@code SyncJobService} with the required UI and logging dependencies.
	 *
	 * @param dialogService the service provider for user interaction dialogs
	 * @param uiLog         the formatter instance used to compile text summaries for the UI
	 */
	public SyncJobService( final DialogService dialogService, final UiLogFormatter uiLog ) {
		this.dialogService = dialogService;
		this.uiLog = uiLog;
	}

	/**
	 * Executes bidirectional folder synchronization using task-bound preferences.
	 *
	 * @param context Target environment details providing task variables
	 */
	public void startSynchronize( final SyncJobContext context ) {
		if( context.isRunning() ) return;

		context.setRunning( true );
		context.setStatusMessage( "Synchronisation gestartet. Scanne Verzeichnisse..." );
		context.clearLog();

		final Map<Path, FileAttributes> sourceMap = Model.createMap();
		final Map<Path, FileAttributes> destMap = Model.createMap();
		final Map<Path, FileAttributes> failMap = Model.createMap();
		final Preference pref = context.getPreference();
		final Model model = new Model( new Logger(), sourceMap, destMap );
		final Long[] stats = new Long[4];

		final Thread worker = new Thread( () -> {
			long startTime = System.nanoTime();
			try {
				final Path startDestPath = pref.getDestPaths().get( 0 );
				final Path startSourcePath = pref.getSourcePaths().get( 0 );

				if( startDestPath == null || !Files.exists( startDestPath ) ) {
					updateUIStatus( context, false, "Kein Ziellaufwerk vorhanden" );
					return;
				}

				if( pref.getSourcePaths().size() > 1 ) {
					updateUIStatus( context, false, "Die Synchronisierung funktioniert nur mit einem Quellordner!" );
					return;
				}

				failMap.putAll( model.scanSyncFiles( pref.getSourcePaths(), pref.getDestPaths(), stats, pref.getScanMode(), false, false ) );
				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				final ArrayList<Map<Path, FileAttributes>> result = model.getSyncFiles( pref.getSyncMap(), startSourcePath, startDestPath );
				final String scanTimeFormatted = uiLog.getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Scannen";

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				appendLogData( context, uiLog.formatMaps( pref.getScanMode(), sourceMap, destMap, failMap ) );
				appendLogData( context, String.format( "Quelldateien: %d Stück und Zieldateien: %d Stück", stats[0], stats[1] ) );
				appendLogData( context, String.format( "Größe aller Quelldateien: %s | Größe aller Zieldateien: %s", uiLog.getReadableBytes( stats[2] ), uiLog.getReadableBytes( stats[3] ) ) );
				appendLogData( context, String.format( "Fehlerhafter Zugriff: %d", failMap.size() ) );

				startTime = System.nanoTime();
				final boolean success = model.syncFiles( context, result, pref.getSyncMap(), startSourcePath, startDestPath, false );
				final String syncTimeFormatted = uiLog.getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Synchronisieren";

				if( success ) {
					pref.saveLastScanTime();
					appendLogData( context, scanTimeFormatted );
					appendLogData( context, syncTimeFormatted );
					updateUIStatus( context, false, "Synchronisation erfolgreich!" );
					Debug.printDebug( "[Controller Helper]  Synchronization completed successfully for profile: %s", context.getJobName() );
				}else {
					updateUIStatus( context, false, "Synchronisation fehlgeschlagen!" );
					Debug.printDebug( "[Controller Helper Error] Synchronization routine failed for: %s", context.getJobName() );
				}
				Debug.printDebug( "[Controller Helper] Scan Time -> " + scanTimeFormatted );
				Debug.printDebug( "[Controller Helper]  Sync Time -> " + syncTimeFormatted );

			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Synchronisation abgebrochen." );
				Debug.printDebug( "[Controller Helper Error] Synchronization routine aborted due to interruption: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler: " + e.getMessage() );
				Debug.printDebug( "[Controller Helper Error] Synchronization routine failed: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}
			context.setRunning( false );
		} );

		worker.setDaemon( true );
		context.setActiveWorkerThread( worker );
		worker.start();
	}

	/**
	 * Runs localized multi-threaded incremental backups using task-isolated rules.
	 *
	 * @param context Target environment details providing task variables
	 */
	public void startBackup( final SyncJobContext context ) {
		if( context.isRunning() ) return;

		context.setRunning( true );
		context.setStatusMessage( "Backup gestartet. Analysiere geänderte Daten..." );
		context.clearLog();

		final Map<Path, FileAttributes> sourceMap = Model.createMap();
		final Map<Path, FileAttributes> destMap = Model.createMap();
		final Map<Path, FileAttributes> failMap = Model.createMap();
		final Preference pref = context.getPreference();
		final Model model = new Model( new Logger(), sourceMap, destMap );
		final Long[] stats = new Long[4];

		final Thread worker = new Thread( () -> {
			long startTime = System.nanoTime();
			try {
				final Path startDestPath = pref.getDestPaths().get( 0 );
				if( startDestPath == null || !Files.exists( startDestPath ) ) {
					updateUIStatus( context, false, "Kein Ziellaufwerk vorhanden" );
					return;
				}

				failMap.putAll( model.scanSyncFiles( pref.getSourcePaths(), pref.getDestPaths(), stats, pref.getScanMode(), pref.isSubDir(), pref.isTrashbin() ) );
				model.compareEqualsFiles();
				final String scanTimeFormatted = uiLog.getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Scannen";
				Debug.printDebug( "[Controller Helper]  sourceMap size = %d, destMap size = %d, failtures = %d", stats[0], stats[1], failMap.size() );

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				appendLogData( context, uiLog.formatMaps( pref.getScanMode(), sourceMap, destMap, failMap ) );
				appendLogData( context, String.format( "Quelldateien: %d Stück und Zieldateien: %d Stück", stats[0], stats[1] ) );
				appendLogData( context, String.format( "Größe aller Quelldateien: %s | Größe aller Zieldateien: %s", uiLog.getReadableBytes( stats[2] ), uiLog.getReadableBytes( stats[3] ) ) );
				appendLogData( context, String.format( "Fehlerhafter Zugriff: %d", failMap.size() ) );

				boolean success = false;
				String backupTimeFormatted = "";

				boolean delete = true;
				if( !pref.isAutoDel() ) {
					delete = dialogService.confirmUser( "Dateien löschen", "Löschen bestätigen?", "Alle gelöschten Dateien auch im Zielverzeichnis löschen?" );
				}

				if( pref.isAutoSync() || dialogService.confirmUser( "Dateien sichern", "Kopieren bestätigen?", "Alle neuen Dateien in  das Zielverzeichnis kopieren?" ) ) {
					startTime = System.nanoTime();
					success = model.backupFiles( delete, pref.isLogOn(), startDestPath, pref.isTrashbin(), pref.getTrashbinPath() );
					backupTimeFormatted = uiLog.getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Synchronisieren";
				}
//				TODO output at manual abort
				if( success ) {
					pref.saveLastScanTime();
					appendLogData( context, scanTimeFormatted );
					appendLogData( context, backupTimeFormatted );
					updateUIStatus( context, false, "Backup erfolgreich abgeschlossen!" );
					Debug.printDebug( "[Controller Helper]  Backup completed successfully for profile: %s", context.getJobName() );
				}else {
					updateUIStatus( context, false, "Backup fehlgeschlagen!" );
					Debug.printDebug( "[Controller Helper Error] Backup routine failed in: %s", context.getJobName() );
				}
				Debug.printDebug( "[Controller Helper] Scan Time -> " + scanTimeFormatted );
				Debug.printDebug( "[Controller Helper]  Backup Time -> " + backupTimeFormatted );
			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Backup-Vorgang abgebrochen." );
				Debug.printDebug( "[Controller Helper Error] Backup routine aborted due to interruption: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler während des Backups: " + e.getMessage() );
				Debug.printDebug( "[Controller Helper Error] Backup routine failed: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}
			context.setRunning( false );
		} );

		worker.setDaemon( true );
		context.setActiveWorkerThread( worker );
		worker.start();
	}

	/**
	 * Scans source paths asynchronously to list file checksum collisions.
	 *
	 * @param job Target environment details providing task variables
	 */
	public void startDuplicateScan( final SyncJobContext job ) {
		if( job.isRunning() ) return;

		job.setRunning( true );
		job.setStatusMessage( "Scanne nach Duplikaten..." );
		job.clearLog();

		final Map<Path, FileAttributes> sourceMap = Model.createMap();
		final Map<Path, FileAttributes> destMap = Model.createMap();
		final Long[] stats = new Long[4];

		final Preference pref = job.getPreference();
		final Model model = new Model( new Logger(), sourceMap, destMap );

		final Thread worker = new Thread( () -> {
			final long startTime = System.nanoTime();
			try {
				final Map<Path, FileAttributes> duplicateMap = model.scanDublicates( pref.getSourcePaths(), stats );
				final String scanTimeFormatted = uiLog.getEndTimeFormatted( System.nanoTime() - startTime );

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException( "Manual abort ..." );

				final List<SyncJobContext.FileRow> preparedRows = new ArrayList<>();
				if( duplicateMap != null && !duplicateMap.isEmpty() ) {
					for( final Map.Entry<Path, FileAttributes> entry : duplicateMap.entrySet() ) {
						preparedRows.add( new SyncJobContext.FileRow(
								entry.getKey(),
								entry.getValue(),
								uiLog.getReadableBytes( entry.getValue().getSize() ) ) );
					}
				}

				Platform.runLater( () -> {
					job.getDuplicateFiles().clear();
					job.getDuplicateFiles().addAll( preparedRows );
					job.setRunning( false );
					job.setStatusMessage( "Scan abgeschlossen. Duplikate insgesamt gefunden: " + job.getDuplicateFiles().size() + " (" + scanTimeFormatted + "s)" );
					Debug.printDebug( "[Controller Helper] duplicateMap size = %d, sourceMap size = %d, failtures = %d", duplicateMap.size(), stats[0], stats[1] );
					Debug.printDebug( "[Controller Helper] Scan Time -> " + scanTimeFormatted );
				} );
			}catch( final InterruptedException e ) {
				updateUIStatus( job, false, "Scan abgebrochen." );
				Debug.printDebug( "[Controller Helper Error] Duplicate scan aborted due to interruption: %s", e.getMessage() );
			}catch( final Exception e ) {
				updateUIStatus( job, false, "Fehler beim Duplikat Scan: " + e.getMessage() );
				Debug.printDebug( "[Controller Helper Error] Duplicat scan abord: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}
			job.setRunning( false );
		} );

		worker.setDaemon( true );
		job.setActiveWorkerThread( worker );
		worker.start();
	}

	/**
	 * Purges marked elements from active disks securely on a detached stack.
	 *
	 * @param context Target environment details providing task variables
	 */
	public void deleteSelectedDuplicates( final SyncJobContext context ) {
		final ArrayList<SyncJobContext.FileRow> toDelete = new ArrayList<>();
		for( final SyncJobContext.FileRow row : context.getDuplicateFiles() ) {
			if( row.isSelected() ) {
				toDelete.add( row );
			}
		}

		if( toDelete.isEmpty() ) {
			context.setStatusMessage( "Keine Dateien zum Löschen ausgewählt." );
			return;
		}
		if( !dialogService.confirmUser( "Duplikate entfernen", "Löschen bestätigen?", "Alle ausgewählten Dateien wirklich löschen?" ) ) return;

		context.setRunning( true );
		context.setStatusMessage( "Lösche ausgewählte Duplikate..." );

		final Thread worker = new Thread( () -> {
			int successCount = 0;
			try {
				for( final SyncJobContext.FileRow row : toDelete ) {
					if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
					if( Files.deleteIfExists( row.getFileSystemPath() ) ) {
						successCount++;
					}
				}
				final int finalSuccess = successCount;
				Platform.runLater( () -> {
					context.getDuplicateFiles().removeIf( SyncJobContext.FileRow::isSelected );
					context.setRunning( false );
					context.setStatusMessage( finalSuccess + " Duplikate erfolgreich gelöscht." );
					Debug.printDebug( "[Controller Helper] Duplicates successfully deleted: %d items.", finalSuccess );
				} );
			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Löschvorgang unterbrochen." );
				Debug.printDebug( "[Controller Helper Error] Duplicate deletion aborted: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler beim Löschen: " + e.getMessage() );
				Debug.printDebug( "[Controller Helper Error] Duplicate deletion failed: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}
		} );

		worker.setDaemon( true );
		context.setActiveWorkerThread( worker );
		worker.start();
	}

	/**
	 * Configures or evicts background host operating system daemon startup triggers.
	 * Adjusts system registry run hives on Windows environments or modifies cron execution
	 * tables on Linux platforms via secure subprocess execution abstractions.
	 *
	 * @param set Target flag to dictate whether to register or clear system integration entries.
	 * @return true if the underlying system environment sub-process sequences executed without exceptions.
	 */
	public boolean setOSAutostart( final boolean set ) {
		final String javaPath = System.getProperty( "sun.boot.library.path" );
		final String exePath = System.getProperty( "jpackage.app-path" );
		final String datei = System.getProperty( "sun.java.command" );
		final String fullPath = Paths.get( "" ).toAbsolutePath().toString() + System.getProperty( "file.separator" ) + datei;
		final String possibleOS = System.getProperty( "os.name" );
		String operatingSystem = "";
		if( possibleOS != null ) operatingSystem = possibleOS.toLowerCase( Locale.ROOT );
		final String flags = computeBootFlags();

		if( operatingSystem.contains( "win" ) ) {
			final String regCmd = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
			try {
				if( set ) {
					// No literal interior quote escapes required; ProcessBuilder insulates whitespaces
					final String dataPayload = ( exePath == null )
							? String.format( "%s\\javaw.exe -Xmx200m -jar %s %s %s", javaPath, datei, Main.BOOT_DELAY_LONG, flags )
							: String.format( "%s %s %s", exePath, Main.BOOT_DELAY_LONG, flags );

					final ProcessBuilder pb = new ProcessBuilder( "reg", "add", regCmd, "/v", "DataSync", "/t", "REG_SZ", "/d", dataPayload, "/f" );
					pb.start();
				}else {
					final ProcessBuilder pb = new ProcessBuilder( "reg", "delete", regCmd, "/v", "DataSync", "/f" );
					pb.start();
				}
			}catch( final IOException e ) {
				Debug.printError( "[Controller Helper Error] Set Windows registry autostart tracking hive failed: %s", e.getMessage() );
				Debug.printException( getClass(), e );
				return false;
			}
		}else if( operatingSystem.contains( "nix" ) || operatingSystem.contains( "aix" ) || operatingSystem.contains( "nux" ) ) {
			final String crontab = "crontab";
			try {
				if( set ) {
					final String cronPayload = ( exePath == null )
							? String.format( "@reboot %s/java -jar %s %s %s", javaPath, fullPath, Main.BOOT_DELAY_LONG, flags )
							: String.format( "@reboot %s %s %s", exePath, Main.BOOT_DELAY_LONG, flags );

					// Feed crontab structural inputs directly via process input stream pipelining
					final ProcessBuilder pb = new ProcessBuilder( crontab, "-" );
					final Process process = pb.start();

					try( BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter( process.getOutputStream(), StandardCharsets.UTF_8 ) ) ) {
						writer.write( cronPayload );
						writer.newLine(); // Unix crontab standard strictly requires a trailing newline character
					}
					process.waitFor();
				}else {
					final ProcessBuilder pb = new ProcessBuilder( crontab, "-r" );
					pb.start().waitFor();
				}
			}catch( final IOException | InterruptedException e ) {
				Debug.printError( "[Controller Helper Error] Set Unix crontab daemon automated launch failed: %s", e.getMessage() );
				Debug.printException( getClass(), e );
				if( e instanceof InterruptedException ) {
					Thread.currentThread().interrupt();
				}
				return false;
			}
		}
		return true;
	}

	private String computeBootFlags() {
		final StringBuilder stringBuilder = new StringBuilder();
		final PreferenceManager manager = PreferenceManager.getInstance();
		if( Main.isDebugToFile() ) {
			stringBuilder.append( " " + Main.DEBUG_TO_FILE_LONG );
		}
		if( manager.isCustomConfigDir() ) {
			stringBuilder.append( " " + Main.CONFIG_DIR_LONG );
			stringBuilder.append( " " + manager.getConfigPath().getParent().toString() );
		}
		return stringBuilder.toString();
	}

	private void updateUIStatus( final SyncJobContext context, final boolean running, final String message ) {
		Platform.runLater( () -> {
			context.setRunning( running );
			context.setStatusMessage( message );
		} );
	}

	private void appendLogData( final SyncJobContext context, final String line ) {
		Platform.runLater( () -> context.appendLog( line ) );
	}
}
