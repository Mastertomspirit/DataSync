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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;
import org.mockito.MockedStatic;

import de.spiritscorp.datasync.BgTime;
import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.model.BgModel;
import de.spiritscorp.datasync.model.FileAttributes;
import de.spiritscorp.datasync.model.Model;

/**
 * Integration test suite validating the background processing pipeline of {@link BgModel}.
 * <p>
 * This suite orchestrates authentic disk I/O transactions within localized test directories
 * to guarantee structural synchronization accuracy, metadata resolution, and state preservation.
 * Environmental diagnostics are safely intercepted to prevent downstream log pollution.
 * </p>
 */
@SelectPackages( value = { "de.spiritscorp.datasync.model" } )
//@DisplayName( "Background Model Integration Tests" )
class BgModelIT {

	private Map<Path, FileAttributes> sourceMap, destMap, sourceMapRef, destMapRef, syncMap;
	private TestHelper helper;
	private BgModel bgModel;
	private Preference prefMock;
	/** Insulates the static {@link Debug} diagnostics subsystem during test execution. */
	private MockedStatic<Debug> mockedDebug;

	/**
	 * Configures the localized I/O environment and stubs the runtime preference layout.
	 *
	 * @throws Exception If physical directory allocation fails.
	 */
	@BeforeEach
	void setUp() throws Exception {
		sourceMap = Model.createMap();
		destMap = Model.createMap();
		sourceMapRef = Model.createMap();
		destMapRef = Model.createMap();
		syncMap = Model.createMap();

		prefMock = mock( Preference.class );
		bgModel = new BgModel( prefMock, mock( Logger.class ), sourceMap, destMap );
		helper = new TestHelper( ModelTest.TEST_PATH );
		mockedDebug = mockStatic( Debug.class );

		final ArrayList<Path> sourcePath = new ArrayList<>();
		sourcePath.add( ModelTest.TEST_PATH.resolve( "source" ) );
		final ArrayList<Path> destPath = new ArrayList<>();
		destPath.add( ModelTest.TEST_PATH.resolve( "dest" ) );

		when( prefMock.getSourcePath() ).thenReturn( sourcePath );
		when( prefMock.getDestPath() ).thenReturn( destPath );
		when( prefMock.isLogOn() ).thenReturn( false );
		when( prefMock.getStartSourcePath() ).thenReturn( ModelTest.TEST_PATH.resolve( "source" ) );
		when( prefMock.getStartDestPath() ).thenReturn( ModelTest.TEST_PATH.resolve( "dest" ) );
		when( prefMock.getTrashbinPath() ).thenReturn( null );
		when( prefMock.isTrashbin() ).thenReturn( false );
//		when( prefMock.isAutoBgDel() ).thenReturn( true );
		when( prefMock.getLastScanTime() ).thenReturn( 0L );
		when( prefMock.isSubDir() ).thenReturn( false );
		when( prefMock.getBgTime() ).thenReturn( BgTime.MIN_30 );
		when( prefMock.getSyncMap() ).thenReturn( syncMap );
	}

	/**
	 * Tears down allocated disk matrices and safely deregistrates static diagnostic proxies.
	 *
	 * @throws Exception If OS file handles block directory liquidation.
	 */
	@AfterEach
	void tearDown() throws Exception {
		if( Files.exists( ModelTest.TEST_PATH ) ) {
			final List<Path> paths = Files.walk( ModelTest.TEST_PATH ).filter( ( a ) -> !Files.isDirectory( a ) ).collect( Collectors.toList() );
			for( final Path path : paths ) {
				Files.delete( path );
			}
			final List<Path> dirs = Files.walk( ModelTest.TEST_PATH ).filter( ( a ) -> Files.isDirectory( a ) ).collect( Collectors.toList() );
			for( int i = dirs.size() - 1; i >= 0; i-- ) {
				Files.delete( dirs.get( i ) );
			}
		}
		if( mockedDebug != null ) {
			mockedDebug.close();
		}
	}

	/**
	 * Verifies bidirectionally synchronized background execution when driven by an existing mapping frame.
	 *
	 * @throws IOException If state verification passes encounter dead file streams.
	 */
	@Test
	@DisplayName( "1. Synchronize Mode: Processing with pre-allocated SyncMap blueprints" )
	final void testRunBgJobSyncronizeWithSyncMap() throws IOException {
		when( prefMock.getScanMode() ).thenReturn( ScanType.SYNCHRONIZE );
		helper.createSyncMap( sourceMapRef, destMapRef, syncMap );
		for( final Entry<Path, FileAttributes> entry : sourceMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		for( final Entry<Path, FileAttributes> entry : destMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		assertTrue( bgModel.runBgJob(), "Die Listen sind nicht leer" );
		final List<Path> destList = ModelTest.getFileNamesInDirectory( ModelTest.TEST_PATH.resolve( "dest" ) );
		final List<Path> sourceList = ModelTest.getFileNamesInDirectory( ModelTest.TEST_PATH.resolve( "source" ) );
		assertEquals( destList, sourceList, "Quelle und Ziel passen nicht überein" );
		final String[] resultList = { "elastisch.txt", "fleißig.txt", "robust.txt", "schnurrend.txt", "wissend.txt", "uralt.txt", "zierlich.txt" };
		for( int i = 0; i < resultList.length; i++ ) {
			assertTrue( sourceList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			sourceList.remove( Paths.get( resultList[i] ) );
			assertTrue( destList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			destList.remove( Paths.get( resultList[i] ) );
		}
		assertEquals( 0, sourceList.size(), "Liste ist nicht leer" );
		assertEquals( 0, destList.size(), "Liste ist nicht leer" );
	}

	/**
	 * Verifies full fallback recovery synchronization when running with an uninitialized or purged reference ledger.
	 *
	 * @throws IOException If state verification passes encounter dead file streams.
	 */
	@Test
	@DisplayName( "2. Synchronize Mode: Processing across cleared/empty SyncMap historical contexts" )
	final void testRunBgJobSyncronizeWithOutSyncMap() throws IOException {
		when( prefMock.getScanMode() ).thenReturn( ScanType.SYNCHRONIZE );

		helper.createSyncMap( sourceMapRef, destMapRef, syncMap );
		for( final Entry<Path, FileAttributes> entry : sourceMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		for( final Entry<Path, FileAttributes> entry : destMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		syncMap.clear();
		assertTrue( bgModel.runBgJob(), "Die Listen sind nicht leer" );
		final List<Path> destList = Files.walk( ModelTest.TEST_PATH.resolve( "dest" ) ).filter( ( a ) -> !Files.isDirectory( a ) ).map( ( a ) -> a.getFileName() ).collect( Collectors.toList() );
		final List<Path> sourceList = Files.walk( ModelTest.TEST_PATH.resolve( "source" ) ).filter( ( a ) -> !Files.isDirectory( a ) ).map( ( a ) -> a.getFileName() ).collect( Collectors.toList() );
		assertEquals( destList, sourceList, "Quelle und Ziel passen nicht überein" );
		final String[] resultList = { "eiskalt.txt", "elastisch.txt", "fleißig.txt", "robust.txt", "schnurrend.txt", "wissend.txt", "uralt.txt", "vernünftig.txt", "zierlich.txt" };
		for( int i = 0; i < resultList.length; i++ ) {
			assertTrue( sourceList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			sourceList.remove( Paths.get( resultList[i] ) );
			assertTrue( destList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			destList.remove( Paths.get( resultList[i] ) );
		}
		assertEquals( 0, sourceList.size(), "Liste ist nicht leer" );
		assertEquals( 0, destList.size(), "Liste ist nicht leer" );
	}

	/**
	 * Verifies downstream sequential backup distribution mappings under explicit flat file configurations.
	 *
	 * @throws IOException If state verification passes encounter dead file streams.
	 */
	@Test
	@DisplayName( "3. Flat Scan Mode: Unidirectional flat repository archive distribution check" )
	final void testRunBgJobBackup() throws IOException {
		when( prefMock.getScanMode() ).thenReturn( ScanType.FLAT_SCAN );

		helper.createBackupFiles( sourceMapRef, destMapRef );
		for( final Entry<Path, FileAttributes> entry : sourceMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		for( final Entry<Path, FileAttributes> entry : destMapRef.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}

		assertTrue( bgModel.runBgJob(), "Die Listen sind nicht leer" );
		final List<Path> destList = ModelTest.getFileNamesInDirectory( ModelTest.TEST_PATH.resolve( "dest" ) );
		final List<Path> sourceList = ModelTest.getFileNamesInDirectory( ModelTest.TEST_PATH.resolve( "source" ) );
		assertEquals( destList, sourceList, "Quelle und Ziel passen nicht überein" );
		final String[] resultList = { "testFile1.txt", "testFile3.txt" };
		for( int i = 0; i < resultList.length; i++ ) {
			assertTrue( sourceList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			sourceList.remove( Paths.get( resultList[i] ) );
			assertTrue( destList.contains( Paths.get( resultList[i] ) ), "Datei passt nicht -> " + resultList[i] );
			destList.remove( Paths.get( resultList[i] ) );
		}
		assertEquals( 0, sourceList.size(), "Liste ist nicht leer" );
		assertEquals( 0, destList.size(), "Liste ist nicht leer" );
	}
}
