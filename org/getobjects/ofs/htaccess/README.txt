htaccess
********

Classes for dealing with htaccess files. We want to preserve as much of the
structure of the file as possible so that we can use the parser to edit
htaccess files (read/save, not just to interpret them).


HtConfigBuilder
===============
This object 'executes' the directives contained in a HtConfigFile against
a given lookup context.
The directives are the IHtConfigEvaluation objects contained in the 'eval'
subpackage.

HtConfigSection
===============
This is a collection of directives, eg a <FilesMatch>...</FilesMatch>.

HtConfigDirective
=================
A parsed directive. Has a name (eg SetHandler) and an array of String[]
arguments.
The actual processing of the directive is done by an 'IHtConfigEvaluation'
objects. Those processing objects are stored in the 'eval' subpackage.
