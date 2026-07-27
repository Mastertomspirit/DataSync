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

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;

import de.spiritscorp.datasync.io.Debug;

/**
 * Service responsible for managing UI dialogs and alerts.
 * Ensures strict thread safety by bridging background thread calls to the JavaFX Application Thread.
 *
 * @author Tom Spirit
 */
public class DialogService {

	/** The primary application stage acting as the owner window for modal dialogs. */
	private final Stage stage;

	/**
	 * Constructs a new DialogService bound to a specific primary window stage.
	 *
	 * @param stage The parent stage container, must not be null
	 * @throws NoSuchElementException if the provided stage is null
	 */
	public DialogService( final Stage stage ) {
		this.stage = Optional.ofNullable( stage ).orElseThrow();
	}

	/**
	 * Displays a modal confirmation dialog requesting an OK or Cancel decision from the user.
	 * Automatically handles cross-thread invocations without throwing structural exceptions.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The message body containing instructions or questions
	 * @return true if the user confirmed via OK, false if canceled, dismissed, or closed
	 */
	public boolean promptOkChancel( final String title, final String header, final String content ) {
		return promptConfirmation( title, header, content, true );
	}

	/**
	 * Displays a modal confirmation dialog requesting a Yes or No decision from the user. Automatically handles cross-thread invocations without throwing structural exceptions.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The message body containing instructions or questions
	 * @return true if the user confirmed via YES, false if denied, dismissed, or closed
	 */
	public boolean promptYesNo( final String title, final String header, final String content ) {
		return promptConfirmation( title, header, content, false );
	}

	/**
	 * Dispatches a modal text input dialog to capture a textual response from the user.
	 * This method blocks the calling thread and safely orchestrates thread switches if invoked
	 * outside the primary JavaFX Application Thread.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The descriptive label text guiding the user input
	 * @return The sanitized string captured from the input field, or an empty string if dismissed
	 */
	public String promptTextInput( final String title, final String header, final String content ) {
		// Execute immediately if invoked directly on the JavaFX Application Thread
		if( Platform.isFxApplicationThread() ) return showTextDialog( title, header, content );

		// Bridge execution synchronously if called from a background worker thread
		final CompletableFuture<String> userResponse = new CompletableFuture<>();

		Platform.runLater( () -> {
			try {
				userResponse.complete( showTextDialog( title, header, content ) );
			}catch( final IllegalStateException exception ) {
				userResponse.completeExceptionally( exception );
			}
		} );

		try {
			return userResponse.get();
		}catch( InterruptedException _ ) {
			Thread.currentThread().interrupt();
		}catch( final ExecutionException exception ) {
			Debug.printDebug( "[Dialog Service Error] Execution Exception -> ", exception.getMessage() );
			Debug.printException( getClass(), exception );
		}
		return "";
	}

	/**
	 * Internal orchestration engine filtering thread contexts before rendering confirmation alerts.
	 * Synchronously locks background tasks via futures until human interaction concludes.
	 *
	 * @param title        The descriptive title of the dialog window
	 * @param header       The contextual header text of the notification
	 * @param content      The message body containing instructions or questions
	 * @param buttonTypeOk If true, initializes OK/CANCEL buttons; if false, initializes YES/NO options.
	 * @return true if an affirmative action was captured, false otherwise
	 */
	private boolean promptConfirmation( final String title, final String header, final String content, final boolean buttonTypeOk ) {
		// Execute immediately if invoked directly on the JavaFX Application Thread
		if( Platform.isFxApplicationThread() ) {
			return buttonTypeOk ? showConfirmationDialog( title, header, content, ButtonType.OK, ButtonType.CANCEL ) : showConfirmationDialog( title, header, content, ButtonType.YES, ButtonType.NO );
		}

		// Bridge execution synchronously if called from a background worker thread
		final CompletableFuture<Boolean> userResponse = new CompletableFuture<>();

		Platform.runLater( () -> {
			try {
				final boolean response = buttonTypeOk ? showConfirmationDialog( title, header, content, ButtonType.OK, ButtonType.CANCEL )
						: showConfirmationDialog( title, header, content, ButtonType.YES, ButtonType.NO );
				userResponse.complete( response );
			}catch( final IllegalStateException exception ) {
				userResponse.completeExceptionally( exception );
			}
		} );

		try {
			return userResponse.get(); // Halts the background worker here until the user interacts with the UI
		}catch( InterruptedException _ ) {
			Thread.currentThread().interrupt();
		}catch( final ExecutionException exception ) {
			Debug.printDebug( "[Dialog Service Error] Execution Exception -> ", exception.getMessage() );
			Debug.printException( getClass(), exception );
		}
		return false;
	}

	/**
	 * Builds and visualizes the native JavaFX Alert window component. This operation must strictly execute inside the boundaries of the FX core thread.
	 *
	 * @param title    The descriptive title of the dialog window.
	 * @param header   The contextual header text of the notification.
	 * @param content  The message body containing instructions or questions.
	 * @param buttType A variable argument array of button types used to dynamically populate the interface.
	 * @throws IllegalStateException if invoked outside the JavaFX Application Thread.
	 * @return true if an affirmative action (OK/YES) was captured, false otherwise.
	 */
	private boolean showConfirmationDialog( final String title, final String header, final String content, final ButtonType... buttType ) {
		final FontIcon icon = Gui.createIcon( MaterialDesignA.ALERT );
		icon.setIconSize( 50 );
		icon.setIconColor( Paint.valueOf( "blue" ) );
		icon.getStyleClass().add( "dialog-custom-icon" );
		final Label contentLabel = new Label( content );
		contentLabel.setPrefWidth( 450 );
		contentLabel.setWrapText( true );
		final Alert confirmation = new Alert( Alert.AlertType.CONFIRMATION );
		confirmation.setTitle( title );
		confirmation.setGraphic( icon );
		confirmation.setHeaderText( header );
		confirmation.getDialogPane().setContent( contentLabel );
		confirmation.getButtonTypes().setAll( buttType );
		confirmation.initOwner( stage );
		final ButtonType result = confirmation.showAndWait().orElse( ButtonType.CANCEL );
		return result == ButtonType.OK || result == ButtonType.YES;
	}

	/**
	 * Low-level helper to instantiate and display the native JavaFX TextInputDialog.
	 * Combines the custom wrapped text node with the internal input control into a synchronized layout container.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The descriptive label text guiding the user input
	 * @throws IllegalStateException if invoked outside the JavaFX Application Thread.
	 * @return The trimmed user input string, or an empty string if aborted
	 */
	private String showTextDialog( final String title, final String header, final String content ) {
		final FontIcon icon = Gui.createIcon( MaterialDesignK.KEYBOARD_OUTLINE );
		icon.setIconSize( 50 );
		icon.setIconColor( Paint.valueOf( "blue" ) );
		icon.getStyleClass().add( "dialog-custom-icon" );
		final TextInputDialog dialog = new TextInputDialog();
		dialog.setGraphic( icon );
		dialog.setTitle( title );
		dialog.setHeaderText( header );
		dialog.setContentText( content );
		dialog.initOwner( stage );
		return dialog.showAndWait().orElse( "" ).trim();
	}
}
