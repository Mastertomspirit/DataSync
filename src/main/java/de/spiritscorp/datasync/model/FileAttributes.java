package de.spiritscorp.datasync.model;

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

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * Immutable value object representing the metadata and diagnostic attributes of a file
 * managed within the synchronization engine context.
 * <p>
 * This class stores timestamps, cryptographic checksums, sizes, and path configurations
 * required to compute delta states between replication nodes.
 * <p>
 *
 * @author Tom Spirit
 * @version 2.0.0
 */
public final class FileAttributes implements Comparable<FileAttributes> {

	private final Path relativeFilePath;
	private final String fileName;
	private final String createTimeString;
	private final String modTimeString;
	private final long size;
	private final String fileHash;
	private final FileTime createTime;
	private final FileTime modTime;

	/**
	 * Constructs a comprehensive metadata record for a single tracked file.
	 *
	 * @param relativeFilePath the target file path relative to the source or destination root directory
	 * @param createTimeString a string representation of the file creation timestamp
	 * @param createTime       the raw {@link FileTime} of when the file was created
	 * @param modTimeString    a string representation of the last modification timestamp
	 * @param modTime          the raw {@link FileTime} of the last modification event
	 * @param size             the size of the file in bytes
	 * @param fileHash         the cryptographic checksum signature (SHA-256 or higher)
	 * @throws NullPointerException if {@code relativeFilePath} is null
	 */
	public FileAttributes( final Path relativeFilePath, final String createTimeString, final FileTime createTime, final String modTimeString, final FileTime modTime, final long size,
			final String fileHash ) {

		// Guard against null paths entering the tracking context
		this.relativeFilePath = Objects.requireNonNull( relativeFilePath, "Relative file path context cannot be null" );
		// Safely extract the file name string context even for root path definitions
		final Path namePath = relativeFilePath.getFileName();
		this.fileName = ( namePath != null ) ? namePath.toString() : relativeFilePath.toString();

		this.createTime = ( createTime != null ) ? FileTime.fromMillis( createTime.toMillis() ) : FileTime.fromMillis( 0 );
		this.modTime = ( modTime != null ) ? FileTime.fromMillis( modTime.toMillis() ) : FileTime.fromMillis( 0 );
		this.createTimeString = createTimeString;
		this.modTimeString = modTimeString;
		this.size = size;
		this.fileHash = fileHash;
	}

	/**
	 * Compares this file attribute record with another based primarily on file size metrics.
	 *
	 * @param o the other {@code FileAttributes} object to compare against
	 * @return a negative integer, zero, or a positive integer as this file size
	 *         is less than, equal to, or greater than the specified object's size
	 */
	@Override
	public int compareTo( final FileAttributes o ) {
		return Long.compare( this.size, o.getSize() );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( createTimeString == null ) ? 0 : createTimeString.hashCode() );
		result = prime * result + ( ( fileName == null ) ? 0 : fileName.hashCode() );
		result = prime * result + ( ( relativeFilePath == null ) ? 0 : relativeFilePath.hashCode() );
		result = prime * result + ( ( modTimeString == null ) ? 0 : modTimeString.hashCode() );
		result = prime * result + (int) ( size ^ ( size >>> 32 ) );
		return result;
	}

	@Override
	public boolean equals( final Object obj ) {
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		final FileAttributes other = (FileAttributes) obj;
		if( fileHash == null ) {
			if( other.fileHash != null )
				return false;
		}else if( !other.fileHash.equals( fileHash ) )
			return false;
		if( fileName == null ) {
			if( other.fileName != null )
				return false;
		}else if( !fileName.equals( other.fileName ) )
			return false;
		if( relativeFilePath == null ) {
			if( other.relativeFilePath != null )
				return false;
		}else if( !relativeFilePath.equals( other.relativeFilePath ) )
			return false;
		if( modTimeString == null ) {
			if( other.modTimeString != null )
				return false;
		}else if( !modTimeString.equals( other.modTimeString ) )
			return false;
		if( size != other.size )
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileAttributes [fileName=" + fileName + ", relativeFilePath=" + relativeFilePath + ", createTimeString="
				+ createTimeString + ", modTimeString=" + modTimeString + ", size=" + size + ", fileHash=" + fileHash
				+ ", createTime=" + createTime + ", modTime=" + modTime + "]";
	}

	// --- Standard Java-Bean Property Accessors APIs layer ---

	/**
	 * Retrieves the file path relative to the active deployment endpoint layer root.
	 *
	 * @return the relative {@link Path}
	 */
	public Path getRelativeFilePath() { return relativeFilePath; }

	/**
	 * Retrieves the human-readable string mapping of the modification timeline entry.
	 *
	 * @return the modification timestamp string literal
	 */
	public String getModTimeString() { return modTimeString; }

	/**
	 * Retrieves the native hardware accurate last modified metric vector.
	 *
	 * @return the high-precision modification {@link FileTime}
	 */
	public FileTime getModTime() { return FileTime.fromMillis( modTime.toMillis() ); }

	/**
	 * Retrieves the human-readable string mapping of the original creation timeline entry.
	 *
	 * @return the creation timestamp string literal
	 */
	public String getCreateTimeString() { return createTimeString; }

	/**
	 * Retrieves the data payload capacity volume layer quantified in bytes.
	 *
	 * @return the length of the file as a primitive long value
	 */
	public long getSize() { return size; }

	/**
	 * Retrieves the unique data signature cryptographic validation token checksum.
	 *
	 * @return the string-formatted hexadecimal file hash
	 */
	public String getFileHash() { return fileHash; }

	/**
	 * Retrieves the structural file name node excluding directory routing segments.
	 *
	 * @return the localized file name string representation
	 */
	public String getFileName() { return fileName; }

	/**
	 * Retrieves the native hardware accurate original initialization filesystem creation marker.
	 *
	 * @return the high-precision creation {@link FileTime}
	 */
	public FileTime getCreateTime() { return FileTime.fromMillis( createTime.toMillis() ); }
}
