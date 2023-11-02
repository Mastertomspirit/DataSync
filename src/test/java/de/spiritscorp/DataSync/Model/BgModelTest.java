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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;

/**
 * 
 */
class BgModelTest {

	private static Map<Path,FileAttributes> sourceMap, destMap, sourceMapRef, destMapRef, syncMap;
	private TestHelper helper;
	private BgModel bgModel;
	private static Preference prefMock;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		prefMock = mock(Preference.class);

		ArrayList<Path> sourcePath = new ArrayList<>();
		sourcePath.add(ModelTest.TEST_PATH.resolve("source"));
		ArrayList<Path> destPath = new ArrayList<>();
		destPath.add(ModelTest.TEST_PATH.resolve("dest"));
		
		when(prefMock.getSourcePath()).thenReturn(sourcePath);
		when(prefMock.getDestPath()).thenReturn(destPath);
		when(prefMock.isLogOn()).thenReturn(false);
		when(prefMock.getStartSourcePath()).thenReturn(ModelTest.TEST_PATH.resolve("source"));
		when(prefMock.getStartDestPath()).thenReturn(ModelTest.TEST_PATH.resolve("dest"));
		when(prefMock.getTrashbinPath()).thenReturn(null);
		when(prefMock.isTrashbin()).thenReturn(false);
		when(prefMock.isAutoBgDel()).thenReturn(true);
		when(prefMock.getLastScanTime()).thenReturn(0L);
		when(prefMock.isSubDir()).thenReturn(false);
		when(prefMock.getBgTime()).thenReturn(BgTime.MIN_30);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		sourceMap = Model.createMap();
		destMap = Model.createMap();
		sourceMapRef = Model.createMap();
		destMapRef = Model.createMap();
		syncMap = Model.createMap();
		helper = new TestHelper(ModelTest.TEST_PATH);
		bgModel = new BgModel(prefMock, mock(Logger.class), sourceMap, destMap);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		if(Files.exists(ModelTest.TEST_PATH)) {		
			List<Path> paths = Files.walk(ModelTest.TEST_PATH).filter((a) -> !Files.isDirectory(a)).collect(Collectors.toList());
			for(Path path : paths) {
				Files.delete(path);
			}
			List<Path> dirs = Files.walk(ModelTest.TEST_PATH).filter((a) -> Files.isDirectory(a)).collect(Collectors.toList());
			for(int i = dirs.size() -1; i >= 0; i--) {
				Files.delete(dirs.get(i));
			}
		}
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.BgModel#runBgJob()}.
	 * @throws IOException 
	 */
	@Test
	final void testRunBgJob_syncronize_with_SyncMap() throws IOException {
		when(prefMock.getDeepScan()).thenReturn(ScanType.SYNCHRONIZE);
		helper.createSyncMap(sourceMapRef, destMapRef, syncMap);
		when(prefMock.getSyncMap()).thenReturn(syncMap);
		for(Entry<Path, FileAttributes> entry : sourceMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : destMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		assertTrue(bgModel.runBgJob(), "Die Listen sind nicht leer");
		List<Path> destList = Files.walk(ModelTest.TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		List<Path> sourceList = Files.walk(ModelTest.TEST_PATH.resolve("source")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		assertEquals(destList, sourceList, "Quelle und Ziel passen nicht überein");
		final String[] resultList = {"elastisch.txt", "fleißig.txt", "robust.txt", "schnurrend.txt", "wissend.txt", "uralt.txt", "zierlich.txt"};
		for(int i = 0; i < resultList.length; i++) {
				assertTrue(sourceList.contains(Paths.get(resultList[i])), "Datei passt nicht -> " + resultList[i]);
				sourceList.remove(Paths.get(resultList[i]));
		}
		assertEquals(sourceList.size(), 0, "Liste ist nicht leer");
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.BgModel#runBgJob()}.
	 * @throws IOException 
	 */
	@Test
	final void testRunBgJob_syncronize_withOut_SyncMap() throws IOException {
		when(prefMock.getDeepScan()).thenReturn(ScanType.SYNCHRONIZE);

		helper.createSyncMap(sourceMapRef, destMapRef, syncMap);
		for(Entry<Path, FileAttributes> entry : sourceMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : destMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		syncMap.clear();
		assertTrue(bgModel.runBgJob(), "Die Listen sind nicht leer");
		List<Path> destList = Files.walk(ModelTest.TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		List<Path> sourceList = Files.walk(ModelTest.TEST_PATH.resolve("source")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		assertEquals(destList, sourceList, "Quelle und Ziel passen nicht überein");
		final String[] resultList = {"eiskalt.txt", "fleißig.txt", "robust.txt", "schnurrend.txt", "wissend.txt", "uralt.txt", "vernünftig.txt"};
		for(int i = 0; i < resultList.length; i++) {
			assertTrue(sourceList.contains(Paths.get(resultList[i])), "Datei passt nicht -> " + resultList[i]);
			sourceList.remove(Paths.get(resultList[i]));
		}
		assertEquals(sourceList.size(), 0, "Liste ist nicht leer");
	}
	
	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.BgModel#runBgJob()}.
	 * @throws IOException 
	 */
	@Test
	final void testRunBgJob_backup() throws IOException {
		when(prefMock.getDeepScan()).thenReturn(ScanType.FLAT_SCAN);

		helper.createBackupFiles(sourceMapRef, destMapRef);
		for(Entry<Path, FileAttributes> entry : sourceMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : destMapRef.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		
		assertTrue(bgModel.runBgJob(), "Die Listen sind nicht leer");
		List<Path> destList = Files.walk(ModelTest.TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		List<Path> sourceList = Files.walk(ModelTest.TEST_PATH.resolve("source")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		assertEquals(destList, sourceList, "Quelle und Ziel passen nicht überein");
		final String[] resultList = {"testFile1.txt", "testFile3.txt"};
		for(int i = 0; i < resultList.length; i++) {
			assertTrue(sourceList.contains(Paths.get(resultList[i])), "Datei passt nicht -> " + resultList[i]);
			sourceList.remove(Paths.get(resultList[i]));
		}
		assertEquals(sourceList.size(), 0, "Liste ist nicht leer");
	}
}
