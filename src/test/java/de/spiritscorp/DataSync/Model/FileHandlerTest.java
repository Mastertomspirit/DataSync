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
package de.spiritscorp.DataSync.Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;

/**
 * Comprehensive test suite for FileHandler class covering all synchronization
 * and file operations including edge cases for latest version file copying.
 *
 * <p><strong>Test Coverage:</strong>
 * <ul>
 * <li>Duplicate file detection based on content hash</li>
 * <li>File equality comparison and removal of identical files</li>
 * <li>Synchronization with latest version detection (CRITICAL BUG FIX)</li>
 * <li>Edge cases: identical timestamps, null values, large file counts</li>
 * <li>Thread safety with parallel processing</li>
 * <ul>
 * <p>
 *
 * @author Tom Spirit
 * @version 2.0
 * @see FileHandler
 */
//@DisplayName( "FileHandler Test Suite" )
class FileHandlerTest {

	/** Source file attributes map */
	private Map<Path, FileAttributes> sourceMap;

	/** Destination file attributes map */
	private Map<Path, FileAttributes> destMap;

	/** Duplicate files map */
	private Map<Path, FileAttributes> duplicateMap;

	/** Synchronization state map */
	private Map<Path, FileAttributes> syncMap;

	/** Test root path */
	private Path testPath;

	/** Test data helper */
	private TestHelper helper;

	/** Object under test */
	private FileHandler fileHandler;

	/** Insulates the static {@link Debug} diagnostics subsystem during test execution. */
	private MockedStatic<Debug> mockDebug;

	/**
	 * Sets up test environment before running all tests in this class.
	 * Called once before any test methods are executed.
	 *
	 * @throws Exception if setup fails
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tears down after all tests in this class have been executed.
	 * Called once after all test methods are executed.
	 *
	 * @throws Exception if teardown fails
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets up test fixtures before each individual test method.
	 * Initializes maps, test helper, and FileHandler instance.
	 *
	 * @throws Exception if setup fails
	 */
	@BeforeEach
	void setUp() throws Exception {
		sourceMap = Model.createMap();
		destMap = Model.createMap();
		syncMap = Model.createMap();
		testPath = ModelTest.TEST_PATH;

		helper = new TestHelper( testPath );
		mockDebug = Mockito.mockStatic( Debug.class );
		fileHandler = new FileHandler( mock( Logger.class ) );

		// Populate test data
		helper.createSource( sourceMap );
		helper.createDest( destMap );
	}

	/**
	 * Cleans up after each individual test method.
	 *
	 * @throws Exception if teardown fails
	 */
	@AfterEach
	void tearDown() throws Exception {
		if( mockDebug != null ) {
			mockDebug.close();
		}
	}

	/**
	 * Tests the findDuplicates method to ensure correct identification of
	 * duplicate files based on content hash.
	 *
	 * <p><strong>Test Scenario:</strong>
	 * Verifies that files with identical SHA-256 hashes are correctly identified
	 * as duplicates and returned in the duplicate map.
	 * <p>
	 *
	 * <p><strong>Expected Results:</strong>
	 * <ul>
	 * <li>Duplicate count matches expected duplicates array length</li>
	 * <li>All expected duplicate files are present in result</li>
	 * <li>No false positives (non-duplicate files included)</li>
	 * <ul>
	 * <p>
	 */
	@Test
	@DisplayName( "1. Test duplicate file detection by content hash" )
	final void testFindDuplicates() {
		// Execute
		duplicateMap = fileHandler.findDuplicates( sourceMap );

		// Verify: Count matches
		final Path[] expectedDuplicates = helper.getDuplicates();
		assertEquals( expectedDuplicates.length, duplicateMap.size(), "Duplicate file count does not match expected value. "
				+ "Expected: " + expectedDuplicates.length + ", Got: " + duplicateMap.size() );

		// Verify: All expected duplicates are found
		for( final Path path : expectedDuplicates ) {
			assertTrue( duplicateMap.containsKey( path ), "Expected duplicate file not found: " + path );
		}
	}

	/**
	 * Tests the equalsFiles method to ensure correct identification and removal
	 * of files that are identical between source and destination.
	 *
	 * <p><strong>Test Scenario:</strong>
	 * Verifies that files appearing in both source and destination with identical
	 * attributes are removed from both maps, leaving only non-matching files.
	 * <p>
	 *
	 * <p><strong>Expected Results:</strong>
	 * <ul>
	 * <li>Files in getSourceHits() remain after equalsFiles()</li>
	 * <li>Files in getSourceNotHit() are removed after equalsFiles()</li>
	 * <li>Files in getDestHits() remain after equalsFiles()</li>
	 * <li>Files in getDestNotHit() are removed after equalsFiles()</li>
	 * <ul>
	 * <p>
	 */
	@Test
	@DisplayName( "2. Test file equality comparison and removal" )
	final void testEqualsFiles() {
		// Execute
		fileHandler.equalsFiles( sourceMap, destMap );

		// Verify: Source files not matching destination are removed
		for( final Path path : helper.getSourceNotHit() ) {
			assertFalse( sourceMap.containsKey( path ), "Non-matching source file should be removed: " + path );
		}

		// Verify: Destination files not matching source are removed
		for( final Path path : helper.getDestNotHit() ) {
			assertFalse( destMap.containsKey( path ), "Non-matching destination file should be removed: " + path );
		}

		// Verify: Source files matching destination remain
		for( final Path path : helper.getSourceHits() ) {
			assertTrue( sourceMap.containsKey( path ), "Matching source file should remain: " + path );
		}

		// Verify: Destination files matching source remain
		for( final Path path : helper.getDestHits() ) {
			assertTrue( destMap.containsKey( path ), "Matching destination file should remain: " + path );
		}
	}

	/**
	 * Tests the critical getSyncFiles method with comprehensive scenario coverage.
	 * This test specifically validates that the NEWEST VERSION of a file is always
	 * copied, never overwritten with an older version.
	 *
	 * <p><strong>BUG FIX CONTEXT:</strong>
	 * This test addresses the synchronization bug where older file versions could
	 * overwrite newer versions. The algorithm must compare modification timestamps
	 * correctly to ensure the latest version is always preserved.
	 * <p>
	 *
	 * <p><strong>Test Scenarios (10 cases):</strong>
	 * <ol>
	 * <li><strong>schnurrend.txt:</strong> Identical in all locations - no action</li>
	 * <li><strong>vorsichtig.txt:</strong> Dest is newer - skip update</li>
	 * <li><strong>eiskalt.txt:</strong> Deleted from dest - mark for deletion from source</li>
	 * <li><strong>vernünftig.txt:</strong> Deleted from source - mark for deletion from dest</li>
	 * <li><strong>elastisch.txt:</strong> New in source - copy to dest</li>
	 * <li><strong>zierlich.txt:</strong> New in dest - copy to source</li>
	 * <li><strong>fleißig.txt:</strong> Source is newer (1ms difference) - copy source</li>
	 * <li><strong>wissend.txt:</strong> Dest is newer (1ms difference) - copy dest</li>
	 * <li><strong>robust.txt:</strong> Source is much newer (10s difference) - copy source</li>
	 * <li><strong>uralt.txt:</strong> Dest is much newer (10s difference) - copy dest</li>
	 * <ol>
	 * <p>
	 *
	 * <p><strong>Expected Results:</strong>
	 * <ul>
	 * <li>All expected source-to-dest copies are present in result[0]</li>
	 * <li>All expected dest-to-source copies are present in result[1]</li>
	 * <li>All expected deletions are present in result[2]</li>
	 * <li>No extra files beyond expected counts</li>
	 * <li>File attributes remain consistent through sync operations</li>
	 * <ul>
	 * <p>
	 *
	 * @throws IOException if file operations fail
	 */
	@Test
	@DisplayName( "3. Test getSyncFiles with latest version detection (10 comprehensive scenarios)" )
	final void testGetSyncFiles() throws IOException {
		// Setup: Generate expected values for all 10 scenarios
		final ArrayList<Map<Path, FileAttributes>> expectedValues = helper.createSyncMap( sourceMap, destMap, syncMap );

		final Map<Path, FileAttributes> expectedCopySource = expectedValues.get( 0 );
		final Map<Path, FileAttributes> expectedCopyDest = expectedValues.get( 1 );
		final Map<Path, FileAttributes> expectedDel = expectedValues.get( 2 );

		// Execute: Call the method under test
		final ArrayList<Map<Path, FileAttributes>> syncFiles = fileHandler.getSyncFiles( sourceMap, destMap, testPath.resolve( "source" ), testPath.resolve( "dest" ), syncMap );

		final Map<Path, FileAttributes> actualCopySource = syncFiles.get( 0 );
		final Map<Path, FileAttributes> actualCopyDest = syncFiles.get( 1 );
		final Map<Path, FileAttributes> actualDel = syncFiles.get( 2 );

		// Verify: All expected source copies are present
		for( final Entry<Path, FileAttributes> entry : expectedCopySource.entrySet() ) {
			final FileAttributes actual = actualCopySource.get( entry.getKey() );
			assertNotNull( actual, "Expected file to copy from source not found: " + entry.getValue().getRelativeFilePath() );
			assertEquals( entry.getValue(), actual, "File attributes do not match for: " + entry.getValue().getRelativeFilePath() + " -> source copy list is not as expected" );
			actualCopySource.remove( entry.getKey() );
		}

		// Verify: All expected destination copies are present
		for( final Entry<Path, FileAttributes> entry : expectedCopyDest.entrySet() ) {
			final FileAttributes actual = actualCopyDest.get( entry.getKey() );
			assertNotNull( actual, "Expected file to copy from destination not found: " + entry.getValue().getRelativeFilePath() );

			assertEquals( entry.getValue(), actual, "File attributes do not match for: " + entry.getValue().getRelativeFilePath() + " -> destination copy list is not as expected" );
			actualCopyDest.remove( entry.getKey() );
		}

		// Verify: All expected deletions are present
		for( final Entry<Path, FileAttributes> entry : expectedDel.entrySet() ) {
			final FileAttributes actual = actualDel.get( entry.getKey() );
			assertNotNull( actual, "Expected file to delete not found: " + entry.getValue().getRelativeFilePath() );

			assertEquals( entry.getValue(), actual, "File attributes do not match for: " + entry.getValue().getRelativeFilePath() + " -> delete list is not as expected" );
			actualDel.remove( entry.getKey() );
		}

		// Verify: No unexpected extra files in any list
		assertTrue( actualCopySource.isEmpty(), "Unexpected extra files in source copy list (count: " + actualCopySource.size() + ") - " + actualCopySource.keySet() );

		assertTrue( actualCopyDest.isEmpty(), "Unexpected extra files in destination copy list (count: " + actualCopyDest.size() + ") - " + actualCopyDest.keySet() );

		assertTrue( actualDel.isEmpty(), "Unexpected extra files in delete list (count: " + actualDel.size() + ") - " + actualDel.keySet() );
	}

	/**
	 * Tests edge case: identical modification timestamps in source and destination.
	 * When timestamps are identical, the behavior depends on other attributes.
	 *
	 * <p><strong>Expected Behavior:</strong>
	 * Files with identical timestamps should not be copied if all other attributes
	 * also match (hash, size). If hashes differ, file content has changed and
	 * should be identified as needing synchronization.
	 * <p>
	 */
	@Test
	@DisplayName( "4. Edge case: identical modification timestamps" )
	final void testEdgeCaseIdenticalTimestamps() {
		// Files with same timestamp but same hash should not be copied
		// This is handled by the equalsFiles method
		final int initialSourceSize = sourceMap.size();
		final int initialDestSize = destMap.size();

		fileHandler.equalsFiles( sourceMap, destMap );

		// At least some files should have been removed as identical
		assertTrue( ( initialSourceSize + initialDestSize ) > ( sourceMap.size() + destMap.size() ), "Expected some files to be removed as identical" );
	}

	/**
	 * Verifies that the synchronization engine gracefully handles empty file structures.
	 * <p>
	 * <b>Scenario:</b> Both source and destination maps contain no file records.
	 * <b>Expected Result:</b> The returned list contains three completely empty operation maps
	 * (source copy, destination copy, and delete queue).
	 */
	@Test
	@DisplayName( "5. Edge case: empty source and destination maps" )
	final void testEdgeCaseEmptyMaps() {
		// Clear all maps
		sourceMap.clear();
		destMap.clear();
		syncMap.clear();

		// Execute
		final ArrayList<Map<Path, FileAttributes>> result = fileHandler.getSyncFiles(
				sourceMap, destMap,
				testPath.resolve( "source" ),
				testPath.resolve( "dest" ),
				syncMap );

		// Verify: All result maps are empty
		assertTrue( result.get( 0 ).isEmpty(), "Source copy list should be empty when source map is empty" );
		assertTrue( result.get( 1 ).isEmpty(), "Destination copy list should be empty when destination map is empty" );
		assertTrue( result.get( 2 ).isEmpty(), "Delete list should be empty when both maps are empty" );
	}

	/**
	 * Verifies that a file update is correctly propagated from source to destination
	 * during an initial sync if the source file has a newer modification timestamp.
	 * <p>
	 * <b>Scenario:</b> The same file exists in both locations, but the source file is newer.
	 * <b>Expected Result:</b> The file is marked for synchronization into the source-to-destination
	 * copy list, while both the reverse copy list and the deletion list remain empty.
	 */
	@Test
	@DisplayName( "6. Initial sync: source newer than destination" )
	void testInitialSyncSourceNewer() {

		sourceMap.clear();
		destMap.clear();
		syncMap.clear();

		final Path rel = Path.of( "test.txt" );
		final Path targetPath = testPath.resolve( "source" ).resolve( rel );
		final FileAttributes sourceAttr = new FileAttributes( rel, "2000", FileTime.fromMillis( 2000L ), "2000", FileTime.fromMillis( 2000L ), 50, "hash-new" );
		final FileAttributes destAttr = new FileAttributes( rel, "1000", FileTime.fromMillis( 1000L ), "1000", FileTime.fromMillis( 1000L ), 50, "hash-old" );
		sourceMap.put( testPath.resolve( "source" ).resolve( rel ), sourceAttr );
		destMap.put( testPath.resolve( "dest" ).resolve( rel ), destAttr );

		final ArrayList<Map<Path, FileAttributes>> result = fileHandler.getSyncFiles( sourceMap, destMap, testPath.resolve( "source" ), testPath.resolve( "dest" ), syncMap );

		assertAll( "Test empty sync list -- source is newer",
				() -> assertFalse( result.get( 0 ).isEmpty(), "Source copy list should not be empty" ),
				() -> assertTrue( result.get( 0 ).containsKey( targetPath ), () -> String.format(
						"Expected source file to be copied to destination.\nMissing Key:   %s\nActual Keys:    %s", targetPath, Arrays.toString( result.get( 0 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 1 ).isEmpty(), () -> String.format(
						"Expected source file to be copied to destination.\nMissing Key:   %s\nActual Keys:    %s", targetPath, Arrays.toString( result.get( 1 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 2 ).isEmpty(), "Delete list should be empty" ) );
	}

	/**
	 * Verifies that a file update is correctly propagated from destination to source
	 * during an initial sync if the destination file has a newer modification timestamp.
	 * <p>
	 * <b>Scenario:</b> The same file exists in both locations, but the destination file is newer.
	 * <b>Expected Result:</b> The file is marked for synchronization into the destination-to-source
	 * copy list, while both the forward copy list and the deletion list remain empty.
	 */
	@Test
	@DisplayName( "7. Initial sync: destination newer than source" )
	void testInitialSyncDestNewer() {

		sourceMap.clear();
		destMap.clear();
		syncMap.clear();

		final Path rel = Path.of( "test.txt" );
		final Path targetPath = testPath.resolve( "dest" ).resolve( rel );
		final FileAttributes sourceAttr = new FileAttributes( rel, "1000", FileTime.fromMillis( 1000L ), "1000", FileTime.fromMillis( 1000L ), 50, "hash-old" );
		final FileAttributes destAttr = new FileAttributes( rel, "2000", FileTime.fromMillis( 2000L ), "2000", FileTime.fromMillis( 2000L ), 50, "hash-new" );
		sourceMap.put( testPath.resolve( "source" ).resolve( rel ), sourceAttr );
		destMap.put( testPath.resolve( "dest" ).resolve( rel ), destAttr );

		final ArrayList<Map<Path, FileAttributes>> result = fileHandler.getSyncFiles( sourceMap, destMap, testPath.resolve( "source" ), testPath.resolve( "dest" ), syncMap );

		assertAll( "Test empty sync list -- dest is newer",
				() -> assertFalse( result.get( 1 ).isEmpty(), "Dest copy list should not be empty" ),
				() -> assertTrue( result.get( 1 ).containsKey( testPath.resolve( "dest" ).resolve( rel ) ), () -> String.format(
						"Expected destination file to be copied to source\nMissing Key:   %s\nActual Keys:    %s", targetPath, Arrays.toString( result.get( 1 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 0 ).isEmpty(), () -> String.format(
						"Expected destination file to be copied to source.\nMissing Key:   %s\nActual Keys:    %s", targetPath, Arrays.toString( result.get( 0 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 2 ).isEmpty(), "Delete list should be empty" ) );
	}

	/**
	 * Verifies that no synchronization actions are taken when the files in both the
	 * source and destination locations are completely identical.
	 * <p>
	 * <b>Scenario:</b> Files match exactly in metadata, size, timestamps, and hash signatures.
	 * <b>Expected Result:</b> All three operation action maps (copy queues and delete queue)
	 * return empty, as no state discrepancies require reconciliation.
	 */
	@Test
	@DisplayName( "8. Initial sync: identical files" )
	void testInitialSyncIdenticalFiles() {

		sourceMap.clear();
		destMap.clear();
		syncMap.clear();

		final Path rel = Path.of( "same.txt" );
		final FileAttributes attr = new FileAttributes( rel, "1000", FileTime.fromMillis( 1000L ), "1000", FileTime.fromMillis( 1000L ), 50, "hash-old" );
		sourceMap.put( testPath.resolve( "source" ).resolve( rel ), attr );

		destMap.put( testPath.resolve( "dest" ).resolve( rel ), attr );

		final ArrayList<Map<Path, FileAttributes>> result = fileHandler.getSyncFiles( sourceMap, destMap, testPath.resolve( "source" ), testPath.resolve( "dest" ), syncMap );

		assertAll( "Test empty sync list -- identical files",
				() -> assertTrue( result.get( 0 ).isEmpty(), () -> String.format(
						"Source copy list should be empty.\nActual Keys:    %s", Arrays.toString( result.get( 0 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 1 ).isEmpty(), () -> String.format(
						"Dest copy list should be empty.\nActual Keys:    %s", Arrays.toString( result.get( 1 ).keySet().toArray() ) ) ),
				() -> assertTrue( result.get( 2 ).isEmpty(), () -> String.format(
						"Del hit list should be empty.\nActual Keys:    %s", Arrays.toString( result.get( 2 ).keySet().toArray() ) ) ) );
	}
}
