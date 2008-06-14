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

package org.getobjects.appserver.core;

import org.getobjects.foundation.NSObject;

/*
 * WEClientCapabilities
 * 
 * 
 * 
 * TODO: complete user-agent parsing
 */
public class WEClientCapabilities extends NSObject {
  
  protected String  userAgent  = null;
  protected short   browserMajorVersion = 0;
  protected short   browserMinorVersion = 0;
  protected short   browser    = WEUA_UNKNOWN;
  protected short   os         = WEOS_UNKNOWN;
  protected short   cpu        = WECPU_UNKNOWN;
  protected boolean acceptUTF8 = true; 
  
  public WEClientCapabilities(WORequest _rq) {
    this.loadFromRequest(_rq);
  }
  
  protected void loadFromRequest(WORequest _rq) {
    if (_rq == null) // TODO: print a warning?
      return;
    
    /* check charset */
    
    String ac = _rq.headerForKey("accept-charset");
    if (ac != null) {
      ac = ac.toLowerCase();
      this.acceptUTF8 = ac.contains("utf-8") || ac.contains("utf8");
    }
    
    /* process user-agent */
    
    this.userAgent = _rq.headerForKey("user-agent");
    if (this.userAgent == null) {
      /* no user-agent, eg telnet */
      this.userAgent = "";
    }
    
    this.loadBrowserTypeForUserAgent(this.userAgent);
    
    /* detect OS */
    
    if (this.userAgent.contains("Windows") || this.userAgent.contains("WinNT"))
      this.os = WEOS_WINDOWS;
    else if (this.userAgent.contains("Maemo")) /* also: Linux! */
      this.os = WEOS_MAEMO;
    else if (this.userAgent.contains("Symbian"))
      this.os = WEOS_SYMBIAN;
    else if (this.userAgent.contains("Series60"))
      this.os = WEOS_SYMBIAN;
    else if (this.userAgent.contains("Linux"))
      this.os = WEOS_LINUX;
    else if (this.userAgent.contains("Mac"))
      this.os = WEOS_MACOS;
    else if (this.userAgent.contains("SunOS"))
      this.os = WEOS_SUNOS;
    else {
      switch (this.browser) {
        case WEUA_IE:
        case WEUA_WebFolder:
          this.os = WEOS_WINDOWS;
          break;
        case WEUA_Safari:
        case WEUA_MACOSX_DAVFS:
          this.os = WEOS_MACOS;
          break;
        default:
          this.os = WEOS_UNKNOWN;
      }
    }
  
    /* detect CPU */
  
    if (this.userAgent.contains("sun4u"))
      this.cpu = WECPU_SPARC;
    else if (this.userAgent.contains("i686") || this.userAgent.contains("i586"))
      this.cpu = WECPU_IX86;
    else if (this.userAgent.contains("PowerPC") || 
             this.userAgent.contains("ppc") || 
             this.userAgent.contains("PPC"))
      this.cpu = WECPU_PPC;
    else if (this.os == WEOS_WINDOWS)
      /* assume ix86 if OS is Windows .. */
      this.cpu = WECPU_IX86;
    else {
      switch (this.browser) {
        case WEUA_IE:
        case WEUA_WebFolder:
          if (this.os == WEOS_UNKNOWN)
            this.os = WECPU_IX86;
          break;
        default:
          if (this.os == WEOS_UNKNOWN)
            this.os = WECPU_UNKNOWN;
      }
    }
  }
  
  protected void loadBrowserTypeForUserAgent(String ua) {
    @SuppressWarnings("unused")
    int tmp;
    
    if ((tmp = ua.indexOf("Opera")) != -1) {
      /* Opera (can fake to be MSIE or Netscape) */
      this.browser = WEUA_Opera;
  
      // TODO: Opera detection
      /* go to next space */
      //while (!isspace(*tmp) && (*tmp != '\0')) tmp++;
      /* skip spaces */
      //while (isspace(*tmp) && (*tmp != '\0')) tmp++;
      
      //this.browserMajorVersion = atoi(tmp);
      //if ((tmp = index(tmp, '.'))) {
      //  tmp++;
      //  this.browserMinorVersion = atoi(tmp);
      //}
    }
    else if (ua.contains("NeonConnection") || 
             ua.contains("ZIDEStore") ||
             ua.contains("ZideLook-Codeon")) {
      this.browser = WEUA_ZideLook;
      this.browserMinorVersion = 0;
      this.browserMajorVersion = 0;
    }
    else if ((tmp = ua.indexOf("Safari/")) != -1) {
      /* Hm, Safari says it is a Mozilla/5.0 ? */
      this.browser = WEUA_Safari;

      // TODO: Safari detection
//      int combinedVersion;
//      tmp += 7; /* skip "Safari/" */
//      combinedVersion = Integer.parseInt(tmp)
//      /* well, don't know how this is supposed to work? 100=v1.1 */
//      if (combinedVersion == 100 /* 100 is v1.1 */) {
//        this.browserMajorVersion = 1;
//        this.browserMinorVersion = 1;
//      }
//      else {
//        /* watch for upcoming versions ... */
//        this.browserMajorVersion = combinedVersion / 100;
//      }
    }
    else if (ua.contains("Outlook-Express/")) {
      /* Outlook Express 5.5 mailbox access via http */
      this.browser = WEUA_MSOutlookExpress;
    }
    else if (ua.contains("Outlook Express/")) {
      /* Outlook Express 5.0 mailbox access via http */
      this.browser = WEUA_MSOutlookExpress;
    }
    else if (ua.contains("Microsoft-Outlook/")) {
      /* Outlook 2002 mailbox access via http */
      this.browser = WEUA_MSOutlook;
    }
    else if (ua.contains("Microsoft HTTP Post")) {
      /* Outlook 2000 with WebPublishing Assistent */
      this.browser = WEUA_MSWebPublisher;
    }
    else if (ua.contains("Entourage/10")) {
      /* Entourage MacOSX 10.1.4 */
      this.browser = WEUA_Entourage;
    }
    else if (ua.contains("Microsoft-WebDAV-MiniRedir/5")) {
      /* WebFolders Win XP SP 2 */
      this.browser = WEUA_WebFolder;
    }
    else if ((tmp = ua.indexOf("MSIE")) != -1) {
      /* Internet Explorer */
      this.browser = WEUA_IE;
  
      // TODO: IE detection
//      /* go to next space */
//      while (!isspace(*tmp) && (*tmp != '\0')) tmp++;
//      /* skip spaces */
//      while (isspace(*tmp) && (*tmp != '\0')) tmp++;
//      
//      this.browserMajorVersion = atoi(tmp);
//      if ((tmp = index(tmp, '.'))) {
//        tmp++;
//        this.browserMinorVersion = atoi(tmp);
//      }
    }
    else if ((tmp = ua.indexOf("Konqueror")) != -1) {
      /* Konqueror (KDE2 FileManager) */
      this.browser = WEUA_Konqueror;

      // TODO: Konq detection
//      if ((tmp = index(tmp, '/'))) {
//        tmp++;
//        this.browserMajorVersion = atoi(tmp);
//        if ((tmp = index(tmp, '.'))) {
//          tmp++;
//          this.browserMinorVersion = atoi(tmp);
//        }
//      }
    }
    else if ((tmp = ua.indexOf("Netscape6")) != -1) {
      /* Netscape 6 */
      this.browser = WEUA_Netscape;

      // TODO: Netscape detection
//      if ((tmp = index(tmp, '/'))) {
//        tmp++;
//        this.browserMajorVersion = atoi(tmp);
//        if ((tmp = index(tmp, '.'))) {
//          tmp++;
//          this.browserMinorVersion = atoi(tmp);
//        }
//      }
    }
    else if (ua.contains("Lynx")) {
      /* Lynx */
      this.browser = WEUA_Lynx;
    }
    else if (ua.contains("Links")) {
      /* Links */
      this.browser = WEUA_Links;
    }
    else if (ua.contains("gnome-vfs")) {
      /* Links */
      this.browser = WEUA_GNOMEVFS;
    }
    else if (ua.contains("cadaver")) {
      /* Cadaver DAV browser */
      this.browser = WEUA_CADAVER;
    }
    else if (ua.contains("GoLive")) {
      /* Adobe GoLive */
      this.browser = WEUA_GOLIVE;
    }
    else if (ua.contains("DAV.pm")) {
      /* Perl HTTP::DAV */
      this.browser = WEUA_PerlHTTPDAV;
    }
    else if (ua.contains("Darwin") && ua.contains("fetch/")) {
      /* MacOSX 10.0 DAV FileSystem */
      this.browser = WEUA_MACOSX_DAVFS;
    }
    else if (ua.contains("Darwin") && ua.contains("WebDAVFS/")) {
      /* MacOSX DAV FileSystem */
      this.browser = WEUA_MACOSX_DAVFS;
    }
    else if (ua.contains("OmniWeb")) {
      /* OmniWeb */
      this.browser = WEUA_OmniWeb;
    }
    else if (ua.contains("Evolution")) {
      /* Evolution */
      this.browser = WEUA_Evolution;
    }
    else if (ua.contains("Soup/")) {
      /* SOUP (GNOME WebDAV library) */
      this.browser = WEUA_SOUP;
    }
    else if (ua.contains("amaya")) {
      /* W3C Amaya */
      this.browser = WEUA_Amaya;
    }
    else if (ua.contains("NetNewsWire/")) {
      /* NetNewsWire */
      this.browser = WEUA_NetNewsWire;
    }
    else if (ua.contains("Dillo")) {
      /* Dillo */
      this.browser = WEUA_Dillo;
    }
    else if (ua.contains("Java")) {
      /* Java SDK */
      this.browser = WEUA_JavaSDK;
    }
    else if (ua.contains("Python-urllib")) {
      /* Python URL module */
      this.browser = WEUA_PythonURLLIB;
    }
    else if (ua.contains("xmlrpclib.py/")) {
      /* Python XML-RPC module */
      this.browser = WEUA_xmlrpclib_py;
    }
    else if (ua.contains("Emacs")) {
      /* Emacs */
      this.browser = WEUA_Emacs;
    }
    else if (ua.contains("iCab")) {
      /* iCab ?? */
      this.browser = WEUA_iCab;
    }
    else if (ua.contains("Wget")) {
      /* Wget */
      this.browser = WEUA_Wget;
    }
    else if (ua.contains("DAVAccess")) {
      /* Apple MacOSX 10.2.1 / iCal 1.0 DAV Access Framework */
      this.browser = WEUA_AppleDAVAccess;
    }
    else if (ua.contains("DAVKit/")) {
      /* some iCal 1.x DAV Access Framework, report as Apple DAV access */
      this.browser = WEUA_AppleDAVAccess;
    }
    else if (ua.contains("Microsoft Data Access Internet Publishing Provider")) {
      /* WebFolder */
      this.browser = WEUA_WebFolder;
    }
    else if (ua.contains("Microsoft Office Protocol Discovery")) {
      /* Word 2003, treat as WebFolder */
      this.browser = WEUA_WebFolder;
    }
    else if (ua.contains("curl")) {
      /* curl program */
      this.browser = WEUA_CURL;
    }
    else if (ua.contains("Mozilla")) {
      /* other Netscape browser */
      if (ua.contains("Mozilla/5")) {
        this.browser = WEUA_Mozilla;
        this.browserMajorVersion = 5;
      }
      else if (ua.contains("Mozilla/4")) {
        this.browser = WEUA_Netscape;
        this.browserMajorVersion = 4;
      }
      else {
        // TODO: improve log
        System.err.println
          ("Unknown Mozilla Browser: user-agent=" + this.userAgent);
      }
    }
    else if (ua.contains("Morgul")) {
      this.browser = WEUA_Morgul;
    }
    else if (ua.contains("CFNetwork/1.1")) {
      this.browser = WEUA_CFNetwork;
    }
    else if (ua.contains("Kung-Log/")) {
      this.browser = WEUA_KungLog;
    }
    else if (ua.contains("ecto")) {
      this.browser = WEUA_Ecto;
    }
    else if (ua.contains("NewsFire")) {
      this.browser = WEUA_NewsFire;
    }
    else if (ua.contains("Goliath")) {
      this.browser = WEUA_Goliath;
    }
    else if (ua.contains("SOPE/")) {
      this.browser = WEUA_SOPE;
    }
    else if (ua.contains("Mediapartners-Google/")) {
      this.browser = WEUA_Google;
    }
    else {
      /* unknown browser */
      this.browser = WEUA_UNKNOWN;
      
      if (this.userAgent != null) {
        // TODO: improve log
        System.err.println("Unknown WebClient: user-agent=" + this.userAgent);
      }
    }

  }
  
  /* accessors */
  
  public short majorVersion() {
    return this.browserMajorVersion;
  }
  
  public short minorVersion() {
    return this.browserMinorVersion;
  }
  
  /* string accessors */
  
  public String userAgent() {
    return this.userAgent;
  }
  
  public String userAgentType() {
    switch (this.browser) {
      case WEUA_IE:               return "IE";
      case WEUA_Netscape:         return "Netscape";
      case WEUA_Lynx:             return "Lynx";
      case WEUA_Links:            return "Links";
      case WEUA_Opera:            return "Opera";
      case WEUA_Amaya:            return "Amaya";
      case WEUA_Emacs:            return "Emacs";
      case WEUA_Wget:             return "Wget";
      case WEUA_WebFolder:        return "WebFolder";
      case WEUA_DAVFS:            return "DAVFS";
      case WEUA_MACOSX_DAVFS:     return "MacOSXDAVFS";
      case WEUA_CADAVER:          return "Cadaver";
      case WEUA_GOLIVE:           return "GoLive";
      case WEUA_Mozilla:          return "Mozilla";
      case WEUA_OmniWeb:          return "OmniWeb";
      case WEUA_iCab:             return "iCab";
      case WEUA_Konqueror:        return "Konqueror";
      case WEUA_Dillo:            return "Dillo";
      case WEUA_JavaSDK:          return "Java";
      case WEUA_PythonURLLIB:     return "Python-urllib";
      case WEUA_AppleDAVAccess:   return "AppleDAVAccess";
      case WEUA_MSWebPublisher:   return "MSWebPublisher";
      case WEUA_CURL:             return "CURL";
      case WEUA_Evolution:        return "Evolution";
      case WEUA_SOUP:             return "SOUP";
      case WEUA_MSOutlook:        return "MSOutlook";
      case WEUA_MSOutlookExpress: return "MSOutlookExpress";
      case WEUA_GNOMEVFS:         return "GNOME-VFS";
      case WEUA_ZideLook:         return "ZideLook";
      case WEUA_Safari:           return "Safari";
      case WEUA_Entourage:        return "Entourage";
      case WEUA_NetNewsWire:      return "NetNewsWire";
      case WEUA_xmlrpclib_py:     return "xmlrpclib.py";
      case WEUA_Morgul:           return "Morgul";
      case WEUA_KungLog:          return "KungLog";
      case WEUA_Ecto:             return "Ecto";
      case WEUA_NewsFire:         return "NewsFire";
      case WEUA_Goliath:          return "Goliath";
      case WEUA_PerlHTTPDAV:      return "PerlHTTPDAV";
      case WEUA_Google:           return "Google";
      default:                    return "unknown";
    }
  }
  
  public String os() {
    switch (this.os) {
      case WEOS_WINDOWS: return "Windows";
      case WEOS_LINUX:   return "Linux";
      case WEOS_MACOS:   return "MacOS";
      case WEOS_SUNOS:   return "SunOS";
      case WEOS_MAEMO:   return "Maemo";
      case WEOS_SYMBIAN: return "Symbian";
      default:           return "unknown";
    }
  }
  
  public String cpu() {
    switch (this.cpu) {
      case WECPU_IX86:  return "ix86";
      case WECPU_SPARC: return "sparc";
      case WECPU_PPC:   return "ppc";
      default:          return "unknown";
    }
  }
  
  /* browser capabilities */

  public boolean isJavaScriptBrowser() {
    switch (this.browser) {
      case WEUA_Mozilla:
      case WEUA_IE:
      case WEUA_Opera:
      case WEUA_Netscape:
      case WEUA_OmniWeb:
      case WEUA_Konqueror:
        return true;
        
      default:
        return false;
    }
  }
  
  public boolean isVBScriptBrowser() {
    switch (this.browser) {
      case WEUA_IE:
        return true;
      
      default:
        return false;
    }
  }
  
  public boolean isFastTableBrowser() {
    switch (this.browser) {
      case WEUA_Mozilla:
      case WEUA_IE:
      case WEUA_Opera:
        return true;
  
      case WEUA_Safari:
      case WEUA_Konqueror:
        return true;
        
      case WEUA_Netscape:
        return (this.browserMajorVersion >= 6);
        
      default:
        return false;
    }
  }
  
  public boolean isCSS1Browser() {
    switch (this.browser) {
      case WEUA_IE:        return (this.browserMajorVersion >= 4);
      case WEUA_Netscape:  return (this.browserMajorVersion >= 4);
      case WEUA_Opera:     return (this.browserMajorVersion >= 4);
      case WEUA_Safari:    return true;
      case WEUA_Konqueror: return false;
      default:             return false;
    }
  }
  
  public boolean isCSS2Browser() {
    switch (this.browser) {
      case WEUA_IE:        return (this.browserMajorVersion >= 5);
      case WEUA_Netscape:  return (this.browserMajorVersion >= 6);
      case WEUA_Opera:     return (this.browserMajorVersion >= 4);
      case WEUA_Mozilla:   return true;
      case WEUA_Safari:    return true;
      case WEUA_Konqueror: return false;
      default:             return false;
    }
  }
  
  public boolean ignoresCSSOnFormElements() {
    if (this.browser == WEUA_Safari) /* Safari always displays Aqua buttons */
      return true;
    
    return !this.isCSS1Browser();
  }
  
  public boolean isTextModeBrowser() {
    if (this.browser == WEUA_Lynx)  return true;
    if (this.browser == WEUA_Links) return true;
    if (this.browser == WEUA_Emacs) return true;
    return false;
  }
  
  public boolean isIFrameBrowser() {
    if ((this.browser == WEUA_IE) && (this.browserMajorVersion >= 5))
      return true;
    
    /* as suggested in OGo bug #634 */
    if ((this.browser == WEUA_Mozilla) && (this.browserMajorVersion >= 5))
      return true;
    
    return false;
  }
  
  public boolean isXULBrowser() {
    if (this.browser == WEUA_Safari) // TODO: Safari supports some XUL stuff
      return false;
    if ((this.browser == WEUA_Netscape) && (this.browserMajorVersion >= 6))
      return true;
    if (this.browser == WEUA_Mozilla)
      return true;
    return false;
  }
  
  public boolean isRobot() {
    if (this.browser == WEUA_Wget)         return true;
    if (this.browser == WEUA_JavaSDK)      return true;
    if (this.browser == WEUA_PythonURLLIB) return true;
    if (this.browser == WEUA_Google)       return true;
    return false;
  }
  
  public boolean isDAVClient() {
    if (this.browser == WEUA_WebFolder)        return true;
    if (this.browser == WEUA_DAVFS)            return true;
    if (this.browser == WEUA_MACOSX_DAVFS)     return true;
    if (this.browser == WEUA_CADAVER)          return true;
    if (this.browser == WEUA_GOLIVE)           return true;
    if (this.browser == WEUA_AppleDAVAccess)   return true;
    if (this.browser == WEUA_Evolution)        return true;
    if (this.browser == WEUA_SOUP)             return true;
    if (this.browser == WEUA_MSOutlook)        return true;
    if (this.browser == WEUA_MSOutlookExpress) return true;
    if (this.browser == WEUA_GNOMEVFS)         return true;
    if (this.browser == WEUA_ZideLook)         return true;
    if (this.browser == WEUA_Entourage)        return true;
    if (this.browser == WEUA_Morgul)           return true;
    if (this.browser == WEUA_Goliath)          return true;
    if (this.browser == WEUA_PerlHTTPDAV)      return true;
    return false;
  }
  
  public boolean isXmlRpcClient() {
    if (this.browser == WEUA_xmlrpclib_py) return true;
    if (this.browser == WEUA_KungLog)      return true;
    if (this.browser == WEUA_Ecto)         return true;
    return false;
  }
  
  public boolean isBLogClient() {
    if (this.browser == WEUA_KungLog) return true;
    if (this.browser == WEUA_Ecto)    return true;
    return false;
  }
  
  public boolean isRSSClient() {
    if (this.browser == WEUA_NetNewsWire) return true;
    if (this.browser == WEUA_NewsFire)    return true;
    return false;
  }

  public boolean doesSupportCSSOverflow() {
    if (!this.isCSS1Browser())
      return false;

    // TODO: also supported by Firefox now?
    return this.browser == WEUA_IE && this.browserMajorVersion >= 5;
  }
  
  public boolean doesSupportDHTMLDragAndDrop() {
    if (!this.isJavaScriptBrowser())
      return false;
    if (this.os != WEOS_WINDOWS) // TODO: Safari also supports this!
      return false;
    
    return this.browser == WEUA_IE && this.browserMajorVersion >= 5;
  }
  
  public boolean doesSupportXMLDataIslands() {
    return this.browser == WEUA_IE && this.browserMajorVersion >= 5;
  }
  
  public boolean doesSupportUTF8Encoding() {
    if (this.acceptUTF8)
      /* explicit UTF-8 support signaled in HTTP header */
      return true;
    
    switch (this.browser) {
    case WEUA_Mozilla:
    case WEUA_Safari:
    case WEUA_ZideLook:
    case WEUA_Evolution:
    case WEUA_SOUP:
    case WEUA_Morgul:
      /* browser so new, that they always supported UTF-8 ... */
      return true;
    case WEUA_IE:
      if (this.browserMajorVersion >= 5)
        return true;
      return false; // TODO: find out, whether IE 4 gurantees UTF-8 support
    default:
      return false;
    }
  }

  /* user-agent (it's better to use ^capabilities !) */

  public boolean isInternetExplorer() {
    return this.browser == WEUA_IE;
  }
  public boolean isInternetExplorer5() {
    return this.browser == WEUA_IE && this.browserMajorVersion == 5;
  }
  public boolean isNetscape() {
    return this.browser == WEUA_Netscape;
  }
  public boolean isNetscape6() {
    return this.browser == WEUA_Netscape && this.browserMajorVersion == 6;
  }
  public boolean isLynx() {
    return this.browser == WEUA_Lynx;
  }
  public boolean isOpera() {
    return this.browser == WEUA_Opera;
  }
  public boolean isAmaya() {
    return this.browser == WEUA_Amaya;
  }
  public boolean isEmacs() {
    return this.browser == WEUA_Emacs;
  }
  public boolean isWget() {
    return this.browser == WEUA_Wget;
  }
  public boolean isWebFolder() {
    return this.browser == WEUA_WebFolder;
  }
  public boolean isMozilla() {
    return this.browser == WEUA_Mozilla;
  }
  public boolean isOmniWeb() {
    return this.browser == WEUA_OmniWeb;
  }
  public boolean isICab() {
    return this.browser == WEUA_iCab;
  }
  public boolean isKonqueror() {
    return this.browser == WEUA_Konqueror;
  }

/* OS */

  public boolean isWindowsBrowser() {
    return this.os == WEOS_WINDOWS;
  }
  public boolean isLinuxBrowser() {
    return this.os == WEOS_LINUX || this.os == WEOS_MAEMO;
  }
  public boolean isMacBrowser() {
    return this.os == WEOS_MACOS;
  }
  public boolean isSunOSBrowser() {
    return this.os == WEOS_SUNOS;
  }

  public boolean isUnixBrowser() {
    switch (this.os) {
      case WEOS_LINUX:
      case WEOS_SUNOS:
      case WEOS_MAEMO:
        return true;
      default:
        return false;
    }
  }
  
  public boolean isX11Browser() {
    if (this.isTextModeBrowser())
      return false;
    if (!this.isUnixBrowser()) /* well, could be Win/X11 ... ;-) */
      return false;
    
    return true;
  }

  /* constants */
  
  public static final short WEUA_UNKNOWN          = 0;
  public static final short WEUA_IE               = 1;
  public static final short WEUA_Netscape         = 2;
  public static final short WEUA_Lynx             = 3;
  public static final short WEUA_Opera            = 4;
  public static final short WEUA_Amaya            = 5;
  public static final short WEUA_Emacs            = 6;
  public static final short WEUA_Wget             = 7;
  public static final short WEUA_WebFolder        = 8;
  public static final short WEUA_Mozilla          = 9;
  public static final short WEUA_OmniWeb          = 10;
  public static final short WEUA_iCab             = 11;
  public static final short WEUA_Konqueror        = 12;
  public static final short WEUA_Links            = 13;
  public static final short WEUA_DAVFS            = 14;
  public static final short WEUA_CADAVER          = 15;
  public static final short WEUA_GOLIVE           = 16;
  public static final short WEUA_MACOSX_DAVFS     = 17;
  public static final short WEUA_Dillo            = 18;
  public static final short WEUA_JavaSDK          = 19;
  public static final short WEUA_PythonURLLIB     = 20;
  public static final short WEUA_AppleDAVAccess   = 21;
  public static final short WEUA_MSWebPublisher   = 22;
  public static final short WEUA_CURL             = 23;
  public static final short WEUA_Evolution        = 24;
  public static final short WEUA_MSOutlook        = 25;
  public static final short WEUA_MSOutlookExpress = 26;
  public static final short WEUA_GNOMEVFS         = 27;
  public static final short WEUA_ZideLook         = 28;
  public static final short WEUA_Safari           = 29;
  public static final short WEUA_SOUP             = 30;
  public static final short WEUA_Entourage        = 31;
  public static final short WEUA_NetNewsWire      = 32;
  public static final short WEUA_xmlrpclib_py     = 33;
  public static final short WEUA_Morgul           = 34;
  public static final short WEUA_CFNetwork        = 35;
  public static final short WEUA_KungLog          = 36;
  public static final short WEUA_SOPE             = 37;
  public static final short WEUA_Ecto             = 38;
  public static final short WEUA_NewsFire         = 39;
  public static final short WEUA_Goliath          = 40;
  public static final short WEUA_PerlHTTPDAV      = 41;
  public static final short WEUA_Google           = 42;

  public static final short WEOS_UNKNOWN   = 0;
  public static final short WEOS_WINDOWS   = 1;
  public static final short WEOS_LINUX     = 2;
  public static final short WEOS_MACOS     = 3;
  public static final short WEOS_SUNOS     = 4;
  public static final short WEOS_MAEMO     = 5;
  public static final short WEOS_SYMBIAN   = 6;

  public static final short WECPU_UNKNOWN  = 0;
  public static final short WECPU_IX86     = 1;
  public static final short WECPU_SPARC    = 2;
  public static final short WECPU_PPC      = 3;
}
