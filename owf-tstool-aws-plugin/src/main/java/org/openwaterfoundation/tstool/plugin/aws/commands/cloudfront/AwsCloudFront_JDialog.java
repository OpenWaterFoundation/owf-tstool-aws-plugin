// AwsCloudFront_JDialog - editor for AwsCloudFront command

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

package org.openwaterfoundation.tstool.plugin.aws.commands.cloudfront;

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
public class AwsCloudFront_JDialog extends JDialog
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
private SimpleJComboBox __CloudFrontCommand_JComboBox = null;
private SimpleJComboBox __Region_JComboBox = null;
private JTextField __RegionDefault_JTextField = null; // View only (not a command parameter).
private JLabel __RegionDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __DistributionId_JComboBox = null;
private JTextArea __Tags_JTextArea = null;
private JTextField __Comment_JTextField = null;
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Invalidate tab.
private JTextArea __InvalidationPaths_JTextArea = null;
private JTextField __CallerReference_JTextField = null;
private SimpleJComboBox __WaitForCompletion_JComboBox = null;

// List Distributions tab.
private JTextField __ListDistributionsCountProperty_JTextField = null;

// List Invalidations tab.
private JTextField __ListInvalidationsCountProperty_JTextField = null;
private SimpleJComboBox __InvalidationStatus_JComboBox = null;

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private AwsCloudFront_Command __command = null;
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
public AwsCloudFront_JDialog ( JFrame parent, AwsCloudFront_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	String routine = getClass().getSimpleName() + ".actionPerformed";
	if ( this.ignoreEvents ) {
        return; // Startup.
    }

	Object o = event.getSource();

    if ( o == this.__CloudFrontCommand_JComboBox ) {
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
    else if ( event.getActionCommand().equalsIgnoreCase("EditTags") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String Tags = __Tags_JTextArea.getText().trim();
        String [] notes = {
            "Match the CloudFront distribution by matching the tag(s).",
            "All specified tags must be matched to match the distribution.",
            "Tag Name - distribution tag name to match (can use ${property})",
            "Tag Value - distribution tag value to match (can use ${property})"
        };
        String tagDict = (new DictionaryJDialog ( __parent, true, Tags, "Edit Tags Parameter",
            notes, "Tag Name", "Tag Value",10)).response();
        if ( tagDict != null ) {
            __Tags_JTextArea.setText ( tagDict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AwsCloudFront", PluginMeta.getDocumentationRootUrl() );
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditInvalidationPaths") ) {
        // Edit the list in the dialog.  It is OK for the string to be blank.
        String InvalidationPaths = __InvalidationPaths_JTextArea.getText().trim();
        String [] notes = {
            "Invalidation paths should start with / and match the S3 bucket key for the CloudFront distribution.",
            "The top-level S3 bucket does not need to start with /.",
            "Use * at the end of a path as a wildcard to match patterns.",
            "   /* - invalidate all files in a distribution",
            "   /path/* - invalidate all files in a folder, including subfolders",
            "   /path* - invalidate all files in a folder matching a leading path part",
            "   /path/filename.* - invalidate all files in a folder matching an extension",
            "   /path/filename* - invalidate all files in a folder matching any extension",
            "Use the checkboxes with Insert and Remove.",
            "All non-blank paths will be included in the command parameter.",
            "Paths can use ${Property} syntax."
        };
        String delim = ",";
        String list = (new StringListJDialog ( this.__parent, true, InvalidationPaths,
            "Edit InvalidationPaths Parameter", notes, "Invalidation Path", delim, 10)).response();
        if ( list != null ) {
            __InvalidationPaths_JTextArea.setText ( list );
            refresh();
        }
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
                Message.printWarning ( 1,"AwsCloudFront_JDialog",
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
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
	String CloudFrontCommand = __CloudFrontCommand_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String DistributionId = __DistributionId_JComboBox.getSelected();
	String Tags = __Tags_JTextArea.getText().trim().replace("\n"," ");
	String Comment = __Comment_JTextField.getText().trim();
	String InvalidationPaths = __InvalidationPaths_JTextArea.getText().trim().replace("\n"," ");
	String CallerReference = __CallerReference_JTextField.getText().trim();
    String WaitForCompletion = __WaitForCompletion_JComboBox.getSelected();
	String ListDistributionsCountProperty = __ListDistributionsCountProperty_JTextField.getText().trim();
    String InvalidationStatus = __InvalidationStatus_JComboBox.getSelected();
	String ListInvalidationsCountProperty = __ListInvalidationsCountProperty_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( CloudFrontCommand.length() > 0 ) {
		props.set ( "CloudFrontCommand", CloudFrontCommand );
	}
	if ( Profile.length() > 0 ) {
		props.set ( "Profile", Profile );
	}
	if ( Region.length() > 0 ) {
		props.set ( "Region", Region );
	}
	if ( (DistributionId != null) && !DistributionId.isEmpty() ) {
		props.set ( "DistributionId", DistributionId );
	}
	if ( (Tags != null) && !Tags.isEmpty() ) {
		props.set ( "Tags", Tags );
	}
	if ( (Comment != null) && !Comment.isEmpty() ) {
		props.set ( "Comment", Comment );
	}
	if ( (InvalidationPaths != null) && !InvalidationPaths.isEmpty() ) {
		props.set ( "InvalidationPaths", InvalidationPaths );
	}
	if ( (CallerReference != null) && !CallerReference.isEmpty() ) {
		props.set ( "CallerReference", CallerReference );
	}
	if ( (WaitForCompletion != null) && !WaitForCompletion.isEmpty() ) {
		props.set ( "WaitForCompletion", WaitForCompletion );
	}
	if ( (ListDistributionsCountProperty != null) && !ListDistributionsCountProperty.isEmpty() ) {
		props.set ( "ListDistributionsCountProperty", ListDistributionsCountProperty );
	}
	if ( (InvalidationStatus != null) && !InvalidationStatus.isEmpty() ) {
		props.set ( "InvalidationStatus", InvalidationStatus );
	}
	if ( (ListInvalidationsCountProperty != null) && !ListInvalidationsCountProperty.isEmpty() ) {
		props.set ( "ListInvalidationsCountProperty", ListInvalidationsCountProperty );
	}
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( OutputTableID.length() > 0 ) {
        props.set ( "OutputTableID", OutputTableID );
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
	String CloudFrontCommand = __CloudFrontCommand_JComboBox.getSelected();
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String DistributionId = __DistributionId_JComboBox.getSelected();
	String Tags = __Tags_JTextArea.getText().trim().replace("\n"," ");
	String Comment = __Comment_JTextField.getText().trim();
	String InvalidationPaths = __InvalidationPaths_JTextArea.getText().trim().replace("\n"," ");
	String CallerReference = __CallerReference_JTextField.getText().trim();
    String WaitForCompletion = __WaitForCompletion_JComboBox.getSelected();
	String ListDistributionsCountProperty = __ListDistributionsCountProperty_JTextField.getText().trim();
    String InvalidationStatus = __InvalidationStatus_JComboBox.getSelected();
	String ListInvalidationsCountProperty = __ListInvalidationsCountProperty_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "CloudFrontCommand", CloudFrontCommand );
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	__command.setCommandParameter ( "DistributionId", DistributionId );
	__command.setCommandParameter ( "Tags", Tags );
	__command.setCommandParameter ( "Comment", Comment );
	__command.setCommandParameter ( "InvalidationPaths", InvalidationPaths );
	__command.setCommandParameter ( "CallerReference", CallerReference );
	__command.setCommandParameter ( "WaitForCompletion", WaitForCompletion );
	__command.setCommandParameter ( "ListDistributionsCountProperty", ListDistributionsCountProperty );
	__command.setCommandParameter ( "InvalidationStatus", InvalidationStatus );
	__command.setCommandParameter ( "ListInvalidationsCountProperty", ListInvalidationsCountProperty );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	//__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
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
private void initialize ( JFrame parent, AwsCloudFront_Command command, List<String> tableIDChoices ) {
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Execute Amazon Web Services (AWS) CloudFront commands." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("CloudFront uses 'distributions' to manage files in a content delivery network (CDN)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"CloudFront distributions are often associated with files stored in an S3 bucket, for example to publish https://sub.organization.org content." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("The 'aws-global' region may need to be used for CloudFront distributions (not the S3 region)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify a distribution by matching the distribution ID, tag(s), or comment (description),"),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "for exmaple if the comment includes the website domain name."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that output file names are relative to the working directory, which is:"),
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
        	"<html><b>ERROR: User's AWS configuration does not exist (errors will occur): " + AwsToolkit.getInstance().getAwsUserConfigFile() + "</b></html>" ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"User's AWS configuration: " + AwsToolkit.getInstance().getAwsUserConfigFile() ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "CloudFront command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CloudFrontCommand_JComboBox = new SimpleJComboBox ( false );
	__CloudFrontCommand_JComboBox.setToolTipText("CloudFront command to execute.");
	List<String> commandChoices = AwsCloudFrontCommandType.getChoicesAsStrings(false);
	__CloudFrontCommand_JComboBox.setData(commandChoices);
	__CloudFrontCommand_JComboBox.select ( 0 );
	__CloudFrontCommand_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CloudFrontCommand_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - CloudFront command to run."),
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
	__Region_JComboBox.setToolTipText("AWS region that server requests should be sent to.");
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
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - AWS region (default=see below)."),
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Distribution ID:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DistributionId_JComboBox = new SimpleJComboBox ( false );
	__DistributionId_JComboBox.setToolTipText("AWS CloudFront distribution ID.");
	// Choices will be populated when refreshed, based on profile.
	__DistributionId_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DistributionId_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - distribution ID (specify the distribution using ID)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("CloudFront distribution tag(s):"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Tags_JTextArea = new JTextArea (3,35);
    __Tags_JTextArea.setLineWrap ( true );
    __Tags_JTextArea.setWrapStyleWord ( true );
    __Tags_JTextArea.setToolTipText("Distribution tag(s) to match in format Tag1:Value1,Tag2:Value2, can use ${Property}.");
    __Tags_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Tags_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - tag(s) to match."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditTags",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Comment:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Comment_JTextField = new JTextField ( "", 20 );
    __Comment_JTextField.setToolTipText("Distribution comment (description) to match, use * for wildcard, ${Property} can be used.");
    __Comment_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Comment_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - comment to match (specify the distribution comment, e.g., *sub.domain*)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'Invalidate' parameters.
    int yInvalidate = -1;
    JPanel invalidate_JPanel = new JPanel();
    invalidate_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Invalidate", invalidate_JPanel );

    JGUIUtil.addComponent(invalidate_JPanel, new JLabel (
    	"Files that are part of a CloudFront distribution need to be \"invalidated\" to be visible in CloudFront-served websites."),
		0, ++yInvalidate, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel (
    	"Otherwise, uploaded files will not be visible until a later time, which is often unpredictable."),
		0, ++yInvalidate, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ("Specify CloudFront paths to invalidate, separated by commas."),
		0, ++yInvalidate, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yInvalidate, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ("Invalidation paths:"),
        0, ++yInvalidate, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InvalidationPaths_JTextArea = new JTextArea (6,35);
    __InvalidationPaths_JTextArea.setLineWrap ( true );
    __InvalidationPaths_JTextArea.setWrapStyleWord ( true );
    __InvalidationPaths_JTextArea.setToolTipText("Invalidation paths, separated by commas.");
    __InvalidationPaths_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(invalidate_JPanel, new JScrollPane(__InvalidationPaths_JTextArea),
        1, yInvalidate, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ("CloudFront paths, separated by commas."),
        3, yInvalidate, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(invalidate_JPanel, new SimpleJButton ("Edit","EditInvalidationPaths",this),
        3, ++yInvalidate, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    yInvalidate += 6;
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ( "Caller reference:"),
        0, ++yInvalidate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CallerReference_JTextField = new JTextField ( "", 30 );
    __CallerReference_JTextField.setToolTipText("Unique identifier for invalidation, to avoid duplicates, ${Property} can be used.");
    __CallerReference_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(invalidate_JPanel, __CallerReference_JTextField,
        1, yInvalidate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ( "Optional - caller reference (default=user and current time)."),
        3, yInvalidate, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(invalidate_JPanel, new JLabel ("Wait for completion?:"),
        0, ++yInvalidate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WaitForCompletion_JComboBox = new SimpleJComboBox ( false );
    __WaitForCompletion_JComboBox.add ( "" );
    __WaitForCompletion_JComboBox.add ( __command._False );
    __WaitForCompletion_JComboBox.add ( __command._True );
    __WaitForCompletion_JComboBox.addItemListener ( this );
    __WaitForCompletion_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(invalidate_JPanel, __WaitForCompletion_JComboBox,
        1, yInvalidate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(invalidate_JPanel, new JLabel (
        "Optional - wait for invalidation to complete (default=" + __command._True + ")."),
        3, yInvalidate, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for 'List Distributions' parameters:
    // - this includes filtering
    int yListDist = -1;
    JPanel listDist_JPanel = new JPanel();
    listDist_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Distributions", listDist_JPanel );

    JGUIUtil.addComponent(listDist_JPanel, new JLabel ("List CloudFront distributions."),
		0, ++yListDist, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDist_JPanel, new JLabel ("Use the Output tab to specify an output table and/or file."),
		0, ++yListDist, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDist_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListDist, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listDist_JPanel, new JLabel("List distributions count property:"),
        0, ++yListDist, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListDistributionsCountProperty_JTextField = new JTextField ( "", 30 );
    __ListDistributionsCountProperty_JTextField.setToolTipText("Specify the property name for the list result size, can use ${Property} notation");
    __ListDistributionsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listDist_JPanel, __ListDistributionsCountProperty_JTextField,
        1, yListDist, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDist_JPanel, new JLabel ( "Optional - processor property to set as list count." ),
        3, yListDist, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'List Invalidations' parameters:
    // - this includes filtering
    int yListInv = -1;
    JPanel listInv_JPanel = new JPanel();
    listInv_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Invalidations", listInv_JPanel );

    JGUIUtil.addComponent(listInv_JPanel, new JLabel ("List CloudFront invalidations."),
		0, ++yListInv, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDist_JPanel, new JLabel ("Use the Output tab to specify an output table and/or file."),
		0, ++yListInv, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listInv_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListInv, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listInv_JPanel, new JLabel ("Invalidation status:"),
        0, ++yListInv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InvalidationStatus_JComboBox = new SimpleJComboBox ( false );
    __InvalidationStatus_JComboBox.add ( "" );
    __InvalidationStatus_JComboBox.add ( __command._All );
    __InvalidationStatus_JComboBox.add ( __command._Completed );
    __InvalidationStatus_JComboBox.add ( __command._InProcess );
    __InvalidationStatus_JComboBox.addItemListener ( this );
    __InvalidationStatus_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(listInv_JPanel, __InvalidationStatus_JComboBox,
        1, yListInv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listInv_JPanel, new JLabel (
        "Optional - invalidation status to list (default=" + __command._InProcess + ")."),
        3, yListInv, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(listInv_JPanel, new JLabel("List invalidations count property:"),
        0, ++yListInv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListInvalidationsCountProperty_JTextField = new JTextField ( "", 30 );
    __ListInvalidationsCountProperty_JTextField.setToolTipText("Specify the property name for the list result size, can use ${Property} notation");
    __ListInvalidationsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listInv_JPanel, __ListInvalidationsCountProperty_JTextField,
        1, yListInv, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listInv_JPanel, new JLabel ( "Optional - processor property to set as list count." ),
        3, yListInv, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
    	"The following are used with the ListDistributions and ListInvalidations CloudFront commands, which generate output lists."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: csv"),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output table ID:" ),
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
	if ( o == this.__CloudFrontCommand_JComboBox ) {
		setTabForS3Command();
	}
	else if ( o == this.__Profile_JComboBox ) {
    	this.awsSession.setProfile(this.__Profile_JComboBox.getSelected());
		populateDistributionIdChoices();
	}
	else if ( o == this.__Region_JComboBox ) {
		populateDistributionIdChoices();
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
private void populateDistributionIdChoices() {
	String routine = getClass().getSimpleName() + ".populateDistributionIdChoices";
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
		// Get the list of buckets.
		String region = getSelectedRegion();
		if ( region == null ) {
			// Startup - can't populate the buckets.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is null - can't populate the list of distributions." );
			}
			return;
		}
		else {
			// Have a region.
			if ( debug ) {
				Message.printStatus(2, routine, "Region is \"" + region + "\" - populating the list of distributions." );
			}
			List<DistributionSummary> distributions = AwsToolkit.getInstance().getCloudFrontDistributions(awsSession, region);
			List<String> distributionIdChoices = new ArrayList<>();
			for ( DistributionSummary distribution : distributions ) {
				distributionIdChoices.add ( distribution.id() );
				if ( debug ) {
					Message.printStatus(2, routine, "Populated distributions: " + distribution.comment() );
				}
			}
			Collections.sort(distributionIdChoices);
			// Add a blank because may specify a different way.
			distributionIdChoices.add(0,"");
			__DistributionId_JComboBox.setData(distributionIdChoices);
			if ( __DistributionId_JComboBox.getItemCount() > 0 ) {
				// Select the first bucket by default.
				__DistributionId_JComboBox.select ( 0 );
			}
		}
	}
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String CloudFrontCommand = "";
	String Profile = "";
	String Region = "";
	String DistributionId = "";
	String Tags = "";
	String Comment = "";
	String InvalidationPaths = "";
	String CallerReference = "";
	String WaitForCompletion = "";
	String ListDistributionsCountProperty = "";
	String InvalidationStatus = "";
	String ListInvalidationsCountProperty = "";
	String OutputTableID = "";
	String OutputFile = "";
	//String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		CloudFrontCommand = parameters.getValue ( "CloudFrontCommand" );
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		DistributionId = parameters.getValue ( "DistributionId" );
		Tags = parameters.getValue ( "Tags" );
		Comment = parameters.getValue ( "Comment" );
		InvalidationPaths = parameters.getValue ( "InvalidationPaths" );
		CallerReference = parameters.getValue ( "CallerReference" );
		WaitForCompletion = parameters.getValue ( "WaitForCompletion" );
		ListDistributionsCountProperty = parameters.getValue ( "ListDistributionsCountProperty" );
		InvalidationStatus = parameters.getValue ( "InvalidationStatus" );
		ListInvalidationsCountProperty = parameters.getValue ( "ListInvalidationsCountProperty" );
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( JGUIUtil.isSimpleJComboBoxItem(__CloudFrontCommand_JComboBox, CloudFrontCommand,JGUIUtil.NONE, null, null ) ) {
			__CloudFrontCommand_JComboBox.select ( CloudFrontCommand );
		}
		else {
            if ( (CloudFrontCommand == null) ||	CloudFrontCommand.equals("") ) {
				// New command...select the default.
				__CloudFrontCommand_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CloudFrontCommand parameter \"" + CloudFrontCommand + "\".  Select a value or Cancel." );
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
        populateDistributionIdChoices();
		if ( JGUIUtil.isSimpleJComboBoxItem(__DistributionId_JComboBox, DistributionId,JGUIUtil.NONE, null, null ) ) {
			__DistributionId_JComboBox.select ( DistributionId );
		}
		else {
            if ( (DistributionId == null) || DistributionId.equals("") ) {
				// New command...select the default.
            	if ( __DistributionId_JComboBox.getItemCount() > 0 ) {
            		__DistributionId_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"DistributionId parameter \"" + DistributionId + "\".  Select a value or Cancel." );
			}
		}
        if ( Tags != null ) {
            __Tags_JTextArea.setText ( Tags );
        }
        if ( Comment != null ) {
            __Comment_JTextField.setText ( Comment );
        }
        if ( InvalidationPaths != null ) {
            __InvalidationPaths_JTextArea.setText ( InvalidationPaths );
        }
        if ( CallerReference != null ) {
            __CallerReference_JTextField.setText ( CallerReference );
        }
        if ( WaitForCompletion == null ) {
            // Select default.
            if ( __WaitForCompletion_JComboBox.getItemCount() > 0 ) {
                __WaitForCompletion_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __WaitForCompletion_JComboBox,WaitForCompletion, JGUIUtil.NONE, null, null ) ) {
                __WaitForCompletion_JComboBox.select ( WaitForCompletion );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid WaitForCompletion value \"" + WaitForCompletion +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ListDistributionsCountProperty != null ) {
            __ListDistributionsCountProperty_JTextField.setText ( ListDistributionsCountProperty );
        }
        if ( InvalidationStatus == null ) {
            // Select default.
            if ( __InvalidationStatus_JComboBox.getItemCount() > 0 ) {
                __InvalidationStatus_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __InvalidationStatus_JComboBox,InvalidationStatus, JGUIUtil.NONE, null, null ) ) {
                __InvalidationStatus_JComboBox.select ( InvalidationStatus );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid InvalidationStatus value \"" + InvalidationStatus +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ListInvalidationsCountProperty != null ) {
            __ListInvalidationsCountProperty_JTextField.setText ( ListInvalidationsCountProperty );
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
		AwsCloudFrontCommandType command = AwsCloudFrontCommandType.valueOfIgnoreCase(CloudFrontCommand);
		if ( command == AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION ) {
			__main_JTabbedPane.setSelectedIndex(0);
		}
		else if ( (command == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS) ||
			(command == AwsCloudFrontCommandType.LIST_INVALIDATIONS) ) {
			__main_JTabbedPane.setSelectedIndex(0);
		}
		// Set the tab for the above selections.
		setTabForS3Command();
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	CloudFrontCommand = __CloudFrontCommand_JComboBox.getSelected();
	Profile = __Profile_JComboBox.getSelected();
	if ( Profile == null ) {
		Profile = "";
	}
	Region = getSelectedRegion();
	DistributionId = __DistributionId_JComboBox.getSelected();
	if ( DistributionId == null ) {
		DistributionId = "";
	}
	Tags = __Tags_JTextArea.getText().trim().replace("\n"," ");
	Comment = __Comment_JTextField.getText().trim();
	InvalidationPaths = __InvalidationPaths_JTextArea.getText().trim().replace("\n","");
	CallerReference = __CallerReference_JTextField.getText().trim();
	WaitForCompletion = __WaitForCompletion_JComboBox.getSelected();
	ListDistributionsCountProperty = __ListDistributionsCountProperty_JTextField.getText().trim();
	InvalidationStatus = __InvalidationStatus_JComboBox.getSelected();
	ListInvalidationsCountProperty = __ListInvalidationsCountProperty_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "CloudFrontCommand=" + CloudFrontCommand );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "DistributionId=" + DistributionId );
	props.add ( "Tags=" + Tags );
	props.add ( "Comment=" + Comment );
	props.add ( "InvalidationPaths=" + InvalidationPaths );
	props.add ( "CallerReference=" + CallerReference );
	props.add ( "WaitForCompletion=" + WaitForCompletion );
	props.add ( "ListDistributionsCountProperty=" + ListDistributionsCountProperty );
	props.add ( "InvalidationStatus=" + InvalidationStatus );
	props.add ( "ListInvalidationsCountProperty=" + ListInvalidationsCountProperty );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "OutputTableID=" + OutputTableID );
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
	String command = __CloudFrontCommand_JComboBox.getSelected();
	if ( command.equalsIgnoreCase("" + AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
	else if ( command.equalsIgnoreCase("" + AwsCloudFrontCommandType.LIST_DISTRIBUTIONS) ) {
		__main_JTabbedPane.setSelectedIndex(1);
	}
	else if ( command.equalsIgnoreCase("" + AwsCloudFrontCommandType.LIST_INVALIDATIONS) ) {
		__main_JTabbedPane.setSelectedIndex(2);
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