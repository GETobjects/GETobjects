// Controller of the Main component

importPackage(Packages.java.lang);

var a = 5;
var b = 14;
var c = 0;

function addAction(rq, name, comp) {
  //System.out.println("Add called on " + this + " (" + (typeof this) + ")");
  //System.out.println("  a is " + a + " (" + (typeof a) + ")"); 
  //System.out.println("  b is " + b + " (" + (typeof b) + ")");

  //b = (b + a).toFixed(); // otherwise we get a Double?! Yes, Its JS!
  b = (b + a); // Note: .toFixed() returns a String
  
  if (this.application.sayHello)
    this.application.sayHello(); // TBD: fails ATM
  else {
    log.error("APP: " + this.application);
    log.error("APP.sayHello: " + this.application.sayHello);
  }
  
  return null; /* means: stay on page */
}

function addAndReturnNewAction() {
  var newMain = this.pageWithName("index");
  
  if (true) { /* either way works, but note that .a does NOT trigger KVC */
    newMain.a = a;
    newMain.b = a + b;
  }
  else {
    newMain.takeValueForKey(a, "a");
    newMain.takeValueForKey(a + b, "b");
  }
  
  return newMain;
}

function resetAction() {
  b = 14;
}

function appendToResponse(_r, _ctx) {
  this.super_appendToResponse(_r, _ctx);
  _r.appendContentString("<sub>Hello World, direct HTML</sub>");
}
