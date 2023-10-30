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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.spiritscorp.DataSync.ScanType;

class FileScanTest {

	long size;
	String fileHash = "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2";
	Path path = Paths.get(System.getProperty("user.home"), "DataSyncTemp");
	Path file = Paths.get("testFile.txt");
	FileTime modTime = FileTime.fromMillis(1641335619384L);
	FileTime accessTime = FileTime.fromMillis(1641335620384L);
	HashMap<Path,FileAttributes> map = new HashMap<>();
	TestHelper helper = new TestHelper(path);
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		
		if(!Files.exists(path)) 	Files.createDirectory(path);
		
		Files.writeString(path.resolve(file), "Das ist ein Teststring", StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		Files.setAttribute(path.resolve(file), "lastModifiedTime", modTime);
		Files.setAttribute(path.resolve(file), "lastAccessTime", accessTime);
		size = (long) Files.getAttribute(path.resolve(file), "size");	
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		Files.deleteIfExists(path.resolve(file));
		Files.deleteIfExists(path);
	}

	/**
	 * Test method for {@link de.spiritscorp.DataSync.Model.FileScan#FileScan(java.nio.file.Path, java.nio.file.Path, java.util.Map, de.spiritscorp.DataSync.ScanType)}.
	 * @throws InterruptedException 
	 */
	@Test
	final void testFileScan() throws IOException {
		BasicFileAttributes bfa = Files.readAttributes(path.resolve(file), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		
		new FileScan(path.resolve(file),path,map,ScanType.FLAT_SCAN, bfa);		
		for(Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			assertEquals(path.resolve(file), entry.getKey(), "Map Key / Pfad falsch!!");
			assertEquals(size, entry.getValue().getSize(), "File Size passt nicht");
			assertEquals(helper.fileTimeToString(modTime), entry.getValue().getModTimeString(), "ModifiedTime passt nicht");
			assertEquals(file, entry.getValue().getRelativeFilePath(), "RelativePath passt nicht");
			assertNull(entry.getValue().getFileHash(), "FileHash falscher Wert");
		}
		
		new FileScan(path.resolve(file), path, map, ScanType.DEEP_SCAN, bfa);
		for(Map.Entry<Path, FileAttributes> entry : map.entrySet()) {
			assertEquals(path.resolve(file), entry.getKey(), "Map Key / Pfad falsch!!");
			assertEquals(size, entry.getValue().getSize(), "File Size passt nicht");
			assertEquals(helper.fileTimeToString(modTime), entry.getValue().getModTimeString(), "ModifiedTime passt nicht");
			assertEquals(file, entry.getValue().getRelativeFilePath(), "RelativePath passt nicht");
			assertEquals(fileHash, entry.getValue().getFileHash(), "FileHash falscher Wert");
		}
	}
}
