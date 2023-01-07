# TSTool / Troubleshooting #

Troubleshooting TSTool for AWS involves confirming that the core product and plugin are performing as expected.

*   [Troubleshooting Core TSTool Product](#troubleshooting-core-tstool-product)
*   [Troubleshooting AWS TSTool Integration](#troubleshooting-aws-tstool-integration)
    +   [***Commands(Plugin)*** Menu Contains Duplicate Commands](#commandsplugin-menu-contains-duplicate-commands)
    +   [Authentication and permission problems](#authentication-and-permission-problems)

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
