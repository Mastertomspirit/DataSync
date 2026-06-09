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
package de.spiritscorp.DataSync;

public enum ScanType {

	SYNCHRONIZE("Syncronisieren"),
	FLAT_SCAN ("flacher Scan"),
	DEEP_SCAN("tiefer Scan"),
	DUBLICATE_SCAN("Dublikate suchen");
	
	private String describtion;
	
	private ScanType(String description){
		this.describtion = description;
	}
		
	public static String[] getAllDescriptions() {
		String[] ret = new String[ScanType.values().length];
		int i = 0;
		for(ScanType s : ScanType.values()) {
			ret[i] = s.getDescription();
			i++;
		}
		return ret;
	}
	
	public static ScanType get(String str) {
		for(ScanType st : ScanType.values()) {
			if(str.equals(st.getDescription())) return st;
		}
		return null;
	}
	
	public String getDescription() {
		return describtion;
	}
	
}
