# TSTool / Getting Started #

This chapter provides an overview of getting started with TSTool and the AWS plugin commands.

*   [Introduction](#introduction)
*   [Installing and Configuring TSTool](#installing-and-configuring-tstool)
*   [Starting TSTool](#starting-tstool)
*   [Installing the AWS Plugin](#installing-the-aws-plugin)
*   [Using the AWS Plugin Commands](#using-the-aws-plugin-commands)

----------------

## Introduction ##

The TSTool user interface (UI) provides features to browse data, create command file workflows to process data,
and view time series and other information products.
The TSTool UI design follows several basic concepts and interactions.

*   See the [TSTool Documentation Getting Started](https://opencdss.state.co.us/tstool/latest/doc-user/getting-started/getting-started/)
    documentation for core product information.

The remainder of this documentation focuses on getting started with TSTool using the AWS plugin.

## Installing and Configuring TSTool ##

If not already done, [install TSTool](https://opencdss.state.co.us/tstool/latest/doc-user/appendix-install/install/).
TSTool is most often installed on Windows but also runs on Linux.
Contact the Open Water Foundation if a Linux version is required.

## Starting TSTool ##

TSTool can be run in interactive mode and batch (command line) mode.
Interactive mode is typically used to prototype and configure automated workflows and products.
Batch mode can be used to run the software in headless mode on a server,
for example on a schedule or in response to web requests.

When using the State of Colorado’s TSTool default configuration on Windows,
the software can be started using ***Start / Programs / CDSS / TSTool-Version***.
The menus vary slightly depending on the operating system version.

Use the `tstool` script to start the software on Linux.
The program can be found in the software's `/bin` folder.
Multiple versions of TSTool can be installed to facilitate transition from older to newer versions.

To process a command file in batch mode without showing the user interface,
use a command line similar to the following:

```
tstool -–commands commands.tstool
```

It is customary to name command files with a `.tstool` file extension.
It may be necessary to specify a full (absolute) path to the command file when
running in batch mode in order for TSTool to fully understand the working directory.

See the [Running TSTool in Various Modes appendix](https://opencdss.state.co.us/tstool/latest/doc-user/appendix-running/running/)
for more information about running in batch and other modes.

## Installing the AWS Plugin ##

See the [Appendix - Install Plugin](../appendix-install/install.md) documentation for instructions to install the plugin.

## Using the AWS Plugin Commands ##

TSTool AWS plugin commands are provided for commonly-used AWS features.
Additional commands will be added in the future as needed.
The primary uses of the plugin commands are:

*   streamline publishing TSTool output products to the cloud, for use in web applications and downloads
*   integrating TSTool with larger workflows and systems

The following is a summary of plugin commands,
which are listed in the ***Commmands(Plugin)*** menu.

**<p style="text-align: center;">
AWS Plugin Commands
</p>**

| **Command**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | **Description** |
| -- | -- |
| [`AwsS3`](../command-ref/AwsS3/AwsS3.md) | Perform S3 tasks: <ul><li>copy bucket objects</li><li>delete bucket objects</li><li>list buckets</li><li>list bucket directories</li><li>upload files and folders</li><li>download files and folders</li></ul>|
| [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md) | Create a catalog (listing) of datasets or other products as a landing page to access the products. The command is envisioned to create landing pages for single products (e.g., dataset with multiple versiosn) and multiple products (e.g., a catalog of all datasets). |
| [`CloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) | Perform CloudFront tasks: <ul><li>list distributions</li><li>invalidate distribution files</li><li>list invalidations</li></ul> |
