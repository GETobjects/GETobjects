package org.getobjects.foundation.kvc;

import org.getobjects.foundation.UObject;

/**
 * Adds special KVC syntax for accessing slices of strings utilizing
 * the same specification as Python's slices, i.e.
 * 
 * <p>
 * <code>[start:stop:step]</code>
 * </p>
 * 
 * <p>
 * <code>start</code>, <code>:stop</code> and <code>:step</code>
 * are all optional. If set, <code>step</code> MUST NOT be zero.
 * </p>

 * <p>
 * Note that <code>[:]</code> and <code>[::]</code> are valid expressions,
 * whereas <code>[]</code> is NOT!
 * </p>
 * 
 * <pre>
 * +---+---+---+---+
 * | T | e | s | t |  <= string characters
 * +---+---+---+---+
 * | 0 | 1 | 2 | 3 |  <= positive indexes
 * +---+---+---+---+
 * |-4 |-3 |-2 |-1 |  <= negative indexes
 * +---+---+---+---+
 * </pre>
 * 
 * <p>
 * EXAMPLES:
 * <pre>
 * "Test".[1:] -> "est"
 * "Test".[-1] -> "t"
 * </pre>
 * </p>
 * <p>
 * NOTE:

 * Contrary to Python's implementation, index accesses which are out of bounds
 * don't throw exceptions: instead, they're logged as KVCWrapper warnings
 * and <code>null</code> is returned as result. This allows the result to
 * be properly distinguished but also handled gracefully.
 * <pre>
 * "".[-1]  -> null
 * "".[-1:] -> ""
 * "".[0]   -> null
 * "".[:0]  -> ""
 * </pre>
 * </p>
 **/

public class StringKVCWrapper extends KVCWrapper {
  
  public abstract static class StringSliceAccessor implements IPropertyAccessor {

    // special placeholder to distinguish valid indexes from an unset index
    public static final int NO_INDEX = Integer.MIN_VALUE;
    
    public static StringSliceAccessor stringSliceAccessorForSpec(final String _spec) {
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
        return new StringSliceIndexAccessor(_spec);
      
      final int lastColonIdx = _spec.lastIndexOf(':');
      if (firstColonIdx == lastColonIdx)
        return new StringSliceRangeAccessor(_spec, firstColonIdx);

      // optimization
      final String stepCountString = _spec.substring(lastColonIdx + 1, _spec.length() - 1).trim();
      int stepCount = 1;
      if (UObject.isNotEmpty(stepCountString))
        stepCount = Integer.parseInt(stepCountString);
      if (stepCount == 1)
        return new StringSliceRangeAccessor(_spec.substring(0, lastColonIdx + 1), firstColonIdx); // way faster
      else if (stepCount == 0) {
        throw new IllegalArgumentException("slice step cannot be zero");
      }
      // NOTE: this does include the trailing ':' on purpose!
      return new StringSliceRangeWithStepAccessor(_spec.substring(0, lastColonIdx + 1), firstColonIdx, stepCount);
    }
    
    public StringSliceAccessor(final String _spec) {
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
    public void set(Object instance, String key, Object value) {
    }
  }

  /**
   * Examples:
   * <pre>
   * "Test".[0]  -> "T"
   * "Test".[4]  -> null
   * "Test".[-1] -> "t"
   * "Test".[-5] -> null
   * </pre>
   **/
  public static class StringSliceIndexAccessor extends StringSliceAccessor {

    protected int index;

    public StringSliceIndexAccessor(final String _spec) {
      super(_spec);
      this.index = indexFromString(_spec.substring(1, _spec.length() - 1));
    }
    
    @Override
    public Object get(final Object _stringObj, final String _key) {
      if (this.index == NO_INDEX)
        return null;

      final int sLength = ((String)_stringObj).length();
      if (this.index < 0) {
        this.index = sLength + this.index;
      }
      if (this.index < 0 || this.index >= sLength) {
        KVCWrapper.logger.warn("StringIndexOutOfBounds for " + _key);
        return null;
      }
      return ((String)_stringObj).charAt(this.index);
    }
  }

  /**
   * Examples:
   * <pre>
   * "Test".[:]   -> "Test"
   * "Test".[1:]  -> "est"
   * "Test".[4:]  -> ""
   * "Test".[2:2] -> ""
   * "Test".[2:3] -> "s"
   * "Test".[:-2] -> "Te"
   * "Test".[:2]  -> "Te"
   * "Test".[-1:] -> "t"
   * "Test".[-5:] -> "Test"
   * </pre>
   */
  public static class StringSliceRangeAccessor extends StringSliceAccessor {

    protected int sepIdx;
    protected int startIdx;
    protected int stopIdx;
 
    public StringSliceRangeAccessor(final String _spec, final int _rangeSeparationIndex) {
      super(_spec);

      this.sepIdx = _rangeSeparationIndex;
      final int specLength = _spec.length();

      this.startIdx = indexFromString(_spec.substring(1, this.sepIdx));
      this.stopIdx  = NO_INDEX;
      if ((this.sepIdx + 1) < (specLength - 1)) {
        this.stopIdx = indexFromString(_spec.substring(this.sepIdx + 1, specLength - 1));
      }
    }
    
    @Override
    public Object get(final Object _stringObj, final String _spec) {
      final int sLength = ((String)_stringObj).length();

      // normalize all indexes to fit the actual string
      if (this.startIdx < 0 && this.startIdx != NO_INDEX) {
        this.startIdx = sLength + this.startIdx;
        if (this.startIdx < 0)
          this.startIdx = 0;          
      }
      else if (this.startIdx > sLength) {
        this.startIdx = sLength;
      }
      if (this.stopIdx < 0 && this.stopIdx != NO_INDEX) {
        this.stopIdx = sLength + this.stopIdx;
        if (this.stopIdx < 0)
          this.stopIdx = 0;
      }
      else if (this.stopIdx > sLength) {
        this.stopIdx = sLength;
      }

      if (this.startIdx != NO_INDEX && this.stopIdx == NO_INDEX) {
        return ((String)_stringObj).substring(this.startIdx);
      }
      else if (this.startIdx == NO_INDEX && this.stopIdx != NO_INDEX) {
        return ((String)_stringObj).substring(0, this.stopIdx);
      }
      if (this.startIdx == NO_INDEX && this.stopIdx == NO_INDEX) {
        return _stringObj; // [:]
      }
      return ((String)_stringObj).substring(this.startIdx, this.stopIdx);          
    }
  }

  /**
   * Examples:
   * <pre>
   * "Test".[::]     -> "Test"
   * "Test".[::-1]   -> "tseT"
   * "Test".[1:4:2]  -> "et"
   * "Test".[-2::2]  -> "s"
   * "Test".[-2::-2] -> "sT"
   * </pre>
   */
  public static class StringSliceRangeWithStepAccessor extends StringSliceRangeAccessor {

    protected int stepCount;
    
    public StringSliceRangeWithStepAccessor(final String _spec, final int _rangeSeparationIndex, final int _stepCount) {
      super(_spec, _rangeSeparationIndex);
      this.stepCount = _stepCount;
    }
    
    @Override
    public Object get(final Object _stringObj, final String _spec) {
      final String s = (String)_stringObj;
      final int sLength = s.length();

      StringBuilder sb = new StringBuilder(sLength);

      final boolean ascending = this.stepCount > 0;
      if (ascending) {
        int idx = this.startIdx;
        if (idx < 0) // includes NO_INDEX!
          idx += sLength;
        if (idx < 0)
          idx = 0;
        int maxIdx = this.stopIdx;
        if (maxIdx < 0) {
          if (maxIdx == NO_INDEX)
            maxIdx = sLength - 1;
          else
            maxIdx += sLength;
        }
        else {
          maxIdx -= 1;
        }
        if (maxIdx >= sLength)
          maxIdx = sLength - 1;

        for (;; idx += this.stepCount) {
          if (idx > maxIdx)
            break;
          sb.append(s.charAt(idx));
        }
      }
      else {
        int idx = this.startIdx;
        if (idx < 0) {
          if (idx == NO_INDEX)
            idx = sLength - 1;
          else
            idx += sLength;
        }
        if (idx >= sLength)
          idx = sLength - 1;

        int minIdx = this.stopIdx;
        if (minIdx < 0) {
          if (minIdx == NO_INDEX)
            minIdx = 0;
          else
            minIdx += sLength;
        }
        else {
          minIdx += 1;
        }
        if (minIdx < 0)
          minIdx = 0;

        for (;; idx += this.stepCount) {
          if (idx < minIdx)
            break;
          sb.append(s.charAt(idx));
        }
      }
      return sb.toString();
    }
  }

  public StringKVCWrapper(Class _class) {
    super(_class);
  }

  @Override
  public IPropertyAccessor getAccessor(final Object _target, final String _name) {
    // this is invoked by valueForKey/takeValueForKey of NSObject and
    // NSKeyValueCoding.DefaultImplementation
    IPropertyAccessor result;

    result = super.getAccessor(_target, _name);

    if (result == null)
      result = StringSliceAccessor.stringSliceAccessorForSpec(_name);

    return result;
  }
}
