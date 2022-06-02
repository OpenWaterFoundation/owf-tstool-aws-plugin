// AwsS3_JDialog - editor for AwsS3 command

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

import org.openwaterfoundation.tstool.plugin.aws.AwsS3CommandType;
import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import RTi.Util.GUI.DictionaryJDialog;
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
import software.amazon.awssdk.services.s3.model.Bucket;

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
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __Profile_JComboBox = null;
private SimpleJComboBox __S3Command_JComboBox = null;
private SimpleJComboBox __Region_JComboBox = null;
private SimpleJComboBox __Bucket_JComboBox = null;
private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Copy tab.
private JTextField __CopySourceKey_JTextField = null;
private JTextField __CopyDestKey_JTextField = null;

// Delete tab.
private JTextField __DeleteKey_JTextField = null;

// Download tab.
private JTextArea __DownloadFiles_JTextArea = null;
private JTextArea __DownloadDirectories_JTextArea = null;

// List tab.
private JTextField __MaxKeys_JTextField = null;
private JTextField __Prefix_JTextField = null;
private JTextField __MaxObjects_JTextField = null;

// Upload tab.
private JTextArea __UploadFiles_JTextArea = null;
private JTextArea __UploadDirectories_JTextArea = null;

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private AwsS3_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private boolean ignoreEvents = false; // Ignore events when initializing, to avoid infinite loop.
private JFrame __parent = null;

// AWS session used to interact with AWS:
// - will be null until the profile is set, which will happen when refresh() is called once
private AwsSession awsSession = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
public AwsS3_JDialog ( JFrame parent, AwsS3_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	if ( this.ignoreEvents ) {
        return; // Startup.
    }

	Object o = event.getSource();

	if ( o == __browseOutput_JButton ) {
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
					Message.printWarning ( 1,"CopyFile_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadDirectories") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadDirectories = __DownloadDirectories_JTextArea.getText().trim();
        String [] notes = {
            "Specify the S3 bucket object prefix (e.g., topfolder/childfolder/) to download a directory (folder).",
            "Only directories can be downloaded. Specify files to download with the 'DownloadFiles' command parameter.",
            "A trailing / in the bucket prefix (S3 directory path) is optional (typically include to indicate a directory).",
            "A leading / in the bucket prefix (S3 directory path) is optional (typically omit since relative to the bucket).",
            "The local folder is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadDirectories,
            "Edit DownloadDirectories Parameter", notes, "Bucket Prefix (S3 directory path)", "Local Folder",10)).response();
        if ( dict != null ) {
            __DownloadDirectories_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFiles = __DownloadFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the bucket object S3 key (S3 file path) to download a file.",
            "Only files can be downloaded.  Specify directories (folders) to download with the 'DownloadDirectories' command parameter.",
            "The key is the full path for the the bucket object.",
            "A leading / in the S3 key is optional (typically omit since relative to the bucket).",
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFiles,
            "Edit DownloadFiles Parameter", notes, "Bucket Key (S3 object path)", "Local File",10)).response();
        if ( dict != null ) {
            __DownloadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CopyFile");
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
                Message.printWarning ( 1,"CopyFile_JDialog",
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadDirectories") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadDirectories = __UploadDirectories_JTextArea.getText().trim();
        String [] notes = {
            "The local folder is relative to the working folder:",
            "  " + this.__working_dir,
            "Specify the bucket object prefix (e.g., topfolder/childfolder/) to upload a directory (folder).",
            "The S3 location will be created if it does not exist, or overwritten if it does exist.",
            "Only directories can be uploaded. Specify files to upload with the 'UploadFiles' command parameter.",
            "A trailing / in the bucket prefix (S3 directory path) is optional (typically include to indicate a directory).",
            "A leading / in the bucket prefix (S3 directory path) is optional (typically omit since relative to the bucket).",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadDirectories,
            "Edit UploadDirectories Parameter", notes, "Local Folder", "Bucket Prefix (S3 directory path)", 10)).response();
        if ( dict != null ) {
            __UploadDirectories_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadFiles = __UploadFiles_JTextArea.getText().trim();
        String [] notes = {
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "Specify the bucket object key (S3 file path) to upload a file.",
            "Only files can be uploaded. Specify directories (folder) to upload with the 'UploadDirectories' command parameter.",
            "The key is the full path for the the bucket object.",
            "A leading / in the S3 key is optional (typically omit since relative to the bucket).",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadFiles,
            "Edit UploadFiles Parameter", notes, "Local File", "Bucket Key (S3 object path)", 10)).response();
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String S3Command = __S3Command_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String CopySourceKey = __CopySourceKey_JTextField.getText().trim();
	String CopyDestKey = __CopyDestKey_JTextField.getText().trim();
	String DownloadDirectories = __DownloadDirectories_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	String DeleteKey = __DeleteKey_JTextField.getText().trim();
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String Prefix = __Prefix_JTextField.getText().trim();
	String UploadDirectories = __UploadDirectories_JTextArea.getText().trim().replace("\n"," ");
	String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( S3Command.length() > 0 ) {
		props.set ( "S3Command", S3Command );
	}
	if ( Profile.length() > 0 ) {
		props.set ( "Profile", Profile );
	}
	if ( Region.length() > 0 ) {
		props.set ( "Region", Region );
	}
	if ( (Bucket != null) && !Bucket.isEmpty() ) {
		props.set ( "Bucket", Bucket );
	}
	if ( (CopySourceKey != null) && !CopySourceKey.isEmpty() ) {
		props.set ( "CopySourceKey", CopySourceKey );
	}
	if ( (CopyDestKey != null) && !CopyDestKey.isEmpty() ) {
		props.set ( "CopyDestKey", CopyDestKey );
	}
	if ( (DeleteKey != null) && !DeleteKey.isEmpty() ) {
		props.set ( "DeleteKey", DeleteKey );
	}
	if ( (DownloadDirectories != null) && !DownloadDirectories.isEmpty() ) {
		props.set ( "DownloadDirectories", DownloadDirectories );
	}
	if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
		props.set ( "DownloadFiles", DownloadFiles );
	}
	if ( (DeleteKey != null) && !DeleteKey.isEmpty() ) {
		props.set ( "DeleteKey", DeleteKey );
	}
	if ( (MaxKeys != null) && !MaxKeys.isEmpty() ) {
		props.set ( "MaxKeys", MaxKeys );
	}
	if ( (MaxObjects != null) && !MaxObjects.isEmpty() ) {
		props.set ( "MaxObjects", MaxObjects );
	}
	if ( (Prefix != null) && !Prefix.isEmpty() ) {
		props.set ( "Prefix", Prefix );
	}
	if ( (UploadDirectories != null) && !UploadDirectories.isEmpty() ) {
		props.set ( "UploadDirectories", UploadDirectories );
	}
	if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
		props.set ( "UploadFiles", UploadFiles );
	}
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( OutputTableID.length() > 0 ) {
        props.set ( "OutputTableID", OutputTableID );
    }
	if ( IfInputNotFound.length() > 0 ) {
		props.set ( "IfInputNotFound", IfInputNotFound );
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
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits () {
	String S3Command = __S3Command_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String CopySourceKey = __CopySourceKey_JTextField.getText().trim();
	String CopyDestKey = __CopyDestKey_JTextField.getText().trim();
	String DeleteKey = __DeleteKey_JTextField.getText().trim();
	String DownloadDirectories = __DownloadDirectories_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String Prefix = __Prefix_JTextField.getText().trim();
	String UploadDirectories = __UploadDirectories_JTextArea.getText().trim().replace("\n"," ");
	String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "S3Command", S3Command );
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	__command.setCommandParameter ( "Bucket", Bucket );
	__command.setCommandParameter ( "CopySourceKey", CopySourceKey );
	__command.setCommandParameter ( "CopyDestKey", CopyDestKey );
	__command.setCommandParameter ( "DeleteKey", DeleteKey );
	__command.setCommandParameter ( "DownloadDirectories", DownloadDirectories );
	__command.setCommandParameter ( "DownloadFiles", DownloadFiles );
	__command.setCommandParameter ( "MaxKeys", MaxKeys );
	__command.setCommandParameter ( "MaxObjects", MaxObjects );
	__command.setCommandParameter ( "Prefix", Prefix );
	__command.setCommandParameter ( "UploadDirectories", UploadDirectories );
	__command.setCommandParameter ( "UploadFiles", UploadFiles );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
}

/**
Return the selected region, omitting the trailing description.
*/
private String getSelectedRegion() {
    if ( __Region_JComboBox == null ) {
        return null;
    }
    String region = __Region_JComboBox.getSelected();
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
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
private void initialize ( JFrame parent, AwsS3_Command command, List<String> tableIDChoices )
{	this.__command = command;
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Execute Amazon S3 actions." ),
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
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - S3 command to run."),
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
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - profile for authentication (default=only profile or 'default')."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
	// TODO smalers 2022-05-39 evaluate whether region can default.
	//regionChoices.add(""); // Default - region is not specified.
	__Region_JComboBox.setData(regionChoices);
	__Region_JComboBox.select ( 0 );
	__Region_JComboBox.addItemListener ( this );
   JGUIUtil.addComponent(main_JPanel, __Region_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - AWS region (default=determined by service)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Bucket:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bucket_JComboBox = new SimpleJComboBox ( false );
	__Bucket_JComboBox.setToolTipText("AWS S3 bucket.");
	// Choices will be populated when refreshed, based on profile.
	__Bucket_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Bucket_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - S3 bucket (required by some services)."),
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

    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Specify S3 object to copy."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Source key:"),
        0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopySourceKey_JTextField = new JTextField ( "", 30 );
    __CopySourceKey_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopySourceKey_JTextField,
        1, yCopy, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Optional - Source key for object to copy."),
        3, yCopy, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Destination key:"),
        0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyDestKey_JTextField = new JTextField ( "", 30 );
    __CopyDestKey_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopyDestKey_JTextField,
        1, yCopy, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Optional - Dest key for object to copy."),
        3, yCopy, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Delete' parameters:
    // - specify S3 files and folders to delete
    int yDelete = -1;
    JPanel delete_JPanel = new JPanel();
    delete_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Delete", delete_JPanel );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Specify S3 object to delete."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Delete key:"),
        0, ++yDelete, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteKey_JTextField = new JTextField ( "", 30 );
    __DeleteKey_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(delete_JPanel, __DeleteKey_JTextField,
        1, yDelete, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Optional - Source key for object to copy."),
        3, yDelete, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Download' parameters:
    // - map bucket objects to files and folders
    int yDownload = -1;
    JPanel download_JPanel = new JPanel();
    download_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Download", download_JPanel );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Specify files and folders to download."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Use the 'Edit' button to view information about local and S3 file and folder paths."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download directories:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadDirectories_JTextArea = new JTextArea (6,35);
    __DownloadDirectories_JTextArea.setLineWrap ( true );
    __DownloadDirectories_JTextArea.setWrapStyleWord ( true );
    __DownloadDirectories_JTextArea.setToolTipText("Key1:Folder1,Key2:Folder2,...");
    __DownloadDirectories_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadDirectories_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("S3 bucket key(s) (prefix) and local folder(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadDirectories",this),
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

    // Panel for 'List' parameters:
    // - this includes filtering
    int yBucketObjects = -1;
    JPanel bucketObjects_JPanel = new JPanel();
    bucketObjects_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List", bucketObjects_JPanel );

    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ("Use the following to control listing S3 objects."),
		0, ++yBucketObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(bucketObjects_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yBucketObjects, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Maximum keys:"),
        0, ++yBucketObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxKeys_JTextField = new JTextField ( "", 10 );
    __MaxKeys_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(bucketObjects_JPanel, __MaxKeys_JTextField,
        1, yBucketObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Optional - Maximum number of object keys read per request (default=1000)."),
        3, yBucketObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Key prefix to match:"),
        0, ++yBucketObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Prefix_JTextField = new JTextField ( "", 30 );
    __Prefix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(bucketObjects_JPanel, __Prefix_JTextField,
        1, yBucketObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Optional - object key prefix to match (default=match all)."),
        3, yBucketObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Maximum objects:"),
        0, ++yBucketObjects, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxObjects_JTextField = new JTextField ( "", 10 );
    __MaxObjects_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(bucketObjects_JPanel, __MaxObjects_JTextField,
        1, yBucketObjects, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(bucketObjects_JPanel, new JLabel ( "Optional - Maximum number of object read (default=2000)."),
        3, yBucketObjects, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Upload' parameters:
    // - map files and folders to bucket objects
    int yUpload = -1;
    JPanel upload_JPanel = new JPanel();
    upload_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Upload", upload_JPanel );

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Specify files and directories (folders) to upload."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Use the 'Edit' button to view information about local and S3 file and folder paths."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Upload directories:"),
        0, ++yUpload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UploadDirectories_JTextArea = new JTextArea (6,35);
    __UploadDirectories_JTextArea.setLineWrap ( true );
    __UploadDirectories_JTextArea.setWrapStyleWord ( true );
    __UploadDirectories_JTextArea.setToolTipText("Folder1:Key1,Folder2:Key2,...");
    __UploadDirectories_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(upload_JPanel, new JScrollPane(__UploadDirectories_JTextArea),
        1, yUpload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Local folder(s) and S3 bucket key(s) (prefix)."),
        3, yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(upload_JPanel, new SimpleJButton ("Edit","EditUploadDirectories",this),
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

    JGUIUtil.addComponent(output_JPanel, new JLabel ("The following are used for commands that generate bucket and bucket object lists."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: 'csv"),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Output file:" ), 
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify the output file to copy, can use ${Property} notation.");
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
		"Optional - action if input file is not found (default=" + __command._Warn + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
    if ( o == this.__Profile_JComboBox ) {
		populateBucketChoices();
	}
	else if ( o == this.__Region_JComboBox ) {
		populateBucketChoices();
	}
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
 * Populate the Bucket choices based no other selections.
 */
private void populateBucketChoices() {
	String routine = getClass().getSimpleName() + ".populateBucketChoices";
	boolean debug = true;
	if ( awsSession == null ) {
		// Startup - can't populate the buckets.
		if ( debug ) {
			Message.printStatus(2, routine, "Startup - not populating the list of buckets." );
		}
		return;
	}
	else {
		if ( debug ) {
			Message.printStatus(2, routine, "Getting the list of buckets." );
		}
		// Get the list of buckets.
		String region = getSelectedRegion();
		if ( region == null ) {
			// Startup - can't populate the buckets.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is null - can't populate the list of buckets." );
			}
			return;
		}
		else {
			// Have a region.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is \"" + region + "\" - populating the list of buckets." );
			}	
			List<Bucket> buckets = AwsToolkit.getInstance().getBuckets(awsSession, region);
			List<String> bucketChoices = new ArrayList<>();
			for ( Bucket bucket : buckets ) {
				bucketChoices.add ( bucket.name() );
				if ( debug ) {
					Message.printStatus(2, routine, "Populated bucket: " + bucket.name() );
				}
			}
			Collections.sort(bucketChoices);
			// Add a blank because some services don't use
			bucketChoices.add(0,"");
			__Bucket_JComboBox.setData(bucketChoices);
			if ( __Bucket_JComboBox.getItemCount() > 0 ) {
				// Select the first bucket by default.
				__Bucket_JComboBox.select ( 0 );
			}
		}
	}
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String S3Command = "";
	String Profile = "";
	String Region = "";
	String Bucket = "";
	String CopySourceKey = "";
	String CopyDestKey = "";
	String DeleteKey = "";
	String DownloadDirectories = "";
	String DownloadFiles = "";
	String MaxKeys = "";
	String Prefix = "";
	String MaxObjects = "";
	String UploadDirectories = "";
	String UploadFiles = "";
	String OutputTableID = "";
	String OutputFile = "";
	String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		S3Command = parameters.getValue ( "S3Command" );
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		Bucket = parameters.getValue ( "Bucket" );
		CopySourceKey = parameters.getValue ( "CopySourceKey" );
		CopyDestKey = parameters.getValue ( "CopyDestKey" );
		DeleteKey = parameters.getValue ( "DeleteKey" );
		DownloadDirectories = parameters.getValue ( "DownloadDirectories" );
		DownloadFiles = parameters.getValue ( "DownloadFiles" );
		MaxKeys = parameters.getValue ( "MaxKeys" );
		Prefix = parameters.getValue ( "Prefix" );
		MaxObjects = parameters.getValue ( "MaxObjects" );
		UploadDirectories = parameters.getValue ( "UploadDirectories" );
		UploadFiles = parameters.getValue ( "UploadFiles" );
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
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
        int [] index = new int[1];
        //Message.printStatus(2,routine,"Checking to see if Region=\"" + Region + "\" is a choice.");
        // Choice values are similar to the following so need to parse by " - ", not just "-".
        if ( JGUIUtil.isSimpleJComboBoxItem(__Region_JComboBox, Region, JGUIUtil.CHECK_SUBSTRINGS, "seq: - ", 0, index, true ) ) {
            // Existing command so select the matching choice.
            //Message.printStatus(2,routine,"Region=\"" + Region + "\" was a choice, selecting index " + index[0] + "...");
            __Region_JComboBox.select(index[0]);
        }
        else {
            Message.printStatus(2,routine,"Region=\"" + Region + "\" was not a choice.");
            if ( (Region == null) || Region.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Region parameter \"" + Region + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        populateBucketChoices();
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
        if ( CopySourceKey != null ) {
            __CopySourceKey_JTextField.setText ( CopySourceKey );
        }
        if ( CopyDestKey != null ) {
            __CopyDestKey_JTextField.setText ( CopyDestKey );
        }
        if ( DeleteKey != null ) {
            __DeleteKey_JTextField.setText ( DeleteKey );
        }
        if ( DownloadDirectories != null ) {
            __DownloadDirectories_JTextArea.setText ( DownloadDirectories );
        }
        if ( DownloadFiles != null ) {
            __DownloadFiles_JTextArea.setText ( DownloadFiles );
        }
        if ( MaxKeys != null ) {
            __MaxKeys_JTextField.setText ( MaxKeys );
        }
        if ( Prefix != null ) {
            __Prefix_JTextField.setText ( Prefix );
        }
        if ( MaxObjects != null ) {
            __MaxObjects_JTextField.setText ( MaxObjects );
        }
        if ( UploadDirectories != null ) {
            __UploadDirectories_JTextArea.setText ( UploadDirectories );
        }
        if ( UploadFiles != null ) {
            __UploadFiles_JTextArea.setText ( UploadFiles );
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
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
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
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
	CopySourceKey = __CopySourceKey_JTextField.getText().trim();
	CopyDestKey = __CopyDestKey_JTextField.getText().trim();
	DeleteKey = __DeleteKey_JTextField.getText().trim();
	DownloadDirectories = __DownloadDirectories_JTextArea.getText().trim().replace("\n"," ");
	DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	MaxKeys = __MaxKeys_JTextField.getText().trim();
	Prefix = __Prefix_JTextField.getText().trim();
	MaxObjects = __MaxObjects_JTextField.getText().trim();
	UploadDirectories = __UploadDirectories_JTextArea.getText().trim().replace("\n"," ");
	UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	OutputFile = __OutputFile_JTextField.getText().trim();
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "S3Command=" + S3Command );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "Bucket=" + Bucket );
	props.add ( "CopySourceKey=" + CopySourceKey );
	props.add ( "CopyDestKey=" + CopyDestKey );
	props.add ( "DeleteKey=" + DeleteKey );
	props.add ( "DownloadDirectories=" + DownloadDirectories );
	props.add ( "DownloadFiles=" + DownloadFiles );
	props.add ( "MaxKeys=" + MaxKeys );
	props.add ( "Prefix=" + Prefix );
	props.add ( "MaxObjects=" + MaxObjects );
	props.add ( "UploadDirectories=" + UploadDirectories );
	props.add ( "UploadFiles=" + UploadFiles );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props) );
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
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}