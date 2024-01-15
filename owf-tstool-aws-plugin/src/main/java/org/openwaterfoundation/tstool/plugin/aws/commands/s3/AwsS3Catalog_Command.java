// AwsS3Catalog_Command - This class initializes, checks, and runs the AwsS3Catalog command.

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
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
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
import RTi.Util.IO.Markdown.MarkdownWriter;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

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
private List<File> outputFiles = new ArrayList<>();

/**
The output table that is created for discovery mode.
*/
private DataTable discoveryOutputTable = null;

/**
Constructor.
*/
public AwsS3Catalog_Command () {
	super();
	setCommandName ( "AwsS3Catalog" );
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void addOutputFile ( File file ) {
    this.outputFiles.add(file);
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
	String Profile = parameters.getValue ( "Profile" );
	String Region = parameters.getValue ( "Region" );
    String Bucket = parameters.getValue ( "Bucket" );
    //String StartingPrefix = parameters.getValue ( "StartingPrefix" );
    String ProcessSubdirectories = parameters.getValue ( "ProcessSubdirectories" );
    //String CatalogFile = parameters.getValue ( "CatalogFile" );
    //String CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" );
	String UploadCatalogFiles = parameters.getValue ( "UploadCatalogFiles" );
    //String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" );
	String KeepFiles = parameters.getValue ( "KeepFiles" );
	String UploadDatasetFiles = parameters.getValue ( "UploadDatasetFiles" );
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

	if ( (ProcessSubdirectories != null) && !ProcessSubdirectories.equals("") ) {
		if ( !ProcessSubdirectories.equalsIgnoreCase(_False) && !ProcessSubdirectories.equalsIgnoreCase(_True) ) {
			message = "The ProcessSubdirectories parameter \"" + ProcessSubdirectories + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True ));
		}
	}

	if ( (UploadCatalogFiles != null) && !UploadCatalogFiles.equals("") ) {
		if ( !UploadCatalogFiles.equalsIgnoreCase(_False) && !UploadCatalogFiles.equalsIgnoreCase(_True) ) {
			message = "The UploadCatalogFiles parameter \"" + UploadCatalogFiles + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True ));
		}
	}

	if ( (UploadDatasetFiles != null) && !UploadDatasetFiles.equals("") ) {
		if ( !UploadDatasetFiles.equalsIgnoreCase(_False) && !UploadDatasetFiles.equalsIgnoreCase(_True) ) {
			message = "The UploadDatasetFiles parameter \"" + UploadDatasetFiles + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True ));
		}
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
	List<String> validList = new ArrayList<>(17);
	validList.add ( "Profile" );
	validList.add ( "Region" );
	validList.add ( "Bucket" );
	validList.add ( "StartingPrefix" );
	validList.add ( "ProcessSubdirectories" );
	validList.add ( "CatalogFile" );
	validList.add ( "CatalogIndexFile" );
	validList.add ( "UploadCatalogFiles" );
	validList.add ( "DistributionId" );
	validList.add ( "DatasetIndexFile" );
	validList.add ( "DatasetIndexHeadFile" );
	validList.add ( "DatasetIndexBodyFile" );
	validList.add ( "DatasetIndexFooterFile" );
	validList.add ( "UploadDatasetFiles" );
	validList.add ( "OutputTableID" );
	validList.add ( "KeepFiles" );
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
 * Create the dataset index file in HTML format.
 * Currently this is a very basic file.
 * @param dataset the dataset being processed
 * @param parentDataset the parent dataset for the dataset being processed
 * @param datasetIndexFile path to the dataset index file to create
 * @param datasetIndexHeadFile path to the dataset index <head> file to insert, or null to ignore
 * @param datasetIndexBodyFile path to the dataset index <body> file to insert, or null to ignore
 * @param datasetIndexFooterFile path to the dataset index <footer> file to insert, or null to ignore
 * @param uploadDatasetFiles if true, format for uploading; if false, format for local review
 */
private void createDatasetIndexFileHtml ( DcatDataset dataset, DcatDataset parentDataset,
	String datasetIndexFile, String datasetIndexHeadFile, String datasetIndexBodyFile, String datasetIndexFooterFile,
	boolean uploadDatasetFiles ) {
	String routine = getClass().getSimpleName() + ".createDatasetIndexFileHtml";
	Message.printStatus(2, routine, "Creating file \"" + datasetIndexFile +
		"\" for dataset identifier \"" + dataset.getIdentifier() + "\".");
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
		String cssUrl = null;
    	writeHeadHtml(html, title, cssUrl, customStyleText, datasetIndexHeadFile);
    	// Start the body section.
    	html.bodyStart();
    	if ( (datasetIndexBodyFile != null) && datasetIndexBodyFile.toUpperCase().endsWith(".HTML") ) {
    		// Insert the header content.
    		Message.printStatus(2,routine, "Insert file into <body>: " + datasetIndexBodyFile);
    		html.comment("Start inserting file: " + datasetIndexBodyFile);
    		html.write(IOUtil.fileToStringBuilder(datasetIndexBodyFile).toString());
    		html.comment("End inserting file into <body>: " + datasetIndexBodyFile);
    	}
    	// Use a <div> around all the displayed body content:
    	// - the class matches the CSS file
    	html.write("<div class=\"dataset-content-container\">");
    	html.header(1, "Dataset: " + dataset.getTitle());
    	
    	// The image is optional.
    	//String imageURL = "dataset.png";
    	String imageFile = dataset.getLocalImagePath();
    	if ( (imageFile == null) && (parentDataset != null) ) {
    		// If the image is null but is available for the parent, use that.
    		imageFile = parentDataset.getLocalImagePath();
    		//imageURL = "../dataset.png";
    	}

    	boolean doImage = false;
    	if ( (imageFile != null) && IOUtil.fileExists(imageFile) ) {
    		doImage = true;
    		if ( uploadDatasetFiles ) {
    			// The image in the production system is always named 'dataset.png' and is in the same folder as the index.html file.
    			imageFile = "dataset.png";
    		}
    		else {
    			// Use the path to the local image file:
    			// - just use the filename without path since it will be in the same folder as the index.html file
    			File f = new File(imageFile);
    			imageFile = f.getName();
    		}
    	}
    	else {
    		// No image.
    		doImage = false;
    	}

    	// Add a 'div' with flex layout for the side-by-side image and tables.
    	// Always have a place for the image:
    	// - if an image does not exist, add placeholder text
    	html.write("\n<div class=\"dataset-image-and-property-container\">\n");
    	
    	if ( doImage ) {
   			html.write("    <img src=\"" + imageFile + "\" alt=\"dataset.png\" border=\"0\" class=\"dataset-image\">\n");
    	}
    	else {
    		// Placeholder since no image.
   			html.write("    <p>No image is available.</p>\n");
    	}
		html.write("    <div class=\"dataset-property-table-container\">\n");
    	
    	// Always add the property table.
    	
    	// Create the property table.
   		PropList tableProps = new PropList("properties");
   		tableProps.set("class", "dataset-property-table");
    	html.tableStart(tableProps);
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
    	
    	html.write("\n  </div> <!-- dataset-property-table-container -->\n");
    	html.write("  </div> <!-- dataset-image-and-property-container -->\n");
    	
    	// Add the "Dataset Publisher" section.

  		html.header(1, "Dataset Publisher");
    	// Create the dataset publisher table.
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

    		// Load CommonMark extensions.
    		List<Extension> extensions = Arrays.asList(TablesExtension.create());
    		
    		// Parse the Markdown and convert to HTMTL
    		Parser parser = Parser
    			.builder()
    			.extensions(extensions)
    			.build();
    		Node document = parser.parse(b.toString());
    		HtmlRenderer renderer = HtmlRenderer
    			.builder()
    			.extensions(extensions)
    			.build();

    		// Add the HTML from Markdown to the index.
    		Message.printStatus(2,routine,"Insert markdown file into dataset details: " + markdownFile);
    		if ( Message.isDebugOn ) {
    			Message.printStatus(2,routine,"Markdown to insert: " + b.toString());
    			Message.printStatus(2,routine,"Inserting: " + renderer.render(document));
    		}
    		html.write(renderer.render(document));
    	}

    	html.write("\n</div> <!-- class=\"dataset-content-container\" -->\n");
		html.bodyEnd();
    	if ( (datasetIndexFooterFile != null) && datasetIndexFooterFile.toUpperCase().endsWith(".HTML") ) {
    		// Insert the footer content.
    		Message.printStatus(2,routine,"Insert file into <footer>: " + datasetIndexFooterFile);
    		html.comment("Start inserting file: " + datasetIndexFooterFile);
    		html.write(IOUtil.fileToStringBuilder(datasetIndexFooterFile).toString());
    		html.comment("End inserting file: " + datasetIndexFooterFile);
    	}
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
 * Create the dataset index file in Markdown format.
 * Currently this is a very basic file.
 * @param dataset the dataset being processed
 * @param parentDataset the parent dataset for the dataset being processed
 * @param datasetIndexFile path to the dataset Markdown index file to create
 * @param datasetIndexHeadFile path to the dataset index <head> file to insert, or null to ignore (must be Markdown)
 * @param datasetIndexBodyFile path to the dataset index <body> file to insert, or null to ignore (must be Markdown)
 * @param datasetIndexFooterFile path to the dataset index <footer> file to insert, or null to ignore (must be Markdown)
 * @param uploadDatasetFiles if true, format for uploading; if false, format for local review
 */
private void createDatasetIndexFileMarkdown ( DcatDataset dataset, DcatDataset parentDataset,
	String datasetIndexFile, String datasetIndexHeadFile, String datasetIndexBodyFile, String datasetIndexFooterFile,
	boolean uploadDatasetFiles ) throws IOException {
	String routine = getClass().getSimpleName() + ".createDatasetIndexFileMarkdown";
	Message.printStatus(2, routine, "Creating file \"" + datasetIndexFile +
		"\" for dataset identifier \"" + dataset.getIdentifier() + "\".");
	//PrintWriter fout = null;
	// The page layout is simple:
	
	/*
	DataSet: Title
	
	Image  PropertyTable
	
	Dataset Details
	
	Markdown dataset insert.
	 */
	MarkdownWriter markdown = null;
	try {
		String title = dataset.getTitle();
		// Start the file and write the head section:
		// - currently styles depend on the rendering code
		markdown = new MarkdownWriter(datasetIndexFile, false);
    	writeHeadMarkdown(markdown, title, datasetIndexHeadFile);
    	// Start the body section.
    	if ( (datasetIndexBodyFile != null) && (datasetIndexBodyFile.toUpperCase().endsWith(".MD"))) {
    		// Insert the header content.
    		Message.printStatus(2,routine, "Insert file into <body>: " + datasetIndexBodyFile);
    		markdown.comment("Start inserting file: " + datasetIndexBodyFile);
    		markdown.write(IOUtil.fileToStringBuilder(datasetIndexBodyFile).toString());
    		markdown.comment("End inserting file into <body>: " + datasetIndexBodyFile);
    	}
    	markdown.heading(2, "Dataset: " + dataset.getTitle());
    	
    	// The image is optional.
    	//String imageURL = "dataset.png";
    	String imageFile = dataset.getLocalImagePath();
    	if ( (imageFile == null) && (parentDataset != null) ) {
    		// If the image is null but is available for the parent, use that.
    		imageFile = parentDataset.getLocalImagePath();
    		//imageURL = "../dataset.png";
    	}

    	boolean doImage = false;
    	if ( (imageFile != null) && IOUtil.fileExists(imageFile) ) {
    		doImage = true;
    		if ( uploadDatasetFiles ) {
    			// The image in the production system is always named 'dataset.png' and is in the same folder as the index.html file.
    			imageFile = "dataset.png";
    		}
    		else {
    			// Use the path to the local image file:
    			// - just use the filename without path since it will be in the same folder as the index.html file
    			File f = new File(imageFile);
    			imageFile = f.getName();
    		}
    	}
    	else {
    		// No image.
    		doImage = false;
    	}

    	// Add a 'div' with flex layout for the side-by-side image and tables.
    	// Always have a place for the image:
    	// - if an image does not exist, add placeholder text
    	//markdown.write("\n<div class=\"dataset-image-and-property-container\">\n");
    	
    	if ( doImage ) {
   			//markdown.write("    <img src=\"" + imageFile + "\" alt=\"dataset.png\" border=\"0\" class=\"dataset-image\">\n");
    	}
    	else {
    		// Placeholder since no image.
   			//markdown.write("    <p>No image is available.</p>\n");
    	}
		//markdown.write("    <div class=\"dataset-property-table-container\">\n");
    	
    	// Always add the property table.
    	
    	// Create the property table.
   		PropList tableProps = new PropList("properties");
   		tableProps.set("class", "dataset-property-table");
    	//markdown.tableStart(tableProps);
    	String [] tableHeaders = {
    		"Property",
    		"Description"
    	};
    	markdown.tableHeaders ( tableHeaders );
    	markdown.tableRowStart();
    	markdown.tableCell("title");
  		markdown.tableCell ( dataset.getTitle() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "identifier" );
  		markdown.tableCell ( dataset.getIdentifier() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "description" );
  		markdown.tableCell ( dataset.getDescription() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "issued" );
  		markdown.tableCell ( dataset.getIssued() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "modified" );
  		markdown.tableCell ( dataset.getModified() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "version" );
  		markdown.tableCell ( dataset.getVersion() );
    	markdown.tableRowEnd();
    	markdown.tableRowStart();
  		markdown.tableCell ( "keyword" );
  		markdown.tableCell ( StringUtil.toString(dataset.getKeyword(), ",") );
    	markdown.tableRowEnd();
    	markdown.write("\n");
    	
    	//markdown.write("\n  </div> <!-- dataset-property-table-container -->\n");
    	//markdown.write("  </div> <!-- dataset-image-and-property-container -->\n");
    	
    	// Add the "Dataset Publisher" section.

  		markdown.heading(2, "Dataset Publisher");
    	// Create the publisher table.
    	String [] tableHeaders2 = {
    		"Property",
    		"Description"
    	};
    	markdown.tableHeaders ( tableHeaders2 );
  		markdown.tableRowStart();
  		markdown.tableCell ( "name" );
  		markdown.tableCell ( dataset.getPublisher().getName() );
  		markdown.tableRowEnd();
  		markdown.tableRowStart();
  		markdown.tableCell ( "mbox" );
  		markdown.tableCell ( dataset.getPublisher().getMbox() );
  		markdown.tableRowEnd();
    	markdown.write("\n");

    	// Add the "Dataset Details" selection if a 'dataset-details.md' Markdown file was provided.
    	String markdownFile = dataset.getLocalMarkdownPath();
    	if ( IOUtil.fileExists(markdownFile) && !markdownFile.isEmpty() ) {
    		markdown.heading(2, "Dataset Details");
    		// Read the MarkDown file into memory.
    		StringBuilder b = IOUtil.fileToStringBuilder(markdownFile);
    		// Write the insert to the Markdown file.
    		markdown.write(b.toString());
    		markdown.write("\n");
    	}

    	//markdown.write("\n</div> <!-- class=\"dataset-content-container\" -->\n");
    	if ( (datasetIndexFooterFile != null) && datasetIndexFooterFile.toUpperCase().endsWith(".MD") ) {
    		// Insert the footer content.
    		Message.printStatus(2,routine,"Insert file into <footer>: " + datasetIndexFooterFile);
    		markdown.comment("Start inserting file: " + datasetIndexFooterFile);
    		markdown.write(IOUtil.fileToStringBuilder(datasetIndexFooterFile).toString());
    		markdown.comment("End inserting file: " + datasetIndexFooterFile);
    	}
		//fout = new PrintWriter ( new FileOutputStream ( datasetIndexFile ) );
		//fout.print(markdown.getMarkdown());
	}
	catch ( Exception e ) {
		String message = "Error writing \"" + datasetIndexFile + "\" (" + e + ").";
		Message.printWarning( 2, routine, message );
		Message.printWarning( 3, routine, e );
		throw new RuntimeException (message);
	}
	finally {
		if ( markdown != null )  {
			markdown.closeFile();
		}
		//if ( fout != null ) {
			//fout.close();
		//}
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
	
	// Also download the dataset details markdown file if it exists (allow error if the optional file does not exist).
	String mdTempName = tempName.replace("dataset.json","dataset-details.md");
	String mdS3FileKey = s3FileKey.replace("dataset.json", "dataset-details.md");
	try {
		problems.clear();
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
		Message.printStatus(2, routine, "Key does not exist for markdown file from S3 \"" + mdS3FileKey + "\" to \"" + mdTempName + "\" - will not insert dataset-details.md");
		if ( Message.isDebugOn ) {
			Message.printWarning(3, routine, e );
		}
	}

	// Also download the dataset image file if it exists (allow error if the optional file does not exist).
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
		Message.printStatus(2, routine, "Key does not exist for dataset image file from S3 \"" + imageS3FileKey + "\" - will not use dataset image file.");
		if ( Message.isDebugOn ) {
			Message.printWarning(3, routine, e );
		}
	}
	
	return dataset;
}

/**
 * Download a dataset file and add to the dataset list.
 * @param datasetList the dataset list (may be the list to process with no parents, or the full list, with parents)
 * @param allDatasetList the dataset list (contains all datasets but parent datasets are not yet processed)
 * @param tm the S3TransferManager used for downloads
 * @param bucket the S3 bucket for downloads
 * @param startingPrefix the S3 bucket starting prefix for downloads
 * @param s3Object S3 object for dataset.json file, retrieved from bucket listing
 * @param timezone time zone string
 * @param zoneId time zone ID
 * @param status command status object
 * @param command_tag tag used with command status
 * @param warning_count initial count of warnings, returned as an updated value
 * @return the warning count after updating
 */
private int downloadDatasetFileAndAddToList (
	List<DcatDataset> datasetList,
	List<DcatDataset> allDatasetList,
    S3TransferManager tm,
    String bucket,
    String startingPrefix,
	S3Object s3Object,
    String timezone,
    ZoneId zoneId,
	CommandStatus status,
	String command_tag,
	int warning_count
	) {
	String routine = getClass().getSimpleName() + ".dataloadDatasetAndAddToList";
	String message;
	int warning_level = 2;

	// Download the dataset file to a temporary file.
    Message.printStatus(2, routine, "Downloading S3 object with key: \"" + s3Object.key() + "\"" );
    // Download the file and return a preliminary object with local file names but object is not filled out.
    DcatDataset datasetLocalInfo = downloadDatasetFile ( tm, bucket, startingPrefix, s3Object.key(), status, command_tag, warning_count );
    	    			
    // Get the folder for use with invalidation.
    File s3File = new File(s3Object.key());
    String s3Folder = s3File.getParent();
    if ( !s3Folder.startsWith("/") ) {
    	// Make sure the folder starts with a slash for CloudFront invalidation.
    	s3Folder = "/" + s3Folder;
    }
    	    			
    // Read the dataset JSON file into an object:
    // - the dataset properties may be incomplete if a child but will process below
    Message.printStatus(2, routine, "Creating dataset object from file." );
   	DcatDataset dataset = null;
    try {
    	dataset = readDatasetFile ( datasetLocalInfo.getLocalPath() );
    }
    catch ( Exception e ) {
    	dataset = null;
    }
    if ( dataset == null ) {
   	    message = "Error creating dataset from file \"" + datasetLocalInfo.getLocalPath() + "\".";
   	    Message.printWarning ( warning_level, 
   	    	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
   	    status.addToLog(CommandPhaseType.RUN,
   	    	new CommandLogRecord(CommandStatusType.FAILURE,
   	    		message, "Confirm that the dataset.json file is correct."));
   	    // Can't continue processing.
   	    return warning_count;
    }
    // If here the dataset is not null:
    // - set AWS data in the dataset for use later
    // - set the local files from the 'datasetLocalInfo' preliminary dataset
   	// - add the dataset to the main list (may be removed later)
    	    			
    // Set the S3 information.
    dataset.setCloudPath(s3Object.key());
    dataset.setCloudSizeKb(s3Object.size());
   	if ( s3Object.owner() == null ) {
   	    dataset.setCloudOwner("");
   	}
   	else {
   	    dataset.setCloudOwner(s3Object.owner().displayName());
   	}
   	int behaviorFlag = 0;
   	dataset.setCloudLastModified( new DateTime(OffsetDateTime.ofInstant(s3Object.lastModified(), zoneId), behaviorFlag, timezone));

   	// Set the local dataset information:
   	// - set based on what was downloaded
   	// - the information is used later
   	dataset.setLocalPath(datasetLocalInfo.getLocalPath());
   	//dataset.setLocalMarkdownPath(datasetLocalInfo.getLocalMarkdownPath());
   	dataset.setLocalMarkdownPath(datasetLocalInfo.getLocalMarkdownPath());
   	//dataset.setLocalImagePath(datasetLocalInfo.getLocalImagePath());
   	dataset.setLocalImagePath(datasetLocalInfo.getLocalImagePath());

   	// Add to the preliminary dataset list to process.
   	if ( datasetList != null ) {
   		datasetList.add(dataset);
   	}
   	
   	// Also add to the full list (which will also contain parent datasets in a later step).
   	if ( allDatasetList != null ) {
   		allDatasetList.add(dataset);
   	}
   	
   	return warning_count;
}

/**
 * Download necessary parent dataset files for datasets that have parent datasets.
 * This is necessary because the initial search/download logic only matches the requested dataset.
 * @param datasetList list of datasets to process
 * @param allDatasetList list of all datasets, which contains 'datsetList' and necessary parent datasets to process, will be updated
 */
private int downloadParentDatasetFiles (
	List<DcatDataset> datasetList,
	List<DcatDataset> allDatasetList,
	S3Client s3,
    S3TransferManager tm,
    String bucket,
    String timezone,
    ZoneId zoneId,
	CommandStatus status,
	String command_tag,
	int warning_count
	) {
	String routine = getClass().getSimpleName() + ".downloadParentDatasetFiles";
   	Message.printStatus(2, routine, "Downloading parent datasets referenced in datasets." ); 
	// Always add to the full list of datset files:
	// - this is needed to find the parent dataset file for the specific dataset
	for ( DcatDataset dataset : datasetList ) {
		String parentFile = dataset.getParentDatasetFile();
		if ( (parentFile != null) && !parentFile.isEmpty() ) {
			// Dataset has a parent:
			// - first search for the dataset in all the datasets
			// - if not found, download the parent files.
			DcatDataset parentDataset = findParentDataset ( allDatasetList, dataset );
			if ( parentDataset == null ) {
				// Read the parent dataset:
				// - must download the S3 object for the specific parent dataset file
				// - the prefix is the parent's folder
    	    	// Key is the parent folder's dataset.json
    	    	File f = new File(dataset.getCloudPath());
    	    	String startingPrefix = f.getParentFile().getParent().replace("\\", "/");
    	    	String parentDatasetJsonKey = startingPrefix + "/dataset.json";
    	    	Message.printStatus(2, routine, "Downloading parent dataset using prefix: \"" + startingPrefix + "\"");
    	    	Message.printStatus(2, routine, "  Trying to match key: \"" + parentDatasetJsonKey + "\"");
   	        	software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = ListObjectsV2Request
    	    		.builder()
    	    		.fetchOwner(Boolean.TRUE)
    	    		.prefix(startingPrefix)
    	    		.bucket(bucket)
    	    		.maxKeys(10); // Use to limit the query to only the parent folder's files (subdirectories are listed after parent folder).
    	    	ListObjectsV2Request request = builder.build();
    	    	ListObjectsV2Response response = s3.listObjectsV2(request);
    	    	boolean found = false;
   	    		for ( S3Object s3Object : response.contents() ) {
   	    			// Only include the dataset file for the matching parent dataset.
   	    			Message.printStatus(2, routine, "  Checking object key: \"" + s3Object.key() + "\"");
    				if ( s3Object.key().equals(parentDatasetJsonKey) ) {
   	    			    Message.printStatus(2, routine, "  Matched object key: \"" + s3Object.key() + "\"");
   	    			    found = true;
    					warning_count = downloadDatasetFileAndAddToList (
    	    		 		null,
    	    		 		allDatasetList,
    	    		 		tm,
    	    		 		bucket,
    	    		 		startingPrefix,
    	    		 		s3Object,
    	    		 		timezone,
    	    		 		zoneId,
    	    		 		status,
	   	    		 		command_tag,
	   	    		 		warning_count
    	    	 		);
    					// Don't need to keep checking.
    					break;
    				}
   	    		}
   	    		if ( !found ) {
    			    Message.printStatus(2, routine, "  DID NOT MATCH object key." );
   	    		}
			}
		}
	}
	return warning_count;
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
	return (new AwsS3Catalog_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
 * Find a parent dataset given a child dataset.
 * This is called if it is known that parent is used ("parentDatasetFile" is set in the child's dataset.json file).
 * @param datasetList list of datasets including parents (because parent datasets should be listed before children)
 */
private DcatDataset findParentDataset ( List<DcatDataset> datasetList, DcatDataset dataset ) {
	String routine = getClass().getSimpleName() + ".findParentDataset";
	DcatDataset parentDataset = null;
	// Get the parent folder for the current dataset key.
	String datasetKey = dataset.getCloudPath();
	String parentKey = null;
	// First get the current folder for the key (e.g., 'latest' or '2022-06-29'):
	// - should work OK since it is similar to a relative file system path
	Message.printStatus(2, routine, "Starting with dataset key: " + datasetKey);
	File f = new File(datasetKey);
	String currentFolder = f.getParent();
	Message.printStatus(2, routine, "Current folder for dataset key: " + currentFolder);
	// Adjust the current folder to get the path to the parent dataset file.
	try {
		parentKey = IOUtil.adjustPath(currentFolder, dataset.getParentDatasetFile());
	}
	catch ( Exception e ) {
		Message.printWarning(3,routine,"Cannot get adjusted parent dataset file path using folder \"" + currentFolder
			+ "\" and \"" + dataset.getParentDatasetFile() + "\".");
		return parentDataset;
	}
	// Search the existing datasets to find a matching key for the parent:
	// - the local path has \ on windows so have to do a replace before comparing
	Message.printStatus(2,routine,"Have " + datasetList.size() + " datasets to search for parent." );
	for ( DcatDataset dataset2 : datasetList ) {
		String parentKey2 = parentKey.replace("\\", "/");
		Message.printStatus(2,routine,"Comparing \"" + parentKey2 + "\" and \"" + dataset2.getCloudPath() + "\"" );
		if ( dataset2.getCloudPath().replace("\\","/").equals(parentKey2) ) {
			// Found the parent dataset.
			parentDataset = dataset2;
			break;
		}
	}
	return parentDataset;
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
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
		if ( (profile == null) || profile.isEmpty() ) {
			if ( (profile == null) || profile.isEmpty() ) {
				message = "The profile is not specified and unable to determine the default.";
				Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Make sure that the AWS configuration file exists with at least one profile: " +
						AwsToolkit.getInstance().getAwsUserConfigFile() ) );
			}
		}
	}
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
	// Bucket must be final because of lambda use below.
	String bucket0 = parameters.getValue ( "Bucket" );
	final String bucket = TSCommandProcessorUtil.expandParameterValue(processor,this,bucket0);
	String StartingPrefix = parameters.getValue ( "StartingPrefix" );
	StartingPrefix = TSCommandProcessorUtil.expandParameterValue(processor,this,StartingPrefix);
	if ( (StartingPrefix == null) || StartingPrefix.isEmpty() ) {
		StartingPrefix = "/";
	}
	String ProcessSubdirectories = parameters.getValue ( "ProcessSubdirectories" );
	boolean processSubdirectories = false; // Default.
	if ( (ProcessSubdirectories != null) && ProcessSubdirectories.equalsIgnoreCase("true")) {
	    processSubdirectories = true;
	}
	boolean doTable = false;
	String OutputTableID = parameters.getValue ( "OutputTableID" );
	OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
	if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
		doTable = true;
	}
	//String CatalogFile = parameters.getValue ( "CatalogFile" ); // Expand below.
	//String CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" ); // Expand below.
	String UploadCatalogFiles = parameters.getValue ( "UploadCatalogFiles" );
	boolean doUploadCatalog = false; // Default.
	if ( (UploadCatalogFiles != null) && UploadCatalogFiles.equalsIgnoreCase("true")) {
	    doUploadCatalog = true;
	}
	String DistributionId = parameters.getValue ( "DistributionId" );
	DistributionId = TSCommandProcessorUtil.expandParameterValue(processor,this,DistributionId);
	String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" ); // Expand below.
	String DatasetIndexHeadFile = parameters.getValue ( "DatasetIndexHeadFile" ); // Expand below.
	String DatasetIndexBodyFile = parameters.getValue ( "DatasetIndexBodyFile" ); // Expand below.
	String DatasetIndexFooterFile = parameters.getValue ( "DatasetIndexFooterFile" ); // Expand below.
	String UploadDatasetFiles = parameters.getValue ( "UploadDatasetFiles" );
	boolean doUploadDataset = false; // Default.
	if ( (UploadDatasetFiles != null) && UploadDatasetFiles.equalsIgnoreCase("true")) {
	    doUploadDataset = true;
	}
	String KeepFiles = parameters.getValue ( "KeepFiles" );
	boolean keepFiles = false; // Default.
	if ( (KeepFiles != null) && KeepFiles.equalsIgnoreCase("true")) {
	    keepFiles = true;
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

	message = "The AwsCatalog command is disabled.";
	Message.printWarning(warning_level,
		MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
		message, "Use the AwsS3LandingPage command to create dataset landing pages." ) );
	
  	// Create a session with the credentials:
	// - this works for S3 with the default region
   	AwsSession awsSession = new AwsSession(profile);
   	
	ProfileCredentialsProvider credentialsProvider0 = null;
	credentialsProvider0 = ProfileCredentialsProvider.create(profile);
	ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

	String distributionId = null;
	// CloudFront seems to use the 'aws-global' region always.
	String cloudFrontRegion = "aws-global";
	if ( (DistributionId != null) && !DistributionId.isEmpty() ) {
		// Get the distribution Id given input.
		String commentPattern = DistributionId;
		// Don't match the tags.
		StringDictionary tagDict = null;
		String distributionId2 = null;
		if ( DistributionId.indexOf("*") >= 0 ) {
			// DistributionId was specified with comment pattern:
			// - convert to Java wildcard
			commentPattern = DistributionId.replace("*", ".*");
		}
		else {
			// Assume a specific Distribution ID was specified.
			commentPattern = null;
			distributionId2 = DistributionId;
		}
		distributionId = AwsToolkit.getInstance().getCloudFrontDistributionId(
			awsSession, cloudFrontRegion, distributionId2, tagDict, commentPattern);
	}

	// The following is used to create a temporary table before outputting to a file.
	//boolean useTempTable = false;
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
   	List<String> invalidationPathList = new ArrayList<>();

   	// TODO smalers 2023-02-10 enable the command when have time to do it right.
	boolean isEnabled = false;
	try {
		if ( isEnabled ) {

		// Create the table if it does not exist.

	    // Make sure the table has the columns that are needed.
	    if ( commandPhase == CommandPhaseType.RUN ) {
	    	boolean append = false;
    		// Column numbers are used later.
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
    	    	// TODO smalers 2022-05-31 for now use UTC time.
    	    	String timezone = "Z";
    	    	ZoneId zoneId = ZoneId.of("Z");
    	    	boolean done = false;
    	    	int maxObjects = 10000; // Default for now.
    	    	int objectCount = 0;
    	    	// File pattern using Java regular expression.
    	    	String dcatFilePattern = ".*dataset.json";
    	    	// List of all datasets, needed to create the main catalog and also find parent dataset files.
    	    	List<DcatDataset> allDatasetList = new ArrayList<>();
    	    	// List of datasets to process, may contain only a single dataset and not its parent.
    	    	List<DcatDataset> datasetList = new ArrayList<>();

    	    	// Get the initial list of datasets:
    	    	// - have to do additional processing later because parent and child dataset order is not guaranteed

    			Message.printStatus(2, routine, "Download dataset files from S3 and create initial dataset objects.");

    			// Key for the dataset.json file in the starting prefix (or root if prefix is not specified):
    			// - the key will not start with /
    			String startingPrefixDatasetJson = null;
    			if ( StartingPrefix.equals("/") )  {
    				// Dataset is at the root of the bucket - no separator needed.
    				startingPrefixDatasetJson = "dataset.json";
    			}
    			else {
    				startingPrefixDatasetJson = StartingPrefix + "/dataset.json";
    				if ( startingPrefixDatasetJson.startsWith("/") ) {
    					// Remove the leading slash.
    					startingPrefixDatasetJson = startingPrefixDatasetJson.substring(1);
    				}
    			}

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
 	    			
    	    			// Check whether processing subdirectories:
    	    			// - if false, only process the top-level dataset
    	    			if ( !processSubdirectories ) {
    	    				Message.printStatus ( 2, routine, "Checking S3 key to skip subdirectories: " + s3Object.key() );
    	    				// The key will not start with /.
    	    				if ( !s3Object.key().equals(startingPrefixDatasetJson) ) {
    	    					// Not the top level dataset for the starting prefix.
    	    					continue;
    	    				}
    	    			}

    	    			// Download dataset and add to the list.
    	    			
    	    			warning_count = downloadDatasetFileAndAddToList (
    	    				datasetList,
    	    				allDatasetList,
    	    				tm,
    	    				bucket,
    	    				StartingPrefix,
    	    				s3Object,
    	    				timezone,
    	    				zoneId,
    	    				status,
	   	    				command_tag,
	   	    				warning_count
    	    			);
    	    		}
    	    		if ( response.nextContinuationToken() == null ) {
    	    			done = true;
    	    		}
    	    		request = request.toBuilder()
   	    				.continuationToken(response.nextContinuationToken())
   	    				.build();
    	    	}
    	    	
    	    	// Get the list of parent datasets, as necessary:
    	    	// - the above may have downloaded a specific dataset but not its parent file
   	    				
    	    	warning_count = downloadParentDatasetFiles (
    	    		datasetList,
    	    		allDatasetList,
    	    		s3,
    	    		tm,
    	    		bucket,
    	    		timezone,
    	    		zoneId,
    	    		status,
	   	    		command_tag,
	   	    		warning_count
    	    	);
    	    	
    	    	// Loop through the list of datasets and do final processing:
    	    	// - initial datasets will have been processed but parent/child will not have been checked
    	    	
    			Message.printStatus(2, routine, "Process dataset objects.");

    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;
    	    	
    	    	if ( datasetList.size() == 0 ) {
    	    		// Likely a problem in the input or dataset files.
					message = "No dataset.json files were found.";
  						Message.printWarning ( warning_level, 
							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
  						status.addToLog(CommandPhaseType.RUN,
  							new CommandLogRecord(CommandStatusType.WARNING,
							message, "Check the starting prefix and that 'dataset.json' files exist in the S3 folder(s)."));
    	    	}

    	    	for ( DcatDataset dataset : datasetList ) {
    	    		Message.printStatus(2, routine, "Start processing dataset with cloud path=" + dataset.getCloudPath() );

    	    		// Get the folder for S3 storage.
   	    			File s3Path = new File(dataset.getCloudPath());
   	    			String s3Folder = s3Path.getParent();
   	    			if ( !s3Folder.startsWith("/") ) {
   	    				// Make sure the folder starts with a slash for CloudFront invalidation.
   	    				s3Folder = "/" + s3Folder;
   	    			}

    				String parentDatasetFile = dataset.getParentDatasetFile();
    				DcatDataset parentDataset = null;
    				if ( (parentDatasetFile != null) && !parentDatasetFile.isEmpty() ) {
  						Message.printStatus(2, routine, "Dataset has a parent. Will find and copy its properties.");
    					// Get the parent dataset for this dataset:
  						// - use the full list of datasets (use the full dataset list to search)
    					parentDataset = findParentDataset ( allDatasetList, dataset );
    					if ( parentDataset == null ) {
    						// Could not find the parent.  This is a problem.
    						message = "Dataset is configured to have a parent but could not find the parent dataset for S3 key: " + dataset.getCloudPath();
    						File f = new File(dataset.getCloudPath());
    						Message.printWarning ( warning_level, 
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog(CommandPhaseType.RUN,
    							new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Confirm that the dataset.json file is correct and that a 'dataset.json' (main dataset) file exists in S3 with key: " +
    								f.getParentFile().getParent().replace("\\", "/") ));
    						// Can't continue processing.
    						continue;
    					}
    					else {
    						// Copy parent dataset properties into this dataset.
    						Message.printStatus(2, routine, "Copying parent dataset properties to child dataset.");
    						dataset.copyParent ( parentDataset );
    					}
    				}

  	    			// Output to table and/or file, as requested.
  	    			if ( table != null ) {
   	    				if ( !allowDuplicates ) {
   	    					// Try to match the object key, which is the unique identifier.
   	    					rec = table.getRecord ( objectKeyCol, s3Path.toString() );
   	    				}
   	    				if ( rec == null ) {
   	    					// Create a new record.
   	    					rec = table.addRecord(table.emptyRecord());
   	    				}
   	    				// Set the data in the record.
   	    				rec.setFieldValue(objectKeyCol,s3Path);
   	    				rec.setFieldValue(objectSizeKbCol,dataset.getCloudSizeKb());
   	    				rec.setFieldValue(objectOwnerCol,dataset.getCloudOwner());
   	    				rec.setFieldValue(objectLastModifiedCol, dataset.getCloudLastModified());
   	    				// Increment the count of objects processed.
   	    				++objectCount;
   	    			}

   	    			// Create the index if requested.
   	    			// Create the dataset index.html landing page file.
   	    			String datasetIndexFile = null;
   	    			if ( (DatasetIndexFile != null) && !DatasetIndexFile.isEmpty() ) {
   	    				boolean doIndex = true;

   	    				// Do checks for required data needed for index.
   	    				if ( (dataset.getTitle() == null) || dataset.getTitle().isEmpty() ) {
   	    					message = "Dataset does not contain 'title' - file may be invalid - skipping index.";
    						Message.printWarning ( warning_level, 
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog(CommandPhaseType.RUN,
    							new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Confirm that the dataset.json file is correct."));
    						// Can't continue processing.
    						doIndex = false;
   	    				}

   	    				if ( doIndex ) {
    	    				// Want to create the dataset index file.
    	    				if ( DatasetIndexFile.equalsIgnoreCase("Temp") ) {
    	    					// Use a temporary file similar to the dataset.json path, which was previously determined in the temporary folder.
    	    					//datasetIndexFile = IOUtil.tempFileName() + "-index.html";
    	    					// Create as the same name as the dataset.json file but use index.html
    	    					//datasetIndexFile = datasetLocalInfo.getLocalPath().replace("dataset.json","index.html");
    	    					datasetIndexFile = dataset.getLocalPath().replace("dataset.json","index.html");
    	    				}
    	    				else {
    	    					// Use the given filename, which can be easier to troubleshoot.
    	    					datasetIndexFile = IOUtil.verifyPathForOS(
    	    						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            						TSCommandProcessorUtil.expandParameterValue(processor,this,DatasetIndexFile)));
    	    				}
    	    				String datasetIndexHeadFile = null;
    	    				if ( DatasetIndexHeadFile != null ) {
    	    					datasetIndexHeadFile = IOUtil.verifyPathForOS(
    	    						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            						TSCommandProcessorUtil.expandParameterValue(processor,this,DatasetIndexHeadFile)));
    	    					if ( !IOUtil.fileExists(datasetIndexHeadFile) ) {
    	    						// Warn and set to null to disable the insert.
    	    						message = "Dataset index <head> file does not exist: " + datasetIndexHeadFile;
    								Message.printWarning ( warning_level, 
    									MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    								status.addToLog(CommandPhaseType.RUN,
    									new CommandLogRecord(CommandStatusType.FAILURE,
    										message, "Confirm that the insert file is correct."));
    	    					}
    	    				}
    	    				String datasetIndexBodyFile = null;
    	    				if ( DatasetIndexBodyFile != null ) {
    	    					datasetIndexBodyFile = IOUtil.verifyPathForOS(
    	    						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            						TSCommandProcessorUtil.expandParameterValue(processor,this,DatasetIndexBodyFile)));
    	    					if ( !IOUtil.fileExists(datasetIndexBodyFile) ) {
    	    						// Warn and set to null to disable the insert.
    	    						message = "Dataset index <body> file does not exist: " + datasetIndexBodyFile;
    								Message.printWarning ( warning_level, 
    									MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    								status.addToLog(CommandPhaseType.RUN,
    									new CommandLogRecord(CommandStatusType.FAILURE,
    										message, "Confirm that the insert file is correct."));
    	    					}
    	    				}
    	    				String datasetIndexFooterFile = null;
    	    				if ( DatasetIndexFooterFile != null ) {
    	    					datasetIndexFooterFile = IOUtil.verifyPathForOS(
    	    						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            						TSCommandProcessorUtil.expandParameterValue(processor,this,DatasetIndexFooterFile)));
    	    					if ( !IOUtil.fileExists(datasetIndexFooterFile) ) {
    	    						// Warn and set to null to disable the insert.
    	    						message = "Dataset index footer file does not exist: " + datasetIndexFooterFile;
    								Message.printWarning ( warning_level, 
    									MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    								status.addToLog(CommandPhaseType.RUN,
    									new CommandLogRecord(CommandStatusType.FAILURE,
    										message, "Confirm that the insert file is correct."));
    	    					}
    	    				}
    	    				String indexExt = null;
    	    				if ( datasetIndexFile.toUpperCase().endsWith(".HTML") ) {
    	    					// Want to create an HTML file.
    	    					createDatasetIndexFileHtml ( dataset, parentDataset,
    	    						datasetIndexFile, datasetIndexHeadFile, datasetIndexBodyFile, datasetIndexFooterFile, doUploadDataset );
    	    					indexExt = "html";
    	    				}
    	    				else if ( datasetIndexFile.toUpperCase().endsWith(".MD") ) {
    	    					// Else want to create a Markdown file.
    	    					createDatasetIndexFileMarkdown ( dataset, parentDataset,
    	    						datasetIndexFile, datasetIndexHeadFile, datasetIndexBodyFile, datasetIndexFooterFile, doUploadDataset );
    	    					indexExt = "md";
    	    				}
    	    				else {
    	    					message = "Dataset index file does not use extension .html or .md - cannot create the index file.";
    							Message.printWarning ( warning_level, 
    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    							status.addToLog(CommandPhaseType.RUN,
    								new CommandLogRecord(CommandStatusType.FAILURE,
    									message, "Confirm that the index file name is correct."));
    	    				}
    	    				if ( indexExt != null ) {
    	    					// Have an index file to upload.
    	    					tempfileList.add(datasetIndexFile);
    	    					// Upload the index to the S3 bucket:
    	    					// - store in the same folder as the dataset file:
    	    					// - also keep track of the file so it can be invalidated
    	    					String s3UploadKey = null;
    	    					if ( doUploadDataset ) {
    	    						// Upload the dataset index file:
    	    						// - the image file used in the index will already have been uploaded
    	    						// - invalidation paths start with /
    	    						s3UploadKey = s3Folder + "/index." + indexExt;
    	    						if ( !s3Folder.startsWith("/") ) {
    	    							s3UploadKey = "/" + s3UploadKey;
    	    						}
    	    						s3UploadKey = s3UploadKey.replace("\\", "/");
    	    						try {
    	    							warning_count = uploadDatasetIndexFile ( tm, bucket, datasetIndexFile, s3UploadKey, status, command_tag, warning_count );
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
    	    					invalidationPathList.add(s3UploadKey);
    	    				}
    	    			}
    	    		}
    	    	}
    	    	
    	    } // end doCatalog.
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
		} // End isEnabled
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
        if ( doUploadDataset && (invalidationPathList.size() > 0) && (distributionId != null) ) {
        	// Invalidate the list of uploaded files.
        	Message.printStatus(2, routine, "Invalidating " + invalidationPathList.size() + " uploaded files.");
        	// Invalidate the distribution:
        	// - use the aws-global region because that seems to be where distributions live
        	
        	// Adjust the initial invalidation list to make sure that sufficient variations are handled:
        	// - otherwise, 'folder', 'folder/', and 'folder/index.html' URL requests may not all be up to date
        	List<String> invalidationPathList2 = new ArrayList<>();
        	List<String> invalidationPathList3 = new ArrayList<>();
        	List<String> invalidationPathList4 = new ArrayList<>();
        	for ( String path : invalidationPathList ) {
        		// If an index.html file, also invalidate the folder with and without slash.
       			if ( path.endsWith("/index.html") ) {
       				// This may invalidate more files than just the index but ensures that all variations of the index URL
       				// are invalidated and the cost for invalidations does not depend on the number of files.
       				// CloudFront complains if the invalidations overlap (even though these seem distinct),
       				// so handle with separate invalidation calls.
       				invalidationPathList2.add(path);
       				invalidationPathList3.add(path.replace("/index.html", "/"));
       				invalidationPathList4.add(path.replace("/index.html", ""));
       			}
       			else {
       				invalidationPathList2.add(path);
        		}
        	}
        	// Use the current time to help ensure a unique caller reference.
        	DateTime dt = new DateTime ( DateTime.DATE_CURRENT);
		    String callerReference = "AwsS3Catalog-" + TimeUtil.formatDateTime(dt, "%Y-%m-%dT%H:%M:%S");
        	AwsToolkit.getInstance().invalidateCloudFrontDistribution(awsSession, cloudFrontRegion,
        		distributionId, invalidationPathList2,
        		callerReference);
        	if ( invalidationPathList3.size() > 0 ) {
        		AwsToolkit.getInstance().invalidateCloudFrontDistribution(awsSession, cloudFrontRegion,
        			distributionId, invalidationPathList3,
        			callerReference + "-2");
        	}
        	if ( invalidationPathList4.size() > 0 ) {
        		AwsToolkit.getInstance().invalidateCloudFrontDistribution(awsSession, cloudFrontRegion,
        			distributionId, invalidationPathList4,
        			callerReference + "-3");
        	}
        	boolean waitForCompletion = true;
        	// If wait for completion is true, wait until the invalidation is complete:
        	// - wait up to 3600 seconds (720 x 5 seconds)
        	int waitTimeout = 3600*1000;
        	int waitMs = 5000;
        	if ( waitForCompletion ) {
   				AwsToolkit.getInstance().waitForCloudFrontInvalidations(awsSession, region, distributionId, waitMs, waitTimeout);
        	}
  	    }
        else {
        	Message.printStatus(2, routine, "Not invalidating files, doUpload=" + doUploadDataset +
        		", invalidationPathList.size=" + invalidationPathList.size() +
        		", distributionId=\"" + distributionId + "\"");
        }
        // Clean up temporary files used for main catalog and index, and dataset index files.
       	for ( String tempfile : tempfileList ) {
       		if ( keepFiles ) {
        		// Keep the temporary files and add to the output files.
        		addOutputFile(new File(tempfile));
        	}
        	else {
        		// Remove the temporary files and DO NOT add to the output files.
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		// General.
		"Profile",
		"Region",
		"Bucket",
		//"MaxKeys",
		"StartingPrefix",
		"ProcessSubdirectories",
		//"MaxObjects",
		// Dataset.
		"DatasetIndexFile",
		"DatasetIndexHeadFile",
		"DatasetIndexBodyFile",
		"DatasetIndexFooterFile",
		"UploadDatasetFiles",
		// Catalog.
		"CatalogFile",
		"CatalogIndexFile",
		"UploadCatalogFiles",
		"DistributionId",
		// Output.
		"OutputTableID",
		"KeepFiles",
		"IfInputNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

/**
 * Upload the dataset index file to S3.
 * @param datasetIndexFile the path to the local index file to upload.
 * @param s3FileKey the key to the file on S3
 * @return the updated warning count
 */
private int uploadDatasetIndexFile ( S3TransferManager tm, String bucket,
	String datasetIndexFile, String s3FileKey,
	CommandStatus status, String commandTag, int warningCount ) {
	String routine = getClass().getSimpleName() + ".uploadDatasetFile";
	// Make sure that the S3 key is valid:
	// - remove leading /
	// - replace Windows \ path separators with /
	s3FileKey = s3FileKey.replace("\\", "/");
	if ( s3FileKey.startsWith("/") ) {
		s3FileKey = s3FileKey.substring(1);
	}
	Message.printStatus(2, routine, "Uploading file from local \"" + datasetIndexFile + "\" to S3 \"" + s3FileKey + "\"");
	List<String> problems = new ArrayList<>();
	AwsToolkit.getInstance().uploadFileToS3 ( tm, bucket, datasetIndexFile, s3FileKey, problems );
	if ( problems.size() == 0 ) {
		Message.printStatus(2, routine, "Success uploading file from local \"" + datasetIndexFile + "\" to S3 \"" + s3FileKey + "\"");
	}
	else {
		int warning_level = 3;
		for ( String problem : problems ) {
    	 	Message.printWarning ( warning_level, 
    	    	MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, problem );
    	    		status.addToLog(CommandPhaseType.RUN,
    	    		new CommandLogRecord(CommandStatusType.FAILURE,
    	    		problem, "Check the command input."));
		}
	}
	return warningCount;
}

/**
Writes the start tags for the HTML index file.
@param html HTMLWriter object.
@param title title for the document.
@throws Exception
*/
private void writeHeadHtml( HTMLWriter html, String title, String cssUrl, String customStyleText, String datasetIndexHeadFile ) throws Exception {
	String routine = getClass().getSimpleName() + ".writeHtmlHead";
    if ( html != null ) {
        html.headStart();
        html.title(title);
    	if ( (datasetIndexHeadFile != null) && datasetIndexHeadFile.toUpperCase().endsWith(".HTML") ) {
    		// Insert the header content.
    		Message.printStatus(2,routine, "Insert file into <head>: " + datasetIndexHeadFile);
    		html.comment("Start inserting file: " + datasetIndexHeadFile);
    		html.write(IOUtil.fileToStringBuilder(datasetIndexHeadFile).toString());
    		html.comment("End inserting file: " + datasetIndexHeadFile);
    	}
    	// Write custom styles last so they take precedence.
        writeStylesHtml(html, cssUrl, customStyleText);
        html.headEnd();
    }
}

/**
Writes the start tags for the Markdown index file.
@param markdown MarkdownWriter object.
@param title title for the document.
@throws Exception
*/
private void writeHeadMarkdown ( MarkdownWriter markdown, String title, String datasetIndexHeadFile ) throws Exception {
	String routine = getClass().getSimpleName() + ".writeHtmlMarkdown";
    if ( markdown != null ) {
        //markdown.headStart();
        //markdown.title(title);
        markdown.heading(1,title);
    	if ( datasetIndexHeadFile != null ) {
    		// Insert the header content.
    		Message.printStatus(2,routine, "Insert file into <head>: " + datasetIndexHeadFile);
    		markdown.comment("Start inserting file: " + datasetIndexHeadFile);
    		markdown.write(IOUtil.fileToStringBuilder(datasetIndexHeadFile).toString());
    		markdown.comment("End inserting file: " + datasetIndexHeadFile);
    	}
    	// Write custom styles last so they take precedence.
        //writeStylesHtml(html, cssUrl, customStyleText);
        //markdown.headEnd();
    }
}

/**
Inserts the style attributes for a dataset index.
This was copied from the TSHtmlFormatter since tables are used with time series also.
@throws Exception
*/
private void writeStylesHtml(HTMLWriter html, String cssUrl, String customStyleText )
throws Exception {
	// For now disable the in-lined styles since the overall website should control.
	boolean doInline = false;
	if ( (cssUrl != null) && !cssUrl.isEmpty() ) {
		// Use the CSS provided in a URL, typically shared across a website.
		html.comment("Start inserting dataset landing page css.");
		html.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\">\n");
		html.comment("End inserting dataset landing page css.");
	}
	else if ( doInline ){
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