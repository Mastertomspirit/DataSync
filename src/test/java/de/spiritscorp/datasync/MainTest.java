package de.spiritscorp.datasync;

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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.spiritscorp.datasync.io.Debug;
import de.spiritscorp.datasync.io.PreferenceManager;

/**
 * Comprehensive unit test suite for the {@link Main} application lifecycle and CLI parser.
 * <p>
 * This class validates argument configuration boundaries, lookahead safeguards, and edge cases.
 * Inter-system boundaries (I/O, preferences subsystem) are isolated using inline static mocking
 * to prevent environment pollution during testing.
 * <p>
 *
 * @author Tom Spirit
 * @version 1.0.0
 * @see Main
 */
// @DisplayName( "Main Application CLI Parser Test Suite" )
class MainTest {

	/** Insulates the static {@link PreferenceManager} subsystem during test execution. */
	private MockedStatic<PreferenceManager> mockPrefManager;

	/** Insulates the static {@link Debug} diagnostics subsystem during test execution. */
	private MockedStatic<Debug> mockDebug;

	/** Insulates the {@link PreferenceManager} instance. */
	private PreferenceManager mockInstance;

	/**
	 * Initializes the static test environment prior to each individual test execution.
	 * <p>
	 * Resets the static application state inside {@link Main} and activates strict static
	 * method intercepts to circumvent underlying physical disk operations.
	 * <p>
	 */
	@BeforeEach
	void setUp() {
		Main.resetForTesting();

		mockPrefManager = Mockito.mockStatic( PreferenceManager.class );
		mockDebug = Mockito.mockStatic( Debug.class );

		mockInstance = Mockito.mock( PreferenceManager.class );
		Mockito.when( mockInstance.getConfigPath() ).thenReturn( Path.of( "/mock/default/path" ) );
		mockPrefManager.when( PreferenceManager::getInstance ).thenReturn( mockInstance );
	}

	/**
	 * Cleans up and releases static framework intercepts after each test execution.
	 * <p>
	 * This ensures no static mock leakages persist into subsequent executions within the JVM.
	 * <p>
	 */
	@AfterEach
	void tearDown() {
		if( mockPrefManager != null ) {
			mockPrefManager.close();
		}
		if( mockDebug != null ) {
			mockDebug.close();
		}
	}

	/**
	 * Verifies that the boot delay sequence flag is correctly parsed and applied globally.
	 *
	 * @param flag The specific syntax variation of the boot delay flag.
	 */
	@ParameterizedTest
	@ValueSource( strings = { "--boot-delay", "-b" } )
	@DisplayName( "1. Should enable boot delay when corresponding flags are provided" )
	void testBootDelayFlags( final String flag ) {
		Main.parseArguments( flag );
		assertAll(
				() -> assertTrue( Main.isFirstStart(), "Boot delay should be active" ),
				() -> assertFalse( Main.isDebug(), "Debug mode should remain disabled" ) );
	}

	/**
	 * Verifies that standard console-bound diagnostic logging flags are evaluated correctly.
	 *
	 * @param flag The specific syntax variation of the console debug flag.
	 */
	@ParameterizedTest
	@ValueSource( strings = { "--debug", "-d" } )
	@DisplayName( "2. Should enable debug mode when console debug flags are provided" )
	void testDebugFlags( final String flag ) {
		Main.parseArguments( flag );

		assertAll(
				() -> assertTrue( Main.isDebug(), "Debug mode should be active" ),
				() -> assertFalse( Main.isFirstStart(), "Boot delay should remain disabled" ),
				() -> mockDebug.verify( () -> Debug.printDebug( any(), any( Object[].class ) ), times( 2 ) ) );
	}

	/**
	 * Verifies that the file redirection flag cascades into an implicit debug activation
	 * and targets the system subsystem correctly.
	 *
	 * @param flag The specific syntax variation of the debug-to-file redirection flag.
	 */
	@ParameterizedTest
	@ValueSource( strings = { "--debug-to-file", "-f" } )
	@DisplayName( "3. Should redirect debug logs to file when file logging flags are provided" )
	void testDebugToFileFlags( final String flag ) {
		Main.parseArguments( flag );

		assertAll(
				() -> assertTrue( Main.isDebug(), "Debug mode should be implicitly active" ),
				() -> mockDebug.verify( Debug::setDebugToFile, times( 1 ) ),
				() -> mockDebug.verify( () -> Debug.printDebug( any(), any( Object[].class ) ), times( 2 ) ) );
	}

	/**
	 * Tests all four structural variations of assigning a custom configuration directory path.
	 *
	 * @param args         The processed array of tokens mimicking a runtime shell command.
	 * @param expectedPath The string value expected to hit the configuration engine.
	 * @param description  Context details outlining the target execution permutation.
	 */
	@ParameterizedTest( name = "{index} ==> Variant: {2}" )
	@MethodSource( "provideConfigDirTestCases" )
	@DisplayName( "4. Should successfully resolve all 4 variations of config directory routing parameters" )
	void testAllConfigDirPermutations( final String[] args, final String expectedPath, final String description ) {
		Main.parseArguments( args );
		verify( mockInstance, times( 1 ) ).initGlobalRootConfigPath( Path.of( expectedPath ) );
	}

	/**
	 * Provides a dynamic matrix mapping out all permitted configuration directory argument variations.
	 *
	 * @return A stream of argument combinations covering key-value assignments.
	 */
	private static Stream<Arguments> provideConfigDirTestCases() {
		final String testPath = "/var/log/sync";
		return Stream.of(
				Arguments.of( new String[] { "--config-dir=" + testPath }, testPath, "Long flag with equals" ),
				Arguments.of( new String[] { "-c=" + testPath }, testPath, "Short flag with equals" ),
				Arguments.of( new String[] { "--config-dir", testPath }, testPath, "Long flag with space lookahead" ),
				Arguments.of( new String[] { "-c", testPath }, testPath, "Short flag with space lookahead" ) );
	}

	/**
	 * Safeguards the lookahead parser array scope boundary checks when the configuration argument
	 * is provided as the final entry point token with no value to consume.
	 */
	@Test
	@DisplayName( "5. Should gracefully handle configuration flag missing its value token at the array end boundary" )
	void testConfigDirMissingValueAtArrayEnd() {
		assertAll(
				() -> assertDoesNotThrow( () -> Main.parseArguments( "-c" ) ),
				() -> verify( mockInstance, times( 0 ) ).initGlobalRootConfigPath( any() ) );
	}

	/**
	 * Verifies path resolution consistency when confronting OS-specific variations (e.g., Windows backslashes).
	 */
	@Test
	@DisplayName( "6. Should handle complex platform paths containing spaces or backslashes" )
	void testComplexPathFormatting() {
		final String windowsPath = "D:\\App Data\\DataSync Config";
		Main.parseArguments( "-c", windowsPath );

		verify( mockInstance, times( 1 ) ).initGlobalRootConfigPath( Path.of( windowsPath ) );
	}

	/**
	 * Validates that the lookahead isolation loop skips values starting with a hyphen,
	 * preventing standard independent command flags from being parsed as folder strings.
	 */
	@Test
	@DisplayName( "7. Should not consume subsequent flag as configuration directory path" )
	void testConfigDirLookaheadSafeguard() {
		Main.parseArguments( "-c", "--debug" );

		assertAll(
				() -> verify( mockInstance, times( 0 ) ).initGlobalRootConfigPath( any() ),
				() -> assertTrue( Main.isDebug(), "Subsequent flag '--debug' should still be processed" ) );
	}

	/**
	 * Verifies processing order correctness when running combinations of overlapping parameters in a single execution pass.
	 */
	@Test
	@DisplayName( "8. Should handle complex mixed execution arguments gracefully" )
	void testMixedArguments() {
		final String[] args = { "-b", "--config-dir=/custom/path", "-d" };

		Main.parseArguments( args );
		assertAll(
				() -> assertTrue( Main.isFirstStart(), "Boot delay should be enabled" ),
				() -> assertTrue( Main.isDebug(), "Debug logging should be enabled" ),
				() -> verify( mockInstance, times( 1 ) ).initGlobalRootConfigPath( Path.of( "/custom/path" ) ) );
	}

	/**
	 * Verifies system resilience and isolation patterns when confronting unknown, non-standard flags
	 * or structural variants like {@code -m} or {@code --config-file}.
	 */
	@Test
	@DisplayName( "9. Should gracefully pass over unknown options without interrupting standard parameter processing" )
	void testUnknownAndUnregisteredFlags() {
		final String[] mixedArgs = { "-m", "--config-file=/etc/unsupported.json", "-b", "--unknown-flag", "-d" };

		assertAll(
				() -> assertDoesNotThrow( () -> Main.parseArguments( mixedArgs ) ),
				() -> assertTrue( Main.isFirstStart(), "Valid subsequent flag '-b' should be honored" ),
				() -> assertTrue( Main.isDebug(), "Valid subsequent flag '-d' should be honored" ) );
	}

	/**
	 * Confirms the entry parser's defensive design handles empty structures and malformed inputs
	 * without raising internal runtime errors.
	 */
	@Test
	@DisplayName( "10. Should resiliently ignore null or empty input arguments" )
	void testResilienceAgainstNullAndEmptyArgs() {

		assertAll(
				() -> assertDoesNotThrow( () -> Main.parseArguments( "   ", "", null ), "Empty, blank or null should not throw an exception" ),
				() -> assertFalse( Main.isDebug(), "Debug logging should be disabled" ),
				() -> assertFalse( Main.isFirstStart(), "Boot delay should be disabled" ) );
	}
}
