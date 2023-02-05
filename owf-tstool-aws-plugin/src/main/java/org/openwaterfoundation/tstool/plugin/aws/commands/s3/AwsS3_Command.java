// AwsS3_Command - This class initializes, checks, and runs the Aws_S3() command.

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
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
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
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the AwsS3() command.
*/
public class AwsS3_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{
	/**
	Data members used for DeleteFolders parameter values.
	*/
	protected final String _AllFilesAndFolders = "AllFilesAndFolders";
	protected final String _FolderFiles = "FolderFiles";

	/**
	Data members used for ListBucketObjectsScope parameter values.
	*/
	protected final String _Root = "Root";
	protected final String _Folder = "Folder";
	protected final String _All = "All";

	/**
	Data members used for parameter values.
	*/
	protected final String _False = "False";
	protected final String _True = "True";

	/**
	 * Default delete folder minimum depth.
	 */
    protected final int _DeleteFoldersMinDepth = 3;

	/**
	 * Default maximum number of objects per request.
	 */
	protected final int _MaxKeys = 1000;

	/**
	 * Default maximum number of objects.
	 */
	protected final int _MaxObjects = 2000;

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
	public AwsS3_Command () {
		super();
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
	throws InvalidCommandParameterException {
		String S3Command = parameters.getValue ( "S3Command" );
		String Profile = parameters.getValue ( "Profile" );
		String Region = parameters.getValue ( "Region" );
    	String Bucket = parameters.getValue ( "Bucket" );
    	String CopyKeys = parameters.getValue ( "CopyKeys" );
    	String DeleteKeys = parameters.getValue ( "DeleteKeys" );
    	String DeleteFolders = parameters.getValue ( "DeleteFolders" );
    	String DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
    	String DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
    	String ListBucketObjectsScope = parameters.getValue ( "ListBucketObjectsScope" );
    	//String Prefix = parameters.getValue ( "Prefix" );
    	String Delimiter = parameters.getValue ( "Delimiter" );
    	String ListFiles = parameters.getValue ( "ListFiles" );
    	String ListFolders = parameters.getValue ( "ListFolders" );
    	String MaxKeys = parameters.getValue ( "MaxKeys" );
    	String MaxObjects = parameters.getValue ( "MaxObjects" );
    	// Output
    	String OutputTableID = parameters.getValue ( "OutputTableID" );
    	String OutputFile = parameters.getValue ( "OutputFile" );
    	String AppendOutput = parameters.getValue ( "AppendOutput" );
    	// CloudFront
    	String InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
    	String CloudFrontDistributionId = parameters.getValue ( "CloudFrontDistributionId" );
    	String CloudFrontComment = parameters.getValue ( "CloudFrontComment" );
    	String CloudFrontWaitForCompletion = parameters.getValue ( "CloudFrontWaitForCompletion" );
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
			// Get the default region for checks.
			String region = AwsToolkit.getInstance().getDefaultRegion(Profile);
			if ( (region == null) || region.isEmpty() ) {
				message = "The region is not specified and unable to determine the default.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the region."));
			}
		}

		/* OK to use / if the bucket uses / as the top.
		if ( (Prefix != null) && !Prefix.isEmpty() && Prefix.equals("/") ) {
			message = "The prefix cannot be /.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the prefix as a path ending in / or leave blank to list all files."));
		}
		*/

		if ( (Delimiter != null) && !Delimiter.isEmpty() && (Delimiter.length() != 1) ) {
			message = "The delimiter, if specified, must be 1 character.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the prefix as a single character."));
		}

		if ( (DeleteFoldersScope != null) && !DeleteFoldersScope.equals("") ) {
			if ( !DeleteFoldersScope.equalsIgnoreCase(_AllFilesAndFolders) && !DeleteFoldersScope.equalsIgnoreCase(_FolderFiles) ) {
				message = "The DeleteFoldersScope parameter \"" + DeleteFoldersScope + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _AllFilesAndFolders + " or " + _FolderFiles + " (default)."));
			}
		}

		if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.equals("") && !StringUtil.isInteger(DeleteFoldersMinDepth)) {
			message = "The DeleteFoldersMinDepth parameter (" + DeleteFoldersMinDepth + ") is not an integer.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}

		if ( (ListFiles != null) && !ListFiles.equals("") ) {
			if ( !ListFiles.equalsIgnoreCase(_False) && !ListFiles.equalsIgnoreCase(_True) ) {
				message = "The ListFiles parameter \"" + ListFiles + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
			}
		}

		if ( (ListFolders != null) && !ListFolders.equals("") ) {
			if ( !ListFolders.equalsIgnoreCase(_False) && !ListFolders.equalsIgnoreCase(_True) ) {
				message = "The ListFolders parameter \"" + ListFolders + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
			}
		}

		if ( (MaxKeys != null) && !MaxKeys.isEmpty() && !StringUtil.isInteger(MaxKeys) ) {
			message = "The maximum keys (" + MaxKeys + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify an integer 1 to " + this._MaxKeys + "."));
		}

		if ( (MaxObjects != null) && !MaxObjects.isEmpty() && !StringUtil.isInteger(MaxObjects) ) {
			message = "The maximum objects (" + MaxObjects + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify an integer."));
		}

		if ( (AppendOutput != null) && !AppendOutput.equals("") ) {
			if ( !AppendOutput.equalsIgnoreCase(_False) && !AppendOutput.equalsIgnoreCase(_True) ) {
				message = "The AppendOutput parameter \"" + AppendOutput + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
			}
		}

		// Put ListBucketObjectsScope at the end so combinations of parameters can be checked.
		if ( (ListBucketObjectsScope != null) && !ListBucketObjectsScope.equals("") ) {
			if ( !ListBucketObjectsScope.equalsIgnoreCase(_All) && !ListBucketObjectsScope.equalsIgnoreCase(_Folder) && !ListBucketObjectsScope.equalsIgnoreCase(_Root)) {
				message = "The ListBucketObjectsScope parameter \"" + ListBucketObjectsScope + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _All + " (default), " + _Folder + ", or " + _Root + "."));
			}
			// Cannot be specified with Prefix.
			/*
			if ( ListBucketObjectsScope.equalsIgnoreCase(_True) && (Prefix != null) && !Prefix.isEmpty() ) {
				message = "ListBucketObjectsScope=True cannot be specified with a Prefix value.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Change to not list root only or remove the prefix."));
			}
			*/
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

		if ( s3Command != AwsS3CommandType.LIST_BUCKETS ) {
			// All commands except listing buckets needs the bucket.
			if ( (Bucket == null) || Bucket.isEmpty() ) {
				message = "The bucket must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the bucket."));
			}
		}

		if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
			if ( (CopyKeys == null) || CopyKeys.isEmpty() ) {
				message = "The copy keys must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the copy keys."));
			}
		}
		else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
			if ( ((DeleteKeys == null) || DeleteKeys.isEmpty()) && ((DeleteFolders == null) || DeleteFolders.isEmpty()) ) {
				message = "The keys or folders must be specified for the delete.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify 1+ keys or folder paths to delete."));
			}
		}

		// The output table or file is needed for lists:
		// - some internal logic such as counts uses the table
		if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
			(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
			// Must specify table and/or file.
			if ( ((OutputTableID == null) || OutputTableID.isEmpty()) && ((OutputFile == null) || OutputFile.isEmpty()) ) {
				message = "The output table and/or file must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the output table ID and or file name."));
			}
		}

		// Make sure that only one of the CloudFront distribution ID or comment is specified:
		// - listing does not require the distribution
		if ( (s3Command == AwsS3CommandType.COPY_OBJECTS) ||
			(s3Command == AwsS3CommandType.DELETE_OBJECTS) ||
			(s3Command == AwsS3CommandType.UPLOAD_OBJECTS) ) {
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
		}

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(37);
		// General.
		validList.add ( "S3Command" );
		validList.add ( "Profile" );
		validList.add ( "Region" );
		validList.add ( "Bucket" );
		// Copy.
		validList.add ( "CopyKeys" );
		validList.add ( "CopyBucket" );
		validList.add ( "CopyObjectsCountProperty" );
		// Delete.
		validList.add ( "DeleteKeys" );
		validList.add ( "DeleteFolders" );
		validList.add ( "DeleteFoldersScope" );
		validList.add ( "DeleteFoldersMinDepth" );
		// Download.
		validList.add ( "DownloadFolders" );
		validList.add ( "DownloadFiles" );
		// List buckets.
		validList.add ( "ListBucketsRegEx" );
		validList.add ( "ListBucketsCountProperty" );
		// List bucket objects.
		validList.add ( "ListBucketObjectsScope" );
		validList.add ( "Prefix" );
		validList.add ( "Delimiter" );
		validList.add ( "ListBucketObjectsRegEx" );
		validList.add ( "ListFiles" );
		validList.add ( "ListFolders" );
		validList.add ( "MaxKeys" );
		validList.add ( "MaxObjects" );
		validList.add ( "ListBucketObjectsCountProperty" );
		// Upload.
		validList.add ( "UploadFolders" );
		validList.add ( "UploadFiles" );
		// Output
		validList.add ( "OutputTableID" );
		validList.add ( "OutputFile" );
		validList.add ( "AppendOutput" );
		// CloudFront
		validList.add ( "InvalidateCloudFront" );
		validList.add ( "CloudFrontRegion" );
		validList.add ( "CloudFrontDistributionId" );
		validList.add ( "CloudFrontComment" );
		validList.add ( "CloudFrontCallerReference" );
		validList.add ( "CloudFrontWaitForCompletion" );
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
	 * Invalidate paths that were updated.
	 */
	private int doCloudFrontInvalidation (
		AwsSession awsSession,
		String region, String distributionId, String commentPattern,
		List<String> cloudFrontPaths, String callerReference, boolean waitForCompletion,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doCloudFrontInvalidation";
		String message;
		CommandPhaseType commandPhase = CommandPhaseType.RUN;

   	   	// Invalidate files in a distribution using one or more paths:
   	   	// - see: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html
   	   	// List the distributions given the input parameters.
   	   	distributionId = AwsToolkit.getInstance().getCloudFrontDistributionId(awsSession, region, distributionId, commentPattern);
       	boolean doInvalidate = true;
       	if ( distributionId == null ) {
   			message = "Unable to determine CloudFront distribution ID for invalidation.";
   			Message.printWarning(warningLevel,
   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
   			status.addToLog ( commandPhase,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Verify that the distribution ID is valid for the region." ) );
   			doInvalidate = false;
       	}
       	if ( cloudFrontPaths.size() == 0 ) {
   			message = "No paths have been specified to invalidate.";
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

       		Region regionO = Region.of(region);

			// Handle credentials.

			ProfileCredentialsProvider credentialsProvider0 = null;
			credentialsProvider0 = ProfileCredentialsProvider.create(awsSession.getProfile());
			ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

			CloudFrontClient cloudfront = CloudFrontClient.builder()
				.region(regionO)
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
			AwsToolkit.getInstance().waitForCloudFrontInvalidations(awsSession, region, distributionId, waitMs, waitTimeout);
      	}

       	// Return the updated warning count.
       	return warningCount;
	}

	/**
 	* Run the CopyObject command.
 	* @param s3 the S3 client to use for S3 requests
 	* @param sourceBucket source bucket for the key
 	* @param copySourceKeyList list of source object keys to copy
 	* @param destBucket destination bucket for the key
 	* @param copyDestKeyList list of destination object key keys for copy (must align with copySourceKeyList)
 	* @param copyObjectCountProperty the processor property name to set the copy count
 	* @param cloudFrontPaths list of CloudFront paths to invalidate, should invalidation be requested
 	* @exception Exception let the exceptions
 	*/
	private int doS3CopyObjects (
		CommandProcessor processor,
		S3Client s3,
		String sourceBucket, List<String> copySourceKeyList, String destBucket, List<String> copyDestKeyList,
		String copyObjectCountProperty,
		List<String> cloudFrontPaths,
		CommandStatus status, int logLevel, int warningCount, String commandTag )
		throws Exception {
		String routine = getClass().getSimpleName() + ".doS3CopyObject";
		String message;

		if ( (destBucket == null)  || destBucket.isEmpty() ) {
			destBucket = sourceBucket;
		}
    	// CopyObjectRequest:
    	//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/CopyObjectRequest.html
    	// CopyObjectRequestBuilder:
    	//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/CopyObjectRequest.Builder.html
    	// CopyObjectResponse:
    	//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/CopyObjectResponse.html
		int copyCount = 0;
		for ( int i = 0; i < copySourceKeyList.size(); i++ ) {
			String sourceKey = copySourceKeyList.get(i);
			String destKey = copyDestKeyList.get(i);
			CopyObjectRequest request = CopyObjectRequest
				.builder()
				.sourceBucket(sourceBucket)
				.sourceKey(sourceKey)
				.destinationBucket(destBucket)
				.destinationKey(destKey)
				.build();
			// Error exception is caught in the main catch below.
			CopyObjectResponse response = s3.copyObject(request);
			if ( response.sdkHttpResponse().statusCode() == HttpURLConnection.HTTP_OK ) {
				// Successful.
				++copyCount;
				cloudFrontPaths.add(destKey);
			}
			else {
				message = "Copy object returned HTTP status " + response.sdkHttpResponse().statusCode() + " - object copy failed.";
				Message.printWarning(logLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount),
					routine, message );
				status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check that the original object exists." ) );
			}
       	}

		// Set the property indicating the number of object copied.
		if ( (copyObjectCountProperty != null) && !copyObjectCountProperty.equals("") ) {
			PropList requestParams = new PropList ( "" );
			requestParams.setUsingObject ( "PropertyName", copyObjectCountProperty );
			requestParams.setUsingObject ( "PropertyValue", new Integer(copyCount) );
			try {
				processor.processRequest( "SetProperty", requestParams);
			}
			catch ( Exception e ) {
				message = "Error requesting SetProperty(Property=\"" + copyObjectCountProperty + "\") from processor.";
				Message.printWarning(logLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount),
					routine, message );
				status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report the problem to software support." ) );
			}
		}

      	// Return the updated warning count.
      	return warningCount;
	}

	/**
	 * Delete S3 objects given a list of keys and/or folders.
	 * Folders require listing objects because the API does not include a delete folders.
	 * @param s3 S3Client instance to use for requests
	 * @param bucket bucket containing objects
	 * @param deleteFilesKeys list of keys to delete
	 * @param deleteFoldersKeys list of folders (keys ending in /) to delete
	 * @param deleteFoldersScope scope for deleting folders, controls whether shallow or deep delete
	 * @param deleteFoldersMinDepth minimum number of folders in keys to allow delete,
	 * used to protect against accidental deletes
	 * @param status command status for command logging messages
	 * @param logLevel log level for messages
	 * @param warningLevel warning level for messages
	 * @param warningCount warning count
	 * @param commandTag command tag for warning messages
	*/
	private int doS3DeleteObjects (
		S3Client s3,
		String bucket,
		List<String> deleteFilesKeys, List<String> deleteFoldersKeys,
		String deleteFoldersScope, int deleteFoldersMinDepth,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag
		) throws Exception {
		String routine = getClass().getSimpleName() + ".doS3DeleteObjects";
		String message;

		boolean debug = false;
		if ( Message.isDebugOn ) {
			debug = true;
		}

    	// DeleteObjectsRequest:
    	//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/DeleteObjectsRequest.html
    	// DeleteObjectsRequestBuilder:
    	//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/DeleteObjectsRequest.Builder.html
		//
    	// See:
		//    https://stackoverflow.com/questions/53950202/deleteobjects-using-aws-sdk-v2
		//
    	// Always do a DeleteObjectsRequest so that multiple objects can be deleted.

		// TODO smalers 2023-02-02 the following only deletes one object.
		/*
    	DeleteObjectRequest request = DeleteObjectRequest
    		.builder()
    		.bucket(bucket)
    		.key(DeleteKey)
    		.build();
  	    	s3.deleteObject(request);
  	   */

		// Create a list of identifiers:
		// - reuse the ObjectIdentifier.Builder
		List<ObjectIdentifier> objectIds = new ArrayList<>();
		ObjectIdentifier.Builder objectIdBuilder = ObjectIdentifier.builder();
		for ( String deleteFilesKey : deleteFilesKeys ) {
			if ( keyFolderDepthIsAtLeast(deleteFilesKey, deleteFoldersMinDepth) ) {
				objectIds.add(
					objectIdBuilder
						.key(deleteFilesKey)
						.build());
			}
			else {
				if ( debug ) {
					Message.printStatus(2, routine, "Skipping key \"" + deleteFilesKey +
						"\" because key # of folders is less than minimum of " + deleteFoldersMinDepth );
				}
			}
		}

		boolean deleteFolderFiles = false;
		boolean deleteAllFilesAndFolders = false;
		if ( deleteFoldersScope.equalsIgnoreCase(this._FolderFiles) ) {
			deleteFolderFiles = true;
		}
		else if ( deleteFoldersScope.equalsIgnoreCase(this._AllFilesAndFolders) ) {
			deleteAllFilesAndFolders = true;
		}

		// For folders, have to list the keys and add to the above list:
		// - the list depends on the DeleteFoldersScope
		// - previous code will have checked to make sure that the folders end in /
		int maxKeys = -1;
		int maxObjects = -1;
		for ( String deleteFoldersKey : deleteFoldersKeys ) {
			if ( deleteFolderFiles ) {
				// List the folder using the prefix of the folder AND the delimiter.
				boolean useDelimiter = true;
				String delimiter = "/";
				String prefix = deleteFoldersKey;
				boolean listFiles = true;
				boolean listFolders = true;
				String regex = null;
				List<AwsS3Object> s3Objects = AwsToolkit.getInstance().getS3BucketObjects(
					s3,
					bucket, prefix, delimiter, useDelimiter,
					maxKeys, maxObjects, listFiles, listFolders, regex);
				for ( AwsS3Object s3Object : s3Objects ) {
					if ( keyFolderDepthIsAtLeast(s3Object.getKey(), deleteFoldersMinDepth) ) {
						objectIds.add(
							objectIdBuilder
								.key(s3Object.getKey())
								.build());
					}
					else {
						if ( debug ) {
							Message.printStatus(2, routine, "Skipping key \"" + s3Object.getKey() +
								"\" because key # of folders is less than minimum of " + deleteFoldersMinDepth );
						}
					}
				}
			}
			else if ( deleteAllFilesAndFolders ) {
				// List the folder using the prefix including the folder and NOT the delimiter.
				boolean useDelimiter = false;
				String delimiter = null;
				String prefix = deleteFoldersKey;
				boolean listFiles = true;
				boolean listFolders = true;
				String regex = null;
				List<AwsS3Object> s3Objects = AwsToolkit.getInstance().getS3BucketObjects(
					s3,
					bucket, prefix, delimiter, useDelimiter,
					maxKeys, maxObjects, listFiles, listFolders, regex);
				for ( AwsS3Object s3Object : s3Objects ) {
					if ( keyFolderDepthIsAtLeast(s3Object.getKey(), deleteFoldersMinDepth) ) {
						objectIds.add(
							objectIdBuilder
								.key(s3Object.getKey())
								.build());
					}
					else {
						if ( debug ) {
							Message.printStatus(2, routine, "Skipping key \"" + s3Object.getKey() +
								"\" because key # of folders is less than minimum of " + deleteFoldersMinDepth );
						}
					}
				}
			}
		}

		// Delete the objects.
    	DeleteObjectsRequest request = DeleteObjectsRequest
    		.builder()
    		.bucket(bucket)
    		.delete(
    			Delete.builder()
    				.objects(objectIds)
    				.build())
    		.build();

  	    	DeleteObjectsResponse response = s3.deleteObjects(request);

  	    	if ( response.deleted().size() != objectIds.size() ) {
  	    		// Create a list of booleans to check which files were deleted.
  	    		boolean [] isDeleted = new boolean[objectIds.size()];
  	    		for ( int i = 0; i < isDeleted.length; i++ ) {
  	    			isDeleted[i] = false;
  	    		}
  	    		// Go through the list of what was actually deleted.
  	    		for ( DeletedObject deleted : response.deleted() ) {
  	    			// Search for the deleted object in the original list.
  	    			for ( int i = 0; i < objectIds.size(); i++ ) {
  	    				ObjectIdentifier objectId = objectIds.get(i);
  	    				if ( objectId.key().equals(deleted.key()) ) {
  	    					isDeleted[i] = true;
  	    					break;
  	    				}
  	    			}
  	    		}

  	    		// Now have the list of undeleted keys.
  	    		for ( int i = 0; i < isDeleted.length; i++ ) {
  	    			if ( !isDeleted[i] ) {
    					message = "Unable to delete key \"" + objectIds.get(i).key() + "\".";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Check permissions."));
  	    			}
  	    		}
  	    	}

		// Return the updated warning count.
		return warningCount;
	}

	/**
	 * Download S3 files and folders.
	 */
	private int doS3DownloadObjects (
		CommandProcessor processor,
		ProfileCredentialsProvider credentialsProvider,
		String bucket, String region,
		List<String> downloadFilesKeys, List<String> downloadFilesFiles,
		List<String> downloadFoldersKeys, List<String> downloadFoldersDirectories,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag
		) {
		String routine = getClass().getSimpleName() + ".doS3DownloadObjects";
		String message;

    	// The following is from the S3TransferManager javadoc.
    	S3TransferManager tm = null;

    	Region regionObject = Region.of(region);

    	if ( (downloadFilesFiles.size() > 0) || (downloadFoldersDirectories.size() > 0) ) {
    		tm = S3TransferManager
    			.builder()
    			.s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
   				.region(regionObject))
    			.build();
    	}

    	if ( downloadFilesFiles.size() > 0 ) {
    		// Process each file in the list.
    		boolean error = false;
    		int iFile = -1;
    		for ( String downloadKey : downloadFilesKeys ) {
    			++iFile;
    			error = false;
    			String localFile = null;
    			try {
    				downloadKey = downloadKey.trim();
    				localFile = downloadFilesFiles.get(iFile).trim();
    				if ( (localFile == null) || localFile.trim().isEmpty() ) {
    					// Use the same name as the key, but only the file name.
    					int pos = localFile.lastIndexOf("/");
    					if ( pos >= 0 ) {
    						if ( pos < (localFile.length() - 1) ) {
    							localFile = localFile.substring(pos + 1).trim();
    						}
    						else {
    							// Error because / at the end of the key.
    							message = "No local file given and key (" + downloadKey + ") ends in / - cannot default file name to copy.";
    							Message.printWarning ( warningLevel,
    								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
    					final String downloadKeyFinal = downloadKey;
    	    			FileDownload download = tm
   	    					.downloadFile(d -> d.getObjectRequest(g -> g.bucket(bucket).key(downloadKeyFinal))
    						.destination(Paths.get(localFileFull)));
    	    			download.completionFuture().join();
    	    		}
    	    	}
    	    	catch ( Exception e ) {
    	    		message = "Error downloading S3 key \"" + downloadKey + "\" to file \"" + localFile + "\" (" + e + ")";
    	    		Message.printWarning ( warningLevel,
    	    			MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    	    		Message.printWarning ( 3, routine, e );
    	    		status.addToLog(CommandPhaseType.RUN,
    	    			new CommandLogRecord(CommandStatusType.FAILURE,
    	    				message, "See the log file for details."));
    	    	}
    		}
      	}
      	if ( downloadFoldersDirectories.size() > 0 ) {
    		// Process each folder in the list.
    		boolean error = false;
    		int iDir = -1;
    		for ( String downloadKey : downloadFoldersKeys ) {
    			++iDir;
    			error = false;
    			String localFolder = null;
    			try {
    				downloadKey = downloadKey.trim();
    				localFolder = downloadFoldersDirectories.get(iDir);
    				if ( (localFolder == null) || localFolder.trim().isEmpty() ) {
    					// Use the same name as the key, but only the folder name.
    					int pos = localFolder.lastIndexOf("/");
    					if ( pos >= 0 ) {
    						if ( pos < (localFolder.length() - 1) ) {
    							localFolder = localFolder.substring(pos + 1).trim();
    						}
    						else {
    							// Error because / at the end of the key.
    							message = "No local folder given and key (" + downloadKey + ") ends in / - cannot default folder name to copy.";
    							Message.printWarning ( warningLevel,
    								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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
   								Message.printWarning ( warningLevel,
   									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   								status.addToLog(CommandPhaseType.RUN,
   									new CommandLogRecord(CommandStatusType.FAILURE,
   										message, "Check command parameters."));
    							break;
    						}
   							message = "Error downloading folder \"" + downloadKey + "\" to folder \"" + localFolderFull + "\"(" + fail + ").";
   							Message.printWarning ( warningLevel,
   								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   							status.addToLog(CommandPhaseType.RUN,
   								new CommandLogRecord(CommandStatusType.FAILURE,
   									message, "Check command parameters."));
    					}
    				}
    			}
    			catch ( Exception e ) {
    				message = "Error downloading S3 key \"" + downloadKey + "\" to folder \"" + localFolder + "\" (" + e + ")";
    				Message.printWarning ( warningLevel,
    					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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

    	// Return the updated warning count.
    	return warningCount;
	}

	/**
	 * List S3 bucket objects.
	 */
	private int doS3ListBucketObjects (
		CommandProcessor processor,
		S3Client s3,
		String bucket,
		String listScope, String prefix, String delimiter, int maxKeys, int maxObjects,
		boolean listFiles, boolean listFolders, String regex,
		DataTable table, int objectKeyCol, int objectTypeCol, int objectNameCol, int objectParentNameCol,
		String listBucketObjectsCountProperty,
		int objectSizeCol, int objectOwnerCol, int objectLastModifiedCol,
		CommandStatus status, int logLevel, int warningCount, String commandTag
		) throws Exception {
		String routine = getClass().getSimpleName() + ".doS3ListBucketObjects";
		String message;

   	    // List bucket objects for files and/or "common prefix" for folders.
   	    ListObjectsV2Request.Builder builder = ListObjectsV2Request
    		.builder()
    		.fetchOwner(Boolean.TRUE) // Get the owner so it can be shown in output.
    		.bucket(bucket); // Bucket is required.
    	if ( maxKeys > 0 ) {
    		// Set the maximum number of keys that will be returned per request.
    		builder.maxKeys(maxKeys);
    	}
    	// Indicate whether prefix is being used, to speed up checks.
    	boolean doPrefix = false;
    	if ( listScope.equalsIgnoreCase(this._Root) ) {
    		// Listing root files and/or folders:
    		// - do not specify prefix
    		// - do specify delimiter
    		// - if the bucket uses / as the root, the folder listing can be used
    		if ( (prefix != null) && prefix.isEmpty() ) {
    			builder.prefix(prefix);
    			doPrefix = true;
    		}
    		if ( (delimiter == null) || delimiter.isEmpty() ) {
    			builder.delimiter("/");
    		}
    		else {
    			// Set what the command provided.
    			builder.delimiter(delimiter);
    		}
    		Message.printStatus ( 2, routine, "Requesting all objects in the bucket root." );
      	}
    	else if ( listScope.equalsIgnoreCase(this._Folder) ) {
    		// Listing a specific folder:
    		// - prefix will have been checked previously
    		// - delimiter is required and will have been checked previously
    		if ( (prefix != null) && !prefix.isEmpty() ) {
    			builder.prefix(prefix);
    			doPrefix = true;
    		}
    		// Also need to set the delimiter.
    		if ( (delimiter == null) || delimiter.isEmpty() ) {
    			builder.delimiter("/");
    		}
    		else {
    			// Set what the command provided.
    			builder.delimiter(delimiter);
    		}
    		Message.printStatus ( 2, routine, "Requesting all objects matching prefix \"" + prefix + "\"." );
    	}
    	else {
    		// Listing everything in the bucket:
    		// - can use the prefix to filter
    		// - no delimiter is used
    		// - ok to return a folder matching a prefix
    		Message.printStatus ( 2, routine, "Requesting all objects in the bucket." );
    		if ( (prefix != null) && !prefix.isEmpty() ) {
    			builder.prefix(prefix);
    		}
    	}

    	ListObjectsV2Request request = builder.build();
    	ListObjectsV2Response response = null;
    	TableRecord rec = null;
    	boolean allowDuplicates = false;
    	// TODO smalers 2022-05-31 for now use UTC time.
    	String timezone = "Z";
    	ZoneId zoneId = ZoneId.of("Z");
    	int dateTimeBehaviorFlag = 0;
    	boolean done = false;
    	int objectCount = 0;
    	int fileCount = 0;
    	int folderCount = 0;
    	while ( !done ) {
    		response = s3.listObjectsV2(request);
    		// Process files and folders separately, with the maximum count checked based on what is returned.
    		if ( listFiles || listFolders ) {
    			// S3Objects can contain files or folders (objects with key ending in /, typically with size=0).
    			// Loop in any case to get the count.
    			for ( S3Object s3Object : response.contents() ) {
    				// Check the maximum object count, to protect against runaway processes.
    				if ( objectCount >= maxObjects ) {
			  			// Quit saving objects when the limit has been reached.
    					break;
    				}
    				// Output to table:
    				// - key is the full path to the file
    				// - have size, owner and modification time properties
   					String key = s3Object.key();
   					if ( doPrefix && prefix.endsWith("/") && key.equals(prefix) ) {
   						// Do not include the requested prefix itself because want the contents of the folder,
   						// not the folder itself.
   						Message.printStatus(2, routine, "Ignoring Prefix that is a folder because want folder contents.");
   						continue;
   					}
   					if ( regex != null ) {
   						// Want to apply a regular expression to the key.
   						if ( !key.matches(regex) ) {
   							continue;
   						}
   					}
   					if ( !listFolders && key.endsWith("/") ) {
   						// Is a folder and don't want folders so continue.
   						continue;
   					}
   					else if ( !listFiles && !key.endsWith("/") ) {
   						// Is a file and don't files want so continue.
   						continue;
   					}
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
    					// Set the name as the end of the key without folder delimiter.
    					String name = s3Object.key();
    					if ( name.endsWith("/") ) {
    						name = name.substring(0,(name.length() - 1));
    						int pos = name.lastIndexOf("/");
    						if ( pos >= 0 ) {
    							// Have subfolders:
    							// - strip so only the name remains
    							name = name.substring(pos + 1);
    						}
    					}
   						rec.setFieldValue(objectNameCol,name);
    					// Set the parent name as the folder above the current name string.
   						int pos = name.lastIndexOf("/");
   						String parentName = "";
   						if ( pos >= 0 ) {
   							// Have subfolders:
   							// - strip so only the name remains
   							parentName = name.substring(pos + 1);
   						}
   						rec.setFieldValue(objectParentNameCol,parentName);
    					if ( key.endsWith("/") ) {
    						rec.setFieldValue(objectTypeCol,"folder");
    					}
    					else {
    						rec.setFieldValue(objectTypeCol,"file");
    					}
    					rec.setFieldValue(objectSizeCol,s3Object.size());
    					if ( s3Object.owner() == null ) {
    						rec.setFieldValue(objectOwnerCol,"");
    					}
    					else {
    						rec.setFieldValue(objectOwnerCol,s3Object.owner().displayName());
    					}
    					rec.setFieldValue(objectLastModifiedCol,
    						new DateTime(OffsetDateTime.ofInstant(s3Object.lastModified(), zoneId), dateTimeBehaviorFlag, timezone));
    				}
   					// Increment the count of objects processed (includes files and folders).
   					++objectCount;
   					if ( key.endsWith("/") ) {
   						++folderCount;
   					}
   					else {
   						++fileCount;
   					}
    			}
    		}
    		if ( listFolders ) {
    			// Common prefixes are only used with folders:
    			// - the key will be from the root to the / (inclusive) after the prefix
    			for ( CommonPrefix commonPrefix : response.commonPrefixes() ) {
			  		// Check the maximum object count, to protect against runaway processes.
			  		if ( objectCount >= maxObjects ) {
			  			// Quit saving objects when the limit has been reached.
						break;
			  		}
   					if ( doPrefix && prefix.endsWith("/") && commonPrefix.prefix().equals(prefix) ) {
   						// Do not include the requested prefix itself because want the contents of the folder,
   						// not the folder itself.
   						Message.printStatus(2, routine, "Ignoring Prefix that is a folder because want folder contents.");
   						continue;
   					}
   					if ( regex != null ) {
   						// Want to apply a regular expression to the key.
   						if ( !commonPrefix.prefix().matches(regex) ) {
   							continue;
   						}
   					}
    				// Output to table:
			  		// - key is the path to the folder including trailing / to indicate a folder
			  		// - only have the key since folders are virtual and have no properties
    				if ( table != null ) {
    					if ( !allowDuplicates ) {
    						// Try to match the object key, which is the unique identifier.
    						rec = table.getRecord ( objectKeyCol, commonPrefix.prefix() );
    					}
    					if ( rec == null ) {
    						// Create a new record.
    						rec = table.addRecord(table.emptyRecord());
    					}
    					// Set the data in the record.
    					rec.setFieldValue(objectKeyCol, commonPrefix.prefix());
    					// Set the name as the end of the key without folder delimiter.
    					String name = commonPrefix.prefix();
    					if ( name.endsWith("/") ) {
    						name = name.substring(0,(name.length() - 1));
    						int pos = name.lastIndexOf("/");
    						if ( pos >= 0 ) {
    							// Have subfolders:
    							// - strip so only the name remains
    							name = name.substring(pos + 1);
    						}
    					}
   						rec.setFieldValue(objectNameCol,name);
   						int pos = name.lastIndexOf("/");
   						String parentName = "";
   						if ( pos >= 0 ) {
   							// Have subfolders:
   							// - strip so only the name remains
   							parentName = name.substring(pos + 1);
   						}
   						rec.setFieldValue(objectParentNameCol,parentName);
    					rec.setFieldValue(objectTypeCol,"folder");
    				}
   					// Increment the count of objects processed (includes files and folders).
			  		++objectCount;
   					++folderCount;
		  		}
    		}
    		if ( response.nextContinuationToken() == null ) {
    			done = true;
    		}
    		request = request.toBuilder()
   				.continuationToken(response.nextContinuationToken())
   				.build();
    	}
    	// Sort the table by key if both files and folders were queried:
    	// - necessary because files come out of the objects and folders out of common prefixes
    	if ( listFiles && listFolders ) {
    		String [] sortColumns = { "Key" };
    		int [] sortOrder = { 1 };
    		table.sortTable( sortColumns, sortOrder);
    	}
    	Message.printStatus ( 2, routine, "Response has objects=" + response.contents().size()
    		+ ", commonPrefixes=" + response.commonPrefixes().size() );
    	Message.printStatus ( 2, routine, "List has fileCount=" + fileCount + ", folderCount="
    		+ folderCount + ", objectCount=" + objectCount );
    	// Set the property indicating the number of bucket objects.
       	if ( (listBucketObjectsCountProperty != null) && !listBucketObjectsCountProperty.equals("") ) {
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", listBucketObjectsCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(objectCount) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + listBucketObjectsCountProperty + "\") from processor.";
               	Message.printWarning(logLevel,
                   	MessageUtil.formatMessageTag( commandTag, ++warningCount),
                   	routine, message );
                    	status.addToLog ( CommandPhaseType.RUN,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Report the problem to software support." ) );
           	}
       	}

       	// Return the updated warning count.
       	return warningCount;
	}

	/**
	 * List S3 buckets.
	 */
	private int doS3ListBuckets (
		CommandProcessor processor,
		S3Client s3,
		DataTable table,
		int bucketNameCol, int bucketCreationDateCol,
		String regEx, String listBucketsCountProperty,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doS3ListBuckets";
		String message;

    	ListBucketsRequest request = ListBucketsRequest
    		.builder()
    		.build();
    	ListBucketsResponse response = s3.listBuckets(request);

    	TableRecord rec = null;
    	boolean allowDuplicates = false;

    	boolean doRegEx = false;
    	if ( (regEx != null) && !regEx.isEmpty() ) {
    		// Check whether the bucket names match the regular expression.
    		doRegEx = true;
    	}

		// Output to table.
   		if ( table != null ) {
   			for ( Bucket bucketObject : response.buckets() ) {
   				String bucketName = bucketObject.name();
   				if ( doRegEx ) {
   					if ( !bucketName.matches(regEx) ) {
   						continue;
   					}
   				}
    			if ( !allowDuplicates ) {
    				// Try to match the bucket name, which is the unique identifier.
    				rec = table.getRecord ( bucketNameCol, bucketName );
    			}
    			if ( rec == null ) {
    				// Create a new record.
    				rec = table.addRecord(table.emptyRecord());
    			}
    			// Set the data in the record.
    			rec.setFieldValue(bucketNameCol,bucketName);
    			DateTime creationDate = null;
    			try {
    				creationDate = DateTime.parse(bucketObject.creationDate().toString());
    			}
    			catch ( Exception e ) {
    				// Leave the creation date as null.
    			}
    			rec.setFieldValue(bucketCreationDateCol,creationDate);
    		}
    	}
    	// Set the property indicating the number of buckets.
        if ( (listBucketsCountProperty != null) && !listBucketsCountProperty.equals("") ) {
          	int bucketCount = 0;
          	if ( table != null ) {
          		bucketCount = table.getNumberOfRecords();
          	}
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", listBucketsCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(bucketCount) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + listBucketsCountProperty + "\") from processor.";
               	Message.printWarning(logLevel,
                   	MessageUtil.formatMessageTag( commandTag, ++warningCount),
                   	routine, message );
               	status.addToLog ( CommandPhaseType.RUN,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Report the problem to software support." ) );
           	}
        }

        // Return the updated warning count.
        return warningCount;
	}

	/**
	 * Do S3 upload files and folders.
	 */
	private int doS3UploadObjects (
		CommandProcessor processor,
		ProfileCredentialsProvider credentialsProvider,
		String bucket, String region,
		List<String> uploadFilesOrig, List<String> uploadFilesFileList, List<String> uploadFilesKeyList,
		List<String> uploadFoldersOrig, List<String> uploadFoldersDirectoryList, List<String> uploadFoldersKeyList,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag
		) throws Exception {
		String routine = getClass().getSimpleName() + ".doS3UploadObjects";
		String message;

    	Region regionObject = Region.of(region);

    	// The following is from the S3TransferManager javadoc.
    	S3TransferManager tm = null;
    	if ( (uploadFilesFileList.size() > 0) || (uploadFoldersDirectoryList.size() > 0) ) {
    		tm = S3TransferManager
    			.builder()
    			.s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
   				.region(regionObject))
    			.build();
    	}

    	Message.printStatus(2, routine, "Have " + uploadFilesFileList.size() + " files to upload.");
    	if ( uploadFilesFileList.size() > 0 ) {
    		// Process each file in the list:
    		// - don't allow null or empty key or name
    		boolean error = false;
 				int iFile = -1;
    		for ( String localFile : uploadFilesFileList ) {
    			++iFile;
    			error = false;
    			String uploadKey = null;
    			try {
    				localFile = localFile.trim();
    				uploadKey = uploadFilesKeyList.get(iFile);
    				if ( (localFile == null) || localFile.trim().isEmpty() ) {
    					// Don't allow default destination because could cause problems clobbering S3 files.
    					message = "No local file given - cannot upload file.";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the local file name."));
    					error = true;
    				}
    				File localFileFile = new File(localFile);
    				if ( !localFileFile.exists() ) {
    					// Local file does not exist so cannot upload.
    					message = "Local file does not exist: " + localFile + " (UploadFiles parameter = \"" + uploadFilesOrig.get(iFile) + "\").";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the local file name."));
    					error = true;
    				}
    				if ( (uploadKey == null) || uploadKey.trim().isEmpty() ) {
    					// Don't allow default because could cause problems clobbering S3 files.
    					message = "No S3 key (object path) given - cannot upload file \"" + localFile + "\".";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the key name."));
    					error = true;
    				}
    				if ( !error ) {
    					final String localFileFinal = localFile.trim();
    					uploadKey = uploadKey.trim();
    					Message.printStatus(2, routine, "Uploading local file \"" + localFileFinal + "\" to S3 key \"" + uploadKey + "\".");
    					final String uploadKeyFinal = uploadKey;
    					FileUpload upload = tm
   							.uploadFile(d -> d.putObjectRequest(g -> g.bucket(bucket).key(uploadKeyFinal))
								.source(Paths.get(localFileFinal)));
    					upload.completionFuture().join();
    				}
    			}
    			catch ( Exception e ) {
    				message = "Error uploading file \"" + localFile + "\" to S3 key \"" + uploadKey + "\" (" + e + ").";
    				Message.printWarning ( warningLevel,
    					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    				Message.printWarning ( 3, routine, e );
    				status.addToLog(CommandPhaseType.RUN,
    					new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "See the log file for details."));
    			}
    		}
    	}
    	Message.printStatus(2, routine, "Have " + uploadFoldersDirectoryList.size() + " folders to upload.");
    	if ( uploadFoldersDirectoryList.size() > 0 ) {
    		// Process each folder in the list.
    		boolean error = false;
				int iDir = -1;
    		for ( String localFolder : uploadFoldersDirectoryList ) {
    			++iDir;
    			error = false;
    			String uploadKey = null;
    			try {
    				localFolder = localFolder.trim();
    				uploadKey = uploadFoldersKeyList.get(iDir).trim();
    				if ( (localFolder == null) || localFolder.trim().isEmpty() ) {
    					// Don't allow default because could cause problems clobbering S3 files.
    					message = "No local folder given - cannot upload folder.";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the local folder name."));
    					error = true;
    				}
    				File localFolderFile = new File(localFolder);
    				if ( !localFolderFile.exists() ) {
    					// Local folder does not exist so cannot upload.
    					message = "Local folder does not exist: " + localFolder
    						+ " (UploadFolders parameter = \"" + uploadFoldersOrig.get(iDir) + "\").";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the local folder name."));
    					error = true;
    				}
    				if ( (uploadKey == null) || uploadKey.trim().isEmpty() ) {
    					// Don't allow default because could cause problems clobbering S3 files.
    					message = "No S3 key given - cannot upload folder \"" + localFolder + "\".";
    					Message.printWarning ( warningLevel,
    						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    					status.addToLog(CommandPhaseType.RUN,
    						new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Fix the key name."));
    					error = true;
    				}
    				if ( !error ) {
    					Message.printStatus(2, routine, "Uploading local folder \"" + localFolder + "\" to S3 key \"" + uploadKey + "\".");
    					DirectoryUpload upload = tm.uploadDirectory(UploadDirectoryRequest.builder()
								.sourceDirectory(Paths.get(localFolder))
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
   								Message.printWarning ( warningLevel,
   									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   								status.addToLog(CommandPhaseType.RUN,
   									new CommandLogRecord(CommandStatusType.FAILURE,
   										message, "Check command parameters."));
    							break;
    						}
   							message = "Error uploading folder \"" + localFolder + "\" to key \"" + uploadKey + "\"(" + fail + ").";
   							Message.printWarning ( warningLevel,
   								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   							status.addToLog(CommandPhaseType.RUN,
   								new CommandLogRecord(CommandStatusType.FAILURE,
   									message, "Check command parameters."));
    					}
    				}
    			}
    			catch ( Exception e ) {
    				message = "Error uploading folder \"" + localFolder + "\" to S3 key \"" + uploadKey + "\" (" + e + ").";
    				Message.printWarning ( warningLevel,
    					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
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

    	// Return the updated warning count.
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
	 * Determine whether a key has a folder depth at least the requested value.
	 * For example:
	 * <pre>
	 *    file.txt - folder depth 0
	 *    /file.txt - folder depth 0
	 *
	 *    folder1/file.txt - folder depth 1
	 *    /folder1/file.txt - folder depth 1
	 *
	 *    /folder1/folder2/file.txt - folder depth 2
	 *    folder1/folder2/file.txt - folder depth 2
	 *
	 *    folder1/folder2/folder3/file.txt - folder depth 3
	 *    /folder1/folder2/folder3/file.txt - folder depth 3
	 * </pre>
	 * @param key the key to evaluate
	 * @param minDepth minimum required folder depth
	 */
	public boolean keyFolderDepthIsAtLeast ( String key, int minDepth ) {
		if ( key == null ) {
			return false;
		}
		// Folder delimiter character.
		String delim = "/";
		// Count the number of /.
		int delimCount = StringUtil.patternCount(key, "/");
		// If the key did not start with /, add one to the count as if it did.
		if ( !key.startsWith(delim) ) {
			++delimCount;
		}
		if ( delimCount >= minDepth ) {
			return true;
		}
		else {
			return false;
		}
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
	@param commandNumber Number of command in sequence (1+).
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
	*/
	private void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommand", message;
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

    	// Clear the output file.
    	setOutputFile ( null );

		String Profile = parameters.getValue ( "Profile" );
		Profile = TSCommandProcessorUtil.expandParameterValue(processor,this,Profile);
		String profile = Profile;
		if ( (Profile == null) || Profile.isEmpty() ) {
			// Get the default.
			profile = AwsToolkit.getInstance().getDefaultProfile();
			if ( (profile == null) || profile.isEmpty() ) {
				message = "The profile is not specified and unable to determine the default.";
				Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Make sure that the AWS configuration file exists with at least one profile: " +
						AwsToolkit.getInstance().getAwsUserConfigFile() ) );
			}
		}
		String S3Command = parameters.getValue ( "S3Command" );
		AwsS3CommandType s3Command = AwsS3CommandType.valueOfIgnoreCase(S3Command);
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
			else {
				Message.printStatus(2, routine, "Region was not specified.  Using default region from profile: " + region);
			}
		}
		// Bucket must be final because of lambda use below.
		String bucket0 = parameters.getValue ( "Bucket" );
		final String bucket = TSCommandProcessorUtil.expandParameterValue(processor,this,bucket0);

		// Copy.
		String CopyKeys = parameters.getValue ( "CopyKeys" );
		CopyKeys = TSCommandProcessorUtil.expandParameterValue(processor,this,CopyKeys);
		int copyKeysCount = 0;
		List<String> copySourceKeyList = new ArrayList<>();
		List<String> copyDestKeyList = new ArrayList<>();
    	if ( (CopyKeys != null) && !CopyKeys.isEmpty() && (CopyKeys.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(CopyKeys, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++copyKeysCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String sourceKey = parts[0].trim();
            		String destKey = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			if ( sourceKey.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Source key " + copyKeysCount + " (" + sourceKey +
       			   				") contains ${ due to unknown processor property - skipping to avoid copy error."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( destKey.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Destination key " + copyKeysCount + " (" + destKey +
       			   				") contains ${ due to unknown processor property - skipping to avoid copy error."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( sourceKey.endsWith("/") ) {
       			   			message = "Source key " + copyKeysCount + " (" + sourceKey +
       			   				") ends with /, which indicates a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that a key for the source file is specified." ) );
        		   			continue;
        	   			}
            			if ( destKey.endsWith("/") ) {
       			   			message = "Destination key " + copyKeysCount + " (" + sourceKey +
       			   				") ends with /, which indicates a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that a key for the destination file is specified." ) );
        		   			continue;
        	   			}
            			if ( sourceKey.indexOf("*") >= 0 ) {
            				// Source key has a wildcard:
            				// - not supported
            				message = "Source key uses * wildcard - skipping.";
			       			Message.printWarning(warningLevel,
				   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			       			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
				   				message, "Wildcard is not allowed for the source key." ) );
			       			continue;
            			}
            			if ( destKey.endsWith("*") ) {
   						    // Replace the wildcard with the file name for the source.
            				// Get the name from the source key.
            				int pos = sourceKey.lastIndexOf("/");
            				String sourceName = null;
            				if ( pos >= 0 ) {
            					// This will work because sourceKey was checked above for ending in /.
            					sourceName = sourceKey.substring(pos + 1);
            				}
            				else {
            					// Source has no folder so just use the folder name.
            					sourceName = sourceKey;
            				}
            				// Handle specific wildcard cases for destKey:
           					// - destination must equal "*" or end in "/*"
           					// - handles the edge case for copying root folder files
           					if ( destKey.equals("*") ) {
           						destKey = sourceName;
   						    	if ( Message.isDebugOn ) {
   						    		Message.printStatus(2, routine, "                     Source key: " + sourceKey );
   						    		Message.printStatus(2, routine, "Destination key from * wildcard: " + destKey );
   						    	}
           					}
           					else if ( destKey.endsWith("/*") ) {
            					destKey = sourceKey.replace("*", sourceName );
   						    	if ( Message.isDebugOn ) {
   						    		Message.printStatus(2, routine, "                      Source key: " + sourceKey );
   						    		Message.printStatus(2, routine, "Destination key from /* wildcard: " + destKey );
   						    	}
           					}
           					else {
           						message = "Destination key must equal * or end in /* to use source key file name.";
		       					Message.printWarning(warningLevel,
			   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		       					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
			   						message, "Fix the destination key to equal * or end in /*." ) );
		       					continue;
           					}	
            			}
            			// Add to the lists for further processing.
            			//uploadFilesOrig.add(localFile);
            			copySourceKeyList.add(sourceKey);
            			copyDestKeyList.add(destKey);
            		}
            	}
        	}
    	}
		// Bucket must be final because of lambda use below.
		String copyBucket0 = parameters.getValue ( "CopyBucket" );
		final String copyBucket = TSCommandProcessorUtil.expandParameterValue(processor,this,copyBucket0);
    	String CopyObjectsCountProperty = parameters.getValue ( "CopyObjectsCountProperty" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		CopyObjectsCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, CopyObjectsCountProperty);
    	}

		// Delete.
		String DeleteKeys = parameters.getValue ( "DeleteKeys" );
		DeleteKeys = TSCommandProcessorUtil.expandParameterValue(processor,this,DeleteKeys);
		List<String> deleteFilesKeys = new ArrayList<>();
		if ( (DeleteKeys == null) || !DeleteKeys.isEmpty() ) {
			deleteFilesKeys = StringUtil.breakStringList(DeleteKeys,",", StringUtil.DELIM_TRIM_STRINGS);
		}
		String DeleteFolders = parameters.getValue ( "DeleteFolders" );
		DeleteFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,DeleteFolders);
		List<String> deleteFoldersKeys = new ArrayList<>();
		if ( (DeleteFolders == null) || !DeleteFolders.isEmpty() ) {
			deleteFoldersKeys = StringUtil.breakStringList(DeleteFolders,",", StringUtil.DELIM_TRIM_STRINGS);
		}
		String DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
		if ( (DeleteFoldersScope == null) || DeleteFoldersScope.isEmpty() ) {
			DeleteFoldersScope = this._FolderFiles;
		}
		String DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
		int deleteFoldersMinDepth = _DeleteFoldersMinDepth;
		if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.isEmpty() ) {
			try {
				deleteFoldersMinDepth = Integer.parseInt(DeleteFoldersMinDepth.trim());
			}
			catch ( NumberFormatException e ) {
				// Warning will have been generated by checkCommandParameters().  Use the default.
				deleteFoldersMinDepth = _DeleteFoldersMinDepth;
			}
		}

		// Download.
    	String DownloadFolders = parameters.getValue ( "DownloadFolders" );
		DownloadFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadFolders);
		// Can't use a hashtable because sometimes download the same folders to multiple S3 locations.
    	List<String> downloadFoldersKeys = new ArrayList<>();
    	List<String> downloadFoldersDirectories = new ArrayList<>();
    	if ( (DownloadFolders != null) && (DownloadFolders.length() > 0) && (DownloadFolders.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(DownloadFolders, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		downloadFoldersKeys.add(parts[0].trim());
            		downloadFoldersDirectories.add(parts[1].trim());
            	}
            	else {
            		downloadFoldersKeys.add(parts[0].trim());
            		downloadFoldersDirectories.add("");
            	}
        	}
    	}
    	String DownloadFiles = parameters.getValue ( "DownloadFiles" );
		DownloadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadFiles);
		// Can't use a hashtable because sometimes download the same files to multiple S3 locations.
    	List<String> downloadFilesKeys = new ArrayList<>();
    	List<String> downloadFilesFiles = new ArrayList<>();
    	if ( (DownloadFiles != null) && (DownloadFiles.length() > 0) && (DownloadFiles.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(DownloadFiles, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		downloadFilesKeys.add(parts[0].trim());
            		downloadFilesFiles.add(parts[1].trim());
            	}
            	else {
            		downloadFilesKeys.add(parts[0].trim());
            		downloadFilesFiles.add("");
            	}
        	}
    	}

    	// List buckets.
		String ListBucketsRegEx = parameters.getValue ( "ListBucketsRegEx" );
		// TODO smalers 2023-01-27 evaluate whether regex can be expanded or will have conflicts.
		//ListBucketsRegEx = TSCommandProcessorUtil.expandParameterValue(processor,this,ListBucketsRegEx);
		// Convert the RegEx to Java style.
		String listBucketsRegEx = null;
		if ( (ListBucketsRegEx != null) && !ListBucketsRegEx.isEmpty() ) {
			if ( ListBucketsRegEx.toUpperCase().startsWith("JAVA:") ) {
				// Use as is for a Java regular expression.
				listBucketsRegEx = ListBucketsRegEx.substring(5);
			}
			else {
				// Default to glob so convert to Java regex.
				// TODO smalers 2023-02-01 need to hanle [abc] and [a-z].
				listBucketsRegEx = ListBucketsRegEx.replace(".", "\\.").replace("*", ".*");
			}
		}
    	String ListBucketsCountProperty = parameters.getValue ( "ListBucketsCountProperty" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		ListBucketsCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ListBucketsCountProperty);
    	}

    	// List bucket objects.
		String ListBucketObjectsScope = parameters.getValue ( "ListBucketObjectsScope" );
		if ( (ListBucketObjectsScope == null) || ListBucketObjectsScope.isEmpty() ) {
			ListBucketObjectsScope = this._All; // Default.
		}
		String Prefix = parameters.getValue ( "Prefix" );
		Prefix = TSCommandProcessorUtil.expandParameterValue(processor,this,Prefix);
		// Set the default delimiter lower in the code because defaulting to / has implications.
		String Delimiter = parameters.getValue ( "Delimiter" );
		Delimiter = TSCommandProcessorUtil.expandParameterValue(processor,this,Delimiter);
		String ListBucketObjectsRegEx = parameters.getValue ( "ListBucketObjectsRegEx" );
		// TODO smalers 2023-01-27 evaluate whether regex can be expanded or will have conflicts.
		//ListBucketObjectsRegEx = TSCommandProcessorUtil.expandParameterValue(processor,this,ListBucketObjectsRegEx);
		// Convert the RegEx to Java style.
		String listBucketObjectsRegEx = null;
		if ( (ListBucketObjectsRegEx != null) && !ListBucketObjectsRegEx.isEmpty() ) {
			if ( ListBucketObjectsRegEx.toUpperCase().startsWith("JAVA:") ) {
				// Use as is for a Java regular expression.
				listBucketObjectsRegEx = ListBucketObjectsRegEx.substring(5);
			}
			else {
				// Default to glob so convert * to Java .*
				listBucketObjectsRegEx = ListBucketObjectsRegEx.replace("*", ".*");
			}
		}
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
		String ListFiles = parameters.getValue ( "ListFiles" );
		boolean listFiles = true; // Default.
		if ( (ListFiles != null) && ListFiles.equalsIgnoreCase("false") ) {
			listFiles = false;
		}
		String ListFolders = parameters.getValue ( "ListFolders" );
		boolean listFolders = true; // Default.
		if ( (ListFolders != null) && ListFolders.equalsIgnoreCase("false") ) {
			listFolders = false;
		}
    	String ListBucketObjectsCountProperty = parameters.getValue ( "ListBucketObjectsCountProperty" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		ListBucketObjectsCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ListBucketObjectsCountProperty);
    	}

    	// Upload.
    	String UploadFolders = parameters.getValue ( "UploadFolders" );
		UploadFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadFolders);
		// Can't use a hashtable because sometimes upload the same folders to multiple S3 locations.
    	List<String> uploadFoldersOrig = new ArrayList<>(); // For log messages.
    	List<String> uploadFoldersDirectoryList = new ArrayList<>();
    	List<String> uploadFoldersKeyList = new ArrayList<>();
       	int uploadFoldersCount = 0;
    	if ( (UploadFolders != null) && (UploadFolders.length() > 0) && (UploadFolders.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(UploadFolders, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++uploadFoldersCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String localFolder = parts[0].trim();
            		String remoteFolder = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			// Convert the command parameter folder to absolute path.
			   			String localFolderFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
			   			// Make sure that the local folder does not contain wildcard or ${ for properties. // } to match bracket
            			if ( localFolderFull.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") contains ${ due to unknown processor property - skipping to avoid upload error."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( localFolderFull.indexOf("*") >= 0 ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") contains wildcard * in name, which is not allowed - skipping folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Check the local folder name." ) );
        		   			continue;
        	   			}
            			File f = new File(localFolderFull);
            			if ( !f.exists() ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") does not exist - skipping to avoid upload error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder exists." ) );
        		   			continue;
        	   			}
            			if ( !f.isDirectory() ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") is not a folder - skipping.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder exists." ) );
        		   			continue;
        	   			}
            			if ( remoteFolder.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") contains ${ due to unknown processor property - skipping to avoid unexpected file on S3."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFolder.indexOf("*") >= 0 ) { // } to match bracket
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") contains * in name - skipping to avoid unexpected file on S3.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( !remoteFolder.endsWith("/") ) {
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") does not end in / to indicate a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder ends in / to indicate a folder." ) );
        		   			continue;
        	   			}
            			// If here then the input seems valid.
            			uploadFoldersOrig.add(localFolder);
               			uploadFoldersDirectoryList.add(localFolderFull);
               			uploadFoldersKeyList.add(remoteFolder);
            		}
            	}
        	}
    	}
    	String UploadFiles = parameters.getValue ( "UploadFiles" );
    	// Expand the entire parameter string before parsing into pairs.
		UploadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadFiles);
		// Can't use a hashtable because sometimes upload the same files to multiple S3 locations.
    	List<String> uploadFilesOrig = new ArrayList<>(); // For log messages.
    	List<String> uploadFilesFileList = new ArrayList<>();
    	List<String> uploadFilesKeyList = new ArrayList<>();
        int uploadFilesCount = 0;
    	if ( (UploadFiles != null) && !UploadFiles.isEmpty() && (UploadFiles.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(UploadFiles, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++uploadFilesCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String localFile = parts[0].trim();
            		String remoteFile = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			if ( localFile.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Local file " + uploadFilesCount + " (" + localFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid upload error."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.indexOf("${") >= 0 ) { // } to match bracket
       			   			message = "Remote file (object key) for file " + uploadFilesCount + " (" + remoteFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid unexpected file on S3."; // } to match bracket
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.endsWith("/") ) {
       			   			message = "Remote file (object key) " + uploadFilesCount + " (" + remoteFile +
       			   				") ends with /, which indicates a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the S3 file does not ends in /." ) );
        		   			continue;
        	   			}
            		}
            		if ( localFile.indexOf("*") >= 0 ) {
            			// Local file has a wildcard so it should be a folder with file wildcard pattern:
            			// - this does not handle * in the folder as in: folder/*/folder/file.*
            			Message.printStatus(2, routine, "Local file has a wildcard.");
            			if ( !remoteFile.endsWith("/*") ) {
            				// Remote file must end with /* so that local file can also be used on S3.
            				// This limits wildcards in the root folder but that is unlikely.
            				message = "Local file uses * wildcard but bucket key does not end in /* - skipping.";
			        		Message.printWarning(warningLevel,
				    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
				    			message, "Specify the bucket key with /* at the end." ) );
			        		continue;
            			}
            			// Local file has a wildcard so need to expand to matching files and then process each:
            			// - this will expand the leading folder(s) for properties
			   			String localFileFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
            			List<File> localPathList = null;
            			try {
            				Message.printStatus(2,routine,"Getting local file list using wildcard:" + localFileFull );
            				// The following method requires forward slashes.
            				localPathList = IOUtil.getFilesMatchingPattern("glob:" + localFileFull.replace("\\", "/"));
            				for ( File localPath : localPathList ) {
            					if ( remoteFile.endsWith("/*") ) {
            						 remoteFile = remoteFile.replace("*", localPath.getName());
            					}
            					uploadFilesOrig.add(localFile);
            					uploadFilesFileList.add(localPath.getAbsolutePath());
            					uploadFilesKeyList.add(remoteFile);
            					if ( Message.isDebugOn ) {
            						Message.printStatus(2, routine, "Local file from wildcard: " + localPath.getAbsolutePath() );
            						Message.printStatus(2, routine, "             Remote file: " + remoteFile );
            					}
            				}
            			}
            			catch ( Exception e ) {
            				message = "Error getting list of local files for \"" + localFileFull + "\" (" + e + ").";
			        		Message.printWarning(warningLevel,
				    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				    			message, "Report problem to software support." ) );
            			}
            		}
            		else {
            			// Simple file with no wildcard at the end.
            			Message.printStatus(2, routine, "Local file is a simple file.");
            			// Convert the command parameter local file to absolute path.
			   			String localFileFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
			   			File f = new File(localFileFull);
		   				if ( !f.exists() ) {
		   					// Local file does not exist.
		   					message = "Local file " + uploadFilesCount + " (" + localFile + ") does not exist - skipping.";
		   					Message.printWarning(warningLevel,
		   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		   					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		   						message, "Verify that the folder exists." ) );
		   					continue;
			   			}
		   				if ( f.isDirectory() ) {
		   					// File path is actually a folder.
		   					message = "Local file " + uploadFilesCount + " (" + localFile + ") is actually a folder - skipping.";
		   					Message.printWarning(warningLevel,
		   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		   					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		   						message, "Use the UploadFolders parameter to upload folders." ) );
		   					continue;
			   			}
            			// Have passed checks so can upload.
            			uploadFilesOrig.add(localFile);
               			uploadFilesFileList.add(localFileFull);
               			if ( remoteFile.endsWith("/*") ) {
               				Message.printStatus(2, routine, "Remote file ends with /*");
               				// Use the file from the local path and replace *.
               				remoteFile = remoteFile.replace("*", f.getName());
               				Message.printStatus(2, routine, "Remote file after replacing * is: " + remoteFile );
               			}
               			else {
               				// Just add the remote file as is without adjusting the remote name.
               			}
               			uploadFilesKeyList.add(remoteFile);
            		}
            	}
        	}
    	}

    	// Output.
		boolean doTable = false;
		String OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
		if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
			doTable = true;
		}
		// If an output file is to be written:
		// - output using the table, if available
		// - if an output table is not being created, create a temporary table and write it
		boolean doOutputFile = false;
		String OutputFile = parameters.getValue ( "OutputFile" ); // Expand below.
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			doOutputFile = true;
		}
		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    	IfInputNotFound = _Warn; // Default
		}
		String AppendOutput = parameters.getValue ( "AppendOutput" );
		boolean appendOutput = false;
		if ( (AppendOutput != null) && AppendOutput.equalsIgnoreCase(_True)) {
			appendOutput = true;
		}

    	// CloudFront.
		String InvalidateCloudFront = parameters.getValue ( "InvalidateCloudFront" );
		boolean invalidateCloudFront = false; // Default.
		if ( (InvalidateCloudFront != null) && InvalidateCloudFront.equalsIgnoreCase(this._True) ) {
			invalidateCloudFront = true;
		}
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
			callerReference = System.getProperty("user.name") + "-" + TimeUtil.formatDateTime(dt, "%Y-%m-%dT%H:%M:%S");
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

    	// Final checks, based on the command parameters:
		// - only warn if the original file and folder list was not empty
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
    			if ( (uploadFoldersCount != 0) && (uploadFoldersDirectoryList.size() == 0) ) {
      				message = "No folders were found for the upload.";
      				Message.printWarning(warningLevel,
      					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the UploadFolders parameter and existence of associated files." ) );
    			}
    			if ( (uploadFilesCount != 0) && (uploadFilesFileList.size() == 0) ) {
      				message = "No files were found for the upload.";
      				Message.printWarning(warningLevel,
      					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the UploadFiles parameter and existence of associated files." ) );
    			}
    		}
    	}

		// Get the table to process:
		// - only if appending
		// - if not appending, (re)create below

		DataTable table = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (OutputTableID != null) && !OutputTableID.equals("") && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", OutputTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		table = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing table for append.");
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
    	}

		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings about command parameters.";
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(commandTag, ++warningCount), routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		// Handle credentials.

		ProfileCredentialsProvider credentialsProvider0 = null;
		credentialsProvider0 = ProfileCredentialsProvider.create(profile);
		ProfileCredentialsProvider credentialsProvider = credentialsProvider0;

		// The following is used to create a temporary table before outputting to a file.
		//boolean useTempTable = false;

		Region regionObject = Region.of(region);

		S3Client s3 = S3Client.builder()
			.region(regionObject)
			.credentialsProvider(credentialsProvider)
			.build();

		try {
			// Create the table if it does not exist.

	    	// Make sure the table has the columns that are needed.
	    	if ( commandPhase == CommandPhaseType.RUN ) {

	    		// Create a session with the credentials.
	    		AwsSession awsSession = new AwsSession(profile);

    			// Column numbers are used later.

	    		// Bucket list columns.
        		int bucketNameCol = -1;
        		int bucketCreationDateCol = -1;

        		// Bucket object list columns.
        		int objectKeyCol = -1;
        		int objectNameCol = -1;
        		int objectParentNameCol = -1;
        		int objectTypeCol = -1;
   	    		int objectSizeCol = -1;
   	    		int objectOwnerCol = -1;
   	    		int objectLastModifiedCol = -1;

   	    		// List of paths to invalidate, if using copy, delete, or upload commands.
   	    		List<String> cloudFrontPaths = new ArrayList<>();

	    		if ( doTable || doOutputFile) {
	    			// Requested a table and/or file:
	    			// - if only file is request, create a temporary table that is then written to output
    	    		if ( (table == null) || !appendOutput ) {
    	        		// The table needs to be created because it does not exist or NOT appending (so need new table):
    	    			// - the table columns depend on the S3 command being executed
    	    			// 1. Define the column names based on S3 commands.
    	        		List<TableField> columnList = new ArrayList<>();
    	        		if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "CreationDate", -1) );
    	        		}
    	        		else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Key", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Name", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ParentName", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Type", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "Size", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Owner", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "LastModified", -1) );
    	        		}
    	        		// 2. Create the table if not found from the processor above.
    	        		if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
    	        			(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
    	        			// Create the table.
    	        			table = new DataTable( columnList );
    	        		}
                		// 3. Get the column numbers from the names for later use.
    	        		if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        			bucketNameCol = table.getFieldIndex("BucketName");
    	        			bucketCreationDateCol = table.getFieldIndex("CreationDate");
    	        		}
    	        		else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        			objectKeyCol = table.getFieldIndex("Key");
    	        			objectNameCol = table.getFieldIndex("Name");
    	        			objectParentNameCol = table.getFieldIndex("ParentName");
    	        			objectTypeCol = table.getFieldIndex("Type");
    	        			objectSizeCol = table.getFieldIndex("Size");
    	        			objectOwnerCol = table.getFieldIndex("Owner");
    	        			objectLastModifiedCol = table.getFieldIndex("LastModified");
    	        		}
    	        		// 4. Set the table in the processor:
    	        		//    - if new will add
    	        		//    - if append will overwrite by replacing the matching table ID
    	        		if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
    	        			(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
    	        			if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
    	        				table.setTableID ( OutputTableID );
                				Message.printStatus(2, routine, "Created new table \"" + OutputTableID + "\" for output.");
                				// Set the table in the processor:
                				// - do not set if a temporary table is being used for the output file
                				PropList requestParams = new PropList ( "" );
                				requestParams.setUsingObject ( "Table", table );
                				try {
                    				processor.processRequest( "SetTable", requestParams);
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
    	        				// Temporary table used for file only:
    	        				// - do not set in the processor
    	        				table.setTableID ( "AwsS3" );
    	        			}
    	        		}
    	        		// 5. The table contents will be filled in when the doS3* methods are called.
    	    		}
    	    		else {
    	    			// Table exists:
    	        		// - make sure that the needed columns exist and otherwise add them
    	        		if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	        			bucketNameCol = table.getFieldIndex("BucketName");
    	        			bucketCreationDateCol = table.getFieldIndex("CreationDate");
    	        			if ( bucketNameCol < 0 ) {
    	            			bucketNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "BucketName", -1), "");
    	        			}
    	        			if ( bucketCreationDateCol < 0 ) {
    	            			bucketCreationDateCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "CreationDate", -1), "");
    	        			}
    	        		}
    	        		else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
    	        			objectKeyCol = table.getFieldIndex("Key");
    	        			objectNameCol = table.getFieldIndex("Name");
    	        			objectParentNameCol = table.getFieldIndex("ParentName");
    	        			objectTypeCol = table.getFieldIndex("Key");
    	        			objectSizeCol = table.getFieldIndex("Size");
    	        			objectOwnerCol = table.getFieldIndex("Owner");
    	        			objectLastModifiedCol = table.getFieldIndex("LastModified");
    	        			if ( objectKeyCol < 0 ) {
    	            			objectKeyCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Key", -1), "");
    	        			}
    	        			if ( objectNameCol < 0 ) {
    	            			objectNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Name", -1), "");
    	        			}
    	        			if ( objectParentNameCol < 0 ) {
    	            			objectParentNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "ParentName", -1), "");
    	        			}
    	        			if ( objectTypeCol < 0 ) {
    	            			objectTypeCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Type", -1), "");
    	        			}
    	        			if ( objectSizeCol < 0 ) {
    	            			objectSizeCol = table.addField(new TableField(TableField.DATA_TYPE_LONG, "Size", -1), "");
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

    	    	// Call the service that was requested to create the requested output.
   	    		// S3Client:
   	    		//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html

    	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
    	    		warningCount = doS3CopyObjects(
    	    			processor,
		  	    		s3,
		  	    		bucket, copySourceKeyList, copyBucket, copyDestKeyList,
  	    				CopyObjectsCountProperty,
		  	    		cloudFrontPaths,
		  	    		status, logLevel, warningCount, commandTag );
    	    	}
    	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
    	    		warningCount = doS3DeleteObjects (
    	    			s3,
		  	    		bucket,
   	        			deleteFilesKeys, deleteFoldersKeys,
   	        			DeleteFoldersScope, deleteFoldersMinDepth,
		  	    		status, logLevel, warningLevel, warningCount, commandTag );
    	    	}
    	    	else if ( s3Command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
    	    		warningCount = doS3DownloadObjects (
    	    			processor,
    	    			credentialsProvider, bucket, region,
    	    			downloadFilesKeys, downloadFilesFiles, downloadFoldersKeys, downloadFoldersDirectories,
    	    			status, logLevel, warningLevel, warningCount, commandTag );
    	    	}
    	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	    		warningCount = doS3ListBuckets (
    	    			processor,
    	    			s3,
    	    			table, bucketNameCol, bucketCreationDateCol,
    	    			listBucketsRegEx, ListBucketsCountProperty,
    	    			status, logLevel, warningCount, commandTag );
    	    	}
   	        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
   	        		warningCount = doS3ListBucketObjects (
   	        			processor,
   	        			s3,
		 	       		bucket,
		 	       		ListBucketObjectsScope, Prefix, Delimiter, maxKeys, maxObjects,
		 	       		listFiles, listFolders, listBucketObjectsRegEx,
		 	       		table, objectKeyCol, objectTypeCol, objectNameCol, objectParentNameCol,
		 	       		ListBucketObjectsCountProperty,
		 	       		objectSizeCol, objectOwnerCol, objectLastModifiedCol,
		 	       		status, logLevel, warningCount, commandTag );
    	    	}
   	        	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
   	        		warningCount = doS3UploadObjects (
   	        			processor,
   	        			credentialsProvider,
   	        			bucket, region,
   	        			uploadFilesOrig, uploadFilesFileList, uploadFilesKeyList,
   	        			uploadFoldersOrig, uploadFoldersDirectoryList, uploadFoldersKeyList,
   	        			status, logLevel, warningLevel, warningCount, commandTag );
    	    	}

    	    	// If any files were copied, deleted, or uploaded and CloudFront invalidation is requested, do it.
	        	if ( invalidateCloudFront && (cloudFrontPaths.size() > 0) ) {
	        		warningCount = doCloudFrontInvalidation (
	        			awsSession,
	        			region, CloudFrontDistributionId, commentPattern,
	        			cloudFrontPaths, callerReference, waitForCompletion,
	        			status, logLevel, warningLevel, warningCount, commandTag );
	        	}

	        	// Create the output file:
	    	   	// - write the table to a delimited file
	    	   	// - TODO smalers 2023-01-28 for now do not write comments, keep very basic

	    	   	if ( doOutputFile ) {
	    		   	String OutputFile_full = IOUtil.verifyPathForOS(
	        		   	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            		   	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	    		   	if ( OutputFile_full.toUpperCase().endsWith("CSV") ) {
	    			   	boolean writeColumnNames = true;
	    			   	List<String> comments = null;
	    			   	String commentLinePrefix = "#";
	    			   	HashMap<String,Object> writeProps = new HashMap<>();
	    			   	if ( appendOutput && ((OutputTableID == null) || OutputTableID.isEmpty()) ) {
	    			   		// Requested append but the output table was not given:
	    			   		// - therefore the output table was a temporary table
	    			   		// - the output is only for this command so must append to the file (if it exists)
	    				   	writeProps.put("Append", "True");
	    			   	}
	    			   	table.writeDelimitedFile(OutputFile_full, ",", writeColumnNames, comments, commentLinePrefix, writeProps);
	           			setOutputFile(new File(OutputFile_full));
	    		   	}
	    		   	// TODO smalers 2023-01-31 need to implement.
	    		   	//else if ( OutputFile_full.toUpperCase().endsWith("JSON") ) {
	    		   	//}
	    		   	else {
                	   	message = "Requested output file has unknown extension - don't know how to write.";
                	   	Message.printWarning(warningLevel,
                		   	MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
                	   	status.addToLog ( commandPhase,
                		   	new CommandLogRecord(CommandStatusType.FAILURE,
                		   	message, "Use an output file with 'csv' file extension." ) );
	    		   	}
	    	   	}
	    	}
	    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	        	if ( (s3Command == AwsS3CommandType.LIST_BUCKETS) ||
   	        		(s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS) ) {
   	        		if ( (table == null) && (OutputTableID != null) && !OutputTableID.isEmpty() ) {
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
  	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
				message = "Unexpected error copying objects (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
				message = "Unexpected error deleting object (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
				message = "Unexpected error downloading object(s) (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
				message = "Unexpected error listing buckets (" + e.awsErrorDetails().errorMessage() + ").";
			}
        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
				message = "Unexpected error listing bucket objects (" + e.awsErrorDetails().errorMessage() + ").";
        	}
  	    	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
				message = "Unexpected error uploading object(s) (" + e.awsErrorDetails().errorMessage() + ").";
			}
			else {
				message = "Unexpected error for unknown S3 command (" + e.awsErrorDetails().errorMessage() + ": " + S3Command;
			}
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
    	}
    	catch ( Exception e ) {
  	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
				message = "Unexpected error copying objects (" + e + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
				message = "Unexpected error deleting object (" + e + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
				message = "Unexpected error downloading objects (" + e + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
				message = "Unexpected error listing buckets (" + e + ").";
			}
        	else if ( s3Command == AwsS3CommandType.LIST_BUCKET_OBJECTS ) {
				message = "Unexpected error listing bucket objects (" + e + ").";
        	}
  	    	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
				message = "Unexpected error uploading objects (" + e + ").";
			}
			else {
				message = "Unexpected error for unknown S3 command: " + S3Command;
			}
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
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
		// General.
		String S3Command = parameters.getValue("S3Command");
		String Profile = parameters.getValue("Profile");
		String Region = parameters.getValue("Region");
		String Bucket = parameters.getValue("Bucket");
		// Copy.
		String CopyKeys = parameters.getValue("CopyKeys");
		String CopyBucket = parameters.getValue("CopyBucket");
		String CopyObjectsCountProperty = parameters.getValue("CopyObjectsCountProperty");
		// Delete.
		String DeleteKeys = parameters.getValue("DeleteKeys");
		String DeleteFolders = parameters.getValue("DeleteFolders");
		String DeleteFoldersScope = parameters.getValue("DeleteFoldersScope");
		String DeleteFoldersMinDepth = parameters.getValue("DeleteFoldersMinDepth");
		// Download.
		String DownloadFolders = parameters.getValue("DownloadFolders");
		String DownloadFiles = parameters.getValue("DownloadFiles");
		// List buckets.
		String ListBucketsRegEx = parameters.getValue("ListBucketsRegEx");
		String ListBucketsCountProperty = parameters.getValue("ListBucketsCountProperty");
		// List bucket objects.
		String ListBucketObjectsScope = parameters.getValue("ListBucketObjectsScope");
		String Prefix = parameters.getValue("Prefix");
		String Delimiter = parameters.getValue("Delimiter");
		String ListBucketObjectsRegEx = parameters.getValue("ListBucketObjectsRegEx");
		String ListFiles = parameters.getValue("ListFiles");
		String ListFolders = parameters.getValue("ListFolders");
		String MaxKeys = parameters.getValue("MaxKeys");
		String MaxObjects = parameters.getValue("MaxObjects");
		String ListBucketObjectsCountProperty = parameters.getValue("ListBucketObjectsCountProperty");
		// Upload.
		String UploadFolders = parameters.getValue("UploadFolders");
		String UploadFiles = parameters.getValue("UploadFiles");
		// Output.
		String OutputTableID = parameters.getValue("OutputTableID");
		String OutputFile = parameters.getValue("OutputFile");
		String AppendOutput = parameters.getValue("AppendOutput");
		// CloudFront.
		String InvalidateCloudFront = parameters.getValue("InvalidateCloudFront");
		String CloudFrontRegion = parameters.getValue("CloudFrontRegion");
		String CloudFrontDistributionId = parameters.getValue("CloudFrontDistributionId");
		String CloudFrontComment = parameters.getValue("CloudFrontComment");
		String CloudFrontCallerReference = parameters.getValue("CloudFrontCallerReference");
		String CloudFrontWaitForCompletion = parameters.getValue("CloudFrontWaitForCompletion");
		String IfInputNotFound = parameters.getValue("IfInputNotFound");
		StringBuffer b = new StringBuffer ();
		// General.
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
		// Copy.
		if ( (CopyKeys != null) && (CopyKeys.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CopyKeys=\"" + CopyKeys + "\"");
		}
		if ( (CopyBucket != null) && (CopyBucket.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CopyBucket=\"" + CopyBucket + "\"");
		}
		if ( (CopyObjectsCountProperty != null) && (CopyObjectsCountProperty.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "CopyObjectsCountProperty=\"" + CopyObjectsCountProperty + "\"");
		}
		// Delete.
		if ( (DeleteKeys != null) && (DeleteKeys.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DeleteKeys=\"" + DeleteKeys + "\"");
		}
		if ( (DeleteFolders != null) && (DeleteFolders.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DeleteFolders=\"" + DeleteFolders + "\"");
		}
		if ( (DeleteFoldersScope != null) && (DeleteFoldersScope.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DeleteFoldersScope=\"" + DeleteFoldersScope + "\"");
		}
		if ( (DeleteFoldersMinDepth != null) && (DeleteFoldersMinDepth.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DeleteFoldersMinDepth=\"" + DeleteFoldersMinDepth + "\"");
		}
		// Download.
		if ( (DownloadFolders != null) && (DownloadFolders.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DownloadFolders=\"" + DownloadFolders + "\"");
		}
		if ( (DownloadFiles != null) && (DownloadFiles.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "DownloadFiles=\"" + DownloadFiles + "\"");
		}
		// List buckets.
		if ( (ListBucketsRegEx != null) && (ListBucketsRegEx.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListBucketsRegEx=\"" + ListBucketsRegEx + "\"");
		}
		if ( (ListBucketsCountProperty != null) && (ListBucketsCountProperty.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListBucketsCountProperty=\"" + ListBucketsCountProperty + "\"");
		}
		// List bucket objects.
		if ( (ListBucketObjectsScope != null) && (ListBucketObjectsScope.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListBucketObjectsScope=" + ListBucketObjectsScope );
		}
		if ( (Prefix != null) && (Prefix.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "Prefix=\"" + Prefix + "\"");
		}
		if ( (Delimiter != null) && (Delimiter.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "Delimiter=\"" + Delimiter + "\"");
		}
		if ( (ListBucketObjectsRegEx != null) && (ListBucketObjectsRegEx.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListBucketObjectsRegEx=\"" + ListBucketObjectsRegEx + "\"");
		}
		if ( (ListFiles != null) && (ListFiles.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListFiles=" + ListFiles );
		}
		if ( (ListFolders != null) && (ListFolders.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListFolders=" + ListFolders );
		}
		if ( (MaxKeys != null) && (MaxKeys.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "MaxKeys=" + MaxKeys );
		}
		if ( (MaxObjects != null) && (MaxObjects.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "MaxObjects=" + MaxObjects );
		}
		if ( (ListBucketObjectsCountProperty != null) && (ListBucketObjectsCountProperty.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "ListBucketObjectsCountProperty=\"" + ListBucketObjectsCountProperty + "\"");
		}
		// Upload.
		if ( (UploadFolders != null) && (UploadFolders.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "UploadFolders=\"" + UploadFolders + "\"");
		}
		if ( (UploadFiles != null) && (UploadFiles.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
			b.append ( "UploadFiles=\"" + UploadFiles + "\"");
		}
		// Output.
    	if ( (OutputTableID != null) && (OutputTableID.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "OutputTableID=\"" + OutputTableID + "\"" );
    	}
    	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "OutputFile=\"" + OutputFile + "\"");
    	}
    	if ( (AppendOutput != null) && (AppendOutput.length() > 0) ) {
        	if ( b.length() > 0 ) {
            	b.append ( "," );
        	}
        	b.append ( "AppendOutput=\"" + AppendOutput + "\"");
    	}
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
		if ( (IfInputNotFound != null) && !IfInputNotFound.isEmpty() ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "IfInputNotFound=" + IfInputNotFound );
		}
		return getCommandName() + "(" + b.toString() + ")";
	}

}