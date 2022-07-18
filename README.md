# owf-tstool-aws-plugin #

This repository contains the Open Water Foundation TSTool Amazon Web Services (AWS) plugin.
This plugin can be installed to enable commands that integrate TSTool with AWS.

TSTool is part of [Colorado's Decision Support Systems (CDSS)](https://www.colorado.gov/cdss).
See the following online resources:

* [TSTool Developer Documentation](https://opencdss.state.co.us/tstool/latest/doc-dev/)
* [TSTool User Documentation](https://opencdss.state.co.us/tstool/latest/doc-user/)

See the following sections in this page:

* [Repository Folder Structure](#repository-folder-structure)
* [Repository Dependencies](#repository-dependencies)
* [Building the Plugin Jar File](#building-the-plugin-jar-file)
* [Building an Installer](#building-an-installer)
* [Contributing](#contributing)
* [License](#license)
* [Contact](#contact)

-----

## Repository Folder Structure ##

The following are the main folders and files in this repository, listed alphabetically.

```
C:\Users\user\                      User's files on windows.
/c/Users/user/                      User's files in Git bash.
  cdss-dev/                         Recommended folder for CDSS product development.
    TSTool/                         Recommended folder for TSTool development.
      git-repos/                    Recommended folder for TSTool product repositories.
        owf-tstool-aws-plugin/      Repository working files.
        .gitattributes              Git configuration file for repository.
        .gitignore                  Git configuration file for repository.
        build-util/                 Utilities used in the build process.
        dist/                       Folder containing software installers.
        doc-user-mkdocs-project/    MkDocs project for user documentation.
        owf-tstool-aws-plugin/      Maven project source code and supporting files.
          .classpath                Eclipse configuration file.
          .project                  Eclipse configuration file.
          .settings/                Eclipse settings for developer.
          src/                      Plugin source code.
          target/                   Compiled code.
        README.md                   This file.
        test/                       End to end tests for commands.
```

## Repository Dependencies ##

This project is configured using Maven, which manages third-party dependencies, such as AWS packages.

Additionally, the following table lists Eclipse project dependencies that are not managed by Maven.

|**Repository**|**Description**|
|------------------------------------------------------------------------------------------|----------------------------------------------------|
|[`cdss-lib-common-java`](https://github.com/OpenCDSS/cdss-lib-common-java)                |Library of core utility code used by multiple TSTool repositories (projects).|
|[`cdss-lib-processor-ts-java`](https://github.com/OpenCDSS/cdss-lib-processor-ts-java)    |Library containing processor code for TSTool commands.|

## Building the Plugin Jar File ##

Plugin Eclipse projects are not part of the "built in" TSTool code.
During development, plugins are handled as follows:

1. Eclipse project:
    1. The `owf-tstool-aws-plugin` repository is added as a Maven project.
    2. The project's build path is configured to use appropriate Maven dependencies, such as the AWS libraries
       and other TSTool projects, such as `cdss-lib-common-java`.
2. Plugin jar file:
    1. The plugin is recognized by TSTool via the plugin design, not the Eclipse build path.
       In other words, plugins are handled as per the production code.
    2. Therefore, run the `build-util/create-plubin-jar.bash` script to create the
       plugin `jar` file in the user's `.tstool/NN/plugins` folder in order to test the plugin.
    3. The above script relies on the `owf-tstool-aws-plugin/src/main/resources/META-INF/MANIFEST.MF`
       file to provide information about the plugin to TSTool,
       including the list of plugin commands and third-party `jar` files that are used by the plugin.

## Building an Installer ##

The plugin is currently distributed on Windows using a zip file,
using the following procedure:

1. Update the version:
    1. Update the `src/main/java/owf-tstool-aws-plugin/org/openwaterfoundation/tstool/plugin/aws/Aws.java` source file.
    2. The version is extracted by scripts below to use in the zip file name,
       which results in versioned installers being listed in the product landing page.
2. Update the documentation:
    1. Update the documentation to be current and include release notes consistent with the version.
    2. Update the `index.md` file to be consistent with the version.
    3. Run the `doc-user-mkdocs-project/build-util/1-create-installer.bash` script to create the installer.
3. Run the `build-util/2-copy-to-owf-amazon-s3.bash` script:
    1. This will upload the local zip file to OWF's S3 bucket.
    2. The script prompts as to whether to update the product landing page `index.html`.

## Contributing ##

Contributions to this project can be submitted using the following options:

1. TSTool software developers with commit privileges can write to this repository.
2. Post an issue on GitHub with suggested change.
3. Fork the repository, make changes, and do a pull request.
   Contents of the current master branch should be merged with the fork to minimize
   code review before committing the pull request.

See also the [OpenCDSS / TSTool protocols](http://learn.openwaterfoundation.org/cdss-website-opencdss/tstool/tstool/).

## License ##

Copyright Open Water Foundation.

The software is licensed under GPL v3+. See the [LICENSE.md](LICENSE.md) file.

## Contact ##

Steve Malers, @smalers, steve.malers@openwaterfoundation.org.
