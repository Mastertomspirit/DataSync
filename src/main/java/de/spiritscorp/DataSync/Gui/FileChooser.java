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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.DimensionUIResource;

public class FileChooser extends JFileChooser {

	private static final long serialVersionUID = -4898558728203521262L;
	private final int width = 1000, height = 500;

	/**
	 * The file chooser
	 * 
	 * @param titel Witch kind of directory
	 * @param currentPath The current path where the fileChooser starts
	 */
	public FileChooser(String titel, Path currentPath) {
		if(currentPath != null && Files.exists(currentPath)) {
			setCurrentDirectory(currentPath.toFile());
		}else {
			setCurrentDirectory(new File(System.getProperty("user.home")));
		}
		setPreferredSize(new DimensionUIResource(width, height));
		setDialogTitle(titel + "-Verzeichnis auswählen");
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		setMultiSelectionEnabled(true);
		setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(new FileNameExtensionFilter("Nur Verzeichnisse", "*.*"));
			
		showDialog(FileChooser.this, "Auswählen");
	}
}
