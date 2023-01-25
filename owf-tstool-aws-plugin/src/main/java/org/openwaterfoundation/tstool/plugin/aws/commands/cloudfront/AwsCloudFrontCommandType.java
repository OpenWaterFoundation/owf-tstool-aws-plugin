// AwsCloudFrontCommandType - AWS CloudFront command enumeration

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

import java.util.ArrayList;
import java.util.List;

/**
AWS S3 commands, as enumeration to simplify code.
*/
public enum AwsCloudFrontCommandType {
	/**
	Invalidate CloudFront distribution objects.
	*/
	INVALIDATE_DISTRIBUTION ( "InvalidateDistribution", "Invalidate CloudFront distribution" ),

	/**
	List CloudFront distributions.
	*/
	LIST_DISTRIBUTIONS ( "ListDistributions", "List CloudFront distributions" ),

	/**
	List CloudFront invalidations.
	*/
	LIST_INVALIDATIONS ( "ListInvalidations", "List CloudFront invalidations" );

	/**
	The name that is used for choices and other technical code (terse).
	*/
	private final String name;

	/**
	The description, useful for UI notes.
	*/
	private final String description;

	/**
	Construct an enumeration value.
	@param name name that should be displayed in choices, etc.
	@param descritpion command description.
	*/
	private AwsCloudFrontCommandType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	Get the list of command types, in appropriate order.
	@return the list of command types.
	*/
	public static List<AwsCloudFrontCommandType> getChoices() {
    	List<AwsCloudFrontCommandType> choices = new ArrayList<AwsCloudFrontCommandType>();
    	choices.add ( AwsCloudFrontCommandType.INVALIDATE_DISTRIBUTION );
    	choices.add ( AwsCloudFrontCommandType.LIST_DISTRIBUTIONS );
    	choices.add ( AwsCloudFrontCommandType.LIST_INVALIDATIONS );
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@return the list of command types as strings.
	@param includeNote Currently not implemented.
	*/
	public static List<String> getChoicesAsStrings( boolean includeNote ) {
    	List<AwsCloudFrontCommandType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	AwsCloudFrontCommandType choice = choices.get(i);
        	String choiceString = "" + choice;
        	//if ( includeNote ) {
            //	choiceString = choiceString + " - " + choice.toStringVerbose();
        	//}
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}

	/**
	Return the command name for the type.  This is the same as the value.
	@return the display name.
	*/
	@Override
	public String toString() {
    	return this.name;
	}

	/**
	Return the enumeration value given a string name (case-independent).
	@param name the name to match
	@return the enumeration value given a string name (case-independent), or null if not matched.
	*/
	public static AwsCloudFrontCommandType valueOfIgnoreCase (String name) {
	    if ( name == null ) {
        	return null;
    	}
    	AwsCloudFrontCommandType [] values = values();
    	for ( AwsCloudFrontCommandType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}