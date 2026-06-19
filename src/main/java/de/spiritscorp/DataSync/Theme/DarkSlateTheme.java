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
package de.spiritscorp.DataSync.Theme;

import javafx.scene.Scene;

public class DarkSlateTheme implements AppTheme {
	@Override
	public void apply(Scene scene) {
		scene.getRoot().setStyle("-fx-font-size: 14; -fx-font-family: 'Comic Sans MS', 'Segoe UI'; -fx-background-color: #f4f6f9;");
		final String css = """
					.list-view {
				    -fx-background-color: transparent;
				    -fx-background-insets: 0;
				    -fx-padding: 0;
				}
				.list-cell {
				    -fx-background-color: transparent;
				    -fx-text-fill: #b8c7ce;
				    -fx-padding: 12px 16px;
				    -fx-font-size: 13px;
				    -fx-border-color: #243342;
				    -fx-border-width: 0 0 1px 0;
				}
				.list-cell:hover {
				    -fx-background-color: #34495e;
				    -fx-text-fill: #ffffff;
				}
				.list-cell:selected {
				    -fx-background-color: #3498db;
				    -fx-text-fill: #ffffff;
				    -fx-font-weight: bold;
				}
				.context-menu {
				    -fx-background-color: #ffffff;
				    -fx-border-color: #bdc3c7;
				    -fx-border-radius: 4px;
				    -fx-padding: 5px;
				}
				.button {
				    -fx-background-radius: 4px;
				    -fx-cursor: hand;
				}
				.text-field {
				    -fx-background-radius: 4px;
				    -fx-border-radius: 4px;
				    -fx-border-color: #bdc3c7;
				    -fx-background-color: #ffffff;
				    -fx-text-fill: #2c3e50;
				}
				.text-field:focused {
				    -fx-border-color: #3498db;
				}
				/* High-contrast ComboBox styling preventing transparency bugs */
				.combo-box {
				    -fx-background-color: #ffffff;
				    -fx-background-radius: 4px;
				    -fx-border-radius: 4px;
				    -fx-border-color: #bdc3c7;
				    -fx-text-fill: #2c3e50;
				}
				.combo-box:hover {
				    -fx-border-color: #95a5a6;
				}
				.combo-box:focused {
				    -fx-border-color: #3498db;
				}
				.combo-box .list-cell {
				    -fx-background-color: #ffffff;
				    -fx-text-fill: #2c3e50;
				    -fx-border-width: 0;
				}
				.combo-box .list-cell:hover, .combo-box .list-cell:selected {
				    -fx-background-color: #3498db;
				    -fx-text-fill: #ffffff;
				}
				.check-box .box {
				    -fx-background-radius: 3px;
				    -fx-border-radius: 3px;
				}
				""";
		scene.getStylesheets().add("data:text/css," + css.replace("\n", "").replace(" ", "%20"));
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() { return "Dark Slate Theme"; }
}