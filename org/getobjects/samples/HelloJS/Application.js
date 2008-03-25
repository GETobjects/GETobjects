// Global state, attached to the WOApplication object.

importPackage(Packages.java.lang);


System.err.println("loading app script: " + this);

function sayHello() {
  // Somehow the function isn't executed in the script scope when being called
  // from outside, hence our importPackage doesn't apply. Weird.
  Packages.java.lang.System.err.println("Hello says the app!");
}

//System.err.println("sayHello(): " + sayHello);
