// AwsBillingDimensionType - dimension for Cost Explorer

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

import software.amazon.awssdk.services.costexplorer.model.Dimension;

import java.util.ArrayList;
import java.util.List;

/**
AWS Cost Explorer dimension, as enumeration to simplify code.
Use this because nice string versions of the Dimension enumeration return all upper case
and want to show the same strings as the Cost Explorer web page.
See:  https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/costexplorer/model/Dimension.html
*/
public enum AwsBillingDimensionType {
	// Don't know which of these match the Cost Explorer web site "Cost Category", "Tag", and "Charge Type".
	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	AGREEMENT_END_DATE_TIME_AFTER ( "AgreementEndDateTimeBefore", Dimension.AGREEMENT_END_DATE_TIME_AFTER ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	AGREEMENT_END_DATE_TIME_BEFORE ( "AgreementEndDateTimeBefore", Dimension.AGREEMENT_END_DATE_TIME_BEFORE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	 * Only available with anomaly subscriptions?
	*/
	//ANOMALY_TOTAL_IMPACT_ABSOLUTE ( "?", Dimension.ANOMALY_TOTAL_IMPACT_ABSOLUTE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	 * Only available with anomaly subscriptions?
	*/
	//ANOMALY_TOTAL_IMPACT_PERCENTAGE ( "?", Dimension.ANOMALY_TOTAL_IMPACT_PERCENTAGE ),

	/**
	 * Is listed in the Cost Explorer web site.
	 * Note that the enumeration value is OPERATION.
	*/
	API_OPERATION ( "ApiOperation", Dimension.OPERATION ),

	/**
	 * Availability zone (AZ).
	 * Is listed in the Cost Explorer web site.
	*/
	AVAILABILITY_ZONE ( "AvailabilityZone", Dimension.AZ ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	BILLING_ENTITY ( "BillingEntity", Dimension.BILLING_ENTITY ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	CACHE_ENGINE ( "?", Dimension.CACHE_ENGINE ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	DATABASE_ENGINE ( "DatabaseEngine", Dimension.DATABASE_ENGINE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	DEPLOYMENT_OPTION ( "DeploymentOption", Dimension.DEPLOYMENT_OPTION ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	INSTANCE_TYPE ( "InstanceType", Dimension.INSTANCE_TYPE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	INSTANCE_TYPE_FAMILY ( "InstanceTypeFamily", Dimension.INSTANCE_TYPE_FAMILY ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13 (see also BILLING_ENTITY).
	*/
	INVOICING_ENTITY ( "InvoicingEntity", Dimension.INVOICING_ENTITY ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	LEGAL_ENTITY ( "LegalEntity", Dimension.LEGAL_ENTITY_NAME ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	LINKED_ACCOUNT ( "LinkedAccount", Dimension.LINKED_ACCOUNT ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	LINKED_ACCOUNT_NAME ( "LinkedAccountName", Dimension.LINKED_ACCOUNT_NAME ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	OPERATING_SYSTEM ( "OperatingSystem", Dimension.OPERATING_SYSTEM ),

	/**
	 * Is listed in the Cost Explorer web site but as "Purchase Option"?
	*/
	PAYMENT_OPTION ( "PaymentOption", Dimension.PAYMENT_OPTION ),

	/**
	 * Is listed in the Cost Explorer web site but as "Purchase Option"?
	*/
	PLATFORM ( "Platform", Dimension.PLATFORM ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	 * Indicates On-Demand or Reserved.
	*/
	PURCHASE_TYPE ( "PurchaseType", Dimension.PURCHASE_TYPE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	RECORD_TYPE ( "RecordType", Dimension.RECORD_TYPE ),

	/**
	 * Is listed in the Cost Explorer web site.
	 * AWS region.
	*/
	REGION ( "Region", Dimension.REGION ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	RESERVATION_ID ( "ReservationID", Dimension.RESERVATION_ID ),

	/**
	 * Is listed in the Cost Explorer web site as "Resource".
	*/
	RESOURCE_ID ( "ResourceID", Dimension.RESOURCE_ID ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	RIGHTSIZING_TYPE ( "RightsizingType", Dimension.RIGHTSIZING_TYPE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	SAVINGS_PLAN_ARN ( "SavingsPlanARN", Dimension.SAVINGS_PLAN_ARN ),

	/**
	 * Not listed in Cost Explorer web site?  In Javadoc but not in package?
	*/
	//SAVINGS_PLAN_TYPE ( "SavingsPlanType", Dimension.SAVINGS_PLAN_TYPE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	SCOPE ( "Scope", Dimension.SCOPE ),

	/**
	 * Is listed in the Cost Explorer web site as "Resource".
	 * AWS service (e.g., "Amazon EC2", "Amazon S3").
	*/
	SERVICE ( "Service", Dimension.SERVICE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	SERVICE_CODE ( "ServiceCode", Dimension.SERVICE_CODE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	SUBSCRIPTION_ID ( "SubscriptionID", Dimension.SUBSCRIPTION_ID ),

	/**
	 * Is listed in the Cost Explorer web site.
	 * Does not exactly match the SDK enumeration.
	*/
	TAG ( "Tag", Dimension.UNKNOWN_TO_SDK_VERSION ),

	/**
	 * Is listed in the Cost Explorer web site.
	*/
	TENANCY ( "Tenancy", Dimension.TENANCY ),

	/**
	 * This can be used for unknown dimension instead of null.
	*/
	UNKNOWN_TO_SDK_VERSION ( "Unknown", Dimension.UNKNOWN_TO_SDK_VERSION ),

	/**
	 * This can be used for unknown dimension instead of null.
	*/
	USAGE_TYPE ( "UsageType", Dimension.USAGE_TYPE ),

	/**
	 * Is not listed in the Cost Explorer web site as of 2024-01-13.
	*/
	USAGE_TYPE_GROUPE ( "UsageTypeGroup", Dimension.USAGE_TYPE_GROUP );

	/**
	The description, useful for UI notes.
	*/
	private String displayName;
	
	/**
	 * The AWS Cost Explorer dimension.
	 */
	private Dimension dimension;

	/**
	Construct an enumeration value.
	@param displayName name that can be displayed for the type
	*/
	private AwsBillingDimensionType ( String displayName, Dimension dimension ) {
    	this.displayName = displayName;
    	this.dimension = dimension;
	}

	/**
	Get the list of dimension types, in alphabetical order.
	Include dimensions that have been tested or are known to be useful.
	@return the list of command types.
	*/
	public static List<AwsBillingDimensionType> getChoices() {
    	List<AwsBillingDimensionType> choices = new ArrayList<>();
    	choices.add ( AwsBillingDimensionType.AVAILABILITY_ZONE );
    	choices.add ( AwsBillingDimensionType.REGION );
    	choices.add ( AwsBillingDimensionType.SERVICE );
    	choices.add ( AwsBillingDimensionType.TAG );
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
	Get the list of dimension type as strings, using MixedCase, rather than the AWS API upper case with underscores.
	The returned values closely match the Cost Explorer web site to facilitate comparison.
	@return the list of dimension types as strings
	*/
	public static List<String> getChoicesAsStrings () {
    	List<AwsBillingDimensionType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	AwsBillingDimensionType choice = choices.get(i);
        	String choiceString = "" + choice;
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}
	
	/**
	 * Return the SDK Dimension.
	 * @return the SDK Dimension
	 */
	public Dimension getDimension() {
		return this.dimension;
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
	public static AwsBillingDimensionType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	AwsBillingDimensionType [] values = values();
    	for ( AwsBillingDimensionType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}