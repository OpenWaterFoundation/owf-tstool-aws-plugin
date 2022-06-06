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

package org.openwaterfoundation.tstool.plugin.aws.commands.s3;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

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
{	String S3Command = parameters.getValue ( "S3Command" );
	String Profile = parameters.getValue ( "Profile" );
	String Region = parameters.getValue ( "Region" );
    String Bucket = parameters.getValue ( "Bucket" );
    String CopySourceKey = parameters.getValue ( "CopySourceKey" );
    String CopyDestKey = parameters.getValue ( "CopyDestKey" );
    String DeleteKey = parameters.getValue ( "DeleteKey" );
    String MaxKeys = parameters.getValue ( "MaxKeys" );
    String Prefix = parameters.getValue ( "Prefix" );
    String MaxObjects = parameters.getValue ( "MaxObjects" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	AwsS3CommandType s3Command = null;
	if ( (S3Command == null) || S3Command.isEmpty() ) {
		message = "The S3 command must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the S3 command."));
	}
	else {
		s3Command = AwsS3CommandType.valueOfIgnoreCase(S3Command);
		if ( s3Command == null ) {
			message = "The S3 command (" + S3Command + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a valid S3 command."));
		}
	}

	if ( (Profile == null) || Profile.isEmpty() ) {
		// Use the default profile.
		Profile = AwsToolkit.getInstance().getDefaultProfile();
	}

	if ( (Region == null) || Region.isEmpty() ) {
		message = "The region must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the region."));
	}
	if ( (MaxKeys != null) && !MaxKeys.isEmpty() ) {
		if ( !StringUtil.isInteger(MaxKeys) ) {
			message = "The maximum keys (" + MaxKeys + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify an integer 1 to 1000."));
		}
		else {
			int maxKeys = Integer.parseInt(MaxKeys);
			if ( (maxKeys < 0) || (maxKeys > 1000) ) {
				message = "The maximum keys (" + MaxKeys + ") is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an integer 1 to 1000."));
			}
		}
	}
	if ( (MaxObjects != null) && !MaxObjects.isEmpty() && !StringUtil.isInteger(MaxObjects) ) {
		message = "The maximum keys (" + MaxObjects + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an integer."));
	}
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

	// Additional checks specific to a command.
	if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
		if ( (Bucket == null) || Bucket.isEmpty() ) {
			message = "The bucket must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the bucket."));
		}
	}
	else if ( s3Command == AwsS3CommandType.COPY_OBJECT ) {
		if ( (CopySourceKey == null) || CopySourceKey.isEmpty() ) {
			message = "The source key must be specified for the copy.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the source key."));
		}
		if ( (CopyDestKey == null) || CopyDestKey.isEmpty() ) {
			message = "The destination key must be specified for the copy.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the destination key."));
		}
	}
	else if ( s3Command == AwsS3CommandType.DELETE_OBJECT ) {
		if ( (DeleteKey == null) || DeleteKey.isEmpty() ) {
			message = "The key must be specified for the delete.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the key to delete."));
		}
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(17);
	validList.add ( "S3Command" );
	validList.add ( "Profile" );
	validList.add ( "Region" );
	validList.add ( "Bucket" );
	validList.add ( "CopySourceKey" );
	validList.add ( "CopyDestKey" );
	validList.add ( "DeleteKey" );
	validList.add ( "DownloadDirectories" );
	validList.add ( "DownloadFiles" );
	validList.add ( "MaxKeys" );
	validList.add ( "Prefix" );
	validList.add ( "MaxObjects" );
	validList.add ( "UploadDirectories" );
	validList.add ( "UploadFiles" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
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
	String profile = Profile;
	if ( (Profile == null) || Profile.isEmpty() ) {
		// Get the default.
		profile = AwsToolkit.getInstance().getDefaultProfile();
	}
	String S3Command = parameters.getValue ( "S3Command" );
	AwsS3CommandType s3Command = AwsS3CommandType.valueOfIgnoreCase(S3Command);
	String region = parameters.getValue ( "Region" );
	region = TSCommandProcessorUtil.expandParameterValue(processor,this,region);
	// Bucket must be final because of lambda use below.
	String bucket0 = parameters.getValue ( "Bucket" );
	final String bucket = TSCommandProcessorUtil.expandParameterValue(processor,this,bucket0);
	String CopySourceKey = parameters.getValue ( "CopySourceKey" );
	CopySourceKey = TSCommandProcessorUtil.expandParameterValue(processor,this,CopySourceKey);
	String DeleteKey = parameters.getValue ( "DeleteKey" );
	DeleteKey = TSCommandProcessorUtil.expandParameterValue(processor,this,DeleteKey);
    String DownloadDirectories = parameters.getValue ( "DownloadDirectories" );
	DownloadDirectories = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadDirectories);
    Hashtable<String,String> downloadDirectories = new Hashtable<>();
    if ( (DownloadDirectories != null) && (DownloadDirectories.length() > 0) && (DownloadDirectories.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(DownloadDirectories, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            if ( parts.length == 2 ) {
            	downloadDirectories.put(parts[0].trim(), parts[1].trim() );
            }
            else {
            	downloadDirectories.put(parts[0].trim(), "" );
            }
        }
    }
    String DownloadFiles = parameters.getValue ( "DownloadFiles" );
	DownloadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadFiles);
    Hashtable<String,String> downloadFiles = new Hashtable<>();
    if ( (DownloadFiles != null) && (DownloadFiles.length() > 0) && (DownloadFiles.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(DownloadFiles, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            if ( parts.length == 2 ) {
            	downloadFiles.put(parts[0].trim(), parts[1].trim() );
            }
            else {
            	downloadFiles.put(parts[0].trim(), "" );
            }
        }
    }
	String CopyDestKey = parameters.getValue ( "CopyDestKey" );
	CopyDestKey = TSCommandProcessorUtil.expandParameterValue(processor,this,CopyDestKey);
	String MaxKeys = parameters.getValue ( "MaxKeys" );
	int maxKeys = -1; // Use default, which is 1000.
	if ( MaxKeys != null && !MaxKeys.isEmpty() ) {
		try {
			maxKeys = Integer.parseInt(MaxKeys);
		}
		catch ( NumberFormatException e ) {
			// Use default from above.
		}
	}
	String MaxObjects = parameters.getValue ( "MaxObjects" );
	int maxObjects = 2000;
	if ( MaxObjects != null && !MaxObjects.isEmpty() ) {
		try {
			maxObjects = Integer.parseInt(MaxObjects);
		}
		catch ( NumberFormatException e ) {
			// Use default from above.
		}
	}
	String Prefix = parameters.getValue ( "Prefix" );
	Prefix = TSCommandProcessorUtil.expandParameterValue(processor,this,Prefix);
    String UploadDirectories = parameters.getValue ( "UploadDirectories" );
	UploadDirectories = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadDirectories);
    Hashtable<String,String> uploadDirectories = new Hashtable<>();
    if ( (UploadDirectories != null) && (UploadDirectories.length() > 0) && (UploadDirectories.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(UploadDirectories, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            uploadDirectories.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String UploadFiles = parameters.getValue ( "UploadFiles" );
	UploadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadFiles);
    Hashtable<String,String> uploadFiles = new Hashtable<>();
    if ( (UploadFiles != null) && (UploadFiles.length() > 0) && (UploadFiles.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(UploadFiles, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            uploadFiles.put(parts[0].trim(), parts[1].trim() );
        }
    }
	boolean doTable = false;
	String OutputTableID = parameters.getValue ( "OutputTableID" );
	OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
	if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
		doTable = true;
	}
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
	
	ProfileCredentialsProvider credentialsProvider0 = null;
	credentialsProvider0 = ProfileCredentialsProvider.create(profile);
	ProfileCredentialsProvider credentialsProvider = credentialsProvider0;
	
	// The following is used to create a temporary table before outputting to a file.
	boolean useTempTable = false;

	Region regionO = Region.of(region);
	
	S3Client s3 = S3Client.builder()
		.region(regionO)
		.credentialsProvider(credentialsProvider)
		.build();
	
	// Process the files.  Each input file is opened to scan the file.
	// The output file is opened once in append mode.
    // TODO SAM 2014-02-03 Enable copying a list to a folder, etc. see AppendFile() for example.
	/*
	String OutputFile_full = IOUtil.verifyPathForOS(
	    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	        */
	try {
		// Create the table if it does not exist.

	    // Make sure the table has the columns that are needed.
	    if ( commandPhase == CommandPhaseType.RUN ) {
	    	boolean append = false;
    		// Column numbers are used later.
        	int bucketNameCol = -1;
        	int objectKeyCol = -1;
   	    	int objectSizeKbCol = -1;
   	    	int objectOwnerCol = -1;
   	    	int objectLastModifiedCol = -1;
	    	if ( doTable ) {
    	    	if ( (table == null) || !append ) {
    	        	// The table needs to be created:
    	    		// - the table columns depend on the S3 command being executed
    	        	List<TableField> columnList = new ArrayList<>();
    	        	if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        		columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1) );
    	        	}
    	        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        		columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Key", -1) );
    	        		columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "SizeKb", -1) );
    	        		columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Owner", -1) );
    	        		columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "LastModified", -1) );
    	        	}
    	        	if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
    	        		(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
    	        		table = new DataTable( columnList );
    	        	}
                	// Get the column numbers for later use.
    	        	if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        		bucketNameCol = table.getFieldIndex("BucketName");
    	        	}
    	        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        		objectKeyCol = table.getFieldIndex("Key");
    	        		objectSizeKbCol = table.getFieldIndex("SizeKb");
    	        		objectOwnerCol = table.getFieldIndex("Owner");
    	        		objectLastModifiedCol = table.getFieldIndex("LastModified");
    	        	}
    	        	if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
    	        		(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
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
    	    	}
    	    	else {
    	        	// Make sure that the needed columns exist - otherwise add them.
    	        	if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        		bucketNameCol = table.getFieldIndex("BucketName");
    	        		if ( bucketNameCol < 0 ) {
    	            		bucketNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1), "");
    	        		}
    	        	}
    	        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        		objectKeyCol = table.getFieldIndex("Key");
    	        		objectSizeKbCol = table.getFieldIndex("SizeKb");
    	        		objectOwnerCol = table.getFieldIndex("Owner");
    	        		objectLastModifiedCol = table.getFieldIndex("LastModified");
    	        		if ( objectKeyCol < 0 ) {
    	            		objectKeyCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Key", -1), "");
    	        		}
    	        		if ( objectSizeKbCol < 0 ) {
    	            		objectSizeKbCol = table.addField(new TableField(TableField.DATA_TYPE_LONG, "SizeKb", -1), "");
    	        		}
    	        		if ( objectOwnerCol < 0 ) {
    	            		objectOwnerCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Owner", -1), "");
    	        		}
    	        		if ( objectLastModifiedCol < 0 ) {
    	            		objectLastModifiedCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "LastModified", -1), "");
    	        		}
    	        	}
    	        }
    	    }

    	    // Call the service that was requested.
		
    	    if ( s3Command == AwsS3CommandType.COPY_OBJECT ) {
    	    	CopyObjectRequest request = CopyObjectRequest
    	    		.builder()
    	    		.sourceBucket(bucket)
    	    		.sourceKey(CopySourceKey)
    	    		.destinationBucket(bucket)
    	    		.destinationKey(CopyDestKey)
    	    		.build();
    	    	s3.copyObject(request);
    	    }
    	    else if ( s3Command == AwsS3CommandType.DELETE_OBJECT ) {
    	    	DeleteObjectRequest request = DeleteObjectRequest
    	    		.builder()
    	    		.bucket(bucket)
    	    		.key(DeleteKey)
    	    		.build();
    	    	s3.deleteObject(request);
    	    }
    	    else if ( s3Command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
    	    	// The following is from the S3TransferManager javadoc.
    	    	S3TransferManager tm = null;
    	    	if ( (downloadFiles.size() > 0) || (downloadDirectories.size() > 0) ) {
    	    		tm = S3TransferManager
    	    			.builder()
    	    			.s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
    	    				.region(regionO))
    	    			.build();
    	    	}
    	    	if ( downloadFiles.size() > 0 ) {
    	    		// Process each file in the list.
    	    		boolean error = false;
    	    		for ( Map.Entry<String,String> set : downloadFiles.entrySet() ) {
    	    			error = false;
    	    			try {
    	    				String downloadKey = set.getKey().trim();
    	    				String localFile = set.getValue();
    	    				if ( (localFile == null) || localFile.trim().isEmpty() ) {
    	    					// Use the same name as the key, but only the file name.
    	    					int pos = localFile.lastIndexOf("/");
    	    					if ( pos >= 0 ) {
    	    						if ( pos < (localFile.length() - 1) ) {
    	    							localFile = localFile.substring(pos + 1).trim();
    	    						}
    	    						else {
    	    							// Error because / at the end of the key.
    	    							message = "No local file given and key (" + set.getKey() + ") ends in / - cannot default file name to copy.";
    	    							Message.printWarning ( warning_level, 
    	    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    							status.addToLog(CommandPhaseType.RUN,
    	    								new CommandLogRecord(CommandStatusType.FAILURE,
    	    									message, "Fix the local file name."));
    	    							error = true;
    	    						}
    	    					}
    	    				}
    	    				if ( !error ) {
    	    					localFile = localFile.trim();
    	    					String localFileFull = IOUtil.verifyPathForOS(
    	    							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	           	    				TSCommandProcessorUtil.expandParameterValue(processor,this,localFile)));
    	    					/*
    	    					FileDownload download = tm.downloadFile(DownloadFileRequest
    	    						.builder()
    	    						// getObjectRequest
    	    						.key(downloadKey)
    								.destination(Paths.get(localFileFull))
    	    						.build());
    	    						*/
    	    					FileDownload download = tm
   	    							.downloadFile(d -> d.getObjectRequest(g -> g.bucket(bucket).key(downloadKey))
    									.destination(Paths.get(localFileFull)));
    	    					download.completionFuture().join();
    	    				}
    	    			}
    	    			catch ( Exception e ) {
    	    				message = "Error downloading S3 key \"" + set.getKey() + "\" to file \"" + set.getValue() + "\" (" + e + ")";
    	    				Message.printWarning ( warning_level, 
    	    					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    				Message.printWarning ( 3, routine, e );
    	    				status.addToLog(CommandPhaseType.RUN,
    	    					new CommandLogRecord(CommandStatusType.FAILURE,
    	    						message, "See the log file for details."));
    	    			}
    	    		}
    	    	}
    	    	if ( downloadDirectories.size() > 0 ) {
    	    		// Process each folder in the list.
    	    		boolean error = false;
    	    		for ( Map.Entry<String,String> set : downloadDirectories.entrySet() ) {
    	    			error = false;
    	    			try {
    	    				String downloadKey = set.getKey().trim();
    	    				String localFolder = set.getValue();
    	    				if ( (localFolder == null) || localFolder.trim().isEmpty() ) {
    	    					// Use the same name as the key, but only the folder name.
    	    					int pos = localFolder.lastIndexOf("/");
    	    					if ( pos >= 0 ) {
    	    						if ( pos < (localFolder.length() - 1) ) {
    	    							localFolder = localFolder.substring(pos + 1).trim();
    	    						}
    	    						else {
    	    							// Error because / at the end of the key.
    	    							message = "No local folder given and key (" + set.getKey() + ") ends in / - cannot default folder name to copy.";
    	    							Message.printWarning ( warning_level, 
    	    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    							status.addToLog(CommandPhaseType.RUN,
    	    								new CommandLogRecord(CommandStatusType.FAILURE,
    	    									message, "Fix the local folder name."));
    	    							error = true;
    	    						}
    	    					}
    	    				}
    	    				if ( !error ) {
    	    					localFolder = localFolder.trim();
    	    					String localFolderFull = IOUtil.verifyPathForOS(
    	    							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	           	    				TSCommandProcessorUtil.expandParameterValue(processor,this,localFolder)));
    	    					DirectoryDownload download = tm.downloadDirectory(DownloadDirectoryRequest.builder()
    									.destinationDirectory(Paths.get(localFolderFull))
    									.bucket(bucket)
    									.prefix(downloadKey)
    									.build());
    	    					// Wait for the transfer to complete.
    	    					CompletedDirectoryDownload completed = download.completionFuture().join();
    	    					// Log failed downloads, up to 50 messages.
    	    					int maxMessage = 50, count = 0;
    	    					for ( FailedFileDownload fail : completed.failedTransfers() ) {
    	    						++count;
    	    						if ( count > maxMessage ) {
    	    							// Limit messages.
    	    							message = "Only listing " + maxMessage + " download errors.";
   	    								Message.printWarning ( warning_level, 
   	    									MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    								status.addToLog(CommandPhaseType.RUN,
   	    									new CommandLogRecord(CommandStatusType.FAILURE,
   	    										message, "Check command parameters."));
    	    							break;
    	    						}
   	    							message = "Error downloading folder \"" + downloadKey + "\" to folder \"" + localFolderFull + "\"(" + fail + ").";
   	    							Message.printWarning ( warning_level, 
   	    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    							status.addToLog(CommandPhaseType.RUN,
   	    								new CommandLogRecord(CommandStatusType.FAILURE,
   	    									message, "Check command parameters."));
    	    					}
    	    				}
    	    			}
    	    			catch ( Exception e ) {
    	    				message = "Error downloading S3 key \"" + set.getKey() + "\" to folder \"" + set.getValue() + "\" (" + e + ")";
    	    				Message.printWarning ( warning_level, 
    	    					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    				Message.printWarning ( 3, routine, e );
    	    				status.addToLog(CommandPhaseType.RUN,
    	    					new CommandLogRecord(CommandStatusType.FAILURE,
    	    						message, "See the log file for details."));
    	    			}
    	    		}
    	    	}
    	    	if ( tm != null ) {
    	    		tm.close();
    	    	}
    	    }
    	    else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	    	ListBucketsRequest request = ListBucketsRequest
    	    		.builder()
    	    		.build();
    	    	ListBucketsResponse response = s3.listBuckets(request);
    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;
    	    	for ( Bucket bucketO : response.buckets() ) {
    	    		// Output to table and/or file, as requested.
    	    		if ( table != null ) {
    	    			if ( !allowDuplicates ) {
    	    				// Try to match the bucket name, which is the unique identifier.
    	    				rec = table.getRecord ( bucketNameCol, bucketO.name() );
    	    			}
    	    			if ( rec == null ) {
    	    				// Create a new record.
    	    				rec = table.addRecord(table.emptyRecord());
    	    			}
    	    			// Set the data in the record.
    	    			rec.setFieldValue(bucketNameCol,bucketO.name());
    	    		}
    	    	}
    	    }
   	        else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
   	        	software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = ListObjectsV2Request
    	    		.builder()
    	    		.fetchOwner(Boolean.TRUE)
    	    		.bucket(bucket);
    	    	if ( maxKeys > 0 ) {
    	    		// Set the maximum number of keys that will be returned.
    	    		builder.maxKeys(maxKeys);
    	    	}
    	    	if ( (Prefix != null) && !Prefix.isEmpty() ) {
    	    		// Set the key prefix to match.
    	    		builder.prefix(Prefix);
    	    	}
    	    	
    	    	ListObjectsV2Request request = builder.build();
    	    	ListObjectsV2Response response = null;
    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;
    	    	// TODO smalers 2022-05-31 for now use UTC time.
    	    	String timezone = "Z";
    	    	ZoneId zoneId = ZoneId.of("Z");
    	    	int behaviorFlag = 0;
    	    	boolean done = false;
    	    	int objectCount = 0;
    	    	while ( !done ) {
    	    		response = s3.listObjectsV2(request);
    	    		for ( S3Object s3Object : response.contents() ) {
    	    			// Check the maximum object count, to protect against runaway processes.
    	    			if ( objectCount >= maxObjects ) {
    	    				break;
    	    			}
    	    			// Output to table and/or file, as requested.
    	    			if ( table != null ) {
    	    				if ( !allowDuplicates ) {
    	    					// Try to match the object key, which is the unique identifier.
    	    					rec = table.getRecord ( objectKeyCol, s3Object.key() );
    	    				}
    	    				if ( rec == null ) {
    	    					// Create a new record.
    	    					rec = table.addRecord(table.emptyRecord());
    	    				}
    	    				// Set the data in the record.
    	    				rec.setFieldValue(objectKeyCol,s3Object.key());
    	    				rec.setFieldValue(objectSizeKbCol,s3Object.size());
    	    				if ( s3Object.owner() == null ) {
    	    					rec.setFieldValue(objectOwnerCol,"");
    	    				}
    	    				else {
    	    					rec.setFieldValue(objectOwnerCol,s3Object.owner().displayName());
    	    				}
    	    				rec.setFieldValue(objectLastModifiedCol,
    	    					new DateTime(OffsetDateTime.ofInstant(s3Object.lastModified(), zoneId), behaviorFlag, timezone));
    	    				// Increment the count of objects processed.
    	    				++objectCount;
    	    			}
    	    		}
    	    		if ( response.nextContinuationToken() == null ) {
    	    			done = true;
    	    		}
    	    		request = request.toBuilder()
   	    				.continuationToken(response.nextContinuationToken())
   	    				.build();
    	    	}
    	    }
   	        else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
    	    	// The following is from the S3TransferManager javadoc.
    	    	S3TransferManager tm = null;
    	    	if ( (uploadFiles.size() > 0) || (uploadDirectories.size() > 0) ) {
    	    		tm = S3TransferManager
    	    			.builder()
    	    			.s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
    	    				.region(regionO))
    	    			.build();
    	    	}
    	    	if ( uploadFiles.size() > 0 ) {
    	    		// Process each file in the list:
    	    		// - don't allow null or empty key or name
    	    		boolean error = false;
    	    		for ( Map.Entry<String,String> set : uploadFiles.entrySet() ) {
    	    			error = false;
    	    			try {
    	    				String localFile = set.getKey();
    	    				String uploadKey = set.getValue();
    	    				if ( (localFile == null) || localFile.trim().isEmpty() ) {
    	    					// Don't allow default because could cause problems clobbering S3 files.
    	    					message = "No local file given - cannot upload file.";
    	    					Message.printWarning ( warning_level, 
    	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    					status.addToLog(CommandPhaseType.RUN,
    	    						new CommandLogRecord(CommandStatusType.FAILURE,
    	    							message, "Fix the local file name."));
    	    					error = true;
    	    				}
    	    				if ( (uploadKey == null) || uploadKey.trim().isEmpty() ) {
    	    					// Don't allow default because could cause problems clobbering S3 files.
    	    					message = "No S3 key given - cannot upload file \"" + localFile + "\"";
    	    					Message.printWarning ( warning_level, 
    	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    					status.addToLog(CommandPhaseType.RUN,
    	    						new CommandLogRecord(CommandStatusType.FAILURE,
    	    							message, "Fix the key name."));
    	    					error = true;
    	    				}
    	    				if ( !error ) {
    	    					localFile = localFile.trim();
    	    					uploadKey = uploadKey.trim();
    	    					final String uploadKeyFinal = uploadKey;
    	    					String localFileFull = IOUtil.verifyPathForOS(
    	    							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	           	    				TSCommandProcessorUtil.expandParameterValue(processor,this,localFile)));
    	    					FileUpload upload = tm
   	    							.uploadFile(d -> d.putObjectRequest(g -> g.bucket(bucket).key(uploadKeyFinal))
    									.source(Paths.get(localFileFull)));
    	    					upload.completionFuture().join();
    	    				}
    	    			}
    	    			catch ( Exception e ) {
    	    				message = "Error downloading S3 key \"" + set.getKey() + "\" to file \"" + set.getValue() + "\" (" + e + ")";
    	    				Message.printWarning ( warning_level, 
    	    					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    				Message.printWarning ( 3, routine, e );
    	    				status.addToLog(CommandPhaseType.RUN,
    	    					new CommandLogRecord(CommandStatusType.FAILURE,
    	    						message, "See the log file for details."));
    	    			}
    	    		}
    	    	}
    	    	if ( uploadDirectories.size() > 0 ) {
    	    		// Process each folder in the list.
    	    		boolean error = false;
    	    		for ( Map.Entry<String,String> set : uploadDirectories.entrySet() ) {
    	    			error = false;
    	    			try {
    	    				String localFolder = set.getKey();
    	    				String uploadKey = set.getValue();
    	    				if ( (localFolder == null) || localFolder.trim().isEmpty() ) {
    	    					// Don't allow default because could cause problems clobbering S3 files.
    	    					message = "No local folder given - cannot upload folder.";
    	    					Message.printWarning ( warning_level, 
    	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    					status.addToLog(CommandPhaseType.RUN,
    	    						new CommandLogRecord(CommandStatusType.FAILURE,
    	    							message, "Fix the local folder name."));
    	    					error = true;
    	    				}
    	    				if ( (uploadKey == null) || uploadKey.trim().isEmpty() ) {
    	    					// Don't allow default because could cause problems clobbering S3 files.
    	    					message = "No S3 key given - cannot upload folder \"" + localFolder + "\"";
    	    					Message.printWarning ( warning_level, 
    	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    					status.addToLog(CommandPhaseType.RUN,
    	    						new CommandLogRecord(CommandStatusType.FAILURE,
    	    							message, "Fix the key name."));
    	    					error = true;
    	    				}
    	    				if ( !error ) {
    	    					String localFolderFull = IOUtil.verifyPathForOS(
    	    							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	           	    				TSCommandProcessorUtil.expandParameterValue(processor,this,localFolder)));
    	    					DirectoryUpload upload = tm.uploadDirectory(UploadDirectoryRequest.builder()
    									.sourceDirectory(Paths.get(localFolderFull))
    									.bucket(bucket)
    									.prefix(uploadKey)
    									.build());
    	    					// Wait for the transfer to complete.
    	    					CompletedDirectoryUpload completed = upload.completionFuture().join();
    	    					// Log failed uploads, up to 50 messages.
    	    					int maxMessage = 50, count = 0;
    	    					for ( FailedFileUpload fail : completed.failedTransfers() ) {
    	    						++count;
    	    						if ( count > maxMessage ) {
    	    							// Limit messages.
    	    							message = "Only listing " + maxMessage + " upload errors.";
   	    								Message.printWarning ( warning_level, 
   	    									MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    								status.addToLog(CommandPhaseType.RUN,
   	    									new CommandLogRecord(CommandStatusType.FAILURE,
   	    										message, "Check command parameters."));
    	    							break;
    	    						}
   	    							message = "Error uploading folder \"" + localFolderFull + "\" to key \"" + uploadKey + "\"(" + fail + ").";
   	    							Message.printWarning ( warning_level, 
   	    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    							status.addToLog(CommandPhaseType.RUN,
   	    								new CommandLogRecord(CommandStatusType.FAILURE,
   	    									message, "Check command parameters."));
    	    					}
    	    				}
    	    			}
    	    			catch ( Exception e ) {
    	    				message = "Error uploading folder \"" + set.getKey() + "\" to S3 key \"" + set.getValue() + "\" (" + e + ")";
    	    				Message.printWarning ( warning_level, 
    	    					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    				Message.printWarning ( 3, routine, e );
    	    				status.addToLog(CommandPhaseType.RUN,
    	    					new CommandLogRecord(CommandStatusType.FAILURE,
    	    						message, "See the log file for details."));
    	    			}
    	    		}
    	    	}
    	    	if ( tm != null ) {
    	    		tm.close();
    	    	}
    	    }
	    }
	    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	        if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
   	        	(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
   	        	if ( table == null ) {
	               	// Did not find table so is being created in this command.
	               	// Create an empty table and set the ID.
	               	table = new DataTable();
	               	table.setTableID ( OutputTableID );
	           	}
	           	setDiscoveryTable ( table );
   	        }
	    }
	}
    catch ( S3Exception e ) {
  	    if ( s3Command == AwsS3CommandType.COPY_OBJECT ) {
			message = "Unexpected error copying object (" + e.awsErrorDetails().errorMessage() + ").";
		}
  	    else if ( s3Command == AwsS3CommandType.DELETE_OBJECT ) {
			message = "Unexpected error deleting object (" + e.awsErrorDetails().errorMessage() + ").";
		}
  	    else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
			message = "Unexpected error listing buckets (" + e.awsErrorDetails().errorMessage() + ").";
		}
        else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
			message = "Unexpected error listing bucket objects (" + e.awsErrorDetails().errorMessage() + ").";
        }
		else {
			message = "Unexpected error for unknown S3 command (" + e.awsErrorDetails().errorMessage() + ": " + S3Command;
		}
		Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
		throw new CommandException ( message );
    }
    catch ( Exception e ) {
  	    if ( s3Command == AwsS3CommandType.COPY_OBJECT ) {
			message = "Unexpected error copying object (" + e + ").";
		}
  	    else if ( s3Command == AwsS3CommandType.DELETE_OBJECT ) {
			message = "Unexpected error deleting object (" + e + ").";
		}
  	    else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
			message = "Unexpected error listing buckets (" + e + ").";
		}
        else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
			message = "Unexpected error listing bucket objects (" + e + ").";
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
	String S3Command = parameters.getValue("S3Command");
	String Profile = parameters.getValue("Profile");
	String Region = parameters.getValue("Region");
	String Bucket = parameters.getValue("Bucket");
	String CopySourceKey = parameters.getValue("CopySourceKey");
	String CopyDestKey = parameters.getValue("CopyDestKey");
	String DeleteKey = parameters.getValue("DeleteKey");
	String DownloadDirectories = parameters.getValue("DownloadDirectories");
	String DownloadFiles = parameters.getValue("DownloadFiles");
	String MaxKeys = parameters.getValue("MaxKeys");
	String Prefix = parameters.getValue("Prefix");
	String MaxObjects = parameters.getValue("MaxObjects");
	String UploadDirectories = parameters.getValue("UploadDirectories");
	String UploadFiles = parameters.getValue("UploadFiles");
	String OutputTableID = parameters.getValue("OutputTableID");
	String OutputFile = parameters.getValue("OutputFile");
	String IfInputNotFound = parameters.getValue("IfInputNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (S3Command != null) && (S3Command.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "S3Command=\"" + S3Command + "\"" );
	}
	if ( (Profile != null) && (Profile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Profile=\"" + Profile + "\"" );
	}
	if ( (Region != null) && (Region.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Region=\"" + Region + "\"" );
	}
	if ( (Bucket != null) && (Bucket.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Bucket=\"" + Bucket + "\"" );
	}
	if ( (CopySourceKey != null) && (CopySourceKey.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "CopySourceKey=\"" + CopySourceKey + "\"");
	}
	if ( (CopyDestKey != null) && (CopyDestKey.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "CopyDestKey=\"" + CopyDestKey + "\"");
	}
	if ( (DeleteKey != null) && (DeleteKey.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "DeleteKey=\"" + DeleteKey + "\"");
	}
	if ( (DownloadDirectories != null) && (DownloadDirectories.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "DownloadDirectories=\"" + DownloadDirectories + "\"");
	}
	if ( (DownloadFiles != null) && (DownloadFiles.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "DownloadFiles=\"" + DownloadFiles + "\"");
	}
	if ( (MaxKeys != null) && (MaxKeys.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "MaxKeys=" + MaxKeys );
	}
	if ( (Prefix != null) && (Prefix.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Prefix=" + Prefix );
	}
	if ( (MaxObjects != null) && (MaxObjects.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "MaxObjects=" + MaxObjects );
	}
	if ( (UploadDirectories != null) && (UploadDirectories.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "UploadDirectories=\"" + UploadDirectories + "\"");
	}
	if ( (UploadFiles != null) && (UploadFiles.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "UploadFiles=\"" + UploadFiles + "\"");
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