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
package de.spiritscorp.DataSync.Controller;

import java.awt.AWTException;
import java.awt.SystemTray;
import de.spiritscorp.DataSync.Gui.View;
import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.IO.Debug;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.BgModel;
import de.spiritscorp.DataSync.Model.Model;

class BgController  {
	
	private Thread thread;
	private final SystemTray sysTray;
	private final Preference pref;
	private final Logger logger;
	private final BgView bgView;
	
	/**
	 * Background Controller
	 * 
	 * @param bgView The background view
	 * @param pref The settings to be used
	 */
	BgController(final BgView bgView, final Preference pref, final Logger logger) {
		this.pref = pref;
		this.bgView = bgView;
		this.logger = logger;
		sysTray = SystemTray.getSystemTray();
	}
	
	/**
	 * 
	 * Run the Background Job
	 * 
	 * @param view The view
	 * @param helper
	 * @param firstStart If true, the thread wait 10 minutes
	 */
	void startBgJob(final View view, final ControllerHelper helper, final boolean firstStart) {
		try {
			sysTray.add(bgView.getTrayIcon());
		} catch (AWTException e) {e.printStackTrace();}
		thread = new Thread( () -> {
			Debug.PRINT_DEBUG("bgJob starts");
			BgModel bgModel = new BgModel(pref, logger, Model.createMap(), Model.createMap());
			boolean bgRun = true;
			try {
				if(firstStart) Thread.sleep(60000);
			}catch(InterruptedException e) {e.printStackTrace();}
			while(bgRun) {
				helper.setScanRun(true);
				view.setScanRun(true);
				if(pref.isBgSync()) bgModel.runBgJob();
				helper.setScanRun(false);
				view.setScanRun(false);
				try {
					Thread.sleep(pref.getBgTime().getCheckTime());
				} catch (InterruptedException e) {bgRun = false;}
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e1) {e1.printStackTrace();}
		sysTray.remove(bgView.getTrayIcon());
	}

	/**
	 * Interrupt the Background Job
	 *  
	 */
	void interruptBgJob() {
		this.thread.interrupt();
	}
}
