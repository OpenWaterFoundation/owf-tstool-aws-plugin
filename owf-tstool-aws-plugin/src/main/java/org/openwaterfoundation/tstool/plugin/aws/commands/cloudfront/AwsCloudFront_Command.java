// AwsCloudFront_Command - This class initializes, checks, and runs the Aws_CloudFront() command.

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

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.InvalidationSummary;
import software.amazon.awssdk.services.cloudfront.model.Paths;
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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the AwsCloudFront() command.
*/
public class AwsCloudFront_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{

/**
Values for WaitForCompletion.
*/
protected final String _False = "False";
protected final String _True = "True";

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
public AwsCloudFront_Command () {
	super();
	setCommandName ( "AwsCloudFront" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String CloudFrontCommand = parameters.getValue ( "CloudFrontCommand" );
	String Profile = parameters.getValue ( "Profile" );
	String Region = parameters.getValue ( "Region" );
    String DistributionId = parameters.getValue ( "DistributionId" );
    String Comment = parameters.getValue ( "Comment" );
    String InvalidationPaths = parameters.getValue ( "InvalidationPaths" );
    String WaitForCompletion = parameters.getValue ( "WaitForCompletion" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputTableID = parameters.getValue ( "OutputTableID" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	AwsCloudFrontCommandType cloudfrontCommand = null;
	if ( (CloudFrontCommand == null) || CloudFrontCommand.isEmpty() ) {
		message = "The CloudFront command must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the CloudFront command."));
	}
	else {
		cloudfrontCommand = AwsCloudFrontCommandType.valueOfIgnoreCase(CloudFrontCommand);
		if ( cloudfrontCommand == null ) {
			message = "The CloudFront command (" + CloudFrontCommand + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a valid CloudFront command."));
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
	
	// Make sure that only one of the distribution ID or comment is specified:
	// - listing does not require the distribution
	if ( cloudfrontCommand == AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION ) {
		if ( ((DistributionId == null) || DistributionId.isEmpty()) &&
			((Comment == null) || Comment.isEmpty()) ) {
			message = "The distribution ID or comment must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the distribution ID or comment."));
		}
		if ( ((DistributionId != null) && !DistributionId.isEmpty()) &&
			((Comment != null) && !Comment.isEmpty()) ) {
			message = "The distribution ID or comment must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the distribution ID or comment."));
		}
	}

	if ( (InvalidationPaths != null) && !InvalidationPaths.isEmpty() ) {
		// Make sure that invalidation paths start with /.
		List<String> invalidationPathsList = null;
		if ( InvalidationPaths.indexOf(",") > 0 ) {
			// Split the paths.
			invalidationPathsList = StringUtil.breakStringList(InvalidationPaths, ",", StringUtil.DELIM_SKIP_BLANKS);
		}
		else {
			// Use as is.
			invalidationPathsList = new ArrayList<>();
			invalidationPathsList.add(InvalidationPaths.trim());
		}
		for ( String path : invalidationPathsList ) {
			path = path.trim();
			if ( !path.startsWith("/") ) {
				message = "The invalidation path (" + path + ") does not start with /.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the invalidation path starting with /."));
			}
		}
	}

	if ( (WaitForCompletion != null) && (WaitForCompletion.length() != 0) &&
        !WaitForCompletion.equalsIgnoreCase(_False) && !WaitForCompletion.equalsIgnoreCase(_True)) {
		message = "The value for WaitForCompletion (" + WaitForCompletion + ") is invalid.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify blank, " + _False + ", or " + _True + " (default)." ) );
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

	if ( (cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS) ||
		(cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS) ) {
		// Make sure that an output table or file is specified for output:
		// - warn since not fatal, but not much use if no output
		if ( ((OutputTableID == null) || OutputTableID.isEmpty()) &&
			((OutputFile == null) || OutputFile.isEmpty()) ) {
			message = "The output table ID and/or file must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
					message, "Specify the output table ID or file."));
		}
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(11);
	validList.add ( "CloudFrontCommand" );
	validList.add ( "Profile" );
	validList.add ( "Region" );
	validList.add ( "DistributionId" );
	validList.add ( "Comment" );
	validList.add ( "InvalidationPaths" );
	validList.add ( "CallerReference" );
	validList.add ( "WaitForCompletion" );
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
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new AwsCloudFront_JDialog ( parent, this, tableIDChoices )).ok();
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
public <T> List<T> getObjectList ( Class<T> c ) {
    DataTable table = getDiscoveryTable();
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
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
		if ( (profile == null) || profile.isEmpty() ) {
			message = "The profile is not specified and unable to determine the default.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Make sure that the AWS configuration file exists with at least one profile: " +
					AwsToolkit.getInstance().getAwsUserConfigFile() ) );
		}
	}
	String CloudFrontCommand = parameters.getValue ( "CloudFrontCommand" );
	AwsCloudFrontCommandType cloudfrontCommand = AwsCloudFrontCommandType.valueOfIgnoreCase(CloudFrontCommand);
	String region = parameters.getValue ( "Region" );
	region = TSCommandProcessorUtil.expandParameterValue(processor,this,region);
	if ( (region == null) || region.isEmpty() ) {
		// Get the default region.
		region = AwsToolkit.getInstance().getDefaultRegion(profile);
		if ( (region == null) || region.isEmpty() ) {
			message = "The region is not specified and unable to determine the default.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Make sure that the AWS configuration file exists with default region: " +
					AwsToolkit.getInstance().getAwsUserConfigFile() ) );
		}
	}
	String DistributionID = parameters.getValue ( "DistributionID" );
	DistributionID = TSCommandProcessorUtil.expandParameterValue(processor,this,DistributionID);
	String Comment = parameters.getValue ( "Comment" );
	Comment = TSCommandProcessorUtil.expandParameterValue(processor,this,Comment);
	// Convert the comment to a Java pattern.
	String commentPattern = null;
	if ( (Comment != null) && !Comment.isEmpty() ) {
		commentPattern = Comment.replace("*", ".*");
	}
    String InvalidationPaths = parameters.getValue ( "InvalidationPaths" );
	InvalidationPaths = TSCommandProcessorUtil.expandParameterValue(processor,this,InvalidationPaths);
	// Break the list of paths into a list.
	List<String> invalidationPathsList = new ArrayList<>();
	if ( (InvalidationPaths != null) && !InvalidationPaths.trim().isEmpty() ) {
		if ( InvalidationPaths.indexOf(",") > 0 ) {
			// Split the paths.
			invalidationPathsList = StringUtil.breakStringList(InvalidationPaths, ",", StringUtil.DELIM_SKIP_BLANKS);
		}
		else {
			// Use as is.
			invalidationPathsList.add(InvalidationPaths.trim());
		}
	}
	String CallerReference = parameters.getValue ( "CallerReference" );
	CallerReference = TSCommandProcessorUtil.expandParameterValue(processor,this,CallerReference);
	String callerReference = CallerReference;
	if ( (CallerReference == null) || CallerReference.isEmpty() ) {
		// Default is user-time.
		DateTime dt = new DateTime ( DateTime.DATE_CURRENT);
		callerReference = System.getProperty("user.name") + "-" + TimeUtil.formatDateTime(dt, "%Y-%m-%dT%H:%M:%S");
	}
	else {
		// Append current time to ensure uniqueness.
		DateTime dt = new DateTime ( DateTime.DATE_CURRENT);
		callerReference = CallerReference + "-" + TimeUtil.formatDateTime(dt, "%Y-%m-%dT%H:%M:%S");
	}
	String WaitForCompletion = parameters.getValue ( "WaitForCompletion" );
	boolean waitForCompletion = true;
	if ( (WaitForCompletion != null) && WaitForCompletion.equalsIgnoreCase("false") ) {
		waitForCompletion = false;
	}
	String OutputTableID = parameters.getValue ( "OutputTableID" );
	OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
	boolean doTable = false;
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
	
	CloudFrontClient cloudfront = CloudFrontClient.builder()
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
		boolean append = false;
	    if ( commandPhase == CommandPhaseType.RUN ) {
	    	// Create a session with the credentials.
	    	AwsSession awsSession = new AwsSession(profile);
	    	// Column numbers are used later:
	    	// - shared, then distributions, then invalidations
	        int idCol = -1;

	        int arnCol = -1;
    	    int commentCol = -1;
    	    int domainNameCol = -1;
    	    int enabledCol = -1;

    	    int statusCol = -1;
    	    int createTimeCol = -1;
	    	if ( doTable ) {
	    		if ( (table == null) || !append ) {
	    			// The table needs to be created:
	    			// - the table columns depend on the S3 command being executed
	    			List<TableField> columnList = new ArrayList<>();
	    			if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ID", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ARN", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Comment", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "DomainName", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_BOOLEAN, "Enabled", -1) );
	    			}
	    			else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS ) {
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ID", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Status", -1) );
	    				columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "CreateTime", -1) );
	    			}
	    			if ( (cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS) ||
	    				(cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS) ) {
	    				table = new DataTable( columnList );
	    			}
	    			// Get the column numbers for later use.
	    			if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
	    				idCol = table.getFieldIndex("ID");
	    				arnCol = table.getFieldIndex("ARN");
	    				commentCol = table.getFieldIndex("Comment");
	    				domainNameCol = table.getFieldIndex("DomainName");
	    				enabledCol = table.getFieldIndex("Enabled");
	    			}
	    			else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS ) {
	    				idCol = table.getFieldIndex("ID");
	    				statusCol = table.getFieldIndex("Status");
	    				createTimeCol = table.getFieldIndex("CreateTime");
	    			}
	    			if ( (cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS) ||
	    				(cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS) ) {
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
	    			if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
	    				idCol = table.getFieldIndex("ID");
	    				arnCol = table.getFieldIndex("ARN");
	    				commentCol = table.getFieldIndex("Comment");
	    				domainNameCol = table.getFieldIndex("DomainName");
	    				enabledCol = table.getFieldIndex("Enabled");
	    			}
	    			else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS ) {
	    				idCol = table.getFieldIndex("ID");
	    				statusCol = table.getFieldIndex("Status");
	    				createTimeCol = table.getFieldIndex("CreateTime");
	    			}
	    		}
    	    }

    	    // Call the service that was requested.
		
   	        if ( cloudfrontCommand == AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION ) {
   	        	// Invalidate files in a distribution using one or more paths:
   	        	// - see: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html
   	        	// List the distributions given the input parameters.
   	        	String distributionId = AwsToolkit.getInstance().getCloudFrontDistributionId(awsSession, region, DistributionID, commentPattern);
   	        	boolean doInvalidate = true;
   	        	if ( distributionId == null ) {
	    			message = "Unable to determine CloudFront distribution ID for invalidation.";
	    			Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	    			status.addToLog ( commandPhase,
	    				new CommandLogRecord(CommandStatusType.FAILURE,
	    					message, "Verify that the distribution ID is valid for the region." ) );
	    			doInvalidate = false;
   	        	}
   	        	if ( invalidationPathsList.size() == 0 ) {
	    			message = "No paths have been specified to invalidate.";
	    			Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	    			status.addToLog ( commandPhase,
	    				new CommandLogRecord(CommandStatusType.FAILURE,
	    					message, "Specify invalidation paths." ) );
	    			doInvalidate = false;
   	        	}
   	        	if ( doInvalidate) {
   	        		// Invalidate:
   	        		// - see:  https://stackoverflow.com/questions/28527188/how-to-invalidate-a-fileto-be-refreshed-served-from-cloudfront-cdn-via-java-aw
   	        		// - exception will be caught below
   	        		for ( String path : invalidationPathsList ) {
   	        			Message.printStatus(2, routine, "Invalidating path \"" + path + "\".");
   	        		}
   	        		Paths invalidationPaths = Paths
   	        			.builder()
   	        			.items(invalidationPathsList)
   	        			.quantity(invalidationPathsList.size())
   	        			.build();
   	        		InvalidationBatch batch = InvalidationBatch
   	        			.builder()
   	        			.paths(invalidationPaths)
   	        			.callerReference(callerReference)
   	        			.build();
   	        		CreateInvalidationRequest request = CreateInvalidationRequest
   	        			.builder()
   	        			.distributionId(distributionId)
   	        			.invalidationBatch(batch)
   	        			.build();
   	        		//CreateInvalidationResponse response =
   	        		cloudfront.createInvalidation(request);
   	        	}
   	        	// If wait for completion is true, wait until the invalidation is complete:
   	        	// - wait up to 3600 seconds (720 x 5 seconds)
   	        	int waitTimeout = 3600*1000;
   	        	int waitMs = 5000;
   	        	if ( waitForCompletion ) {
       				AwsToolkit.getInstance().waitForCloudFrontInvalidations(awsSession, region, distributionId, waitMs, waitTimeout);
   	        	}
   	        }
   	        else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
    			// Output to table and/or file, as requested.
   	        	List<DistributionSummary> distributions = AwsToolkit.getInstance().getCloudFrontDistributions(awsSession, region);
   	        	Message.printStatus(2, routine, "Have " + distributions.size() + " distributions.");
   	        	if ( table == null ) {
   	        		Message.printStatus(2, routine, "The table is null - not creating list of distributions.");
   	        	}
   	        	else {
   					boolean allowDuplicates = false;
    				for ( DistributionSummary distribution : distributions ) {
    					if ( commentPattern != null ) {
    						// Check the comment using the pattern for a match.
    						if ( !distribution.comment().matches(commentPattern) ) {
    							// Does not match the pattern so ignore.
    							continue;
    						}
    					}
    					if ( (DistributionID != null) && !DistributionID.isEmpty() ) {
    						// Check the distribution ID for a match.
    						if ( !distribution.id().equals(DistributionID) ) {
    							// Does not exactly match so ignore.
    							continue;
    						}
    					}
    					TableRecord rec = null;
    					if ( !allowDuplicates ) {
    						// Try to match the object key, which is the unique identifier.
    						rec = table.getRecord ( idCol, distribution.id() );
    					}
    					if ( rec == null ) {
    						// Create a new record.
    						rec = table.addRecord(table.emptyRecord());
    					}
    					// Set the data in the record.
    					rec.setFieldValue(idCol,distribution.id());
    					rec.setFieldValue(arnCol,distribution.arn());
    					rec.setFieldValue(commentCol,distribution.comment());
    					rec.setFieldValue(domainNameCol,distribution.domainName());
    					rec.setFieldValue(enabledCol,distribution.enabled());
    				}
   	    		}
    	    }
   	        else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_INVALIDATIONS ) {
   	        	// List invalidations:
   	        	// - useful for troubleshooting
    			// Output to table and/or file, as requested.
   	        	String distributionId = AwsToolkit.getInstance().getCloudFrontDistributionId(awsSession, region, DistributionID, commentPattern);
   	        	boolean doList = true;
   	        	if ( distributionId == null ) {
	    			message = "Unable to determine CloudFront distribution ID for invalidation.";
	    			Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	    			status.addToLog ( commandPhase,
	    				new CommandLogRecord(CommandStatusType.FAILURE,
	    					message, "Verify that the distribution ID is valid for the region." ) );
	    			doList = false;
   	        	}
    	    	// TODO smalers 2022-05-31 for now use UTC time.
    	    	String timezone = "Z";
    	    	ZoneId zoneId = ZoneId.of("Z");
    	    	int behaviorFlag = 0;
   	        	List<InvalidationSummary> invalidations = AwsToolkit.getInstance().getCloudFrontInvalidations(awsSession, region, distributionId );
   	        	Message.printStatus(2, routine, "Have " + invalidations.size() + " invalidations.");
   	        	if ( table == null ) {
   	        		Message.printStatus(2, routine, "The table is null - not creating list of invalidations.");
	    			doList = false;
   	        	}
   	        	if ( doList ) {
   					boolean allowDuplicates = false;
    				for ( InvalidationSummary invalidation : invalidations ) {
    					TableRecord rec = null;
    					if ( !allowDuplicates ) {
    						// Try to match the object key, which is the unique identifier.
    						rec = table.getRecord ( idCol, invalidation.id() );
    					}
    					if ( rec == null ) {
    						// Create a new record.
    						rec = table.addRecord(table.emptyRecord());
    					}
    					// Set the data in the record.
    					rec.setFieldValue(idCol,invalidation.id());
    					rec.setFieldValue(statusCol,invalidation.status());
    					rec.setFieldValue(createTimeCol,
   	    					new DateTime(OffsetDateTime.ofInstant(invalidation.createTime(), zoneId), behaviorFlag, timezone));
    				}
   	    		}
   	        }
	    }
	    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	        if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
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
    catch ( CloudFrontException e ) {
  	    if ( cloudfrontCommand == AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION ) {
			message = "Unexpected error invalidating distribution (" + e.awsErrorDetails().errorMessage() + ").";
		}
        else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
			message = "Unexpected error listing distribution (" + e.awsErrorDetails().errorMessage() + ").";
        }
		else {
			message = "Unexpected error for unknown CloudFront command (" + e.awsErrorDetails().errorMessage() + ": " + CloudFrontCommand;
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
  	    if ( cloudfrontCommand == AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION ) {
			message = "Unexpected error invalidating distribution (" + e + ").";
		}
        else if ( cloudfrontCommand == AwsCloudFrontCommandType.LIST_DISTRIBUTIONS ) {
			message = "Unexpected error listing distributions (" + e + ").";
        }
		else {
			message = "Unexpected error for unknown CloudFront command: " + cloudfrontCommand;
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
public String toString ( PropList parameters ) {
	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String CloudFrontCommand = parameters.getValue("CloudFrontCommand");
	String Profile = parameters.getValue("Profile");
	String Region = parameters.getValue("Region");
	String DistributionId = parameters.getValue("DistributionId");
	String Comment = parameters.getValue("Comment");
	String InvalidationPaths = parameters.getValue("InvalidationPaths");
	String CallerReference = parameters.getValue("CallerReference");
	String WaitForCompletion = parameters.getValue("WaitForCompletion");
	String OutputTableID = parameters.getValue("OutputTableID");
	String OutputFile = parameters.getValue("OutputFile");
	String IfInputNotFound = parameters.getValue("IfInputNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (CloudFrontCommand != null) && (CloudFrontCommand.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "CloudFrontCommand=\"" + CloudFrontCommand + "\"" );
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
	if ( (DistributionId != null) && (DistributionId.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "DistributionId=\"" + DistributionId + "\"" );
	}
	if ( (Comment != null) && (Comment.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "Comment=\"" + Comment + "\"" );
	}
	if ( (InvalidationPaths != null) && (InvalidationPaths.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "InvalidationPaths=\"" + InvalidationPaths + "\"");
	}
	if ( (CallerReference != null) && (CallerReference.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "CallerReference=\"" + CallerReference + "\"" );
	}
	if ( (WaitForCompletion != null) && (WaitForCompletion.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "WaitForCompletion=" + WaitForCompletion );
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