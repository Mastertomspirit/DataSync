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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.google.common.jimfs.Jimfs;

import de.spiritscorp.datasync.io.Debug;

/**
 * Regression test for duplicate flattened filenames when multiple source roots are merged.
 */
class FileAnalyzerCollisionTest {

    /** Static debug mock keeps the test output clean and avoids noisy runtime logs. */
    private MockedStatic<Debug> mockedDebug;

    @BeforeEach
    void setUp() {
        mockedDebug = mockStatic(Debug.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedDebug != null) {
            mockedDebug.close();
        }
    }

    /**
     * Ensures duplicate flattened names receive deterministic unique suffixes before comparison.
     */
    @Test
    void shouldNormalizeDuplicateFlatScanNamesBeforeComparison() throws Exception {
        try (FileSystem fileSystem = Jimfs.newFileSystem()) {
            final Path sourceA = fileSystem.getPath("/sourceA");
            final Path sourceB = fileSystem.getPath("/sourceB");
            final Path destination = fileSystem.getPath("/backup");

            Files.createDirectories(sourceA);
            Files.createDirectories(sourceB);
            Files.createDirectories(destination);

            final Path fileA = sourceA.resolve("debug.log");
            final Path fileB = sourceB.resolve("debug.log");
            final Path destinationFile = destination.resolve("debug.log");

            Files.writeString(fileA, "AAAA");
            Files.writeString(fileB, "BBBB");
            Files.writeString(destinationFile, "C");

            final Map<Path, FileAttributes> sourceMap = new HashMap<>();
            sourceMap.put(fileA, new FileAttributes(
                    Path.of("debug.log"),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(fileA),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(fileA),
                    Files.size(fileA),
                    null));

            sourceMap.put(fileB, new FileAttributes(
                    Path.of("debug.log"),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(fileB),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(fileB),
                    Files.size(fileB),
                    null));

            final Map<Path, FileAttributes> destMap = new HashMap<>();
            destMap.put(destinationFile, new FileAttributes(
                    Path.of("debug.log"),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(destinationFile),
                    "01.01.2024 00:00:00",
                    Files.getLastModifiedTime(destinationFile),
                    Files.size(destinationFile),
                    null));

            final FileAnalyzer analyzer = new FileAnalyzer();
            final java.lang.reflect.Method method = FileAnalyzer.class.getDeclaredMethod(
                    "normalizeDuplicateFlatNames",
                    Map.class,
                    Map.class);
            method.setAccessible(true);
            method.invoke(analyzer, sourceMap, destMap);

            final List<Path> sourceNames = new ArrayList<>();
            for (final FileAttributes attributes : sourceMap.values()) {
                sourceNames.add(attributes.getRelativeFilePath());
            }

            assertAll(
                    () -> assertEquals(2, sourceMap.size(), "Both source files must remain present after normalization."),
                    () -> assertNotEquals(sourceNames.get(0), sourceNames.get(1),
                            "Duplicate names must receive different deterministic suffixes."),
                    () -> assertTrue(sourceNames.stream().allMatch(path -> !path.toString().equals("debug.log")),
                            "The raw duplicate filename must not remain unresolved."));
        }
    }
}
