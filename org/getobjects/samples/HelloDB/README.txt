HelloDB
=======

This applications demonstrates how to access a relational (JDBC) database
using Go.

The demo runs against the OpenGroupware.org database schema which you can find
over here:
  http://svn.opengroupware.org/OpenGroupware.org/trunk/Database/PostgreSQL/pg-build-schema.psql

You can configure the database connection in the Defaults.properties file.


When you start the application by calling the HelloDB main() function, you will
be able to access it using:

  http://localhost:8181/HelloDB

You can change the HTTP port the server runs on using the -DWOPort=12345
commandline parameter.
