// AwsToolkit - utility functions for AWS as singleton

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

public class AwsToolkit {
	/**
	 * Singleton object.
	 */
	private static AwsToolkit instance = new AwsToolkit();

	/**
	 * Get the AwsToolkit singleton instance.
	 */
	public static AwsToolkit getInstance() {
		return instance;
	}

	/**
 	* Get the list of buckets.
 	*/
	public List<Bucket> getBuckets ( AwsSession awsSession, String region ) {
		ProfileCredentialsProvider credentialsProvider = null;
		String profile = awsSession.getProfile();
		try {
			credentialsProvider = ProfileCredentialsProvider.create(profile);
			Region regionObject = Region.of(region);
			S3Client s3 = S3Client.builder()
				.region(regionObject)
				.credentialsProvider(credentialsProvider)
				.build();
    		ListBucketsRequest request = ListBucketsRequest.builder().build();
    		ListBucketsResponse response = s3.listBuckets(request);
    		return response.buckets();
		}
		catch ( Exception e ) {
			// Log the error and return an empty list:
			// - may have requested an invalid region
			String routine = getClass().getSimpleName() + ".getBuckets";
			Message.printWarning(3, routine, "Error getting list of buckets (" + e + ").");
		}
		return new ArrayList<Bucket>();
	}
	
	/**
	 * Determine the default profile, typically called when code has not specified a profile.
	 * @return the default profile to use, which will be the only profile if one exists,
	 * or the profile 'default' if multiple profiles exist and 'default' is one of those,
	 * or null if can't determine a default.
	 */
	public String getDefaultProfile () {
		List<String> profiles = getProfiles();
		if ( profiles.size() == 0 ) {
			// No profiles.
			return null;
		}
		else if ( profiles.size() == 1 ) {
			// One profile so use it by default.
			return profiles.get(0);
		}
		else {
			// Multiple profiles.  Only match if 'default' is found.
			for ( String profile : profiles ) {
				if ( profile.equals("default") ) {
					return profile;
				}
			}
			// No profile named 'default' so can't determine the default profile.
			return null;
		}
	}
	
	/**
	 * Get a list of profiles from the ~/.config file entries.
	 * See: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-profiles.html
	 */
	public List<String> getProfiles () {
		String routine = getClass().getSimpleName() + ".getProfiles";
		List<String> profiles = new ArrayList<>();
		// Get the user's home folder.
		String userFolder = System.getProperty("user.home");
		String configFileName = userFolder + File.separator + ".aws" + File.separator + "config";
		File configFile = new File(configFileName);
		if ( configFile.exists() ) {
			try {
				List<String> lines = IOUtil.fileToStringList(configFileName);
				for ( String line : lines ) {
					// Profile sections have the following form:
					// [default]
					// [profile profileName]
					line = line.trim();
					if ( line.equals("[default]") ) {
						profiles.add("default");
					}
					else if ( line.startsWith("[profile") ) {
						int pos = line.indexOf(" ");
						if ( pos > 0 ) {
							profiles.add(line.substring(pos).replace("]","").trim());
						}
					}
				}
			}
			catch ( IOException e ) {
				Message.printWarning(3, routine, "Error processing AWS configuration file: " + configFileName);
				Message.printWarning(3, routine, e);
			}
		}
		else {
			Message.printWarning(3, routine, "AWS configuration file does not exist: " + configFileName);
		}
		return profiles;
	}
}