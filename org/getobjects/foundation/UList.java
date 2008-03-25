/*
 * Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of JOPE.
 * 
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UList
 * <p>
 * List and array related utility functions.
 */
@SuppressWarnings("unchecked")
public class UList extends NSObject {

  private UList() {} /* do not allow construction */
  
  
  /* searching in arrays */
  
  /**
   * Searches the given array for the given object (which can be null) by
   * calling equals() on each item.
   * 
   * @param _array  - array of Objects to be searched
   * @param _object - object to search
   * @return true if the array contains the _object, false otherwise
   */
  public static boolean contains(final Object[] _array, final Object _object) {
    // TBD: isn't that covered by the Java Arrays class?
    if (_array == null || _array.length == 0)
      return false;
    
    for (int i = 0; i < _array.length; i++) {
      if (_object == _array[i])
        return true;
      
      if (_object != null && _object.equals(_array[i]))
        return true;
    }
    
    return false;
  }
  
  /**
   * This methods takes a list and separates it into a set of batches.
   * <p>
   * Example: batchToList(source, 2):
   * <pre>
   *   source: ( 1, 2, 3, 4, 5 )
   *   result: ( ( 1, 2 ), ( 3, 4 ), ( 5 ) )</pre>
   * 
   * @param _list      - the list which should be split
   * @param _batchSize - the size of a batch
   * @return a list of lists containing the objects
   */
  @SuppressWarnings("unchecked")
  public static List batchToList(final List _list, final int _batchSize) {
    if (_list == null)
      return null;
    
    List<List<Object>> batches;
    final int len = _list.size();
    
    if (_batchSize >= len || _batchSize < 1) {
      batches = new ArrayList<List<Object>>(1);
      batches.add(new ArrayList<Object>(_list));
      return batches;
    }
    
    int batchCount = len / _batchSize;
    if (len % _batchSize != 0) batchCount++;
    batches = new ArrayList<List<Object>>(batchCount);
    
    List<Object> batch = new ArrayList<Object>(_batchSize);
    batches.add(batch);
    
    int fillSize = 0;
    for (int i = 0; i < len; i++) {
      if (fillSize == _batchSize) {
        batch = new ArrayList<Object>(_batchSize);
        batches.add(batch);
        fillSize = 0;
      }
      
      batch.add(_list.get(i));
      fillSize++;
    }
    
    return batches;
  }
  
  /**
   * This methods takes an array and separates it into a set of batches.
   * <p>
   * Example: batchToList(source, 2):
   * <pre>
   *   source: [ 1, 2, 3, 4, 5 ]
   *   result: ( ( 1, 2 ), ( 3, 4 ), ( 5 ) )</pre>
   * 
   * @param _array     - the array which should be split
   * @param _batchSize - the size of a batch
   * @return a list of lists containing the objects
   */
  @SuppressWarnings("unchecked")
  public static List batchToList(final Object[] _array, final int _batchSize) {
    if (_array == null)
      return null;
    
    List<List<Object>> batches;
    int len = _array.length;
    
    if (_batchSize >= len || _batchSize < 1) {
      batches = new ArrayList<List<Object>>(1);
      batches.add(Arrays.asList(_array));
      return batches;
    }
    
    
    int batchCount = len / _batchSize;
    if (len % _batchSize != 0) batchCount++;
    batches = new ArrayList<List<Object>>(batchCount);
    
    List<Object> batch = new ArrayList<Object>(_batchSize);
    batches.add(batch);

    int fillSize = 0;
    for (int i = 0; i < len; i++) {
      if (fillSize == _batchSize) {
        batch = new ArrayList<Object>(_batchSize);
        batches.add(batch);
        fillSize = 0;
      }
      
      batch.add(_array[i]);
      fillSize++;
    }
    
    return batches;
  }
  
  
  /* key/value coding helpers */
  
  private static final List<Object> emptyList =
    Collections.EMPTY_LIST;
  private static final Object[] emptyArray = new Object[0];
  
  /**
   * Iterates over all objects in the list, retrieves the value for the given
   * KVC key and stores it into a new list in the same order.
   * This method is slightly faster than using valuesForKeyPath, which of course
   * can also be used for simple keys.
   * <p>
   * Example:
   * <pre>List projectIds = UList.valuesForKeys(projects, "id");</pre>
   * <p>
   * The returned List should be considered immutable.
   * 
   * @see valuesForKeyPath()
   * 
   * @param _objects the objects to retrieve the values from
   * @param _key     the key of the values to be retrieved
   * @return a list containing the values for the given key
   */
  public static List valuesForKey(Collection _objects, String _key) {
    if (_objects == null)
      return null;
    
    int len = _objects.size();
    if (len == 0) return emptyList;
    
    final List values = new ArrayList(len);
    for (Object o: _objects) {
      if (o == null)
        ;
      else if (o instanceof NSKeyValueCoding)
        o = ((NSKeyValueCoding)o).valueForKey(_key);
      else if (o instanceof Map) /* minor optimization, Maps might be common */
        o = ((Map)o).get(_key);
      else
        o = NSKeyValueCoding.Utility.valueForKey(o, _key);
      
      values.add(o);
    }
    
    return values;
  }
  
  /**
   * Iterates over all objects in the list, retrieves the value for the given
   * KVC key path and stores it into a new list in the same order.
   * <p>
   * Example:
   * <pre>List ownerIds = UList.valuesForKeyPath(projects, "owner.id");</pre>
   * <p>
   * The returned List should be considered immutable.
   * 
   * @see valuesForKey()
   * 
   * @param _objects the objects to retrieve the values from
   * @param _path    the keypath of the values to be retrieved
   * @return a list containing the values for the given key
   */
  public static List<Object> valuesForKeyPath
    (final List<Object> _objects, final String _path)
  {
    /* separate method for performance reasons */
    if (_objects == null)
      return null;
    
    final int len = _objects.size();
    if (len == 0) return emptyList;
    
    final List<Object> values = new ArrayList<Object>(len);
    for (int i = 0; i < len; i++) {
      Object o = _objects.get(i);
      
      if (o == null)
        ;
      else if (o instanceof NSKeyValueCodingAdditions)
        o = ((NSKeyValueCodingAdditions)o).valueForKeyPath(_path);
      else
        o = NSKeyValueCodingAdditions.Utility.valueForKeyPath(o, _path);
      
      values.add(o);
    }
    
    return values;
  }
  
  /**
   * Iterates over all objects in the array, retrieves the value for the given
   * KVC key and stores it into a new array in the same order.
   * This method is slightly faster than using valuesForKeyPath, which of course
   * can also be used for simple keys.
   * <p>
   * Example:
   * <pre>Object[] projectIds = UList.valuesForKeys(projects, "id");</pre>
   * 
   * @see valuesForKeyPath()
   * 
   * @param _objects the objects to retrieve the values from
   * @param _key     the key of the values to be retrieved
   * @return an array containing the values for the given key
   */
  public static Object[] valuesForKey(Object[] _objects, String _key) {
    if (_objects == null)
      return null;
    
    final int len = _objects.length;
    if (len == 0) return emptyArray;
    
    final Object[] values = new Object[len];
    for (int i = 0; i < len; i++) {
      Object o = _objects[i];
      
      if (o == null)
        ;
      else if (o instanceof NSKeyValueCoding)
        o = ((NSKeyValueCoding)o).valueForKey(_key);
      else if (o instanceof Map) /* minor optimization, Maps might be common */
        o = ((Map)o).get(_key);
      else
        o = NSKeyValueCoding.Utility.valueForKey(o, _key);
      
      values[i] = o;
    }
    
    return values;
  }

  /**
   * Iterates over all objects in the array, retrieves the value for the given
   * KVC key path and stores it into a new array in the same order.
   * <p>
   * Example:
   * <pre>Object[] ownerIds = UList.valuesForKeyPath(projects, "owner.id");</pre>
   * 
   * @see valuesForKey()
   * 
   * @param _objects the objects to retrieve the values from
   * @param _path    the keypath of the values to be retrieved
   * @return an array containing the values for the given key
   */
  public static Object[] valuesForKeyPath(Object[] _objects, String _path) {
    /* separate method for performance reasons */
    if (_objects == null)
      return null;
    
    int len = _objects.length;
    if (len == 0) return emptyArray;
    
    final Object[] values = new Object[len];
    for (int i = 0; i < len; i++) {
      Object o = _objects[i];
      
      if (o == null)
        ;
      else if (o instanceof NSKeyValueCodingAdditions)
        o = ((NSKeyValueCodingAdditions)o).valueForKeyPath(_path);
      else
        o = NSKeyValueCodingAdditions.Utility.valueForKeyPath(o, _path);
      
      values[i] = o;
    }
    
    return values;
  }
  
  /**
   * This method walks over a collection and groups the contained object based
   * on the value of some keypath. Example:
   * <pre>Map groupedByEntity =
   *  UList.groupByKeyPath(myObjects, "entity.name")</pre>
   * This will iterate over the given objects and group them by the entity, a
   * result could look like:
   * <pre>{ Person = [ xyz, def ]; Team = [ abc, def ]; }</pre>
   * 
   * @param _objects objects which should be grouped by the given keypath
   * @param _key     the criteria to group on
   * @return a Map which contains the group values at the keys
   */
  public static Map<Object, List<Object>> groupByKeyPath
    (final Collection _objects, final String _keyPath)
  {
    if (_objects == null)
      return null;
    
    final Map<Object, List<Object>> resultMap =
      new HashMap<Object, List<Object>>(16);
    
    for (Object object: _objects) {
      /* we do the instanceof because the first case is the usual one */
      final Object group = (object instanceof NSKeyValueCodingAdditions)
        ? ((NSKeyValueCodingAdditions)object).valueForKeyPath(_keyPath)
        : NSKeyValueCodingAdditions.Utility.valueForKeyPath(object, _keyPath);
      
      List<Object> groupValues = resultMap.get(group);
      if (groupValues == null) {
        groupValues = new ArrayList<Object>(4);
        resultMap.put(group, groupValues);
      }
      
      groupValues.add(object);
    }
    
    return resultMap;
  }
  
  /**
   * This method walks over a collection and groups the contained object based
   * on the value of some key.
   * Example:
   * <pre>Map groupedByEntity =
   *  UList.groupByKeyPath(myObjects, "city")</pre>
   * This will iterate over the given objects and group them by the city, a
   * result could look like:
   * <pre>{
   *   Magdeburg = [
   *     { city = Madgeburg; name = "SWM"; },
   *     { city = Magdeburg; name = "Skyrix AG"; }
   *   ];
   *   Dortmund = [ ... ]; }</pre>
   * <p>
   * This method currently calls groupByKeyPath() with the given key as the
   * keypath.
   * 
   * @param _objects objects which should be grouped by the given key
   * @param _key     the criteria to group on
   * @return a Map which contains the group values at the keys
   */
  public static Map<Object, List<Object>> groupByKey
    (final Collection _objects, final String _key)
  {
    // TBD: we could optimize that with a specific implementation
    return groupByKeyPath(_objects, _key);
  }

  /**
   * This method groups a collection based on a set of criterias. Eg given those
   * keys: [ city, zip ] a result could look like:
   * <pre>{
   *  Magdeburg = {
   *    39104 = [ { name = Skyrix; ...}, { name = TLG; ...} ];
   *    39122 = [ { name = Telekom; ...} ];
   *  };
   *  Berlin = {
   *    10233 = ...
   *    23882 = ...
   *  };
   * }</pre>
   * Note that only the last object in the tree is a List, all the parent
   * groups are Maps.
   * <p>
   * @param _objects   the objects to be grouped
   * @param _keyPathes the keypathes to group the objects on
   * @return a Map of Maps or Lists representing the grouping-tree
   */
  public static Map groupByKeyPathes
    (final Collection _objects, final String[] _keyPathes)
  {
    final int keyPathCount = _keyPathes != null ? _keyPathes.length : 0;
    if (_objects == null || keyPathCount == 0)
      return null;
    
    if (keyPathCount == 1)
      return UList.groupByKeyPath(_objects, _keyPathes[0]);
    
    final Map<Object, Object> resultMap = new HashMap<Object, Object>(16);

    for (Object object: _objects) {
      Map    groupCursor = resultMap;
      Object group;
      
      /* iterate until the last group, which contains a list, not a map */
      
      for (int i = 1; i < keyPathCount; i++) {
        /* we do the instanceof because the first case is the usual one */
        group = NSKeyValueCodingAdditions.Utility
          .valueForKeyPath(object, _keyPathes[i - 1]);
        
        Map groupContent = (Map)groupCursor.get(group);
        if (groupContent == null) {
          groupContent = new HashMap(16);
          groupCursor.put(group, groupContent);
        }
        
        groupCursor = groupContent;
      }
      
      /* process the last group */
      
      group = NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(object, _keyPathes[keyPathCount - 1]);
      
      List<Object> groupValues = (List<Object>)groupCursor.get(group);
      if (groupValues == null) {
        groupValues = new ArrayList<Object>(4);
        groupCursor.put(group, groupValues);
      }

      groupValues.add(object);
    }

    return resultMap;
  }
  
  
  /**
   * Extracts a Map from an array of values.
   * <p>
   * Using the _keyIndices array you can use one value of the _array multiple
   * times, eg:<pre>
   *   array   = [ 'Donald', '1954-12-12' ]
   *   keys    = [ 'name', 'birthdayAsText', 'birthdayAsDate' ]
   *   indices = [ 0, 1, 1 ]
   *   formats = [ null, null, new SimpleDateFormat() ]</pre>
   * Results in:<pre>
   *   { name           = Donald;
   *     birthdayAsText = '1954-12-12';
   *     birthdayAsDate = &lt;CalendarDate: 1954-12-12&gt;; }</pre>
   * And another simple example:<pre>
   *   extractRecordFromArray(new Object[] { 'Donald', '1954-12-12' ] });</pre>
   * Results in:<pre>
   *   { 0 = Donald; 1 = '1954-12-12'; }</pre>
   * 
   * <p>
   * If no keys are specified, numeric Integer keys are generated (0,1,2,3,...).
   * <p>
   * If a format throws a ParseException, the exception is stored in the slot.
   * 
   * @param _array      - array of values
   * @param _keys       - array of Map keys to generate
   * @param _keyIndices - if null, _keys and _array indices must match
   * @param _keyFormats - value Format objects, when null, use value as-is
   * @return a Map constructed according to the specification
   */
  public static Map extractRecordFromArray
    (Object[] _array, Object[] _keys, int[] _keyIndices, Format[] _keyFormats,
     boolean _excludeNulls)
  {
    if (_array == null)
      return null;
    
    if (_keys == null)
      _keys = UList.listForCount(_array.length).toArray(); // 0,1,2,3
    
    final int count = _keys.length;
    final Map map   = new HashMap(count);
    
    for (int i = 0; i < count; i++) {
      final Object key = _keys[i];
      Object value;
      
      /* retrieve value */
      
      if (_keyIndices != null) {
        int idx = i < _keyIndices.length ? _keyIndices[i] : -1;
        value = i >= 0 && i < _array.length ? _array[idx] : null;
      }
      else
        value = (i < _array.length) ? value = _array[i] : null;
        
      /* apply format */
      
      if (_keyFormats != null && _keyFormats.length > i) {
        try {
          value = _keyFormats[i].parseObject(value.toString());
        }
        catch (ParseException e) {
          value = e;
        }
      }
      
      /* add to map */

      if (value != null || !_excludeNulls)
      map.put(key, value);
    }
    
    return map;
  }
  
  /**
   * Extracts a List of Maps from an two-dimensional array of values. This is
   * useful for converting parsed CSV files to a set of records.
   * <p>
   * Using the _keyIndices array you can use one value of the _array multiple
   * times, eg:<pre>
   *   array   = [ [ 'Donald', '1954-12-12' ] ]
   *   keys    = [ 'name', 'birthdayAsText', 'birthdayAsDate' ]
   *   indices = [ 0, 1, 1 ]
   *   formats = [ null, null, new SimpleDateFormat() ]</pre>
   * Results in:<pre>
   *   [ { name           = Donald;
   *       birthdayAsText = '1954-12-12';
   *       birthdayAsDate = &lt;CalendarDate: 1954-12-12&gt;; } ]</pre>
   * 
   * <p>
   * If no keys are specified, numeric Integer keys are generated (0,1,2,3,...).
   * 
   * @param _array      - an array of arrays of values
   * @param _keys       - array of Map keys to generate
   * @param _keyIndices - if null, _keys and _array indices must match
   * @param _keyFormats - value Format objects, when null, use value as-is
   * @return a List of Maps constructed according to the specification
   */
  public static List<Map> extractRecordsFromArrays
    (Object[][] _array, Object[] _keys, int[] _keyIndices, Format[] _keyFormats,
     boolean _excludeNulls)
  {
    if (_array == null)
      return null;
    
    final List<Map> list = new ArrayList<Map>(_array.length);
    
    for (Object[] recordArray: _array) {
      list.add(UList.extractRecordFromArray(
          recordArray, _keys, _keyIndices, _keyFormats, _excludeNulls));
    }
    
    return list;
  }
  
  
  /**
   * Converts Collections and primitive arrays to List's. If the object already
   * is a List, its returned directly.
   * 
   * @param _array - an arbitary Java array (eg int[], Object[]), or Collection
   * @return a List containing the object items
   */
  public static List asList(final Object _array) {
    // TBD: whats the difference to the Arrays.asList function?
    if (_array == null)
      return null;
    
    if (_array instanceof List)
      return (List)_array;
    if (_array instanceof Collection)
      return new ArrayList((Collection)_array);
    
    final Class itemClazz = _array.getClass().getComponentType();
    if (itemClazz == null) { /* not an array */
      List al = new ArrayList(1);
      al.add(_array);
      return al;
    }
    
    if (itemClazz == java.lang.Integer.TYPE) {
      final int[] nums = (int[])_array;
      List<Integer> al = new ArrayList<Integer>(nums.length);
      for (int i = 0; i < nums.length; i++)
        al.add(nums[i]);
      return al;
    }
    
    if (itemClazz == java.lang.Long.TYPE) {
      final long[] nums = (long[])_array;
      List<Long> al = new ArrayList<Long>(nums.length);
      for (int i = 0; i < nums.length; i++)
        al.add(nums[i]);
      return al;
    }

    return Arrays.asList((Object[])_array);
  }
  
  /**
   * Calls isEmpty() on each object of the given collection and returns only
   * those which are not empty.
   * 
   * @param _objects - Collection of objects
   * @return List of objects which are not considered 'empty'
   */
  public static List extractNonEmptyObjects(final Collection _objects) {
    if (_objects == null)
      return null;
    
    final List al = new ArrayList(_objects.size());
    for (Object o: _objects) {
      if (o instanceof NSObject) {
        if (((NSObject)o).isEmpty())
          continue;
      }
      else if (o instanceof String) {
        final String s = (String)o;
        if (s.length() == 0)
          continue;
        
        // TBD: trim?
      }

      al.add(o);
    }
    
    return al;
  }

  /**
   * Converts an array of objects into an array of int's.
   * If a slot is a Number object, this will store the intValue() of the number.
   * If its a String object, the String is parsed using Integer.parseInt(). If
   * a NumberFormatException occures, we store 0.
   * For other objects we invoke UObject.intValue() to determine the 'int'
   * representation of the object.
   * 
   * @param _values - an array of objects to convert to integers
   * @return an array of int's
   */
  public static int[] intValuesForObjects(final Object[] _values) {
    if (_values == null)
      return null;
    
    final int   count = _values.length;
    final int[] nums = new int[count];
    for (int i = 0; i < count; i++) {
      final Object v = _values[i];
    
      if (v instanceof Number) {
        nums[i] = ((Number)v).intValue();
      }
      else if (v instanceof String) {
        /* Note: we return 0 for empty strings */
        if (((String)v).length() == 0) {
          nums[i] = 0;
        }
        else {
          try {
            nums[i] = Integer.parseInt((String)v);
          }
          catch (NumberFormatException e) {
            nums[i] = 0;
          }
        }
      }
      else
        nums[i] = UObject.intValue(v.toString());
    }
    
    return nums;
  }

  /**
   * Returns a List which contains a sequence of Integer objects.
   * <p>
   * Example:
   * <pre>List<Number> a = UList.listForCount(10);</pre>
   * This will return the List "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]".
   * 
   * @param _count - number of items in the List
   * @return a List
   */
  public static List<Number> listForCount(final int _count) {
    // TBD: we should return a special List object which calculates the indices
    //      on the fly
    if (_count < 0)
      return null; // TBD: we could return neg lists, eg 0,-1,-2,-3,-4
    if (_count == 0)
      return Collections.EMPTY_LIST;
    
    final List<Number> l = new ArrayList<Number>(_count);
    for (int i = 0; i < _count; i++)
      l.add(new Integer(i));
    return l;
  }
}
