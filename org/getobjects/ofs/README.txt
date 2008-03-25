OFS - Object File System, bla bla
========================

In Zope OFS maps a filesystem on top of the ZODB object database. We basically
do the reverse. We instantiate certain 'controller' objects for files in the
filesystem.
We do not directly access the filesystem but rather use the IOFSFileManager
interface, so that we can hook in virtual filesystems (eg expose zip files as
filesystem mount points).

Conversion of a filesystem file/dir to an object is done by the
OFSRestorationFactory. Which per default looks at the type (file/dir) and the
filename extension. The latter are key to the default implementation and you
would define custom extensions for additional classes.

Restored OFS objects do not need to expose their 'child' hierarchy as-is. Eg an
IMAP4 server entry point could expose the IMAP4 folders dynamically.
Some container objects even do a mixture (hold some objects in the local FS and
retrieve others from an external server, eg a database or an LDAP server).
