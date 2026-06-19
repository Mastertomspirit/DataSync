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
module de.spiritscorp.DataSync {
	requires javafx.controls;
    
    // Falls Ikonli auch noch drin ist, brauchst du diese weiterhin:
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    // 2. Das Paket mit deiner MainView für das JavaFX-Grafiksystem öffnen
    exports de.spiritscorp.DataSync.Gui to javafx.graphics;
    
    // Falls deine Main-Klasse direkt im Root-Paket liegt:
    exports de.spiritscorp.DataSync to javafx.graphics;
//	exports de.spiritscorp.DataSync;
	opens de.spiritscorp.DataSync.Model;
	//requires java.desktop;
	requires jakarta.json;
}
