#!/bin/bash
#
# Copy the latest plugin installer matching the plugin version to the software.openwaterfoundation.org website
# - replace the installer file on the web with local files
# - also update the catalog file that lists available plugin downloaders
# - must specify Amazon profile as argument to the script
# - DO NOT FORGET TO CHANGE THE programVersion and programVersionDate when making changes

# Supporting functions, alphabetized

# Check input
# - make sure that the Amazon profile was specified
checkInput() {
  if [ -z "$awsProfile" ]; then
    logError ""
    logError "Amazon profile to use for upload was not specified with --aws-profile option.  Exiting."
    printUsage
    exit 1
  fi
}

# Determine the operating system that is running the script
# - sets the variable operatingSystem to 'cygwin', 'linux', or 'mingw' (Git Bash)
# - sets the variable operatingSystemShort to 'cyg', 'lin', or 'min' (Git Bash)
checkOperatingSystem() {
  if [ ! -z "${operatingSystem}" ]; then
    # Have already checked operating system so return
    return
  fi
  operatingSystem="unknown"
  os=$(uname | tr [a-z] [A-Z])
  case "${os}" in
    CYGWIN*)
      operatingSystem="cygwin"
      operatingSystemShort="cyg"
      ;;
    LINUX*)
      operatingSystem="linux"
      operatingSystemShort="lin"
      ;;
    MINGW*)
      operatingSystem="mingw"
      operatingSystemShort="min"
      ;;
  esac
  logInfo ""
  logInfo "Detected operatingSystem=$operatingSystem operatingSystemShort=$operatingSystemShort"
  logInfo ""
}

# Echo to stderr
# - if necessary, quote the string to be printed
# - this function is called to print various message types
echoStderr() {
  echo "$@" 1>&2
}

# Get the plugin version (e.g., 1.2.0):
# - the version is printed to stdout so assign function output to a variable
getPluginVersion() {
  local srcFile

  # Maven folder structure results in duplicate 'owf-tstool-aws-plugin'?
  # TODO smalers 2022-05-19 need to enable this.
  srcFile="${repoFolder}/owf-tstool-aws-plugin/src/main/java/org/openwaterfoundation/tstool/plugin/aws/Aws.java"  
  # Get the version from the code
  # line looks like:
  #  public static final String VERSION = "1.0.0 (2022-05-27)";
  if [ -f "${srcFile}" ]; then
    cat ${srcFile} | grep 'VERSION =' | cut -d '"' -f 2 | cut -d ' ' -f 1 | tr -d '"' | tr -d ' '
  else
    # Don't echo error to stdout.
    echoStderr "[ERROR] Source file with version does not exist:"
    echoStderr "[ERROR]   ${srcFile}"
    cat ""
  fi
}

# Print a DEBUG message, currently prints to stderr.
logDebug() {
   echoStderr "[DEBUG] $@"
}

# Print an ERROR message, currently prints to stderr.
logError() {
   echoStderr "[ERROR] $@"
}

# Print an INFO message, currently prints to stderr.
logInfo() {
   echoStderr "[INFO] $@"
}

# Print an WARNING message, currently prints to stderr.
logWarning() {
   echoStderr "[WARNING] $@"
}

# Parse the command parameters:
# - use the getopt command line program so long options can be handled
parseCommandLine() {
  # Single character options.
  optstring="hv"
  # Long options.
  optstringLong="aws-profile::,dryrun,help,include-cygwin::,include-mingw::,include-windows::,version"
  # Parse the options using getopt command.
  GETOPT_OUT=$(getopt --options $optstring --longoptions $optstringLong -- "$@")
  exitCode=$?
  if [ $exitCode -ne 0 ]; then
    # Error parsing the parameters such as unrecognized parameter.
    echoStderr ""
    printUsage
    exit 1
  fi
  # The following constructs the command by concatenating arguments.
  eval set -- "$GETOPT_OUT"
  # Loop over the options.
  while true; do
    #logDebug "Command line option is ${opt}".
    case "$1" in
      --aws-profile) # --aws-profile=profile  Specify the AWS profile (use default).
        case "$2" in
          "") # Nothing specified so error.
            logError "--aws-profile=profile is missing profile name"
            exit 1
            ;;
          *) # profile has been specified.
            awsProfile=$2
            shift 2
            ;;
        esac
        ;;
      --dryrun) # --dryrun  Indicate to AWS commands to do a dryrun but not actually upload.
        logInfo "--dryrun dectected - will not change files on S3"
        dryrun="--dryrun"
        shift 1
        ;;
      -h|--help) # -h or --help  Print the program usage.
        printUsage
        exit 0
        ;;
      --include-cygwin) # --include-cygwin=yes|no  Include the Cygwin installer for upload.
        case "$2" in
          "") # Nothing specified so default to yes.
            includeCygwin="yes"
            shift 2
            ;;
          *) # yes or no has been specified.
            includeCygwin=$2
            shift 2
            ;;
        esac
        ;;
      --include-mingw) # --include-mingw=yes|no  Include the MinGW installer for upload.
        case "$2" in
          "") # Nothing specified so default to yes.
            includeMingw="yes"
            shift 2
            ;;
          *) # yes or no has been specified.
            includeMingw=$2
            shift 2
            ;;
        esac
        ;;
      --include-windows) # --include-windows=yes|no  Include the Windows installer for upload (not just Cygwin).
        case "$2" in
          "") # Nothing specified so default to yes.
            includeWindows="yes"
            shift 2
            ;;
          *) # yes or no has been specified.
            includeWindows=$2
            shift 2
            ;;
        esac
        ;;
      -v|--version) # -v or --version  Print the program version.
        printVersion
        exit 0
        ;;
      --) # No more arguments.
        shift
        break
        ;;
      *) # Unknown option.
        logError "" 
        logError "Invalid option $1." >&2
        printUsage
        exit 1
        ;;
    esac
  done
}

# Print the program usage to stderr:
# - calling code must exit with appropriate code
printUsage() {
  echoStderr ""
  echoStderr "Usage:  ${programName} --aws-profile=profile"
  echoStderr ""
  echoStderr "Copy the TSTool AWS plugin installer files to the Amazon S3 static website folder:"
  echoStderr "  ${s3FolderUrl}"
  echoStderr "The latest installer matching the current plugin version is uploaded."
  echoStderr ""
  echoStderr "--aws-profile=profile   Specify the Amazon profile to use for AWS credentials."
  echoStderr "--dryrun                Do a dryrun but don't actually upload anything."
  echoStderr "-h or --help            Print the usage."
  echoStderr "--include-cygwin=no     Turn off Cygwin upload (default is include if available)."
  echoStderr "--include-mingw=no      Turn off MinGW upload (default is include if available)."
  echoStderr "--include-window=no     Turn off Windows upload (default is include if available)."
  echoStderr "-v or --version         Print the version and copyright/license notice."
  echoStderr ""
}

# Print the script version and copyright/license notices to stderr.
# - calling code must exit with appropriate code
printVersion() {
  echoStderr ""
  echoStderr "${programName} version ${programVersion} ${programVersionDate}"
  echoStderr ""
  echoStderr "TSTool AWS Plugin"
  echoStderr "Copyright 2022 Open Water Foundation."
  echoStderr ""
  echoStderr "License GPLv3+:  GNU GPL version 3 or later"
  echoStderr ""
  echoStderr "There is ABSOLUTELY NO WARRANTY; for details see the"
  echoStderr "'Disclaimer of Warranty' section of the GPLv3 license in the LICENSE file."
  echoStderr "This is free software: you are free to change and redistribute it"
  echoStderr "under the conditions of the GPLv3 license in the LICENSE file."
  echoStderr ""
}

# Set the 'aws' executable based on the operating system.
# This allows the rest of the script to call the program without issue.
setAwsExe () {
  awsExe=""
  if [ -z "${operatingSystem}" ]; then
    logError "The opererating system is unknown.  Cannot determine 'aws' program.  Exiting."
    exit 1
  elif [ "${operatingSystem}" = "cygwin" -o "${operatingSystem}" = "linux" ]; then
    # aws is in a standard location such as /usr/bin/aws and is found via the PATH.
    awsExe="aws"
  elif [ "${operatingSystem}" = "mingw" ]; then
    # For Windows Python 3.7, aws may be installed in Windows %USERPROFILE%\AppData\Local\Programs\Python\Python37\scripts
    # - use Linux-like path to avoid backslash issues
    # - TODO smalers 2019-01-04 could try to find where py thinks Python is installed but not sure how
    awsExe="${HOME}/AppData/Local/Programs/Python/Python37/scripts/aws"
  else
    logError ""
    logError "Don't know how to run on operating system ${operatingSystem}"
    exit 1
  fi
  # Make sure that the command is found.
  if [ -z "${awsExe}" ]; then
    logError "The opererating system is unknown.  Cannot determine 'aws' program.  Exiting."
  elif ! command -v "${awsExe}"; then
    logError "Could not find 'aws' program: ${awsExe}"
    exit 1
  else
    logInfo "Found 'aws': ${awsExe}"
    return 0
  fi
}

# Update the Amazon S3 index that lists files for download:
# - this calls the create-s3-index.bash script
updateIndex() {
  local answer
  echo ""
  read -p "Do you want to update the S3 index file [Y/n]? " answer
  if [ -z "$answer" -o "$answer" = "y" -o "$answer" = "Y" ]; then
    # TODO smalers 2020-04-06 comment out for now.
    if [ -z "${awsProfile}" ]; then
      # No AWS profile given so rely on default.
      ${scriptFolder}/create-s3-index.bash
    else
      # AWS profile given so use it.
      ${scriptFolder}/create-s3-index.bash --aws-profile=${awsProfile}
    fi
  fi
}

# Upload local installer files to Amazon S3
# - includes the most recent .zip file for the current plugin version
uploadInstaller() {
  # The location of the installer is
  # ===========================================================================
  # Step 1. Upload the installer file for the current version
  #         - use copy to force upload
  # The following handles Cygwin, Linux, and MinGW uploads
  # TODO smalers 2020-04-06 not sure what the following is doing
  # includeNix="yes"
  # if [ "$operatingSystem" = "cygwin" ]; then
  #   # On Cygwin, can turn off
  #   includeNix=$includeCygwin
  # elif [ "$operatingSystem" = "linux" ]; then
  #   # On Linux, can turn off
  #   includeNix="$includeLinux"
  # elif [ "$operatingSystem" = "mingw" ]; then
  #   # On MinGW, can turn off
  #   includeNix="$includeMingw"
  # fi

  # Get the latest zip file for the plugin version.
  # Count of successful installer uploads.
  installerUploadCount=0
    logInfo "Processing installers for plugin version=${pluginVersion}"
    if [ "${includeNix}" = "yes" ]; then
      # File for the tar.gz file (Linux variants):
      # - TODO smalers 2020-04-06 disable gptest for now since not actively being developed
      # virtualenvGptestTargzPath="$virtualenvTmpFolder/gptest-${gpVersion}-${operatingSystemShort}-qgis-${qgisVersion}venv.tar.gz"
      # virtualenvGptestTargzFile=$(basename $virtualenvGptestTargzPath)
      logInfo "  [INFO] Uploading Linux plugin installation file(s) is currently disabled."
      # logInfo "Uploading available (if any) plugin installation file(s) for $operatingSystem..."
      # TODO smalers 2020-04-06 disable gptest for now since not currently under development - was only available on Linux.
      # # set -x
      # s3virtualenvGptestTargzUrl="${s3FolderUrl}/$gpVersion/$virtualenvGptestTargzFile"
      # if [ ! -f "$virtualenvGptestTargzPath" ]; then
      #  echo ""
      #  echo "Installer file does not exist:  $virtualenvGptestTargzPath"
      #  exit 1
      #fi
      #aws s3 cp $virtualenvGptestTargzPath $s3virtualenvGptestTargzUrl ${dryrun} --profile "$awsProfile" ; errorCode=$?
      # { set +x; } 2> /dev/null
      #if [ $errorCode -ne 0 ]; then
      #  logError ""
      #  logError "[Error] Error uploading plugin installer file for $operatingSystem."
      #  logError "        Use --include-${operatingSystemShort}=no to ignore installer upload for $operatingSystem."
      #  continue
      #else
      #  installerUploadCount=$(expr ${installerUploadCount} + 1)
      #fi
    else
      logInfo "  Skipping uploading plugin installation file(s) for ${operatingSystem} because Linux omitted."
      sleep 1
    fi

    # The following handles Windows upload when run on Cygwin.
    if [ "$includeWindows" = "yes" ]; then
      # File for the zip file (Windows).
      zipFilePattern="${distFolder}/tstool-aws-plugin-${pluginVersion}-win-*.zip"
      logInfo "Zip file pattern is: ${zipFilePattern}"
      # Get the latest installer for the version.
      latestZipFile=$(ls -1 ${zipFilePattern} | sort -r | head -1)
      # File for S3.
      s3ZipUrl="${s3FolderUrl}/${pluginVersion}/software/$(basename ${latestZipFile})"
      if [ -f "${latestZipFile}" ]; then
        logInfo "  Uploading Windows installer using following command:"
        logInfo "    ${awsExe} s3 cp ${latestZipFile} ${s3ZipUrl} ${dryrun} --profile \"${awsProfile}\""
        # Set this in case aws command is commented out, so 'if' statement below continues to work.
        # errorCode=0
        ${awsExe} s3 cp ${latestZipFile} ${s3ZipUrl} ${dryrun} --profile "${awsProfile}"
        errorCode=$?
        # { set +x; } 2> /dev/null
        if [ ${errorCode} -ne 0 ]; then
          logError ""
          logError "    [ERROR ${errorCode}] Error uploading plugin installer file for Windows."
          logError "              Use --include-win=no to ignore Windows installer for upload."
        else
          installerUploadCount=$(( ${installerUploadCount} + 1 ))
        fi
      else
        logError "  Windows zip file does not exist: ${latestZipFile}"
      fi
    else
      logInfo "  Skip uploading the plugin installation file for Windows because Window omitted."
      sleep 1
    fi
}

# Entry point into script.

# Get the location where this script is located since it may have been run from any folder.
programName=$(basename $0)
programVersion="1.0.0"
programVersionDate="2022-06-08"

# Check the operating system:
# - used to make logic decisions and for some file/folder names so do first
checkOperatingSystem

# Define top-level folders - everything is relative to this below to avoid confusion.
scriptFolder=$(cd $(dirname "$0") && pwd)
buildUtilFolder=${scriptFolder}
repoFolder=$(dirname ${buildUtilFolder})
distFolder="${repoFolder}/dist"
scriptsFolder="${repoFolder}/scripts"

# Get the current plugin version number from development environment files.
pluginVersion=$(getPluginVersion)

# Set the 'aws' program to use:
# - must set after the operating system is set
setAwsExe

# Root AWS S3 location where files are to be uploaded.
s3FolderUrl="s3://software.openwaterfoundation.org/tstool-aws-plugin"
gpDownloadUrl="https://software.openwaterfoundation.org/tstool-aws-plugin"

# Defaults for whether operating systems are included in upload:
# - default is to upload all but change when Windows is not involved since won't be on the machine
includeCygwin="yes"
includeLinux="yes"
includeMingw="yes"
includeWindows="yes"
if [ "${operatingSystem}" = "linux" ]; then
  includeWindows="no"
fi

# Parse the command line.
# Specify AWS profile with --aws-profile
awsProfile=""
# Default is not to do 'aws' dry run
# - override with --dryrun
dryrun=""
parseCommandLine "$@"

# Check input:
# - check that Amazon profile was specified
checkInput

# Upload the installer file(s) to Amazon S3:
# - depends on current plugin version
uploadInstaller

# Also update the index, which lists all available installers that exist on S3.
# -installerUploadCount is set in uploadInstaller()
if [ "${installerUploadCount}" -eq 0 ]; then
  logInfo ""
  logInfo "No installers were uploaded - not updating the catalog."
  logInfo "Run create-s3-index.bash separately if necessary."
else
  # Update the index for the installer
  updateIndex
fi

# Print useful information to use after running the script.
logInfo ""
logInfo "${installerUpdateCount} TSTool AWS plugin installers were uploaded to Amazon S3 location:"
logInfo ""
logInfo "  ${s3FolderUrl}"
logInfo "  ${gpDownloadUrl}"
logInfo ""
logInfo "Next steps are to do the following:"
logInfo "  - Visit the following folder to download the TSTool AWS plugin:"
logInfo "      ${gpDownloadUrl}/"
logInfo ""

exit 0
