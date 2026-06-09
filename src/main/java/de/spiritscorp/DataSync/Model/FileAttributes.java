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

public final class FileAttributes implements Comparable<FileAttributes> {

	private final Path relativeFilePath;
	private final String fileName;
	private final String createTimeString, modTimeString;
	private final long size;
	private final String fileHash;
	private final FileTime createTime, modTime;
	
	/**
	 * 
	 * @param relativeFilePath 		Relative from the begin of source or destination path
	 * @param createTimeString		create time of file
	 * @param modTimeString			last modified time of file
	 * @param size					File size
	 * @param fileHash				hash of the file with SHA 256 or higher
	 */
	public FileAttributes(Path relativeFilePath, String createTimeString, FileTime createTime, String modTimeString, FileTime modTime, long size, String fileHash) {
		this.relativeFilePath = relativeFilePath;
		fileName = relativeFilePath.getFileName().toString();
		this.createTime = createTime;
		this.createTimeString = createTimeString;
		this.modTimeString = modTimeString;
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
		result = prime * result + ((createTimeString == null) ? 0 : createTimeString.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((relativeFilePath == null) ? 0 : relativeFilePath.hashCode());
		result = prime * result + ((modTimeString == null) ? 0 : modTimeString.hashCode());
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
		if (fileHash == null) {
			if(other.fileHash != null)	
				return false;
		} else if(!other.fileHash.equals(fileHash))
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
		if (modTimeString == null) {
			if (other.modTimeString != null)
				return false;
		} else if (!modTimeString.equals(other.modTimeString))
			return false;
		if (size != other.size)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "FileAttributes [fileName=" + fileName + ", relativeFilePath=" + relativeFilePath + ", createTimeString="
				+ createTimeString + ", modTimeString=" + modTimeString + ", size=" + size + ", fileHash=" + fileHash
				+ ", createTime=" + createTime + ", modTime=" + modTime + "]";
	}

	public Path getRelativeFilePath() {
		return relativeFilePath;
	}
	public String getModTimeString() {
		return modTimeString;
	}
	public FileTime getModTime() {
		return modTime;
	}
	public String getCreateTimeString() {
		return createTimeString;
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
	public FileTime getCreateTime() {
		return createTime;
	}	
}
