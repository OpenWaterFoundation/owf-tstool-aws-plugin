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
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3ObjectType;

import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
Organizes S3 objects in an hierarchical tree.
This class manages a tree view without including all of the UI components.
*/
public class S3TreeView {
    
	/**
	The view identifier.
	*/
	private String bucket = "";

	/**
	The list of TreeViewNode objects.
	*/
	private S3JTreeNode rootNode = null;

	/**
	 * Tree used for the view.
	 */
    private S3JTree s3JTree = null;

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
	This sets up the tree the first time.
	@problems problems that occur setting up the tree
	*/
	public void createTreeView ( List<String> problems ) throws IOException {
		String routine = getClass().getSimpleName() + ".createTreeView";

    	// Root node matches the bucket:
    	// - use a null tree because it is created below
		Message.printStatus(2, routine, "Creating root node for bucket: " + this.bucket);
        AwsS3Object rootS3Object = new AwsS3Object(this.bucket);
        this.rootNode = new S3JTreeNode(rootS3Object, null);
        
        // Create a tree with the root node:
        // - because the root node did not have the tree set, set it below
        this.s3JTree = new S3JTree(this.rootNode);
        this.rootNode.setS3JTree(s3JTree);
        
        // Create the tree under the bucket:
        // - if lazy load will only populate the top level folders
        // - currently the full tree is populated (doLazyLoad=false)
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
	 * @param doLazyLoad if true do a lazy load when folders are expanded, if false load the whole tree up front
	 */
	private void listObjects ( S3JTree s3JTree, S3JTreeNode parentNode, String prefix, boolean doLazyLoad ) {
		String routine = getClass().getSimpleName() + ".listObjects";
		int maxKeys = -1;
		String delim = "/";
		boolean debug = false;
		if ( Message.isDebugOn ) {
			debug = true;
		}

		if ( doLazyLoad ) {
			// Lazy load:
			// - only add the top-level folders and files
			prefix = "";
			delim = "/";
		}
		else {
			// Doing a full load:
			// - set the behavior to load all the bucket files
			prefix = null;
			delim = null;
		}

		// TODO smalers 2023-01-10 use the following to test combinations, comment out when done testing.
		//prefix = "state/co/dwr/";
		//delim = "/";

		// Indicates whether to process the two main representations for node data:
		// - CommonPrefix is is used with prefix and delimiter to list folders
		// - S3Object is used to list files
		boolean doCommonPrefix = true;
		boolean doS3Object = true;

       	software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = ListObjectsV2Request
    		.builder()
    		.fetchOwner(Boolean.TRUE)
    		.bucket(bucket);
    	if ( (prefix != null) && !prefix.isEmpty() ) {
    		// Set the key prefix to match.
    		builder.prefix(prefix);
    	}
       	if ( (delim != null) && !delim.isEmpty() ) {
       		// Add the requested delimiter to the request.
    		builder.delimiter(delim);
       	}
    	if ( maxKeys > 0 ) {
    		// Set the maximum number of keys that will be returned.
    		builder.maxKeys(maxKeys);
    	}
    	    	
    	ListObjectsV2Request request = builder.build();
    	ListObjectsV2Response response = null;
       	//int dateTimeBehaviorFlag = 0;
       	boolean done = false;
      	int objectCount = 0;
      	int commonPrefixCount = 0;
      	int maxObjects = 10000;

       	while ( !done ) {
       		response = s3.listObjectsV2(request);
       		if ( debug ) {
       			Message.printStatus(2, routine,
       				"S3 list response for bucket=" + bucket
   					+ " prefix=" + prefix
   					+ " delimiter=\"" + delim + "\""
   					+ " has " + response.contents().size() + " S3Objects and "
   					+ response.commonPrefixes().size() + " common prefixes"
       			);
       		}
   			if ( doLazyLoad ) {
   				// TODO smalers 2023-01-10 only load the top-level folders and files and wait for
   				// folders to be clicked on before loading them.
   				Message.printWarning(3, routine, "Lazy load of the tree is not implemented.");
   			}
   			else {
   				// Load the full tree up front.
   				if ( debug ) {
   					Message.printStatus(2, routine, "Doing a full load of the tree.");
   				}
   				if ( doCommonPrefix ) { // Keep this for now if need for testing.
   				for ( CommonPrefix commonPrefix : response.commonPrefixes() ) {
   					// Check the maximum object count, to protect against runaway processes.
   					if ( commonPrefixCount >= maxObjects ) {
   						// TODO smalers 2023-01-10 use continue during development to understand the response
   						//break;
   						continue;
   					}
   					// Transfer the AWS S3Object into an AwsS3_Object for use with the tree node.
   					AwsS3Object s3Object = new AwsS3Object(
   							bucket,
   							commonPrefix.prefix(),
   							null,
   							null,
   							null );
   					if ( debug ) {
   						Message.printStatus(2, routine, "Adding CommonPrefix node as folder: " + s3Object.getKey());
   					}
   					s3JTree.addFolder( parentNode, s3Object);
   					++commonPrefixCount;
   				}
   				}
   				if ( doS3Object ) { // Keep this for now if need for testing.
   				for ( S3Object serviceS3Object : response.contents() ) {
   					// Check the maximum object count, to protect against runaway processes.
   					if ( objectCount >= maxObjects ) {
   						// TODO smalers 2023-01-10 use continue during development to understand the response
   						//break;
   						continue;
   					}
   					// Transfer the AWS S3Object into an AwsS3_Object for use with the tree node.
   					AwsS3Object s3Object = new AwsS3Object(
   							bucket,
   							serviceS3Object.key(),
   							serviceS3Object.size(),
   							serviceS3Object.owner().displayName(),
   							serviceS3Object.lastModified() );
   					if ( serviceS3Object.key().endsWith("/") ) {
   						// Add a folder.
   						if ( debug ) {
   							Message.printStatus(2, routine, "Adding S3Object node as folder: " + s3Object.getKey());
   						}
   						s3JTree.addFolder( parentNode, s3Object);
   					}
   					else {
   						// Add a file.
   						if ( debug ) {
   							Message.printStatus(2, routine, "Adding S3Object node as file: " + s3Object.getKey());
   						}
   						s3JTree.addFile( parentNode, s3Object);
   					}
   					
   					++objectCount;
   				}
   				}
   			}
   			boolean doTest = false;
   			if ( doTest ) {
   			// TODO smalers 2023-01-10 this code worked to test solutions but implement the production code above.
   			if ( doCommonPrefix ) {
   				for ( CommonPrefix commonPrefix : response.commonPrefixes() ) {
   					// Check the maximum object count, to protect against runaway processes.
   					if ( commonPrefixCount >= maxObjects ) {
   						// TODO smalers 2023-01-10 use continue during development to understand the response
   						//break;
   						continue;
   					}
   					// Transfer the AWS S3Object into an AwsS3_Object for use with the tree node.
   					AwsS3Object s3Object = new AwsS3Object(
   							bucket,
   							commonPrefix.prefix(),
   							null,
   							null,
   							null );
   					if ( debug ) {
   						Message.printStatus(2, routine, "Adding CommonPrefix node: " + s3Object.getKey());
   					}
   					parentNode.add( new S3JTreeNode(s3Object, s3JTree));
   					++commonPrefixCount;
   				}
   			}
   			if ( doS3Object ) {
   				for ( S3Object serviceS3Object : response.contents() ) {
   					// Check the maximum object count, to protect against runaway processes.
   					if ( objectCount >= maxObjects ) {
   						// TODO smalers 2023-01-10 use continue during development to understand the response
   						//break;
   						continue;
   					}
   					// Transfer the AWS S3Object into an AwsS3_Object for use with the tree node.
   					AwsS3Object s3Object = new AwsS3Object(
   							bucket,
   							serviceS3Object.key(),
   							serviceS3Object.size(),
   							serviceS3Object.owner().displayName(),
   							serviceS3Object.lastModified() );
   					if ( debug ) {
   						Message.printStatus(2, routine, "Adding S3Object node: " + s3Object.getKey());
   					}
   					parentNode.add( new S3JTreeNode(s3Object, s3JTree));
   					++objectCount;
   				}
       		}
   			}
       		if ( response.nextContinuationToken() == null ) {
       			done = true;
       		}
       		request = request.toBuilder()
      				.continuationToken(response.nextContinuationToken())
      				.build();
       	}
       	if ( debug ) {
       		Message.printStatus(2, routine, "Total S3Object=" + objectCount + " total CommonPrefix=" + commonPrefixCount);
       	}
	}

	/**
	Refresh the S3 view based on the current settings,
	for example in response to selecting a new bucket.
	@problems problems that occur setting up the tree
	*/
	public void refresh( List<String> problems ) throws IOException {
		String routine = getClass().getSimpleName() + ".refresh";

		// Remove the children from the the root node.
        if ( Message.isDebugOn ) {
        	Message.printStatus(2,routine,"Removing all children from " + this.rootNode.getText());
        }
		this.rootNode.removeAllChildren();
		
    	// Root node matches the bucket:
    	// - do not set the tree because it is created below
        //this.rootNode = new S3JTreeNode(new AwsS3Object(this.bucket, AwsS3ObjectType.BUCKET), null);
        if ( Message.isDebugOn ) {
        	Message.printStatus(2,routine,"Setting root node 'name', 'text', and bucket to: " + this.bucket);
        }
		this.rootNode.getS3Object().setBucket(this.bucket);
		this.rootNode.setText(this.bucket);
		this.rootNode.setName(this.bucket);
		this.rootNode.refresh();
        
        // Create a tree with the root node:
        // - because the root node did not have the tree set, set it below
        //S3JTree s3JTree = new S3JTree(this.rootNode);
        //this.rootNode.setS3JTree(s3JTree);
        
        // Create the tree under the bucket:
        // - if lazy load will only populate the top level folders
        // - currently the full tree is populated (doLazyLoad=false)
        listObjects ( this.s3JTree, this.rootNode, "", this.doLazyLoad );
        // Redraw the tree.
        if ( Message.isDebugOn ) {
        	Message.printStatus(2,routine,"Refreshing the tree...");
        }
		// Expand the root node to show its immediate children.
        this.s3JTree.expandNode(this.rootNode);
        this.s3JTree.refresh();
        if ( Message.isDebugOn ) {
        	Message.printStatus(2,routine,"...done refreshing the tree.");
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
