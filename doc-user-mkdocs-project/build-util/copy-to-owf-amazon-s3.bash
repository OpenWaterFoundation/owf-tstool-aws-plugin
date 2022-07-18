#!/bin/bash
#
# Copy the site/* contents to the software.openwaterfoundation.org website
# - replace all the files on the web with local files
# - must specify Amazon profile as argument to the script

# Supporting functions, alphabetized

# Make sure the MkDocs version is consistent with the documentation content:
# - require that at least version 1.0 is used because of use_directory_urls = True default
# - must use "file.md" in internal links whereas previously "file" would work
# - it is not totally clear whether version 1 is needed but try this out to see if it helps avoid broken links
checkMkdocsVersion() {
  # Required MkDocs version is at least 1.
  requiredMajorVersion="1"
  # On Cygwin, mkdocs --version gives:  mkdocs, version 1.0.4 from /usr/lib/python3.6/site-packages/mkdocs (Python 3.6)
  # On Debian Linux, similar to Cygwin:  mkdocs, version 0.17.3
  if [ "${operatingSystem}" = "cygwin" -o "${operatingSystem}" = "linux" ]; then
    mkdocsVersionFull=$(mkdocs --version)
  elif [ "${operatingSystem}" = "mingw" ]; then
    mkdocsVersionFull=$(py -m mkdocs --version)
  else
    echo ""
    echo "Don't know how to run on operating system ${operatingSystem}"
    exit 1
  fi
  echo "MkDocs --version:  ${mkdocsVersionFull}"
  mkdocsVersion=$(echo ${mkdocsVersionFull} | cut -d ' ' -f 3)
  echo "MkDocs full version number:  ${mkdocsVersion}"
  mkdocsMajorVersion=$(echo ${mkdocsVersion} | cut -d '.' -f 1)
  echo "MkDocs major version number:  ${mkdocsMajorVersion}"
  if [ "${mkdocsMajorVersion}" -lt ${requiredMajorVersion} ]; then
    echo ""
    echo "MkDocs version for this documentation must be version ${requiredMajorVersion} or later."
    echo "MkDocs mersion that is found is ${mkdocsMajorVersion}, from full version ${mkdocsVersion}."
    exit 1
  else
    echo ""
    echo "MkDocs major version (${mkdocsMajorVersion}) is OK for this documentation."
  fi
}

# Determine the operating system that is running the script:
# - mainly care whether Cygwin or MINGW
checkOperatingSystem() {
  if [ ! -z "${operatingSystem}" ]; then
    # Have already checked operating system so return.
    return
  fi
  operatingSystem="unknown"
  os=$(uname | tr [a-z] [A-Z])
  case "${os}" in
    CYGWIN*)
      operatingSystem="cygwin"
      ;;
    LINUX*)
      operatingSystem="linux"
      ;;
    MINGW*)
      operatingSystem="mingw"
      ;;
  esac
}

# Check the source files for issues
# - the main issue is internal links need to use [](file.md), not [](file)
checkSourceDocs() {
  # Currently don't do anything but could check the above
  # Need one line to not cause an error
  :
}

# Determine which echo to use, needs to support -e to output colored text
# - normally built-in shell echo is OK, but on Debian Linux dash is used, and it does not support -e
configureEcho() {
  echo2='echo -e'
  testEcho=$(echo -e test)
  if [ "${testEcho}" = '-e test' ]; then
    # The -e option did not work as intended:
    # -using the normal /bin/echo should work
    # -printf is also an option
    echo2='/bin/echo -e'
    # The following does not seem to work
    #echo2='printf'
  fi

  # Strings to change colors on output, to make it easier to indicate when actions are needed
  # - Colors in Git Bash:  https://stackoverflow.com/questions/21243172/how-to-change-rgb-colors-in-git-bash-for-windows
  # - Useful info:  http://webhome.csc.uvic.ca/~sae/seng265/fall04/tips/s265s047-tips/bash-using-colors.html
  # - See colors:  https://en.wikipedia.org/wiki/ANSI_escape_code#Unix-like_systems
  # - Set the background to black to eensure that white background window will clearly show colors contrasting on black.
  # - Yellow "33" in Linux can show as brown, see:  https://unix.stackexchange.com/questions/192660/yellow-appears-as-brown-in-konsole
  # - Tried to use RGB but could not get it to work - for now live with "yellow" as it is
  warnColor='\e[1;40;93m' # user needs to do something, 40=background black, 33=yellow, 93=bright yellow
  errorColor='\e[0;40;31m' # serious issue, 40=background black, 31=red
  menuColor='\e[1;40;36m' # menu highlight 40=background black, 36=light cyan
  okColor='\e[1;40;32m' # status is good, 40=background black, 32=green
  endColor='\e[0m' # To switch back to default color
}

# Echo a string to standard error (stderr).
# This is done so that TSTool results output printed to stdout is not mixed with stderr.
# For example, TSTool may be run headless on a server to output to CGI,
# where stdout formatting is important.
echoStderr() {
  ${echo2} "$@" >&2
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

# Set the MkDocs executable to use, depending operating system and PATH:
# - sets the global ${mkdocsExe} variable
# - return 0 if the executable is found, exit with 1 if not
setMkDocsExe() {
  if [ "${operatingSystem}" = "cygwin" -o "${operatingSystem}" = "linux" ]; then
    # Is usually in the PATH.
    mkdocsExe="mkdocs"
    if hash py 2>/dev/null; then
      echo "mkdocs is not found (not in PATH)."
      exit 1
    fi
  elif [ "${operatingSystem}" = "mingw" ]; then
    # This is used by Git Bash:
    # - calling 'hash' is a way to determine if the executable is in the path
    if hash py 2>/dev/null; then
      mkdocsExe="py -m mkdocs"
    else
      # Try adding the Windows folder to the PATH and rerun:
      # - not sure why C:\Windows is not in the path in the first place
      PATH=/C/Windows:${PATH}
      if hash py 2>/dev/null; then
        mkdocsExe="py -m mkdocs"
      else
        echo 'mkdocs is not found in C:\Windows.'
        exit 1
      fi
    fi
  fi
  return 0
}

# Entry point into the script.

# Configure the echo command to output color.
configureEcho

# Check the operating system.
checkOperatingSystem

# Set the 'aws' program to use:
# - must set after the operating system is set
setAwsExe

# Set the MkDocs executable:
# - will exit if MkDocs is not found
setMkDocsExe

# Make sure the MkDocs version is OK.
checkMkdocsVersion

# Check the source files for issues.
checkSourceDocs

# Get the folder where this script is located since it may have been run from any folder.
scriptFolder=$(cd $(dirname "$0") && pwd)
repoFolder=$(dirname $(dirname ${scriptFolder}))
echo "Script folder = ${scriptFolder}"
# Change to the folder where the script is since other actions below are relative to that.
cd ${scriptFolder}

# Get the plugin version, which is used in the installer file name.
pluginVersion=$(getPluginVersion)
if [ -z "${pluginVersion}" ]; then
  echoStderr "[ERROR] ${errorColor}Unable to determine plugin version.${endColor}"
  exit 1
else
  echoStderr "[INFO] Plugin version:  ${pluginVersion}"
fi

# Set --dryrun to test before actually doing.
dryrun=""
#dryrun="--dryrun"
s3VersionFolder="s3://software.openwaterfoundation.org/tstool-aws-plugin/${pluginVersion}/doc-user"
s3LatestFolder="s3://software.openwaterfoundation.org/tstool-aws-plugin/latest/doc-user"

if [ "$1" == "" ]; then
  echo ""
  echo "Usage:  $0 AmazonConfigProfile"
  echo ""
  echo "Copy the site files to the Amazon S3 static website folders:"
  echo "  ${s3VersionFolder}"
  echo "  ${s3LatestFolder}"
  echo ""
  exit 0
fi

awsProfile="$1"

# First build the site so that the "site" folder contains current content:
# - "mkdocs serve" does not do this

echo "Building mkdocs-project/site folder..."
#cd ../mkdocs-project
cd ..
${mkdocsExe} build --clean
if [ $? -ne 0 ]; then
  echoStderr "[ERROR] Error running MkDocs."
  exit 1
fi
cd ${scriptFolder}

# Now sync the local files up to Amazon S3.
if [ -n "${pluginVersion}" ]; then
  # Upload documentation to the versioned folder.
  echo "Uploading documentation to:  ${s3VersionFolder}"
  read -p "Continue [Y/n/q]? " answer
  if [ -z "${answer}" -o "${answer}" = "y" -o "${answer}" = "Y" ]; then 
    echo ${awsExe} s3 sync ../site ${s3VersionFolder} ${dryrun} --delete --profile "${awsProfile}"
    ${awsExe} s3 sync ../site ${s3VersionFolder} ${dryrun} --delete --profile "${awsProfile}"
    exitStatusVersion=$?
  elif [ "${answer}" = "q" ]; then 
    exit 0
  fi

  # Also invalidate the CloudFront distribution so that new version will be displayed:
  # - see:  https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html
  # Determine the distribution ID:
  # The distribution list contains a line like the following (the actual distribution ID is not included here):
  # ITEMS   arn:aws:cloudfront::132345689123:distribution/E1234567891234    software.openwaterfoundation.org  something.cloudfront.net    True    HTTP2   E1234567891334  True    2022-01-06T19:02:50.640Z        PriceClass_100Deployed
  subdomain="software.openwaterfoundation.org"
  cloudFrontDistributionId=$(${awsExe} cloudfront list-distributions --output text --profile "${awsProfile}" | grep ${subdomain} | grep "arn" | awk '{print $2}' | cut -d ':' -f 6 | cut -d '/' -f 2)
  if [ -z "${cloudFrontDistributionId}" ]; then
    logError "Unable to find CloudFront distribution ID."
    exit 1
  else
    logInfo "Found CloudFront distribution ID: ${cloudFrontDistributionId}"
  fi
  logInfo "Invalidating files so that CloudFront will make new files available..."
  ${awsExe} cloudfront create-invalidation --distribution-id ${cloudFrontDistributionId} --paths "/tstool-aws-plugin/${pluginVersion}/doc-user/*" --profile "${awsProfile}"
  errorCode=$?
  if [ $errorCode -ne 0 ]; then
    logError " "
    logError "Error invalidating CloudFront file(s)."
    exit 1
  else
    logInfo "Success invalidating CloudFront file(s)."
  fi
fi

read -p "Also copy documentation to 'latest' [y/n/q]? " answer
exitStatusLatest=0
if [ "${answer}" = "y" ]; then 
  echo "Uploading documentation to:  ${s3LatestFolder}"
  read -p "Continue [Y/n/q]? " answer
  if [ -z "${answer}" -o "${answer}" = "y" -o "${answer}" = "Y" ]; then 
    ${awsExe} s3 sync ../site ${s3LatestFolder} ${dryrun} --delete --profile "${awsProfile}"
    exitStatusLatest=$?
  elif [ "${answer}" = "q" ]; then 
    exit 0
  fi

  # Also invalidate the CloudFront distribution so that new version will be displayed:
  # - see:  https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html
  # Determine the distribution ID:
  # The distribution list contains a line like the following (the actual distribution ID is not included here):
  # ITEMS   arn:aws:cloudfront::132345689123:distribution/E1234567891234    software.openwaterfoundation.org  something.cloudfront.net    True    HTTP2   E1234567891334  True    2022-01-06T19:02:50.640Z        PriceClass_100Deployed
  subdomain="software.openwaterfoundation.org"
  cloudFrontDistributionId=$(${awsExe} cloudfront list-distributions --output text --profile "${awsProfile}" | grep ${subdomain} | grep "arn" | awk '{print $2}' | cut -d ':' -f 6 | cut -d '/' -f 2)
  if [ -z "${cloudFrontDistributionId}" ]; then
    logError "Unable to find CloudFront distribution ID."
    exit 1
  else
    logInfo "Found CloudFront distribution ID: ${distributionId}"
  fi
  logInfo "Invalidating files so that CloudFront will make new files available..."
  ${awsExe} cloudfront create-invalidation --distribution-id ${cloudFrontDistributionId} --paths "/tstool-aws-plugin/latest/doc-user/*" --profile "${awsProfile}"
  errorCode=$?
  if [ $errorCode -ne 0 ]; then
    logError " "
    logError "Error invalidating CloudFront file(s)."
    exit 1
  else
    logInfo "Success invalidating CloudFront file(s)."
  fi
fi

exitStatus=$(( ${exitStatusVersion} + ${exitStatusLatest} ))
