# TSTool / Install AWS Plugin #

This appendix describes how to install the AWS Plugin.

*   [Overview](#overview)
*   [Install TSTool](#install-tstool)
*   [Install and Configure the TSTool AWS Plugin](#install-and-configure-the-tstool-aws-plugin)

-------

## Overview ##

TSTool is used with AWS to automate file uploads and downloads.
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

TSTool must have been previously installed and run at least once.

1.  Download the `tstool-aws-plugin` software installer file from the
    [TSTool AWS Download page](https://software.openwaterfoundation.org/tstool-aws-plugin/).
    For example with a name similar to `tstool-aws-plugin-1.1.1-win-202212021709.zip`.
2.  If installing the plugin in user files and if TSTool was not run before,
    run TSTool once to automatically create user folders and files needed by the plugin.
3.  The plugin installation folder is as follows, where `C:\Users\user` should match the specific user's folder:
    1.  If installing in user files (`NN` is the TSTool major version):
        *   Windows: `C:\Users\user\.tstool\NN\plugins\`
        *   Linux (`~` indicates the user's home folder): `~/.tstool/NN/plugins/`
    2.  If installing in system files on Linux (`opt`), install in the following folder:
        *   Linux: `/opt/tstool-version/plugins/`
4.  If an old version of the plugin was previous installed:
    1.  Delete the old `jar` file or move to the `plugins-old/` folder (same level as the `plugins` folder) to archive.
        Only one copy of the plugin `jar` file can be found in the `plugins` folder to avoid software conflicts.
    2.  Similarly, the contents of the `dep/` folder should be deleted or moved before installing the plugin.
        These files are required by the AWS plugin software and multiple versions of the files will confuse the plugin software.
5.  Copy the `owf-tstool-aws-plugin` folder from the zip file to the following folder:
    1.  If installing in user files (`NN` is the TSTool major version):
        *   Windows: `C:\Users\user\.tstool\NN\plugins\`
        *   Linux (`~` indicates the user's home folder): `~/.tstool/NN/plugins/`
    2.  If installing in system files on Linux (`opt`), install in the following folder:
        *   Linux: `/opt/tstool-version/plugins/`
6.  Restart TSTool and test the commands.
    Try a simple command like using the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command to list S3 buckets.
7.  Troubleshooting:
    1.  If the AWS plugin features are not functioning properly, it may be due to conflicting jar files.
        The ***Commands(Plugin)*** menu will usually contain duplicate menus if
        multiple `jar` files with different versions are found in the `plugins` folder.
    2.  See the [Troubleshooting](../troubleshooting/troubleshooting.md) documentation.

After installing, the plugin software folders should be similar to the following,
where `C:\Users\user` is the specific user's folder.
```
C:\Users\user\.tstool\plugins\owf-tstool-aws-plugin\
  owf-tstool-aws-plugin-1.1.1.jar
  dep\
    *.jar (many files)
```
