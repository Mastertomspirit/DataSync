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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

class TestHelper {
	
	private Path path = Paths.get(System.getProperty("user.home"), "DataSyncTemp");
	private FileTime[] time = {FileTime.fromMillis(1641335618384L), FileTime.fromMillis(1641335619384L), FileTime.fromMillis(1641335620384L),
						FileTime.fromMillis(1641335621384L), FileTime.fromMillis(1641335622384L), FileTime.fromMillis(1641335623384L),
						FileTime.fromMillis(1641335624384L), FileTime.fromMillis(1641335625384L), FileTime.fromMillis(1641335626384L)};
	private ArrayList<Path> paths, sourceList, destList;
	
	TestHelper(){
		paths = new ArrayList<>();
		sourceList = new ArrayList<>();
		destList = new ArrayList<>();
		paths.add(path.resolve("source"));
		paths.add(path.resolve("dest"));
		
		for(int i = 0; i < 4; i++) {
			sourceList.add(path.resolve("source").resolve("testFile" + i + ".txt"));
		}
		for(int i = 0; i < 3; i++) {
			sourceList.add(path.resolve("source").resolve("testDir").resolve("testFile" + i + ".txt"));
		}
		for(int i = 0; i < 3; i++) {
			destList.add(path.resolve("dest").resolve("testFile" + i + ".txt"));
		}
		for(int i = 0; i < 5; i++) {
			destList.add(path.resolve("dest").resolve("testDir").resolve("testFile" + i + ".txt"));
		}
	}

	Map<Path, FileAttributes> createSource(Map<Path, FileAttributes> map){
		map.put(sourceList.get(0), new FileAttributes(paths.get(0).relativize(sourceList.get(0)), fileTimeToString(time[0]), time[1], fileTimeToString(time[2]), 12L, "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(1), new FileAttributes(paths.get(0).relativize(sourceList.get(1)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 102L, "3330b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(2), new FileAttributes(paths.get(0).relativize(sourceList.get(2)), fileTimeToString(time[2]), time[3], fileTimeToString(time[1]), 120L, "44b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(3), new FileAttributes(paths.get(0).relativize(sourceList.get(3)), fileTimeToString(time[3]), time[1], fileTimeToString(time[2]), 125L, "5520b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(4), new FileAttributes(paths.get(0).relativize(sourceList.get(4)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(5), new FileAttributes(paths.get(0).relativize(sourceList.get(5)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(sourceList.get(6), new FileAttributes(paths.get(0).relativize(sourceList.get(6)), fileTimeToString(time[1]), time[2], fileTimeToString(time[4]), 12L, "abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
	return map;
	}
	
	Map<Path, FileAttributes> createDest(Map<Path, FileAttributes> map){
		map.put(destList.get(0), new FileAttributes(paths.get(1).relativize(destList.get(0)), fileTimeToString(time[0]), time[1], fileTimeToString(time[2]), 12L, "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(1), new FileAttributes(paths.get(1).relativize(destList.get(1)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 102L, "3330b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(2), new FileAttributes(paths.get(1).relativize(destList.get(2)), fileTimeToString(time[2]), time[3], fileTimeToString(time[1]), 120L, "aaa9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(3), new FileAttributes(paths.get(1).relativize(destList.get(3)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(4), new FileAttributes(paths.get(1).relativize(destList.get(4)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "1a60b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(5), new FileAttributes(paths.get(1).relativize(destList.get(5)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "abb0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(6), new FileAttributes(paths.get(1).relativize(destList.get(6)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
		map.put(destList.get(7), new FileAttributes(paths.get(1).relativize(destList.get(7)), fileTimeToString(time[1]), time[2], fileTimeToString(time[3]), 12L, "ccc0b9cd4c7355dc427a8f622961fa971d9401e0626e447352d701f1671423f2"));
	return map;
	}

	Path[] getDuplicates() {
		return new Path[] {sourceList.get(0), sourceList.get(4), sourceList.get(5), sourceList.get(6)};
	}
	Path[] getSourceHits() {
		return new Path[] {sourceList.get(2), sourceList.get(3), sourceList.get(5), sourceList.get(6)};
	}
	Path[] getDestHits() {
		return new Path[] {destList.get(2), destList.get(4), destList.get(5), destList.get(6), destList.get(7)};
	}
	Path[] getSourceNotHit() {
		return new Path[] {sourceList.get(0), sourceList.get(1), sourceList.get(4)};
	}
	Path[] getDestNotHit() {
		return new Path[] {destList.get(0), destList.get(1), destList.get(3)};
	}
	String fileTimeToString(FileTime fileTime) {
		return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss"));
	}
}