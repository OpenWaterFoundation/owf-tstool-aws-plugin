// S3Browser_JFrame - JFrame to browse and interact with S3 files

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Message.Message;

/**
 * Create the S3 browser JFrame to interact with S3 files.
 */
@SuppressWarnings("serial")
public class S3Browser_JFrame extends JFrame implements ActionListener, WindowListener {

	/**
 	* AWS session used to interact with AWS:
 	* - will be null until the profile is set, which will happen when refresh() is called once
 	*/
	private AwsSession awsSession = null;
	
	/**
	 * The region to use for S3 browsing.
	 */
	private String region = null;
	
	/**
	 * Close button.
	 */
	private JButton closeButton = null;

	/**
	 * Refresh button.
	 */
	private JButton refreshButton = null;

	/**
	 * The panel containing the browswer content, including the tree.
	 */
	S3Browser_JPanel s3Panel = null;
	
	/**
	 * Open the S3Browser application window.
	 * @param profile AWS profile to use.
	 * @param region AWS region to use for S3.
	 */
	S3Browser_JFrame ( String title, AwsSession awsSession, String region ) {
		// Save input.
		this.awsSession = awsSession;
		this.region = region; 
		
		// Set up the user interface.

		setupUI(title, this);
	}

	/**
	Handle action events (button press, etc.)
	@param e ActionEvent to handle.
	*/
	public void actionPerformed ( ActionEvent e ) {
		Object o = e.getSource();
		if ( o == this.closeButton ) {
			closeWindow();
		}
		else if ( o == this.refreshButton ) {
			this.s3Panel.refresh();
		}
	}
	
	/**
	 * Close the window.
	 */
	private void closeWindow () {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(false);
		try {
            dispose();
		}
		catch ( Exception e ) {
			// Why is this a problem?
		}
		// Close the currently opened log file.
		Message.closeLogFile();
		// Exit with status 0 indicating normal exit.
		System.exit(0);
	}

	/**
	Set the icon for the application.  This will be used for all windows.
	*/
	public void setIcon () {
		// Loading the icon from the class path.
		String iconPath = "org/openwaterfoundation/tstool/plugin/aws/ui/OWF-Logo-Favicon-32x32.png";
		try {
        	// The icon files live in the main application folder in the classpath.
			JGUIUtil.setIconImage( iconPath );
			JGUIUtil.setIcon(this, JGUIUtil.getIconImage());
		}
		catch ( Exception e ) {
			Message.printStatus ( 2, "", "S3 browser icon \"" + iconPath + "\" does not exist in classpath." );
		}
	}

	/**
	 * Set up the UI.
	 */
	private void setupUI( String title, Component parentUIComponent ) {
	    JGUIUtil.setSystemLookAndFeel(true);
		setTitle(title);
		setIcon();
		addWindowListener ( this ); // Handles the "X" event.

		this.s3Panel = new S3Browser_JPanel ( this.awsSession, this.region );
		getContentPane().add ( s3Panel );

		// Put the buttons on the bottom of the window.

		JPanel button_JPanel = new JPanel ();
		button_JPanel.setLayout ( new FlowLayout(FlowLayout.CENTER) );

		this.refreshButton = new SimpleJButton ("Refresh", this );
		button_JPanel.add ( this.refreshButton );
		this.refreshButton.setToolTipText("Refresh the view from the S3 bucket contents" );

		this.closeButton = new SimpleJButton ("Close", this );
		this.closeButton.setToolTipText("Close the browser (will close the browser application)" );
		button_JPanel.add ( this.closeButton );

		getContentPane().add ( "South", button_JPanel );

		// Center on the UI component rather than the graph, because the graph screen seems to get tied screen 0?
		JGUIUtil.center ( this, parentUIComponent );
		//setPreferredSize ( new Dimension(600, 400) );
		setSize ( new Dimension(600, 400) );
		pack();
		
		setVisible ( true );
	}

	/**
	Responds to WindowEvents.
	@param event WindowEvent object
	*/
	public void windowClosing( WindowEvent event ) {
		closeWindow();
	}

	public void windowActivated( WindowEvent evt ) {
	}

	public void windowClosed( WindowEvent evt ) {
	}

	public void windowDeactivated( WindowEvent evt ) {
	}

	public void windowDeiconified( WindowEvent evt ) {
	}

	public void windowIconified( WindowEvent evt ) {
	}

	public void windowOpened( WindowEvent evt ) {
	}

}