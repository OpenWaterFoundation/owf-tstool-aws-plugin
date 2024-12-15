# TSTool AWS Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
The core software and plugins are maintained separately and may be updated at different times.
See the [TSTool release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/).

Plugin release notes are listed below.
The repository issue for release note item is shown where applicable.

*   [Version 1.5.7](#version-157)
*   [Version 1.5.6](#version-156)
*   [Version 1.5.5](#version-155)
*   [Version 1.5.4](#version-154)
*   [Version 1.5.3](#version-153)
*   [Version 1.5.2](#version-152)
*   [Version 1.5.1](#version-151)
*   [Version 1.5.0](#version-150)
*   [Version 1.4.2](#version-142)
*   [Version 1.4.1](#version-141)
*   [Version 1.4.0](#version-140)
*   [Version 1.3.0](#version-130)
*   [Version 1.2.0](#version-120)
*   [Version 1.1.1](#version-111)
*   [Version 1.1.0](#version-110)
*   [Version 1.0.0](#version-100)

----------

## Version 1.5.7 ##

**Maintenance release to improve the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) delete objects features.**

*   ![change](change.png) Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   [#57] Enhance the `DeleteObjects` command to allow deleting objects
        listed in the ***List Objects*** output and handle the API limit of 1000 objects per delete.
    +   [#58] Add the `StorageClass` to output when listing objects
    +   [#58] Improve the command progress messages shown in the TSTool progress bar.
    +   [#59] Enhance the `ListBuckets` command to output tags for each bucket.

## Version 1.5.6 ##

**Maintenance release to improve the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command for versions and tags.**

*   ![change](change.png) Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   [#53] The `ListObjects` command now properly sets the output table for discovery mode
        when a property is used in the table name.
        This allows other command editors to see the table name with unexpanded property.
    +   [#54] Add features to tag objects and handle versions:
        -   Add the `TagObjects` S3 command with `Tags` and `TagMode` parameters to allow tagging existing objects.
        -   Update the `UploadObjects` S3 command with new `UploadTags` and `UploadTagMode`
            parameters to allow tagging objects as they are uploaded.
        -   Update the `ListObjects` S3 command with new `ListVersions` parameter so that versioned objects can be listed
            and `OutputObjectTags` parameter to control whether tags are output.
        -   Update the command to show progress in cases where the number of operations is known,
            such as uploading and tagging files.
        -   Update the `ListBuckets` S3 command to allow selecting a specific bucket or `*` for all buckets.
            All other S3 commands operate on a single selected bucket.
            This functionality may be expanded in the future to other S3 commands.
        -   Improve command editor error checking to make sure that specified parameters are only used with
            the S3 command being run.

## Version 1.5.5 ##

**Maintenance release to improve the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command.**

*   ![change](change.png) Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   [#52] Missing local files now do not stop the upload.
        Other errors in input will cause the command to not run.

## Version 1.5.4 ##

**Maintenance release to improve the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command.**

*   ![change](change.png) Update the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command:
    +   [#50] Fix VPN connection and Elastic IP service properties table output to show tags.

## Version 1.5.3 ##

**Maintenance release to improve the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command.**

*   ![change](change.png) Update the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command:
    +   [#50] Add VPN connection and Elastic IP services to the service properties table.
    +   [#50] Add public and private IP and DNS name output to the service properties table.

## Version 1.5.2 ##

**Maintenance release to improve the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command.**

*   ![change](change.png) Update the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command:
    +   [#51] Grouped data or total data can be read, but not both with one command.
        This change reflects the features of the AWS API.
        Command parameter names were renamed to be more explicit.
    +   [#48] New parameters are available to output service properties,
        useful to check that tags are defined as expected:
        -   `EBSSnapshotsTableID` - lists EBS snapshots
        -   `EC2PropertiesTableID` - lists EC2 and related service properties
        -   `EC2ImagesTableID` - lists EC2 images (Amazon Machine Images, AMI)

## Version 1.5.1 ##

**Maintenance release to fix CloudFront invalidation issue.**

*   ![bug](bug.png) [#46] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   Paths that are invalidated previously had `*` automatically appended.
        This resulted in hitting the limit of 15 paths with wildcards.
        The wildcard is no longer automatically added.
        If necessary, use the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command for more control of invalidations.

## Version 1.5.0 ##

**Feature release to add `AwsBilling` command.**

*   ![new](new.png) [#43] Add the [`AwsBilling`](../command-ref/AwsBilling/AwsBilling.md) command:
    +   Query AWS Cost Explorer costs in tabular form.
    +   Create time series for cost records.

## Version 1.4.2 ##

**Maintenence release to improve workflow integration.**

*   ![change](change.png) [#41] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    +   Add the `Tags` command parameter to specify the CloudFront distribution using tag(s).
*   ![change](change.png) [#41] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   Add the `CloudFrontTags` command parameter to specify the CloudFront distribution using tag(s).
*   ![change](change.png) [#41] Update the [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command:
    +   Add the `CloudFrontTags` command parameter to specify the CloudFront distribution using tag(s).

## Version 1.4.1 ##

**Maintenence release to refresh data when new profile is selected in command editors.**

*   ![bug](bug.png) [1.4.1] Fix the root URL to command help pages.
*   ![bug](bug.png) [1.4.1] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    +   Fix bug where changing the AWS profile did not update the distribution ID choices.
*   ![bug](bug.png) [1.4.1] Update the [`AwsCloudFront`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command:
    +   Fix bug where changing the AWS profile did not refresh the bucket choices.
*   ![bug](bug.png) [1.4.1] Update the [`AwsCloudFront`](../command-ref/AwsS3/AwsS3.md) command:
    +   Fix bug where changing the AWS profile did not refresh the bucket choices.

## Version 1.4.0 ##

**Feature release to improve creation of landing pages.**

*   ![bug](bug.png) [1.4.0] Fix bug in the [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command:
    +   An `UploadFiles` parameter with wildcard was causing the first S3 file to be overwritten with each local file.
        This has been fixed.
*   ![change](change.png) [1.4.0] Update the [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command:
    +   Change the `DatasetIndexHeadFile` parameter to `DatasetIndexHeadInsertTopFiles` to provide more clarity and flexibility.
    +   Change the `DatasetIndexBodyFile` parameter to `DatasetIndexBodyInsertTopFiles` to provide more clarity and flexibility.
    +   Change the `DatasetIndexFooterFile` parameter to `DatasetIndexBodyInsertBottomFiles` to provide more clarity and flexibility.
    +   The above command parameters are automatically migrated when the command file is read.

## Version 1.3.0 ##

**Feature release to add command indentation, compatible with TSTool 14.6.0.**

*   ![change](change.png) [1.3.0] Update the plugin commands to implement command indentation
    compatible with TSTool 14.6.0 command indentation features.

## Version 1.2.0 ##

**Feature release to add the S3 Browser application and significant cleanup of all commands for consistency and to simplify use.**

*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command with the following general changes:
    +  ** Significant changes to command parameters have been made.
        Old command files will need to be updated to use the new syntax.**
    +   In general, use "folder" rather than "directory" in terminology and command parameters.
        For eample, `DownloadDirectories` has been replaced with `DownloadFolders`.
    +   Emphasize "files" and "folders" where appropriate rather than using "keys".
        For example `DeleteKey` has been replaced with `DeleteFiles` and `DeleteFolders`.
    +   Where possible, allow multiple files and folders to be processed in one command in order to streamline workflows.
        Command parameter editors have been improved to provide editing tools for comma-separated lists
        and dictionary maps.
    +   Constraints for folders have been tightened,
        including requiring `/` at the end of folder S3 object keys to clearly identify folders compared to files.
        This allows for more explicit error handling.
    +   Treat folder keys that start with `/` similar to other keys,
        although S3 bucket objects typically don't use `/` at the root level.
    +   Enhance the documentation to be more detailed.
    +   Implement automated tests for all S3 commands to verify functionality and link to the automated tests for examples.
    +   More extensive error handling has been added to detect input errors.
    +   Add the `AppendOutput` parameter to ***Output*** tab parameters to allow multiple S3 `ListBuckets` and `ListObjects` commands
        to append output in the table and file.
    +   Add the ***CloudFront*** tab parameters to allow new or changed S3 objects to automatically
        be invalidated for the CloudFront distribution associated with the S3 bucket.
        Consequently, a separate [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command can be avoided in many cases.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `CopyObjects` command:
    +   The command now allows copying multiple objects.
        The previous `CopyObject` S3 command has been changed to `CopyObjects` (plural).
    +   The previous `CopySourceKey` and `CopyDestKey` parameters are no longer supported.
    +   The `CopyFiles`, `CopyBucket` (for destination bucket), and `CopyObjectsCountProperty` parameters
        have been added.
    +   Copying folders is not currently supported but may be added in the future.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `DeleteObjects` command:
    +   The command has been changed from `DeleteObject` to `DeleteObjects` (plural).
    +   The command now allows deleting multiple files and folders.
        Deleting a folder automatically lists the folder contents and then deletes the file objects.
    +   The `DeleteFiles` and `DeleteFolders` parameters have been added to specify the objects to delete.
    +   The `DeleteFoldersMinDepth` parameter has been added to guard against deleting high-level files and folders.
    +   The `DeleteFoldersScope` parameter has been added to control whether only files in a folder,
        or the folder and all of its contents are deleted.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `DownloadObjects` command:
    +   The `DownloadDirectories` parameter has been changed to `DownloadFolders`.
    +   Added support for `*` wildcard in local file names when downloading files.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `ListBuckets` command:
    +   Added the `ListBucketsRegEx` parameter to filter bucket output.
    +   Added the `ListBucketsCountProperty` parameter to set a processor property to the list size,
        which is useful for error checks and automated tests.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `ListObjects` command:
    +   Changed the command from `ListBucketObjects` to `ListObjects` consistent with other command names.
    +   Add the `ListObjectsScope` parameter to indicate whether only files in a folder or all files and subfolder
        contents will be listed.
    +   Add the `Prefix` parameter to filter the object list.
    +   Add the `Delimiter` parameter to allow specifying a folder delimiter other than the default (`/`).
    +   Add the `ListObjectsRegEx` parameter to filter the object list.
    +   Add the `ListFiles` and `ListFolders` parameters to filter by object type.
    +   Add the `ListObjectsCountProperty` parameter to set a processor property to the list size,
        which is useful for error checks and automated tests.
*   ![change](change.png) [1.2.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) `UploadObjects` command:
    +   The `UploadDirectories` parameter has been changed to `UploadFolders`.
*   ![change](change.png) [1.2.0] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    +   Enhance the documentation to be more detailed.
    +   Implement automated tests for all CloudFront commands to verify functionality and link to the automated tests for examples.
    +   Add the form for editing the invalidation paths.
    +   Add the `ListDistributionsCountProperty` and `ListInvalidationsCountProperty` parameters to set
        to the count of the output list.
    +   Add the `InvalidationStatus` parameter when listing invalidations to allow filtering on the status.
    +   Enable output to a file.
*   ![change](change.png) [1.2.0] Split the previous [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md) command
    to add the new 
    [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command
    to simplify the functionality of each command.
    Previous command files that used the
    [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md) command need to be updated to use the
    [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command.
    The [`AwsS3LandingPage`](../command-ref/AwsS3LandingPage/AwsS3LandingPage.md) command has been modified
    from the previoius as follows:
    +   The command editor dialog has been updated to provide more information explaining the parameters.
    +   The ***AWS S3*** tab has been added to the editor and includes parameters related to the AWS S3 connection.
    +   The command now focuses only on creating dataset landing pages and features to create
        a dataset catalog (list of datasets) have been removed.
    +   The `StartingPrefix` parameter has been renamed to `StartingFolder`.
    +   The `ProcessSubdirectories` parameter has been renamed to `ProcessSubfolders`.
    +   The `UploadDatasetFiles` parameter has been renamed to `UploadFiles`.
    +   The ***HTML Inserts*** tab has been added to the command editor for HTML-related landing page insert files.
    +   The ***Markdown Inserts*** tab has been added to the command editor for Markdown-related landing page insert files.
    +   The ***Output*** tab has been removed from the command editor since these features were used with the catalog landing page.
    +   The ***CloudFront*** tab has been added to the command editor with parameters similar to the
        [`AwsS3`](../AwsS3/AwsS3.md) command to automate CloudFront invalidations.
        The previous `DistributionId` has been renamed to `CloudFrontDistributionId` for consistency.
*   ![new](new.png) [1.2.0] Add the ***Browse S3*** button to command editors,
    which starts a separate [S3 Browser application](../app-ref/S3Browser/S3Browser.md) to browse S3 objects.
    This application will be enhanced over time to improve integration of the plugin with S3
    and to minimize the need to use the AWS console.
    For example, use the S3 Browser to determine the keys to use for file and folder objects to use in command parameters.

## Version 1.1.1 ##

**Maintenance release to add more error handling and improve usability.**

*   ![change](change.png) [1.1.1] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   Display default values for the profile and region.
    +   Add warning messages if the AWS user configuration file does not exist.
    +   Better handle default values for profile and region and don't allow a default bucket.
    +   Show the correct editor tab based on the S3 command.
*   ![change](change.png) [1.1.1] Update the [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md):
    +   Display default values for the profile and region.
    +   Add warning messages if the AWS user configuration file does not exist.
    +   Better handle default values for profile and region.
    +   Add support for generating Markdown landing page.
    +   Add the `ProcessSubdirectories` parameter to control whether subdirectories should be processed,
        which provides flexibility to process main dataset landing page without recursively processing child folders.
*   ![change](change.png) [1.1.1] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    +   Display default values for the profile and region.
    +   Add warning messages if the AWS user configuration file does not exist.
    +   Better handle default values for profile and region.
    +   Show the correct editor tab based on the CloudFront command.
*   ![change](change.png) [1.1.1] Update the AWS SDK library:
    +   The `software.amazon.aswssdk` libraries for `s3`, `cloudfront`,
        and `s3-transfer-manager` have been updated from version 2.17.198 to 2.18.19.
    +   The `software.amazon.aswssdk.crt` library for `aws-crt` has been updated from version
        0.16.13 to 0.19.10.

## Version 1.1.0 ##

**Feature release to add the `AwsS3Catalog` command.**

*   ![bug](bug.png) [1.1.0] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    +   Fix issue where waiting for invalidation to complete was not occurring.
*   ![new](new.png) [1.1.0] Add the [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md) command to
    generate landing pages for S3 datasets.
*   ![change](change.png) [1.1.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    +   Allow the file pattern for upload to contain wildcards.

## Version 1.0.0 ##

**Initial release.**

*   ![new](new.png) [1.0.0] The initial release includes the following commands:
    +   [`AwsS3`](../command-ref/AwsS3/AwsS3.md)
    +   [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md)
