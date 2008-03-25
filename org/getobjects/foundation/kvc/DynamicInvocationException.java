//
// THIS CODE IS DERIVED FROM THE TAPESTRY WEB APPLICATION FRAMEWORK
// BY HOWARD LEWIS SHIP. EXCELLENT CODE.
//
// ALL EXTENSIONS AND MODIFICATIONS BY MARCUS MUELLER <znek@mulle-kybernetik.com>,
// EVERYTHING AVAILABLE UNDER THE TERMS AND CONDITIONS OF
// THE GNU LESSER GENERAL PUBLIC LICENSE (LGPL). SEE BELOW FOR MORE DETAILS.
//
// Tapestry Web Application Framework
// Copyright (c) 2000-2002 by Howard Lewis Ship
//
// Howard Lewis Ship
// http://sf.net/projects/tapestry
// mailto:hship@users.sf.net
//
// This library is free software.
//
// You may redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation.
//
// Version 2.1 of the license should be included with this distribution in
// the file LICENSE, as well as License.html. If the license is not
// included with this distribution, you may find a copy at the FSF web
// site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
// Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied waranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//

package org.getobjects.foundation.kvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  An exception raised when a dynamic invocation fails with some
 *  form of exception.  This exception is a
 *  {@link RuntimeException} (which
 *  prevents anyone from having to declare it) ... it should only get
 *  raised as a result of programmer error.
 *
 *  This exception is raised 'on behalf' of a more fundamental
 *  exception, which is packaged inside the
 *  <code>DynamicInvocationException</code>.  This root cause exception
 *  may or may not be a runtime exception.
 *
 *  @author Howard Lewis Ship
 *  @version $Id: DynamicInvocationException.java,v 1.2 2002/07/24 15:52:09 znek Exp $
 *
 **/

public class DynamicInvocationException extends RuntimeException
{
  private static final long serialVersionUID = -6526784320428448859L;
    private Throwable rootCause;

    public DynamicInvocationException(String message) {
      super(message);
    }

    public DynamicInvocationException(Method _method,
                                      Object _target,
                                      Throwable _ex)
    {
        super("An " + _ex.getClass().getName() + " exception occured " +
              "while executing method " + _method.getName() +
              " on " + _target + ", " + _ex.getCause());
        this.rootCause = _ex;
    }

    public DynamicInvocationException(Method _method,
                                      Object _target,
                                      InvocationTargetException _ex)
    {
      super("An error occured while executing method " + _method.getName() +
            " on " + _target + ", " + _ex.getCause());
      this.rootCause = _ex;
    }

    public DynamicInvocationException(String message, Throwable _rootCause) {
      super(message + " RootCause: " + _rootCause);
      this.rootCause = _rootCause;
    }

    public DynamicInvocationException(Throwable _rootCause) {
      this.rootCause = _rootCause;
    }

    public Throwable getRootCause() {
      return this.rootCause;
    }
}