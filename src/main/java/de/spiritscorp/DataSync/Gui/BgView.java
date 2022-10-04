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
import java.awt.Toolkit;
import java.awt.TrayIcon;

import de.spiritscorp.DataSync.Controller.Controller;

public class BgView {
	
	private TrayIcon trayIcon;
	private PopupMenu popup;
	private MenuItem open, close;
/**
 * Create the SystemTrayIcon
 * 
 * @param controller The controller
 */
	public BgView(Controller controller) {
		popup = new PopupMenu();
		
		open = new MenuItem("öffnen");
		open.setFont(new Font("Comic Sans MS", 0, 14));
		open.addActionListener(controller);

		close = new MenuItem("schließen");
		close.setFont(new Font("Comic Sans MS", 0, 14));
		close.addActionListener(controller);

		popup.add(open);
		popup.addSeparator();
		popup.add(close);
		
		trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(BgView.class.getClassLoader().getResource("16x16.png")), "DataSync");
		trayIcon.setPopupMenu(popup);
		trayIcon.addMouseListener(controller);
	}
	
	/**
	 * @return TrayIcon
	 */
	public TrayIcon getTrayIcon() {
		return trayIcon;
	}

	/**
	 * @return MenuItem
	 */
	public MenuItem getExitIcon() {
		return close;
	}

	/**
	 * @return MenuItem
	 */
	public MenuItem getOpenItem() {
		return open;
	}	
}
