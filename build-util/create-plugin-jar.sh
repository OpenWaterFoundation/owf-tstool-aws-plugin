#!/bin/sh
#
# Create the plugin jar file for installation in the deployed system
# - the class files and manifest are jar'ed up
# - the resulting Jar file is created in the user's (developer's)
#   ./tstool/NN/plugins/owf-tstool-aws-plugin/ folder for use by TSTool

# Supporting functions, alphabetized.

# Determine the operating system that is running the script:
# - mainly care whether Cygwin
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
  echo "operatingSystem=${operatingSystem} (used to check for Cygwin and filemode compatibility)"

  if [ "${operatingSystem}" != "mingw" ]; then
    ${echo2} "${errorColor}Currently this script only works for MINGW (Git Bash)${endColor}"
    exit 1
  fi
}

# Determine which echo to use, needs to support -e to output colored text
# - normally built-in shell echo is OK, but on Debian Linux dash is used, and it does not support -e
configureEcho() {
  echo2='echo -e'
  testEcho=`echo -e test`
  if [ "${testEcho}" = '-e test' ]; then
    # The -e option did not work as intended.
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

# Get the plugin version (e.g., 1.2.0)
# - the version is printed to stdout so assign function output to a variable
getPluginVersion() {
  # Maven folder structure results in duplicate 'owf-tstool-aws-plugin'?
  # TODO smalers 2022-05-19 need to enable this.
  srcFile="${repoFolder}/owf-tstool-aws-plugin/src/main/java/openwaterfoundation/tstool/aws/plugin/zzz.java"  
  # Get the version from the code
  # line looks like:   this.pluginProperties.put("Version", "1.2.0 (2020-05-29");
  cat ${srcFile} | grep pluginProperties | grep Version | cut -d '(' -f 2 | cut -d ',' -f 2 | tr -d '"' | tr -d ' '
}

# Get the TSTool major version (e.g., "13" for 13.3.0):
# - the version is printed to stdout so assign function output to a variable
getTSToolMajorVersion() {
  srcFile="${tstoolMainRepoFolder}/src/DWR/DMI/tstool/TSToolMain.java"  
  # Get the version from the code.
  # line looks like:   this.pluginProperties.put("Version", "1.2.0 (2020-05-29");
  cat ${srcFile} | grep 'public static final String PROGRAM_VERSION' | cut -d '=' -f 2 | cut -d '(' -f 1 | tr -d ' ' | tr -d '"' | cut -d '.' -f 1
}

# Determine the Java install home, consistent with TSTool development environment.
setJavaInstallHome() {
  javaInstallHome='/C/Program Files/Java/jdk8'
  if [ ! -d "${javaInstallHome}" ]; then
    echo ""
    ${echo2} "${errorColor}Unable to determine Java location.  Exiting,${endColor}"
    exit 1
  fi
}

# Main entry point.

# Configure the echo command to output color.
configureEcho

# Make sure the operating system is supported.
checkOperatingSystem

# Get the location where this script is located since it may have been run from any folder.
scriptFolder=$(cd $(dirname "$0") && pwd)
repoFolder=$(dirname ${scriptFolder})
gitReposFolder=$(dirname ${repoFolder})
tstoolMainRepoFolder=${gitReposFolder}/cdss-app-tstool-main

# Get the plugin version, which is used in the jar file name.
pluginVersion=$(getPluginVersion)
if [ -z "${pluginVersion}" ]; then
  ${echo2} "${errorColor}Unable to determine plugin version.${endColor}"
  exit 1
else
  echo "Plugin version:  ${pluginVersion}"
fi

# TODO smalers 2019-06-16 figure out how to handle different TSTool/plugin versions.
#tstoolVersion=12
#tstoolVersion=13
tstoolMajorVersion=$(getTSToolMajorVersion)
if [ -z "${tstoolMajorVersion}" ]; then
  ${echo2} "${errorColor}Unable to determine TSTool main version.${endColor}"
  exit 1
else
  echo "TSTool main version:  ${tstoolMajorVersion}"
fi

# Standard locations for plugin files:
# - put after determining versions
# - the folders adhere to Maven folder structure
devBinFolder="${repoFolder}/owf-tstool-aws-plugin/target/classes"
pluginsFolder="$HOME/.tstool/${tstoolMajorVersion}/plugins"
jarFolder="${pluginsFolder}/owf-tstool-aws-plugin"
jarFile="${jarFolder}/owf-tstool-aws-plugin-${pluginVersion}.jar"
manifestFile="${repoFolder}/owf-tstool-aws-plugin/src/main/resources/META-INF/MANIFEST.MF"

# Set the javaInstallHome variable.
setJavaInstallHome

# Create the jar file in user's development files.
echo "Creating a jar file from class files in folder:  ${devBinFolder}"
echo "Manifest file for jar file:  ${manifestFile}"
echo "Jar file: ${jarFile}"

# Remove the jar file first to make sure it does not append.
#rm ${jarFile}
if [ ! -d "${devBinFolder}" ]; then
  echo ""
  ${echo2} "${errorColor}Project bin folder does not exist:  ${devBinFolder}${endColor}"
  ${echo2} "${errorColor}Make sure to compile software in Eclipse.${endColor}"
  exit 1
fi

# Make sure the folder exists for the jar file.
if [ ! -d "${jarFolder}" ]; then
  echo ""
  echo "jar folder does not exist:  ${jarFolder}"
  echo "Creating it."
  mkdir "${jarFolder}"
fi

cd ${devBinFolder}
"${javaInstallHome}/bin/jar" -cvfm ${jarFile} ${manifestFile} *
if [ ! "$?" = "0" ]
then
  ${echo2} "${errorColor}Error creating jar file.  Exiting.${endColor}"
  exit 1
fi

# Echo out the jar file contents.
echo "Listing of jar file that was created..."
"${javaInstallHome}/bin/jar" -tvf ${jarFile}

# Print the java file location again.
echo ""
echo "Jar file is:  ${jarFile}"
jarCount=$(ls -1 ${jarFolder} | wc -l)
if [ ${jarCount} -eq 1 ]; then
  echo "1 plugin jar file is installed (see below).  OK."
else
  ${echo2} "${errorColor}${jarCount} plugin jar files are installed (see below).${endColor}"
  ${echo2} "${errorColor}There should only be one, typically the latest version.${endColor}"
  ${echo2} "${errorColor}Remove old versions or move to 'plugins-old' in case need to restore.${endColor}"
fi
# Do not put quotes around the following.
ls -1 ${jarFolder}/*

# Make sure that the plugins for the old name are not found.
if [ -d "${oldJarFolder}" ]; then
  echo ""
  ${echo2} "${errorColor}Old plugin folder exists: ${oldJarFolder}${endColor}"
  ${echo2} "${errorColor}This will interfere with the latest plugin that is installed.${endColor}"
  ${echo2} "${errorColor}Remove the old version or move to 'plugins-old' in case need to restore.${endColor}"
else
  echo ""
  echo "Old jar folder does not exist (OK): ${oldJarFolder}"
fi

exit 0
