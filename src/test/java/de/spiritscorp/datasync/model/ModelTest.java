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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.datasync.controller.SyncJobContext;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;

/**
 *
 */
class ModelTest {

	public static final Path TEST_PATH = Paths.get( System.getProperty( "user.home" ), ".DataSyncTemp" );

	private Map<Path, FileAttributes> sourceMap;
	private Map<Path, FileAttributes> destMap;
	private Map<Path, FileAttributes> syncMap;
	private TestHelper helper;
	private Model model;
	private SyncJobContext ctx;

	/** Insulates the static {@link Debug} diagnostics subsystem during test execution. */
	private MockedStatic<Debug> mockDebug;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		sourceMap = Model.createMap();
		destMap = Model.createMap();
		syncMap = Model.createMap();
		helper = new TestHelper( TEST_PATH );
		mockDebug = Mockito.mockStatic( Debug.class );
		ctx = mock( SyncJobContext.class, RETURNS_DEEP_STUBS );
		model = new Model( mock( Logger.class ), sourceMap, destMap );

		when( ctx.getPreference().getSourcePaths() ).thenReturn( new ArrayList<>() );
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		if( Files.exists( TEST_PATH ) ) {
			try( Stream<Path> walk = Files.walk( TEST_PATH ) ) {
				for( final Path path : (Iterable<Path>) walk.sorted( Comparator.reverseOrder() )::iterator ) {
					Files.delete( path );
				}
			}
		}
		if( mockDebug != null ) {
			mockDebug.close();
		}
	}

	/**
	 * Testmethode für {@link de.spiritscorp.datasync.model.Model#backupFiles(int, boolean, java.nio.file.Path, boolean, java.nio.file.Path)}.
	 *
	 * @throws IOException
	 */
	@Test
	final void testBackupFiles() throws IOException {
		helper.createBackupFiles( sourceMap, destMap );
		final Path[] destFiles = new Path[2];
		int i = 0;
		for( final Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
			destFiles[i] = TEST_PATH.resolve( "dest" ).resolve( entry.getValue().getRelativeFilePath() );
			i++;
		}
		for( final Entry<Path, FileAttributes> entry : destMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}

		final List<Path> listSource = new ArrayList<>();
		final List<Path> listDest = new ArrayList<>();

		assertAll(
				() -> {
					assertTrue( model.backupFiles( true, false, TEST_PATH.resolve( "dest" ), false, null ), "Die listen sind nicht leer" );
					listSource.addAll( getFileNamesInDirectory( TEST_PATH.resolve( "source" ) ) );
					listDest.addAll( getFileNamesInDirectory( TEST_PATH.resolve( "dest" ) ) );
				},
				() -> assertEquals( 2, listSource.size(), "source Ordner, anzahl passt nicht" ),
				() -> assertEquals( 2, listDest.size(), "dest Ordner, anzahl passt nicht" ),
				() -> assertEquals( destFiles[0].getFileName(), listDest.get( 0 ).getFileName(), "FileName passt nicht überein" ),
				() -> assertEquals( destFiles[1].getFileName(), listDest.get( 1 ).getFileName(), "FileName passt nicht überein" ) );
	}

	/**
	 * Testmethode für {@link de.spiritscorp.datasync.model.Model#syncFiles(java.util.ArrayList, java.util.Map, java.nio.file.Path, java.nio.file.Path, boolean)}.
	 *
	 * @throws IOException
	 */
	@Test
	final void testSyncFiles() throws IOException {
		final ArrayList<Map<Path, FileAttributes>> expectedList = helper.createSyncMap( sourceMap, destMap, syncMap );
		for( final Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		for( final Entry<Path, FileAttributes> entry : destMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}

		// Dynamic assetion collector
		final List<Executable> assertions = new ArrayList<>();
		final List<Path> destList = new ArrayList<>();
		final List<Path> sourceList = new ArrayList<>();

		assertions.add( () -> {
			assertTrue( model.syncFiles( ctx, expectedList, syncMap, TEST_PATH.resolve( "source" ), TEST_PATH.resolve( "dest" ), true ), "Die listen sind nicht leer" );
			destList.addAll( getFileNamesInDirectory( TEST_PATH.resolve( "dest" ) ) );
			sourceList.addAll( getFileNamesInDirectory( TEST_PATH.resolve( "source" ) ) );
		} );

		assertions.add( () -> assertEquals( destList, sourceList, "Quelle und Ziel passen nicht überein" ) );

		assertAll( assertions );
	}

	/**
	 * Testmethode für {@link de.spiritscorp.datasync.model.Model#getSyncFiles(java.util.Map, java.nio.file.Path, java.nio.file.Path)}.
	 *
	 * @throws IOException
	 */
	@Test
	final void testGetSyncFiles() throws IOException {
		final ArrayList<Map<Path, FileAttributes>> expectedLists = helper.createSyncMap( sourceMap, destMap, syncMap );
		for( final Entry<Path, FileAttributes> entry : sourceMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		for( final Entry<Path, FileAttributes> entry : destMap.entrySet() ) {
			Files.createDirectories( entry.getKey().getParent() );
			Files.createFile( entry.getKey() );
		}
		final ArrayList<Map<Path, FileAttributes>> result = model.getSyncFiles( syncMap, TEST_PATH.resolve( "source" ), TEST_PATH.resolve( "dest" ) );

		// Dynamic assetion collector
		final List<Executable> assertions = new ArrayList<>();

		for( int i = 0; i < expectedLists.size(); i++ ) {
			final Map<Path, FileAttributes> expectedMap = expectedLists.get( i );
			final Map<Path, FileAttributes> resultMap = result.get( i );
			assertions.add( () -> assertEquals( expectedMap.size(), resultMap.size(), "Die Größe der Liste passt nicht!" ) );
			for( final Path path : expectedMap.keySet() ) {
				assertions.add( () -> assertTrue( resultMap.containsKey( path ), "Key stimmt nicht!" ) );
				assertions.add( () -> assertTrue( resultMap.containsValue( expectedMap.get( path ) ), "Werte stimmen nicht" ) );
			}
		}
		assertAll( assertions );
	}

	/**
	 * Safely extracts all non-directory file names from a target directory path.
	 * Guarantees immediate closure of native file system handles.
	 */
	static List<Path> getFileNamesInDirectory( final Path directory ) throws IOException {
		try( Stream<Path> stream = Files.walk( directory ) ) {
			return stream
					.filter( path -> !Files.isDirectory( path ) )
					.map( Path::getFileName )
					.toList();
		}
	}
}