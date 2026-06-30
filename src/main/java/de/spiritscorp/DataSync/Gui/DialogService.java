package de.spiritscorp.DataSync.Gui;
/*
Data Sync
	Application to synchronize your data

@author Tom Spirit
@email tomspirit@spiritscorp.network

Copyright ©

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import de.spiritscorp.DataSync.IO.Debug;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Service responsible for managing UI dialogs and alerts.
 * Ensures strict thread safety by bridging background thread calls to the JavaFX Application Thread.
 */
public class DialogService {

	/** The primary application stage acting as the owner window for modal dialogs. */
	private final Stage stage;

	/**
	 * Constructs a new DialogService bound to a specific primary window stage.
	 *
	 * @param stage The parent stage container, must not be null
	 * @throws NullPointerException if the provided stage is null
	 */
	public DialogService( final Stage stage ) {
		this.stage = stage;
	}

	/**
	 * Displays a confirmation dialog to the user and blocks until a selection is made.
	 * Automatically handles cross-thread invocations without throwing structural exceptions.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The message body containing instructions or questions
	 * @return true if the user confirmed via OK, false if canceled or closed
	 */
	public boolean askUser( final String title, final String header, final String content ) {
		// Execute immediately if invoked directly on the JavaFX Application Thread
		if( Platform.isFxApplicationThread() ) { return showConfirmationDialog( title, header, content ); }

		// Bridge execution synchronously if called from a background worker thread
		final CompletableFuture<Boolean> userResponse = new CompletableFuture<>();

		Platform.runLater( () -> {
			try {
				final boolean response = showConfirmationDialog( title, header, content );
				userResponse.complete( response );
			}catch( final Exception e ) {
				userResponse.completeExceptionally( e );
			}
		} );

		try {
			return userResponse.get(); // Halts the background worker here until the user interacts with the UI
		}catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
			return false;
		}catch( final ExecutionException e ) {
			Debug.printDebug( "[Dialog Service Error] Execution Exception -> ", e.getMessage() );
			Debug.printException( getClass(), e );
			return false;
		}
	}

	/**
	 * Builds and visualizes the native JavaFX Alert window component.
	 * This operation must strictly execute inside the boundaries of the FX core thread.
	 *
	 * @param title   The descriptive title of the dialog window
	 * @param header  The contextual header text of the notification
	 * @param content The message body containing instructions or questions
	 * @return true if OK was selected, false otherwise
	 */
	private boolean showConfirmationDialog( final String title, final String header, final String content ) {
		final Alert confirmation = new Alert( Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL );
		confirmation.setTitle( title );
		confirmation.setHeaderText( header );
		confirmation.initOwner( stage );
		return confirmation.showAndWait().orElse( ButtonType.CANCEL ) == ButtonType.OK;
	}
}
