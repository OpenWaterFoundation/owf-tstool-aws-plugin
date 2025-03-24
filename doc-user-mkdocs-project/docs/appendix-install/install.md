# TSTool / Install AWS Plugin #

This appendix describes how to install the AWS Plugin.

*   [Overview](#overview)
*   [Install TSTool](#install-tstool)
*   [Install and Configure the TSTool AWS Plugin](#install-and-configure-the-tstool-aws-plugin)

-------

## Overview ##

TSTool is used with AWS to automate file uploads, downloads, and other tasks.
The TSTool AWS plugin is developed and tested on Windows but can also be installed on Linux.

## Install TSTool ##

TSTool must be installed before installing the AWS plugin.
Typically the latest stable release should be used, although a development version can be installed
in order to use new features.
Multiple versions of TSTool can be installed at the same time.

1.  Download TSTool:
    *   Download the Windows version from the
        [State of Colorado's TSTool Software Downloads](https://opencdss.state.co.us/tstool/) page.
    *   Download the Linux version from the
        [Open Water Foundation TSTool download page](https://software.openwaterfoundation.org/tstool/).
2.  Run the installer and accept defaults.
3.  Run TSTool once by using the ***Start / CDSS / TSTool-Version*** menu on Windows
    (or run the `tstool` program on Linux).
    This will automatically create folders needed to install the plugin.

## Install and Configure the TSTool AWS Plugin ##

The plugin installation folder structure is as follows and is explained below.
The convention of using a version folder (e.g., `2.0.0`) was introduced in TSTool 15.0.0.

```
C:\Users\user\.tstool\NN\plugins\owf-tstool-aws-plugin\    (Windows)
/home/user/.tstool/NN/plugins/owf-tstool-aws-plugin/       (Linux)
  1.5.7/
    owf-tstool-aws-plugin-1.5.7.jar
    dep/
      *.jar (many files)
  2.0.0/
    owf-tstool-aws-plugin-1.5.7.jar
    dep/
      *.jar (many files)
```

To install the plugin:

1.  TSTool must have been previously installed and run at least once.
    This will ensure that folders are properly created and, if appropriate,
    a previous version's files will be copied to a new major version run for the first time.
2.  Download the TSTool AWS Plugin software installer file from the
    [TSTool AWS Download page](https://software.openwaterfoundation.org/tstool-aws-plugin/).
    The installer will have a name similar to `tstool-aws-plugin-2.0.0-win-202503241451.zip`.
3.  The plugin installation folders are as shown above.
    If installing the plugin in system files on Linux, install in the following folder:
    `/opt/tstool-version/plugins/`
4.  If an old version of the plugin was previous installed and does not exist in a version folder:
    1.  Create a folder with the version (e.g., `1.2.3`) consistent with the software
        and move the files into the folder.
        The files will be available to TSTool versions that are compatible.
    2.  Delete the files if not needed.
5.  Copy files from the `zip` file to the `owf-tstool-aws-plugin` folder as shown in the above example:
    *   Windows:  Use File Explorer, 7-Zip, or other software to extract files.
    *   Linux:  Unzip the `zip` file to a temporary folder and copy the files.
6.  Restart TSTool and test the commands.
    Try a simple command like using the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command to list S3 buckets.
7.  Troubleshooting:
    1.  If the AWS plugin features are not functioning properly, it may be due to conflicting jar files.
        TSTool versions before 15.0.0 may show duplicate commands in the ***Commands(Plugin)*** menu.
    2.  See the [Troubleshooting](../troubleshooting/troubleshooting.md) documentation.
8.  For TSTool 15.0.0 and later, use the TSTool ***Tools / Plugin Manager*** menu to review installed plugins.
