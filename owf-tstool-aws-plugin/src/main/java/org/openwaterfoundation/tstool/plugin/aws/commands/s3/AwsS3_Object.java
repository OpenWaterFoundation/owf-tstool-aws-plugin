// AwsS3_Object - object to hold S3 object

/* NoticeStart

OWF AWS TSTool Plugin
Copyright (C) 2022 Open Water Foundation

OWF TSTool AWS Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OWF TSTool AWS Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OWF TSTool AWS Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.aws.commands.s3;

import java.time.Instant;

import RTi.Util.Time.DateTime;

/**
 * Object to hold S3 object.
 */
public class AwsS3_Object {
	
	/**
	 * Is the object a folder?
	 */
	private boolean isFolder = false;

	/**
	 * S3 object key.
	 */
	private String key = "";

	/**
	 * S3 object last modified time.
	 */
	private Instant lastModified = null;

	/**
	 * S3 object owner.
	 */
	private String owner = "";

	/**
	 * S3 object size, bytes.
	 */
	private Long size = null;

	/**
	 * Constructor.
	 */
	public AwsS3_Object ( String key, Long size, String owner, Instant lastModified) {
		this.key = key;
		this.lastModified = lastModified;
		this.owner = owner;
		this.size = size;
		
		// If the key ends in "/", treat as a folder.
		if ( key.endsWith("/") ) {
			this.isFolder = true;
		}
	}
	
	/**
	 * Return whether a folder.
	 * @return whether a folder.
	 */
	public boolean getIsFolder () {
		return this.isFolder;
	}

	/**
	 * Return the key.
	 * @return the key.
	 */
	public String getKey () {
		return this.key;
	}

	/**
	 * Return the last modified date/time.
	 * @return the last modified date/time.
	 */
	public Instant getLastModified () {
		return this.lastModified;
	}

	/**
	 * Return the owner.
	 * @return the owner.
	 */
	public String getOwner () {
		return this.owner;
	}

	/**
	 * Return the size, bytes.
	 * @return the size, bytes.
	 */
	public Long getSize () {
		return this.size;
	}
}
