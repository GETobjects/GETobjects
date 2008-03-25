JOPE JavaScript Applications
============================

OK, so this kinda works now. This package lets you write JOPE applications in
JavaScript, w/o the need to compile any Java code. To do so, it uses the Rhino
JavaScript interpreter provided by the Mozilla project.

Since Rhino can access all Java classes you can reuse all classes from JOPE,
even other Java JAR packages. But there are a few limitations, eg you cannot
subclass Java classes in Rhino.

Hence this 'jsapp' package provides the necessary hooks and JOPE subclasses to
allow scripted applications.


Basic Structure
***************

A jsapp application currently has the following directory layout:

  AppName/				(eg HelloWorld)
    Application.js
    Session.js
    Context.js
    Main.wo/
      Main.html
      Main.wod				[optional]
      Main.js				[optional]

You can put in as many components as you like, but they must be 'wrapper' style
components (.wo directories). The .wod and .js are optional, the .js can contain
JS functions and variables of the component.


Running an App
**************

To run the JS application you should use the 'run' class from the jsapp package:

  java org.opengroupware.jope.jsapp.run AppName/

Also check the run.sh, it contains a template of a shell script to run the app.
Be sure to include all necessary JARs in the classpath, eg js.jar, the Rhino
interpreter.

During development it makes sense to add a Defaults.properties file containing:

  WOCachingEnabled=false

so that template or script changes require no restart.  


Example
*******

Check samples/HelloJS for a fully scripted example application.


API
***

The API exposed towards Application, Component, Context and Session scripts its
mostly the same like the API of JOPE. If a script implements a relevant
function (eg awake()), jsapp will call it.

To call 'super' implementations (default implementations), prefix the function
with a 'super_', eg:

  function shouldTakeValuesFromRequest(_rq, _ctx) {
    return super_shouldTakeValuesFromRequest(_rq, _ctx);
  }

Be careful, in JS you do not need to specify all parameters you get in the
function definition (eg you could leave out '_ctx'), but when you call super
you must specify everything which is required.


Tips
****

* 1) Same Namespace for Variables and Functions (aka Accessors)

Remember that JS has the same namespace for ivar and function slots, eg this
does not work in JS:
  
     var note;
     function setNote(_o) { this.note = _o; }
     function note() { return this.note; }
  
The function overrides the ivar. In fact, the function here returns itself,
in KVC eval this will show up as:
  "org.mozilla.javascript.gen.c17@2cb491"
(the compiled JS function)

* 2) Importing Java Packages

Remember that you need to import packages. Its reasonably easy:

  importPackage(org.opengroupware.jope.foundation);

* 3) OGNL Interop (aka no OGNL on JavaScript components)

Remember that OGNL does not work against JS, just use JavaScript! Eg:

  <wo:a js:href="context.viewUrlForObject(note)" string="view" />

[TBD: we might be able to implement a proper OGNL handler?]

* 4) Writing Format classes

To create a formatter in JavaScript just use the JSFormat object. It takes a
function, eg:

  var firstLast = new JSFormat(context, function(_obj) {
    return _obj.firstname + " " + _obj.lastname;
  });

This could be used in a WOString, eg:

  <wo:get value="$person" var:format="firstLast" />

* 5) Scopes
TDB: fix this documentation for the new JSCachedScriptScope stuff

Scopes are tricky, especially when you do cross-component calls. Consider this:

  var newPage = pageWithName("TheOtherPage");
  newPage.setTitle("Hello World");

where NewPage.js looks like this:

  var title;
  function setTitle(_value) {
    title = _value;
  }

This does NOT work properly because setTitle() won't find the 'title' variable.
The name lookup happens at the *calling* site (the one which triggers setTitle).
Hence, its better to use 'this' to target components slots in functions:

  function setTitle(_value) {
    this.title = _value;
  }

[TBD: I'm still not 100% sure whether thats a bug in our code, eg wouldn't the
 component object be the prototype or something of the setTitle function?]
