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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.spiritscorp.DataSync.IO.Logger;

/**
 * 
 */
class ModelTest {

	public static final Path TEST_PATH = Paths.get(System.getProperty("user.home"), ".DataSyncTemp");

	private Map<Path,FileAttributes> sourceMap, destMap, syncMap;
	private TestHelper helper;
	private Model model;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {

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
		syncMap = Model.createMap();
		helper = new TestHelper(TEST_PATH);
		model = new Model(mock(Logger.class), sourceMap, destMap);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		if(Files.exists(TEST_PATH)) {		
			List<Path> paths = Files.walk(TEST_PATH).filter((a) -> !Files.isDirectory(a)).collect(Collectors.toList());
			for(Path path : paths) {
				Files.delete(path);
			}
			List<Path> dirs = Files.walk(TEST_PATH).filter((a) -> Files.isDirectory(a)).collect(Collectors.toList());
			for(int i = dirs.size() -1; i >= 0; i--) {
				Files.delete(dirs.get(i));
			}
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
	 * @throws IOException 
	 */
	@Test
	final void testBackupFiles() throws IOException {
		helper.createBackupFiles(sourceMap, destMap);
		Path[] destFiles = new Path[2];
		int i = 0;
		for(Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
			destFiles[i] = TEST_PATH.resolve("dest").resolve(entry.getValue().getRelativeFilePath());
			i++;
		}
		for(Entry<Path, FileAttributes> entry : destMap.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
	
		assertTrue(model.backupFiles(0, false, TEST_PATH.resolve("dest"), false, null), "Die listen sind nicht leer");
		assertEquals(2, Files.walk(TEST_PATH.resolve("source")).filter((a) -> !Files.isDirectory(a)).count(), "source Ordner, anzahl passt nicht");
		assertEquals(2, Files.walk(TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).count(), "dest Ordner, anzahl passt nicht");
		List<Path> listDest = Files.walk(TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).collect(Collectors.toList());
		assertEquals(destFiles[0].getFileName(), listDest.get(0).getFileName(), "FileName passt nicht überein");
		assertEquals(destFiles[1].getFileName(), listDest.get(1).getFileName(), "FileName passt nicht überein");
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.Model#syncFiles(java.util.Map, java.nio.file.Path, java.nio.file.Path)}.
	 * @throws IOException 
	 */
	@Test
	final void testSyncFiles() throws IOException {
		helper.createSyncMap(sourceMap, destMap, syncMap);
		for(Entry<Path, FileAttributes> entry : sourceMap.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : destMap.entrySet()) {
			Files.createDirectories(entry.getKey().getParent());
			Files.createFile(entry.getKey());
		}
		
		assertTrue(model.syncFiles(syncMap, TEST_PATH.resolve("source"), TEST_PATH.resolve("dest")), "Die listen sind nicht leer");
		List<Path> destList = Files.walk(TEST_PATH.resolve("dest")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		List<Path> sourceList = Files.walk(TEST_PATH.resolve("source")).filter((a) -> !Files.isDirectory(a)).map((a) -> a.getFileName()).collect(Collectors.toList());
		assertEquals(destList, sourceList, "Quelle und Ziel passen nicht überein");
	}
}