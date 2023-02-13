// AwsS3LandingPage_Command - This class initializes, checks, and runs the AwsS3LandingPage command.

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
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.s3.S3Client;
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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the AwsS3LandingPage() command.
*/
public class AwsS3LandingPage_Command extends AbstractCommand
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
	public AwsS3LandingPage_Command () {
		super();
		setCommandName ( "AwsS3LandingPage" );
	}

	/**
	 * Add a path to the CloudFront invalidation list.
	 * The path is added only if not already in the list.
	 * The * wildcard should be added by the calling code.
	 * @param cloudFrontPaths the full list of CloudFront paths
	 * @param cloudFrontPath single CloudFront path to add.
	 * A leading / is added if not present.
	 */
	private void addCloudFrontPath ( List<String> cloudFrontPaths, String cloudFrontPath ) {
		if ( !cloudFrontPath.startsWith("/") ) {
			// Prepend /
			cloudFrontPath = "/" + cloudFrontPath;
		}
		if ( cloudFrontPaths == null ) {
			return;
		}
		boolean found = false;
		for ( String cloudFrontPath0 : cloudFrontPaths ) {
			if ( cloudFrontPath.equals(cloudFrontPath0) ) {
				found = true;
				break;
			}
		}
		if ( !found ) {
			cloudFrontPaths.add(cloudFrontPath);
		}
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
	@param commandTag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
	@param warningLevel The warning level to use when printing parse warnings
	(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
	*/
	public void checkCommandParameters ( PropList parameters, String commandTag, int warningLevel )
	throws InvalidCommandParameterException {
		String Profile = parameters.getValue ( "Profile" );
		String Region = parameters.getValue ( "Region" );
    	String Bucket = parameters.getValue ( "Bucket" );
    	//String StartingFolder = parameters.getValue ( "StartingFolder" );
    	String ProcessSubfolders = parameters.getValue ( "ProcessSubfolders" );
    	//String CatalogFile = parameters.getValue ( "CatalogFile" );
    	//String CatalogIndexFile = parameters.getValue ( "CatalogIndexFile" );
		String UploadCatalogFiles = parameters.getValue ( "UploadCatalogFiles" );
    	//String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" );
		String KeepFiles = parameters.getValue ( "KeepFiles" );
		String UploadFiles = parameters.getValue ( "UploadFiles" );
		//String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
   		// CloudFront
   		String InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
   		String CloudFrontDistributionId = parameters.getValue ( "CloudFrontDistributionId" );
   		String CloudFrontComment = parameters.getValue ( "CloudFrontComment" );
   		String CloudFrontWaitForCompletion = parameters.getValue ( "CloudFrontWaitForCompletion" );

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

		if ( (ProcessSubfolders != null) && !ProcessSubfolders.equals("") ) {
			if ( !ProcessSubfolders.equalsIgnoreCase(_False) && !ProcessSubfolders.equalsIgnoreCase(_True) ) {
				message = "The ProcessSubfolders parameter \"" + ProcessSubfolders + "\" is invalid.";
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

		if ( (UploadFiles != null) && !UploadFiles.equals("") ) {
			if ( !UploadFiles.equalsIgnoreCase(_False) && !UploadFiles.equalsIgnoreCase(_True) ) {
				message = "The UploadFiles parameter \"" + UploadFiles + "\" is invalid.";
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

		/*
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
		*/

		// Make sure that only one of the CloudFront distribution ID or comment is specified:
		// - listing does not require the distribution
			if ( (InvalidateCloudFront != null) && (InvalidateCloudFront.length() != 0) ) {
				if ( !InvalidateCloudFront.equalsIgnoreCase(_False) && !InvalidateCloudFront.equalsIgnoreCase(_True)) {
					message = "The value for InvalidateCloudFront (" + InvalidateCloudFront + ") is invalid.";
					warning += "\n" + message;
					status.addToLog ( CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify blank, " + _False + " (default), or " + _True + "." ) );
				}
				if ( InvalidateCloudFront.equalsIgnoreCase(_True) ) {
					if ( ((CloudFrontDistributionId == null) || CloudFrontDistributionId.isEmpty()) &&
						((CloudFrontComment == null) || CloudFrontComment.isEmpty()) ) {
						message = "The CloudFront distribution ID or CloudFront comment must be specified.";
						warning += "\n" + message;
						status.addToLog(CommandPhaseType.INITIALIZATION,
							new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Specify the CloudFront distribution ID or comment."));
					}
					if ( ((CloudFrontDistributionId != null) && !CloudFrontDistributionId.isEmpty()) &&
						((CloudFrontComment != null) && !CloudFrontComment.isEmpty()) ) {
						message = "The CloudFront distribution ID or CloudFront comment must be specified (not both).";
						warning += "\n" + message;
						status.addToLog(CommandPhaseType.INITIALIZATION,
							new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Specify the CloudFront distribution ID or comment."));
					}

					if ( (CloudFrontWaitForCompletion != null) && (CloudFrontWaitForCompletion.length() != 0) &&
        				!CloudFrontWaitForCompletion.equalsIgnoreCase(_False) && !CloudFrontWaitForCompletion.equalsIgnoreCase(_True)) {
						message = "The value for CloudFrontWaitForCompletion (" + CloudFrontWaitForCompletion + ") is invalid.";
						warning += "\n" + message;
						status.addToLog ( CommandPhaseType.INITIALIZATION,
							new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify blank, " + _False + ", or " + _True + " (default)." ) );
					}
				}
			}

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(23);
		validList.add ( "Profile" );
		validList.add ( "Region" );
		validList.add ( "Bucket" );
		validList.add ( "StartingFolder" );
		validList.add ( "ProcessSubfolders" );
		//validList.add ( "CatalogFile" );
		//validList.add ( "CatalogIndexFile" );
		//validList.add ( "UploadCatalogFiles" );
		validList.add ( "DatasetIndexFile" );
		validList.add ( "DatasetIndexHeadFile" );
		validList.add ( "DatasetIndexBodyFile" );
		validList.add ( "DatasetIndexFooterFile" );
		validList.add ( "UploadFiles" );
		//validList.add ( "OutputTableID" );
		validList.add ( "KeepFiles" );
		//validList.add ( "IfInputNotFound" );
			// CloudFront
			validList.add ( "InvalidateCloudFront" );
			validList.add ( "CloudFrontRegion" );
			validList.add ( "CloudFrontDistributionId" );
			validList.add ( "CloudFrontComment" );
			validList.add ( "CloudFrontCallerReference" );
			validList.add ( "CloudFrontWaitForCompletion" );
		warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

		if ( warning.length() > 0 ) {
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(commandTag,warningLevel),warning );
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
    			html.comment("Start inserting body file: " + datasetIndexBodyFile);
    			html.write(IOUtil.fileToStringBuilder(datasetIndexBodyFile).toString());
    			html.comment("End inserting body file into <body>: " + datasetIndexBodyFile);
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
    			html.comment("Start inserting footer file: " + datasetIndexFooterFile);
    			html.write(IOUtil.fileToStringBuilder(datasetIndexFooterFile).toString());
    			html.comment("End inserting footer file: " + datasetIndexFooterFile);
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
    			markdown.comment("Start inserting body file: " + datasetIndexBodyFile);
    			markdown.write(IOUtil.fileToStringBuilder(datasetIndexBodyFile).toString());
    			markdown.comment("End inserting body file into <body>: " + datasetIndexBodyFile);
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
    			markdown.comment("Start inserting footer file: " + datasetIndexFooterFile);
    			markdown.write(IOUtil.fileToStringBuilder(datasetIndexFooterFile).toString());
    			markdown.comment("End inserting footer file: " + datasetIndexFooterFile);
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
	 * Invalidate paths that were updated.
	 */
	private int doCloudFrontInvalidation (
		AwsSession awsSession,
		String region, String cloudFrontRegion, String distributionId, String commentPattern,
		List<String> cloudFrontPaths, String callerReference, boolean waitForCompletion,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doCloudFrontInvalidation";
		String message;
		CommandPhaseType commandPhase = CommandPhaseType.RUN;

		// If the CloudFront region is not specified, use the Region value or default.
		if ( (cloudFrontRegion == null) || cloudFrontRegion.isEmpty() ) {
			cloudFrontRegion = region;
		}

   	   	// Invalidate files in a distribution using one or more paths:
   	   	// - see: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html
   	   	// List the distributions given the input parameters.
   	   	distributionId = AwsToolkit.getInstance().getCloudFrontDistributionId(
   	   		awsSession, cloudFrontRegion, distributionId, commentPattern);
       	boolean doInvalidate = true;
       	if ( distributionId == null ) {
   			message = "Unable to determine CloudFront distribution ID for invalidation.";
   			Message.printWarning(warningLevel,
   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
   			status.addToLog ( commandPhase,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Verify that the distribution ID is valid for the CloudFront region." ) );
   			doInvalidate = false;
       	}
       	if ( cloudFrontPaths.size() == 0 ) {
   			message = "No paths have been specified to invalidate - copy, delete, or upload commands must have failed.";
   			Message.printWarning(warningLevel,
   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
   			status.addToLog ( commandPhase,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Specify invalidation paths." ) );
   			doInvalidate = false;
       	}
       	if ( doInvalidate) {
      		// Invalidate:
       		// - see:  https://stackoverflow.com/questions/28527188/how-to-invalidate-a-fileto-be-refreshed-served-from-cloudfront-cdn-via-java-aw
      		// - exception will be caught below
       		for ( String path : cloudFrontPaths ) {
       			Message.printStatus(2, routine, "Invalidating path \"" + path + "\".");
       		}
       		software.amazon.awssdk.services.cloudfront.model.Paths invalidationPaths =
   				software.amazon.awssdk.services.cloudfront.model.Paths
       			.builder()
       			.items(cloudFrontPaths)
      			.quantity(cloudFrontPaths.size())
       			.build();
       		InvalidationBatch batch = InvalidationBatch
       			.builder()
       			.paths(invalidationPaths)
       			.callerReference(callerReference)
       			.build();

       		Region regionObject = Region.of(cloudFrontRegion);

			// Handle credentials.

			ProfileCredentialsProvider credentialsProvider0 = null;
			credentialsProvider0 = ProfileCredentialsProvider.create(awsSession.getProfile());
			ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

			CloudFrontClient cloudfront = CloudFrontClient.builder()
				.region(regionObject)
				.credentialsProvider(credentialsProvider)
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
			AwsToolkit.getInstance().waitForCloudFrontInvalidations(awsSession, cloudFrontRegion, distributionId, waitMs, waitTimeout);
      	}

       	// Return the updated warning count.
       	return warningCount;
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
		int warningLevel = 3;
		for ( String problem : problems ) {
    	 	Message.printWarning ( warningLevel,
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
 	* @param commandTag tag used with command status
 	* @param warningCount initial count of warnings, returned as an updated value
 	* @return the warning count after updating
 	*/
	private int downloadDatasetFileAndAddToList (
		List<DcatDataset> datasetList,
		List<DcatDataset> allDatasetList,
    	S3TransferManager tm,
    	String bucket,
    	String startingPrefix,
		//S3Object s3Object,
		AwsS3Object s3Object,
    	String timezone,
    	ZoneId zoneId,
		CommandStatus status,
		String commandTag,
		int warningCount
		) {
		String routine = getClass().getSimpleName() + ".dataloadDatasetAndAddToList";
		String message;
		int warningLevel = 2;

		// Download the dataset file to a temporary file.
    	Message.printStatus(2, routine, "Downloading S3 object with key: \"" + s3Object.getKey() + "\"" );
    	// Download the file and return a preliminary object with local file names but object is not filled out.
    	DcatDataset datasetLocalInfo = downloadDatasetFile ( tm, bucket, startingPrefix, s3Object.getKey(), status, commandTag, warningCount );

    	// Get the folder for use with invalidation.
    	File s3File = new File(s3Object.getKey());
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
   	    	Message.printWarning ( warningLevel,
   	    		MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   	    	status.addToLog(CommandPhaseType.RUN,
   	    		new CommandLogRecord(CommandStatusType.FAILURE,
   	    			message, "Confirm that the dataset.json file is correct."));
   	    	// Can't continue processing.
   	    	return warningCount;
    	}
    	// If here the dataset is not null:
    	// - set AWS data in the dataset for use later
    	// - set the local files from the 'datasetLocalInfo' preliminary dataset
   		// - add the dataset to the main list (may be removed later)

    	// Set the S3 information.
    	dataset.setCloudPath(s3Object.getKey());
    	dataset.setCloudSizeKb(s3Object.getSize());
   		if ( s3Object.getOwner() == null ) {
   	    	dataset.setCloudOwner("");
   		}
   		else {
   	    	dataset.setCloudOwner(s3Object.getOwner());
   		}
   		int behaviorFlag = 0;
   		dataset.setCloudLastModified( new DateTime(OffsetDateTime.ofInstant(s3Object.getLastModified(), zoneId), behaviorFlag, timezone));

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

   		return warningCount;
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
		String commandTag,
		int warningCount
		) throws Exception {
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

					/*
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
    	    			.maxKeys(10); // Use to limit the query to only the parent folder's files (subfolders are listed after parent folder).
    	    		ListObjectsV2Request request = builder.build();
    	    		ListObjectsV2Response response = s3.listObjectsV2(request);
    	    		boolean found = false;
    	    		*/

    	    		File f = new File(dataset.getCloudPath());
					String startingPrefix = f.getParentFile().getParent().replace("\\", "/");
    	    		String parentDatasetJsonKey = startingPrefix + "/dataset.json";
					String delimiter = null;
					boolean useDelimiter = false; // Don't use the delimiter so a single folder is used.
					int maxKeys = -1;
					int maxObjects = -1; 
					boolean listFiles = true;
					boolean listFolders = false;
					String regex = ".*dataset.json"; // Only match the dataset metadata.
					List<AwsS3Object> s3Objects = AwsToolkit.getInstance().getS3BucketObjects(
						s3,
						bucket,
						parentDatasetJsonKey,
						delimiter,
						useDelimiter,
						maxKeys,
						maxObjects,
						listFiles,
						listFolders,
						regex );
					
					// Should be one item in the list.

					if ( s3Objects.size() == 1 ) {
    			    	Message.printStatus(2, routine, "  Matched object key: \"" + s3Objects.get(0).getKey() + "\"");
   						warningCount = downloadDatasetFileAndAddToList (
   	    		 			null,
   	    		 			allDatasetList,
   	    		 			tm,
   	    		 			bucket,
   	    		 			startingPrefix,
   	    		 			s3Objects.get(0),
   	    		 			timezone,
   	    		 			zoneId,
   	    		 			status,
   	    		 			commandTag,
   	    		 			warningCount
   	    	 			);
   	    			}
					else {
    			    	Message.printStatus(2, routine, "  DID NOT MATCH object key." );
   	    			}
				}
			}
		}
		return warningCount;
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
		return (new AwsS3LandingPage_JDialog ( parent, this, tableIDChoices )).ok();
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
	@param commandNumber Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommand ( int commandNumber )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( commandNumber, CommandPhaseType.RUN );
	}

	/**
	Run the command in discovery mode.
	@param commandNumber Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommandDiscovery ( int commandNumber )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( commandNumber, CommandPhaseType.DISCOVERY );
	}

	/**
	Run the command.
	@param commandNumber Number of command in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
	*/
	private void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommandInternal", message;
		int warningLevel = 2;
		int logLevel = 3; // Level for non-user messages for log file.
		String commandTag = "" + commandNumber;
		int warningCount = 0;

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
					Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
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
				Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Make sure that the AWS configuration file exists with default region: " +
						AwsToolkit.getInstance().getAwsUserConfigFile() ) );
			}
		}
		// Bucket must be final because of lambda use below.
		String bucket0 = parameters.getValue ( "Bucket" );
		final String bucket = TSCommandProcessorUtil.expandParameterValue(processor,this,bucket0);
		String StartingFolder = parameters.getValue ( "StartingFolder" );
		StartingFolder = TSCommandProcessorUtil.expandParameterValue(processor,this,StartingFolder);
		if ( (StartingFolder == null) || StartingFolder.isEmpty() ) {
			StartingFolder = "/";
		}
		String ProcessSubfolders = parameters.getValue ( "ProcessSubfolders" );
		boolean processSubfolders = false; // Default.
		if ( (ProcessSubfolders != null) && ProcessSubfolders.equalsIgnoreCase("true")) {
	    	processSubfolders = true;
		}
		boolean doTable = false;
		String OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
		if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
			doTable = true;
		}
		String DatasetIndexFile = parameters.getValue ( "DatasetIndexFile" ); // Expand below.
		String DatasetIndexHeadFile = parameters.getValue ( "DatasetIndexHeadFile" ); // Expand below.
		String DatasetIndexBodyFile = parameters.getValue ( "DatasetIndexBodyFile" ); // Expand below.
		String DatasetIndexFooterFile = parameters.getValue ( "DatasetIndexFooterFile" ); // Expand below.
		String UploadFiles = parameters.getValue ( "UploadFiles" );
		boolean doUploadDataset = false; // Default.
		if ( (UploadFiles != null) && UploadFiles.equalsIgnoreCase("true")) {
	    	doUploadDataset = true;
		}
		String KeepFiles = parameters.getValue ( "KeepFiles" );
		boolean keepFiles = false; // Default.
		if ( (KeepFiles != null) && KeepFiles.equalsIgnoreCase("true")) {
	    	keepFiles = true;
		}
		/*
		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    IfInputNotFound = _Warn; // Default
		}
		*/

    	// CloudFront.
		String InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
		boolean invalidateCloudFront = false; // Default.
		if ( (InvalidateCloudFront != null) && InvalidateCloudFront.equalsIgnoreCase(this._True) ) {
			invalidateCloudFront = true;
		}
		String CloudFrontRegion = parameters.getValue ( "CloudFrontRegion" );
		String CloudFrontDistributionId = parameters.getValue ( "CloudFrontDistributionId" );
		CloudFrontDistributionId = TSCommandProcessorUtil.expandParameterValue(processor,this,CloudFrontDistributionId);
		String CloudFrontComment = parameters.getValue ( "CloudFrontComment" );
		CloudFrontComment = TSCommandProcessorUtil.expandParameterValue(processor,this,CloudFrontComment);
		// Convert the comment to a Java pattern.
		String commentPattern = null;
		if ( (CloudFrontComment != null) && !CloudFrontComment.isEmpty() ) {
			commentPattern = CloudFrontComment.replace("*", ".*");
		}
		String CloudFrontCallerReference = parameters.getValue ( "CloudFrontCallerReference" );
		CloudFrontCallerReference = TSCommandProcessorUtil.expandParameterValue(processor,this,CloudFrontCallerReference);
		String callerReference = CloudFrontCallerReference;
		if ( (CloudFrontCallerReference == null) || CloudFrontCallerReference.isEmpty() ) {
			// Default is user-time.
			DateTime dt = new DateTime ( DateTime.DATE_CURRENT);
			callerReference = "TSTool-" + System.getProperty("user.name").replace(" ", "")
				+ "-" + TimeUtil.formatDateTime(dt, "%Y%m%d%H%M%S");
		}
		else {
			// Append current time to ensure uniqueness.
			DateTime dt = new DateTime ( DateTime.DATE_CURRENT);
			callerReference = CloudFrontCallerReference + "-" + TimeUtil.formatDateTime(dt, "%Y-%m-%dT%H:%M:%S");
		}
		String CloudFrontWaitForCompletion = parameters.getValue ( "CloudFrontWaitForCompletion" );
		boolean waitForCompletion = true;
		if ( (CloudFrontWaitForCompletion != null) && CloudFrontWaitForCompletion.equalsIgnoreCase("false") ) {
			waitForCompletion = false;
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
				Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report problem to software support." ) );
			}
		}

		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings about command parameters.";
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(commandTag, ++warningCount), routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		// Handle credentials.

  		// Create a session with the credentials:
		// - this works for S3 with the default region
   		AwsSession awsSession = new AwsSession(profile);

		ProfileCredentialsProvider credentialsProvider0 = null;
		credentialsProvider0 = ProfileCredentialsProvider.create(profile);
		ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

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

   		// List of temporary files to delete.
   		List<String> tempfileList = new ArrayList<>();

 		// List of paths to invalidate for files updated on S3.
		List<String> cloudFrontPaths = new ArrayList<>();
		try {
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
                   			Message.printWarning(warningLevel,
                       			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
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

    	    	// List files from the starting folder:
	    		// - specify the starting prefix to begin the search for datasets

   	        	String prefix = StartingFolder;  // Start of the search.
   	        	String delimiter = "/"; // Delimiter for folders, won't be used for listing unless useDelimiter=true
   	        	boolean useDelimiter = true; // Use the delimiter so a single folder is used.
   	        	if ( processSubfolders ) {
   	        		// Want to process a single folder.
   	        		useDelimiter = false;
   	        	}
   	        	int maxKeys = -1;
   	        	int maxObjects = -1; 
   	        	boolean listFiles = true;
   	        	boolean listFolders = false;
   	        	String regex = ".*dataset.json"; // Only match the dataset metadata.
   	        	List<AwsS3Object> s3Objects = AwsToolkit.getInstance().getS3BucketObjects(
   	        		s3,
   	        		bucket,
   	        		prefix,
   	        		delimiter,
   	        		useDelimiter,
   	        		maxKeys,
   	        		maxObjects,
   	        		listFiles,
   	        		listFolders,
   	        		regex );
   	        	
   	        	Message.printStatus(2, routine, "Read " + s3Objects.size() + " AwsS3Object matching dataset.json.");
   	        	
   	        	// Loop through the found dataset.json files.
   	        	
   	        	// Timezone to use for date/time objects.
    	    	String timezone = "Z";
    	    	ZoneId zoneId = ZoneId.of("Z");
    	    	// List of all datasets, needed to create the main catalog and also find parent dataset files.
    	    	List<DcatDataset> allDatasetList = new ArrayList<>();
    	    	// List of datasets to process, may contain only a single dataset and not its parent.
    	    	List<DcatDataset> datasetList = new ArrayList<>();
   	        	for ( AwsS3Object s3Object : s3Objects ) {

   	    			// Download the dataset file and add to the list.

   	    			warningCount = downloadDatasetFileAndAddToList (
   	    				datasetList,
   	    				allDatasetList,
   	    				tm,
   	    				bucket,
   	    				StartingFolder,
   	    				s3Object,
   	    				timezone,
   	    				zoneId,
   	    				status,
   	    				commandTag,
   	    				warningCount
   	    			);
   	        		
   	        	}

    	    	// Get the list of parent datasets, as necessary:
    	    	// - the above may have downloaded a specific dataset but not its parent file

    	    	warningCount = downloadParentDatasetFiles (
    	    		datasetList,
    	    		allDatasetList,
    	    		s3,
    	    		tm,
    	    		bucket,
    	    		timezone,
    	    		zoneId,
    	    		status,
	   	    		commandTag,
	   	    		warningCount
    	    	);

    	    	// Loop through the list of datasets and do final processing:
    	    	// - initial datasets will have been processed but parent/child will not have been checked

    			Message.printStatus(2, routine, "Process dataset objects.");

    	    	TableRecord rec = null;
    	    	boolean allowDuplicates = false;

    	    	if ( datasetList.size() == 0 ) {
    	    		// Likely a problem in the input or dataset files.
					message = "No dataset.json files were found on S3.";
  						Message.printWarning ( warningLevel,
							MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
  						status.addToLog(CommandPhaseType.RUN,
  							new CommandLogRecord(CommandStatusType.WARNING,
							message, "Check the starting folder and that 'dataset.json' files exist in the S3 folder(s)."));
    	    	}

    	    	for ( DcatDataset dataset : datasetList ) {
    	    		Message.printStatus(2, routine, "Start processing dataset with cloud path=" + dataset.getCloudPath() );

    	    		// Get the folder for S3 storage.
   	    			File s3Path = new File(dataset.getCloudPath());
   	    			String s3Folder = s3Path.getParent();

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
    						Message.printWarning ( warningLevel,
    							MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
   	    			}

   	    			// Create the index if requested.
   	    			// Create the dataset index.html landing page file.
   	    			String datasetIndexFile = null;
   	    			if ( (DatasetIndexFile != null) && !DatasetIndexFile.isEmpty() ) {
   	    				boolean doIndex = true;

   	    				// Do checks for required data needed for index.
   	    				if ( (dataset.getTitle() == null) || dataset.getTitle().isEmpty() ) {
   	    					message = "Dataset does not contain 'title' - file may be invalid - skipping index.";
    						Message.printWarning ( warningLevel,
    							MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    						status.addToLog(CommandPhaseType.RUN,
    							new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Confirm that the dataset.json file is correct."));
    						// Can't continue processing.
    						doIndex = false;
   	    				}

   	    				if ( doIndex ) {
    	    				// Create the dataset index file.
    	    				if ( DatasetIndexFile.equalsIgnoreCase("Temp.html") ) {
    	    					// Use a temporary file similar to the dataset.json path, which was previously determined in the temporary folder.
    	    					//datasetIndexFile = IOUtil.tempFileName() + "-index.html";
    	    					// Create as the same name as the dataset.json file but use index.html
    	    					//datasetIndexFile = datasetLocalInfo.getLocalPath().replace("dataset.json","index.html");
    	    					datasetIndexFile = dataset.getLocalPath().replace("dataset.json","index.html");
    	    				}
    	    				else if ( DatasetIndexFile.equalsIgnoreCase("Temp.md") ) {
    	    					// Use a temporary file similar to the dataset.json path, which was previously determined in the temporary folder.
    	    					//datasetIndexFile = IOUtil.tempFileName() + "-index.html";
    	    					// Create as the same name as the dataset.json file but use index.html
    	    					//datasetIndexFile = datasetLocalInfo.getLocalPath().replace("dataset.json","index.html");
    	    					datasetIndexFile = dataset.getLocalPath().replace("dataset.json","index.md");
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
    								Message.printWarning ( warningLevel,
    									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
    								Message.printWarning ( warningLevel,
    									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
    								Message.printWarning ( warningLevel,
    									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    								status.addToLog(CommandPhaseType.RUN,
    									new CommandLogRecord(CommandStatusType.FAILURE,
    										message, "Confirm that the insert file is correct."));
    	    					}
    	    				}
    	    				String indexExt = null;
    	    				if ( datasetIndexFile.toUpperCase().endsWith(".HTML") ) {
    	    					// Create an HTML file.
    	    					createDatasetIndexFileHtml ( dataset, parentDataset,
    	    						datasetIndexFile, datasetIndexHeadFile, datasetIndexBodyFile, datasetIndexFooterFile, doUploadDataset );
    	    					indexExt = "html";
    	    				}
    	    				else if ( datasetIndexFile.toUpperCase().endsWith(".MD") ) {
    	    					// Else create a Markdown file.
    	    					createDatasetIndexFileMarkdown ( dataset, parentDataset,
    	    						datasetIndexFile, datasetIndexHeadFile, datasetIndexBodyFile, datasetIndexFooterFile, doUploadDataset );
    	    					indexExt = "md";
    	    				}
    	    				else {
    	    					message = "Dataset index file does not use extension .html or .md - cannot create the index file.";
    							Message.printWarning ( warningLevel,
    								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
    	    						s3UploadKey = s3UploadKey.replace("\\", "/");
    	    						try {
    	    							warningCount = uploadDatasetIndexFile ( tm, bucket, datasetIndexFile, s3UploadKey, status, commandTag, warningCount );
    	    							// Upload the index file key as index.html and folder with and without trailing /.
    	    							addCloudFrontPath(cloudFrontPaths, s3UploadKey);
    	    							addCloudFrontPath(cloudFrontPaths, s3UploadKey.replace("/index." + indexExt, "/"));
    	    							addCloudFrontPath(cloudFrontPaths, s3UploadKey.replace("/index." + indexExt, ""));
    	    						}
    	    						catch ( Exception e ) {
    	    							message = "Error uploading file \"" + datasetIndexFile + "\" to S3 key \"" + s3UploadKey + "\" (" + e + ")";
    	    							Message.printWarning ( warningLevel,
    	    								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    	    							Message.printWarning ( 3, routine, e );
    	    							status.addToLog(CommandPhaseType.RUN,
    	    								new CommandLogRecord(CommandStatusType.FAILURE,
    	    									message, "See the log file for details."));
    	    						}
    	    					}
    	    				}
    	    			}
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
			message = "Unexpected error creating dataset landing page (" + e + ").";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
		}
    	finally {
        	if ( doUploadDataset && (cloudFrontPaths.size() > 0) && invalidateCloudFront ) {
        		// Invalidate the list of uploaded files.
        		Message.printStatus(2, routine, "Invalidating " + cloudFrontPaths.size() + " uploaded CloudFront paths.");
        		// Invalidate the distribution:
        		// - use the aws-global region because that seems to be where distributions live
	       		if ( invalidateCloudFront && (cloudFrontPaths.size() > 0) ) {
	       			try {
	       				warningCount = doCloudFrontInvalidation (
	       					awsSession,
	       					region, CloudFrontRegion, CloudFrontDistributionId, commentPattern,
	       					cloudFrontPaths, callerReference, waitForCompletion,
	       					status, logLevel, warningLevel, warningCount, commandTag );
	       			}
	       			catch ( Exception e ) {
        				message = "Error invalidating CloudFront paths \" (" + e + ").";
		   				Message.printWarning ( warningLevel,
		      				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
		   				Message.printWarning ( 3, routine, e );
		   				status.addToLog(CommandPhaseType.RUN,
		     				new CommandLogRecord(CommandStatusType.FAILURE,
			      				message, "See the log file for details."));
	       			}
	       		}
  	    	}
        	else {
        		Message.printStatus(2, routine, "Not invalidating files, doUpload=" + doUploadDataset +
        			", cloudFrontPaths.size=" + cloudFrontPaths.size() );
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
		   				Message.printWarning ( warningLevel,
		      				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
		   				Message.printWarning ( 3, routine, e );
		   				status.addToLog(CommandPhaseType.RUN,
		     				new CommandLogRecord(CommandStatusType.FAILURE,
			      				message, "See the log file for details."));
        			}
        		}
        	}
    	}

    	if ( warningCount > 0 ) {
        	message = "There were " + warningCount + " warnings processing the command.";
        	Message.printWarning ( warningLevel,
            	MessageUtil.formatMessageTag(
            	commandTag, ++warningCount),
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
	public String toString ( PropList parameters ) {
		if ( parameters == null ) {
			return getCommandName() + "()";
		}
		// AWS S3.
		String Profile = parameters.getValue("Profile");
		String Region = parameters.getValue("Region");
		String Bucket = parameters.getValue("Bucket");
		//String MaxKeys = parameters.getValue("MaxKeys");
		// Dataset.
		String DatasetIndexFile = parameters.getValue("DatasetIndexFile");
		String StartingFolder = parameters.getValue("StartingFolder");
		String ProcessSubfolders = parameters.getValue("ProcessSubfolders");
		String KeepFiles = parameters.getValue("KeepFiles");
		String UploadFiles = parameters.getValue("UploadFiles");
		//String MaxObjects = parameters.getValue("MaxObjects");
		String DatasetIndexHeadFile = parameters.getValue("DatasetIndexHeadFile");
		String DatasetIndexBodyFile = parameters.getValue("DatasetIndexBodyFile");
		String DatasetIndexFooterFile = parameters.getValue("DatasetIndexFooterFile");
		// Catalog.
		//String CatalogFile = parameters.getValue("CatalogFile");
		//String CatalogIndexFile = parameters.getValue("CatalogIndexFile");
		//String UploadCatalogFiles = parameters.getValue("UploadCatalogFiles");
		// Output.
		//String OutputTableID = parameters.getValue("OutputTableID");
		//String IfInputNotFound = parameters.getValue("IfInputNotFound");
		// CloudFront.
		String InvalidateCloudFront = parameters.getValue("InvalidateCloudFront");
		String CloudFrontRegion = parameters.getValue("CloudFrontRegion");
		String CloudFrontDistributionId = parameters.getValue("CloudFrontDistributionId");
		String CloudFrontComment = parameters.getValue("CloudFrontComment");
		String CloudFrontCallerReference = parameters.getValue("CloudFrontCallerReference");
		String CloudFrontWaitForCompletion = parameters.getValue("CloudFrontWaitForCompletion");
		//String IfInputNotFound = parameters.getValue("IfInputNotFound");

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
    	if ( (DatasetIndexFile != null) && (DatasetIndexFile.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "DatasetIndexFile=\"" + DatasetIndexFile + "\"");
    	}
		if ( (StartingFolder != null) && (StartingFolder.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "StartingFolder=\"" + StartingFolder + "\"" );
		}
		if ( (ProcessSubfolders != null) && (ProcessSubfolders.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ProcessSubfolders=" + ProcessSubfolders );
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
		/*
		if ( (MaxObjects != null) && (MaxObjects.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "MaxObjects=" + MaxObjects );
		}
		*/
    	if ( (DatasetIndexHeadFile != null) && (DatasetIndexHeadFile.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "DatasetIndexHeadFile=\"" + DatasetIndexHeadFile + "\"");
    	}
    	if ( (DatasetIndexBodyFile != null) && (DatasetIndexBodyFile.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "DatasetIndexBodyFile=\"" + DatasetIndexBodyFile + "\"");
    	}
    	if ( (DatasetIndexFooterFile != null) && (DatasetIndexFooterFile.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "DatasetIndexFooterFile=\"" + DatasetIndexFooterFile + "\"");
    	}
    	/*
    	if ( (OutputTableID != null) && (OutputTableID.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "OutputTableID=\"" + OutputTableID + "\"" );
    	}
    	*/
		/*
		if ( (IfInputNotFound != null) && (IfInputNotFound.length() > 0) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "IfInputNotFound=" + IfInputNotFound );
		}
		*/
    	// CloudFront.
		if ( (InvalidateCloudFront!= null) && !InvalidateCloudFront.isEmpty() ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "InvalidateCloudFront=" + InvalidateCloudFront);
		}
		if ( (CloudFrontRegion != null) && (CloudFrontRegion.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CloudFrontRegion=\"" + CloudFrontRegion + "\"" );
		}
		if ( (CloudFrontDistributionId != null) && !CloudFrontDistributionId.isEmpty() ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CloudFrontDistributionId=\"" + CloudFrontDistributionId + "\"" );
		}
		if ( (CloudFrontComment != null) && !CloudFrontComment.isEmpty() ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CloudFrontComment=\"" + CloudFrontComment + "\"" );
		}
		if ( (CloudFrontCallerReference != null) && !CloudFrontCallerReference.isEmpty() ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CloudFrontCallerReference=\"" + CloudFrontCallerReference + "\"" );
		}
		if ( (CloudFrontWaitForCompletion != null) && !CloudFrontWaitForCompletion.isEmpty() ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CloudFrontWaitForCompletion=" + CloudFrontWaitForCompletion );
		}
		return getCommandName() + "(" + b.toString() + ")";
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
		Message.printStatus(2, routine, "Uploading file from local \"" + datasetIndexFile + "\" to S3 \"" + s3FileKey + "\"");
		List<String> problems = new ArrayList<>();
		AwsToolkit.getInstance().uploadFileToS3 ( tm, bucket, datasetIndexFile, s3FileKey, problems );
		if ( problems.size() == 0 ) {
			Message.printStatus(2, routine, "Success uploading file from local \"" + datasetIndexFile + "\" to S3 \"" + s3FileKey + "\"");
		}
		else {
			int warningLevel = 3;
			for ( String problem : problems ) {
    	 		Message.printWarning ( warningLevel,
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
        	html.addText("");
    		if ( (datasetIndexHeadFile != null) && datasetIndexHeadFile.toUpperCase().endsWith(".HTML") ) {
    			// Insert the header content.
    			Message.printStatus(2,routine, "Insert file into <head>: " + datasetIndexHeadFile);
    			html.comment("Start inserting head file: " + datasetIndexHeadFile);
    			html.write(IOUtil.fileToStringBuilder(datasetIndexHeadFile).toString());
    			html.comment("End inserting head file: " + datasetIndexHeadFile);
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
    			markdown.comment("Start inserting head file: " + datasetIndexHeadFile);
    			markdown.write(IOUtil.fileToStringBuilder(datasetIndexHeadFile).toString());
    			markdown.comment("End inserting head file: " + datasetIndexHeadFile);
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