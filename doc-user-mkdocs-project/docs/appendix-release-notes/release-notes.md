# TSTool AWS Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
Each component is maintained separately and may be updated at different times.
See the [TSTool release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/).

Plugin release notes:

* [Version 1.2.0](#version-120)
* [Version 1.1.1](#version-111)
* [Version 1.1.0](#version-110)
* [Version 1.0.0](#version-100)

----------

## Version 1.2.0 ##

**Feature release to add the S3 Browser application, significant cleanup of all commands for consistency.**

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
*   ![new](new.png) [1.2.0] Add the ***Browse S3*** button to command editors,
    which starts a separate [S3 Browser application](../app-ref/S3Browser/S3Browser.md) to browse S3 objects.
    This application will be enhanced over time to improve integration of the plugin with S3
    and to minimize the need to use the AWS console.

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
