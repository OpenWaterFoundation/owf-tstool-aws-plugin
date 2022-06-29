// AwsS3Catalog_Command - This class initializes, checks, and runs the AwsS3Catalog command.

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
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
import RTi.Util.IO.HTMLWriter;
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
This class initializes, checks, and runs the AwsS3Catalog() command.
*/
public class AwsS3Catalog_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Output file(s) that are created by this command.
*/
private List<File> outputFiles = new ArrayList<File>();

/**
The output table that is created for discovery mode.
*/
private DataTable discoveryOutputTable = null;

/**
Constructor.
*/
public AwsS3Catalog_Command ()
{	super();
	setCommandName ( "AwsS3Catalog" );
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void addOutputFile ( File file ) {
    outputFiles.add(file);
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
{	String Profile = parameters.getValue ( "Profile" );
	String Region = parameters.getValue ( "Region" );
    String Bucket = parameters.getValue ( "Bucket" );
    String StartingPrefix = parameters.getValue ( "StartingPrefix" );
    String CatalogFile = parameters.getValue ( "CatalogFile" );
    String CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" );
    String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" );
	String KeepFiles = parameters.getValue ( "KeepFiles" );
	String UploadFiles = parameters.getValue ( "UploadFiles" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

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

	if ( (Bucket == null) || Bucket.isEmpty() ) {
		message = "The bucket must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the bucket."));
	}

	if ( (KeepFiles != null) && !KeepFiles.equals("") ) {
		if ( !KeepFiles.equalsIgnoreCase(_False) && !KeepFiles.equalsIgnoreCase(_True) ) {
			message = "The KeepFiles parameter \"" + KeepFiles + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True ));
		}
	}

	if ( (UploadFiles != null) && !UploadFiles.equals("") ) {
		if ( !UploadFiles.equalsIgnoreCase(_False) && !UploadFiles.equalsIgnoreCase(_True) ) {
			message = "The UploadFiles parameter \"" + UploadFiles + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True ));
		}
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

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(12);
	validList.add ( "Profile" );
	validList.add ( "Region" );
	validList.add ( "Bucket" );
	validList.add ( "StartingPrefix" );
	validList.add ( "CatalogFile" );
	validList.add ( "CatalogIndexFile" );
	validList.add ( "DatasetIndexFile" );
	validList.add ( "CssUrl" );
	validList.add ( "OutputTableID" );
	validList.add ( "KeepFiles" );
	validList.add ( "UploadFiles" );
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
 * Create the dataset index file.
 * Currently this is a very basic file.
 * @param dataset the dataset being processed
 * @param datasetIndexFile path to the dataset index file to create.
 * @param uploadFiles if true, format for uploading; if false, format for local review
 */
private void createDatasetIndexFile ( DcatDataset dataset, String datasetIndexFile, String cssUrl, boolean uploadFiles ) {
	String routine = getClass().getSimpleName() + "createDatasetIndexFile";
	Message.printStatus(2, routine, "Creating file \"" + datasetIndexFile + "\" for identifier \"" + dataset.getIdentifier() + "\".");
	PrintWriter fout = null;
	// The page layout is simple:
	
	/*
	DataSet: Title
	
	Image  PropertyTable
	
	Dataset Details
	
	Markdown insert.
	 */
	try {
		fout = new PrintWriter ( new FileOutputStream ( datasetIndexFile ) );
		String title = dataset.getTitle();
		HTMLWriter html = new HTMLWriter( null, "Dataset: " + title, false );
		// Start the file and write the head section:
		// - this will include styles
		html.htmlStart();
		String customStyleText = "";
    	writeHtmlHead(html, title, cssUrl, customStyleText);
    	// Start the body section.
    	html.bodyStart();
    	html.header(1, "Dataset: " + dataset.getTitle());
    	
    	// The image is optional.
    	String imageFile = dataset.getLocalImagePath();

    	// Add a layout table with image on the left and property table on the right.
    	PropList tableProps = new PropList("");
    	tableProps.set("border", "none");
    	tableProps.set("padding", "5px");
    	html.tableStart(tableProps);
    	String [] tableHeaders0 = {
    		"",
    		""
    	};
    	boolean doImage = false;
    	if ( IOUtil.fileExists(imageFile) ) {
    		doImage = true;
    	}
    	else {
    		// No image.
    		tableHeaders0 = new String[1];
    		tableHeaders0[0] = "";
    	}
    	html.write("<tr>");
    	for ( String tableHeader : tableHeaders0 ) {
    		html.write ( "<th border=\"none\" padding=\"5px\">" + tableHeader + "</th>" );
    	}
    	html.write("</tr>");
    	
   		html.write("<tr>");
    	if ( doImage ) {
    		// Add table cell for the image.
    		html.write("<td border=\"none\" padding=\"5px\">");
    		if ( uploadFiles ) {
    			// The image in the production system is always named 'dataset.png' and is in the same folder as the index.html file.
    			html.image("dataset.png", "dataset.png");
    		}
    		else {
    			// Use the path to the local image file:
    			// - just use the filename without path since it will be in the same folder as the index.html file
    			File f = new File(dataset.getLocalImagePath());
    			html.image(f.getName(), "dataset.png");
    		}
    		html.write("</td>");
    	}
    	
    	// Always add the property table.
    	
   		html.write("<td border=\"none\" padding=\"5px\">");
    	// Create the property table.
    	html.tableStart();
    	String [] tableHeaders = {
    		"Property",
    		"Description"
    	};
    	//html.tableHeaders ( tableHeaders );
    	html.write("<tr>");
    	for ( String tableHeader : tableHeaders ) {
    		html.write ( "<th>" + tableHeader + "</th>" );
    	}
    	html.write("</tr>");
    	//String [] cells = new String[2];
    	//html.tableCells(cells);
    	html.write("<tr>");
  		  html.write ( "<td>title</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getTitle() );
  		  html.write ( "</td>" );
        html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>identifier</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getIdentifier() );
  		  html.write ( "</td>" );
        html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>description</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getDescription() );
  		  html.write ( "</td>" );
   	    html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>issued</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getIssued() );
  		  html.write ( "</td>" );
   	    html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>modified</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getModified() );
  		  html.write ( "</td>" );
   	    html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>version</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getVersion() );
  		  html.write ( "</td>" );
   	    html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>keyword</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( StringUtil.toString(dataset.getKeyword(), ",") );
  		  html.write ( "</td>" );
   	    html.write("</tr>");
    	html.tableEnd();
    	
    	// End the layout table.
   		html.write("</td>");
   		html.write("</tr>");
    	html.tableEnd();
    	
    	// Add the "Dataset Publisher" section.

  		html.header(1, "Dataset Publisher");
    	// Create the property table.
    	html.tableStart();
    	String [] tableHeaders2 = {
    		"Property",
    		"Description"
    	};
    	html.write("<tr>");
    	for ( String tableHeader : tableHeaders2 ) {
    		html.write ( "<th>" + tableHeader + "</th>" );
    	}
    	html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>name</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getPublisher().getName() );
  		  html.write ( "</td>" );
        html.write("</tr>");
    	html.write("<tr>");
  		  html.write ( "<td>mbox</td>" );
  		  html.write ( "<td>" );
  		  html.addText ( dataset.getPublisher().getMbox() );
  		  html.write ( "</td>" );
        html.write("</tr>");
   		html.write("</td>");
   		html.write("</tr>");
    	html.tableEnd();

    	// Add the "Dataset Details" if Markdown was provided.
    	String markdownFile = dataset.getLocalMarkdownPath();
    	if ( IOUtil.fileExists(markdownFile) && !markdownFile.isEmpty() ) {
    		html.header(1, "Dataset Details");
    		// Read the MarkDown file into memory.
    		StringBuilder b = IOUtil.fileToStringBuilder(markdownFile);
    		Parser parser = Parser.builder().build();
    		Node document = parser.parse(b.toString());
    		HtmlRenderer renderer = HtmlRenderer.builder().build();
    		// Add the HTML from Markdown to the index.
    		html.write(renderer.render(document));
    	}

		html.bodyEnd();
		html.htmlEnd();
		fout.print(html.getHTML());
	}
	catch ( Exception e ) {
		String message = "Error writing \"" + datasetIndexFile + "\" (" + e + ").";
		Message.printWarning( 2, routine, message );
		Message.printWarning( 3, routine, e );
		throw new RuntimeException (message);
	}
	finally {
		if ( fout != null ) {
			fout.close();
		}
	}
}

/**
 * Download the dataset file found on S3 to a temporary file.
 * Then it can be read and turned into other products.
 * Also download the 'dataset.md' file to use as input to the 'index.html' landing page.
 * Also download the 'dataset.png' file to use as an image in the 'index.html' landing page.
 * @param tm S3TransferManager object used to download the file
 * @param bucket the name of the bucket containing the file
 * @param startingPrefix the starting prefix for S3 listing
 * @param s3FileKey the S3 object key corresponding to the dataset file (does NOT contain the starting prefix)
 * @param status the command status to add problem messages
 * @param commandTag the command tag for status messages
 * @param warningCount the warning count for messages
 * @return a preliminary DcatDataset object with local filenames filled in using temporary files
 */
private DcatDataset downloadDatasetFile ( S3TransferManager tm, String bucket,
	String startingPrefix, String s3FileKey,
	CommandStatus status, String commandTag, int warningCount ) {
	String routine = getClass().getSimpleName() + ".downloadDatasetFile";
	// Create a dataset object to store local filenames:
	// - a full object will be created later
	DcatDataset dataset = new DcatDataset();
	// Create a unique temporary file:
	// - use a unique start
	// - use end that matches the key with / replaced by - to slugify the name
	String tempName = IOUtil.tempFileName() + "-" + s3FileKey.replace("/","-");
	//if ( (startingPrefix != null) && !startingPrefix.isEmpty() && !startingPrefix.equals("/")) {
	//	// Reset the local name to also use the prefix.
	//	tempName = IOUtil.tempFileName() + "-" + startingPrefix.replace("/", "-") + "-" + s3FileKey.replace("/","-");
	//}
	dataset.setLocalPath(tempName);
	Message.printStatus(2, routine, "Downloading dataset metadata file from S3 \"" + s3FileKey + "\" to local \"" + tempName + "\"");
	List<String> problems = new ArrayList<>();
	AwsToolkit.getInstance().downloadFileFromS3 ( tm, bucket, s3FileKey, tempName, problems );
	int warning_level = 3;
	for ( String problem : problems ) {
    	 Message.printWarning ( warning_level, 
    	    MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, problem );
    	    	status.addToLog(CommandPhaseType.RUN,
    	    	new CommandLogRecord(CommandStatusType.FAILURE,
    	    	problem, "Check the command input."));
	}
	
	// Also download the dataset markdown file if it exists (allow error).
	String mdTempName = tempName.replace(".json",".md");
	problems.clear();
	String mdS3FileKey = s3FileKey.replace(".json", ".md");
	try {
		AwsToolkit.getInstance().downloadFileFromS3 ( tm, bucket, mdS3FileKey, mdTempName, problems );
		if ( problems.size() == 0 ) {
			Message.printStatus(2, routine, "Success downloading dataset markdown file from S3 \"" + mdS3FileKey + "\" to local \"" + mdTempName + "\"");
			dataset.setLocalMarkdownPath(mdTempName);
		}
		else {
			Message.printStatus(2, routine, "Unable to download dataset markdown file from S3 \"" + mdS3FileKey + "\" to local \"" + mdTempName + "\"");
		}
	}
	catch ( Exception e ) {
		// Key does not exist to download.
		Message.printStatus(2, routine, "Key does not exist for markdown file from S3 \"" + mdS3FileKey + "\" to \"" + mdTempName + "\"");
	}

	// Also download the dataset image file if it exists (allow error).
	String imageTempName = tempName.replace(".json",".png");
	problems.clear();
	String imageS3FileKey = s3FileKey.replace(".json", ".png");
	try {
		AwsToolkit.getInstance().downloadFileFromS3 ( tm, bucket, imageS3FileKey, imageTempName, problems );
		if ( problems.size() == 0 ) {
			Message.printStatus(2, routine, "Success downloading dataset image file from S3 \"" + imageS3FileKey + "\" to local \"" + imageTempName + "\"");
			dataset.setLocalImagePath(imageTempName);
		}
		else {
			Message.printStatus(2, routine, "Unable to download dataset image file from S3 \"" + imageS3FileKey + "\" to local \"" + imageTempName + "\"");
		}
	}
	catch ( Exception e ) {
		// Key does not exist to download.
		Message.printStatus(2, routine, "Key does not exist for dataset image file from S3 \"" + imageS3FileKey + "\" to \"" + imageTempName + "\"");
	}
	
	return dataset;
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
	return (new AwsS3Catalog_JDialog ( parent, this, tableIDChoices )).ok();
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
    for ( File file : this.outputFiles ) {
    	if ( file != null ) {
        	list.add ( file );
    	}
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
 * Read a dataset JSON file and create a dataset object.
 * The file must exist.
 * @param datasetFile the path to the 'dataset.json' file to read
 * @return a dataset object
 */
private DcatDataset readDatasetFile ( String datasetFile ) throws Exception {
	DcatDataset dataset = null;
	ObjectMapper mapper = new ObjectMapper();
	mapper.registerModule(new JavaTimeModule());
	dataset = mapper.readValue(IOUtil.fileToStringBuilder(datasetFile).toString(),
		new TypeReference<DcatDataset>() {});
	return dataset;
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
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
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
	
    // Clear the output files.
    this.outputFiles.clear();
	
	String Profile = parameters.getValue ( "Profile" );
	Profile = TSCommandProcessorUtil.expandParameterValue(processor,this,Profile);
	String profile = Profile;
	if ( (Profile == null) || Profile.isEmpty() ) {
		// Get the default.
		profile = AwsToolkit.getInstance().getDefaultProfile();
	}
	String region = parameters.getValue ( "Region" );
	region = TSCommandProcessorUtil.expandParameterValue(processor,this,region);
	// Bucket must be final because of lambda use below.
	String bucket0 = parameters.getValue ( "Bucket" );
	final String bucket = TSCommandProcessorUtil.expandParameterValue(processor,this,bucket0);
	String StartingPrefix = parameters.getValue ( "StartingPrefix" );
	StartingPrefix = TSCommandProcessorUtil.expandParameterValue(processor,this,StartingPrefix);
	if ( (StartingPrefix == null) || StartingPrefix.isEmpty() ) {
		StartingPrefix = "/";
	}
	String CssUrl = parameters.getValue ( "CssUrl" );
	CssUrl = TSCommandProcessorUtil.expandParameterValue(processor,this,CssUrl);
	boolean doTable = false;
	String OutputTableID = parameters.getValue ( "OutputTableID" );
	OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
	if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
		doTable = true;
	}
	String CatalogFile = parameters.getValue ( "CatalogFile" ); // Expand below.
	String CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" ); // Expand below.
	String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" ); // Expand below.
	String KeepFiles = parameters.getValue ( "KeepFiles" );
	boolean keepFiles = false; // Default.
	if ( (KeepFiles != null) && KeepFiles.equalsIgnoreCase("true")) {
	    keepFiles = true;
	}
	String UploadFiles = parameters.getValue ( "UploadFiles" );
	boolean uploadFiles = false; // Default.
	if ( (UploadFiles != null) && UploadFiles.equalsIgnoreCase("true")) {
	    uploadFiles = true;
	}
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
   	// Create the catalog file.
   	boolean doCatalog = true;
   	// Upload the created dataset index file.

	Region regionO = Region.of(region);
	
	// S3Client is needed to list files.
	S3Client s3 = S3Client.builder()
		.region(regionO)
		.credentialsProvider(credentialsProvider)
		.build();

	// S3TransferManager is needed to efficiently upload and download files.
   	S3TransferManager tm = S3TransferManager
   		.builder()
   		.s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
  		.region(regionO))
   		.build();
	
	// Process the files.  Each input file is opened to scan the file.
	// The output file is opened once in append mode.
    // TODO SAM 2014-02-03 Enable copying a list to a folder, etc. see AppendFile() for example.
	/*
	String OutputFile_full = IOUtil.verifyPathForOS(
	    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	        */

   	// List of temporary files to delete.
   	List<String> tempfileList = new ArrayList<>();
   	// List of files to invalidate.
   	List<String> invalidationList = new ArrayList<>();

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
    	    		// - it contains the keys that are in the catalog
    	        	List<TableField> columnList = new ArrayList<>();
    	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Key", -1) );
    	        	columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "SizeKb", -1) );
    	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Owner", -1) );
    	        	columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "LastModified", -1) );
    	        	table = new DataTable( columnList );
                	// Get the column numbers for later use.
    	        	objectKeyCol = table.getFieldIndex("Key");
    	        	objectSizeKbCol = table.getFieldIndex("SizeKb");
    	        	objectOwnerCol = table.getFieldIndex("Owner");
    	        	objectLastModifiedCol = table.getFieldIndex("LastModified");
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

    	    // Call the service that was requested:
	    	// - specify the starting prefix to begin the search for datasets
		
   	        if ( doCatalog ) {
   	        	software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = ListObjectsV2Request
    	    		.builder()
    	    		.fetchOwner(Boolean.TRUE)
    	    		.prefix(StartingPrefix)
    	    		.bucket(bucket);
   	        	/*
    	    	if ( maxKeys > 0 ) {
    	    		// Set the maximum number of keys that will be returned.
    	    		builder.maxKeys(maxKeys);
    	    	}
    	    	if ( (Prefix != null) && !Prefix.isEmpty() ) {
    	    		// Set the key prefix to match.
    	    		builder.prefix(Prefix);
    	    	}
    	    	*/
    	    	
    	    	ListObjectsV2Request request = builder.build();
    	    	ListObjectsV2Response response = null;
    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;
    	    	// TODO smalers 2022-05-31 for now use UTC time.
    	    	String timezone = "Z";
    	    	ZoneId zoneId = ZoneId.of("Z");
    	    	int behaviorFlag = 0;
    	    	boolean done = false;
    	    	int maxObjects = 10000; // Default for now.
    	    	int objectCount = 0;
    	    	String dcatFilePattern = ".*dataset.json";
    	    	// Read bucket list one chunk at a time and process dataset files as they are matched.
    	    	while ( !done ) {
    	    		response = s3.listObjectsV2(request);
    	    		for ( S3Object s3Object : response.contents() ) {
    	    			// Check the maximum object count, to protect against runaway processes.
    	    			if ( objectCount >= maxObjects ) {
    	    				break;
    	    			}

    	    			// Filter to only the DCAT files (*dataset.json).
    	    			if ( !s3Object.key().matches(dcatFilePattern) ) {
    	    				// Does not match so ignore.
    	    				continue;
    	    			}
    	    			// Download the dataset file to a temporary file.
    	    			Message.printStatus(2, routine, "Downloading key \"" + s3Object.key() + "\"." );
    	    			// Download the file and return a preliminary object with local file names but object is not filled out.
    	    			DcatDataset datasetLocalInfo = downloadDatasetFile ( tm, bucket, StartingPrefix, s3Object.key(), status, command_tag, warning_count );
    	    			
    	    			// Get the folder for use with invalidation.
    	    			File s3File = new File(s3Object.key());
    	    			String s3Folder = s3File.getParent();
    	    			if ( !s3Folder.startsWith("/") ) {
    	    				// Make sure the folder starts with a slash for CloudFront invalidation.
    	    				s3Folder = "/" + s3Folder;
    	    			}
    	    			
    	    			
    	    			// Read the dataset JSON file into an object.
    	    			Message.printStatus(2, routine, "Creating dataset object from file." );
    	    			DcatDataset dataset = readDatasetFile ( datasetLocalInfo.getLocalPath() );
    	    			if ( dataset == null ) {
   	    					message = "Error creating dataset from file \"" + datasetLocalInfo.getLocalPath() + "\".";
   	    					Message.printWarning ( warning_level, 
   	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    					status.addToLog(CommandPhaseType.RUN,
   	    						new CommandLogRecord(CommandStatusType.FAILURE,
   	    							message, "Confirm that the dataset.json file is correct."));
   	    					// Can't continue processing.
   	    					continue;
    	    			}
    	    			else if ( (dataset.getTitle() == null) || dataset.getTitle().isEmpty() ) {
   	    					message = "Dataset does not contain 'title' - file may be invalid.";
   	    					Message.printWarning ( warning_level, 
   	    						MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    					status.addToLog(CommandPhaseType.RUN,
   	    						new CommandLogRecord(CommandStatusType.FAILURE,
   	    							message, "Confirm that the dataset.json file is correct."));
   	    					// Can't continue processing.
   	    					continue;
    	    			}
    	    			// Set the AWS cloud path for easy access.
    	    			dataset.setCloudPath(s3Object.key());
    	    			// Create the dataset index.html landing page file.
    	    			String datasetIndexFile = null;
    	    			if ( (DatasetIndexFile != null) && !DatasetIndexFile.isEmpty() ) {
    	    				// Want to create the dataset index file.
    	    				if ( DatasetIndexFile.equalsIgnoreCase("Temp") ) {
    	    					// Use a temporary file.
    	    					//datasetIndexFile = IOUtil.tempFileName() + "-index.html";
    	    					// Create as the same name as the dataset.json file but use index.html
    	    					datasetIndexFile = datasetLocalInfo.getLocalPath().replace("dataset.json","index.html");
    	    				}
    	    				else {
    	    					datasetIndexFile = IOUtil.verifyPathForOS(
    	    						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            						TSCommandProcessorUtil.expandParameterValue(processor,this,DatasetIndexFile)));
    	    				}
    	    				// Set the local dataset information:
    	    				// - local dataset index path is set to created index
    	    				// - other files consistent with the download (markdown, image) are copied
    	    				//dataset.setLocalPath(datasetIndexFile);
    	    				dataset.setLocalMarkdownPath(datasetLocalInfo.getLocalMarkdownPath());
    	    				dataset.setLocalImagePath(datasetLocalInfo.getLocalImagePath());
    	    				createDatasetIndexFile ( dataset, datasetIndexFile, CssUrl, uploadFiles );
    	    				tempfileList.add(datasetIndexFile);
    	    				// Upload the index to the S3 bucket:
    	    				// - store in the same folder as the dataset file:
    	    				// - also keep track of the file so it can be invalidated
    	    				if ( uploadFiles ) {
    	    					// Upload the dataset index file:
    	    					// - the image file used in the index will already have been uploaded
    	    					String s3UploadKey = s3Folder + "index.html";
    	    					try {
    	    						warning_count = uploadDatasetFile ( tm, bucket, datasetIndexFile, s3UploadKey, status, command_tag, warning_count );
    	    					}
    	    					catch ( Exception e ) {
    	    						message = "Error uploading file \"" + datasetIndexFile + "\" to S3 key \"" + s3UploadKey + "\" (" + e + ")";
    	    						Message.printWarning ( warning_level, 
    	    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    	    						Message.printWarning ( 3, routine, e );
    	    						status.addToLog(CommandPhaseType.RUN,
    	    							new CommandLogRecord(CommandStatusType.FAILURE,
    	    								message, "See the log file for details."));
    	    					}
    	    				}
    	    				invalidationList.add(datasetIndexFile);
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
		message = "Unexpected error creating catalog (" + e + ").";
		Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
		throw new CommandException ( message );
	}
    finally {
        if ( uploadFiles && (invalidationList.size() > 0) ) {
        	// Invalidate the list of uploaded files.
        	AwsToolkit.getInstance().invalidateCloudFrontDistribution(invalidationList);
  	    }
        // Clean up temporary files used for main catalog and index, and dataset index files.
       	for ( String tempfile : tempfileList ) {
       		if ( keepFiles ) {
        		// Keep the temporary files and add to the output files.
        		this.outputFiles.add(new File(tempfile));
        	}
        	else {
        		// Remove the temporary files.
        		try {
        			Files.delete(Paths.get(tempfile));
        		}
        		catch ( IOException e ) {
        			message = "Error deleting temporary file \"" + tempfile + "\" (" + e + ").";
		   			Message.printWarning ( warning_level, 
		      			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		   			Message.printWarning ( 3, routine, e );
		   			status.addToLog(CommandPhaseType.RUN,
		     			new CommandLogRecord(CommandStatusType.FAILURE,
			      			message, "See the log file for details."));
        		}
        	}
        }
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
	String Region = parameters.getValue("Region");
	String Bucket = parameters.getValue("Bucket");
	//String MaxKeys = parameters.getValue("MaxKeys");
	String StartingPrefix = parameters.getValue("StartingPrefix");
	//String MaxObjects = parameters.getValue("MaxObjects");
	String CatalogFile = parameters.getValue("CatalogFile");
	String CatalogIndexFile = parameters.getValue("CatalogIndexFile");
	String DatasetIndexFile = parameters.getValue("DatasetIndexFile");
	String CssUrl = parameters.getValue("CssUrl");
	String OutputTableID = parameters.getValue("OutputTableID");
	String KeepFiles = parameters.getValue("KeepFiles");
	String UploadFiles = parameters.getValue("UploadFiles");
	String IfInputNotFound = parameters.getValue("IfInputNotFound");
	StringBuffer b = new StringBuffer ();
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
	/*
	if ( (MaxKeys != null) && (MaxKeys.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "MaxKeys=" + MaxKeys );
	}
	*/
	if ( (StartingPrefix != null) && (StartingPrefix.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "StartingPrefix=\"" + StartingPrefix + "\"" );
	}
	/*
	if ( (MaxObjects != null) && (MaxObjects.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "MaxObjects=" + MaxObjects );
	}
	*/
    if ( (CatalogFile != null) && (CatalogFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CatalogFile=\"" + CatalogFile + "\"");
    }
    if ( (CatalogIndexFile != null) && (CatalogIndexFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CatalogIndexFile=\"" + CatalogIndexFile + "\"");
    }
    if ( (DatasetIndexFile != null) && (DatasetIndexFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DatasetIndexFile=\"" + DatasetIndexFile + "\"");
    }
    if ( (CssUrl != null) && (CssUrl.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CssUrl=\"" + CssUrl + "\"" );
    }
    if ( (OutputTableID != null) && (OutputTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableID=\"" + OutputTableID + "\"" );
    }
	if ( (KeepFiles != null) && (KeepFiles.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "KeepFiles=" + KeepFiles );
	}
	if ( (UploadFiles != null) && (UploadFiles.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "UploadFiles=" + UploadFiles );
	}
	if ( (IfInputNotFound != null) && (IfInputNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfInputNotFound=" + IfInputNotFound );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

/**
 * Upload the dataset index file to S3.
 * @param datasetIndexFile the path to the local index file to upload.
 * @param s3FileKey the key to the file on S3
 * @return the updated warning count
 */
private int uploadDatasetFile ( S3TransferManager tm, String bucket,
	String datasetIndexFile, String s3FileKey,
	CommandStatus status, String commandTag, int warningCount ) {
	String routine = getClass().getSimpleName() + ".uploadDatasetFile";
	Message.printStatus(3, routine, "Uploading file from \"" + datasetIndexFile + "\" to \"" + s3FileKey + "\"");
	List<String> problems = new ArrayList<>();
	AwsToolkit.getInstance().uploadFileToS3 ( tm, bucket, datasetIndexFile, s3FileKey, problems );
	int warning_level = 3;
	for ( String problem : problems ) {
    	 Message.printWarning ( warning_level, 
    	    MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, problem );
    	    	status.addToLog(CommandPhaseType.RUN,
    	    	new CommandLogRecord(CommandStatusType.FAILURE,
    	    	problem, "Check the command input."));
	}
	return warningCount;
}

/**
Writes the start tags for the HTML indext file.
@param html HTMLWriter object.
@param title title for the document.
@throws Exception
*/
private void writeHtmlHead( HTMLWriter html, String title, String cssUrl, String customStyleText ) throws Exception {
    if ( html != null ) {
        html.headStart();
        html.title(title);
        writeHtmlStyles(html, cssUrl, customStyleText);
        html.headEnd();
    }
}

/**
Inserts the style attributes for a dataset index.
This was copied from the TSHtmlFormatter since tables are used with time series also.
@throws Exception
*/
private void writeHtmlStyles(HTMLWriter html, String cssUrl, String customStyleText )
throws Exception {
	if ( (cssUrl != null) && !cssUrl.isEmpty() ) {
		// Use the CSS provided in a URL, typically shared across a website.
		html.write("<link rel=\"stylesheet\" href=\"" + cssUrl + "\">\n");
	}
	else {
		// Add build-in CSS.
		html.write("<style>\n"
        + "@media screen {\n"
        + "#titles {\n"
        + "  font-weight:bold;\n"
        + "  color:#303044\n"
        + "}\n"
        + "table {\n"
        + "  background-color:black;\n"
        + "  text-align:left;\n"
        + "  border:1;\n"
        + "  bordercolor:black;\n"
        + "  cellspacing:1;\n"
        + "  cellpadding:1\n"
        + "}\n"  
        + "th {\n"
        + "  background-color:#333366;\n"
        + "  text-align:center;\n"
        + "  vertical-align:bottom;\n"
        + "  color:white\n"
        + "}\n"
        + "tr {\n"
        + "  valign:bottom;\n"
        + "  halign:right\n"
        + "}\n"
        + "td {\n"
        + "  background-color:white;\n"
        + "  text-align:right;\n"
        + "  vertical-align:bottom;\n"
        + "  font-style:normal;\n"
        + "  font-family:courier;\n"
        + "  font-size:.75em\n"
        + "}\n" 
        + "body {\n"
        + "  text-align:left;\n"
        + "  font-size:12pt;\n"
        + "}\n"
        + "pre {\n"
        + "  font-size:12pt;\n"
        + "  margin: 0px\n"
        + "}\n"
        + "p {\n"
        + "  font-size:12pt;\n"
        + "}\n"
        + "/* The following controls formatting of data values in tables */\n"
        + ".flagcell {\n"
        + "  background-color:lightgray;\n"
        + "}\n"
        + ".missing {\n"
        + "  background-color:yellow;\n"
        + "}\n"
        + ".flag {\n"
        + "  vertical-align: super;\n"
        + "}\n"
        + ".flagnote {\n"
        + "  font-style:normal;\n"
        + "  font-family:courier;\n"
        + "  font-size:.75em;\n"
        + "}\n" );
		if ( (customStyleText != null) && !customStyleText.equals("") ) {
    	    // Add the custom styles.
            html.write ( customStyleText );
        }
		html.write (
        "}\n"  // End screen media.
        + "@media print {\n"
        + "#titles {\n"
        + "  font-weight:bold;\n"
        + "  color:#303044\n"
        + "}\n"
        + "table {\n"
        + "  border-collapse: collapse;\n"
        + "  background-color:white;\n"
        + "  text-align:left;\n"
        + "  border:1pt solid #000000;\n"
        + "  cellspacing:2pt;\n"
        + "  cellpadding:2pt\n"
        + "}\n"  
        + "th {\n"
        + "  background-color:white;\n"
        + "  text-align:center;\n"
        + "  vertical-align:bottom;\n"
        + "  color:black\n"
        + "}\n"
        + "tr {\n"
        + "  valign:bottom;\n"
        + "  halign:right;\n"
        + "}\n"
        + "td {\n"
        + "  background-color:white;\n"
        + "  border: 1pt solid #000000;\n"
        + "  text-align:right;\n"
        + "  vertical-align:bottom;\n"
        + "  font-style:normal; "
        + "  font-family:courier;\n"
        + "  font-size:11pt;\n"
        + "  padding: 2pt;\n"
        + "}\n" 
        + "body {\n"
        + "  text-align:left;\n"
        + "  font-size:11pt;\n"
        + "}\n"
        + "pre {\n"
        + "  font-size:11pt;\n"
        + "  margin: 0px\n"
        + "}\n"
        + "p {\n"
        + "  font-size:11pt;\n"
        + "}\n"
        + "/* The following controls formatting of data values in tables */\n"
        + ".flagcell {\n"
        + "  background-color:lightgray;\n"
        + "}\n"
        + ".missing {\n"
        + "  background-color:yellow;\n"
        + "}\n"
        + ".flag {\n"
        + "  vertical-align: super;\n"
        + "}\n"
        + ".flagnote {\n"
        + "  font-style:normal;\n"
        + "  font-family:courier;\n"
        + "  font-size:11pt;\n"
        + "}\n" );
        if ( (customStyleText != null) && !customStyleText.equals("") ) {
            html.write ( customStyleText );
        }
        html.write (
            "}\n"  // End print media.
            + "</style>\n");
	}
}

}