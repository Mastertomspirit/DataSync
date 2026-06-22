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
	requires transitive javafx.graphics;

	requires transitive org.kordamp.ikonli.core;
	requires transitive org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.materialdesign2;

	exports de.spiritscorp.DataSync.Gui to javafx.graphics;

	exports de.spiritscorp.DataSync.Controller to de.spiritscorp.DataSync.Gui;
	exports de.spiritscorp.DataSync.Model to de.spiritscorp.DataSync.Gui;
	exports de.spiritscorp.DataSync.IO to de.spiritscorp.DataSync.Gui;
	exports de.spiritscorp.DataSync.Theme to de.spiritscorp.DataSync.Gui;
	exports de.spiritscorp.DataSync to de.spiritscorp.DataSync.Controller;

//	opens de.spiritscorp.DataSync.Gui;

	// Falls deine Main-Klasse direkt im Root-Paket liegt:
//	exports de.spiritscorp.DataSync to javafx.graphics;

//	opens de.spiritscorp.DataSync.Model;

	requires transitive jakarta.json;
	requires transitive java.desktop;
	requires javafx.base;
}
