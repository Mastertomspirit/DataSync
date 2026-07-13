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
 * DarkSlateTheme provides a professional dark mode interface using slate and deep blue tones.
 */
public class DarkSlateTheme implements AppTheme {
	@Override
	public void apply( final Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Comic Sans MS', 'Segoe UI'; -fx-background-color: #2c3e50; -fx-text-fill: #b9c0c7;" );
		final String css = """
				/* Extended JavaFX Controls */
				.tooltip { -fx-background-color: black !important; -fx-text-fill: #b9c0c7 !important; -fx-font-weight: normal !important; }
				.label { -fx-text-fill: #b9c0c7; }
				.button { -fx-background-color: #34495e; -fx-text-fill: #b9c0c7; -fx-border-color: #3498db; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-cursor: hand; -fx-padding: 8px 16px; }
				.button:hover { -fx-background-color: #415b76; -fx-border-color: #3498db; }

				/* List View */
				.list-view { -fx-background-color: #2c3e50 !important; -fx-border-color: #506880; -fx-border-width: 1px;}
				.list-cell { -fx-background-color: #2c3e50; -fx-text-fill: #b9c0c7; -fx-padding: 12px; -fx-border-color: #506880; -fx-border-width: 0 0 1px 0; -fx-cursor: hand;}
				.list-cell:hover { -fx-background-color: #34495e; }
				.list-cell:selected { -fx-background-color: #3498db; -fx-text-fill: #b9c0c7;  -fx-font-weight: bold; }

				/* Form Inputs & Dropdowns */
				.text-field, .text-area { -fx-background-color: #b9c0c7; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7;  -fx-border-width: 1px; -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.text-field:focused, .combo-box:focused, .text-area:focused { -fx-border-color: #3498db; }
				.text-area, .text-area .content { -fx-background-color: #2c3e50; -fx-text-fill: #b9c0c7 !important; }

				/* Dropdown Menu Button */
				 .menu-button, .menu-item { -fx-background-color: #415b75 !important; -fx-text-fill: #b9c0c7 !important; -fx-border-color: #2c3e50; -fx-background-radius: 16px; -fx-border-radius: 16px; }
				.menu-button .label { -fx-text-fill: #b9c0c7 !important; }
				.menu-button:hover { -fx-background-color: #506f8f !important;  -fx-border-color: #3498db; -fx-cursor: hand; }
				.menu-button:hover .label { -fx-text-fill: #b9c0c7; }

				/* Context Menu */
				.context-menu { -fx-background-color: #b9c0c7 !important; -fx-border-color: #bdc3c7 !important; -fx-border-width: 1px; -fx-padding: 5px; -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.context-menu .menu-item { -fx-background-color: #b9c0c7 !important;  -fx-background-radius: 16px; -fx-border-radius: 16px;}
				.context-menu .menu-item .label { -fx-text-fill: #2c3e50 !important; }
				.context-menu .menu-item:focused, .context-menu .menu-item:hover { -fx-background-color: #3498db !important; }
				.context-menu .menu-item:focused .label, .context-menu .menu-item:hover .label { -fx-text-fill: #b9c0c7 !important; -fx-cursor: hand; }
				.context-menu .separator .line { -fx-border-color: #9da3a7 !important; }

				/* Table View */
				.table-view { -fx-background-color: #2c3e50; -fx-border-color: black; -fx-border-width: 1px; }
				.table-view .column-header, .table-view .filler { -fx-background-color: #2c3e50; -fx-border-color: #3498db; }
				.table-view .column-header .label { -fx-text-fill: #b9c0c7 !important; -fx-font-weight: bold; }
				.table-row-cell { -fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-border-color: #e2e6ea; -fx-border-width: 0 0 1px 0; }
				.table-row-cell:odd { -fx-background-color: #f4f6f9; }
				.table-row-cell:selected { -fx-background-color: #2980b9; -fx-text-fill: #b9c0c7 !important; }
				.table-row-cell:selected .text { -fx-fill: #ffffff !important; }

				/* Scroll Pane */
				.scroll-pane { -fx-background-color: #2c3e50; -fx-background: #f4f6f9 !important; -fx-text-fill: #b9c0c7; -fx-border-color: #bdc3c7 !important; -fx-border-width: 1px !important; }
				.scroll-pane .viewport{ -fx-background-color: #2c3e50; -fx-background: #f4f6f9 !important; -fx-text-fill: #b9c0c7; }
				.scroll-pane .label { -fx-text-fill: #b9c0c7 !important; }
				.scroll-pane .scroll-bar:vertical, .scroll-pane .scroll-bar:horizontal { -fx-background-color: #e2e6ea !important; }
				.scroll-pane .scroll-bar .thumb { -fx-background-color: #bdc3c7 !important; -fx-border-color: #95a5a6 !important; -fx-background-radius: 4px !important; }

				/* Progress Bar */
				.progress-bar .track { -fx-background-color: #e2e6ea; -fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-radius: 4px; }
				.progress-bar .bar { -fx-background-color: #3498db !important; -fx-background-radius: 4px; -fx-background-insets: 0; }

				/* Dialog */
				.dialog-pane { -fx-background-color: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-width: 1px; }
				.dialog-pane .header-panel { -fx-background-color: #2c3e50; -fx-border-color: transparent; }
				.dialog-pane .content-panel, .dialog-pane .button-bar { -fx-background-color: #2c3e50; }
				.dialog-pane .label, .dialog-custom-icon {  -fx-text-fill: #b9c0c7; -fx-fill: #b9c0c7; }
				.dialog-pane .button { -fx-background-color: #34495e; -fx-text-fill: #b9c0c7; -fx-border-color: #2c3e50; }
				.dialog-pane .button:hover { -fx-background-color: #3498db; -fx-text-fill: #b9c0c7; }

				/* Combo Box */
				.combo-box { -fx-background-color: #2488cb; -fx-border-color: #3498db; -fx-text-fill: #b9c0c7 !important;  -fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand; }
				.combo-box:hover { -fx-border-color: #000000; }
				.combo-box .list-cell { -fx-background-color: #b9c0c7 !important; -fx-text-fill: #2c3e50 !important; -fx-border-width: 0; -fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand; }
				.combo-box .list-cell:hover, .combo-box .list-cell:selected { -fx-background-color: #3498db !important; -fx-text-fill: #b9c0c7 !important; }

				/* Check Box */
				.check-box .box { -fx-background-color: #b9c0c7; -fx-border-color: #f4f6f9; -fx-border-width: 1px; -fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;  }
				.check-box:hover .box { -fx-border-color: #3498db; }
				.check-box .text { -fx-fill: #b9c0c7 !important; -fx-cursor: hand; }
				.check-box:selected .mark { -fx-background-color: #3498db; }

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
				.settings-grid { -fx-background-color: #2c3e50; }
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
				.info-box { -fx-background-color: #2c3e50; -fx-padding: 15px; }
				.app-title-label { -fx-font-size: 18px; -fx-font-weight: bold; }
				.legal-text { -fx-font-family: monospace; }

				/* Status messages */
				.status-success { -fx-text-fill: #27ae60; -fx-font-weight: bold; }
				.status-error   { -fx-text-fill: #c0392b; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #f39c12; -fx-font-weight: bold; }
				""";

		scene.getStylesheets().clear();
		scene.getStylesheets().add( "data:text/css," + css.replace( "\n", "" ).replace( " ", "%20" ) );
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() { return "Dark Slate"; }
}