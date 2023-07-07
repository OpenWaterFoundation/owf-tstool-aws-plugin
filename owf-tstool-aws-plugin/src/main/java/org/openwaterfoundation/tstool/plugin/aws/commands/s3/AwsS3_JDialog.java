// AwsS3_JDialog - editor for AwsS3 command

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

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.StringListJDialog;
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
public class AwsS3_JDialog extends JDialog
implements ActionListener, ChangeListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __browseS3_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __Profile_JComboBox = null;
private JTextField __ProfileDefault_JTextField = null; // View only (not a command parameter).
private JLabel __ProfileDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __S3Command_JComboBox = null;
private SimpleJComboBox __Region_JComboBox = null;
private JTextField __RegionDefault_JTextField = null; // View only (not a command parameter).
private JLabel __RegionDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __Bucket_JComboBox = null;
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Copy tab.
private JTextArea __CopyFiles_JTextArea = null;
private SimpleJComboBox __CopyBucket_JComboBox = null;
private JTextField __CopyObjectsCountProperty_JTextField = null;

// Delete tab.
private JTextArea __DeleteFiles_JTextArea = null;
private JTextArea __DeleteFolders_JTextArea = null;
private SimpleJComboBox __DeleteFoldersScope_JComboBox = null;
private JTextField __DeleteFoldersMinDepth_JTextField = null;

// Download tab.
private JTextArea __DownloadFiles_JTextArea = null;
private JTextArea __DownloadFolders_JTextArea = null;

// List Buckets tab.
private JTextField __ListBucketsRegEx_JTextField = null;
private JTextField __ListBucketsCountProperty_JTextField = null;

// List Objects tab.
private SimpleJComboBox __ListObjectsScope_JComboBox = null;
private JTextField __Prefix_JTextField = null;
private JTextField __Delimiter_JTextField = null;
private JTextField __ListObjectsRegEx_JTextField = null;
private SimpleJComboBox __ListFiles_JComboBox = null;
private SimpleJComboBox __ListFolders_JComboBox = null;
private JTextField __MaxKeys_JTextField = null;
private JTextField __MaxObjects_JTextField = null;
private JTextField __ListObjectsCountProperty_JTextField = null;

// Upload tab.
private JTextArea __UploadFiles_JTextArea = null;
private JTextArea __UploadFolders_JTextArea = null;

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __AppendOutput_JComboBox = null;

// CloudFront tab.
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
private AwsS3_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private boolean ignoreEvents = false; // Ignore events when initializing, to avoid infinite loop.
private JFrame __parent = null;

/**
 * AWS session used to interact with AWS:
 * - will be null until the profile is set, which will happen when refresh() is called once
 */
private AwsSession awsSession = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
public AwsS3_JDialog ( JFrame parent, AwsS3_Command command, List<String> tableIDChoices ) {
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

    if ( o == this.__S3Command_JComboBox ) {
    	setTabForS3Command();
    }
    else if ( o == __browseOutput_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Output File");

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
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
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
    		S3Browser_App.launchBrowser ( title, awsSession, getSelectedRegion(true), this.__Bucket_JComboBox.getSelected() );
    	}
    	catch ( Exception e ) {
    		// Should not happen.
    	}
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditCopyFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String CopyFiles = __CopyFiles_JTextArea.getText().trim();
        String [] notes = {
        	"Copy S3 file objects by specifying source and destination keys for bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "Specify the S3 object key using a path (e.g., folder1/folder2/file.ext).",
            "The S3 destination will be created if it does not exist, or overwritten if it does exist.",
            "A leading / in the S3 bucket key is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, CopyFiles,
            "Edit CopyFiles Parameter", notes, "Source S3 Key (Path)", "Destination S3 Key (Path)", 10)).response();
        if ( dict != null ) {
            __CopyFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDeleteFolders") ) {
        // Edit the list in the dialog.  It is OK for the string to be blank.
        String DeleteFolders = __DeleteFolders_JTextArea.getText().trim();
        String [] notes = {
            "Specify the paths ending in / for folders to delete.  Use the S3 Browser in the command editor to view folders and their keys.",
            "All files in the folder and the folder itself will be deleted.  This requires doing a folder listing first.",
            "Do not specify a leading / unless the key actually contains a starting / (default for S3 buckets is no leading /).",
            "${Property} notation can be used to expand at run time.",
            "Use the checkboxes with Insert and Remove.",
            "All non-blank object keys will be included in the command parameter.",
        };
        String delim = ",";
        String list = (new StringListJDialog ( __parent, true, DeleteFolders,
            "Edit DeleteFolders Parameter", notes, "Folder Key", delim, 10)).response();
        if ( list != null ) {
            __DeleteFolders_JTextArea.setText ( list );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDeleteFiles") ) {
        // Edit the list in the dialog.  It is OK for the string to be blank.
        String DeleteFiles = __DeleteFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the S3 object keys for file objects to delete.  Use the S3 Browser in the command editor to view objects and their keys.",
            "Do not specify a folder key ending in / (see also the DeleteFolders command parameter).",
            "Do not specify a leading / unless the key actually contains a starting / (default for S3 buckets is no leading /).",
            "${Property} notation can be used to expand at run time.",
            "Use the checkboxes with Insert and Remove.",
            "All non-blank object keys will be included in the command parameter.",
        };
        String delim = ",";
        String list = (new StringListJDialog ( __parent, true, DeleteFiles,
            "Edit DeleteFiles Parameter", notes, "S3 File Object Key", delim, 10)).response();
        if ( list != null ) {
            __DeleteFiles_JTextArea.setText ( list );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFolders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFolders = __DownloadFolders_JTextArea.getText().trim();
        String [] notes = {
            "Specify the S3 bucket folder path key (e.g., topfolder/childfolder/) to download.",
            "Only folders (directories) can be downloaded. Specify files to download with the 'DownloadFiles' command parameter.",
            "A leading / in the folder key should be used only if the S3 bucket uses a top-level /.",
            "A trailing / in the bucket prefix (S3 directory path) is required to indicate a folder.",
            "The local folder is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFolders,
            "Edit DownloadFolders Parameter", notes, "S3 Folder Path (ending in /)", "Local Folder (optionally ending in /)",10)).response();
        if ( dict != null ) {
            __DownloadFolders_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFiles = __DownloadFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the bucket file object S3 key (e.g., topfolder/childfolder/file.ext) to download a file.",
            "Only files can be downloaded.  Specify folders to download with the 'DownloadFolders' command parameter.",
            "The key is the full path for the the file object.",
            "A leading / in the folder key should be used only if the S3 bucket uses a top-level /.",
            "The local file name can be * to use the same name as the S3 object.",
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFiles,
            "Edit DownloadFiles Parameter", notes, "S3 File Path", "Local File",10)).response();
        if ( dict != null ) {
            __DownloadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AwsS3", PluginMeta.getDocumentationRootUrl());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __pathOutput_JButton ) {
        if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
        }
        else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"AwsS3_JDialog",
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadFolders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadFolders = __UploadFolders_JTextArea.getText().trim();
        String [] notes = {
        	"Upload local folders (and all subfolders and files in each folder) to S3 bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "The local folder is relative to the working folder:",
            "    " + this.__working_dir,
            "Specify the S3 folder using a path ending in / (e.g., topfolder/childfolder/).",
            "The S3 location will be created if it does not exist, or overwritten if it does exist.",
            "Only folders (directories) can be specified. Specify files to upload with the 'UploadFiles' command parameter.",
            "All files in the folders are uploaded, resulting in corresponding virtual folders in S3.",
            "A leading / in the S3 bucket folder is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadFolders,
            "Edit UploadFolders Parameter", notes, "Local Folder (optionally ending in /)", "S3 Folder Path (ending in /)", 10)).response();
        if ( dict != null ) {
            __UploadFolders_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadFiles = __UploadFiles_JTextArea.getText().trim();
        String [] notes = {
        	"Upload local files to S3 bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "Specify the S3 bucket object key (S3 file path) to upload a file.",
            "Use * in the 'Local File' to match a pattern and /* at the end of the 'Bucket Key' to use the same file name in the S3 bucket.",
            "  For example, local=folder1/folder2/fileZ.txt and s3=foldera/folderb/* would save foldera/folderb/fileZ.txt on S3",
            "Only files can be uploaded with this parameter. Specify folders to upload with the 'UploadFolders' command parameter.",
            "The key is the full path for the bucket object.",
            "A leading / in the S3 bucket object key is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadFiles,
            "Edit UploadFiles Parameter", notes, "Local File", "S3 Bucket Object Key (object path)", 10)).response();
        if ( dict != null ) {
            __UploadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
	else {
		// Choices.
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	// General.
	String S3Command = __S3Command_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	// Copy.
	String CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	String CopyBucket = __CopyBucket_JComboBox.getSelected();
	String CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	String DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	String DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	String DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	String DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	// Download.
	String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	String ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	String ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	// List bucket objects.
	String ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	String Prefix = __Prefix_JTextField.getText().trim();
	String Delimiter = __Delimiter_JTextField.getText().trim();
	String ListObjectsRegEx = __ListObjectsRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String ListObjectsCountProperty = __ListObjectsCountProperty_JTextField.getText().trim();
	// Upload.
	String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output.
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	// CloudFront.
    String InvalidateCloudFront = __InvalidateCloudFront_JComboBox.getSelected();
	String CloudFrontRegion = getSelectedCloudFrontRegion();
	String CloudFrontDistributionId = __CloudFrontDistributionId_JComboBox.getSelected();
	String CloudFrontComment = __CloudFrontComment_JTextField.getText().trim();
	String CloudFrontCallerReference = __CloudFrontCallerReference_JTextField.getText().trim();
    String CloudFrontWaitForCompletion = __CloudFrontWaitForCompletion_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( (S3Command != null) && !S3Command.isEmpty() ) {
		props.set ( "S3Command", S3Command );
	}
	if ( (Profile != null) && !Profile.isEmpty() ) {
		props.set ( "Profile", Profile );
	}
	if ( (Region != null) && !Region.isEmpty() ) {
		props.set ( "Region", Region );
	}
	if ( (Bucket != null) && !Bucket.isEmpty() ) {
		props.set ( "Bucket", Bucket );
	}
	// Copy.
	if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
		props.set ( "CopyFiles", CopyFiles );
	}
	if ( (CopyBucket != null) && !CopyBucket.isEmpty() ) {
		props.set ( "CopyBucket", CopyBucket );
	}
	if ( (CopyObjectsCountProperty != null) && !CopyObjectsCountProperty.isEmpty() ) {
		props.set ( "CopyObjectsCountProperty", CopyObjectsCountProperty );
	}
	// Delete.
	if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
		props.set ( "DeleteFiles", DeleteFiles );
	}
	if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
		props.set ( "DeleteFolders", DeleteFolders );
	}
	if ( (DeleteFoldersScope != null) && !DeleteFoldersScope.isEmpty() ) {
		props.set ( "DeleteFoldersScope", DeleteFoldersScope );
	}
	if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.isEmpty() ) {
		props.set ( "DeleteFoldersMinDepth", DeleteFoldersMinDepth );
	}
	// Download.
	if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
		props.set ( "DownloadFolders", DownloadFolders );
	}
	if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
		props.set ( "DownloadFiles", DownloadFiles );
	}
	// List buckets.
	if ( (ListBucketsRegEx != null) && !ListBucketsRegEx.isEmpty() ) {
		props.set ( "ListBucketsRegEx", ListBucketsRegEx );
	}
	if ( (ListBucketsCountProperty != null) && !ListBucketsCountProperty.isEmpty() ) {
		props.set ( "ListBucketsCountProperty", ListBucketsCountProperty );
	}
	// List bucket objects.
	if ( (ListObjectsScope != null) && !ListObjectsScope.isEmpty() ) {
		props.set ( "ListObjectsScope", ListObjectsScope);
	}
	if ( (Prefix != null) && !Prefix.isEmpty() ) {
		props.set ( "Prefix", Prefix );
	}
	if ( (Delimiter != null) && !Delimiter.isEmpty() ) {
		props.set ( "Delimiter", Delimiter );
	}
	if ( (ListObjectsRegEx != null) && !ListObjectsRegEx.isEmpty() ) {
		props.set ( "ListObjectsRegEx", ListObjectsRegEx );
	}
	if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
		props.set ( "ListFiles", ListFiles );
	}
	if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
		props.set ( "ListFolders", ListFolders );
	}
	if ( (MaxKeys != null) && !MaxKeys.isEmpty() ) {
		props.set ( "MaxKeys", MaxKeys );
	}
	if ( (MaxObjects != null) && !MaxObjects.isEmpty() ) {
		props.set ( "MaxObjects", MaxObjects );
	}
	if ( (ListObjectsCountProperty != null) && !ListObjectsCountProperty.isEmpty() ) {
		props.set ( "ListObjectsCountProperty", ListObjectsCountProperty );
	}
	// Upload.
	if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
		props.set ( "UploadFolders", UploadFolders );
	}
	if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
		props.set ( "UploadFiles", UploadFiles );
	}
	// Output.
    if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
        props.set ( "OutputTableID", OutputTableID );
    }
    if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( (AppendOutput != null) && !AppendOutput.isEmpty() ) {
        props.set ( "AppendOutput", AppendOutput );
    }
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
    /*
	if ( IfInputNotFound.length() > 0 ) {
		props.set ( "IfInputNotFound", IfInputNotFound );
	}
	*/
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
	// General.
	String S3Command = __S3Command_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	// Copy.
	String CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	String CopyBucket = __CopyBucket_JComboBox.getSelected();
	String CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	String DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	String DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	String DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	String DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	// Download.
	String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	String ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	String ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	// List bucket objects.
	String ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	String Prefix = __Prefix_JTextField.getText().trim();
	String Delimiter = __Delimiter_JTextField.getText().trim();
	String ListObjectsRegEx = __ListObjectsRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String ListObjectsCountProperty = __ListObjectsCountProperty_JTextField.getText().trim();
	// Upload.
	String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	// CloudFront
    String InvalidateCloudFront = __InvalidateCloudFront_JComboBox.getSelected();
	String CloudFrontRegion = getSelectedCloudFrontRegion();
	String CloudFrontDistributionId = __CloudFrontDistributionId_JComboBox.getSelected();
	String CloudFrontComment = __CloudFrontComment_JTextField.getText().trim();
	String CloudFrontCallerReference = __CloudFrontCallerReference_JTextField.getText().trim();
    String CloudFrontWaitForCompletion = __CloudFrontWaitForCompletion_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();

    // General.
	__command.setCommandParameter ( "S3Command", S3Command );
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	__command.setCommandParameter ( "Bucket", Bucket );
	// Copy.
	__command.setCommandParameter ( "CopyFiles", CopyFiles );
	__command.setCommandParameter ( "CopyBucket", CopyBucket );
	__command.setCommandParameter ( "CopyObjectsCountProperty", CopyObjectsCountProperty );
	// Delete.
	__command.setCommandParameter ( "DeleteFiles", DeleteFiles );
	__command.setCommandParameter ( "DeleteFolders", DeleteFolders );
	__command.setCommandParameter ( "DeleteFoldersScope", DeleteFoldersScope );
	__command.setCommandParameter ( "DeleteFoldersMinDepth", DeleteFoldersMinDepth );
	// Download.
	__command.setCommandParameter ( "DownloadFolders", DownloadFolders );
	__command.setCommandParameter ( "DownloadFiles", DownloadFiles );
	// List Buckets.
	__command.setCommandParameter ( "ListBucketsRegEx", ListBucketsRegEx );
	__command.setCommandParameter ( "ListBucketsCountProperty", ListBucketsCountProperty );
	// List Objects.
	__command.setCommandParameter ( "ListObjectsScope", ListObjectsScope );
	__command.setCommandParameter ( "Prefix", Prefix );
	__command.setCommandParameter ( "Delimiter", Delimiter );
	__command.setCommandParameter ( "ListObjectsRegEx", ListObjectsRegEx );
	__command.setCommandParameter ( "ListFiles", ListFiles );
	__command.setCommandParameter ( "ListFolders", ListFolders );
	__command.setCommandParameter ( "MaxKeys", MaxKeys );
	__command.setCommandParameter ( "MaxObjects", MaxObjects );
	__command.setCommandParameter ( "ListObjectsCountProperty", ListObjectsCountProperty );
	// Upload.
	__command.setCommandParameter ( "UploadFolders", UploadFolders );
	__command.setCommandParameter ( "UploadFiles", UploadFiles );
	// Output.
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "AppendOutput", AppendOutput );
	// CloudFront.
	__command.setCommandParameter ( "InvalidateCloudFront", InvalidateCloudFront );
	__command.setCommandParameter ( "CloudFrontRegion", CloudFrontRegion );
	__command.setCommandParameter ( "CloudFrontDistributionId", CloudFrontDistributionId );
	__command.setCommandParameter ( "CloudFrontComment", CloudFrontComment );
	__command.setCommandParameter ( "CloudFrontCallerReference", CloudFrontCallerReference );
	__command.setCommandParameter ( "CloudFrontWaitForCompletion", CloudFrontWaitForCompletion );
	//__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
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
private void initialize ( JFrame parent, AwsS3_Command command, List<String> tableIDChoices ) {
	this.__command = command;
	this.__parent = parent;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Run an Amazon Web Services (AWS) S3 (Simple Storage Service) command."
        + "  S3 manages 'objects' (files) in 'buckets' similar to a storage device." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"An S3 bucket may be associated with a CloudFront distribution, which provides a Content Deliver Network (CDN) suitable for public websites."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Each S3 object (file) is identified with a \"key\", similar to a file path.  "
        + "An S3 object key typically does not start with / but some buckets may use keys starting in /."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "S3 may store folders as objects with key ending in /, but otherwise folders are virtual and indicate storage hierarchy."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Hover on fields to view tool tips to see which parameters can use ${Property} notation." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file and folders on the local computer are relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    String config = AwsToolkit.getInstance().getAwsUserConfigFile();
    File f = null;
    if ( config != null ) {
    	f = new File(config);
    }
    if ( (f == null) || !f.exists() ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"<html><b>ERROR: User's AWS configuration file does not exist (errors will occur): "
        	+ AwsToolkit.getInstance().getAwsUserConfigFile() + "</b></html>" ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"User's AWS configuration file: " + AwsToolkit.getInstance().getAwsUserConfigFile() ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "S3 command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__S3Command_JComboBox = new SimpleJComboBox ( false );
	__S3Command_JComboBox.setToolTipText("S3 command to execute.");
	List<String> commandChoices = AwsS3CommandType.getChoicesAsStrings(false);
	__S3Command_JComboBox.setData(commandChoices);
	__S3Command_JComboBox.select ( 0 );
	__S3Command_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __S3Command_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - S3 command to run (see tabs below)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Profile:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
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
    JGUIUtil.addComponent(main_JPanel, __Profile_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - profile for authentication (default=see below)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Profile (default):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProfileDefault_JTextField = new JTextField ( 20 );
	__ProfileDefault_JTextField.setToolTipText("Default profile determined from user's .aws/config file).");
	__ProfileDefault_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ProfileDefault_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __ProfileDefaultNote_JLabel = new JLabel("From: " + AwsToolkit.getInstance().getAwsUserConfigFile());
    JGUIUtil.addComponent(main_JPanel, __ProfileDefaultNote_JLabel,
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__ProfileDefault_JTextField.setEditable(false);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Region:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
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
    JGUIUtil.addComponent(main_JPanel, __Region_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - AWS region (default=see below)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Region (default):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RegionDefault_JTextField = new JTextField ( 20 );
	__RegionDefault_JTextField.setToolTipText("Default region for profile determined from user's .aws/config file).");
	__RegionDefault_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RegionDefault_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __RegionDefaultNote_JLabel = new JLabel("From: " + AwsToolkit.getInstance().getAwsUserConfigFile() );
    JGUIUtil.addComponent(main_JPanel, __RegionDefaultNote_JLabel,
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__RegionDefault_JTextField.setEditable(false);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Bucket:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bucket_JComboBox = new SimpleJComboBox ( false );
	__Bucket_JComboBox.setToolTipText("AWS S3 bucket.");
	// Choices will be populated when refreshed, based on profile.
	__Bucket_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Bucket_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Required (except for ListBuckets command) - S3 bucket."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'Copy' parameters:
    // - specify original and copy
    int yCopy = -1;
    JPanel copy_JPanel = new JPanel();
    copy_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Copy", copy_JPanel );

    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Copy S3 object(s) by specifying source and destination keys."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Currently only single file objects can be copied (not folders)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Use object keys (paths) similar to:  folder1/folder2/file.ext"),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("The keys should not start with / unless the bucket uses top-level / for keys."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("The keys should not end with / (support for copying folders may be added in the future)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Use the 'Browse S3' button to visually confirm S3 object keys (paths)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Copy files:"),
        0, ++yCopy, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyFiles_JTextArea = new JTextArea (6,35);
    __CopyFiles_JTextArea.setLineWrap ( true );
    __CopyFiles_JTextArea.setWrapStyleWord ( true );
    __CopyFiles_JTextArea.setToolTipText("SourceKey1:DestKey1,SourceKey2:DestKey2,...");
    __CopyFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(copy_JPanel, new JScrollPane(__CopyFiles_JTextArea),
        1, yCopy, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Source and destination key(s)."),
        3, yCopy, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(copy_JPanel, new SimpleJButton ("Edit","EditCopyFiles",this),
        3, ++yCopy, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Copy destination bucket:"),
		0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CopyBucket_JComboBox = new SimpleJComboBox ( false );
	__CopyBucket_JComboBox.setToolTipText("AWS S3 destination bucket.");
	// Choices will be populated when refreshed, based on profile.
	__CopyBucket_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopyBucket_JComboBox,
		1, yCopy, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel(
		"Optional - S3 destination bucket (default=source bucket)."),
		3, yCopy, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel("Copy objects count property:"),
        0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyObjectsCountProperty_JTextField = new JTextField ( "", 30 );
    __CopyObjectsCountProperty_JTextField.setToolTipText("Specify the property name for the copy result size, can use ${Property} notation");
    __CopyObjectsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopyObjectsCountProperty_JTextField,
        1, yCopy, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Optional - processor property to set as copy count." ),
        3, yCopy, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Delete' parameters:
    // - specify S3 files and folders to delete
    int yDelete = -1;
    JPanel delete_JPanel = new JPanel();
    delete_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Delete", delete_JPanel );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Specify the S3 object(s) to delete."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Object keys should not start with / unless the bucket uses a top-level / object."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("File object keys should NOT end with /."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Folder path keys should end with /."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Delete files:"),
        0, ++yDelete, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFiles_JTextArea = new JTextArea (6,35);
    __DeleteFiles_JTextArea.setLineWrap ( true );
    __DeleteFiles_JTextArea.setWrapStyleWord ( true );
    __DeleteFiles_JTextArea.setToolTipText("S3 file object keys to delete, separated by commas.");
    __DeleteFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(delete_JPanel, new JScrollPane(__DeleteFiles_JTextArea),
        1, yDelete, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("S3 bucket key(s)."),
        3, yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(delete_JPanel, new SimpleJButton ("Edit","EditDeleteFiles",this),
        3, ++yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Delete folders:"),
        0, ++yDelete, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFolders_JTextArea = new JTextArea (6,35);
    __DeleteFolders_JTextArea.setLineWrap ( true );
    __DeleteFolders_JTextArea.setWrapStyleWord ( true );
    __DeleteFolders_JTextArea.setToolTipText("Folders to delete, as paths ending in /, separated by commas.");
    __DeleteFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(delete_JPanel, new JScrollPane(__DeleteFolders_JTextArea),
        1, yDelete, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("S3 folders(s) ending in /."),
        3, yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(delete_JPanel, new SimpleJButton ("Edit","EditDeleteFolders",this),
        3, ++yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Delete folders scope:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DeleteFoldersScope_JComboBox = new SimpleJComboBox ( false );
	__DeleteFoldersScope_JComboBox.setToolTipText("AWS S3 bucket.");
	List<String> deleteChoices = new ArrayList<>();
	deleteChoices.add("");
	deleteChoices.add(command._AllFilesAndFolders);
	deleteChoices.add(command._FolderFiles);
	__DeleteFoldersScope_JComboBox.setData(deleteChoices);
	__DeleteFoldersScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(delete_JPanel, __DeleteFoldersScope_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel(
		"Optional - scope of folder delete (default=" + command._FolderFiles + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Delete folders minimum depth:"),
        0, ++yDelete, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFoldersMinDepth_JTextField = new JTextField ( "", 10 );
    __DeleteFoldersMinDepth_JTextField.setToolTipText("Folder depth that is required to delete, to guard against deleting top folders.");
    __DeleteFoldersMinDepth_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(delete_JPanel, __DeleteFoldersMinDepth_JTextField,
        1, yDelete, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Optional - minimum required folder depth (default=" + command._DeleteFoldersMinDepth + ")."),
        3, yDelete, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Download' parameters:
    // - map bucket objects to files and folders
    int yDownload = -1;
    JPanel download_JPanel = new JPanel();
    download_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Download", download_JPanel );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Specify files and folders to download."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Use the 'Edit' button to view information about S3 and local file and folder paths."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download folders:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFolders_JTextArea = new JTextArea (6,35);
    __DownloadFolders_JTextArea.setLineWrap ( true );
    __DownloadFolders_JTextArea.setWrapStyleWord ( true );
    __DownloadFolders_JTextArea.setToolTipText("Key1:Folder1,Key2:Folder2,...");
    __DownloadFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFolders_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("S3 bucket key(s) (prefix) and local folder(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadFolders",this),
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download files:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFiles_JTextArea = new JTextArea (6,35);
    __DownloadFiles_JTextArea.setLineWrap ( true );
    __DownloadFiles_JTextArea.setWrapStyleWord ( true );
    __DownloadFiles_JTextArea.setToolTipText("Key1:File1,Key2:File2,...");
    __DownloadFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFiles_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("S3 bucket key(s) (prefix) and local file(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadFiles",this),
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for 'List Buckets' parameters.
    int yListBuckets = -1;
    JPanel listBuckets_JPanel = new JPanel();
    listBuckets_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Buckets", listBuckets_JPanel );

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("List all buckets that are visible to the user based on the profile."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("Use * in the regular expression as wildcards to filter the results."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("See the 'Output' tab to specify the output table and/or file for the bucket list."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Regular expression:"),
        0, ++yListBuckets, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListBucketsRegEx_JTextField = new JTextField ( "", 30 );
    __ListBucketsRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListBucketsRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listBuckets_JPanel, __ListBucketsRegEx_JTextField,
        1, yListBuckets, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yListBuckets, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel("List buckets count property:"),
        0, ++yListBuckets, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListBucketsCountProperty_JTextField = new JTextField ( "", 30 );
    __ListBucketsCountProperty_JTextField.setToolTipText("Specify the property name for the object list result size, can use ${Property} notation");
    __ListBucketsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listBuckets_JPanel, __ListBucketsCountProperty_JTextField,
        1, yListBuckets, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Optional - processor property to set as bucket count." ),
        3, yListBuckets, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'List Objects' parameters:
    // - this includes filtering
    int yListObjects = -1;
    JPanel listObjects_JPanel = new JPanel();
    listObjects_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Objects", listObjects_JPanel );

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ("List all bucket objects that are visible to the user based on the profile."
    	+ "  See the 'Output' tab to specify the output file and/or table for the bucket object list."),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel (
    	"Limit the output using parameters as follows and by using the 'Regular expression', 'List files', and 'List folders' filters."),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //String style = " style=\"border: 1px solid black; border-collapse: collapse; background-color: white;\"";
    String style = " style=\"border-collapse: collapse; border-spacing: 0px;\"";
    String tableStyle = style; 
    String trStyle = "";
    String tdStyle = " style=\"border: 1px solid black; background-color: white;\"";
    String table =
    		  "<html>"
    		  + "<table " + tableStyle + ">"
    		+ "    <tr" + trStyle + ">"
    		+ "       <th" + tdStyle + ">List what?</th>"
    		+ "       <th" + tdStyle + ">List scope</th>"
    		+ "       <th" + tdStyle + ">Prefix to match</th>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">One object</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + ">Path (key) for the object</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in root</td>"
    		+ "       <td" + tdStyle + ">Folder</td>"
    		+ "       <td" + tdStyle + "></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in folder</td>"
    		+ "       <td" + tdStyle + ">Folder</td>"
    		+ "       <td" + tdStyle + ">Folder path (key) ending in /</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">All objects matching leading path</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + ">Path (key) to match, can be partial file name</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">All files in bucket</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + "></td>"
    		+ "    </tr>"
    		+ "  </table>"
    		+ "</html>";
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel (table),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    Message.printStatus(2, "", table);
    /*
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ("    list all objects in a bucket: ListScope=" + _All + ", Prefix"),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ("    no Prefix (and list only root) - list all the top-level (root) folder objects"),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ("    Prefix = folder1/ - list 'folder1/ objects (output will include the trailing /)"),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ("    Prefix = file or folder1/folder2/file - list one file (must match exactly)"),
		0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/
    JGUIUtil.addComponent(listObjects_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "List scope:"),
		0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListObjectsScope_JComboBox = new SimpleJComboBox ( false );
	__ListObjectsScope_JComboBox.setToolTipText("Scope (depth) of the list, which controls the output");
	List<String> listRootChoices = new ArrayList<>();
	listRootChoices.add ( "" );	// Default.
	listRootChoices.add ( __command._All );
	listRootChoices.add ( __command._Folder );
	//listRootChoices.add ( __command._Root );
	__ListObjectsScope_JComboBox.setData(listRootChoices);
	__ListObjectsScope_JComboBox.select ( 0 );
	__ListObjectsScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __ListObjectsScope_JComboBox,
		1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel(
		"Optional - scope (depth) of the listing (default=" + __command._All + ")."),
		3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Prefix to match:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Prefix_JTextField = new JTextField ( "", 30 );
    __Prefix_JTextField.setToolTipText("Specify the start of the key to match, ending in / if listing a folder's contents.");
    __Prefix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __Prefix_JTextField,
        1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - object key prefix to match (default=list all)."),
        3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Delimiter:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Delimiter_JTextField = new JTextField ( "", 10 );
    __Delimiter_JTextField.setToolTipText("Delimiter to use for folders, default=/.");
    __Delimiter_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __Delimiter_JTextField,
        1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - delimiter for folders (default=/)."),
        3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Regular expression:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListObjectsRegEx_JTextField = new JTextField ( "", 30 );
    __ListObjectsRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListObjectsRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __ListObjectsRegEx_JTextField,
        1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "List files?:"),
		0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFiles_JComboBox = new SimpleJComboBox ( false );
	__ListFiles_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFilesChoices = new ArrayList<>();
	listFilesChoices.add ( "" );	// Default.
	listFilesChoices.add ( __command._False );
	listFilesChoices.add ( __command._True );
	__ListFiles_JComboBox.setData(listFilesChoices);
	__ListFiles_JComboBox.select ( 0 );
	__ListFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __ListFiles_JComboBox,
		1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel(
		"Optional - list files? (default=" + __command._True + ")."),
		3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "List folders?:"),
		0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFolders_JComboBox = new SimpleJComboBox ( false );
	__ListFolders_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFoldersChoices = new ArrayList<>();
	listFoldersChoices.add ( "" );	// Default.
	listFoldersChoices.add ( __command._False );
	listFoldersChoices.add ( __command._True );
	__ListFolders_JComboBox.setData(listFoldersChoices);
	__ListFolders_JComboBox.select ( 0 );
	__ListFolders_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __ListFolders_JComboBox,
		1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel(
		"Optional - list folders? (default=" + __command._True + ")."),
		3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Maximum keys:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxKeys_JTextField = new JTextField ( "", 10 );
    __MaxKeys_JTextField.setToolTipText("Used internally by AWS web services.");
    __MaxKeys_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __MaxKeys_JTextField,
        1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - maximum number of object keys read per request (default="
    	+ this.__command._MaxKeys + ")."),
        3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Maximum objects:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxObjects_JTextField = new JTextField ( "", 10 );
    __MaxObjects_JTextField.setToolTipText("Use to limit the size of the query results.");
    __MaxObjects_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __MaxObjects_JTextField,
        1, yListObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - maximum number of object read (default=2000)."),
        3, yListObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listObjects_JPanel, new JLabel("List objects count property:"),
        0, ++yListObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListObjectsCountProperty_JTextField = new JTextField ( "", 30 );
    __ListObjectsCountProperty_JTextField.setToolTipText("Specify the property name for the bucket object list result size, can use ${Property} notation");
    __ListObjectsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listObjects_JPanel, __ListObjectsCountProperty_JTextField,
        1, yListObjects, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listObjects_JPanel, new JLabel ( "Optional - processor property to set as object count." ),
        3, yListObjects, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Upload' parameters:
    // - map files and folders to bucket objects
    int yUpload = -1;
    JPanel upload_JPanel = new JPanel();
    upload_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Upload", upload_JPanel );

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Specify files and folders (directories) to upload."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Use the 'Edit' button to view information about local and S3 file and folder paths."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Upload folders:"),
        0, ++yUpload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UploadFolders_JTextArea = new JTextArea (6,35);
    __UploadFolders_JTextArea.setLineWrap ( true );
    __UploadFolders_JTextArea.setWrapStyleWord ( true );
    __UploadFolders_JTextArea.setToolTipText("Folder1:Key1,Folder2:Key2,...");
    __UploadFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(upload_JPanel, new JScrollPane(__UploadFolders_JTextArea),
        1, yUpload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Local folder(s) and S3 bucket key(s) ending in /."),
        3, yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(upload_JPanel, new SimpleJButton ("Edit","EditUploadFolders",this),
        3, ++yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Upload files:"),
        0, ++yUpload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UploadFiles_JTextArea = new JTextArea (6,35);
    __UploadFiles_JTextArea.setLineWrap ( true );
    __UploadFiles_JTextArea.setWrapStyleWord ( true );
    __UploadFiles_JTextArea.setToolTipText("File1:Key1,File2:Key2,...");
    __UploadFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(upload_JPanel, new JScrollPane(__UploadFiles_JTextArea),
        1, yUpload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Local file(s) and S3 bucket key(s)."),
        3, yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(upload_JPanel, new SimpleJButton ("Edit","EditUploadFiles",this),
        3, ++yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for output.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
    	"The following parameters are used with 'List Buckets' and 'List Objects' commands."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("An output table and/or file can be created."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("An existing table will be appended to if found."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("The output file uses the specified table (or a temporary table) to create the output file."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: csv"),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("See also other commands to write tables."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output Table ID:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __OutputTableID_JComboBox.setToolTipText("Table for output, available for List Buckets and List Objects");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __OutputTableID_JComboBox.setData ( tableIDChoices );
    __OutputTableID_JComboBox.addItemListener ( this );
    __OutputTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__OutputTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(output_JPanel, __OutputTableID_JComboBox,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - table for output."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Output file:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText(
    	"Output file, available for List Buckets and List Objects, can use ${Property} notation.");
    __OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(output_JPanel, OutputFile_JPanel,
		1, yOutput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(output_JPanel, new JLabel ( "Append output?:"),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AppendOutput_JComboBox = new SimpleJComboBox ( false );
	__AppendOutput_JComboBox.setToolTipText("Append output to existing table or file?");
	List<String> appendChoices = new ArrayList<>();
	appendChoices.add ( "" );	// Default.
	appendChoices.add ( __command._False );
	appendChoices.add ( __command._True );
	__AppendOutput_JComboBox.setData(appendChoices);
	__AppendOutput_JComboBox.select ( 0 );
	__AppendOutput_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(output_JPanel, __AppendOutput_JComboBox,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
		"Optional - append to output (default=" + __command._False + ")."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'CloudFront' parameters:
    // - this streamlines invalidation
    int yCloudFront = -1;
    JPanel cf_JPanel = new JPanel();
    cf_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "CloudFront", cf_JPanel );

    JGUIUtil.addComponent(cf_JPanel, new JLabel (
    	"Use these parameters to automatically invalidate paths in a CloudFront distribution for Copy, Delete, and Upload commands."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("This ensures that modified objects are quickly visible to the CloudFront distribution website."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("The bucket S3 keys must match the CloudFront paths "
   		+ "(a leading / is added if the S3 keys to not have / at the front)."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("The 'aws-global' region may be required for CloudFront distributions."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel (
    	"Specify a CloudFront distribution using the distribution ID or comment (description) pattern (e.g., *some.domain.org*)."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("<html><b>CloudFront invalidations may have higher relative cost than S3 uploads.</b></hmtl>"),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cf_JPanel, new JLabel ("The AwsCloudFront() command can also be used separately."),
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
    if ( o == this.__S3Command_JComboBox ) {
    	setTabForS3Command();
    }
    else if ( o == this.__Profile_JComboBox ) {
    	this.awsSession.setProfile(this.__Profile_JComboBox.getSelected());
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __CopyBucket_JComboBox, true );
	}
	else if ( o == this.__Region_JComboBox ) {
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __CopyBucket_JComboBox, true );
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
	// General.
	String S3Command = "";
	String Profile = "";
	String Region = "";
	String Bucket = "";
	// Copy.
	String CopyFiles = "";
	String CopyBucket = "";
	String CopyObjectsCountProperty = "";
	// Delete.
	String DeleteFiles = "";
	String DeleteFolders = "";
	String DeleteFoldersScope = "";
	String DeleteFoldersMinDepth = "";
	// Download.
	String DownloadFolders = "";
	String DownloadFiles = "";
	// List buckets.
	String ListBucketsRegEx = "";
	String ListBucketsCountProperty = "";
	// List bucket objects.
	String ListObjectsScope = "";
	String Prefix = "";
	String Delimiter = "";
	String ListObjectsRegEx = "";
	String ListFiles = "";
	String ListFolders = "";
	String MaxKeys = "";
	String MaxObjects = "";
	String ListObjectsCountProperty = "";
	// Upload.
	String UploadFolders = "";
	String UploadFiles = "";
	// Output.
	String OutputTableID = "";
	String OutputFile = "";
	String AppendOutput = "";
	// CloudFront.
	String InvalidateCloudFront = "";
	String CloudFrontRegion = "";
	String CloudFrontDistributionId = "";
	String CloudFrontComment = "";
	String CloudFrontCallerReference = "";
    String CloudFrontWaitForCompletion = "";
	//String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        // General.
		S3Command = parameters.getValue ( "S3Command" );
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		Bucket = parameters.getValue ( "Bucket" );
		// Copy.
		CopyFiles = parameters.getValue ( "CopyFiles" );
		CopyBucket = parameters.getValue ( "CopyBucket" );
		CopyObjectsCountProperty = parameters.getValue ( "CopyObjectsCountProperty" );
		// Delete.
		DeleteFiles = parameters.getValue ( "DeleteFiles" );
		DeleteFolders = parameters.getValue ( "DeleteFolders" );
		DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
		DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
		// Download.
		DownloadFolders = parameters.getValue ( "DownloadFolders" );
		DownloadFiles = parameters.getValue ( "DownloadFiles" );
		// List buckets.
		ListBucketsRegEx = parameters.getValue ( "ListBucketsRegEx" );
		ListBucketsCountProperty = parameters.getValue ( "ListBucketsCountProperty" );
		// List bucket objects.
		ListObjectsScope = parameters.getValue ( "ListObjectsScope" );
		Prefix = parameters.getValue ( "Prefix" );
		Delimiter = parameters.getValue ( "Delimiter" );
		ListObjectsRegEx = parameters.getValue ( "ListObjectsRegEx" );
		ListFiles = parameters.getValue ( "ListFiles" );
		ListFolders = parameters.getValue ( "ListFolders" );
		MaxKeys = parameters.getValue ( "MaxKeys" );
		MaxObjects = parameters.getValue ( "MaxObjects" );
		ListObjectsCountProperty = parameters.getValue ( "ListObjectsCountProperty" );
		// Upload.
		UploadFolders = parameters.getValue ( "UploadFolders" );
		UploadFiles = parameters.getValue ( "UploadFiles" );
		// Output
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		AppendOutput = parameters.getValue ( "AppendOutput" );
		// CloudFront
		InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
		CloudFrontRegion = parameters.getValue ( "CloudFrontRegion" );
		CloudFrontDistributionId = parameters.getValue ( "CloudFrontDistributionId" );
		CloudFrontComment = parameters.getValue ( "CloudFrontComment" );
		CloudFrontCallerReference = parameters.getValue ( "CloudFrontCallerReference" );
    	CloudFrontWaitForCompletion = parameters.getValue ( "CloudFrontWaitForCompletion" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( JGUIUtil.isSimpleJComboBoxItem(__S3Command_JComboBox, S3Command,JGUIUtil.NONE, null, null ) ) {
			__S3Command_JComboBox.select ( S3Command );
		}
		else {
            if ( (S3Command == null) ||	S3Command.equals("") ) {
				// New command...select the default.
				__S3Command_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"S3Command parameter \"" + S3Command + "\".  Select a value or Cancel." );
			}
		}
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
        // Populate the bucket choices, which depends on the above profile and region.
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
        if ( CopyFiles != null ) {
            __CopyFiles_JTextArea.setText ( CopyFiles );
        }
        // Populate the copy bucket choices, which depends on the above profile and region.
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __CopyBucket_JComboBox, true );
		if ( JGUIUtil.isSimpleJComboBoxItem(__CopyBucket_JComboBox, CopyBucket,JGUIUtil.NONE, null, null ) ) {
			__CopyBucket_JComboBox.select ( CopyBucket );
		}
		else {
            if ( (CopyBucket == null) || CopyBucket.equals("") ) {
				// New command...select the default.
            	if ( __CopyBucket_JComboBox.getItemCount() > 0 ) {
            		__CopyBucket_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CopyBucket parameter \"" + CopyBucket + "\".  Select a value or Cancel." );
			}
		}
        if ( CopyObjectsCountProperty != null ) {
            __CopyObjectsCountProperty_JTextField.setText ( CopyObjectsCountProperty );
        }
        if ( DeleteFiles != null ) {
            __DeleteFiles_JTextArea.setText ( DeleteFiles );
        }
        if ( DeleteFolders != null ) {
            __DeleteFolders_JTextArea.setText ( DeleteFolders );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__DeleteFoldersScope_JComboBox, DeleteFoldersScope,JGUIUtil.NONE, null, null ) ) {
			__DeleteFoldersScope_JComboBox.select ( DeleteFoldersScope );
		}
		else {
            if ( (DeleteFoldersScope == null) || DeleteFoldersScope.equals("") ) {
				// New command...select the default.
				__DeleteFoldersScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"DeleteFoldersScope parameter \"" + DeleteFoldersScope + "\".  Select a value or Cancel." );
			}
		}
        if ( DownloadFolders != null ) {
            __DownloadFolders_JTextArea.setText ( DownloadFolders );
        }
        if ( DeleteFoldersMinDepth != null ) {
            __DeleteFoldersMinDepth_JTextField.setText ( DeleteFoldersMinDepth );
        }
        if ( DownloadFiles != null ) {
            __DownloadFiles_JTextArea.setText ( DownloadFiles );
        }
        if ( ListBucketsRegEx != null ) {
            __ListBucketsRegEx_JTextField.setText ( ListBucketsRegEx );
        }
        if ( ListBucketsCountProperty != null ) {
            __ListBucketsCountProperty_JTextField.setText ( ListBucketsCountProperty );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListObjectsScope_JComboBox, ListObjectsScope,JGUIUtil.NONE, null, null ) ) {
			__ListObjectsScope_JComboBox.select ( ListObjectsScope );
		}
		else {
            if ( (ListObjectsScope == null) || ListObjectsScope.equals("") ) {
				// New command...select the default.
				__ListObjectsScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListObjectsScope parameter \"" + ListObjectsScope + "\".  Select a value or Cancel." );
			}
		}
        if ( Prefix != null ) {
            __Prefix_JTextField.setText ( Prefix );
        }
        if ( Delimiter != null ) {
            __Delimiter_JTextField.setText ( Delimiter );
        }
        if ( ListObjectsRegEx != null ) {
            __ListObjectsRegEx_JTextField.setText ( ListObjectsRegEx );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFiles_JComboBox, ListFiles,JGUIUtil.NONE, null, null ) ) {
			__ListFiles_JComboBox.select ( ListFiles );
		}
		else {
            if ( (ListFiles == null) ||	ListFiles.equals("") ) {
				// New command...select the default.
				__ListFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFiles parameter \"" + ListFiles + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFolders_JComboBox, ListFolders,JGUIUtil.NONE, null, null ) ) {
			__ListFolders_JComboBox.select ( ListFolders );
		}
		else {
            if ( (ListFolders == null) ||	ListFolders.equals("") ) {
				// New command...select the default.
				__ListFolders_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFolders parameter \"" + ListFolders + "\".  Select a value or Cancel." );
			}
		}
        if ( MaxKeys != null ) {
            __MaxKeys_JTextField.setText ( MaxKeys );
        }
        if ( MaxObjects != null ) {
            __MaxObjects_JTextField.setText ( MaxObjects );
        }
        if ( ListObjectsCountProperty != null ) {
            __ListObjectsCountProperty_JTextField.setText ( ListObjectsCountProperty );
        }
        if ( UploadFolders != null ) {
            __UploadFolders_JTextArea.setText ( UploadFolders );
        }
        if ( UploadFiles != null ) {
            __UploadFiles_JTextArea.setText ( UploadFiles );
        }
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
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__AppendOutput_JComboBox, AppendOutput,JGUIUtil.NONE, null, null ) ) {
			__AppendOutput_JComboBox.select ( AppendOutput );
		}
		else {
            if ( (AppendOutput == null) ||	AppendOutput.equals("") ) {
				// New command...select the default.
				__AppendOutput_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"AppendOutput parameter \"" + AppendOutput + "\".  Select a value or Cancel." );
			}
		}
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
		AwsS3CommandType command = AwsS3CommandType.valueOfIgnoreCase(S3Command);
		if ( command == AwsS3CommandType.COPY_OBJECTS ) {
			__main_JTabbedPane.setSelectedIndex(0);
		}
		else if ( command == AwsS3CommandType.DELETE_OBJECTS ) {
			__main_JTabbedPane.setSelectedIndex(1);
		}
		else if ( command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
			__main_JTabbedPane.setSelectedIndex(2);
		}
		else if ( (command == AwsS3CommandType.LIST_BUCKETS) ||
			(command == AwsS3CommandType.LIST_OBJECTS) ) {
			__main_JTabbedPane.setSelectedIndex(3);
		}
		else if ( command == AwsS3CommandType.UPLOAD_OBJECTS ) {
			__main_JTabbedPane.setSelectedIndex(4);
		}
		// Set the tab for selected S3 command.
		setTabForS3Command();
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	// General.
	S3Command = __S3Command_JComboBox.getSelected();
	Profile = __Profile_JComboBox.getSelected();
	if ( Profile == null ) {
		Profile = "";
	}
	Region = getSelectedRegion();
	Bucket = __Bucket_JComboBox.getSelected();
	if ( Bucket == null ) {
		Bucket = "";
	}
	// Copy.
	CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	CopyBucket = __CopyBucket_JComboBox.getSelected();
	if ( CopyBucket == null ) {
		CopyBucket = "";
	}
	CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	// Download.
	DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	// List bucket objects.
	ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	Prefix = __Prefix_JTextField.getText().trim();
	Delimiter = __Delimiter_JTextField.getText().trim();
	ListObjectsRegEx = __ListObjectsRegEx_JTextField.getText().trim();
	ListFiles = __ListFiles_JComboBox.getSelected();
	ListFolders = __ListFolders_JComboBox.getSelected();
	MaxKeys = __MaxKeys_JTextField.getText().trim();
	MaxObjects = __MaxObjects_JTextField.getText().trim();
	ListObjectsCountProperty = __ListObjectsCountProperty_JTextField.getText().trim();
	// Upload.
	UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	AppendOutput = __AppendOutput_JComboBox.getSelected();
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
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
    // General.
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "S3Command=" + S3Command );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "Bucket=" + Bucket );
	// Copy.
	props.add ( "CopyFiles=" + CopyFiles );
	props.add ( "CopyBucket=" + CopyBucket );
	props.add ( "CopyObjectsCountProperty=" + CopyObjectsCountProperty );
	// Delete.
	props.add ( "DeleteFiles=" + DeleteFiles );
	props.add ( "DeleteFolders=" + DeleteFolders );
	props.add ( "DeleteFoldersScope=" + DeleteFoldersScope );
	props.add ( "DeleteFoldersMinDepth=" + DeleteFoldersMinDepth );
	// Download.
	props.add ( "DownloadFolders=" + DownloadFolders );
	props.add ( "DownloadFiles=" + DownloadFiles );
	// List buckets.
	props.add ( "ListBucketsRegEx=" + ListBucketsRegEx );
	props.add ( "ListBucketsCountProperty=" + ListBucketsCountProperty );
	// List bucket objects.
	props.add ( "ListObjectsScope=" + ListObjectsScope );
	props.add ( "Prefix=" + Prefix );
	props.add ( "Delimiter=" + Delimiter );
	props.add ( "ListObjectsRegEx=" + ListObjectsRegEx );
	props.add ( "ListFiles=" + ListFiles );
	props.add ( "ListFolders=" + ListFolders );
	props.add ( "MaxKeys=" + MaxKeys );
	props.add ( "MaxObjects=" + MaxObjects );
	props.add ( "ListObjectsCountProperty=" + ListObjectsCountProperty );
	// Upload.
	props.add ( "UploadFolders=" + UploadFolders );
	props.add ( "UploadFiles=" + UploadFiles );
	// Output.
	props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "AppendOutput=" + AppendOutput );
	// CloudFront.
	props.add ( "InvalidateCloudFront=" + InvalidateCloudFront);
	props.add ( "CloudFrontRegion=" + CloudFrontRegion );
	props.add ( "CloudFrontDistributionId=" + CloudFrontDistributionId );
	props.add ( "CloudFrontComment=" + CloudFrontComment );
	props.add ( "CloudFrontCallerReference=" + CloudFrontCallerReference );
    props.add ( "CloudFrontWaitForCompletion=" + CloudFrontWaitForCompletion );
	//props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Set the default values as FYI.
	AwsToolkit.getInstance().uiPopulateProfileDefault(__ProfileDefault_JTextField, __ProfileDefaultNote_JLabel);
	AwsToolkit.getInstance().uiPopulateRegionDefault( __Profile_JComboBox.getSelected(), __RegionDefault_JTextField, __RegionDefaultNote_JLabel);
	// Check the path and determine what the label on the path button should be.
    if ( __pathOutput_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__pathOutput_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutput_JButton.setText ( __AddWorkingDirectory );
            	__pathOutput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutput_JButton.setEnabled(false);
		}
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
 * Set the parameter tab based on the selected command.
 */
private void setTabForS3Command() {
	String command = __S3Command_JComboBox.getSelected();
	if ( command.equalsIgnoreCase("" + AwsS3CommandType.COPY_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.DELETE_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(1);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.DOWNLOAD_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(2);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.LIST_BUCKETS) ) {
		__main_JTabbedPane.setSelectedIndex(3);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.LIST_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(4);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.UPLOAD_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(5);
	}
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