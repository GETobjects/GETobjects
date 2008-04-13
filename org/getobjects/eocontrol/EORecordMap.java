package org.getobjects.eocontrol;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.kvc.MissingPropertyException;

/**
 * EORecordMap
 * <p>
 * A Map implementation which is optimized for result set objects.
 * Keys are not allowed to be null, values can be null.
 * <p>
 * The basic feature is that the keys and the key hashes are shared between
 * all result objects.
 */
public class EORecordMap extends AbstractMap<String, Object>
  implements Map<String, Object>, Cloneable, 
             NSKeyValueCoding, NSKeyValueCodingAdditions
{
  protected int      size;
  protected String[] keys;
  protected int[]    keyHashes;
  protected Object[] values;
  protected Set<Map.Entry<String, Object>> entrySet;
  
  public EORecordMap(String[] _keys, int[] _hashes, Object[] _values) {
    /* Those arrays are used as-is, they are considered constant for a
     * group of records!
     */
    this.size = _keys != null ? _keys.length : 0;
    this.keys = _keys;
    
    if (_hashes != null)
      this.keyHashes = _hashes;
    else {
      this.keyHashes = new int[this.size];
      for (int i = 0; i < this.size; i++)
        this.keyHashes[i] = _keys[i].hashCode();
    }
    
    /* this array must be copied */
    this.values = new Object[this.size];
    if (_values != null) {
      for (int i = this.size - 1; i >= 0; i--)
        this.values[i] = _values[i];
    }
  }
  public EORecordMap(String[] _keys, int[] _hashes) {
    this(_keys, _hashes, null /* no values */);
  }
  public EORecordMap(String[] _keys) {
    this(_keys, null /* calc hashes */, null /* no values */);
  }
  
  
  /* basic ops */
  
  /**
   * Changes the key of some value. This is used during column=>attribute
   * mapping.
   * <p>
   * CAREFUL: this modifies the key in ALL 'associated' records (eg all results
   * of a single fetch).
   * 
   * @param _oldKey - key to replace (eg c_first_name)
   * @param _newKey - new key to use (eg firstName)
   * @return true if the key was found (and got switched), false if not
   */
  public boolean switchKey(final String _oldKey, final String _newKey) {
    final int oldHash = _oldKey.hashCode();
    
    for (int i = this.size - 1; i >= 0; i--) {
      if (this.keyHashes[i] == oldHash) {
        /* same hash */
        if (this.keys[i] == _oldKey || _oldKey.equals(this.keys[i])) {
          this.keys[i]      = _newKey;
          this.keyHashes[i] = _newKey.hashCode();
          return true; /* DONE */
        }
      }
    }
    return false;
  }
  
  @Override
  public Object put(final String _key, final Object _value) {
    if (_key == null)
      return null;
    
    final int keyHash = _key.hashCode();
    for (int i = this.size - 1; i >= 0; i--) {
      if (this.keyHashes[i] == keyHash) {
        /* same hash */
        if (this.keys[i] == _key || _key.equals(this.keys[i])) {
          Object oldValue = this.values[i];
          this.values[i] = _value;
          return oldValue; /* DONE */
        }
      }
    }
    
    throw new NSException("given key is not a fetch result: " + _key);
  }

  @Override
  public Object get(final Object _key) {
    if (_key == null) return null;
    final int keyHash = _key.hashCode();
    
    for (int i = this.size - 1; i >= 0; i--) {
      if (this.keyHashes[i] == keyHash) {
        /* same hash */
        if (this.keys[i] == _key || _key.equals(this.keys[i]))
          return this.values[i];
      }
    }
    return null;
  }
  
  @Override
  public boolean containsKey(final Object _key) {
    if (_key == null) return false;
    
    final int keyHash = _key.hashCode();
    for (int i = this.size - 1; i >= 0; i--) {
      if (this.keyHashes[i] == keyHash) {
        /* same hash */
        if (this.keys[i] == _key || _key.equals(this.keys[i]))
          return true;
      }
    }
    return false;
  }
  
  
  /* AbstractMap Entry Point */

  @Override
  public Set<Map.Entry<String, Object>> entrySet() {
    if (this.entrySet == null)
      this.entrySet = new EntrySet(this);
    return this.entrySet;
  }

  
  /* cloning */
  
  @Override
  public Object clone() {
    return new EORecordMap(this.keys, this.keyHashes, this.values);
  }
  
  
  /* KVC */
  
  public void takeValueForKey(final Object _value, final String _key) {
    if (_key != null) {
      final int keyHash = _key.hashCode();

      for (int i = this.size - 1; i >= 0; i--) {
        if (this.keyHashes[i] == keyHash) {
          /* same hash */
          if (this.keys[i] == _key || _key.equals(this.keys[i])) {
            this.values[i] = _value;
            return; /* DONE */
          }
        }
      }
    }
    
    /* failed */
    this.handleTakeValueForUnboundKey(_value, _key);
  }
  public Object valueForKey(final String _key) {
    if (_key == null) return null;
    final int keyHash = _key.hashCode();
    
    for (int i = this.size - 1; i >= 0; i--) {
      if (this.keyHashes[i] == keyHash) {
        /* same hash */
        if (this.keys[i] == _key || _key.equals(this.keys[i]))
          return this.values[i];
      }
    }
    return this.handleQueryWithUnboundKey(_key);
  }
  
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    // do nothing?
    throw new MissingPropertyException(_value, _key);
  }
  public Object handleQueryWithUnboundKey(String _key) {
    return null;
  }

  public void takeValueForKeyPath(final Object _value, final String _keyPath) {
    NSKeyValueCodingAdditions.DefaultImplementation.
      takeValueForKeyPath(this, _value, _keyPath);
  }
  public Object valueForKeyPath(final String _keyPath) {
    return NSKeyValueCodingAdditions.DefaultImplementation.
             valueForKeyPath(this, _keyPath);    
  }
  
  /**
   * Calls takeValueForKey() for each key/value pair in the Map.
   * 
   * @param _map - the key/value pairs to be applied on the object
   */
  public void takeValuesFromDictionary(final Map<String, Object> _map) {
    if (_map == null)
      return;
    
    for (String key: _map.keySet())
      this.takeValueForKey(_map.get(key), key);
  }
  /**
   * Calls valueForKey() for each key in the array. If there is no value for the
   * given key (method returned 'null'), we do NOT add the value to the Map.
   * <p>
   * If the key array is empty, we still return an empty map. If the key array
   * is null, we return null.
   * 
   * @param _keys - keys to be extracted
   * @return a Map containg the values for the keys, null if _keys is null
   */
  public Map<String, Object> valuesForKeys(final String[] _keys) {
    if (_keys == null)
      return null;
    
    Map<String, Object> vals = new HashMap<String, Object>(_keys.length);
    if (_keys.length == 0) return vals;
    
    for (int i = 0; i < _keys.length; i++) {
      Object v = this.valueForKey(_keys[i]);
      if (v != null) vals.put(_keys[i], v);
    }
    return vals;
  }

  
  /* Entry helpers */
  
  static class EntrySet extends AbstractSet<Map.Entry<String, Object>>
    implements Set<Map.Entry<String, Object>>
  {
    
    protected EORecordMap map;
    protected Entry[] entries;
    
    public EntrySet(final EORecordMap _map) {
      this.map = _map;
    }

    public boolean contains(final Object _o) {
      // TBD: complete me
      if (!(_o instanceof Entry))
        return false;
      
      // hm, should be ok ;-)
      if (((Entry)_o).map == this.map)
        return true;
      
      return false;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
      if (this.entries == null)
        this.entries = new Entry[this.map.size];
      
      return new EntrySetIterator(this);
    }

    @Override
    public int size() {
      return this.map.size;
    }
    
  }
  
  static class EntrySetIterator implements Iterator<Map.Entry<String, Object>> {

    protected EORecordMap map;
    protected EntrySet    set;
    protected int         idx;
    
    public EntrySetIterator(final EntrySet _set) {
      this.set = _set;
      this.map = _set.map;
      this.idx = 0;
    }

    public boolean hasNext() {
      return this.idx < this.map.size;
    }

    public Entry next() {
      if (this.idx >= this.map.size)
        return null;
      
      Entry e = this.set.entries[this.idx];
      if (e == null) {
        e = new Entry(this.map, this.idx);
        this.set.entries[this.idx] = e;
      }
      
      this.idx++;
      return e;
    }

    public void remove() {
      throw new UnsupportedOperationException("remove not supported");
    }
    
  }

  static class Entry implements Map.Entry<String,Object> {
    
    protected EORecordMap map;
    protected int         idx;
    
    public Entry(final EORecordMap _map, final int _idx) {
      this.map = _map;
      this.idx = _idx;
    }

    public String getKey() {
      return this.map.keys[this.idx];
    }

    public Object getValue() {
      return this.map.values[this.idx];
    }

    public Object setValue(Object _value) {
      final Object oldValue = this.map.values[this.idx];
      this.map.values[this.idx] = _value;
      return oldValue;
    }
    
  }
}
