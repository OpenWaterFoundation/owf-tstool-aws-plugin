package org.openwaterfoundation.tstool.plugin.aws.commands.billing;

import java.util.HashMap;

/**
 * Class to hold image (AMI) properties.
 * Once instance should be created for every image.
 */
public class AwsAmiProperties {

	// General properties.

	/**
	 * Region.
	 */
	String region = "";

	// EC2 properties.

	/**
	 * EC2 instance ID, not currently used.
	 */
	String ec2InstanceId = "";

	/**
	 * Map of EC2 tags, useful to identify the EC2 instance, not currently used.
	 */
	HashMap<String,String> ec2TagMap = new HashMap<>();

	// Image properties.

	/**
	 * Image ID.
	 */
	String imageId = "";

	/**
	 * Image description.
	 */
	String imageDescription = "";

	/**
	 * Image size in GB.
	 */
	int imageSizeGB = 0;

	/**
	 * Map of image tags.
	 */
	HashMap<String,String> imageTagMap = new HashMap<>();

	/**
	 * Constructor.
	 */
	public AwsAmiProperties () {
	}

	/**
	 * Get the EC2 instance ID.
	 * @return the EC2 instance ID.
	 */
	public String getEc2InstanceId () {
		return this.ec2InstanceId;
	}

	/**
	 * Get the EC2 tag value.
	 * @param tagName the tag name
	 * @return get the tag value
	 */
	public String getEc2TagValue ( String tagName ) {
		return this.ec2TagMap.get(tagName);
	}

	/**
	 * Get the region.
	 * @return the region.
	 */
	public String getRegion () {
		return this.region;
	}

	/**
	 * Get the image description.
	 * @return the image description.
	 */
	public String getImageDescription () {
		return this.imageDescription;
	}

	/**
	 * Get the image ID.
	 * @return the image ID.
	 */
	public String getImageId () {
		return this.imageId;
	}

	/**
	 * Get the image tag value.
	 * @param tagName the tag name
	 * @return get the tag value
	 */
	public String getImageTagValue ( String tagName ) {
		return this.imageTagMap.get(tagName);
	}

	/**
	 * Get the image size in GB.
	 * @return the image size.
	 */
	public int getImageSizeGB () {
		return this.imageSizeGB;
	}

	/**
	 * Set the EC2 instance ID.
	 * @param ec2InstanceId the EC2 instance ID
	 */
	public void setEc2InstanceId ( String ec2InstanceId ) {
		this.ec2InstanceId = ec2InstanceId;
	}

	/**
	 * Set an EC2 tag.
	 * @param tagName tag name to set
	 * @param tagValue tag value to set
	 */
	public void setEc2Tag ( String tagName, String tagValue ) {
		this.ec2TagMap.put ( tagName, tagValue );
	}

	/**
	 * Set the region.
	 * @param region the service region
	 */
	public void setRegion ( String region ) {
		this.region = region;
	}

	/**
	 * Set the image description.
	 * @param imageDescription the image description
	 */
	public void setImageDescription ( String imageDescription ) {
		this.imageDescription = imageDescription;
	}

	/**
	 * Set the image ID.
	 * @param imageId the image ID
	 */
	public void setImageId ( String imageId ) {
		this.imageId = imageId;
	}

	/**
	 * Set an image tag.
	 * @param tagName tag name to set
	 * @param tagValue tag value to set
	 */
	public void setImageTag ( String tagName, String tagValue ) {
		this.imageTagMap.put ( tagName, tagValue );
	}

	/**
	 * Set the image size, GB.
	 * @param imageSizeGB the image size
	 */
	public void setImageSizeGB ( int imageSizeGB ) {
		this.imageSizeGB = imageSizeGB;
	}

}