# JabRef Debianized

This branch is used to prepare a clean version for distribution of JabRef at Debian.
See https://tracker.debian.org/pkg/jabref for the current status of Debian at JabRef.

Full information of JabRef is available at https://github.com/JabRef/jabref

Until the release of JabRef v3.00, this branch will be rebased on the master branch to have a clean diff.
After releasing v3.00, this branch will be rebased on the last release.

## Changes in comparison to master branch

* rewrite REAMDE.md to list the differences to the master branch
* adapt build.gradle: Only the plugins required for building remain
  * also remove install4j dependency
* remove library AppleJavaExtension.jar not required for linux
* remove MacAdapter.java
* adapt JabRefFrame.java not to use MacAdapter
* adapt circle.yml accordingly
* adapt external-libraries.txt accordingly
