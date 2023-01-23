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
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;
import org.openwaterfoundation.tstool.plugin.aws.S3CommandResult;
import org.openwaterfoundation.tstool.plugin.aws.commands.s3.AwsS3Object;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.ReportJFrame;
import RTi.Util.GUI.ResponseJDialog;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJMenuItem;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.DiagnosticsJFrame;
import RTi.Util.Message.Message;

/**
 * Create the S3 browser JFrame to interact with S3 files.
 */
@SuppressWarnings("serial")
public class S3Browser_JFrame extends JFrame implements ActionListener, TreeSelectionListener, WindowListener {

	/**
 	* AWS session used to interact with AWS:
 	* - will be null until the profile is set, which will happen when refresh() is called once
 	*/
	private AwsSession awsSession = null;
	
	/**
	 * The region to use for S3 browsing.
	 */
	private String region = null;
	
	// Tool buttons.

	private SimpleJButton cut_JButton = null;
	private SimpleJButton copy_JButton = null;
	private SimpleJButton paste_JButton = null;
	private SimpleJButton delete_JButton = null;
	private SimpleJButton rename_JButton = null;

	// Content in the main part of the frame.
	
	/**
	 * The panel containing the browser content, including the tree.
	 */
	S3Browser_JPanel s3Panel = null;
	
	// Buttons at the bottom of the frame.
	
	/**
	 * Close button.
	 */
	private JButton close_JButton = null;

	/**
	 * Refresh button.
	 */
	private JButton refresh_JButton = null;

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
	Handle action events (button press, menus, etc.)
	@param e ActionEvent to handle.
	*/
	public void actionPerformed ( ActionEvent e ) {
		Object o = e.getSource();
		String command = e.getActionCommand();

		// Application menus.
		
		if ( command.equals("Exit") ) {
			// Exit from the file menu.
			closeWindow();
		}
		else if ( command.equals("Diagnostics...") ) {
			// Show the diagnostics configuration window.
			showDiagnostics();
		}
		
		// Diagnostics tool menus.

		else if ( command.equals("View Log File") ) {
			// View the log file.
			showLogFile();
		}
		else if ( o == this.close_JButton ) {
			// Exit from the "Close" button.
			closeWindow();
		}
		
		// Application buttons.
		
		else if ( o == this.delete_JButton ) {
			// Delete the objects, prompting for confirmation.
			deleteS3Objects(true);
		}
		else if ( o == this.refresh_JButton ) {
			// Refresh the tree in the panel, for example if S3 has been updated outside of the tool
			// or the tree is otherwise out of synchronization.
			this.s3Panel.refreshTree();
		}
	}

	/**
	 * Check the UI state:
	 * - enable and disable buttons as appropriate
	 */
	public void checkState() {
		S3JTree s3Tree = this.s3Panel.getS3JTree();
		int nSelected = s3Tree.getSelectionCount();
		if ( nSelected == 0 ) {
			this.copy_JButton.setEnabled(false);
			this.cut_JButton.setEnabled(false);
			this.paste_JButton.setEnabled(false);
			this.delete_JButton.setEnabled(false);
			this.rename_JButton.setEnabled(false);
		}
		else {
			// TODO smalers 2023-01-15 need to enable cut/copy/paste.
			this.copy_JButton.setEnabled(false);
			this.cut_JButton.setEnabled(false);
			this.paste_JButton.setEnabled(false);
			// Delete is enabled.
			this.delete_JButton.setEnabled(true);
			// TODO smalers 2023-01-15 need to enable rename.
			this.rename_JButton.setEnabled(false);
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
	 * Delete selected S3 file and folder objects.
	 * @param doConfirmation whether or not to confirm the deletion
	 */
	private void deleteS3Objects ( boolean doConfirmation ) {
		String routine = getClass().getSimpleName() + ".deleteNode";
		// Prompt for confirmation and then delete nodes that are selected.
		int response = ResponseJDialog.OK;
		
		// Get the list of selected nodes:
		// - delete in reverse sorted order so that the innermost files are deleted first
		List<S3JTreeNode> selectedNodeList0 = this.s3Panel.getS3JTree().getSelectedS3JTreeNodes(true);
		if ( selectedNodeList0.size() == 0 ) {
			// Should not happen if node selection handling is working correctly.
			return;
		}
		List<S3JTreeNode> selectedNodeList = new ArrayList<>();
		for ( int i = selectedNodeList.size() - 1; i >= 0; i-- ) {
			selectedNodeList.add(selectedNodeList0.get(i));
		}

		AwsS3Object s3Object = null;
		if ( doConfirmation ) {
			StringBuilder b = new StringBuilder("The following S3 objects (keys) will be deleted:\n\n");
			b.append("Bucket: " + this.s3Panel.getSelectedBucket() );
			b.append("\n\nFiles and folders: " + this.s3Panel.getSelectedBucket() + "\n\n");
			for ( S3JTreeNode node : selectedNodeList ) {
				s3Object = node.getS3Object();
				b.append ( "  " + s3Object.getKey() + "\n");
			}
			response = new ResponseJDialog(this, "Confirm S3 Object Delete",
				b.toString(), ResponseJDialog.OK).response();
		}
		
		if ( response == ResponseJDialog.OK ) {
			// Continue with the object delete.
			// Create a list of objects (keys) to delete.
			List<AwsS3Object> selectedObjects = new ArrayList<>();
			for ( S3JTreeNode node : selectedNodeList ) {
				s3Object = node.getS3Object();
				selectedObjects.add ( s3Object );
			}
			// Delete the S3 resources.
			List<S3CommandResult> deleteResults = null;
			try {
				deleteResults = AwsToolkit.getInstance().deleteS3Objects ( selectedObjects );
				if ( deleteResults.size() > 0 ) {
					
				}
				
				// Regardless, update the network based on the results:
				// - remove tree nodes for nodes that were successfully deleted
			}
			catch ( Exception e ) {
				// Show a top-level warning and write the exception to the log.
				Message.printWarning(1, routine, "Error deleting S3 objects.  See the log file.");
				Message.printWarning(3, routine, e);
			}
		}
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
	 * Set up the menu bar.
	 */
	private void setupMenus () {
		JMenuBar menuBar = new JMenuBar();

		// File menu.
		
		JMenu File_JMenu = new JMenu( "File", true );
		menuBar.add( File_JMenu );

		SimpleJMenuItem File_Exit_JMenuItem = new SimpleJMenuItem( "Exit", this);
		File_Exit_JMenuItem.setToolTipText("Exit the S3 Browser.");
		File_JMenu.add( File_Exit_JMenuItem );

		// Tools menu.

		JMenu Tools_JMenu = new JMenu( "Tools", true );
		menuBar.add( Tools_JMenu );

		// Create the diagnostics GUI, specifying the key for the help
		// button.  Events are handled from within the diagnostics GUI.
		DiagnosticsJFrame diagnostics_JFrame = new DiagnosticsJFrame (this);
		diagnostics_JFrame.attachMainMenu ( Tools_JMenu );

		// Set the menu bar for the window.

		setJMenuBar ( menuBar );
	}

	/**
	 * Set up the tool bar.
	 */
	private void setupToolBar () {
		JToolBar toolBar = new JToolBar();
		// Disable floating (dockable) toolbar.
		toolBar.setFloatable(false);

		Insets insetsNNNN = new Insets(0,0,0,0);

		// Add standard tool icons:
		// - icons are distributed with the application
		// - run checkState in the main setup function to set the state of buttons
		String resourcePath = "/org/openwaterfoundation/tstool/plugin/aws/ui/resources";

		// From Oracle JDK.
		//URL cutButtonUrl = this.getClass().getResource( resourcePath + "/Cut16.gif" );
		// From Material icons.
		URL cutButtonUrl = this.getClass().getResource( resourcePath + "/cut_FILL0_wght400_GRAD0_opsz48.png" );
		ImageIcon cutImageIcon = new ImageIcon(cutButtonUrl);
		Image cutImage = cutImageIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
		cutImageIcon = new ImageIcon(cutImage);
		this.cut_JButton = new SimpleJButton(cutImageIcon,
				"Cut",
				"Cut selected S3 objects",
				insetsNNNN, false, this);
		toolBar.add(cut_JButton);

		// From Oracle JDK.
		//URL copyButtonUrl = this.getClass().getResource( resourcePath + "/Copy16.gif" );
		// From Material icons.
		URL copyButtonUrl = this.getClass().getResource( resourcePath + "/file_copy_FILL0_wght400_GRAD0_opsz48.png" );
		ImageIcon copyImageIcon = new ImageIcon(copyButtonUrl);
		Image copyImage = copyImageIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
		copyImageIcon = new ImageIcon(copyImage);
		this.copy_JButton = new SimpleJButton(copyImageIcon,
				"Copy",
				"Copy selected S3 objects",
				insetsNNNN, false, this);
		toolBar.add(copy_JButton);

		// From Oracle JDK.
		//URL pasteButtonUrl = this.getClass().getResource( resourcePath + "/Paste16.gif" );
		// From Material icons.
		URL pasteButtonUrl = this.getClass().getResource( resourcePath + "/content_paste_FILL0_wght400_GRAD0_opsz48.png" );
		ImageIcon pasteImageIcon = new ImageIcon(pasteButtonUrl);
		Image pasteImage = pasteImageIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
		pasteImageIcon = new ImageIcon(pasteImage);
		this.paste_JButton = new SimpleJButton(pasteImageIcon,
				"Paste",
				"Paste selected S3 objects",
				insetsNNNN, false, this);
		toolBar.add(paste_JButton);

		toolBar.addSeparator();
		// From Oracle JDK.
		//URL deleteButtonUrl = this.getClass().getResource( resourcePath + "/Delete16.gif" );
		// From Material icons.
		URL deleteButtonUrl = this.getClass().getResource( resourcePath + "/delete_FILL0_wght400_GRAD0_opsz48.png" );
		ImageIcon deleteImageIcon = new ImageIcon(deleteButtonUrl);
		Image deleteImage = deleteImageIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
		deleteImageIcon = new ImageIcon(deleteImage);
		this.delete_JButton = new SimpleJButton(deleteImageIcon,
				"Delete",
				"Delete selected S3 objects",
				insetsNNNN, false, this);
		toolBar.add(delete_JButton);

		// From icon8 icons.
		URL renameButtonUrl = this.getClass().getResource( resourcePath + "/icons8-rename-50.png" );
		ImageIcon renameImageIcon = new ImageIcon(renameButtonUrl);
		Image renameImage = renameImageIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
		renameImageIcon = new ImageIcon(renameImage);
		this.rename_JButton = new SimpleJButton(renameImageIcon,
				"Rename",
				"Rename selected S3 object",
				insetsNNNN, false, this);
		toolBar.add(rename_JButton);

		getContentPane().add("North", toolBar);
	}

	/**
	 * Set up the UI.
	 */
	private void setupUI( String title, Component parentUIComponent ) {
	    JGUIUtil.setSystemLookAndFeel(true);
		setTitle(title);
		setIcon();
		addWindowListener ( this ); // Handles the "X" event.

		// Set up the menus and tool bar.
		setupMenus();
		setupToolBar();

		this.s3Panel = new S3Browser_JPanel ( this.awsSession, this.region );
		getContentPane().add ( "Center", s3Panel );

		// Put the buttons on the bottom of the window.

		JPanel button_JPanel = new JPanel ();
		button_JPanel.setLayout ( new FlowLayout(FlowLayout.CENTER) );

		this.refresh_JButton = new SimpleJButton ("Refresh", this );
		button_JPanel.add ( this.refresh_JButton );
		this.refresh_JButton.setToolTipText("Refresh the view from the S3 bucket contents" );

		this.close_JButton = new SimpleJButton ("Close", this );
		this.close_JButton.setToolTipText("Close the browser (will close the browser application)" );
		button_JPanel.add ( this.close_JButton );
		
		// Set up listeners so that the frame can respond to events in the tree:
		// - only care if he tree selections change because individual node listeners are handled in the node
		S3JTree s3Tree = this.s3Panel.getS3JTree();
		s3Tree.addTreeSelectionListener(this);

		// Add the mane panel to the frame UI.
		getContentPane().add ( "South", button_JPanel );

		// Center on the UI component rather than the graph, because the graph screen seems to get tied screen 0?
		JGUIUtil.center ( this, parentUIComponent );
		//setPreferredSize ( new Dimension(600, 400) );
		setSize ( new Dimension(600, 400) );
		pack();
		
		// Check the state of the UI:
		// - enable and disable tools, etc.
		checkState();
		
		setVisible ( true );
	}

	/**
	 * Show the log file.
	 */
	private void showLogFile () {
		String routine = getClass().getSimpleName() + ".showLogFile";
		// View the startup log file.
		String logFile = Message.getLogFile();
		// Show in a simple viewer.
		PropList reportProp = new PropList ("S3 Browser Log File");
		reportProp.set ( "TotalWidth", "800" );
		reportProp.set ( "TotalHeight", "600" );
		reportProp.set ( "DisplayFont", "Courier" );
		reportProp.set ( "DisplaySize", "11" );
		reportProp.set ( "PrintFont", "Courier" );
		reportProp.set ( "PrintSize", "7" );
		reportProp.set ( "Title", "S3 Browser Log File" );
		reportProp.setUsingObject ( "ParentUIComponent", this );
		try {
			List<String> logLines = IOUtil.fileToStringList(logFile);
			new ReportJFrame ( logLines, reportProp );
		}
		catch ( Exception e ) {
			Message.printWarning(1, routine, "Error viewing log file (" + e + ")." );
		}
	}
	
	/**
	 * Show the diagnostics.
	 */
	private void showDiagnostics () {
		
	}

   	/**
   	 * Event handler for TreeSelectionListener.
   	 * @param e event to handle
   	 */
   	public void valueChanged ( TreeSelectionEvent e ) {
   		//String routine = getClass().getSimpleName() + ".valueChanged";
   		checkState();
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