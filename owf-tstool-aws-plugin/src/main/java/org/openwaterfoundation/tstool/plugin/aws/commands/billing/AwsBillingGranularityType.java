// AwsBillingGranularityType - granularity for Cost Explorer

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

import software.amazon.awssdk.services.costexplorer.model.Granularity;

import java.util.ArrayList;
import java.util.List;

/**
AWS Cost Explorer granularity, as enumeration to simplify code.
Use this because nice string versions of the Granularity enumeration return all upper case
and want to show the same strings as the Cost Explorer web page.
See:  https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/model/Granularity.html
*/
public enum AwsBillingGranularityType {
	/**
	Daily granularity.
	*/
	DAILY ( "Daily", Granularity.DAILY, "Day" ),

	/**
	Hourly granularity.
	*/
	HOURLY ( "Hourly", Granularity.HOURLY, "Hour" ),

	/**
	Monthly granularity.
	*/
	MONTHLY ( "Monthly", Granularity.MONTHLY, "Month" );

	/**
	The description, useful for UI notes.
	*/
	private String displayName;
	
	/**
	 * The AWS Cost Explorer granularity.
	 */
	private Granularity granularity;

	/**
	The data interval used with time series identifiers.
	*/
	private String interval;

	/**
	Construct an enumeration value.
	@param displayName name that can be displayed for the type
	*/
	private AwsBillingGranularityType ( String displayName, Granularity granularity, String interval ) {
    	this.displayName = displayName;
    	this.granularity = granularity;
    	this.interval = interval;
	}

	/**
	Get the list of granularity types, in appropriate order.
	@return the list of command types.
	*/
	public static List<AwsBillingGranularityType> getChoices() {
    	List<AwsBillingGranularityType> choices = new ArrayList<>();
    	choices.add ( AwsBillingGranularityType.MONTHLY );
    	choices.add ( AwsBillingGranularityType.DAILY );
    	choices.add ( AwsBillingGranularityType.HOURLY );
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
	Get the list of granularity type as strings.
	@return the list of granularity types as strings.
	*/
	public static List<String> getChoicesAsStrings () {
    	List<AwsBillingGranularityType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	AwsBillingGranularityType choice = choices.get(i);
        	String choiceString = "" + choice;
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}
	
	/**
	 * Return the AWS SDK Granularity.
	 * @return the AWS SDK Granularity
	 */
	public Granularity getGranularity () {
		return this.granularity;
	}

	/**
	 * Return the data interval used with time series identifiers.
	 * @return the data interval used with time series identifiers
	 */
	public String getInterval () {
		return this.interval;
	}

	/**
	Return the display name for the type.
	@return the display name.
	*/
	@Override
	public String toString() {
    	return this.displayName;
	}

	/**
	Return the enumeration value given a string name (case-independent).
	@param name the name to match
	@return the enumeration value given a string name (case-independent), or null if not matched.
	*/
	public static AwsBillingGranularityType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	AwsBillingGranularityType [] values = values();
    	for ( AwsBillingGranularityType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}