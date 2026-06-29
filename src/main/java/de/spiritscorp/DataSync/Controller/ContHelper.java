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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;
import javafx.application.Platform;

/**
 * Orchestrates background thread processing for synchronization, backup,
 * and duplicate detection, utilizing task-isolated configuration states.
 *
 * @author Tom Spirit
 */
public class ContHelper {

	/**
	 * Executes bidirectional folder synchronization using task-bound preferences.
	 *
	 * @param context Target environment details providing task variables
	 */
	public void startSynchronize( SyncJobContext context ) {
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
				final Path startDestPath = pref.getStartDestPath();
				final Path startSourcePath = pref.getStartSourcePath();

				if( startDestPath == null || !Files.exists( startDestPath ) ) {
					updateUIStatus( context, false, "Kein Ziellaufwerk vorhanden" );
					return;
				}

				if( pref.getSourcePath().size() > 1 ) {
					updateUIStatus( context, false, "Die Synchronisierung funktioniert nur mit einem Quellordner!" );
					return;
				}

				failMap.putAll( model.scanSyncFiles( pref.getSourcePath(), pref.getDestPath(), stats, pref.getScanMode(), false, false ) );
				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				final ArrayList<Map<Path, FileAttributes>> result = model.getSyncFiles( pref.getSyncMap(), startSourcePath, startDestPath );
				final String scanTimeFormatted = getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Scannen";

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				appendLogData( context, formatMaps( pref.getScanMode(), sourceMap, destMap, failMap ) );
				appendLogData( context, String.format( "Quelldateien: %d Stück und Zieldateien: %d Stück", stats[0], stats[1] ) );
				appendLogData( context, String.format( "Größe aller Quelldateien: %s | Größe aller Zieldateien: %s", getReadableBytes( stats[2] ), getReadableBytes( stats[3] ) ) );
				appendLogData( context, String.format( "Fehlerhafter Zugriff: %d", failMap.size() ) );

				startTime = System.nanoTime();
				final boolean success = model.syncFiles( context, result, pref.getSyncMap(), startSourcePath, startDestPath, false );
				final String syncTimeFormatted = getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Synchronisieren";

				if( success ) {
					pref.saveLastScanTime();
					appendLogData( context, scanTimeFormatted );
					appendLogData( context, syncTimeFormatted );
					updateUIStatus( context, false, "Synchronisation erfolgreich!" );
					Debug.printDebug( "[Info] Synchronization completed successfully for profile: %s", context.getJobName() );
				}else {
					updateUIStatus( context, false, "Synchronisation fehlgeschlagen!" );
					Debug.printDebug( "[Error] Synchronization routine failed for: %s", context.getJobName() );
				}
				Debug.printDebug( scanTimeFormatted );
				Debug.printDebug( syncTimeFormatted );

			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Synchronisation abgebrochen." );
				Debug.printDebug( "[Error] Synchronization routine aborted due to interruption: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler: " + e.getMessage() );
				Debug.printDebug( "[Error] Synchronization routine failed: %s", e.getMessage() );
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
	public void startBackup( SyncJobContext context ) {
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
				final Path startDestPath = pref.getStartDestPath();
				if( startDestPath == null || !Files.exists( startDestPath ) ) {
					updateUIStatus( context, false, "Kein Ziellaufwerk vorhanden" );
					return;
				}

				failMap.putAll( model.scanSyncFiles( pref.getSourcePath(), pref.getDestPath(), stats, pref.getScanMode(), pref.isSubDir(), pref.isTrashbin() ) );
				model.getEqualsFiles();
				final String scanTimeFormatted = getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Scannen";
				Debug.printDebug( "[Info] sourceMap size = %d, destMap size = %d, failtures = %d", stats[0], stats[1], failMap.size() );

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				appendLogData( context, formatMaps( pref.getScanMode(), sourceMap, destMap, failMap ) );
				appendLogData( context, String.format( "Quelldateien: %d Stück und Zieldateien: %d Stück", stats[0], stats[1] ) );
				appendLogData( context, String.format( "Größe aller Quelldateien: %s | Größe aller Zieldateien: %s", getReadableBytes( stats[2] ), getReadableBytes( stats[3] ) ) );
				appendLogData( context, String.format( "Fehlerhafter Zugriff: %d", failMap.size() ) );

//	TODO Abfrage
				final int del = pref.isAutoDel() ? 0 : 1;

				startTime = System.nanoTime();
				final boolean success = model.backupFiles( del, pref.isLogOn(), startDestPath, pref.isTrashbin(), pref.getTrashbinPath() );
				final String backupTimeFormatted = getEndTimeFormatted( System.nanoTime() - startTime ) + " für das Synchronisieren";

				if( success ) {
					pref.saveLastScanTime();
					appendLogData( context, scanTimeFormatted );
					appendLogData( context, backupTimeFormatted );
					updateUIStatus( context, false, "Backup erfolgreich abgeschlossen!" );
					Debug.printDebug( "[Info] Backup completed successfully for profile: %s", context.getJobName() );
				}else {
					updateUIStatus( context, false, "Backup fehlgeschlagen!" );
					Debug.printDebug( "[Error] Backup routine failed in: %s", context.getJobName() );
				}
				Debug.printDebug( scanTimeFormatted );
				Debug.printDebug( backupTimeFormatted );
			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Backup-Vorgang abgebrochen." );
				Debug.printDebug( "[Error] Backup routine aborted due to interruption: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler während des Backups: " + e.getMessage() );
				Debug.printDebug( "[Error] Backup routine failed: %s", e.getMessage() );
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
	 * @param context Target environment details providing task variables
	 */
	public void startDuplicateScan( SyncJobContext context ) {
		if( context.isRunning() ) return;

		context.setRunning( true );
		context.setStatusMessage( "Scanne nach Duplikaten..." );
		context.clearLog();

		final Preference pref = context.getPreference();
		final Model model = new Model( new Logger(), Model.createMap(), Model.createMap() );

		final Thread worker = new Thread( () -> {
			final long startTime = System.nanoTime();
			try {
				final Map<Path, FileAttributes> scanResult = model.scanDublicates( pref.getSourcePath() );
				final String scanTimeFormatted = getEndTimeFormatted( System.nanoTime() - startTime );

				if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();

				Platform.runLater( () -> {
					context.getDuplicateFiles().clear();
					if( scanResult != null ) {
						for( final Map.Entry<Path, FileAttributes> entry : scanResult.entrySet() ) {
							context.getDuplicateFiles().add( new SyncJobContext.FileRow(
									entry.getKey(),
									entry.getValue(),
									getReadableBytes( entry.getValue().getSize() ) ) );
						}
					}
					context.setRunning( false );
					context.setStatusMessage( "Scan abgeschlossen. Duplikate gefunden: " + ( context.getDuplicateFiles().size() / 2 ) + " (" + scanTimeFormatted + "s)" );
					Debug.printDebug( scanTimeFormatted + "First runlater" );
				} );
//	TODO	testen
				Debug.printDebug( scanTimeFormatted + "Second runlater" );
			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Scan abgebrochen." );
				Debug.printDebug( "[Error] Duplicate scan aborted due to interruption: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler beim Duplikat Scan: " + e.getMessage() );
				Debug.printDebug( "[Error] Duplicat scan abord: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}
			context.setRunning( false );
		} );

		worker.setDaemon( true );
		context.setActiveWorkerThread( worker );
		worker.start();
	}

	/**
	 * Purges marked elements from active disks securely on a detached stack.
	 *
	 * @param context Target environment details providing task variables
	 */
	public void deleteSelectedDuplicates( SyncJobContext context ) {
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
					Debug.printDebug( "[Info] Duplicates successfully deleted: %d items.", finalSuccess );
				} );
			}catch( final InterruptedException e ) {
				updateUIStatus( context, false, "Löschvorgang unterbrochen." );
				Debug.printDebug( "[Error] Duplicate deletion aborted: %s", e.getMessage() );
				Debug.printException( this.getClass(), e );
			}catch( final Exception e ) {
				updateUIStatus( context, false, "Fehler beim Löschen: " + e.getMessage() );
				Debug.printDebug( "[Error] Duplicate deletion failed: %s", e.getMessage() );
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
	public boolean setOSAutostart( boolean set ) {
		final String javaPath = System.getProperty( "sun.boot.library.path" );
		final String exePath = System.getProperty( "jpackage.app-path" );
		final String datei = System.getProperty( "sun.java.command" );
		final String fullPath = Paths.get( "" ).toAbsolutePath().toString() + System.getProperty( "file.separator" ) + datei;
		final String possibleOS = System.getProperty( "os.name" );
		String os = "";
		if( possibleOS != null ) os = possibleOS.toLowerCase( Locale.ROOT );

		if( os.contains( "win" ) ) {
			final String regCmd = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
			try {
				if( set ) {
					// No literal interior quote escapes required; ProcessBuilder insulates whitespaces
					final String dataPayload = ( exePath == null )
							? javaPath + "\\javaw.exe -Xmx200m -jar " + datei + " firstStart"
							: exePath + " firstStart";

					final ProcessBuilder pb = new ProcessBuilder( "reg", "add", regCmd, "/v", "DataSync", "/t", "REG_SZ", "/d", dataPayload, "/f" );
					pb.start();
				}else {
					final ProcessBuilder pb = new ProcessBuilder( "reg", "delete", regCmd, "/v", "DataSync", "/f" );
					pb.start();
				}
			}catch( final IOException e ) {
				Debug.printError( "[Error] Set Windows registry autostart tracking hive failed: %s", e.getMessage() );
				Debug.printException( getClass(), e );
				return false;
			}
		}else if( os.contains( "nix" ) || os.contains( "aix" ) || os.contains( "nux" ) ) {
			try {
				if( set ) {
					final String cronPayload = ( exePath == null )
							? String.format( "@reboot %s/java -jar \"%s\" firstStart", javaPath, fullPath )
							: String.format( "@reboot %s firstStart", exePath );

					// Feed crontab structural inputs directly via process input stream pipelining
					final ProcessBuilder pb = new ProcessBuilder( "crontab", "-" );
					final Process process = pb.start();

					try( BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter( process.getOutputStream(), StandardCharsets.UTF_8 ) ) ) {
						writer.write( cronPayload );
						writer.newLine(); // Unix crontab standard strictly requires a trailing newline character
					}
					process.waitFor();
				}else {
					final ProcessBuilder pb = new ProcessBuilder( "crontab", "-r" );
					pb.start().waitFor();
				}
			}catch( final IOException | InterruptedException e ) {
				Debug.printError( "[Error] Set Unix crontab daemon automated launch failed: %s", e.getMessage() );
				Debug.printException( getClass(), e );
				if( e instanceof InterruptedException ) {
					Thread.currentThread().interrupt();
				}
				return false;
			}
		}
		return true;
	}

	private void updateUIStatus( SyncJobContext context, boolean running, String message ) {
		Platform.runLater( () -> {
			context.setRunning( running );
			context.setStatusMessage( message );
		} );
	}

	private void appendLogData( SyncJobContext context, String line ) {
		Platform.runLater( () -> context.appendLog( line ) );
	}

	/**
	 * Give back a formatted string for visualizing at the textArea
	 *
	 * @param deepScan Witch ScanType
	 * @return <b>String</b> The formatted string
	 */
	private String formatMaps( ScanType deepScan, Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap, Map<Path, FileAttributes> failMap ) {
		final String line = System.lineSeparator();
		final StringBuilder sb = new StringBuilder();
		final int displayLimit = 10000;
		if( deepScan == ScanType.SYNCHRONIZE ) {
			sb.append( "Scan abgeschlossen!" + line );
			sb.append( "----------------------" + line );
		}else {
			sb.append( "Scan abgeschlossen!" + line );
			sb.append( "Zu kopierende Dateien:" + line );
			sb.append( "----------------------" + line );
			int limit = 0;
			if( sourceMap != null ) {
				for( final Map.Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
					final FileAttributes value = entry.getValue();
					sb.append( value.getFileName() + " , " +
							getReadableBytes( value.getSize() ) + " , " +
							value.getModTimeString() + " , " +
							value.getCreateTimeString() + " , " +
							value.getFileHash() + "  " +
							"   " + entry.getKey().toString() +
							line );
					limit++;
					if( limit > ( displayLimit / 2 ) ) break;
				}
			}
			limit = 0;
			sb.append( line );
			sb.append( "Zu löschende Dateien:" + line );
			sb.append( "---------------------" + line );
			if( destMap != null ) {
				for( final Map.Entry<Path, FileAttributes> entry : destMap.entrySet() ) {
					final FileAttributes value = entry.getValue();
					sb.append( value.getFileName() + " , " +
							getReadableBytes( value.getSize() ) + " , " +
							value.getModTimeString() + " , " +
							value.getCreateTimeString() + " , " +
							value.getFileHash() + "  " +
							"   " + entry.getKey().toString() +
							line );
					limit++;
					if( limit > ( displayLimit / 2 ) ) break;
				}
			}
			if( failMap != null && !failMap.isEmpty() ) {
				limit = 0;
				sb.append( line );
				sb.append( "Fehlerhafter Zugriff:" + line );
				sb.append( "---------------------" + line );
				for( final Map.Entry<Path, FileAttributes> entry : failMap.entrySet() ) {
					final FileAttributes value = entry.getValue();
					sb.append( value.getFileName() + " , " +
							getReadableBytes( value.getSize() ) + " , " +
							value.getModTimeString() + " , " +
							value.getCreateTimeString() + " , " +
							value.getFileHash() + "  " +
							"   " + entry.getKey().toString() +
							line );
					limit++;
					if( limit > ( displayLimit / 2 ) ) break;
				}
			}
		}
		return sb.toString();
	}

	private String getEndTimeFormatted( long endTimeNano ) {
		final double endTimeSec = ( (double) endTimeNano ) / 1000000000;
		if( endTimeSec >= 7200 )
			return String.format( "%d Stunden %d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / 3600, ( (int) endTimeSec ) % 60, endTimeSec % 60 );
		else if( endTimeSec >= 3600 )
			return String.format( "%d Stunde %d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / 3600, ( (int) endTimeSec ) % 60, endTimeSec % 60 );
		else if( endTimeSec >= 60 )
			return String.format( "%d Minuten %.3f Sekunden Laufzeit", ( (int) endTimeSec ) / 60, endTimeSec % 60 );
		else
			return String.format( "%.3f Sekunden Laufzeit", endTimeSec );
	}

	private String getReadableBytes( long bytes ) {
		if( bytes > 1073741824 )
			return String.format( "%.3f GiB", bytes / 1048576.0 / 1024.0 );
		else if( bytes > 1048576 )
			return ( bytes / 1048576 ) + " MiB";
		else if( bytes > 1024 )
			return ( bytes / 1024 ) + " KiB";
		else if( bytes > 1 )
			return bytes + " bytes";
		else
			return bytes + " byte";
	}
}
