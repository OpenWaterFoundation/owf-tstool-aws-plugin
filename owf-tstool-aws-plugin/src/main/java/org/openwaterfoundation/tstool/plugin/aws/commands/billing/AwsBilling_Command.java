// AwsBilling_Command - This class initializes, checks, and runs the Aws_Billing() command.

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.CostExplorerException;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.Dimension;
import software.amazon.awssdk.services.costexplorer.model.DimensionValues;
import software.amazon.awssdk.services.costexplorer.model.Expression;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageResponse;
import software.amazon.awssdk.services.costexplorer.model.Group;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinition;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinitionType;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.costexplorer.model.ResultByTime;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpnConnection;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
//import software.amazon.awssdk.services.pricing.model.Filter;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.TS.TSUtil;
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

import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the AwsBilling() command.
*/
public class AwsBilling_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{

	/**
	Data members used for parameter values.
	*/
	protected final String _False = "False";
	protected final String _True = "True";

	/**
	Data members used for parameter values.
	*/
	protected final String _Total = "Total";

	/**
	Data members used for parameter values.
	*/
	protected final String _Ignore = "Ignore";
	protected final String _Warn = "Warn";
	protected final String _Fail = "Fail";

	/**
	 * Data members used for GroupedTimeSeriesLocationID parameter values.
	 */
	protected final String _Auto = "Auto";
	protected final String _GroupBy1 = "GroupBy1";
	protected final String _GroupBy2 = "GroupBy2";

	/**
	 * Value used for TimeSeriesMissingGroupBy default.
	 */
	protected final String _Unknown = "Unknown";

	/**
	Grouped table output file that is created by this command.
	*/
	private File __GroupedTableFile_File = null;

	/**
	Total table output file that is created by this command.
	*/
	private File __TotalTableFile_File = null;

	/**
	The output grouped table that is created for discovery mode.
	*/
	private DataTable discoveryGroupedTable = null;

	/**
	The output total table that is created for discovery mode.
	*/
	private DataTable discoveryTotalTable = null;

	/**
	The output EC2 images table that is created for discovery mode.
	*/
	private DataTable discoveryEc2ImagesTable = null;

	/**
	The output EC2 properties table that is created for discovery mode.
	*/
	private DataTable discoveryEc2PropertiesTable = null;

	/**
	The output EBS snapshots table that is created for discovery mode.
	*/
	private DataTable discoveryEbsSnapshotsTable = null;

	/**
	 * Position of value 1 in the metadata array.
	 */
	int metadataValue1Index = 0;

	/**
	 * Position of value 2 in the metadata array.
	 */
	int metadataValue2Index = 3;

	/**
	 * Position of metric in the metadata array.
	 */
	int metadataMetricIndex = 6;

	/**
	Constructor.
	*/
	public AwsBilling_Command () {
		super();
		setCommandName ( "AwsBilling" );
	}

	/**
	 * Add a record of data to a grouped time series.
	 * This may result in creating a new time series or adding a record to an existing time series.
	 * @param tslist the list of existing time series (will be modified as needed)
	 * @param inputStart the starting date for the data (full period, not time chunk)
	 * @param inputEnd the ending date for the data (full period, not time chunk)
	 * @param startDate the start date associated with the cost item
	 * @param granularity the cost data granularity (hourly, daily, or monthly)
	 * @param groupBy1 the "group by 1" enumeration, used in the time series identifier
	 * @param groupByTag1 "group by tag 1" string, used in the time series identifier
	 * @param groupItem1 the "group 1 item" string, used in the time series identifier
	 * @param groupBy2 the "group by 2" enumeration, used in the time series identifier
	 * @param groupByTag2 the "group by tag 2" string, used in the time series identifier
	 * @param groupItem2 the "group 2 item" string, used in the time series identifier
	 * @param metricName the AWS metric name (how costs are computed)
	 * @param costAmount the cost amount
	 * @param costUnit the cost units
	 * @param timeSeriesLocationID the time series location identifier (e.g., from the tag)
	 * @param timeSeriesDataType the time series location identifier (e.g., from the tag)
	 * @param timeSeriesMissingGroupBy string to use if GroupBy is missing (e.g., tag before it was assigned)
	 * @param metadataNames names of metadata used to format the time series locationID and data type
	 * @param metadataValues values of metadata used to format the time series locationID and data type
	 */
	private void addGroupedTimeSeriesData (
		List<TS> tslist,
		DateTime inputStart, DateTime inputEnd,
		DateTime startDate, AwsBillingGranularityType granularity,
    	AwsBillingDimensionType groupBy1, String groupByTag1, String groupItem1,
    	AwsBillingDimensionType groupBy2, String groupByTag2, String groupItem2,
    	String metricName, Double costAmount, String costUnit,
    	String timeSeriesLocationID, String timeSeriesDataType, String timeSeriesMissingGroupBy,
    	String [] metadataNames, String [] metadataValues
	) throws Exception {
		// Create a time series identifier string using the input.
		/*
		if ( groupItem1.isEmpty() ) {
			groupItem1 = "Unknown";
		}
		if ( groupItem2.isEmpty() ) {
			groupItem2 = "Unknown";
		}
		*/
		/*
		if ( groupItem3.isEmpty() ) {
			groupItem3 = "Unknown";
		}
		*/
		// Update the metadata values for the group items:
		// - if null or empty and timeSeriesMissingGroupBy is specified
		if ( (groupItem1 == null) || groupItem1.isEmpty() ) {
			if ( (timeSeriesMissingGroupBy != null) && !timeSeriesMissingGroupBy.isEmpty() ) {
				groupItem1 = timeSeriesMissingGroupBy;
			}
		}
		if ( (groupItem2 == null) || groupItem2.isEmpty() ) {
			if ( (timeSeriesMissingGroupBy != null) && !timeSeriesMissingGroupBy.isEmpty() ) {
				groupItem2 = timeSeriesMissingGroupBy;
			}
		}
		metadataValues[this.metadataValue1Index] = groupItem1; // Probably not null.
		metadataValues[this.metadataValue2Index] = groupItem2; // Might be null (could be reset above).
		metadataValues[this.metadataMetricIndex] = metricName; // Might be null (could be reset above).
		StringBuilder tsidBuilder = new StringBuilder();
		formatTimeSeriesLocationID (
			tsidBuilder, timeSeriesLocationID, timeSeriesMissingGroupBy,
			groupBy1, groupByTag1, groupItem1,
			groupBy2, groupByTag2, groupItem2,
			metadataNames, metadataValues);
		// Add the data source.
		tsidBuilder.append(".");
		tsidBuilder.append("AWS");
		// Add the data type.
		tsidBuilder.append(".");
		formatTimeSeriesDataType (
			tsidBuilder, timeSeriesLocationID, timeSeriesDataType,
			groupBy1, groupByTag1, groupItem1,
			groupBy2, groupByTag2, groupItem2,
			metricName,
			metadataNames, metadataValues);
		// Add the data interval.
		tsidBuilder.append(".");
		tsidBuilder.append(granularity.getInterval());
		// Get the TSID string to use below.
		String tsid = tsidBuilder.toString();

		// Search the exiting time series for a matching time series identifier.
		TS ts = null;
		for ( TS ts0 : tslist ) {
			if ( tsid.equals(ts0.getIdentifierString()) ) {
				// Found a matching time series.
				ts = ts0;
				break;
			}
		}

		if ( ts == null ) {
			// Need to create a new time series.
			ts = TSUtil.newTimeSeries(tsid, true);
			ts.setIdentifier(tsid);
			ts.setDate1(inputStart);
			ts.setDate1Original(inputStart);
			ts.setDate2(inputEnd);
			ts.setDate2Original(inputEnd);
			ts.setDataUnits(costUnit);
			ts.setDataUnitsOriginal(costUnit);
			ts.allocateDataSpace();

			// Add the time series to the list of output time series.
			tslist.add(ts);
		}

		// Set the data value for the single cost explorer data value for the matching time series determined above:
		// - increment the previous value because multiple records may be processed with same identifier data
		// - this is less likely if grouping is used
		double costAmountOld = ts.getDataValue(startDate);
		if ( ts.isDataMissing(costAmountOld) ) {
			// Previous value is missing:
			// - just set the value.
			ts.setDataValue ( startDate, costAmount );
		}
		else {
			// Have a non-missing previous value:
			// - add to it
			ts.setDataValue ( startDate, (costAmountOld + costAmount) );
		}
	}

	/**
	 * Add a record of data to a total time series.
	 * This may result in creating a new time series or adding a record to an existing time series.
	 * @param tslist the list of existing time series (will be modified as needed)
	 * @param inputStart the starting date for the data (full period, not time chunk)
	 * @param inputEnd the ending date for the data (full period, not time chunk)
	 * @param startDate the start date associated with the cost item
	 * @param granularity the cost data granularity (hourly, daily, or monthly)
	 * @param groupBy1 the "group by 1" enumeration, used in the time series identifier
	 * @param groupByTag1 "group by tag 1" string, used in the time series identifier
	 * @param groupItem1 the "group 1 item" string, used in the time series identifier
	 * @param groupBy2 the "group by 2" enumeration, used in the time series identifier
	 * @param groupByTag2 the "group by tag 2" string, used in the time series identifier
	 * @param groupItem2 the "group 2 item" string, used in the time series identifier
	 * @param metricName the AWS metric name (how costs are computed)
	 * @param costAmount the cost amount
	 * @param costUnit the cost units
	 * @param timeSeriesLocationID the time series location identifier (e.g., from the tag)
	 * @param timeSeriesDataType the time series location identifier (e.g., from the tag)
	 * @param timeSeriesMissingGroupBy string to use if GroupBy is missing (e.g., tag before it was assigned)
	 * @param metadataNames names of metadata used to format the time series locationID and data type
	 * @param metadataValues values of metadata used to format the time series locationID and data type
	 */
	private void addTotalTimeSeriesData (
		List<TS> tslist,
		DateTime inputStart, DateTime inputEnd,
		DateTime startDate, AwsBillingGranularityType granularity,
    	AwsBillingDimensionType groupBy1, String groupByTag1, String groupItem1,
    	AwsBillingDimensionType groupBy2, String groupByTag2, String groupItem2,
    	String metricName, Double costAmount, String costUnit,
    	String timeSeriesLocationID, String timeSeriesDataType, String timeSeriesMissingGroupBy,
    	String [] metadataNames, String [] metadataValues
	) throws Exception {
		// Create a time series identifier string using the input.
		/*
		if ( groupItem1.isEmpty() ) {
			groupItem1 = "Unknown";
		}
		if ( groupItem2.isEmpty() ) {
			groupItem2 = "Unknown";
		}
		*/
		/*
		if ( groupItem3.isEmpty() ) {
			groupItem3 = "Unknown";
		}
		*/
		// Update the metadata values for the group items:
		// - if null or empty and timeSeriesMissingGroupBy is specified
		if ( (groupItem1 == null) || groupItem1.isEmpty() ) {
			if ( (timeSeriesMissingGroupBy != null) && !timeSeriesMissingGroupBy.isEmpty() ) {
				groupItem1 = timeSeriesMissingGroupBy;
			}
		}
		if ( (groupItem2 == null) || groupItem2.isEmpty() ) {
			if ( (timeSeriesMissingGroupBy != null) && !timeSeriesMissingGroupBy.isEmpty() ) {
				groupItem2 = timeSeriesMissingGroupBy;
			}
		}
		metadataValues[this.metadataValue1Index] = groupItem1; // Probably not null.
		metadataValues[this.metadataValue2Index] = groupItem2; // Might be null (could be reset above).
		metadataValues[this.metadataMetricIndex] = metricName; // Might be null (could be reset above).
		StringBuilder tsidBuilder = new StringBuilder();
		formatTimeSeriesLocationID (
			tsidBuilder, timeSeriesLocationID, timeSeriesMissingGroupBy,
			groupBy1, groupByTag1, groupItem1,
			groupBy2, groupByTag2, groupItem2,
			metadataNames, metadataValues);
		// Add the data source.
		tsidBuilder.append(".");
		tsidBuilder.append("AWS");
		// Add the data type.
		tsidBuilder.append(".");
		formatTimeSeriesDataType (
			tsidBuilder, timeSeriesLocationID, timeSeriesDataType,
			groupBy1, groupByTag1, groupItem1,
			groupBy2, groupByTag2, groupItem2,
			metricName,
			metadataNames, metadataValues);
		// Add the data interval.
		tsidBuilder.append(".");
		tsidBuilder.append(granularity.getInterval());
		// Get the TSID string to use below.
		String tsid = tsidBuilder.toString();

		// Search the exiting time series for a matching time series identifier.
		TS ts = null;
		for ( TS ts0 : tslist ) {
			if ( tsid.equals(ts0.getIdentifierString()) ) {
				// Found a matching time series.
				ts = ts0;
				break;
			}
		}

		if ( ts == null ) {
			// Need to create a new time series.
			ts = TSUtil.newTimeSeries(tsid, true);
			ts.setIdentifier(tsid);
			ts.setDate1(inputStart);
			ts.setDate1Original(inputStart);
			ts.setDate2(inputEnd);
			ts.setDate2Original(inputEnd);
			ts.setDataUnits(costUnit);
			ts.setDataUnitsOriginal(costUnit);
			ts.allocateDataSpace();

			// Add the time series to the list of output time series.
			tslist.add(ts);
		}

		// Set the data value for the single cost explorer data value for the matching time series determined above:
		// - increment the previous value because multiple records may be processed with same identifier data
		// - this is less likely if grouping is used
		double costAmountOld = ts.getDataValue(startDate);
		if ( ts.isDataMissing(costAmountOld) ) {
			// Previous value is missing:
			// - just set the value.
			ts.setDataValue ( startDate, costAmount );
		}
		else {
			// Have a non-missing previous value:
			// - add to it
			ts.setDataValue ( startDate, (costAmountOld + costAmount) );
		}
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
		// General.
		String Profile = parameters.getValue ( "Profile" );
		String Region = parameters.getValue ( "Region" );
		// Cost Explorer.
    	String InputStart = parameters.getValue ( "InputStart" );
    	String InputEnd = parameters.getValue ( "InputEnd" );
    	String TimeChunk = parameters.getValue ( "TimeChunk" );
    	String Granularity = parameters.getValue ( "Granularity" );
    	String GroupBy1 = parameters.getValue ( "GroupBy1" );
    	String GroupByTag1 = parameters.getValue ( "GroupByTag1" );
    	String GroupBy2 = parameters.getValue ( "GroupBy2" );
    	String GroupByTag2 = parameters.getValue ( "GroupByTag2" );
    	//String GroupBy3 = parameters.getValue ( "GroupBy3" );
    	//String GroupByTag3 = parameters.getValue ( "GroupByTag3" );
    	String Metric = parameters.getValue ( "Metric" );
    	// Output Cost Tables.
    	String GroupedTableID = parameters.getValue ( "GroupedTableID" );
    	String GroupedTableFile = parameters.getValue ( "GroupedTableFile" );
    	String TotalTableID = parameters.getValue ( "TotalTableID" );
    	String TotalTableFile = parameters.getValue ( "TotalTableFile" );
    	String AppendOutput = parameters.getValue ( "AppendOutput" );
    	// Output Time Series.
    	String CreateGroupedTimeSeries = parameters.getValue ( "CreateGroupedTimeSeries" );
    	String GroupedTimeSeriesLocationID = parameters.getValue ( "GroupedTimeSeriesLocationID" );
    	String GroupedTimeSeriesDataType = parameters.getValue ( "GroupedTimeSeriesDataType" );
    	String TimeSeriesMissingGroupBy = parameters.getValue ( "TimeSeriesMissingGroupBy" );
    	String CreateTotalTimeSeries = parameters.getValue ( "CreateTotalTimeSeries" );
    	String TotalTimeSeriesLocationID = parameters.getValue ( "TotalTimeSeriesLocationID" );
    	String TotalTimeSeriesDataType = parameters.getValue ( "TotalTimeSeriesDataType" );

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

		if ( (TimeChunk != null) && !TimeChunk.isEmpty() ) {
			try {
				TimeInterval timeChunk = TimeInterval.parseInterval(TimeChunk);
				if ( timeChunk.getBase() != TimeInterval.DAY ) {
					message = "The TimeChunk (" + TimeChunk + ") is invalid.";
					warning += "\n" + message;
					status.addToLog(CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a mutipleof days (e.g., 30Day)."));
				}
			}
			catch ( InvalidTimeIntervalException e ) {
				message = "The TimeChunk (" + TimeChunk + ") is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify an interval (e.g., 30Day)."));
			}
		}

		if ( (Granularity != null) && !Granularity.isEmpty() ) {
			AwsBillingGranularityType granularity = AwsBillingGranularityType.valueOfIgnoreCase(Granularity);
			if ( granularity == null ) {
				message = "The Granularity (" + Granularity + ") is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as one of: " + AwsBillingGranularityType.getChoicesAsCsv()));
			}
		}

		if ( (GroupBy1 != null) && !GroupBy1.isEmpty() ) {
			AwsBillingDimensionType dimension = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy1);
			if ( dimension == null ) {
				message = "The GroupBy1 (" + GroupBy1 + ") value is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as one of: " + AwsBillingDimensionType.getChoicesAsCsv()));
			}

			if ( GroupBy1.equalsIgnoreCase("" + AwsBillingDimensionType.TAG) && ((GroupByTag1 == null) || GroupByTag1.isEmpty()) ) {
				message = "The GroupByTag1 must be specified if GroupBy1=Tag is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the GroupByTag1."));
			}
		}

		if ( (GroupBy1 == null) || GroupBy1.isEmpty() ) {
			if ( (GroupByTag1 != null) && !GroupByTag1.isEmpty() ) {
				message = "The GroupByTag1 should only be specified if GroupBy1 is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify GroupBy1 or clear the GroupByTag1."));
			}
		}

		if ( (GroupBy2 != null) && !GroupBy2.isEmpty() ) {
			AwsBillingDimensionType dimension = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy2);
			if ( dimension == null ) {
				message = "The GroupBy2 (" + GroupBy2 + ") value is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as one of: " + AwsBillingDimensionType.getChoicesAsCsv()));
			}

			if ( GroupBy2.equalsIgnoreCase("" + AwsBillingDimensionType.TAG) && ((GroupByTag2 == null) || GroupByTag2.isEmpty()) ) {
				message = "The GroupByTag2 must be specified if GroupBy2=Tag is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the GroupByTag2."));
			}
		}

		if ( (GroupBy2 == null) || GroupBy2.isEmpty() ) {
			if ( (GroupByTag2 != null) && !GroupByTag2.isEmpty() ) {
				message = "The GroupByTag2 should only be specified if GroupBy2 is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify GroupBy2 or clear the GroupByTag2."));
			}
		}

		if ( (Metric != null) && !Metric.isEmpty() ) {
			AwsBillingMetricType aggregateCostsBy = AwsBillingMetricType.valueOfIgnoreCase(Metric);
			if ( aggregateCostsBy == null ) {
				message = "The Metric (" + Metric + ") value is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as one of: " + AwsBillingMetricType.getChoicesAsCsv()));
			}
		}

		// Output table.
		int tableCount = 0;
		if ( (GroupedTableID != null) && !GroupedTableID.equals("") ) {
			++tableCount;
		}
		if ( (TotalTableID != null) && !TotalTableID.equals("") ) {
			++tableCount;
		}
		// Only allow grouped OR total table.
		if ( tableCount >= 2 ) {
			message = "The GroupedTableID and TotalTableID parameters cannot both be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify zero or one of GroupedTableID or TotalTableID."));
		}

		// Output file.
		int fileCount = 0;
		if ( (GroupedTableFile != null) && !GroupedTableFile.equals("") ) {
			++fileCount;
		}
		if ( (TotalTableFile != null) && !TotalTableFile.equals("") ) {
			++fileCount;
		}
		// Only allow grouped OR total file.
		if ( fileCount >= 2 ) {
			message = "The GroupedTableFile and TotalTableFile parameters cannot both be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify zero or one of GroupedTableFile or TotalTableFile."));
		}

		// Time series.
		int tsCount = 0;
		if ( (CreateGroupedTimeSeries != null) && !CreateGroupedTimeSeries.equals("") ) {
			++tsCount;
			if ( !CreateGroupedTimeSeries.equalsIgnoreCase(_False) && !CreateGroupedTimeSeries.equalsIgnoreCase(_True) ) {
				message = "The CreateGroupedTimeSeries parameter \"" + CreateGroupedTimeSeries + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
			}
		}

		if ( (GroupedTimeSeriesLocationID != null) && !GroupedTimeSeriesLocationID.equals("") ) {
			// Allow free-form text.
			/*
			if ( !TimeSeriesLocationID.equalsIgnoreCase(_Auto) && !TimeSeriesLocationID.equalsIgnoreCase(_GroupBy1) &&
				!TimeSeriesLocationID.equalsIgnoreCase(_GroupBy2) ) { //&& !TimeSeriesLocationID.equalsIgnoreCase(_GroupBy3) ) { // }
				message = "The TimeSeriesLocationID parameter \"" + TimeSeriesLocationID + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Auto + " (default), " + _GroupBy1 + ", or " + _GroupBy2 + "." )); //", or " + _GroupBy3 + "."));
			}
			*/
			if ( GroupedTimeSeriesLocationID.contains(".") ) {
				message = "The GroupedTimeSeriesLocationID parameter \"" + GroupedTimeSeriesLocationID + "\" is invalid (contains a period).";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "The GroupedTimeSeriesLocationID cannot contain a period."));
			}
		}

		if ( (GroupedTimeSeriesDataType != null) && !GroupedTimeSeriesDataType.equals("") ) {
			if ( GroupedTimeSeriesDataType.contains(".") ) {
				message = "The GroupedTimeSeriesDataType parameter \"" + GroupedTimeSeriesDataType + "\" is invalid (contains a period).";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "The GroupedTimeSeriesDataType cannot contain a period."));
			}
		}

		if ( (TimeSeriesMissingGroupBy != null) && !TimeSeriesMissingGroupBy.equals("") ) {
			if ( TimeSeriesMissingGroupBy.contains(".") ) {
				message = "The TimeSeriesMissingGroupBy parameter \"" + TimeSeriesMissingGroupBy + "\" is invalid (contains a period).";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "The TimeSeriesMissingGroupBy cannot contain a period."));
			}
		}

		if ( (CreateTotalTimeSeries != null) && !CreateTotalTimeSeries.equals("") ) {
			++tsCount;
			if ( !CreateTotalTimeSeries.equalsIgnoreCase(_False) && !CreateTotalTimeSeries.equalsIgnoreCase(_True) ) {
				message = "The CreateTotalTimeSeries parameter \"" + CreateTotalTimeSeries + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
			}
		}

		if ( (TotalTimeSeriesLocationID != null) && !TotalTimeSeriesLocationID.equals("") ) {
			// Allow free-form text.
			if ( TotalTimeSeriesLocationID.contains(".") ) {
				message = "The TotalTimeSeriesLocationID parameter \"" + TotalTimeSeriesLocationID + "\" is invalid (contains a period).";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "The TotalTimeSeriesLocationID cannot contain a period."));
			}
		}

		if ( (TotalTimeSeriesDataType != null) && !TotalTimeSeriesDataType.equals("") ) {
			if ( TotalTimeSeriesDataType.contains(".") ) {
				message = "The TotalTimeSeriesDataType parameter \"" + TotalTimeSeriesDataType + "\" is invalid (contains a period).";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "The TotalTimeSeriesDataType cannot contain a period."));
			}
		}

		// Only allow grouped OR total time series.
		if ( tsCount >= 2 ) {
			message = "The CreateGroupedTimeSeries and CreateTotalTimeSeries parameters cannot both be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify zero or one of CreateGroupedTimeSeries or CreateTotalTimeSeries."));
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

		if ( (tableCount > 0) || (fileCount > 0) ) {
			// Processing costs so need a period.
			if ( (InputStart != null) && (InputStart.indexOf("${") < 0) ) { // }
				try {
					DateTime.parse(InputStart);
				}
				catch ( Exception e ) {
					message = "The input start (" + InputStart + ") is invalid.";
					warning += "\n" + message;
					status.addToLog(CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the input start as YYYY-MM-DD or a ${Property}."));
				}
			}

			if ( (InputEnd != null) && (InputEnd.indexOf("${") < 0) ) { // }
				try {
					DateTime.parse(InputEnd);
				}
				catch ( Exception e ) {
					message = "The input end (" + InputEnd + ") is invalid.";
					warning += "\n" + message;
					status.addToLog(CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the input end as YYYY-MM-DD or a ${Property}."));
				}
			}
		}

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(27);
		// General.
		validList.add ( "Profile" );
		validList.add ( "Region" );
		// Cost Explorer Query.
		validList.add ( "InputStart" );
		validList.add ( "InputEnd" );
		validList.add ( "TimeChunk" );
		validList.add ( "Granularity" );
		validList.add ( "GroupBy1" );
		validList.add ( "GroupByTag1" );
		validList.add ( "GroupBy2" );
		validList.add ( "GroupByTag2" );
		//validList.add ( "GroupBy3" );
		//validList.add ( "GroupByTag3" );
		validList.add ( "Metric" );
		// Cost Explorer Filter.
		validList.add ( "FilterAvailabilityZones" );
		validList.add ( "FilterInstanceTypes" );
		validList.add ( "FilterRegions" );
		validList.add ( "FilterServices" );
		validList.add ( "FilterTags" );
		// Output Cost Tables.
		validList.add ( "GroupedTableID" );
		validList.add ( "GroupedTableFile" );
		validList.add ( "GroupedTableRowCountProperty" );
		validList.add ( "TotalTableID" );
		validList.add ( "TotalTableFile" );
		validList.add ( "TotalTableRowCountProperty" );
		validList.add ( "AppendOutput" );
		// Output Time series.
		validList.add ( "CreateGroupedTimeSeries" );
		validList.add ( "GroupedTimeSeriesLocationID" );
		validList.add ( "GroupedTimeSeriesDataType" );
		validList.add ( "GroupedTimeSeriesAlias" );
		validList.add ( "TimeSeriesMissingGroupBy" );
		validList.add ( "CreateTotalTimeSeries" );
		validList.add ( "TotalTimeSeriesLocationID" );
		validList.add ( "TotalTimeSeriesDataType" );
		validList.add ( "TotalTimeSeriesAlias" );
		// Output Service Properties.
		validList.add ( "EC2PropertiesTableID" );
		validList.add ( "EBSSnapshotsTableID" );
		validList.add ( "EC2ImagesTableID" );
		//
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
	 * Clean up a Group By key to be usable in output.
	 * For example, when a Group By is used with a tag "SystemId",
	 * the key will have a value of "SystemId$" if no matching tag is found and
	 * "SystemId$TagValue" if a matching tag is found.
	 * Because the tag is already known, the leading "SystemId$" is stripped and returned.
	 * @param key the group key before cleaning (e.g, "SystemId$TagValue")
	 * @return the group key after cleaning (e.g., "TagValue")
	 */
	private String cleanGroupKey ( String key ) {
		// Have to be careful because if ${Property} is used and does not get
		int pos1 = key.indexOf("$");
		int pos2 = key.indexOf("${");
		// Currently this only checks for the first instance.
		if ( (pos1 > 0) && (pos1 != pos2) ) {
			// Found a $ so return what is after it, may be an empty string.
			if ( pos1 == (key.length() - 1) ) {
				// End of string, OK to return an empty string.
				return "";
			}
			else {
				// Strip off the $ and following characters.
				return key.substring(pos1 + 1);
			}
		}
		else {
			// Return the original string.
			return key;
		}
	}

	/**
	 * Create the EBS snapshots table columns.
	 * @param ebsSnapshotsTable table for EBS snapshots
	 * @param ec2TagNameList list of tag names used with EC2
	 * @param ebsSnapshotsTagNameList list of tag names used with EBS snapshots
	 * @param tableColMap map for table column names and numbers
	 */
	private void createEbsSnapshotsTableColumns (
		DataTable ebsSnapshotsTable,
		List<String> ec2TagNameList,
		List<String> ebsSnapshotsTagNameList,
		HashMap<String,Integer> tableColMap )
		throws Exception {
		// Column numbers are used later:
  		// - initialize to -1 to indicate the column is not used
   		//tableColMap.put ( "ServiceCol", Integer.valueOf(-1) );
   		tableColMap.put ( "RegionCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2/InstanceIdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EBSSnapshot/IdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EBSSnapshot/DescriptionCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EBSSnapshot/VolumeSizeGBCol", Integer.valueOf(-1) );
   		for ( String tagName : ebsSnapshotsTagNameList ) {
   			tableColMap.put ( "EBSSnapshot-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}

   		// Check for column existence and create if not found.
   		// General columns.
       	//int serviceCol = servicePropertiesTable.getFieldIndex("ServiceCol", false);
       	//if ( serviceCol < 0 ) {
       	//	serviceCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Service", -1), "");
        // 	tableColMap.put("ServiceCol", Integer.valueOf(serviceCol));
       //	}
       	int regionCol = ebsSnapshotsTable.getFieldIndex("RegionCol", false);
       	if ( regionCol < 0 ) {
       		regionCol = ebsSnapshotsTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Region", -1), "");
         	tableColMap.put("RegionCol", Integer.valueOf(regionCol));
       	}
       	int snapshotIdCol = ebsSnapshotsTable.getFieldIndex("EBSSnapshot/IdCol", false);
       	if ( snapshotIdCol < 0 ) {
       		snapshotIdCol = ebsSnapshotsTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSSnapshot/Id", -1), "");
         	tableColMap.put("EBSSnapshot/IdCol", Integer.valueOf(snapshotIdCol));
       	}
       	int snapshotDescriptionCol = ebsSnapshotsTable.getFieldIndex("EBSSnapshot/DescriptionCol", false);
       	if ( snapshotDescriptionCol < 0 ) {
       		snapshotDescriptionCol = ebsSnapshotsTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSSnapshot/Description", -1), "");
         	tableColMap.put("EBSSnapshot/DescriptionCol", Integer.valueOf(snapshotDescriptionCol));
       	}
       	int snapshotVolumeSizeCol = ebsSnapshotsTable.getFieldIndex("EBSSnapshot/VolumeSizeGBCol", false);
       	if ( snapshotVolumeSizeCol < 0 ) {
       		snapshotDescriptionCol = ebsSnapshotsTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSSnapshot/VolumeSizeGB", -1), "");
         	tableColMap.put("EBSSnapshot/VolumeSizeGBCol", Integer.valueOf(snapshotDescriptionCol));
       	}
   		for ( String tagName : ebsSnapshotsTagNameList ) {
   			int col = ebsSnapshotsTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = ebsSnapshotsTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSSnapshot-Tag/" + tagName, -1), "");
   				tableColMap.put ( "EBSSnapshot-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
	}

	/**
	 * Create the EC2 images table columns.
	 * @param ec2ImagesTable table for EC2 images
	 * @param ec2TagNameList list of tag names used with EC2
	 * @param ec2ImagesTagNameList list of tag names used with EC2 images
	 * @param tableColMap map for table column names and numbers
	 */
	private void createEc2ImagesTableColumns (
		DataTable ec2ImagesTable,
		List<String> ec2TagNameList,
		List<String> ec2ImagesTagNameList,
		HashMap<String,Integer> tableColMap )
		throws Exception {
		// Column numbers are used later:
  		// - initialize to -1 to indicate the column is not used
   		//tableColMap.put ( "ServiceCol", Integer.valueOf(-1) );
   		tableColMap.put ( "RegionCol", Integer.valueOf(-1) );
   		//tableColMap.put ( "EC2/InstanceIdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2Image/IdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2Image/DescriptionCol", Integer.valueOf(-1) );
   		//tableColMap.put ( "EC2Image/VolumeSizeGBCol", Integer.valueOf(-1) );
   		for ( String tagName : ec2ImagesTagNameList ) {
   			tableColMap.put ( "EC2Image-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}

   		// Check for column existence and create if not found.
   		// General columns.
       	//int serviceCol = servicePropertiesTable.getFieldIndex("ServiceCol", false);
       	//if ( serviceCol < 0 ) {
       	//	serviceCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Service", -1), "");
        // 	tableColMap.put("ServiceCol", Integer.valueOf(serviceCol));
       //	}
       	int regionCol = ec2ImagesTable.getFieldIndex("RegionCol", false);
       	if ( regionCol < 0 ) {
       		regionCol = ec2ImagesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Region", -1), "");
         	tableColMap.put("RegionCol", Integer.valueOf(regionCol));
       	}
       	int snapshotIdCol = ec2ImagesTable.getFieldIndex("EC2Image/IdCol", false);
       	if ( snapshotIdCol < 0 ) {
       		snapshotIdCol = ec2ImagesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2Image/Id", -1), "");
         	tableColMap.put("EC2Image/IdCol", Integer.valueOf(snapshotIdCol));
       	}
       	int snapshotDescriptionCol = ec2ImagesTable.getFieldIndex("EC2Image/DescriptionCol", false);
       	if ( snapshotDescriptionCol < 0 ) {
       		snapshotDescriptionCol = ec2ImagesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2Image/Description", -1), "");
         	tableColMap.put("EC2Image/DescriptionCol", Integer.valueOf(snapshotDescriptionCol));
       	}
       	/*
       	int snapshotVolumeSizeCol = ec2ImagesTable.getFieldIndex("EC2Image/VolumeSizeGBCol", false);
       	if ( snapshotVolumeSizeCol < 0 ) {
       		snapshotDescriptionCol = ec2ImagesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2Image/VolumeSizeGB", -1), "");
         	tableColMap.put("EC2Image/VolumeSizeGBCol", Integer.valueOf(snapshotDescriptionCol));
       	}
       	*/
   		for ( String tagName : ec2ImagesTagNameList ) {
   			int col = ec2ImagesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = ec2ImagesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2Image-Tag/" + tagName, -1), "");
   				tableColMap.put ( "EC2Image-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
	}

	/**
	 * Create the service properties table columns.
	 * @param servicePropertiesTable table for service properties
	 * @param ec2TagNameList list of EC2 tag names to add as columns
	 * @param vpcTagNameList list of VPC tag names to add as columns
	 * @param vpnTagNameList list of VPN tag names to add as columns
	 * @param elasticIpTagNameList list of Elastic IP tag names to add as columns
	 * @param ebsVolumeTagNameList list of EBS volume tag names to add as columns
	 * @param tableColMap map for table column names and numbers
	 */
	private void createEc2PropertiesTableColumns (
		DataTable servicePropertiesTable,
		List<String> ec2TagNameList,
		List<String> vpcTagNameList,
		List<String> vpnTagNameList,
		List<String> elasticIpTagNameList,
		List<String> ebsVolumeTagNameList,
		HashMap<String,Integer> tableColMap )
		throws Exception {
		// Column numbers are used later:
  		// - initialize to -1 to indicate the column is not used
   		tableColMap.put ( "ServiceCol", Integer.valueOf(-1) );
   		tableColMap.put ( "RegionCol", Integer.valueOf(-1) );
   		tableColMap.put ( "PublicIpCol", Integer.valueOf(-1) );
   		tableColMap.put ( "PublicDnsNameCol", Integer.valueOf(-1) );
   		tableColMap.put ( "PrivateIpCol", Integer.valueOf(-1) );
   		tableColMap.put ( "PrivateDnsNameCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2/InstanceIdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2/InstanceTypeCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EC2/InstanceStateCol", Integer.valueOf(-1) );
   		tableColMap.put ( "EBSVolume/IdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "ElasticIpCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPC/IdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPN/ConnectionIdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPN/ConnectionStateCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPN/CustomerGatewayIdCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPN/CustomerGatewayOutsideIpCol", Integer.valueOf(-1) );
   		tableColMap.put ( "VPN/GatewayIdCol", Integer.valueOf(-1) );
   		for ( String tagName : ec2TagNameList ) {
   			tableColMap.put ( "EC2-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}
   		for ( String tagName : vpcTagNameList ) {
   			tableColMap.put ( "VPC-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}
   		for ( String tagName : vpnTagNameList ) {
   			tableColMap.put ( "VPNConnection-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}
   		for ( String tagName : elasticIpTagNameList ) {
   			tableColMap.put ( "ElasticIp-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}
   		for ( String tagName : ebsVolumeTagNameList ) {
   			tableColMap.put ( "EBSVolume-Tag/" + tagName + "Col", Integer.valueOf(-1) );
   		}

   		// Check for column existence and create if not found.
   		// General columns.
       	int serviceCol = servicePropertiesTable.getFieldIndex("ServiceCol", false);
       	if ( serviceCol < 0 ) {
       		serviceCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Service", -1), "");
         	tableColMap.put("ServiceCol", Integer.valueOf(serviceCol));
       	}
       	int regionCol = servicePropertiesTable.getFieldIndex("RegionCol", false);
       	if ( regionCol < 0 ) {
       		regionCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Region", -1), "");
         	tableColMap.put("RegionCol", Integer.valueOf(regionCol));
       	}
       	int publicIpCol = servicePropertiesTable.getFieldIndex("PublicIpCol", false);
       	if ( publicIpCol < 0 ) {
       		publicIpCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "PublicIp", -1), "");
         	tableColMap.put("PublicIpCol", Integer.valueOf(publicIpCol));
       	}
       	int publicDnsNameCol = servicePropertiesTable.getFieldIndex("PublicDnsNameCol", false);
       	if ( publicDnsNameCol < 0 ) {
       		publicDnsNameCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "PublicDnsName", -1), "");
         	tableColMap.put("PublicDnsNameCol", Integer.valueOf(publicDnsNameCol));
       	}
       	int privateIpCol = servicePropertiesTable.getFieldIndex("PrivateIpCol", false);
       	if ( privateIpCol < 0 ) {
       		privateIpCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "PrivateIp", -1), "");
         	tableColMap.put("PrivateIpCol", Integer.valueOf(privateIpCol));
       	}
       	int privateDnsNameCol = servicePropertiesTable.getFieldIndex("PrivateDnsNameCol", false);
       	if ( privateDnsNameCol < 0 ) {
       		privateDnsNameCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "PrivateDnsName", -1), "");
         	tableColMap.put("PrivateDnsNameCol", Integer.valueOf(privateDnsNameCol));
       	}
       	// EC2 columns.
       	int ec2InstanceIdCol = servicePropertiesTable.getFieldIndex("EC2/InstanceIdCol", false);
       	if ( ec2InstanceIdCol < 0 ) {
       		ec2InstanceIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2/InstanceId", -1), "");
         	tableColMap.put("EC2/InstanceIdCol", Integer.valueOf(ec2InstanceIdCol));
       	}
       	int ec2InstanceTypeCol = servicePropertiesTable.getFieldIndex("EC2/InstanceTypeCol", false);
       	if ( ec2InstanceTypeCol < 0 ) {
       		ec2InstanceTypeCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2/InstanceType", -1), "");
         	tableColMap.put("EC2/InstanceTypeCol", Integer.valueOf(ec2InstanceTypeCol));
       	}
       	int ec2InstanceStateCol = servicePropertiesTable.getFieldIndex("EC2/InstanceStateCol", false);
       	if ( ec2InstanceStateCol < 0 ) {
       		ec2InstanceStateCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2/InstanceState", -1), "");
         	tableColMap.put("EC2/InstanceStateCol", Integer.valueOf(ec2InstanceStateCol));
       	}
   		for ( String tagName : ec2TagNameList ) {
   			int col = servicePropertiesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EC2-Tag/" + tagName, -1), "");
   				tableColMap.put ( "EC2-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
   		// EBS volume columns.
       	int ebsVolumeIdCol = servicePropertiesTable.getFieldIndex("EBSVolume/IdCol", false);
       	if ( ebsVolumeIdCol < 0 ) {
       		ebsVolumeIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSVolume/Id", -1), "");
         	tableColMap.put("EBSVolume/IdCol", Integer.valueOf(ebsVolumeIdCol));
       	}
   		for ( String tagName : ebsVolumeTagNameList ) {
   			int col = servicePropertiesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "EBSVolume-Tag/" + tagName, -1), "");
   				tableColMap.put ( "EBSVolume-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
   		// Elastic IP columns.
       	int elasticIpCol = servicePropertiesTable.getFieldIndex("ElasticIp", false);
       	if ( elasticIpCol < 0 ) {
       		elasticIpCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "ElasticIp", -1), "");
         	tableColMap.put("ElasticIpCol", Integer.valueOf(elasticIpCol));
       	}
   		for ( String tagName : elasticIpTagNameList ) {
   			int col = servicePropertiesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "ElasticIp-Tag/" + tagName, -1), "");
   				tableColMap.put ( "ElasticIp-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
   		// VPC columns.
       	int vpcIdCol = servicePropertiesTable.getFieldIndex("VPC/IdCol", false);
       	if ( vpcIdCol < 0 ) {
       		vpcIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPC/Id", -1), "");
         	tableColMap.put("VPC/IdCol", Integer.valueOf(vpcIdCol));
       	}
   		for ( String tagName : vpcTagNameList ) {
   			int col = servicePropertiesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPC-Tag/" + tagName, -1), "");
   				tableColMap.put ( "VPC-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
   		// VPN columns.
       	int vpnConnectionIdCol = servicePropertiesTable.getFieldIndex("VPN/ConnectionIdCol", false);
       	if ( vpnConnectionIdCol < 0 ) {
       		vpnConnectionIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPN/ConnectionId", -1), "");
         	tableColMap.put("VPN/ConnectionIdCol", Integer.valueOf(vpnConnectionIdCol));
       	}
       	int vpnConnectionStateCol = servicePropertiesTable.getFieldIndex("VPN/ConnectionStateCol", false);
       	if ( vpnConnectionStateCol < 0 ) {
       		vpnConnectionStateCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPN/ConnectionState", -1), "");
         	tableColMap.put("VPN/ConnectionStateCol", Integer.valueOf(vpnConnectionStateCol));
       	}
       	int vpnCustomerGatewayIdCol = servicePropertiesTable.getFieldIndex("VPN/CustomerGatewayIdCol", false);
       	if ( vpnCustomerGatewayIdCol < 0 ) {
       		vpnCustomerGatewayIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPN/CustomerGatewayId", -1), "");
         	tableColMap.put("VPN/CustomerGatewayIdCol", Integer.valueOf(vpnCustomerGatewayIdCol));
       	}
       	int vpnCustomerGatewayOutsideIpCol = servicePropertiesTable.getFieldIndex("VPN/CustomerGatewayOutsideIpCol", false);
       	if ( vpnCustomerGatewayOutsideIpCol < 0 ) {
       		vpnCustomerGatewayOutsideIpCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPN/CustomerGatewayOutsideIp", -1), "");
         	tableColMap.put("VPN/CustomerGatewayOutsideIpCol", Integer.valueOf(vpnCustomerGatewayOutsideIpCol));
       	}
       	int vpnGatewayIdCol = servicePropertiesTable.getFieldIndex("VPN/GatewayIdCol", false);
       	if ( vpnGatewayIdCol < 0 ) {
       		vpnGatewayIdCol = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPN/GatewayId", -1), "");
         	tableColMap.put("VPN/GatewayIdCol", Integer.valueOf(vpnGatewayIdCol));
       	}
   		for ( String tagName : vpnTagNameList ) {
   			int col = servicePropertiesTable.getFieldIndex(tagName, false);
   			if ( col < 0 ) {
   				col = servicePropertiesTable.addField(new TableField(TableField.DATA_TYPE_STRING, "VPNConnection-Tag/" + tagName, -1), "");
   				tableColMap.put ( "VPNConnection-Tag/" + tagName + "Col", Integer.valueOf(col) );
   			}
   		}
	}

	/**
	 * Create the grouped table.
	 * @param processor the time series processor
	 * @param doGroupedTable whether a grouped table for output is created
	 * @param groupedTableID the table ID for the grouped output table
	 * @param groupTableID the table identifier for the grouped data table
	 * @param doGroupedFile whether a grouped table for output file is created (needed to indicate if a temporary table is needed)
	 * @param appendOutput whether the output is appended to the table
	 * @param groupedTableColMap map of the table columns to integers (so don't have to pass many parameters)
	 * @param groupBy1 the AWS enumeration value for the first "group by"
	 * @param groupBy2 the AWS enumeration value for the second "group by"
	 * @param problems the list of problem messages (will be logged in the calling code)
	 * @return the grouped table
	 */
	private DataTable createGroupedTable (
    	CommandProcessor processor,
		boolean doGroupedTable, String groupedTableID, DataTable groupedTable,
		boolean doGroupedFile,
		boolean appendOutput,
   		HashMap<String,Integer> groupedTableColMap,
   		AwsBillingDimensionType groupBy1,
   		AwsBillingDimensionType groupBy2,
   		List<String> problems
		) throws Exception {
		String routine = getClass().getSimpleName() + ".createGroupedTable";

		// Column numbers are used later:
  		// - initialize to -1 to indicate the column is not used
   		groupedTableColMap.put ( "RecordTypeCol", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "StartDateCol", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "EndDateCol", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GranularityCol", Integer.valueOf (-1) );
   		groupedTableColMap.put ( "GroupBy1Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GroupByTag1Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GroupItem1Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GroupBy2Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GroupByTag2Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "GroupItem2Col", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "MetricCol", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "AmountCol", Integer.valueOf(-1) );
   		groupedTableColMap.put ( "UnitsCol", Integer.valueOf(-1) );

  		if ( doGroupedTable || doGroupedFile ) {
   			// Requested a table and/or file:
   			// - if only file is request, create a temporary table that is then written to output
    		if ( (groupedTable == null) || !appendOutput ) {
        		// The table needs to be created because it does not exist or NOT appending (so need new table):
    			// - the table columns depend on the S3 command being executed
    			// 1. Define the column names based on S3 commands.
        		List<TableField> columnList = new ArrayList<>();
       			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "StartDate", -1) );
      			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "EndDate", -1) );
       			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "RecordType", -1) );
      			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1) );
    			if ( groupBy1 != null ) {
       				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupBy1", -1) );
       				if ( groupBy1 == AwsBillingDimensionType.TAG ) {
       					columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupByTag1", -1) );
       				}
       				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupItem1", -1) );
       			}
       			if ( groupBy2 != null ) {
       				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupBy2", -1) );
      				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
       					columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupByTag2", -1) );
       				}
       				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupItem2", -1) );
       			}
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Metric", -1) );
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2) );
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Units", -1) );
    	    	// 2. Create the table if not found from the processor above.
   	        	// Create the table.
   	        	groupedTable = new DataTable( columnList );
            	// 3. Get the column numbers from the names for later use.
   	        	groupedTableColMap.put("RecordTypeCol", Integer.valueOf(groupedTable.getFieldIndex("RecordType", false)));
   	        	groupedTableColMap.put("StartDateCol" , Integer.valueOf(groupedTable.getFieldIndex("StartDate", false)));
   	        	groupedTableColMap.put("EndDateCol" , Integer.valueOf(groupedTable.getFieldIndex("EndDate", false)));
   	        	groupedTableColMap.put("GranularityCol" , Integer.valueOf(groupedTable.getFieldIndex("Granularity", false)));
   	        	if ( groupBy1 != null ) {
   	        		groupedTableColMap.put("GroupBy1Col", Integer.valueOf(groupedTable.getFieldIndex("GroupBy1", false)));
   	        		if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        			groupedTableColMap.put("GroupByTag1Col", Integer.valueOf(groupedTable.getFieldIndex("GroupByTag1", false)));
   	        		}
   	        		groupedTableColMap.put("GroupItem1Col", Integer.valueOf(groupedTable.getFieldIndex("GroupItem1", false)));
   	        	}
   	        	if ( groupBy2 != null ) {
   	        		groupedTableColMap.put("GroupBy2Col", Integer.valueOf(groupedTable.getFieldIndex("GroupBy2", false)));
   	        		if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        			groupedTableColMap.put("GroupByTag2Col", Integer.valueOf(groupedTable.getFieldIndex("GroupByTag2", false)));
   	        		}
   	        		groupedTableColMap.put("GroupItem2Col", Integer.valueOf(groupedTable.getFieldIndex("GroupItem2", false)));
   	        	}
   	        	if ( (groupBy1 == null) && (groupBy2 == null) ) {
   	        		groupedTableColMap.put("ServiceCol", Integer.valueOf(groupedTable.getFieldIndex("Service", false)));
   	        		groupedTableColMap.put("RegionCol", Integer.valueOf(groupedTable.getFieldIndex("Region", false)));
   	        	}
   	        	groupedTableColMap.put("MetricCol", Integer.valueOf(groupedTable.getFieldIndex("Metric", false)));
   	        	groupedTableColMap.put("AmountCol", Integer.valueOf(groupedTable.getFieldIndex("Amount", false)));
   	        	groupedTableColMap.put("UnitsCol", Integer.valueOf(groupedTable.getFieldIndex("Units", false)));
    	    	// 4. Set the table in the processor:
    	    	//    - if new will add
    	    	//    - if append will overwrite by replacing the matching table ID
   	        	if ( (groupedTableID != null) && !groupedTableID.isEmpty() ) {
   	        		groupedTable.setTableID ( groupedTableID );
            		Message.printStatus(2, routine, "Created new grouped table \"" + groupedTableID + "\" for output.");
            		// Set the table in the processor:
            		// - do not set if a temporary table is being used for the output file
            		PropList requestParams = new PropList ( "" );
            		requestParams.setUsingObject ( "Table", groupedTable );
            		try {
            			processor.processRequest( "SetTable", requestParams);
            		}
            		catch ( Exception e ) {
            			problems.add ( "Error requesting SetTable(Table=...) from processor." );
            		}
   	        	}
   	        	else {
   	        		// Temporary table used for file only:
   	        		// - do not set in the processor
   	        		groupedTable.setTableID ( "AwsGroupedBilling" );
   	        	}
    	    	// 5. The table contents will be filled in when the doCostExplorer method is called.
    	    }
    	    else {
    	    	// Table exists:
    	    	// - make sure that the needed columns exist and otherwise add them
   	        	int startDateCol = groupedTable.getFieldIndex("StartDate", false);
   	        	if ( startDateCol < 0 ) {
   	        		startDateCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "StartDate", -1), "");
   	        	}
   	        	groupedTableColMap.put("StartDateCol", Integer.valueOf(startDateCol));
   	        	int endDateCol = groupedTable.getFieldIndex("EndDate", false);
   	        	if ( endDateCol < 0 ) {
   	        		endDateCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "EndDate", -1), "");
   	        	}
   	        	groupedTableColMap.put("EndDateCol", Integer.valueOf(endDateCol));
   	        	int recordTypeCol = groupedTable.getFieldIndex("RecordType", false);
   	        	if ( recordTypeCol < 0 ) {
   	        		recordTypeCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "RecordType", -1), "");
   	        	}
   	        	groupedTableColMap.put("RecordTypeCol", Integer.valueOf(recordTypeCol));
   	        	int granularityCol = groupedTable.getFieldIndex("Granularity", false);
   	        	if ( granularityCol < 0 ) {
   	        		granularityCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1), "");
   	        	}
   	        	groupedTableColMap.put("GranularityCol", Integer.valueOf(granularityCol));
   	        	if ( groupBy1 != null ) {
   	        		int groupBy1Col = groupedTable.getFieldIndex("GroupBy1", false);
   	        		if ( groupBy1Col < 0 ) {
   	        			groupBy1Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupBy1", -1), "");
   	        		}
   	        		groupedTableColMap.put("GroupBy1Col", Integer.valueOf(groupBy1Col));
   	        		if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        			int groupByTag1Col = groupedTable.getFieldIndex("GroupBy1Tag", false);
   	        			if ( groupByTag1Col < 0 ) {
   	        				groupByTag1Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupByTag1", -1), "");
   	        			}
   	        			groupedTableColMap.put("GroupByTag1Col", Integer.valueOf(groupByTag1Col));
   	        		}
   	        		int groupItem1Col = groupedTable.getFieldIndex("GroupItem1", false);
   	        		if ( groupItem1Col < 0 ) {
   	        			groupItem1Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupItem1", -1), "");
   	        		}
          			groupedTableColMap.put("GroupItem1Col", Integer.valueOf(groupItem1Col));
   	        	}
   	        	if ( groupBy2 != null ) {
   	        		int groupBy2Col = groupedTable.getFieldIndex("GroupBy2", false);
   	        		if ( groupBy2Col < 0 ) {
   	        			groupBy2Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupBy2", -1), "");
   	        		}
   	        		groupedTableColMap.put("GroupBy2Col", Integer.valueOf(groupBy2Col));
   	        		if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        			int groupByTag2Col = groupedTable.getFieldIndex("GroupByTag2", false);
   	        			if ( groupByTag2Col < 0 ) {
   	        				groupByTag2Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupByTag2", -1), "");
   	        			}
   	        			groupedTableColMap.put("GroupByTag2Col", Integer.valueOf(groupBy2Col));
   	        		}
   	        		int groupItem2Col = groupedTable.getFieldIndex("GroupItem2", false);
   	        		if ( groupItem2Col < 0 ) {
   	        			groupItem2Col = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupItem2", -1), "");
   	        		}
          			groupedTableColMap.put("GroupItem2Col", Integer.valueOf(groupItem2Col));
   	        	}
   	        	// If GroupBy1 and GroupBy2 are not used, add columns for raw data.
   	        	if ( (groupBy1 == null) && (groupBy2 == null) ) {
   	        		int serviceCol = groupedTable.getFieldIndex("Service", false);
   	        		if ( serviceCol < 0 ) {
   	        			serviceCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Service", -1), "");
   	        		}
          			groupedTableColMap.put("ServiceCol", Integer.valueOf(serviceCol));
   	        		int regionCol = groupedTable.getFieldIndex("Region", false);
   	        		if ( regionCol < 0 ) {
   	        			regionCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Region", -1), "");
   	        		}
          			groupedTableColMap.put("RegionCol", Integer.valueOf(regionCol));
   	        	}
   	        	int metricCol = groupedTable.getFieldIndex("Metric", false);
   	        	if ( metricCol < 0 ) {
   	        		metricCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Metric", -1), "");
   	        	}
       			groupedTableColMap.put("MetricCol", Integer.valueOf(metricCol));
   	        	int amountCol = groupedTable.getFieldIndex("Amount", false);
   	        	if ( amountCol < 0 ) {
   	        		amountCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2), "");
   	        	}
       			groupedTableColMap.put("AmountCol", Integer.valueOf(amountCol));
   	        	int unitsCol = groupedTable.getFieldIndex("Units", false);
   	        	if ( unitsCol < 0 ) {
   	        		unitsCol = groupedTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Units", -1), "");
   	        	}
       			groupedTableColMap.put("UnitsCol", Integer.valueOf(unitsCol));
    	   	}
		}

  		// Return the table (either the original or new temporary table).
   		return groupedTable;
	}

	/**
	 * Create the total table.
	 * @param processor the time series processor
	 * @param doTotalTable whether a total table for output is created
	 * @param totalTableID the table ID for the total output table
	 * @param groupTableID the table identifier for the grouped data table
	 * @param doTotalFile whether a total table for output file is created (needed to indicate if a temporary table is needed)
	 * @param appendOutput whether the output is appended to the table
	 * @param totalTableColMap map of the table columns to integers (so don't have to pass many parameters)
	 * @param problems the list of problem messages (will be logged in the calling code)
	 * @return the grouped table
	 */
	private DataTable createTotalTable (
    	CommandProcessor processor,
		boolean doTotalTable, String totalTableID, DataTable totalTable,
		boolean doTotalFile,
		boolean appendOutput,
   		HashMap<String,Integer> totalTableColMap,
   		List<String> problems
		) throws Exception {
		String routine = getClass().getSimpleName() + ".createTotalTable";

		// Column numbers are used later:
  		// - initialize to -1 to indicate the column is not used
   		totalTableColMap.put ( "RecordTypeCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "StartDateCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "EndDateCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "GranularityCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "MetricCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "AmountCol", Integer.valueOf(-1) );
   		totalTableColMap.put ( "UnitsCol", Integer.valueOf(-1) );

  		if ( doTotalTable || doTotalFile ) {
   			// Requested a table and/or file:
   			// - if only file is request, create a temporary table that is then written to output
    		if ( (totalTable == null) || !appendOutput ) {
        		// The table needs to be created because it does not exist or NOT appending (so need new table):
    			// - the table columns depend on the S3 command being executed
    			// 1. Define the column names based on S3 commands.
        		List<TableField> columnList = new ArrayList<>();
       			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "StartDate", -1) );
      			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "EndDate", -1) );
       			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "RecordType", -1) );
      			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1) );
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Metric", -1) );
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2) );
   	        	columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Units", -1) );
    	    	// 2. Create the table if not found from the processor above.
   	        	// Create the table.
   	        	totalTable = new DataTable( columnList );
            	// 3. Get the column numbers from the names for later use.
   	        	totalTableColMap.put("RecordTypeCol", Integer.valueOf(totalTable.getFieldIndex("RecordType", false)));
   	        	totalTableColMap.put("StartDateCol" , Integer.valueOf(totalTable.getFieldIndex("StartDate", false)));
   	        	totalTableColMap.put("EndDateCol" , Integer.valueOf(totalTable.getFieldIndex("EndDate", false)));
   	        	totalTableColMap.put("GranularityCol" , Integer.valueOf(totalTable.getFieldIndex("Granularity", false)));
   	        	totalTableColMap.put("MetricCol", Integer.valueOf(totalTable.getFieldIndex("Metric", false)));
   	        	totalTableColMap.put("AmountCol", Integer.valueOf(totalTable.getFieldIndex("Amount", false)));
   	        	totalTableColMap.put("UnitsCol", Integer.valueOf(totalTable.getFieldIndex("Units", false)));
    	    	// 4. Set the table in the processor:
    	    	//    - if new will add
    	    	//    - if append will overwrite by replacing the matching table ID
   	        	if ( (totalTableID != null) && !totalTableID.isEmpty() ) {
   	        		totalTable.setTableID ( totalTableID );
            		Message.printStatus(2, routine, "Created new total table \"" + totalTableID + "\" for output.");
            		// Set the table in the processor:
            		// - do not set if a temporary table is being used for the output file
            		PropList requestParams = new PropList ( "" );
            		requestParams.setUsingObject ( "Table", totalTable );
            		try {
            			processor.processRequest( "SetTable", requestParams);
            		}
            		catch ( Exception e ) {
            			problems.add ( "Error requesting SetTable(Table=...) from processor." );
            		}
   	        	}
   	        	else {
   	        		// Temporary table used for file only:
   	        		// - do not set in the processor
   	        		totalTable.setTableID ( "AwsTotalBilling" );
   	        	}
    	    	// 5. The table contents will be filled in when the doCostExplorer method is called.
    	    }
    	    else {
    	    	// Table exists:
    	    	// - make sure that the needed columns exist and otherwise add them
   	        	int startDateCol = totalTable.getFieldIndex("StartDate", false);
   	        	if ( startDateCol < 0 ) {
   	        		startDateCol = totalTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "StartDate", -1), "");
   	        	}
   	        	totalTableColMap.put("StartDateCol", Integer.valueOf(startDateCol));
   	        	int endDateCol = totalTable.getFieldIndex("EndDate", false);
   	        	if ( endDateCol < 0 ) {
   	        		endDateCol = totalTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "EndDate", -1), "");
   	        	}
   	        	totalTableColMap.put("EndDateCol", Integer.valueOf(endDateCol));
   	        	int recordTypeCol = totalTable.getFieldIndex("RecordType", false);
   	        	if ( recordTypeCol < 0 ) {
   	        		recordTypeCol = totalTable.addField(new TableField(TableField.DATA_TYPE_STRING, "RecordType", -1), "");
   	        	}
   	        	totalTableColMap.put("RecordTypeCol", Integer.valueOf(recordTypeCol));
   	        	int granularityCol = totalTable.getFieldIndex("Granularity", false);
   	        	if ( granularityCol < 0 ) {
   	        		granularityCol = totalTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1), "");
   	        	}
   	        	totalTableColMap.put("GranularityCol", Integer.valueOf(granularityCol));
   	        	int metricCol = totalTable.getFieldIndex("Metric", false);
   	        	if ( metricCol < 0 ) {
   	        		metricCol = totalTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Metric", -1), "");
   	        	}
       			totalTableColMap.put("MetricCol", Integer.valueOf(metricCol));
   	        	int amountCol = totalTable.getFieldIndex("Amount", false);
   	        	if ( amountCol < 0 ) {
   	        		amountCol = totalTable.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2), "");
   	        	}
       			totalTableColMap.put("AmountCol", Integer.valueOf(amountCol));
   	        	int unitsCol = totalTable.getFieldIndex("Units", false);
   	        	if ( unitsCol < 0 ) {
   	        		unitsCol = totalTable.addField(new TableField(TableField.DATA_TYPE_STRING, "Units", -1), "");
   	        	}
       			totalTableColMap.put("UnitsCol", Integer.valueOf(unitsCol));
    	   	}
		}

  		// Return the table (either the original or new temporary table).
   		return totalTable;
	}

	/**
	 * Get data from the cost explorer.
	 * @param processor the processor for commands
	 * @param groupByTable the table for grouped output, or null if no output
	 * @param totalTable the table for total output, or null if no output
	 * @param groupedTableRowCountProperty the processor property to set with the groupByTable row count
	 * @param totalTableRowCountProperty the processor property to set with the totalTable row count
	 * @param groupedTableColMap map for grouped table columns
	 * @param totalTableColMap map for total table columns
	 * @param inputStart the input start for the query (must not be null)
	 * @param inputEnd the input end for the query (must not be null)
	 * @param timeChunk the time chunk interval for queries, used when the period is long enough tht no chunk would return partial data
	 * @param granularity the AWS cost granularity for output, must be non-null
	 * @param groupBy1 the AWS "group by 1" dimension to match, or null to not use
	 * @param groupByTag1 the AWS "group by tag 1" String to match, or null to not use
	 * @param groupBy2 the AWS "group by 2" dimension to match, or null to not use
	 * @param groupByTag2 the AWS "group by tag 2" String to match, or null to not use
	 * @param metric the AWS metric to match, or null to not use
	 * @param filterAvailabilityZones the AWS availability zones to match, or null to not use
	 * @param filterInstanceTypes the AWS EC2 instance types to match, or null to not use
	 * @param filterRegions the AWS regions to match, or null to not use
	 * @param filterServices the AWS services to match, or null to not use
	 * @param filterTags the AWS tags to match, or null to not use
	 * @param createGroupedTimeSeries true to create grouped time series, false if not
	 * @param groupedTimeSeriesLocationId for grouped data, "GroupBy1", "GroupBy2", or "Auto", indicating what to use for the locationId
	 * (if "Auto" use the GroupByTag2:TagValue or GroupByTag2:TagValue)
	 * @param groupedTimeSeriesDataType for grouped data, data type format as "Auto" (default) or a combination of characters and
	 * TAG1, TAG1VALUE, TAG2, TAG2VALUE, GROUPBY1, GROUPBY1VALUE, GROUPBY2, GROUPBY2VALUE.
	 * @param groupedTslist for grouped data, the list of grouped data time series to add to if reading time series (must be non-null if reading time series)
	 * @param createTotalTimeSeries true to create total time series, false if not
	 * @param totalTimeSeriesLocationId for total data, indicate what to use for the locationId
	 * (if "Auto" use "Total")
	 * @param totalTimeSeriesDataType for total data, data type format as "Auto" (default) or a combination of characters and
	 * METRIC.
	 * @param totalTslist for total data, the list of total time series to add to if reading time series (must be non-null if reading time series)
	 * @param timeSeriesMissingGroupBy value to use for missing GroupBy values (e.g., "Unknown")
	 * @param status the command status for logging
	 * @param logLevel the logging level for messages
	 * @param warningCount the number of warnings before this function, will be added to and returned
	 * @param commandTag the command tag for identifying log messages
	 * @return the updated warning count
	 */
	private int doCostExplorer (
		CommandProcessor processor,
		CostExplorerClient costExplorer,
		DataTable groupByTable, DataTable totalTable,
		String groupedTableRowCountProperty, String totalTableRowCountProperty,
   		HashMap<String,Integer> groupedTableColMap,
   		HashMap<String,Integer> totalTableColMap,
		DateTime inputStart, DateTime inputEnd, TimeInterval timeChunk,
		AwsBillingGranularityType granularity,
		AwsBillingDimensionType groupBy1, String groupByTag1,
		AwsBillingDimensionType groupBy2, String groupByTag2,
		// Only 2 group by can be used with the API.
		AwsBillingMetricType metric,
		List<String> filterAvailabilityZones,
		List<String> filterInstanceTypes,
		List<String> filterRegions,
		List<String> filterServices,
		List<String> filterTags,
		boolean createGroupedTimeSeries, String groupedTimeSeriesLocationId, String groupedTimeSeriesDataType, List<TS> groupedTsList,
		boolean createTotalTimeSeries, String totalTimeSeriesLocationId, String totalTimeSeriesDataType, List<TS> totalTsList,
		String timeSeriesMissingGroupBy,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doCostExplorer";
		String message;

		// Get column numbers out of the map.
    	int groupedTable_recordTypeCol = (Integer)groupedTableColMap.get("RecordTypeCol");
    	int groupedTable_startDateCol = (Integer)groupedTableColMap.get("StartDateCol");
    	int groupedTable_endDateCol = (Integer)groupedTableColMap.get("EndDateCol");
    	int groupedTable_granularityCol = (Integer)groupedTableColMap.get("GranularityCol");
    	int groupedTable_groupBy1Col = (Integer)groupedTableColMap.get("GroupBy1Col");
    	int groupedTable_groupByTag1Col = (Integer)groupedTableColMap.get("GroupByTag1Col");
    	int groupedTable_groupItem1Col = (Integer)groupedTableColMap.get("GroupItem1Col");
    	int groupedTable_groupBy2Col = (Integer)groupedTableColMap.get("GroupBy2Col");
    	int groupedTable_groupByTag2Col = (Integer)groupedTableColMap.get("GroupByTag2Col");
    	int groupedTable_groupItem2Col = (Integer)groupedTableColMap.get("GroupItem2Col");
    	int groupedTable_metricCol = (Integer)groupedTableColMap.get("MetricCol");
    	int groupedTable_amountCol = (Integer)groupedTableColMap.get("AmountCol");
    	int groupedTable_unitsCol = (Integer)groupedTableColMap.get("UnitsCol");
    	// Raw data columns used when not grouping.
    	//int groupedTable_regionCol,
    	//int groupedTable_serviceCol,

		// Get column numbers out of the map.
    	int totalTable_recordTypeCol = (Integer)totalTableColMap.get("RecordTypeCol");
    	int totalTable_startDateCol = (Integer)totalTableColMap.get("StartDateCol");
    	int totalTable_endDateCol = (Integer)totalTableColMap.get("EndDateCol");
    	int totalTable_granularityCol = (Integer)totalTableColMap.get("GranularityCol");
    	//int totalTable_groupBy1Col = (Integer)totalTableColMap.get("GroupBy1Col");
    	//int totalTable_groupByTag1Col = (Integer)totalTableColMap.get("GroupByTag1Col");
    	//int totalTable_groupItem1Col = (Integer)totalTableColMap.get("GroupItem1");
    	//int totalTable_groupBy2Col = (Integer)totalTableColMap.get("GroupBy2Col");
    	//int totalTable_groupByTag2Col = (Integer)totalTableColMap.get("GroupByTag2Col");
    	//int totalTable_groupItem2Col = (Integer)totalTableColMap.get("GroupItem2");
    	int totalTable_metricCol = (Integer)totalTableColMap.get("MetricCol");
    	int totalTable_amountCol = (Integer)totalTableColMap.get("AmountCol");
    	int totalTable_unitsCol = (Integer)totalTableColMap.get("UnitsCol");

		// Shared data used when processing time series:

		// Process the format string:
		// - list the most specific values first, then the substring, to avoid replacing the substring by accident
		String [] metadataNames = {
			"GROUPBY1VALUE", // [0] The data value for a single record (set below).
			"GROUPBY1",      // [1] For example "Service", "Region", "Tag"
			"GROUPBYTAG1",   // [2] index 2: For example the tag name.
			"GROUPBY2VALUE", // [3] The data value for a single record (set below).
			"GROUPBY2",      // [4] For example "Service", "Region", "Tag"
			"GROUPBYTAG2",   // [5] For example the tag name.
			"METRIC"         // [6] For example "UnblendedCost"
		};
		// The values corresponding to the above:
		// - can set all but GROUPVALUE1 and GROUPVALUE2 here, otherwise set below for each record processed
		String [] metadataValues = new String[7];
		// metadataValues[metadataValue1Index] will be set below when processing data records
		if ( groupBy1 == null ) {
			metadataValues[1] = null;
		}
		else {
			metadataValues[1] = groupBy1.toString();
		}
		metadataValues[2] = groupByTag1;
		// metadataValues[metadataValue2Index] // Will be set below when processing data records
		if ( groupBy2 == null ) {
			metadataValues[4] = null;
		}
		else {
			metadataValues[4] = groupBy2.toString();
		}
		metadataValues[5] = groupByTag2;
		// metadataValues[metadataMetricIndex] will be set below when processing data records

		// Dates for the request, which may be less than the full input period if chunking time.
		DateTime requestStart = null;
		DateTime requestEnd = null;
		// Count the number of chunks processed.
		int timeChunkCount = 0;
		while ( true ) {
			// The count of how many chunks are being processed:
			// - will be 1 at the start of the first time
			++timeChunkCount;
			// Put all the query period checks up front in case exceptions later interrupt the logic
			// and make it hard to check time at the end.
			if ( timeChunk == null ) {
				// Time chunks are not being used so query the full period.
				if ( timeChunkCount == 1 ) {
					// First iteration so set the time to the full period.
					requestStart = new DateTime(inputStart);
					requestEnd = new DateTime(inputEnd);
					// The API uses a request end that is exclusive of the date to only contain complete months and days.
					Message.printStatus(2,routine,"Requesting data for full period " + requestStart + " (inclusive) to " + requestEnd + " (exclusive).");
				}
				else {
					// Done processing.
					break;
				}
			}
			else {
				// Time chunks are being processed so set the request period based on the timeChunkCount.
				if ( timeChunkCount == 1 ) {
					// First time:
					// - initialize to the first chunk
					requestStart = new DateTime(inputStart);
					requestEnd = new DateTime(inputStart);
					requestEnd.addInterval ( timeChunk.getBase(), timeChunk.getMultiplier() );
				}
				else {
					// Second and later iteration:
					// - advance the request period by the time chunk
					// - make sure that the start of the chunk is one 'granularity' past the previous 'requestEnd'
					if ( requestEnd.greaterThanOrEqualTo(inputEnd) ) {
						// Last iteration ended at the InputEnd so done.
						break;
					}
					else {
						// Need to process more data:
						// - first copy the previous 'requestEnd' to the new 'requestStart'
						// - this is OK because 'requestEnd' is exclusive and will not have been counted in the previous request
						requestStart = new DateTime(requestEnd);
						// Copy the start and to the end and increment by the 'timeChunk'.
						requestEnd = new DateTime(requestStart);
						requestEnd.addInterval ( timeChunk.getBase(), timeChunk.getMultiplier() );
						if ( requestEnd.greaterThan(inputEnd) ) {
							// Set the request to the input end.
							requestEnd = new DateTime(inputEnd);
						}
					}
				}
				Message.printStatus(2,routine,"Requesting data for time chunk " + timeChunkCount + " period " + requestStart + " (inclusive) to "
					+ requestEnd + " (exclusive).");
			}

			// Create a request builder that handles the input parameters.
			GetCostAndUsageRequest.Builder costAndUsageRequestBuilder = GetCostAndUsageRequest
				.builder()
				// Add filters:
				// - see:  https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/model/GetCostAndUsageRequest.Builder.html
				// The dimension for output:
				// - some service output may only be by month and show up on a certain day
				// - each ResultByTime instance below will correspond to the time block
				.granularity(granularity.getGranularity())
				// The metric is the "Aggregate costs by" in the Cost Explorer web site:
				// - pass the string value from the SDK metric
				.metrics(metric.getMetric().toString())
				// Period for the analysis (typically want to be full months or years for invoicing).
				.timePeriod(DateInterval.builder()
					// Start date is inclusive, up and to the current date.
					.start(requestStart.toString())
					// End date is exclusive, can be up to and including the current date.
					.end(requestEnd.toString())
					.build());

			// Add additional query parameters.
			List<GroupDefinition> groupDefinitions = new ArrayList<>();
			if ( groupBy1 != null ) {
				if ( groupBy1 == AwsBillingDimensionType.TAG ) {
					// The group definition is defined by a tag.
					groupDefinitions.add (
						GroupDefinition.builder()
   							.key(groupByTag1)
   							.type(GroupDefinitionType.TAG)
   							.build()
					);
				}
				else {
					// The group definition is defined by the dimension.
					groupDefinitions.add (
						GroupDefinition.builder()
   							.key("" + groupBy1.getDimension())
   							.type(GroupDefinitionType.DIMENSION)
   							.build()
					);
				}
			}
			if ( groupBy2 != null ) {
				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
					// The group definition is defined by a tag.
					groupDefinitions.add (
						GroupDefinition.builder()
   							.key(groupByTag2)
   							.type(GroupDefinitionType.TAG)
   							.build()
					);
				}
				else {
					// The group definition is defined by the dimension.
					groupDefinitions.add (
						GroupDefinition.builder()
						.key("" + groupBy2.getDimension())
						.type(GroupDefinitionType.DIMENSION)
   						.build()
					);
				}
			}
			if ( !groupDefinitions.isEmpty() ) {
				costAndUsageRequestBuilder = costAndUsageRequestBuilder.groupBy(groupDefinitions);
			}

			// Add filters using an Expression:
			// - use OR within a list of filters
			// - use AND for the groups
			//List<Filter> filters = new ArrayList<>();
			//int filterGroupCount = 0;
			Expression.Builder expressionBuilder = null;
			if ( filterAvailabilityZones.size() > 0 ) {
				// Create dimension values instance for availability zone.
				DimensionValues dimensionValues = DimensionValues.builder()
					.key(Dimension.AZ)
					.values(filterAvailabilityZones)
					.build();
				// Add the dimensions to the expression.
				if ( expressionBuilder == null ) {
					// First filter group so can just add the dimensions.
					expressionBuilder = Expression.builder();
					expressionBuilder.dimensions(dimensionValues);
				}
			}
			if ( filterRegions.size() > 0 ) {
				// Create dimension values instance for region.
				DimensionValues dimensionValues = DimensionValues.builder()
					.key(Dimension.REGION)
//					.values(filterRegions)
					.build();
				// Add the dimensions to the expression.
				if ( expressionBuilder == null ) {
					// First expression so can just add the dimensions.
					expressionBuilder = Expression.builder();
					expressionBuilder.dimensions(dimensionValues);
				}
				else {
					// Expression builder was previously created so just add more values.
					expressionBuilder.dimensions(dimensionValues);
				}
			}
			if ( filterServices.size() > 0 ) {
				// Create dimension values instance for service.
				DimensionValues dimensionValues = DimensionValues.builder()
					// The service can be specified two ways:
					// - SERVICE is the longer name such as "Amazon Simple Storage Solution", as shown in the table output
					// - SERVICE_CODE is the shorter name such as "AmazonS3"
					.key(Dimension.SERVICE)
					.values(filterServices)
					.build();
				// Add the dimensions to the expression.
				if ( expressionBuilder == null ) {
					// First expression so can just add the dimensions.
					expressionBuilder = Expression.builder();
					expressionBuilder.dimensions(dimensionValues);
				}
				else {
					// Expression builder was previously created so just add more values.
					expressionBuilder.dimensions(dimensionValues);
				}
			}
			if ( filterInstanceTypes.size() > 0 ) {
				// Create dimension values instance for region.
				DimensionValues dimensionValues = DimensionValues.builder()
					.key(Dimension.INSTANCE_TYPE)
					.values(filterInstanceTypes)
					.build();
				// Add the dimensions to the expression.
				if ( expressionBuilder == null ) {
					// First expression so can just add the dimensions.
					expressionBuilder = Expression.builder();
					expressionBuilder.dimensions(dimensionValues);
				}
				else {
					// Expression builder was previously created so just add more values.
					expressionBuilder.dimensions(dimensionValues);
				}
			}
			if ( expressionBuilder != null ) {
				// Have an expression to add to the query.
				Expression expression = expressionBuilder.build();
				costAndUsageRequestBuilder = costAndUsageRequestBuilder
					.filter(expression);
			}

			// The API has a maximum number of records (1000? 5000?) per response:
			// - need to page through the results
			String nextPageToken = null;
			// Number of individual records processed for time chunk, needed to check for a query limit.
			int chunkRecordCount = 0;

			do {
				// Build the request.
				if ( nextPageToken == null ) {
					Message.printStatus(2, routine, "Querying cost and usage data for the first page of results." );
				}
				else {
					// Set the page request.
					costAndUsageRequestBuilder.nextPageToken(nextPageToken);
					Message.printStatus(2, routine, "Querying cost and usage data using nextPageToken=" + nextPageToken );
				}
				GetCostAndUsageRequest request = costAndUsageRequestBuilder.build();

				// Get the response:
				// - set 'haveError' if there is an error
				boolean haveError = false;
				GetCostAndUsageResponse response = null;
				try {
					response = costExplorer.getCostAndUsage(request);
					Message.printStatus(2, routine, "Cost and usage results 'resultsByTime' size = " + response.resultsByTime().size());
				}
				catch ( Exception e ) {
					message = "Error getting cost and usage (" + e + ")";
					Message.printWarning(3,routine,message);
					Message.printWarning(3,routine,e);
					status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Check the command parameters." ) );
					haveError = true;
				}

				List<ResultByTime> resultsByTimeList = null;
				if ( haveError ) {
					// Had an error getting the data:
					// - create an empty list so that the logic below will be skipped
					resultsByTimeList = new ArrayList<>();
				}
				else {
					// Can get the results from the response.
					resultsByTimeList = response.resultsByTime();
				}

				// Output to the log file:
				// - convert to debug later if it is too much
				for ( ResultByTime resultByTime : resultsByTimeList ) {
					Message.printStatus(2, routine, "Time period: " + resultByTime.timePeriod());
					Message.printStatus(2, routine, "Estimated?: " + resultByTime.estimated());
					Message.printStatus(2, routine, "Has total metrics?: " + resultByTime.hasTotal());
					Message.printStatus(2, routine, "Has group metrics?: " + resultByTime.hasGroups());
					if ( resultByTime.hasTotal() ) {
						Map<String, MetricValue> totalMetricsMap = resultByTime.total();
						for ( Map.Entry<String,MetricValue> entry : totalMetricsMap.entrySet() ) {
							String totalMetricName = entry.getKey();
							String totalCostAmount = entry.getValue().amount();
							String totalCostUnit = entry.getValue().unit();
							if ( Message.isDebugOn ) {
								Message.printStatus(2, routine, "Metric \"" + totalMetricName + "\" amount = " + totalCostAmount + " " + totalCostUnit );
							}
						}
					}
					else {
						Message.printStatus(2, routine, "No total costs are available.");
					}
					if ( resultByTime.hasGroups() ) {
						for ( Group group : resultByTime.groups() ) {
							Message.printStatus(2, routine, "Group keys: " + group.keys());
							//Message.printStatus(2, routine, "Group service: " + getServiceNameFromKeys(group.keys()));
							Map<String,MetricValue> groupMetricsMap = group.metrics();
							for ( Map.Entry<String,MetricValue> entry : groupMetricsMap.entrySet() ) {
								String groupMetricName = entry.getKey();
								String groupCostAmount = entry.getValue().amount();
								String groupCostUnit = entry.getValue().unit();
								if ( Message.isDebugOn ) {
									Message.printStatus(2, routine, "Metric \"" + groupMetricName + "\" amount = " + groupCostAmount + " " + groupCostUnit );
								}
							}
						}
					}
					else {
						Message.printStatus(2, routine, "No group costs are available.");
						// Process the raw data.
						Message.printStatus(2, routine, "Have raw data records.");
					}
				}

				TableRecord rec = null;
				// Used to control whether duplicate records are allowed in tabular output:
				// - currently always add each cost record to the table even if the date and other data are not unique
				//boolean allowDuplicates = false;

				boolean doGroupByTable = false;
				boolean doTotalTable = false;
				if ( groupByTable != null ) {
					doGroupByTable = true;
				}
				if ( totalTable != null ) {
					doTotalTable = true;
				}

				// Process the output:
				// - process the table and/or time series together
				if ( doGroupByTable || doTotalTable || createGroupedTimeSeries || createTotalTimeSeries ) {
					if ( doGroupByTable ) {
						Message.printStatus(2, routine, "Transferring Cost Explorer results to grouped table.");
					}
					if ( doTotalTable ) {
						Message.printStatus(2, routine, "Transferring Cost Explorer results to total table.");
					}
					if ( createGroupedTimeSeries ) {
						Message.printStatus(2, routine, "Transferring Cost Explorer results to groupled time series.");
					}
					if ( createTotalTimeSeries ) {
						Message.printStatus(2, routine, "Transferring Cost Explorer results to total time series.");
					}
					for ( ResultByTime resultByTime : resultsByTimeList ) {
						// For daily granularity:
						// - the start and end will be one day such as 2024-09-03 and 2024-09-04
						// - the period is inclusive of the start and exclusive of the end (e.g., 00:00:00 to 00:00:59),
						//   so midnight is considered part of the "next" day
						Message.printStatus(2, routine, "Time period: " + resultByTime.timePeriod());
						Message.printStatus(2, routine, "  Estimated?: " + resultByTime.estimated());
						Message.printStatus(2, routine, "  Has group metrics?: " + resultByTime.hasGroups());
						Message.printStatus(2, routine, "  Has total metrics?: " + resultByTime.hasTotal());
						// Returns a string for the start of the period:
						// - is it always a day YYYY-MM-DD?
						String startDateString = resultByTime.timePeriod().start();
						DateTime startDate = DateTime.parse(startDateString);
						String endDateString = resultByTime.timePeriod().end();
						DateTime endDate = DateTime.parse(endDateString);
						if ( granularity == AwsBillingGranularityType.MONTHLY ) {
							//startDate.setPrecision(DateTime.PRECISION_MONTH);
							startDate.setPrecision(DateTime.PRECISION_DAY);
							endDate.setPrecision(DateTime.PRECISION_DAY);
						}
						else if ( granularity == AwsBillingGranularityType.DAILY ) {
							startDate.setPrecision(DateTime.PRECISION_DAY);
							endDate.setPrecision(DateTime.PRECISION_DAY);
						}
						else if ( granularity == AwsBillingGranularityType.HOURLY ) {
							// TODO smalers 2024-01-14 might also be DAILY?  Assume this is the case for now since hourly is not often used.
							//costDate.setPrecision(DateTime.PRECISION_HOUR);
						}
						// Returns a string for the end of the period, exclusive:
						// - so for monthly granularity, YYYY-MM-01 includes costs for the day for the end of the previous month
						//String endDate = resultByTime.timePeriod().start();
						if ( resultByTime.hasGroups() ) {
							// Process the group data for the date:
							// - for example, group by service and system identifier tag
							for ( Group group : resultByTime.groups() ) {
								Message.printStatus(2, routine, "  Group keys: " + group.keys());
								//Message.printStatus(2, routine, "  Group service: " + getServiceNameFromKeys(group.keys()));
								for ( String key : group.keys() ) {
									// This is not clear but the list always seems to have one key, which is the service.
									if ( Message.isDebugOn ) {
										Message.printStatus(2, routine, "    Group has key: " + key);
									}
								}
								Map<String,MetricValue> groupMetricsMap = group.metrics();
								for ( Map.Entry<String,MetricValue> entry : groupMetricsMap.entrySet() ) {
									String groupMetricName = entry.getKey();
									String groupCostAmount = entry.getValue().amount();
									String groupCostUnit = entry.getValue().unit();
									if ( Message.isDebugOn ) {
										Message.printStatus(2, routine, "    Metric \"" + groupMetricName + "\" amount = " + groupCostAmount + " " + groupCostUnit );
									}
									String groupItem1 = cleanGroupKey(group.keys().get(0));
									String groupItem2 = "";
									if ( groupedTable_groupBy2Col >= 0 ) {
										groupItem2 = cleanGroupKey(group.keys().get(1));
									}
									// Increment the 'chunkRecordCount' regardless of whether saved in a table or time series.
									++chunkRecordCount;
									if ( doGroupByTable ) {
										// Initialize the record to null for checks below.
										rec = null;
										/* For now always add new records.
    									if ( !allowDuplicates ) {
    										// Try to match the bucket name, which is the unique identifier.
    										rec = table.getRecord ( bucketNameCol, bucketName );
    									}
										 */
										if ( rec == null ) {
											// Create a new empty record.
											rec = groupByTable.addRecord(groupByTable.emptyRecord());
										}
										// Set the data in the table record.
										rec.setFieldValue(groupedTable_recordTypeCol, "Grouped");
										rec.setFieldValue(groupedTable_startDateCol, startDate);
										rec.setFieldValue(groupedTable_endDateCol, endDate);
										rec.setFieldValue(groupedTable_granularityCol, granularity.toString());
										if ( groupedTable_groupBy1Col >= 0 ) {
											rec.setFieldValue(groupedTable_groupBy1Col, groupBy1.toString() );
										}
										if ( groupedTable_groupByTag1Col >= 0 ) {
											rec.setFieldValue(groupedTable_groupByTag1Col, groupByTag1 );
										}
										if ( groupedTable_groupItem1Col >= 0 ) {
											rec.setFieldValue(groupedTable_groupItem1Col, groupItem1 );
										}
										if ( groupedTable_groupBy2Col >= 0 ) {
											rec.setFieldValue(groupedTable_groupBy2Col, groupBy2.toString() );
											if ( groupedTable_groupByTag2Col >= 0 ) {
												rec.setFieldValue(groupedTable_groupByTag2Col, groupByTag2 );
											}
											rec.setFieldValue(groupedTable_groupItem2Col, groupItem2);
										}
										/*
										if ( groupedTable_regionCol >= 0 ) {
											rec.setFieldValue(groupedTable_regionCol, groupItem2);
										}
										if ( groupedTable_serviceCol >= 0 ) {
											rec.setFieldValue(groupedTable_serviceCol, groupItem2);
										}
										*/
										rec.setFieldValue(groupedTable_metricCol, groupMetricName);
										rec.setFieldValue(groupedTable_amountCol, Double.valueOf(groupCostAmount) );
										rec.setFieldValue(groupedTable_unitsCol, groupCostUnit);
									}
									if  ( createGroupedTimeSeries ) {
										addGroupedTimeSeriesData ( groupedTsList, inputStart, inputEnd,
											startDate, granularity,
											groupBy1, groupByTag1, groupItem1,
											groupBy2, groupByTag2, groupItem2,
											groupMetricName, Double.valueOf(groupCostAmount), groupCostUnit,
											groupedTimeSeriesLocationId, groupedTimeSeriesDataType,
											timeSeriesMissingGroupBy,
											// Metadata needed to match a time series identifier.
											metadataNames, metadataValues
										);
									}
								}
							}
						}
						else {
							Message.printStatus(2, routine, "  No group costs are available (processing raw data records).");
						}
						if ( doTotalTable && resultByTime.hasTotal() ) {
							// Process the total cost for the date:
							// - this data will be redundant with grouped data so put in a separate table
							Map<String, MetricValue> totalMetricsMap = resultByTime.total();
							Message.printStatus(2, routine, "Time period: " + resultByTime.timePeriod());
							Message.printStatus(2, routine, "  Have " + totalMetricsMap.size() + " items in ResultByTime.total()" );
							for ( Map.Entry<String,MetricValue> entry : totalMetricsMap.entrySet() ) {
								String totalMetricName = entry.getKey();
								String totalCostAmount = entry.getValue().amount();
								String totalCostUnit = entry.getValue().unit();
								if ( Message.isDebugOn ) {
									Message.printStatus(2, routine, "Metric=\"" + totalMetricName + "\" cost amount=" + totalCostAmount + " " + totalCostUnit );
								}

								String groupItem1 = "";
								String groupItem2 = "";
								//String groupItem3 = "";
								// Increment the record processed whether saved in a table or time series or not.
								++chunkRecordCount;
								if ( doTotalTable ) {
									// Initialize to null for checks below.
									rec = null;
									/* For now always add new records.
   									if ( !allowDuplicates ) {
   										// Try to match the bucket name, which is the unique identifier.
   										rec = table.getRecord ( bucketNameCol, bucketName );
   									}
									 */
									if ( rec == null ) {
										// Create a new empty record.
										rec = totalTable.addRecord(totalTable.emptyRecord());
									}
									// Set the data in the table record.
									rec.setFieldValue(totalTable_recordTypeCol, "Total");
									rec.setFieldValue(totalTable_startDateCol, startDate);
									rec.setFieldValue(totalTable_endDateCol, endDate);
									rec.setFieldValue(totalTable_granularityCol, granularity.toString());
									rec.setFieldValue(totalTable_metricCol, totalMetricName);
									rec.setFieldValue(totalTable_amountCol, Double.valueOf(totalCostAmount) );
									rec.setFieldValue(totalTable_unitsCol, totalCostUnit);
								}
								if  ( createTotalTimeSeries ) {
									addTotalTimeSeriesData ( totalTsList, inputStart, inputEnd,
										startDate, granularity,
										groupBy1, groupByTag1, groupItem1,
										groupBy2, groupByTag2, groupItem2,
										totalMetricName, Double.valueOf(totalCostAmount), totalCostUnit,
										totalTimeSeriesLocationId, totalTimeSeriesDataType,
										timeSeriesMissingGroupBy,
										// Metadata needed to match a time series identifier.
										metadataNames, metadataValues
									);
								}
							}
						}
						else {
							Message.printStatus(2, routine, "No total costs are available.");
						}
					}
				}
			}
			while ( nextPageToken != null );

			// It appears that AWS limits the total number of ResultByTime internal records to 5000?
			// - see: https://docs.aws.amazon.com/cost-management/latest/userguide/ce-resource-daily.html
			//   (but not clear if that is the same thing)
			// - there there does not seem way to be a way to increase the limit or page
			//   (or is buried in the documentation somewhere)
			// - therefore, do a check here to see if the count is exactly 5000 and if so some programming is probably needed

			if ( chunkRecordCount == 5000 ) {
				message = "The number of cost records is 5000 for request period " + requestStart + " to " + requestEnd +
					" - probably need to use a shorter TimeChunk to avoid truncation.";
				Message.printWarning(3, routine, message );
				status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Change the TimeChunk for queries." ) );
			}
		}

    	// Set the property indicating the number of rows in the groupby table.
        if ( (groupedTableRowCountProperty != null) && !groupedTableRowCountProperty.isEmpty() ) {
          	int rowCount = 0;
          	if ( groupByTable != null ) {
          		rowCount = groupByTable.getNumberOfRecords();
          	}
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", groupedTableRowCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(rowCount) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + groupedTableRowCountProperty + "\") from processor.";
               	Message.printWarning(logLevel,
                   	MessageUtil.formatMessageTag( commandTag, ++warningCount),
                   	routine, message );
               	status.addToLog ( CommandPhaseType.RUN,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Report the problem to software support." ) );
           	}
        }

    	// Set the property indicating the number of rows in the total table.
        if ( (totalTableRowCountProperty != null) && !totalTableRowCountProperty.isEmpty() ) {
          	int rowCount = 0;
          	if ( totalTable != null ) {
          		rowCount = totalTable.getNumberOfRecords();
          	}
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", totalTableRowCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(rowCount) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + totalTableRowCountProperty + "\") from processor.";
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
	 * Get EBS snapshot data.
	 * @param processor the processor for commands
	 * @param ebsSnapshotsTable the table for EBS snapshots, or null if no output
	 * @param status the command status for logging
	 * @param logLevel the logging level for messages
	 * @param warningCount the number of warnings before this function, will be added to and returned
	 * @param commandTag the command tag for identifying log messages
	 * @return the updated warning count
	 */
	private int doEbsSnapshots (
		CommandProcessor processor,
		DataTable ebsSnapshotsTable,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doEc2Snapshots";
		//String message;

		// Get the account information for the current session:
		// - this is used to filter the snapshots
		// - if not filtered, the API seems to return a very large number of snapshots (bug?)
		StsClient stsClient = StsClient.builder().build();
		GetCallerIdentityResponse identityResponse = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
		String accountId = identityResponse.account();

        // Step 1: Create an EC2 client for the default region to get the list of regions.
        Ec2Client ec2ClientGeneral = Ec2Client.builder().build();

        // Step 2: Get the list of regions.
        DescribeRegionsResponse regionsResponse = ec2ClientGeneral.describeRegions(DescribeRegionsRequest.builder().build());

        // Step 3: Loop through each region:
        // - get the service instances of interest in each region
        // - save the service properties to get a comprehensive list of relevant metadata and tags
        List<String> ec2TagNameList = new ArrayList<>();
        List<String> snapshotTagNameList = new ArrayList<>();
        List<AwsEbsSnapshotProperties> snapshotProperties = new ArrayList<>();
        for ( software.amazon.awssdk.services.ec2.model.Region region : regionsResponse.regions() ) {
            String regionName = region.regionName();
            Message.printStatus(2,routine,"Fetching snapshot data for region: " + regionName);

            // Step 4: Create an EC2 client for the specific region.
            Ec2Client ec2ClientForRegion = Ec2Client.builder()
                .region(Region.of(regionName))  // Set the region dynamically.
                .build();

            DescribeSnapshotsResponse snapshotsResponse = ec2ClientForRegion.describeSnapshots (
               	DescribeSnapshotsRequest.builder()
               	.ownerIds(accountId)
              	.build());

            Message.printStatus(2, routine, "Have " + snapshotsResponse.snapshots().size() + " EBS snapshots for region " + region.regionName() );

            for ( Snapshot snapshot : snapshotsResponse.snapshots() ) {
               	// Create an object for snapshot properties.
               	AwsEbsSnapshotProperties props = new AwsEbsSnapshotProperties ();
              	// Get the tags associated with the instance.
               	/*
               	for ( Tag tag : instance.tags() ) {
                   	props.setEc2Tag(tag.key(), tag.value());
                   	// Add the tag names as columns.
                   	boolean foundTag = false;
                   	for ( String tagName0 : ec2TagNameList ) {
                   		if ( tagName0.equals(tag.key()) ) {
                   			foundTag = true;
                   			break;
                   		}
                   	}
                   	if ( !foundTag ) {
                   		ec2TagNameList.add(tag.key());
                   	}
               	}
               	*/
               	snapshotProperties.add(props);
               	props.setRegion ( region.regionName() );
               	//props.setEc2InstanceId ( instance.instanceId() );
               	props.setSnapshotId(snapshot.snapshotId() );
               	props.setSnapshotDescription(snapshot.description() );
               	props.setSnapshotVolumeSizeGB(snapshot.volumeSize() );
               	for ( Tag tag : snapshot.tags() ) {
               		props.setSnapshotTag(tag.key(), tag.value());
               		boolean foundTag = false;
               		for ( String tagName0 : snapshotTagNameList ) {
               			if ( tagName0.equals(tag.key()) ) {
               				foundTag = true;
               				break;
               			}
               		}
               		if ( !foundTag ) {
               			snapshotTagNameList.add(tag.key());
               		}
               	}
            }
        }

        // Add columns to the table for the EBS snapshot properties.
        HashMap<String,Integer> snapshotTableMap = new HashMap<>();
        int regionCol = -1;
        //int ec2InstanceIdCol = -1;
        int snapshotIdCol = -1;
        int snapshotDescriptionCol = -1;
        int snapshotVolumeSizeGBCol = -1;
        createEbsSnapshotsTableColumns ( ebsSnapshotsTable, ec2TagNameList, snapshotTagNameList, snapshotTableMap );
        regionCol = snapshotTableMap.get("RegionCol");
        snapshotIdCol = snapshotTableMap.get("EBSSnapshot/IdCol");
        snapshotDescriptionCol = snapshotTableMap.get("EBSSnapshot/DescriptionCol");
        snapshotVolumeSizeGBCol = snapshotTableMap.get("EBSSnapshot/VolumeSizeGBCol");

        // Set the values in the table.
       	TableRecord rec = null;
        for ( AwsEbsSnapshotProperties props : snapshotProperties ) {
			rec = ebsSnapshotsTable.addRecord(ebsSnapshotsTable.emptyRecord());
			rec.setFieldValue(regionCol, props.getRegion());
			rec.setFieldValue(snapshotIdCol, props.getSnapshotId());
			rec.setFieldValue(snapshotDescriptionCol, props.getSnapshotDescription());
			rec.setFieldValue(snapshotVolumeSizeGBCol, props.getSnapshotVolumeSizeGB());
			for ( String tagName : snapshotTagNameList ) {
				rec.setFieldValue(snapshotTableMap.get("EBSSnapshot-Tag/" + tagName + "Col"), props.getSnapshotTagValue(tagName) );
			}
        }

        // Return the updated warning count.
        return warningCount;
	}

	/**
	 * Get EC2 images (AMI) data.
	 * @param processor the processor for commands
	 * @param amiImagesTable the table for AMI images, or null if no output
	 * @param status the command status for logging
	 * @param logLevel the logging level for messages
	 * @param warningCount the number of warnings before this function, will be added to and returned
	 * @param commandTag the command tag for identifying log messages
	 * @return the updated warning count
	 */
	private int doEc2Images (
		CommandProcessor processor,
		DataTable ec2ImagesTable,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doEc2Images";
		//String message;

		// Get the account information for the current session:
		// - this is used to filter the snapshots
		// - if not filtered, the API seems to return a very large number of snapshots (bug?)
		StsClient stsClient = StsClient.builder().build();
		GetCallerIdentityResponse identityResponse = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
		String accountId = identityResponse.account();

        // Step 1: Create an EC2 client for the default region to get the list of regions.
        Ec2Client ec2ClientGeneral = Ec2Client.builder().build();

        // Step 2: Get the list of regions.
        DescribeRegionsResponse regionsResponse = ec2ClientGeneral.describeRegions(DescribeRegionsRequest.builder().build());

        // Step 3: Loop through each region:
        // - get the service instances of interest in each region
        // - save the service properties to get a comprehensive list of relevant metadata and tags
        //List<String> ec2TagNameList = new ArrayList<>();
        List<String> imageTagNameList = new ArrayList<>();
        List<AwsAmiProperties> imageProperties = new ArrayList<>();
        for ( software.amazon.awssdk.services.ec2.model.Region region : regionsResponse.regions() ) {
            String regionName = region.regionName();
            Message.printStatus(2,routine,"Fetching image data for region: " + regionName);

            // Step 4: Create an EC2 client for the specific region.
            Ec2Client ec2ClientForRegion = Ec2Client.builder()
                .region(Region.of(regionName))  // Set the region dynamically.
                .build();

            DescribeImagesResponse imagesResponse = ec2ClientForRegion.describeImages (
               	DescribeImagesRequest.builder()
               	.owners(accountId)
              	.build());

            Message.printStatus(2, routine, "Have " + imagesResponse.images().size() + " EC2 images (AMI) for region " + region.regionName() );

            for ( Image image : imagesResponse.images() ) {
               	// Create an object for image properties.
               	AwsAmiProperties props = new AwsAmiProperties ();
               	imageProperties.add(props);
               	props.setRegion ( region.regionName() );
               	//props.setEc2InstanceId ( instance.instanceId() );
               	props.setImageId(image.imageId() );
               	props.setImageDescription(image.description() );
               	//props.setImageSizeGB(image. );
               	for ( Tag tag : image.tags() ) {
               		props.setImageTag(tag.key(), tag.value());
               		boolean foundTag = false;
               		for ( String tagName0 : imageTagNameList ) {
               			if ( tagName0.equals(tag.key()) ) {
               				foundTag = true;
               				break;
               			}
               		}
               		if ( !foundTag ) {
               			imageTagNameList.add(tag.key());
               		}
               	}
            }
        }

        // Add columns to the table for the image properties.
        HashMap<String,Integer> imageTableMap = new HashMap<>();
        int regionCol = -1;
        //int ec2InstanceIdCol = -1;
        int imageIdCol = -1;
        int imageDescriptionCol = -1;
        //int imageSizeGBCol = -1;
        createEc2ImagesTableColumns ( ec2ImagesTable, imageTagNameList, imageTagNameList, imageTableMap );
        regionCol = imageTableMap.get("RegionCol");
        imageIdCol = imageTableMap.get("EC2Image/IdCol");
        imageDescriptionCol = imageTableMap.get("EC2Image/DescriptionCol");
        //imageSizeGBCol = snapshotTableMap.get("EC2Image/SizeGBCol");

        // Set the values in the table.
       	TableRecord rec = null;
        for ( AwsAmiProperties props : imageProperties ) {
			rec = ec2ImagesTable.addRecord(ec2ImagesTable.emptyRecord());
			rec.setFieldValue(regionCol, props.getRegion());
			rec.setFieldValue(imageIdCol, props.getImageId());
			rec.setFieldValue(imageDescriptionCol, props.getImageDescription());
			//rec.setFieldValue(imageSizeGBCol, props.getImageSizeGB());
			for ( String tagName : imageTagNameList ) {
				rec.setFieldValue(imageTableMap.get("EC2Image-Tag/" + tagName + "Col"), props.getImageTagValue(tagName) );
			}
        }

        // Return the updated warning count.
        return warningCount;
	}


	/**
	 * Get service data for high-cost services (mostly related to EC2).
	 * @param processor the processor for commands
	 * @param servicePropertiesTable the table for service properties, or null if no output
	 * @param status the command status for logging
	 * @param logLevel the logging level for messages
	 * @param warningCount the number of warnings before this function, will be added to and returned
	 * @param commandTag the command tag for identifying log messages
	 * @return the updated warning count
	 */
	private int doEc2Properties (
		CommandProcessor processor,
		DataTable servicePropertiesTable,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doEc2Properties";
		String message;

        // Step 1: Create an EC2 client for the default region to get the list of regions.
        Ec2Client ec2ClientGeneral = Ec2Client.builder().build();

        // Step 2: Get the list of regions.
        DescribeRegionsResponse regionsResponse = ec2ClientGeneral.describeRegions(DescribeRegionsRequest.builder().build());

        // Step 3: Loop through each region:
        // - get the service instances of interest in each region
        // - save the service properties to get a comprehensive list of relevant metadata and tags
        List<String> ec2TagNameList = new ArrayList<>();
        List<String> vpcTagNameList = new ArrayList<>();
        List<String> vpnConnectionTagNameList = new ArrayList<>();
        List<String> elasticIpTagNameList = new ArrayList<>();
        List<String> ebsVolumeTagNameList = new ArrayList<>();
        List<AwsEc2Properties> serviceProperties = new ArrayList<>();
        for ( software.amazon.awssdk.services.ec2.model.Region region : regionsResponse.regions() ) {
            String regionName = region.regionName();
            Message.printStatus(2,routine,"Fetching instance data for region: " + regionName);

            // Step 4: Create an EC2 client for the specific region.
            Ec2Client ec2ClientForRegion = Ec2Client.builder()
                .region(Region.of(regionName))  // Set the region dynamically.
                .build();

            // Step 5: Describe instances in this region.
            DescribeInstancesResponse describeInstancesResponse = ec2ClientForRegion.describeInstances(DescribeInstancesRequest.builder().build());

            // Step 6: Process and print instance details.
            for ( Reservation reservation : describeInstancesResponse.reservations() ) {
                for (Instance instance : reservation.instances()) {

                	// Get the main EC2 information.

                    Message.printStatus(2,routine,"Region: " + regionName + ", Instance ID: " + instance.instanceId());
                    // Create an object for service properties.
                    AwsEc2Properties props = new AwsEc2Properties ();
                    serviceProperties.add(props);
                    props.setService("Amazon Elastic Compute Cloud");
                    props.setRegion ( region.regionName() );
                    props.setEc2InstanceId ( instance.instanceId() );
                    props.setEc2InstanceType ( instance.instanceTypeAsString() );
                    props.setEc2InstanceState ( instance.state().nameAsString() );

                    // Get the tags associated with the instance.
                    for ( Tag tag : instance.tags() ) {
                        props.setEc2Tag(tag.key(), tag.value());
                        // Add the tag names as columns.
                        boolean foundTag = false;
                        for ( String tagName0 : ec2TagNameList ) {
                        	if ( tagName0.equals(tag.key()) ) {
                        		foundTag = true;
                        		break;
                        	}
                        }
                        if ( !foundTag ) {
                        	ec2TagNameList.add(tag.key());
                        }
                    }

                    // Get the VPC and VPN information for the EC2 instance:
                    // - only use the single VPC that is associated with the EC2 instance

                    String vpcId = instance.vpcId();
                    List<String> vpcIdList = new ArrayList<>();
                    vpcIdList.add(vpcId);
                    DescribeVpcsResponse vpcsResponse = ec2ClientForRegion.describeVpcs(
                    	DescribeVpcsRequest.builder().
                    	// Filter on the specific VPC ID.
                    	vpcIds(vpcIdList).
                    	build());
                    if ( vpcsResponse.vpcs().size() > 1 ) {
                    	// Warn because currently the code does not handle.
                    	message = "EC2 instance " + instance.instanceId() +
                    		" has " + vpcsResponse.vpcs().size() + " VPCs - only know how to handle 1 for properties table.";
						Message.printWarning(3,routine,message);
						status.addToLog ( CommandPhaseType.RUN,
							new CommandLogRecord(CommandStatusType.WARNING,
								message, "Might need to enhance the software." ) );
                    }
                   	for ( Vpc vpc : vpcsResponse.vpcs() ) {
                   		props.setVpcId(vpc.vpcId() );
                   		for ( Tag tag : vpc.tags() ) {
                   			props.setVpcTag(tag.key(), tag.value());
                   			boolean foundTag = false;
                   			for ( String tagName0 : vpcTagNameList ) {
                   				if ( tagName0.equals(tag.key()) ) {
                   					foundTag = true;
                   					break;
                   				}
                   			}
                   			if ( !foundTag ) {
                   				vpcTagNameList.add(tag.key());
                   			}
                   		}
                   		
                   		// Process available VPN information, which is associated with a VPC.
                   		
                        // Request VPN connection details
                        DescribeVpnConnectionsRequest vpnRequest = DescribeVpnConnectionsRequest.builder().build();
                        DescribeVpnConnectionsResponse vpnResponse = ec2ClientForRegion.describeVpnConnections(vpnRequest);

                        if ( vpnResponse.vpnConnections().size() > 1 ) {
                        	// Warn because currently the code does not handle.
                    	    message = "EC2 instance " + instance.instanceId() + " VPC " + vpcId +
                    		    " has " + vpnResponse.vpnConnections().size() + " VPNs - only know how to handle 1 for properties table.";
						    Message.printWarning(3,routine,message);
						    status.addToLog ( CommandPhaseType.RUN,
							    new CommandLogRecord(CommandStatusType.WARNING,
							        message, "Might need to enhance the software." ) );
                       }
                       // Iterate through the VPN connections and find ones associated with the VP:
                       // - will only process the first one
                       for ( VpnConnection vpnConnection : vpnResponse.vpnConnections() ) {
                           Message.printStatus(2,routine,"VPN Connection ID: " + vpnConnection.vpnConnectionId());
                           //System.out.println("VPC ID: " + vpnConnection.vpcId());
                           Message.printStatus(2,routine,"State: " + vpnConnection.stateAsString());
                           Message.printStatus(2,routine,"Customer Gateway ID: " + vpnConnection.customerGatewayId());
                           Message.printStatus(2,routine,"Virtual Private Gateway ID: " + vpnConnection.vpnGatewayId());

                           props.setVpnConnectionId(vpnConnection.vpnConnectionId() );
                           props.setVpnConnectionState(vpnConnection.stateAsString() );
                           props.setVpnCustomerGatewayId(vpnConnection.customerGatewayId() );
                           props.setVpnGatewayId(vpnConnection.vpnGatewayId() );
                           props.setVpnCustomerGatewayOutsideIp(vpnConnection.customerGatewayConfiguration() );
                               
                           // Set tags for the VPN connection.
                           for ( Tag tag : vpnConnection.tags() ) {
                         	   props.setVpnConnectionTag(tag.key(), tag.value());
                           	   boolean foundTag = false;
                           	   for ( String tagName0 : vpnConnectionTagNameList ) {
                           		   if ( tagName0.equals(tag.key()) ) {
                           			   foundTag = true;
                           				break;
                               		}
                           	   }
                           	   if ( !foundTag ) {
                           		   vpnConnectionTagNameList.add(tag.key());
                           	   }
                           }

                           // Break out after the first VPN is processed since can only handle one.
                  		   break;
                        }
                   		
                   		// Break out after the first VPC is processed since can only handle one.
                   		break;
                   	}
                   	
                   	// Get the Elastic IP information.
                   	
                   	String publicIp = instance.publicIpAddress();
                   	String publicDnsName = instance.publicDnsName();
                   	if ( publicDnsName == null ) {
                   		publicDnsName = "";
                   	}
                   	props.setPublicDnsName(publicDnsName);
                   	String privateIp = instance.privateIpAddress();
                   	if ( privateIp == null ) {
                   		privateIp = "";
                   	}
                   	props.setPrivateIp(privateIp);
                   	String privateDnsName = instance.privateDnsName();
                   	if ( privateDnsName == null ) {
                   		privateDnsName = "";
                   	}
                   	props.setPrivateDnsName(privateDnsName);
                   	String elasticIp = "";

                   	if ( publicIp == null ) {
                   		publicIp = "";
                   	}
                   	else {
                   		// Check whether the public IP is an elastic IP:
                        // - this returns all the IP addresses associated with the account, not just the specific EC2 instance
                        DescribeAddressesRequest addressesRequest = DescribeAddressesRequest.builder().build();
                        DescribeAddressesResponse addressesResponse = ec2ClientForRegion.describeAddresses(addressesRequest);

                        String publicIpToCheck = publicIp;

                    	message = "Account for EC2 instance " + instance.instanceId() +
                    		    " has " + addressesResponse.addresses().size() + " addresses in region " + region.regionName() +
                    		    " - will find the matching public IP address.";
					    Message.printStatus(2,routine,message);
                        props.setPublicIp(publicIp);
                        for ( Address address : addressesResponse.addresses() ) {
                            if ( address.publicIp().equals(publicIpToCheck) ) {
                            	// The EC2 instance is a public address, which is the Elastic IP address.
                                Message.printStatus(2,routine,"Elastic IP Address: " + address.publicIp());
                                Message.printStatus(2,routine,"Allocation ID: " + address.allocationId());
                                Message.printStatus(2,routine,"Instance ID: " + address.instanceId());
                                Message.printStatus(2,routine,"Association ID: " + address.associationId());
                                Message.printStatus(2,routine,"Network Interface ID: " + address.networkInterfaceId());
                                elasticIp = publicIp;
                                props.setElasticIp(elasticIp);

                                // Set tags for the Elastic IP.
                                for ( Tag tag : address.tags() ) {
                         	        props.setElasticIpTag(tag.key(), tag.value());
                           	        boolean foundTag = false;
                           	        for ( String tagName0 : elasticIpTagNameList ) {
                           		        if ( tagName0.equals(tag.key()) ) {
                           			        foundTag = true;
                           				    break;
                               		    }
                           	        }
                           	        if ( !foundTag ) {
                           		        elasticIpTagNameList.add(tag.key());
                           	        }
                                }
                                // Matched address so quit searching.
                                break;
                            }
                        }
                   	}

                    // Get the EBS volume information for the specific EC2 instance:
                    // - must filter for the instance

                    Filter instanceFilter = Filter.builder()
                    	.name("attachment.instance-id")
                    	.values(instance.instanceId())
                    	.build();

                    DescribeVolumesResponse volumesResponse = ec2ClientForRegion.describeVolumes(
                    	DescribeVolumesRequest.builder()
                    	.filters(instanceFilter)
                    	.build());
                    if ( volumesResponse.volumes().size() > 1 ) {
                    	// Warn because currently the code does not handle.
                    	message = "EC2 instance " + instance.instanceId() +
                    		" has " + volumesResponse.volumes().size() + " EBS volumes - only know how to handle 1 for properties table (last in list will be used).";
						Message.printWarning(3,routine,message);
						status.addToLog ( CommandPhaseType.RUN,
							new CommandLogRecord(CommandStatusType.WARNING,
								message, "Check the command parameters." ) );
                    }
                    for ( Volume volume : volumesResponse.volumes() ) {
                    	props.setEbsVolumeId(volume.volumeId() );
                    	for ( Tag tag : volume.tags() ) {
                    		props.setEbsVolumeTag(tag.key(), tag.value());
                    		boolean foundTag = false;
                    		for ( String tagName0 : ebsVolumeTagNameList ) {
                    			if ( tagName0.equals(tag.key()) ) {
                    				foundTag = true;
                    				break;
                    			}
                    		}
                    		if ( !foundTag ) {
                    			ebsVolumeTagNameList.add(tag.key());
                    		}
                    	}
                    }
                } // End EC2 instance loop.
            } // End EC2 reservation loop.
        } // End regions loop.

        // Add columns to the table for the EC2 properties.
        HashMap<String,Integer> servicePropertiesTableMap = new HashMap<>();
        int serviceCol = -1;
        int regionCol = -1;
        int publicIpCol = -1;
        int publicDnsNameCol = -1;
        int privateIpCol = -1;
        int privateDnsNameCol = -1;
        int ec2InstanceIdCol = -1;
        int ec2InstanceStateCol = -1;
        int ec2InstanceTypeCol = -1;
        int vpcIdCol = -1;
        int vpnConnectionIdCol = -1;
        int vpnConnectionStateCol = -1;
        int vpnCustomerGatewayIdCol = -1;
        int vpnCustomerGatewayOutsideIpCol = -1;
        int vpnGatewayIdCol = -1;
        int elasticIpCol = -1;
        int ebsVolumeIdCol = -1;
        createEc2PropertiesTableColumns (
        	servicePropertiesTable,
        	ec2TagNameList,
        	vpcTagNameList,
        	vpnConnectionTagNameList,
        	elasticIpTagNameList,
        	ebsVolumeTagNameList,
        	servicePropertiesTableMap );
        serviceCol = servicePropertiesTableMap.get("ServiceCol");
        regionCol = servicePropertiesTableMap.get("RegionCol");
        publicIpCol = servicePropertiesTableMap.get("PublicIpCol");
        publicDnsNameCol = servicePropertiesTableMap.get("PublicDnsNameCol");
        privateIpCol = servicePropertiesTableMap.get("PrivateIpCol");
        privateDnsNameCol = servicePropertiesTableMap.get("PrivateDnsNameCol");
        ec2InstanceIdCol = servicePropertiesTableMap.get("EC2/InstanceIdCol");
        ec2InstanceTypeCol = servicePropertiesTableMap.get("EC2/InstanceTypeCol");
        ec2InstanceStateCol = servicePropertiesTableMap.get("EC2/InstanceStateCol");
        vpcIdCol = servicePropertiesTableMap.get("VPC/IdCol");
        vpnConnectionIdCol = servicePropertiesTableMap.get("VPN/ConnectionIdCol");
        vpnConnectionStateCol = servicePropertiesTableMap.get("VPN/ConnectionStateCol");
        vpnCustomerGatewayIdCol = servicePropertiesTableMap.get("VPN/CustomerGatewayIdCol");
        vpnCustomerGatewayOutsideIpCol = servicePropertiesTableMap.get("VPN/CustomerGatewayOutsideIpCol");
        vpnGatewayIdCol = servicePropertiesTableMap.get("VPN/GatewayIdCol");
        elasticIpCol = servicePropertiesTableMap.get("ElasticIpCol");
        ebsVolumeIdCol = servicePropertiesTableMap.get("EBSVolume/IdCol");
        
        // Set the values in the table.
       	TableRecord rec = null;
        for ( AwsEc2Properties props : serviceProperties ) {
			rec = servicePropertiesTable.addRecord(servicePropertiesTable.emptyRecord());
			rec.setFieldValue(serviceCol, props.getService());
			rec.setFieldValue(regionCol, props.getRegion());
			rec.setFieldValue(publicIpCol, props.getPublicIp());
			rec.setFieldValue(publicDnsNameCol, props.getPublicDnsName());
			rec.setFieldValue(privateIpCol, props.getPrivateIp());
			rec.setFieldValue(privateDnsNameCol, props.getPrivateDnsName());
			rec.setFieldValue(ec2InstanceIdCol, props.getEc2InstanceId());
			rec.setFieldValue(ec2InstanceStateCol, props.getEc2InstanceState());
			rec.setFieldValue(ec2InstanceTypeCol, props.getEc2InstanceType());
			rec.setFieldValue(vpcIdCol, props.getVpcId());
			rec.setFieldValue(vpnConnectionIdCol, props.getVpnConnectionId());
			rec.setFieldValue(vpnConnectionStateCol, props.getVpnConnectionState());
			rec.setFieldValue(vpnCustomerGatewayIdCol, props.getVpnCustomerGatewayId());
			rec.setFieldValue(vpnCustomerGatewayOutsideIpCol, props.getVpnCustomerGatewayOutsideIp());
			rec.setFieldValue(vpnGatewayIdCol, props.getVpnGatewayId());
			rec.setFieldValue(elasticIpCol, props.getElasticIp());
			rec.setFieldValue(ebsVolumeIdCol, props.getEbsVolumeId());
			for ( String tagName : ec2TagNameList ) {
				rec.setFieldValue(servicePropertiesTableMap.get("EC2-Tag/" + tagName + "Col"), props.getEc2TagValue(tagName) );
			}
			for ( String tagName : vpcTagNameList ) {
				rec.setFieldValue(servicePropertiesTableMap.get("VPC-Tag/" + tagName + "Col"), props.getVpcTagValue(tagName) );
			}
			for ( String tagName : vpnConnectionTagNameList ) {
				rec.setFieldValue(servicePropertiesTableMap.get("VPNConnection-Tag/" + tagName + "Col"), props.getVpnConnectionTagValue(tagName) );
			}
			for ( String tagName : elasticIpTagNameList ) {
				rec.setFieldValue(servicePropertiesTableMap.get("ElasticIp-Tag/" + tagName + "Col"), props.getVpnConnectionTagValue(tagName) );
			}
			for ( String tagName : ebsVolumeTagNameList ) {
				rec.setFieldValue(servicePropertiesTableMap.get("EBSVolume-Tag/" + tagName + "Col"), props.getEbsVolumeTagValue(tagName) );
			}
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
		return (new AwsBilling_JDialog ( parent, this, tableIDChoices )).ok();
	}

	/**
	 * Format the time series data type for the TSID.
	 * @param tsidBuilder the TSID to append to
	 * @param timeSeriesLocationID the format for the TSID
	 * @param timeSeriesDataType data type format as "Auto" (default) or a combination of characters and
	 * GROUPBYTAG1, TAG1VALUE, GROUPBYTAG2, TAG2VALUE, GROUPBY1, GROUPBY1VALUE, GROUPBY2, GROUPBY2VALUE.
	 * @param groupBy1 the "group by 1" enumeration, used in the time series identifier
	 * @param groupByTag1 "group by tag 1" string, used in the time series identifier
	 * @param groupItem1 the "group 1 item" string, used in the time series identifier
	 * @param groupBy2 the "group by 2" enumeration, used in the time series identifier
	 * @param groupByTag2 the "group by tag 2" string, used in the time series identifier
	 * @param groupItem2 the "group 2 item" string, used in the time series identifier
	 * @param metricName the AWS metric name (how costs are computed)
	 * @param metadataNames names of metadata used to format the time series locationID and data type
	 * @param metadataValues values of metadata used to format the time series locationID and data type
	 */
	private void formatTimeSeriesDataType (
		StringBuilder tsidBuilder, String timeSeriesLocationID, String timeSeriesDataType,
    	AwsBillingDimensionType groupBy1, String groupByTag1, String groupItem1,
    	AwsBillingDimensionType groupBy2, String groupByTag2, String groupItem2,
    	String metricName,
    	String [] metadataNames, String [] metadataValues
		) {
		// Process the built-in values first and then handle the free-form format.
		if ( timeSeriesLocationID.equals(this._Auto) ) {
			// For example
			tsidBuilder.append(groupItem1);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy1) ) {
			// Location is GroupBy1 so use the other group by as the data type.
			tsidBuilder.append(groupItem2);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy2) ) {
			// Location is GroupBy2 so use the other group by as the data type.
			tsidBuilder.append(groupItem1);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
		else {
			// Free-form data type:
			// - process the data type from metadata
			int nKeys = metadataNames.length;
			for ( int i = 0; i < nKeys; i++ ) {
				if ( metadataValues[i] != null ) {
					timeSeriesDataType = timeSeriesDataType.replace(metadataNames[i], metadataValues[i]);
				}
			}
			tsidBuilder.append(timeSeriesDataType);
		}
	}

	/**
	 * Format the time series location identifier for the TSID.
	 * @param tsidBuilder the TSID to append to
	 * @param timeSeriesLocationID the format for the TSID locationID
	 * @param timeSeriesDataType the format for the TSID data type
	 * @param timeSeriesMissingGroupBy the value to use when GroupBy items are missing
	 * @param groupBy1 the "group by 1" enumeration, used in the time series identifier
	 * @param groupByTag1 "group by tag 1" string, used in the time series identifier
	 * @param groupItem1 the "group 1 item" string, used in the time series identifier
	 * @param groupBy2 the "group by 2" enumeration, used in the time series identifier
	 * @param groupByTag2 the "group by tag 2" string, used in the time series identifier
	 * @param groupItem2 the "group 2 item" string, used in the time series identifier
	 * @param metadataNames names of metadata used to format the time series locationID and data type
	 * @param metadataValues values of metadata used to format the time series locationID and data type
	 */
	private void formatTimeSeriesLocationID (
		StringBuilder tsidBuilder, String timeSeriesLocationID, String timeSeriesMissingGroupBy,
    	AwsBillingDimensionType groupBy1, String groupByTag1, String groupItem1,
    	AwsBillingDimensionType groupBy2, String groupByTag2, String groupItem2,
    	String [] metadataNames, String [] metadataValues
	) {
		// Add the location:
		// - tag is preferred if tag is specified as any Group By
		// - if no tag, use the first Group by
		// - process the built-in formats first and then free-form format
		boolean tagFound = false;
		if ( timeSeriesLocationID.equals(this._Auto) ) {
			// Default behavior for location identifier.
			if ( (groupByTag1 != null) && !groupByTag1.isEmpty() ) {
				// Tag is specified in "group by 1":
				// - LocType is the tag variable and LocId is the tag value.
				tsidBuilder.append(groupByTag1);
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem1);
				if ( groupBy2 != null ) {
					// Append the "group by 2".
					tsidBuilder.append("-");
					tsidBuilder.append(groupItem2.toString());
				}
				tagFound = true;
			}
			else if ( (groupByTag2 != null) && !groupByTag2.isEmpty() ) {
				// Tag is specified in "group by 2":
				// - LocType is the tag variable and LocId is the tag value.
				tsidBuilder.append(groupByTag2);
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem2);
				if ( groupBy1 != null ) {
					// Append the "group by 1".
					tsidBuilder.append("-");
					tsidBuilder.append(groupItem1.toString());
				}
				tagFound = true;
			}
			/*
			else if ( (groupByTag3 != null) && !groupByTag3.isEmpty() ) {
				tsidBuilder.append(groupByTag3);
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem3);
				tagFound = true;
			}
			*/
			if ( ! tagFound ) {
				// No tag so use the first Group by for the location, typically something terse like region or instance type.
				if ( groupBy1 != null ) {
					tsidBuilder.append(groupBy1.toString());
					tsidBuilder.append(":");
					tsidBuilder.append(groupItem1);
					tagFound = true;
				}
				if ( groupBy2 != null ) {
					// Append the second group by value.
					tsidBuilder.append("-");
					tsidBuilder.append(groupItem2.toString());
				}
			}
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy1) ) {
			if ( groupBy1 != null ) {
				tsidBuilder.append(groupBy1.toString());
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem1);
			}
			else {
				tsidBuilder.append(timeSeriesMissingGroupBy);
			}
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy2) ) {
			if ( groupBy2 != null ) {
				tsidBuilder.append(groupBy2.toString());
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem2);
			}
			else {
				tsidBuilder.append(timeSeriesMissingGroupBy);
			}
		}
		else {
			// Free-form data type:
			// - process the data type from metadata
			for ( int i = 0; i < metadataNames.length; i++ ) {
				if ( metadataValues[i] != null ) {
					timeSeriesLocationID = timeSeriesLocationID.replace(metadataNames[i], metadataValues[i]);
				}
			}
			tsidBuilder.append(timeSeriesLocationID);
		}
	}

	/**
	Return the EBS snapshots table that is read by this class when run in discovery mode.
	@return the EBS snapshots table
	*/
	private DataTable getDiscoveryEbsSnapshotsTable() {
    	return this.discoveryEbsSnapshotsTable;
	}

	/**
	Return the EC2 images table that is read by this class when run in discovery mode.
	@return the EC2 images table
	*/
	private DataTable getDiscoveryEc2ImagesTable() {
    	return this.discoveryEc2ImagesTable;
	}

	/**
	Return the EC2 properties table that is read by this class when run in discovery mode.
	@return the EC2 properties table
	*/
	private DataTable getDiscoveryEc2PropertiesTable() {
    	return this.discoveryEc2PropertiesTable;
	}

	/**
	Return the grouped table that is read by this class when run in discovery mode.
	@return the grouped table
	*/
	private DataTable getDiscoveryGroupedTable() {
    	return this.discoveryGroupedTable;
	}

	/**
	Return the total table that is read by this class when run in discovery mode.
	@return the total table
	*/
	private DataTable getDiscoveryTotalTable() {
    	return this.discoveryTotalTable;
	}

	/**
	Return the list of files that were created by this command.
	@return the list of files that were created by this command
	*/
	public List<File> getGeneratedFileList () {
    	List<File> list = new ArrayList<>();
    	if ( getGroupedTableFile() != null ) {
        	list.add ( getGroupedTableFile() );
    	}
    	if ( getTotalTableFile() != null ) {
        	list.add ( getTotalTableFile() );
    	}
    	return list;
	}

	/**
	Return the grouped table output file generated by this file.  This method is used internally.
	@return the grouped table output file generated by this file
	*/
	private File getGroupedTableFile () {
    	return __GroupedTableFile_File;
	}

	/**
	Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
	The following classes can be requested:  DataTable
	@return a list of objects of the requested type
	*/
	@SuppressWarnings("unchecked")
	public <T> List<T> getObjectList ( Class<T> c ) {
    	List<T> v = null;
		DataTable table = getDiscoveryGroupedTable();
    	if ( (table != null) && (c == table.getClass()) ) {
        	v = new ArrayList<>();
        	v.add ( (T)table );
    	}
		table = getDiscoveryTotalTable();
    	if ( (table != null) && (c == table.getClass()) ) {
    		if ( v == null ) {
    			v = new ArrayList<>();
    		}
        	v.add ( (T)table );
    	}
		table = getDiscoveryEc2PropertiesTable();
    	if ( (table != null) && (c == table.getClass()) ) {
    		if ( v == null ) {
    			v = new ArrayList<>();
    		}
        	v.add ( (T)table );
    	}
		table = getDiscoveryEbsSnapshotsTable();
    	if ( (table != null) && (c == table.getClass()) ) {
    		if ( v == null ) {
    			v = new ArrayList<>();
    		}
        	v.add ( (T)table );
    	}
		table = getDiscoveryEc2ImagesTable();
    	if ( (table != null) && (c == table.getClass()) ) {
    		if ( v == null ) {
    			v = new ArrayList<>();
    		}
        	v.add ( (T)table );
    	}
    	return v;
	}

	/**
	 * Get the service name for Group.keys().
	 * @param keys keys from Group.keys() call
	 * @return the service name corresponding to the group
	 */
	/* Not needed?
    private static String getServiceNameFromKeys ( List<String> keys ) {
        // Logic to extract the service name from the keys.
        // Adjust this based on your actual data structure and naming conventions.
        for ( String key : keys ) {
            if ( key.startsWith("service:") ) {
                return key.substring("service:".length());
            }
        }
        return "Unknown"; // Default value if service name is not found in keys.
    }
    */

	/**
	Return the total table output file generated by this file.  This method is used internally.
	@return the total table output file generated by this file
	*/
	private File getTotalTableFile () {
    	return __TotalTableFile_File;
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

    	// Clear the output files.
    	setGroupedTableFile ( null );
    	setTotalTableFile ( null );

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
		String region = parameters.getValue ( "Region" );
		region = TSCommandProcessorUtil.expandParameterValue(processor,this,region);
		if ( (region == null) || region.isEmpty() ) {
			// Get the default region.
			region = AwsToolkit.getInstance().getDefaultRegionForCostExplorer();
			Message.printStatus(2, routine, "Region was not specified.  Using default region for Cost Explorer: " + region);
		}

    	// Cost Explorer Query.

		String InputStart = parameters.getValue("InputStart");
		if ( (InputStart == null) || InputStart.isEmpty() ) {
			// Global input start.
			InputStart = "${InputStart}";
		}
    	String InputEnd = parameters.getValue("InputEnd");
		if ( (InputEnd == null) || InputEnd.isEmpty() ) {
			// Global input end.
			InputEnd = "${InputEnd}";
		}

		String TimeChunk = parameters.getValue ( "TimeChunk" );
		TimeInterval TimeChunk_interval = null; // Default.
		if ( (TimeChunk != null) && !TimeChunk.isEmpty() ) {
			TimeChunk_interval = TimeInterval.parseInterval(TimeChunk);
		}
		String Granularity = parameters.getValue ( "Granularity" );
		if ( (Granularity == null) || Granularity.isEmpty() ) {
			Granularity = "" + AwsBillingGranularityType.MONTHLY; // Default.
		}
		Granularity = TSCommandProcessorUtil.expandParameterValue(processor,this,Granularity);
		AwsBillingGranularityType granularity = AwsBillingGranularityType.valueOfIgnoreCase(Granularity);

    	String GroupBy1 = parameters.getValue ( "GroupBy1" );
    	// OK to not have any grouping, in order to return raw data (will always be grouped by granularity).
		//if ( (GroupBy1 == null) || GroupBy1.isEmpty() ) {
		//	GroupBy1 = "" + AwsBillingDimensionType.SERVICE; // Default.
		//}
		GroupBy1 = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupBy1);
		AwsBillingDimensionType groupBy1 = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy1);
    	String GroupByTag1 = parameters.getValue ( "GroupByTag1" );

    	String GroupBy2 = parameters.getValue ( "GroupBy2" );
		GroupBy2 = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupBy2);
		AwsBillingDimensionType groupBy2 = null;
		if ( (GroupBy2 != null) && !GroupBy2.isEmpty() ) {
			groupBy2 = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy2);
		}
    	String GroupByTag2 = parameters.getValue ( "GroupByTag2" );

    	String Metric = parameters.getValue ( "Metric" );
		if ( (Metric == null) || Metric.isEmpty() ) {
			Metric = "" + AwsBillingMetricType.UNBLENDED_COSTS; // Default.
		}
		Metric = TSCommandProcessorUtil.expandParameterValue(processor,this,Metric);
		AwsBillingMetricType metric = AwsBillingMetricType.valueOfIgnoreCase(Metric);

    	// Cost Explorer Filter.

    	String FilterAvailabilityZones = parameters.getValue ( "FilterAvailabilityZones" );
		FilterAvailabilityZones = TSCommandProcessorUtil.expandParameterValue(processor,this,FilterAvailabilityZones);
		List<String> filterAvailabilityZones = new ArrayList<>();
		if ( (FilterAvailabilityZones != null) && !FilterAvailabilityZones.isEmpty() ) {
			if ( !FilterAvailabilityZones.contains(",") ) {
				filterAvailabilityZones.add(FilterAvailabilityZones.trim());
			}
			else {
				String [] parts = FilterAvailabilityZones.split(",");
				for ( String part : parts ) {
					filterAvailabilityZones.add(part.trim());
				}
			}
		}

    	String FilterInstanceTypes = parameters.getValue ( "FilterInstanceTypes" );
		FilterInstanceTypes = TSCommandProcessorUtil.expandParameterValue(processor,this,FilterInstanceTypes);
		List<String> filterInstanceTypes = new ArrayList<>();
		if ( (FilterInstanceTypes != null) && !FilterInstanceTypes.isEmpty() ) {
			if ( !FilterInstanceTypes.contains(",") ) {
				filterInstanceTypes.add(FilterInstanceTypes.trim());
			}
			else {
				String [] parts = FilterInstanceTypes.split(",");
				for ( String part : parts ) {
					filterInstanceTypes.add(part.trim());
				}
			}
		}

    	String FilterRegions = parameters.getValue ( "FilterRegions" );
		FilterRegions = TSCommandProcessorUtil.expandParameterValue(processor,this,FilterRegions);
		List<String> filterRegions = new ArrayList<>();
		if ( (FilterRegions != null) && !FilterRegions.isEmpty() ) {
			if ( !FilterRegions.contains(",") ) {
				filterRegions.add(FilterRegions.trim());
			}
			else {
				String [] parts = FilterRegions.split(",");
				for ( String part : parts ) {
					filterRegions.add(part.trim());
				}
			}
		}

    	String FilterServices = parameters.getValue ( "FilterServices" );
		FilterServices = TSCommandProcessorUtil.expandParameterValue(processor,this,FilterServices);
		List<String> filterServices = new ArrayList<>();
		if ( (FilterServices != null) && !FilterServices.isEmpty() ) {
			if ( !FilterServices.contains(",") ) {
				filterServices.add(FilterServices.trim());
			}
			else {
				String [] parts = FilterServices.split(",");
				for ( String part : parts ) {
					filterServices.add(part.trim());
				}
			}
		}

    	String FilterTags = parameters.getValue ( "FilterTags" );
		FilterTags = TSCommandProcessorUtil.expandParameterValue(processor,this,FilterTags);
		List<String> filterTags = new ArrayList<>();
		if ( (FilterTags != null) && !FilterTags.isEmpty() ) {
			if ( !FilterTags.contains(",") ) {
				filterTags.add(FilterTags.trim());
			}
			else {
				String [] parts = FilterTags.split(",");
				for ( String part : parts ) {
					filterTags.add(part.trim());
				}
			}
		}

		// Whether processing cost data.
		boolean doCosts = false;

    	// Output Cost Tables.
		boolean doGroupedTable = false;
		String GroupedTableID = parameters.getValue ( "GroupedTableID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GroupedTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupedTableID);
		}
		if ( (GroupedTableID != null) && !GroupedTableID.isEmpty() ) {
			doCosts = true;
			doGroupedTable = true;
		}
		// If an output file is to be written:
		// - output using the table, if available
		// - if an output table is not being created, create a temporary table and write it
		boolean doGroupedFile = false;
		String GroupedTableFile = parameters.getValue ( "GroupedTableFile" ); // Expand below.
		if ( (GroupedTableFile != null) && !GroupedTableFile.isEmpty() ) {
			doCosts = true;
			doGroupedFile = true;
		}
		String GroupedTableRowCountProperty = parameters.getValue ( "GroupedTableRowCountProperty" );

		boolean doTotalTable = false;
		String TotalTableID = parameters.getValue ( "TotalTableID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			TotalTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,TotalTableID);
		}
		if ( (TotalTableID != null) && !TotalTableID.isEmpty() ) {
			doCosts = true;
			doTotalTable = true;
		}
		boolean doTotalFile = false;
		String TotalTableFile = parameters.getValue ( "TotalTableFile" ); // Expand below.
		if ( (TotalTableFile != null) && !TotalTableFile.isEmpty() ) {
			doTotalFile = true;
		}
		String TotalTableRowCountProperty = parameters.getValue ( "TotalTableRowCountProperty" );

		// Output Time Series.
		String CreateGroupedTimeSeries = parameters.getValue ( "CreateGroupedTimeSeries" );
		boolean createGroupedTimeSeries = false; // Default.
		if ( (CreateGroupedTimeSeries != null) && CreateGroupedTimeSeries.equalsIgnoreCase(_True) ) {
			doCosts = true;
	    	createGroupedTimeSeries = true;
		}

		String GroupedTimeSeriesLocationID = parameters.getValue ( "GroupedTimeSeriesLocationID" );
		GroupedTimeSeriesLocationID = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupedTimeSeriesLocationID);
		if ( (GroupedTimeSeriesLocationID == null) || GroupedTimeSeriesLocationID.isEmpty() ) {
			GroupedTimeSeriesLocationID = this._Auto;
		}

		String GroupedTimeSeriesDataType = parameters.getValue ( "GroupedTimeSeriesDataType" );
		GroupedTimeSeriesDataType = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupedTimeSeriesDataType);
		if ( (GroupedTimeSeriesDataType == null) || GroupedTimeSeriesDataType.isEmpty() ) {
			GroupedTimeSeriesDataType = this._Auto;
		}

        String GroupedTimeSeriesAlias = parameters.getValue ( "GroupedTimeSeriesAlias" ); // Expanded dynamically below.

    	// Output Service Properties.
		String EC2PropertiesTableID = parameters.getValue ( "EC2PropertiesTableID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			EC2PropertiesTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,EC2PropertiesTableID);
		}
		String EBSSnapshotsTableID = parameters.getValue ( "EBSSnapshotsTableID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			EBSSnapshotsTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,EBSSnapshotsTableID);
		}
		String EC2ImagesTableID = parameters.getValue ( "EC2ImagesTableID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			EC2ImagesTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,EC2ImagesTableID);
		}

		String CreateTotalTimeSeries = parameters.getValue ( "CreateTotalTimeSeries" );
		boolean createTotalTimeSeries = false; // Default.
		if ( (CreateTotalTimeSeries != null) && CreateTotalTimeSeries.equalsIgnoreCase(_True) ) {
			doCosts = true;
	    	createTotalTimeSeries = true;
		}

		String TotalTimeSeriesLocationID = parameters.getValue ( "TotalTimeSeriesLocationID" );
		TotalTimeSeriesLocationID = TSCommandProcessorUtil.expandParameterValue(processor,this,TotalTimeSeriesLocationID);
		if ( (TotalTimeSeriesLocationID == null) || TotalTimeSeriesLocationID.isEmpty() ) {
			TotalTimeSeriesLocationID = this._Total;
		}

		String TotalTimeSeriesDataType = parameters.getValue ( "TotalTimeSeriesDataType" );
		TotalTimeSeriesDataType = TSCommandProcessorUtil.expandParameterValue(processor,this,TotalTimeSeriesDataType);
		if ( (TotalTimeSeriesDataType == null) || TotalTimeSeriesDataType.isEmpty() ) {
			// Instead of "Auto", set the string to parse here so GroupBy "Auto" don't confuse later.
			//TotalTimeSeriesDataType = this._Auto;
			TotalTimeSeriesDataType = "METRIC";
		}

		String TimeSeriesMissingGroupBy = parameters.getValue ( "TimeSeriesMissingGroupBy" );
		TimeSeriesMissingGroupBy = TSCommandProcessorUtil.expandParameterValue(processor,this,TimeSeriesMissingGroupBy);
		if ( (TimeSeriesMissingGroupBy == null) || TimeSeriesMissingGroupBy.isEmpty() ) {
			TimeSeriesMissingGroupBy = _Unknown; // Default.
		}

        String TotalTimeSeriesAlias = parameters.getValue ( "TotalTimeSeriesAlias" ); // Expanded dynamically below.

		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    	IfInputNotFound = _Warn; // Default.
		}
		String AppendOutput = parameters.getValue ( "AppendOutput" );
		boolean appendOutput = false;
		if ( (AppendOutput != null) && AppendOutput.equalsIgnoreCase(_True)) {
			appendOutput = true;
		}

		// Get the period to process:
		// - correct the dates to appropriate precision if necessary
		DateTime InputStart_DateTime = null;
		DateTime InputEnd_DateTime = null;
		if ( commandPhase == CommandPhaseType.RUN ) {
			try {
				InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
					status, warningLevel, commandTag );
				if ( InputStart_DateTime != null ) {
					// Make sure it is day precision.
					if ( InputStart_DateTime.getPrecision() > DateTime.PRECISION_DAY ) {
						// Precision is > day so also set the day to 1.
						InputStart_DateTime.setDay(1);
					}
					else {
						// Precision is <= day so set to day precision and the existing day in the date is fine.
					}
					InputStart_DateTime.setPrecision(DateTime.PRECISION_DAY);
				}
			}
			catch ( InvalidCommandParameterException e ) {
				// Warning will have been added above.
				++warningCount;
			}
			try {
				InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
					status, warningLevel, commandTag );
				if ( InputEnd_DateTime != null ) {
					// Make sure it is day precision.
					if ( InputEnd_DateTime.getPrecision() > DateTime.PRECISION_DAY ) {
						// Precision is > day so also set the day to the number of days in the month.
						InputEnd_DateTime.setDay(TimeUtil.numDaysInMonth(InputEnd_DateTime));
					}
					else {
						// Precision is <= day so set to day precision and the existing day in the date is fine.
					}
					InputStart_DateTime.setPrecision(DateTime.PRECISION_DAY);
				}
			}
			catch ( InvalidCommandParameterException e ) {
				// Warning will have been added above.
				++warningCount;
			}

		  	if ( doCosts ) {
		  		// Make sure that input start and end are specified at runtime, after evaluation global data:
				// - also make sure the precision matches the granularity of output
				if ( InputStart_DateTime == null ) {
				message = "The input start must be specified.";
		  			Message.printWarning(warningLevel,
				 		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		  	 		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				 		message, "Specify the input start for this command or set the global input period start." ) );
				}
				else {
					if ( InputStart_DateTime.getPrecision() != TimeInterval.DAY ) {
				 		message = "The input start must be a date YYYY-MM-DD or other accepted format.";
		  		 		Message.printWarning(warningLevel,
					 		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		  		 		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					 		message, "Specify the input start as a date." ) );
			 		}
				}

				if ( InputEnd_DateTime == null ) {
			 		message = "The input end must be specified.";
		  	 		Message.printWarning(warningLevel,
				 		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		  			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				 		message, "Specify the input end for this command or set the global input period end." ) );
				}
				else {
			 		if ( InputEnd_DateTime.getPrecision() != TimeInterval.DAY ) {
				 		message = "The input end must be a date YYYY-MM-DD or other accepted format.";
		  		 		Message.printWarning(warningLevel,
					 		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		  		 		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					 		message, "Specify the input end as a date." ) );
			 		}
				}
			}
		}

		// Get the table(s) to process:
		// - only if appending
		// - if not appending, (re)create below

		DataTable groupedTable = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (GroupedTableID != null) && !GroupedTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", GroupedTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		groupedTable = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing grouped table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(GroupedTableID=\"" + GroupedTableID + "\") from processor (" + e + ").";
			  		Message.printWarning(warningLevel,
				  		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			  		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				  		message, "Report problem to software support." ) );
				}
		  	}
    	}

		DataTable totalTable = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (TotalTableID != null) && !TotalTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", TotalTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		totalTable = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing total table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(TotalTableID=\"" + TotalTableID + "\") from processor (" + e + ").";
			  		Message.printWarning(warningLevel,
				  		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			  		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				  		message, "Report problem to software support." ) );
				}
		  	}
    	}

		DataTable ec2PropertiesTable = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (EC2PropertiesTableID != null) && !EC2PropertiesTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", EC2PropertiesTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		ec2PropertiesTable = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing EC2 properties table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(EC2PropertiesTableID=\"" + EC2PropertiesTableID + "\") from processor (" + e + ").";
			  		Message.printWarning(warningLevel,
				  		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			  		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				  		message, "Report problem to software support." ) );
				}
		  	}
    	}

		DataTable ebsSnapshotsTable = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (EBSSnapshotsTableID != null) && !EBSSnapshotsTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", EBSSnapshotsTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		ebsSnapshotsTable = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing EBS snapshots table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(EBSSnapshotsTableID=\"" + EBSSnapshotsTableID + "\") from processor (" + e + ").";
			  		Message.printWarning(warningLevel,
				  		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			  		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				  		message, "Report problem to software support." ) );
				}
		  	}
    	}

		DataTable ec2ImagesTable = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (EC2ImagesTableID != null) && !EC2ImagesTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", EC2ImagesTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		ec2ImagesTable = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing EC2 images table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(EC2ImagesTableID=\"" + EC2ImagesTableID + "\") from processor (" + e + ").";
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

		CostExplorerClient costExplorer = CostExplorerClient.builder()
			.region(regionObject)
			.credentialsProvider(credentialsProvider)
			.build();

		try {
	    	if ( commandPhase == CommandPhaseType.RUN ) {

	    		HashMap<String,Integer> groupedTableColMap = new HashMap<>();
	    		List<String> groupedProblems = new ArrayList<>();
	    		groupedTable = createGroupedTable (
	    			processor,
	    			doGroupedTable, GroupedTableID, groupedTable,
	    			doGroupedFile,
	    			appendOutput,
	    			groupedTableColMap,
	    			groupBy1, groupBy2,
	    			groupedProblems
	    		);
	    		for ( String problem: groupedProblems ) {
            		Message.printWarning(warningLevel,
               			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, problem );
            		status.addToLog ( commandPhase,
               			new CommandLogRecord(CommandStatusType.FAILURE,
                			problem, "Report problem to software support." ) );
	    		}

	    		HashMap<String,Integer> totalTableColMap = new HashMap<>();
	    		List<String> totalProblems = new ArrayList<>();
	    		totalTable = createTotalTable (
    				processor,
	    			doTotalTable, TotalTableID, totalTable,
	    			doTotalFile,
	    			appendOutput,
	    			totalTableColMap,
	    			totalProblems
	    		);
	    		for ( String problem: totalProblems ) {
            		Message.printWarning(warningLevel,
               			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, problem );
            		status.addToLog ( commandPhase,
               			new CommandLogRecord(CommandStatusType.FAILURE,
                			problem, "Report problem to software support." ) );
	    		}

	    		// Create an empty EC2 properties table, to be filled later.
	    		if ( (EC2PropertiesTableID != null) && !EC2PropertiesTableID.isEmpty() ) {
	    			if ( ec2PropertiesTable == null ) {
	    				ec2PropertiesTable = new DataTable();
	    				ec2PropertiesTable.setTableID(EC2PropertiesTableID);

	    				// Set the table in the processor:
	    				// - if new will add
	    				// - if append will overwrite by replacing the matching table ID
	    				Message.printStatus(2, routine, "Created new EC2 properties table \"" + EC2PropertiesTableID + "\" for output.");
	    				// Set the table in the processor.
	    				PropList requestParams = new PropList ( "" );
	    				requestParams.setUsingObject ( "Table", ec2PropertiesTable );
	    				try {
	    					processor.processRequest( "SetTable", requestParams);
	    				}
	    				catch ( Exception e ) {
	    					Message.printWarning ( 3, routine, "Error requesting SetTable(Table=...) from processor." );
	    				}
	    			}
	    			else {
	    				// Got an existing table from the processor.
	    			}
	    		}

	    		// Create an empty EBS snapshots table, to be filled later.
	    		if ( (EBSSnapshotsTableID != null) && !EBSSnapshotsTableID.isEmpty() ) {
	    			if ( ebsSnapshotsTable == null ) {
	    				ebsSnapshotsTable = new DataTable();
	    				ebsSnapshotsTable.setTableID(EBSSnapshotsTableID);

	    				// Set the table in the processor:
	    				// - if new will add
	    				// - if append will overwrite by replacing the matching table ID
	    				Message.printStatus(2, routine, "Created new EBS snapshots table \"" + EBSSnapshotsTableID + "\" for output.");
	    				// Set the table in the processor.
	    				PropList requestParams = new PropList ( "" );
	    				requestParams.setUsingObject ( "Table", ebsSnapshotsTable );
	    				try {
	    					processor.processRequest( "SetTable", requestParams);
	    				}
	    				catch ( Exception e ) {
	    					Message.printWarning ( 3, routine, "Error requesting SetTable(Table=...) from processor." );
	    				}
	    			}
	    			else {
	    				// Got an existing table from the processor.
	    			}
	    		}

	    		// Create an empty EC2 images table, to be filled later.
	    		if ( (EC2ImagesTableID != null) && !EC2ImagesTableID.isEmpty() ) {
	    			if ( ec2ImagesTable == null ) {
	    				ec2ImagesTable = new DataTable();
	    				ec2ImagesTable.setTableID(EC2ImagesTableID);

	    				// Set the table in the processor:
	    				// - if new will add
	    				// - if append will overwrite by replacing the matching table ID
	    				Message.printStatus(2, routine, "Created new EC2 images table \"" + EC2ImagesTableID + "\" for output.");
	    				// Set the table in the processor.
	    				PropList requestParams = new PropList ( "" );
	    				requestParams.setUsingObject ( "Table", ec2ImagesTable );
	    				try {
	    					processor.processRequest( "SetTable", requestParams);
	    				}
	    				catch ( Exception e ) {
	    					Message.printWarning ( 3, routine, "Error requesting SetTable(Table=...) from processor." );
	    				}
	    			}
	    			else {
	    				// Got an existing table from the processor.
	    			}
	    		}

    	    	// Call the service that was requested to create the requested output.
   	    		// CostExplorerClient:
   	    		//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/CostExplorerClient.html

	    		// Time series to be created.
	    		List<TS> groupedTslist = new ArrayList<>();
	    		List<TS> totalTslist = new ArrayList<>();
	    		if ( doCosts ) {
	    			warningCount = doCostExplorer (
	    				processor,
	    				costExplorer,
	    				groupedTable, totalTable,
	    				GroupedTableRowCountProperty, TotalTableRowCountProperty,
	    				groupedTableColMap, totalTableColMap,
	    				InputStart_DateTime, InputEnd_DateTime, TimeChunk_interval,
	    				granularity,
	    				groupBy1, GroupByTag1,
	    				groupBy2, GroupByTag2,
	    				metric,
	    				filterAvailabilityZones, filterInstanceTypes, filterRegions, filterServices, filterTags,
	    				createGroupedTimeSeries, GroupedTimeSeriesLocationID, GroupedTimeSeriesDataType, groupedTslist,
	    				createTotalTimeSeries, TotalTimeSeriesLocationID, TotalTimeSeriesDataType, totalTslist,
	    				TimeSeriesMissingGroupBy,
	    				status, logLevel, warningCount, commandTag );

	    			// Create the output file:
	    			// - write the table to a delimited file
	    			// - TODO smalers 2023-01-28 for now do not write comments, keep very basic

	    			if ( doGroupedFile ) {
	    				String GroupedTableFile_full = IOUtil.verifyPathForOS(
	    					IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    						TSCommandProcessorUtil.expandParameterValue(processor,this,GroupedTableFile)));
	    				if ( GroupedTableFile_full.toUpperCase().endsWith("CSV") ) {
	    					boolean writeColumnNames = true;
	    					List<String> comments = null;
	    					String commentLinePrefix = "#";
	    					HashMap<String,Object> writeProps = new HashMap<>();
	    					if ( appendOutput && ((GroupedTableID == null) || GroupedTableID.isEmpty()) ) {
	    						// Requested append but the output table was not given:
	    						// - therefore the output table was a temporary table
	    						// - the output is only for this command so must append to the file (if it exists)
	    						writeProps.put("Append", "True");
	    					}
	    					groupedTable.writeDelimitedFile(GroupedTableFile_full, ",", writeColumnNames, comments, commentLinePrefix, writeProps);
	    					setGroupedTableFile(new File(GroupedTableFile_full));
	    				}
	    				// TODO smalers 2023-01-31 need to implement.
	    				//else if ( OutputFile_full.toUpperCase().endsWith("JSON") ) {
	    				//}
	    				else {
	    					message = "Requested grouped output file has unknown extension - don't know how to write.";
	    					Message.printWarning(warningLevel,
	    						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	    					status.addToLog ( commandPhase,
	    						new CommandLogRecord(CommandStatusType.FAILURE,
	    							message, "Use a grouped output file with 'csv' file extension." ) );
	    				}
	    			}

	    			if ( createGroupedTimeSeries && (groupedTslist.size() > 0) ) {
	    				// Add the grouped time series to the processor:
	    				// - also set the alias here since it depends on command status, etc.
	    				if ( commandPhase == CommandPhaseType.RUN ) {
	    					if ( (GroupedTimeSeriesAlias != null) && !GroupedTimeSeriesAlias.isEmpty() ) {
	    						// Expand the alias before setting.
	    						for ( TS ts : groupedTslist ) {
	    							ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString (
    									processor, ts, GroupedTimeSeriesAlias, status, commandPhase) );
	    						}
	    					}
	    				}

	    				int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, groupedTslist );
	    				if ( wc2 > 0 ) {
               	   			message = "Error adding AWS Cost Explorer grouped time series after read.";
               	   			Message.printWarning ( warningLevel,
               	   				MessageUtil.formatMessageTag(commandTag,
               	   					++warningCount), routine, message );
                   	   		status.addToLog ( commandPhase,
                   	   			new CommandLogRecord(CommandStatusType.FAILURE,
                          	   	message, "Report the problem to software support." ) );
                   	   		throw new CommandException ( message );
	    				}
	    			}

	    			if ( doTotalFile ) {
	    				String TotalTableFile_full = IOUtil.verifyPathForOS(
	    					IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    						TSCommandProcessorUtil.expandParameterValue(processor,this,TotalTableFile)));
	    				if ( TotalTableFile_full.toUpperCase().endsWith("CSV") ) {
	    					boolean writeColumnNames = true;
	    					List<String> comments = null;
	    					String commentLinePrefix = "#";
	    					HashMap<String,Object> writeProps = new HashMap<>();
	    					if ( appendOutput && ((TotalTableID == null) || TotalTableID.isEmpty()) ) {
	    						// Requested append but the output table was not given:
	    						// - therefore the output table was a temporary table
	    						// - the output is only for this command so must append to the file (if it exists)
	    						writeProps.put("Append", "True");
	    					}
	    					groupedTable.writeDelimitedFile(TotalTableFile_full, ",", writeColumnNames, comments, commentLinePrefix, writeProps);
	    					setTotalTableFile(new File(TotalTableFile_full));
	    				}
	    				// TODO smalers 2023-01-31 need to implement.
	    				//else if ( OutputFile_full.toUpperCase().endsWith("JSON") ) {
	    				//}
	    				else {
	    					message = "Requested total output file has unknown extension - don't know how to write.";
	    					Message.printWarning(warningLevel,
	    						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	    					status.addToLog ( commandPhase,
	    						new CommandLogRecord(CommandStatusType.FAILURE,
	    							message, "Use a grouped output file with 'csv' file extension." ) );
	    				}
	    			}

	    			if ( createTotalTimeSeries && (totalTslist.size() > 0) ) {
	    				// Add the total time series to the processor:
	    				// - also set the alias here since it depends on command status, etc.
   	   					if ( commandPhase == CommandPhaseType.RUN ) {
   	   						if ( (TotalTimeSeriesAlias != null) && !TotalTimeSeriesAlias.isEmpty() ) {
   	   							// Expand the alias before setting.
   	   							for ( TS ts : totalTslist ) {
   	   								ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString (
   	   									processor, ts, TotalTimeSeriesAlias, status, commandPhase) );
   	   							}
   	   						}
   	   					}

   	   					int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, totalTslist );
   	   					if ( wc2 > 0 ) {
   	   						message = "Error adding AWS Cost Explorer total time series after read.";
   	   						Message.printWarning ( warningLevel,
   	   							MessageUtil.formatMessageTag(commandTag,
   	   								++warningCount), routine, message );
                   	   		status.addToLog ( commandPhase,
                   	   			new CommandLogRecord(CommandStatusType.FAILURE,
                   	   				message, "Report the problem to software support." ) );
                   	   		throw new CommandException ( message );
   	   					}
   			   		}
	    		}

	    	   	// Get the EC2 service properties.
	    	   	if ( (EC2PropertiesTableID != null) && !EC2PropertiesTableID.isEmpty() ) {
	    	   		warningCount = doEc2Properties (
    	    			processor,
    	    			ec2PropertiesTable,
    	    			status, logLevel, warningCount, commandTag );

	    	   	}

	    	   	// Get the EBS snapshots.
	    	   	if ( (EBSSnapshotsTableID != null) && !EBSSnapshotsTableID.isEmpty() ) {
	    	   		warningCount = doEbsSnapshots (
    	    			processor,
    	    			ebsSnapshotsTable,
    	    			status, logLevel, warningCount, commandTag );

	    	   	}

	    	   	// Get the EC2 images.
	    	   	if ( (EC2ImagesTableID != null) && !EC2ImagesTableID.isEmpty() ) {
	    	   		warningCount = doEc2Images (
    	    			processor,
    	    			ec2ImagesTable,
    	    			status, logLevel, warningCount, commandTag );

	    	   	}
	    	}
	    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	       		if ( (GroupedTableID != null) && !GroupedTableID.isEmpty() ) {
	          		// Did not find table so is being created in this command.
	           		// Create an empty table and set the ID.
	           		groupedTable = new DataTable();
	           		groupedTable.setTableID ( GroupedTableID );
	           		setDiscoveryGroupedTable ( groupedTable );
	       		}

   	       		if ( (TotalTableID != null) && !TotalTableID.isEmpty() ) {
	          		// Did not find table so is being created in this command.
	           		// Create an empty table and set the ID.
	           		totalTable = new DataTable();
	           		totalTable.setTableID ( TotalTableID );
	           		setDiscoveryTotalTable ( totalTable );
	       		}

   	       		if ( (EC2PropertiesTableID != null) && !EC2PropertiesTableID.isEmpty() ) {
	          		// Did not find table so is being created in this command.
	           		// Create an empty table and set the ID.
	           		ec2PropertiesTable = new DataTable();
	           		ec2PropertiesTable.setTableID ( EC2PropertiesTableID );
	           		setDiscoveryEc2PropertiesTable ( ec2PropertiesTable );
	       		}

   	       		if ( (EBSSnapshotsTableID != null) && !EBSSnapshotsTableID.isEmpty() ) {
	          		// Did not find table so is being created in this command.
	           		// Create an empty table and set the ID.
	           		ebsSnapshotsTable = new DataTable();
	           		ebsSnapshotsTable.setTableID ( EBSSnapshotsTableID );
	           		setDiscoveryEbsSnapshotsTable ( ebsSnapshotsTable );
	       		}

   	       		if ( (EC2ImagesTableID != null) && !EC2ImagesTableID.isEmpty() ) {
	          		// Did not find table so is being created in this command.
	           		// Create an empty table and set the ID.
	           		ec2ImagesTable = new DataTable();
	           		ec2ImagesTable.setTableID ( EC2ImagesTableID );
	           		setDiscoveryEc2ImagesTable ( ec2ImagesTable );
	       		}
	    	}

		}
    	catch ( CostExplorerException e ) {
			message = "Unexpected error for AWS Cost Explorer (" + e.awsErrorDetails().errorMessage() + ").";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
    	}
    	catch ( Exception e ) {
			message = "Unexpected error for AWS Cost Explorer command (" + e + ").";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
		}
		finally {
			// Close the cost explorer.
			if ( costExplorer != null ) {
				costExplorer.close();
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
	Set the EBS snapshots table that is read by this class in discovery mode.
	@param table EBS snapshots data table with output
	*/
	private void setDiscoveryEbsSnapshotsTable ( DataTable table ) {
    	this.discoveryEbsSnapshotsTable = table;
	}

	/**
	Set the EC2 images table that is read by this class in discovery mode.
	@param table EC2 images table with output
	*/
	private void setDiscoveryEc2ImagesTable ( DataTable table ) {
    	this.discoveryEc2ImagesTable = table;
	}

	/**
	Set the EC2 properties table that is read by this class in discovery mode.
	@param table EC2 properties table with output
	*/
	private void setDiscoveryEc2PropertiesTable ( DataTable table ) {
    	this.discoveryEc2PropertiesTable = table;
	}

	/**
	Set the grouped table that is read by this class in discovery mode.
	@param table grouped data table with output
	*/
	private void setDiscoveryGroupedTable ( DataTable table ) {
    	this.discoveryGroupedTable = table;
	}

	/**
	Set the total table that is read by this class in discovery mode.
	@param table total data table with output
	*/
	private void setDiscoveryTotalTable ( DataTable table ) {
    	this.discoveryTotalTable = table;
	}

	/**
	Set the grouped table file that is created by this command.  This is only used internally.
	@param file the grouped table file
	*/
	private void setGroupedTableFile ( File file ) {
    	this.__GroupedTableFile_File = file;
	}

	/**
	Set the total table file that is created by this command.  This is only used internally.
	@param file the total table file
	*/
	private void setTotalTableFile ( File file ) {
    	this.__TotalTableFile_File = file;
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
			// Cost Explorer Query.
			"InputStart",
			"InputEnd",
			"TimeChunk",
			"Granularity",
			"GroupBy1",
			"GroupByTag1",
			"GroupBy2",
			"GroupByTag2",
			//"GroupBy3",
			//"GroupByTag3",
			"Metric",
			// Cost Explorer Filter.
			"FilterAvailabilityZones",
			"FilterInstanceTypes",
			"FilterRegions",
			"FilterServices",
			"FilterTags",
			// Output Cost Tables.
			"GroupedTableID",
			"GroupedTableFile",
			"GroupedTableRowCountProperty",
			"TotalTableID",
			"TotalTableFile",
			"TotalTableRowCountProperty",
			"AppendOutput",
			// Output Time Series.
			"CreateGroupedTimeSeries",
			"GroupedTimeSeriesLocationID",
			"GroupedTimeSeriesDataType",
			"GroupedTimeSeriesAlias",
			"TimeSeriesMissingGroupBy",
			"CreateTotalTimeSeries",
			"TotalTimeSeriesLocationID",
			"TotalTimeSeriesDataType",
			"TotalTimeSeriesAlias",
			// Output Service Properties.
			"EC2PropertiesTableID",
			"EBSSnapshotsTableID",
			"EC2ImagesTableID"
		};
		return this.toString(parameters, parameterOrder);
	}

}