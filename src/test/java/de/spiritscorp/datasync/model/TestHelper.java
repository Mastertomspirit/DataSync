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

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import de.spiritscorp.datasync.model.FileAttributes;

/**
 * Test helper utility class providing test data generation and validation methods
 * for file synchronization and backup operations.
 *
 * <p>This class manages the creation of test file scenarios including:
 * <ul>
 * <li>Source and destination file maps with predefined attributes</li>
 * <li>Duplicate file detection scenarios</li>
 * <li>Synchronization state mapping</li>
 * <li>File timestamp and hash consistency</li>
 * <ul>
 * <p>
 *
 * @author Tom Spirit
 * @version 2.0
 */
class TestHelper {

	/** Sync files scenario definition provider */
	private final SyncFiles syncFiles;

	/** Predefined FileTime instances for consistent test data across scenarios */
	private final FileTime[] time = {
			FileTime.fromMillis( 1641335618384L ), // time[0]: 2022-01-04 20:33:38
			FileTime.fromMillis( 1641335619384L ), // time[1]: 2022-01-04 20:33:39
			FileTime.fromMillis( 1641335620384L ), // time[2]: 2022-01-04 20:33:40
			FileTime.fromMillis( 1641335621384L ), // time[3]: 2022-01-04 20:33:41
			FileTime.fromMillis( 1641335622384L ), // time[4]: 2022-01-04 20:33:42
			FileTime.fromMillis( 1641335623384L ), // time[5]: 2022-01-04 20:33:43
			FileTime.fromMillis( 1641335624384L ), // time[6]: 2022-01-04 20:33:44
			FileTime.fromMillis( 1641335625384L ), // time[7]: 2022-01-04 20:33:45
			FileTime.fromMillis( 1641335626384L ) // time[8]: 2022-01-04 20:33:46
	};

	/** List of source directory paths for test files */
	private final ArrayList<Path> paths;

	/** List of source file paths */
	private final ArrayList<Path> sourceList;

	/** List of destination file paths */
	private final ArrayList<Path> destList;

	/**
	 * Constructs a TestHelper instance with the specified root test path.
	 * Initializes source and destination file lists with predefined test file paths.
	 *
	 * @param path The root path for test file operations (e.g., HOME/.DataSyncTemp)
	 */
	TestHelper( Path path ) {
		syncFiles = new SyncFiles( path );
		paths = new ArrayList<>();
		sourceList = new ArrayList<>();
		destList = new ArrayList<>();
		paths.add( path.resolve( "source" ) );
		paths.add( path.resolve( "dest" ) );

		// Initialize source files: 7 files (4 in root + 3 in subdirectory)
		for( int i = 0; i < 4; i++ ) {
			sourceList.add( path.resolve( "source" ).resolve( "testFile" + i + ".txt" ) );
		}
		for( int i = 0; i < 3; i++ ) {
			sourceList.add( path.resolve( "source" ).resolve( "testDir" ).resolve( "testFile" + i + ".txt" ) );
		}

		// Initialize destination files: 8 files (3 in root + 5 in subdirectory)
		for( int i = 0; i < 3; i++ ) {
			destList.add( path.resolve( "dest" ).resolve( "testFile" + i + ".txt" ) );
		}
		for( int i = 0; i < 5; i++ ) {
			destList.add( path.resolve( "dest" ).resolve( "testDir" ).resolve( "testFile" + i + ".txt" ) );
		}
	}

	/**
	 * Populates the source map with predefined file attributes representing
	 * files available in the source directory.
	 *
	 * <p>Creates 7 test files with varying attributes including:
	 * <ul>
	 * <li>Different file sizes (12, 102, 120, 125 bytes)</li>
	 * <li>Different modification timestamps</li>
	 * <li>Different SHA-256 hashes (some duplicates for duplicate testing)</li>
	 * <ul>
	 * <p>
	 *
	 * @param map The map to populate with source file attributes
	 */
	void createSource( Map<Path, FileAttributes> map ) {
		// File 0: size=12, modTime=time[2], hash=1a60... (duplicate with file 4)
		map.put( sourceList.get( 0 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 0 ) ),
				fileTimeToString( time[0] ), time[1],
				fileTimeToString( time[2] ), time[2],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 1: size=102, modTime=time[3], hash=3330...
		map.put( sourceList.get( 1 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 1 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				102L,
				"3330b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 2: size=120, modTime=time[1], hash=44b9... (different from dest)
		map.put( sourceList.get( 2 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 2 ) ),
				fileTimeToString( time[2] ), time[3],
				fileTimeToString( time[1] ), time[1],
				120L,
				"44b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 3: size=125, modTime=time[2], hash=5520...
		map.put( sourceList.get( 3 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 3 ) ),
				fileTimeToString( time[3] ), time[1],
				fileTimeToString( time[2] ), time[2],
				125L,
				"5520b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 4: testDir/testFile0.txt, size=12, modTime=time[3], hash=1a60... (duplicate with file 0)
		map.put( sourceList.get( 4 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 4 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 5: testDir/testFile1.txt, size=12, modTime=time[3], hash=abb0... (duplicate with file 6)
		map.put( sourceList.get( 5 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 5 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 6: testDir/testFile2.txt, size=12, modTime=time[4], hash=abb0... (duplicate with file 5)
		map.put( sourceList.get( 6 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 6 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[4] ), time[4],
				12L,
				"abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );
	}

	/**
	 * Populates the destination map with predefined file attributes representing
	 * files available in the destination directory.
	 *
	 * <p>Creates 8 test files with varying attributes and partial overlaps
	 * with source files for testing synchronization scenarios.
	 * <p>
	 *
	 * @param map The map to populate with destination file attributes
	 */
	void createDest( Map<Path, FileAttributes> map ) {
		// File 0: size=12, modTime=time[2], hash=1a60... (matches source file 0)
		map.put( destList.get( 0 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 0 ) ),
				fileTimeToString( time[0] ), time[1],
				fileTimeToString( time[2] ), time[2],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 1: size=102, modTime=time[3], hash=3330... (matches source file 1)
		map.put( destList.get( 1 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 1 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				102L,
				"3330b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 2: size=120, modTime=time[1], hash=aaa9... (different from source file 2)
		map.put( destList.get( 2 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 2 ) ),
				fileTimeToString( time[2] ), time[3],
				fileTimeToString( time[1] ), time[1],
				120L,
				"aaa9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 3: testDir/testFile0.txt, size=12, modTime=time[3], hash=1a60... (matches source file 4)
		map.put( destList.get( 3 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 3 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 4: testDir/testFile1.txt, size=12, modTime=time[3], hash=1a60... (mismatched hash)
		map.put( destList.get( 4 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 4 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 5: testDir/testFile2.txt, size=12, modTime=time[3], hash=abb0... (matches source file 5)
		map.put( destList.get( 5 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 5 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 6: testDir/testFile3.txt, size=12, modTime=time[3], hash=ccc0...
		map.put( destList.get( 6 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 6 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// File 7: testDir/testFile4.txt, size=12, modTime=time[3], hash=ccc0... (duplicate of file 6)
		map.put( destList.get( 7 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 7 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );
	}

	/**
	 * Returns an array of file paths representing duplicate files in the source map.
	 * Files are considered duplicates if they share the same SHA-256 hash.
	 *
	 * @return Array of Path objects representing duplicate files
	 *         [sourceFile0, sourceFile4, sourceFile5, sourceFile6]
	 */
	Path[] getDuplicates() {
		return new Path[] {
				sourceList.get( 0 ), // Hash: 1a60... (same as file 4)
				sourceList.get( 4 ), // Hash: 1a60... (same as file 0)
				sourceList.get( 5 ), // Hash: abb0... (same as file 6)
				sourceList.get( 6 ) // Hash: abb0... (same as file 5)
		};
	}

	/**
	 * Returns an array of source file paths that match files in the destination.
	 * These files are considered "hits" and should not be copied.
	 *
	 * @return Array of matching source file paths
	 *         [sourceFile2, sourceFile3, sourceFile5, sourceFile6]
	 */
	Path[] getSourceHits() { return new Path[] {
			sourceList.get( 2 ),
			sourceList.get( 3 ),
			sourceList.get( 5 ),
			sourceList.get( 6 )
	}; }

	/**
	 * Returns an array of destination file paths that match files in the source.
	 * These files are considered "hits" and should be removed during sync.
	 *
	 * @return Array of matching destination file paths
	 *         [destFile2, destFile4, destFile5, destFile6, destFile7]
	 */
	Path[] getDestHits() { return new Path[] {
			destList.get( 2 ),
			destList.get( 4 ),
			destList.get( 5 ),
			destList.get( 6 ),
			destList.get( 7 )
	}; }

	/**
	 * Returns an array of source file paths that do NOT match any destination files.
	 * These files should be copied to the destination during sync.
	 *
	 * @return Array of non-matching source file paths
	 *         [sourceFile0, sourceFile1, sourceFile4]
	 */
	Path[] getSourceNotHit() { return new Path[] {
			sourceList.get( 0 ),
			sourceList.get( 1 ),
			sourceList.get( 4 )
	}; }

	/**
	 * Returns an array of destination file paths that do NOT match any source files.
	 * These files should be deleted from the destination during sync.
	 *
	 * @return Array of non-matching destination file paths
	 *         [destFile0, destFile1, destFile3]
	 */
	Path[] getDestNotHit() { return new Path[] {
			destList.get( 0 ),
			destList.get( 1 ),
			destList.get( 3 )
	}; }

	/**
	 * Converts a FileTime instance to a human-readable string representation
	 * using the system default timezone and the pattern "dd.MM.yyyy HH:mm:ss".
	 *
	 * @param fileTime The FileTime to convert
	 * @return Formatted time string (e.g., "04.01.2022 20:33:38")
	 */
	String fileTimeToString( FileTime fileTime ) {
		return fileTime.toInstant()
				.atZone( ZoneId.systemDefault() )
				.toLocalDateTime()
				.format( DateTimeFormatter.ofPattern( "dd.MM.yyyy  HH:mm:ss" ) );
	}

	/**
	 * Creates and returns the expected sync file lists for testing purposes.
	 * Delegates to the SyncFiles utility class to generate comprehensive
	 * synchronization scenarios.
	 *
	 * <p>Returns an ArrayList containing three maps:
	 * <ol>
	 * <li>Map 0: Files to copy from source to destination</li>
	 * <li>Map 1: Files to copy from destination to source</li>
	 * <li>Map 2: Files to delete</li>
	 * <ol>
	 * <p>
	 *
	 * @param sourceMap The source file attributes map (will be cleared and repopulated)
	 * @param destMap   The destination file attributes map (will be cleared and repopulated)
	 * @param syncMap   The synchronization state map (will be cleared and repopulated)
	 * @return ArrayList of expected sync maps for verification in tests
	 */
	public ArrayList<Map<Path, FileAttributes>> createSyncMap(
			Map<Path, FileAttributes> sourceMap,
			Map<Path, FileAttributes> destMap,
			Map<Path, FileAttributes> syncMap ) {
		return syncFiles.createSyncFiles( sourceMap, destMap, syncMap );
	}

	/**
	 * Populates the source and destination maps with backup-specific test data.
	 * This scenario tests the backup operation (copy from source to destination).
	 *
	 * @param sourceMap The source file attributes map to populate
	 * @param destMap   The destination file attributes map to populate
	 */
	public void createBackupFiles( Map<Path, FileAttributes> sourceMap, Map<Path, FileAttributes> destMap ) {
		// Source files for backup
		sourceMap.put( sourceList.get( 3 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 3 ) ),
				fileTimeToString( time[3] ), time[1],
				fileTimeToString( time[2] ), time[2],
				125L,
				"5520b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		sourceMap.put( sourceList.get( 5 ), new FileAttributes(
				paths.get( 0 ).relativize( sourceList.get( 5 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		// Destination files to delete during backup
		destMap.put( destList.get( 0 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 3 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		destMap.put( destList.get( 1 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 6 ) ),
				fileTimeToString( time[1] ), time[2],
				fileTimeToString( time[3] ), time[3],
				12L,
				"ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );

		destMap.put( destList.get( 2 ), new FileAttributes(
				paths.get( 1 ).relativize( destList.get( 7 ) ),
				fileTimeToString( time[3] ), time[3],
				fileTimeToString( time[4] ), time[3],
				12L,
				"ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2" ) );
	}
}
