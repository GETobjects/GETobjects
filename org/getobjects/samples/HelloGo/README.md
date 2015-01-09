HelloGo
=======

Simple Go app demonstrating some concepts. Mainly what a client object is,
how a viewer component can display such an object, and how localization can be
implemented.

The application processes pathes like '/HelloGo/en/pojo/view'. Lookup starts at
the application object (a WOApplication subclass which is hooked into Jetty).

****Lookup Phase

In this case the application has one fix 'subkey' called 'pojo'. This returns
a plain Java object ('public class PoJo {}') which just has a single KVC
property called 'name'.

Notice that in '/en/pojo/view' the 'pojo' key is *not* used as a direct subkey
of the application object.

It first has 'en' which is processed by the LanguageSelector class. 'en' is
implemented in such a way, that it doesn't actually resolve to an own object.
Instead it just sets the language in the WOContext (an object representing the
current request/response transaction) and then again returns the application
object.

Subsequently 'pojo' will be looked up. Now 'pojo' itself is a plain Java object,
and has no keys which Go can process as URL components. So the lookup of 'view'
in /en/pojo/view will fail. Right? Wrong. ;-)

The 'view' key is attached to the 'PoJo' class in the 'product.plist' of the
HelloGo project:

    org.getobjects.samples.HelloGo.PoJo = {
      protectedBy   = "<public>";
      defaultAccess = "allow";

      methods = {
      	"view" = {
      	  protectedBy = "<public>";
      	  pageName    = "PoJoViewer";
      	};
      };
    };

This maps the 'view' key to the 'PoJoViewer' WOComponent. This component can
access the 'pojo' object looked up before using the 'clientObject' method of
the WOContext.
Using the mechanism you can make arbitrary Java objects publishable via Go. They
don't have to know anything about Go.

****Client Object

Why is 'pojo' the clientObject, and not 'view'? This is because 'view' is a
*IGoCallable* object (a method).
If the last object in the lookup path is a callable, the object right before
that is going to be the clientObject. If the last object is not a callable,
it'll become the clientObject itself.

Note: you can have a callable which can work as methods as a callable. Eg this
will do the right thing: /pojo/view/editsourcecode. In this case 'view' will
be the clientObject despite being a callable. The hypothetical 'editsourcecode'
would then be the method which is being invoked.

****Go Callable Invocation

If the last object looked up was an IGoCallable, Go will call it and return the
result. If it wasn't, it will look for a default-method (usually 'view' or
'index') and call that. If there is no default method either, Go will use the
clientObject itself as the result of the lookup (the usual case in StaticCMS).

****Rendering

After the invocation there is a result. In the case of this example it is just
the viewer component iself. Which will then get rendered as usual. In here it'll
extract the 'name' parameter from the PoJo via KVC and just output that as HTML.

Note that conceptually methods can return any kind of Java object. Which will
then trigger the lookup of a proper render object.
