# owf-tstool-aws-plugin User Documentation #

This folder contains the user documentation for the Open Water Foundation TSTool AWS plugin software, including release notes.

See also:

*   [Colorado's Decision Support Systems TSTool](http://opencdss.state.co.us/tstool/latest/doc-user/) documentation.
*   [TSTool Software Downloads](http://opencdss.state.co.us/tstool/)

See the following sections on this page:

*   [Repository Contents](#repository-contents)
*   [Development Environment](#development-environment)
*   [Editing and Viewing Content](#editing-and-viewing-content)
*   [Deploying the Documentation](#deploying-the-documentation)
*   [Style Guide](#style-guide)

---------------------------

## Repository Contents ##

The folder contains the following:

```text
README.md             This file.
build-util/           Useful scripts to view, build, and deploy documentation.
mkdocs-project/       Typical MkDocs project for this documentation.
  mkdocs.yml          MkDocs configuration file for website.
  docs/               Folder containing source Markdown and other files for website.
    css/              Custom CSS to augment MkDocs theme.
    markdown files    Files and folders containing Markdown documentation.
  site/               Folder created by MkDocs containing the static website - ignored using .gitignore.
```

## Development Environment ##

The development environment for contributing to this documentation requires
installation of Python, MkDocs, and Material MkDocs theme.
Python 3 and Markdown 1+ has been used for development.
See the [OWF / Learn MkDocs](http://learn.openwaterfoundation.org/owf-learn-mkdocs/)
documentation for information about installing MkDocs.

## Editing and Viewing Content ##

If the development environment is properly configured, edit and view content as follows:

1.  Edit content in the `docs` folder and update `mkdocs.yml` as appropriate.
    +   Markdown is used as much as possible.
    +   Screen shot images are typically created using Gimp or similar software,
        using small PNG files if possible to ensure good performance.
2.  Run the `build-util/run-mkdocs-serve-8000.bash` script (Git Bash/Cygwin/Linux) to process and serve the documentation.
    Any issues should be resolved by updating the run script to support as many environments as possible.
3.  View content in a web browser using URL `http://localhost:8000`.

## Deploying the Documentation ##

The documentation is deployed to the public OWF software website by running the following script.
The `AWS.java` version in the code is used to create the version folder for S3.

```
build-util/copy-to-owf-s3.bash
```

## Style Guide ##

The following are general style guide recommendations for this documentation,
with the goal of keeping formatting simple in favor of focusing on useful content:

*   Use the Material MkDocs theme - it looks nice, provides good navigation features, and enables search.
*   Follow MkDocs Markdown standards - use extensions beyond basic Markdown when useful.
*   Show files and program names as `code (tick-surrounded)` formatting.
*   Where a source file can be linked to in GitHub, provide a link so that the most current file can be viewed.
*   Use triple-tick formatting for code blocks, with language specifier.
*   Use ***bold italics*** when referencing UI components such as menus.
*   Use slashes surrounded by spaces to indicate ***Menu / SubMenu*** (slashes in
    menus can be indicated with no surrounding spaces).
*   Images are handled as follows:
    +   Where narrative content pages are sufficiently separated into folders,
        image files exist in those folder with names that match the original TSTool Word documentation.
        This approach has been used for the most part, for example command reference.
    +   If necessary, place images in an `images` folder.
    +   When using images in the documents, consider providing a link to view the full-sized image.
*   Minimize the use of inlined HTML elements, but use it where Markdown formatting is limited.
*   Although the Material theme provides site and page navigation sidebars,
    provide in-line table of contents on pages, where appropriate, to facilitate review of page content.
    Use a simple list at the top of the page with links to sections on the page.
