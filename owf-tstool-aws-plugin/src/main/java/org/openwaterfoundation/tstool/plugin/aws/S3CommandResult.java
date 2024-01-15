// S3CommandResult - result of an S3 command

/* NoticeStart

OWF TSTool AWS Plugin
Copyright (C) 2022-2024 Open Water Foundation

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

package org.openwaterfoundation.tstool.plugin.aws;

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;

/**
 * Result of whether an S3 command completed successfully.
 * For example, each key requested to be deleted will have a status.
 */
public class S3CommandResult {

	/**
	 * The object on which a command was executed (e.g., "delete").
	 */
	private AwsS3Object s3Object = null;
	
	/**
	 * Whether successful.
	 */
	private boolean commandSuccessful = false;
	
	/**
	 * Message for the command, useful when there is a problem, can contain newlines.
	 */
	private String message = "";
	
	/**
	 * Constructor.
	 * @param s3Object the AwsS3Object that is being operated on by the command
	 * @param commandSuccessful whether the command was successful (true) or not (false)
	 * @param message message to explain the result, such as a problem
	 */
	public S3CommandResult ( AwsS3Object s3Object, boolean commandSuccessful, String message ) {
		this.s3Object = s3Object;
		this.commandSuccessful = commandSuccessful;
		this.message = message;
	}
	
	/**
	 * Return whether the command was successful.
	 * @return whether the command was successful (true) or not (false)
	 */
	public boolean commandWasSuccessful() {
		return this.commandSuccessful;
	}

	/**
	 * Return the message explaining the command result.
	 * @return the message explaining the command result
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Return the s3Object  that the command operated on.
	 * @return the s3Object  that the command operated on
	 */
	public String getS3Object() {
		return this.message;
	}
}