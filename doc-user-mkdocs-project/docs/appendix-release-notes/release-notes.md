# TSTool AWS Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
Each component is maintained separately and may be updated at different times.
See the [TSTool release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/).

Plugin release notes:

* [Version 1.1.0](#version-110)
* [Version 1.0.0](#version-100)

----------

## Version 1.1.0 ##

**Feature release to add the `AwsS3Catalog` command.**

* ![bug](bug.png) [1.1.0] Update the [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md) command:
    + Fix issue where waiting for invalidation to complete was not occurring.
* ![new](new.png) [1.1.0] Add the [`AwsS3Catalog`](../command-ref/AwsS3Catalog/AwsS3Catalog.md) command to
  generate landing pages for S3 datasets.
* ![change](change.png) [1.1.0] Update the [`AwsS3`](../command-ref/AwsS3/AwsS3.md) command:
    + Allow the file pattern for upload to contain wildcards.

## Version 1.0.0 ##

**Initial release.**

* ![new](new.png) [1.0.0] The initial release includes the following commands:
    + [`AwsS3`](../command-ref/AwsS3/AwsS3.md)
    + [`AwsCloudFront`](../command-ref/AwsCloudFront/AwsCloudFront.md)
