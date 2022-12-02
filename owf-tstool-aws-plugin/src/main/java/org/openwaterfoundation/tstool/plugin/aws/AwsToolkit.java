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
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeUtil;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
//import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
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

/**
 * AWS toolkit singleton.
 * Retrieve the instance with getInstance() and then use methods.
 */
public class AwsToolkit {
	/**
	 * Singleton object.
	 */
	private static AwsToolkit instance = null;

	/**
	 * Get the AwsToolkit singleton instance.
	 */
	public static AwsToolkit getInstance() {
		// Use lazy loading.
		if ( instance == null ) {
			instance = new AwsToolkit();
		}
		return instance;
	}
	
	/**
	 * Private constructor.
	 */
	private AwsToolkit () {
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
	 * Get the path to the user's AWS configuration file.
	 * @return the path to the user's AWS configuration file.
	 */
	public String getAwsUserConfigFile () {
		String userFolder = System.getProperty("user.home");
		String configFileName = userFolder + File.separator + ".aws" + File.separator + "config";
		return configFileName;
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
 	 * Get the list of CloudFront invalidations with status=InProgress.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @param distribtionId CloudFront distribution ID to list invalidations
 	 * @return a list of InvalidationSummary or an empty list if no invalidations
 	 */
	public List<InvalidationSummary> getCloudFrontInvalidations ( AwsSession awsSession, String region, String distributionId ) {
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
    		ListInvalidationsRequest request = ListInvalidationsRequest
    			.builder()
       			.distributionId(distributionId)
    			.build();
    		// The following lists all invalidations:
    		// - most recent is first in the list
    		// - 100 default list size
    		// - status will be either "InProgress" or "Completed"
    		ListInvalidationsResponse response = cloudfront.listInvalidations(request);
    		// Only return invalidations that are not complete.
    		List<InvalidationSummary> activeList = new ArrayList<>();
			for ( InvalidationSummary summary : response.invalidationList().items() ) {
				if ( summary.status().equals("InProgress") ) {
					activeList.add(summary);
				}
			}
    		return activeList;
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
		String configFileName = getAwsUserConfigFile();
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
		String configFileName = getAwsUserConfigFile();
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
	 * @param distributionId CloudFront distribution ID to invalidate
	 * @param invalicationPathsList list of files to invalidate
	 * @param callerReference unique string to identify the CloudFront invalidation
	 */
    public void invalidateCloudFrontDistribution ( AwsSession awsSession,
    	String regionS,
    	String distributionId, List<String> invalidationPathsList, String callerReference ) {
    	String routine = getClass().getSimpleName() + ".invalidateCloudFrontDistribution";
   	    // Invalidate:
   	    // - see:  https://stackoverflow.com/questions/28527188/how-to-invalidate-a-fileto-be-refreshed-served-from-cloudfront-cdn-via-java-aw
   	    // - exception will be caught below
   	    for ( String path : invalidationPathsList ) {
   	        Message.printStatus(2, routine, "Invalidating path \"" + path + "\".");
   	    }
   	    
   	    String profile = awsSession.getProfile();
   	    
   	    // Get the region object from the string.
   	    Region regionO = Region.of(regionS);

   	    ProfileCredentialsProvider credentialsProvider0 = null;
	  	    credentialsProvider0 = ProfileCredentialsProvider.create(profile);
	  	    ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

   	    CloudFrontClient cloudfront = CloudFrontClient.builder()
		  	.region(regionO)
		  	.credentialsProvider(credentialsProvider)
		  	.build();

   	    software.amazon.awssdk.services.cloudfront.model.Paths invalidationPaths =
   	    				software.amazon.awssdk.services.cloudfront.model.Paths
   	        			.builder()
   	        			.items(invalidationPathsList)
   	        			.quantity(invalidationPathsList.size())
   	        			.build();
   	    InvalidationBatch batch = InvalidationBatch
   	        			.builder()
   	        			.paths(invalidationPaths)
   	        			.callerReference(callerReference)
   	        			.build();
   	    CreateInvalidationRequest request = CreateInvalidationRequest
   	        			.builder()
   	        			.distributionId(distributionId)
   	        			.invalidationBatch(batch)
   	        			.build();
   	    //CreateInvalidationResponse response =
   	    	cloudfront.createInvalidation(request);
       	// If wait for completion is true, wait until the invalidation is complete:
       	// - TODO smalers 2022-06-06 does this happen automatically?
       	int maxTries = 3600;
       	int tryCount = 0;
       	boolean waitForCompletion = false;
       	if ( waitForCompletion ) {
       		while ( tryCount <= maxTries ) {
       			++tryCount;
       			// Get the current invalidations.
       			List<InvalidationSummary> invalidations = getCloudFrontInvalidations(awsSession, regionO.id(), distributionId);
       			if ( invalidations.size() == 0 ) {
       				// No more invalidations on the distribution:
       				// - TODO smalers 2022-06-06 evaluate whether to check caller reference
       				break;
      			}
       			// Wait 5 seconds to allow invalidation to complete.
       			TimeUtil.sleep(5000);
       		}
       	}
    }

    /**
     * UI helper to populate the Bucket choices based on profile and region selections.
     * @param selectedRegion the selected region
     */
    public void uiPopulateBucketChoices ( AwsSession awsSession, String region,
    	SimpleJComboBox Profile_JComboBox, SimpleJComboBox Bucket_JComboBox) {
    	String routine = getClass().getSimpleName() + ".uiPopulateBucketChoices";
    	boolean debug = true;
    	List<String> bucketChoices = new ArrayList<>();
    	if ( awsSession == null ) {
    		// Startup - can't populate the buckets.
    		if ( debug ) {
    			Message.printStatus(2, routine, "Startup - not populating the list of buckets." );
    		}
    		return;
    	}
    	else {
    		if ( debug ) {
    			Message.printStatus(2, routine, "Getting the list of buckets." );
    		}
    		// Get the list of buckets.
    		if ( (region == null) || region.isEmpty() ) {
    			// Startup or region specified:
    			// - try to get the default region from the user's configuration file
    			// - can't populate the buckets.
    			String profile = Profile_JComboBox.getSelected();
    			if ( (profile == null) || profile.isEmpty() ) {
    				// Get the default profile.
    				profile = AwsToolkit.getInstance().getDefaultProfile();
    			}
    			if ( (profile == null) || profile.isEmpty() ) {
    				Message.printStatus(2, routine, "Region is not specified and no default profile - can't populate the list of buckets." );
    			}
    			else {
    				region = AwsToolkit.getInstance().getDefaultRegion(profile);
    				if ( (region == null) || region.isEmpty() ) {
    					Message.printStatus(2, routine, "Region is not specified and no default profile - can't populate the list of buckets." );
    					Bucket_JComboBox.setData(bucketChoices);
    					return;
    				}
    				else {
    					// Region is used below.
    				}
    			}
    		}
    		// Have a region from specified value or default.
    		if ( debug ) {
    			Message.printStatus(2, routine, "Region is \"" + region + "\" - populating the list of buckets." );
    		}	
    		List<Bucket> buckets = AwsToolkit.getInstance().getS3Buckets(awsSession, region);
    		for ( Bucket bucket : buckets ) {
    			bucketChoices.add ( bucket.name() );
    			if ( debug ) {
    				Message.printStatus(2, routine, "Populated bucket: " + bucket.name() );
    			}
    		}
    		Collections.sort(bucketChoices);
    		// Add a blank because some services don't use.
    		bucketChoices.add(0,"");
    		Bucket_JComboBox.setData(bucketChoices);
    		if ( Bucket_JComboBox.getItemCount() > 0 ) {
    			// Select the first bucket by default.
    			Bucket_JComboBox.select ( 0 );
    		}
    	}
    }

    /**
     * UI helper to populate the profile default value.
     * This method is called from command editors.
     */
    public void uiPopulateProfileDefault ( JTextField ProfileDefault_JTextField, JLabel ProfileDefaultNote_JLabel ) {
    	String profile = getDefaultProfile();
    	if ( (profile == null) || profile.isEmpty() ) {
    		// No default profile, which is a problem.
    		ProfileDefault_JTextField.setText("");
    		ProfileDefaultNote_JLabel.setText("<html><b>Unknown.</b></html>");
    	}
    	else {
    		// Default profile exists.
    		ProfileDefault_JTextField.setText(profile);
    		ProfileDefaultNote_JLabel.setText("From: " + getAwsUserConfigFile() );
    	}
    }

    /**
     * Populate the region default value, which depends on the profile.
     */
    public void uiPopulateRegionDefault ( String profile, JTextField RegionDefault_JTextField, JLabel RegionDefaultNote_JLabel ) {
    	if ( (profile == null) || profile.isEmpty() ) {
    		// Try to use the default profile.
    		profile = getDefaultProfile();
    		if ( (profile == null) || profile.isEmpty() ) {
    			// No default profile, which is a problem.
    			RegionDefault_JTextField.setText("");
    			RegionDefaultNote_JLabel.setText("<html><b>Unknown.</b></html>");
    		}
    		else {
    			// Have a default profile.
    			String region = getDefaultRegion(profile);
    			if ( (region == null) || region.isEmpty() ) {
    				// No region for the default profile.
    				RegionDefault_JTextField.setText("");
    				RegionDefaultNote_JLabel.setText("<hbml><b>Unknown</b></html>");
    			}
    			else {
    				// Have a region for the default profile.
    				RegionDefault_JTextField.setText(region);
    				RegionDefaultNote_JLabel.setText("From: " + getAwsUserConfigFile() + ", Profile (default): " + profile );
    			}
    		}
    	}
    	else {
    		// Profile has been specified so use it for the default region.
    		String region = getDefaultRegion(profile);
    		if ( (region == null) || region.isEmpty() ) {
    			// No region specified for the specified profile.
    			RegionDefault_JTextField.setText("");
    			RegionDefaultNote_JLabel.setText("<hbml><b>Unknown</b></html>");
    		}
    		else {
    			// Have a region for the specified profile.
    			RegionDefault_JTextField.setText(getDefaultRegion(profile));
    			RegionDefaultNote_JLabel.setText("From: " + getAwsUserConfigFile() + ", Profile: " + profile);
    		}
    	}
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

	/**
	 * Wait for invalidations that are in progress on a CloudFront distribution to complete.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @param distribtionId CloudFront distribution ID to list invalidations
 	 * @param waitMs the number of milliseconds to wait between invalidation checks
 	 * @param waitTimeout the number of milliseconds total to wait on invalidation checks
 	 * @param return true if invalidations completed, false if the timeout was reached
	 */
    public boolean waitForCloudFrontInvalidations(AwsSession awsSession, String region, String distributionId, int waitMs, int waitTimeout) {
    	String routine = getClass().getSimpleName() + "waitForCloudFrontInvalidations";
    	int waitTotal = -waitMs;
   		while ( waitTotal < waitTotal ) {
   			waitTotal += waitMs;
   			// Get the current invalidations.
   			List<InvalidationSummary> invalidations = getCloudFrontInvalidations(awsSession, region, distributionId);
   			if ( invalidations.size() == 0 ) {
   				// No more invalidations on the distribution:
   				// - TODO smalers 2022-06-06 evaluate whether to check caller reference
				Message.printStatus(2, routine,
					"Invalidation(s) have completed after after " + waitTotal + " ms (" + waitTotal/1000 + " seconds).");
   				return true;
   			}
   			else {
   				// Wait 5 seconds to allow invalidation to complete.
   				for ( InvalidationSummary summary : invalidations ) {
   					Message.printStatus(2, routine, "Invalidation status=" + summary.status() + " " + summary.createTime());
   				}
   				Message.printStatus(2, routine, "Have " + invalidations.size() +
   					" invalidations.  Waiting " + waitMs + " ms for invalidations to complete.");
      				TimeUtil.sleep(waitMs);
       		}
       	}
   		return false;
    }
	
}