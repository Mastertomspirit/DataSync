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
	public void apply( final Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Comic Sans MS', 'Segoe UI'; -fx-background-color: #f4f6f9; -fx-text-fill: #2e44a4;" );
		final String css = """
				/* Extended JavaFX Controls */
				.tooltip { -fx-background-color: #88c0d0 !important; -fx-text-fill: #2e44a4 !important; -fx-font-weight: normal !important; }
				.label { -fx-text-fill: #2e44a4; }
				.button { -fx-background-color: #88c0d0; -fx-text-fill: #2e44a4; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-cursor: hand; }
				.button:hover { -fx-background-color: #2e44a4; -fx-text-fill: #88c0d0; }

				/* List View */
				.list-view { -fx-background-color: #e0e0e7; -fx-border-color: #d8dee9; -fx-border-width: 1px; }
				.list-cell { -fx-background-color: #e0e0e7; -fx-text-fill: #2e44a4; -fx-padding: 12px; -fx-border-color: #d8dee9; -fx-border-width: 0 0 1px 0; -fx-cursor: hand; }
				.list-cell:hover { -fx-background-color: #88c0d0; }
				.list-cell:selected { -fx-background-color: #88c0d0; -fx-text-fill: #2e44a4 !important; -fx-font-weight: bold; }

				/* Form Inputs & Dropdowns */
				.text-field, .text-area, .text-area .content { -fx-background-color: #ffffff; -fx-text-fill: #2e3440; -fx-border-color: #d8dee9; -fx-background-radius: 10px; -fx-border-radius: 10px; }
				.text-field:focused, .combo-box:focused, .text-area:focused { -fx-border-color: #3498db; }

				/* Dropdown Menu Button */
				 .menu-button, .menu-item { -fx-background-color: #88c0d0; -fx-text-fill: #2e44a4; -fx-border-color: #2c3e50; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-cursor: hand; }
				.menu-button .label { -fx-text-fill: #2e44a4 !important; }
				.menu-button:hover { -fx-background-color: #2e44a4;  -fx-border-color: #3498db; }
				.menu-button:hover .label { -fx-text-fill: #88c0d0 !important; }

				/* Context Menu */
				.context-menu { -fx-background-color: #b9c0c7 !important; -fx-border-color: #bdc3c7 !important; -fx-border-width: 1px !important; -fx-padding: 5px !important; -fx-border-radius: 10px !important; }
				.context-menu .menu-item { -fx-background-color: #b9c0c7 !important;  -fx-background-radius: 16px; -fx-border-radius: 16px;}
				.context-menu .menu-item .label { -fx-text-fill: #2c3e50 !important; }
				.context-menu .menu-item:focused, .context-menu .menu-item:hover { -fx-background-color: #3498db !important; -fx-cursor: hand; }
				.context-menu .menu-item:focused .label, .context-menu .menu-item:hover .label { -fx-text-fill: #b9c0c7 !important; }
				.context-menu .separator .line { -fx-border-color: #9da3a7 !important; }

				/* Table View */
				.table-view { -fx-background-color: #ffffff; -fx-border-color: #88c0d0; }
				.table-view .column-header, .table-view .filler { -fx-background-color: #e5e9f0; -fx-border-color: #88c0d0; }
				.table-view .column-header .label { -fx-text-fill: #2e44a4; }
				.table-row-cell { -fx-background-color: #ffffff; -fx-text-fill: #2e4494; }
				.table-row-cell:odd { -fx-background-color: #f8fafc; }
				.table-row-cell:selected { -fx-background-color: #88c0d0; }
				.table-row-cell:selected .text { -fx-fill: #000000; }

				/* Scroll Pane */
				.scroll-pane{ -fx-background-color: #f4f6f9; -fx-background: #e0e0e7 !important; -fx-text-fill: #2e44a4; -fx-border-color: #bdc3c7 !important; -fx-border-width: 1px !important; }
				.scroll-pane .viewport{ -fx-background-color: #f4f6f9; -fx-background: #e0e0e7 !important; -fx-text-fill: #2e44a4; }
				.scroll-pane .label { -fx-text-fill: #2e44a4 !important; }
				.scroll-pane .scroll-bar:vertical, .scroll-pane .scroll-bar:horizontal { -fx-background-color: #f4f6f9 !important; }
				.scroll-pane .scroll-bar .thumb { -fx-background-color: #bdc3c7 !important; -fx-border-color: #95a5a6 !important; -fx-background-radius: 4px !important; }

				/* Progress Bar */
				.progress-bar .track { -fx-background-color: #e5e9f0; -fx-background-radius: 4px; }
				.progress-bar .bar { -fx-background-color: #81a1c1; -fx-background-radius: 4px; }

				/* Dialog */
				.dialog-pane { -fx-background-color: #ffffff; }

				/* Combo Box */
				.combo-box { -fx-background-color: #78b0c0; -fx-border-color: #95a5a6; -fx-text-fill: #2e44a4 !important;
				-fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand;
				}
				.combo-box:hover { -fx-border-color: #000000; }
				.combo-box .list-cell { -fx-background-color: #ffffff; -fx-text-fill: #2e44a4; -fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand; }
				.combo-box .list-cell:hover, .combo-box .list-cell:selected { -fx-background-color: #88c0d0; -fx-text-fill: #2e44a4 !important; }

				/* Check Box */
				.check-box .box { -fx-background-color: #ffffff; -fx-border-color: #d8dee9;  -fx-background-radius: 8px; -fx-border-radius: 8px;  -fx-cursor: hand; }
				.check-box:hover .box { -fx-border-color: #3498db; -fx-background-color: rgb(52, 152, 219, 0.15); }
				.check-box .text { -fx-fill: #2e44a4;  -fx-background-color: #2e44a4; -fx-cursor: hand;}
				.check-box:selected .mark { -fx-background-color: #81a1c1; }

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
				.settings-grid { -fx-background-color: #e0e0e7; }
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
				.info-box { -fx-background-color: #e0e0e7; -fx-padding: 15px; }
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
	public String getName() { return "Nordic Light"; }

	@Override
	public String toString() {
		return getName();
	}
}