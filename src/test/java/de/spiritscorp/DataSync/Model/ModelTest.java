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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.DataSync.Controller.SyncJobContext;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;

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

		when( ctx.getPreference().getSourcePath() ).thenReturn( new ArrayList<>() );
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
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.Model#scanSyncFiles(java.util.ArrayList, java.util.ArrayList, java.lang.Long[], de.spiritscorp.DataSync.ScanType, boolean, boolean)}.
	 */
	@Test
	final void testScanSyncFiles() {
//		fail("Noch nicht implementiert");
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.Model#backupFiles(int, boolean, java.nio.file.Path, boolean, java.nio.file.Path)}.
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

		assertTrue( model.backupFiles( 0, false, TEST_PATH.resolve( "dest" ), false, null ), "Die listen sind nicht leer" );
		final List<Path> listSource = getFileNamesInDirectory( TEST_PATH.resolve( "source" ) );
		final List<Path> listDest = getFileNamesInDirectory( TEST_PATH.resolve( "dest" ) );

		assertEquals( 2, listSource.size(), "source Ordner, anzahl passt nicht" );
		assertEquals( 2, listDest.size(), "dest Ordner, anzahl passt nicht" );
		assertEquals( destFiles[0].getFileName(), listDest.get( 0 ).getFileName(), "FileName passt nicht überein" );
		assertEquals( destFiles[1].getFileName(), listDest.get( 1 ).getFileName(), "FileName passt nicht überein" );
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.Model#syncFiles(java.util.ArrayList, java.util.Map, java.nio.file.Path, java.nio.file.Path, boolean)}.
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
		assertTrue( model.syncFiles( ctx, expectedList, syncMap, TEST_PATH.resolve( "source" ), TEST_PATH.resolve( "dest" ), true ), "Die listen sind nicht leer" );
		final List<Path> destList = getFileNamesInDirectory( TEST_PATH.resolve( "dest" ) );
		final List<Path> sourceList = getFileNamesInDirectory( TEST_PATH.resolve( "source" ) );
		assertEquals( destList, sourceList, "Quelle und Ziel passen nicht überein" );
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.Model#getSyncFiles(java.util.Map, java.nio.file.Path, java.nio.file.Path)}.
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
		for( int i = 0; i < expectedLists.size(); i++ ) {
			assertEquals( expectedLists.get( i ).size(), result.get( i ).size(), "Die Größe der Liste passt nicht!" );
			for( final Path path : expectedLists.get( i ).keySet() ) {
				assertTrue( result.get( i ).containsKey( path ), "Key stimmt nicht!" );
				assertTrue( result.get( i ).containsValue( expectedLists.get( i ).get( path ) ), "Werte stimmen nicht" );
			}
		}
	}

	/**
	 * Safely extracts all non-directory file names from a target directory path.
	 * Guarantees immediate closure of native file system handles.
	 */
	static List<Path> getFileNamesInDirectory( Path directory ) throws IOException {
		try( Stream<Path> stream = Files.walk( directory ) ) {
			return stream
					.filter( path -> !Files.isDirectory( path ) )
					.map( Path::getFileName )
					.collect( Collectors.toList() );
		}
	}
}