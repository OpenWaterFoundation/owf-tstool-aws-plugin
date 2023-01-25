// S3TreeView_JTree - JTree to use in the S3TreeView

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

package org.openwaterfoundation.tstool.plugin.aws.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3ObjectType;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.ReportJDialog;
import RTi.Util.GUI.SimpleJMenuItem;
import RTi.Util.GUI.SimpleJTree;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
JTree to use in the TimeSeriesTreeView.
This primarily uses SimpleJTree functionality, with some overrides for popup menus.
The parent SimpleJTree class methods should be used when manipulating the tree rather than
node methods so that the tree refreshes.
The root node contains the bucket name and children are folders in the bucket.
*/
@SuppressWarnings("serial")
public class S3JTree extends SimpleJTree
implements ActionListener,
//ItemListener,
MouseListener
//, TreeSelectionListenerx
{

	/**
	Strings used in menus.
	*/
	private String MENU_CopyKey = "Copy Key";
	private String MENU_Delee = "Delete";
	private String MENU_Rename = "Rename";
	private String MENU_Properties = "Properties";

	/**
	A single popup menu that is used to provide access to other features from the tree.
	The single menu has its items added/removed as necessary based on the state of the tree.
	*/
	private JPopupMenu popup_JPopupMenu = null;

	/**
	The node that last opened a popup menu.
	*/
	private SimpleJTree_Node popup_Node = null;

	/**
	 * Icon used for closed folders.
	 */
	private Icon closedFolderIcon = null;

	/**
	 * Icon used for open folders.
	 */
	private Icon openFolderIcon = null;

	/**
	 * Indicate whether checkbox should be shown for nodes:
	 * - TODO smalers 2023-01-13 for now do not show because it causes issues tracking selection and popup menu issues
	 */
	//private boolean useCheckBox = false;

	/**
 	* AWS session used to interact with AWS:
 	* - will be null until the profile is set, which will happen when refresh() is called once
 	*/
	private AwsSession awsSession = null;

	/**
	The S3 bucket.
	*/
	private String bucket = "";

	/**
	The S3 region.
	*/
	private String region = "";

	/**
	 * S3 client to use for S3 interactions.
	 */
	S3Client s3 = null;

	/**
	Constructor.
	Create an S3JTree.
	The tree will be empty so call the loadFromS3 function after the UI is initialized.
	@param awsSession the AwsSession that provides login credentials
	@param bucket the bucket that is currently selected in calling code
	@param region the region that is currently selected in calling code
	*/
	public S3JTree ( AwsSession awsSession, String bucket, String region ) {
		// Wait to initialize in the constructor so the root node can be created below.
    	super(true);
		this.awsSession = awsSession;
		this.bucket = bucket;
		// Create the root node and then initialize the tree.
		AwsS3Object s3Object = new AwsS3Object(bucket);
		S3JTreeNode rootNode = new S3JTreeNode(s3Object, this);
		initializeTree ( rootNode );

		// Setting the region causes the S3 client to be created.
		setRegion ( region );
    	this.openFolderIcon = getOpenIcon();
    	showRootHandles(true);
    	setRootVisible(true);
    	addMouseListener(this);
    	setTreeTextEditable(false);

    	// Create a popup menu that is shared between all tree nodes:
    	// - listeners for each menu item are added in 'showPopupMenu'
    	this.popup_JPopupMenu = new JPopupMenu();
    	// Add a listener for Tree selection events in case need for troubleshooting or functionality.
    	//this.addTreeSelectionListener(this);

    	setLeafIcon(null);
    	// The following ensures that folders are the closed folder.
    	// TODO smalers 2023-01-14 need to enable closed and open icons for folders.
    	this.closedFolderIcon = getClosedIcon();
	}

	/**
	Responds to action performed events sent by popup menus of the tree nodes.
	@param event the ActionEvent that happened.
	*/
	public void actionPerformed(ActionEvent event) {
	    String action = event.getActionCommand();
    	//Object o = event.getSource();
    	//String routine = getClass().getSimpleName() + ".actionPerformed";

    	if ( action.equals(this.MENU_Properties)) {
    		// Popup menu "Properties" has been selected.
    		uiAction_Popup_Properties ();
    	}
    	else if ( action.equals(this.MENU_CopyKey)) {
    		// Popup menu "Copy Key" has been selected.
    		uiAction_Popup_CopyKey ();
    	}
	}

	/**
	 * Add a file node to the tree by parsing the object key.
	 * The key in the S3JTreeNode is of the form 'folder1/folder2/file'.
	 * This should not be called for folders (e.g., 'folder1/' or 'folder1/folder2/').
	 * @param parentNode the parent node under which to add the node, if null the root node will be used
	 * @param s3Object the S3 object to add
	 */
   	public void addFile ( SimpleJTree_Node parentNode,  AwsS3Object s3Object ) throws Exception {
   		String routine = getClass().getSimpleName() + ".addFile";
   		String key = s3Object.getKey();
   		boolean debug = false;
   		if ( Message.isDebugOn ) {
   			debug = true;
   		}
   		// Indent so that messages align with 'loadFromS3' method messages.
   		String indent = "         ";
   		if ( key.endsWith("/") ) {
   			// This should not occur but put in a check to help ensure integrity of the tree.
   			Message.printStatus(2, routine, indent + "Adding a file and key ends in /: " + key );
   			Message.printStatus(2, routine, indent + "  Calling addFolder instead." );
   			addFolder ( parentNode, s3Object );
   			return;
   		}
   		boolean parentIsRoot = false;
   		if ( parentNode == null ) {
   			Message.printStatus(2, routine, indent + "Initial parent node is null so will add under root." );
   			// If the parentNode is null set to the root node.
   			if ( parentNode == null ) {
   				parentIsRoot = true;
   				parentNode = getRoot();
   			}
   		}
   		if ( StringUtil.patternCount(key, "/") > 0 ) {
   			// The key in s3Object contains at least one folder level (folder1/file.txt, folder1/folder2/file.txt, etc.) so split:
   			// - first parts will be leading folders
   			// - last part will be the file
   			String [] keyParts = splitKey(s3Object.getKey());
   			// keyTotal is the cumulative key path, used as intervening folders are added.
   			String keyTotal = "";
   			for ( int ipart = 0; ipart < keyParts.length; ipart++ ) {
   				String keyPart = keyParts[ipart];
   				if ( ipart == (keyParts.length - 1) ) {
   					// Last part:
   					// - will be a file
					// - create a new AwsS3Object distinct from the full key, inheriting the original properties
   					// - 'keyTotal' is the cumulative key from folders and files
 					keyTotal += keyPart;
					AwsS3Object newS3Object = new AwsS3Object(s3Object.getBucket(), keyTotal,
						s3Object.getSize(), s3Object.getOwner(), s3Object.getLastModified());
					// The following only uses the last part for the visible name.
					S3JTreeNode newFileNode = new S3JTreeNode(newS3Object,this);
					//parentNode.add(newFileNode);
					if ( parentIsRoot ) {
						// Adding to the root folder.
						addNode(newFileNode);
						if ( debug ) {
							Message.printStatus(2, routine, indent
								+ "Added file node with key=" + newS3Object.getKey()
								+ " parent=root"
								+ " name=" + newFileNode.getName()
								+ " text=" + newFileNode.getText());
						}
					}
					else {
						// Adding to a parent folder.
						addNode(newFileNode,parentNode);
						if ( debug ) {
							Message.printStatus(2, routine, indent
								+ "Added file node with key=" + newS3Object.getKey()
								+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
								+ " name=" + newFileNode.getName()
								+ " text=" + newFileNode.getText());
						}
					}
   				}
   				else {
   					// Not the last part:
   					// - is an intermediate folder
   					// - find the matching top-level folder
   					SimpleJTree_Node folderNode = findFolderMatchingText(parentNode, keyPart);
   					// Folder keys always have a slash at the end.
					keyTotal += keyPart + "/";
   					if ( folderNode == null ) {
   						// Folder does not exist:
   						// - add it using a partial path for the key
   						// - create a new AwsS3Object distinct from the full key (folders don't have other properties)
   						AwsS3Object newS3Object = new AwsS3Object( s3Object.getBucket(), keyTotal);
   						// The following only uses the last part for the visible name.
   						S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
   						newFolderNode.setIcon(this.closedFolderIcon);
   						//parentNode.add(newFolderNode);
   						if ( parentIsRoot ) {
   							// Adding to the root folder.
   							addNode(newFolderNode);
   							if ( debug ) {
								Message.printStatus(2, routine, indent
									+ "Added folder node with key=" + newS3Object.getKey()
									+ " parent=root"
									+ " name=" + newFolderNode.getName()
									+ " text=" + newFolderNode.getText());
							}
   						}
   						else {
   							// Adding to a parent folder.
   							addNode(newFolderNode, parentNode);
   							if ( debug ) {
								Message.printStatus(2, routine, indent
									+ "Added folder node with key=" + newS3Object.getKey()
									+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
									+ " name=" + newFolderNode.getName()
									+ " text=" + newFolderNode.getText());
							}
   						}
   						// The parent for additional adds is the new node.
   						parentNode = newFolderNode;
   						parentIsRoot = false;
   					}
   					else {
   						// The folder node exists:
   						// - no need to add
   						// - use it as the parent node
   						if ( debug ) {
							Message.printStatus(2, routine, indent + "Found sub-folder node with keyTotal=" + keyTotal + " keyPart=" + keyPart);
						}
   						parentNode = (S3JTreeNode)folderNode;
   						parentIsRoot = false;
   					}
   				}
   			}
   		}
   		else {
   			// Single top-level file (e.g., "index.html"):
   			// - can use a the passed-in 's3Object' as is since the key is accurate
			//parentNode.add(new S3JTreeNode(s3Object, this));
   			S3JTreeNode newFileNode = new S3JTreeNode(s3Object, this);
			if ( parentIsRoot ) {
				// Adding to the root folder.
				addNode(newFileNode);
				if ( debug ) {
					Message.printStatus(2, routine, indent
						+ "Added top-level file node with key=" + s3Object.getKey()
						+ " parent=root"
						+ " name=" + newFileNode.getName()
						+ " text=" + newFileNode.getText());
				}
			}
			else {
				// Adding to a parent folder.
				addNode(newFileNode, parentNode);
				if ( debug ) {
					Message.printStatus(2, routine, indent
						+ "Added top-level file node with key=" + s3Object.getKey()
						+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
						+ " name=" + newFileNode.getName()
						+ " text=" + newFileNode.getText());
				}
			}
   		}
   	}

	/**
	 * Add a folder node to the tree.
	 * @param parentNode the parent node under which to add the node, if null add to the root
	 * @param s3Object the S3 object to add
	 */
   	public void addFolder ( SimpleJTree_Node parentNode, AwsS3Object s3Object ) throws Exception {
   		String routine = getClass().getSimpleName() + ".addFolder";
   		String key = s3Object.getKey();
   		boolean debug = true;
   		if ( Message.isDebugOn ) {
   			debug = true;
   		}
   		// Indent so that messages align with 'loadFromS3' method messages.
   		String indent = "         ";
   		if ( !key.endsWith("/") ) {
   			// This should not occur but put in a check to help ensure integrity of the tree.
   			Message.printStatus(2, routine, indent + "Adding a folder and key DOES NOT end in /: " + key );
   			Message.printStatus(2, routine, indent + "  Calling addFile instead." );
   			addFile ( parentNode, s3Object );
   			return;
   		}
   		boolean parentIsRoot = false;
   		if ( parentNode == null ) {
   			Message.printStatus(2, routine, indent + "Initial parent node is null so will add under root." );
   			// If the parentNode is null set to the root node.
   			parentNode = getRoot();
   			parentIsRoot = true;
   		}
   		if ( StringUtil.patternCount(key, "/") > 1 ) {
   			// The key in s3Object contains multiple levels so split.
   			String [] keyParts = splitKey(s3Object.getKey());
   			String keyTotal = "";
   			for ( int ipart = 0; ipart < keyParts.length; ipart++ ) {
   				String keyPart = keyParts[ipart];
   				if ( ipart == (keyParts.length - 1) ) {
   					// Last part:
   					// - will be a folder
					// - create a new AwsS3Object distinct from the full key (folders don't have other properties)
					keyTotal += keyPart + "/";
					AwsS3Object newS3Object = new AwsS3Object(s3Object.getBucket(), keyTotal);
					S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
					newFolderNode.setIcon(this.closedFolderIcon);
					//parentNode.add(newFolderNode);
					if ( parentIsRoot ) {
						// Adding to the root folder.
						addNode(newFolderNode);
						if ( debug ) {
							Message.printStatus(2, routine, indent
								+ "Added file node with key=" + newS3Object.getKey()
								+ " parent=root"
								+ " name=" + newFolderNode.getName()
								+ " text=" + newFolderNode.getText());
						}
					}
					else {
						// Adding to a parent folder.
						addNode(newFolderNode, parentNode);
						if ( debug ) {
							Message.printStatus(2, routine, indent
								+ "Added file node with key=" + newS3Object.getKey()
								+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
								+ " name=" + newFolderNode.getName()
								+ " text=" + newFolderNode.getText());
						}
					}
   				}
   				else {
   					// Not the last part:
   					// - is a folder
   					// - find the matching top-level folder
   					//S3JTreeNode folderNode = parentNode.findFolderMatchingText(keyPart);
   					//parentNode = (S3JTreeNode)getRoot();
   					SimpleJTree_Node folderNode = findFolderMatchingText(parentNode, keyPart);
   					if ( folderNode == null ) {
   						// Folder does not exist:
   						// - add it using a partial path for the key
   						// - create a new AwsS3Object distinct from the full key (folders don't have other properties)
   						keyTotal += keyPart + "/";
   						AwsS3Object newS3Object = new AwsS3Object(s3Object.getBucket(), keyTotal);
   						S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
   						newFolderNode.setIcon(this.closedFolderIcon);
   						//parentNode.add(newFolderNode);
   						if ( parentIsRoot ) {
   							// Adding to the root folder.
   							addNode(newFolderNode);
   							if ( debug ) {
								Message.printStatus(2, routine, indent
									+ "Added folder node with key=" + newS3Object.getKey()
									+ " parent=root"
									+ " name=" + newFolderNode.getName()
									+ " text=" + newFolderNode.getText());
							}
   						}
   						else {
   							// Adding to a parent folder.
   							addNode(newFolderNode, parentNode);
   							if ( debug ) {
								Message.printStatus(2, routine, indent
									+ "Added folder node with key=" + newS3Object.getKey()
									+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
									+ " name=" + newFolderNode.getName()
									+ " text=" + newFolderNode.getText());
							}
   						}
   						// The parent for additional adds is the new node.
   						parentNode = newFolderNode;
   						parentIsRoot = false;
   					}
   					else {
   						// The folder node exists so no need to add.
   						parentNode = (S3JTreeNode)folderNode;
   						parentIsRoot = false;
   					}
   				}
   			}
   		}
   		else {
   			// Single top-level folder (e.g., "folder/"):
   			// - can use a the passed-in 's3Object' as is since the key is accurate

			S3JTreeNode newFolderNode = new S3JTreeNode(s3Object, this);
			newFolderNode.setIcon(this.closedFolderIcon);
			//parentNode.add(newFolderNode);
			if ( parentIsRoot ) {
				// Adding to the root folder.
				addNode(newFolderNode);
				if ( debug ) {
					Message.printStatus(2, routine, indent
						+ "Added top-level folder with key=" + s3Object.getKey()
						+ " parent=root"
						+ " name=" + newFolderNode.getName()
						+ " text=" + newFolderNode.getText());
				}
			}
			else {
				// Adding to a parent folder.
				addNode(newFolderNode, parentNode);
				if ( debug ) {
					Message.printStatus(2, routine, indent
						+ "Added top-level folder with key=" + s3Object.getKey()
						+ " parentKey=" + ((AwsS3Object)parentNode.getData()).getKey()
						+ " name=" + newFolderNode.getName()
						+ " text=" + newFolderNode.getText());
				}
			}
   		}
   	}

   	/**
   	 * Check the UI state.  This ensures that buttons are enabled as appropriate, etc.
   	 */
	public void checkState() {
		// TODO smalers 2023-01-13 need to call back to the frame, which contains the buttons.
	}

	/**
	Clear all data from the tree.
	*/
	public void clear() {
		String routine = getClass().getSimpleName() + ".clear";
		SimpleJTree_Node rootNode = getRoot();
		List<SimpleJTree_Node> nodeList = getChildrenList(rootNode);
		if ( nodeList != null ) {
			for ( SimpleJTree_Node node : nodeList ) {
				try {
					removeNode(node, false);
				}
				catch (Exception e) {
					Message.printWarning(2, routine, "Error removing children of root node: " + rootNode );
					Message.printWarning(2, routine, e);
				}
			}
		}
	}

	/**
	 * Create the S3 client to use for interactions.
	 */
	private void createS3Client () {
		String routine = getClass().getSimpleName() + ".createS3Client";
		// Create the s3 client that will be used to interact with S3.
		try {
			Region regionO = Region.of(this.region);

			this.s3 = S3Client.builder()
				.region(regionO)
				.credentialsProvider(awsSession.getProfileCredentialsProvider())
				.build();
		}
		catch ( Exception e ) {
			Message.printWarning(1, routine, "Error creating S3 client." );
			Message.printWarning(1, routine, e );
		}
	}

   	/**
   	 * Return the shared popup JMenu.
   	 * This can be used, for example, when constructing nodes that enable the popup menu.
   	 * @return the shared popup JMenu.
   	 */
   	public JPopupMenu getPopupJMenu () {
   		return this.popup_JPopupMenu;
   	}

   	/**
   	 * Return the selected nodes.
   	 * The order is that of the selection (not sorted by network order).
   	 * @param sortKeys sort the selected nodes by the S3 keys (true) or return in the order from the parent tree (false)
   	 * @return the selected nodes.
   	 * This method casts the SimpleJTree_Node to S3JTreeNode.
   	 */
   	public List<S3JTreeNode> getSelectedS3JTreeNodes ( boolean sortKeys ) {
   		String routine = getClass().getSimpleName() + ".getSelectedS3JTreeNodes";
   		List<SimpleJTree_Node> superNodes = super.getSelectedNodes();
   		List<S3JTreeNode> selectedNodes = new ArrayList<>();
   		S3JTreeNode s3Node = null;
   		Object data = null;
   		AwsS3Object s3Object = null;
   		if ( sortKeys ) {
   			// Create a list of strings to sort.
   			List<String> keys = new ArrayList<>();
   			for ( SimpleJTree_Node node : superNodes ) {
   				if ( node instanceof S3JTreeNode ) {
   					s3Node = (S3JTreeNode)node;
   					data = s3Node.getData();
   					if ( data != null ) {
   						s3Object = (AwsS3Object)data;
   						keys.add(s3Object.getKey());
   					}
   				}
   				else {
   					// Unknown type:
   					// - should never happen
   					// - must add to preserve the overall index position
   					// - add at the end
   					keys.add("zzzz");
   				}
   			}
   			// Sort the keys.
   			boolean ignoreCase = false;
   			int [] sortOrder = new int[keys.size()];
   			StringUtil.sortStringList(keys, StringUtil.SORT_ASCENDING, sortOrder, true, ignoreCase);
   			// Add the selected nodes in the key-sorted order:
   			// - only add S3JTreeNode
   			for ( int sortPos : sortOrder ) {
   				if ( superNodes.get(sortPos) instanceof S3JTreeNode ) {
   					selectedNodes.add((S3JTreeNode)superNodes.get(sortPos));
   				}
   			}
   		}
   		else {
   			// Return the selected nodes
   			for ( SimpleJTree_Node node : superNodes ) {
   				if ( node instanceof S3JTreeNode ) {
   					// Put this first since more specific.
   					//Message.printStatus(2, routine, "Node is a S3JTreeNode, name=" + node.getName() + ", text=" + node.getText() );
   					s3Node = (S3JTreeNode)node;
   					selectedNodes.add(s3Node);
   				}
   				else if ( node instanceof SimpleJTree_Node ) {
   					// This should not happen given that the current design has been tested.
   					Message.printStatus(2, routine, "Node is a SimpleJTree_Node, name=" + node.getName() + ", text=" + node.getText() );
   				}
   				else {
   					Message.printStatus(2, routine, "Unhandled node type for: " + node );
   				}
   			}
   		}
   		return selectedNodes;
   	}

	/**
	 * List objects and add to a folder.
	 * @param parentNode the parent tree node under which to list files and folders
	 * @param prefix the prefix used to query the results
	 * @param doLazyLoad if true do a lazy load when folders are expanded, if false load the whole tree up front
	 */
	public void loadFromS3 ( String prefix, boolean doLazyLoad ) throws Exception {
		String routine = getClass().getSimpleName() + ".loadFromS3";
		int maxKeys = -1;
		String delim = "/";

		// Enclose the entire function in a try so that fast add is turned off if a major failure.
		try {
		// Set 'debug' to true to troubleshoot.
		boolean debug = false;
		if ( Message.isDebugOn ) {
			debug = true;
		}

		if ( debug ) {
			Message.printStatus(2, routine, "Listing S3 objects into the tree.");
			Message.printStatus(2, routine, "  bucket=" + this.bucket);
			Message.printStatus(2, routine, "  region=" + this.region);
			Message.printStatus(2, routine, "  Setting tree to fastAdd=true.");
		}
		// Turn on fast add.
		setFastAdd(true);

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
    		.bucket(this.bucket);
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

		if ( debug ) {
			Message.printStatus(2, routine, "  Getting the bucket listing from S3.");
		}
       	while ( !done ) {
       		response = this.s3.listObjectsV2(request);
       		if ( debug ) {
       			Message.printStatus(2, routine,
       				"  S3 list response for bucket=" + this.bucket
   					+ " prefix=" + prefix
   					+ " delimiter=\"" + delim + "\""
   					+ " has " + response.contents().size() + " S3Objects and "
   					+ response.commonPrefixes().size() + " common prefixes"
       			);
       		}
   			if ( doLazyLoad ) {
   				// TODO smalers 2023-01-10 only load the top-level folders and files and wait for
   				// folders to be clicked on before loading them.
   				Message.printWarning(3, routine, "  Lazy load of the tree is not implemented.");
   			}
   			else {
   				// Load the full tree up front.
   				if ( debug ) {
   					Message.printStatus(2, routine, "  Doing a full load of the tree.");
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
   							this.bucket,
   							commonPrefix.prefix(),
   							null,
   							null,
   							null );
   					if ( debug ) {
   						Message.printStatus(2, routine, "    Adding CommonPrefix node as folder: " + s3Object.getKey());
   					}
   					try {
   						addFolder( null, s3Object);
   						++commonPrefixCount;
   					}
   					catch ( Exception e ) {
   						Message.printWarning(2, routine, e);
   					}
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
   							this.bucket,
   							serviceS3Object.key(),
   							serviceS3Object.size(),
   							serviceS3Object.owner().displayName(),
   							serviceS3Object.lastModified() );
   					if ( serviceS3Object.key().endsWith("/") ) {
   						// Add a folder.
   						if ( debug ) {
   							Message.printStatus(2, routine, "    Adding S3Object node as folder: " + s3Object.getKey());
   						}
   						try {
   							addFolder( null, s3Object);
   							++objectCount;
   						}
   						catch ( Exception e ) {
   							Message.printWarning(2, routine, e);
   						}
   					}
   					else {
   						// Add a file.
   						if ( debug ) {
   							Message.printStatus(2, routine, "    Adding S3Object node as file: " + s3Object.getKey());
   						}
   						try {
   							addFile( null, s3Object);
   							++objectCount;
   						}
   						catch ( Exception e ) {
   							Message.printWarning(2, routine, e);
   						}
   					}
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
   							this.bucket,
   							commonPrefix.prefix(),
   							null,
   							null,
   							null );
   					if ( debug ) {
   						Message.printStatus(2, routine, "    Adding CommonPrefix node: " + s3Object.getKey());
   					}
   					addNode( new S3JTreeNode(s3Object, this));
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
   							this.bucket,
   							serviceS3Object.key(),
   							serviceS3Object.size(),
   							serviceS3Object.owner().displayName(),
   							serviceS3Object.lastModified() );
   					if ( debug ) {
   						Message.printStatus(2, routine, "    Adding S3Object node: " + s3Object.getKey());
   					}
   					addNode( new S3JTreeNode(s3Object, this));
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
       		Message.printStatus(2, routine, "  Total S3Object=" + objectCount + " total CommonPrefix=" + commonPrefixCount);
       		// The following shows visible rows.
        	//Message.printStatus(2, routine, "  Tree has " + this.s3JTree.getRowCount() + " rows.");
       		Message.printStatus(2, routine, "  Tree dump:");
       		dumpTree();
       	}
       	// Turn off fast add so that interactive editing such as deleting nodes will be fast.
		}
       	finally {
       		if ( Message.isDebugOn ) {
       			Message.printStatus(2, routine, "  Setting tree to fastAdd=false.");
       		}
       		setFastAdd(false);
       	}
	}

	/**
	Responds to mouse clicked events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseClicked(MouseEvent event) {
	}

	/**
	Responds to mouse entered events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseEntered(MouseEvent event) {
	}

	/**
	Responds to mouse exited events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseExited(MouseEvent event) {
	}

	/**
	Responds to mouse pressed events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mousePressed(MouseEvent event) {
	}

	/**
	Responds to mouse released events and possibly shows a popup menu.
	@param event the MouseEvent that happened.
	*/
	public void mouseReleased(MouseEvent event) {
    	showPopupMenu(event);
	}

    /**
     * Set the root node properties.
     * This should be called when refreshing the tree.
     * The tree bucket must have been set first.
     */
    public void refreshRootNodeProperties() {
    	S3JTreeNode rootNode = (S3JTreeNode)getRoot();
    	rootNode.setName(this.bucket);
    	rootNode.setText(this.bucket);
    	AwsS3Object s3Object = (AwsS3Object)rootNode.getData();
    	s3Object.setBucket(this.bucket);
    	// Also set the user object to a string:
    	// - otherwise the original data continues to be used
    	// - see:  https://stackoverflow.com/questions/16013097/change-the-jtree-node-text-runtime
    	rootNode.setUserObject(this.bucket);
    }

	/**
	 * Set the AWS session for the tree.
	 */
	public void setAwsSession ( AwsSession awsSession ) {
		this.awsSession = awsSession;
	}

	/**
	 * Set the bucket for the tree.
	 */
	public void setBucket ( String bucket ) {
		this.bucket = bucket;
	}

	/**
	 * Set the region for the tree.
	 * This results in a new S3 client being initialized.
	 */
	public void setRegion ( String region ) {
		this.region = region;

		// Set up the client, which depends on the region.
		createS3Client ();
	}

	/**
	 * Set the S3 client.
	 */
	public void setS3Client ( S3Client s3 ) {
		this.s3 = s3;
	}

	/**
	Checks to see if the mouse event would trigger display of the popup menu.
	The popup menu does not display if it is null.
	@param e the MouseEvent that happened.
	*/
	private void showPopupMenu(MouseEvent e) {
	    String routine = getClass().getName() + ".showPopupMenu";
	    try {
    	if ( !e.isPopupTrigger() ) {
	    	Message.printStatus(2, routine, "Not a popupTrigger.");
        	// Do not do anything.
        	return;
    	}
    	TreePath path = getPathForLocation(e.getX(), e.getY());
    	if (path == null) {
	    	Message.printStatus(2, routine, "TreePath is null for " + e.getX() + ", " + e.getY() );
        	return;
    	}
    	// The node that last resulted in the popup menu
    	this.popup_Node = (SimpleJTree_Node)path.getLastPathComponent();
    	// First remove the menu items that are currently in the menu.
    	this.popup_JPopupMenu.removeAll();
    	// Now reset the popup menu based on the selected node:
    	// - this class is added as an  ActionListener for menu selections
    	//
    	JMenuItem item = null;
        item = new SimpleJMenuItem ( this.MENU_CopyKey, this );
        this.popup_JPopupMenu.add ( item );
        item = new SimpleJMenuItem ( this.MENU_Properties, this );
        this.popup_JPopupMenu.add ( item );
    	// Now display the popup so that the user can select the appropriate menu item.
    	Point pt = JGUIUtil.computeOptimalPosition ( e.getPoint(), e.getComponent(), this.popup_JPopupMenu );
	   	Message.printStatus(2, routine, "Showing popup menu at " + pt.x + ", " + pt.y );
    	popup_JPopupMenu.show(e.getComponent(), pt.x, pt.y);
	    }
	    catch ( Exception ex ) {
	    	Message.printWarning(2, routine, "Error showing popup menu.");
	    	Message.printWarning(2, routine, ex);
	    }
	}

	/**
	 * Split the key into parts.
	 * @param key the key to split, for example "top/second/third/" for a folder and
	 * "top/second/third/file.ext" for a file.
	 * @return the kay path split into its parts
	 */
   	private String [] splitKey ( String key ) {
   		String [] parts = key.split("/");
   		return parts;
   	}

	/**
	 * Copy the first selected object key to operating system clipboard.
	 */
	private void uiAction_Popup_CopyKey () {
    	List<S3JTreeNode> selectedNodes = getSelectedS3JTreeNodes(true);
    	if ( selectedNodes.size() > 0 ) {
    		S3JTreeNode node = selectedNodes.get(0);
    		Object o = node.getData();
    		if ( o != null ) {
    			AwsS3Object s3Object = (AwsS3Object)o;
    			String key = s3Object.getKey();
    			StringSelection stringSelection = new StringSelection(key);
    			// Set the string in the clipboard.
    			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    			ClipboardOwner owner = null;
    			clipboard.setContents(stringSelection, owner);
    		}
    	}
	}

	/**
	 * Show selected objects' properties in a dialog.
	 */
	private void uiAction_Popup_Properties () {
		String routine = getClass().getSimpleName() + ".showObjectProperties";

    	//Object data = __popup_Node.getData();
		// Get the selected nodes:
		// - TODO smalers 2023-01-20 the order is that of the selection, not the tree order
    	Message.printStatus(2, routine, "Showing properties for selected nodes.");
    	List<S3JTreeNode> selectedNodes = getSelectedS3JTreeNodes(true);
    	Message.printStatus(2, routine, "Showing properties for " + selectedNodes.size() + " selected nodes.");

       	String div = "---------------------------------------------------------------------------";
       	try {
  			List<String> infoList = new ArrayList<>();
  			if ( selectedNodes.size() > 1 ) {
  				infoList.add ( div );
  				infoList.add ( "Objects are sorted by key, which should match the tree top to bottom." );
  				infoList.add ( div );
  				infoList.add ( "" );
  			}
   			int objectCount = 0;
       		for ( S3JTreeNode node : selectedNodes ) {
       			++objectCount;
       			if ( objectCount > 1 ) {
       				infoList.add ( "" );
       				infoList.add ( div );
       			}
       			if ( node == null ) {
       				Message.printWarning(3,routine,"Selected node is null.");
       			}
       			S3JTreeNode s3Node = node;
       			AwsS3Object s3Object = s3Node.getS3Object();
       			if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
       				infoList.add ( "Object is an S3 bucket." );
       			}
       			else if ( s3Object.getObjectType() == AwsS3ObjectType.FILE ) {
       				infoList.add ( "Object is an S3 file (is an object on S3)." );
       			}
       			else if ( s3Object.getObjectType() == AwsS3ObjectType.FOLDER ) {
       				infoList.add ( "Object is an S3 pseudo-folder (organizes objects but is not an object on S3)." );
       			}
       			infoList.add ( "S3 bucket:              " + s3Object.getBucket() );
       			if ( s3Object.getObjectType() != AwsS3ObjectType.BUCKET ) {
       				// Buckets don't use the key.
       			    infoList.add ( "Label for tree:         " + s3Node.getText() );
       				infoList.add ( "S3 object key:          " + s3Object.getKey() );
       			}
       			if ( s3Object.getObjectType() == AwsS3ObjectType.FILE ) {
       				// Only files have additional properties in S3.
       				infoList.add ( "S3 File size (bytes):   " + s3Object.getSize() );
   					infoList.add ( "S3 File owner:          " + s3Object.getOwner() );
   					infoList.add ( "S3 File last modified:  " + s3Object.getLastModified() );
       			}
       		}
           	PropList props = new PropList("CellContents");
	        props.set("Title=S3 Object Properties");
	        Component parent = SwingUtilities.getWindowAncestor(this);
	        boolean modal = false;
	        if ( parent instanceof JFrame ) {
	        	new ReportJDialog((JFrame)parent, infoList, props, modal);
	        }
	        else if ( parent instanceof JFrame ) {
	        	new ReportJDialog((JFrame)parent, infoList, props, modal);
	        }
       	}
       	catch ( Exception e ) {
           	Message.printWarning(1, routine, "Unable to show node properties (" + e + ")." );
           	Message.printWarning(3, routine, e);
       	}
	}


   	/**
   	 * Event handler for TreeSelectionListener.
   	 * @param e event to handle
   	 */
   	/*
   	public void valueChanged ( TreeSelectionEvent e ) {
   		String routine = getClass().getSimpleName() + ".valueChanged";
   		Object o = e.getSource();
   		if ( o instanceof S3JTreeNode ) {
   			S3JTreeNode node = (S3JTreeNode)o;
   			Object lastSelected = this.getLastSelectedPathComponent();
   			Message.printStatus(2, routine, "Selected node (S3 node) = " + lastSelected + " selection count = "
   				+ getSelectionCount() );
   		}
   		else {
   			Object lastSelected = this.getLastSelectedPathComponent();
   			Message.printStatus(2, routine, "Last selected node (NOT S3 node) = " + lastSelected + " selection count = "
   				+ getSelectionCount() );
   		}
   	}
   	*/

}