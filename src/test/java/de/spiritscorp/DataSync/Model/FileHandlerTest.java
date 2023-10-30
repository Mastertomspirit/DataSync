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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.spiritscorp.DataSync.IO.Logger;

class FileHandlerTest {

	private Map<Path, FileAttributes> sourceMap = Model.createMap();
	private Map<Path, FileAttributes> destMap = Model.createMap();
	private Map<Path, FileAttributes> duplicateMap = Model.createMap();
	private Map<Path, FileAttributes> syncMap = Model.createMap();
	private Path path = ModelTest.TEST_PATH;


	private TestHelper helper = new TestHelper(path);
	private FileHandler fileHandler;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		helper.createSource(sourceMap);
		helper.createDest(destMap);
		fileHandler = new FileHandler(mock(Logger.class));

	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#listFiles(java.util.ArrayList, de.spiritscorp.DataSync.ScanType, boolean)}.
	 */
/*	@Test
	final void testListFiles() {
		fail("Noch nicht implementiert"); // TODO
	}
*/
	
	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#findDuplicates(java.util.Map)}.
	 */
	@Test
	final void testFindDuplicates() {
		duplicateMap = fileHandler.findDuplicates(sourceMap);
		assertEquals(helper.getDuplicates().length, duplicateMap.size(), "Duplicate size dont match");
		for(Path path : helper.getDuplicates()) {
			assertTrue(duplicateMap.containsKey(path), "Duplicate dont match");
		}
	}

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#equalsFiles(java.util.Map, java.util.Map)}.
	 */
	@Test
	final void testEqualsFiles() {
		fileHandler.equalsFiles(sourceMap, destMap);
		for(Path path : helper.getSourceNotHit()) {
			assertFalse(sourceMap.containsKey(path), "sourceMap contains wrong key");
		}
		for(Path path : helper.getDestNotHit()) {
			assertFalse(destMap.containsKey(path), "destMap contains wrong key");
		}
		for(Path path : helper.getSourceHits()) {
			assertTrue(sourceMap.containsKey(path), "Key is missing in sourceMap");
		}
		for(Path path : helper.getDestHits()) {
			assertTrue(destMap.containsKey(path), "Key is missing in destMap");
		}
	}
	
	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#getSyncFiles(java.util.Map, java.util.Map, java.nio.file.Path, java.nio.file.Path, java.util.Map)}.
	 * @throws IOException 
	 */
	@Test
	final void testGetSyncFiles() throws IOException {
		ArrayList<Map<Path, FileAttributes>> expectedValues = helper.createSyncMap(sourceMap, destMap, syncMap);
		Map<Path, FileAttributes> expectedCopySource = expectedValues.get(0);
		Map<Path, FileAttributes> expectedCopyDest = expectedValues.get(1);
		Map<Path, FileAttributes> expectedDel = expectedValues.get(2);

		ArrayList<Map<Path,FileAttributes>> syncFiles = fileHandler.getSyncFiles(sourceMap, destMap, path.resolve("source"), path.resolve("dest"), syncMap);
		Map<Path, FileAttributes> actualCopySource = syncFiles.get(0);
		Map<Path, FileAttributes> actualCopyDest = syncFiles.get(1);
		Map<Path, FileAttributes> actualDel = syncFiles.get(2);
		
		for(Entry<Path, FileAttributes> entry : expectedCopySource.entrySet()) {
			assertEquals(entry.getValue(), actualCopySource.get(entry.getKey()), entry.getValue().getRelativeFilePath() + " -> copySourceList is not as expected" );
			actualCopySource.remove(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : expectedCopyDest.entrySet()) {
			assertEquals(entry.getValue(), actualCopyDest.get(entry.getKey()), entry.getValue().getRelativeFilePath() + " -> copyDestList is not as expected" );
			actualCopyDest.remove(entry.getKey());
		}
		for(Entry<Path, FileAttributes> entry : expectedDel.entrySet()) {
			assertEquals(entry.getValue(), actualDel.get(entry.getKey()), entry.getValue().getRelativeFilePath() + " -> delList is not as expected" );
			actualDel.remove(entry.getKey());
		}
		
		assertTrue(actualCopySource.isEmpty(), "sourceMap isn´t empty -> " + actualCopySource.size());
		assertTrue(actualCopyDest.isEmpty(), "destMap isn´t empty -> " + actualCopyDest.size());
		assertTrue(actualDel.isEmpty(), "delMap isn´t empty -> " + actualDel.size());
	}	
	

	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#deleteFiles(java.util.Map, boolean, boolean, java.nio.file.Path)}.
	 */
/*	@Test
	final void testDeleteFiles() {
		fail("Noch nicht implementiert"); // TODO
	}
*/
	/**
	 * Testmethode für {@link de.spiritscorp.DataSync.Model.FileHandler#copyFiles(java.util.Map, boolean, java.nio.file.Path)}.
	 */
/*	@Test
	final void testCopyFiles() {
		fail("Noch nicht implementiert"); // TODO
	}
*/
}
