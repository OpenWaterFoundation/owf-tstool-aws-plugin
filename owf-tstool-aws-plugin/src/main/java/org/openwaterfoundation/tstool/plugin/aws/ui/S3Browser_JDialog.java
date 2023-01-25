// S3Browser_JDialog - dialog to browse and interact with S3 files

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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
This class is a dialog for browsing and interacting with S3 files.
The dialog is helpful when AWS Console cannot be used.
*/
@SuppressWarnings("serial")
public class S3Browser_JDialog extends JDialog implements ActionListener, KeyListener, MouseListener, WindowListener {

	/**
	Button labels.
	*/
	private final String
		BUTTON_CLOSE = "Close";

	/**
	Dialog buttons.
	*/
	private SimpleJButton
		closeButton = null;

	private String 
		response = null, // Dictionary string that is returned via response().
		title = null;

	/**
 	* AWS session used to interact with AWS:
 	* - will be null until the profile is set, which will happen when refresh() is called once
 	*/
	private AwsSession awsSession = null;

	/**
	 * S3 client to use for S3 interactions.
	 */
	private S3Client s3 = null;

	/**
	 * List of S3 buckets.
	 */
	private SimpleJComboBox bucket_JComboBox = null;
	
	/**
	 * The region that has been selected for S3 browsing.
	 */
	private String s3Region = null;

	/**
	Constructor.
	@param parent the parent JFrame on which the dialog will appear.
	This cannot be null.  If necessary, pass in a new JFrame.
	@param modal whether the dialog is modal.
	@param title dialog title
	@param s3 S3 client to use for interactions
	*/
	public S3Browser_JDialog ( JFrame parent, boolean modal,String title, AwsSession awsSession, String s3Region ) {
		super(parent, modal);

		this.title = title;
		this.awsSession = awsSession;
		this.s3Region = s3Region;
		
		// Create an S3 client to use for browser interactions.
		
		Region regionO = Region.of(this.s3Region);
		this.s3 = S3Client.builder()
			.region(regionO)
			.credentialsProvider(this.awsSession.getProfileCredentialsProvider())
			.build();

 		setupUI();
	}

	/**
	Responds to ActionEvents.
	@param event ActionEvent object
	*/
	public void actionPerformed(ActionEvent event) {
		String s = event.getActionCommand();
	
    	if (s.equals(this.BUTTON_CLOSE)) {
			response ( true );
		}
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

	/**
	 * Populate the list of buckets available to browse.
	 */
	private void populateBucketList () {
        AwsToolkit.getInstance().uiPopulateBucketChoices(
        	this.awsSession,
        	this.s3Region,
        	this.bucket_JComboBox,
        	false);
	}

	/**
	Return the user response and dispose the dialog.
	@return the dialog response.  If <code>null</code>, the user pressed Cancel.
	*/
	public void response ( boolean ok ) {
		setVisible(false);
		dispose();
		if ( !ok ) {
			this.response = null;
		}
	}

	/**
	Return the user response and dispose the dialog.
	The key values do not need to be unique since handled in a string rather than a hash.
	@return the dialog response in form:  key1:value1,key2,value2
 	If <code>null</code> is returned, the user pressed Cancel.
	*/
	public String response () {
		return this.response;
	}

	/**
	Sets up the GUI.
	*/
	private void setupUI() {
    	if ( this.title != null ) {
        	setTitle(this.title );
    	}

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		getContentPane().add("Center", panel);

		Insets insetsTLBR = new Insets(2,2,2,2);
	
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

		s3Panel.setLayout(new GridBagLayout());
		int yS3 = -1;
	    JGUIUtil.addComponent(s3Panel, 
	        new JLabel("S3 Files"),
	        0, ++yS3, 2, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(s3Panel, 
	        new JLabel("S3 bucket:"),
	        0, ++yS3, 1, 1, 0, 0, insetsTLBR,
	        GridBagConstraints.NONE, GridBagConstraints.WEST);
	    this.bucket_JComboBox = new SimpleJComboBox (false);
	    populateBucketList();
	    JGUIUtil.addComponent(s3Panel, 
	        this.bucket_JComboBox,
	        1, yS3, 1, 1, 1, 0, insetsTLBR,
	        GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		JPanel s3FilePanel = new JPanel();
		s3FilePanel.setLayout(new GridBagLayout());
	    JGUIUtil.addComponent(s3Panel, 
    		new JScrollPane(s3FilePanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),
    		0, ++yS3, 2, 2, 1.0, 1.0, insetsTLBR,
    		GridBagConstraints.BOTH, GridBagConstraints.WEST);
	
		JPanel south = new JPanel();
		south.setLayout(new FlowLayout(FlowLayout.RIGHT));

		this.closeButton = new SimpleJButton(this.BUTTON_CLOSE, this);
		this.closeButton.setToolTipText("Close the dialog.");
		south.add(this.closeButton);

		getContentPane().add("South", south);

		pack();
		// Set the window size.  Otherwise large numbers of items in the dictionary will cause the scrolled panel to
		// be bigger than the screen at startup in some cases.
		setSize(650,400);
		setResizable ( true );
		JGUIUtil.center(this);
		setVisible(true);
		JGUIUtil.center(this);
	}

	/**
	Respond to WindowEvents.
	@param event WindowEvent object.
	*/
	public void windowClosing(WindowEvent event) {
		this.response = null;
		response ( false );
	}

	/**
	Does nothing.
	*/
	public void windowActivated(WindowEvent evt) {
	}

	/**
	Does nothing.
	*/
	public void windowClosed(WindowEvent evt) {
	}

	/**
	Does nothing.
	*/
	public void windowDeactivated(WindowEvent evt) {
	}

	/**
	Does nothing.
	*/
	public void windowDeiconified(WindowEvent evt) {
	}

	/**
	Does nothing.
	*/
	public void windowIconified(WindowEvent evt) {
	}

	/**
	Does nothing.
	*/
	public void windowOpened(WindowEvent evt) {
	}

}