package de.spiritscorp.datasync.model;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;

public class BgModel {

	private final Preference pref;
	private final FileHandler handler;
	private final Map<Path, FileAttributes> sourceMap, destMap;

	public BgModel( Preference pref, Logger logger, Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap ) {
		this.pref = pref;
		this.sourceMap = sourceMap;
		this.destMap = destMap;
		handler = new FileHandler( logger );
	}

	/**
	 * List all files, equals them and make the prefer sync in the background
	 *
	 * @return <b>boolean</b> <br>
	 *         true if the process ran and both maps are empty
	 */
	public boolean runBgJob() {
		final boolean logOn = pref.isLogOn();
		final Map<Path, FileAttributes> syncMap = pref.getSyncMap();
		final Path startSourcePath = pref.getStartSourcePath();
		final Path startDestPath = pref.getStartDestPath();
		final Path trashbinPath = pref.getTrashbinPath();
		final boolean trashbin = pref.isTrashbin();
		final boolean autoBgDel = pref.isAutoBgDel();
		final ScanType scanType = pref.getScanMode();

		Debug.printDebug( "[BgModel] Time since last scan: %s", formatDuration( System.currentTimeMillis() - pref.getLastScanTime() ) );
		if( scanType == ScanType.SYNCHRONIZE ) {
			if( Files.exists( pref.getStartDestPath() ) ) {
				if( System.currentTimeMillis() - pref.getLastScanTime() > pref.getBgTime().getTime() ) {
					Debug.printDebug( "[BgModel] BgJob running" );
					Debug.printDebug( "[BgModel] List start" );
					final Thread t1 = new Thread( () -> handler.listFiles( pref.getSourcePath(), sourceMap, scanType, false ) );
					final Thread t2 = new Thread( () -> handler.listFiles( pref.getDestPath(), destMap, scanType, false ) );
					t1.start();
					t2.start();
					try {
						t1.join();
						t2.join();
					}catch( final InterruptedException e ) {
						Debug.printException( this.getClass(), e );
					}
					Debug.printDebug( "[BgModel] List ready" );
					Debug.printDebug( "[BgModel] SourceMap size -> %s | DestMap size -> %s", sourceMap.size(), destMap.size() );
					Debug.printDebug( "[BgModel] Synchronization start" );
//					if (syncMap.isEmpty()) {
//						final Map<Path, FileAttributes> tempSyncMap = Model.createMap();
//						handler.listFiles(pref.getSourcePath(), tempSyncMap, ScanType.SYNCHRONIZE, false);
//						for (final Map.Entry<Path, FileAttributes> entry : tempSyncMap.entrySet()) {
//							syncMap.put(entry.getValue().getRelativeFilePath(), entry.getValue());
//						}
//					}
					final ArrayList<Map<Path, FileAttributes>> result = handler.getSyncFiles( sourceMap, destMap, startSourcePath, startDestPath, syncMap );
					Debug.printDebug( "[BgModel] Synchronization list ready" );

					Debug.printDebug( "[BgModel] Process files start" );
					if( !result.get( 0 ).isEmpty() ) handler.copyFiles( result.get( 0 ), false, startDestPath );
					if( !result.get( 1 ).isEmpty() ) handler.copyFiles( result.get( 1 ), false, startSourcePath );
					if( !result.get( 2 ).isEmpty() ) handler.deleteFiles( result.get( 2 ), false, false, null );

					sourceMap.clear();
					destMap.clear();
					syncMap.clear();

					final Map<Path, FileAttributes> tempMap = Model.createMap();
					handler.listFiles( pref.getSourcePath(), tempMap, scanType, false );
					for( final Map.Entry<Path, FileAttributes> entry : tempMap.entrySet() ) {
						syncMap.put( entry.getValue().getRelativeFilePath(), entry.getValue() );
					}
					pref.writeSyncMap();
					pref.saveLastScanTime();
					Debug.printDebug( "[BgModel] Files successfully processed" );
					Debug.printDebug( "[BgModel] BgJob finish" );
					return result.get( 0 ).isEmpty() && result.get( 1 ).isEmpty() && result.get( 2 ).isEmpty();
				}
			}
		}else if( scanType == ScanType.DEEP_SCAN || scanType == ScanType.FLAT_SCAN ) {
			if( Files.exists( pref.getStartDestPath() ) ) {
				if( System.currentTimeMillis() - pref.getLastScanTime() > pref.getBgTime().getTime() ) {
					Debug.printDebug( "[BgModel] BgJob running" );
					Debug.printDebug( "[BgModel] List start" );
					final Thread t1 = new Thread( () -> handler.listFiles( pref.getSourcePath(), sourceMap, scanType, pref.isSubDir() ) );
					final Thread t2 = new Thread( () -> handler.listFiles( pref.getDestPath(), destMap, scanType, pref.isSubDir() ) );
					t1.start();
					t2.start();
					try {
						t1.join();
						t2.join();
					}catch( final InterruptedException e ) {
						Debug.printException( this.getClass(), e );
					}
					Debug.printDebug( "[BgModel] List ready" );
					Debug.printDebug( "[BgModel] SourceMap size -> %s | DestMap size -> %s", sourceMap.size(), destMap.size() );

					Debug.printDebug( "[BgModel] Equals Files start" );
					handler.equalsFiles( sourceMap, destMap );
					Debug.printDebug( "[BgModel] Equals Files ready" );

					Debug.printDebug( "[BgModel] Process files start" );
					if( autoBgDel && !destMap.isEmpty() ) handler.deleteFiles( destMap, logOn, trashbin, trashbinPath );
					if( !sourceMap.isEmpty() ) handler.copyFiles( sourceMap, logOn, startDestPath );
					pref.saveLastScanTime();
					Debug.printDebug( "[BgModel] Files successfully processed" );
					Debug.printDebug( "[BgModel] BgJob finish" );
					return sourceMap.isEmpty() && destMap.isEmpty();
				}
			}
		}else {
			Debug.printDebug( "[BgModel] No valid background job" );
		}
		return false;
	}

	/**
	 * Formats a millisecond duration into a human-readable string using the largest necessary time units.
	 *
	 * @param durationMs The active delta time measured in milliseconds.
	 * @return A concisely formatted string (e.g., "2d 4h 15m", "45m 12s", or "8s").
	 */
	private static String formatDuration( final long durationMs ) {
		if( durationMs < 1000 ) { return durationMs + "ms"; }

		final long totalSeconds = durationMs / 1_000;
		final long seconds = totalSeconds % 60;
		final long totalMinutes = totalSeconds / 60;
		final long minutes = totalMinutes % 60;
		final long totalHours = totalMinutes / 60;
		final long hours = totalHours % 24;
		final long days = totalHours / 24;

		final StringBuilder builder = new StringBuilder();

		if( days > 0 ) {
			builder.append( days ).append( "d " );
		}
		if( hours > 0 ) {
			builder.append( hours ).append( "h " );
		}
		if( minutes > 0 ) {
			builder.append( minutes ).append( "m " );
		}
		// Seconds are always shown if there are no days/hours, or if there is a remaining second balance
		if( seconds > 0 || builder.isEmpty() ) {
			builder.append( seconds ).append( 's' );
		}

		return builder.toString().trim();
	}
}
