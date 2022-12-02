# TSTool AWS Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
Each component is maintained separately and may be updated at different times.
See the [TSTool release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/).

Plugin release notes:

* [Version 1.1.1](#version-111)
* [Version 1.1.0](#version-110)
* [Version 1.0.0](#version-100)

----------

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
