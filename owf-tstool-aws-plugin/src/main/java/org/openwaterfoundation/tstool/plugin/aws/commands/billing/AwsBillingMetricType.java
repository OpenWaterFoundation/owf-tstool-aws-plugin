// AwsBillingType - how to aggregate costs for the Cost Explorer

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

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.costexplorer.model.Metric;

/**
AWS Cost Explorer "Aggregate costs by", as enumeration to simplify code, called "metric" internally in the API.
Use this because nice string versions of the Granularity enumeration return all upper case
and want to show the same strings as the Cost Explorer web page.
See: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/model/Metric.html
See "Understanding your AWS Cost Datasets:  A Cheat Sheet"
(https://aws.amazon.com/blogs/aws-cloud-financial-management/understanding-your-aws-cost-datasets-a-cheat-sheet/).
*/
public enum AwsBillingMetricType {
	/**
	Amortized costs.
	This is not included in the Cost Explorer web page as of 2024-01-14.
	*/
	AMORTIZED_COSTS ( "AmortizedCosts", Metric.AMORTIZED_COST ),

	/**
	Blended costs.
	This is included in the Cost Explorer web page.
	*/
	BLENDED_COSTS ( "BlendedCosts", Metric.BLENDED_COST ),

	/**
	Net amortized costs.
	This is included in the Cost Explorer web page.
	*/
	NET_AMORTIZED_COSTS ( "NetAmortizedCosts", Metric.NET_AMORTIZED_COST ),

	/**
	Net unblended costs.
	This is included in the Cost Explorer web page.
	*/
	NET_UNBLENDED_COSTS ( "NetUnblendedCosts", Metric.NET_UNBLENDED_COST ),

	/**
	Normalized usage amount.
	This is not included in the Cost Explorer web page as of 2024-01-14.
	*/
	NORMALIZED_USAGE_AMOUNT ( "NormalizedUsageAmount", Metric.NORMALIZED_USAGE_AMOUNT ),

	/**
	Unblended costs.
	This is included in the Cost Explorer web page.
	Costs for reserved instances, savings plans, etc. are shown on the first day of the month.
	*/
	UNBLENDED_COSTS ( "UnblendedCosts", Metric.UNBLENDED_COST ),

	/**
	Unknown.
	This is an internal value not meant to be displayed.
	*/
	UNKNOWN ( "Unknown", Metric.UNKNOWN_TO_SDK_VERSION ),

	/**
	Usage quantity.
	This is not included in the Cost Explorer web page as of 2024-01-14.
	*/
	USAGE_QUANTITY ( "UsageQuantity", Metric.USAGE_QUANTITY );

	/**
	The description, useful for UI notes.
	*/
	private final String displayName;

	/**
	 * The AWS SDK metric.
	 */
	private final Metric metric;

	/**
	Construct an enumeration value.
	@param displayName name that can be displayed for the type
	@param metric the metric used for costs
	*/
	private AwsBillingMetricType ( String displayName, Metric metric ) {
    	this.displayName = displayName;
    	this.metric = metric;
	}

	/**
	Get the list of types, in appropriate order.
	@return the list of command types.
	*/
	public static List<AwsBillingMetricType> getChoices () {
    	List<AwsBillingMetricType> choices = new ArrayList<>();
    	choices.add ( AwsBillingMetricType.BLENDED_COSTS );
    	choices.add ( AwsBillingMetricType.NET_AMORTIZED_COSTS );
    	choices.add ( AwsBillingMetricType.NET_UNBLENDED_COSTS);
    	choices.add ( AwsBillingMetricType.UNBLENDED_COSTS );
    	return choices;
	}

	/**
	Get the list of types as strings formatted as a csv.
	@return the list of types as strings.
	*/
	public static String getChoicesAsCsv () {
    	List<String> choices = getChoicesAsStrings();
    	StringBuilder csv = new StringBuilder();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	String choice = choices.get(i);
        	if ( i > 0 ) {
        		csv.append(", ");
        	}
        	String choiceString = "" + choice;
        	csv.append ( choiceString );
    	}
    	return csv.toString();
	}

	/**
	Get the list of types as strings.
	@return the list of types as strings.
	*/
	public static List<String> getChoicesAsStrings () {
    	List<AwsBillingMetricType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	AwsBillingMetricType choice = choices.get(i);
        	String choiceString = "" + choice;
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}

	/**
	 * Return the metric.
	 * @return the metric
	 */
	public Metric getMetric () {
		return this.metric;
	}

	/**
	Return the display name for the type.
	@return the display name.
	*/
	@Override
	public String toString () {
    	return this.displayName;
	}

	/**
	Return the enumeration value given a string name (case-independent).
	@param name the name to match
	@return the enumeration value given a string name (case-independent), or null if not matched.
	*/
	public static AwsBillingMetricType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	AwsBillingMetricType [] values = values();
    	for ( AwsBillingMetricType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}