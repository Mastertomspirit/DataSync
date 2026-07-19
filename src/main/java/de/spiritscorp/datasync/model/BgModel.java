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
import java.util.List;
import java.util.Map;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;

public class BgModel {

	private final Preference pref;
	private final FileAnalyzer analyzer;
	private final FileHandler handler;
	private final Map<Path, FileAttributes> sourceMap;
	private final Map<Path, FileAttributes> destMap;

	public BgModel( final Preference pref, final Logger logger, final Map<Path, FileAttributes> sourceMap, final Map<Path, FileAttributes> destMap ) {
		this.pref = pref;
		this.sourceMap = sourceMap;
		this.destMap = destMap;
		analyzer = new FileAnalyzer();
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
		final Path startSourcePath = pref.getSourcePaths().get( 0 );
		final Path startDestPath = pref.getDestPaths().get( 0 );
		final Path trashbinPath = pref.getTrashbinPath();
		final boolean trashbin = pref.isTrashbin();
		final boolean autoDel = pref.isAutoDel();
		final ScanType scanType = pref.getScanMode();
		final long lastScanDuration = System.currentTimeMillis() - pref.getLastScanTime();
		if( lastScanDuration > pref.getBgTime().getTime() && Files.exists( startDestPath ) ) {
			Debug.printDebug( "[BgModel] Time since last scan: %s", formatDuration( lastScanDuration ) );
			if( scanType == ScanType.SYNCHRONIZE ) {
				Debug.printDebug( "[Bg Model] BgJob running" );
				Debug.printDebug( "[Bg Model] List start" );
				final Thread thread1 = new Thread( () -> handler.listFiles( pref.getSourcePaths(), sourceMap, scanType, false ) );
				final Thread thread2 = new Thread( () -> handler.listFiles( pref.getDestPaths(), destMap, scanType, false ) );
				thread1.start();
				thread2.start();
				try {
					thread1.join();
					thread2.join();
				}catch( InterruptedException _ ) {
					Thread.currentThread().interrupt();
				}
				Debug.printDebug( "[Bg Model] List ready" );
				Debug.printDebug( "[Bg Model] SourceMap size -> %s | DestMap size -> %s", sourceMap.size(), destMap.size() );
				Debug.printDebug( "[Bg Model] Synchronization start" );
				final List<Map<Path, FileAttributes>> result = analyzer.getSyncFiles( sourceMap, destMap, startSourcePath, startDestPath, syncMap );
				Debug.printDebug( "[Bg Model] Synchronization list ready" );

				Debug.printDebug( "[Bg Model] Process files start" );
				if( !result.get( 0 ).isEmpty() ) handler.copyFiles( result.get( 0 ), logOn, startDestPath );
				if( !result.get( 1 ).isEmpty() ) handler.copyFiles( result.get( 1 ), logOn, startSourcePath );
				if( !result.get( 2 ).isEmpty() ) handler.deleteFiles( result.get( 2 ), logOn, false, null );

				sourceMap.clear();
				destMap.clear();
				syncMap.clear();

				final Map<Path, FileAttributes> tempMap = Model.createMap();
				handler.listFiles( pref.getSourcePaths(), tempMap, scanType, false );
				for( final Map.Entry<Path, FileAttributes> entry : tempMap.entrySet() ) {
					syncMap.put( entry.getValue().getRelativeFilePath(), entry.getValue() );
				}
				pref.writeSyncMap();
				pref.saveLastScanTime();
				Debug.printDebug( "[Bg Model] Files successfully processed" );
				Debug.printDebug( "[Bg Model] BgJob finish" );
				return result.get( 0 ).isEmpty() && result.get( 1 ).isEmpty() && result.get( 2 ).isEmpty();
			}else if( scanType == ScanType.DEEP_SCAN || scanType == ScanType.FLAT_SCAN ) {
				Debug.printDebug( "[Bg Model] BgJob running" );
				Debug.printDebug( "[Bg Model] List start" );
				final Thread thread1 = new Thread( () -> handler.listFiles( pref.getSourcePaths(), sourceMap, scanType, pref.isSubDir() ) );
				final Thread thread2 = new Thread( () -> handler.listFiles( pref.getDestPaths(), destMap, scanType, pref.isSubDir() ) );
				thread1.start();
				thread2.start();
				try {
					thread1.join();
					thread2.join();
				}catch( InterruptedException _ ) {
					Thread.currentThread().interrupt();
				}
				Debug.printDebug( "[Bg Model] List ready" );
				Debug.printDebug( "[Bg Model] SourceMap size -> %s | DestMap size -> %s", sourceMap.size(), destMap.size() );

				Debug.printDebug( "[Bg Model] Equals Files start" );
				analyzer.equalsFiles( sourceMap, destMap );
				Debug.printDebug( "[Bg Model] Equals Files ready" );

				Debug.printDebug( "[Bg Model] Process files start" );
				if( autoDel && !destMap.isEmpty() ) handler.deleteFiles( destMap, logOn, trashbin, trashbinPath );
				if( !sourceMap.isEmpty() ) handler.copyFiles( sourceMap, logOn, startDestPath );
				pref.saveLastScanTime();
				Debug.printDebug( "[Bg Model] Files successfully processed" );
				Debug.printDebug( "[Bg Model] BgJob finish" );
				return sourceMap.isEmpty() && destMap.isEmpty();
			}else {
				Debug.printDebug( "[Bg Model] No valid background job" );
			}
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
		if( durationMs < 1_000 ) { return durationMs + "ms"; }

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
