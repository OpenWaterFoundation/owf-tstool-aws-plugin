// DcatDataset - corresponds to DCAT dataset JSON file

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import RTi.Util.Time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
This code may move to a more general location but is being kept with the AWS commands for now.

This class holds data for a DCAT dataset JSON file, used to read a dataset file with Jackson.
See CKAN examples:  https://github.com/ckan/ckanext-dcat/blob/master/examples/dataset.json
The above appears to be based on DCAT 2:  https://www.w3.org/TR/vocab-dcat/

A more recent DCAT draft has been published and may do a better job with versioning.
See: https://www.w3.org/TR/vocab-dcat-3/

The above documents use RDF (Resource Description Framework) to describe the data model.
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcatDataset {

	// ----------------- Start DCAT standard properties --------------------------------

	/**
	 * Dataset title, a few words, capitalized, no period.
	 */
	private String title = null;
	
	/**
	 * Dataset description, one sentence with period.
	 */
	private String description = null;
	
	/**
	 * Dataset identifier, a unique string within the application.
	 */
	private String identifier = null;
	
	/**
	 * Dataset landing page URL.
	 */
	private String landingPage = null;
	
	/**
	 * Issue date/time string using format:  YYYY-MM-DD or YYYY-MM-DDThh:mm?
	 */
	private String issued = null;

	/**
	 * Modification time string using format: YYYY-MM-DDThh:mm
	 */
	private String modified = null;

	/**
	 * Keywords to use for searches.
	 */
	private List<String> keyword = new ArrayList<>();
	
	/**
	 * Publisher for the dataset.
	 */
	private DcatPublisher publisher = null;
	
	/**
	 * Distribution information.
	 */
	private List<DcatDistribution> distribution = new ArrayList<>();

	// ----------------- End DCAT standard properties --------------------------------

	// ----------------- Start OWF extensions --------------------------------

	/**
	 * This is an OWF extension.
	 * Indicate the parent dataset file, used so that a versioned dataset 'dataset.json' file can include
	 * minimal override data.
	 */
	private String parentDatasetFile = null;

	/**
	 * This is an OWF extension.
	 * Version as a string, can be a date such as YYYY-MM-DD, YYYY, YYYY-MM, or a semantic version such as 1.2.3.
	 */
	private String version = null;

	/**
	 * This is an OWF extension.
	 * Cloud storage last modified time.
	 */
	@JsonIgnore
	private DateTime cloudLastModified = null;

	/**
	 * This is an OWF extension.
	 * Cloud storage owner.
	 */
	@JsonIgnore
	private String cloudOwner = null;

	/**
	 * This is an OWF extension.
	 * Cloud storage path such as AWS bucket key used when manipulating remote path.
	 */
	@JsonIgnore
	private String cloudPath = null;

	/**
	 * This is an OWF extension.
	 * Cloud object size, KB (for the dataset file).
	 */
	@JsonIgnore
	private Long cloudSizeKb = null;

	/**
	 * This is an OWF extension.
	 * Local JSON file used as Markdown input to the index file "Dataset Details" section (e.g., 'dataset-details.md').
	 */
	@JsonIgnore
	private String localMarkdownPath = null;

	/**
	 * This is an OWF extension.
	 * Local JSON file used as input to the index file.
	 */
	@JsonIgnore
	private String localImagePath = null;

	/**
	 * This is an OWF extension.
	 * Local storage path to file, use to locate a local file, which may be a temporary file from a cloud download.
	 */
	@JsonIgnore
	private String localPath = null;

	// ----------------- End OWF extensions --------------------------------
	
	/**
	 * Default constructor.
	 */
	public DcatDataset () {
	}

	/**
	 * Copy the parent dataset's properties into this dataset, if not set.
	 * @param parentDataset parent dataset to copy
	 */
   	public void copyParent ( DcatDataset parentDataset ) {
   		if ( parentDataset == null ) {
   			return;
   		}
   		// Alphabetize.
   		if ( (this.description == null) || this.description.isEmpty() && (parentDataset.getDescription() != null) ) {
   			this.description = parentDataset.getDescription();
   		}
   		if ( (this.identifier == null) || this.identifier.isEmpty() && (parentDataset.getIdentifier() != null) ) {
   			this.identifier = parentDataset.getIdentifier();
   		}
   		if ( (this.issued == null) || this.issued.isEmpty() && (parentDataset.getIssued() != null) ) {
   			this.issued = parentDataset.getIssued();
   		}
   		if ( (this.keyword == null) || (this.keyword.size() == 0) && (parentDataset.getKeyword() != null) ) {
   			this.keyword = new ArrayList<>();
   			this.keyword.addAll(parentDataset.getKeyword());
   		}
   		// landingPage is specific to the dataset.
   		if ( (this.modified == null) || this.modified.isEmpty() && (parentDataset.getModified() != null) ) {
   			this.modified = parentDataset.getModified();
   		}
   		if ( (this.publisher == null) && (parentDataset.getIssued() != null) ) {
   			// Don't need to do a deep copy.
   			this.publisher = parentDataset.getPublisher();
   		}
   		if ( (this.title == null) || this.title.isEmpty() && (parentDataset.getTitle() != null) ) {
   			this.title = parentDataset.getTitle();
   		}
   	}

	/**
	 * Return the last modification time of the cloud resource file.
	 */
	public DateTime getCloudLastModified() {
		return this.cloudLastModified;
	}

	/**
	 * Return the owner of the cloud resource file.
	 */
	public String getCloudOwner() {
		return this.cloudOwner;
	}

	/**
	 * Return the path to the cloud resource file,
	 * for example the key (path) to a bucket file.
	 */
	public String getCloudPath() {
		return this.cloudPath;
	}

	/**
	 * Return the size of the cloud resource file, KB
	 */
	public Long getCloudSizeKb() {
		return this.cloudSizeKb;
	}

	/**
	 * Return the description.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Return the distribution list.
	 */
	public List<DcatDistribution> getDistribution() {
		return this.distribution;
	}

	/**
	 * Return the identifier.
	 */
	public String getIdentifier() {
		return this.identifier;
	}

	/**
	 * Return the issue date/time.
	 */
	public String getIssued() {
		return this.issued;
	}

	/**
	 * Return the keyword list.
	 */
	public List<String> getKeyword() {
		return this.keyword;
	}

	/**
	 * Return the landing page URL.
	 */
	public String getLandingPage() {
		return this.landingPage;
	}

	/**
	 * Return the path to the local image file, for example the path to a downloaded image file (e.g., 'dataset.png).
	 */
	public String getLocalImagePath() {
		return this.localImagePath;
	}

	/**
	 * Return the path to the local markdown file (e.g., dataset-details.md).
	 */
	public String getLocalMarkdownPath() {
		return this.localMarkdownPath;
	}

	/**
	 * Return the path to the local resource file, for example the path to a downloaded 'dataset.json' file.
	 */
	public String getLocalPath() {
		return this.localPath;
	}

	/**
	 * Return the modified date/time.
	 */
	public String getModified() {
		return this.modified;
	}

	/**
	 * Return the parent dataset file, such as "../dataset.json".
	 */
	public String getParentDatasetFile() {
		return this.parentDatasetFile;
	}

	/**
	 * Return the publisher name.
	 */
	public DcatPublisher getPublisher() {
		return this.publisher;
	}
	
	/**
	 * Return the title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Return the version.
	 */
	public String getVersion() {
		return this.version;
	}
	
	/**
	 * Set the cloud last modified for the dataset file.
	 */
	public void setCloudLastModified ( DateTime cloudLastModified ) {
		this.cloudLastModified = cloudLastModified;
	}

	/**
	 * Set the cloud owner for the dataset file.
	 */
	public void setCloudOwner ( String cloudOwner ) {
		this.cloudOwner = cloudOwner;
	}

	/**
	 * Set the cloud path for the dataset file.
	 */
	public void setCloudPath ( String cloudPath ) {
		this.cloudPath = cloudPath;
	}

	/**
	 * Set the cloud size (KB) for the dataset file.
	 */
	public void setCloudSizeKb ( Long cloudSizeKb ) {
		this.cloudSizeKb = cloudSizeKb;
	}

	/**
	 * Set the local path to the dataset image file.
	 */
	public void setLocalImagePath ( String localImagePath ) {
		this.localImagePath = localImagePath;
	}

	/**
	 * Set the local path to the dataset MarkDown file (dataset-details.md).
	 */
	public void setLocalMarkdownPath ( String localMarkdownPath ) {
		this.localMarkdownPath = localMarkdownPath;
	}

	/**
	 * Set the local path to the dataset file.
	 */
	public void setLocalPath ( String localPath ) {
		this.localPath = localPath;
	}
}