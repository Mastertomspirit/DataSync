package de.spiritscorp.datasync.theme;

/*-
 * 		Data Sync
 *
 * 		Copyright ©   2022    The Spirit
 * 		@email                        thespirit@spiritscorp.network
 *
 * 		This program is free software; you can redistribute it and/or modify
 * 		it under the terms of the GNU General Public License as published by
 * 		the Free Software Foundation; either version 3 of the License, or
 * 		(at your option) any later version.
 *
 * 		This program is distributed in the hope that it will be useful,
 * 		but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 		See the GNU General Public License for more details.
 *
 * 		You should have received a copy of the GNU General Public License
 * 		along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import javafx.scene.Scene;

/**
 * NordLightTheme provides a clean, modern light theme based on the Nord color palette.
 */
public class NordicLightTheme implements AppTheme {
	@Override
	public void apply( Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Comic Sans MS', 'Segoe UI'; -fx-background-color: #f4f6f9;" );
		final String css = """
				.list-view { -fx-background-color: #e5e9f0; }
				.list-cell { -fx-background-color: #e5e9f0; -fx-text-fill: #4c566a; -fx-padding: 12px; -fx-border-color: #d8dee9; -fx-border-width: 0 0 1px 0; }
				.list-cell:hover { -fx-background-color: #d8dee9; }
				.list-cell:selected { -fx-background-color: #88c0d0; -fx-text-fill: #2e3440; -fx-font-weight: bold; }
				.label { -fx-text-fill: #2e44a4 !important; }
				.button { -fx-background-color: #81a1c1; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;}
				.button:hover { -fx-background-color: #5e81ac; }
				.text-field, .combo-box, .text-area { -fx-background-color: #ffffff; -fx-text-fill: #2e3440; -fx-border-color: #d8dee9; -fx-background-radius: 4px; -fx-border-radius: 4px; }
				.text-field:focused, .combo-box:focused, .text-area:focused { -fx-border-color: #3498db; }
				.combo-box:hover { -fx-border-color: #95a5a6; }
				.combo-box .list-cell { -fx-background-color: #ffffff; -fx-text-fill: #2e3440; }
				.combo-box .list-cell:selected { -fx-background-color: #88c0d0; -fx-text-fill: #2e3440; }
				.status-success { -fx-text-fill: #22aa22; -fx-font-weight: bold; }
				.status-error { -fx-text-fill: #ff3333; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #ffaa00; -fx-font-weight: bold; }

				/* Extended JavaFX Controls */
				.table-view { -fx-background-color: #ffffff; -fx-border-color: #d8dee9; }
				.table-view .column-header, .table-view .filler { -fx-background-color: #e5e9f0; -fx-border-color: #d8dee9; }
				.table-view .column-header .label { -fx-text-fill: #4c566a; }
				.table-row-cell { -fx-background-color: #ffffff; -fx-text-fill: #2e3440; }
				.table-row-cell:odd { -fx-background-color: #f8fafc; }
				.table-row-cell:selected { -fx-background-color: #88c0d0; }

				.tab-pane .tab-header-area .tab-header-background { -fx-background-color: #e5e9f0; }
				.tab { -fx-background-color: #d8dee9; -fx-background-radius: 4px 4px 0 0; }
				.tab:selected { -fx-background-color: #81a1c1; }
				.tab:selected .tab-label { -fx-text-fill: #ffffff !important; }
				.tab .tab-label { -fx-text-fill: #4c566a; }

				.progress-bar .track { -fx-background-color: #e5e9f0; -fx-background-radius: 4px; }
				.progress-bar .bar { -fx-background-color: #81a1c1; -fx-background-radius: 4px; }
				.dialog-pane { -fx-background-color: #ffffff; }
				.check-box .box { -fx-background-color: #ffffff; -fx-border-color: #d8dee9; -fx-background-radius: 3px; }
				.check-box:selected .mark { -fx-background-color: #81a1c1; }
				""";
		scene.getStylesheets().clear();
		scene.getStylesheets().add( "data:text/css," + css.replace( "\n", "" ).replace( " ", "%20" ) );
	}

	@Override
	public String getName() { return "Nordic Light"; }

	@Override
	public String toString() {
		return getName();
	}
}