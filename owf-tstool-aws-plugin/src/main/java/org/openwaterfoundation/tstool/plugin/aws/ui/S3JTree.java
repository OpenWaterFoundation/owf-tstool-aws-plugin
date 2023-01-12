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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;

import RTi.TS.TS;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJMenuItem;
import RTi.Util.GUI.SimpleJTree;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
JTree to use in the TimeSeriesTreeView.
This primarily uses SimpleJTree functionality, with some overrides for popup menus.
The parent SimpleJTree class methods should be used when manipulating the tree rather than
node methods so that the tree refreshes.
*/
@SuppressWarnings("serial")
public class S3JTree extends SimpleJTree implements ActionListener, ItemListener, MouseListener {
    
	/**
	Strings used in menus.
	*/
	private String MENU_Graph_Line = "Graph - Line";
	private String MENU_Graph_Product = "View Time Series Product";

	/**
	A single popup menu that is used to provide access to other features from the tree.
	The single menu has its items added/removed as necessary based on the state of the tree.
	*/
	private JPopupMenu popup_JPopupMenu;

	/**
	The node that last opened a popup menu.
	*/
	private SimpleJTree_Node popup_Node;
    
	/**
	Constructor.
	This creates a tree containing the provided root node.
	@param root the root node to use to initialize the tree.
	*/
	public S3JTree( SimpleJTree_Node root ) {
    	super(root);
    	//__folderIcon = getClosedIcon();     
    	showRootHandles(true);
    	setRootVisible(true);
    	addMouseListener(this);
    	setLeafIcon(null);
    	setTreeTextEditable(false);
    	popup_JPopupMenu = new JPopupMenu();
	}

	/**
	Responds to action performed events sent by popup menus of the tree nodes.
	@param event the ActionEvent that happened.
	*/
	public void actionPerformed(ActionEvent event) {
	    String action = event.getActionCommand();
    	//Object o = event.getSource();
    	String routine = getClass().getName() + ".actionPerformed";
	
    	//Object data = __popup_Node.getData();
    	List<SimpleJTree_Node> selectedNodes = getSelectedNodes();
    
    	/*
    	if ( action.equals(__MENU_Graph_Line)) {
        	List<TS> tslist = new Vector<TS>();
        	for ( SimpleJTree_Node node : selectedNodes ) {
            	Object data = node.getData();
            	if ( data instanceof TS ) {
            		tslist.add ( (TS)data );
            	}
        	}
        	PropList graphprops = new PropList ( "GraphProperties");
        	// For now always use new graph...
        	graphprops.set ( "InitialView", "Graph" );
        	// Summary properties for secondary displays (copy from summary output)...
        	//graphprops.set ( "HelpKey", "TSTool.ExportMenu" );
        	graphprops.set ( "TotalWidth", "600" );
        	graphprops.set ( "TotalHeight", "400" );
        	//graphprops.set ( "Title", "Summary" );
        	graphprops.set ( "DisplayFont", "Courier" );
        	graphprops.set ( "DisplaySize", "11" );
        	graphprops.set ( "PrintFont", "Courier" );
        	graphprops.set ( "PrintSize", "7" );
        	graphprops.set ( "PageLength", "100" );
        	try {
            	new TSViewJFrame ( tslist, graphprops );
        	}
        	catch ( Exception e ) {
            	Message.printWarning(1, routine, "Unable to graph data (" + e + ")." );
            	Message.printWarning(3, routine, e);
        	}
    	}
    	else if ( action.equals(__MENU_Graph_Product) ) {
        	for ( Object o : selectedNodes ) {
            	// TODO SAM Don't like all the casting but the low-level code deals with generic objects
            	if ( o instanceof SimpleJTree_Node ) {
                	SimpleJTree_Node node = (SimpleJTree_Node)o;
                	Object data = node.getData();
                	if ( data instanceof String ) {
                		// Name of TSP file
                		processProduct((String)data);
                	}
            	}
        	}
    	}
    	*/
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
					S3JTreeNode newNode = new S3JTreeNode(newS3Object,this);
					parentNode.add(newNode);
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
			parentNode.add(new S3JTreeNode(s3Object, this));
   		}
   	}

	/**
	Handle ItemEvent events.
	@param e ItemEvent to handle.
	*/
	public void itemStateChanged ( ItemEvent e ) {
		// TOOD smalers 2023-01-10 need to handle events generated in a tree node, such as selection.
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
	Checks to see if the mouse event would trigger display of the popup menu.
	The popup menu does not display if it is null.
	@param e the MouseEvent that happened.
	*/
	private void showPopupMenu(MouseEvent e) {
	    String routine = getClass().getName() + ".showPopupMenu";
    	if ( !e.isPopupTrigger() ) {
        	// Do not do anything.
        	return;
    	}
    	TreePath path = getPathForLocation(e.getX(), e.getY()); 
    	if (path == null) {
        	return;
    	}
    	// The node that last resulted in the popup menu
    	this.popup_Node = (SimpleJTree_Node)path.getLastPathComponent();
    	// First remove the menu items that are currently in the menu.
    	this.popup_JPopupMenu.removeAll();
    	Object data = null;     // Data object associated with the node.
    	// Now reset the popup menu based on the selected node.
    	// Get the data for the node.
    	// If the node is a data object, the type can be checked to know what to display.
    	// The tree is displaying data objects so the popup will show specific JFrames for each data group.
    	// If the group folder was selected, then display the JFrame showing the first item selected.
    	// If a specific data item in the group was selected, then show the specific data item.
    	JMenuItem item;
    	data = this.popup_Node.getData();
    	boolean typeOk = false;
    	if ( data instanceof TS ) {
        	// Time series object(s) are selected...
        	item = new SimpleJMenuItem ( this.MENU_Graph_Line, this );
        	this.popup_JPopupMenu.add ( item );
        	typeOk = true;
    	}
    	if ( !typeOk ) {
        	item = new SimpleJMenuItem ( "Unknown data", this );
        	this.popup_JPopupMenu.add ( item );
        	Message.printWarning ( 3, routine, "Tree data type is not recognized for popup menu." );
        	return;
    	}
    	// Now display the popup so that the user can select the appropriate menu item...
    	Point pt = JGUIUtil.computeOptimalPosition ( e.getPoint(), e.getComponent(), popup_JPopupMenu );
    	popup_JPopupMenu.show(e.getComponent(), pt.x, pt.y);
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

}