// S3TreeView_JTree - JTree to use in the S3TreeView

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

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

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

/**
JTree to use in the TimeSeriesTreeView.
This primarily uses SimpleJTree functionality, with some overrides for popup menus.
The parent SimpleJTree class methods should be used when manipulating the tree rather than
node methods so that the tree refreshes.
*/
@SuppressWarnings("serial")
public class S3JTree extends SimpleJTree implements ActionListener, ItemListener, MouseListener, TreeSelectionListener {
    
	/**
	Strings used in menus.
	*/
	private String MENU_Properties = "Properties";

	/**
	A single popup menu that is used to provide access to other features from the tree.
	The single menu has its items added/removed as necessary based on the state of the tree.
	*/
	private JPopupMenu popup_JPopupMenu = null;

	/**
	The node that last opened a popup menu.
	*/
	private S3JTreeNode popup_Node = null;
	
	/**
	 * Icon used for folders.
	 */
	private Icon folderIcon = null;
	
	/**
	 * Indicate whether checkbox should be shown for nodes:
	 * - TODO smalers 2023-01-13 for now do not show because it causes issues tracking selection and popup menu issues
	 */
	private boolean useCheckBox = false;
    
	/**
	Constructor.
	This creates a tree containing the provided root node.
	@param root the root node to use to initialize the tree.
	*/
	public S3JTree( S3JTreeNode root ) {
    	super(root);
    	// The following ensures that folders are closed folder.
    	this.folderIcon = getClosedIcon();     
    	showRootHandles(true);
    	setRootVisible(true);
    	addMouseListener(this);
    	setLeafIcon(null);
    	setTreeTextEditable(false);
    	// Create a popup menu that is shared between all tree nodes:
    	// - listeners for each menu item are added in 'showPopupMenu'
    	this.popup_JPopupMenu = new JPopupMenu();
    	// Add a listener for Tree selection events in case need for troubleshooting or functionality.
    	this.addTreeSelectionListener(this);
	}

	/**
	Responds to action performed events sent by popup menus of the tree nodes.
	@param event the ActionEvent that happened.
	*/
	public void actionPerformed(ActionEvent event) {
	    String action = event.getActionCommand();
    	//Object o = event.getSource();
    	String routine = getClass().getSimpleName() + ".actionPerformed";
	
    	if ( action.equals(this.MENU_Properties)) {
    		// Popup menu "Properties" has been selected.
    		showObjectProperties ();
    	}
	}

	/**
	 * Add a file node to the tree by parsing the object key.
	 * The key is of the form 'folder1/folder2/file'.
	 * This should not be called for folders (e.g., 'folder1/' or 'folder1/folder2/').
	 * @param parentNode the parent node under which to add the node
	 * @param s3Object the S3 object to add
	 */
   	public void addFile ( S3JTreeNode parentNode,  AwsS3Object s3Object ) {
   		String routine = getClass().getSimpleName() + ".addFile";
   		String key = s3Object.getKey();
   		boolean debug = false;
   		if ( key.endsWith("/") ) {
   			Message.printWarning(3, routine, "Adding a file and key does ends in /: " + key );
   		}
   		if ( StringUtil.patternCount(key, "/") > 0 ) {
   			// The key in s3Object contains multiple levels so split.
   			String [] keyParts = splitKey(s3Object.getKey());
   			String keyTotal = "";
   			for ( int ipart = 0; ipart < keyParts.length; ipart++ ) {
   				String keyPart = keyParts[ipart];
   				if ( ipart == (keyParts.length - 1) ) {
   					// Last part:
   					// - will be a file
					// - create a new AwsS3Object distinct from the full key, inheriting the original properties
 					keyTotal += keyPart;
					AwsS3Object newS3Object = new AwsS3Object(s3Object.getBucket(), keyTotal,
						s3Object.getSize(), s3Object.getOwner(), s3Object.getLastModified());
					// The following only uses the last part for the visible name.
					if ( debug ) {
						Message.printStatus(2, routine, "Adding file node with key=" + newS3Object.getKey());
					}
					S3JTreeNode newFileNode = new S3JTreeNode(newS3Object,this);
					parentNode.add(newFileNode);
   				}
   				else {
   					// Not the last part:
   					// - is a folder
   					// - find the matching top-level folder
   					S3JTreeNode folderNode = parentNode.findFolderMatchingText(keyPart);
   					// Folder keys always have a slash at the end.
					keyTotal += keyPart + "/";
   					if ( folderNode == null ) {
   						// Folder does not exist:
   						// - add it using a partial path for the key
   						// - create a new AwsS3Object distinct from the full key (folders don't have other properties)
   						AwsS3Object newS3Object = new AwsS3Object( s3Object.getBucket(), keyTotal);
   						// The following only uses the last part for the visible name.
   						S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
   						if ( debug ) {
							Message.printStatus(2, routine, "Adding sub-folder node with key=" + newS3Object.getKey());
						}
   						newFolderNode.setIcon(this.folderIcon);
   						parentNode.add(newFolderNode);
   						// The parent for additional adds is the new node.
   						parentNode = newFolderNode;
   					}
   					else {
   						// The folder node exists so no need to add.
   						if ( debug ) {
							Message.printStatus(2, routine, "Found sub-folder node with keyTotal=" + keyTotal + " keyPart=" + keyPart);
						}
   						parentNode = folderNode;
   					}
   				}
   			}
   		}
   		else {
   			// Single top-level file (e.g., "index.html"):
   			// - can use a the passed-in 's3Object' as is since the key is accurate
			if ( debug ) {
				Message.printStatus(2, routine, "Adding top-level file node with key=" + s3Object.getKey());
			}
			parentNode.add(new S3JTreeNode(s3Object, this));
   		}
   	}

	/**
	 * Add a folder node to the tree.
	 * @param parentNode the parent node under which to add the node
	 * @param s3Object the S3 object to add
	 */
   	public void addFolder ( S3JTreeNode parentNode, AwsS3Object s3Object ) {
   		String routine = getClass().getSimpleName() + ".addFolder";
   		String key = s3Object.getKey();
   		boolean debug = false;
   		if ( !key.endsWith("/") ) {
   			Message.printWarning(3, routine, "Adding a folder and key does not end in /: " + key );
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
					if ( debug ) {
						Message.printStatus(2, routine, "Adding last folder node with key=" + newS3Object.getKey());
					}
					S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
					newFolderNode.setIcon(this.folderIcon);
					parentNode.add(newFolderNode);
   				}
   				else {
   					// Not the last part:
   					// - is a folder
   					// - find the matching top-level folder
   					S3JTreeNode folderNode = parentNode.findFolderMatchingText(keyPart);
   					if ( folderNode == null ) {
   						// Folder does not exist:
   						// - add it using a partial path for the key
   						// - create a new AwsS3Object distinct from the full key (folders don't have other properties)
   						keyTotal += keyPart + "/";
   						AwsS3Object newS3Object = new AwsS3Object(s3Object.getBucket(), keyTotal);
   						S3JTreeNode newFolderNode = new S3JTreeNode(newS3Object,this);
   						if ( debug ) {
							Message.printStatus(2, routine, "Adding sub-folder node with key=" + newS3Object.getKey());
						}
   						newFolderNode.setIcon(this.folderIcon);
   						parentNode.add(newFolderNode);
   						// The parent for additional adds is the new node.
   						parentNode = newFolderNode;
   					}
   					else {
   						// The folder node exists so no need to add.
   						parentNode = folderNode;
   					}
   				}
   			}
   		}
   		else {
   			// Single top-level folder (e.g., "folder/"):
   			// - can use a the passed-in 's3Object' as is since the key is accurate
			if ( debug ) {
				Message.printStatus(2, routine, "Should not happen. Adding top-folder with no / key=" + s3Object.getKey());
			}
			S3JTreeNode newFolderNode = new S3JTreeNode(s3Object, this);
			newFolderNode.setIcon(this.folderIcon);
			parentNode.add(newFolderNode);
   		}
   	}
   	
   	/**
   	 * Check the UI state.  This ensures that buttons are enabled as appropriate, etc.
   	 */
	public void checkState() {
		// TODO smalers 2023-01-13 need to call back to the frame, which contains the buttons.
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
   	 * @return the selected nodes.
   	 * This method casts the SimpleJTree_Node to S3JTreeNode.
   	 */
   	public List<S3JTreeNode> getSelectedS3JTreeNodes () {
   		List<SimpleJTree_Node> superNodes = super.getSelectedNodes();
   		List<S3JTreeNode> selectedNodes = new ArrayList<>();
   		S3JTreeNode s3Node = null;
   		for ( SimpleJTree_Node node : superNodes ) {
   			s3Node = (S3JTreeNode)node;
   			selectedNodes.add(s3Node);
   		}
   		return selectedNodes;
   	}

   	/**
   	 * Return whether to use checkboxes.
   	 * @return true if checkboxes should be use, false if not
   	 */
   	public boolean getUseCheckBox () {
   		return this.useCheckBox;
   	}

	/**
	Handle ItemEvent events.
	@param e ItemEvent to handle.
	*/
	public void itemStateChanged ( ItemEvent e ) {
		// TOOD smalers 2023-01-10 need to handle events generated in a tree node, such as checkbox selection.
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
	 * Remove all the child nodes from a node.
	 */
	public void removeAll(S3JTreeNode node) {
		node.removeAllChildren();
	}
	
	/**
	 * Show the selected object's properties in a dialog.
	 */
	private void showObjectProperties () {
		String routine = getClass().getSimpleName() + ".showObjectProperties";

    	//Object data = __popup_Node.getData();
    	List<S3JTreeNode> selectedNodes = getSelectedS3JTreeNodes();
    	Message.printStatus(2, routine, "Showing properties for " + selectedNodes.size() + " selected nodes.");
    
       	try {
  			List<String> infoList = new ArrayList<>();
   			int objectCount = 0;
       		for ( S3JTreeNode node : selectedNodes ) {
       			++objectCount;
       			if ( objectCount > 1 ) {
       				infoList.add ( "" );
       				infoList.add ( "---------------------------------------------------------------------------" );
       			}
       			S3JTreeNode s3Node = (S3JTreeNode)node;
       			AwsS3Object s3Object = s3Node.getS3Object();
       			if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
       				infoList.add ( "Object is an S3 bucket." );
       			}
       			else if ( s3Object.getObjectType() == AwsS3ObjectType.FILE ) {
       				infoList.add ( "Object is an S3 file." );
       			}
       			else if ( s3Object.getObjectType() == AwsS3ObjectType.FOLDER ) {
       				infoList.add ( "Object is an S3 pseudo-folder (organizes objects but does not exist on S3)." );
       			}
       			infoList.add ( "S3 bucket:              " + s3Object.getBucket() );
       			if ( s3Object.getObjectType() != AwsS3ObjectType.BUCKET ) {
       				// Buckets don't use the key.
       				infoList.add ( "S3 object key:          " + s3Object.getKey() );
       				if ( s3Object.getObjectType() == AwsS3ObjectType.FOLDER ) {
       					infoList.add ( "S3 object without path: " + s3Node.getText() );
       				}
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
    	this.popup_Node = (S3JTreeNode)path.getLastPathComponent();
    	// First remove the menu items that are currently in the menu.
    	this.popup_JPopupMenu.removeAll();
    	// Now reset the popup menu based on the selected node:
    	// - this class is added as an  ActionListener for menu selections
    	// 
    	JMenuItem item = null;
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
   	 * Event handler for TreeSelectionListener.
   	 * @param e event to handle
   	 */
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

}