/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.foundation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * NSNotificationCenter
 * 
 * TODO: document
 */
public class NSNotificationCenter extends NSObject {
  protected static final Log log = LogFactory.getLog("NSNotificationCenter");
  
  protected List<NSNotificationObserver>              omniscientObservers;
  protected Map<String, List<NSNotificationObserver>> nameToObserver;
  protected Map<Object, List<NSNotificationObserver>> objectToObserver;
  
  /* managing observers */
  
  public void addObserver
    (Object _observer, NSSelector _sel, String _name, Object _object)
  {
    if (_observer == null || _sel == null)
      return;
    
    if (_name == null && _object == null) {
      this.addOmniscientObserver(_observer, _sel);
      return;
    }
    
    NSNotificationObserver observer =
      new NSNotificationSelectorObserver(_observer, _sel);
    
    /* add to name list */
    
    if (_name != null) {
      List<NSNotificationObserver> observers = null;

      if (this.nameToObserver == null) {
        this.nameToObserver = 
          new HashMap<String, List<NSNotificationObserver>>(4);
      }
      else
        observers = this.nameToObserver.get(_name);

      if (observers == null) {
        observers = new ArrayList<NSNotificationObserver>(4);
        this.nameToObserver.put(_name, observers);
      }
      
      observers.add(observer);
    }
    
    /* add to object list */
    
    if (_object != null) {
      List<NSNotificationObserver> observers = null;

      if (this.objectToObserver == null) {
        this.objectToObserver =
          new WeakHashMap<Object, List<NSNotificationObserver>>(4);
      }
      else
        observers = this.objectToObserver.get(_object);

      if (observers == null) {
        observers = new ArrayList<NSNotificationObserver>(4);
        this.nameToObserver.put(_name, observers);
      }
      
      observers.add(observer);
    }
  }
  
  public void removeObserver(Object _observer) {
    this.removeObserver(_observer, null /* name */, null /* object */);
  }
  
  public void removeObserver(Object _observer, String _name, Object _object) {
    if (_observer == null)
      return;
    
    // TODO: check semantics
    
    if (_name != null && this.nameToObserver != null) {
      List<NSNotificationObserver> observers = this.nameToObserver.get(_name);
      if (observers != null) {
        // TODO: remove NSNotificationObserver
      }
    }

    if (_object != null && this.objectToObserver != null) {
      List<NSNotificationObserver> observers =
        this.objectToObserver.get(_object);
      if (observers != null) {
        // TODO: remove NSNotificationObserver
      }
    }
    
    // TODO: implement me
  }
  
  public void addOmniscientObserver(Object _observer, NSSelector _sel) {
    if (_observer == null || _sel == null)
      return;
    
    if (this.omniscientObservers == null)
      this.omniscientObservers = new ArrayList<NSNotificationObserver>(4);
    
    this.omniscientObservers.add
      (new NSNotificationSelectorObserver(_observer, _sel));
  }
  public void removeOmniscientObserver(Object _observer) {
    if (this.omniscientObservers == null || _observer == null)
      return;
    
    // TODO: implement me
  }
  
  /* posting */
  
  public void postNotification(NSNotification _notification) {
    if (_notification == null)
      return;
    
    /* first post to omniscient observers */
    
    if (this.omniscientObservers != null) {
      for (NSNotificationObserver observer: this.omniscientObservers)
        observer.notificationGotPosted(_notification);
    }
    
    /* next post to specific observers */
    
    String name = _notification.name();
    Object obj  = _notification.object();
    
    if (name == null && obj == null) /* no key, just post to omniscient */
      return;

    if (name != null && obj == null && this.nameToObserver != null) {
      /* just key on name */
      List<NSNotificationObserver> observers = this.nameToObserver.get(name);
      if (observers != null) {
        for (NSNotificationObserver o: observers)
          o.notificationGotPosted(_notification);
      }
      return;
    }
    
    if (obj != null && name == null && this.objectToObserver != null) {
      /* just key on object */
      List<NSNotificationObserver> observers = this.objectToObserver.get(obj);
      if (observers != null) {
        for (NSNotificationObserver o: observers)
          o.notificationGotPosted(_notification);
      }
      return;
    }
    
    /* ok, both must match */
    
    List<NSNotificationObserver> observers = this.objectToObserver.get(obj);
    if (observers == null) /* no observers for the object */
      return;
    
    List<NSNotificationObserver> nameObservers = this.nameToObserver.get(name);
    if (nameObservers == null) /* no observers for the name */
      return;
    
    for (NSNotificationObserver o: observers) {
      if (!nameObservers.contains(o)) /* ensure the name also matches */
        continue;
      
      o.notificationGotPosted(_notification);
    }
  }
  
  public void postNotification(String _name, Object _object) {
    this.postNotification(new NSNotification(_name, _object));
  }
  
  public void postNotification(String _n, Object _o, Map<String, Object> _ui) {
    this.postNotification(new NSNotification(_n, _o, _ui));
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.omniscientObservers != null)
      _d.append(" #omni=" + this.omniscientObservers.size());
    if (this.nameToObserver != null)
      _d.append(" #name=" + this.nameToObserver.size());
    if (this.objectToObserver != null)
      _d.append(" #obj=" + this.objectToObserver.size());
  }
  
  /* inner class */
  
  public static class NSNotificationSelectorObserver extends NSObject
    implements NSNotificationObserver
  {
    protected WeakReference<Object> observer;
    protected NSSelector            selector;
    
    public NSNotificationSelectorObserver(Object _observer, NSSelector _sel) {
      this.observer = new WeakReference<Object>(_observer);
      this.selector = _sel;
    }
    
    /* posting the notification */
    
    public void notificationGotPosted(NSNotification _notification) {
      Object observerObject = this.observer != null ? this.observer.get():null;
      
      if (observerObject == null || this.selector == null) {
        /* object got garbage collected */
        this.observer = null;
        this.selector = null;
        return;
      }
      
      try {
        this.selector.invoke(observerObject, _notification);
      }
      catch (Exception e) {
        log.error("could not post notification to object: " + observerObject +
                  "\n  notification: " + _notification +
                  "\n  center:       " + this);
      }
    }
  }
}
