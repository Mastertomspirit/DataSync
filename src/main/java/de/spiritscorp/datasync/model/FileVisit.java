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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;

class FileVisit implements FileVisitor<Path> {
	private final ExecutorService executor;
	private final Path path;
	private final Map<Path, FileAttributes> map;
	private final ScanType deepScan;

	/**
	 * @param executor
	 * @param path
	 * @param map
	 * @param deepScan
	 */
	FileVisit( ExecutorService executor, Path path, Map<Path, FileAttributes> map, ScanType deepScan ) {
		this.executor = executor;
		this.path = path;
		this.map = map;
		this.deepScan = deepScan;
	}

	@Override
	public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
		if( dir != null ) {
			if( dir.endsWith( "$RECYCLE.BIN" ) ||
					dir.endsWith( "Papierkorb" ) ||
					attrs.isSymbolicLink() ||
					attrs.isOther() ) {
//					Debug.PRINT_DEBUG("Skip_preVisitDirectory -> " + dir);
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
		if( attrs.isSymbolicLink() || !attrs.isRegularFile() ) {
			Debug.printDebug( "[FileVisit] Skip_VisitFile -> " + file );
			return FileVisitResult.CONTINUE;
		}
		executor.execute( new FileScan( file, path, map, deepScan, attrs ) );
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed( final Path file, final IOException exc ) throws IOException {
		Debug.printDebug( "[FileVisit] VisitFileFailed -> " + file );
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
