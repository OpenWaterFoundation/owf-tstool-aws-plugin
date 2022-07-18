# TSTool / Install AWS Plugin #

This appendix describes how to install the AWS Plugin.

* [Overview](#overview)
* [Install TSTool](#install-tstool)
* [Install and Configure the TSTool AWS Plugin](#install-and-configure-the-tstool-aws-plugin)

-------

## Overview ##

TSTool is used with AWS to automate file uploads and downloads.
The TSTool AWS plugin is currently only available for Windows
and must be installed after the TSTool software is installed.

Multiple versions of TSTool can be installed at the same time, which allows transition during software updates.

## Install TSTool ##

See the core product [Install TSTool appendix](https://opencdss.state.co.us/tstool/latest/doc-user/appendix-install/install/)
for general information about installing TSTool.
The core product must be installed first before plugins are installed.

## Install and Configure the TSTool AWS Plugin ##

The AWS plugin software is currently installed by unzipping the installation file.
The plugin is currently only available for Windows.

Download the TSTool AWS Plugin from the [OWF Software page](https://software.openwaterfoundation.org/tstool-aws-plugin/),
for example saving in the `Downloads` folder on Windows.
Use a tool such as [7zip](https://www.7-zip.org/) or open the zip file using Windows File Explorer to see its contents.
Unzip (or copy) the zip file contents to the folder `C:\Users\user\.tstool\14\plugins\owf-tstool-aws-plugin` similar to the following,
where `user` should be replaced with the specific user's login:

```
C:\Users\user\.tstool\plugins\owf-tstool-aws-plugin\
  owf-tstool-aws-plugin-1.0.0.jar
  dep\
    *.jar (many files)
```

If the `plugins` folder does not exist, make sure to run TSTool at least one time to automatically create the folders
before installing the plugin.

If a previous version of the plugin was installed, remove the files before installing.

Restart TSTool.
The plugin commands described in the next section should be listed in the ***Commmands(Plugin)*** menu.

TSTool will be enhanced in the future to provide a "plugin manager" to help with these tasks.
