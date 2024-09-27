# TSTool / Troubleshooting #

Troubleshooting TSTool for AWS involves confirming that the core product and plugin are performing as expected.

*   [Troubleshooting Core TSTool Product](#troubleshooting-core-tstool-product)
*   [Troubleshooting AWS TSTool Integration](#troubleshooting-aws-tstool-integration)
    +   [***Commands(Plugin)*** Menu Contains Duplicate Commands](#commandsplugin-menu-contains-duplicate-commands)
    +   [Authentication and permission problems](#authentication-and-permission-problems)
    +   [`Not authorized` message in the TSTool log file](#not-authorized-message-in-the-tstool-log-file)

------------------

## Troubleshooting Core TSTool Product ##

See the main [TSTool Troubleshooting documentation](https://opencdss.state.co.us/tstool/latest/doc-user/troubleshooting/troubleshooting/).

## Troubleshooting AWS TSTool Integration ##

The following are typical issues that are encountered when using TSTool with AWS.

### ***Commands(Plugin)*** Menu Contains Duplicate Commands ###

If the ***Commands(Plugin)*** menu contains duplicate commands,
TSTool is finding multiple plugin `jar` files.
To fix, check the `plugins` folder and subfolders for the software installation folder
and the user's `.tstool/NN/plugins` folder.
Remove extra jar files, leaving only the version that is desired (typically the most recent version).

If necessary, completely remove plugin files and re-install the plugin to avoid confusion about mixing
software files from multiple versions.

### Authentication and permission problems ###

The TSTool AWS plugin uses desktop software features.
Consequently, TSTool authenticates similar to the
[AWS command line interface (CLI)](https://aws.amazon.com/cli/).
It is not necessary that the AWS CLI is installed to use the TSTool AWS plugin;
however, the AWS CLI configuration files must be in place.
Installing the AWS CLI is useful for troubleshooting.

1.  Optionally, install the AWS CLI software.
2.  Configure the AWS CLI credentials using information provided by the organization's AWS administrator.
3.  If the AWS CLI is installed, try running a simple command such as `aws s3 ls`
    ([see documentation](https://docs.aws.amazon.com/cli/latest/reference/s3/ls.html)).
    If this works, then the TSTool AWS plugin authentication should be OK.
4.  If the plugin command has issues with permissions, 
    verify the command parameter input and check with the organization's AWS administrator to make sure
    that AWS user, group, and policy configuration allows access to the files.

### `Not authorized` message in the TSTool log file ###

New plugin software features may rely on AWS service permissions as defined in the policies associated with the user running the workflow.
It may be necessary to modify policies for the user.
For example, the following log file error is typical and in this case the EC2 `DescribeRegions` permission must be granted.
The solution is to use the AWS console and adjust policy permissions to allow the service to be used.
Policies should only provide enough access necessary for the user and workflows.

```
software.amazon.awssdk.services.ec2.model.Ec2Exception: You are not authorized to perform this operation. User: arn:aws:iam::xxxxxxx:user/xxxxxxx is not authorized to perform: ec2:DescribeRegions because no identity-based policy allows the ec2:DescribeRegions action (Service: Ec2, Status Code: 403, Request ID: 96ba8eca-b185-48bd-a332-eb402fee9853)
        at software.amazon.awssdk.core.internal.http.CombinedResponseHandler.handleErrorResponse(CombinedResponseHandler.java:125)
        at software.amazon.awssdk.core.internal.http.CombinedResponseHandler.handleResponse(CombinedResponseHandler.java:82)
        at software.amazon.awssdk.core.internal.http.CombinedResponseHandler.handle(CombinedResponseHandler.java:60)
        at software.amazon.awssdk.core.internal.http.CombinedResponseHandler.handle(CombinedResponseHandler.java:41)
```
