// S3JTree_Node - node class corresponding to S3JTree tree

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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ItemSelectable;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JCheckBox;
//import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.tree.TreeNode;

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3ObjectType;

import RTi.Util.GUI.JGUIUtil;

import RTi.Util.GUI.SimpleJTree_CellRenderer;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;

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
implements FocusListener, MouseListener, ItemListener, ItemSelectable {

	/**
	Whether this node has been selected (i.e., the label has been clicked on) or not.
	*/
	private boolean selected = false;

	/**
	The Color in which the background of the non-selected node text should be drawn.
	*/
	private Color bg = null;

	/**
	The Color in which the foreground of the non-selected node text should be drawn.
	*/
	private Color fg = null;

	/**
	Reference to the S3 tree in which this component appears.
	*/
	private S3JTree s3Tree;

	/**
 	* The AwsS3_Object associated with the node:
 	* - contains the S3 key (object path) in all cases
 	* - folders only contain the key and files contain owner, size, last modified
 	* - the object type indicates whether a bucket, folder, or file
 	*/
	private AwsS3Object s3Object = null;

	/**
	Reference to the unlabeled checkbox that appears in this component.
	*/
	private JCheckBox check = null;

	/**
	The popup menu associated with this node.
	*/
	private JPopupMenu popup_JMenu = null;

	/**
	Label that that is used when displaying the node.
	Originally used a JTextField to automatically handle some of the selection rendering.
	However, JTextField did not cleanly handle HTML labels so switch to a JLabel.
	*/
	//private JEditorPane __field = null;
	//private JButton __field = null;
	//private JLabel __field = null;
	private JTextField label_JTextField = null;

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
	Deselects all the labels in all the other nodes in the tree.
	*/
	private void deselectAllOthers() {
		deselectAllOthers((S3JTreeNode)this.s3Tree.getRoot());
	}

	/**
	Utility method used by deselectAllOthers()
	@param node the node from which to recurse the tree.
	*/
	private void deselectAllOthers(S3JTreeNode node) {
		if (node instanceof S3JTreeNode) {
			if ( node != this ) {
				node.deselectField();
			}		
		}
		if (node.getChildCount() >= 0) {
			for (Enumeration<S3JTreeNode> e = node.children(); e.hasMoreElements();) {
				S3JTreeNode n = e.nextElement();
				deselectAllOthers(n);
			}
		}
	}

	/**
	Deselects the text field in this node.
	*/
	public void deselectField() {
		this.label_JTextField.setBackground(bg);
		this.label_JTextField.setForeground(fg);
		this.label_JTextField.repaint();
		this.selected = false;
	}

	/**
	Returns the label for this node.
	@return the label for this node.
	*/
	public String getLabel() {
		if (this.label_JTextField == null) {
			return null;
		}
		return this.label_JTextField.getText().trim();
	}

	/**
	 * Find a matching node under a node. Do not recurse to children.
	 * @param text visible folder name text to match (only the folder name, not the path)
	 */
   	public S3JTreeNode findFolderMatchingText ( String text ) {
   		return findFolderMatchingText ( text, false );
   	}
	
	/**
	 * Find a matching node under a node based on the visible text. Do not recurse to children.
	 * @param text visible folder name text to match (only the name, not the path)
	 * @param doRecurs whether to recursively search sub-folders (not currently implemented)
	 */
   	public S3JTreeNode findFolderMatchingText ( String text, boolean doRecurse ) {
   		S3JTreeNode node = null;
   		if ( doRecurse ) {
   			// TODO smalers 2023-01-11 not currently implemented.
   			return null;
   		}
   		else {
   			this.children();
   			for ( @SuppressWarnings("unchecked")
   				Enumeration<? extends TreeNode> e = this.children(); e.hasMoreElements(); ) {
   				TreeNode n = e.nextElement();
   				node = (S3JTreeNode)n;
   				if ( node.getText().equals(text) ) {
   					return node;
   				}
   			}
   			// Node was not found so return null;
   			return null;
   		}
   	}

	/**
	Indicate when focus is gained on the component.
	*/
	public void focusGained ( FocusEvent e ) {
		Message.printStatus(2,"","Legend item focused gained for label component " + this.label_JTextField );
	}

	/**
	Indicate when focus is lost on the component.
	*/
	public void focusLost ( FocusEvent e ) {
		Message.printStatus(2,"","Legend item focused gained for label component " + this.label_JTextField );
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
		return this.s3Object;
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
		//String text = s3Object.getKey();
		//String name = s3Object.getKey();

		this.s3Tree = tree;
		this.s3Object = s3Object;

		boolean doCheckBox = false;
		if ( doCheckBox ) {
			// Get the text for the node and use for the JTextField.
			String text = getText();

			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			this.check = new JCheckBox();
			this.check.setBackground(UIManager.getColor("Tree.textBackground"));
			label_JTextField = new JTextField();
			if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
				// Bucket does not have a key.
				label_JTextField.setToolTipText("S3 bucket: " + s3Object.getBucket());
			}
			else {
				// Files and folders use the key.
				label_JTextField.setToolTipText("S3 key: " + s3Object.getKey());
			}
			//__field = new JEditorPane();

			// Because of the way these two components (the checkbox and the label) are drawn,
			// sometimes the first letter of the JLabel is slightly (like, 2 pixels) overlapped by the CheckBox.
			// Adding a single space at the front of the label text seems to avoid this.
	
			if ( text.startsWith("<") ) {
				// Assume HTML so just set it.
				label_JTextField.setText(text);
				// TODO SAM 2010-12-15 Uncomment this if using a JEditPane with HTML.
				//__field.setContentType("mime/html");
			}
			else {
				// Add extra space.
				label_JTextField.setText(" " + text);
				label_JTextField.setFont((new SimpleJTree_CellRenderer()).getFont());
			}

			// Add listeners to the label so that popup menu can be shown:
			// - listen in the node to handle selection and highlighting
			// - listen in the tree to handle right-click popups
			label_JTextField.addMouseListener(this);
			label_JTextField.addMouseListener(this.s3Tree);
			// JTextField and JEditorPane.
			label_JTextField.setEditable(false);
			// JButton and JLabel.
			//__field.addFocusListener(this);
			//__field.setFocusable(true);
			// Don't want any decorative border.
			label_JTextField.setBorder(null);
			label_JTextField.setBackground(UIManager.getColor("Tree.textBackground"));
			JGUIUtil.addComponent(panel, this.check, 0, 0, 1, 1, 0, 0,
				GridBagConstraints.NONE, GridBagConstraints.WEST);
			JGUIUtil.addComponent(panel, label_JTextField, 1, 0, 2, 1, 1, 1,
				GridBagConstraints.BOTH, GridBagConstraints.WEST);

			// Add an item listener to the tree for the checkbox.
			this.check.addItemListener(this);
			this.itemListeners = new ArrayList<>();
			addItemListener(tree);

			this.popup_JMenu = popup_JMenu;

			// Store the default label drawing colors.
			bg = label_JTextField.getBackground();
			fg = label_JTextField.getForeground();

			// Set the panel as the component used for the tree node.
			setComponent(panel);
		}
	}

	/**
	Returns whether the check box is selected or not.
	@return whether the check box is selected or not.
	*/
	public boolean isCheckBoxSelected() {
		return this.check.isSelected();
	}

	/**
	Returns whether the layer associated with this node is visible or not.
	@return whether the layer associated with this node is visible or not.
	*/
	public boolean isVisible() {
		return isCheckBoxSelected();
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
	The internal item state changed event that occurs when the JCheckBox is clicked.
	Internally, this class is its own listener for the JCheckBox's item state changed event.
	It catches the event and then RE-posts it so that the GeoViewLegendJTree that catches the new event
	can see which specific node issued the event.
	@param e the ItemEvent that happened.
	*/
	public void itemStateChanged(ItemEvent e) {
		ItemEvent newEvt = new ItemEvent(this, 0, null, e.getStateChange());
		for ( ItemListener l : this.itemListeners ) {
			l.itemStateChanged(newEvt);
		}
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
	}

	/**
	Responds to mouse released events.
	@param event the MouseEvent that happened.
	*/
	public void mouseReleased(MouseEvent event) {
		maybeShowPopup(event);
	}
	
	/**
	 * Refresh the node based on input:
	 * - reset the text field for the label to the current text
	 */
	public void refresh () {
		// This is similar to initialize().
		String text = getText();
		this.label_JTextField.setText(text);
		if ( s3Object.getObjectType() == AwsS3ObjectType.BUCKET ) {
			// Bucket does not have a key so use the bucket only.
			//label_JTextField.setToolTipText("S3 bucket: " + s3Object.getBucket() + " text=" + text);
			label_JTextField.setToolTipText("S3 bucket: " + s3Object.getBucket() );
		}
		else {
			// Files and folders use the key.
			//label_JTextField.setToolTipText("S3 key: " + s3Object.getKey() + " text=" + text);
			label_JTextField.setToolTipText("S3 key: " + s3Object.getKey() );
		}
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
	Select's this node's text field.
	*/
	public void selectField() {
		this.selected = true;
		JTextField tf = new JTextField(); // Use this to get selection colors to mimic a JTextField.
		//__field.setBackground(__field.getSelectionColor());
		//__field.setForeground(__field.getSelectedTextColor());
		this.label_JTextField.setBackground(tf.getSelectionColor());
		this.label_JTextField.setForeground(tf.getSelectedTextColor());
		this.label_JTextField.repaint();
		//GeoLayerView layerView = (GeoLayerView)getData();
		//if (layerView != null) {
		//	layerView.isSelected(true);
		//}
	}

	/**
	Sets the selected state of the JCheckBox.
	@param selected the state to set the JCheckBox to
	*/
	public void setCheckBoxSelected(boolean selected) {
		this.check.setSelected(selected);
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

	/**
	Sets whether the layer associated with this node is selected or not.
	@param sel whether the layer is selected or not.
	*/
	public void setSelected(boolean sel) {
		if (sel) {
			selectField();
		}
		else {
			
			deselectField();
		}
	}

	/**
	Sets whether the layer associated with this node is visible or not.
	@param vis whether the layer is visible or not.
	*/
	public void setVisible(boolean vis) {
		if (vis) {
			this.check.setSelected(true);
		}
		else {
			this.check.setSelected(false);
		}
	}

}