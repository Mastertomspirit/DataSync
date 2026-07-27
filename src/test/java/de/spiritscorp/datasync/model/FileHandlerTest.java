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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.Logger;

/**
 * High-fidelity corporate-grade test suite for {@link FileAnalyzer}.
 * <p>
 * This suite exhaustively maps out every operational boundary, potential filesystem failure,
 * path composition logic, thread state mutation, and edge case scenario for the application's
 * core file synchronization engine.
 * </p>
 * <p>
 * <b>Testing Strategy:</b>
 * <ul>
 * <li><b>Jimfs (Virtual RAM Storage):</b> Used to evaluate correct path composition, resolution
 * hierarchies, and structural state transformations without altering host hardware.</li>
 * <li><b>Mockito Static Mocking:</b> Deployed to explicitly inject hardware faults, simulated
 * OS file locks, runtime IOExceptions, and volatile thread lifecycle interrupts.</li>
 * </ul>
 * </p>
 *
 * @author Tom Spirit
 * @version 2.0.0
 */
@SuppressWarnings( {
		"PMD.LooseCoupling", // Allowed in tests to verify specific implementation types like ArrayList or HashMap
		"PMD.UseConcurrentHashMap", // Standard HashMaps are intentional here to test sequential thread-concurrency isolation
		"PMD.LongVariable", // Descriptive, expressive variable names are prioritized over brevity in this test suite
		"PMD.ExcessiveImports" // Required to bring in both Mockito static subsystems and Google Jimfs structures
} )
class FileHandlerTest {

	private MockedStatic<Debug> mockedDebug; // NOPMD
	private Logger mockedLogger; // NOPMD
	private FileHandler fileHandler; // NOPMD

	private static final String LOG_COPY = "kopiert"; // NOPMD
	private static final String LOG_COPY_FAILED = "FEHLER BEIM KOPIEREN"; // NOPMD
	private static final String LOG_COPY_UNWRITABLE = "SCHREIBSCHUTZ BEIM KOPIEREN"; // NOPMD
	private static final String LOG_DELETE = "gelöscht"; // NOPMD
	private static final String LOG_DELETE_FAILED = "FEHLER BEIM LÖSCHEN"; // NOPMD
	private static final String LOG_DELETE_UNWRITABLE = "SCHREIBSCHUTZ BEIM LÖSCHEN"; // NOPMD
	private static final String LOG_COULD_NOT_MOVE_TO_TRASHBIN = "FEHLER BEIM VERSCHIEBEN IN DEN PAPIERKORB"; // NOPMD

	@BeforeEach
	void setUp() {
		mockedDebug = mockStatic( Debug.class );
		mockedLogger = mock( Logger.class );
		fileHandler = new FileHandler( mockedLogger );
	}

	@AfterEach
	void tearDown() {
		if( mockedDebug != null )
			mockedDebug.close();
	}

	// =========================================================================
	// 1. DELETE FILES - EXTREME SCENARIOS
	// =========================================================================

	/**
	 * Validates runtime lifecycle thread interruption defenses uniformly across all core transaction methods.
	 * <br>
	 * <br>
	 * This execution safety test arms the interrupt flag of the current thread context immediately before
	 * invoking the processing targets. It ensures that the file engine components proactively inspect
	 * thread status matrices and immediately abort processing loops. This prevents partial disk mutations
	 * or dirty memory allocations during application shutdown scenarios.
	 *
	 * @param operation the specific internal system method execution pipeline branch under test ("delete", "copy", "list")
	 * @throws IOException if a nested system component unexpected failure escapes the immediate lifecycle termination guard
	 */
	@ParameterizedTest( name = "1.{index} Lifecycle Interruption Guard -> Operation: {0}" )
	@ValueSource( strings = { "delete", "copy", "list" } )
	@DisplayName( "1. Lifecycle Interruption Guard" )
	void threadInterruptionGuardAllMethodsTest( final String operation ) throws IOException {
		final Path mockPath = mock( Path.class );
		final ArrayList<Path> pathList = new ArrayList<>();
		pathList.add( mockPath );
		final FileAttributes mockAttr = mock( FileAttributes.class );
		final Map<Path, FileAttributes> targetMap = new HashMap<>();
		targetMap.put( mockPath, mockAttr );
		final String payload = "Test";
		when( mockPath.toString() ).thenReturn( payload );

		// Actively arm the current worker thread context interrupt flag state directly
		Thread.currentThread().interrupt();

		try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class ) ) {
			switch( operation ) {
				case "delete" -> fileHandler.deleteFiles( targetMap, false, false, null );
				case "copy" -> fileHandler.copyFiles( targetMap, false, mock( Path.class ) );
				case "list" -> {
					targetMap.clear();
					fileHandler.listFiles( pathList, targetMap, ScanType.FLAT_SCAN, false );
				}
				default -> {
				}
			}

			assertAll( "Verifiziere sofortigen Abbruch der Verarbeitung bei gesetztem Interrupt-Flag",
					() -> mockedDebug.verify( () -> Debug.printDebug( contains( "[File Handler] Interrupt! " ), eq( payload ) ), times( 1 ) ),
					() -> staticFilesMock.verify( () -> Files.delete( any( Path.of( "" ).getClass() ) ), never() ),
					() -> staticFilesMock.verify( () -> Files.copy( any( Path.of( "" ).getClass() ), any() ), never() ),
					() -> staticFilesMock.verify( () -> Files.walkFileTree( any( Path.of( "" ).getClass() ), any() ), never() ),
					() -> assertTrue( targetMap.isEmpty(), "Collections müssen auch bei Abbruch bereinigt werden" ) );
		}finally {
			// Clear interrupt status to restore environmental baselines for sequential test runners
			Thread.interrupted();
		}
	}

	/**
	 * Evaluates standard multi-permutation execution variables for bulk deletion routines.
	 * Validates map clearing behaviors alongside optional execution metrics printing.
	 *
	 * @param useTrashbin Determines whether the file should be moved to the trash layout first
	 * @param logOn       Controls whether the status report flag is processed post-execution
	 */
	@ParameterizedTest( name = "2.{index} -> deleteFiles variations [Trashbin: {0}, LogOn: {1}]" )
	@CsvSource( {
			"false, false",
			"false, true",
			"true,  false",
			"true,  true"
	} )
	@DisplayName( "2. Delete Files variations of Trashbin and LogOn" )
	void deleteFilesMatrixScenariosTest( final boolean useTrashbin, final boolean logOn ) throws IOException { // NOPMD -> CognitiveComplexity
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			final String fileName = "dump_log.xml";
			final Path sourceDir = fileSystem.getPath( "/var/storage/source" );
			final Path trashDir = fileSystem.getPath( "/var/storage/trash" );
			Files.createDirectories( sourceDir );

			final Path fileTarget = sourceDir.resolve( fileName );
			Files.writeString( fileTarget, "Volatile XML Event Log Chunk Data" );

			final FileAttributes attributes = mock( FileAttributes.class );
			when( attributes.getRelativeFilePath() ).thenReturn( fileSystem.getPath( fileName ) );
			when( attributes.getCreateTime() ).thenReturn( FileTime.fromMillis( System.currentTimeMillis() ) );
			if( useTrashbin && logOn ) {
				// Create the trash target as a regular file.
				// This causes Files.createDirectories() to throw a native IOException.
				Files.writeString( trashDir, "Blocker Content" );
			}

			final Map<Path, FileAttributes> executionMap = new HashMap<>();
			executionMap.put( fileTarget, attributes );

			fileHandler.deleteFiles( executionMap, logOn, useTrashbin, trashDir );

			assertAll( "Verify standard batch deletion post-conditions and tracking loops",
					() -> assertTrue( executionMap.isEmpty(), "Operational map must clear completely to prevent memory leaks" ),
					() -> assertFalse( Files.exists( fileTarget ), "Target file must be permanently detached from source node path" ),
					() -> {
						if( useTrashbin && logOn ) {
							verify( mockedLogger, times( 1 ) ).setEntry( eq( fileTarget.toString() ), eq( LOG_COULD_NOT_MOVE_TO_TRASHBIN ), any() );
						}
					},
					() -> {
						if( useTrashbin && !logOn ) {
							assertTrue( Files.exists( trashDir.resolve( fileName ) ), "File payload must be retained inside virtual trash bin directory structure" );
						}else {
							assertFalse( Files.exists( trashDir.resolve( fileName ) ), "File payload shouldend be inside the virtual trash bin directory structure" );
						}
					},
					() -> {
						if( logOn && !useTrashbin ) {
							verify( mockedLogger, times( 1 ) ).setEntry( eq( fileTarget.toString() ), eq( LOG_DELETE ), any() );
							verify( mockedLogger, times( 1 ) ).printStatus();
						}else if( logOn && useTrashbin ) {
							verify( mockedLogger, times( 1 ) ).printStatus();
						}else {
							verify( mockedLogger, never() ).printStatus();
						}
					} );
		}
	}

	/**
	 * Provides a localized multidimensional execution matrix for simulating filesystem deletion boundary faults.
	 *
	 * @return a stream of test arguments containing path definitions, permission configurations, and fault flags
	 */
	private static Stream<Arguments> provideDeleteEdgeCases() {
		return Stream.of(
				Arguments.of( "/mnt/share/ok.txt", false, true, false, LOG_DELETE ),
				Arguments.of( "/mnt/share/locked.dat", false, false, false, LOG_DELETE_UNWRITABLE ),
				Arguments.of( "/mnt/share/corrupt.io", true, true, true, LOG_DELETE_FAILED ) );
	}

	/**
	 * Validates the resilience, error isolation, and operational logging recovery mechanisms of the batch deletion pipeline.
	 * <br>
	 * <br>
	 * This parameterized boundary test evaluates distinct exceptional execution branches in isolation. It ensures that
	 * localized faults (e.g., physical write-protection, runtime I/O exceptions) within a single file transaction
	 * do not abort the collective processing loop. Additionally, it verifies that tracking maps are systematically
	 * flushed to prevent stale architectural memory states.
	 *
	 * @param pathStr            the theoretical absolute file path layout being processed
	 * @param canWrite           the initial write-permission state configuration of the target file
	 * @param setWritableSuccess the operational outcome simulation when attempting a forced permission override
	 * @param throwException     flag forcing the underlying OS layer abstraction to throw an explicit I/O exception
	 * @param expectedLogStatus  the exact status string key expected to be recorded in the persistent telemetry logging framework
	 * @throws IOException if an unhandled underlying hardware or file-locking allocation fault occurs
	 */
	@ParameterizedTest( name = "3.{index} -> deleteFiles isolated scenario for: {0}" )
	@MethodSource( "provideDeleteEdgeCases" )
	@DisplayName( "3. Delete Files isolated scenario" )
	void deleteFilesIsolatedScenariosTest( final String pathStr, final boolean canWrite, final boolean setWritableSuccess,
			final boolean throwException, final String expectedLogStatus ) throws IOException {
		final Path mockPath = mock( Path.class );
		final FileAttributes mockAttr = mock( FileAttributes.class );

		when( mockPath.toString() ).thenReturn( pathStr );

		final Map<Path, FileAttributes> executionMap = new HashMap<>();
		executionMap.put( mockPath, mockAttr );

		try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class ) ) {
			staticFilesMock.when( () -> Files.exists( eq( mockPath ), any() ) ).thenReturn( true );
			staticFilesMock.when( () -> Files.isWritable( mockPath ) ).thenReturn( canWrite );
			final DosFileAttributeView mockDos = setWritableSuccess ? mock( DosFileAttributeView.class ) : null;

			staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockPath ), eq( DosFileAttributeView.class ), any() ) ).thenReturn( mockDos );

			if( throwException )
				staticFilesMock.when( () -> Files.delete( mockPath ) ).thenThrow( new IOException( "Simulated device link failure" ) );

			fileHandler.deleteFiles( executionMap, true, false, null );

			assertAll( "Verify isolated error interception mechanics and tracking map flushing",
					() -> verify( mockedLogger, times( 1 ) ).setEntry( pathStr, expectedLogStatus, mockAttr ),
					() -> assertTrue( executionMap.isEmpty(), "Operational tracking map must be cleared down upon loop completion" ) );
		}
	}

	/**
	 * Validates resilience against internal structural anomalies (null map inputs),
	 * permission workarounds (read-only file adjustments), and complex hardware or network drops
	 * during the removal pipeline.
	 */
	@Test
	@DisplayName( "4. Delete Files handling permissions, exceptions, and null entries" )
	void deleteFilesBoundaryFaultInterceptionTest() throws IOException {
		// Scenario A: DOS (Windows) write elevation succeeds
		final Path mockPathSuccess = mock( Path.class );
		final FileAttributes mockAttrSuccess = mock( FileAttributes.class );
		when( mockPathSuccess.toString() ).thenReturn( "/mnt/share/ok.txt" );

		// Scenario B: POSIX (macOS/Linux) write elevation succeeds
		final Path mockPathPosixSuccess = mock( Path.class );
		final FileAttributes mockAttrPosixSuccess = mock( FileAttributes.class );
		when( mockPathPosixSuccess.toString() ).thenReturn( "/mnt/share/posix_ok.dat" );

		// Scenario C: Both permission elevation strategies fail (File remains locked)
		final Path mockPathLock = mock( Path.class );
		final FileAttributes mockAttrLock = mock( FileAttributes.class );
		when( mockPathLock.toString() ).thenReturn( "/mnt/share/locked.dat" );

		// Scenario D: File is writable but triggers a critical I/O hardware drop during deletion
		final Path mockPathException = mock( Path.class );
		final FileAttributes mockAttrException = mock( FileAttributes.class );
		when( mockPathException.toString() ).thenReturn( "/mnt/share/corrupt.io" );

		final Map<Path, FileAttributes> executionMap = new HashMap<>();
		executionMap.put( mockPathSuccess, mockAttrSuccess );
		executionMap.put( mockPathPosixSuccess, mockAttrPosixSuccess );
		executionMap.put( mockPathLock, mockAttrLock );
		executionMap.put( mockPathException, mockAttrException );
		executionMap.put( mock( Path.class ), null ); // Edge Case: Map contains a null entry value

		try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class ) ) {
			staticFilesMock.when( () -> Files.isWritable( mockPathSuccess ) ).thenReturn( false );
			staticFilesMock.when( () -> Files.isWritable( mockPathPosixSuccess ) ).thenReturn( false );
			staticFilesMock.when( () -> Files.isWritable( mockPathLock ) ).thenReturn( false );
			staticFilesMock.when( () -> Files.isWritable( mockPathException ) ).thenReturn( true );

			// 1. Stubbing for Scenario A: DOS View Activation
			final DosFileAttributeView mockDos = mock( DosFileAttributeView.class );
			staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockPathSuccess ), eq( DosFileAttributeView.class ), any() ) ).thenReturn( mockDos );

			// 2. Stubbing for Scenario B: Complex POSIX Nested Chain Evaluation
			final PosixFileAttributeView mockPosix = mock( PosixFileAttributeView.class );
			final PosixFileAttributes mockPosixAttrs = mock( PosixFileAttributes.class );
			when( mockPosix.readAttributes() ).thenReturn( mockPosixAttrs );
			when( mockPosixAttrs.permissions() ).thenReturn( Set.of( PosixFilePermission.OWNER_READ ) );

			staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockPathPosixSuccess ), eq( PosixFileAttributeView.class ), any() ) ).thenReturn( mockPosix );
			staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockPathLock ), any(), any() ) ).thenReturn( null );
			staticFilesMock.when( () -> Files.exists( any(), any() ) ).thenReturn( true );
			staticFilesMock.when( () -> Files.delete( mockPathException ) ).thenThrow( new IOException( "Simulated device link failure" ) );

			fileHandler.deleteFiles( executionMap, true, false, null );

			assertAll( "Verify deep permission workarounds and defensive logging output metrics",
					// 1x ok.txt(Dos) 2x posix_ok.dat 2x locked.dat = 5 times
					() -> staticFilesMock.verify( () -> Files.getFileAttributeView( any(), any(), any() ), times( 5 ) ),
					() -> staticFilesMock.verify( () -> Files.delete( mockPathSuccess ), times( 1 ) ),
					() -> staticFilesMock.verify( () -> Files.delete( mockPathPosixSuccess ), times( 1 ) ),
					() -> staticFilesMock.verify( () -> Files.delete( mockPathLock ), never() ),
					() -> verify( mockPosix, times( 1 ) ).setPermissions( any() ),
					() -> verify( mockDos, times( 1 ) ).setReadOnly( false ),
					() -> verify( mockedLogger, times( 1 ) ).setEntry( "/mnt/share/ok.txt", LOG_DELETE, mockAttrSuccess ),
					() -> verify( mockedLogger, times( 1 ) ).setEntry( "/mnt/share/posix_ok.dat", LOG_DELETE, mockAttrPosixSuccess ),
					() -> verify( mockedLogger, times( 1 ) ).setEntry( "/mnt/share/locked.dat", LOG_DELETE_UNWRITABLE, mockAttrLock ),
					() -> verify( mockedLogger, times( 1 ) ).setEntry( "/mnt/share/corrupt.io", LOG_DELETE_FAILED, mockAttrException ),
					() -> assertTrue( executionMap.isEmpty(), "Context tracking map must be cleared down regardless of single internal element failures" ) );
		}
	}

	// =========================================================================
	// 2. COPY FILES - PATH COMPOSITION & EXTREME SCENARIOS
	// =========================================================================

	private static Stream<Arguments> provideCopyLayoutsAndSystems() {
		return Stream.of(
				// Format: Relative Path, TargetPreExists, JimfsConfiguration
				Arguments.of( "flat_file.txt", false, Configuration.unix() ),
				Arguments.of( "flat_file.txt", true, Configuration.unix() ),
				Arguments.of( "sub/folder/nested_file.bin", false, Configuration.unix() ),
				Arguments.of( "sub/folder/nested_file.bin", true, Configuration.unix() ),
				// Windows equivalents using backslash/drive semantics natively under the hood
				Arguments.of( "flat_file.txt", false, Configuration.windows() ),
				Arguments.of( "sub\\folder\\nested_file.bin", false, Configuration.windows() ),
				Arguments.of( "deep\\level\\nodes\\item.assets", true, Configuration.windows() ) );
	}

	/**
	 * Validates deep file replication capabilities across complex, multi-tiered nested directory structures.
	 * <br>
	 * <br>
	 * Utilizing an in-memory virtual Unix file topology provided by {@link Jimfs}, this test replicates
	 * precise path calculations and permission assertions. It validates that the replication engine
	 * transparently creates missing parent directories on the fly and maintains total data stream integrity.
	 *
	 * @param relativePathStr          the multi-tiered nested layout configuration mapping the path structure
	 * @param targetDirectoryPreExists state configuration indicating whether the target structural tree already exists
	 * @throws IOException if data payload streaming or target directory node allocation fails
	 */
	@ParameterizedTest( name = "5. {index} -> copyFiles Layout: \"{0}\" | targetExists: {1}" )
	@MethodSource( "provideCopyLayoutsAndSystems" )
	@DisplayName( "5. Copy Files validates multi-level path resolution and pre-existing target directory layouts" )
	void copyFilesPathResolutionAndDirectoryStructureTest( final String relativePathStr, final boolean targetDirectoryPreExists, final Configuration openOsConfig ) throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( openOsConfig ) ) {
			final String dataPayload = "Data payload sync stream data payload";

			// Dynamic root resolution: Works for Unix ("/") and Windows ("C:\")
			final Path root = fileSystem.getRootDirectories().iterator().next();
			final Path sourceRoot = root.resolve( "source/pool" );
			final Path destinationRoot = root.resolve( "destination/pool" );
			Files.createDirectories( sourceRoot );

			if( targetDirectoryPreExists ) Files.createDirectories( destinationRoot );

			// Prepare source path structure dynamically including nested parent nodes
			final Path sourceFile = sourceRoot.resolve( relativePathStr );
			Files.createDirectories( sourceFile.getParent() );
			Files.writeString( sourceFile, dataPayload );

			final FileAttributes mockAttr = mock( FileAttributes.class );
			final FileTime fileTime = FileTime.fromMillis( System.currentTimeMillis() );
			when( mockAttr.getRelativeFilePath() ).thenReturn( fileSystem.getPath( relativePathStr ) );
			when( mockAttr.getCreateTime() ).thenReturn( fileTime );

			final Map<Path, FileAttributes> jobMap = new HashMap<>();
			jobMap.put( sourceFile, mockAttr );

			// Act
			fileHandler.copyFiles( jobMap, false, destinationRoot );

			// Assert
			final Path targetExpectedPath = destinationRoot.resolve( relativePathStr );
			assertAll( "Verify deep path replication routing and automatic target structure generation",
					() -> assertTrue( Files.exists( targetExpectedPath ), "Missing target directory hierarchies must resolve automatically" ),
					() -> assertEquals( dataPayload, Files.readString( targetExpectedPath ), "Payload integrity verified" ),
					() -> assertEquals( fileTime, Files.getAttribute( targetExpectedPath, "creationTime" ), "Create Time is not as expected" ),
					() -> assertTrue( jobMap.isEmpty(), "Job task map cache must be cleanly flushed upon execution success" ) );
		}
	}

	/** Defines the execution profiles used to inject targeted boundary faults and operational edge cases into file replication transaction tests. */
	enum CopyFaultScenario {
		COPY_SUCCESS,
		UNEXPECTED_FILESYSTEM,
		PERMISSION_FAULT_POSIX,
		PERMISSION_FAULT_DOS,
		COPY_IO_ERROR;
	}

	/**
	 * Verifies that {@link FileHandler#copyFiles} gracefully intercepts and isolates boundary faults—such
	 * as unwritable targets, OS-specific permission constraints (POSIX/DOS), or mid-stream I/O failures—and
	 * appropriately registers the corresponding diagnostic states without disrupting the broader replication lifecycle.
	 *
	 * @param scenario the designated transactional failure profile under evaluation
	 * @throws IOException if an unhandled file system interaction escapes the isolated boundary fault zone
	 */
	@ParameterizedTest( name = "6.{index} -> Copy Files Interception: {0}" )
	@EnumSource( CopyFaultScenario.class )
	@DisplayName( "6. Copy Files gracefully intercepts boundary faults and registers failure states" )
	void copyFilesBoundaryFaultInterceptionTest( final CopyFaultScenario scenario ) throws IOException {
		final Path mockSrcPath = mock( Path.class );
		final Path mockDestPathBase = mock( Path.class );
		final Path mockResolvedDestPath = mock( Path.class );
		final Path mockParentPath = mock( Path.class );
		final FileAttributes mockAttr = mock( FileAttributes.class );

		// Setup deterministic path layout responses
		when( mockAttr.getRelativeFilePath() ).thenReturn( Path.of( "target_locked.dat" ) );
		when( mockDestPathBase.resolve( any( Path.class ) ) ).thenReturn( mockResolvedDestPath );
		when( mockResolvedDestPath.getParent() ).thenReturn( mockParentPath );
		when( mockResolvedDestPath.toString() ).thenReturn( "/err/target_locked.dat" );

		// Populate active transactional execution registry queue
		final Map<Path, FileAttributes> jobMap = new HashMap<>();
		jobMap.put( mockSrcPath, mockAttr );
		jobMap.put( mock( Path.class ), null );

		// Diese Variable hält dynamisch die erwartete Log-Meldung für die Assertions bereit
		final String expectedLogMessage;

		try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class ) ) {
			staticFilesMock.when( () -> Files.exists( eq( mockParentPath ), any() ) ).thenReturn( true );
			staticFilesMock.when( () -> Files.exists( eq( mockResolvedDestPath ), any() ) ).thenReturn( true );

			switch( scenario ) { // NOPMD
				case COPY_SUCCESS -> {
					expectedLogMessage = LOG_COPY;
					staticFilesMock.when( () -> Files.isWritable( mockResolvedDestPath ) ).thenReturn( true );
				}
				case UNEXPECTED_FILESYSTEM -> {
					expectedLogMessage = LOG_COPY_UNWRITABLE;
					staticFilesMock.when( () -> Files.isWritable( mockResolvedDestPath ) ).thenReturn( false );
					staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockResolvedDestPath ), any(), any() ) ).thenReturn( null );
				}
				case PERMISSION_FAULT_POSIX -> {
					expectedLogMessage = LOG_COPY_UNWRITABLE;
					staticFilesMock.when( () -> Files.isWritable( mockResolvedDestPath ) ).thenReturn( false );
					final PosixFileAttributeView mockPosixView = mock( PosixFileAttributeView.class );
					staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockResolvedDestPath ), eq( PosixFileAttributeView.class ), any() ) ).thenReturn( mockPosixView );
					final Set<PosixFilePermission> permissions = EnumSet.noneOf( PosixFilePermission.class );
					permissions.add( PosixFilePermission.OWNER_WRITE );
					final PosixFileAttributes mockAttribute = mock( PosixFileAttributes.class );
					when( mockPosixView.readAttributes() ).thenReturn( mockAttribute );
					when( mockAttribute.permissions() ).thenReturn( permissions );
				}
				case PERMISSION_FAULT_DOS -> {
					expectedLogMessage = LOG_COPY_UNWRITABLE;
					staticFilesMock.when( () -> Files.isWritable( mockResolvedDestPath ) ).thenReturn( false );
					final DosFileAttributeView mockDosView = mock( DosFileAttributeView.class );
					staticFilesMock.when( () -> Files.getFileAttributeView( eq( mockResolvedDestPath ), eq( DosFileAttributeView.class ), any() ) ).thenReturn( mockDosView );
					doThrow( new IOException( "DOS permission table lock modification denied" ) ).when( mockDosView ).setReadOnly( false );
				}
				case COPY_IO_ERROR -> {
					expectedLogMessage = LOG_COPY_FAILED;
					staticFilesMock.when( () -> Files.isWritable( mockResolvedDestPath ) ).thenReturn( true );
					staticFilesMock.when( () -> Files.copy( any( Path.class ), any( Path.class ), any(), any() ) ).thenThrow( new IOException( "Disk target space exhausted midway" ) );
				}
				default -> throw new IllegalArgumentException( "Unexpected boundary fault type injection: " + scenario.toString() );
			}

			// Act
			fileHandler.copyFiles( jobMap, true, mockDestPathBase );

			// Assert
			assertAll( "Verify that replication breaks safely before invoking illegal filesystem manipulations",
					() -> {
						if( CopyFaultScenario.UNEXPECTED_FILESYSTEM == scenario ||
								CopyFaultScenario.PERMISSION_FAULT_POSIX == scenario ||
								CopyFaultScenario.PERMISSION_FAULT_DOS == scenario ) {
							staticFilesMock.verify( () -> Files.copy( any( Path.class ),
									any( Path.class ),
									eq( StandardCopyOption.REPLACE_EXISTING ),
									eq( StandardCopyOption.COPY_ATTRIBUTES ) ), never() );
						}else if( CopyFaultScenario.COPY_SUCCESS == scenario ) {
							staticFilesMock.verify( () -> Files.copy( any( Path.class ), any( Path.class ), any(), any() ), times( 1 ) );
						}
					},
					() -> verify( mockedLogger, times( 1 ) ).setEntry( "/err/target_locked.dat", expectedLogMessage, mockAttr ),
					() -> assertTrue( jobMap.isEmpty(), "The transactional engine queue map must be forcefully flushed" ) );
		}
	}

	// =========================================================================
	// 3. LIST FILES - ASYNCHRONOUS WALKING & CONCURRENCY
	// =========================================================================

	/**
	 * Exhaustively checks the Base Directory Resolution logic.
	 * <br>
	 * This parameterized test evaluates how the engine shifts its base anchor path depending
	 * on the {@code subDir} configuration flag. Instead of fragile static mocking, it uses
	 * a live in-memory filesystem context to verify that the underlying visitor correctly computes
	 * shifted relative file tracks within the final result map structure.
	 *
	 * @param subDirFlag           controls whether the target scanning profile isolates subdirectories
	 * @param expectedRelativePath the calculated relative path segment topology expected in the output map
	 * @throws IOException if virtual filesystem allocations or directory creations fail
	 */
	@ParameterizedTest( name = "7. {index} -> listFiles path tracking strategy [subDir: {0}]" )
	@CsvSource( {
			"false, file.txt",
			"true,  workspace_module/file.txt"
	} )
	@DisplayName( "7. List Files adapts structural path tracking strategies across directory walking scenarios" )
	void listFilesDirectoryWalkingScenariosTest( final boolean subDirFlag, final String expectedRelativePath ) throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			// Setup a clear, deterministic directory tree structure in memory
			final Path targetBaseRoot = fileSystem.getPath( "/cloud/sync/workspace_module" );
			final Path sampleFile = targetBaseRoot.resolve( "file.txt" );

			Files.createDirectories( targetBaseRoot );
			Files.writeString( sampleFile, "Target sync stream payload telemetry" );

			final ArrayList<Path> scanRegistryList = new ArrayList<>();
			scanRegistryList.add( targetBaseRoot );
			final Map<Path, FileAttributes> outputResultMap = new HashMap<>();

			// Act - Run the actual path traversal loop against the virtual storage pool
			fileHandler.listFiles( scanRegistryList, outputResultMap, ScanType.FLAT_SCAN, subDirFlag );

			// Assert - Verify that the internal baseDir shift maps perfectly into the tracking attributes
			assertAll( "Verify architectural edge-path evaluation layers and framework tracking links",
					() -> assertEquals( 1, outputResultMap.size(), "The operation container must catch and register exactly one active file node" ),
					() -> assertTrue( outputResultMap.containsKey( sampleFile ), "The absolute file system path node pointer must be present as a key lookup index" ),
					() -> assertEquals( fileSystem.getPath( expectedRelativePath ), outputResultMap.get( sampleFile ).getRelativeFilePath(),
							"The calculated relative path layout configuration must match the targeted tracking strategy matrix" ) );
		}
	}

	/**
	 * Verifies that {@link FileHandler#listFiles} isolates structural storage faults—such as
	 * unexpected unmounts—on a corrupted drive path and continues processing subsequent healthy
	 * batch paths smoothly.
	 *
	 * @throws IOException if an unexpected I/O error escapes outside the isolated target fault zone
	 */
	@Test
	@DisplayName( "8. List Files isolates structural disk faults and maintains processing batch continuity" )
	void listFilesFileTreeWalkingFaultToleranceTest() throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			final String exceptionPayload = "Hardware link unmounted or disconnected unexpectedly";
			// Setup two distinct roots to simulate a multi-path batch operation
			final Path failingRoot = fileSystem.getPath( "/pool/corrupted_drive" );
			final Path healthyRoot = fileSystem.getPath( "/pool/healthy_drive" );
			final Path healthyFile = healthyRoot.resolve( "intact_asset.txt" );

			Files.createDirectories( failingRoot );
			Files.createDirectories( healthyRoot );
			Files.writeString( healthyFile, "Healthy disk data stream telemetry" );

			final ArrayList<Path> pathRegistry = new ArrayList<>();
			pathRegistry.add( failingRoot );
			pathRegistry.add( healthyRoot ); // Batch pipeline must continue here!
			final Map<Path, FileAttributes> resultsMap = new HashMap<>();

			// Use CALLS_REAL_METHODS so Jimfs works natively for the healthy parts
			try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class, CALLS_REAL_METHODS ) ) {
				// Inject a severe structural I/O fault ONLY on the first registry path
				staticFilesMock.when( () -> Files.walkFileTree( eq( failingRoot ), any() ) ).thenThrow( new IOException( exceptionPayload ) );

				// Act - Execute batch processing loop
				fileHandler.listFiles( pathRegistry, resultsMap, ScanType.FLAT_SCAN, false );

				// Assert
				assertAll( "Verify that structural disk faults are isolated cleanly without crashing parallel processing lanes",
						// 1. Verify the exception inside walkTree was caught and routed to the debug frame
						() -> mockedDebug.verify( () -> Debug.printDebug(
								contains( "[File Handler Error] Error on walking directory path: %s with message: %s" ),
								eq( failingRoot.toString() ),
								eq( exceptionPayload ) ),
								times( 1 ) ),
						// 2. The failure must not clear or prevent population of healthy scan segments
						() -> assertEquals( 1, resultsMap.size(), "The results map must contain exactly one entry from the healthy directory segment" ),
						// 3. Ensure the healthy file asset was safely resolved
						() -> assertTrue( resultsMap.containsKey( healthyFile ), "The tracking index must contain the verified file from the operational partition" ) );
			}
		}
	}

	/**
	 * Verify entry guards and execution structures inside directories listing sweeps.
	 */
	@Test
	@DisplayName( "9. List Files ignores non-existent system base path targets silently" )
	void listFilesBypassesNonExistentPathsTest() {
		final Path deadPath = mock( Path.class );
		when( deadPath.toString() ).thenReturn( "/volumes/dev_null_offline" );

		final ArrayList<Path> searchTargets = new ArrayList<>();
		searchTargets.add( deadPath );
		final Map<Path, FileAttributes> outputMap = new HashMap<>();

		try( MockedStatic<Files> staticFilesMock = mockStatic( Files.class ) ) {
			// Simulate structural path visibility failure
			staticFilesMock.when( () -> Files.exists( deadPath ) ).thenReturn( false );

			// Act
			fileHandler.listFiles( searchTargets, outputMap, ScanType.FLAT_SCAN, false );

			// Assert
			assertAll( "Verify dead execution segments drop early before activating file walker routines",
					() -> staticFilesMock.verify( () -> Files.walkFileTree( eq( deadPath ), any() ), never() ),
					() -> assertTrue( outputMap.isEmpty(), "Output configuration maps must remain blank when target volumes are offline" ) );
		}
	}

	@SuppressWarnings( { "PMD.AvoidDuplicateLiterals" } )
	private static Stream<Arguments> provideCrossPlatformNormalizationScenarios() {
		return Stream.of(
				// Format: OS-Config, Raw Base Expression, Relative File Segment, SubDir-Flag, Expected Clean Relative Path
				Arguments.of( Configuration.unix(), "cloud/./sync/../sync", "nested/file.log", false, "nested/file.log", "/cloud/sync/nested/file.log" ),
				Arguments.of( Configuration.unix(), "cloud/./sync/../sync", "nested/file.log", true, "sync/nested/file.log", "/cloud/sync/nested/file.log" ),
				Arguments.of( Configuration.unix(), "cloud/./sync/../sync/a/b/c/d/e/f/g/../../../", "nested/file.log",
						true, "d/nested/file.log", "/cloud/sync/a/b/c/d/nested/file.log" ),
				// Windows equivalents demonstrating dirty token cleanup and native backslash handling
				Arguments.of( Configuration.windows(), "data\\.\\sync\\..\\sync", "nested\\file.log", false, "nested\\file.log", "C:\\data\\sync\\nested\\file.log" ),
				Arguments.of( Configuration.windows(), "data\\.\\sync\\..\\sync", "nested\\file.log", true, "sync\\nested\\file.log", "C:\\data\\sync\\nested\\file.log" ),
				Arguments.of( Configuration.windows(), "data\\.\\sync\\..\\sync\\..\\workspace\\to\\path\\go", "nested\\file.log",
						true, "go\\nested\\file.log", "C:\\data\\workspace\\to\\path\\go\\nested\\file.log" ) );
	}

	/**
	 * Validates the file scanner's ability to cleanly resolve and eliminate redundant path segments
	 * (such as self-references '.' and parent-references '..') across cross-platform topologies.
	 * <br>
	 * <br>
	 * This test ensures that dirty input paths do not pollute the internal tracking registry with
	 * raw token fragments, guaranteeing pristine relative path metrics regardless of the host OS semantics.
	 *
	 * @param openOsConfig              the virtual operating system configuration provided via Jimfs
	 * @param baseExpression            the un-normalized root-relative directory sequence to be registered for scanning
	 * @param relativeFileSeg           the structural layout of the actual payload file node
	 * @param subDirFlag                toggle strategy indicating if the immediate base directory name should be preserved
	 * @param expectedCleanRelativePath the expected clean, normalized relative outcome for tracking database keys
	 * @param expectedCleanFullPath     the expected clean, normalized full outcome for tracking database keys
	 * @throws IOException if physical in-memory directory allocation or file streaming operations fail
	 */
	@ParameterizedTest( name = "10.{index} -> listFiles Normalization [OS: {0} | SubDir: {3}]" )
	@MethodSource( "provideCrossPlatformNormalizationScenarios" )
	@DisplayName( "10. List Files purges redundant path segments and normalizes cross-platform tracking layouts" )
	void listFilesCrossPlatformPathNormalizationAndTrackingTest(
			final Configuration openOsConfig,
			final String baseExpression,
			final String relativeFileSeg,
			final boolean subDirFlag,
			final String expectedCleanRelativePath,
			final String expectedCleanFullPath ) throws IOException {

		try( FileSystem fileSystem = Jimfs.newFileSystem( openOsConfig ) ) {
			// Resolve native system root dynamically (e.g., "/" or "C:\")
			final Path systemRoot = fileSystem.getRootDirectories().iterator().next();

			// Intentionally build a "dirty", un-normalized base tracking path
			final Path rawBaseDirectory = systemRoot.resolve( baseExpression );
			final Path sampleFileTarget = rawBaseDirectory.resolve( relativeFileSeg );

			// Create paths (NIO handles un-normalized structural creations natively)
			Files.createDirectories( sampleFileTarget.getParent() );
			Files.writeString( sampleFileTarget, "Edge-case synchronization stream metadata payload" );

			final List<Path> scanRegistryList = new ArrayList<>();
			scanRegistryList.add( rawBaseDirectory );
			final Map<Path, FileAttributes> outputResultMap = new HashMap<>();

			// Act - Trigger directory walker loop
			fileHandler.listFiles( scanRegistryList, outputResultMap, ScanType.FLAT_SCAN, subDirFlag );

			// Fetch the attributes profile dynamically via the registered absolute path pointer
			final Path registeredAbsoluteKey = outputResultMap.keySet().iterator().next();
			final FileAttributes trackedAttributes = outputResultMap.get( registeredAbsoluteKey );

			assertAll( "Ensure the file was found despite the dirty lookup index",
					() -> assertEquals( 1, outputResultMap.size(), "The registry container must catch exactly one active file node" ),
					() -> assertEquals( expectedCleanRelativePath, trackedAttributes.getRelativeFilePath().toString(),
							"The internal tracking layout must be fully normalized, stripping all relative directory tokens" ),
					() -> assertEquals( expectedCleanFullPath, registeredAbsoluteKey.toString(),
							"The internal tracking layout must be fully normalized, stripping all relative directory tokens" ) );
		}
	}

	/**
	 * Performs a comprehensive integration-level validation of the listFiles loop topology.
	 * <br>
	 * <br>
	 * This integration test builds a virtual, multi-tiered file infrastructure inside an in-memory
	 * filesystem using Jimfs. It populates excluded system trash folders with dummy files and ensures that the
	 * underlying {@code FileVisit} state engine entirely bypasses these subtrees via {@code SKIP_SUBTREE}.
	 * Finally, it validates that exactly one valid entry reaches the final tracking map after
	 * the internal asynchronous processing engine safely terminates.
	 *
	 * @throws IOException if virtual I/O node allocations or filesystem constraints fail
	 */
	@Test
	@DisplayName( "11. Integration -> List Files full lifecycle data population and folder filtering evaluation" )
	void listFilesFullIntegrationWithJimfsTest() throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			// Setup target filesystem directory nodes
			final Path root = fileSystem.getPath( "/data" );
			final Path recycleBin = root.resolve( "$RECYCLE.BIN" );
			final Path germanTrash = root.resolve( "Papierkorb" );
			final Path validDir = root.resolve( "documents" );

			// Define active files and nested targets inside the excluded paths
			final Path activeFile = validDir.resolve( "report.pdf" );
			final Path ignoredRecycleFile = recycleBin.resolve( "stale_cache.bak" );
			final Path ignoredTrashFile = germanTrash.resolve( "deleted_invoice.csv" );

			// Physically create structures within the virtual file layout
			Files.createDirectories( recycleBin );
			Files.createDirectories( germanTrash );
			Files.createDirectories( validDir );

			// Write payload streams to verify processing capability
			Files.writeString( activeFile, "Valid binary sync stream payload metrics" );
			Files.writeString( ignoredRecycleFile, "Should be completely unreachable due to SKIP_SUBTREE filtering" );
			Files.writeString( ignoredTrashFile, "Should be completely unreachable due to SKIP_SUBTREE filtering" );

			final ArrayList<Path> searchPaths = new ArrayList<>();
			searchPaths.add( root );
			final Map<Path, FileAttributes> resultMap = new HashMap<>();

			// Act
			// Note: The internal awaitTermination loop blocks synchronously until all background tasks complete
			fileHandler.listFiles( searchPaths, resultMap, ScanType.FLAT_SCAN, false );

			// Assert
			assertAll( "Verify comprehensive file tree traversal filtering and asynchronous data synchronization footprint",
					() -> assertEquals( 1, resultMap.size(),
							"The resulting map must contain exactly 1 entry; system trash subtrees must be bypassed completely" ),
					() -> assertTrue( resultMap.containsKey( activeFile ),
							"The tracking collection must map and contain the valid regular file path 'report.pdf'" ),
					() -> assertNotNull( resultMap.get( activeFile ),
							"The mapped entry value for the valid file must be a fully initialized FileAttributes instance" ),
					() -> assertFalse( resultMap.containsKey( ignoredRecycleFile ),
							"The synchronization tracking layout must explicitly exclude files located inside the $RECYCLE.BIN directory" ),
					() -> assertFalse( resultMap.containsKey( ignoredTrashFile ),
							"The synchronization tracking layout must explicitly exclude files located inside the Papierkorb directory" ) );
		}
	}

	/**
	 * Verifies a fully successful batch copy operation over deep layout paths.
	 * <br>
	 * <br>
	 * This integration test utilizes Jimfs to perform a live end-to-end file copying execution.
	 * It ensures that the replication logic creates missing destination directories on the fly,
	 * preserves total data payload integrity, and cleanly flushes the tracking job map upon success.
	 *
	 * @throws IOException if virtual disk mutations or stream reads fail
	 */
	@Test
	@DisplayName( "12. Integration -> Copy Files successfully replicates file assets and builds target trees" )
	void copyFilesSuccessfulReplicationTest() throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			// Define virtual workspace roots
			final String dataPayload = "CRITICAL_SYNC_PAYLOAD_TOKEN_2026";
			final Path sourceRoot = fileSystem.getPath( "/source/pool" );
			final Path destinationRoot = fileSystem.getPath( "/destination/pool" );

			// Create target file within deep subdirectories to challenge directory generation
			final String relativeLayout = "archive/2026/metrics_report.data";
			final Path sourceFile = sourceRoot.resolve( relativeLayout );

			Files.createDirectories( sourceFile.getParent() );
			Files.writeString( sourceFile, dataPayload );

			// Build mock attributes matching the processed file asset
			final FileAttributes mockAttr = mock( FileAttributes.class );
			when( mockAttr.getRelativeFilePath() ).thenReturn( fileSystem.getPath( relativeLayout ) );
			when( mockAttr.getCreateTime() ).thenReturn( FileTime.fromMillis( System.currentTimeMillis() ) );

			// Populate the active transactional execution registry queue
			final Map<Path, FileAttributes> jobMap = new HashMap<>();
			jobMap.put( sourceFile, mockAttr );

			// Act - Execute the real copy procedure inside the isolated memory landscape
			fileHandler.copyFiles( jobMap, false, destinationRoot );

			// Assert
			final Path expectedTargetFile = destinationRoot.resolve( relativeLayout );
			assertAll( "Verify absolute payload transfer success and environmental structural integrity",
					() -> assertTrue( Files.exists( expectedTargetFile ),
							"The engine must automatically generate missing target directory trees during the copy phase" ),
					() -> assertEquals( dataPayload, Files.readString( expectedTargetFile ),
							"The target file contents must match the source stream data exactly without corruption" ),
					// Since the transaction succeeded, the job map must be flushed to confirm completion tracking
					() -> assertTrue( jobMap.isEmpty(),
							String.format( "The job tracking queue map must be empty after execution success but contained %d items", jobMap.size() ) ) );
		}
	}

	/**
	 * Verifies that {@link FileHandler#deleteFiles} successfully purges regular file assets
	 * from the storage layer and appropriately flushes the tracking job registry map upon completion.
	 *
	 * @throws IOException if an unexpected file system interaction fails during the integration run
	 */
	@Test
	@DisplayName( "13. Integration -> Delete Files successfully purges file assets from disk and flushes job registry" )
	void deleteFilesSuccessfulPurgeIntegrationTest() throws IOException {
		try( FileSystem fileSystem = Jimfs.newFileSystem( Configuration.unix() ) ) {
			// Define virtual asset workspace nodes
			final Path targetRoot = fileSystem.getPath( "/workspace/cleanup_zone" );
			final Path targetFile = targetRoot.resolve( "stale_temporary_cache.tmp" );

			Files.createDirectories( targetRoot );
			Files.writeString( targetFile, "Temporary testing discardable telemetry payload stream" );

			// Build mock attributes associated with the sacrificial file item
			final FileAttributes mockAttr = mock( FileAttributes.class );
			// Populate the active transactional execution registry queue layout
			final Map<Path, FileAttributes> jobMap = new HashMap<>();
			jobMap.put( targetFile, mockAttr );

			// Act - Execute the real low-level purge transaction sequence
			fileHandler.deleteFiles( jobMap, false, false, null );

			// Assert
			assertAll( "Verify absolute file purge execution and operational structural cleanup consistency",
					// 1. Check if the file is completely wiped from the virtual storage block
					() -> assertTrue( Files.notExists( targetFile ), "The file engine must physically wipe the targeted file asset from the storage layer" ),
					// 2. Since the deletion succeeded, the tracking map must be flushed clean
					() -> assertTrue( jobMap.isEmpty(), String.format( "The job tracking queue map must be empty after execution success but contained %d items", jobMap.size() ) ) );
		}
	}
}