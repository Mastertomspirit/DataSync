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

/**
 * DarkSlateTheme provides a professional dark mode interface using slate and deep blue tones.
 */
public class DarkSlateTheme implements AppTheme {
	// CHECKSTYLE:OFF
	@Override
	public void apply( Scene scene ) {
		scene.getRoot().setStyle( "-fx-font-size: 14; -fx-font-family: 'Comic Sans MS', 'Segoe UI'; -fx-background-color: #f4f6f9;" );
		final String css = """
				/* Global & Core Controls */
				.label {
				    -fx-text-fill: #202020;
				}
				.vbox, .hbox, .grid-pane {
				    -fx-background-color: #f4f6f9;
				}

				/* Sidebar / ListView (Die linke Tabelle komplett überarbeitet) */
				.list-view {
				    -fx-background-color: #2c3e50; /* Dunkleres Blaugrau für die gesamte Sidebar */
				    -fx-border-color: #1a252f;
				    -fx-border-width: 0 1px 0 0; /* Trennlinie nach rechts */
				}
				.list-cell {
				    -fx-background-color: #2c3e50;
				    -fx-text-fill: #ecf0f1; /* Deutlich lesbareres, helles Grau/Weiß */
				    -fx-padding: 12px 16px;
				    -fx-border-color: #34495e;
				    -fx-border-width: 0 0 1px 0; /* Klare Untertrennung der Zeilen */
				}
				.list-cell:hover {
				    -fx-background-color: #34495e; /* Subtiler Hover-Effekt innerhalb der Sidebar */
				    -fx-text-fill: #ffffff;
				}
				.list-cell:selected {
				    -fx-background-color: #3498db; /* Kräftiges Navigations-Blau */
				    -fx-text-fill: #ffffff;
				    -fx-border-color: #2980b9;
				    -fx-font-weight: bold;
				}

				/* Scroll Panes (Zentraler Viewport) */
				.scroll-pane, .scroll-pane .viewport{
				    -fx-background-color: #2c3e50;
				    -fx-background: #f4f6f9 !important;
					-fx-text-fill: #ffffff;
					-fx-border-color: #bdc3c7 !important; /* Rahmen um das Hauptfenster */
				    -fx-border-width: 1px !important;
				}
				.scroll-pane .label {
				    -fx-text-fill: #a0a0a0 !important;
				}
				.scroll-pane .scroll-bar:vertical, .scroll-pane .scroll-bar:horizontal {
				    -fx-background-color: #e2e6ea !important;
				}
				.scroll-pane .scroll-bar .thumb {
				    -fx-background-color: #bdc3c7 !important;
				    -fx-border-color: #95a5a6 !important;
				    -fx-background-radius: 4px !important;
				}

				/* Buttons (Lesbarkeit maximiert durch blaugrauen Body & Rahmen) */
				.button {
				    -fx-background-color: #34495e;
				    -fx-text-fill: #ffffff !important;
				    -fx-border-color: #2c3e50;
				    -fx-border-width: 1px;
				    -fx-background-radius: 4px;
				    -fx-border-radius: 4px;
				    -fx-cursor: hand;
				    -fx-padding: 8px 16px;
				}
				.button:hover {
				    -fx-background-color: #415b76;
				    -fx-border-color: #3498db; /* Blaues Aufleuchten beim Hovern */
				}

				/* Form Inputs & Dropdowns */
				.text-field, .combo-box, .text-area {
				    -fx-background-color: #ffffff;
				    -fx-text-fill: #2c3e50;
				    -fx-border-color: #bdc3c7; /* Klarer Rahmen für Textfelder */
				    -fx-border-width: 1px;
				    -fx-border-radius: 4px;
				    -fx-background-radius: 4px;
				}
				.text-field:focused, .combo-box:focused, .text-area:focused {
				    -fx-border-color: #3498db; /* Fokus-Blau */
				}

				.text-area, .text-area .content {
				    -fx-background-color: #2c3e50;
				    -fx-text-fill: #ffffff !important;
				}

				/* ComboBox Dropdown-Zellen */
				.combo-box:hover {
				    -fx-border-color: #3498db;
				}
				.combo-box .list-cell {
				    -fx-background-color: #ffffff !important;
				    -fx-text-fill: #2c3e50 !important;
				    -fx-border-width: 0;
				}
				.combo-box .list-cell:hover, .combo-box .list-cell:selected {
				    -fx-background-color: #3498db !important;
				    -fx-text-fill: #ffffff !important;
				}

				/* CheckBox (Mehr Blau & klare Umrandung) */
				.check-box .box {
				    -fx-background-color: #ffffff;
				    -fx-border-color: #bdc3c7;
				    -fx-border-width: 1px;
				    -fx-background-radius: 3px;
				    -fx-border-radius: 3px;
				}
				.check-box:hover .box {
				    -fx-border-color: #3498db;
				}
				.check-box .text {
				    -fx-fill: #ffffff !important;
				}
				.check-box:selected .mark {
				    -fx-background-color: #3498db; /* Blaues Häkchen */
				}

				/* Dropdown-Menü-Buttons & Kontextmenüs */
				 .menu-button, .menu-item {
				    -fx-background-color: #34495e;
				    -fx-text-fill: white;
				    -fx-border-color: #2c3e50;
				    -fx-background-radius: 4px;
				    -fx-border-radius: 4px;
				}
				.menu-button .label {
				    -fx-text-fill: #ffffff !important;
				}
				.menu-button:hover {
				    -fx-background-color: #415b76;
				    -fx-border-color: #3498db;
				}
				/* Wenn man die Maus über das geöffnete Menü bewegt */
				.menu-button:hover .label {
				    -fx-text-fill: #ffffff !important;
				}
				.context-menu {
				    -fx-background-color: #ffffff !important;
				    -fx-border-color: #bdc3c7 !important;
				    -fx-border-width: 1px !important;
				    -fx-padding: 5px !important;
				    -fx-border-radius: 4px !important;
				}
				.context-menu .menu-item {
				    -fx-background-color: #ffffff !important;
				}
				.context-menu .menu-item .label {
				    -fx-text-fill: #2c3e50 !important;
				}
				.context-menu .menu-item:focused, .context-menu .menu-item:hover {
				    -fx-background-color: #3498db !important;
				}
				.context-menu .menu-item:focused .label, .context-menu .menu-item:hover .label {
				    -fx-text-fill: #ffffff !important;
				}
				.context-menu .separator .line {
				    -fx-border-color: #bdc3c7 !important;
				}

				.table-view {
				    -fx-background-color: #2c3e50;
				    -fx-border-color: #000000; /* Klarer blauer Außenrahmen */
				    -fx-border-width: 1px;
				}
				/* Tabellenkopf in hellem Navigations-Blau */
				.table-view .column-header, .table-view .filler {
				    -fx-background-color: #2c3e50;
				    -fx-border-color: #3498db; /* Etwas dunkleres Blau für die Spaltentrenner */
				}
				.table-view .column-header .label {
				    -fx-text-fill: #ffffff !important; /* Weiße Spaltenüberschriften */
				    -fx-font-weight: bold;
				}
				/* Die Tabellenzeilen selbst */
				.table-row-cell {
				    -fx-background-color: #ffffff;
				    -fx-text-fill: #2c3e50;
				    -fx-border-color: #e2e6ea; /* Subtile hellgraue Linien zwischen den Zeilen */
				    -fx-border-width: 0 0 1px 0;
				}
				/* Jede zweite Zeile leicht bläulich/grau absetzen für bessere Lesbarkeit */
				.table-row-cell:odd {
				    -fx-background-color: #f4f6f9;
				}
				/* Wenn eine Tabellenzeile ausgewählt ist */
				.table-row-cell:selected {
				    -fx-background-color: #2980b9;
				    -fx-text-fill: #ffffff !important;
				}
				.table-row-cell:selected .text {
				    -fx-fill: #ffffff !important;
				}

				/* TabPane */
				.tab-pane .tab-header-area .tab-header-background {
				    -fx-background-color: #2c3e50;
				}
				.tab {
				    -fx-background-color: #34495e;
				    -fx-border-color: #2c3e50;
				    -fx-background-radius: 4px 4px 0 0;
				    -fx-border-radius: 4px 4px 0 0;
				}
				.tab:selected {
				    -fx-background-color: #3498db;
				}
				.tab:selected .tab-label {
				    -fx-text-fill: #ffffff !important;
				}
				.tab .tab-label {
				    -fx-text-fill: #b8c7ce;
				}

				.progress-bar .track {
				    -fx-background-color: #e2e6ea;
				    -fx-border-color: #bdc3c7;
				    -fx-border-width: 1px;
				    -fx-background-radius: 4px;
				}
				.progress-bar .bar {
				    -fx-background-color: #3498db !important;
				    -fx-background-radius: 4px;
				    -fx-background-insets: 0;
				}

				.dialog-pane {
				    -fx-background-color: #2c3e50 !important;
				    -fx-border-color: #bdc3c7 !important;
				    -fx-border-width: 1px !important;
				}
				.dialog-pane .header-panel {
				    -fx-background-color: #2c3e50 !important;
				    -fx-border-color: transparent !important;
				}
				.dialog-pane .content-panel, .dialog-pane .button-bar {
				    -fx-background-color: #2c3e50 !important;
				}
				.dialog-pane .label {
				    -fx-text-fill: #ffffff !important;
				}
				.dialog-pane .button {
				    -fx-background-color: #34495e !important;
				    -fx-text-fill: white !important;
				    -fx-border-color: #2c3e50 !important;
				    -fx-background-radius: 4px !important;
				    -fx-border-radius: 4px !important;
				}
				.dialog-pane .button:hover {
				    -fx-background-color: #3498db !important;
				    -fx-text-fill: #ffffff !important;
				}
				/* Status Signals */
				.status-success { -fx-text-fill: #27ae60; -fx-font-weight: bold; }
				.status-error   { -fx-text-fill: #c0392b; -fx-font-weight: bold; }
				.status-warning { -fx-text-fill: #f39c12; -fx-font-weight: bold; }
				""";

		scene.getStylesheets().clear();
		scene.getStylesheets().add( "data:text/css," + css.replace( "\n", "" ).replace( " ", "%20" ) );
	}
	// CHECKSTYLE:OFF

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() { return "Dark Slate"; }
}