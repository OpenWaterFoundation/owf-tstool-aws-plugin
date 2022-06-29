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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.InvalidationSummary;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsRequest;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsResponse;
import software.amazon.awssdk.services.cloudfront.model.ListInvalidationsRequest;
import software.amazon.awssdk.services.cloudfront.model.ListInvalidationsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

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
	
	// -----------------------------------------------------------------------------

	/**
	 * Download a file from S3.
	 * @param tm S3TransferManager to use for download
	 * @param bucket S3 bucket to download from
	 * @param downloadKey S3 key for the file
	 * @param localFile path to the local file
	 * @param problems list of problems encountered
	 */
	public void downloadFileFromS3 ( S3TransferManager tm, String bucket, String downloadKey, String localFile, List<String> problems ) {
    	if ( (localFile == null) || localFile.trim().isEmpty() ) {
    		// Don't allow default.
    		problems.add("No local file given - cannot download file.");
    		return;
    	}
    	if ( (downloadKey == null) || downloadKey.trim().isEmpty() ) {
    	    // Don't allow default because could cause problems identifying S3 files.
    	    problems.add("No S3 key given - cannot download file \"" + downloadKey + "\"");
    	    return;
    	}
    	localFile = localFile.trim();
    	downloadKey = downloadKey.trim();
    	final String downloadKeyFinal = downloadKey;
    	final String localFileFinal = localFile;

    	FileDownload download = tm
   	    	.downloadFile(d -> d.getObjectRequest(g -> g.bucket(bucket).key(downloadKeyFinal))
    		.destination(Paths.get(localFileFinal)));
    	    download.completionFuture().join();
    }

	/**
	 * Get the CloudFront distribution ID to use for CloudFront commands.
  	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
	 * @param distributionId if not null and not empty, the distribution ID to use, takes precedence over the comment
	 * @param commentPattern if not null and not empty, a Java pattern to match the distribution comment
	 * @return the distribution ID to use after evaluating input, or null if not matched.
	 */
   	public String getCloudFrontDistributionId( AwsSession awsSession, String region, String distributionId, String commentPattern ) {
   		// Get the list of distributions.
   		List<DistributionSummary> distributions = getCloudFrontDistributions ( awsSession, region );
   		// Match a specific distribution.
   		for ( DistributionSummary distribution : distributions ) {
   			if ( (distributionId != null) && !distributionId.isEmpty() && distribution.id().equals(distributionId) ) {
   				return distributionId;
   			}
   			else if ( (commentPattern != null) && !commentPattern.isEmpty() && distribution.comment().matches(commentPattern) ) {
   				return distribution.id();
   			}
   		}
   		// No match, return null.
   		return null;
   	}

	/**
 	 * Get the list of CloudFront distributions.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @return a list of DistributionSummary or an empty list if no distributions
 	 */
	public List<DistributionSummary> getCloudFrontDistributions ( AwsSession awsSession, String region ) {
		ProfileCredentialsProvider credentialsProvider = null;
		String profile = awsSession.getProfile();
		// If the region is not specified, use the default.
		if ( (region == null) || region.isEmpty() ) {
			region = getDefaultRegion ( profile );
		}
		try {
			credentialsProvider = ProfileCredentialsProvider.create(profile);
			Region regionObject = Region.of(region);
			CloudFrontClient cloudfront = CloudFrontClient.builder()
				.region(regionObject)
				.credentialsProvider(credentialsProvider)
				.build();
    		ListDistributionsRequest request = ListDistributionsRequest.builder().build();
    		ListDistributionsResponse response = cloudfront.listDistributions(request);
    		return response.distributionList().items();
		}
		catch ( Exception e ) {
			// Log the error and return an empty list:
			// - may have requested an invalid region
			String routine = getClass().getSimpleName() + ".getCloudFrontDistributions";
			Message.printWarning(3, routine, "Error getting list of distributions (" + e + ").");
		}
		return new ArrayList<DistributionSummary>();
	}

	/**
 	 * Get the list of CloudFront invalidations.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @return a list of InvalidationSummary or an empty list if no invalidations
 	 */
	public List<InvalidationSummary> getCloudFrontInvalidations ( AwsSession awsSession, String region ) {
		ProfileCredentialsProvider credentialsProvider = null;
		String profile = awsSession.getProfile();
		// If the region is not specified, use the default.
		if ( (region == null) || region.isEmpty() ) {
			region = getDefaultRegion ( profile );
		}
		try {
			credentialsProvider = ProfileCredentialsProvider.create(profile);
			Region regionObject = Region.of(region);
			CloudFrontClient cloudfront = CloudFrontClient.builder()
				.region(regionObject)
				.credentialsProvider(credentialsProvider)
				.build();
    		ListInvalidationsRequest request = ListInvalidationsRequest.builder().build();
    		ListInvalidationsResponse response = cloudfront.listInvalidations(request);
    		return response.invalidationList().items();
		}
		catch ( Exception e ) {
			// Log the error and return an empty list:
			// - may have requested an invalid region
			String routine = getClass().getSimpleName() + ".getCloudFrontInvalidations";
			Message.printWarning(3, routine, "Error getting list of invalidations (" + e + ").");
		}
		return new ArrayList<InvalidationSummary>();
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
	 * Get the default region from the ~/.config file entry for the specified profile.
	 * See: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-profiles.html
	 * See: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html#cli-configure-quickstart-region
	 * 
	 * The file will have something like the following if the region is set:
	 * 
	 * [profile profile-name]
	 * region = us-west-2
	 * output = text
	 * 
	 * [default]
	 * region = us-west-2
	 * output = text
	 */
	public String getDefaultRegion ( String profile ) {
		String routine = getClass().getSimpleName() + ".getDefaultRegion";
		String region = "";
		// Get the user's home folder.
		String userFolder = System.getProperty("user.home");
		String configFileName = userFolder + File.separator + ".aws" + File.separator + "config";
		File configFile = new File(configFileName);
		if ( configFile.exists() ) {
			try {
				List<String> lines = IOUtil.fileToStringList(configFileName);
				boolean profileFound = false;
				for ( String line : lines ) {
					// Profile sections have the following form:
					// [default]
					// [profile profileName]
					line = line.trim();
					if ( line.equals("[default]") ) {
						if ( profile.equals("default") ) {
							// Found the requested profile.
							profileFound = true;
						}
						else {
							profileFound = false;
						}
					}
					else if ( line.startsWith("[profile") ) {
						int pos = line.indexOf(" ");
						if ( pos > 0 ) {
							if ( profile.equals(line.substring(pos).replace("]","").trim()) ) {
								// Found the requested profile.
								profileFound = true;
							}
							else {
								profileFound = false;
							}
						}
					}
					if ( profileFound ) {
						// Profile was found so search for region.
						if ( line.startsWith("region") ) {
							int pos = line.indexOf("=");
							if ( (pos > 0) && ((pos + 1) < line.length()) ) {
								// Make sure the string includes value to the right of =.
								region = line.substring(pos + 1).trim();
								break;
							}
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
		return region;
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

	/**
 	* Get the list of S3 buckets.
 	* @param awsSession the AWS session, containing profile
 	* @param region the AWS region
 	*/
	public List<Bucket> getS3Buckets ( AwsSession awsSession, String region ) {
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
			String routine = getClass().getSimpleName() + ".getS3Buckets";
			Message.printWarning(3, routine, "Error getting list of buckets (" + e + ").");
		}
		return new ArrayList<Bucket>();
	}
	
	/**
	 * Invalidate a list of files in a CloudFront distribution.
	 * Currently this does nothing.
	 */
    public void invalidateCloudFrontDistribution ( List<String> invalidationList ) {
    	
    }

	/**
	 * Upload a file to S3.
	 * @param tm S3TransferManager to use for upload
	 * @param bucket S3 bucket to upload to
	 * @param localFile file to upload
	 * @param uploadKey S3 key for the file
	 * @param problems list of problems encountered
	 */
	public void uploadFileToS3 ( S3TransferManager tm, String bucket, String localFile, String uploadKey, List<String> problems ) {
    	if ( (localFile == null) || localFile.trim().isEmpty() ) {
    		// Don't allow default because could cause problems clobbering S3 files.
    		problems.add("No local file given - cannot upload file.");
    		return;
    	}
    	if ( (uploadKey == null) || uploadKey.trim().isEmpty() ) {
    	    // Don't allow default because could cause problems clobbering S3 files.
    	    problems.add("No S3 key given - cannot upload file \"" + localFile + "\"");
    	    return;
    	}
    	localFile = localFile.trim();
    	uploadKey = uploadKey.trim();
    	final String uploadKeyFinal = uploadKey;
    	final String localFileFinal = localFile;
    	FileUpload upload = tm
   	    	.uploadFile(d -> d.putObjectRequest(g -> g.bucket(bucket).key(uploadKeyFinal))
    		.source(Paths.get(localFileFinal)));
    	upload.completionFuture().join();
    }
	
}