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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class FileHandlerCollisionTest {

    @Test
    void shouldRenameFlattenedFilesWhenMultipleSourcesShareSameName() throws IOException {
        final Path temp = Files.createTempDirectory( "datasync-collision" );

        final Path sourceA = temp.resolve( "Source_A" );
        final Path sourceB = temp.resolve( "Source_B" );
        final Path destination = temp.resolve( "backup" );

        Files.createDirectories( sourceA );
        Files.createDirectories( sourceB );
        Files.createDirectories( destination );

        final Path fileA = sourceA.resolve( "debug.log" );
        final Path fileB = sourceB.resolve( "debug.log" );

        Files.writeString( fileA, "AAAA", StandardOpenOption.CREATE, StandardOpenOption.WRITE );
        Files.writeString( fileB, "BBBB", StandardOpenOption.CREATE, StandardOpenOption.WRITE );

        final Map<Path, FileAttributes> sourceMap = new HashMap<>();
        sourceMap.put( fileA, new FileAttributes( Paths.get( "debug.log" ), "01.01.2024 00:00:00", Files.getLastModifiedTime( fileA ), "01.01.2024 00:00:00",
                Files.getLastModifiedTime( fileA ), Files.size( fileA ), null ) );
        sourceMap.put( fileB, new FileAttributes( Paths.get( "debug.log" ), "01.01.2024 00:00:00", Files.getLastModifiedTime( fileB ), "01.01.2024 00:00:00",
                Files.getLastModifiedTime( fileB ), Files.size( fileB ), null ) );

        final FileHandler handler = new FileHandler( null );
        final Path targetA = handler.resolveCopyTarget( fileA, sourceMap.get( fileA ), destination, sourceMap );
        final Path targetB = handler.resolveCopyTarget( fileB, sourceMap.get( fileB ), destination, sourceMap );

        assertTrue( targetA.toString().endsWith( "debug_Source_A.log" ) );
        assertTrue( targetB.toString().endsWith( "debug_Source_B.log" ) );
    }
}
