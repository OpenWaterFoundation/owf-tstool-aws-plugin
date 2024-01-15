// DcatPublisher - corresponds to DCAT dataset publisher

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
This class holds data for a DCAT dataset publisher JSON file, used to read a dataset file with Jackson.
See CKAN examples:  https://github.com/ckan/ckanext-dcat/blob/master/examples/dataset.json
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcatPublisher {

	/**
	 * Publisher name, organization.
	 */
	private String name = null;
	
	/**
	 * Publisher email address.
	 */
	private String mbox = null;

	/**
	 * Default constructor.
	 */
	public DcatPublisher () {
	}

	/**
	 * Return the publisher email.
	 */
	public String getMbox() {
		return this.mbox;
	}

	/**
	 * Return the publisher name.
	 */
	public String getName() {
		return this.name;
	}
}