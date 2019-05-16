package org.getobjects.foundation.kvc;

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSKeyValueCoding;

public class ListKVCWrapper extends KVCWrapper {

  public static class ListAccessor implements IPropertyAccessor {

    public ListAccessor() {
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
    public Class getWriteType() {
      return Object.class;
    }

    @Override
    public void set
      (final Object _target, final String _key, final Object _value)
    {
    }

    @Override
    public String toString() {
      return "ListKVCWrapper.ListAccessor";
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
    IPropertyAccessor result;

    result = super.getAccessor(_target, _name);

    if (result == null)
      result = commonListAccessor;

    return result;
  }

}
