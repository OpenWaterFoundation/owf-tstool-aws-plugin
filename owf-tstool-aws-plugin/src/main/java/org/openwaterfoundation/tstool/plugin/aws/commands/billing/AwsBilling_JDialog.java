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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openwaterfoundation.tstool.plugin.aws.PluginMeta;
import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import RTi.TS.TSFormatSpecifiersJPanel;
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
implements ActionListener, ChangeListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseGroupedFile_JButton = null;
private SimpleJButton __pathGroupedFile_JButton = null;
private SimpleJButton __browseTotalFile_JButton = null;
private SimpleJButton __pathTotalFile_JButton = null;
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
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Cost Explorer Query tab.
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private SimpleJComboBox __TimeChunk_JComboBox = null;
private SimpleJComboBox __Granularity_JComboBox = null;
private SimpleJComboBox __GroupBy1_JComboBox = null;
private JTextField __GroupByTag1_JTextField = null;
private SimpleJComboBox __GroupBy2_JComboBox = null;
private JTextField __GroupByTag2_JTextField = null;
// The AWS SDK is limited to 2 GroupBy but maybe more can be enabled later.
//private SimpleJComboBox __GroupBy3_JComboBox = null;
//private JTextField __GroupByTag3_JTextField = null;
private SimpleJComboBox __Metric_JComboBox = null;

// Cost Explorer Filter tab.
private JTextField __FilterAvailabilityZones_JTextField = null;
private JTextField __FilterInstanceTypes_JTextField = null;
private JTextField __FilterRegions_JTextField = null;
private JTextField __FilterServices_JTextField = null;
private JTextField __FilterTags_JTextField = null;

// 'Output Cost Tables' tab.
private SimpleJComboBox __GroupedTableID_JComboBox = null;
private JTextField __GroupedTableFile_JTextField = null;
private JTextField __GroupedTableRowCountProperty_JTextField = null;
private SimpleJComboBox __TotalTableID_JComboBox = null;
private JTextField __TotalTableFile_JTextField = null;
private JTextField __TotalTableRowCountProperty_JTextField = null;
private SimpleJComboBox __AppendOutput_JComboBox = null;

// Output Time Series tab.
private SimpleJComboBox __CreateGroupedTimeSeries_JComboBox = null;
private SimpleJComboBox __GroupedTimeSeriesLocationID_JComboBox = null;
private SimpleJComboBox __GroupedTimeSeriesDataType_JComboBox = null;
private TSFormatSpecifiersJPanel __GroupedTimeSeriesAlias_JTextField = null;
private JTextField __TimeSeriesMissingGroupBy_JTextField = null;
private SimpleJComboBox __CreateTotalTimeSeries_JComboBox = null;
private JTextField __TotalTimeSeriesLocationID_JTextField = null;
private SimpleJComboBox __TotalTimeSeriesDataType_JComboBox = null;
private TSFormatSpecifiersJPanel __TotalTimeSeriesAlias_JTextField = null;

// 'Output Service Properties' tab.
private SimpleJComboBox __EC2PropertiesTableID_JComboBox = null;
private SimpleJComboBox __EBSSnapshotsTableID_JComboBox = null;
private SimpleJComboBox __EC2ImagesTableID_JComboBox = null;

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

    if ( o == __browseGroupedFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Output File for Grouped Table");

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
					__GroupedTableFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
    else if ( o == __browseTotalFile_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Output File for Total Table");

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
					__TotalTableFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
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
		HelpViewer.getInstance().showHelp("command", "AwsBilling", PluginMeta.getDocumentationRootUrl());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __pathGroupedFile_JButton ) {
        if ( __pathGroupedFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __GroupedTableFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__GroupedTableFile_JTextField.getText() ) );
        }
        else if ( __pathGroupedFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __GroupedTableFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __GroupedTableFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"AwsBilling_JDialog", "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __pathTotalFile_JButton ) {
        if ( __pathTotalFile_JButton.getText().equals(__AddWorkingDirectory) ) {
            __TotalTableFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__TotalTableFile_JTextField.getText() ) );
        }
        else if ( __pathTotalFile_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __TotalTableFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __TotalTableFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"AwsBilling_JDialog", "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
	else {
		// Choices.
		refresh();
	}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e ) {
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
    refresh();
}

// ...end event handlers for DocumentListener.

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
	// Cost Explorer Query.
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TimeChunk = __TimeChunk_JComboBox.getSelected();
	String Granularity = __Granularity_JComboBox.getSelected();
	String GroupBy1 = __GroupBy1_JComboBox.getSelected();
	String GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	String GroupBy2 = __GroupBy2_JComboBox.getSelected();
	String GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	//String GroupBy3 = __GroupBy3_JComboBox.getSelected();
	//String GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	String Metric = __Metric_JComboBox.getSelected();
	// Cost Explorer Filter.
	String FilterAvailabilityZones = __FilterAvailabilityZones_JTextField.getText().trim();
	String FilterInstanceTypes = __FilterInstanceTypes_JTextField.getText().trim();
	String FilterRegions = __FilterRegions_JTextField.getText().trim();
	String FilterServices = __FilterServices_JTextField.getText().trim();
	String FilterTags = __FilterTags_JTextField.getText().trim();
	// Output Cost Tables.
	String GroupedTableID = __GroupedTableID_JComboBox.getSelected();
	String GroupedTableFile = __GroupedTableFile_JTextField.getText().trim();
	String GroupedTableRowCountProperty = __GroupedTableRowCountProperty_JTextField.getText().trim();
	String TotalTableID = __TotalTableID_JComboBox.getSelected();
	String TotalTableFile = __TotalTableFile_JTextField.getText().trim();
	String TotalTableRowCountProperty = __TotalTableRowCountProperty_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	// Output Time series.
	String CreateGroupedTimeSeries = __CreateGroupedTimeSeries_JComboBox.getSelected();
	String GroupedTimeSeriesLocationID = __GroupedTimeSeriesLocationID_JComboBox.getSelected();
	String GroupedTimeSeriesDataType = __GroupedTimeSeriesDataType_JComboBox.getSelected();
	String GroupedTimeSeriesAlias = __GroupedTimeSeriesAlias_JTextField.getText().trim();
	String TimeSeriesMissingGroupBy = __TimeSeriesMissingGroupBy_JTextField.getText().trim();
	String CreateTotalTimeSeries = __CreateTotalTimeSeries_JComboBox.getSelected();
	String TotalTimeSeriesLocationID = __TotalTimeSeriesLocationID_JTextField.getText().trim();
	String TotalTimeSeriesDataType = __TotalTimeSeriesDataType_JComboBox.getSelected();
	String TotalTimeSeriesAlias = __TotalTimeSeriesAlias_JTextField.getText().trim();
	// Output Service Properties.
	String EC2PropertiesTableID = __EC2PropertiesTableID_JComboBox.getSelected();
	String EBSSnapshotsTableID = __EBSSnapshotsTableID_JComboBox.getSelected();
	String EC2ImagesTableID = __EC2ImagesTableID_JComboBox.getSelected();

	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
	__error_wait = false;
	if ( (Profile != null) && !Profile.isEmpty() ) {
		props.set ( "Profile", Profile );
	}
	if ( (Region != null) && !Region.isEmpty() ) {
		props.set ( "Region", Region );
	}
	// Cost Explorer Query.
	if ( (InputStart != null) && !InputStart.isEmpty()) {
		props.set ( "InputStart", InputStart );
	}
	if ( (InputEnd != null) && !InputEnd.isEmpty() ) {
		props.set ( "InputEnd", InputEnd );
	}
	if ( (TimeChunk != null) && !TimeChunk.isEmpty() ) {
		props.set ( "TimeChunk", TimeChunk );
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
	//if ( (GroupBy3 != null) && !GroupBy3.isEmpty() ) {
	//	props.set ( "GroupBy3", GroupBy3 );
	//}
	//if ( (GroupByTag3 != null) && !GroupByTag3.isEmpty() ) {
	//	props.set ( "GroupByTag3", GroupByTag3 );
	//}
	if ( (Metric != null) && !Metric.isEmpty() ) {
		props.set ( "Metric", Metric );
	}
	// Cost Explorer Filter.
	if ( (FilterAvailabilityZones != null) && !FilterAvailabilityZones.isEmpty()) {
		props.set ( "FilterAvailabilityZones", FilterAvailabilityZones );
	}
	if ( (FilterInstanceTypes != null) && !FilterInstanceTypes.isEmpty()) {
		props.set ( "FilterInstanceTypes", FilterInstanceTypes );
	}
	if ( (FilterRegions != null) && !FilterRegions.isEmpty()) {
		props.set ( "FilterRegions", FilterRegions );
	}
	if ( (FilterServices != null) && !FilterServices.isEmpty()) {
		props.set ( "FilterServices", FilterServices );
	}
	if ( (FilterTags != null) && !FilterTags.isEmpty()) {
		props.set ( "FilterTags", FilterTags );
	}
	// Output Cost Tables.
    if ( (GroupedTableID != null) && !GroupedTableID.isEmpty() ) {
        props.set ( "GroupedTableID", GroupedTableID );
    }
    if ( (GroupedTableFile != null) && !GroupedTableFile.isEmpty() ) {
        props.set ( "GroupedTableFile", GroupedTableFile );
    }
    if ( (GroupedTableRowCountProperty != null) && !GroupedTableRowCountProperty.isEmpty() ) {
        props.set ( "GroupedTableRowCountProperty", GroupedTableRowCountProperty );
    }
    if ( (TotalTableID != null) && !TotalTableID.isEmpty() ) {
        props.set ( "TotalTableID", TotalTableID );
    }
    if ( (TotalTableFile != null) && !TotalTableFile.isEmpty() ) {
        props.set ( "TotalTableFile", TotalTableFile );
    }
    if ( (TotalTableRowCountProperty != null) && !TotalTableRowCountProperty.isEmpty() ) {
        props.set ( "TotalTableRowCountProperty", TotalTableRowCountProperty );
    }
    if ( (AppendOutput != null) && !AppendOutput.isEmpty() ) {
        props.set ( "AppendOutput", AppendOutput );
    }
	// Output Time series.
    if ( (CreateGroupedTimeSeries != null) && !CreateGroupedTimeSeries.isEmpty() ) {
        props.set ( "CreateGroupedTimeSeries", CreateGroupedTimeSeries );
    }
    if ( (GroupedTimeSeriesLocationID != null) && !GroupedTimeSeriesLocationID.isEmpty() ) {
        props.set ( "GroupedTimeSeriesLocationID", GroupedTimeSeriesLocationID );
    }
    if ( (GroupedTimeSeriesDataType != null) && !GroupedTimeSeriesDataType.isEmpty() ) {
        props.set ( "GroupedTimeSeriesDataType", GroupedTimeSeriesDataType );
    }
    if ( (GroupedTimeSeriesAlias != null) && !GroupedTimeSeriesAlias.isEmpty() ) {
        props.set ( "GroupedTimeSeriesAlias", GroupedTimeSeriesAlias );
    }
    if ( (TimeSeriesMissingGroupBy != null) && !TimeSeriesMissingGroupBy.isEmpty() ) {
        props.set ( "TimeSeriesMissingGroupBy", TimeSeriesMissingGroupBy );
    }
    if ( (CreateTotalTimeSeries != null) && !CreateTotalTimeSeries.isEmpty() ) {
        props.set ( "CreateTotalTimeSeries", CreateTotalTimeSeries );
    }
    if ( (TotalTimeSeriesLocationID != null) && !TotalTimeSeriesLocationID.isEmpty() ) {
        props.set ( "TotalTimeSeriesLocationID", TotalTimeSeriesLocationID );
    }
    if ( (TotalTimeSeriesDataType != null) && !TotalTimeSeriesDataType.isEmpty() ) {
        props.set ( "TotalTimeSeriesDataType", TotalTimeSeriesDataType );
    }
    if ( (TotalTimeSeriesAlias != null) && !TotalTimeSeriesAlias.isEmpty() ) {
        props.set ( "TotalTimeSeriesAlias", TotalTimeSeriesAlias );
    }
	// Output Service Properties.
    if ( (EC2PropertiesTableID != null) && !EC2PropertiesTableID.isEmpty() ) {
        props.set ( "EC2PropertiesTableID", EC2PropertiesTableID );
    }
    if ( (EBSSnapshotsTableID != null) && !EBSSnapshotsTableID.isEmpty() ) {
        props.set ( "EBSSnapshotsTableID", EBSSnapshotsTableID );
    }
    if ( (EC2ImagesTableID != null) && !EC2ImagesTableID.isEmpty() ) {
        props.set ( "EC2ImagesTableID", EC2ImagesTableID );
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
	// Cost Explorer Query.
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TimeChunk = __TimeChunk_JComboBox.getSelected();
	String Granularity = __Granularity_JComboBox.getSelected();
	String GroupBy1 = __GroupBy1_JComboBox.getSelected();
	String GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	String GroupBy2 = __GroupBy2_JComboBox.getSelected();
	String GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	//String GroupBy3 = __GroupBy3_JComboBox.getSelected();
	//String GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	String Metric = __Metric_JComboBox.getSelected();
	// Cost Explorer Filter.
	String FilterAvailabilityZones = __FilterAvailabilityZones_JTextField.getText().trim();
	String FilterInstanceTypes = __FilterInstanceTypes_JTextField.getText().trim();
	String FilterRegions = __FilterRegions_JTextField.getText().trim();
	String FilterServices = __FilterServices_JTextField.getText().trim();
	String FilterTags = __FilterTags_JTextField.getText().trim();
	// Output Cost Tables.
	String GroupedTableID = __GroupedTableID_JComboBox.getSelected();
    String GroupedTableFile = __GroupedTableFile_JTextField.getText().trim();
    String GroupedTableRowCountProperty = __GroupedTableRowCountProperty_JTextField.getText().trim();
	String TotalTableID = __TotalTableID_JComboBox.getSelected();
    String TotalTableFile = __TotalTableFile_JTextField.getText().trim();
    String TotalTableRowCountProperty = __TotalTableRowCountProperty_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	// Output Time series.
	String CreateGroupedTimeSeries = __CreateGroupedTimeSeries_JComboBox.getSelected();
	String GroupedTimeSeriesLocationID = __GroupedTimeSeriesLocationID_JComboBox.getSelected();
	String GroupedTimeSeriesDataType = __GroupedTimeSeriesDataType_JComboBox.getSelected();
	String GroupedTimeSeriesAlias = __GroupedTimeSeriesAlias_JTextField.getText().trim();
	String TimeSeriesMissingGroupBy = __TimeSeriesMissingGroupBy_JTextField.getText().trim();
	String CreateTotalTimeSeries = __CreateTotalTimeSeries_JComboBox.getSelected();
	String TotalTimeSeriesLocationID = __TotalTimeSeriesLocationID_JTextField.getText().trim();
	String TotalTimeSeriesDataType = __TotalTimeSeriesDataType_JComboBox.getSelected();
	String TotalTimeSeriesAlias = __TotalTimeSeriesAlias_JTextField.getText().trim();
	// Output Service Properties.
	String EC2PropertiesTableID = __EC2PropertiesTableID_JComboBox.getSelected();
	String EBSSnapshotsTableID = __EBSSnapshotsTableID_JComboBox.getSelected();
	String EC2ImagesTableID = __EC2ImagesTableID_JComboBox.getSelected();
	//String IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();

    // General.
	__command.setCommandParameter ( "Profile", Profile );
	__command.setCommandParameter ( "Region", Region );
	// Cost Explorer Query.
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "TimeChunk", TimeChunk );
	__command.setCommandParameter ( "Granularity", Granularity );
	__command.setCommandParameter ( "GroupBy1", GroupBy1 );
	__command.setCommandParameter ( "GroupByTag1", GroupByTag1 );
	__command.setCommandParameter ( "GroupBy2", GroupBy2 );
	__command.setCommandParameter ( "GroupByTag2", GroupByTag2 );
	//__command.setCommandParameter ( "GroupBy3", GroupBy3 );
	//__command.setCommandParameter ( "GroupByTag3", GroupByTag3 );
	__command.setCommandParameter ( "Metric", Metric );
	// Cost Explorer Filter.
	__command.setCommandParameter ( "FilterAvailabilityZones", FilterAvailabilityZones );
	__command.setCommandParameter ( "FilterInstanceTypes", FilterInstanceTypes );
	__command.setCommandParameter ( "FilterRegions", FilterRegions );
	__command.setCommandParameter ( "FilterServices", FilterServices );
	__command.setCommandParameter ( "FilterTags", FilterTags );
	// Output Cost Tables.
	__command.setCommandParameter ( "GroupedTableID", GroupedTableID );
	__command.setCommandParameter ( "GroupedTableFile", GroupedTableFile );
	__command.setCommandParameter ( "GroupedTableRowCountProperty", GroupedTableRowCountProperty );
	__command.setCommandParameter ( "TotalTableID", TotalTableID );
	__command.setCommandParameter ( "TotalTableFile", TotalTableFile );
	__command.setCommandParameter ( "TotalTableRowCountProperty", TotalTableRowCountProperty );
	__command.setCommandParameter ( "AppendOutput", AppendOutput );
	// Output Time Series.
	__command.setCommandParameter ( "CreateGroupedTimeSeries", CreateGroupedTimeSeries );
	__command.setCommandParameter ( "GroupedTimeSeriesLocationID", GroupedTimeSeriesLocationID );
	__command.setCommandParameter ( "GroupedTimeSeriesDataType", GroupedTimeSeriesDataType );
	__command.setCommandParameter ( "GroupedTimeSeriesAlias", GroupedTimeSeriesAlias );
	__command.setCommandParameter ( "TimeSeriesMissingGroupBy", TimeSeriesMissingGroupBy );
	__command.setCommandParameter ( "CreateTotalTimeSeries", CreateTotalTimeSeries );
	__command.setCommandParameter ( "TotalTimeSeriesLocationID", TotalTimeSeriesLocationID );
	__command.setCommandParameter ( "TotalTimeSeriesDataType", TotalTimeSeriesDataType );
	__command.setCommandParameter ( "TotalTimeSeriesAlias", TotalTimeSeriesAlias );
	// Output Service Properties.
	__command.setCommandParameter ( "EC2PropertiesTableID", EC2PropertiesTableID );
	__command.setCommandParameter ( "EBSSnapshotsTableID", EBSSnapshotsTableID );
	__command.setCommandParameter ( "EC2ImagesTableID", EC2ImagesTableID );
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

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for general 'Cost Explorer' query parameters.
    int yCostExplorer = -1;
    JPanel costExplorer_JPanel = new JPanel();
    costExplorer_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Cost Explorer Query", costExplorer_JPanel );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Query AWS Cost Explorer data.  See AWS console Billing and Cost Management / Cost Explorer."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Some choices may be limited based on AWS Billing configuration "
   		+ "(e.g., daily data are only available for 14 previous days)."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Up to two 'Group by' can be specified, for example to group by service and a tag."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("See the 'Output' tab to specify the output table and/or file for the output."),
		0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCostExplorer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ("Input start:"),
        0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (40);
    __InputStart_JTextField.setToolTipText("Input start for billing data, with daily precision, can use ${Property} syntax.");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __InputStart_JTextField,
        1, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Input end:"),
        0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (40);
    __InputEnd_JTextField.setToolTipText("Input end for billing data, with daily precision, can use ${Property} syntax.");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __InputEnd_JTextField,
        1, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Time chunk:"),
		0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TimeChunk_JComboBox = new SimpleJComboBox ( false );
	__TimeChunk_JComboBox.setToolTipText("Time chunk to break up queries (default is full period).");
	List<String> timeChunkChoices = new ArrayList<>();
	timeChunkChoices.add("");
	// Use multiple of days to simplify handling of time chunking.
	timeChunkChoices.add("30Day");
	timeChunkChoices.add("60Day");
	timeChunkChoices.add("90Day");
	timeChunkChoices.add("180Day");
	timeChunkChoices.add("365Day");
	__TimeChunk_JComboBox.setData(timeChunkChoices);
	__TimeChunk_JComboBox.select ( 0 );
	__TimeChunk_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(costExplorer_JPanel, __TimeChunk_JComboBox,
		1, yCostExplorer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel(
		"Optional - time chunk for queries (default=full period)."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
		//"Optional - dimension by which to group output (default=" + AwsBillingDimensionType.SERVICE + ")."),
		"Optional - dimension by which to group output (default=no grouping)."),
		3, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Group by tag (1):"),
        0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupByTag1_JTextField = new JTextField (20);
    __GroupByTag1_JTextField.setToolTipText("If GroupBy1=Tag, specify the tag name to group by, can use ${Property} syntax.");
    __GroupByTag1_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupByTag1_JTextField,
        1, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
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
        0, ++yCostExplorer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupByTag2_JTextField = new JTextField (20);
    __GroupByTag2_JTextField.setToolTipText("If GroupBy2=Tag, specify the tag name to group by, can use ${Property} syntax.");
    __GroupByTag2_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(costExplorer_JPanel, __GroupByTag2_JTextField,
        1, yCostExplorer, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Optional - used with GroupBy2=Tag."),
        3, yCostExplorer, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(costExplorer_JPanel, new JLabel ( "Metric (aggregate costs by):"),
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

    // Panel for general 'Cost Explorer' filter parameters.
    int yFilter = -1;
    JPanel filter_JPanel = new JPanel();
    filter_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Cost Explorer Filters", filter_JPanel );

    JGUIUtil.addComponent(filter_JPanel, new JLabel ("Filter Cost Explorer data.  See AWS console Billing and Cost Management / Cost Explorer."),
		0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ("A limited number of filters have been implemented."),
		0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ("The filter groups are ANDed with values in a group ORed."),
		0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ("See the 'Output Cost Tables' and 'Output Time Series' tabs to specify output formats."),
		0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ("See the documentation for possible values or use a group by and then filter on group values."),
		0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yFilter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Filter to availability zone(s):"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilterAvailabilityZones_JTextField = new JTextField (30);
    __FilterAvailabilityZones_JTextField.setToolTipText("One or more availability zones (e.g., us-west-1a) to filter output, separated by commas, can use ${Property} syntax.");
    __FilterAvailabilityZones_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(filter_JPanel, __FilterAvailabilityZones_JTextField,
        1, yFilter, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - availability zone(s) to filter output."),
        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Filter to instance type(s):"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilterInstanceTypes_JTextField = new JTextField (30);
    __FilterInstanceTypes_JTextField.setToolTipText("One or more instance types (e.g., t2.xlarge) to filter output, separated by commas, can use ${Property} syntax.");
    __FilterInstanceTypes_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(filter_JPanel, __FilterInstanceTypes_JTextField,
        1, yFilter, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - instance type(s) to filter output."),
        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Filter to region(s):"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilterRegions_JTextField = new JTextField (30);
    __FilterRegions_JTextField.setToolTipText("One or more regions (e.g., us-west-1) to filter output, separated by commas, can use ${Property} syntax.");
    __FilterRegions_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(filter_JPanel, __FilterRegions_JTextField,
        1, yFilter, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - region(s) to filter output."),
        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Filter to services(s):"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilterServices_JTextField = new JTextField (30);
    __FilterServices_JTextField.setToolTipText("One or more services (e.g., Amazon Simple Storage Service) to filter output, separated by commas, can use ${Property} syntax.");
    __FilterServices_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(filter_JPanel, __FilterServices_JTextField,
        1, yFilter, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - services(s) to filter output."),
        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Filter to tag(s):"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FilterTags_JTextField = new JTextField (30);
    // TODO Enable later once API is understood.
    __FilterTags_JTextField.setEnabled(false);
    __FilterTags_JTextField.setToolTipText("One or more tags to filter output, separated by commas, can use ${Property} syntax.");
    __FilterTags_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(filter_JPanel, __FilterTags_JTextField,
        1, yFilter, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - tag(s) to filter output (CURRENTLY DISABLED)."),
        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for output tables.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Cost Tables", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
    	"The following parameters are used to specify the output cost tables for Cost Explorer results."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Grouped or total data table(s) and/or file(s) can be created."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("An existing table will be appended to if found and AppendOutput=True."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("The output file uses the specified table (or a temporary table) to create the output file."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: csv"),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("See also other commands to write tables in different formats."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Grouped table ID:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __GroupedTableID_JComboBox.setToolTipText("Table for output, can use ${Property} notation.");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __GroupedTableID_JComboBox.setData ( tableIDChoices );
    __GroupedTableID_JComboBox.addItemListener ( this );
    __GroupedTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__GroupedTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(output_JPanel, __GroupedTableID_JComboBox,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - table for grouped output."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Grouped table output file:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTableFile_JTextField = new JTextField ( 50 );
    __GroupedTableFile_JTextField.setToolTipText("Grouped table output file, can use ${Property} notation.");
    __GroupedTableFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel GroupedTableFile_JPanel = new JPanel();
	GroupedTableFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(GroupedTableFile_JPanel, __GroupedTableFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseGroupedFile_JButton = new SimpleJButton ( "...", this );
	__browseGroupedFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(GroupedTableFile_JPanel, __browseGroupedFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathGroupedFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(GroupedTableFile_JPanel, __pathGroupedFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(output_JPanel, GroupedTableFile_JPanel,
		1, yOutput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Grouped table row count property:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTableRowCountProperty_JTextField = new JTextField (40);
    __GroupedTableRowCountProperty_JTextField.setToolTipText("Property name for grouped table row count.");
    __GroupedTableRowCountProperty_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(output_JPanel, __GroupedTableRowCountProperty_JTextField,
        1, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Optional - property for output table row count."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Total table ID:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __TotalTableID_JComboBox.setToolTipText("Table for output, can use ${Property} notation.");
    //tableIDChoices.add(0,""); // Add blank to ignore table.
    __TotalTableID_JComboBox.setData ( tableIDChoices );
    __TotalTableID_JComboBox.addItemListener ( this );
    __TotalTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TotalTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(output_JPanel, __TotalTableID_JComboBox,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - table for totalized output."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Total table output file:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTableFile_JTextField = new JTextField ( 50 );
    __TotalTableFile_JTextField.setToolTipText("Total table output file, can use ${Property} notation.");
    __TotalTableFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel TotalTableFile_JPanel = new JPanel();
	TotalTableFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(TotalTableFile_JPanel, __TotalTableFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseTotalFile_JButton = new SimpleJButton ( "...", this );
	__browseTotalFile_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(TotalTableFile_JPanel, __browseTotalFile_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathTotalFile_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(TotalTableFile_JPanel, __pathTotalFile_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(output_JPanel, TotalTableFile_JPanel,
		1, yOutput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Total table row count property:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTableRowCountProperty_JTextField = new JTextField (40);
    __TotalTableRowCountProperty_JTextField.setToolTipText("Property name for output table row count.");
    __TotalTableRowCountProperty_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(output_JPanel, __TotalTableRowCountProperty_JTextField,
        1, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Optional - property for total table row count."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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

    // Panel for 'Output Time Series' parameters.
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Time Series", ts_JPanel );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Indicate whether grouped or total time series output should be created."),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("The time series interval will match the granularity."),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Time series identifiers use the Cost Explorer data to create unique values:"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("  - can use a built-in format"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("  - use " + this.__command._Auto + " for default that is useful to review available data"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("  - or specify a string using GROUPBY1, GROUPBYTAG1, GROUPBY1VALUE, GROUPBY2, GROUPBYTAG2, GROUPBY2VALUE, "
    		+ "which will be replaced with data"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("  - missing group by values, such as missing tags, by default are indicated by 'Unknown'"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("  - see the command documentation for details"),
		0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yts, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Create grouped time series?:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CreateGroupedTimeSeries_JComboBox = new SimpleJComboBox ( false ); // Don't allow edit.
    __CreateGroupedTimeSeries_JComboBox.setToolTipText("Create grouped time series for the results?");
    List<String> createGroupedTimeSeriesChoices = new ArrayList<>();
    createGroupedTimeSeriesChoices.add("");
    createGroupedTimeSeriesChoices.add(__command._False);
    createGroupedTimeSeriesChoices.add(__command._True);
    __CreateGroupedTimeSeries_JComboBox.setData ( createGroupedTimeSeriesChoices );
    __CreateGroupedTimeSeries_JComboBox.addItemListener ( this );
    __CreateGroupedTimeSeries_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __CreateGroupedTimeSeries_JComboBox,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - whether to create grouped time series (default=" + __command._False + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Grouped time series location ID:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTimeSeriesLocationID_JComboBox = new SimpleJComboBox ( 30, true ); // Do allow edit.
    __GroupedTimeSeriesLocationID_JComboBox.setToolTipText(
    	"Location ID for grouped time series, use a built-in format or can be a string containing "
    	+ "GROUPBY1, GROUPBYTAG1, GROUPBY1VALUE, GROUPBY2, GROUPBYTAG2, GROUPBY2VALUE");
    List<String> locationIdChoices = new ArrayList<>();
    locationIdChoices.add("");
    locationIdChoices.add(__command._Auto);
    locationIdChoices.add(__command._GroupBy1);
    locationIdChoices.add(__command._GroupBy2);
    //locationIdChoices.add(__command._GroupBy3);
    __GroupedTimeSeriesLocationID_JComboBox.setData ( locationIdChoices );
    __GroupedTimeSeriesLocationID_JComboBox.addItemListener ( this );
    __GroupedTimeSeriesLocationID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __GroupedTimeSeriesLocationID_JComboBox,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - location ID for grouped time series (default=" + __command._Auto + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Grouped time series data type:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTimeSeriesDataType_JComboBox = new SimpleJComboBox ( 30, true ); // Do allow edit.
    __GroupedTimeSeriesDataType_JComboBox.setToolTipText(
    	"Data type for grouped time series, use a built-in format or can be a string containing "
    	+ "GROUPBY1, GROUPBYTAG1, GROUPBY1VALUE, GROUPBY2, GROUPBYTAG2, GROUPBY2VALUE");
    List<String> dataTypeChoices = new ArrayList<>();
    dataTypeChoices.add("");
    dataTypeChoices.add(__command._Auto);
    dataTypeChoices.add(__command._GroupBy1);
    dataTypeChoices.add(__command._GroupBy2);
    __GroupedTimeSeriesDataType_JComboBox.setData ( dataTypeChoices );
    __GroupedTimeSeriesDataType_JComboBox.addItemListener ( this );
    __GroupedTimeSeriesDataType_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __GroupedTimeSeriesDataType_JComboBox,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - grouped time series data type (default=" + __command._Auto + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel("Grouped time series alias to assign:"),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GroupedTimeSeriesAlias_JTextField = new TSFormatSpecifiersJPanel(10);
    __GroupedTimeSeriesAlias_JTextField.setToolTipText("Grouped time series alias, use %L for location, %T for data type, %I for interval.");
    __GroupedTimeSeriesAlias_JTextField.addKeyListener ( this );
    __GroupedTimeSeriesAlias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(ts_JPanel, __GroupedTimeSeriesAlias_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Group by missing value:"),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesMissingGroupBy_JTextField = new JTextField (10);
    __TimeSeriesMissingGroupBy_JTextField.setToolTipText("Data value to use for missing GroupBy values (e.g., Unknown), can use ${Property} syntax.");
    __TimeSeriesMissingGroupBy_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __TimeSeriesMissingGroupBy_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - group by missing value (default=" + this.__command._Unknown + ")."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Create total time series?:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CreateTotalTimeSeries_JComboBox = new SimpleJComboBox ( false ); // Don't allow edit.
    __CreateTotalTimeSeries_JComboBox.setToolTipText("Create grouped time series for the results?");
    List<String> createTotalTimeSeriesChoices = new ArrayList<>();
    createTotalTimeSeriesChoices.add("");
    createTotalTimeSeriesChoices.add(__command._False);
    createTotalTimeSeriesChoices.add(__command._True);
    __CreateTotalTimeSeries_JComboBox.setData ( createTotalTimeSeriesChoices );
    __CreateTotalTimeSeries_JComboBox.addItemListener ( this );
    __CreateTotalTimeSeries_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __CreateTotalTimeSeries_JComboBox,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - whether to create total time series (default=" + __command._False + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Total time series location ID:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTimeSeriesLocationID_JTextField = new JTextField ( 20 );
    __TotalTimeSeriesLocationID_JTextField.setToolTipText( "Location ID for total time series." );
    __TotalTimeSeriesLocationID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TotalTimeSeriesLocationID_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - location ID for total time series (default=" + __command._Total + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Total time series data type:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTimeSeriesDataType_JComboBox = new SimpleJComboBox ( 30, true ); // Do allow edit.
    __TotalTimeSeriesDataType_JComboBox.setToolTipText(
    	"Data type for grouped time series, use a built-in format or can be a string containing "
    	+ "GROUPBY1, GROUPBYTAG1, GROUPBY1VALUE, GROUPBY2, GROUPBYTAG2, GROUPBY2VALUE");
    List<String> totalDataTypeChoices = new ArrayList<>();
    totalDataTypeChoices.add("");
    totalDataTypeChoices.add(__command._Auto);
    __TotalTimeSeriesDataType_JComboBox.setData ( totalDataTypeChoices );
    __TotalTimeSeriesDataType_JComboBox.addItemListener ( this );
    __TotalTimeSeriesDataType_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TotalTimeSeriesDataType_JComboBox,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - total time series data type (default=" + __command._Auto + ")."),
        3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel("Total time series alias to assign:"),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TotalTimeSeriesAlias_JTextField = new TSFormatSpecifiersJPanel(10);
    __TotalTimeSeriesAlias_JTextField.setToolTipText("Total time series alias, use %L for location, %T for data type, %I for interval.");
    __TotalTimeSeriesAlias_JTextField.addKeyListener ( this );
    __TotalTimeSeriesAlias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(ts_JPanel, __TotalTimeSeriesAlias_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for 'Output Service Properties' parameters.
    int yProps = -1;
    JPanel props_JPanel = new JPanel();
    props_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Service Properties", props_JPanel );

    JGUIUtil.addComponent(props_JPanel, new JLabel (
    	"The following parameters specify the output for service properties, focusing on high-cost services."),
		0, ++yProps, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel ("For example, use the data to identify services that are not tagged."),
		0, ++yProps, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel (
    	"Zero or more Elastic Block Storage (EBS) snapshots and Amazon Machine Images (AMIs) may exist for an EC2 instance and are listed separately."),
		0, ++yProps, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yProps, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel ( "EC2 properties table ID:" ),
        0, ++yProps, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EC2PropertiesTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __EC2PropertiesTableID_JComboBox.setToolTipText("Table for EC2 service properties, can use ${Property} notation.");
    //ec2PropsTableIDChoices.add(0,""); // Add blank to ignore table.
    __EC2PropertiesTableID_JComboBox.setData ( tableIDChoices );
    __EC2PropertiesTableID_JComboBox.addItemListener ( this );
    __EC2PropertiesTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__EC2PropertiesTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(props_JPanel, __EC2PropertiesTableID_JComboBox,
        1, yProps, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel( "Optional - table for EC2 service properties list."),
        3, yProps, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel ( "EBS snapshots table ID:" ),
        0, ++yProps, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EBSSnapshotsTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __EBSSnapshotsTableID_JComboBox.setToolTipText("Table for EC2 snapshots, can use ${Property} notation.");
    //tableIDChoices.add(0,""); // Add blank to ignore table.
    __EBSSnapshotsTableID_JComboBox.setData ( tableIDChoices );
    __EBSSnapshotsTableID_JComboBox.addItemListener ( this );
    __EBSSnapshotsTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__EBSSnapshotsTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(props_JPanel, __EBSSnapshotsTableID_JComboBox,
        1, yProps, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel( "Optional - table for EC2 snapshots list."),
        3, yProps, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel ( "EC2 images table ID:" ),
        0, ++yProps, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EC2ImagesTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __EC2ImagesTableID_JComboBox.setToolTipText("Table for EC2 images, can use ${Property} notation.");
    //tableIDChoices.add(0,""); // Add blank to ignore table.
    __EC2ImagesTableID_JComboBox.setData ( tableIDChoices );
    __EC2ImagesTableID_JComboBox.addItemListener ( this );
    __EC2ImagesTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__EC2ImagesTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(props_JPanel, __EC2ImagesTableID_JComboBox,
        1, yProps, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel( "Optional - table for EC2 images list."),
        3, yProps, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Panel for buttons.
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
	}
	else if ( o == this.__Region_JComboBox ) {
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
	// Cost Explorer Query.
	String InputStart = "";
	String InputEnd = "";
	String TimeChunk = "";
	String Granularity = "";
	String GroupBy1 = "";
	String GroupByTag1 = "";
	String GroupBy2 = "";
	String GroupByTag2 = "";
	//String GroupBy3 = "";
	//String GroupByTag3 = "";
	String Metric = "";
	// Cost Explorer Filter.
	String FilterAvailabilityZones = "";
	String FilterInstanceTypes = "";
	String FilterRegions = "";
	String FilterServices = "";
	String FilterTags = "";
	// Output Cost Tables.
	String GroupedTableID = "";
	String GroupedTableFile = "";
	String GroupedTableRowCountProperty = "";
	String TotalTableID = "";
	String TotalTableFile = "";
	String TotalTableRowCountProperty = "";
	String AppendOutput = "";
	// Output Time Series.
	String CreateGroupedTimeSeries = "";
	String GroupedTimeSeriesLocationID = "";
	String GroupedTimeSeriesDataType = "";
	String GroupedTimeSeriesAlias = "";
	String TimeSeriesMissingGroupBy = "";
	String CreateTotalTimeSeries = "";
	String TotalTimeSeriesLocationID = "";
	String TotalTimeSeriesDataType = "";
	String TotalTimeSeriesAlias = "";
	// Output Service Properties.
	String EC2PropertiesTableID = "";
	String EBSSnapshotsTableID = "";
	String EC2ImagesTableID = "";
	//String IfInputNotFound = "";
    PropList parameters = null;

	AwsToolkit awsToolkit = AwsToolkit.getInstance();

	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        // General.
		Profile = parameters.getValue ( "Profile" );
		Region = parameters.getValue ( "Region" );
		// Cost Explorer Query.
		InputStart = parameters.getValue ( "InputStart" );
		InputEnd = parameters.getValue ( "InputEnd" );
		TimeChunk = parameters.getValue ( "TimeChunk" );
		Granularity = parameters.getValue ( "Granularity" );
		GroupBy1 = parameters.getValue ( "GroupBy1" );
		GroupByTag1 = parameters.getValue ( "GroupByTag1" );
		GroupBy2 = parameters.getValue ( "GroupBy2" );
		GroupByTag2 = parameters.getValue ( "GroupByTag2" );
		//GroupBy3 = parameters.getValue ( "GroupBy3" );
		//GroupByTag3 = parameters.getValue ( "GroupByTag3" );
		Metric = parameters.getValue ( "Metric" );
		// Cost Explorer Filter.
		FilterAvailabilityZones = parameters.getValue ( "FilterAvailabilityZones" );
		FilterInstanceTypes = parameters.getValue ( "FilterInstanceTypes" );
		FilterRegions = parameters.getValue ( "FilterRegions" );
		FilterServices = parameters.getValue ( "FilterServices" );
		FilterTags = parameters.getValue ( "Tags" );
		// Output Cost Tables.
		GroupedTableID = parameters.getValue ( "GroupedTableID" );
		GroupedTableFile = parameters.getValue ( "GroupedTableFile" );
		GroupedTableRowCountProperty = parameters.getValue ( "GroupedTableRowCountProperty" );
		TotalTableID = parameters.getValue ( "TotalTableID" );
		TotalTableFile = parameters.getValue ( "TotalTableFile" );
		TotalTableRowCountProperty = parameters.getValue ( "TotalTableRowCountProperty" );
		AppendOutput = parameters.getValue ( "AppendOutput" );
		// Output Time Series.
		CreateGroupedTimeSeries = parameters.getValue ( "CreateGroupedTimeSeries" );
		GroupedTimeSeriesLocationID = parameters.getValue ( "GroupedTimeSeriesLocationID" );
		GroupedTimeSeriesDataType = parameters.getValue ( "GroupedTimeSeriesDataType" );
		GroupedTimeSeriesAlias = parameters.getValue ( "GroupedTimeSeriesAlias" );
		TimeSeriesMissingGroupBy = parameters.getValue ( "TimeSeriesMissingGroupBy" );
		CreateTotalTimeSeries = parameters.getValue ( "CreateTotalTimeSeries" );
		TotalTimeSeriesLocationID = parameters.getValue ( "TotalTimeSeriesLocationID" );
		TotalTimeSeriesDataType = parameters.getValue ( "TotalTimeSeriesDataType" );
		TotalTimeSeriesAlias = parameters.getValue ( "TotalTimeSeriesAlias" );
		// Output Service Properties.
		EC2PropertiesTableID = parameters.getValue ( "EC2PropertiesTableID" );
		EBSSnapshotsTableID = parameters.getValue ( "EBSSnapshotsTableID" );
		EC2ImagesTableID = parameters.getValue ( "EC2ImagesTableID" );
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
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__TimeChunk_JComboBox, TimeChunk,JGUIUtil.NONE, null, null ) ) {
			__TimeChunk_JComboBox.select ( TimeChunk );
		}
		else {
            if ( (TimeChunk == null) || TimeChunk.equals("") ) {
				// New command...select the default.
            	if ( __TimeChunk_JComboBox.getItemCount() > 0 ) {
            		__TimeChunk_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"TimeChunk parameter \"" + TimeChunk + "\".  Select a value or Cancel." );
			}
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
		/*
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
		*/
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
		if ( FilterAvailabilityZones != null ) {
			__FilterAvailabilityZones_JTextField.setText ( FilterAvailabilityZones );
		}
		if ( FilterInstanceTypes != null ) {
			__FilterInstanceTypes_JTextField.setText ( FilterInstanceTypes );
		}
		if ( FilterRegions != null ) {
			__FilterRegions_JTextField.setText ( FilterRegions );
		}
		if ( FilterServices != null ) {
			__FilterServices_JTextField.setText ( FilterServices );
		}
		if ( FilterTags != null ) {
			__FilterTags_JTextField.setText ( FilterTags );
		}
		// Output Cost Tables.
        if ( GroupedTableID == null ) {
            // Select default.
            __GroupedTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __GroupedTableID_JComboBox,GroupedTableID, JGUIUtil.NONE, null, null ) ) {
                __GroupedTableID_JComboBox.select ( GroupedTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __GroupedTableID_JComboBox.getItemCount() == 0 ) {
                    __GroupedTableID_JComboBox.add(GroupedTableID);
                }
                else {
                    __GroupedTableID_JComboBox.insert(GroupedTableID, 0);
                }
                __GroupedTableID_JComboBox.select(0);
            }
        }
        if ( GroupedTableFile != null ) {
            __GroupedTableFile_JTextField.setText ( GroupedTableFile );
        }
		if ( GroupedTableRowCountProperty != null ) {
			__GroupedTableRowCountProperty_JTextField.setText ( GroupedTableRowCountProperty );
		}
        if ( TotalTableID == null ) {
            // Select default.
            __TotalTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TotalTableID_JComboBox,TotalTableID, JGUIUtil.NONE, null, null ) ) {
                __TotalTableID_JComboBox.select ( TotalTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __TotalTableID_JComboBox.getItemCount() == 0 ) {
                    __TotalTableID_JComboBox.add(TotalTableID);
                }
                else {
                    __TotalTableID_JComboBox.insert(TotalTableID, 0);
                }
                __TotalTableID_JComboBox.select(0);
            }
        }
        if ( TotalTableFile != null ) {
            __TotalTableFile_JTextField.setText ( TotalTableFile );
        }
		if ( TotalTableRowCountProperty != null ) {
			__TotalTableRowCountProperty_JTextField.setText ( TotalTableRowCountProperty );
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
		// Output Time Series.
        if ( CreateGroupedTimeSeries == null ) {
            // Select default.
            __CreateGroupedTimeSeries_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CreateGroupedTimeSeries_JComboBox,CreateGroupedTimeSeries, JGUIUtil.NONE, null, null ) ) {
                __CreateGroupedTimeSeries_JComboBox.select ( CreateGroupedTimeSeries );
            }
            else {
                // Creating new table so add in the first position.
                if ( __CreateGroupedTimeSeries_JComboBox.getItemCount() == 0 ) {
                    __CreateGroupedTimeSeries_JComboBox.add(CreateGroupedTimeSeries);
                }
                else {
                    __CreateGroupedTimeSeries_JComboBox.insert(CreateGroupedTimeSeries, 0);
                }
                __CreateGroupedTimeSeries_JComboBox.select(0);
            }
        }
        if ( GroupedTimeSeriesLocationID == null ) {
            // Select default.
            __GroupedTimeSeriesLocationID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __GroupedTimeSeriesLocationID_JComboBox,GroupedTimeSeriesLocationID, JGUIUtil.NONE, null, null ) ) {
                __GroupedTimeSeriesLocationID_JComboBox.select ( GroupedTimeSeriesLocationID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __GroupedTimeSeriesLocationID_JComboBox.getItemCount() == 0 ) {
                    __GroupedTimeSeriesLocationID_JComboBox.add(GroupedTimeSeriesLocationID);
                }
                else {
                    __GroupedTimeSeriesLocationID_JComboBox.insert(GroupedTimeSeriesLocationID, 0);
                }
                __GroupedTimeSeriesLocationID_JComboBox.select(0);
            }
        }
        if ( GroupedTimeSeriesDataType == null ) {
            // Select default.
            __GroupedTimeSeriesDataType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __GroupedTimeSeriesDataType_JComboBox,GroupedTimeSeriesDataType, JGUIUtil.NONE, null, null ) ) {
                __GroupedTimeSeriesDataType_JComboBox.select ( GroupedTimeSeriesDataType );
            }
            else {
                // Creating new table so add in the first position.
                if ( __GroupedTimeSeriesDataType_JComboBox.getItemCount() == 0 ) {
                    __GroupedTimeSeriesDataType_JComboBox.add(GroupedTimeSeriesDataType);
                }
                else {
                    __GroupedTimeSeriesDataType_JComboBox.insert(GroupedTimeSeriesDataType, 0);
                }
                __GroupedTimeSeriesDataType_JComboBox.select(0);
            }
        }
	    if ( GroupedTimeSeriesAlias != null ) {
		    __GroupedTimeSeriesAlias_JTextField.setText ( GroupedTimeSeriesAlias );
	    }
		if ( TimeSeriesMissingGroupBy != null ) {
			__TimeSeriesMissingGroupBy_JTextField.setText ( TimeSeriesMissingGroupBy );
		}
        if ( CreateTotalTimeSeries == null ) {
            // Select default.
            __CreateTotalTimeSeries_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CreateTotalTimeSeries_JComboBox,CreateTotalTimeSeries, JGUIUtil.NONE, null, null ) ) {
                __CreateTotalTimeSeries_JComboBox.select ( CreateTotalTimeSeries );
            }
            else {
                // Creating new table so add in the first position.
                if ( __CreateTotalTimeSeries_JComboBox.getItemCount() == 0 ) {
                    __CreateTotalTimeSeries_JComboBox.add(CreateTotalTimeSeries);
                }
                else {
                    __CreateTotalTimeSeries_JComboBox.insert(CreateTotalTimeSeries, 0);
                }
                __CreateTotalTimeSeries_JComboBox.select(0);
            }
        }
        if ( TotalTimeSeriesLocationID != null ) {
            __TotalTimeSeriesLocationID_JTextField.setText ( TotalTimeSeriesLocationID );
        }
        if ( TotalTimeSeriesDataType == null ) {
            // Select default.
            __TotalTimeSeriesDataType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TotalTimeSeriesDataType_JComboBox,TotalTimeSeriesDataType, JGUIUtil.NONE, null, null ) ) {
                __TotalTimeSeriesDataType_JComboBox.select ( TotalTimeSeriesDataType );
            }
            else {
                // Creating new table so add in the first position.
                if ( __TotalTimeSeriesDataType_JComboBox.getItemCount() == 0 ) {
                    __TotalTimeSeriesDataType_JComboBox.add(TotalTimeSeriesDataType);
                }
                else {
                    __TotalTimeSeriesDataType_JComboBox.insert(TotalTimeSeriesDataType, 0);
                }
                __TotalTimeSeriesDataType_JComboBox.select(0);
            }
        }
	    if ( TotalTimeSeriesAlias != null ) {
		    __TotalTimeSeriesAlias_JTextField.setText ( TotalTimeSeriesAlias );
	    }
		// Output Service Properties.
        if ( EC2PropertiesTableID == null ) {
            // Select default.
            __EC2PropertiesTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EC2PropertiesTableID_JComboBox,EC2PropertiesTableID, JGUIUtil.NONE, null, null ) ) {
                __EC2PropertiesTableID_JComboBox.select ( EC2PropertiesTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __EC2PropertiesTableID_JComboBox.getItemCount() == 0 ) {
                    __EC2PropertiesTableID_JComboBox.add(EC2PropertiesTableID);
                }
                else {
                    __EC2PropertiesTableID_JComboBox.insert(EC2PropertiesTableID, 0);
                }
                __EC2PropertiesTableID_JComboBox.select(0);
            }
        }
        if ( EBSSnapshotsTableID == null ) {
            // Select default.
            __EBSSnapshotsTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EBSSnapshotsTableID_JComboBox,EBSSnapshotsTableID, JGUIUtil.NONE, null, null ) ) {
                __EBSSnapshotsTableID_JComboBox.select ( EBSSnapshotsTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __EBSSnapshotsTableID_JComboBox.getItemCount() == 0 ) {
                    __EBSSnapshotsTableID_JComboBox.add(EBSSnapshotsTableID);
                }
                else {
                    __EBSSnapshotsTableID_JComboBox.insert(EBSSnapshotsTableID, 0);
                }
                __EBSSnapshotsTableID_JComboBox.select(0);
            }
        }
        if ( EC2ImagesTableID == null ) {
            // Select default.
            __EC2ImagesTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EC2ImagesTableID_JComboBox,EC2ImagesTableID, JGUIUtil.NONE, null, null ) ) {
                __EC2ImagesTableID_JComboBox.select ( EC2ImagesTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __EC2ImagesTableID_JComboBox.getItemCount() == 0 ) {
                    __EC2ImagesTableID_JComboBox.add(EC2ImagesTableID);
                }
                else {
                    __EC2ImagesTableID_JComboBox.insert(EC2ImagesTableID, 0);
                }
                __EC2ImagesTableID_JComboBox.select(0);
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
	// Cost Explorer Query.
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	TimeChunk = __TimeChunk_JComboBox.getSelected();
	Granularity = __Granularity_JComboBox.getSelected();
	GroupBy1 = __GroupBy1_JComboBox.getSelected();
	GroupByTag1 = __GroupByTag1_JTextField.getText().trim();
	GroupBy2 = __GroupBy2_JComboBox.getSelected();
	GroupByTag2 = __GroupByTag2_JTextField.getText().trim();
	//GroupBy3 = __GroupBy3_JComboBox.getSelected();
	//GroupByTag3 = __GroupByTag3_JTextField.getText().trim();
	Metric = __Metric_JComboBox.getSelected();
	// Cost Explorer Filter.
	FilterAvailabilityZones = __FilterAvailabilityZones_JTextField.getText().trim();
	FilterInstanceTypes = __FilterInstanceTypes_JTextField.getText().trim();
	FilterRegions = __FilterRegions_JTextField.getText().trim();
	FilterServices = __FilterServices_JTextField.getText().trim();
	FilterTags = __FilterTags_JTextField.getText().trim();
	// Output Cost Tables.
	GroupedTableID = __GroupedTableID_JComboBox.getSelected();
	GroupedTableFile = __GroupedTableFile_JTextField.getText().trim();
	GroupedTableRowCountProperty = __GroupedTableRowCountProperty_JTextField.getText().trim();
	TotalTableID = __TotalTableID_JComboBox.getSelected();
	TotalTableFile = __TotalTableFile_JTextField.getText().trim();
	TotalTableRowCountProperty = __TotalTableRowCountProperty_JTextField.getText().trim();
	AppendOutput = __AppendOutput_JComboBox.getSelected();
	// Output Time Series.
	CreateGroupedTimeSeries = __CreateGroupedTimeSeries_JComboBox.getSelected();
	GroupedTimeSeriesLocationID = __GroupedTimeSeriesLocationID_JComboBox.getSelected();
	GroupedTimeSeriesDataType = __GroupedTimeSeriesDataType_JComboBox.getSelected();
	GroupedTimeSeriesAlias = __GroupedTimeSeriesAlias_JTextField.getText().trim();
	TimeSeriesMissingGroupBy = __TimeSeriesMissingGroupBy_JTextField.getText().trim();
	CreateTotalTimeSeries = __CreateTotalTimeSeries_JComboBox.getSelected();
	TotalTimeSeriesLocationID = __TotalTimeSeriesLocationID_JTextField.getText().trim();
	TotalTimeSeriesDataType = __TotalTimeSeriesDataType_JComboBox.getSelected();
	TotalTimeSeriesAlias = __TotalTimeSeriesAlias_JTextField.getText().trim();
	// Output Service Properties.
	EC2PropertiesTableID = __EC2PropertiesTableID_JComboBox.getSelected();
	EBSSnapshotsTableID = __EBSSnapshotsTableID_JComboBox.getSelected();
	EC2ImagesTableID = __EC2ImagesTableID_JComboBox.getSelected();
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
    // General.
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "Profile=" + Profile );
	props.add ( "Region=" + Region );
	// Cost Explorer Query.
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "TimeChunk=" + TimeChunk );
	props.add ( "Granularity=" + Granularity );
	props.add ( "GroupBy1=" + GroupBy1 );
	props.add ( "GroupByTag1=" + GroupByTag1 );
	props.add ( "GroupBy2=" + GroupBy2 );
	props.add ( "GroupByTag2=" + GroupByTag2 );
	//props.add ( "GroupBy3=" + GroupBy3 );
	//props.add ( "GroupByTag3=" + GroupByTag3 );
	props.add ( "Metric=" + Metric );
	// Cost Explorer Filter.
	props.add ( "FilterAvailabilityZones=" + FilterAvailabilityZones );
	props.add ( "FilterInstanceTypes=" + FilterInstanceTypes );
	props.add ( "FilterRegions=" + FilterRegions );
	props.add ( "FilterServices=" + FilterServices );
	props.add ( "FilterTags=" + FilterTags );
	// Output Cost Tables.
	props.add ( "GroupedTableID=" + GroupedTableID );
	props.add ( "GroupedTableFile=" + GroupedTableFile );
	props.add ( "GroupedTableRowCountProperty=" + GroupedTableRowCountProperty );
	props.add ( "TotalTableID=" + TotalTableID );
	props.add ( "TotalTableFile=" + TotalTableFile );
	props.add ( "TotalTableRowCountProperty=" + TotalTableRowCountProperty );
	props.add ( "AppendOutput=" + AppendOutput );
	// Output Time Series.
	props.add ( "CreateGroupedTimeSeries=" + CreateGroupedTimeSeries );
	props.add ( "GroupedTimeSeriesLocationID=" + GroupedTimeSeriesLocationID );
	props.add ( "GroupedTimeSeriesDataType=" + GroupedTimeSeriesDataType );
	props.add ( "GroupedTimeSeriesAlias=" + GroupedTimeSeriesAlias );
	props.add ( "TimeSeriesMissingGroupBy=" + TimeSeriesMissingGroupBy );
	props.add ( "CreateTotalTimeSeries=" + CreateTotalTimeSeries );
	props.add ( "TotalTimeSeriesLocationID=" + TotalTimeSeriesLocationID );
	props.add ( "TotalTimeSeriesDataType=" + TotalTimeSeriesDataType );
	props.add ( "TotalTimeSeriesAlias=" + TotalTimeSeriesAlias );
	// Output Service Properties.
	props.add ( "EC2PropertiesTableID=" + EC2PropertiesTableID );
	props.add ( "EBSSnapshotsTableID=" + EBSSnapshotsTableID );
	props.add ( "EC2ImagesTableID=" + EC2ImagesTableID );
	//props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Set the default values as FYI.
	awsToolkit.uiPopulateProfileDefault(__ProfileDefault_JTextField, __ProfileDefaultNote_JLabel);
	awsToolkit.uiPopulateRegionDefault( __Profile_JComboBox.getSelected(), __RegionDefault_JTextField, __RegionDefaultNote_JLabel);
	// Check the path and determine what the label on the path button should be.
    if ( __pathGroupedFile_JButton != null ) {
		if ( (GroupedTableFile != null) && !GroupedTableFile.isEmpty() ) {
			__pathGroupedFile_JButton.setEnabled ( true );
			File f = new File ( GroupedTableFile );
			if ( f.isAbsolute() ) {
				__pathGroupedFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathGroupedFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathGroupedFile_JButton.setText ( __AddWorkingDirectory );
            	__pathGroupedFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathGroupedFile_JButton.setEnabled(false);
		}
    }
    if ( __pathTotalFile_JButton != null ) {
		if ( (TotalTableFile != null) && !TotalTableFile.isEmpty() ) {
			__pathTotalFile_JButton.setEnabled ( true );
			File f = new File ( TotalTableFile );
			if ( f.isAbsolute() ) {
				__pathTotalFile_JButton.setText ( __RemoveWorkingDirectory );
				__pathTotalFile_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathTotalFile_JButton.setText ( __AddWorkingDirectory );
            	__pathTotalFile_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathTotalFile_JButton.setEnabled(false);
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