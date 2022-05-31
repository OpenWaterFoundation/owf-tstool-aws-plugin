// AwsSession - AWS session data

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

package org.openwaterfoundation.tstool.plugin.aws;

/*
 * AWS session for a user, corresponding to a profile.
 */
public class AwsSession {
	
	/**
	 * Profile name for AWS session.
	 */
	private String profile = "";
	
	/**
	 * Whether a default profile can be used.
	 * Use true because many cases only define one profile.
	 */
	private boolean useDefaultProfile = true;
	
	/**
	 * Create a new session and allow the default profile to be used.
	 * @param profile the profile to use, if null see 'defaultProfile'
	 */
	public AwsSession ( String profile ) {
		this.profile = profile;
		this.useDefaultProfile = true;
	}

	/**
	 * Create a new session.
	 * @param profile the profile to use, if null see 'defaultProfile'
	 * @param useDefaultProfile if true, determine the default profile when 'profile' is null
	 */
	public AwsSession ( String profile, boolean useDefaultProfile ) {
		this.profile = profile;
		this.useDefaultProfile = useDefaultProfile;
	}
	
	/**
	 * Return the profile to use for AWS operations.
	 * @return the profile.
	 */
	public String getProfile () {
		if ( (this.profile == null) || this.profile.isEmpty() ) {
			if ( useDefaultProfile ) {
				 return AwsToolkit.getInstance().getDefaultProfile();
			}
			else {
				return null;
			}
		}
		else {
			return this.profile;
		}
	}
}