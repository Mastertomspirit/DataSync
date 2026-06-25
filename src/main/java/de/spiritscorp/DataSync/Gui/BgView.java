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
package de.spiritscorp.DataSync.Gui;

import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.imageio.ImageIO;

import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.Controller.BgController;
import de.spiritscorp.DataSync.IO.Debug;
import javafx.application.Platform;

/**
 * Encapsulated system tray presenter.
 * Bridges native AWT desktop toolkit hooks securely onto JavaFX platform thread loops.
 *
 * @author Tom Spirit
 */
public class BgView {

	private TrayIcon trayIcon;
	private final BgController controller;

	/**
	 * Create the SystemTrayIcon
	 *
	 * @param controller The controller
	 */
	public BgView(BgController controller) {
		this.controller = controller;
		if (SystemTray.isSupported()) {
			initTray();
		} else {
			Debug.printDebug("[DataSync Core] SystemTray wird von diesem Betriebssystem nicht unterstützt.");
		}
	}

	private void initTray() {
		final PopupMenu popup = new PopupMenu();
		final Font font = new Font("Comic Sans MS", Font.PLAIN, 14);

		final MenuItem openItem = new MenuItem("öffnen");
		openItem.setFont(font);
		// CRITICAL: Bridge AWT event into the JavaFX Application Thread context safely
		openItem.addActionListener(e -> Platform.runLater(() -> {
			controller.interruptBgJob(Main.BACKGROUND_THREAD_TIMEOUT);
		}));

		final MenuItem closeItem = new MenuItem("schließen");
		closeItem.setFont(font);
		closeItem.addActionListener(e -> Platform.runLater(controller::requestApplicationShutdown));

		popup.add(openItem);
		popup.addSeparator();
		popup.add(closeItem);

		// Robustes Laden des Icons über den Context-ClassLoader
		BufferedImage trayImage;
		try {
			trayImage = ImageIO.read(getClass().getResourceAsStream("/16x16.png"));
		} catch (final Exception e) {
			System.err.println("[DataSync] Tray-Icon nicht gefunden, erstelle Fallback-Pixel.");
			trayImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		}

		this.trayIcon = new TrayIcon(trayImage, "DataSync Platform Console", popup);
		this.trayIcon.setImageAutoSize(true);

		// Doppel-Klick auf das Icon öffnet die App direkt
		this.trayIcon.addActionListener(e -> Platform.runLater(() -> controller.interruptBgJob(Main.BACKGROUND_THREAD_TIMEOUT)));
	}

	/**
	 * @return TrayIcon
	 */
	public TrayIcon getTrayIcon() { return trayIcon; }

	@Override
	public int hashCode() {
		return Objects.hash(controller, trayIcon);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BgView other = (BgView) obj;
		return Objects.equals(controller, other.controller) && Objects.equals(trayIcon, other.trayIcon);
	}
}
