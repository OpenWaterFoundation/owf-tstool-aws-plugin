// S3TreeView - organizes S3 objects in an hierarchical tree

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

package org.openwaterfoundation.tstool.plugin.aws.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3_Object;

import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
Organizes S3 objects in an hierarchical tree.
*/
public class S3TreeView {
    
	/**
	The view identifier.
	*/
	private String bucket = "";

	/**
	The list of TreeViewNode objects.
	*/
	private SimpleJTree_Node rootNode;

	/**
	 * S3 client to use for S3 interactions.
	 */
	S3Client s3 = null;
	
	/**
	 * Whether to do lazy loading on folders:
	 * - true, avoids huge S3 bucket download but results in multiple smaller requests, and associated lag
	 * - false, only loads folder list from S3 when folder is expanded, results in multiple small requests
	 *   each with associated lag
	 */
	boolean doLazyLoad = false;

	/**
	Construct a tree view with the given identifier.
	@param awsSession the AWS session, contains authentication information
	@param region the region to use for S3
	@param bucket the bucket to list
	*/
	public S3TreeView ( AwsSession awsSession, String region, String bucket ) {
		String routine = getClass().getSimpleName() + ".S3TreeView";
		setBucket ( bucket );
		List<String> problems = new ArrayList<>();

		Region regionO = Region.of(region);

		// Create the s3 client that will be used to interact with S3.
		try {
			this.s3 = S3Client.builder()
				.region(regionO)
				.credentialsProvider(awsSession.getProfileCredentialsProvider())
				.build();
		}
		catch ( Exception e ) {
			Message.printWarning(1, routine, "Error creating S3 client." );
			Message.printWarning(1, routine, e );
		}

		try {
			createTreeView(problems);
		}
		catch ( Exception e ) {
			Message.printWarning(1, routine, "Error creating S3 tree." );
			Message.printWarning(1, routine, e );
		}
	}

	/**
	Create the S3 view by querying S3.
	*/
	public void createTreeView ( List<String> problems ) throws IOException {
		String routine = getClass().getSimpleName() + ".createTreeView";

    	SimpleJTree_Node folderNode = null; // Active node (a "folder").
    	SimpleJTree_Node nodePrev = null; // The node processed in the previous row.

    	// Root node matches the bucket.
        this.rootNode = new SimpleJTree_Node(this.bucket);
        folderNode = this.rootNode;
        nodePrev = folderNode;
        
        // Create a tree.
        S3TreeView_JTree s3JTree = new S3TreeView_JTree(this.rootNode);
        
        // Add the top-level folders under the bucket:
        // - prefix="" and delimeter="/" will return the top-level folders
        listObjects ( s3JTree, this.rootNode, "", this.doLazyLoad );
	}

	/**
	Get the bucket.
	@return the bucket.
	*/
	public String getBucket() {
    	return this.bucket;
	}
	
	/**
	Get the root node.
	*/
	public SimpleJTree_Node getRootNode () {
    	return this.rootNode;
	}

	/**
	 * List objects and add to a folder.
	 * @param parentNode the parent tree node under which to list files and folders
	 * @param prefix the prefix used to query the results
	 * @param lazyLoad if true do a lazy load when folders are expanded, if false load the whole tree up front
	 */
	private void listObjects ( S3TreeView_JTree s3JTree, SimpleJTree_Node parentNode, String prefix, boolean lazyLoad ) {
		String routine = getClass().getSimpleName() + ".listObjects";
		int maxKeys = -1;
		String delim = "/";
		if ( !lazyLoad ) {
			// Set the behavior to load all the bucket files.
			prefix = null;
			delim = null;
		}
       	software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = ListObjectsV2Request
    		.builder()
    		.fetchOwner(Boolean.TRUE)
    		.bucket(bucket);
       	if ( prefix != null ) {
    		builder.prefix(prefix);
       	}
       	if ( delim != null ) {
    		builder.delimiter(delim);
       	}
    	if ( maxKeys > 0 ) {
    		// Set the maximum number of keys that will be returned.
    		builder.maxKeys(maxKeys);
    	}
    	if ( (prefix != null) && !prefix.isEmpty() ) {
    		// Set the key prefix to match.
    		builder.prefix(prefix);
    	}
    	    	
    	ListObjectsV2Request request = builder.build();
    	ListObjectsV2Response response = null;
       	//int dateTimeBehaviorFlag = 0;
       	boolean done = false;
      	int objectCount = 0;
      	int maxObjects = 2000;

       	while ( !done ) {
       		response = s3.listObjectsV2(request);
   			Message.printStatus(2, routine, "S3 list response for bucket=" + bucket
   				+ " prefix=" + prefix
   				+ " delimiter=\"" + delim + "\""
   				+ " has " + response.contents().size() + " objects.");
       		for ( S3Object serviceS3Object : response.contents() ) {
       			// Check the maximum object count, to protect against runaway processes.
       			if ( objectCount >= maxObjects ) {
       				break;
       			}
       			// Transfer the AWS S3Object into an AwsS3_Object for use with the tree node.
       			AwsS3_Object s3Object = new AwsS3_Object(
       				serviceS3Object.key(),
       				serviceS3Object.size(),
       				serviceS3Object.owner().displayName(),
       				serviceS3Object.lastModified() );
       			Message.printStatus(2, routine, "Adding node: " + s3Object.getKey());
       			parentNode.add( new S3JTree_Node(s3Object, s3JTree));
       			/*
       				rec.setFieldValue(objectSizeKbCol,s3Object.size());
       				if ( s3Object.owner() == null ) {
       					rec.setFieldValue(objectOwnerCol,"");
       				}
       				else {
       					rec.setFieldValue(objectOwnerCol,s3Object.owner().displayName());
       				}
       				rec.setFieldValue(objectLastModifiedCol,
       					new DateTime(OffsetDateTime.ofInstant(s3Object.lastModified(), zoneId), behaviorFlag, timezone));
       				// Increment the count of objects processed.
       			*/
       			++objectCount;
       		}
       		if ( response.nextContinuationToken() == null ) {
       			done = true;
       		}
       		request = request.toBuilder()
      				.continuationToken(response.nextContinuationToken())
      				.build();
       	}
	}

	/**
	Set the bucket.
	@param bucket the bucket.
	*/
	public void setBucket(String bucket) {
    	this.bucket = bucket;
	}

}
