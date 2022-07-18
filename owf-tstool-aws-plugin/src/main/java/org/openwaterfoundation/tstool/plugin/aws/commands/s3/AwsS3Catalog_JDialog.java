// AwsS3Catalog_JDialog - editor for AwsS3Catalog command

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

import org.openwaterfoundation.tstool.plugin.aws.Aws;
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
import software.amazon.awssdk.services.s3.model.Bucket;

@SuppressWarnings("serial")
public class AwsS3Catalog_JDialog extends JDialog
implements ActionListener, ChangeListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseCatalogIndexFile_JButton = null;
private SimpleJButton __pathCatalogIndexFile_JButton = null;
private SimpleJButton __browseCatalogFile_JButton = null;
private SimpleJButton __pathCatalogFile_JButton = null;
private SimpleJButton __browseDatasetIndexFile_JButton = null;
private SimpleJButton __browseDatasetIndexHeadFile_JButton = null;
private SimpleJButton __browseDatasetIndexBodyFile_JButton = null;
private SimpleJButton __browseDatasetIndexFooterFile_JButton = null;
private SimpleJButton __pathDatasetIndexFile_JButton = null;
private SimpleJButton __pathDatasetIndexHeadFile_JButton = null;
private SimpleJButton __pathDatasetIndexBodyFile_JButton = null;
private SimpleJButton __pathDatasetIndexFooterFile_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __Profile_JComboBox = null;
private SimpleJComboBox __Region_JComboBox = null;
private SimpleJComboBox __Bucket_JComboBox = null;
private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Catalog tab.
private JTextField __StartingPrefix_JTextField = null;
private JTextField __CatalogIndexFile_JTextField = null;
private JTextField __CatalogFile_JTextField = null;
private SimpleJComboBox __UploadCatalogFiles_JComboBox = null;

// CloudFront tab.
private JTextField __DistributionId_JTextField = null;

// Dataset tab.
private JTextField __DatasetIndexFile_JTextField = null;
private JTextField __DatasetIndexHeadFile_JTextField = null;
private JTextField __DatasetIndexBodyFile_JTextField = null;
private JTextField __DatasetIndexFooterFile_JTextField = null;
private SimpleJComboBox __UploadDatasetFiles_JComboBox = null;

// List tab.
//private JTextField __MaxKeys_JTextField = null;
//private JTextField __MaxObjects_JTextField = null;

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private SimpleJComboBox __KeepFiles_JComboBox = null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private AwsS3Catalog_Command __command = null;
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
public AwsS3Catalog_JDialog ( JFrame parent, AwsS3Catalog_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	String routine = getClass().getSimpleName() + ".actionPeformed";
	if ( this.ignoreEvents ) {
        return; // Startup.
    }

	Object o = event.getSource();

	if ( o == __browseCatalogFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Catalog File");
        
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
					__CatalogFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseCatalogIndexFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Catalog Index File");
        
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
					__CatalogIndexFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseDatasetIndexFile_JButton ) {
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
	else if ( o == __browseDatasetIndexBodyFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index Body File");
        
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
					__DatasetIndexBodyFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __browseDatasetIndexFooterFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Dataset Index Footer File");
        
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
					__DatasetIndexFooterFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AwsS3", Aws.documentationRootUrl());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __pathCatalogFile_JButton ) {
        if ( __pathCatalogFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __CatalogFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__CatalogFile_JTextField.getText() ) );
        }
        else if ( __pathCatalogFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __CatalogFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __CatalogFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathCatalogIndexFile_JButton ) {
        if ( __pathCatalogIndexFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __CatalogIndexFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__CatalogIndexFile_JTextField.getText() ) );
        }
        else if ( __pathCatalogIndexFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __CatalogIndexFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __CatalogIndexFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting index file name to relative path." );
            }
        }
        refresh ();
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
    else if ( o == __pathDatasetIndexHeadFile_JButton ) {
        if ( __pathDatasetIndexHeadFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __DatasetIndexHeadFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__DatasetIndexHeadFile_JTextField.getText() ) );
        }
        else if ( __pathDatasetIndexHeadFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __DatasetIndexHeadFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __DatasetIndexHeadFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting index <head> file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathDatasetIndexBodyFile_JButton ) {
        if ( __pathDatasetIndexBodyFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __DatasetIndexBodyFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__DatasetIndexBodyFile_JTextField.getText() ) );
        }
        else if ( __pathDatasetIndexBodyFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __DatasetIndexBodyFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __DatasetIndexBodyFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting index <body> file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathDatasetIndexFooterFile_JButton ) {
        if ( __pathDatasetIndexFooterFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __DatasetIndexFooterFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__DatasetIndexFooterFile_JTextField.getText() ) );
        }
        else if ( __pathDatasetIndexFooterFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __DatasetIndexFooterFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __DatasetIndexFooterFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Error converting index <footer> file name to relative path." );
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String StartingPrefix = __StartingPrefix_JTextField.getText().trim();
	String CatalogFile = __CatalogFile_JTextField.getText().trim();
	String CatalogIndexFile = __CatalogIndexFile_JTextField.getText().trim();
	String UploadCatalogFiles = __UploadCatalogFiles_JComboBox.getSelected();
	String DistributionId = __DistributionId_JTextField.getText().trim();
	String DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	String DatasetIndexHeadFile = __DatasetIndexHeadFile_JTextField.getText().trim();
	String DatasetIndexBodyFile = __DatasetIndexBodyFile_JTextField.getText().trim();
	String DatasetIndexFooterFile = __DatasetIndexFooterFile_JTextField.getText().trim();
	String UploadDatasetFiles = __UploadDatasetFiles_JComboBox.getSelected();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String KeepFiles = __KeepFiles_JComboBox.getSelected();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
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
	if ( (StartingPrefix != null) && !StartingPrefix.isEmpty() ) {
		props.set ( "StartingPrefix", StartingPrefix );
	}
    if ( CatalogFile.length() > 0 ) {
        props.set ( "CatalogFile", CatalogFile );
    }
    if ( CatalogIndexFile.length() > 0 ) {
        props.set ( "CatalogIndexFile", CatalogIndexFile );
    }
	if ( UploadCatalogFiles.length() > 0 ) {
		props.set ( "UploadCatalogFiles", UploadCatalogFiles );
	}
    if ( DistributionId.length() > 0 ) {
        props.set ( "DistributionId", DistributionId );
    }
    if ( DatasetIndexFile.length() > 0 ) {
        props.set ( "DatasetIndexFile", DatasetIndexFile );
    }
    if ( DatasetIndexHeadFile.length() > 0 ) {
        props.set ( "DatasetIndexHeadFile", DatasetIndexHeadFile );
    }
    if ( DatasetIndexBodyFile.length() > 0 ) {
        props.set ( "DatasetIndexBodyFile", DatasetIndexBodyFile );
    }
    if ( DatasetIndexFooterFile.length() > 0 ) {
        props.set ( "DatasetIndexFooterFile", DatasetIndexFooterFile );
    }
    if ( OutputTableID.length() > 0 ) {
        props.set ( "OutputTableID", OutputTableID );
    }
	if ( KeepFiles.length() > 0 ) {
		props.set ( "KeepFiles", KeepFiles );
	}
	if ( UploadDatasetFiles.length() > 0 ) {
		props.set ( "UploadDatasetFiles", UploadDatasetFiles );
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
	String Profile = __Profile_JComboBox.getSelected();
	String Region = getSelectedRegion();
	String Bucket = __Bucket_JComboBox.getSelected();
	String StartingPrefix = __StartingPrefix_JTextField.getText().trim();
	String CatalogFile = __CatalogFile_JTextField.getText().trim();
	String CatalogIndexFile = __CatalogIndexFile_JTextField.getText().trim();
	String UploadCatalogFiles = __UploadCatalogFiles_JComboBox.getSelected();
	String DistributionId = __DistributionId_JTextField.getText().trim();
	String DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	String DatasetIndexHeadFile = __DatasetIndexHeadFile_JTextField.getText().trim();
	String DatasetIndexBodyFile = __DatasetIndexBodyFile_JTextField.getText().trim();
	String DatasetIndexFooterFile = __DatasetIndexFooterFile_JTextField.getText().trim();
	String UploadDatasetFiles = __UploadDatasetFiles_JComboBox.getSelected();
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String KeepFiles = __KeepFiles_JComboBox.getSelected();
	String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	__command.setCommandParameter ( "Bucket", Bucket );
	__command.setCommandParameter ( "StartingPrefix", StartingPrefix );
	__command.setCommandParameter ( "CatalogFile", CatalogFile );
	__command.setCommandParameter ( "CatalogIndexFile", CatalogIndexFile );
	__command.setCommandParameter ( "UploadCatalogFiles", UploadCatalogFiles );
	__command.setCommandParameter ( "DistributionId", DistributionId );
	__command.setCommandParameter ( "DatasetIndexFile", DatasetIndexFile );
	__command.setCommandParameter ( "DatasetIndexHeadFile", DatasetIndexHeadFile );
	__command.setCommandParameter ( "DatasetIndexBodyFile", DatasetIndexBodyFile );
	__command.setCommandParameter ( "DatasetIndexFooterFile", DatasetIndexFooterFile );
	__command.setCommandParameter ( "UploadDatasetFiles", UploadDatasetFiles );
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "KeepFiles", KeepFiles );
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
private void initialize ( JFrame parent, AwsS3Catalog_Command command, List<String> tableIDChoices )
{	this.__command = command;
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Create a dataset catalog and landing page by searching an S3 directory." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("This can be used, for example, to create a catalog of datasets." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Datasets must each have a DCAT dataset.json file to be included in the catalog." ),
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

    // Panel for 'Catalog' parameters:
    // - this includes filtering by starting prefix
    int yCatalog = -1;
    JPanel catalog_JPanel = new JPanel();
    catalog_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Catalog", catalog_JPanel );

    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("Use the following parameters to control creation of the catalog."),
		0, ++yCatalog, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("The catalog file contains a merged list of individual dataset metadata files."),
		0, ++yCatalog, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("The catalog index file is a landing page for all the datasets."),
		0, ++yCatalog, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("Use the parameters in the 'Output' tab to specify the output file and table."),
		0, ++yCatalog, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCatalog, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(catalog_JPanel, new JLabel ( "Starting prefix:"),
        0, ++yCatalog, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StartingPrefix_JTextField = new JTextField ( "", 30 );
    __StartingPrefix_JTextField.setToolTipText("Starting prefix (directory) to search for datasets, no / at start, ending with /.");
    __StartingPrefix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(catalog_JPanel, __StartingPrefix_JTextField,
        1, yCatalog, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JLabel ( "Optional - starting object key prefix (default=/)."),
        3, yCatalog, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("Catalog file:" ),
        0, ++yCatalog, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CatalogFile_JTextField = new JTextField ( 50 );
    __CatalogFile_JTextField.setToolTipText("Specify the catalog file to create, can use ${Property} notation.");
    __CatalogFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel CatalogFile_JPanel = new JPanel();
	CatalogFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(CatalogFile_JPanel, __CatalogFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseCatalogFile_JButton = new SimpleJButton ( "...", this );
	__browseCatalogFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(CatalogFile_JPanel, __browseCatalogFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathCatalogFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(CatalogFile_JPanel, __pathCatalogFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(catalog_JPanel, CatalogFile_JPanel,
		1, yCatalog, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(catalog_JPanel, new JLabel ("Catalog index file:" ),
        0, ++yCatalog, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CatalogIndexFile_JTextField = new JTextField ( 50 );
    __CatalogIndexFile_JTextField.setToolTipText("Specify the catalog index file to create, can use ${Property} notation.");
    __CatalogIndexFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel CatalogIndexFile_JPanel = new JPanel();
	CatalogIndexFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(CatalogIndexFile_JPanel, __CatalogIndexFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseCatalogIndexFile_JButton = new SimpleJButton ( "...", this );
	__browseCatalogIndexFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(CatalogIndexFile_JPanel, __browseCatalogIndexFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathCatalogIndexFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(CatalogIndexFile_JPanel, __pathCatalogIndexFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(catalog_JPanel, CatalogIndexFile_JPanel,
		1, yCatalog, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(catalog_JPanel, new JLabel ( "Upload catalog files?:"),
		0, ++yCatalog, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__UploadCatalogFiles_JComboBox = new SimpleJComboBox ( false );
	__UploadCatalogFiles_JComboBox.setToolTipText("Indicate whether to upload catalog index and datasets.json files?");
	List<String> uploadCatalogChoices = new ArrayList<>();
	uploadCatalogChoices.add ( "" );	// Default.
	uploadCatalogChoices.add ( __command._False );
	uploadCatalogChoices.add ( __command._True );
	__UploadCatalogFiles_JComboBox.setData(uploadCatalogChoices);
	__UploadCatalogFiles_JComboBox.select ( 0 );
	__UploadCatalogFiles_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(catalog_JPanel, __UploadCatalogFiles_JComboBox,
		1, yCatalog, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(catalog_JPanel, new JLabel(
		"Optional - upload catalog files? (default=" + __command._False + ")."),
		3, yCatalog, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'CloudFront' parameters:
    // - controls the index file created for the dataset
    int yCloudFront = -1;
    JPanel cloudfront_JPanel = new JPanel();
    cloudfront_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "CloudFront", cloudfront_JPanel );

    JGUIUtil.addComponent(cloudfront_JPanel, new JLabel ("Use the following parameters to control CloudFront invalidations."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cloudfront_JPanel, new JLabel ("Files must be invalidated to be visible on the CloudFront-fronted website."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cloudfront_JPanel, new JLabel ("Specify the distribution ID using a *comment pattern*."),
		0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cloudfront_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCloudFront, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cloudfront_JPanel, new JLabel ( "Distribution ID:"),
        0, ++yCloudFront, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DistributionId_JTextField = new JTextField ( "", 30 );
    __DistributionId_JTextField.setToolTipText("CloudFront distribution ID or comment pattern surrounded by *.");
    __DistributionId_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(cloudfront_JPanel, __DistributionId_JTextField,
        1, yCloudFront, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cloudfront_JPanel, new JLabel ( "Optional - CloudFront distribution ID (default=no invalidation)."),
        3, yCloudFront, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'Dataset' parameters:
    // - controls the index file created for the dataset
    int yDataset = -1;
    JPanel dataset_JPanel = new JPanel();
    dataset_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Dataset", dataset_JPanel );

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Use the following parameters to control creation of the dataset landing page."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("The dataset index will not be created unless a file is specified."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel (
    	"Use the parameters in the 'Output' tab to specify the dataset table and whether to keep temporary files and upload created files."),
		0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDataset, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Dataset index file:" ),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexFile_JTextField = new JTextField ( 50 );
    __DatasetIndexFile_JTextField.setToolTipText("Dataset index file to create, Temp to automatically use temporary file, can use ${Property} notation.");
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

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Dataset index <head> file:" ),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexHeadFile_JTextField = new JTextField ( 50 );
    __DatasetIndexHeadFile_JTextField.setToolTipText("HTML file to insert as the page header at the top of <head>.");
    __DatasetIndexHeadFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexHeadFile_JPanel = new JPanel();
	DatasetIndexHeadFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexHeadFile_JPanel, __DatasetIndexHeadFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexHeadFile_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexHeadFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexHeadFile_JPanel, __browseDatasetIndexHeadFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexHeadFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexHeadFile_JPanel, __pathDatasetIndexHeadFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(dataset_JPanel, DatasetIndexHeadFile_JPanel,
		1, yDataset, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Dataset index <body> file:" ),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexBodyFile_JTextField = new JTextField ( 50 );
    __DatasetIndexBodyFile_JTextField.setToolTipText("HTML file to insert as the page header at the top of <body>.");
    __DatasetIndexBodyFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexBodyFile_JPanel = new JPanel();
	DatasetIndexBodyFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexBodyFile_JPanel, __DatasetIndexBodyFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexBodyFile_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexBodyFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexBodyFile_JPanel, __browseDatasetIndexBodyFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexBodyFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexBodyFile_JPanel, __pathDatasetIndexBodyFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(dataset_JPanel, DatasetIndexBodyFile_JPanel,
		1, yDataset, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dataset_JPanel, new JLabel ("Dataset index <footer> file:" ),
        0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatasetIndexFooterFile_JTextField = new JTextField ( 50 );
    __DatasetIndexFooterFile_JTextField.setToolTipText("HTML file to insert as the page footer after </body>.");
    __DatasetIndexFooterFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel DatasetIndexFooterFile_JPanel = new JPanel();
	DatasetIndexFooterFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DatasetIndexFooterFile_JPanel, __DatasetIndexFooterFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseDatasetIndexFooterFile_JButton = new SimpleJButton ( "...", this );
	__browseDatasetIndexFooterFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DatasetIndexFooterFile_JPanel, __browseDatasetIndexFooterFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathDatasetIndexFooterFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DatasetIndexFooterFile_JPanel, __pathDatasetIndexFooterFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(dataset_JPanel, DatasetIndexFooterFile_JPanel,
		1, yDataset, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(dataset_JPanel, new JLabel ( "Upload dataset files?:"),
		0, ++yDataset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__UploadDatasetFiles_JComboBox = new SimpleJComboBox ( false );
	__UploadDatasetFiles_JComboBox.setToolTipText("Indicate whether to upload dataset index files?");
	List<String> uploadFilesChoices = new ArrayList<>();
	uploadFilesChoices.add ( "" );	// Default.
	uploadFilesChoices.add ( __command._False );
	uploadFilesChoices.add ( __command._True );
	__UploadDatasetFiles_JComboBox.setData(uploadFilesChoices);
	__UploadDatasetFiles_JComboBox.select ( 0 );
	__UploadDatasetFiles_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(dataset_JPanel, __UploadDatasetFiles_JComboBox,
		1, yDataset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dataset_JPanel, new JLabel(
		"Optional - upload dataset files? (default=" + __command._False + ")."),
		3, yDataset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel ("The following are used for commands that generate bucket and bucket object lists."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: 'csv'"),
	//	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

   JGUIUtil.addComponent(output_JPanel, new JLabel ( "Keep files?:"),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__KeepFiles_JComboBox = new SimpleJComboBox ( false );
	__KeepFiles_JComboBox.setToolTipText("Indicate whether to keep temporary files?");
	List<String> keepFilesChoices = new ArrayList<>();
	keepFilesChoices.add ( "" );	// Default.
	keepFilesChoices.add ( __command._False );
	keepFilesChoices.add ( __command._True );
	__KeepFiles_JComboBox.setData(keepFilesChoices);
	__KeepFiles_JComboBox.select ( 0 );
	__KeepFiles_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(output_JPanel, __KeepFiles_JComboBox,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
		"Optional - keep temporary files? (default=" + __command._False + ")."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
			List<Bucket> buckets = AwsToolkit.getInstance().getS3Buckets(awsSession, region);
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
	String Profile = "";
	String Region = "";
	String Bucket = "";
	String StartingPrefix = "";
	String CatalogFile = "";
	String CatalogIndexFile = "";
	String UploadCatalogFiles = "";
	String DistributionId = "";
	String DatasetIndexFile = "";
	String DatasetIndexHeadFile = "";
	String DatasetIndexBodyFile = "";
	String DatasetIndexFooterFile = "";
	String UploadDatasetFiles = "";
	String OutputTableID = "";
	String KeepFiles = "";
	String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		Bucket = parameters.getValue ( "Bucket" );
		StartingPrefix = parameters.getValue ( "StartingPrefix" );
		CatalogFile = parameters.getValue ( "CatalogFile" );
		CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" );
		UploadCatalogFiles = parameters.getValue ( "UploadCatalogFiles" );
		DistributionId = parameters.getValue ( "DistributionId" );
		DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" );
		DatasetIndexHeadFile = parameters.getValue ( "DatasetIndexHeadFile" );
		DatasetIndexBodyFile = parameters.getValue ( "DatasetIndexBodyFile" );
		DatasetIndexFooterFile = parameters.getValue ( "DatasetIndexFooterFile" );
		UploadDatasetFiles = parameters.getValue ( "UploadDatasetFiles" );
		OutputTableID = parameters.getValue ( "OutputTableID" );
		KeepFiles = parameters.getValue ( "KeepFiles" );
		IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
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
        if ( StartingPrefix != null ) {
            __StartingPrefix_JTextField.setText ( StartingPrefix );
        }
        if ( CatalogFile != null ) {
            __CatalogFile_JTextField.setText ( CatalogFile );
        }
        if ( CatalogIndexFile != null ) {
            __CatalogIndexFile_JTextField.setText ( CatalogIndexFile );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__UploadCatalogFiles_JComboBox, UploadCatalogFiles,JGUIUtil.NONE, null, null ) ) {
			__UploadCatalogFiles_JComboBox.select ( UploadCatalogFiles );
		}
		else {
            if ( (UploadCatalogFiles == null) ||	UploadCatalogFiles.equals("") ) {
				// New command...select the default.
				__UploadCatalogFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"UploadCatalogFiles parameter \"" + UploadCatalogFiles + "\".  Select a value or Cancel." );
			}
		}
        if ( DistributionId != null ) {
            __DistributionId_JTextField.setText ( DistributionId );
        }
        if ( DatasetIndexFile != null ) {
            __DatasetIndexFile_JTextField.setText ( DatasetIndexFile );
        }
        if ( DatasetIndexHeadFile != null ) {
            __DatasetIndexHeadFile_JTextField.setText ( DatasetIndexHeadFile );
        }
        if ( DatasetIndexBodyFile != null ) {
            __DatasetIndexBodyFile_JTextField.setText ( DatasetIndexBodyFile );
        }
        if ( DatasetIndexFooterFile != null ) {
            __DatasetIndexFooterFile_JTextField.setText ( DatasetIndexFooterFile );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__UploadDatasetFiles_JComboBox, UploadDatasetFiles,JGUIUtil.NONE, null, null ) ) {
			__UploadDatasetFiles_JComboBox.select ( UploadDatasetFiles );
		}
		else {
            if ( (UploadDatasetFiles == null) ||	UploadDatasetFiles.equals("") ) {
				// New command...select the default.
				__UploadDatasetFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"UploadDatasetFiles parameter \"" + UploadDatasetFiles + "\".  Select a value or Cancel." );
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
		// Set the tab to the input.
		__main_JTabbedPane.setSelectedIndex(0);
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
	StartingPrefix = __StartingPrefix_JTextField.getText().trim();
	CatalogFile = __CatalogFile_JTextField.getText().trim();
	CatalogIndexFile = __CatalogIndexFile_JTextField.getText().trim();
	UploadCatalogFiles = __UploadCatalogFiles_JComboBox.getSelected();
	DistributionId = __DistributionId_JTextField.getText().trim();
	DatasetIndexFile = __DatasetIndexFile_JTextField.getText().trim();
	DatasetIndexHeadFile = __DatasetIndexHeadFile_JTextField.getText().trim();
	DatasetIndexBodyFile = __DatasetIndexBodyFile_JTextField.getText().trim();
	DatasetIndexFooterFile = __DatasetIndexFooterFile_JTextField.getText().trim();
	UploadDatasetFiles = __UploadDatasetFiles_JComboBox.getSelected();
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	KeepFiles = __KeepFiles_JComboBox.getSelected();
	IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	props.add ( "Bucket=" + Bucket );
	props.add ( "StartingPrefix=" + StartingPrefix );
	props.add ( "CatalogFile=" + CatalogFile );
	props.add ( "CatalogIndexFile=" + CatalogIndexFile );
	props.add ( "UploadCatalogFiles=" + UploadCatalogFiles );
	props.add ( "DistributionId=" + DistributionId );
	props.add ( "DatasetIndexFile=" + DatasetIndexFile );
	props.add ( "DatasetIndexHeadFile=" + DatasetIndexHeadFile );
	props.add ( "DatasetIndexBodyFile=" + DatasetIndexBodyFile );
	props.add ( "DatasetIndexFooterFile=" + DatasetIndexFooterFile );
	props.add ( "UploadDatasetFiles=" + UploadDatasetFiles );
	props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "KeepFiles=" + KeepFiles );
	props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be.
    if ( __pathCatalogFile_JButton != null ) {
		if ( (CatalogFile != null) && !CatalogFile.isEmpty() ) {
			__pathCatalogFile_JButton.setEnabled ( true );
			File f = new File ( CatalogFile );
			if ( f.isAbsolute() ) {
				__pathCatalogFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathCatalogFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathCatalogFile_JButton.setText ( __AddWorkingDirectory );
            	__pathCatalogFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathCatalogFile_JButton.setEnabled(false);
		}
    }
    if ( __pathCatalogIndexFile_JButton != null ) {
		if ( (CatalogIndexFile != null) && !CatalogIndexFile.isEmpty() ) {
			__pathCatalogIndexFile_JButton.setEnabled ( true );
			File f = new File ( CatalogIndexFile );
			if ( f.isAbsolute() ) {
				__pathCatalogIndexFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathCatalogIndexFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathCatalogIndexFile_JButton.setText ( __AddWorkingDirectory );
            	__pathCatalogIndexFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathCatalogIndexFile_JButton.setEnabled(false);
		}
    }
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
    if ( __pathDatasetIndexHeadFile_JButton != null ) {
		if ( (DatasetIndexHeadFile != null) && !DatasetIndexHeadFile.isEmpty() ) {
			__pathDatasetIndexHeadFile_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexHeadFile );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexHeadFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexHeadFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexHeadFile_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexHeadFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexHeadFile_JButton.setEnabled(false);
		}
    }
    if ( __pathDatasetIndexBodyFile_JButton != null ) {
		if ( (DatasetIndexBodyFile != null) && !DatasetIndexBodyFile.isEmpty() ) {
			__pathDatasetIndexBodyFile_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexBodyFile );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexBodyFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexBodyFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexBodyFile_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexBodyFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexBodyFile_JButton.setEnabled(false);
		}
    }
    if ( __pathDatasetIndexFooterFile_JButton != null ) {
		if ( (DatasetIndexFooterFile != null) && !DatasetIndexFooterFile.isEmpty() ) {
			__pathDatasetIndexFooterFile_JButton.setEnabled ( true );
			File f = new File ( DatasetIndexFooterFile );
			if ( f.isAbsolute() ) {
				__pathDatasetIndexFooterFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathDatasetIndexFooterFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDatasetIndexFooterFile_JButton.setText ( __AddWorkingDirectory );
            	__pathDatasetIndexFooterFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDatasetIndexFooterFile_JButton.setEnabled(false);
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