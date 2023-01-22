// AwsS3ObjectType - indicate whether AwsS3Object is from S3Object or CommonPrefix

/* NoticeStart

OWF AWS TSTool Plugin
Copyright (C) 2023 Open Water Foundation

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

import java.util.ArrayList;
import java.util.List;

/**
AWS S3 commands, as enumeration to simplify code.
*/
public enum AwsS3ObjectType {
	/**
	Copy S3 bucket object.
	*/
	BUCKET ( "Bucket", "Bucket" ),

	/**
	Copy S3 bucket object.
	*/
	FOLDER ( "CommonPrefix", "Folder" ),

	/**
	Delete S3 bucket object.
	*/
	FILE ( "S3Object", "File" );

	/**
	The name that is used for choices and other technical code (terse).
	*/
	private final String sourceName;

	/**
	The description, useful for UI notes.
	*/
	private final String displayName;

	/**
	Construct an enumeration value.
	@param internalName name that indicates the internal source of the object data
	@param displayName name that can be displayed for the type
	*/
	private AwsS3ObjectType(String sourceName, String displayName ) {
    	this.sourceName = sourceName;
    	this.displayName = displayName;
	}

	/**
	Get the list of command types, in appropriate order.
	@return the list of command types.
	*/
	public static List<AwsS3ObjectType> getChoices() {
    	List<AwsS3ObjectType> choices = new ArrayList<>();
    	choices.add ( AwsS3ObjectType.BUCKET );
    	choices.add ( AwsS3ObjectType.FILE );
    	choices.add ( AwsS3ObjectType.FOLDER );
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@return the list of command types as strings.
	@param includeNote Currently not implemented.
	*/
	public static List<String> getChoicesAsStrings( boolean includeNote ) {
    	List<AwsS3ObjectType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	AwsS3ObjectType choice = choices.get(i);
        	String choiceString = "" + choice;
        	//if ( includeNote ) {
            //	choiceString = choiceString + " - " + choice.toStringVerbose();
        	//}
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
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
	public static AwsS3ObjectType valueOfIgnoreCase (String name) {
	    if ( name == null ) {
        	return null;
    	}
    	AwsS3ObjectType [] values = values();
    	for ( AwsS3ObjectType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}