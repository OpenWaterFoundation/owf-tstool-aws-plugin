// AwsS3LandingPage_JDialog - editor for AwsS3LandingPage command

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

package org.openwaterfoundation.tstool.plugin.aws.commands.s3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openwaterfoundation.tstool.plugin.aws.PluginMeta;
import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;
import org.openwaterfoundation.tstool.plugin.aws.ui.S3Browser_App;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;

@SuppressWarnings("serial")
public class AwsS3LandingPage_JDialog extends JDialog
implements ActionListener, ChangeListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __pathDatasetIndexFile_JButton = null;
private SimpleJButton __pathDatasetIndexHeadInsertTopFiles_JButton = null;
private SimpleJButton __pathDatasetIndexBodyInsertTopFiles_JButton = null;
private SimpleJButton __pathDatasetIndexBodyInsertBottomFiles_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __browseS3_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// AWS S3 tab.
private SimpleJComboBox __Profile_JComboBox = null;
private JTextField __ProfileDefault_JTextField = null; // View only (not a command parameter).
private JLabel __ProfileDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __Region_JComboBox = null;
private JTextField __RegionDefault_JTextField = null; // View only (not a command parameter).
private JLabel __RegionDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __Bucket_JComboBox = null;
private Boolean __bucketHasRootObject = null;
private JTextField __BucketHasRootObject_JTextField = null; // View only (not a command parameter).

// Catalog tab.
//private JTextField __CatalogIndexFile_JTextField = null;
//private JTextField __CatalogFile_JTextField = null;
//private SimpleJComboBox __UploadCatalogFiles_JComboBox = null;

// Dataset tab.
private JTextField __DatasetIndexFile_JTextField = null;
private JTextField __StartingFolder_JTextField = null;
private SimpleJComboBox __ProcessSubfolders_JComboBox = null;
private SimpleJComboBox __UploadFiles_JComboBox = null;
private SimpleJComboBox __KeepFiles_JComboBox = null;
private SimpleJButton __browseDatasetIndexFile_JButton = null;

// HTML Inserts tab.
private JTextField __DatasetIndexHeadInsertTopFiles_JTextField = null;
private JTextField __DatasetIndexBodyInsertTopFiles_JTextField = null;
private JTextField __DatasetIndexBodyInsertBottomFiles_JTextField = null;
private SimpleJButton __browseDatasetIndexHeadInsertTopFiles_JButton = null;
private SimpleJButton __browseDatasetIndexBodyInsertTopFiles_JButton = null;
private SimpleJButton __browseDatasetIndexBodyInsertBottomFiles_JButton = null;

// Output tab.
//private SimpleJComboBox __OutputTableID_JComboBox = null;

// CloudFront tab:
// - this match the AwsS3 command
private SimpleJComboBox __InvalidateCloudFront_JComboBox = null;
private SimpleJComboBox __CloudFrontRegion_JComboBox = null;
private SimpleJComboBox __CloudFrontDistributionId_JComboBox = null;
private JTextField __CloudFrontComment_JTextField = null;
private JTextField __CloudFrontCallerReference_JTextField = null;
private SimpleJComboBox __CloudFrontWaitForCompletion_JComboBox = null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private AwsS3LandingPage_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private boolean ignoreEvents = false; // Ignore events when initializing, to avoid infinite loop.
//private JFrame __parent = null;

// AWS session used to interact with AWS:
// - will be null until the profile is set, which will happen when refresh() is called once
private AwsSession awsSession = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
public AwsS3LandingPage_JDialog ( JFrame parent, AwsS3LandingPage_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	String routine = getClass().getSimpleName() + ".actionPeformed";
	if ( this.ignoreEvents ) {
        return; // Startup.
    }

	Object o = event.getSource();

	if ( o == __browseDatasetIndexFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index File");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				try {
					__DatasetIndexFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseDatasetIndexHeadInsertTopFiles_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index <head> (top) File");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				try {
					String existing = __DatasetIndexHeadInsertTopFiles_JTextField.getText().trim();
					if ( existing.isEmpty() ) {
						// Just set.
						__DatasetIndexHeadInsertTopFiles_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
					}
					else {
						// Append.
						__DatasetIndexHeadInsertTopFiles_JTextField.setText(existing + "," +
							IOUtil.toRelativePath(__working_dir, path));
					}
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseDatasetIndexBodyInsertTopFiles_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index <body> (top) file");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				String existing =__DatasetIndexBodyInsertTopFiles_JTextField.getText().trim();
				try {
					if ( existing.isEmpty() ) {
						// Just set.
						__DatasetIndexBodyInsertTopFiles_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
					}
					else {
						// Append to the existing text.
						__DatasetIndexBodyInsertTopFiles_JTextField.setText(existing + "," +
							IOUtil.toRelativePath(__working_dir, path));
					}
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseDatasetIndexBodyInsertBottomFiles_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index <body> (bottom) file");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				String existing =__DatasetIndexBodyInsertBottomFiles_JTextField.getText().trim();
				try {
					if ( existing.isEmpty() ) {
						// Just set.
						__DatasetIndexBodyInsertBottomFiles_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
					}
					else {
						// Append to the existing text.
						__DatasetIndexBodyInsertBottomFiles_JTextField.setText(existing + "," +
							IOUtil.toRelativePath(__working_dir, path));
					}
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
    else if ( o == this.__browseS3_JButton ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
    	//boolean modal = true;
    	//boolean modal = false;
    	try {
    		String title = null;
    		Message.printStatus(2, routine, "Calling launchBrowser with session.profile=" + awsSession.getProfile()
    			+ " region=" + getSelectedRegion(true));
    		// Launch the stand-alone browser program:
    		// - authentication is via the AwsSession
    		// - use the default region if none is selected
    		String bucket = null;
    		S3Browser_App.launchBrowser ( title, awsSession, getSelectedRegion(true), bucket );
    	}
    	catch ( Exception e ) {
    		// Should not happen.
    	}
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AwsS3", PluginMeta.documentationRootUrl());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __pathDatasetIndexFile_JButton ) {
        if ( __pathDatasetIndexFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __DatasetIndexFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__DatasetIndexFile_JTextField.getText() ) );
        }
        else if ( __pathDatasetIndexFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __DatasetIndexFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __DatasetIndexFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathDatasetIndexHeadInsertTopFiles_JButton ) {
    	// Process all files in the parameter the same way.
        String [] parts = __pathDatasetIndexHeadInsertTopFiles_JButton.getText().split(",");
        StringBuilder files = new StringBuilder();
        for ( String part: parts ) {
        	part = part.trim();
        	if ( files.length() > 0 ) {
        		files.append(",");
        	}
        	if ( __pathDatasetIndexHeadInsertTopFiles_JButton.getText().equals(__AddWorkingDirectory) ) {
            	files.append (IOUtil.toAbsolutePath(__working_dir, part) );
        	}
        	else if ( __pathDatasetIndexHeadInsertTopFiles_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            	try {
                	files.append( IOUtil.toRelativePath ( __working_dir, part ) );
            	}
            	catch ( Exception e ) {
                	Message.printWarning ( 1, routine,
                	"Error converting index <head> file name to relative path: " + part );
            	}
        	}
        }
       	__DatasetIndexHeadInsertTopFiles_JTextField.setText ( files.toString() );
        refresh ();
    }
    else if ( o == __pathDatasetIndexBodyInsertBottomFiles_JButton ) {
    	// Process all files in the parameter the same way.
        String [] parts = __pathDatasetIndexHeadInsertTopFiles_JButton.getText().split(",");
        StringBuilder files = new StringBuilder();
        for ( String part: parts ) {
        	part = part.trim();
        	if ( files.length() > 0 ) {
        		files.append(",");
        	}
        	if ( __pathDatasetIndexBodyInsertBottomFiles_JButton.getText().equals(__AddWorkingDirectory) ) {
            	files.append (IOUtil.toAbsolutePath(__working_dir, part ) );
        	}
        	else if ( __pathDatasetIndexBodyInsertBottomFiles_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            	try {
                	files.append ( IOUtil.toRelativePath ( __working_dir, part ) );
            	}
            	catch ( Exception e ) {
                	Message.printWarning ( 1, routine,
                	"Error converting index <body> (bottom) file name to relative path." );
            	}
        	}
        }
       	__DatasetIndexBodyInsertBottomFiles_JTextField.setText ( files.toString() );
        refresh ();
    }
    else if ( o == __pathDatasetIndexBodyInsertTopFiles_JButton ) {
    	// Process all files in the parameter the same way.
        String [] parts = __pathDatasetIndexHeadInsertTopFiles_JButton.getText().split(",");
        StringBuilder files = new StringBuilder();
        for ( String part: parts ) {
        	part = part.trim();
        	if ( files.length() > 0 ) {
        		files.append(",");
        	}
        	if ( __pathDatasetIndexBodyInsertTopFiles_JButton.getText().equals(__AddWorkingDirectory) ) {
            	 files.append ( IOUtil.toAbsolutePath(__working_dir, part) );
        	}
        	else if ( __pathDatasetIndexBodyInsertTopFiles_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            	try {
                	files.append ( IOUtil.toRelativePath ( __working_dir, part ) );
            	}
            	catch ( Exception e ) {
                	Message.printWarning ( 1, routine,
                	"Error converting index <body> (top) file name to relative path." );
            	}
        	}
        }
       	__DatasetIndexBodyInsertTopFiles_JTextField.setText ( files.toString() );
        refresh ();
    }
	else {
		// Choices.
		refresh();
	}
}

/**
Check the input. If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String StartingFolder = __StartingFolder_JTextField.getText().trim();
	String ProcessSubfolders = __ProcessSubfolders_JComboBox.getSelected();
	String DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	String DatasetIndexHeadInsertTopFiles = __DatasetIndexHeadInsertTopFiles_JTextField.getText().trim();
	String DatasetIndexBodyInsertTopFiles = __DatasetIndexBodyInsertTopFiles_JTextField.getText().trim();
	String DatasetIndexBodyInsertBottomFiles = __DatasetIndexBodyInsertBottomFiles_JTextField.getText().trim();
	String UploadFiles = __UploadFiles_JComboBox.getSelected();
	//String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String KeepFiles = __KeepFiles_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	// CloudFront.
    String InvalidateCloudFront = __InvalidateCloudFront_JComboBox.getSelected();
	String CloudFrontRegion = getSelectedCloudFrontRegion();
	String CloudFrontDistributionId = __CloudFrontDistributionId_JComboBox.getSelected();
	String CloudFrontComment = __CloudFrontComment_JTextField.getText().trim();
	String CloudFrontCallerReference = __CloudFrontCallerReference_JTextField.getText().trim();
    String CloudFrontWaitForCompletion = __CloudFrontWaitForCompletion_JComboBox.getSelected();
	__error_wait = false;
	if ( Profile.length() > 0 ) {
		props.set ( "Profile", Profile );
	}
	if ( Region.length() > 0 ) {
		props.set ( "Region", Region );
	}
	if ( (Bucket != null) && !Bucket.isEmpty() ) {
		props.set ( "Bucket", Bucket );
	}
	if ( (StartingFolder != null) && !StartingFolder.isEmpty() ) {
		props.set ( "StartingFolder", StartingFolder );
	}
	if ( (ProcessSubfolders != null) && !ProcessSubfolders.isEmpty() ) {
		props.set ( "ProcessSubfolders", ProcessSubfolders );
	}
    if ( DatasetIndexFile.length() > 0 ) {
        props.set ( "DatasetIndexFile", DatasetIndexFile );
    }
    if ( DatasetIndexHeadInsertTopFiles.length() > 0 ) {
        props.set ( "DatasetIndexHeadInsertTopFiles", DatasetIndexHeadInsertTopFiles );
    }
    if ( DatasetIndexHeadInsertTopFiles.length() > 0 ) {
        props.set ( "DatasetIndexBodyInsertTopFiles", DatasetIndexBodyInsertTopFiles );
    }
    if ( DatasetIndexBodyInsertBottomFiles.length() > 0 ) {
        props.set ( "DatasetIndexBodyInsertBottomFiles", DatasetIndexBodyInsertBottomFiles );
    }
    /*
    if ( OutputTableID.length() > 0 ) {
        props.set ( "OutputTableID", OutputTableID );
    }
    */
	if ( KeepFiles.length() > 0 ) {
		props.set ( "KeepFiles", KeepFiles );
	}
	if ( UploadFiles.length() > 0 ) {
		props.set ( "UploadFiles", UploadFiles );
	}
	/*
	if ( IfInputNotFound.length() > 0 ) {
		props.set ( "IfInputNotFound", IfInputNotFound );
	}
	*/
    // CloudFront.
	if ( (InvalidateCloudFront != null) && !InvalidateCloudFront.isEmpty() ) {
		props.set ( "InvalidateCloudFront", InvalidateCloudFront );
	}
	if ( (CloudFrontRegion != null) && !CloudFrontRegion.isEmpty() ) {
		props.set ( "CloudFrontRegion", CloudFrontRegion );
	}
	if ( (CloudFrontDistributionId != null) && !CloudFrontDistributionId.isEmpty() ) {
		props.set ( "CloudFrontDistributionId", CloudFrontDistributionId );
	}
	if ( (CloudFrontComment != null) && !CloudFrontComment.isEmpty() ) {
		props.set ( "CloudFrontComment", CloudFrontComment );
	}
	if ( (CloudFrontCallerReference != null) && !CloudFrontCallerReference.isEmpty() ) {
		props.set ( "CloudFrontCallerReference", CloudFrontCallerReference );
	}
	if ( (CloudFrontWaitForCompletion != null) && !CloudFrontWaitForCompletion.isEmpty() ) {
		props.set ( "CloudFrontWaitForCompletion", CloudFrontWaitForCompletion );
	}
	try {
		// This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String StartingFolder = __StartingFolder_JTextField.getText().trim();
	String ProcessSubfolders = __ProcessSubfolders_JComboBox.getSelected();
	String DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	String DatasetIndexHeadInsertTopFiles = __DatasetIndexHeadInsertTopFiles_JTextField.getText().trim();
	String DatasetIndexBodyInsertTopFiles = __DatasetIndexBodyInsertTopFiles_JTextField.getText().trim();
	String DatasetIndexBodyInsertBottomFiles = __DatasetIndexBodyInsertBottomFiles_JTextField.getText().trim();
	String UploadFiles = __UploadFiles_JComboBox.getSelected();
	//String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String KeepFiles = __KeepFiles_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	// CloudFront
    String InvalidateCloudFront = __InvalidateCloudFront_JComboBox.getSelected();
	String CloudFrontRegion = getSelectedCloudFrontRegion();
	String CloudFrontDistributionId = __CloudFrontDistributionId_JComboBox.getSelected();
	String CloudFrontComment = __CloudFrontComment_JTextField.getText().trim();
	String CloudFrontCallerReference = __CloudFrontCallerReference_JTextField.getText().trim();
    String CloudFrontWaitForCompletion = __CloudFrontWaitForCompletion_JComboBox.getSelected();
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	__command.setCommandParameter ( "Bucket", Bucket );
	__command.setCommandParameter ( "StartingFolder", StartingFolder );
	__command.setCommandParameter ( "ProcessSubfolders", ProcessSubfolders );
	__command.setCommandParameter ( "DatasetIndexFile", DatasetIndexFile );
	__command.setCommandParameter ( "DatasetIndexHeadInsertTopFiles", DatasetIndexHeadInsertTopFiles );
	__command.setCommandParameter ( "DatasetIndexBodyInsertTopFiles", DatasetIndexBodyInsertTopFiles );
	__command.setCommandParameter ( "DatasetIndexBodyInsertBottomFiles", DatasetIndexBodyInsertBottomFiles );
	__command.setCommandParameter ( "UploadFiles", UploadFiles );
	//__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "KeepFiles", KeepFiles );
	//__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
	// CloudFront.
	__command.setCommandParameter ( "InvalidateCloudFront", InvalidateCloudFront );
	__command.setCommandParameter ( "CloudFrontRegion", CloudFrontRegion );
	__command.setCommandParameter ( "CloudFrontDistributionId", CloudFrontDistributionId );
	__command.setCommandParameter ( "CloudFrontComment", CloudFrontComment );
	__command.setCommandParameter ( "CloudFrontCallerReference", CloudFrontCallerReference );
	__command.setCommandParameter ( "CloudFrontWaitForCompletion", CloudFrontWaitForCompletion );
}

/**
Return the selected CloudFront region, omitting the trailing description.
The default is NOT returned if messing.
@return the selected region, omitting the trailing description.
*/
private String getSelectedCloudFrontRegion() {
	// Do not return the default if region is missing.
	return getSelectedCloudFrontRegion ( false );
}

/**
Return the selected CloudFront region, omitting the trailing description.
@param returnDefault if true, return the default region if the UI has blank
@return the selected region, omitting the trailing description.
*/
private String getSelectedCloudFrontRegion( boolean returnDefault ) {
    if ( __CloudFrontRegion_JComboBox == null ) {
    	// Region combobox is null, typically at UI startup.
    	if ( returnDefault ) {
    		// Return the (default) S3 region.
    		return getSelectedRegion(true);
    	}
    	else {
    		// Can only return null.
    		return null;
    	}
    }
    // The combo box is not null so can get the value, but may be null or an empty string.
    String region = __CloudFrontRegion_JComboBox.getSelected();
    if ( (region == null) || region.isEmpty() ) {
    	// No region is selected.
    	if ( returnDefault ) {
    		// Return the (default) S3 region.
    		return getSelectedRegion(true);
    	}
    	else {
    		// Return the selection even if null or blank.
    		return region;
    	}
    }
    // Have selected region:
	// - parse the ID, ignoring the description.
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
Return the selected profile.
The default is NOT returned if messing.
@return the selected region, omitting the trailing description.
*/
private String getSelectedProfile() {
	// Do not return the default if profile is missing.
	return getSelectedProfile ( false );
}

/**
Return the selected profile.
@param returnDefault if true, return the default profile if the UI has blank
@return the selected region.
*/
private String getSelectedProfile( boolean returnDefault ) {
	if ( (this.__Profile_JComboBox == null) || this.__Profile_JComboBox.getSelected().isEmpty() ) {
		// Profile choice is null or empty.
		if ( returnDefault ) {
			// Get the default profile from the toolkit.
			return AwsToolkit.getInstance().getDefaultProfile();
		}
		else {
			// Don't return the default.
			if ( this.__Profile_JComboBox == null ) {
				return null;
			}
			else {
				return this.__Profile_JComboBox.getSelected();
			}
		}
	}
	else {
		// Have a profile.
		return this.__Profile_JComboBox.getSelected();
	}
}

/**
Return the selected region, omitting the trailing description.
The default is NOT returned if messing.
@return the selected region, omitting the trailing description.
*/
private String getSelectedRegion() {
	// Do not return the default if region is missing.
	return getSelectedRegion ( false );
}

/**
Return the selected region, omitting the trailing description.
@param returnDefault if true, return the default region if the UI has blank
@return the selected region, omitting the trailing description.
*/
private String getSelectedRegion( boolean returnDefault ) {
    if ( __Region_JComboBox == null ) {
    	// Region combobox is null, typically at UI startup.
    	if ( returnDefault ) {
    		// Return the default region.
    		return AwsToolkit.getInstance().getDefaultRegion(getSelectedProfile(true));
    	}
    	else {
    		// Can only return null.
    		return null;
    	}
    }
    // The combo box is not null so can get the value, but may be null or an empty string.
    String region = __Region_JComboBox.getSelected();
    if ( (region == null) || region.isEmpty() ) {
    	// No region is selected.
    	if ( returnDefault ) {
    		// Return the default region.
    		return AwsToolkit.getInstance().getDefaultRegion(getSelectedProfile(true));
    	}
    	else {
    		// Return the selection even if null or blank.
    		return region;
    	}
    }
    // Have selected region:
	// - parse the ID, ignoring the description.
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
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
private void initialize ( JFrame parent, AwsS3LandingPage_Command command, List<String> tableIDChoices ) {
	this.__command = command;
	//this.__parent = parent;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"This command creates a landing page for one or more datasets published using AWS S3 storage." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Use multiple commands as appropriate to process a single or multiple datasets." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("The output landing page can be an 'index.html' (HTML) or 'index.md' (Markdown) file." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Datasets must each have a 'dataset.json' file to provide dataset metadata." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Filenames can use the notation ${Property} to use global processor properties." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the file names are relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

   	// Tabbed pane for the tabbed panels.
    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'AWS S3' parameters:
    // - parameters to specify S3 information
    int yS3 = -1;
    JPanel s3_JPanel = new JPanel();
    s3_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "AWS S3", s3_JPanel );

    JGUIUtil.addComponent(s3_JPanel, new JLabel (
    	"These parameters control how the AWS session is authenticated using AWS the command line interface (CLI) configuration." ),
		0, ++yS3, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(s3_JPanel, new JLabel (
    	"The AWS region and S3 bucket indicate where S3 files are stored."),
		0, ++yS3, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(s3_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yS3, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Profile:"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Profile_JComboBox = new SimpleJComboBox ( false );
	__Profile_JComboBox.setToolTipText("AWS user profile to use for authentication (see user's .aws/config file).");
	List<String> profileChoices = AwsToolkit.getInstance().getProfiles();
	// Add blank for default.
	Collections.sort(profileChoices);
	profileChoices.add(0,"");
	__Profile_JComboBox.setData(profileChoices);
	__Profile_JComboBox.select ( 0 );
	// Set the the AWS session here so other choices can display something.
	this.awsSession = new AwsSession(__Profile_JComboBox.getSelected());
	__Profile_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __Profile_JComboBox,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(s3_JPanel, new JLabel("Optional - profile for authentication (default=see below)."),
		3, yS3, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Profile (default):"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProfileDefault_JTextField = new JTextField ( 20 );
	__ProfileDefault_JTextField.setToolTipText("Default profile determined from user's .aws/config file).");
	__ProfileDefault_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __ProfileDefault_JTextField,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __ProfileDefaultNote_JLabel = new JLabel("From: " + AwsToolkit.getInstance().getAwsUserConfigFile());
    JGUIUtil.addComponent(s3_JPanel, __ProfileDefaultNote_JLabel,
		3, yS3, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__ProfileDefault_JTextField.setEditable(false);

    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Region:"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Region_JComboBox = new SimpleJComboBox ( false );
	__Region_JComboBox.setToolTipText("AWS region to run service.");
	List<Region> regions = Region.regions();
	List<String> regionChoices = new ArrayList<>();
	for ( Region region : regions ) {
		RegionMetadata meta = region.metadata();
		if ( meta == null ) {
			regionChoices.add ( region.id() );
		}
		else {
			regionChoices.add ( region.id() + " - " + region.metadata().description());
		}
	}
	Collections.sort(regionChoices);
	regionChoices.add(0,""); // Default - region is not specified (get from user's ~/.aws/config file)
	__Region_JComboBox.setData(regionChoices);
	__Region_JComboBox.select ( 0 );
	__Region_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __Region_JComboBox,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(s3_JPanel, new JLabel("Optional - AWS region (default=see below)."),
		3, yS3, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Region (default):"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RegionDefault_JTextField = new JTextField ( 20 );
	__RegionDefault_JTextField.setToolTipText("Default region for profile determined from user's .aws/config file).");
	__RegionDefault_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __RegionDefault_JTextField,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __RegionDefaultNote_JLabel = new JLabel("From: " + AwsToolkit.getInstance().getAwsUserConfigFile() );
    JGUIUtil.addComponent(s3_JPanel, __RegionDefaultNote_JLabel,
		3, yS3, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__RegionDefault_JTextField.setEditable(false);

    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Bucket:"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bucket_JComboBox = new SimpleJComboBox ( false );
	__Bucket_JComboBox.setToolTipText("AWS S3 bucket.");
	// Choices will be populated when refreshed, based on profile.
	__Bucket_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __Bucket_JComboBox,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(s3_JPanel, new JLabel( "Required - S3 bucket."),
		3, yS3, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /* TODO smalers 2023-05-08 need to finish.
    JGUIUtil.addComponent(s3_JPanel, new JLabel ( "Bucket has root object (/)?:"),
		0, ++yS3, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__BucketHasRootObject_JTextField = new JTextField ( 10 );
	__BucketHasRootObject_JTextField.setToolTipText("Does the bucket have a root (/) object (key)?");
	__BucketHasRootObject_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(s3_JPanel, __BucketHasRootObject_JTextField,
		1, yS3, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__BucketHasRootObject_JTextField.setEditable(false);
	*/

    // Panel for 'Dataset' parameters:
    // - controls the index file created for the dataset
    int yDataset = -1;
    JPanel dataset_JPanel = new JPanel();
    dataset_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Dataset", dataset_JPanel );

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Use the following parameters to control creation of the dataset index (landing) page."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel (
    	"Dataset index (landing) pages will be created for all found S3 'dataset.json' files."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel (
    	"The index file is 'index.html' for HTML or 'index.md' for markdown, optionally with a leading path to the local file."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel (
    	"A dataset index file with '.html' extension can use optional <head>, <body>, and <footer> HTML files as input (see the 'HTML Inserts' tab)."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel (
    	"If subfolders are being processed, the index file can be specified as 'temp.html' or 'temp.md' to automatically generate multiple landing pages."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Dataset index file:" ),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexFile_JTextField = new JTextField ( 50 );
    __DatasetIndexFile_JTextField.setToolTipText(
    	"Dataset 'index.html' or 'index.md' file to create. Use 'temp.html' or 'temp.md' to automatically use a temporary file. Can use ${Property} notation.");
    __DatasetIndexFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexFile_JPanel = new JPanel();
	DatasetIndexFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexFile_JPanel, __DatasetIndexFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexFile_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexFile_JPanel, __browseDatasetIndexFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexFile_JPanel, __pathDatasetIndexFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(dataset_JPanel, DatasetIndexFile_JPanel,
		1, yDataset, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Starting S3 folder:"),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StartingFolder_JTextField = new JTextField ( "", 50 );
    __StartingFolder_JTextField.setToolTipText("Starting S3 folder to search for 'dataset.json', ending with /");
    __StartingFolder_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(dataset_JPanel, __StartingFolder_JTextField,
        1, yDataset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Optional - starting S3 folder (default=/)."),
        3, yDataset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Process subfolders?:"),
		0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProcessSubfolders_JComboBox = new SimpleJComboBox ( false );
	__ProcessSubfolders_JComboBox.setToolTipText("Whether to process all datasets in a directory (folder) and subdirectories.");
	List<String> subChoices = new ArrayList<>();
	subChoices.add("");
	subChoices.add(__command._False);
	subChoices.add(__command._True);
	__ProcessSubfolders_JComboBox.setData(subChoices);
	__ProcessSubfolders_JComboBox.select ( 0 );
	__ProcessSubfolders_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(dataset_JPanel, __ProcessSubfolders_JComboBox,
		1, yDataset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel("Optional - process subfolders (default=" + __command._False + ")."),
		3, yDataset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Keep files?:"),
		0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__KeepFiles_JComboBox = new SimpleJComboBox ( false );
	__KeepFiles_JComboBox.setToolTipText("Indicate whether to keep temporary files, useful for troubleshooting.");
	List<String> keepFilesChoices = new ArrayList<>();
	keepFilesChoices.add ( "" );	// Default.
	keepFilesChoices.add ( __command._False );
	keepFilesChoices.add ( __command._True );
	__KeepFiles_JComboBox.setData(keepFilesChoices);
	__KeepFiles_JComboBox.select ( 0 );
	__KeepFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(dataset_JPanel, __KeepFiles_JComboBox,
		1, yDataset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel(
		"Optional - keep temporary files? (default=" + __command._False + ")."),
		3, yDataset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Upload dataset files?:"),
		0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__UploadFiles_JComboBox = new SimpleJComboBox ( false );
	__UploadFiles_JComboBox.setToolTipText("Indicate whether to upload dataset index files, to review before publishing.");
	List<String> uploadFilesChoices = new ArrayList<>();
	uploadFilesChoices.add ( "" );	// Default.
	uploadFilesChoices.add ( __command._False );
	uploadFilesChoices.add ( __command._True );
	__UploadFiles_JComboBox.setData(uploadFilesChoices);
	__UploadFiles_JComboBox.select ( 0 );
	__UploadFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(dataset_JPanel, __UploadFiles_JComboBox,
		1, yDataset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel(
		"Optional - upload dataset files? (default=" + __command._False + ")."),
		3, yDataset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'HTML Inserts' parameters:
    // - indicates inserts if an index.html file is being created
    int yHtml = -1;
    JPanel html_JPanel = new JPanel();
    html_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "HTML Inserts", html_JPanel );

    JGUIUtil.addComponent(html_JPanel, new JLabel (
    	"These parameters can be used when creating an 'index.html' (HTML) landing page."),
		0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(html_JPanel, new JLabel (
    	"HTML inserts are local files that are inserted into the 'index.html' file (before uploading to S3) to provide standard content such as web page \"skin\"."),
		0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(html_JPanel, new JLabel (
    	"If a 'dataset.png' file is found in the same S3 location as 'dataset.json', the image will be used in the landing page."),
		0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(html_JPanel, new JLabel (
    	"The index file is automatically generated from 'dataset.json' metadata, 'dataset.png' image file, and insert files."),
		0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(html_JPanel, new JLabel (
    	"See the command documentation for additional information and examples."),
		0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(html_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yHtml, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(html_JPanel, new JLabel ("Dataset index <head> insert (top) file(s):" ),
        0, ++yHtml, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexHeadInsertTopFiles_JTextField = new JTextField ( 50 );
    __DatasetIndexHeadInsertTopFiles_JTextField.setToolTipText(
    	"HTML file(s), separated by commas, to insert at the top of <head>.");
    __DatasetIndexHeadInsertTopFiles_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexHeadInsertTopFiles_JPanel = new JPanel();
	DatasetIndexHeadInsertTopFiles_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexHeadInsertTopFiles_JPanel, __DatasetIndexHeadInsertTopFiles_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexHeadInsertTopFiles_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexHeadInsertTopFiles_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexHeadInsertTopFiles_JPanel, __browseDatasetIndexHeadInsertTopFiles_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexHeadInsertTopFiles_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexHeadInsertTopFiles_JPanel, __pathDatasetIndexHeadInsertTopFiles_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(html_JPanel, DatasetIndexHeadInsertTopFiles_JPanel,
		1, yHtml, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(html_JPanel, new JLabel ("Dataset index <body> insert (top) file(s):" ),
        0, ++yHtml, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexBodyInsertTopFiles_JTextField = new JTextField ( 50 );
    __DatasetIndexBodyInsertTopFiles_JTextField.setToolTipText(
    	"HTML file(s), separated by commas, to insert at the top of <body>.");
    __DatasetIndexBodyInsertTopFiles_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexBodyInsertTopFiles_JPanel = new JPanel();
	DatasetIndexBodyInsertTopFiles_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexBodyInsertTopFiles_JPanel, __DatasetIndexBodyInsertTopFiles_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexBodyInsertTopFiles_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexBodyInsertTopFiles_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexBodyInsertTopFiles_JPanel, __browseDatasetIndexBodyInsertTopFiles_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexBodyInsertTopFiles_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexBodyInsertTopFiles_JPanel, __pathDatasetIndexBodyInsertTopFiles_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(html_JPanel, DatasetIndexBodyInsertTopFiles_JPanel,
		1, yHtml, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(html_JPanel, new JLabel ("Dataset index <body> insert (bottom) file(s):" ),
        0, ++yHtml, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexBodyInsertBottomFiles_JTextField = new JTextField ( 50 );
    __DatasetIndexBodyInsertBottomFiles_JTextField.setToolTipText(
    	"HTML file(s), separated by commas, to insert at the bottom of the </body>.");
    __DatasetIndexBodyInsertBottomFiles_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexBodyInsertBottomFiles_JPanel = new JPanel();
	DatasetIndexBodyInsertBottomFiles_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexBodyInsertBottomFiles_JPanel, __DatasetIndexBodyInsertBottomFiles_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexBodyInsertBottomFiles_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexBodyInsertBottomFiles_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexBodyInsertBottomFiles_JPanel, __browseDatasetIndexBodyInsertBottomFiles_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexBodyInsertBottomFiles_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexBodyInsertBottomFiles_JPanel, __pathDatasetIndexBodyInsertBottomFiles_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(html_JPanel, DatasetIndexBodyInsertBottomFiles_JPanel,
		1, yHtml, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'Markdown Inserts' parameters:
    // - indicates inserts if an index.md file is being created
    int yMarkdown = -1;
    JPanel markdown_JPanel = new JPanel();
    markdown_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Markdown Inserts", markdown_JPanel );

    JGUIUtil.addComponent(markdown_JPanel, new JLabel (
    	"These parameters can be used when creating an 'index.md' (Markdown) landing page (currently no additional parameters are implemented)."),
		0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(markdown_JPanel, new JLabel (
    	"If a 'dataset-details.md' file is found in the same S3 location as 'dataset.json', the file will automatically be inserted."),
		0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(markdown_JPanel, new JLabel (
    	"If a 'dataset.png' file is found in the same S3 location as 'dataset.json', the image will be used in the landing page."),
		0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(markdown_JPanel, new JLabel (
    	"The index file is automatically generated from inserts and metadata."),
		0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(markdown_JPanel, new JLabel (
    	"See the command documentation for additional information and examples."),
		0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(markdown_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yMarkdown, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for output.
    /*
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel ("The following are used for commands that generate bucket and bucket object lists."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("The table contains a list of datasets in the catalog (if a catalog is generated)."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Temporary files can be kept to facilitate troubleshooting."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: 'csv'"),
	//	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    	*/

    /*
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output Table ID:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __OutputTableID_JComboBox.setToolTipText("Table for output, available for some commands");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __OutputTableID_JComboBox.setData ( tableIDChoices );
    __OutputTableID_JComboBox.addItemListener ( this );
    __OutputTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__OutputTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(output_JPanel, __OutputTableID_JComboBox,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - table for output."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If input not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfInputNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default.
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfInputNotFound_JComboBox.setData(notFoundChoices);
	__IfInputNotFound_JComboBox.select ( 0 );
	__IfInputNotFound_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfInputNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if input file is not found (default=" + __command._Warn + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    // Panel for 'CloudFront' parameters:
    // - controls the index file created for the dataset
    int yCloudFront = -1;
    JPanel cf_JPanel = new JPanel();
    cf_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "CloudFront", cf_JPanel );

    JGUIUtil.addComponent(cf_JPanel, new JLabel ("Use the following parameters to control CloudFront invalidations."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("Files must be invalidated to be visible with small latency on the CloudFront-fronted website."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("All uploaded files will be included in the invalidation path list."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("The 'aws-global' region may be required for CloudFront distributions."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel (
    	"Specify a CloudFront distribution using the distribution ID or comment (description) pattern (e.g., *some.domain.org*)."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "Invalidate CloudFront paths?:"),
		0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InvalidateCloudFront_JComboBox = new SimpleJComboBox ( false );
	__InvalidateCloudFront_JComboBox.setToolTipText("Invalidate CloudFront paths?");
	List<String> invalidateChoices = new ArrayList<>();
	invalidateChoices.add ( "" );	// Default.
	invalidateChoices.add ( __command._False );
	invalidateChoices.add ( __command._True );
	__InvalidateCloudFront_JComboBox.setData(invalidateChoices);
	__InvalidateCloudFront_JComboBox.select ( 0 );
	__InvalidateCloudFront_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(cf_JPanel, __InvalidateCloudFront_JComboBox,
		1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel(
		"Optional - invalidate CloudFront paths? (default=" + __command._False + ")."),
		3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "CloudFront region:"),
		0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CloudFrontRegion_JComboBox = new SimpleJComboBox ( false );
	__CloudFrontRegion_JComboBox.setToolTipText("AWS region that CloudFront server requests should be sent to.");
	List<Region> cfRegions = Region.regions();
	List<String> cfRegionChoices = new ArrayList<>();
	for ( Region cfRegion : cfRegions ) {
		RegionMetadata meta = cfRegion.metadata();
		if ( meta == null ) {
			cfRegionChoices.add ( cfRegion.id() );
		}
		else {
			cfRegionChoices.add ( cfRegion.id() + " - " + cfRegion.metadata().description());
		}
	}
	Collections.sort(cfRegionChoices);
	cfRegionChoices.add(0,""); // Default - region is not specified (get from user's ~/.aws/config file)
	__CloudFrontRegion_JComboBox.setData(cfRegionChoices);
	__CloudFrontRegion_JComboBox.select ( 0 );
	__CloudFrontRegion_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(cf_JPanel, __CloudFrontRegion_JComboBox,
		1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel("Optional - CloudFront AWS region (default=same as S3 region)."),
		3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "CloudFront distribution ID:"),
		0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CloudFrontDistributionId_JComboBox = new SimpleJComboBox ( false );
	__CloudFrontDistributionId_JComboBox.setToolTipText("AWS CloudFront distribution ID.");
	// Choices will be populated when refreshed, based on profile.
	__CloudFrontDistributionId_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(cf_JPanel, __CloudFrontDistributionId_JComboBox,
		1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel(
		"Optional - distribution ID (specify the distribution using ID)."),
		3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "CloudFront comment (description):"),
        0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CloudFrontComment_JTextField = new JTextField ( "", 30 );
    __CloudFrontComment_JTextField.setToolTipText("Distribution comment (description) to match, use * for wildcard, ${Property} can be used.");
    __CloudFrontComment_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(cf_JPanel, __CloudFrontComment_JTextField,
        1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "Optional - comment to match (specify the distribution comment, e.g., *sub.domain*)."),
        3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "Caller reference:"),
        0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CloudFrontCallerReference_JTextField = new JTextField ( "", 30 );
    __CloudFrontCallerReference_JTextField.setToolTipText("Unique identifier for invalidation, to avoid duplicates, ${Property} can be used.");
    __CloudFrontCallerReference_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(cf_JPanel, __CloudFrontCallerReference_JTextField,
        1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ( "Optional - caller reference (default=user and current time)."),
        3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cf_JPanel, new JLabel ("Wait for completion?:"),
        0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CloudFrontWaitForCompletion_JComboBox = new SimpleJComboBox ( false );
    __CloudFrontWaitForCompletion_JComboBox.add ( "" );
    __CloudFrontWaitForCompletion_JComboBox.add ( __command._False );
    __CloudFrontWaitForCompletion_JComboBox.add ( __command._True );
    __CloudFrontWaitForCompletion_JComboBox.addItemListener ( this );
    __CloudFrontWaitForCompletion_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(cf_JPanel, __CloudFrontWaitForCompletion_JComboBox,
        1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel (
        "Optional - wait for invalidation to complete (default=" + __command._True + ")."),
        3, yCloudFront, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");
	button_JPanel.add ( __browseS3_JButton = new SimpleJButton("Browse S3", this) );
	__browseS3_JButton.setToolTipText("Browse S3 files.  A separate application is started (may be a startup delay).");

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

    this.ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade.

	// Refresh the contents.
    refresh ();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable.
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e ) {
	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	Object o = e.getSource();
    if ( o == this.__Bucket_JComboBox ) {
        this.__bucketHasRootObject = AwsToolkit.getInstance().bucketHasRootObject( this.awsSession, getSelectedRegion(),
        	__Bucket_JComboBox.getSelected() );
	}
    else if ( o == this.__Profile_JComboBox ) {
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
	}
	else if ( o == this.__Region_JComboBox ) {
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
	}
	else if ( o == this.__CloudFrontRegion_JComboBox ) {
		populateCloudFrontDistributionIdChoices();
	}
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
	return __ok;
}

/**
 * Populate the CloudFront distribution ID choices based no other selections.
 */
private void populateCloudFrontDistributionIdChoices() {
	String routine = getClass().getSimpleName() + ".populateCloudFrontDistributionIdChoices";
	boolean debug = true;
	if ( awsSession == null ) {
		// Startup - can't populate the buckets.
		if ( debug ) {
			Message.printStatus(2, routine, "Startup - not populating the list of distributions." );
		}
		return;
	}
	else {
		if ( debug ) {
			Message.printStatus(2, routine, "Getting the list of distributions." );
		}
		// Get the list of regions.
		String region = getSelectedCloudFrontRegion();
		if ( (region == null) || region.isEmpty() ) {
			// Startup - can't populate the buckets.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is null - can't populate the list of distributions." );
			}
			return;
		}
		else {
			// Have a region.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is \"" + region + "\" - populating the list of CloudFront distributions." );
			}
			List<DistributionSummary> distributions = AwsToolkit.getInstance().getCloudFrontDistributions(awsSession, region);
			List<String> distributionIdChoices = new ArrayList<>();
			for ( DistributionSummary distribution : distributions ) {
				distributionIdChoices.add ( distribution.id() );
				if ( debug ) {
					Message.printStatus(2, routine, "Populated CloudFront distributions: " + distribution.comment() );
				}
			}
			Collections.sort(distributionIdChoices);
			// Add a blank because may specify using a comment or may not use invalidations with the command.
			distributionIdChoices.add(0,"");
			__CloudFrontDistributionId_JComboBox.setData(distributionIdChoices);
			if ( __CloudFrontDistributionId_JComboBox.getItemCount() > 0 ) {
				// Select the first bucket by default.
				__CloudFrontDistributionId_JComboBox.select ( 0 );
			}
		}
	}
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String Profile = "";
	String Region = "";
	String Bucket = "";
	String StartingFolder = "";
	String ProcessSubfolders = "";
	String DistributionId = "";
	String DatasetIndexFile = "";
	String DatasetIndexHeadInsertTopFiles = "";
	String DatasetIndexBodyInsertTopFiles = "";
	String DatasetIndexBodyInsertBottomFiles = "";
	String UploadFiles = "";
	//String OutputTableID = "";
	String KeepFiles = "";
	//String IfInputNotFound = "";
	// CloudFront.
	String InvalidateCloudFront = "";
	String CloudFrontRegion = "";
	String CloudFrontDistributionId = "";
	String CloudFrontComment = "";
	String CloudFrontCallerReference = "";
    String CloudFrontWaitForCompletion = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		Bucket = parameters.getValue ( "Bucket" );
		StartingFolder = parameters.getValue ( "StartingFolder" );
		ProcessSubfolders = parameters.getValue ( "ProcessSubfolders" );
		DistributionId = parameters.getValue ( "DistributionId" );
		DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" );
		DatasetIndexHeadInsertTopFiles = parameters.getValue ( "DatasetIndexHeadInsertTopFiles" );
		DatasetIndexBodyInsertTopFiles = parameters.getValue ( "DatasetIndexBodyInsertTopFiles" );
		DatasetIndexBodyInsertBottomFiles = parameters.getValue ( "DatasetIndexBodyInsertBottomFiles" );
		UploadFiles = parameters.getValue ( "UploadFiles" );
		//OutputTableID = parameters.getValue ( "OutputTableID" );
		KeepFiles = parameters.getValue ( "KeepFiles" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		// CloudFront
		InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
		CloudFrontRegion = parameters.getValue ( "CloudFrontRegion" );
		CloudFrontDistributionId = parameters.getValue ( "CloudFrontDistributionId" );
		CloudFrontComment = parameters.getValue ( "CloudFrontComment" );
		CloudFrontCallerReference = parameters.getValue ( "CloudFrontCallerReference" );
    	CloudFrontWaitForCompletion = parameters.getValue ( "CloudFrontWaitForCompletion" );
		if ( JGUIUtil.isSimpleJComboBoxItem(__Profile_JComboBox, Profile,JGUIUtil.NONE, null, null ) ) {
			__Profile_JComboBox.select ( Profile );
		}
		else {
            if ( (Profile == null) || Profile.equals("") ) {
				// New command...select the default.
				if ( __Profile_JComboBox.getItemCount() > 0 ) {
					__Profile_JComboBox.select ( 0 );
					// Also initialize an AWS session for interactions.
					this.awsSession = new AwsSession(__Profile_JComboBox.getSelected());
				}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Profile parameter \"" + Profile + "\".  Select a value or Cancel." );
			}
		}
		// The Region may or may not include a note following " - " so try to match with or without the note.
        int [] index = new int[1];
        //Message.printStatus(2,routine,"Checking to see if Region=\"" + Region + "\" is a choice.");
        // Choice values are similar to the following so need to parse by " - ", not just "-":
        // - assume no match and try to match with and without note
        boolean inputOk = false;
        if ( JGUIUtil.isSimpleJComboBoxItem(__Region_JComboBox, Region, JGUIUtil.CHECK_SUBSTRINGS, "seq: - ", 0, index, true ) ) {
            // Existing command so select the matching choice.
            //Message.printStatus(2,routine,"Region=\"" + Region + "\" was a choice, selecting index " + index[0] + "...");
            __Region_JComboBox.select(index[0]);
           	inputOk = true;
        }
        else {
            Message.printStatus(2,routine,"Region=\"" + Region + "\" was not a choice.");
            if ( (Region == null) || Region.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	inputOk = true;
            }
            else {
                // Bad user command.
            	inputOk = false;
            }
        }
        if ( !inputOk ) {
        	// Try matching without the note.
			if ( JGUIUtil.isSimpleJComboBoxItem(__Region_JComboBox, Region,JGUIUtil.NONE, null, null ) ) {
				__Region_JComboBox.select ( Region );
				inputOk = true;
			}
			else {
            	if ( (Region == null) || Region.equals("") ) {
					// New command...select the default.
            		if ( __Region_JComboBox.getItemCount() > 0 ) {
            			__Region_JComboBox.select ( 0 );
            			inputOk = true;
            		}
				}
				else {
					// Bad user command.
            		inputOk = false;
				}
			}
        }
        if ( !inputOk ) {
            Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "Region parameter \"" + Region + "\".  Select a\ndifferent value or Cancel." );
        }
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
		if ( JGUIUtil.isSimpleJComboBoxItem(__Bucket_JComboBox, Bucket,JGUIUtil.NONE, null, null ) ) {
			__Bucket_JComboBox.select ( Bucket );
		}
		else {
            if ( (Bucket == null) || Bucket.equals("") ) {
				// New command...select the default.
            	if ( __Bucket_JComboBox.getItemCount() > 0 ) {
            		__Bucket_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Bucket parameter \"" + Bucket + "\".  Select a value or Cancel." );
			}
		}
        if ( StartingFolder != null ) {
            __StartingFolder_JTextField.setText ( StartingFolder );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__ProcessSubfolders_JComboBox, ProcessSubfolders,JGUIUtil.NONE, null, null ) ) {
			__ProcessSubfolders_JComboBox.select ( ProcessSubfolders );
		}
		else {
            if ( (ProcessSubfolders == null) ||	ProcessSubfolders.equals("") ) {
				// New command...select the default.
				__ProcessSubfolders_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ProcessSubfolders parameter \"" + ProcessSubfolders + "\".  Select a value or Cancel." );
			}
		}
        if ( DatasetIndexFile != null ) {
            __DatasetIndexFile_JTextField.setText ( DatasetIndexFile );
        }
        if ( DatasetIndexHeadInsertTopFiles != null ) {
            __DatasetIndexHeadInsertTopFiles_JTextField.setText ( DatasetIndexHeadInsertTopFiles );
        }
        if ( DatasetIndexBodyInsertTopFiles != null ) {
            __DatasetIndexBodyInsertTopFiles_JTextField.setText ( DatasetIndexBodyInsertTopFiles );
        }
        if ( DatasetIndexBodyInsertBottomFiles != null ) {
            __DatasetIndexBodyInsertBottomFiles_JTextField.setText ( DatasetIndexBodyInsertBottomFiles );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__UploadFiles_JComboBox, UploadFiles,JGUIUtil.NONE, null, null ) ) {
			__UploadFiles_JComboBox.select ( UploadFiles );
		}
		else {
            if ( (UploadFiles == null) || UploadFiles.equals("") ) {
				// New command...select the default.
				__UploadFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"UploadFiles parameter \"" + UploadFiles + "\".  Select a value or Cancel." );
			}
		}
		/*
        if ( OutputTableID == null ) {
            // Select default.
            __OutputTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputTableID_JComboBox,OutputTableID, JGUIUtil.NONE, null, null ) ) {
                __OutputTableID_JComboBox.select ( OutputTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __OutputTableID_JComboBox.getItemCount() == 0 ) {
                    __OutputTableID_JComboBox.add(OutputTableID);
                }
                else {
                    __OutputTableID_JComboBox.insert(OutputTableID, 0);
                }
                __OutputTableID_JComboBox.select(0);
            }
        }
        */
		if ( JGUIUtil.isSimpleJComboBoxItem(__KeepFiles_JComboBox, KeepFiles,JGUIUtil.NONE, null, null ) ) {
			__KeepFiles_JComboBox.select ( KeepFiles );
		}
		else {
            if ( (KeepFiles == null) ||	KeepFiles.equals("") ) {
				// New command...select the default.
				__KeepFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"KeepFiles parameter \"" + KeepFiles + "\".  Select a value or Cancel." );
			}
		}
		/*
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfInputNotFound_JComboBox, IfInputNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfInputNotFound_JComboBox.select ( IfInputNotFound );
		}
		else {
            if ( (IfInputNotFound == null) ||	IfInputNotFound.equals("") ) {
				// New command...select the default.
				__IfInputNotFound_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfInputNotFound parameter \"" + IfInputNotFound + "\".  Select a value or Cancel." );
			}
		}
		*/
		// Set the tab to the input.
		__main_JTabbedPane.setSelectedIndex(0);
		if ( JGUIUtil.isSimpleJComboBoxItem(__InvalidateCloudFront_JComboBox, InvalidateCloudFront,JGUIUtil.NONE, null, null ) ) {
			__InvalidateCloudFront_JComboBox.select ( InvalidateCloudFront );
		}
		else {
            if ( (InvalidateCloudFront == null) ||	InvalidateCloudFront.equals("") ) {
				// New command...select the default.
				__InvalidateCloudFront_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"InvalidateCloudFront parameter \"" + InvalidateCloudFront + "\".  Select a value or Cancel." );
			}
		}
		// The CloudFrontRegion may or may not include a note following " - " so try to match with or without the note.
        //int [] index = new int[1];
        //Message.printStatus(2,routine,"Checking to see if CloudFrontRegion=\"" + CloudFrontRegion + "\" is a choice.");
        // Choice values are similar to the following so need to parse by " - ", not just "-":
        // - assume no match and try to match with and without note
        inputOk = false;
        if ( JGUIUtil.isSimpleJComboBoxItem(__CloudFrontRegion_JComboBox, CloudFrontRegion, JGUIUtil.CHECK_SUBSTRINGS, "seq: - ", 0, index, true ) ) {
            // Existing command so select the matching choice.
            //Message.printStatus(2,routine,"CloudFrontRegion=\"" + CloudFrontRegion + "\" was a choice, selecting index " + index[0] + "...");
            __CloudFrontRegion_JComboBox.select(index[0]);
           	inputOk = true;
        }
        else {
            Message.printStatus(2,routine,"CloudFrontRegion=\"" + CloudFrontRegion + "\" was not a choice.");
            if ( (CloudFrontRegion == null) || CloudFrontRegion.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	inputOk = true;
            }
            else {
                // Bad user command.
            	inputOk = false;
            }
        }
        if ( !inputOk ) {
        	// Try matching without the note.
			if ( JGUIUtil.isSimpleJComboBoxItem(__CloudFrontRegion_JComboBox, CloudFrontRegion,JGUIUtil.NONE, null, null ) ) {
				__CloudFrontRegion_JComboBox.select ( CloudFrontRegion );
				inputOk = true;
			}
			else {
            	if ( (CloudFrontRegion == null) || CloudFrontRegion.equals("") ) {
					// New command...select the default.
            		if ( __CloudFrontRegion_JComboBox.getItemCount() > 0 ) {
            			__CloudFrontRegion_JComboBox.select ( 0 );
            			inputOk = true;
            		}
				}
				else {
					// Bad user command.
            		inputOk = false;
				}
			}
        }
        if ( !inputOk ) {
            Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "CloudFrontRegion parameter \"" + CloudFrontRegion + "\".  Select a\ndifferent value or Cancel." );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__CloudFrontDistributionId_JComboBox, CloudFrontDistributionId,JGUIUtil.NONE, null, null ) ) {
			__CloudFrontDistributionId_JComboBox.select ( CloudFrontDistributionId );
		}
		else {
            if ( (CloudFrontDistributionId == null) || CloudFrontDistributionId.equals("") ) {
				// New command...select the default.
            	if ( __CloudFrontDistributionId_JComboBox.getItemCount() > 0 ) {
            		__CloudFrontDistributionId_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CloudFrontDistributionId parameter \"" + CloudFrontDistributionId + "\".  Select a value or Cancel." );
			}
		}
        if ( CloudFrontComment != null ) {
            __CloudFrontComment_JTextField.setText ( CloudFrontComment );
        }
        if ( CloudFrontCallerReference != null ) {
            __CloudFrontCallerReference_JTextField.setText ( CloudFrontCallerReference );
        }
        if ( CloudFrontWaitForCompletion == null ) {
            // Select default.
            if ( __CloudFrontWaitForCompletion_JComboBox.getItemCount() > 0 ) {
                __CloudFrontWaitForCompletion_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CloudFrontWaitForCompletion_JComboBox,CloudFrontWaitForCompletion, JGUIUtil.NONE, null, null ) ) {
                __CloudFrontWaitForCompletion_JComboBox.select ( CloudFrontWaitForCompletion );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid CloudFrontWaitForCompletion value \"" + CloudFrontWaitForCompletion +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	Profile = __Profile_JComboBox.getSelected();
	if ( Profile == null ) {
		Profile = "";
	}
	Region = getSelectedRegion();
	Bucket = __Bucket_JComboBox.getSelected();
	if ( Bucket == null ) {
		Bucket = "";
	}
	StartingFolder = __StartingFolder_JTextField.getText().trim();
	ProcessSubfolders = __ProcessSubfolders_JComboBox.getSelected();
	DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	DatasetIndexHeadInsertTopFiles = __DatasetIndexHeadInsertTopFiles_JTextField.getText().trim();
	DatasetIndexBodyInsertTopFiles = __DatasetIndexBodyInsertTopFiles_JTextField.getText().trim();
	DatasetIndexBodyInsertBottomFiles = __DatasetIndexBodyInsertBottomFiles_JTextField.getText().trim();
	UploadFiles = __UploadFiles_JComboBox.getSelected();
	//OutputTableID = __OutputTableID_JComboBox.getSelected();
	KeepFiles = __KeepFiles_JComboBox.getSelected();
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	// CloudFront
    InvalidateCloudFront = __InvalidateCloudFront_JComboBox.getSelected();
	CloudFrontRegion = getSelectedCloudFrontRegion();
	CloudFrontDistributionId = __CloudFrontDistributionId_JComboBox.getSelected();
	if ( CloudFrontDistributionId == null ) {
		CloudFrontDistributionId = "";
	}
	CloudFrontComment = __CloudFrontComment_JTextField.getText().trim();
	CloudFrontCallerReference = __CloudFrontCallerReference_JTextField.getText().trim();
    CloudFrontWaitForCompletion = __CloudFrontWaitForCompletion_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "Bucket=" + Bucket );
	props.add ( "StartingFolder=" + StartingFolder );
	props.add ( "ProcessSubfolders=" + ProcessSubfolders );
	props.add ( "DistributionId=" + DistributionId );
	props.add ( "DatasetIndexFile=" + DatasetIndexFile );
	props.add ( "DatasetIndexHeadInsertTopFiles=" + DatasetIndexHeadInsertTopFiles );
	props.add ( "DatasetIndexBodyInsertTopFiles=" + DatasetIndexBodyInsertTopFiles );
	props.add ( "DatasetIndexBodyInsertBottomFiles=" + DatasetIndexBodyInsertBottomFiles );
	props.add ( "UploadFiles=" + UploadFiles );
	//props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "KeepFiles=" + KeepFiles );
	//props.add ( "IfInputNotFound=" + IfInputNotFound );
	// CloudFront.
	props.add ( "InvalidateCloudFront=" + InvalidateCloudFront);
	props.add ( "CloudFrontRegion=" + CloudFrontRegion );
	props.add ( "CloudFrontDistributionId=" + CloudFrontDistributionId );
	props.add ( "CloudFrontComment=" + CloudFrontComment );
	props.add ( "CloudFrontCallerReference=" + CloudFrontCallerReference );
    props.add ( "CloudFrontWaitForCompletion=" + CloudFrontWaitForCompletion );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Set the default values as FYI.
	AwsToolkit.getInstance().uiPopulateProfileDefault(__ProfileDefault_JTextField, __ProfileDefaultNote_JLabel);
	AwsToolkit.getInstance().uiPopulateRegionDefault( __Profile_JComboBox.getSelected(), __RegionDefault_JTextField, __RegionDefaultNote_JLabel);
	// Indicate whether the bucket has a root object:
	// - value is set when the bucket is selected
	/*
	if ( this.__bucketHasRootObject == null ) {
		__BucketHasRootObject_JTextField.setText("");
	}
	else if ( this.__bucketHasRootObject ) {
		__BucketHasRootObject_JTextField.setText("Yes");
	}
	else {
		__BucketHasRootObject_JTextField.setText("No");
	}
	*/
	// Check the path and determine what the label on the path button should be.
    if ( __pathDatasetIndexFile_JButton != null ) {
		if ( (DatasetIndexFile != null) && !DatasetIndexFile.isEmpty() ) {
			__pathDatasetIndexFile_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexFile );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexFile_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexFile_JButton.setEnabled(false);
		}
    }
    if ( __pathDatasetIndexHeadInsertTopFiles_JButton != null ) {
		if ( (DatasetIndexHeadInsertTopFiles != null) && !DatasetIndexHeadInsertTopFiles.isEmpty() ) {
			__pathDatasetIndexHeadInsertTopFiles_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexHeadInsertTopFiles );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexHeadInsertTopFiles_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexHeadInsertTopFiles_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexHeadInsertTopFiles_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexHeadInsertTopFiles_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexHeadInsertTopFiles_JButton.setEnabled(false);
		}
    }
    if ( __pathDatasetIndexBodyInsertTopFiles_JButton != null ) {
		if ( (DatasetIndexBodyInsertTopFiles != null) && !DatasetIndexBodyInsertTopFiles.isEmpty() ) {
			__pathDatasetIndexBodyInsertTopFiles_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexBodyInsertTopFiles );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexBodyInsertTopFiles_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexBodyInsertTopFiles_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexBodyInsertTopFiles_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexBodyInsertTopFiles_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexBodyInsertTopFiles_JButton.setEnabled(false);
		}
    }
    if ( __pathDatasetIndexBodyInsertBottomFiles_JButton != null ) {
		if ( (DatasetIndexBodyInsertBottomFiles != null) && !DatasetIndexBodyInsertBottomFiles.isEmpty() ) {
			__pathDatasetIndexBodyInsertBottomFiles_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexBodyInsertBottomFiles );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexBodyInsertBottomFiles_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexBodyInsertBottomFiles_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexBodyInsertBottomFiles_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexBodyInsertBottomFiles_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexBodyInsertBottomFiles_JButton.setEnabled(false);
		}
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled. If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
 * Handle JTabbedPane changes.
 */
public void stateChanged ( ChangeEvent event ) {
	//JTabbedPane sourceTabbedPane = (JTabbedPane)event.getSource();
	//int index = sourceTabbedPane.getSelectedIndex();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
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