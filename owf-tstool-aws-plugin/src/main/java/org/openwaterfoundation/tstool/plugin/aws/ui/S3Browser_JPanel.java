// S3Browser_JPanel - panel to browse and interact with S3 files

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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Message.Message;

/**
This class is a JPanel for browsing and interacting with S3 files.
The panel is included in JFrame and JDialog as necessary.
*/
@SuppressWarnings("serial")
public class S3Browser_JPanel extends JPanel implements ActionListener, ItemListener, KeyListener, MouseListener {

	/**
 	* AWS session used to interact with AWS:
 	* - will be null until the profile is set, which will happen when refresh() is called once
 	*/
	private AwsSession awsSession = null;

	/**
	 * List of S3 buckets.
	 */
	private SimpleJComboBox bucket_JComboBox = null;

	/**
	 * List of S3 regions, using form:  Region - Description
	 */
	private SimpleJComboBox region_JComboBox = null;
	
	/**
	 * The region that has been selected for S3 browsing.
	 */
	private String s3Region = null;
	
	/**
	 * The tree, which manages the nodes.
	 */
    private S3JTree s3JTree = null;

	/**
	 * S3TreeView that manages the tree.
	 */
	//private S3TreeView s3TreeView = null;
	
	/**
	 * Indicate whether the first time drawing the tree.
	 */
	//private boolean firstTime = true;

	/**
	Constructor.
	@param awsSession AwsSession instance to handle authentication
	@param s3Resion S3 region to use (will be selected in the choice), or null to not select
	*/
	public S3Browser_JPanel ( AwsSession awsSession, String s3Region ) {
		String routine = getClass().getSimpleName() + ".S3Browser_JPanel";
		this.awsSession = awsSession;
		this.s3Region = s3Region;
		
 		setupUI();
 		
 		// Load the initial S3 data.
 		String prefix = null;
 		boolean doLazyLoad = false;
 		try {
 			// Session must be set first.
 			this.s3JTree.setAwsSession(this.awsSession);
 			this.s3JTree.setBucket(getSelectedBucket());
 			// Region triggers creating a new S3 client.
 			this.s3JTree.setRegion(getSelectedRegion());
 			// Load the tree from an S3 request.
 			this.s3JTree.loadFromS3(prefix, doLazyLoad);
 		}
 		catch ( Exception e ) {
 			Message.printWarning(1, routine, "Error loading S3 bucket files.");
 			Message.printWarning(3, routine, e);
 		}
	}

	/**
	Responds to ActionEvents.
	@param event ActionEvent object
	*/
	public void actionPerformed(ActionEvent event) {
		//String s = event.getActionCommand();
	}

	/**
	Return the selected bucket.
	@return the selected bucket.
	*/
	public String getSelectedBucket() {
		return this.bucket_JComboBox.getSelected();
	}

	/**
	Return the selected region, omitting the trailing description.
	*/
	private String getSelectedRegion() {
    	if ( this.region_JComboBox == null ) {
        	return null;
    	}
    	String region = this.region_JComboBox.getSelected();
    	if ( region == null ) {
    		return region;
    	}
		// Parse the ID, ignoring the description.
   		int pos = region.indexOf(" -");
   		String id = "";
   		if ( pos > 0 ) {
   			// Have a description.
   			id = region.substring(0,pos).trim();
   		}
   		else {
   			// No description.
       		id = region.trim();
   		}
   		return id;
	}
	
	/**
	 * Return the S3JTree.
	 * @return the S3JTree.
	 */
	public S3JTree getS3JTree () {
		return this.s3JTree;
	}

	/**
	Handle ItemEvent events.
	@param e ItemEvent to handle.
	*/
	public void itemStateChanged ( ItemEvent e ) {
		String routine = getClass().getSimpleName() + ".itemStateChanged";
		Object o = e.getSource();

    	if ( (o == this.region_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
    		// Region was selected:
    		// - update the bucket list
    		populateBucketList();
    	}
    	else if ( (o == this.bucket_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
    		// Bucket was selected:
    		// - repopulate the tree
    		Message.printStatus(2, routine, "Bucket has been selected: " + getSelectedBucket());
    		Message.printStatus(2, routine, "Popolating tree for bucket: " + getSelectedBucket() );
    		refresh();
    	}
	}

	/**
	Does nothing.
	*/
	public void keyPressed(KeyEvent e) {
	}

	/**
	Does nothing.
	*/
	public void keyReleased(KeyEvent e) {
	}

	/**
	Does nothing.
	*/
	public void keyTyped(KeyEvent e) {
	}

	public void mouseClicked (MouseEvent e ) {
    	//setSelectedTextField(e.getComponent());
	}

	public void mouseEntered (MouseEvent e ) {
	}
	
	public void mouseExited (MouseEvent e ) {
	}

	public void mousePressed (MouseEvent e ) {
	}

	public void mouseReleased (MouseEvent e ) {
	}

	/**
	 * Populate the list of buckets available to browse.
	 */
	private void populateBucketList () {
        AwsToolkit.getInstance().uiPopulateBucketChoices(
        	this.awsSession,
        	this.s3Region,
        	this.bucket_JComboBox,
        	false);
        this.bucket_JComboBox.remove("");
		// TODO smalers 2023-01-10 hard-code during development, if troubleshooting the code.
        //this.bucket_JComboBox.select("software.openwaterfoundation.org");
        //this.bucket_JComboBox.select("data.openwaterfoundation.org");
	}

	/**
	 * Populate the list of regions available to browse.
	 */
	private void populateRegionList () {
        AwsToolkit.getInstance().uiPopulateRegionChoices(
        	this.awsSession,
        	this.region_JComboBox );
	}
	
    /**
     * Refresh the tree based on current settings.
     * This is usually called when the application's "Refresh" button is pressed or another bucket is selected.
     */
    public void refresh () {
    	String routine = getClass().getSimpleName() + ".refresh";
    	List<String> problems = new ArrayList<>();
    	
    	// Clear all the children from the root node.
    	Message.printStatus(2, routine, "Refreshing the tree.");
    	/* Use the code below.
    	try {
    		//S3JTreeNode rootNode = (S3JTreeNode)this.s3JTree.getRoot();
    		//Message.printStatus(2, routine, "Removing all child nodes from the root node: " + rootNode.getText());
    		//this.s3JTree.removeAllNodes();
    	}
    	catch ( Exception e ) {
    		// Should not happen.
    	}
    	*/
    	
    	try {
    		//this.s3TreeView.setBucket(getSelectedBucket());
    		// The following removes all nodes and then add the nodes from the bucket.
    		this.s3JTree.clear();
    		String prefix = null;
    		boolean doLazyLoad = false;
    		this.s3JTree.loadFromS3(prefix,doLazyLoad);//problems);
    	}
    	catch ( Exception e ) {
    		Message.printWarning(2, routine, "Error refreshing the tree." );
    		Message.printWarning(2, routine, e );
    	}
    }

	/**
	Sets up the GUI.
	*/
	private void setupUI() {
		setLayout(new GridBagLayout());

		Insets insetsTLBR = new Insets(2,2,2,2);
		JPanel panel = this;
	
		// Create a panel on the left for local files and on the right for S3 files using a split pane.

		JPanel localPanel = new JPanel();
		JPanel s3Panel = new JPanel();
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, localPanel, s3Panel);
		int y = -1;
	    JGUIUtil.addComponent(panel,
	        splitPane,
	        0, ++y, 1, 1, 1, 1, insetsTLBR,
	        GridBagConstraints.BOTH, GridBagConstraints.WEST);

		localPanel.setLayout(new GridBagLayout());
		int yLocal = -1;
	    JGUIUtil.addComponent(localPanel,
	        new JLabel("Local Files"),
	        0, ++yLocal, 2, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		JPanel localFilePanel = new JPanel();
		localFilePanel.setLayout(new GridBagLayout());
	    JGUIUtil.addComponent(localPanel,
    		new JScrollPane(localFilePanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
    		0, ++y, 2, 1, 1.0, 1.0, insetsTLBR,
    		GridBagConstraints.BOTH, GridBagConstraints.WEST);

	    // Label at top indicating that S3 files are the focus.
		s3Panel.setLayout(new GridBagLayout());
		int yS3 = -1;
	    JGUIUtil.addComponent(s3Panel,
	        new JLabel("S3 Objects (\"keys\" that represent files)"),
	        0, ++yS3, 2, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	    // Profile.
	    JGUIUtil.addComponent(s3Panel,
	        new JLabel("AWS profile:"),
	        0, ++yS3, 1, 1, 0, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);
	    String profile = this.awsSession.getProfile();
	    if ( (profile == null) || profile.isEmpty() ) {
	    	profile = AwsToolkit.getInstance().getDefaultProfile();
	    }
	    JTextField profile_JTextField = new JTextField(profile, 20);
	    profile_JTextField.setToolTipText("AWS profile from configuration file: " + AwsToolkit.getInstance().getAwsUserConfigFile());
	    profile_JTextField.setEditable(false);
	    JGUIUtil.addComponent(s3Panel,
	        profile_JTextField,
	        1, yS3, 1, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);

	    // Regions.
	    JGUIUtil.addComponent(s3Panel,
	        new JLabel("AWS region:"),
	        0, ++yS3, 1, 1, 0, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);
	    this.region_JComboBox = new SimpleJComboBox (false);
	    this.region_JComboBox.setToolTipText("AWS regions that are available for S3 storage buckets.");
	    populateRegionList();
	    this.region_JComboBox.addItemListener(this);
	    JGUIUtil.addComponent(s3Panel,
	        this.region_JComboBox,
	        1, yS3, 1, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);
	
		// Buckets.
	    JGUIUtil.addComponent(s3Panel,
	        new JLabel("S3 bucket:"),
	        0, ++yS3, 1, 1, 0, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);
	    this.bucket_JComboBox = new SimpleJComboBox (false);
	    this.bucket_JComboBox.setToolTipText("S3 buckets that are available for the profile and region.");
	    populateBucketList();
	    this.bucket_JComboBox.addItemListener(this);
	    JGUIUtil.addComponent(s3Panel,
	        this.bucket_JComboBox,
	        1, yS3, 1, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);

	    // Select the region that was passed into the constructor and let the choices cascade.
	    if ( (this.s3Region != null) && !s3Region.isEmpty() ) {
	    	// Try to select:
	    	// - regions have the ID and name separated by a dash so need to select based on the first token
	    	try {
	    		//this.region_JComboBox.select(this.s3Region);
	    		boolean ignoreCase = false;
	    		int flags = 0;
	    		int token = 0;
	    		boolean trimTokens = true;
	    		String defaultItem = null;
	    		JGUIUtil.selectTokenMatches(this.region_JComboBox, ignoreCase, " ", flags, token,
	    			this.s3Region, defaultItem, trimTokens);
	    	}
	    	catch ( Exception e ) {
	    		// Just select the first item.
	    		this.region_JComboBox.select(0);
	    	}
	    }

	    //this.s3TreeView = new S3TreeView(this.awsSession, getSelectedRegion(), getSelectedBucket());
        //this.s3JTree = new S3JTree((S3JTreeNode)s3TreeView.getRootNode());
        this.s3JTree = new S3JTree();

	    // S3 file tree.
		JPanel s3TreePanel = new JPanel();
		s3TreePanel.setLayout(new GridBagLayout());
		JScrollPane s3TreeScrollPane = new JScrollPane(this.s3JTree);
		
		// Add the tree to the file panel.
	    JGUIUtil.addComponent(s3TreePanel,
    		new JScrollPane(s3TreeScrollPane,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
    		0, 0, 1, 1, 1.0, 1.0, insetsTLBR,
    		GridBagConstraints.BOTH, GridBagConstraints.WEST);

	    // Add the tree panel to the S3 panel.
	    JGUIUtil.addComponent(s3Panel,
    		new JScrollPane(s3TreePanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
    		0, ++yS3, 2, 2, 1.0, 1.0, insetsTLBR,
    		GridBagConstraints.BOTH, GridBagConstraints.WEST);
	}

}