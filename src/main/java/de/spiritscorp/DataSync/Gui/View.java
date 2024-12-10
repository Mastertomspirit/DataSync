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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.swing.JComboBox;

import de.spiritscorp.DataSync.BgTime;
import de.spiritscorp.DataSync.Main;
import de.spiritscorp.DataSync.ScanType;
import de.spiritscorp.DataSync.Controller.Controller;
import de.spiritscorp.DataSync.IO.Preference;

public class View extends JFrame {

	private static final long serialVersionUID = 3324969196382862745L;
	private JPanel contentPane;
	private JLabel destLabel, versionInfo;
	private JScrollPane scrollPane;
	private JTextArea sourceLabel, textArea;
	private JButton selectButton, exitButton, startButton, preference;
	private Controller event;
	private StringBuffer sb_textArea;
	private Font font = new Font("Comic Sans MS", 1, 14);
	private Color fgColor = Color.decode("#FFFF00");
	private final ImageIcon loadingGif = new ImageIcon(View.class.getClassLoader().getResource("loading-4.gif"));
	private final ImageIcon savePng = new ImageIcon(View.class.getClassLoader().getResource("floppy-disk-save-file.png"));
	private final Image bgPic;
	private JCheckBox autoDelCheck, platzhalter, autoSyncCheck;
	private JCheckBox bgSyncCheck, logOnBox, trashbinCheck;
	private JComboBox<String> deepScanComboBox;
	private JComboBox<String> bgTimeComboBox;
	{
		Image bgPicTemp = null;
		try {
			bgPicTemp = ImageIO.read(View.class.getClassLoader().getResource("splashing-165192_1280.jpg"));
		} catch (IOException e) {e.printStackTrace();}
		bgPic = bgPicTemp;
	}
	
	/**
	 * Create the frame
	 * 
	 * @param event
	 * @param pref
	 */
	public View(Controller event, Preference pref) {
		this.event = event;
		initComponents(pref.getSourcePath(), pref.getStartDestPath().toString(), pref);
	}
	
	private void initComponents(ArrayList<Path> sourcePath, String destPath, Preference pref) {
		setTitle("DataSync");
		setIconImage(getToolkit().getImage(View.class.getClassLoader().getResource("16x16.png")));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(1100, 800);
		setLocationRelativeTo(null);
		setFont(font);		
		addWindowListener(event);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
        SwingUtilities.updateComponentTreeUI(this);

		contentPane = new JPanel() {
			private static final long serialVersionUID = -4321892096303455821L;
			public void paintComponent(Graphics g) {
				g.drawImage(bgPic, 0, 0, Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height, null);
			}
		};
		contentPane.setDoubleBuffered(true);
		contentPane.setBackground(null);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		loadingGif.setImage(loadingGif.getImage().getScaledInstance(140, 92, Image.SCALE_DEFAULT));
		savePng.setImage(savePng.getImage().getScaledInstance(60, 70, Image.SCALE_DEFAULT));

		sourceLabel = new JTextArea("Dateipfad: ");
		sourceLabel.setWrapStyleWord(true);
		sourceLabel.setBackground(null);
		sourceLabel.setOpaque(false);
		sourceLabel.setFont(font);
		sourceLabel.setForeground(fgColor);
		destLabel = new JLabel("Dateipfad: ");
		destLabel.setFont(font);
		destLabel.setForeground(fgColor);
		
		if(pref.isLoaded()) { 
			this.setSourceLabel(sourcePath);
			destLabel.setText(destPath);
			sb_textArea = new StringBuffer("Einstellungen erfolgreich geladen" + System.lineSeparator());
			if(sourcePath.size() > 3) {
				sourcePath.stream()
						  .forEach((p) -> sb_textArea.append("Quellverzeichnis: " + p.toString() + System.lineSeparator())); 
			}
			sb_textArea.append("Alles bereit zum Starten" + System.lineSeparator());
		}else{
			this.setSourceLabel(sourcePath);
			destLabel.setText(destPath);
			sb_textArea = new StringBuffer("Keine Einstellungen gefunden" + System.lineSeparator());
			sb_textArea.append("Bitte Ordner wählen" + System.lineSeparator());
		}
		
		sourceLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		sourceLabel.setToolTipText(sourceLabel.getText());
		sourceLabel.setEditable(false);
		destLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		destLabel.setToolTipText(destLabel.getText());

		exitButton = new JButton("Schließen");
		exitButton.setEnabled(true);
		exitButton.setFont(font);
		exitButton.setToolTipText("Beendet das Programm");
		exitButton.addActionListener(event);
	
		selectButton = new JButton("Verzeichnisse wählen");
		selectButton.setFont(font);
		selectButton.setToolTipText("Wähle das Quell- und das Zielverzeichnis");
		selectButton.addActionListener(event);
	
		textArea = new JTextArea();
		textArea.setBorder(new EmptyBorder(5, 5, 5, 5));
		textArea.setFont(font.deriveFont(0));
		textArea.setBackground(null);
		textArea.setText(sb_textArea.toString());
		textArea.setLineWrap(false);
		textArea.setEditable(false);
		textArea.setAutoscrolls(true);
		
		scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setAutoscrolls(true);
		
		startButton = new JButton();
		startButton.setIconTextGap(0);
		startButton.setFont(font);
		setScanRun(false);
		startButton.addActionListener(event);
		
		preference = new JButton();
		preference.setIconTextGap(0);
		preference.setToolTipText("Einstellungen speichern");
		preference.setIcon(savePng);
		preference.setSelectedIcon(savePng);
		preference.setContentAreaFilled(true);
		preference.addActionListener(event);
	
		versionInfo = new JLabel(Main.VERSION + "    developed by Tom Spirit     @ 2022 DataSync");
		versionInfo.setForeground(fgColor);
		versionInfo.setFont(font.deriveFont(12f));
		versionInfo.setToolTipText("developed by Tom Spirit     @ 2022 DataSync");

		logOnBox = new JCheckBox("Logging an");
		logOnBox.setToolTipText("Aktiviert das Logging");
		logOnBox.setSelected(pref.isLogOn());
		logOnBox.setMinimumSize(new Dimension(130, 23));
		logOnBox.setFont(font.deriveFont(18f));
		logOnBox.setForeground(fgColor);
		logOnBox.setOpaque(false);
		logOnBox.setBackground(null);
		logOnBox.addActionListener(event);
		
		deepScanComboBox = new JComboBox<String>();
		deepScanComboBox.setFont(font.deriveFont(16f).deriveFont(0));
		deepScanComboBox.setModel(new DefaultComboBoxModel<String>(ScanType.getAllDescriptions()));
		deepScanComboBox.setSelectedItem(pref.getDeepScan().getDescription());
		deepScanComboBox.setToolTipText("Duplikate suchen und Tiefer Scan lesen die Dateien komplett ein, es kann dementsprechend lang dauern");
		deepScanComboBox.addActionListener(event);
		
		autoDelCheck = new JCheckBox("automatisch löschen");
		autoDelCheck.setToolTipText("Überspringt die Benuterbestätigung");
		if(pref.isBgSync()) autoDelCheck.setSelected(pref.isAutoBgDel());
		else autoDelCheck.setSelected(pref.isAutoDel());
		autoDelCheck.setMinimumSize(new Dimension(130, 23));
		autoDelCheck.setFont(font.deriveFont(18f));
		autoDelCheck.setForeground(fgColor);
		autoDelCheck.setOpaque(false);
		autoDelCheck.setBackground(null);
		autoDelCheck.addActionListener(event);
		
		platzhalter = new JCheckBox("Platzhalter");
		platzhalter.setToolTipText("Zeigt gerade true an");
		platzhalter.setSelected(true);
		platzhalter.setMinimumSize(new Dimension(130, 23));
		platzhalter.setFont(font.deriveFont(18f));
		platzhalter.setOpaque(false);
		platzhalter.setEnabled(false);
		platzhalter.setForeground(fgColor);
		platzhalter.setBackground(null);
		platzhalter.addActionListener(event);
		
		autoSyncCheck = new JCheckBox("automatisch syncronisieren");
		autoSyncCheck.setToolTipText("Überspringt die Benuterbestätigung");
		autoSyncCheck.setSelected(pref.isAutoSync());
		autoSyncCheck.setMinimumSize(new Dimension(130, 23));
		autoSyncCheck.setFont(font.deriveFont(18f));
		if(pref.isBgSync()) autoSyncCheck.setEnabled(false);
		else autoSyncCheck.setEnabled(true);
		autoSyncCheck.setOpaque(false);
		autoSyncCheck.setForeground(fgColor);
		autoSyncCheck.setBackground(null);
		autoSyncCheck.addActionListener(event);
		
		trashbinCheck = new JCheckBox("Papierkorb an");
		trashbinCheck.setSelected(pref.isTrashbin());
		trashbinCheck.setMinimumSize(new Dimension(130, 23));
		trashbinCheck.setFont(font.deriveFont(18f));
		trashbinCheck.setOpaque(false);
		trashbinCheck.setForeground(fgColor);
		trashbinCheck.setBackground(null);
		trashbinCheck.setToolTipText(pref.getTrashbinPath().toString());
		trashbinCheck.addActionListener(event);
		
		bgSyncCheck = new JCheckBox("Hintergrund Dienst alle :");
		bgSyncCheck.setToolTipText("Setzt den Autostart und aktiviert die Hintergrundsyncronisierung im nachfolgenden Intervall");
		bgSyncCheck.setSelected(pref.isBgSync());
		bgSyncCheck.setMinimumSize(new Dimension(130, 23));
		bgSyncCheck.setFont(font.deriveFont(18f));
		bgSyncCheck.setForeground(fgColor);
		bgSyncCheck.setOpaque(false);
		bgSyncCheck.setBackground(null);
		bgSyncCheck.addActionListener(event);
		
		bgTimeComboBox = new JComboBox<String>();
		bgTimeComboBox.setModel(new DefaultComboBoxModel<String>(BgTime.getNames()));
		bgTimeComboBox.setFont(font.deriveFont(16f).deriveFont(0));
		bgTimeComboBox.setSelectedItem(pref.getBgTime().getName());
		if(!pref.isBgSync()) bgTimeComboBox.setEnabled(false);
		bgTimeComboBox.addActionListener(event);
		
		if(pref.getDeepScan() == ScanType.SYNCHRONIZE) {
			autoDelCheck.setEnabled(false);
			autoSyncCheck.setEnabled(false);
			trashbinCheck.setEnabled(false);
			logOnBox.setEnabled(false);
		}
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(10)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(versionInfo, GroupLayout.PREFERRED_SIZE, 441, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED, 289, Short.MAX_VALUE)
									.addComponent(preference, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
									.addGap(81)
									.addComponent(startButton, GroupLayout.PREFERRED_SIZE, 184, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(sourceLabel, 300, 504, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(destLabel, 300, 546, Short.MAX_VALUE))))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
								.addComponent(deepScanComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(selectButton, GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE))
							.addGap(18)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(autoDelCheck, GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
								.addComponent(platzhalter, GroupLayout.PREFERRED_SIZE, 245, GroupLayout.PREFERRED_SIZE)
								.addComponent(autoSyncCheck, GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(logOnBox, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(trashbinCheck, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(bgSyncCheck, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(exitButton, GroupLayout.PREFERRED_SIZE, 223, GroupLayout.PREFERRED_SIZE)
								.addComponent(bgTimeComboBox, GroupLayout.PREFERRED_SIZE, 219, GroupLayout.PREFERRED_SIZE)))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1054, Short.MAX_VALUE)))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(sourceLabel, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
						.addComponent(destLabel, GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(selectButton, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
							.addGap(23)
							.addComponent(deepScanComboBox, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(logOnBox, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
								.addComponent(exitButton, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
								.addComponent(autoSyncCheck, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(autoDelCheck, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
								.addComponent(trashbinCheck, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(platzhalter, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
								.addComponent(bgTimeComboBox, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
								.addComponent(bgSyncCheck, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE))))
					.addGap(28)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE)
					.addGap(17)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(startButton, GroupLayout.PREFERRED_SIZE, 51, GroupLayout.PREFERRED_SIZE)
						.addComponent(versionInfo)
						.addComponent(preference, GroupLayout.PREFERRED_SIZE, 62, GroupLayout.PREFERRED_SIZE))
					.addGap(13))
		);
		contentPane.setLayout(gl_contentPane);
	}

	public void setScanRun(boolean scanRun) {
		if(scanRun) {
			startButton.setText(null);
			startButton.setToolTipText("Syncronisierung läuft....");
			startButton.setIcon(loadingGif);
			startButton.setSelectedIcon(loadingGif);
		}else {
			startButton.setIcon(null);
			startButton.setText("Start Sync ");
			startButton.setToolTipText("Startet die Syncronisation");
		}
	}

	public void setSourceLabel(ArrayList<Path> paths) {
		StringBuffer sb = new StringBuffer();
		for(Path path : paths) {
			sb.append(path + System.lineSeparator() );
		}
		sourceLabel.setText(sb.toString());
		sourceLabel.setToolTipText(sb.toString());
	}
	
	public void setDestLabel(String str) {
		destLabel.setText(str);
		destLabel.setToolTipText(destLabel.getText());
	}

	public void setTextArea(String str) {
		sb_textArea.append(System.lineSeparator() + str);
		textArea.setText(sb_textArea.toString());
		textArea.doLayout();
		scrollPane.doLayout();
		scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
	}
	
	public JButton getExitButton() {
		return exitButton;
	}
	public JButton getSelectButton() {
		return selectButton;
	}
	public JButton getStartButton() {
		return startButton;
	}
	public JButton getPreference() {
		return preference;
	}
	public JLabel getDestLabel() {
		return destLabel;
	}
	public JTextArea getSourceLabel() {
		return sourceLabel;
	}
	public JCheckBox getLogOnBox() {
		return logOnBox;
	}
	public JComboBox<String> getScanModeComboBox() {
		return deepScanComboBox;
	}
	public JCheckBox getAutoDelCheck() {
		return autoDelCheck;
	}
	public JCheckBox getPlatzhalter() {
		return platzhalter;
	}
	public JCheckBox getAutoSyncCheck() {
		return autoSyncCheck;
	}
	public JCheckBox getBgSyncCheck() {
		return bgSyncCheck;
	}
	public JCheckBox getTrashbinCheck() {
		return trashbinCheck;
	}
	public JComboBox<String> getBgTimeComboBox() {
		return bgTimeComboBox;
	}
}
