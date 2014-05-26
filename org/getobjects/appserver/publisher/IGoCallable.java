/*
  Copyright (C) 2006-2014 Helge Hess

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

package org.getobjects.appserver.publisher;

/**
 * A IGoCallable is an object which can be called through the web or internally.
 * <p>
 * FIXME: document:
 * <ul>
 *   <li>special behaviour during lookup
 *   <li>time of invocation
 *   <li>use of isCallable
 * </ul>
 * <p>
 * Note: Callables are a bit different to (simpler than) SOPE invocations.
 * SOPE invocations are usually 'bound' to an object - similar to Python
 * bound vs unbound methods.<br>
 * The IGoCallable interface doesn't support this concept. Eg the GoJavaMethod
 * only exists once for all invocations (and hence can't hold invocation
 * specific state).<br>
 * TBD: Should we introduce binding? Might be necessary to support different
 * invocation styles (XML-RPC vs Form vs WebDAV vs SOAP). Right now, it looks
 * like the callables have to deal with such differences.
 * <p>
 * Binding might also be necessary to provide a lookup context. Eg sub
 * invocations of a method which got acquired from somewhere else,
 * might use that context as the 'clientObject'? Not sure.
 */
public interface IGoCallable {

  public boolean isCallableInContext(IGoContext _ctx);
  public Object callInContext(Object _object, IGoContext _ctx);
  
}
