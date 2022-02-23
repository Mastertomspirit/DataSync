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
package de.spiritscorp.DataSync.Model;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileAttributes implements Comparable<FileAttributes> {

	private final Path relativeFilePath;
	private final String fileName;
	private final String createTime, modTime;
	private final long size;
	private final String fileHash;
	private final FileTime createTimeFileTime;
	
	/**
	 * 
	 * @param relativeFilePath 		Relative from the begin of source or destination path
	 * @param createTime			create time of file
	 * @param modTime				last modified time of file
	 * @param size					File size
	 * @param fileHash				hash of the file with SHA 256 or higher
	 */
	public FileAttributes(Path relativeFilePath, String createTime, FileTime createTimeFileTime, String modTime, long size, String fileHash) {
		this.relativeFilePath = relativeFilePath;
		fileName = relativeFilePath.getFileName().toString();
		this.createTimeFileTime = createTimeFileTime;
		this.createTime = createTime;
		this.modTime = modTime;
		this.size = size;
		this.fileHash = fileHash;
	}

	@Override
	public int compareTo(FileAttributes o) {
		Long lo = this.getSize() - o.getSize();
		if(lo == 0) {
			return 0;
		} else if(lo < 0) {
			return -1;
		}else {
			return 1;
		}	
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((relativeFilePath == null) ? 0 : relativeFilePath.hashCode());
		result = prime * result + ((modTime == null) ? 0 : modTime.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileAttributes other = (FileAttributes) obj;
		if (createTime == null) {
			if (other.createTime != null)
				return false;
		} else if (!createTime.equals(other.createTime))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (relativeFilePath == null) {
			if (other.relativeFilePath != null)
				return false;
		} else if (!relativeFilePath.equals(other.relativeFilePath))
			return false;
		if (modTime == null) {
			if (other.modTime != null)
				return false;
		} else if (!modTime.equals(other.modTime))

			return false;
		if (size != other.size)
			return false;
		return true;
	}
	
	public Path getRelativeFilePath() {
		return relativeFilePath;
	}
	public String getModTime() {
		return modTime;
	}
	public String getCreateTime() {
		return createTime;
	}
	public long getSize() {
		return size;
	}
	public String getFileHash() {
		return fileHash;
	}
	public String getFileName() {
		return fileName;
	}
	public FileTime getCreateTimeFileTime() {
		return createTimeFileTime;
	}	
}
