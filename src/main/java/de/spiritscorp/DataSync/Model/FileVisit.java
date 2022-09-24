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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.IO.Debug;

class FileVisit implements FileVisitor<Path> {
	private ExecutorService executor;
	private Path path;
	private Map<Path, FileAttributes> map;
	private ScanType deepScan;
	
	/**
	 * 		@param executor
	 * 		@param path
	 * 		@param map
	 * 		@param deepScan
	 */
	FileVisit(ExecutorService executor, Path path, Map<Path, FileAttributes> map, ScanType deepScan) {
		this.executor = executor;
		this.path = path;
		this.map = map;
		this.deepScan = deepScan;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if(dir != null) {
			if(dir.endsWith("$RECYCLE.BIN") ||
				dir.endsWith("System Volume Information") ||
				dir.endsWith("Papierkorb")) {
					Debug.PRINT_DEBUG("Skip_preVisitDirectory -> " + dir);
					return FileVisitResult.SKIP_SUBTREE;
			}}
		Debug.PRINT_DEBUG("visiting directory: %s", dir.toString());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if(attrs.isSymbolicLink() || !attrs.isRegularFile()) {	
			Debug.PRINT_DEBUG("Skip_VisitFile -> " + file);
			return FileVisitResult.CONTINUE;
		}
		executor.execute(new FileScan(file, path, map, deepScan, attrs));
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		Debug.PRINT_DEBUG("VisitFileFailed: " + file);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
