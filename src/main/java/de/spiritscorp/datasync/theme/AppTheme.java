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