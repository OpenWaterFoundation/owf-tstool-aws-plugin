// S3JTree_Node - convenience class to use when putting S3 data into a SimpleJTree

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

import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3_Object;

import RTi.Util.GUI.JGUIUtil;

import RTi.Util.GUI.SimpleJTree_CellRenderer;
import RTi.Util.GUI.SimpleJTree_Node;
import RTi.Util.Message.Message;

/**
This class is a convenience class for displaying CheckBox and label information for S3 objects.
This code was copied from RTi.GIS.GeoViewLegend.GeoViewLegendJTree_Node and modified.
These nodes contain two components, a JCheckBox (with no text) and a separate JLabel.
*/
@SuppressWarnings("serial")
public class S3JTree_Node
extends SimpleJTree_Node
implements FocusListener, MouseListener, ItemListener, ItemSelectable {

/**
Whether this node has been selected (i.e., the label has been clicked on) or not.
*/
private boolean __selected = false;

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
private S3TreeView_JTree __tree;

/**
 * The AwsS3_Object associated with the node.
 */
private AwsS3_Object s3Object = null;

/**
Reference to the unlabeled checkbox that appears in this component.
*/
private JCheckBox __check = null;

/**
The popup menu associated with this node.
*/
private JPopupMenu __popup = null;

/**
Label that appears in this component.
Originally used a JTextField to automatically handle some of the selection rendering.
However, JTextField did not cleanly handle HTML labels so switch to a JLabel.
*/
//private JEditorPane __field = null;
//private JButton __field = null;
//private JLabel __field = null;
private JTextField __field = null;

/**
The listeners that are registered to listen for this objects item state changed events.
*/
private List<ItemListener> __listeners = null;

/**
Constructor.
@param s3Object S3 object associated with the node
@param tree the tree in which this component appears
*/
public S3JTree_Node(AwsS3_Object s3Object, S3TreeView_JTree tree) {
	super(new JPanel(), s3Object.getKey());
	initialize(s3Object, tree, null);
}

/**
Constructor.
@param s3Object S3 object associated with the node
@param tree the tree in which this component appears
@param popupMenu the popupMenu that this node should display.
*/
public S3JTree_Node(AwsS3_Object s3Object, S3TreeView_JTree tree, JPopupMenu popupMenu) {
	super(new JPanel(), s3Object.getKey());
	initialize(s3Object, tree, popupMenu);
}

/**
Registers an item listener for this component.
@param listener the listener to add to the list of listeners.
*/
public void addItemListener(ItemListener listener) {
	__listeners.add(listener);
}

/**
Deselects all the labels in all the other nodes in the tree.
*/
private void deselectAllOthers() {
	deselectAllOthers(__tree.getRoot());
}

/**
Utility method used by deselectAllOthers()
@param node the node from which to recurse the tree.
*/
private void deselectAllOthers(SimpleJTree_Node node) {
	if (node instanceof S3JTree_Node) {
		if (node != this) {
			((S3JTree_Node)node).deselectField();
		}		
	}
	if (node.getChildCount() >= 0) {
		for (Enumeration<SimpleJTree_Node> e = node.children(); e.hasMoreElements();) {
			SimpleJTree_Node n = (SimpleJTree_Node)e.nextElement();
			deselectAllOthers(n);
		}
	}
}

/**
Deselects the text field in this node.
*/
public void deselectField() {
	__field.setBackground(bg);
	__field.setForeground(fg);
	__field.repaint();
	__selected = false;
}

/**
Returns the text stored in this node.
@return the text stored in this node.
*/
public String getFieldText() {
	if (__field == null) {
		return null;
	}
	return __field.getText().trim();
}

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	bg = null;
	fg = null;
	__tree = null;
	__check = null;
	__popup = null;
	__field = null;
	__listeners = null;
	super.finalize();
}

/**
Indicate when focus is gained on the component.
*/
public void focusGained ( FocusEvent e ) {
	Message.printStatus(2,"","Legend item focused gained for label component " + __field );
}

/**
Indicate when focus is lost on the component.
*/
public void focusLost ( FocusEvent e ) {
	Message.printStatus(2,"","Legend item focused gained for label component " + __field );
}

/**
Returns the layer view stored in this node.
@return the layer view stored in this node.
*/
//public GeoLayerView getLayerView() {
//	return (GeoLayerView)getData();
//}

/**
Gets the selected objects (from extending ItemSelectable; not used).
@return null.
*/
public Object[] getSelectedObjects() {
	return null;
}

/**
Initializes the settings in the S3JTree_Node.
@param s3Object the S3 object associated with the node
@param tree the SimpleJTree that contains this component
@param listener the ItemListener to register for this component
@param popupMenu the popupMenu that this node should display.  If null, no popup will be displayed.
*/
private void initialize(AwsS3_Object s3Object, S3TreeView_JTree tree, JPopupMenu popup) {
	String text = s3Object.getKey();
	String name = s3Object.getKey();
	JPanel panel = new JPanel();
	panel.setLayout(new GridBagLayout());
	__check = new JCheckBox();
	__check.setBackground(UIManager.getColor("Tree.textBackground"));
	__field = new JTextField();
	//__field = new JEditorPane();
	__tree = tree;

	// Because of the way these two components (the checkbox and the label) are drawn,
	// sometimes the first letter of the JLabel is slightly (like, 2 pixels) overlapped by the CheckBox.
	// Adding a single space at the front of the label text seems to avoid this.
	
	if ( text.startsWith("<") ) {
		// Assume HTML so just set it.
		__field.setText(text);
		// TODO SAM 2010-12-15 Uncomment this if using a JEditPane with HTML.
		//__field.setContentType("mime/html");
	}
	else {
		// Add extra space.
		__field.setText(" " + text);
		__field.setFont((new SimpleJTree_CellRenderer()).getFont());
	}

	__field.addMouseListener(this);
	// JTextField and JEditorPane.
	__field.setEditable(false);
	// JButton and JLabel.
	//__field.addFocusListener(this);
	//__field.setFocusable(true);
	// Don't want any decorative border.
	__field.setBorder(null);
	__field.setBackground(UIManager.getColor("Tree.textBackground"));
	JGUIUtil.addComponent(panel, __check, 0, 0, 1, 1, 0, 0,
		GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(panel, __field, 1, 0, 2, 1, 1, 1,
		GridBagConstraints.BOTH, GridBagConstraints.WEST);
	setComponent(panel);

	__check.addItemListener(this);
	__listeners = new ArrayList<ItemListener>();
	addItemListener(tree);

	__popup = popup;

	// Store the default label drawing colors.
	bg = __field.getBackground();
	fg = __field.getForeground();
}

/**
Returns whether the check box is selected or not.
@return whether the check box is selected or not.
*/
public boolean isCheckBoxSelected() {
	return __check.isSelected();
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
	return __selected;
}

/**
Returns whether the layer associated with this node is selected or not.
@return whether the layer associated with this node is selected or not.
*/
public boolean isSelected() {
	return isTextSelected();
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
		__check.setSelected(true);
	}
	else {
		__check.setSelected(false);
	}
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
	for (int i = 0; i < __listeners.size(); i++) {
		ItemListener l = __listeners.get(i);
		l.itemStateChanged(newEvt);
	}
}

/**
Checks to see if the mouse event would trigger display of the popup menu.
The popup menu does not display if it is null.
@param e the MouseEvent that happened.
*/
private void maybeShowPopup(MouseEvent e) {
	if (__popup != null && __popup.isPopupTrigger(e)) {
		__popup.show(e.getComponent(), e.getX(), e.getY());
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
		if (!event.isControlDown()) {	
			deselectAllOthers();
			selectField();
		}
		else {
			if (__selected) {
				deselectField();
			}
			else {
				selectField();
			}	
		}
		__tree.repaint();
	}
	// TODO smalers 2023-01-10 remove when figure out for S3.
	// A node was either selected or deselected - repaint the buttons in the GeoViewJPanel as appropriate.
	//__tree.updateGeoViewJPanelButtons();	
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
	for (int i = 0; i < __listeners.size(); i++) {
		if ((ItemListener)__listeners.get(i) == listener) {
			__listeners.remove(i);
		}
	}
}

/**
Select's this node's text field.
*/
public void selectField() {
	__selected = true;
	JTextField tf = new JTextField(); // Use this to get selection colors to mimic a JTextField.
	//__field.setBackground(__field.getSelectionColor());
	//__field.setForeground(__field.getSelectedTextColor());
	__field.setBackground(tf.getSelectionColor());
	__field.setForeground(tf.getSelectedTextColor());
	__field.repaint();
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
	__check.setSelected(selected);
}

}