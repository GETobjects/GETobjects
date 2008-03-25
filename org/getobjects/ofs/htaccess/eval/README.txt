htaccess.eval
*************

This package contains classes which execute HTAccess directives against a
configuration dictionary.
Its not strictly Apache compatible, but longterm thats the goal.

Satisfy
=======
Require valid-user
Order allow,deny
Allow from 192.168.1
Satisfy Any

- Satisfy Any means that *either* the Require OR the Allow is valid
