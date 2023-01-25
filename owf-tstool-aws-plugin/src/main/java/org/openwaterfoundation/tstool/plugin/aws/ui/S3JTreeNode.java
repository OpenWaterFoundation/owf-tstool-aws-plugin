// S3JTree_Node - node class corresponding to S3JTree tree

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

import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JPopupMenu;

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3ObjectType;

import RTi.Util.GUI.SimpleJTree_Node;

/**
This class is a convenience class for displaying CheckBox and label information for S3 objects as tree nodes.
This code was copied from RTi.GIS.GeoViewLegend.GeoViewLegendJTree_Node and modified.
These nodes contain two components, a JCheckBox (with no text) and a separate JLabel.
The 'test' data member in the 'SimpleJTree_Node' is used for the label.
The 'name' data member in the 'SimpleJTree_Node' is passed to the parent.
Other data are stored in the 's3Object', such as S3 object key and other properties.
*/
@SuppressWarnings("serial")
public class S3JTreeNode
extends SimpleJTree_Node
implements
//FocusListener,
MouseListener
//, ItemListener, ItemSelectable
{

	/**
	Whether this node has been selected (i.e., the label has been clicked on) or not.
	*/
	private boolean selected = false;

	/**
	Reference to the S3 tree in which this component appears.
	*/
	private S3JTree s3Tree;

	/**
	The popup menu associated with this node.
	*/
	private JPopupMenu popup_JMenu = null;

	/**
	The listeners that are registered to listen for this objects item state changed events.
	*/
	private List<ItemListener> itemListeners = null;

	/**
	Constructor.
	@param s3Object S3 object associated with the node
	@param tree the tree in which this component appears
	*/
	public S3JTreeNode(AwsS3Object s3Object, S3JTree tree) {
		// Pass a new panel to indicate that a component will be displayed rather than the default tree node text label:
		// - the component is reset in the initialize() method
		// - currently DO NOT use a custom panel with checkbox because it was causing icon to not display
		//   and selection events did not behave
		//super(new JPanel(), (s3Object.getObjectType() == AwsS3ObjectType.BUCKET ? s3Object.getBucket() : s3Object.getKey()) );
		super( (s3Object.getObjectType() == AwsS3ObjectType.BUCKET ? s3Object.getBucket() : s3Object.getKey()) );
		// Make sure the visible text is only the last part of the key (stored in node base class and not AwsS3Object).
		if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
			super.setText(s3Object.getBucket());
			super.setUserObject(s3Object.getBucket());
		}
		else {
			// File or folder node type.
			// Set the text to the key name without the leading path.
			super.setText(getNameForKey(s3Object.getKey()));
			super.setUserObject(getNameForKey(s3Object.getKey()));
		}
		// Used the shared popup menu.
		if ( tree == null ) {
			// Typically the case when creating the root node and the shared popup menu has not been created:
			// - have to call setPopupMenu() on the root node after the tree is created
			initialize(s3Object, tree, null );
		}
		else {
			// Typically the case when creating other than the root node.
			initialize(s3Object, tree, tree.getPopupJMenu() );
		}
	}

	/**
	Constructor.
	@param s3Object S3 object associated with the node
	@param tree the tree in which this component appears
	@param popupMenu the popupMenu that this node should display.
	*/
	public S3JTreeNode(AwsS3Object s3Object, S3JTree tree, JPopupMenu popupMenu) {
		// Pass a new panel to indicate that a component will be displayed rather than the default tree node text label:
		// - the component is reset in the initialize() method
		// - currently DO NOT use a custom panel with checkbox because it was causing icon to not display
		//   and selection events did not behave
		//super(new JPanel(), (s3Object.getObjectType() == AwsS3ObjectType.BUCKET ? s3Object.getBucket() : s3Object.getKey()) );
		super((s3Object.getObjectType() == AwsS3ObjectType.BUCKET ? s3Object.getBucket() : s3Object.getKey()) );
		// Make sure the visible text is only the last part of the key (stored in node base class and not AwsS3Object).
		if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
			super.setText(s3Object.getBucket());
			super.setUserObject(s3Object.getBucket());
		}
		else {
			super.setText(getNameForKey(s3Object.getKey()));
			super.setUserObject(getNameForKey(s3Object.getKey()));
		}
		initialize(s3Object, tree, popupMenu);
	}

	/**
	Registers an item listener for this component.
	@param listener the listener to add to the list of listeners.
	*/
	public void addItemListener(ItemListener listener) {
		this.itemListeners.add(listener);
	}

	/**
	 * Return the name for the key, which is the last part of a key path, without trailing /.
	 */
	 private String getNameForKey(String key) {
		 if ( key.indexOf("/") >= 0 ) {
			 // Have delimiter(s) in the path so split and return the last path without delimiter.
			 String [] parts = key.split("/");
			 return parts[parts.length - 1].replace("/","");
		 }
		 else {
			 // No delimiters in the key so just return the key.
			 return key;
		 }
	 }

	/**
	Gets the selected objects (from extending ItemSelectable; not used).
	@return null.
	*/
	public Object[] getSelectedObjects() {
		return null;
	}
	
	/**
	 * Return the S3 object associated with the node.
	 * @return the S3 object associated with the node.
	 */
	public AwsS3Object getS3Object () {
		// Object is stored in the SimpleJTree_Node data.
		if ( getData() == null ) {
			return null;
		}
		else {
			return (AwsS3Object)this.getData();
		}
	}

	/**
	Initializes the S3JTree_Node, which creates visual components that are visible.
	@param s3Object the S3 object associated with the node
	@param tree the S3JTree that contains this component
	@param listener the ItemListener to register for this component
	@param popup_JMenu the JPopupMenu that this node should display.
	If null, the JTree's shared popup menu will be displayed.
	*/
	private void initialize(AwsS3Object s3Object, S3JTree tree, JPopupMenu popup_JMenu) {

		this.s3Tree = tree;
		setData(s3Object);

	}

	/**
	Returns whether the text field is selected or not.
	@return whether the text field is selected or not.
	*/
	public boolean isTextSelected() {
		return this.selected;
	}

	/**
	Returns whether the layer associated with this node is selected or not.
	@return whether the layer associated with this node is selected or not.
	*/
	public boolean isSelected() {
		return isTextSelected();
	}

	/**
	Checks to see if the mouse event would trigger display of the popup menu.
	The popup menu does not display if it is null.
	@param e the MouseEvent that happened.
	*/
	private void maybeShowPopup(MouseEvent e) {
		if ( (this.popup_JMenu != null) && this.popup_JMenu.isPopupTrigger(e) ) {
			this.popup_JMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	Responds to mouse clicked events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseClicked ( MouseEvent event ) {
	}

	/**
	Responds to mouse dragged events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseDragged(MouseEvent event) {
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
	Responds to mouse moved events; does nothing.
	@param event the MouseEvent that happened.
	*/
	public void mouseMoved(MouseEvent event) {
	}

	/**
	Responds to mouse pressed events.
	@param event the MouseEvent that happened.
	*/
	public void mousePressed(MouseEvent event) {
		/*
		if (event.getButton() == 1) {
			// Left-click selects the text field by highlighting.
			if ( !event.isControlDown() ) {	
				// Simple left-click so only select the current component.
				deselectAllOthers();
				selectField();
			}
			else {
				// Ctrl-left-click.  Add the current selection to previous selections.
				if (this.selected) {
					deselectField();
				}
				else {
					selectField();
				}	
			}
			this.s3Tree.repaint();
		}
		// A node was either selected or deselected:
		// - check the button state to enable appropriate actions
		this.s3Tree.checkState();	
		*/
	}

	/**
	Responds to mouse released events.
	@param event the MouseEvent that happened.
	*/
	public void mouseReleased(MouseEvent event) {
		maybeShowPopup(event);
	}
	
	/**
	Removes an item listener from the list of listeners.
	@param listener the listener to remove.
	*/
	public void removeItemListener(ItemListener listener) {
		// Remove in reverse order to avoid concurrency exception issues.
		for ( int i = this.itemListeners.size() - 1; i >= 0; i-- ) {
			if ( this.itemListeners.get(i) == listener ) {
				this.itemListeners.remove(i);
			}
		}
	}

	/**
	 * Set the popup menu used with the node.
	 * This is called after creating the root node in a network because the tree does not yet exist
	 * and the tree's shared popup menu can't be passed to the node constructor.
	 * @param popupMenu the popup menu to use for the node
	 */
	public void setPopupMenu ( JPopupMenu popup_JMenu ) {
		this.popup_JMenu = popup_JMenu;
	}

	/**
	 * Set the JTree used with the node.
	 * This is called after creating the root node in a network because the tree does not yet exist
	 * and can't be passed to the node constructor.
	 * @param tree the tree associated with the node
	 */
	public void setS3JTree ( S3JTree tree ) {
		this.s3Tree = tree;
	}

}