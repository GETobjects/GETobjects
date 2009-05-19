/*
  Copyright (C) 2006-2008 Helge Hess

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
package org.getobjects.eoaccess;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.derby.EODerbyAdaptor;
import org.getobjects.eoaccess.frontbase.EOFrontbaseAdaptor;
import org.getobjects.eoaccess.mysql.EOMySQLAdaptor;
import org.getobjects.eoaccess.postgresql.EOPostgreSQLAdaptor;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UMap;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

/**
 * EOAdaptor
 * <p>
 * Wraps a JDBC adaptor. In "real" EOF we would use a subclass, but its not
 * (yet) considered worth the effort ;-)
 *
 * <p>
 * This object can be used for very simple, but pooled, SQL access. Just use
 * the performSQL() and performUpdateSQL() methods. Those will acquire an
 * appropriate EOAdaptorChannel and perform the given SQL.
 * <br>
 * Example:
 * <pre>
 * List persons = adaptor.performSQL("SELECT * FROM Person");</pre>
 * If you need transactions, you can either embed BEGIN/COMMIT in the SQL or
 * use an EOAdaptorChannel.
 *
 * <p>
 * However, its more common and recommended to use EODatabase and
 * EODatabaseChannel. Example:
 * <pre>
 *   EODatabase         db = new EODatabase(adaptor, null);
 *   EOActiveDataSource ds = db.dataSourceForEntity("person");
 *   List persons = ds.fetch();</pre>
 *
 * <p>
 * The adaptor also works as a SQL connection pool.
 * <p>
 * Connection Properties processed by EOAdaptor:
 * <ul>
 *   <li>EOAdaptorMaxPoolSize         (default: 64)
 *   <li>EOAdaptorMaxChannelWaitTime  (default: 3000ms)
 *   <li>EOAdaptorMaxChannelAge       (default: 120s)
 *   <li>EOAdaptorMaintenanceInterval (default: 180s)
 * </ul>
 * <p>
 * THREAD: this class, especially the connection pool, is thread safe. You
 *         should usually use one adaptor per login-configuration.
 * <p>
 * @see EODatabaseChannel
 * @see EODatabase
 */
public class EOAdaptor extends NSObject implements NSDisposable {
  protected static final Log log = LogFactory.getLog("EOAdaptor");

  protected String     url;
  protected Properties connectionProperties;

  protected int     maxConnections;
  protected long    maxChannelWaitTimeInMS;
  protected long    maxChannelAgeInSeconds;

  protected List<EOAdaptorChannel> availableChannels;
  protected List<EOAdaptorChannel> checkedOutChannels;

  protected Timer   maintenanceTimer = null;
  protected int     openCountSinceLastMaintenance    = 0;
  protected int     releaseCountSinceLastMaintenance = 0;
  protected long    maintenanceTimeOutInSeconds;

  protected EOModel model;
  protected EOModel modelPattern;
  protected long    modelFetchTime = 0;
  public    int     modelRefetchTimeout = 3; /* in seconds */

  public static EOAdaptor adaptorWithURL
    (String _url, Properties _p, EOModel _model)
  {
    EOAdaptor adaptor = null;


    if (_url == null)
      return null;
    else if (_url.startsWith("jdbc:mysql"))
      adaptor = new EOMySQLAdaptor(_url, _p, _model);
    else if (_url.startsWith("jdbc:postgres"))
      adaptor = new EOPostgreSQLAdaptor(_url, _p, _model);
    else if (_url.startsWith("jdbc:derby"))
      adaptor = new EODerbyAdaptor(_url, _p, _model);
    else if (_url.toLowerCase().startsWith("jdbc:frontbase"))
      adaptor = new EOFrontbaseAdaptor(_url, _p, _model);
    else {
      log.warn("no specific adaptor for url: " + _url);
      adaptor = new EOAdaptor(_url, null, _model);
    }

    if (adaptor == null)
      return null;
    if (!adaptor.loadDriver()) {
      log.error("failed to load driver for JDBC URL: " + _url);
      return null;
    }

    return adaptor;
  }
  public static EOAdaptor adaptorWithURL(String _url, EOModel _model) {
    return adaptorWithURL(_url, null /* properties */, _model);
  }
  public static EOAdaptor adaptorWithURL(String _url) {
    return adaptorWithURL(_url, null /* properties */, null /* model */);
  }

  /* the constructor */

  protected EOAdaptor(String _url, Properties _p, EOModel _model) {
    super();

    this.url                  = _url;
    this.connectionProperties = _p;
    this.loadConfigurationFromProperties(_p);

    this.availableChannels  = new ArrayList<EOAdaptorChannel>(16);
    this.checkedOutChannels = new ArrayList<EOAdaptorChannel>(16);

    if (_model != null) {
      if (_model.isPatternModel())
        this.modelPattern = _model;
      else
        this.model = _model;
    }
  }

  public void loadConfigurationFromProperties(Properties _p) {
    this.maxConnections         = 64;
    this.maxChannelWaitTimeInMS = 3 * 1000;
    this.maxChannelAgeInSeconds = 2 * 60;
    this.maintenanceTimeOutInSeconds = 3 * 60;

    if (_p == null)
      return;

    /* how many connections will we store in the pool? */
    Object v = _p.getProperty("EOAdaptorMaxPoolSize");
    if (v != null)
      this.maxConnections = UObject.intValue(v);

    /* How long will we wait for a channel to become available (prior returning
     * an error).
     */
    if ((v = _p.getProperty("EOAdaptorMaxChannelWaitTime")) != null)
      this.maxChannelWaitTimeInMS = UObject.intValue(v);

    /* Maximum age of a channel before it is retired (so that we don't keep
     * channels open forever)
     */
    if ((v = _p.getProperty("EOAdaptorMaxChannelAge")) != null)
      this.maxChannelAgeInSeconds = UObject.intValue(v);

    /* How often will we check whether we should retire channels, etc */
    if ((v = _p.getProperty("EOAdaptorMaintenanceInterval")) != null)
      this.maintenanceTimeOutInSeconds = UObject.intValue(v);
  }

  /* accessors */

  public String url() {
    return this.url;
  }

  /* loading a JDBC driver */

  protected boolean loadDriver() {
    /* this should be overridden by subclasses */
    return true;
  }

  /* testing connection */

  public static boolean testConnect(String _url, String _login, String _pwd) {
    try {
      Connection con = _login != null
        ? DriverManager.getConnection(_url, _login, _pwd)
        : DriverManager.getConnection(_url);
      con.close();
    }
    catch (SQLException e) {
      if (log.isInfoEnabled()) log.info("test connect failed: " + _url, e);
      return false;
    }
    return true;
  }

  public boolean testConnect() {
    EOAdaptorChannel channel;

    channel = this.openChannelAndRegisterInPool();
    if (channel == null) {
      log.info("failed to acquire a connection to the DB: " + this.url);
      return false;
    }

    log.debug("managed to acquired a connection to the DB: " + this.url);
    this.releaseChannel(channel);
    return true;
  }

  /* methods */

  public boolean hasOpenChannels() {
    /* Note: this only tracks pooled connections */
    final int coCount, availCount;
    synchronized (this) {
      coCount    = this.checkedOutChannels.size();
      availCount = this.availableChannels.size();
    }
    return coCount > 0 || availCount > 0;
  }

  protected EOAdaptorChannel checkoutFirstAvailableChannel() {
    // this needs to run inside the monitor
    for (EOAdaptorChannel availChannel: this.availableChannels) {
      if (this.shouldKeepChannel(availChannel)) {
        this.checkedOutChannels.add(availChannel);
        this.availableChannels.remove(availChannel);
        return availChannel;
      }
      // else
      //  needsMaintenance = true; /* found an invalid entry */
    }
    return null;
  }

  /**
   * This attempts to acquire a channel from the connection pool, or open a
   * new one if the maximum channel count has not been reached.
   * It calls openChannelAndRegisterInPool to open a new channel and
   * register that channel in the pool.
   * The difference is that openChannelAndRegisterInPool always opens a new
   * connection while this method first attempts to reuse a connection from
   * the pool.
   *
   * @param _attempt How often the adaptor tried to open a channel. The
   *   adaptor will wait for a channel to become available (being put back
   *   into the pool by another thread) if the maximum number of connections
   *   has been reached.
   *
   * @return The EOAdaptorChannle which was opened or acquired or null if the
   *   opening failed or the timeout expired.
   */
  protected EOAdaptorChannel openChannelFromPool(int _attempt) {
    final boolean isDebugOn = log.isDebugEnabled();
    final boolean isInfoOn  = log.isInfoEnabled();
    if (isInfoOn)
      log.info("open channel from pool: (attempt=" + _attempt + ")");

    /* first look for an available connection */
    EOAdaptorChannel channel = null;
    boolean needsMaintenance = false;
    boolean mayCreate        = false;

    synchronized (this) {
      this.openCountSinceLastMaintenance++;

      channel = this.checkoutFirstAvailableChannel();
      if (channel == null)
        mayCreate = this.checkedOutChannels.size() < this.maxConnections;
    }

    /* create a new connection if required */

    if (channel == null) {
      if (mayCreate) {
        if (isDebugOn) log.info("  no avail connection, create ...");
        channel = this.openChannelAndRegisterInPool();
      }
      else {
        log.warn("out of pool connections, attempt: " + _attempt);
        channel = this.primaryWaitForChannel(_attempt);
      }
    }

    /* perform maintenance */

    if (needsMaintenance || this.shouldMaintainPool()) {
      if (isInfoOn) log.info("  maintain ...");
      this.maintainPool();
    }

    /* return */

    if (channel == null)
      return null;

    if (isInfoOn) log.info("connection: " + channel.connection);
    return channel;
  }

  protected EOAdaptorChannel primaryWaitForChannel(int _attempt) {
    // TODO: improve method name

    final int coCount, availCount;
    synchronized (this) {
      coCount    = this.checkedOutChannels.size();
      availCount = this.availableChannels.size();
    }
    log.warn("  checked-out=" + coCount + " / avail=" + availCount +
             " / max=" + this.maxConnections);

    if (_attempt > 3) {
      log.error("too many attempts to open a SQL connection, stop" +
                _attempt);
      return null;
    }

    /* wait a while for a connection to become available */

    boolean gotInterrupted = false;
    final long startTime = new Date().getTime();

    EOAdaptorChannel channel = null;
    synchronized(this) {
      /* the loop is needed to protect against spurious wakeups */
      while (channel == null) {
        /* wait a bit for a channel to become available (we get notified) */
        try {
          /* Release the surrounding lock and wait for notification or
           * timeout.
           *
           * Note: using maxChannelWaitTime is not entirely correct, this
           *       might make us wait up to twice as long.
           */
          this.wait(this.maxChannelWaitTimeInMS /* ms */);
        }
        catch (InterruptedException e) {
          /* OK, we got Interrupted, so we stop doing anything. */
          gotInterrupted = true;
          break; /* leave while loop */
        }

        /* ok, we did wait, so lets check whether there is a connection */
        //System.err.println("SCAN AVAIL: "+this.availableChannels.size());
        channel = this.checkoutFirstAvailableChannel();
        if (channel != null)
          break;

        /* we didn't get a channel, check whether we should still wait */
        long timePassedInMS = new Date().getTime() - startTime;
        if (timePassedInMS > this.maxChannelWaitTimeInMS /* ms */)
          break; /* waited enough, stop waiting */
      }
    }

    if (channel != null) {
      log.info("found a free channel after a bit of waiting.");
      return channel;
    }

    if (gotInterrupted)
      log.error("  wait got interrupted ...");

    log.error("  failed to wait for a free channel ...");
    return channel;
  }

  /**
   * This attempts to acquire a channel from the connection pool, or open a
   * new one if the maximum channel count has not been reached.
   * It calls openChannelFromPool(1) which does all the hard work.
   *
   * The difference to openChannelAndRegisterInPool is that this method first
   * attempts to reuse a connection from the pool before opening a new SQL
   * connection.
   *
   * Note: this is the preferred way to aquire a channel from the adaptor as
   *       it uses the pool.
   *
   * @return The EOAdaptorChannle which was opened or acquired or null if the
   *   opening failed or the timeout expired.
   */
  public EOAdaptorChannel openChannelFromPool() {
    return this.openChannelFromPool(1);
  }

  public void releaseChannel(EOAdaptorChannel _channel, boolean _keepConnect) {
    if (_channel == null)
      return;

    final boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn)
      log.info("releasing channel: " + _channel);

    if (_channel.isInTransaction()) {
      log.error("channel to be released has an open transaction, " +
                "attempting a rollback ...");
      Exception error = _channel.rollback();
      if (error != null) {
        log.error("rollback failed, will close the channel ...");
        _keepConnect = false;
      }
    }

    boolean didKeepEntry = _keepConnect;

    synchronized (this) {
      this.releaseCountSinceLastMaintenance++;

      this.checkedOutChannels.remove(_channel);

      if (_keepConnect && this.shouldKeepChannel(_channel))
        this.availableChannels.add(_channel);
      else
        didKeepEntry = false;
    }

    if (didKeepEntry && isDebugOn && _channel != null)
      log.debug("  kept entry: " + _channel);

    if (!didKeepEntry)
      _channel.close();

    if (!didKeepEntry || this.shouldMaintainPool())
      this.maintainPool();

    /* now notify other threads waiting for a free connection */

    synchronized (this) {
      this.notifyAll();
    }

    if (isDebugOn)
      log.debug("finished release.");
  }

  /**
   * Put a channel back into the pool. If you aquired a connection from the
   * adaptor using openChannelFromPool() or openChannelAndRegisterInPool(),
   * use this method to put it back into the pool. Do *not* close it manually!
   *
   * If you detected an error in the channel, use releaseAfterError()
   * instead. This will (usually) close the channel instead of adding it to
   * the pool.
   *
   * @param _channel
   */
  public void releaseChannel(EOAdaptorChannel _channel) {
    this.releaseChannel(_channel, true /* try to keep connection */);
  }

  /**
   * Use this method to close a channel if you detected an error. If everything
   * went fine, use releaseChannel() to give it back to the pool.
   *
   * @see releaseChannel(EOAdaptorChannel _channel)
   *
   * @param _channel The channel to be closed.
   * @param _e The error which occurred on the connection.
   */
  public void releaseAfterError(EOAdaptorChannel _channel, Exception _e) {
    log.info("releasing connection after error.");
    this.releaseChannel(_channel, false /* do not try to keep */);
  }

  /**
   * This creates a new connection which is not registered in the connection
   * pool. When you are done, directly close the channel. Do not use the
   * adaptor.releaseChannel() method.
   *
   * @see openChannelFromPool()
   */
  public EOAdaptorChannel openChannel() {
    Connection connection = null;

    log.info("opening channel ...");
    try {
      connection = this.connectionProperties != null
        ? DriverManager.getConnection(this.url, this.connectionProperties)
        : DriverManager.getConnection(this.url);
    }
    catch (SQLException e) {
      log.error("failed to create new SQL connection", e);

      final int coCount, availCount;
      synchronized (this) {
        coCount    = this.checkedOutChannels.size();
        availCount = this.availableChannels.size();
      }
      log.error("  checked out=" + coCount + "/avail " + availCount);
      return null;
    }

    /* create entry */

    return this.primaryCreateChannelForConnection(connection);
  }
  protected EOAdaptorChannel primaryCreateChannelForConnection(Connection _c) {
    /* can be overridden by subclasses to provide specific channels */
    return new EOAdaptorChannel(this, _c);
  }

  /**
   * This calls openChannel() to open a new connection to the database and then
   * registers the result in the pool.
   * Use releaseChannel() to free channels you got using this method! Do not
   * close it directly.
   *
   * You wouldn't usually want to call this method because it always opens a
   * new channel. Use openChannelFromPool() instead, this method first checks
   * whether a free connection is available in the pool.
   *
   * @return The fresh EOAdaptorChannel or null if something went wrong.
   */
  public EOAdaptorChannel openChannelAndRegisterInPool() {
    final EOAdaptorChannel channel = this.openChannel();
    if (channel == null)
      return null;

    /* register entry */

    synchronized (this) {
      this.checkedOutChannels.add(channel);

      if (this.maintenanceTimer == null) {
        /* Note: if we do not create a daemon thread, the timer thread won't
         *       allow apps to stop!
         */
        this.maintenanceTimer = new Timer("EOAdaptorPool", true /* daemon */);
        this.maintenanceTimer.scheduleAtFixedRate
          (new MaintenanceTimerTask(this),
           1    /* delay in ms */,
           this.maintenanceTimeOutInSeconds * 1000 /* period in ms */);
      }
    }

    return channel;
  }

  /* pool maintenance */

  protected boolean shouldMaintainPool() {
    synchronized (this) {
      // TODO: find a good maintenance interval
      if (this.releaseCountSinceLastMaintenance > 64)
        return true;
      if (this.openCountSinceLastMaintenance > 64)
        return true;
    }
    return false;
  }

  public void closeAllPooledChannels() {
    /* Can be used to close all unused channels. */
    List<EOAdaptorChannel> toBeClosed = new ArrayList<EOAdaptorChannel>(4);

    /* collect expired connections */

    synchronized (this) {
      toBeClosed.addAll(this.availableChannels);
      this.availableChannels.clear();
    }

    /* close channels */

    for (EOAdaptorChannel entry: toBeClosed)
      this.closeChannel(entry);
  }

  public void maintainPool() {
    final boolean debugOn = log.isDebugEnabled();

    /* scan pool for connections which should be closed */
    if (debugOn) log.debug("running maintenance ...");

    List<EOAdaptorChannel> toBeClosed = null;
    Timer timer = null;

    /* collect expired connections */

    synchronized (this) {
      /* scan for connections which should be closed */

      for (EOAdaptorChannel entry: this.availableChannels) {
        if (!this.shouldKeepChannel(entry)) {
          if (toBeClosed == null)
            toBeClosed = new ArrayList<EOAdaptorChannel>(4);

          toBeClosed.add(entry);
        }
      }

      /* remove expired entries */
      if (toBeClosed != null) {
        if (debugOn) log.debug("  removing expired entries ...");
        for (EOAdaptorChannel entry: toBeClosed)
          this.availableChannels.remove(entry);
      }

      /* stop timer if we don't need it anymore */
      if (this.maintenanceTimer != null &&
          this.availableChannels.size()  == 0 &&
          this.checkedOutChannels.size() == 0)
      {
        timer = this.maintenanceTimer;
        this.maintenanceTimer = null;
      }

      this.openCountSinceLastMaintenance    = 0;
      this.releaseCountSinceLastMaintenance = 0;
    }

    /* close expired entries */

    if (toBeClosed != null) {
      if (debugOn) log.debug("  closing expired entries ...");
      for (EOAdaptorChannel entry: toBeClosed)
        this.closeChannel(entry);
    }

    /* stop timer */

    if (timer != null) {
      /* Hm, will this do a hard cancel to our thread? Not really relevant
       * since we are done anyways ;-).
       */
      timer.cancel();
      timer = null;
    }

    if (debugOn) log.debug("pool maintenance done.");
  }

  /* non-synchronized methods */

  protected void closeChannel(EOAdaptorChannel _entry) {
    if (_entry == null)
      return;

    log.debug("closing entry: " + _entry);

    if (_entry.connection == null) /* nothing to tear down */
      return;

    try {
      if (!_entry.connection.isClosed())
        _entry.connection.close();
    }
    catch (SQLException e) {
      log.debug("SQL exception while tearing down the connection", e);
    }
  }

  /**
   * Locates the EOAdaptorChannel which wraps the given connection. This works
   * by scanning the checkedOutChannels ivar.
   * 
   * @param _c - the JDBC Connection object to check
   * @return the EOAdaptorChannel which manages the Connection, or null
   */
  protected EOAdaptorChannel findChannelForConnection(final Connection _c) {
    // TBD: unused, can be dropped?
    for (EOAdaptorChannel entry: this.checkedOutChannels) {
      if (entry.connection == _c)
        return entry;
    }
    return null;
  }

  /**
   * Checks whether the EOAdaptorChannel should be put into the connection
   * pool.
   * This checks whether the number of connections exceeds the 'maxConnection'
   * setting, it checks whether the channel is closed and it checks whether the
   * age of the channel exceeds the 'maxChannelAgeInSeconds'.
   * <p>
   * IMPORTANT: must run in a synchronized section.
   * 
   * @param _channel - the EOAdaptorChannel
   * @return true if the channel should be reused, false otherwise
   */
  protected boolean shouldKeepChannel(final EOAdaptorChannel _channel) {
    // Note: must be run inside a monitor!

    if (_channel == null)
      return false;

    if (this.maxConnections <= this.availableChannels.size()) {
      log.info("reached connection pool limit: " + this.maxConnections);
      return false;
    }

    if (_channel.connection == null)
      return false;

    try {
      if (_channel.connection.isClosed())
        return false;
    }
    catch (SQLException e) {
      log.debug("catched exception while checking close status", e);
      return false;
    }

    if (_channel.ageInSeconds() > this.maxChannelAgeInSeconds)
      return false;

    return true;
  }


  /* high level convenience methods which maintain their connection */

  /**
   * This is a quick&dirty method to perform fetches in the database. It
   * acquires a channel from the pool, runs the SQL and gives back the channel.
   * The fetched rows are returned, or null on error. There is no way
   * to retrieve error exceptions using this method.
   * <p>
   * Example:<pre>
   *   List records = adaptor.performSQL("SELECT * FROM sessiong_log");
   *   System.out.println("db rows: " + records);</pre>
   * <p>
   * <strong>Careful</strong>:
   * Do not introduce SQL injection issues by adding unquoted values
   * to the SQL string! Use the EOSQLExpression class for database independend
   * quoting, eg:<pre>
   *   EOSQLExpression e = ad.expressionFactory().createExpression(null);
   *   s = e.formatValueForAttribute(";DELETE * FROM accounts", null);</pre>
   *
   * @param _sql - the SQL to execute
   * @return a List of Maps representing the database records or null on error
   */
  public List<Map<String, Object>> performSQL(final String _sql) {
    // TODO: fix return type
    if (_sql == null || _sql.length() == 0)
      return null;

    final EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) return null;

    final List<Map<String, Object>> result = channel.performSQL(_sql);
    if (result == null) { /* failed with some error */
      final Exception error = channel.consumeLastException();
      if (error != null)
        log.warn("performSQL() failed: " + _sql, error);

      this.releaseAfterError(channel, error); /* do not keep channel */
      return null;
    }

    this.releaseChannel(channel);
    return result;
  }

  /**
   * Translates the EOFetchSpecification into a SQL query and evaluates it
   * using a channel.
   * 
   * @param _fs - the EOFetchSpecification to perform
   * @return null on error, or a List containing the raw fetch results
   */
  public List<Map<String, Object>> performSQL(final EOFetchSpecification _fs) {
    if (_fs == null)
      return null;
    
    final EOSQLExpression e = this.expressionFactory().createExpression(null);
    e.prepareSelectExpressionWithAttributes(null, _fs.locksObjects(), _fs);

    final EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) return null;

    final List<Map<String, Object>> result = channel.evaluateQueryExpression(e);
    if (result == null) { /* failed with some error */
      final Exception error = channel.consumeLastException();
      if (error != null)
        log.warn("performSQL() failed: " + e, error);

      this.releaseAfterError(channel, error); /* do not keep channel */
      return null;
    }

    this.releaseChannel(channel);
    return result;
  }
  

  /**
   * Creates a pattern EOFetchSpecification (EOCustomQueryExpressionHintKey)
   * and evaluates it using a channel.
   * <p>
   * Possible arguments:
   * <ul>
   *   <li>q / qualifier (EOQualifier or String, eg "name LIKE 'H*'")
   *   <li>sort (EOSortOrdering[]/EOSortOrdering/String, eg "name,-date")
   *   <li>distinct (bool)
   *   <li>offset
   *   <li>limit
   * </ul>
   * All remaining keys are evaluated as qualifier bindings.
   * <p>
   * Examples:<pre>
   *   ad.performSQL("SELECT * FROM accounts %(where)s",
   *     "q", "name LIKE $query", "query", F("q"));
   *   
   *   this.results = this.application.db.adaptor().performSQL(
   *     "SELECT DISTINCT function FROM employment" +
   *     " %(where)s ORDER BY function ASC %(limit)s",
   *     "limit", limit, "q", "function LIKE '" + this.F("q").trim() + "*'");
   *     // 'limit' is not a variable, its the fetchspec limit (hardcoded)
   *     // you can use variables inside qualifiers, but not in the SQL
   * </pre>
   * For a discussion of the available %(xyz)s patterns, check the
   * EOSQLExpression class.
   * <p>
   * Note: be careful wrt SQL injection! (parameters are good, building query
   * strings using + is bad!)
   * 
   * <p>
   * @param _sqlpat - the SQL pattern, see EOSQLExpression for possible patterns
   * @param _args   - args and bindings in a varargs array
   * @return null on error, or a List containing the raw fetch results
   */
  public List<Map<String, Object>> performSQL
    (final String _sqlpat, final Object... _args)
  {
    return performSQL(buildVarArgsFetchSpec(_sqlpat, _args));
  }
  
  /**
   * Creates a pattern EOFetchSpecification (EOCustomQueryExpressionHintKey).
   * <p>
   * Possible arguments:
   * <ul>
   *   <li>q / qualifier (EOQualifier or String, eg "name LIKE 'H*'")
   *   <li>sort (EOSortOrdering[]/EOSortOrdering/String, eg "name,-date")
   *   <li>distinct (bool)
   *   <li>offset
   *   <li>limit
   * </ul>
   * All remaining keys are evaluated as qualifier bindings.
   * </pre>
   * For a discussion of the available %(xyz)s patterns, check the
   * EOSQLExpression class.
   * <p>
   * Note: be careful wrt SQL injection! (parameters are good, building query
   * strings using + is bad!)
   * 
   * <p>
   * @param _sqlpat - the SQL pattern, see EOSQLExpression for possible patterns
   * @param _args   - args and bindings in a varargs array
   * @return the new EOFetchSpecification
   */
  @SuppressWarnings("unchecked")
  public static EOFetchSpecification buildVarArgsFetchSpec
    (final String _sqlpat, final Object[] _args)
  {
    EOQualifier      q   = null;
    EOSortOrdering[] sos = null;
    boolean          distinct = false;
    Number           limit    = null;
    Number           offset   = null;
    Object o;
    
    final Map<String, Object> args = UMap.createArgs(_args);
    
    if ((o = args.remove("qualifier")) == null)
      o = args.remove("q"); /// allow 'q' and 'qualifier'
    if (o instanceof String)
      q = EOQualifier.parse((String)o);
    else if (o instanceof EOQualifier)
      q = (EOQualifier)o;

    if ((o = args.remove("sort")) != null) {
      if (o instanceof String)
        sos = EOSortOrdering.parse((String)o); // eg: name,-date
      else if (o instanceof EOSortOrdering[])
        sos = (EOSortOrdering[])o;
      else if (o instanceof EOSortOrdering)
        sos = new EOSortOrdering[] { (EOSortOrdering)o };
    }

    if ((o = args.remove("distinct")) != null)
      distinct = UObject.boolValue(o);
    
    if ((o = args.remove("offset")) != null)
      offset = UObject.intValue(o);
    if ((o = args.remove("limit")) != null)
      limit = UObject.intValue(o);
    
    if (q != null && args.size() > 0)
      q = q.qualifierWithBindings(args, false /* do not require all */);
    
    EOFetchSpecification fs = new EOFetchSpecification();
    fs.setQualifier(q);
    fs.setSortOrderings(sos);
    fs.setFetchesRawRows(true);
    fs.setFetchesReadOnly(true);
    fs.setUsesDistinct(distinct);
    if (offset != null) fs.setFetchOffset(offset.intValue());
    if (limit  != null) fs.setFetchLimit(limit.intValue());
    if (_sqlpat != null)
      fs.setHint(EOSQLExpression.EOCustomQueryExpressionHintKey, _sqlpat);
    return fs;
  }
  
  /**
   * This is a quick&dirty method to perform updates in the database. It
   * acquires a channel from the pool, runs the SQL and gives back the channel.
   * The number of affected rows is returned, or -1 on error. There is no way
   * to retrieve error exceptions using this method.
   * <p>
   * Example:<pre>
   *   int affected = adaptor.performUpdateSQL("DELETE FROM session_log");
   *   System.out.println("we deleted " + affected + " rows in the DB!");</pre>
   *
   *
   * @param _sql - the SQL (UPDATE/INSERT/DELETE) to execute
   * @return number of affected records or -1 on error
   */
  public int performUpdateSQL(final String _sql) {
    if (_sql == null || _sql.length() == 0)
      return -1;

    final EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) return -1;

    int result = channel.performUpdateSQL(_sql);
    if (result < 0) { /* failed with some error */
      final Exception error = channel.consumeLastException();
      if (error != null)
        log.warn("performUpdateSQL() failed: " + _sql, error);

      this.releaseChannel(channel, false /* do not keep channel */);
      return result;
    }

    this.releaseChannel(channel);
    return result;
  }

  /**
   * Convenience method which fetches exactly one record. Example:<pre>
   *   Map record = adaptor.fetchRecord("persons", "company_id", 10000);</pre>
   * 
   * @param _table - name of table, eg 'persons'
   * @param _field - column to check, usually the primary key (eg 'id')
   * @param _value - value of the column
   * @return the record as a Map, or null if the record was not found
   */
  public Map<String, Object> fetchRecord
    (final String _table, final String _field, final Object _value)
  {
    /* generate SQL */

    final String sql = this.generateSQLToFetchRecord(_table, _field, _value);
    if (sql == null) return null;

    /* run query */

    final List<Map<String, Object>> records = this.performSQL(sql);
    if (records == null)
      return null;

    if (records.size() == 0) {
      log.debug("found no matching record in table " + _table + ": " +
                     _field + " = " + _value);
      return null;
    }
    if (records.size() > 1) {
      log.warn("found multiple matches for fetchRecord, table " +
                     _table + ": " + _field + " = " + _value);
    }

    return records.get(0);
  }
  /**
   * Generates SQL to fetch a single record, example:<pre>
   *   SELECT * FROM persons WHERE id = 10</pre>
   * 
   * @param _table - name of table, eg 'persons'
   * @param _field - column to check, usually the primary key (eg 'id')
   * @param _value - value of the column
   * @return the record as a Map, or null if the record was not found
   */
  public String generateSQLToFetchRecord
    (final String _table, final String _field, final Object _value)
  {
    if (_table.length() < 1 || _field.length() < 1)
      return null;
    if (_value == null)
      return null;

    /* generate SQL */

    final EOSQLExpression e = this.expressionFactory().createExpression(null);
    final StringBuilder sql = new StringBuilder(255);

    sql.append("SELECT * FROM ");
    sql.append(e.sqlStringForSchemaObjectName(_table));
    sql.append(" WHERE ");
    sql.append(e.sqlStringForSchemaObjectName(_field));
    sql.append(" = ");
    sql.append(e.formatValueForAttribute(_value, null /* attribute */));
    sql.append(" LIMIT 2"); /* 2 so that we can detect multiple records */
    return sql.toString();
  }

  public boolean insertRow(final String _table, Map<String, Object> _record) {
    if (_table == null || _record == null)
      return false;

    final EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) return false;

    if (!channel.insertRow(_table, _record)) {
      this.releaseChannel(channel, false /* do not keep channel */);
      return false;
    }

    this.releaseChannel(channel);
    return true;
  }

  public boolean updateRow
    (String _table, String _pkey, Object _value, Map<String, Object> _record)
  {
    // Note: this does not support insertion of NULLs
    if (_table == null || _record == null)
      return false;

    final EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) return false;

    if (!channel.updateRow(_table, _pkey, _value, _record)) {
      this.releaseChannel(channel, false /* do not keep channel */);
      return false;
    }

    this.releaseChannel(channel);
    return true;
  }

  
  /* quoting SQL expressions */
  // TODO: maybe we want to move that to an own per-adaptor object?

  /**
   * Method used for quoting identifiers. Identifiers are things like
   * table or column names.
   * In modern database they can contain almost any UTF-8 characters, as long
   * as they are properly quoted.
   * <p>
   * Example:<pre>
   *   CREATE TABLE "Hello World" ( "my primary key" INT );</pre>
   * 
   * Most databases use double quotes (") as the quote. MySQL uses a backtick.
   * Also remember that quoted identifiers are resolved in a case sensitive
   * way with PostgreSQL (but unquoted are case insensitive!).
   */
  public String stringByQuotingIdentifier(final String _id) {
    // TBD: could use getIdentifierQuoteString() of java.sql.DatabaseMetaData
    if (_id == null) return null;

    // TODO: fix me
    return "\"" + escape(_id, '"') + "\"";
  }

  public boolean escapeIntoBuffer
    (final StringBuilder _sb, final String _value, final char _quoteChar)
  {
    if (_value == null)
      return false;

    StringCharacterIterator localParser = new StringCharacterIterator(_value);

    for (char c = localParser.current();
         c != CharacterIterator.DONE;
         c = localParser.next())
    {
      if (c == _quoteChar) {
        // TBD: buggy? Just quotes single-quotes?
        _sb.append('\'');
        _sb.append('\'');
      }
      else if (c == '\\') { // escape backslash with double-backslash (why?)
        _sb.append('\\');
        _sb.append('\\');
      }
      else
        _sb.append(c);
    }

    return true;
  }

  public String escape(final String _value, final char _quoteChar) {
    if (_value == null)
      return null;
    if (_value.length() == 0)
      return "";

    // TODO: better use some FastStringBuffer (unsynchronized)
    StringBuilder buffer = new StringBuilder(_value.length());
    if (!this.escapeIntoBuffer(buffer, _value, _quoteChar))
      return null;

    return buffer.toString ();
  }

  public Class defaultExpressionClass() {
    return EOSQLExpression.class;
  }
  public Class expressionClass() {
    return this.defaultExpressionClass();
  }
  public EOSQLExpressionFactory expressionFactory() {
    return new EOSQLExpressionFactory(this);
  }
  public EOSchemaGeneration synchronizationFactory() {
    return new EOSynchronizationFactory(this);
  }
  

  /* model support */

  public boolean hasModel() {
    return this.model != null;
  }
  public boolean hasModelPattern() {
    return this.modelPattern != null;
  }

  public void setModelPattern(final EOModel _pattern) {
    synchronized(this) {
      if (_pattern == null) {
        this.modelPattern = null;
        this.model = null;
      }
      else {
        this.modelPattern = _pattern;
        this.model = _pattern.isPatternModel() ? null : this.modelPattern;
      }
    }
  }
  public EOModel modelPattern() {
    synchronized(this) {
      return this.modelPattern;
    }
  }

  /**
   * Returns or builds the EOModel associated with the EOAdaptor. If the model
   * is not yet set, or if the model is a pattern model, the database schema
   * is fetched and applied.
   * 
   * @return the EOModel set in the adaptor
   */
  public EOModel model() {
    synchronized(this) {
      if (this.model != null) {
        // TODO: check timeout
        return this.model;
      }
    }

    log.debug("determine model for adaptor ...");

    EOModel newModel = null;
    if (this.modelPattern != null) {
      log.debug("  create model by resolving pattern ...");
      newModel = this.resolveModelPattern(this.modelPattern);
    }
    else {
      log.debug("  fetch model from database ...");
      newModel = this.fetchModel();
    }

    if (newModel == null)
      log.info("could not fetch model from database.");

    synchronized(this) {
      this.model = newModel;
      this.modelFetchTime = new Date().getTime();
    }
    return newModel;
  }

  public EOModel fetchModel() {
    EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) {
      log.info("could not open channel to fetch model ...");
      return null;
    }

    EOModel newModel = null;
    try {
      String[] tableNames = channel.describeTableNames();
      newModel = channel.describeModelWithTableNames(tableNames);
    }
    finally {
      this.releaseChannel(channel);
    }

    newModel.connectRelationships();
    return newModel;
  }

  public EOModel resolveModelPattern(EOModel _pattern) {
    if (_pattern == null) return null;
    if (!_pattern.isPatternModel()) return _pattern;

    EOEntity[] entities = _pattern.entities();
    if (entities == null)
      return this.fetchModel();
    if (entities.length == 0) /* not sure whether this is a good idea */
      return this.fetchModel();

    log.info("starting to resolve pattern model ...");

    /* start fetches */

    EOAdaptorChannel channel = this.openChannelFromPool();
    if (channel == null) {
      log.info("could not open channel to fetch model ...");
      return null;
    }

    EOModel newModel = null;
    try {
      /* determine set of entities to work upon (tableNameLike) */

      if (_pattern.hasEntitiesWithExternalNamePattern()) {
        // TODO: maybe we should improve this for database which have a
        //       large number of dynamic tables (some kind of on-demand
        //       loading?)
        //       We could also declare an entity as having a "static"
        //       structure?
        log.info("  resolving dynamic table names ...");

        String[] tableNames = channel.describeTableNames();
        if (log.isInfoEnabled()) {
          log.info("  fetched table names: " +
                   UString.componentsJoinedByString(tableNames, ", "));
        }

        List<EOEntity> resolvedList =
          new ArrayList<EOEntity>(tableNames.length);

        /* now lets each entity produce a clone for the given table */
        for (int i = 0; i < entities.length; i++) {
          entities[i].addEntitiesMatchingTableNamesToList
            (resolvedList, tableNames);
        }

        entities = resolvedList.toArray(new EOEntity[resolvedList.size()]);
      }

      if (entities != null && entities.length > 0) {
        /* now collect all table names */

        String[] tableNames = new String[entities.length];
        for (int i = 0; i < entities.length; i++)
          tableNames[i] = entities[i].externalName();

        /* fetch model for the tables we operate on */

        EOModel storedModel = channel.describeModelWithTableNames(tableNames);
        if (storedModel == null) {
          log.error("the database doesn't provide information for all tables, "+
                    "cannot resolve model: " +
                    UString.componentsJoinedByString(tableNames, ", "));
          return null;
        }

        /* now give all entities a chance to update their information */

        for (int i = 0; i < entities.length; i++)
          entities[i] = entities[i].resolveEntityPatternWithModel(storedModel);
      }

      /* create model object */

      if (entities != null)
        newModel = new EOModel(entities);
    }
    finally {
      this.releaseChannel(channel);
    }
    log.info("finished resolving pattern model: " + newModel);

    newModel.connectRelationships();
    return newModel;
  }

  /* dispose */

  public void dispose() {
    /* Note: we also dispose channels which are checked out! */
    List<EOAdaptorChannel> channels1 = null, channels2 = null;
    Timer timer = null;

    synchronized (this) {
      channels1 = this.availableChannels;
      channels2 = this.checkedOutChannels;
      timer     = this.maintenanceTimer;
      this.availableChannels  = null;
      this.checkedOutChannels = null;
      this.maintenanceTimer   = null;
    }

    if (channels1 != null) {
      for (EOAdaptorChannel channel: channels1)
        channel.dispose();
    }
    if (channels2 != null) {
      for (EOAdaptorChannel channel: channels2)
        channel.dispose();
    }

    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  /* maintenance timer */

  private static class MaintenanceTimerTask extends TimerTask {
    private WeakReference<EOAdaptor> adaptor;

    public MaintenanceTimerTask(final EOAdaptor _adaptor) {
      this.adaptor = new WeakReference<EOAdaptor>(_adaptor);
    }

    @Override
    public void run() {
      final EOAdaptor lAdaptor = this.adaptor.get();
      if (lAdaptor != null)
        lAdaptor.maintainPool();
      else
        this.cancel();
    }
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    int coCount, availCount, mOpCount, mRelCount;
    synchronized (this) {
      coCount    = this.checkedOutChannels.size();
      availCount = this.availableChannels.size();
      mOpCount   = this.openCountSinceLastMaintenance;
      mRelCount  = this.releaseCountSinceLastMaintenance;
    }

    _d.append(" #checked-out=" + coCount);
    _d.append(" #available="   + availCount);

    _d.append(" maintenance={#opened=");
    _d.append(mOpCount);
    _d.append(", #released=");
    _d.append(mRelCount);
    _d.append("}");
  }
}
