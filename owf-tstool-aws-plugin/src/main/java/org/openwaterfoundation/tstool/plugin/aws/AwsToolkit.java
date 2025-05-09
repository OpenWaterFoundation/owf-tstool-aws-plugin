// AwsToolkit - utility functions for AWS as singleton

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.TagModeType;

import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringDictionary;
import RTi.Util.Time.TimeUtil;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
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
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.cloudfront.model.Tag;
import software.amazon.awssdk.services.cloudfront.model.Tags;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
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

	/**
	 * TODO smalers 2023-05-08 need to finish this.
	 * Determine whether the bucket has a root object (/).
	 * This may be by design or is a mistake.
	 * If no root object, removing all objects under the / will result in no / being shown.
	 * @param awsSession AWS session
	 * @param region region for S3
	 * @param bucket bucket
	 * @return null if cannot determine (no bucket specified, etc.), true if / object exists, and false if no / object exists
	 */
    public Boolean bucketHasRootObject( AwsSession awsSession, String region, String bucket ) {
    	String routine = getClass().getSimpleName() + ".bucketHasRootObject";
    	Boolean hasRoot = null;
    	try {
    		Region regionObject = Region.of(region);
    		S3Client s3 = S3Client.builder()
			  		.region(regionObject)
			  		.credentialsProvider(awsSession.getProfileCredentialsProvider())
			  		.build();
    		String prefix = "/";
    		String delimiter = "/";
    		boolean useDelimiter = true;
    		int maxKeys = -1;
    		int maxObjects = -1;
    		boolean listFiles = true;
    		boolean listFolders = true;
    		String regex = null;
    		List<AwsS3Object> objects = getS3BucketObjects (
    			s3,
		  		bucket,
		  		prefix,
		  		delimiter,
		  		useDelimiter,
		  		maxKeys, maxObjects,
	  			listFiles, listFolders,
	  			regex );
    		if ( objects.size() == 0 ) {
    			Message.printStatus(2, routine, "Bucket \"" + bucket + "\" has no root object.");
    			hasRoot = false;
    		}
    		else {
    			Message.printStatus(2, routine, "Bucket \"" + bucket + "\" has no " + objects.size() + " objects for / prefix.");
    			for ( AwsS3Object object : objects ) {
    				Message.printStatus(2, routine, "  Object = " + object);
    			}
    			hasRoot = true;
    		}
    	}
		catch ( Exception e ) {
			hasRoot = null;
		}
    	return hasRoot;
    }

    /**
     * Indicate whether the CloudFront distribution tags match the requested tags.
     * @param tags CloudFront distribution tags
     * @param tagDict a dictionary of tags to match
     * @return true if all the requested tag values are matched, false otherwise
     */
	public boolean cloudFrontDistributionTagsMatch ( Tags tags, StringDictionary tagDict ) {
		String routine = getClass().getSimpleName() + ".cloudFrontDistributionTagsMatch";
		int tagDictSize = 0;
		if ( tagDict != null ) {
			tagDictSize = tagDict.size();
		}
		if ( (tags == null) || !tags.hasItems() ) {
			// Distribution has no tags.
			if ( tagDictSize == 0 ) {
				// No items to check.
				Message.printStatus(2, routine, "No distribution tags and no requested tags so returning true.");
				return true;
			}
			else {
				// Have items to check.
				Message.printStatus(2, routine, "No distribution tags and do have requested tags so returning false.");
				return false;
			}
		}
		if ( tagDictSize == 0 ) {
			// Not checking tags so return true.
			Message.printStatus(2, routine, "No requested tags to check so returning true.");
			return true;
		}
		else {
			// Check the distribution's tags against the requested tag(s).
			int matchCount = 0;
			if ( (tags != null) && (tags.items().size() > 0) ) {
				// Have distribution tags to check.
				Map<String,String> map = tagDict.getLinkedHashMap();
				for ( Map.Entry<String, String> set : map.entrySet() ) {	
					// Tags does not behave like a map so have to iterate through the list of tags.
					for ( Tag tag : tags.items() ) {
						if  ( tag.key().equals(set.getKey()) && tag.value().equals(set.getValue())) {
							++matchCount;
							// TODO smalers 2023-12-15 what if multiple same-named tags are used?
							break;
						}
					}
				}
			}
			if ( matchCount == tagDictSize ) {
				// Matched all the requested tags.
				Message.printStatus(2, routine, "Matched " + matchCount + " requested tags so returning true.");
				return true;
			}
			else {
				// Did not match all the requested tags:
				// - could be because the distribution had no tags
				Message.printStatus(2, routine, "Matched " + matchCount + " of " +
					tagDict.size() + " requested tags so returning false.");
				return false;
			}
		}
	}
	
	/**
	 * Delete a list of S3 objects using keys.
	 * @param objectKeyList of S3 objects (keys) to delete
	 * @param problems list of problems generated during the delete,
	 * the size should be checked to determine if there were errors
	 */
	public List<S3CommandResult> deleteS3Objects ( List<AwsS3Object> objectKeyList ) {
		List<S3CommandResult> commandResults = new ArrayList<>();
		return commandResults;
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
	 * Format tags into a CSV string.
	 * @param tags the CloudFront tags to format
	 * @return tags as a CSV string "Tag1=Value1,Tag2=Value2,...".
	 */
	public String formatCloudFrontTagsAsCsv ( Tags tags, boolean includeSpace ) {
		StringBuilder b = new StringBuilder();
		for ( Tag tag : tags.items() ) {
			if ( b.length() > 0 ) {
				b.append(",");
				if ( includeSpace ) {
					b.append(" "); 
				}
			}
			b.append(tag.key());
			b.append("=");
			b.append(tag.value());
		}
		return b.toString();
	}
	
	/**
	 * Format a tag string from a map.
	 * @param tagMap a map (TreeMap, which is sorted by tag name), never null but may be empty
	 * @return tag string as "TagName1=TagValue1;TagName2=TagValue2..."
	 */
	public String formatTagString ( Map<String,String> tagMap ) {
		StringBuilder tagString = new StringBuilder();
		for ( Entry<String,String> entry : tagMap.entrySet() ) {
			if ( tagString.length() > 0 ) {
				tagString.append("&");
			}
			tagString.append(entry.getKey() + "=" + entry.getValue() );
		}
		return tagString.toString();
	}

	/**
	 * Format at tag string as "TagName1=TagValue1&TagName2=TagValue2...".
	 * @param tagString the tag string to be set, pending other checks
	 * @param tagModeType the tag mode type, which may cause modifications to 'tagString'
	 * @param existingTags the object's existing tags
	 * @return the updated tag string or null if there was an issue such as no input (should not set tags)
	 */
	public String formatTagString ( String tagString, TagModeType tagModeType, Map<String,String> existingTags ) {
		String newTagString = null;
		if ( (tagString == null) || tagString.isEmpty() ) {
			// Return null.
			newTagString = null;
		}
		if ( tagModeType == TagModeType.SET_ALL ) {
			// Use the string as is, which will set the tags to the specified values.
			newTagString = tagString;
		}
		else if ( tagModeType == TagModeType.SET ) {
			// Append/reset the specified tags to the existing tags:
			// - first parse the new tag string
			// - then modify based on the existing tags
			Map<String,String> tagMap = AwsToolkit.getInstance().parseTagString(tagString);
			// Loop through the existing tags:
			// - if any match the new, use the new
			// - else, add to the new
			for ( Map.Entry<String,String> entry : existingTags.entrySet() ) {
				String key = entry.getKey();
				String value = entry.getValue();
				boolean found = false;
				for ( Map.Entry<String,String> newEntry : tagMap.entrySet() ) {
					if ( newEntry.getKey().equals(key) ) {
						found = true;
						break;
					}
				}
				if ( ! found ) {
					// Existing was not found in the new tags so add.
					tagMap.put(key, value);
				}
			}
			newTagString = AwsToolkit.getInstance().formatTagString(tagMap);
		}
		return newTagString;
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
	 * @param tagDict dictionary of tags to match
	 * @param commentPattern if not null and not empty, a Java pattern to match the distribution comment
	 * @return the distribution ID to use after evaluating input, or null if not matched.
	 */
   	public String getCloudFrontDistributionId ( AwsSession awsSession, String region,
   		String distributionId, StringDictionary tagDict, String commentPattern ) {
   		// Get the list of distributions.
   		List<DistributionSummary> distributions = getCloudFrontDistributions ( awsSession, region );
   		// Match a specific distribution.
   		for ( DistributionSummary distribution : distributions ) {
   			if ( (distributionId != null) && !distributionId.isEmpty() && distribution.id().equals(distributionId) ) {
   				// Matched the distribution ID.
   				return distributionId;
   			}
   			else if ( (tagDict != null) && (tagDict.size() > 0) ) {
   				Tags tags = getCloudFrontDistributionTags(awsSession, region, distribution.arn());
   				if ( AwsToolkit.getInstance().cloudFrontDistributionTagsMatch(tags, tagDict) ) {
   					return distribution.id();
   				}
   			}
   			else if ( (commentPattern != null) && !commentPattern.isEmpty() && distribution.comment().matches(commentPattern) ) {
   				// Matched the comment.
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
 	 * Get the list of CloudFront tags for a distribution.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @param distributionArn the CloudFront arn of interest
 	 * @return a list of DistributionSummary or an empty list if no distributions
 	 */
	public Tags getCloudFrontDistributionTags (
		AwsSession awsSession, String region, String distributionArn ) {
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
			ListTagsForResourceRequest request = ListTagsForResourceRequest.builder()
				.resource(distributionArn)
				.build();
			ListTagsForResourceResponse response = cloudfront.listTagsForResource(request);
			Tags tags = response.tags();
    		return tags;
		}
		catch ( Exception e ) {
			// Log the error and return an empty list:
			// - may have requested an invalid region
			String routine = getClass().getSimpleName() + ".getCloudFrontDistributionTags";
			Message.printWarning(3, routine, "Error getting list of distribution tags for distribution ARN \"" +
				distributionArn + "\" (" + e + ").");
		}
		Tags tags = Tags.builder().build();
		return tags;
	}

	/**
 	 * Get the list of CloudFront invalidations with any status.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @param distribtionId CloudFront distribution ID to list invalidations
 	 * @return a list of InvalidationSummary or an empty list if no invalidations
 	 */
	public List<InvalidationSummary> getCloudFrontInvalidations ( AwsSession awsSession, String region, String distributionId ) {
		return getCloudFrontInvalidations ( awsSession, region, distributionId, null );
	}

	/**
 	 * Get the list of CloudFront invalidations with status=InProgress.
 	 * @param awsSession the AWS session, containing profile
 	 * @param region the AWS region
 	 * @param distributionId CloudFront distribution ID to list invalidations
 	 * @param status the invalidation status ("InProcess", "Complete") or special "All" for all.
 	 * The default if not specified is "InProcess".
 	 * @return a list of InvalidationSummary or an empty list if no invalidations
 	 */
	public List<InvalidationSummary> getCloudFrontInvalidations ( AwsSession awsSession, String region, String distributionId,
		String status ) {
		String routine = getClass().getSimpleName() + ".getCloudFrontInvalidations";

		Message.printStatus(2, routine, "Getting CloudFront invalidation for distributionId=" + distributionId + " status=" + status);
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
				if ( (status == null) || status.isEmpty() || status.equalsIgnoreCase("InProcess") ) {
					// Default is InProcess, which are invalidations that are currently being pushed.
					if ( summary.status().equalsIgnoreCase("InProcess") ) {
						activeList.add(summary);
					}
				}
				else if ( (status != null) && summary.status().equalsIgnoreCase(status) ) {
					// Add for the requested status, for example "Completed".
					activeList.add(summary);
				}
				else if ( (status != null) && status.equalsIgnoreCase("All") ) {
					// Always add.
					activeList.add(summary);
				}
			}
    		return activeList;
		}
		catch ( Exception e ) {
			// Log the error and return an empty list:
			// - may have requested an invalid region
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
	 * Get the default region for the Cost Explorer.
	 * This seems to be aws-global because the cost data are mananged there?
	 */
	public String getDefaultRegionForCostExplorer () {
		return "aws-global";
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
	 * Return the region for the bucket.
	 * @param bucketName name of the bucket to check
	 * @return the region name for the bucket
	 */
	public String getRegionForS3Bucket ( S3Client s3Client, String bucketName ) {
		// Request the bucket's location (region)
        GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
            .bucket(bucketName)
            .build();

        GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);

        // Return the region, or return "us-east-1" as the default region for old-style buckets.
        if ( locationResponse.locationConstraintAsString() == null ) {
        	return "us-east-1";
        }
        else {
        	return locationResponse.locationConstraintAsString();
        }
	}

	/**
	 * Return the tags for an S3 bucket.
	 * If the bucket does not exist, an empty list is returned.
	 * @param s3Client the S3 client to use, must be for the bucket's region
	 * @param bucketName name of the bucket to check
	 * @return the tags for the bucket, may be an empty list
	 */
	public List<software.amazon.awssdk.services.s3.model.Tag> getS3BucketTags ( S3Client s3Client, String bucketName ) {
		// Request the bucket's location (region)
        GetBucketTaggingRequest taggingRequest = null;
       	taggingRequest = GetBucketTaggingRequest.builder()
            .bucket(bucketName)
            .build();

        try {
        	GetBucketTaggingResponse taggingResponse = s3Client.getBucketTagging(taggingRequest);
        	return taggingResponse.tagSet();
        }
        catch ( Exception e ) {
        	// Exception will occur if the object does not exist:
        	// - return an empty list of tags
        	return new ArrayList<>();
        }
	}

	/**
	 * Get a single S3 bucket's tags as a map.
	 * This is used, for example, to retrieve an bucket's tags to list the buckets.
	 * @param s3Client S3Client instance to use for the request
	 * @param bucketName S3 bucket name
	 * @return the tags as a map (TreeMap is returned), will never be null
	 */
	public Map<String,String> getS3BucketTagsAsMap ( S3Client s3Client, String bucketName ) {
		Map<String,String> tagMap = new TreeMap<>();
		
		// First get the tags.
		List<software.amazon.awssdk.services.s3.model.Tag> tags = getS3BucketTags ( s3Client, bucketName );
		for ( software.amazon.awssdk.services.s3.model.Tag tag : tags ) {
			tagMap.put(tag.key(), tag.value());
		}
		return tagMap;
	}
	

	/**
	 * Return the tags for an S3 object.
	 * If the object does not exist, an empty list is returned.
	 * @param bucketName name of the bucket to check
	 * @param objectKey object key
	 * @param versionId object version ID, specify null no not use the version
	 * @return the tags for the object, may be an empty list
	 */
	public List<software.amazon.awssdk.services.s3.model.Tag> getS3ObjectTags ( S3Client s3Client, String bucketName, String objectKey, String versionId ) {
		// Request the bucket's location (region)
        GetObjectTaggingRequest taggingRequest = null;
        if ( versionId == null ) {
        	// No version ID.
        	taggingRequest = GetObjectTaggingRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build();
        }
        else {
        	// Use the version ID.
        	taggingRequest = GetObjectTaggingRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .versionId(versionId)
            .build();
        }

        try {
        	GetObjectTaggingResponse taggingResponse = s3Client.getObjectTagging(taggingRequest);
        	return taggingResponse.tagSet();
        }
        catch ( Exception e ) {
        	// Exception will occur if the object does not exist:
        	// - return an empty list of tags
        	return new ArrayList<>();
        }
	}

	/**
	 * Get a single S3 object's tags as a map.
	 * This is used, for example, to retrieve an object's tags so that they can be evaluated before setting new tags.
	 * @param s3Client S3Client instance to use for the request
	 * @param bucketName S3 bucket name
	 * @param objectKey object key to match
	 * @param versionId object version ID to match
	 * @return the tags as a map (TreeMap is returned), will never be null
	 */
	public Map<String,String> getS3ObjectTagsAsMap ( S3Client s3Client, String bucketName, String objectKey, String versionId ) {
		Map<String,String> tagMap = new TreeMap<>();
		
		// First get the tags.
		List<software.amazon.awssdk.services.s3.model.Tag> tags = getS3ObjectTags ( s3Client, bucketName, objectKey, versionId );
		for ( software.amazon.awssdk.services.s3.model.Tag tag : tags ) {
			tagMap.put(tag.key(), tag.value());
		}
		return tagMap;
	}
	
	/**
	 * Get a single S3 object's tags as a string "TagName1=Value1&TagName2=Value2...".
	 * This is used, for example, to retrieve an object's tags so that they can be evaluated before setting new tags.
	 * @param s3Client S3Client instance to use for the request
	 * @param bucketName S3 bucket name
	 * @param objectKey object key to match
	 * @param versionId object version ID to match
	 * @return the tag string, or an empty string if no tags, will never be null
	 */
	public String getS3ObjectTagsAsString ( S3Client s3Client, String bucketName, String objectKey, String versionId ) {
		StringBuilder tagString = new StringBuilder();
		
		// First get the tags.
		List<software.amazon.awssdk.services.s3.model.Tag> tags = getS3ObjectTags ( s3Client, bucketName, objectKey, versionId );
		for ( software.amazon.awssdk.services.s3.model.Tag tag : tags ) {
			if ( tagString.length() > 0 ) {
				tagString.append("&");
			}
			tagString.append(tag.key() + "=" + tag.value());
		}
		return tagString.toString();
	}

	/**
	 * List S3 bucket objects.
	 * This is a utility class that is intended to support more complex code,
	 * for example to list a bucket's objects as keys before deleting a folder,
	 * or to confirm that objects do or don't exist after an action.
	 * @param s3Client S3Client instance to use for the request
	 * @param bucket S3 bucket
	 * @param prefix prefix to filter the keys,
	 * should not start with / unless the bucket has a top-level / object
	 * @param delimiter delimiter to use with prefix requests, only used when useDelimiter=true,
	 * if null, the default is /
	 * @param useDelimiter whether or not to specify the delimiter, used when listing a folder's contents
	 * @param maxKeys maximum number of keys per request, used if > 0
	 * @param maxObjects maximum number of objects returned, used if > 0
	 * @param listFiles whether to list files in the results
	 * @param listFolders whether to list folders in the results
	 * @param regex Java regular expression to filter the output keys
	 * @return list of AwsS3Object instances
	 * @throws Exception
	 */
	public List<AwsS3Object> getS3BucketObjects (
		S3Client s3Client,
		String bucket,
		String prefix, String delimiter, boolean useDelimiter, int maxKeys, int maxObjects,
		boolean listFiles, boolean listFolders, String regex
		) throws Exception {
		String routine = getClass().getSimpleName() + ".doS3ListBucketObjects";
		
		List<AwsS3Object> s3Objects = new ArrayList<>();
		
		if ( (delimiter == null) || delimiter.isEmpty() ) {
			// Default delimiter, will only actually be used in the request if useDelimiter=true.
			delimiter = "/";
		}

   	    ListObjectsV2Request.Builder builder = ListObjectsV2Request
    		.builder()
    		.fetchOwner(Boolean.TRUE) // Get the owner so it can be shown in output.
    		.bucket(bucket); // Bucket is required.
    	if ( maxKeys > 0 ) {
    		// Set the maximum number of keys that will be returned per request.
    		builder.maxKeys(maxKeys);
    	}

    	if ( useDelimiter && (delimiter != null) && !delimiter.isEmpty() ) {
    		// Using the delimiter to list only files in a folder.
    		// - if prefix is not also specified, will list root objects
    		// - if prefix is also specified, will list objects in the folder for the prefix
    		builder.delimiter(delimiter);
      	}

    	boolean doPrefix = false;
    	if ( (prefix != null) && !prefix.isEmpty() ) {
    		// Specify the prefix to list a folder's contents:
    		// - if delimiter is not also specified, will list root objects
    		// - if delimiter is also specified, will list objects in the folder for the prefix
    		builder.prefix(prefix);
    		// Set boolean to simplify the code below.
    		doPrefix = true;
    	}
    	
    	// Create a DataTable to hold the results

    	ListObjectsV2Request request = builder.build();
    	ListObjectsV2Response response = null;
    	//TableRecord rec = null;
    	boolean allowDuplicates = false;
    	boolean done = false;
    	int objectCount = 0;
    	int fileCount = 0;
    	int folderCount = 0;

    	// Indicate that AwsS3Object instances should be returned.
    	boolean doS3Objects = true;

    	while ( !done ) {
    		response = s3Client.listObjectsV2(request);
    		// Process files and folders separately, with the maximum count checked based on what is returned.
    		if ( listFiles || listFolders ) {
    			// S3Objects can contain files or folders (objects with key ending in /, typically with size=0).
    			// Loop in any case to get the count.  May output to table and/or file.
    			for ( S3Object s3Object : response.contents() ) {
    				// Check the maximum object count, to protect against runaway processes.
    				if ( (maxObjects > 0) && (objectCount >= maxObjects) ) {
			  			// Quit saving objects when the limit has been reached.
    					if ( Message.isDebugOn ) {
    						Message.printDebug(1, routine, "Have reached maxObjects limit " + maxObjects
    							+ " - quit getting objects.");
    					}
    					break;
    				}
    				// Output to table and/or file, as requested:
    				// - key is the full path to the file
    				// - have size, owner and modification time properties
   					String key = s3Object.key();
   					if ( Message.isDebugOn ) {
   						Message.printDebug(1, routine, "Processing key \"" + key + "\".");
   					}
   					if ( doPrefix && prefix.endsWith("/") && key.equals(prefix) ) {
   						// Do not include the requested prefix itself because want the contents of the folder,
   						// not the folder itself.
   						Message.printStatus(2, routine, "Ignoring Prefix that is a folder because want folder contents.");
   						continue;
   					}
   					if ( regex != null ) {
   						// Want to apply a regular expression to the key.
   						if ( !key.matches(regex) ) {
   							continue;
   						}
   					}
   					if ( !listFolders && key.endsWith("/") ) {
   						// Is a folder and don't want folders so continue.
   						continue;
   					}
   					else if ( !listFiles && !key.endsWith("/") ) {
   						// Is a file and don't files want so continue.
   						continue;
   					}
   					/* TODO smalers 2023-02-03 evaluate whether to fill a table.
    				if ( table != null ) {
    					if ( !allowDuplicates ) {
    						// Try to match the object key, which is the unique identifier.
    						rec = table.getRecord ( objectKeyCol, s3Object.key() );
    					}
    					if ( rec == null ) {
    						// Create a new record.
    						rec = table.addRecord(table.emptyRecord());
    					}
    					// Set the data in the record.
    					rec.setFieldValue(objectKeyCol,s3Object.key());
    					// Set the name as the end of the key without folder delimiter.
    					String name = s3Object.key();
    					if ( name.endsWith("/") ) {
    						name = name.substring(0,(name.length() - 1));
    						int pos = name.lastIndexOf("/");
    						if ( pos >= 0 ) {
    							// Have subfolders:
    							// - strip so only the name remains
    							name = name.substring(pos + 1);
    						}
    					}
   						rec.setFieldValue(objectNameCol,name);
    					// Set the parent name as the folder above the current name string.
   						int pos = name.lastIndexOf("/");
   						String parentName = "";
   						if ( pos >= 0 ) {
   							// Have subfolders:
   							// - strip so only the name remains
   							parentName = name.substring(pos + 1);
   						}
   						rec.setFieldValue(objectParentNameCol,parentName);
    					if ( key.endsWith("/") ) {
    						rec.setFieldValue(objectTypeCol,"folder");
    					}
    					else {
    						rec.setFieldValue(objectTypeCol,"file");
    					}
    					rec.setFieldValue(objectSizeCol,s3Object.size());
    					if ( s3Object.owner() == null ) {
    						rec.setFieldValue(objectOwnerCol,"");
    					}
    					else {
    						rec.setFieldValue(objectOwnerCol,s3Object.owner().displayName());
    					}
    					rec.setFieldValue(objectLastModifiedCol,
    						new DateTime(OffsetDateTime.ofInstant(s3Object.lastModified(), zoneId), dateTimeBehaviorFlag, timezone));
    				}
    				*/
    				if ( doS3Objects ) {
    					if ( !allowDuplicates ) {
    						// Try to match an existing object in the list:
    						// - the object key is the unique identifier
    						// - not sure that this will be an issue
    						AwsS3Object foundS3Object = null;
    						for ( AwsS3Object s3Object2 : s3Objects ) {
    							if ( s3Object2.getKey().equals(s3Object.key())) {
    								foundS3Object = s3Object2;
    								break;
    							}
    						}
    						if ( foundS3Object != null ) {
    							// Overwrite data in the existing object.
    							foundS3Object.setBucket(bucket);
    							foundS3Object.setKey(s3Object.key());
    							foundS3Object.setSize(s3Object.size());
    							foundS3Object.setOwner(s3Object.owner().displayName());
    							foundS3Object.setLastModified(s3Object.lastModified());
    						}
    						else {
    							// Create a new object and pass data to the constructor.
    							AwsS3Object newS3Object = new AwsS3Object ( bucket, s3Object.key(), s3Object.size(),
    								s3Object.owner().displayName(), s3Object.lastModified() );
    							s3Objects.add ( newS3Object );
    						}
    					}
    					else {
    						// Always create a new instance.
    						AwsS3Object newS3Object = new AwsS3Object ( bucket, s3Object.key(), s3Object.size(),
    							s3Object.owner().displayName(), s3Object.lastModified() );
    						s3Objects.add ( newS3Object );
    					}
    				}
   					// Increment the count of objects processed (includes files and folders).
   					++objectCount;
   					if ( key.endsWith("/") ) {
   						++folderCount;
   					}
   					else {
   						++fileCount;
   					}
    			}
    		}
    		if ( listFolders ) {
    			// Common prefixes are only used with folders:
    			// - the key will be from the root to the / (inclusive) after the prefix
    			for ( CommonPrefix commonPrefix : response.commonPrefixes() ) {
			  		// Check the maximum object count, to protect against runaway processes.
    				if ( (maxObjects > 0) && (objectCount >= maxObjects) ) {
			  			// Quit saving objects when the limit has been reached.
						break;
			  		}
   					if ( doPrefix && prefix.endsWith("/") && commonPrefix.prefix().equals(prefix) ) {
   						// Do not include the requested prefix itself because want the contents of the folder,
   						// not the folder itself.
   						Message.printStatus(2, routine, "Ignoring Prefix that is a folder because want folder contents.");
   						continue;
   					}
   					if ( regex != null ) {
   						// Want to apply a regular expression to the key.
   						if ( !commonPrefix.prefix().matches(regex) ) {
   							continue;
   						}
   					}
    				// Output to table and/or file, as requested:
			  		// - key is the path to the folder including trailing / to indicate a folder
			  		// - only have the key since folders are virtual and have no properties
   					/* TODO smalers 2023-02-03 evaluate whether to fill table.
    				if ( table != null ) {
    					if ( !allowDuplicates ) {
    						// Try to match the object key, which is the unique identifier.
    						rec = table.getRecord ( objectKeyCol, commonPrefix.prefix() );
    					}
    					if ( rec == null ) {
    						// Create a new record.
    						rec = table.addRecord(table.emptyRecord());
    					}
    					// Set the data in the record.
    					rec.setFieldValue(objectKeyCol, commonPrefix.prefix());
    					// Set the name as the end of the key without folder delimiter.
    					String name = commonPrefix.prefix();
    					if ( name.endsWith("/") ) {
    						name = name.substring(0,(name.length() - 1));
    						int pos = name.lastIndexOf("/");
    						if ( pos >= 0 ) {
    							// Have subfolders:
    							// - strip so only the name remains
    							name = name.substring(pos + 1);
    						}
    					}
   						rec.setFieldValue(objectNameCol,name);
   						int pos = name.lastIndexOf("/");
   						String parentName = "";
   						if ( pos >= 0 ) {
   							// Have subfolders:
   							// - strip so only the name remains
   							parentName = name.substring(pos + 1);
   						}
   						rec.setFieldValue(objectParentNameCol,parentName);
    					rec.setFieldValue(objectTypeCol,"folder");
    				}
    				*/
    				if ( doS3Objects ) {
    					if ( !allowDuplicates ) {
    						// Try to match an existing object in the list:
    						// - the object key is the unique identifier
    						// - not sure that this will be an issue
    						AwsS3Object foundS3Object = null;
    						for ( AwsS3Object s3Object2 : s3Objects ) {
    							if ( s3Object2.getKey().equals(commonPrefix.prefix())) {
    								foundS3Object = s3Object2;
    								break;
    							}
    						}
    						if ( foundS3Object != null ) {
    							// Overwrite data in the existing object.
    							Long size = Long.valueOf(0);
    							String owner = "";
    							Instant lastModified = null;
    							foundS3Object.setBucket(bucket);
    							foundS3Object.setKey(commonPrefix.prefix());
    							foundS3Object.setSize(size);
    							foundS3Object.setOwner(owner);
    							foundS3Object.setLastModified(lastModified);
    						}
    						else {
    							// Create a new object and pass data to the constructor.
    							Long size = Long.valueOf(0);
    							String owner = "";
    							Instant lastModified = null;
    							AwsS3Object newS3Object = new AwsS3Object ( bucket, commonPrefix.prefix(), size, owner, lastModified );
    							s3Objects.add ( newS3Object );
    						}
    					}
    					else {
    						// Always create a new instance.
   							Long size = Long.valueOf(0);
   							String owner = "";
   							Instant lastModified = null;
    						AwsS3Object newS3Object = new AwsS3Object ( bucket, commonPrefix.prefix(), size, owner, lastModified );
    						s3Objects.add ( newS3Object );
    					}
    				}
   					// Increment the count of objects processed (includes files and folders).
			  		++objectCount;
   					++folderCount;
		  		}
    		}
    		if ( response.nextContinuationToken() == null ) {
    			done = true;
    		}
    		request = request.toBuilder()
   				.continuationToken(response.nextContinuationToken())
   				.build();
    	}
    	// Sort the table by key if both files and folders were queried:
    	// - necessary because files come out of the objects and folders out of common prefixes
    	if ( listFiles && listFolders ) {
    		String [] sortColumns = { "Key" };
    		int [] sortOrder = { 1 };
  			// TODO smalers 2023-02-03 evaluate whether to fill table.
    		//table.sortTable( sortColumns, sortOrder);
    	}
    	if ( Message.isDebugOn ) {
    		Message.printStatus ( 2, routine, "Response has objects=" + response.contents().size()
    			+ ", commonPrefixes=" + response.commonPrefixes().size() );
    		Message.printStatus ( 2, routine, "List has fileCount=" + fileCount + ", folderCount="
    			+ folderCount + ", objectCount=" + objectCount );
    	}
       		
		// TODO smalers 2023-02-03 evaluate whether to fill table.
       	//return table;
		return s3Objects;
	}

	/**
 	* Get the list of S3 buckets.
 	* @param awsSession the AWS session, containing profile
 	* @param region the AWS region to match (null, empty, or "*" will match all)
 	*/
	public List<Bucket> getS3Buckets ( AwsSession awsSession, String region ) {
		ProfileCredentialsProvider credentialsProvider = null;
		String profile = awsSession.getProfile();
		try {
			credentialsProvider = ProfileCredentialsProvider.create(profile);
			S3Client s3Client = S3Client.builder()
				.credentialsProvider(credentialsProvider)
				// Adding the region here does not actually filter the buckets by region.
				//.region(regionObject)
				.build();
    		ListBucketsRequest request = ListBucketsRequest.builder().build();
    		ListBucketsResponse response = s3Client.listBuckets(request);
    		// Need to filter the buckets after the request.
    		List<Bucket> buckets = new ArrayList<>();
    		boolean doMatchRegion = false;
    		if ( (region != null) && !region.isEmpty() && !region.equals("*") ) {
    			doMatchRegion = true;
    		}
   			if ( doMatchRegion ) {
   				for ( Bucket bucket : response.buckets() ) {
    				if ( getRegionForS3Bucket(s3Client, bucket.name()).equals(region) ) {
    					buckets.add(bucket);
    				}
    			}
   			}
    		else {
    			// Return the full list.
    			buckets = response.buckets();
    		}
    		return buckets;
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
     * Parse a tag string "TagName1=TagValue1&TagName2=TagValue2" into a map.
     * A TreeMap is returned.
     * @param tagString tag string to parse
     * @return a map of the parsed tags
     */
    public Map<String,String> parseTagString ( String tagString ) {
    	SortedMap<String,String> map = new TreeMap<>();
    	String [] parts = null;
    	if ( !tagString.contains("&") ) {
    		// One tag.
    		parts = new String[1];
    		parts[0] = tagString;
    	}
    	else {
    		// Multiple tags.
    		parts = tagString.split("&");
    	}
   		// Split multiple tags.
    	for ( String part : parts ) {
    		String [] tagParts = part.split("=");
    		if ( tagParts.length == 2 ) {
    			// Well-formed tag.
    			map.put ( tagParts[0], tagParts[1] );
    		}
    	}
    	return map;
    }

    /**
     * UI helper to populate the Bucket choices based on profile and region selections.
     * @param awsSession the AwsSession instance for the session, for authentication
     * @param region the selected region (e.g., 'us-west-1' or '*' to match all regions)
     * @param bucket_JComboBox the combo box for buckets
     * @param icludeBlank if true, include a blank choice at the top
     */
    public void uiPopulateBucketChoices ( AwsSession awsSession, String region, SimpleJComboBox bucket_JComboBox, boolean includeBlank) {
    	String routine = getClass().getSimpleName() + ".uiPopulateBucketChoices";
    	boolean debug = false;
    	if ( Message.isDebugOn ) {
    		debug = true;
    	}
    	List<String> bucketChoices = new ArrayList<>();
    	if ( awsSession == null ) {
    		// Startup - can't populate the buckets.
    		if ( debug ) {
    			Message.printStatus(2, routine, "AWS session is null (startup) - not populating the list of buckets." );
    		}
    		return;
    	}
    	else {
    		String profile = awsSession.getProfile();
    		if ( debug ) {
    			Message.printStatus(2, routine, "Getting the list of buckets for profile \"" + profile + "\"." );
    		}
    		// Get the list of buckets.
    		if ( (region == null) || region.isEmpty() ) {
    			// Startup or region specified:
    			// - try to get the default region from the user's configuration file
    			// - can't populate the buckets.
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
    					Message.printStatus(2, routine, "Default region for profile \"" + profile
    						+ "\" is not specified - can't populate the list of buckets." );
    					bucket_JComboBox.setData(bucketChoices);
    					return;
    				}
    				else {
    					// Region is used below.
    				}
    			}
    		}
    		// Have a region from specified value or default.
    		if ( debug ) {
    			Message.printStatus(2, routine, "Profile is \"" + profile + "\", region is \"" + region
    				+ "\" - populating the list of buckets." );
    		}	
    		List<Bucket> buckets = AwsToolkit.getInstance().getS3Buckets(awsSession, region);
    		for ( Bucket bucket : buckets ) {
    			bucketChoices.add ( bucket.name() );
    			if ( debug ) {
    				Message.printStatus(2, routine, "Populated bucket: " + bucket.name() );
    			}
    		}
    		Collections.sort(bucketChoices);
    		if ( includeBlank ) {
    			// Add a blank because some services don't use.
    			bucketChoices.add(0,"");
    		}
    		// This will reset data if already set.
    		bucket_JComboBox.setData(bucketChoices);
    		if ( bucket_JComboBox.getItemCount() > 0 ) {
    			// Select the first bucket by default.
    			bucket_JComboBox.select ( 0 );
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
     * UI helper to populate the Region choices based on profile.
     * @param awsSession the AwsSession instance for the session, for authentication
     * @param region_JComboBox the region combo box to populate
     */
    public void uiPopulateRegionChoices ( AwsSession awsSession, SimpleJComboBox region_JComboBox) {
    	// Getting the regions does not require authentication.
    	List<Region> regions = Region.regions();
	   	List<String> regionChoices = new ArrayList<>();
	   	for ( Region region : regions ) {
		   	RegionMetadata meta = region.metadata();
		   	if ( meta == null ) {
		   		regionChoices.add ( region.id() );
		   	}
		   	else {
			   	regionChoices.add ( region.id() + " - " + region.metadata().description());
		   	}
	   	}

   		Collections.sort(regionChoices);

   		// Add a blank because some services don't use.
   		regionChoices.add(0,"");
   		region_JComboBox.setData(regionChoices);
    	if ( region_JComboBox.getItemCount() > 0 ) {
    		// Select the first bucket by default.
    		region_JComboBox.select ( 0 );
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