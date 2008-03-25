ofs.fs
******

The packages is a small wrapper around the filesystem used as a backend for
JOPE OFS. We can't use java.io.File, because we want to have it pluggable,
eg an important filemanager is one which mounts a .zip/.jar filesystem as
a folder.

Another feature is that the OFSFileManager can associate caches with filesystem
nodes to improve performance.
For example a JavaScript file might be parsed just once, and then only when its
timestamp changes.
A .zip based FileManager can cache ALL its contents until the zip file itself
changes, which makes it very efficient for the deployment of products.
