/*
  Copyright (C) 2006-2007 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.core;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.foundation.NSObject;

/**
 * Superclass for "session stores". A WOSessionStore manages the WOSession
 * objects which are not in use by requests. It maintains a checkin/checkout
 * queue to ensure that access to the sessions is serialized.
 */
public abstract class WOSessionStore extends NSObject implements IJoObject {
  protected static final Log log = LogFactory.getLog("WOSessionStore");
  
  private static WOSessionStore serverSessionStore = new WOServerSessionStore();
  
  protected long sessionCheckOutTimeout;
  
  public static WOSessionStore serverSessionStore() {
    return serverSessionStore;
  }
  
  public WOSessionStore() {
    this.sessionCheckOutTimeout = 5000 /* ms */;
  }

  public abstract void saveSessionForContext(WOContext _ctx);
  public abstract WOSession removeSessionWithID(String _sid);
  public abstract WOSession restoreSessionForID(String _sid, WORequest _rq);
  
  /* checkin / checkout */
  
  protected Set<String> workingSet = new HashSet<String>(128);
  
  public void setSessionCheckOutTimeout(long _timeout) {
    this.sessionCheckOutTimeout = _timeout;
  }
  public long sessionCheckOutTimeout() {
    return this.sessionCheckOutTimeout;
  }
  
  /**
   * Lock the given session ID for other threads. Its criticial that the session
   * is checked in again.
   * <p>
   * This method wraps restoreSessionForID() which does the actual session
   * restoration. Its called by WOApplication.restoreSessionForID().
   * 
   * @param _sid - the session ID to checkout
   * @param _rq  - the request
   * @return the checked out session
   */
  public WOSession checkOutSessionForID(String _sid, WORequest _rq) {
    // TBD: Do we need to ensure somehow that a session is checked in again?
    //      Eg using some timestamp. Somewhat dangerous, but a checked out
    //      session which crashed is dead ...
    // TBD: actually serializing the session has the advantage that we can
    //      do multiple *readonly* checkouts. Which is quite nice.
    
    if (_sid == null) {
      log.error("got no session-id for session checkout!");
      return null;
    }
    
    /* checkout session */
    
    long startMS = new Date().getTime();
    long timeout = this.sessionCheckOutTimeout();
    long now     = startMS;
    
    boolean didCheckOut = false;
    for (int tryCount = 0; !didCheckOut && (now-startMS < timeout);tryCount++) {
      synchronized (this.workingSet) {
        if (!this.workingSet.contains(_sid)) {
          this.workingSet.add(_sid);
          didCheckOut = true;
        }
      }
      
      /* wait a bit prior next attempt to allow other threads to complete */
      if (!didCheckOut) {
        try {
          // TBD: find good values
          int waittime;
          if (tryCount < 5)
            waittime = 20; /* ms */
          else if (tryCount < 10)
            waittime = 50; /* ms */
          else if (tryCount < 20)
            waittime = 100; /* ms */
          else
            waittime = 500; /* ms */
          
          if (waittime > 30)
            log.warn("waiting in session checkout: " + waittime + "ms");
          Thread.sleep(waittime /* ms */);
        }
        catch (InterruptedException e) {
          // could be spurious wakeup?
        }
        now = new Date().getTime();
      }
    }
    
    if (!didCheckOut) {
      // TODO: maybe we should just block the thread?
      log.warn("failed to wait for checked out session (" +
          (now - startMS) + "ms): " + _sid);
      return null;
    }
    
    /* restore session */
    
    WOSession sn = null;
    try {
      sn = this.restoreSessionForID(_sid, _rq);
    }
    finally {
      /* checkin session if restoration failed */
      if (sn == null && _sid != null) {
        synchronized (this.workingSet) {
          this.workingSet.remove(_sid);
        }
      }
    }
    
    return sn;
  }

  /**
   * This is called by WOApplication.saveSessionForContext() to allow other 
   * threads to access the session. Remember that session access is
   * synchronized.
   * 
   * @param _ctx - the context which contains the session.
   */
  public void checkInSessionForContext(WOContext _ctx) {
    if (_ctx == null) {
      log.error("got no context for session checkin!");
      return;
    }
    
    try {
      this.saveSessionForContext(_ctx);
    }
    finally {
      /* we checkin in any case */
      String sid = _ctx.hasSession() ? _ctx.session().sessionID() : null;
      if (sid != null) {
        synchronized (this.workingSet) {
          this.workingSet.remove(sid);
        }
      }
    }
  }
  
  
  /* JoObject */
  
  /**
   * Allows a WOSessionStore to be used as a JoObject. It resolves session-ids
   * to WOSession objects.
   * <p>
   * Note: do not use this, most likely the API will change.
   */
  public Object lookupName(String _name, IJoContext _ctx, boolean _aquire) {
    // TBD: this is probably useless. We really do not want an auto-checkout,
    //      do we? We might want to return a wrapper which can then perform
    //      a checkout or not.
    
    WORequest rq = null;
    if (_ctx instanceof WOContext)
      rq = ((WOContext)_ctx).request();

    // TODO: we need to implement some 'sleep' call or other callback to
    //       checkin the checked out session!
    return this.checkOutSessionForID(_name,rq);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
  
}
