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
import software.amazon.awssdk.services.pricing.model.Filter;
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
	protected final String _Ignore = "Ignore";
	protected final String _Warn = "Warn";
	protected final String _Fail = "Fail";

	/**
	 * Data members used for TimeSeriesLocationID parameter values.
	 */
	protected final String _Auto = "Auto";
	protected final String _GroupBy1 = "GroupBy1";
	protected final String _GroupBy2 = "GroupBy2";
	//protected final String _GroupBy3 = "GroupBy3";

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
	public AwsBilling_Command () {
		super();
		setCommandName ( "AwsBilling" );
	}

	/**
	 * Add a record of data to time series.
	 * This may result in creating a new time series or adding a record to an existing time series.
	 * @param tslist the list of existing time series (will be modified as needed)
	 * @param inputStart the starting date for the data
	 * @param inputEnd the ending date for the data
	 */
	private void addTimeSeriesData ( List<TS> tslist, DateTime inputStart, DateTime inputEnd,
		DateTime costDate, AwsBillingGranularityType granularity,
    	AwsBillingDimensionType groupBy1, String groupByTag1, String groupItem1,
    	AwsBillingDimensionType groupBy2, String groupByTag2, String groupItem2,
    	//AwsBillingDimensionType groupBy3, String groupByTag3, String groupItem3,
    	String metricName, Double costAmount, String costUnit,
    	String timeSeriesLocationID
	) throws Exception {
		// Create a time series identifier string using the input.
		if ( groupItem1.isEmpty() ) {
			groupItem1 = "Unknown";
		}
		if ( groupItem2.isEmpty() ) {
			groupItem2 = "Unknown";
		}
		/*
		if ( groupItem3.isEmpty() ) {
			groupItem3 = "Unknown";
		}
		*/
		StringBuilder tsidBuilder = new StringBuilder();
		// Add the location:
		// - tag is preferred if tag is specified as any Group By
		// - if no tag, use the first Group by
		boolean tagFound = false;
		if ( timeSeriesLocationID.equals(this._Auto) ) {
			// Default behavior for location identifier.
			if ( (groupByTag1 != null) && !groupByTag1.isEmpty() ) {
				tsidBuilder.append(groupByTag1);
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem1);
				tagFound = true;
			}
			else if ( (groupByTag2 != null) && !groupByTag2.isEmpty() ) {
				tsidBuilder.append(groupByTag2);
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem2);
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
			}
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy1) ) {
			if ( groupBy1 != null ) {
				tsidBuilder.append(groupBy1.toString());
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem1);
			}
			else {
				tsidBuilder.append("Unknown");
			}
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy2) ) {
			if ( groupBy2 != null ) {
				tsidBuilder.append(groupBy2.toString());
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem2);
			}
			else {
				tsidBuilder.append("Unknown");
			}
		}
		/*
		else if ( timeSeriesLocationID.equals(this._GroupBy3) ) {
			if ( groupBy3 != null ) {
				tsidBuilder.append(groupBy3.toString());
				tsidBuilder.append(":");
				tsidBuilder.append(groupItem3);
			}
			else {
				tsidBuilder.append("Unknown");
			}
		}
		*/
		// Add the data source.
		tsidBuilder.append(".");
		tsidBuilder.append("AWS");
		// Add the data type.
		tsidBuilder.append(".");
		if ( timeSeriesLocationID.equals(this._Auto) ) {
			tsidBuilder.append(groupItem1);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy1) ) {
			// Use the other group by.
			tsidBuilder.append(groupItem2);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
		else if ( timeSeriesLocationID.equals(this._GroupBy2) ) {
			// Use the other group by.
			tsidBuilder.append(groupItem1);
			tsidBuilder.append("-");
			tsidBuilder.append(metricName);
		}
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

		// Set the data value for the single cost explorer data value.
		ts.setDataValue ( costDate, costAmount );

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
    	String Granularity = parameters.getValue ( "Granularity" );
    	String GroupBy1 = parameters.getValue ( "GroupBy1" );
    	String GroupByTag1 = parameters.getValue ( "GroupByTag1" );
    	String GroupBy2 = parameters.getValue ( "GroupBy2" );
    	String GroupByTag2 = parameters.getValue ( "GroupByTag2" );
    	//String GroupBy3 = parameters.getValue ( "GroupBy3" );
    	//String GroupByTag3 = parameters.getValue ( "GroupByTag3" );
    	String Metric = parameters.getValue ( "Metric" );
    	// Output.
    	//String OutputTableID = parameters.getValue ( "OutputTableID" );
    	//String OutputFile = parameters.getValue ( "OutputFile" );
    	String AppendOutput = parameters.getValue ( "AppendOutput" );
    	// Time series.
    	String CreateTimeSeries = parameters.getValue ( "CreateTimeSeries" );
    	String TimeSeriesLocationID = parameters.getValue ( "TimeSeriesLocationID" );

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

		if ( (InputStart == null) || InputStart.isEmpty() ) {
			message = "The input start is required.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input start as YYYY-MM-DD or a ${Property}."));
		}

		if ( (InputEnd == null) || InputEnd.isEmpty() ) {
			message = "The input end is required.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input end as YYYY-MM-DD or a ${Property}."));
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

		/*
		if ( (GroupBy3 != null) && !GroupBy3.isEmpty() ) {
			AwsBillingDimensionType dimension = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy3);
			if ( dimension == null ) {
				message = "The GroupBy3 (" + GroupBy3 + ") value is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as one of: " + AwsBillingDimensionType.getChoicesAsCsv()));
			}

			if ( GroupBy3.equalsIgnoreCase("" + AwsBillingDimensionType.TAG) && ((GroupByTag3 == null) || GroupByTag3.isEmpty()) ) {
				message = "The GroupByTag3 must be specified if GroupBy3=Tag is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the GroupByTag3."));
			}
		}

		if ( (GroupBy3 == null) || GroupBy3.isEmpty() ) {
			if ( (GroupByTag3 != null) && !GroupByTag3.isEmpty() ) {
				message = "The GroupByTag3 should only be specified if GroupBy3 is spectived.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify GroupBy3 or clear the GroupByTag3."));
			}
		}
		*/

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

		// Time series.
		if ( (CreateTimeSeries != null) && !CreateTimeSeries.equals("") ) {
			if ( !CreateTimeSeries.equalsIgnoreCase(_False) && !CreateTimeSeries.equalsIgnoreCase(_True) ) {
				message = "The CreateTimeSeries parameter \"" + CreateTimeSeries + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
			}
		}

		if ( (TimeSeriesLocationID != null) && !TimeSeriesLocationID.equals("") ) {
			if ( !TimeSeriesLocationID.equalsIgnoreCase(_Auto) && !TimeSeriesLocationID.equalsIgnoreCase(_GroupBy1) &&
				!TimeSeriesLocationID.equalsIgnoreCase(_GroupBy2) ) { //&& !TimeSeriesLocationID.equalsIgnoreCase(_GroupBy3) ) {
				message = "The TimeSeriesLocationID parameter \"" + TimeSeriesLocationID + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Auto + " (default), " + _GroupBy1 + ", or " + _GroupBy2 + "." )); //", or " + _GroupBy3 + "."));
			}
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

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(23);
		// General.
		validList.add ( "Profile" );
		validList.add ( "Region" );
		// Cost Explorer Query.
		validList.add ( "InputStart" );
		validList.add ( "InputEnd" );
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
		// Output.
		validList.add ( "OutputTableID" );
		validList.add ( "OutputFile" );
		validList.add ( "AppendOutput" );
		validList.add ( "OutputTableRowCountProperty" );
		// Time series.
		validList.add ( "CreateTimeSeries" );
		validList.add ( "TimeSeriesLocationID" );
		validList.add ( "Alias" );
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
	 * Get data from the cost explorer.
	 */
	private int doCostExplorer (
		CommandProcessor processor,
		CostExplorerClient costExplorer,
		DataTable table, String outputTableRowCountProperty,
		int dateCol, int granularityCol,
		int groupBy1Col, int groupByTag1Col, int groupItem1Col,
		int groupBy2Col, int groupByTag2Col, int groupItem2Col,
		//int groupBy3Col, int groupByTag3Col, int groupItem3Col,
		int metricCol, int amountCol, int unitsCol,
		DateTime inputStart, DateTime inputEnd,
		AwsBillingGranularityType granularity,
		AwsBillingDimensionType groupBy1, String groupByTag1,
		AwsBillingDimensionType groupBy2, String groupByTag2,
		//AwsBillingDimensionType groupBy3, String groupByTag3,
		AwsBillingMetricType metric,
		List<String> filterAvailabilityZones,
		List<String> filterInstanceTypes,
		List<String> filterRegions,
		List<String> filterServices,
		List<String> filterTags,
		boolean createTimeSeries, String timeSeriesLocationId, List<TS> tslist,
		CommandStatus status, int logLevel, int warningCount, String commandTag ) throws Exception {
		String routine = getClass().getSimpleName() + ".doCostExplorer";
		String message;

   		// Create a request builder that handles the input parameters.
    	GetCostAndUsageRequest.Builder costAndUsageRequestBuilder = GetCostAndUsageRequest
    		.builder()
    		// Add filters:
    		// - see:  https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/model/GetCostAndUsageRequest.Builder.html
    		// The dimension for output (Service
    		// Output is by month:
    		// - each ResultByTime instance below will correspond to the time block.
    		.granularity(granularity.getGranularity())
    		// The metric is the "Aggregate costs by" in the Cost Explorer web site:
    		// - pass the string value from the SDK metric
    		.metrics(metric.getMetric().toString())
    		// Period for the analysis (typically want to be full months or years for invoicing).
    		.timePeriod(DateInterval.builder()
    			.start(inputStart.toString())
    			.end(inputEnd.toString())
    			.build());

   		// Add additional query parameters.
   		List<GroupDefinition> groupDefinitions = new ArrayList<>();
   		if ( groupBy1 != null ) {
   			if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   				// The group definition is defined by a tag.
   				groupDefinitions.add(
   					GroupDefinition.builder()
   						.key(groupByTag1)
   						.type(GroupDefinitionType.TAG)
   						.build()
   				);
   			}
   			else {
   				// The group definition is defined by the dimension.
   				groupDefinitions.add(
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
   				groupDefinitions.add(
   					GroupDefinition.builder()
   						.key(groupByTag2)
   						.type(GroupDefinitionType.TAG)
   						.build()
   				);
   			}
   			else {
   				// The group definition is defined by the dimension.
   				groupDefinitions.add(
   					GroupDefinition.builder()
   						.key("" + groupBy2.getDimension())
   						.type(GroupDefinitionType.DIMENSION)
   						.build()
   				);
   			}
   		}
   		/*
   		if ( groupBy3 != null ) {
   			if ( groupBy3 == AwsBillingDimensionType.TAG ) {
   				// The group definition is defined by a tag.
   				groupDefinitions.add(
   					GroupDefinition.builder()
   						.key(groupByTag3)
   						.type(GroupDefinitionType.TAG)
   						.build()
   				);
   			}
   			else {
   				// The group definition is defined by the dimension.
   				groupDefinitions.add(
   					GroupDefinition.builder()
   						.key("" + groupBy3.getDimension())
   						.type(GroupDefinitionType.DIMENSION)
   						.build()
   				);
   			}
   		}
   		*/
   		if ( !groupDefinitions.isEmpty() ) {
  			costAndUsageRequestBuilder = costAndUsageRequestBuilder.groupBy(groupDefinitions);
   		}

   		// Add filters using an Expression:
   		// - use OR within a list of filters
   		// - use AND for the groups
   		List<Filter> filters = new ArrayList<>();
   		int filterGroupCount = 0;
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
   				.values(filterRegions)
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

   		// Build the request.
   		GetCostAndUsageRequest request = costAndUsageRequestBuilder.build();

    	// Get the response.
    	GetCostAndUsageResponse response = costExplorer.getCostAndUsage(request);

    	// Output to the log file:
    	// - convert to debug later if it is too much
    	for ( ResultByTime resultByTime : response.resultsByTime() ) {
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
   					Message.printStatus(2, routine, "Metric \"" + totalMetricName + "\" amount = " + totalCostAmount + " " + totalCostUnit );
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
    					Message.printStatus(2, routine, "Metric \"" + groupMetricName + "\" amount = " + groupCostAmount + " " + groupCostUnit );
    				}
    			}
    		}
    		else {
    			Message.printStatus(2, routine, "No group costs are available.");
    		}
    	}

    	TableRecord rec = null;
    	boolean allowDuplicates = false;

		boolean doTable = false;
		if ( table != null ) {
			doTable = true;
		}

		// Process the output:
		// - process the table and time series together
   		if ( doTable || createTimeSeries ) {
   			if ( doTable ) {
   				Message.printStatus(2, routine, "Transferring Cost Explorer results to table.");
   			}
   			if ( createTimeSeries ) {
   				Message.printStatus(2, routine, "Transferring Cost Explorer results to time series.");
   			}
   			for ( ResultByTime resultByTime : response.resultsByTime() ) {
   				Message.printStatus(2, routine, "Time period: " + resultByTime.timePeriod());
   				Message.printStatus(2, routine, "  Estimated?: " + resultByTime.estimated());
   				Message.printStatus(2, routine, "  Has group metrics?: " + resultByTime.hasGroups());
   				Message.printStatus(2, routine, "  Has total metrics?: " + resultByTime.hasTotal());
   				// Returns a string for the start of the period:
   				// - is it always a day YYYY-MM-DD?
   				String startDate = resultByTime.timePeriod().start();
   				DateTime costDate = DateTime.parse(startDate);
   				if ( granularity == AwsBillingGranularityType.MONTHLY ) {
   					costDate.setPrecision(DateTime.PRECISION_MONTH);
   				}
   				else if ( granularity == AwsBillingGranularityType.DAILY ) {
   					costDate.setPrecision(DateTime.PRECISION_DAY);
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
    				// - for example the list of services
    				for ( Group group : resultByTime.groups() ) {
    					Message.printStatus(2, routine, "  Group keys: " + group.keys());
    					//Message.printStatus(2, routine, "  Group service: " + getServiceNameFromKeys(group.keys()));
    					String itemName = "";
    					for ( String key : group.keys() ) {
    						// This is not clear but the list always seems to have one key, which is the service.
    						itemName = key;
    						Message.printStatus(2, routine, "    Group has key: " + key);
    					}
    					Map<String,MetricValue> groupMetricsMap = group.metrics();
    					for ( Map.Entry<String,MetricValue> entry : groupMetricsMap.entrySet() ) {
    						String groupMetricName = entry.getKey();
    						String groupCostAmount = entry.getValue().amount();
    						String groupCostUnit = entry.getValue().unit();
    						Message.printStatus(2, routine, "    Metric \"" + groupMetricName + "\" amount = " + groupCostAmount + " " + groupCostUnit );
    						String groupItem1 = cleanGroupKey(group.keys().get(0));
    						String groupItem2 = "";
    						String groupItem3 = "";
   							if ( groupBy2Col > 0 ) {
   								groupItem2 = cleanGroupKey(group.keys().get(1));
   							}
   							/*
   							if ( groupBy3Col > 0 ) {
   								groupItem3 = cleanGroupKey(group.keys().get(2));
   							}
   							*/
    						if ( doTable ) {
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
    								rec = table.addRecord(table.emptyRecord());
    							}
    							// Set the data in the record.
    							rec.setFieldValue(dateCol, costDate);
    							rec.setFieldValue(granularityCol, granularity.toString());
    							rec.setFieldValue(groupBy1Col, groupBy1.toString() );
    							if ( groupByTag1Col > 0 ) {
    								rec.setFieldValue(groupByTag1Col, groupByTag1 );
    							}
    							rec.setFieldValue(groupItem1Col, groupItem1 );
    							if ( groupBy2Col > 0 ) {
    								rec.setFieldValue(groupBy2Col, groupBy2.toString() );
    								if ( groupByTag2Col > 0 ) {
    									rec.setFieldValue(groupByTag2Col, groupByTag2 );
    								}
    								rec.setFieldValue(groupItem2Col, groupItem2);
    							}
    							/*
    							if ( groupBy3Col > 0 ) {
    								rec.setFieldValue(groupBy3Col, groupBy3.toString() );
    								if ( groupByTag3Col > 0 ) {
    									rec.setFieldValue(groupByTag3Col, groupByTag3 );
    								}
    								rec.setFieldValue(groupItem3Col, groupItem3);
    							}
    							*/
    							rec.setFieldValue(metricCol, groupMetricName);
    							rec.setFieldValue(amountCol, Double.valueOf(groupCostAmount) );
    							rec.setFieldValue(unitsCol, groupCostUnit);
    						}
    						if  ( createTimeSeries ) {
    							addTimeSeriesData ( tslist, inputStart, inputEnd,
    								costDate, granularity,
    								groupBy1, groupByTag1, groupItem1,
    								groupBy2, groupByTag2, groupItem2,
    								//groupBy3, groupByTag3, groupItem3,
    								groupMetricName, Double.valueOf(groupCostAmount), groupCostUnit,
    								timeSeriesLocationId
    							);
    						}
    					}
    				}
    			}
    			else {
    				Message.printStatus(2, routine, "  No group costs are available.");
    			}
    			if ( resultByTime.hasTotal() ) {
    				// Process the total cost for the date.
    				Map<String, MetricValue> totalMetricsMap = resultByTime.total();
    				String itemName = "Total";
   					for ( Map.Entry<String,MetricValue> entry : totalMetricsMap.entrySet() ) {
   						String totalMetricName = entry.getKey();
    					String totalCostAmount = entry.getValue().amount();
    					String totalCostUnit = entry.getValue().unit();
   						Message.printStatus(2, routine, "Resource \"" + totalMetricName + "\" cost = " + totalCostAmount + " " + totalCostUnit );

   						String groupItem1 = "";
   						String groupItem2 = "";
   						//String groupItem3 = "";
   						if ( doTable ) {
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
   								rec = table.addRecord(table.emptyRecord());
   							}
   							// Set the data in the record.
   							rec.setFieldValue(dateCol, costDate);
   							rec.setFieldValue(granularityCol, granularity.toString());
   							rec.setFieldValue(groupBy1Col, groupBy1.toString() );
   							if ( groupByTag1Col > 0 ) {
   								rec.setFieldValue(groupByTag1Col, groupByTag1 );
   							}
   							//rec.setFieldValue(groupItem1Col, resultByTime.total().k);
   							if ( groupBy2Col > 0 ) {
   								rec.setFieldValue(groupBy2Col, groupBy2.toString() );
   								if ( groupByTag2Col > 0 ) {
   									rec.setFieldValue(groupByTag2Col, groupByTag2 );
   								}
   								//rec.setFieldValue(groupItem2Col, itemName);
   							}
   							/*
   							if ( groupBy3Col > 0 ) {
   								rec.setFieldValue(groupBy3Col, groupBy3.toString() );
   								if ( groupByTag3Col > 0 ) {
   									rec.setFieldValue(groupByTag3Col, groupByTag3 );
   								}
   								//rec.setFieldValue(groupItem3Col, itemName);
   							}
   							*/
   							rec.setFieldValue(metricCol, totalMetricName);
   							rec.setFieldValue(amountCol, Double.valueOf(totalCostAmount) );
   							rec.setFieldValue(unitsCol, totalCostUnit);
   						}
   						if  ( createTimeSeries ) {
   							addTimeSeriesData ( tslist, inputStart, inputEnd,
   								costDate, granularity,
   								groupBy1, groupByTag1, groupItem1,
   								groupBy2, groupByTag2, groupItem2,
   								//groupBy3, groupByTag3, groupItem3,
   								totalMetricName, Double.valueOf(totalCostAmount), totalCostUnit,
   								timeSeriesLocationId
   							);
   						}
   					}
    			}
    			else {
    				Message.printStatus(2, routine, "No total costs are available.");
    			}
   			}
    	}

    	// Set the property indicating the number of rows in the table.
        if ( (outputTableRowCountProperty != null) && !outputTableRowCountProperty.equals("") ) {
          	int rowCount = 0;
          	if ( table != null ) {
          		rowCount = table.getNumberOfRecords();
          	}
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", outputTableRowCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(rowCount) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + outputTableRowCountProperty + "\") from processor.";
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
	 * Get the service name for Group.keys().
	 * @param keys keys from Group.keys() call
	 * @return the service name corresponding to the group
	 */
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

		String Granularity = parameters.getValue ( "Granularity" );
		if ( (Granularity == null) || Granularity.isEmpty() ) {
			Granularity = "" + AwsBillingGranularityType.MONTHLY; // Default.
		}
		Granularity = TSCommandProcessorUtil.expandParameterValue(processor,this,Granularity);
		AwsBillingGranularityType granularity = AwsBillingGranularityType.valueOfIgnoreCase(Granularity);

    	String GroupBy1 = parameters.getValue ( "GroupBy1" );
		if ( (GroupBy1 == null) || GroupBy1.isEmpty() ) {
			GroupBy1 = "" + AwsBillingDimensionType.SERVICE; // Default.
		}
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

    	/*
    	String GroupBy3 = parameters.getValue ( "GroupBy3" );
		GroupBy3 = TSCommandProcessorUtil.expandParameterValue(processor,this,GroupBy3);
		AwsBillingDimensionType groupBy3 = null;
		if ( (GroupBy3 != null) && !GroupBy3.isEmpty() ) {
			groupBy3 = AwsBillingDimensionType.valueOfIgnoreCase(GroupBy3);
		}
    	String GroupByTag3 = parameters.getValue ( "GroupByTag3" );
    	*/

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
		String CreateTimeSeries = parameters.getValue ( "CreateTimeSeries" );
		boolean createTimeSeries = false;
		if ( (CreateTimeSeries == null) || CreateTimeSeries.equals("")) {
	    	CreateTimeSeries = _False; // Default.
		}
		if ( CreateTimeSeries.equalsIgnoreCase(_True) ) {
			createTimeSeries = true;
		}

		String TimeSeriesLocationID = parameters.getValue ( "TimeSeriesLocationID" );
		TimeSeriesLocationID = TSCommandProcessorUtil.expandParameterValue(processor,this,TimeSeriesLocationID);
		if ( (TimeSeriesLocationID == null) || TimeSeriesLocationID.isEmpty() ) {
			TimeSeriesLocationID = this._Auto;
		}

        String Alias = parameters.getValue ( "Alias" ); // Expanded dynamically below.

		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    	IfInputNotFound = _Warn; // Default.
		}
		String AppendOutput = parameters.getValue ( "AppendOutput" );
		boolean appendOutput = false;
		if ( (AppendOutput != null) && AppendOutput.equalsIgnoreCase(_True)) {
			appendOutput = true;
		}
		String OutputTableRowCountProperty = parameters.getValue ( "OutputTableRowCountProperty" );

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
		}

		// Get the table to process:
		// - only if appending
		// - if not appending, (re)create below

		DataTable table = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (OutputTableID != null) && !OutputTableID.isEmpty() && appendOutput ) {
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

		CostExplorerClient costExplorer = CostExplorerClient.builder()
			.region(regionObject)
			.credentialsProvider(credentialsProvider)
			.build();

		try {
	    	if ( commandPhase == CommandPhaseType.RUN ) {

    			// Column numbers are used later.
        		int dateCol = -1;
        		int granularityCol = -1;
        		int groupBy1Col = -1;
        		int groupByTag1Col = -1;
        		int groupItem1Col = -1;
        		int groupBy2Col = -1;
        		int groupByTag2Col = -1;
        		int groupItem2Col = -1;
        		//int groupBy3Col = -1;
        		//int groupByTag3Col = -1;
        		int groupItem3Col = -1;
        		int metricCol = -1;
        		int amountCol = -1;
   	    		int unitsCol = -1;

	    		if ( doTable || doOutputFile ) {
	    			// Requested a table and/or file:
	    			// - if only file is request, create a temporary table that is then written to output
    	    		if ( (table == null) || !appendOutput ) {
    	        		// The table needs to be created because it does not exist or NOT appending (so need new table):
    	    			// - the table columns depend on the S3 command being executed
    	    			// 1. Define the column names based on S3 commands.
    	        		List<TableField> columnList = new ArrayList<>();
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "Date", -1) );
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1) );
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupBy1", -1) );
   	        			if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupByTag1", -1) );
   	        			}
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupItem1", -1) );
   	        			if ( groupBy2 != null ) {
   	        				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupBy2", -1) );
   	        				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        					columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupByTag2", -1) );
   	        				}
   	        				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupItem2", -1) );
   	        			}
   	        			/*
   	        			if ( groupBy3 != null ) {
   	        				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupBy3", -1) );
   	        				if ( groupBy3 == AwsBillingDimensionType.TAG ) {
   	        					columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupByTag3", -1) );
   	        				}
   	        				columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "GroupItem3", -1) );
   	        			}
   	        			*/
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Metric", -1) );
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2) );
   	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Units", -1) );
    	        		// 2. Create the table if not found from the processor above.
   	        			// Create the table.
   	        			table = new DataTable( columnList );
                		// 3. Get the column numbers from the names for later use.
   	        			dateCol = table.getFieldIndex("Date");
   	        			granularityCol = table.getFieldIndex("Granularity");
   	        			groupBy1Col = table.getFieldIndex("GroupBy1");
   	        			if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        				groupByTag1Col = table.getFieldIndex("GroupByTag1");
   	        			}
   	        			groupItem1Col = table.getFieldIndex("GroupItem1");
   	        			if ( groupBy2 != null ) {
   	        				groupBy2Col = table.getFieldIndex("GroupBy2");
   	        				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        					groupByTag2Col = table.getFieldIndex("GroupByTag2");
   	        				}
   	        				groupItem2Col = table.getFieldIndex("GroupItem2");
   	        			}
   	        			/*
   	        			if ( groupBy3 != null ) {
   	        				groupBy3Col = table.getFieldIndex("GroupBy3");
   	        				if ( groupBy3 == AwsBillingDimensionType.TAG ) {
   	        					groupByTag3Col = table.getFieldIndex("GroupByTag3");
   	        				}
   	        				groupItem3Col = table.getFieldIndex("GroupItem3");
   	        			}
   	        			*/
   	        			metricCol = table.getFieldIndex("Metric");
   	        			amountCol = table.getFieldIndex("Amount");
   	        			unitsCol = table.getFieldIndex("Units");
    	        		// 4. Set the table in the processor:
    	        		//    - if new will add
    	        		//    - if append will overwrite by replacing the matching table ID
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
    	        		// 5. The table contents will be filled in when the doCostExplorer method is called.
    	    		}
    	    		else {
    	    			// Table exists:
    	        		// - make sure that the needed columns exist and otherwise add them
   	        			dateCol = table.getFieldIndex("Date");
   	        			granularityCol = table.getFieldIndex("Granularity");
   	        			groupBy1Col = table.getFieldIndex("GroupBy1");
   	        			if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        				groupByTag1Col = table.getFieldIndex("GroupBy1Tag");
   	        			}
   	        			groupItem1Col = table.getFieldIndex("GroupItem1");
   	        			if ( groupBy2 != null ) {
   	        				groupBy2Col = table.getFieldIndex("GroupBy2");
   	        				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        					groupByTag2Col = table.getFieldIndex("GroupBy2Tag");
   	        				}
   	        				groupItem2Col = table.getFieldIndex("GroupItem2");
   	        			}
   	        			/*
   	        			if ( groupBy3 != null ) {
   	        				groupBy3Col = table.getFieldIndex("GroupBy3");
   	        				if ( groupBy3 == AwsBillingDimensionType.TAG ) {
   	        					groupByTag3Col = table.getFieldIndex("GroupBy3Tag");
   	        				}
   	        				groupItem3Col = table.getFieldIndex("GroupItem3");
   	        			}
   	        			*/
   	        			metricCol = table.getFieldIndex("Metric");
   	        			amountCol = table.getFieldIndex("Amount");
   	        			unitsCol = table.getFieldIndex("Units");
   	        			if ( dateCol < 0 ) {
   	            			dateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "DateCol", -1), "");
   	        			}
   	        			if ( granularityCol < 0 ) {
   	            			granularityCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Granularity", -1), "");
   	        			}
   	        			if ( groupBy1Col < 0 ) {
   	            			groupBy1Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupBy1", -1), "");
   	        			}
   	        			if ( groupBy1 == AwsBillingDimensionType.TAG ) {
   	        				if ( groupByTag1Col < 0 ) {
   	            				groupByTag1Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupByTag1", -1), "");
   	        				}
   	        			}
   	        			if ( groupItem1Col < 0 ) {
   	            			groupItem1Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupItem1", -1), "");
   	        			}
   	        			if ( groupBy2 != null ) {
   	        				if ( groupBy2Col < 0 ) {
   	            				groupBy2Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupBy2", -1), "");
   	        				}
   	        				if ( groupBy2 == AwsBillingDimensionType.TAG ) {
   	        					if ( groupByTag2Col < 0 ) {
   	            					groupByTag2Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupByTag2", -1), "");
   	        					}
   	        				}
   	        				if ( groupItem2Col < 0 ) {
   	            				groupItem2Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupItem2", -1), "");
   	        				}
   	        			}
   	        			/*
   	        			if ( groupBy3 != null ) {
   	        				if ( groupBy3Col < 0 ) {
   	            				groupBy3Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupBy3", -1), "");
   	        				}
   	        				if ( groupBy3 == AwsBillingDimensionType.TAG ) {
   	        					if ( groupByTag3Col < 0 ) {
   	            					groupByTag3Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupByTag3", -1), "");
   	        					}
   	        				}
   	        				if ( groupItem3Col < 0 ) {
   	            				groupItem3Col = table.addField(new TableField(TableField.DATA_TYPE_STRING, "GroupItem3", -1), "");
   	        				}
   	        			}
   	        			*/
   	        			if ( metricCol < 0 ) {
   	            			metricCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Metric", -1), "");
   	        			}
   	        			if ( amountCol < 0 ) {
   	            			amountCol = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "Amount", -1, 2), "");
   	        			}
   	        			if ( unitsCol < 0 ) {
   	            			unitsCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Units", -1), "");
   	        			}
    	        	}
    	    	}

    	    	// Call the service that was requested to create the requested output.
   	    		// CostExplorerClient:
   	    		//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/CostExplorerClient.html

	    		// Time series to be created.
	    		List<TS> tslist = new ArrayList<>();
    	    	warningCount = doCostExplorer (
    	    		processor,
    	    		costExplorer,
    	    		table, OutputTableRowCountProperty,
    	    		dateCol, granularityCol,
    	    		groupBy1Col, groupByTag1Col, groupItem1Col,
    	    		groupBy2Col, groupByTag2Col, groupItem2Col,
    	    		//groupBy3Col, groupByTag3Col, groupItem3Col,
    	    		metricCol, amountCol, unitsCol,
    	    		InputStart_DateTime, InputEnd_DateTime,
    	    		granularity,
    	    		groupBy1, GroupByTag1,
    	    		groupBy2, GroupByTag2,
    	    		//groupBy3, GroupByTag3,
    	    		metric,
    	    		filterAvailabilityZones, filterInstanceTypes, filterRegions, filterServices, filterTags,
    	    		createTimeSeries, TimeSeriesLocationID, tslist,
    	    		status, logLevel, warningCount, commandTag );

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

	    	   	if ( createTimeSeries && (tslist.size() > 0) ) {
   				   	// Add the time series to the processor:
	    	   		// - also set the alias here since it depends on command status, etc.
    	   			if ( commandPhase == CommandPhaseType.RUN ) {
    	   				if ( (Alias != null) && !Alias.isEmpty() ) {
	    	   				// Expand the alias before setting.
    	   					for ( TS ts : tslist ) {
    	   						ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString (
    	   							processor, ts, Alias, status, commandPhase) );
    	   					}
    	   				}
	    	   		}

            	   	int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            	   	if ( wc2 > 0 ) {
                	   	message = "Error adding AWS Cost Explorer time series after read.";
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
	    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	        		if ( (table == null) && (OutputTableID != null) && !OutputTableID.isEmpty() ) {
	               		// Did not find table so is being created in this command.
	               		// Create an empty table and set the ID.
	               		table = new DataTable();
	               		table.setTableID ( OutputTableID );
	           		}
	           		setDiscoveryTable ( table );
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
	Set the output file that is created by this command.  This is only used internally.
	*/
	private void setOutputFile ( File file ) {
    	__OutputFile_File = file;
	}

	/**
	Set the table that is read by this class in discovery mode.
	@param table data table with output
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
			// Cost Explorer Query.
			"InputStart",
			"InputEnd",
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
			// Output.
			"OutputTableID",
			"OutputFile",
			"AppendOutput",
			"OutputTableRowCountProperty",
			// Time series.
			"CreateTimeSeries",
			"TimeSeriesLocationID",
			"Alias"
		};
		return this.toString(parameters, parameterOrder);
	}

}