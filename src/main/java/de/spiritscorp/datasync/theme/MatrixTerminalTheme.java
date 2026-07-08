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
	public void apply( final Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-background-color: #050505; -fx-text-fill: #00ff00;" );
		final String css = """
				/* Extended JavaFX Controls */
				.tooltip { -fx-background-color: #002200 !important; -fx-text-fill: #00ff00 !important; -fx-font-weight: normal !important; }
				.label { -fx-text-fill: #00ff00; }
				.button { -fx-background-color: #002200; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-cursor: hand; }
				.button:hover { -fx-background-color: #00ff00; -fx-text-fill: #002200; }

				/* List View */
				.list-view { -fx-background-color: #050505; -fx-border-color: #003300; -fx-border-width: 1px; }
				.list-cell { -fx-background-color: #050505; -fx-text-fill: #00ff00; -fx-padding: 12px; -fx-border-color: #003300; -fx-border-width: 0 0 1px 0; }
				.list-cell:hover { -fx-background-color: #001a00; }
				.list-cell:selected { -fx-background-color: #003300; -fx-text-fill: #ffffff; -fx-border-color: #00ff00; }

				/* Form Inputs & Dropdowns */
				.text-field, .text-area { -fx-background-color: #002200; -fx-text-fill: #00ff00; -fx-border-color: #00ff00;  -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.text-field:focused, .combo-box:focused, .text-area:focused { -fx-border-color: #000000; }
				.text-area, .text-area .content { -fx-background-color: #050505 !important; -fx-text-fill: #00ff00 !important; }

				/* Dropdown Menu Button */
				.menu-button, .menu-item { -fx-background-color: #002200; -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 16px; -fx-border-radius: 16px; }
				.menu-button .label { -fx-text-fill: #00ff00; }
				.menu-button:hover { -fx-background-color: #006600;  -fx-border-color: #00ff00; -fx-cursor: hand; }
				.menu-button:hover .label { -fx-text-fill: #00ff00 !important; }

				/* Context Menu */
				.context-menu { -fx-background-color: #002200; -fx-border-color: #00ff00; -fx-border-width: 1px; -fx-padding: 5px; -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.context-menu .menu-item { -fx-background-color: #002200; -fx-background-radius: 16px; -fx-border-radius: 16px; }
				.context-menu .menu-item .label { -fx-text-fill: #00ff00; }
				.context-menu .menu-item:focused, .context-menu .menu-item:hover { -fx-background-color: #006600 !important; -fx-cursor: hand; }
				.context-menu .menu-item:focused .label, .context-menu .menu-item:hover .label { -fx-text-fill: #00ff00 !important; }
				.context-menu .separator .line { -fx-border-color: #006600 !important; }

				/* Table View */
				.table-view { -fx-background-color: #050505; -fx-border-color: #00ff00; }
				.table-view .column-header, .table-view .filler { -fx-background-color: #001a00; -fx-border-color: #003300; }
				.table-view .column-header .label { -fx-text-fill: #00ff00; }
				.table-row-cell { -fx-background-color: #050505; -fx-text-fill: #00ff00; }
				.table-row-cell:odd { -fx-background-color: #0a0a0a; }
				.table-row-cell:selected { -fx-background-color: #003300; }
				.table-row-cell:selected .text { -fx-fill: #000000; }

				/* Scroll Pane */
				.scroll-pane { -fx-background-color: #002200; -fx-background: #050505 !important; -fx-text-fill: #00ff00; -fx-border-color: #00ff00 !important; -fx-border-width: 1px !important; }
				.scroll-pane .viewport{ -fx-background-color: #002200; -fx-background: #050505 !important; -fx-text-fill: #b9c0c7; }
				.scroll-pane .label { -fx-text-fill: #00ff00 !important; }
				.scroll-pane .scroll-bar:vertical, .scroll-pane .scroll-bar:horizontal { -fx-background-color: #0d0d0d !important; }
				.scroll-pane .scroll-bar .thumb { -fx-background-color: #002200 !important; -fx-border-color: #00ff00 !important;  -fx-background-radius: 4px !important; }

				/* Progress Bar */
				.progress-bar .track { -fx-background-color: #404a00; -fx-background-radius: 0; }
				.progress-bar .bar { -fx-background-color: #006fa0; -fx-background-radius: 0; }

				/* Dialog */
				.dialog-pane { -fx-background-color: #050505 !important; -fx-border-color: #00ff00 !important; -fx-border-width: 1px !important; }
				.dialog-pane .header-panel { -fx-background-color: #050505 !important; -fx-border-color: transparent !important; }
				.dialog-pane .content-panel, .dialog-pane .button-bar { -fx-background-color: #050505 !important; }
				.dialog-pane .label { -fx-text-fill: #00ff00 !important; }
				.dialog-pane .button { -fx-background-color: #002200 !important; -fx-text-fill: #00ff00 !important; -fx-border-color: #00ff00 !important;}
				.dialog-pane .button:hover { -fx-background-color: #00ff00 !important; -fx-text-fill: #000000 !important; }

				/* Combo Box */
				.combo-box { -fx-background-color: #151515; -fx-text-fill: #00ff00 !important;  -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.combo-box:hover { -fx-border-color: #009900; -fx-cursor: hand; }
				.combo-box .list-cell { -fx-background-color: #00ff00; -fx-text-fill: #252525; -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.combo-box .list-cell:hover, .combo-box .list-cell:selected { -fx-background-color: #252525; -fx-text-fill: #00ff00; -fx-cursor: hand; }

				/* Check Box */
				.check-box .box { -fx-background-color: #000000;  -fx-text-fill: #00ff00; -fx-border-color: #00ff00; -fx-background-radius: 3; }
				.check-box:hover .box { -fx-border-color: #3498db;  -fx-cursor: hand; }
				.check-box .text { -fx-fill: #00ff00;  -fx-background-color: #00ff00; }
				.check-box:selected .mark { -fx-background-color: #00ff00; }

				/* Icons */
				.menu-icon { -fx-icon-color: black; }
				.job-icon { -fx-icon-color: #3498db; }
				.button-icon { -fx-icon-color: white; }

				/* Sidebar View */
				.sidebar-title-label { -fx-font-size: 11px; -fx-font-weight: bold; }
				.drag-over-target { -fx-border-color: #2ecc71 transparent transparent transparent; -fx-border-width: 3px 0 0 0; }
				.add-job-button { -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11px; }
				.exit-button { -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11px; }

				/* Workspace View */
				.console-text-area { -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; }
				.workspace-header-label { -fx-font-size: 22px; -fx-font-weight: bold; }
				.context-info-label { -fx-font-size: 15px; -fx-font-style: italic; -fx-padding: 0 0 8px 0; }
				.action-button { -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; }
				.cancel-button { -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; }

				/* Duplicate Button */
				.delete-button { -fx-background-color: #e74c3c; -fx-text-fill: white; }

				/* Config Menu */
				.settings-grid { -fx-background-color: #050505; }
				.mode-label { -fx-font-weight: bold; }
				.params-title-label { -fx-font-size: 14px; -fx-font-weight: bold; }
				.global-title-label { -fx-font-size: 14px; -fx-font-weight: bold; }
				.save-button { -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 24px; }

				/* Directory rendering */
				.dir-title-label { -fx-font-size: 14px; -fx-font-weight: bold; }
				.multi-src-label { -fx-font-weight: bold; }

				/*Special button hover */
				.cancel-button:hover { -fx-background-color: #7f8c8d;  -fx-text-fill: #d6e0fa;  -fx-icon-color: #d6e0fa; }
				.action-button:hover, .save-button:hover, .add-job-button:hover { -fx-background-color: #1abc9c; -fx-text-fill: #d6e0fa; -fx-icon-color: #d6e0fa; }
				.delete-button:hover, .exit-button:hover { -fx-background-color: #c0392b; -fx-text-fill: #d6e0fa; -fx-icon-color: #d6e0fa; }

				/* Info View */
				.info-box { -fx-background-color: #050505; -fx-padding: 15px; }
				.app-title-label { -fx-font-size: 18px; -fx-font-weight: bold; }
				.legal-text { -fx-font-family: monospace; }

				/* Status messages */
				.status-success { -fx-text-fill: #22aa22; -fx-font-weight: bold; }
				.status-error { -fx-text-fill: #ff3333; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #ffaa00; -fx-font-weight: bold; }
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