// AWS tag mode type - AWS tag mode type

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

/**
AWS modes for tagging, which can be used for any service that uses tags.
*/
public enum TagModeType {
	/**
	Set the specified tag(s) and keep other tags.
	*/
	SET ( "Set", "Set the specific tag(s) and keep other tag(s)" ),

	/**
	Replace all tags with new tags.
	*/
	SET_ALL ( "SetAll", "Set (replace) all tags with new tag(s)" ),

	/**
	Delete the specified tag(s), keep others.
	*/
	DELETE ( "Delete", "Delete specfied tag(s), keep others" ),

	/**
	Delete all the tags, keeping none.
	*/
	DELETE_ALL ( "DeleteAll", "Delete all tags" );

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
	private TagModeType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	 * Return the description.
	 * @return the description
	 */
	public String getDescription () {
		return this.description;
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
	public static TagModeType valueOfIgnoreCase (String name) {
	    if ( name == null ) {
        	return null;
    	}
    	TagModeType [] values = values();
    	for ( TagModeType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}