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
 * MatrixTheme provides a retro cyberpunk/terminal look with high-contrast green text
 * on a pitch-black background.
 */
public class MatrixTerminalTheme implements AppTheme {
	@Override
	public void apply( Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-background-color: #050505; -fx-text-fill: #00ff00;" );
		final String css = """
				.list-view { -fx-background-color: #050505; }
				.list-cell { -fx-background-color: #050505; -fx-text-fill: #00ff00; -fx-padding: 10px; -fx-border-color: #003300; -fx-border-width: 0 0 1px 0; }
				.list-cell:hover { -fx-background-color: #001a00; }
				.list-cell:selected { -fx-background-color: #003300; -fx-text-fill: #ffffff; -fx-border-color: #00ff00; }
				.label { -fx-text-fill: #00ff00 !important; }
				.button { -fx-background-color: #002200; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: hand; }
				.button:hover { -fx-background-color: #00ff00; -fx-text-fill: #000000; }
				.text-field, .combo-box, .text-area { -fx-background-color: #000000; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-border-radius: 0; }
				.text-field:focused, .combo-box:focused, .text-area:focused { -fx-border-color: #000000; }

				.status-success { -fx-text-fill: #22aa22; -fx-font-weight: bold; }
				.status-error   { -fx-text-fill: #ff3333; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #ffaa00; -fx-font-weight: bold; }

				/* Extended JavaFX Controls */
				.table-view { -fx-background-color: #050505; -fx-border-color: #00ff00; }
				.table-view .column-header, .table-view .filler { -fx-background-color: #001a00; -fx-border-color: #003300; }
				.table-view .column-header .label { -fx-text-fill: #00ff00; }
				.table-row-cell { -fx-background-color: #050505; -fx-text-fill: #00ff00; }
				.table-row-cell:odd { -fx-background-color: #0a0a0a; }
				.table-row-cell:selected { -fx-background-color: #003300; }
				.table-row-cell:selected .text { -fx-fill: #000000; }

				.tab-pane .tab-header-area .tab-header-background { -fx-background-color: #000000; }
				.tab { -fx-background-color: #002200; -fx-border-color: #00ff00; }
				.tab:selected { -fx-background-color: #00ff00; }
				.tab:selected .tab-label { -fx-text-fill: #000000 !important; }
				.tab .tab-label { -fx-text-fill: #00ff00; }

				.progress-bar .track { -fx-background-color: #404a00; -fx-background-radius: 0; }
				.progress-bar .bar { -fx-background-color: #006fa0; -fx-background-radius: 0; }

				.check-box .box { -fx-background-color: #000000;  -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 3; }
				.check-box .text { -fx-fill: #00ff00;  -fx-background-color: #00ff00; }
				.check-box:selected .mark { -fx-background-color: #00ff00; }


				.scroll-pane, .scroll-pane .viewport {
				    -fx-background-color: #050505 !important;
				    -fx-background: #050505 !important; /* Wichtig für den leeren ScrollPane-Hintergrund */
				}
				.text-area {
				    -fx-background-color: #050505 !important;
				    -fx-text-fill: #00ff00 !important;
				}
				.text-area .content {
				    -fx-background-color: #050505 !important;
				    -fx-text-fill: #00ff00 !important;
				}
				.scroll-pane .scroll-bar:vertical, .scroll-pane .scroll-bar:horizontal {
				    -fx-background-color: #0d0d0d !important;
				}
				.scroll-pane .scroll-bar .thumb {
				    -fx-background-color: #002200 !important;
				    -fx-border-color: #00ff00 !important;
				}
				.menu-button {
				    -fx-background-color: #002200;
				    -fx-text-fill: #00ff00;
				    -fx-border-color: #00ff00;
				    -fx-background-radius: 0;
				    -fx-border-radius: 0;
				}
				.menu-item {
				    -fx-background-color: #002200;
				    -fx-text-fill: #00ff00;
				    -fx-border-color: #00ff00;
				    -fx-background-radius: 0;
				    -fx-border-radius: 0;
				}
				.menu-button:hover {
				    -fx-background-color: #006600;
				    -fx-text-fill: #ff88ff;
				}
				.dialog-pane {
				    -fx-background-color: #050505 !important;
				    -fx-border-color: #00ff00 !important;
				    -fx-border-width: 1px !important;
				}
				.dialog-pane .header-panel {
				    -fx-background-color: #050505 !important;
				    -fx-border-color: transparent !important; /* Entfernt die weiße/graue Trennlinie */
				}
				.dialog-pane .content-panel,
				.dialog-pane .button-bar {
				    -fx-background-color: #050505 !important;
				}
				.dialog-pane .label {
				    -fx-text-fill: #00ff00 !important;
				}
				.dialog-pane .button {
				    -fx-background-color: #002200 !important;
				    -fx-text-fill: #00ff00 !important;
				    -fx-border-color: #00ff00 !important;
				    -fx-background-radius: 0 !important;
				    -fx-border-radius: 0 !important;
				}
				.dialog-pane .button:hover {
				    -fx-background-color: #00ff00 !important;
				    -fx-text-fill: #000000 !important;
				}
				.combo-box:hover {
				    -fx-border-color: #00ff00;
				}
				.combo-box .list-cell {
				    -fx-background-color: #00ff00;
				    -fx-text-fill: #252525;
				}
				.combo-box .list-cell:hover, .combo-box .list-cell:selected {
				    -fx-background-color: #252525;
				    -fx-text-fill: #00ff00;
				}
					""";
		scene.getStylesheets().clear();
		scene.getStylesheets().add( "data:text/css," + css.replace( "\n", "" ).replace( " ", "%20" ) );
	}

	@Override
	public String getName() { return "Matrix Terminal"; }

	@Override
	public String toString() {
		return getName();
	}
}