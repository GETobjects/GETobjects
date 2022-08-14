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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.EOAccessDataSource;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EODetailDataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOFilterDataSource;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifier.ComparisonOperation;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

/**
 * This is an object which controls a selection of objects fetched from a
 * datasource. It can maintain batches, sorting and has neat ways to construct
 * qualifiers from plain values (see below).
 * <p>
 * Dicts: queryMatch, queryOperator, queryMin, queryMax
 * <p>
 * The keys of those dictionaries are entity attributes/pathes, for example
 * 'lastname'. The values are values to compare the key to or in the case
 * of queryOperator, the operator to use.
 * <pre>
 * Example:
 *   queryMatch.lastname     "Duck"
 *   queryMatch.balance      10
 *   queryOperator.lastname  '='
 *   queryOperator.balance   '<'
 * </pre>
 *
 * This will construct a qualifier like:
 * <pre>
 *   lastname = 'Duck' AND balance < 10
 * </pre>
 *
 * Instead of the explicit balance operator you could have also used queryMin:
 * <pre>
 *   queryMin.balance        10
 * </pre>
 *
 * If you do not specify a query operator, queryMatch will use a default one.
 * For strings this is LIKE with a predefined match-pattern. Eg this one
 * <pre>
 *   queryMatch.lastname     "Duck"
 * </pre>
 * Will result in:
 * <pre>
 *   lastname caseInsensitiveLike 'Duck*'
 * </pre>
 * You can configure the default string operators using
 * <pre>
 *   public void setDefaultStringMatchFormat(String _value);
 *   public void setDefaultStringMatchFormat(String _value);
 * </pre>
 */
/*
 * TBD: document much more
 */
public class WODisplayGroup extends NSObject {
  protected final static Log log = LogFactory.getLog("WODisplayGroup");

  protected static final List<Integer> emptyList = new ArrayList<>(0);
  protected static final List<Integer> int0Array =
    Arrays.asList(new Integer[] { 0 });

  protected EODataSource        dataSource;
  protected EOQualifier         qualifier;
  protected EOSortOrdering[]    sortOrderings;

  protected Map<String, Object> insertedObjectDefaultValues;

  protected int                 numberOfObjectsPerBatch;
  protected int                 currentBatchIndex;
  protected List<Integer>       selectionIndexes;
  protected boolean             fetchesOnLoad;
  protected boolean             selectsFirstObjectAfterFetch;
  protected boolean             validatesChangesImmediatly;
  protected boolean             inQueryMode;

  /*
   * Note: we either have all objects or just one batch. If we have just one
   *       batch (displayObjects), we can fetch the 'count' separately.
   */
  protected List<Object>        objects;
  protected List<Object>        displayObjects;
  protected Integer             count;

  /*
   * Variables for constructing qualifiers based on simple association bindings
   */
  protected Map<String, Object> queryBindings;
  protected Map<String, Object> queryMatch;
  protected Map<String, Object> queryMin;
  protected Map<String, Object> queryMax;
  protected Map<String, Object> queryOperator;
  protected String              defaultStringMatchFormat;
  protected String              defaultStringMatchOperator;
  protected static final String globalDefaultStringMatchFormat = "%@*";
  protected static final String globalDefaultStringMatchOperator =
    "caseInsensitiveLike";

  /* query parameters */
  // TODO: we should probably move those to WEBindDisplayGroup
  protected String qpPrefix      = "dg_";
  protected String qpMatchPrefix = "q";
  protected String qpOpPrefix    = "op";
  protected String qpMinPrefix   = "min";
  protected String qpMaxPrefix   = "max";
  protected String qpIndex       = "batchindex";
  protected String qpBatchSize   = "batchsize";
  protected String qpOrderKey    = "sort";
  protected String countQueryParameterName   = "count";


  /* construction */

  public WODisplayGroup() {
    this.currentBatchIndex = 1;
  }

  public static WODisplayGroup setupAndQualifyWithDataSource
    (EODataSource _ds, final boolean _wrapInFilter)
  {
    final WODisplayGroup dg = new WODisplayGroup();

    if (_wrapInFilter) _ds = new EOFilterDataSource(_ds);
    dg.setDataSource(_ds);

    dg.qualifyDataSource();
    return dg;
  }

  /* accessors */

  public void setFetchesOnLoad(final boolean _flag) {
    this.fetchesOnLoad = _flag;
  }
  public boolean fetchesOnLoad() {
    return this.fetchesOnLoad;
  }

  public void setInsertedObjectDefaultValues(final Map<String, Object> _values) {
    this.insertedObjectDefaultValues = _values;
  }
  public Map<String, Object> insertedObjectDefaultValues() {
    return this.insertedObjectDefaultValues;
  }

  public void setNumberOfObjectsPerBatch(final int _value) {
    if (_value == this.numberOfObjectsPerBatch)
      return;

    this.numberOfObjectsPerBatch = _value;
    this.displayObjects = null; /* needs a recalculation */
  }
  public int numberOfObjectsPerBatch() {
    return this.numberOfObjectsPerBatch;
  }

  public void setSelectsFirstObjectAfterFetch(final boolean _flag) {
    this.selectsFirstObjectAfterFetch = _flag;
  }
  public boolean selectsFirstObjectAfterFetch() {
    return this.selectsFirstObjectAfterFetch;
  }

  public void setValidatesChangesImmediatly(final boolean _flag) {
    this.validatesChangesImmediatly = _flag;
  }
  public boolean validatesChangesImmediatly() {
    return this.validatesChangesImmediatly;
  }

  public void setSortOrderings(final EOSortOrdering[] _sos) {
    this.sortOrderings = _sos;
  }
  public EOSortOrdering[] sortOrderings() {
    return this.sortOrderings;
  }

  /* datasource */

  public void setDataSource(final EODataSource _ds) {
    if (this.dataSource == _ds)
      return;

    if (this.dataSource != null) {
      // TODO: unregister with old editing context
    }

    this.dataSource = _ds;

    if (this.dataSource != null) {
      // TODO: register with new editing context
    }

    /* reset state */
    this.objects        = null;
    this.displayObjects = null;
  }
  public EODataSource dataSource() {
    return this.dataSource;
  }

  public int count() {
    if (this.objects != null) {
      final List<Object> ao = allObjects();
      return ao != null ? ao.size() : 0;
    }
    if (this.count != null)
      return this.count;

    this.count = fetchCount();
    return this.count;
  }

  public boolean hasNoEntries() {
    return count() == 0;
  }
  public boolean hasManyEntries() {
    return count() > 1;
  }
  public boolean hasOneEntry() {
    return count() == 1;
  }

  /* fetching the count */

  public int fetchCount() {
    // TODO: add some way to make this more clever
    if (!(this.dataSource instanceof EOAccessDataSource)) {
      fetch();
      final List<Object> objs = allObjects();
      return objs != null ? objs.size() : 0;
    }

    /* Check whether the fetchspec already contains a SQL fetch hint */
    EOFetchSpecification fs = fetchSpecificationForFetch();
    fs = fs.fetchSpecificationForCount();
    if (fs == null) { // could not derive a count-spec
      fetch();
      final List<Object> objs = allObjects();
      return objs != null ? objs.size() : 0;
    }

    final EOFetchSpecification old = this.dataSource.fetchSpecification();
    this.dataSource.setFetchSpecification(fs);
    final List rows = this.dataSource.fetchObjects();
    this.dataSource.setFetchSpecification(old);

    if (rows == null) {
      log.error("error fetching object count!",
                     this.dataSource.lastException());
      return -1;
    }
    if (rows.size() < 1) {
      log.error("fetch succeeded, but no object count was returned?!");
      return -1;
    }

    final Map row = (Map)rows.get(0);
    return ((Number)(row.values().iterator().next())).intValue();
  }

  /* batches */

  public boolean hasMultipleBatches() {
    return batchCount() > 1;
  }

  public int batchCount() {
    final int nob = numberOfObjectsPerBatch();

    if (this.objects != null) {
      final List<Object> objs = allObjects();
      if (objs == null) return 0;

      final int doc = objs.size();
      return (nob == 0) ? 1 : (doc / nob + ((doc % nob) != 0 ? 1 : 0));
    }

    final int size = count();
    if (size < 1) return 0;

    if (nob < 1) return 1;
    if (size < nob) return 1;
    return (size / nob) + (size % nob > 0 ? 1 : 0);
  }

  public void setCurrentBatchIndex(final int _idx) {
    if (_idx == this.currentBatchIndex) /* same batch */
      return;

    /* Note: do NOT check 'batchCount', it might trigger fetch */

    this.currentBatchIndex = _idx;
    this.displayObjects = null; /* needs a recalculation */
  }
  public int currentBatchIndex() {
    // Don't: we might be asked before a fetch (which manages the count ...)
    // if (this.currentBatchIndex > this.batchCount())
    //   this.currentBatchIndex = 1;
    return this.currentBatchIndex;
  }

  public boolean isFirstBatch() { /* provide only 'next' buttons */
    return this.currentBatchIndex < 2;
  }
  public boolean isLastBatch() { /* provide only 'previous' buttons */
    return this.currentBatchIndex >= batchCount();
  }
  public boolean isInnerBatch() { /* provide 'next' and 'previous' buttons */
    return this.currentBatchIndex > 1 && !isLastBatch();
  }

  public int nextBatchIndex() {
    return (isLastBatch() ? 1 : this.currentBatchIndex + 1);
  }
  public int previousBatchIndex() {
    return (isFirstBatch() ? batchCount() : this.currentBatchIndex-1);
  }

  /* displayed objects */

  public int indexOfFirstDisplayedObject() {
    if (this.currentBatchIndex < 1) {
      log.warn("invalid batch index: " + this.currentBatchIndex);
      return 0;
    }
    if (this.numberOfObjectsPerBatch < 1)
      return 0;

    return (this.currentBatchIndex - 1) * this.numberOfObjectsPerBatch;
  }

  public int indexOfLastDisplayedObject() {
    final int nob = numberOfObjectsPerBatch();

    if (this.objects != null) {
      final List<Object> objs = allObjects();
      final int doc = objs != null ? objs.size() : 0;

      if (nob == 0)
        return doc - 1;

      final int fdo = indexOfFirstDisplayedObject();
      if ((fdo + nob) < doc)
        return (fdo + nob - 1);

      return (doc - 1);
    }

    /* only fetch the count */
    int idx = (this.currentBatchIndex - 1) * nob;
    idx += nob;

    final int size = count();
    if (size == 0) return -1;
    if (idx > size) idx = size; /* last batch can be smaller */
    return idx - 1;
  }

  public int indexOfFirstDisplayedObjectPlusOne() { /* useful for output */
    return indexOfFirstDisplayedObject() + 1;
  }
  public int indexOfLastDisplayedObjectPlusOne() { /* useful for output */
    return indexOfLastDisplayedObject() + 1;
  }

  public WOActionResults displayNextBatch() {
    clearSelection();

    this.currentBatchIndex++;
    if (this.currentBatchIndex > batchCount())
      this.currentBatchIndex = 1;

    updateDisplayedObjects();
    return null; /* stay on page */
  }

  public WOActionResults displayPreviousBatch() {
    clearSelection();

    this.currentBatchIndex--;
    if (currentBatchIndex() <= 0)
      this.currentBatchIndex = batchCount();

    updateDisplayedObjects();
    return null; /* stay on page */
  }

  public WOActionResults displayBatchContainingSelectedObject() {
    // TODO: implement me
    log.error("not implemented: displayBatchContainingSelectedObject");

    updateDisplayedObjects();
    return null; /* stay on page */
  }

  /* selection */

  public boolean setSelectionIndexes(final List<Integer> _selection) {
    // only required for delegate:
    // Set before = this.selectionIndexes != null
    //  ? new HashSet(this.selectionIndexes) : new HashSet();
    // Set after = _selection != null ? new HashSet(_selection) : new HashSet();

    this.selectionIndexes = _selection;
    return true;
  }
  public List selectionIndexes() {
    return this.selectionIndexes;
  }

  public void clearSelection() {
    setSelectionIndexes(emptyList);
  }

  public WOActionResults selectNext() {
    if (this.displayObjects == null || this.displayObjects.size() == 0)
      return null;

    if (this.selectionIndexes == null || this.selectionIndexes.size() == 0) {
      setSelectionIndexes(int0Array);
      return null;
    }

    final int idx = this.selectionIndexes.get(this.selectionIndexes.size() - 1);
    if (idx >= (this.displayObjects.size() - 1)) {
      /* last object is already selected, select first one */
      setSelectionIndexes(int0Array);
      return null;
    }

    /* select next object */
    final List<Integer> list = new ArrayList<>(1);
    list.add(idx + 1);
    setSelectionIndexes(list);
    return null;
  }

  public WOActionResults selectPrevious() {
    if (this.displayObjects == null || this.displayObjects.size() == 0)
      return null;

    if (this.selectionIndexes == null || this.selectionIndexes.size() == 0) {
      setSelectionIndexes(int0Array);
      return null;
    }

    final List<Integer> list = new ArrayList<>(1);
    final int idx = this.selectionIndexes.get(this.selectionIndexes.size() - 1);

    if (idx <= 0) {
      /* first object is selected, now select last one */
      list.add(this.displayObjects.size() - 1);
    }
    else {
      /* select previous object .. */
      list.add(idx - 1);
    }

    setSelectionIndexes(list);
    return null;
  }

  public void setSelectedObject(final Object _obj) {
    final List<Object> objs = new ArrayList<>(1);
    if (_obj != null) objs.add(_obj);
    setSelectedObjects(objs);
  }

  public Object selectedObject() {
    if (this.objects == null)
      return null;
    if (this.selectionIndexes == null || this.selectionIndexes.size() == 0)
      return null;

    int idx = this.selectionIndexes.get(0);

    /* check whether we have fetched all matching objects */

    if (this.objects != null) {
      if (idx >= this.objects.size()) {
        log.warn("selection index is out of range: " + idx);
        return null;
      }

      // TODO: need to ensure that selection is in displayedObjects?
      return this.objects.get(idx);
    }

    /* check whether we have a partial fetch (just one batch) */

    if (this.displayObjects != null) {
      if (this.numberOfObjectsPerBatch > 0)
        idx -= (this.currentBatchIndex * this.numberOfObjectsPerBatch);

      if (idx < 0 || idx >= this.displayObjects.size()) {
        log.warn("selection index is out of display range: " + idx);
        return null;
      }

      return this.displayObjects.get(idx);
    }

    /* no objects are fetched */
    log.warn("we have a selection but no objects: " + idx);
    return null;
  }

  public void setSelectedObjects(final List<Object> _objs) {
    if (_objs == null || _objs.size() == 0) {
      clearSelection();
      return;
    }

    /* scan display objects for selection */

    final List<Integer> selIndexes = new ArrayList<>(_objs.size());

    if (this.objects != null) {
      /* all objects are fetched */
      for (final Object o: _objs) {
        final int idx = this.objects.indexOf(o);
        if (idx >= 0)
          selIndexes.add(Integer.valueOf(idx));
        else
          log.warn("could not apply a selection, object: " + o);
      }
    }
    else if (this.displayObjects != null) {
      /* display objects are fetched */
      for (final Object o: _objs) {
        int idx = this.displayObjects.indexOf(o);

        if (this.numberOfObjectsPerBatch > 0)
          idx += (this.currentBatchIndex - 1) * this.numberOfObjectsPerBatch;

        if (idx >= 0)
          selIndexes.add(Integer.valueOf(idx));
        else
          log.warn("could not apply a display selection, object: " + o);
      }
    }
    else
      log.warn("cannot apply selection, display group as no objects.");

    setSelectionIndexes(selIndexes);
  }

  public List<Object> selectedObjects() {
    if (this.objects == null)
      return null;
    if (this.selectionIndexes == null || this.selectionIndexes.size() == 0)
      return null;

    final int sCount = this.selectionIndexes.size();
    final int oCount = this.objects.size();

    final List<Object> result = new ArrayList<>(sCount);
    for (int i = 0; i < sCount; i++) {
      final int idx = this.selectionIndexes.get(i);
      if (idx < oCount)
        result.add(this.objects.get(idx));
    }
    return result;
  }

  public boolean selectObject(final Object _object) {
    /* returns true if displayedObjects contains _obj, otherwise false */
    if (_object == null)
      return false;

    final int idx = this.objects.indexOf(_object);
    if (idx != -1) {
      final List<Integer> list = new ArrayList<>(1);
      list.add(idx);
      setSelectionIndexes(list);
    }
    else
      setSelectionIndexes(emptyList);
    return true;
  }

  public boolean selectObjectsIdenticalTo(final List<Object> _objs) {
    // return true if t least one obj matches
    // TODO: implement me
    log.error("selectObjectsIdenticalTo is not implemented");
    return false;
  }
  public boolean selectObjectsIdenticalTo
    (final List<Object> _objs, final boolean _firstOnMiss)
  {
    if (this.selectObjectsIdenticalTo(_objs))
      return true;

    if (!_firstOnMiss)
      return false;

    if (this.displayObjects == null || this.displayObjects.size() == 0)
      return selectObject(null);

    return selectObject(this.displayObjects.get(0));
  }

  /* objects */

  public void setObjectArray(final List<Object> _objects) {
    if (this.objects == _objects)
      return;

    this.objects = _objects;

    clearSelection();
    if (this.objects != null && this.objects.size() > 0) {
      if (selectsFirstObjectAfterFetch())
        setSelectionIndexes(int0Array);
    }
  }

  public List<Object> allObjects() {
    return this.objects;
  }

  public List<Object> displayedObjects() {
    /* Note: this is not required:
     *   if (this.displayObjects == null)
     *     this.updateDisplayedObjects();
     * The client is supposed to call something like qualifyDataSource()
     */

    return this.displayObjects;
  }

  @SuppressWarnings("unchecked")
  public WOActionResults fetch() {
    if (log.isDebugEnabled()) log.debug("fetching ...");

    List<Object> objs = null;

    /* fetch from datasource */

    final EODataSource ds = dataSource();
    if (log.isDebugEnabled()) log.debug("  datasource: " + ds);

    if (ds != null) objs = ds.fetchObjects();
    if (log.isDebugEnabled()) {
      if (objs == null) log.debug("  error, ds fetch returned null!");
      else log.debug("  fetched: " + objs.size());
    }

    /* update displaygroup */

    setObjectArray(objs);
    updateDisplayedObjects();

    if (log.isDebugEnabled()) {
      if (this.objects == null) log.debug("  no objects set after fetch.");
      else log.debug("  objects set after fetch: " + this.objects.size());

      if (this.displayObjects == null)
        log.debug("  no dispobjects set after fetch.");
      else
        log.debug("  dispobjects set after fetch: "+this.displayObjects.size());
    }

    /* apply automatic selection */

    if (selectsFirstObjectAfterFetch()) {
      clearSelection();

      if (objs != null && objs.size() > 0)
        setSelectedObject(objs.get(0));
    }

    return null; /* stay on page */
  }

  public void updateDisplayedObjects() {
    if (this.numberOfObjectsPerBatch < 1) { /* display all objects */
      this.displayObjects = this.objects;
      return;
    }

    final int startIdx = indexOfFirstDisplayedObject();
    final int endIdx   = indexOfLastDisplayedObject();
    final int size     = this.objects != null ? this.objects.size() : 0;

    if (startIdx >= size || endIdx < startIdx) {
      /* thats not necessarily an error, it can happen after fetches */
      log.info("got an out-of-range batch for displayed objects: " +
               startIdx + "/" + endIdx + ", count " + size);
      this.displayObjects = null;
      return;
    }

    // TODO: implement me
    this.displayObjects = this.objects != null
      ? this.objects.subList(startIdx, endIdx + 1)
      : null;
  }

  /* query */

  public void setInQueryMode(final boolean _flag) {
    this.inQueryMode = _flag;
  }
  public boolean inQueryMode() {
    return this.inQueryMode;
  }

  public EOQualifier qualifierFromQueryValues() {
    final List<EOQualifier> quals = new ArrayList<>(4);

    /* construct qualifier for all query-match entries */

    if (this.queryMatch != null) {
      final Map<String, Object> opsMap = queryOperator();

      for (final String key: this.queryMatch.keySet()) {
        Object value = this.queryMatch.get(key);

        /* Note: you cannot use queryMatch to compare against NULL */
        if (value == null) /* skip empty values (eg popups w/o selection) */
          continue;

        /* determine operator */

        ComparisonOperation ops;
        String op = (String)opsMap.get(key);

        if (op != null) {
          ops = EOQualifier.operationForString(op);
          if (ops == null || ops == ComparisonOperation.UNKNOWN)
            log.warn("unknown operation: '" + op + "'");
        }
        else if (value instanceof String) {
          /* Special handling for string values. Would be better if this would
           * reflect on the key instead of the value! TODO: why?
           */
          op  = defaultStringMatchOperator();
          if (op == null) op = globalDefaultStringMatchOperator;
          ops = EOQualifier.operationForString(op);
          if (ops == null || ops == ComparisonOperation.UNKNOWN)
            log.warn("unknown string operation: '" + op + "'");
        }
        else {
          /* default operator is equality */
          op  = "=";
          ops = ComparisonOperation.EQUAL_TO;
        }

        /* apply match format */

        if (value instanceof String) {
          if (ops == ComparisonOperation.CASE_INSENSITIVE_LIKE ||
              ops == ComparisonOperation.LIKE) {
            String fmt = defaultStringMatchFormat();
            if (fmt == null) fmt = globalDefaultStringMatchFormat;

            String sv;
            if (value != null) {
              sv = value.toString();
              if (ops == ComparisonOperation.CASE_INSENSITIVE_LIKE)
                sv = sv.toLowerCase(); // TODO: is this required?
            }
            else
              sv = null;

            value = fmt.replace("%@", sv);

            if ("*".equals(value)) {
              /* something like: firstname LIKE '*', which is always true */
              continue;
            }
          }
        }

        // System.err.println("OPS: " + ops);

        /* add qualifier */

        quals.add(new EOKeyValueQualifier(key, ops, value));
      }
      // System.err.println("QUALS: " + quals);
    }

    /* construct min qualifiers */

    if (this.queryMin != null) {
      for (final String key: this.queryMin.keySet()) {
        final Object value = this.queryMin.get(key);
        quals.add(new EOKeyValueQualifier
            (key, EOQualifier.ComparisonOperation.GREATER_THAN, value));
      }
    }

    /* construct max qualifiers */

    if (this.queryMax != null) {
      for (final String key: this.queryMax.keySet()) {
        final Object value = this.queryMax.get(key);
        quals.add(new EOKeyValueQualifier
            (key, EOQualifier.ComparisonOperation.LESS_THAN, value));
      }
    }

    /* conjoin qualifiers */

    EOQualifier q;
    if (quals.size() == 0)
      q = null;
    else if (quals.size() == 1)
      q = quals.get(0);
    else
      q = new EOAndQualifier(quals);

    // System.err.println("Q: " + q);
    return q;
  }

  public Map<String, Object> queryBindings() {
    if (this.queryBindings == null)
      this.queryBindings = new HashMap<>(8);
    return this.queryBindings;
  }
  public Map<String, Object> queryMatch() {
    if (this.queryMatch == null)
      this.queryMatch = new HashMap<>(8);
    return this.queryMatch;
  }

  /**
   * Returns a mutable Map in which you can put 'greaterThan' values.
   * <p>
   * For example:
   * <p>
   *   <code>dg.queryMin().put("balance", 1000)</code>
   * <p>
   * will ensure that records have a balance bigger than 1000. Usually used in
   * .wod files like this:
   * <p>
   *   <code>
   *     MinimumBalance: WOTextField {
   *       value = dg.queryMin.balance;
   *     }
   *   </code>
   *
   * @return the (mutable) Map where limits can be put into
   */
  public Map<String, Object> queryMin() {
    if (this.queryMin == null)
      this.queryMin = new HashMap<>(2);
    return this.queryMin;
  }
  /**
   * Returns a mutable Map in which you can put 'smallerThan' values.
   * <p>
   * For example:
   * <p>
   *   <code>dg.queryMax().put("balance", 1000)</code>
   * <p>
   * will ensure that records have a balance smaller than 1000. Usually used in
   * .wod files like this:
   * <p>
   *   <code>
   *     MaxBalance: WOTextField {
   *       value = dg.queryMax.balance;
   *     }
   *   </code>
   *
   * @return the (mutable) Map where limits can be put into
   */
  public Map<String, Object> queryMax() {
    if (this.queryMax == null)
      this.queryMax = new HashMap<>(2);
    return this.queryMax;
  }

  public Map<String, Object> queryOperator() {
    if (this.queryOperator == null)
      this.queryOperator = new HashMap<>(8);
    return this.queryOperator;
  }

  public void setDefaultStringMatchFormat(final String _value) {
    this.defaultStringMatchFormat = _value;
  }
  public String defaultStringMatchFormat() {
    return this.defaultStringMatchFormat;
  }

  public void setDefaultStringMatchOperator(final String _value) {
    this.defaultStringMatchOperator = _value;
  }
  public String defaultStringMatchOperator() {
    return this.defaultStringMatchOperator;
  }

  /* qualifiers */

  /**
   * Set a qualifier for the display group. Be careful, this gets overridden
   * by qualifierFromQueryValues() (eg called by qualifyDisplayGroup()).
   *
   * @see qualifierFromQueryValues.
   */
  public void setQualifier(final EOQualifier _q) {
    this.qualifier = _q;
  }
  public EOQualifier qualifier() {
    return this.qualifier;
  }

  // TODO: allQualifierOperators
  // TODO: stringQualifierOperators
  // TODO: relationalQualifierOperators

  /**
   * Calculates and sets the qualifier of the display group, then update
   * the displayed objects (by calling updateDisplayedObjects()).
   *
   * If this object is in query mode, the query mode is disabled.
   */
  public void qualifyDisplayGroup() {
    final EOQualifier q = qualifierFromQueryValues();
    if (q != null)
      setQualifier(q);

    updateDisplayedObjects();

    if (inQueryMode())
      setInQueryMode(false);
  }

  /**
   * Return a fetch spec which has the qualifier and sort orderings of the
   * display group applied.
   * Note that this always returns a new fetch specification, never the one
   * of the datasource.
   *
   * @return fetch specification with qualifiers applied
   */
  protected EOFetchSpecification fetchSpecificationForFetch() {
    /* Note: this MUST return a copy of the fetchspec */
    EOFetchSpecification fs = this.dataSource.fetchSpecification();

    EOQualifier qv = qualifierFromQueryValues();
    EOQualifier q;

    if (qv == null)
      q = this.qualifier;
    else if (this.qualifier == null)
      q = qv;
    else {
      // TBD: I think this is non-sense. qualifierFromQueryValues patches the
      //      qualifier, no? Hm.
      q = new EOAndQualifier(this.qualifier, qv);
    }

    if (fs == null) {
      fs = new EOFetchSpecification(null, q, this.sortOrderings);
    }
    else {
      fs = new EOFetchSpecification(fs);

      if (this.sortOrderings != null)
        fs.setSortOrderings(this.sortOrderings);

      qv = fs.qualifier();
      if (qv == null && q == null)
        fs.setQualifier(null);
      else if (q == null)
        fs.setQualifier(qv);
      else if (qv == null)
        fs.setQualifier(q);
      else
        fs.setQualifier(new EOAndQualifier(qv, q));
    }

    // TODO: apply qualifier bindings

    return fs;
  }

  /**
   * This is like fetchSpecificationForFetch() but also applies the fetch
   * offset and limit required for the current batch.
   *
   * @return fetch specification with qualifiers, sorts and limits applied
   */
  protected EOFetchSpecification fetchSpecificationForDisplayFetch() {
    final EOFetchSpecification fs = fetchSpecificationForFetch();

    /* apply offset/limit */

    if (this.currentBatchIndex > 1)
      fs.setFetchOffset(indexOfFirstDisplayedObject());

    if (this.numberOfObjectsPerBatch > 0)
      fs.setFetchLimit(this.numberOfObjectsPerBatch);
    return fs;
  }

  @SuppressWarnings("unchecked")
  protected List<Object> primaryFetchDisplayedObjects() {
    /* This differs to WODisplayGroup because it treats the count as a separate
     * data item.
     */
    if (this.numberOfObjectsPerBatch < 1) /* display all objects */
      return allObjects();

    if (log.isDebugEnabled())
      log.debug("fetch displayed, limit: " + this.numberOfObjectsPerBatch);

    if (this.objects != null) {
      final int startIdx = indexOfFirstDisplayedObject();
      final int endIdx   = indexOfLastDisplayedObject();
      final int size     = this.objects.size();

      if (startIdx >= size || endIdx < startIdx) {
        log.info("got an out-of-range batch for displayed objects: " +
                      startIdx + "/" + endIdx + ", count " + size);
        return null;
      }

      this.displayObjects = this.objects.subList(startIdx, endIdx + 1);
    }
    else {
      final EOFetchSpecification fs  = fetchSpecificationForDisplayFetch();
      final EOFetchSpecification old = this.dataSource.fetchSpecification();
      this.dataSource.setFetchSpecification(fs);
      this.displayObjects = this.dataSource.fetchObjects();
      this.dataSource.setFetchSpecification(old);

      if (this.displayObjects == null) {
        log.error("error fetching display objects!",
                       this.dataSource.lastException());
      }
    }

    if (log.isDebugEnabled())
      log.debug("fetched displayed objects: " + this.displayObjects);

    return this.displayObjects;
  }

  @SuppressWarnings("unchecked")
  protected List<Object> primaryFetchAllObjects() {
    final EOFetchSpecification fs  = fetchSpecificationForFetch();
    final EOFetchSpecification old = this.dataSource.fetchSpecification();
    this.dataSource.setFetchSpecification(fs);
    this.objects = this.dataSource.fetchObjects();
    this.dataSource.setFetchSpecification(old);

    if (this.objects == null) {
      log.error("error fetching all objects!",
                     this.dataSource.lastException());
    }
    return this.objects;
  }

  /**
   * This key method applies the qualifications and sorts of the display group
   * on the datasource and then performs a fetch.
   * <p>
   * This method does filtering/sorting in the datasource while
   * qualifyDisplayGroup() just does it in-memory.
   *
   * <p>
   * If the query-mode was enabled, it gets disabled.
   *
   * @see qualifyDisplayGroup
   */
  public void qualifyDataSource() {
    final boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) log.debug("qualifyDataSource");

    final EODataSource ds = dataSource();
    if (ds == null) {
      log.warn("no datasource set.");
      return;
    }

    /* build qualifier */

    EOQualifier q = qualifierFromQueryValues();
    if (q != null) {
      setQualifier(q);
      if (isDebugOn) log.debug("  qualifier: " + q);
    }
    else {
      q = qualifier();
    }

    Map<String, Object> bindings = queryBindings();
    if (bindings != null && bindings.size() == 0) bindings = null;
    if (isDebugOn) log.debug("  bindings: " + bindings);

    /* set qualifier in datasource */

    // TODO: do something better here ...
    if (ds instanceof EOAccessDataSource) {
      if (isDebugOn) log.debug("  db datasource: " + ds);
      final EOAccessDataSource dbds = (EOAccessDataSource)ds;
      dbds.setAuxiliaryQualifier(q);
      dbds.setQualifierBindings(bindings);

      if (this.sortOrderings != null) {
        EOFetchSpecification fs = dbds.fetchSpecification();
        if (fs != null) {
          fs = new EOFetchSpecification(fs);
          fs.setSortOrderings(this.sortOrderings);
        }
        else {
          fs = new EOFetchSpecification(dbds.entity().name(), null,
                                        this.sortOrderings);
        }

        dbds.setFetchSpecification(fs);
        if (log.isDebugEnabled()) log.debug("  patched fspec: " + fs);
      }
    }
    else {
      if (isDebugOn) log.debug("  generic datasource: " + ds);
      EOFetchSpecification fs = ds.fetchSpecification();
      fs = fs != null
        ? new EOFetchSpecification(fs)
        : new EOFetchSpecification();

      /* This one is very tricky. We overwrite all qualifiers previously set in
       * the datasource. And we always *do* set a qualifier.
       */
      // TBD: we should probably nest the input datasource in an
      //      EOFilterDataSource to avoid those side effects
      fs.setQualifier(q);

      fs.setSortOrderings(this.sortOrderings);

      /* apply */
      ds.setFetchSpecification(fs);
      if (isDebugOn) log.debug("  patched fspec: " + fs);
    }

    /* perform fetch */

    if (isDebugOn) log.debug("  will fetch ...");
    fetch();
    if (isDebugOn) log.debug("  did fetch.");

    if (inQueryMode()) {
      setInQueryMode(false);
      if (isDebugOn) log.debug("  disabled query mode.");
    }
  }

  /* object creation */

  public WOActionResults insert() {
    int idx;

    if (this.selectionIndexes != null && this.selectionIndexes.size() > 0)
      idx = this.selectionIndexes.get(0) + 1;
    else
      idx = this.objects != null ? this.objects.size() : 0;

    return this.insertObjectAtIndex(idx);
  }

  public WOActionResults insertObjectAtIndex(final int _idx) {
    final EODataSource ds = dataSource();
    if (ds == null) {
      log.warn("no datasource set for object insert.");
      return null;
    }

    final Object newObject = ds.createObject();
    if (newObject == null) {
      // TODO: report some error (using delegate?)
      return null;
    }

    /* apply default values */

    // TODO: add KVC helper?
    // TODO: takeValuesFromDictionary(this.insertedObjectDefaultValues())

    /* insert */

    this.insertObjectAtIndex(newObject, _idx);

    return null /* stay on page */;
  }

  public void insertObjectAtIndex(final Object _o, final int _idx) {
    /* insert in datasource */

    final EODataSource ds = dataSource();
    if (ds != null) {
      // TODO: error handling?
      ds.insertObject(_o);
    }

    /* update object-array (Note: ignores qualifier for new objects!) */

    if (this.objects == null)
      this.objects = new ArrayList<>(1);

    if (_idx <= this.objects.size())
      this.objects.set(_idx, _o); // TODO: is this correct? (does it _insert_?)
    else
      this.objects.add(_o);

    updateDisplayedObjects();

    /* select object */

    selectObject(_o);
  }

  /* object deletion */

  public WOActionResults delete() {
    deleteSelection();
    return null;
  }

  public boolean deleteSelection() {
    if (this.selectionIndexes == null || this.selectionIndexes.size() == 0)
      return true;
    if (this.objects == null || this.objects.size() == 0)
      return false;

    final List<Object> objsToDelete = new ArrayList<>(selectedObjects());
    for (int i = 0; i < objsToDelete.size(); i++) {
      final int idx = this.objects.indexOf(objsToDelete.get(i));

      if (idx == -1) {
        log.error("did not find object in selection: " +
                  objsToDelete.get(i));
        return false;
      }

      if (!deleteObjectAtIndex(idx))
        return false;
    }
    return true;
  }

  public boolean deleteObjectAtIndex(final int _idx) {
    if (this.objects == null || this.objects.size() == 0)
      return false;
    if (_idx >= this.objects.size())
      return false;

    final Object object = this.objects.get(_idx);

    /* delete in datasource */

    final EODataSource ds = dataSource();
    if (ds != null)
      ds.deleteObject(object);

    /* update array */

    this.objects.remove(_idx);
    updateDisplayedObjects();

    return true;
  }

  /* master details */

  public boolean hasDetailDataSource() {
    final EODataSource ds = dataSource();
    return ds != null ? (ds instanceof EODetailDataSource) : false;
  }

  public void setDetailKey(final String _key) {
    final EODataSource ds = dataSource();
    if (ds != null && (ds instanceof EODetailDataSource))
      ((EODetailDataSource)ds).setDetailKey(_key);
  }
  public String detailKey() {
    final EODataSource ds = dataSource();
    return (ds != null && (ds instanceof EODetailDataSource))
      ? ((EODetailDataSource)ds).detailKey() : null;
  }

  public void setMasterObject(final Object _v) {
    final EODataSource ds = dataSource();
    if (ds == null) return;
    if (!(ds instanceof EODetailDataSource)) return;

    ds.qualifyWithRelationshipKey(detailKey(), _v);
  }
  public Object masterObject() {
    final EODataSource ds = dataSource();
    return (ds != null && (ds instanceof EODetailDataSource))
      ? ((EODetailDataSource)ds).masterObject()
      : null;
  }

  /* key/value coding */

  /**
   * This methods hacks the KVC algorithm to allow key pathes in queryMatch,
   * queryMin etc dictionaries. For example:
   * <p>
   *   <code>
   *     value = queryMatch.customer.toCompany.name;
   *   </code>
   * <p>
   * This will actually put "customer.toCompany.name" into the queryMatch
   * dictionary instead of traversing the path.
   *
   * @see valueForKeyPath()
   */
  @Override
  public void takeValueForKeyPath(final Object _value, final String _keypath) {
    if (_keypath != null && _keypath.length() > 8 && _keypath.charAt(0)=='q') {
      if (_value != null) {
        if (_keypath.startsWith("queryMatch."))
          queryMatch().put(_keypath.substring(11), _value);
        else if (_keypath.startsWith("queryMax."))
          queryMax().put(_keypath.substring(9), _value);
        else if (_keypath.startsWith("queryMin."))
          queryMin().put(_keypath.substring(9), _value);
        else if (_keypath.startsWith("queryOperator."))
          queryOperator().put(_keypath.substring(14), _value);
      }
      else {
        if (_keypath.startsWith("queryMatch."))
          queryMatch().remove(_keypath.substring(11));
        else if (_keypath.startsWith("queryMax."))
          queryMax().remove(_keypath.substring(9));
        else if (_keypath.startsWith("queryMin."))
          queryMin().remove(_keypath.substring(9));
        else if (_keypath.startsWith("queryOperator."))
          queryOperator().remove(_keypath.substring(14));
      }
    }
    super.takeValueForKeyPath(_value, _keypath);
  }

  /**
   * Hacks the KVC algorithm to allow for keypathes in queryXYZ dictionaries.
   *
   * @see takeValueForKeyPath()
   */
  @Override
  public Object valueForKeyPath(final String _keypath) {
    if (_keypath != null && _keypath.length() > 8 && _keypath.charAt(0)=='q') {
      if (_keypath.startsWith("queryMatch."))
        return queryMatch().get(_keypath.substring(11));
      if (_keypath.startsWith("queryMax."))
        return queryMax().get(_keypath.substring(9));
      if (_keypath.startsWith("queryMin."))
        return queryMin().get(_keypath.substring(9));
      if (_keypath.startsWith("queryOperator."))
        return queryOperator().get(_keypath.substring(14));
    }
    return super.valueForKeyPath(_keypath);
  }


  /* processing query parameters */

  public boolean isAttributeAllowedForSorting(final EOSortOrdering _key) {
    // we might want to restrict the allowed attributes for sorting
    if (_key == null) return false;
    return true;
  }

  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    if (_rq == null)
      return;

    final String prefix = this.qpPrefix;

    /* Preserve object count so that we don't need to refetch it.
     * IMPORTANT: run first, other code might rely on it (and refetch if it
     *            isn't available
     */
    final String cs = _rq.stringFormValueForKey
      (this.qpPrefix + this.countQueryParameterName);
    if (cs != null)
      this.count = UObject.intValue(cs);

    final int lBatchIndex = UObject.intValue
      (_rq.stringFormValueForKey(prefix + this.qpIndex));
    final int lBatchSize  = UObject.intValue
      (_rq.stringFormValueForKey(prefix + this.qpBatchSize));

    if (lBatchSize  > 0) setNumberOfObjectsPerBatch(lBatchSize);
    if (lBatchIndex > 0) setCurrentBatchIndex(lBatchIndex);

    /* sort orderings */

    final String sOrderings =
      _rq.stringFormValueForKey(prefix + this.qpOrderKey);
    this.sortOrderings = sortOrderingsFromQueryValue(sOrderings);

    /* scan for query dict stuff */

    final String qm = prefix + this.qpMatchPrefix;
    final String qo = prefix + this.qpOpPrefix;
    final String qi = prefix + this.qpMinPrefix;
    final String qx = prefix + this.qpMaxPrefix;

    for (final String key: _rq.formValueKeys()) {
      // System.err.println("CHECK: " + key);

      if (!key.startsWith(prefix))
        continue;

      if (key.startsWith(qm)) {
        queryMatch().put
          (key.substring(qm.length()),_rq.formValueForKey(key));
      }
      else if (key.startsWith(qi)) {
        queryMin().put
          (key.substring(qi.length()),_rq.formValueForKey(key));
      }
      else if (key.startsWith(qx)) {
        queryMax().put
          (key.substring(qx.length()),_rq.formValueForKey(key));
      }
      else if (key.startsWith(qo)) {
        queryOperator().put
          (key.substring(qo.length()), _rq.formValueForKey(key));
      }
    }

    // System.err.println("QM: " + this.queryMatch);

    // TODO: qualifier
  }

  public void appendStateToQueryDictionary(final Map<String, Object> _qd) {
    if (_qd == null)
      return;

    final String prefix = this.qpPrefix;

    if (this.count != null) {
      _qd.put(this.qpPrefix + this.countQueryParameterName,
              this.count);
    }

    if (this.currentBatchIndex > 1)
      _qd.put(prefix + this.qpIndex, this.currentBatchIndex);

    if (this.numberOfObjectsPerBatch > 0) {
      _qd.put(prefix + this.qpBatchSize,
              this.numberOfObjectsPerBatch);
    }

    /* add sort orderings */

    final String sos = queryValueForSortOrderings();
    if (sos != null)
      _qd.put(prefix + this.qpOrderKey, sos);

    /* add query dict stuff */

    if (this.queryMatch != null) {
      appendMapStateToQueryDictionary
        (_qd, prefix + this.qpMatchPrefix, this.queryMatch);
    }
    if (this.queryOperator != null) {
      appendMapStateToQueryDictionary
        (_qd, prefix + this.qpOpPrefix, this.queryOperator);
    }
    if (this.queryMin != null) {
      appendMapStateToQueryDictionary
        (_qd, prefix + this.qpMinPrefix, this.queryMin);
    }
    if (this.queryMax != null) {
      appendMapStateToQueryDictionary
        (_qd, prefix + this.qpMaxPrefix, this.queryMax);
    }

    /* add qualifier if we have one ... */
    // TODO
  }

  public void appendMapStateToQueryDictionary
    (final Map<String, Object> _qd, final String _prefix, final Map<String, Object> _map)
  {
    if (_qd == null || _map == null || _map.size() == 0)
      return;

    for (final String key: _map.keySet()) {
      final String qp = _prefix != null ? _prefix + key : key;

      // TODO: maybe we should type the parameter for values, eg:
      //         balance:int = 100
      //       if the value is an integer?
      _qd.put(qp, _map.get(key));
    }
  }

  /* sorting */

  public EOSortOrdering sortOrderingFromQueryValue(String key) {
    Object sel;

    if (key.startsWith("-")) {
      /* support -name for descending sorts */
      sel = EOSortOrdering.EOCompareDescending;
      key = key.substring(1);
    }
    else if (key.endsWith("-D")) { /* eg name-D */
      sel = EOSortOrdering.EOCompareDescending;
      key = key.substring(0, key.length() - 2);
    }
    else if (key.endsWith("-DI")) { /* eg lastname-DI */
      sel = EOSortOrdering.EOCompareCaseInsensitiveDescending;
      key = key.substring(0, key.length() - 3);
    }
    else if (key.endsWith("-AI")) { /* eg lastname-AI */
      sel = EOSortOrdering.EOCompareCaseInsensitiveAscending;
      key = key.substring(0, key.length() - 3);
    }
    else if (key.endsWith("-A")) {
      sel = EOSortOrdering.EOCompareAscending;
      key = key.substring(0, key.length() - 3);
    }
    else
      sel = EOSortOrdering.EOCompareAscending;

    return new EOSortOrdering(key, sel);
  }

  public EOSortOrdering[] sortOrderingsFromQueryValue(final String _s) {
    if (_s == null || _s.length() == 0) return null;

    final String[] ops = _s.split(",");

    final EOSortOrdering[] qSortOrderings = new EOSortOrdering[ops.length];
    for (int i = 0; i < ops.length; i++) {
      /* reconstruct */
      qSortOrderings[i] = sortOrderingFromQueryValue(ops[i]);

      /* check permissions */
      if (!isAttributeAllowedForSorting(qSortOrderings[i]))
        qSortOrderings[i] = null;
    }

    // TODO: we should compact empty cells in the array
    return qSortOrderings;
  }

  public String queryValueForSortOrderings() {
    if (this.sortOrderings == null || this.sortOrderings.length == 0)
      return null;

    final StringBuilder sb = new StringBuilder(128);

    for (int i = 0; i < this.sortOrderings.length; i++) {
      if (i != 0) sb.append(",");
      sb.append(this.sortOrderings[i].key());

      final Object sel = this.sortOrderings[i].selector();
      if (sel != null && sel != EOSortOrdering.EOCompareAscending) {
        final String orderOp = opKeyForSortOrdering(this.sortOrderings[i]);
        sb.append("-");
        sb.append(orderOp);
      }
    }
    return sb.toString();
  }

  public String opKeyForSortOrdering(final EOSortOrdering _so) {
    if (_so == null) return null;
    final Object sel = _so.selector();
    if (sel == null)
      return null;

    if (sel == EOSortOrdering.EOCompareAscending)
      return "A";
    if (sel.equals(EOSortOrdering.EOCompareDescending))
      return "D";
    if (sel.equals(EOSortOrdering.EOCompareCaseInsensitiveDescending))
      return "DI";
    if (sel.equals(EOSortOrdering.EOCompareCaseInsensitiveAscending))
      return "AI";
    return sel.toString();
  }

  public String currentSortDirection() {
    if (this.sortOrderings == null || this.sortOrderings.length < 1)
      return "A";

    final String op = opKeyForSortOrdering(this.sortOrderings[0]);
    return op != null ? op : "A";
  }
  public String nextSortDirection() {
    final String cs = currentSortDirection();
    if (cs == null || cs.length() == 0)
      return "D";

    if ("A".equals(cs))  return "D";
    if ("D".equals(cs))  return "A";
    if ("AI".equals(cs)) return "DI";
    if ("DI".equals(cs)) return "AI";
    return cs;
  }

  /* query parameters */

  public void setQpPrefix(final String _s) {
    this.qpPrefix = _s;
  }
  public String qpPrefix() {
    return this.qpPrefix;
  }
  public String qpIndex() {
    return this.qpIndex;
  }
  public String qpBatchSize() {
    return this.qpBatchSize;
  }
  public String qpOrderKey() {
    return this.qpOrderKey;
  }

  public String qpMatchPrefix() {
    return this.qpMatchPrefix;
  }
  public String qpOpPrefix() {
    return this.qpOpPrefix;
  }
  public String qpMinPrefix() {
    return this.qpMinPrefix;
  }
  public String qpMaxPrefix() {
    return this.qpMaxPrefix;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    _d.append(" batch=" + this.currentBatchIndex + "/" +
              this.numberOfObjectsPerBatch);

    if (this.count != null)
      _d.append(" has-count=" + this.count);

    if (this.objects != null)
      _d.append(" has-all=#" + this.objects.size());
    if (this.displayObjects != null)
      _d.append(" has-displayed=#" + this.displayObjects.size());

    if (this.dataSource != null)
      _d.append(" ds=" + this.dataSource);

    if (this.qualifier != null)
      _d.append(" q=" + this.qualifier.stringRepresentation());

    if (this.sortOrderings != null && this.sortOrderings.length > 0) {
      _d.append(" so=");
      for (int i = 0; i < this.sortOrderings.length; i++) {
        if (i > 0) _d.append(",");
        _d.append(this.sortOrderings[i]);
      }
    }
  }
}
