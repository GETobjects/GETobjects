The 'www' subdirectory is a special subdirectory which is mapped to /
in Jetty.

In other words its for static resources (like images and stylesheets) which
should be served directly by Jetty.

Note: this is NOT mapped to /www, but directly into /! Eg you usually want to
create 'css' and 'img' directories below www.
'www' just means that its directly served by Jetty w/o hitting WOApplication.
