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

import RTi.TS.TS;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJMenuItem;
import RTi.Util.GUI.SimpleJTree;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;

/**
JTree to use in the TimeSeriesTreeView.  This primarily uses SimpleJTree functionality, with some
overrides for popup menus.
*/
@SuppressWarnings("serial")
public class S3TreeView_JTree extends SimpleJTree implements ActionListener, ItemListener, MouseListener {
    
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
	public S3TreeView_JTree( SimpleJTree_Node root ) {
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

}