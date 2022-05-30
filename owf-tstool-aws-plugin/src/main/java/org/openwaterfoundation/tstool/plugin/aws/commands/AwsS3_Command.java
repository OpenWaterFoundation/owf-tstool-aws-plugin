// AwsS3_Command - This class initializes, checks, and runs the Aws_S3() command.

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

package org.openwaterfoundation.tstool.plugin.aws.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the AwsS3() command.
*/
public class AwsS3_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
 * Values for 'S3Command' parameter.
 */
protected final String ListBuckets = "ListBuckets";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
The output table that is created for discovery mode.
*/
private DataTable discoveryOutputTable = null;

/**
Constructor.
*/
public AwsS3_Command ()
{	super();
	setCommandName ( "AwsS3" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String Region = parameters.getValue ( "Region" );
	String InputFile = parameters.getValue ( "InputFile" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (Region == null) || (Region.length() == 0) ) {
		message = "The regioin must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the region."));
	}
	/*
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
	}
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output file."));
    }
    */
	if ( (IfInputNotFound != null) && !IfInputNotFound.equals("") ) {
		if ( !IfInputNotFound.equalsIgnoreCase(_Ignore) && !IfInputNotFound.equalsIgnoreCase(_Warn)
		    && !IfInputNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfInputNotFound parameter \"" + IfInputNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(7);
	validList.add ( "Profile" );
	validList.add ( "S3Command" );
	validList.add ( "Region" );
	validList.add ( "InputFile" );
	validList.add ( "OutputFile" );
	validList.add ( "OutputTableID" );
	validList.add ( "IfInputNotFound" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new AwsS3_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return this.discoveryOutputTable;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
    List<File> list = new ArrayList<>();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
The following classes can be requested:  DataTable
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile () {
    return __OutputFile_File;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
	
    // Clear the output file.
    setOutputFile ( null );
	
	String Profile = parameters.getValue ( "Profile" );
	String S3Command = parameters.getValue ( "S3Command" );
	String Region_Parameter = parameters.getValue ( "Region" );
	Region_Parameter = TSCommandProcessorUtil.expandParameterValue(processor,this,Region_Parameter);
	String InputFile = parameters.getValue ( "InputFile" ); // Expand below.
	String OutputTableID = parameters.getValue ( "OutputTableID" );
	OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expand below.
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    IfInputNotFound = _Warn; // Default
	}

	// Get the table to process (may be null if need to create below).

	DataTable table = null;
	PropList request_params = null;
	CommandProcessorRequestResultsBean bean = null;
	if ( (OutputTableID != null) && !OutputTableID.equals("") ) {
		// Get the table to be updated/created.
		request_params = new PropList ( "" );
		request_params.set ( "TableID", OutputTableID );
		try {
			bean = processor.processRequest( "GetTable", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object o_Table = bean_PropList.getContents ( "Table" );
			if ( o_Table != null ) {
				// Found the table so no need to create it.
				table = (DataTable)o_Table;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting GetTable(TableID=\"" + OutputTableID + "\") from processor (" + e + ").";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Report problem to software support." ) );
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Handle credentials.
	
	ProfileCredentialsProvider credentialsProvider = null;
	if ( (Profile == null) || Profile.isEmpty() ) {
		credentialsProvider = ProfileCredentialsProvider.create();
	}
	else {
		credentialsProvider = ProfileCredentialsProvider.create(Profile);
	}
	Region region = Region.of(Region_Parameter);
	
	S3Client s3 = S3Client.builder()
		.region(region)
		.credentialsProvider(credentialsProvider)
		.build();
	
	// Process the files.  Each input file is opened to scan the file.
	// The output file is opened once in append mode.
    // TODO SAM 2014-02-03 Enable copying a list to a folder, etc. see AppendFile() for example.
	/*
    String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
	String OutputFile_full = IOUtil.verifyPathForOS(
	    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	        */
	try {
		// Create the table if it does not exist.

	    // Make sure the table has the columns that are needed.
		boolean append = false;
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        int bucketNameCol = -1;
    	    if ( (table == null) || !append ) {
    	        // The table needs to be created.
    	        List<TableField> columnList = new ArrayList<>();
    	        columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1) );
                table = new DataTable( columnList );
                bucketNameCol = table.getFieldIndex("BucketName");
                table.setTableID ( OutputTableID );
                Message.printStatus(2, routine, "Was not able to match existing table \"" + OutputTableID + "\" so created new table.");
                // Set the table in the processor.
                request_params = new PropList ( "" );
                request_params.setUsingObject ( "Table", table );
                try {
                    processor.processRequest( "SetTable", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetTable(Table=...) from processor.";
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
                }
    	    }
    	    else {
    	        // Make sure that the needed columns exist - otherwise add them.
    	        bucketNameCol = table.getFieldIndex("BucketName");
    	        if ( bucketNameCol < 0 ) {
    	            bucketNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1), "");
    	        }
    	    }

    	    // Call the service that was requested.
		
    	    if ( S3Command.equalsIgnoreCase(this.ListBuckets) ) {
    	    	ListBucketsRequest request = ListBucketsRequest.builder().build();
    	    	ListBucketsResponse response = s3.listBuckets(request);
    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;
    	    	for ( Bucket bucket : response.buckets() ) {
    	    		// Output to table and/or file, as requested.
    	    		if ( table != null ) {
    	    			if ( !allowDuplicates ) {
    	    				// Try to match the TSID.
    	    				rec = table.getRecord ( bucketNameCol, bucket.name() );
    	    			}
    	    			if ( rec == null ) {
    	    				// Create a new record.
    	    				rec = table.addRecord(table.emptyRecord());
    	    			}
    	    			// Set the data in the record.
    	    			rec.setFieldValue(bucketNameCol,bucket.name());
    	    		}
    	    	}
    	    }
		
    	    /*
	    	File in = new File(InputFile_full);
	    	if ( in.exists() ) {
	        	IOUtil.copyFile(new File(InputFile_full), new File(OutputFile_full) );
	        	// Save the output file name...
	        	setOutputFile ( new File(OutputFile_full));
	    	}
	    	else {
	        	// Input file does not exist so generate a warning
	        	message = "Input file does not exist for InputFile=\"" + InputFile + "\"";
	        	if ( IfInputNotFound.equalsIgnoreCase(_Fail) ) {
	            	Message.printWarning ( warning_level,
	                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Verify that the input file exists at the time the command is run."));
	        	}
	        	else if ( IfInputNotFound.equalsIgnoreCase(_Warn) ) {
	            	Message.printWarning ( warning_level,
	                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                	message, "Verify that the input file exists at the time the command is run."));
	        	}
	    	}
    	     */
	    }
	    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	        if ( table == null ) {
	            // Did not find table so is being created in this command.
	            // Create an empty table and set the ID.
	            table = new DataTable();
	            table.setTableID ( OutputTableID );
	        }
	        setDiscoveryTable ( table );
	    }
	}
    catch ( Exception e ) {
		if ( S3Command.equalsIgnoreCase(this.ListBuckets) ) {
			message = "Unexpected error listing buckets (" + e + ").";
		}
		else {
			message = "Unexpected error for unknown S3 command: " + S3Command;
		}
		Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
		throw new CommandException ( message );
	}
	
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file ) {
    __OutputFile_File = file;
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table ) {
    this.discoveryOutputTable = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String Profile = parameters.getValue("Profile");
	String S3Command = parameters.getValue("S3Command");
	String Region = parameters.getValue("Region");
	String InputFile = parameters.getValue("InputFile");
	String OutputTableID = parameters.getValue("OutputTableID");
	String OutputFile = parameters.getValue("OutputFile");
	String IfInputNotFound = parameters.getValue("IfInputNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (Profile != null) && (Profile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Profile=\"" + Profile + "\"" );
	}
	if ( (S3Command != null) && (S3Command.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "S3Command=\"" + S3Command + "\"" );
	}
	if ( (Region != null) && (Region.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Region=\"" + Region + "\"" );
	}
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"");
    }
    if ( (OutputTableID != null) && (OutputTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableID=\"" + OutputTableID + "\"" );
    }
	if ( (IfInputNotFound != null) && (IfInputNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfInputNotFound=" + IfInputNotFound );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}