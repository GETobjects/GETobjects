/*
  Copyright (C) 2006 Helge Hess

  This file is part of Go.

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with Go; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.appserver.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.getobjects.appserver.publisher.IJoCallable;
import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.appserver.publisher.JoTraversalPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TJoTraversalPath extends WOTestWithFullEnvironment {
  
  /* root => level1 => level2 => method */
  @SuppressWarnings("synthetic-access")
  static Object testTree =
    new MyJoObject("level1",
        new MyJoObject("level2",
            new MyJoObject("method",
                new MyJoMethod()))); 

  @Before
  public void setUp() {
    super.setUp();
  }

  @After
  public void tearDown() {
    super.tearDown();
  }
  
  /* tests */
  
  @Test public void testSimpleLookup() {
    JoTraversalPath path = 
      new JoTraversalPath( new String[] { "level1" }, testTree, this.context );
    assertNotNull("got no traversal path object for path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertNotNull("got no result",        path.resultObject());
    assertNotNull("got no client object", path.clientObject());
    assertEquals("result and clientObject are not equal",
                 path.resultObject(), path.clientObject());
  }
  
  @Test public void testEmptyTraversalPath() {
    JoTraversalPath path =
      new JoTraversalPath( new String[0], testTree, this.context );
    assertNotNull("got no traversal path object for empty path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertEquals("empty lookup path didn't return root", 
                 testTree, path.resultObject());
    assertEquals("empty lookup path didn't return root as clientObject", 
                 testTree, path.clientObject());
  }
  
  @Test public void testNullRoot() {
    JoTraversalPath path = 
      new JoTraversalPath( new String[] { "level1" }, null, this.context );
    assertNotNull("got no traversal path object for path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertNull("got a result despite null root",        path.resultObject());
    assertNull("got a client object despite null root", path.clientObject());
  }
   
  @Test public void testMethodLookup() {
    JoTraversalPath path = 
      new JoTraversalPath( new String[] { "level1", "level2", "method" },
                           testTree, this.context );
    assertNotNull("got no traversal path object for path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertNotNull("got no result",        path.resultObject());
    assertNotNull("got no client object", path.clientObject());
    assertEquals("result and clientObject are equal despite method in path",
                 path.resultObject(), path.clientObject());
  }

  @Test public void testMethodLookupWithPathInfo() {
    JoTraversalPath path = 
      new JoTraversalPath( new String[] {
                             "level1", "level2", "method", "1", "2"
                           },
                           testTree, this.context );
    assertNotNull("got no traversal path object for path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertNotNull("got no result",        path.resultObject());
    assertNotNull("got no client object", path.clientObject());
    assertEquals("result and clientObject are equal despite method in path",
                 path.resultObject(), path.clientObject());
    
    String[] pathInfo = path.pathInfo();
    assertNotNull("missing path info ...", pathInfo);
    assertEquals("expected path info length does not match", 
                 2, pathInfo.length);
    assertEquals("first path info value does not match", 
                 "1", pathInfo[0]);
    assertEquals("second path info value does not match", 
                 "2", pathInfo[1]);
  }
  
  @Test public void testLookupMissing() {
    JoTraversalPath path = 
      new JoTraversalPath( new String[] { "missinglink.html" },
                           testTree, this.context );
    assertNotNull("got no traversal path object for path", path);
    
    path.traverse();
    assertNull("a traversal error occurred: " + path.lastException(),
               path.lastException());
    
    assertNull("got a result",        path.resultObject());
    assertNull("got a client object", path.clientObject());
  }
  
  /* objects to traverse */
  
  private static class MyJoObject implements IJoObject {
    
    private String name;
    private Object child;
    
    public MyJoObject(String _name, Object _child) {
      this.name  = _name;
      this.child = _child;
    }
    
    public Object lookupName(String _name, IJoContext _ctx, boolean _aquire) {
      if (this.name.equals(_name))
        return this.child;
      
      return null;
    }
  }

  private static class MyJoMethod implements IJoCallable {

    public Object callInContext(Object _object, IJoContext _ctx) {
      return this;
    }

    public boolean isCallableInContext(IJoContext _ctx) {
      return true;
    }
    
  }
}
