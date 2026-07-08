package de.spiritscorp.datasync.gui;

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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import de.spiritscorp.datasync.ScanType;
import de.spiritscorp.datasync.io.Preference;
import de.spiritscorp.datasync.io.PreferenceManager;

/**
 * Component renderer responsible for dynamically constructing and mutating layout nodes
 * within the user interface based on the active structural {@link ScanType}.
 * <p>
 * This class orchestrates single-source inputs, multi-source lists, and destination selection
 * panes, ensuring that the visual representation consistently matches the capabilities of the
 * selected execution profile.
 * </p>
 *
 * @author Tom Spirit
 * @since 1.0.0
 */
class ContextPathRenderer {

	/**
	 * The horizontal gap spacing in pixels used uniformly across structural grid layouts.
	 */
	private static final int H_GAP = 12;
	/**
	 * The vertical gap spacing in pixels used uniformly across structural box and grid configurations.
	 */
	private static final int V_GAP = 10;
	/**
	 * The preferred layout width in pixels allocated for descriptive structural labels.
	 */
	private static final int LABEL_WIDTH = 100;
	/**
	 * The preferred layout width in pixels allocated for path display text input fields.
	 */
	private static final int TEXT_FIELD_WIDTH = 400;
	/**
	 * The preferred layout width in pixels allocated for the multi-source directory selection list view.
	 */
	private static final int PATH_LIST_WIDTH = 400;
	/**
	 * The preferred layout height in pixels allocated for the multi-source directory selection list view.
	 */
	private static final int PATH_LIST_HEIGHT = 140;
	/**
	 * The system-wide fallback path context extracted from the global preference manager initialization state.
	 */
	private static final Path DEFAULT_PATH = PreferenceManager.getInstance().getRootPath();

	/**
	 * Dynamically morphs and populates the layout components inside a target container
	 * mapped to the contextual rules of the active {@link ScanType}.
	 *
	 * @param type         The operational scanning mode defining which layout parts are required
	 * @param container    The target UI container to be cleared and rebuilt with the generated nodes
	 * @param pref         The operational data model context carrying active synchronization properties
	 * @param primaryStage The application-level window stage acting as the parent for modal dialogue screens
	 */
	void renderContextPaths( final ScanType type, final VBox container, final Preference pref, final Stage primaryStage ) {
		// Clear previous components to prepare for dynamic layout generation
		container.getChildren().clear();

		final GridPane pathsGrid = new GridPane();
		pathsGrid.setHgap( H_GAP );
		pathsGrid.setVgap( V_GAP );

		// Set up descriptive section title header
		final Label dirTitleLabel = new Label( "Verzeichnis-Konfiguration (" + type.getDescription() + ")" );
		dirTitleLabel.getStyleClass().addAll( "dir-title-label" );
		container.getChildren().add( dirTitleLabel );

		// Evaluate source component requirements
		if( ScanType.SYNCHRONIZE == type ) {
			pathsGrid.add( getSourceBox( pref, primaryStage ), 0, 0, 2, 1 );
		}else if( ScanType.FLAT_SCAN == type || ScanType.DEEP_SCAN == type || ScanType.DUBLICATE_SCAN == type ) {
			pathsGrid.add( getSourceList( type, pref, primaryStage ), 0, 0, 2, 1 );
		}

		// Evaluate destination component requirements
		if( ScanType.FLAT_SCAN == type || ScanType.DEEP_SCAN == type || ScanType.SYNCHRONIZE == type ) {
			pathsGrid.add( getDestBox( pref, primaryStage ), 0, 1, 2, 1 );
		}

		container.getChildren().add( pathsGrid );
	}

	/**
	 * Constructs a single-source path input panel complete with an interactive file browser trigger.
	 *
	 * @param pref         The underlying preference data model containing current source configuration
	 * @param primaryStage The parent stage context required to display the file selection overlay modal
	 * @return A configured {@link VBox} layout element encapsulating the single-source configuration nodes
	 */
	private VBox getSourceBox( final Preference pref, final Stage primaryStage ) {
		final GridPane destGrid = new GridPane();
		destGrid.setHgap( H_GAP );
		final String initialString = getInitialPath( pref.getSourcePaths() ).toString();
		final TextField srcTextField = new TextField( initialString );
		srcTextField.setPrefWidth( TEXT_FIELD_WIDTH );
		final Button srcBtn = new Button( "Durchsuchen..." );
		srcBtn.setOnAction( _ -> {
			final File initialSrc = getInitialPath( pref.getSourcePaths() );
			final File dirPath = chooseDirectory( initialSrc, "Quellverzeichnis für " + pref.getScanMode().getDescription(), primaryStage );
			if( dirPath != null ) {
				srcTextField.setText( dirPath.getAbsolutePath() );
				pref.setSourcePaths( new ArrayList<>( List.of( dirPath.toPath() ) ) );
			}
		} );
		final Label label = new Label( "Quellverzeichnis:" );
		label.setPrefWidth( LABEL_WIDTH );
		destGrid.add( label, 0, 0 );
		destGrid.add( new HBox( 8, srcTextField, srcBtn ), 1, 0 );
		return new VBox( V_GAP, new Separator(), destGrid );
	}

	/**
	 * Builds a multi-source selection control allowing multiple distinct execution directories
	 * to be systematically managed and registered.
	 *
	 * @param type         The specific profile category to tailor UI labeling rules
	 * @param pref         The underlying data model containing cumulative configuration state
	 * @param primaryStage The structural window layout owner used to bind standard chooser instances
	 * @return A structurally complete {@link VBox} controlling multi-directory source path management
	 */
	private VBox getSourceList( final ScanType type, final Preference pref, final Stage primaryStage ) {
		// Contextual labeling mapping specific scan constraints
		final String label = ScanType.DUBLICATE_SCAN == type ? "Scanverzeichnisse:" : "Quellverzeichnisse (Multi-Source Pathing):";
		final String chooserTitle = String.format( ScanType.DUBLICATE_SCAN == type ? "Scanverzeichnis für %s" : "Quellverzeichnis für %s", pref.getScanMode().getDescription() );

		final VBox multiSrcBox = new VBox( 6 );
		final Label multiSrcLabel = new Label( label );
		multiSrcLabel.getStyleClass().addAll( "multi-src-label" );

		// Populate view wrapper backed by existing preferences
		final ObservableList<String> actualSourcePaths = FXCollections.observableArrayList();
		for( final Path path : pref.getSourcePaths() ) {
			actualSourcePaths.add( path.toString() );
		}

		final ListView<String> pathsListView = new ListView<>( actualSourcePaths );
		pathsListView.setPrefHeight( PATH_LIST_HEIGHT );
		pathsListView.setPrefWidth( PATH_LIST_WIDTH );

		// Setup add action handler
		final Button add = new Button( "Verzeichnis hinzufügen", Gui.createIcon( MaterialDesignP.PLUS ) );
		add.setOnAction( _ -> {
			final File dirPath = chooseDirectory( getInitialPath( pref.getSourcePaths() ), chooserTitle, primaryStage );
			if( dirPath != null && !actualSourcePaths.contains( dirPath.getAbsolutePath() ) ) {
				actualSourcePaths.add( dirPath.getAbsolutePath() );
				pref.setSourcePath( dirPath.toPath() );
			}
		} );

		// Setup remove action handler
		final Button remove = new Button( "Entfernen", Gui.createIcon( MaterialDesignD.DELETE ) );
		remove.setOnAction( _ -> {
			final String selectedPath = pathsListView.getSelectionModel().getSelectedItem();
			if( selectedPath != null ) {
				actualSourcePaths.remove( selectedPath );
				pref.removeSourcePath( Paths.get( selectedPath ) );
			}
		} );

		multiSrcBox.getChildren().addAll( multiSrcLabel, pathsListView, new HBox( 8, add, remove ) );
		return multiSrcBox;
	}

	/**
	 * Constructs a single-destination path input panel configured with targeting properties.
	 *
	 * @param pref         The core configurations instance carrying structural file target references
	 * @param primaryStage The parent stage context tracking native file directory browser overlays
	 * @return A configured {@link VBox} layout component ready for application visualization
	 */
	private VBox getDestBox( final Preference pref, final Stage primaryStage ) {
		final GridPane destGrid = new GridPane();
		destGrid.setHgap( H_GAP );
		final String initialString = getInitialPath( pref.getDestPaths() ).toString();
		final TextField destField = new TextField( initialString );
		destField.setPrefWidth( TEXT_FIELD_WIDTH );
		final Button destBtn = new Button( "Durchsuchen..." );
		destBtn.setOnAction( _ -> {
			final File initialDest = getInitialPath( pref.getDestPaths() );
			final File dirPath = chooseDirectory( initialDest, "Zielverzeichnis für " + pref.getScanMode().getDescription(), primaryStage );
			if( dirPath != null ) {
				destField.setText( dirPath.getAbsolutePath() );
				pref.setDestPaths( new ArrayList<>( List.of( dirPath.toPath() ) ) );
			}
		} );
		final Label label = new Label( "Zielverzeichnis:" );
		label.setPrefWidth( LABEL_WIDTH );
		destGrid.add( label, 0, 0 );
		destGrid.add( new HBox( 8, destField, destBtn ), 1, 0 );
		return new VBox( V_GAP, new Separator(), destGrid );
	}

	/**
	 * Creates and presents a platform-native directory modal browser window.
	 *
	 * @param initialDir   The file system path representation establishing the initial viewport directory
	 * @param title        The descriptive display header initialized across the file system modal dialog frame
	 * @param primaryStage The system UI layout thread window mapping modal ownership constraints
	 * @return The selected {@link File} context, or {@code null} if processing was terminated by the user
	 */
	private File chooseDirectory( final File initialDir, final String title, final Stage primaryStage ) {
		final DirectoryChooser chooser = new DirectoryChooser();
		if( initialDir != null && initialDir.exists() ) chooser.setInitialDirectory( initialDir );
		chooser.setTitle( title );
		return chooser.showDialog( primaryStage );
	}

	/**
	 * Extends lookup heuristics to safely retrieve a reliable initial navigation directory context.
	 * <p>
	 * This strategy evaluates existing records to pick the last interacted directory path location
	 * to support persistent scrolling memory across subsequent browser invocations.
	 * </p>
	 *
	 * @param paths The list of recorded historical directory path variables available for extraction
	 * @return A valid {@link File} instance referencing either the last list entry or the fallback default root
	 */
	private File getInitialPath( final List<Path> paths ) {
		return ( paths != null && !paths.isEmpty() ) ? paths.get( paths.size() - 1 ).toFile() : DEFAULT_PATH.toFile();
	}
}
