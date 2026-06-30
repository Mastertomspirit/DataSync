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
 * Strategy interface definition for injecting interchangeable CSS skins into the workspace stage.
 *
 * @author Tom Spirit
 */
public interface AppTheme {
	/**
	 * Applies specific color palettes, fonts, and controls overrides to the target scene.
	 *
	 * @param scene The active window scene graph context.
	 */
	void apply( Scene scene );

	/**
	 * @return Display name of the theme for UI selection components.
	 */
	String getName();
}