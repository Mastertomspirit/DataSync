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

public class MatrixTerminalTheme implements AppTheme {
	@Override
	public void apply(Scene scene) {
		scene.getRoot().setStyle("-fx-font-size: 14; -fx-font-family: 'Consolas', monospace; -fx-background-color: #0d0d0d;");
		final String css = """
				.list-view { -fx-background-color: #050505; }
				.list-cell { -fx-background-color: #050505; -fx-text-fill: #00ff00; -fx-padding: 10px; -fx-border-color: #003300; -fx-border-width: 0 0 1px 0; }
				.list-cell:hover { -fx-background-color: #001a00; }
				.list-cell:selected { -fx-background-color: #003300; -fx-text-fill: #ffffff; -fx-border-color: #00ff00; }
				.label { -fx-text-fill: #00ff00 !important; }
				.button { -fx-background-color: #002200; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 0; -fx-border-radius: 0; }
				.button:hover { -fx-background-color: #00ff00; -fx-text-fill: #000000; }
				.text-field, .combo-box, .text-area { -fx-background-color: #000000; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-border-radius: 0; }
				.combo-box .list-cell { -fx-background-color: #000000; -fx-text-fill: #00ff00; }
				.combo-box .list-cell:selected { -fx-background-color: #00ff00; -fx-text-fill: #000000; }
				.scroll-pane { -fx-background-color: #0d0d0d; }
				.status-success { -fx-text-fill: #22aa22; -fx-font-weight: bold; }
				.status-error   { -fx-text-fill: #ff3333; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #ffaa00; -fx-font-weight: bold; }
				""";
		scene.getStylesheets().add("data:text/css," + css.replace("\n", "").replace(" ", "%20"));
	}

	@Override
	public String getName() { return "Matrix Terminal (Sci-Fi)"; }

	@Override
	public String toString() {
		return getName();
	}
}