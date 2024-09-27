package org.openwaterfoundation.tstool.plugin.aws.commands.billing;

import java.util.HashMap;

/**
 * Class to hold EC2-related properties.
 */
public class AwsEc2Properties {

	// General properties.

	/**
	 * Service.
	 */
	String service = "";

	/**
	 * Region.
	 */
	String region = "";

	// EC2 properties.

	/**
	 * Map of EC2 tags.
	 */
	HashMap<String,String> ec2TagMap = new HashMap<>();

	// EBS volume properties

	/**
	 * EBS volume ID.
	 */
	String ebsVolumeId = "";

	/**
	 * Map of EBS volume tags.
	 */
	HashMap<String,String> ebsVolumeTagMap = new HashMap<>();

	/**
	 * EC2 instance ID.
	 */
	String ec2InstanceId = "";

	/**
	 * EC2 instance state.
	 */
	String ec2InstanceState = "";

	/**
	 * EC2 instance type.
	 */
	String ec2InstanceType = "";
	
	// VPC properties.

	/**
	 * VPC ID.
	 */
	String vpcId = "";

	/**
	 * Map of VPC tags.
	 */
	HashMap<String,String> vpcTagMap = new HashMap<>();

	/**
	 * Constructor.
	 */
	public AwsEc2Properties () {
	}

	/**
	 * Get the EBS volume ID.
	 * @return the EBS volume ID.
	 */
	public String getEbsVolumeId () {
		return this.ebsVolumeId;
	}

	/**
	 * Get the EBS volume tag value.
	 * @param tagName the tag name
	 * @return get the tag value
	 */
	public String getEbsVolumeTagValue ( String tagName ) {
		return this.ebsVolumeTagMap.get(tagName);
	}

	/**
	 * Get the EC2 instance ID.
	 * @return the EC2 instance ID.
	 */
	public String getEc2InstanceId () {
		return this.ec2InstanceId;
	}

	/**
	 * Get the EC2 instance state.
	 * @return the EC2 instance state.
	 */
	public String getEc2InstanceState () {
		return this.ec2InstanceState;
	}

	/**
	 * Get the EC2 instance type.
	 * @return the EC2 instance type.
	 */
	public String getEc2InstanceType () {
		return this.ec2InstanceType;
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
	 * Get the service.
	 * @return the service.
	 */
	public String getService () {
		return this.service;
	}

	/**
	 * Get the VPC ID.
	 * @return the VPC ID.
	 */
	public String getVpcId () {
		return this.vpcId;
	}

	/**
	 * Get the VPC tag value.
	 * @param tagName the tag name
	 * @return get the tag value
	 */
	public String getVpcTagValue ( String tagName ) {
		return this.vpcTagMap.get(tagName);
	}

	/**
	 * Set the EBS volume ID.
	 * @param ebsVolumeId the EBS volume ID
	 */
	public void setEbsVolumeId ( String ebsVolumeId ) {
		this.ebsVolumeId = ebsVolumeId;
	}

	/**
	 * Set an EBS volume tag.
	 * @param tagName tag name to set
	 * @param tagValue tag value to set
	 */
	public void setEbsVolumeTag ( String tagName, String tagValue ) {
		this.ebsVolumeTagMap.put ( tagName, tagValue );
	}

	/**
	 * Set the EC2 instance ID.
	 * @param ec2InstanceId the EC2 instance ID
	 */
	public void setEc2InstanceId ( String ec2InstanceId ) {
		this.ec2InstanceId = ec2InstanceId;
	}

	/**
	 * Set the EC2 instance state.
	 * @param ec2InstanceState the EC2 instance state
	 */
	public void setEc2InstanceState ( String ec2InstanceState ) {
		this.ec2InstanceState = ec2InstanceState;
	}

	/**
	 * Set the EC2 instance type.
	 * @param ec2InstanceType the EC2 instance type
	 */
	public void setEc2InstanceType ( String ec2InstanceType ) {
		this.ec2InstanceType = ec2InstanceType;
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
	 * Set the service.
	 * @param service the service name
	 */
	public void setService ( String service ) {
		this.service = service;
	}

	/**
	 * Set the VPC ID.
	 * @param vpcId the VPC ID
	 */
	public void setVpcId ( String vpcId ) {
		this.vpcId = vpcId;
	}

	/**
	 * Set a VPC tag.
	 * @param tagName tag name to set
	 * @param tagValue tag value to set
	 */
	public void setVpcTag ( String tagName, String tagValue ) {
		this.vpcTagMap.put ( tagName, tagValue );
	}
}