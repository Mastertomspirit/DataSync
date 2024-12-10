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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;

import javax.swing.JOptionPane;

import de.spiritscorp.DataSync.Gui.BgView;
import de.spiritscorp.DataSync.Gui.View;
import de.spiritscorp.DataSync.IO.Logger;
import de.spiritscorp.DataSync.IO.Preference;
import de.spiritscorp.DataSync.Model.FileAttributes;
import de.spiritscorp.DataSync.Model.Model;
import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.ScanType;

public class Controller extends WindowAdapter implements ActionListener, MouseListener {
	
	private View view;
	private final BgView bgView;
	private final Model model;
	private final Preference pref;	
	private final Logger logger;
	private final ControllerHelper helper;
	private final BgController bgController;
	private final Controller event = this;

/**
 * 		Check if a background job should execute, elsewhere the view is started 
 * 
 * 		@param firstStart Delayed scan at system startup
 */
	public Controller(boolean firstStart) {
		Map<Path, FileAttributes> sourceMap = Model.createMap();
		Map<Path, FileAttributes> destMap = Model.createMap();
		pref = Preference.getInstance();
		logger = new Logger();
		model = new Model(logger, sourceMap, destMap);
		bgView = new BgView(event);
		try {
			EventQueue.invokeAndWait(() ->{
				try {
					view = new View(event, pref);
					if(!firstStart) view.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			});
		} catch (InvocationTargetException | InterruptedException e1) {e1.printStackTrace();	}	
		helper = new ControllerHelper(model, pref, sourceMap, destMap);
		bgController = new BgController(bgView, pref, logger);
		if(firstStart) {
			new Thread(()->bgController.startBgJob(view, helper, true)).start();
		}
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		if(!helper.isScanRun()) {
			view.setVisible(false);
			new Thread(()-> bgController.startBgJob(view, helper, false)).start();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

//		START BUTTON
		if(e.getSource() == view.getStartButton()) {	
			if(!helper.isScanRun() && pref.getSourcePath() != null && pref.getDestPath() != null) {
				if(pref.getDeepScan() == ScanType.SYNCHRONIZE && pref.getSourcePath().size() > 1) {
					view.setTextArea("Die syncronisierung funktioniert nur mit einem Quellordner!");
					return;
				}
				if(pref.getDeepScan() == ScanType.DUBLICATE_SCAN) 		new Thread(()-> helper.startDuplicateScan(view)).start();
				else if(pref.getDeepScan() == ScanType.SYNCHRONIZE) 		new Thread(()-> helper.startSyncronize(view)).start();
				else 		new Thread(()-> helper.startBackup(view)).start();
			}else {
				view.setTextArea("Syncronisierung läuft bereits");
			}
		}
		
//		CHECKBOX BACKGROUND SYNCRONISATION		
		if(e.getSource() == view.getBgSyncCheck()) {
			pref.setBgSync(!pref.isBgSync());
			if(pref.isBgSync() == true) {
				helper.setOSAutostart(true);
				view.getAutoSyncCheck().setEnabled(false);
				view.getAutoDelCheck().setText("im Hintergrund löschen");
				view.getAutoDelCheck().setSelected(pref.isAutoBgDel());
				view.getBgTimeComboBox().setEnabled(true);
			}else {
				helper.setOSAutostart(false);
				if(pref.getDeepScan() != ScanType.SYNCHRONIZE)		 view.getAutoSyncCheck().setEnabled(true);
				view.getAutoDelCheck().setText("automatisch löschen");
				view.getAutoDelCheck().setSelected(pref.isAutoDel());
				view.getBgTimeComboBox().setEnabled(false);
			}
		}
		
//		CHECKBOX AUTO DELETE
		if(e.getSource() == view.getAutoDelCheck()) {
			if(pref.isBgSync() == true) {
				pref.setAutoBgDel(!pref.isAutoBgDel());
			}else {
				pref.setAutoDel(!pref.isAutoDel());
			}
		}
		
//		CLOSE THE APPLICATION
		if(e.getSource() == view.getExitButton()) {
			if(JOptionPane.showConfirmDialog(view, "Wirklich beenden?", "Programm beenden", JOptionPane.OK_CANCEL_OPTION, 0) == 0)	System.exit(0);
		}
		
//		START THE WHOLE DIRECTORIES SELECTION
		if(e.getSource() == view.getSelectButton()) {
			helper.selectButton(view);
		}
		
//		SAVE ALL PREFERENCES
		if(e.getSource() == view.getPreference()) {	 
			if(pref.savePrefs()) view.setTextArea("Einstellungen erfolgreich gespeichert");
			else view.setTextArea("Einstellungen konnten nicht gespeichert werden");
		}
		
//		COMBO BOX FOR SCAN MODE
		if(e.getSource() == view.getScanModeComboBox()) {
			pref.setScanMode(ScanType.get((String)view.getScanModeComboBox().getSelectedItem()));
			if(pref.getDeepScan() == ScanType.SYNCHRONIZE) {
				view.getAutoDelCheck().setEnabled(false);
				view.getAutoSyncCheck().setEnabled(false);
				view.getTrashbinCheck().setEnabled(false);
				view.getLogOnBox().setEnabled(false);
			}else {
				view.getAutoDelCheck().setEnabled(true);
				if(!pref.isBgSync()) 		 view.getAutoSyncCheck().setEnabled(true);
				view.getTrashbinCheck().setEnabled(true);
				view.getLogOnBox().setEnabled(true);
			}
		}
		
//		COMBO BOX FOR BACKGROUND TIME INTERVALL
		if(e.getSource() == view.getBgTimeComboBox()) {
			pref.setBgTime(BgTime.get((String)view.getBgTimeComboBox().getSelectedItem()));
		}
		
//		ITEM BACKGROUND VIEW OPEN
		if(e.getSource() == bgView.getOpenItem()) {
			bgController.interruptBgJob();
			view.setVisible(true);
		}
		
//		ITEM BACKGROUND VIEW EXIT
		if(e.getSource() == bgView.getExitIcon()) {
			System.exit(0);
		}
		
//		CHECKBOX LOGGING ON
		if(e.getSource() == view.getLogOnBox()) {
			pref.setLogOn(!pref.isLogOn());
		}
		
//		CHECKBOX NO SUBDIR
		if(e.getSource() == view.getPlatzhalter()) {
//			TODO Platzhalter
		}
		
//		CHECKBOX AUTOMATIC SYNCRONISATION
		if(e.getSource() == view.getAutoSyncCheck()) {
			pref.setAutoSync(!pref.isAutoSync());
		}
		
//		CHECKBOX TRASHBIN
		if(e.getSource() == view.getTrashbinCheck()) {
			pref.setTrashbin(!pref.isTrashbin());
		}		
	}

	@Override
	public void mousePressed(MouseEvent e) {
//		SYSTEM TRAY -> RIGHT CLICK 
		if(e.getButton() == 1) {
			bgController.interruptBgJob();
			view.setVisible(true);
		}		
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {	}
	@Override
	public void mouseReleased(MouseEvent e) {	}
	@Override
	public void mouseEntered(MouseEvent e) {	}
	@Override
	public void mouseExited(MouseEvent e) {		}
	
}

