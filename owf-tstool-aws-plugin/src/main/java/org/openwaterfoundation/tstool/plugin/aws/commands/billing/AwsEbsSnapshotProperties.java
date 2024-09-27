package org.openwaterfoundation.tstool.plugin.aws.commands.billing;

import java.util.HashMap;

/**
 * Class to hold EBS snapshot properties.
 * Once instance should be created for every snapshot.
 */
public class AwsEbsSnapshotProperties {

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

	// Snapshot properties.

	/**
	 * Snapshot ID.
	 */
	String snapshotId = "";

	/**
	 * Snapshot description.
	 */
	String snapshotDescription = "";

	/**
	 * Snapshot volume size in GB.
	 */
	int snapshotVolumeSizeGB = 0;

	/**
	 * Map of snapshot tags.
	 */
	HashMap<String,String> snapshotTagMap = new HashMap<>();

	/**
	 * Constructor.
	 */
	public AwsEbsSnapshotProperties () {
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
	 * Get the snapshot description.
	 * @return the snapshot description.
	 */
	public String getSnapshotDescription () {
		return this.snapshotDescription;
	}

	/**
	 * Get the snapshot ID.
	 * @return the snapshot ID.
	 */
	public String getSnapshotId () {
		return this.snapshotId;
	}

	/**
	 * Get the snapshot tag value.
	 * @param tagName the tag name
	 * @return get the tag value
	 */
	public String getSnapshotTagValue ( String tagName ) {
		return this.snapshotTagMap.get(tagName);
	}

	/**
	 * Get the snapshot volume size in GB.
	 * @return the snapshot volume size.
	 */
	public int getSnapshotVolumeSizeGB () {
		return this.snapshotVolumeSizeGB;
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
	 * Set the snapshot description.
	 * @param snapshotDescription the EBS snapshot description
	 */
	public void setSnapshotDescription ( String snapshotDescription ) {
		this.snapshotDescription = snapshotDescription;
	}

	/**
	 * Set the snapshot ID.
	 * @param snapshotId the EBS snapshot ID
	 */
	public void setSnapshotId ( String snapshotId ) {
		this.snapshotId = snapshotId;
	}

	/**
	 * Set a snapshot tag.
	 * @param tagName tag name to set
	 * @param tagValue tag value to set
	 */
	public void setSnapshotTag ( String tagName, String tagValue ) {
		this.snapshotTagMap.put ( tagName, tagValue );
	}

	/**
	 * Set the snapshot volume size, GB.
	 * @param snapshotVolumeSizeGB the EBS snapshot volume size
	 */
	public void setSnapshotVolumeSizeGB ( int snapshotVolumeSizeGB ) {
		this.snapshotVolumeSizeGB = snapshotVolumeSizeGB;
	}

}