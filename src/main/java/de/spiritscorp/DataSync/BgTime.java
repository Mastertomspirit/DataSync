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

public enum BgTime {

	MIN_1(60000L, "1 Minute", 5000L),
	MIN_5(300000L, "5 Minuten", 10000L),
	MIN_30(1800000L, "30 Minuten", 60000L),
	HOURLY(3600000L, "Stündlich", 300000L),
	DAYLY(86400000L, "Täglich", 1800000L),
	WEEKLY(604800000L, "Wöchentlich", 1800000L),
	MONTHLY(2592000000L, "Monatlich", 1800000L);
	
	private long time;
	private String name;
	private long checkTime;
	
	private BgTime(long time, String name, long checkTime) {
		this.time = time;
		this.name = name;
		this.checkTime = checkTime;
	}
	
	public static String[] getNames() {
		String[] str = new String[BgTime.values().length];
		int i = 0;
		for(BgTime bg : BgTime.values()) {
			str[i] = bg.getName();
			i++;
		}
		return str;
	}
	
	public static BgTime get(String str) {
		for(BgTime st : BgTime.values()) {
			if(str.equals(st.getName())) return st;
		}
		return null;
	}
	
	public long getTime() {
		return time;
	}
	public String getName() {
		return name;
	}
	public long getCheckTime() {
		return checkTime;
	}
}
