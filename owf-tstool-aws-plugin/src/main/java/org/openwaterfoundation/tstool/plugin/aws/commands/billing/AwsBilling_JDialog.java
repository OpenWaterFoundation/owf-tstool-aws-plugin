// AwsBilling_JDialog - editor for AwsBilling command

/* NoticeStart

OWF TSTool AWS Plugin
Copyright (C) 2022-2024 Open Water Foundation

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

package org.openwaterfoundation.tstool.plugin.aws.commands.billing;

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

@SuppressWarnings("serial")
public class AwsBilling_JDialog extends JDialog
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
private JTextField __ProfileDefault_JTextField = null; // View only (not a command parameter).
private JLabel __ProfileDefaultNote_JLabel = null; // To explain the default.
private SimpleJComboBox __Region_JComboBox = null;
private JTextField __RegionDefault_JTextField = null; // View only (not a command parameter).
private JLabel __RegionDefaultNote_JLabel = null; // To explain the default.
// TODO smalers 2023-01-11 costs are not by bucket although may use in the future somehow?
//private SimpleJComboBox __Bucket_JComboBox = null;
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Cost Explorer tab.
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private SimpleJComboBox __Granularity_JComboBox = null;
private SimpleJComboBox __GroupBy1_JComboBox = null;
private JTextField __GroupByTag1_JTextField = null;
private SimpleJComboBox __GroupBy2_JComboBox = null;
private JTextField __GroupByTag2_JTextField = null;
private SimpleJComboBox __GroupBy3_JComboBox = null;
private JTextField __GroupByTag3_JTextField = null;
private SimpleJComboBox __Metric_JComboBox = null;

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __AppendOutput_JComboBox = null;

// Time Series tab.

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private AwsBilling_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private boolean ignoreEvents = false; // Ignore events when initializing, to avoid infinite loop.

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
public AwsBilling_JDialog ( JFrame parent, AwsBilling_Command command, List<String> tableIDChoices ) {
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
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
    /*
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
    */
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AwsBilling", PluginMeta.getDocumentationRootUrl());
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
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	//String Bucket = __Bucket_JComboBox.getSelected();
	// Cost Explorer.
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Granularity = __Granularity_JComboBox.getSelected();
	String GroupBy1 = __GroupBy1_JComboBox.getSelected();
	String GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	String GroupBy2 = __GroupBy2_JComboBox.getSelected();
	String GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	String GroupBy3 = __GroupBy3_JComboBox.getSelected();
	String GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	String Metric = __Metric_JComboBox.getSelected();
	// Output.
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( (Profile != null) && !Profile.isEmpty() ) {
		props.set ( "Profile", Profile );
	}
	if ( (Region != null) && !Region.isEmpty() ) {
		props.set ( "Region", Region );
	}
	//if ( (Bucket != null) && !Bucket.isEmpty() ) {
		//props.set ( "Bucket", Bucket );
	//}
	// Cost Explorer.
	if ( (InputStart != null) && !InputStart.isEmpty()) {
		props.set ( "InputStart", InputStart );
	}
	if ( (InputEnd != null) && !InputEnd.isEmpty() ) {
		props.set ( "InputEnd", InputEnd );
	}
	if ( (Granularity != null) && !Granularity.isEmpty() ) {
		props.set ( "Granularity", Granularity );
	}
	if ( (GroupBy1 != null) && !GroupBy1.isEmpty() ) {
		props.set ( "GroupBy1", GroupBy1 );
	}
	if ( (GroupByTag1 != null) && !GroupByTag1.isEmpty() ) {
		props.set ( "GroupByTag1", GroupByTag1 );
	}
	if ( (GroupBy2 != null) && !GroupBy2.isEmpty() ) {
		props.set ( "GroupBy2", GroupBy2 );
	}
	if ( (GroupByTag2 != null) && !GroupByTag2.isEmpty() ) {
		props.set ( "GroupByTag2", GroupByTag2 );
	}
	if ( (GroupBy3 != null) && !GroupBy3.isEmpty() ) {
		props.set ( "GroupBy3", GroupBy3 );
	}
	if ( (GroupByTag3 != null) && !GroupByTag3.isEmpty() ) {
		props.set ( "GroupByTag3", GroupByTag3 );
	}
	if ( (Metric != null) && !Metric.isEmpty() ) {
		props.set ( "Metric", Metric );
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
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	//String Bucket = __Bucket_JComboBox.getSelected();
	// Cost Explorer.
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Granularity = __Granularity_JComboBox.getSelected();
	String GroupBy1 = __GroupBy1_JComboBox.getSelected();
	String GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	String GroupBy2 = __GroupBy2_JComboBox.getSelected();
	String GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	String GroupBy3 = __GroupBy3_JComboBox.getSelected();
	String GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	String Metric = __Metric_JComboBox.getSelected();
	// Output
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();

    // General.
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	//__command.setCommandParameter ( "Bucket", Bucket );
	// Cost Explorer.
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "Granularity", Granularity );
	__command.setCommandParameter ( "GroupBy1", GroupBy1 );
	__command.setCommandParameter ( "GroupByTag1", GroupByTag1 );
	__command.setCommandParameter ( "GroupBy2", GroupBy2 );
	__command.setCommandParameter ( "GroupByTag2", GroupByTag2 );
	__command.setCommandParameter ( "GroupBy3", GroupBy3 );
	__command.setCommandParameter ( "GroupByTag3", GroupByTag3 );
	__command.setCommandParameter ( "Metric", Metric );
	// Output.
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "AppendOutput", AppendOutput );
	//__command.setCommandParameter ( "IfInputNotFound", IfInputNotFound );
}

/**
Return the selected profile.
@param returnDefault if true, return the default profile if the UI has blank
@return the selected region.
*/
private String getSelectedProfile ( boolean returnDefault ) {
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
private void initialize ( JFrame parent, AwsBilling_Command command, List<String> tableIDChoices ) {
	this.__command = command;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

    // Get the toolkit for useful functions.
    AwsToolkit awsToolkit = AwsToolkit.getInstance();

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Read AWS Billing and Cost Management data."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"The data can be filtered and output to a table."),
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
    String config = awsToolkit.getAwsUserConfigFile();
    File f = null;
    if ( config != null ) {
    	f = new File(config);
    }
    if ( (f == null) || !f.exists() ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"<html><b>ERROR: User's AWS configuration file does not exist (errors will occur): "
        	+ awsToolkit.getAwsUserConfigFile() + "</b></html>" ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"User's AWS configuration file: " + awsToolkit.getAwsUserConfigFile() ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Profile:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Profile_JComboBox = new SimpleJComboBox ( false );
	__Profile_JComboBox.setToolTipText("AWS user profile to use for authentication (see user's .aws/config file).");
	List<String> profileChoices = awsToolkit.getProfiles();
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
    __ProfileDefaultNote_JLabel = new JLabel("From: " + awsToolkit.getAwsUserConfigFile());
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
	regionChoices.add(0,""); // Default - region is not specified so will use 'aws-global'.
	__Region_JComboBox.setData(regionChoices);
	__Region_JComboBox.select ( 0 );
	__Region_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Region_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - AWS region (default=" + awsToolkit.getDefaultRegionForCostExplorer() + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Region (profile default):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RegionDefault_JTextField = new JTextField ( 20 );
	__RegionDefault_JTextField.setToolTipText("Default region for profile determined from user's .aws/config file).");
	__RegionDefault_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RegionDefault_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __RegionDefaultNote_JLabel = new JLabel("From: " + awsToolkit.getAwsUserConfigFile() );
    JGUIUtil.addComponent(main_JPanel, __RegionDefaultNote_JLabel,
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__RegionDefault_JTextField.setEditable(false);

	/*
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
		*/

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for general 'Cost Explorer' parameters.
    int yCostExplorer = -1;
    JPanel costExplorer_JPanel = new JPanel();
    costExplorer_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Cost Explorer", costExplorer_JPanel );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Read AWS Cost Explorer data.  See AWS console Billing and Cost Management / Cost Explorer."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Some choices may be limited based on AWS Billing configuration "
   		+ "(e.g., daily data are only available for 14 previous days)."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Multiple 'Group by' can be specified, for example to group by service and a tag."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("See the 'Output' tab to specify the output table and/or file for the output."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Input start:"),
        0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.setToolTipText("Input start for billing data, with daily precision, can use ${Property} syntax.");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __InputStart_JTextField,
        1, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Input end:"),
        0, ++yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.setToolTipText("Input end for billing data, with daily precision, can use ${Property} syntax.");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __InputEnd_JTextField,
        1, yCostExplorer, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Granularity:"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Granularity_JComboBox = new SimpleJComboBox ( false );
	__Granularity_JComboBox.setToolTipText("AWS Cost Explorer granularity (interval for costs).");
	List<String> granularityChoices = AwsBillingGranularityType.getChoicesAsStrings();
	granularityChoices.add(0,"");
	__Granularity_JComboBox.setData(granularityChoices);
	__Granularity_JComboBox.select ( 0 );
	__Granularity_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __Granularity_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - time granularity of output (default=" + AwsBillingGranularityType.MONTHLY + ")."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by (1):"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__GroupBy1_JComboBox = new SimpleJComboBox ( false );
	__GroupBy1_JComboBox.setToolTipText("How to group (aggregate) the output by a Dimension.");
	List<String> groupByChoices = AwsBillingDimensionType.getChoicesAsStrings();
	groupByChoices.add(0,"");
	__GroupBy1_JComboBox.setData(groupByChoices);
	__GroupBy1_JComboBox.select ( 0 );
	__GroupBy1_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupBy1_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - dimension by which to group output (default=" + AwsBillingDimensionType.SERVICE + ")."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by tag (1):"),
        0, ++yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupByTag1_JTextField = new JTextField (20);
    __GroupByTag1_JTextField.setToolTipText("If GroupBy1=Tag, specify the tag name to group by, can use ${Property} syntax.");
    __GroupByTag1_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupByTag1_JTextField,
        1, yCostExplorer, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - used with GroupBy1=Tag."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by (2):"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__GroupBy2_JComboBox = new SimpleJComboBox ( false );
	__GroupBy2_JComboBox.setToolTipText("How to group (aggregate) the output by a Dimension.");
	__GroupBy2_JComboBox.setData(groupByChoices);
	__GroupBy2_JComboBox.select ( 0 );
	__GroupBy2_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupBy2_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - dimension by which to group output (default=no group by 2)."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by tag (2):"),
        0, ++yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupByTag2_JTextField = new JTextField (20);
    __GroupByTag2_JTextField.setToolTipText("If GroupBy2=Tag, specify the tag name to group by, can use ${Property} syntax.");
    __GroupByTag2_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupByTag2_JTextField,
        1, yCostExplorer, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - used with GroupBy2=Tag."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by (3):"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__GroupBy3_JComboBox = new SimpleJComboBox ( false );
	__GroupBy3_JComboBox.setToolTipText("How to group (aggregate) the output by a Dimension.");
	__GroupBy3_JComboBox.setData(groupByChoices);
	__GroupBy3_JComboBox.select ( 0 );
	__GroupBy3_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupBy3_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - dimension by which to group output (default=no group by 3)."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by tag (3):"),
        0, ++yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupByTag3_JTextField = new JTextField (20);
    __GroupByTag3_JTextField.setToolTipText("If GroupBy3=Tag, specify the tag name to group by, can use ${Property} syntax.");
    __GroupByTag3_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupByTag3_JTextField,
        1, yCostExplorer, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - used with GroupBy3=Tag."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Aggregate costs by (metric):"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Metric_JComboBox = new SimpleJComboBox ( false );
	__Metric_JComboBox.setToolTipText("How to aggregate the output (metric).");
	List<String> aggregateCostsByChoices = AwsBillingMetricType.getChoicesAsStrings();
	aggregateCostsByChoices.add(0,"");
	__Metric_JComboBox.setData(aggregateCostsByChoices);
	__Metric_JComboBox.select ( 0 );
	__Metric_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __Metric_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - output metric (default=" + AwsBillingMetricType.UNBLENDED_COSTS + ")."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
    	"The following parameters are used to specify the output for Cost Explorer results."),
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
    __OutputTableID_JComboBox.setToolTipText("Table for output, can use ${Property} notation.");
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
    __OutputFile_JTextField.setToolTipText("Output file, can use ${Property} notation.");
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

    // Panel for 'Time Series' parameters.
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series", ts_JPanel );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Indicate whether time series output should be created."),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("The time series interval will match the granularity."),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Time series identifiers use the Cost Explorer Dimension and metric."),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
    	this.awsSession.setProfile(this.__Profile_JComboBox.getSelected());
        //AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
	}
	else if ( o == this.__Region_JComboBox ) {
        //AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
	}
	refresh();
}

/**
Respond to key press events.
@param event KeyEvent to handle
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

/**
Respond to key release events.
@param event KeyEvent to handle
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
Respond to key release events.
@param event KeyEvent to handle
*/
public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	// General.
	String Profile = "";
	String Region = "";
	String Bucket = "";
	// Cost Explorer.
	String InputStart = "";
	String InputEnd = "";
	String Granularity = "";
	String GroupBy1 = "";
	String GroupByTag1 = "";
	String GroupBy2 = "";
	String GroupByTag2 = "";
	String GroupBy3 = "";
	String GroupByTag3 = "";
	String Metric = "";
	// Output.
	String OutputTableID = "";
	String OutputFile = "";
	String AppendOutput = "";
	//String IfInputNotFound = "";
    PropList parameters = null;

	AwsToolkit awsToolkit = AwsToolkit.getInstance();

	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        // General.
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		Bucket = parameters.getValue ( "Bucket" );
		// Cost Explorer.
		InputStart = parameters.getValue ( "InputStart" );
		InputEnd = parameters.getValue ( "InputEnd" );
		Granularity = parameters.getValue ( "Granularity" );
		GroupBy1 = parameters.getValue ( "GroupBy1" );
		GroupByTag1 = parameters.getValue ( "GroupByTag1" );
		GroupBy2 = parameters.getValue ( "GroupBy2" );
		GroupByTag2 = parameters.getValue ( "GroupByTag2" );
		GroupBy3 = parameters.getValue ( "GroupBy3" );
		GroupByTag3 = parameters.getValue ( "GroupByTag3" );
		Metric = parameters.getValue ( "Metric" );
		// Output
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		AppendOutput = parameters.getValue ( "AppendOutput" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
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
        /*
        awsToolkit.uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __Bucket_JComboBox, true );
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
		*/
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__Granularity_JComboBox, Granularity,JGUIUtil.NONE, null, null ) ) {
			__Granularity_JComboBox.select ( Granularity );
		}
		else {
            if ( (Granularity == null) || Granularity.equals("") ) {
				// New command...select the default.
            	if ( __Granularity_JComboBox.getItemCount() > 0 ) {
            		__Granularity_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Granularity parameter \"" + Granularity + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__GroupBy1_JComboBox, GroupBy1,JGUIUtil.NONE, null, null ) ) {
			__GroupBy1_JComboBox.select ( GroupBy1 );
		}
		else {
            if ( (GroupBy1 == null) || GroupBy1.equals("") ) {
				// New command...select the default.
            	if ( __GroupBy1_JComboBox.getItemCount() > 0 ) {
            		__GroupBy1_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"GroupBy1 parameter \"" + GroupBy1 + "\".  Select a value or Cancel." );
			}
		}
		if ( GroupByTag1 != null ) {
			__GroupByTag1_JTextField.setText ( GroupByTag1 );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__GroupBy2_JComboBox, GroupBy2,JGUIUtil.NONE, null, null ) ) {
			__GroupBy2_JComboBox.select ( GroupBy2 );
		}
		else {
            if ( (GroupBy2 == null) || GroupBy2.equals("") ) {
				// New command...select the default.
            	if ( __GroupBy2_JComboBox.getItemCount() > 0 ) {
            		__GroupBy2_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 2, routine,
				"Existing command references an invalid\n"+
				"GroupBy2 parameter \"" + GroupBy2 + "\".  Select a value or Cancel." );
			}
		}
		if ( GroupByTag2 != null ) {
			__GroupByTag2_JTextField.setText ( GroupByTag2 );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__GroupBy3_JComboBox, GroupBy3,JGUIUtil.NONE, null, null ) ) {
			__GroupBy3_JComboBox.select ( GroupBy3 );
		}
		else {
            if ( (GroupBy3 == null) || GroupBy3.equals("") ) {
				// New command...select the default.
            	if ( __GroupBy3_JComboBox.getItemCount() > 0 ) {
            		__GroupBy3_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 3, routine,
				"Existing command references an invalid\n"+
				"GroupBy3 parameter \"" + GroupBy3 + "\".  Select a value or Cancel." );
			}
		}
		if ( GroupByTag3 != null ) {
			__GroupByTag3_JTextField.setText ( GroupByTag3 );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__Metric_JComboBox, Metric,JGUIUtil.NONE, null, null ) ) {
			__Metric_JComboBox.select ( Metric );
		}
		else {
            if ( (Metric == null) || Metric.equals("") ) {
				// New command...select the default.
            	if ( __Metric_JComboBox.getItemCount() > 0 ) {
            		__Metric_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Metric parameter \"" + Metric + "\".  Select a value or Cancel." );
			}
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
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	// General.
	Profile = __Profile_JComboBox.getSelected();
	if ( Profile == null ) {
		Profile = "";
	}
	Region = getSelectedRegion();
	//Bucket = __Bucket_JComboBox.getSelected();
	if ( Bucket == null ) {
		Bucket = "";
	}
	// Cost Explorer.
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	Granularity = __Granularity_JComboBox.getSelected();
	GroupBy1 = __GroupBy1_JComboBox.getSelected();
	GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	GroupBy2 = __GroupBy2_JComboBox.getSelected();
	GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	GroupBy3 = __GroupBy3_JComboBox.getSelected();
	GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	Metric = __Metric_JComboBox.getSelected();
	// Output
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	AppendOutput = __AppendOutput_JComboBox.getSelected();
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
    // General.
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "Bucket=" + Bucket );
	// Cost Explorer.
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Granularity=" + Granularity );
	props.add ( "GroupBy1=" + GroupBy1 );
	props.add ( "GroupByTag1=" + GroupByTag1 );
	props.add ( "GroupBy2=" + GroupBy2 );
	props.add ( "GroupByTag2=" + GroupByTag2 );
	props.add ( "GroupBy3=" + GroupBy3 );
	props.add ( "GroupByTag3=" + GroupByTag3 );
	props.add ( "Metric=" + Metric );
	// Output.
	props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "AppendOutput=" + AppendOutput );
	//props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Set the default values as FYI.
	awsToolkit.uiPopulateProfileDefault(__ProfileDefault_JTextField, __ProfileDefaultNote_JLabel);
	awsToolkit.uiPopulateRegionDefault( __Profile_JComboBox.getSelected(), __RegionDefault_JTextField, __RegionDefaultNote_JLabel);
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