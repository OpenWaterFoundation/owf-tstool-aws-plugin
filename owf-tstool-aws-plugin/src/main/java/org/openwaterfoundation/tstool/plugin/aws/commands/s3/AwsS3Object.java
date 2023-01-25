// AwsS3_Object - object to hold S3 object

/* NoticeStart

OWF AWS TSTool Plugin
Copyright (C) 2022-2023 Open Water Foundation

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

/**
 * Object to hold S3 object.
 */
public class AwsS3Object {
	
	/**
	 * Is the object a folder?
	 */
	private AwsS3ObjectType objectType = null;

	/**
	 * S3 bucket for the object.
	 */
	private String bucket = "";

	/**
	 * S3 object key, used for files and folders.
	 */
	private String key = "";

	/**
	 * S3 object last modified time, not used for a folder.
	 */
	private Instant lastModified = null;

	/**
	 * S3 object owner, not used for a folder.
	 */
	private String owner = "";

	/**
	 * S3 object size, bytes, not used for a folder.
	 */
	private Long size = null;

	/**
	 * Constructor for files, used for keys that DO NOT end in / and includes properties for the file/object.
	 * @param bucket the bucket for the object
	 * @param key the full S3 key, with trailing / for folder
	 * @param size the file size in bytes
	 * @param owner the owner, as AWS user
	 * @param lastModified the last modified time
	 */
	public AwsS3Object ( String bucket, String key, Long size, String owner, Instant lastModified) {
		this.bucket = bucket;
		this.key = key;
		this.lastModified = lastModified;
		this.owner = owner;
		this.size = size;
		
		// Set whether a file or folder:
		// - if the key ends in "/", treat as a folder
		// - may remove the check once two constructors are implemented for files and folders
		if ( key.endsWith("/") ) {
			this.objectType = AwsS3ObjectType.FOLDER;
		}
		else {
			this.objectType = AwsS3ObjectType.FILE;
		}
	}

	/**
	 * Constructor for buckets (no key or properties are saved).
	 * @param bucket the bucket for the object
	 */
	public AwsS3Object ( String bucket ) {
		this.bucket = bucket;
		this.objectType = AwsS3ObjectType.BUCKET;
	}

	/**
	 * Constructor for folders.
	 * @param bucket the bucket for the object
	 * @param objectType the object type, used to set the root node using type of BUCKET
	 */
	/*
	public AwsS3Object ( String bucket, AwsS3ObjectType objectType ) {
		this.bucket = bucket;
		
		this.objectType = objectType;
	}
	*/

	/**
	 * Constructor for folders, used for keys that end in /.
	 * @param bucket the bucket for the object
	 * @param key the full S3 key, with trailing / for folder (or bucket name for bucket node)
	 * @param objectType the object type, used to set the root node using type of BUCKET
	 */
	/*
	public AwsS3Object ( String bucket, String key, AwsS3ObjectType objectType ) {
		this.bucket = bucket;
		this.key = key;
		
		this.objectType = objectType;
	}
	*/

	/**
	 * Constructor for folders, used for keys that end in / and have no other properties
	 * since folders are not stored as objects in S3.
	 * @param key the full S3 key, with trailing / for folder
	 */
	public AwsS3Object ( String bucket, String key ) {
		this.bucket = bucket;
		this.key = key;
		
		// Set whether a file or folder:
		// - if the key ends in "/", treat as a folder
		// - may remove the check once two constructors are implemented for files and folders
		if ( key.endsWith("/") ) {
			this.objectType = AwsS3ObjectType.FOLDER;
		}
		else {
			this.objectType = AwsS3ObjectType.FILE;
		}
	}
	
	/**
	 * Return the object type (whether a folder or file).
	 * @return the object type (whether a folder or file).
	 */
	public AwsS3ObjectType getObjectType () {
		return this.objectType;
	}

	/**
	 * Return the bucket.
	 * @return the bucket.
	 */
	public String getBucket () {
		return this.bucket;
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
	
	/**
	 * Set the bucket.
	 * @param bucket the bucket for the object
	 */
	public void setBucket ( String bucket ) {
		this.bucket = bucket;
	}
}
