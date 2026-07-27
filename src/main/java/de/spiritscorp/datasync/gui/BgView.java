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

import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import javafx.application.Platform;

import javax.imageio.ImageIO;

import de.spiritscorp.datasync.controller.BgController;
import de.spiritscorp.datasync.controller.MainViewController;
import de.spiritscorp.datasync.io.Debug;

/**
 * Encapsulated system tray presenter. Bridges native AWT desktop toolkit hooks securely onto JavaFX platform thread loops.
 *
 * @author Tom Spirit
 */
public class BgView {

	/** The native system tray integration handle managing background execution presence, taskbar context menus, and platform notifications. */
	private TrayIcon trayIcon;
	/** The execution controller overseeing scheduled background worker threads and daemon interval routines. */
	private final BgController bgController;

	/**
	 * Create the SystemTrayIcon
	 *
	 * @param bgController The bgController
	 */
	public BgView( final BgController bgController ) {
		this.bgController = bgController;
		if( SystemTray.isSupported() ) {
			initTray();
		}else {
			Debug.printDebug( "[DataSync Core] SystemTray wird von diesem Betriebssystem nicht unterstützt." );
		}
	}

	private void initTray() {
		final PopupMenu popup = new PopupMenu();
		final Font font = new Font( "Comic Sans MS", Font.PLAIN, 14 );

		final MenuItem openItem = new MenuItem( "öffnen" );
		openItem.setFont( font );
		// CRITICAL: Bridge AWT event into the JavaFX Application Thread context safely
		openItem.addActionListener( _ -> Platform.runLater( () -> bgController.interruptBgJob( MainViewController.BG_TIMEOUT ) ) );

		final MenuItem closeItem = new MenuItem( "schließen" );
		closeItem.setFont( font );
		closeItem.addActionListener( _ -> Platform.runLater( bgController::requestApplicationShutdown ) );

		popup.add( openItem );
		popup.addSeparator();
		popup.add( closeItem );

		// Robustes Laden des Icons über den Context-ClassLoader
		BufferedImage trayImage;
		try {
			trayImage = ImageIO.read( getClass().getResourceAsStream( "/icons/16x16.png" ) );
		}catch( IOException _ ) {
			Debug.printError( "[DataSync] Tray-Icon nicht gefunden, erstelle Fallback-Pixel." );
			trayImage = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB );
		}

		this.trayIcon = new TrayIcon( trayImage, "DataSync Platform Console", popup );
		this.trayIcon.setImageAutoSize( true );

		// Doppel-Klick auf das Icon öffnet die App direkt
		this.trayIcon.addActionListener( _ -> Platform.runLater( () -> bgController.interruptBgJob( MainViewController.BG_TIMEOUT ) ) );
	}

	/**
	 * @return TrayIcon
	 */
	public TrayIcon getTrayIcon() { return trayIcon; }

	@Override
	public int hashCode() {
		return Objects.hash( bgController, trayIcon );
	}

	@Override
	public boolean equals( final Object obj ) {
		if( this == obj ) { return true; }
		if( obj == null ) { return false; }
		if( getClass() != obj.getClass() ) { return false; }
		final BgView other = (BgView) obj;
		return Objects.equals( bgController, other.bgController ) && Objects.equals( trayIcon, other.trayIcon );
	}
}
