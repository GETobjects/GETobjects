package org.getobjects.foundation.kvc;

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UObject;

public class ListKVCWrapper extends KVCWrapper {

  public abstract static class ListSliceAccessor implements IPropertyAccessor {

    // special placeholder to distinguish valid indexes from an unset index
    public static final int NO_INDEX = Integer.MIN_VALUE;

    public static ListSliceAccessor listSliceAccessorForSpec(final String _spec) {
      if (UObject.isEmpty(_spec))
        return null;

      if ((_spec.length() < 3) ||
          !(_spec.startsWith("[") && _spec.endsWith("]")))
      {
        KVCWrapper.logger.error("Incorrect slice specification: " + _spec);
        return null;
      }

      final int firstColonIdx = _spec.indexOf(':');
      if (firstColonIdx == -1)
        return new ListSliceIndexAccessor(_spec);

      throw new IllegalArgumentException("ranges not implemented");
    }

    public ListSliceAccessor(final String _spec) {
      KVCWrapper.logger.debug("got specification: " + _spec);
    }

    protected int indexFromString(final String _index) {
      if (UObject.isEmpty(_index))
        return NO_INDEX;
      return Integer.parseInt(_index);
    }

    @Override
    public boolean canReadKey(final String key) {
      return true;
    }

    @Override
    public boolean canWriteKey(final String key) {
      return false;
    }

    @Override
    public Class getWriteType() {
      return null;
    }

    @Override
    public void set(final Object instance, final String key, final Object value) {
    }
  }

  /**
   * Examples:
   * <pre>
   * ("T").[0] -> "T"
   * ("T").[1] -> null
   * ("T", "t").[-1] -> "t"
   * ("T", "t").[-3] -> null
   * </pre>
   **/
  public static class ListSliceIndexAccessor extends ListSliceAccessor {

    protected int index;

    public ListSliceIndexAccessor(final String _spec) {
      super(_spec);
      this.index = indexFromString(_spec.substring(1, _spec.length() - 1));
    }

    @Override
    public Object get(final Object _listObj, final String _key) {
      if (this.index == NO_INDEX)
        return null;

      final int lCount = ((List)_listObj).size();
      if (this.index < 0) {
        this.index = lCount + this.index;
      }
      if (this.index < 0 || this.index >= lCount) {
        KVCWrapper.logger.warn("IndexOutOfBounds for " + _key);
        return null;
      }
      return ((List)_listObj).get(this.index);
    }
  }

  public static class ListAccessor implements IPropertyAccessor {

    public ListAccessor() {
    }

    @Override
    public boolean canReadKey(final String key) {
      return true;
    }

    @Override
    public Object get(final Object _instance, final String _key) {
      @SuppressWarnings("unchecked")
      final List<Object> l = (List<Object>)_instance;

      // TODO: avg, etc.
      // @see https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/KeyValueCoding/CollectionOperators.html
      if (_key.equals("@count"))
        return l.size();

      final List<Object> tmp = new ArrayList<>(l.size());
      for (final Object o : l) {
        final Object v = NSKeyValueCoding.Utility.valueForKey(o, _key);
        if (v != null)
          tmp.add(v);
      }
      return tmp;
    }

    @Override
    public boolean canWriteKey(final String key) {
      return true;
    }

    @Override
    public void set
      (final Object _target, final String _key, final Object _value)
    {
      @SuppressWarnings("unchecked")
      final List<Object> l = (List<Object>)_target;
      for (final Object o : l) {
        NSKeyValueCoding.Utility.takeValueForKey(o, _value, _key);
      }
    }

    @Override
    public Class getWriteType() {
      return Object.class;
    }
  }

  private static final IPropertyAccessor commonListAccessor = new ListAccessor();

  public ListKVCWrapper(final Class _class) {
    super(_class);

  }

  @Override
  public IPropertyAccessor getAccessor(final Object _target, final String _name) {
    // this is invoked by valueForKey/takeValueForKey of NSObject and
    // NSKeyValueCoding.DefaultImplementation

    final IPropertyAccessor result = super.getAccessor(_target, _name);
    if (result != null)
      return result;

    if (UObject.isNotEmpty(_name) &&
        _name.startsWith("[") && _name.endsWith("]"))
    {
      return ListSliceAccessor.listSliceAccessorForSpec(_name);
    }
    return commonListAccessor;
  }
}
