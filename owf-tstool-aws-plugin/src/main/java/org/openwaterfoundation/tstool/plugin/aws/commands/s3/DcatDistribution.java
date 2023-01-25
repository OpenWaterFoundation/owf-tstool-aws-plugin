// DcatDistribution - corresponds to DCAT dataset distribution

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
This class holds data for a DCAT dataset distribution JSON file, used to read a dataset file with Jackson.
See CKAN examples:  https://github.com/ckan/ckanext-dcat/blob/master/examples/dataset.json
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcatDistribution {

	/**
	 * URL to access the dataset.
	 */
	private String accessURL = null;
	
	/**
	 * Distribution size in bytes.
	 */
	private Integer byteSize = null;

	/**
	 * Distribution description.
	 */
	private String description = null;

	/**
	 * Distribution format, mime-type if known.
	 */
	private String format = null;

	/**
	 * Distribution title, mixed case with no period.
	 */
	private String title = null;

	/**
	 * Default constructor.
	 */
	public DcatDistribution () {
	}

	/**
	 * Return the access URL.
	 */
	public String getAccessURL() {
		return this.accessURL;
	}

	/**
	 * Return the byte size.
	 */
	public int getByteSize() {
		return this.byteSize;
	}

	/**
	 * Return the description.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Return the format.
	 */
	public String getFormat() {
		return this.format;
	}

	/**
	 * Return the title.
	 */
	public String getTitle() {
		return this.title;
	}
}