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

import java.util.concurrent.ConcurrentHashMap;

/**
 * WOServerSessionStore
 * <p>
 * This class keeps session in-memory.
 * <p>
 * Note: we just keep the live object. I think WO serializes the session which
 *       loweres memory requirements (because only persistent values are saved)
 *       and improves persistent store interoperability.
 * <p>
 * THREAD: the overridden operations are not threadsafe, but thread safety
 *         is accomplished by the checkout/checkin mechanism implemented in
 *         the superclass.
 */
public class WOServerSessionStore extends WOSessionStore {
  
  /* Threading: our session is checked out, so we do not need to deal with
   * concurrent requests, BUT our hashmap might be accessed from concurrent
   * sessions! Hence it must be a proper concurrent HashMap. 
   */
  protected ConcurrentHashMap<String, WOSession> store;
  
  public WOServerSessionStore() {
    super();
    this.store = new ConcurrentHashMap<String, WOSession>(128);
  }

  @Override
  public void saveSessionForContext(WOContext _ctx) {
    WOSession sn = _ctx.session();
    if (sn == null) {
      log.warn("got no session to save ...");
      return;
    }
    
    String sid = sn.sessionID();
    if (sid == null) {
      log.warn("session has no ID?: " + sid);
      return;
    }
    
    if (log.isInfoEnabled())
      log.info("saving session: " + sid);
    this.store.put(sid, sn);
  }

  @Override
  public WOSession removeSessionWithID(String _sid) {
    if (_sid == null) {
      log.info("got no session-id to remove ...");
      return null;
    }
    
    if (log.isInfoEnabled())
      log.info("removing session: " + _sid);
    return this.store.remove(_sid);
  }
  
  @Override
  public WOSession restoreSessionForID(String _sid, WORequest _rq) {
    boolean debugOn = log.isDebugEnabled();
    
    if (_sid == null) {
      if (debugOn) log.debug("got no session-id to restore ...");
      return null;
    }
    
    if (debugOn) log.debug("restore session: " + _sid);
    WOSession sn = this.store.get(_sid);
    if (sn == null)
      log.info("failed to restore session: " + _sid);
    
    if (debugOn) log.debug("  restored: " + sn);
    return sn;
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    if (this.store == null)
      _d.append(" no-store");
    else {
      _d.append(" store=#");
      _d.append(this.store.size());
    }
  }
}
