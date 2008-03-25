package org.getobjects.appserver.core;


/**
 * IWOAssociation
 * <p>
 * Associations define how
 * dynamic elements (stateless, non-WOComponent template elements) pull and
 * push their 'bindings'.
 * <p>
 * The most common implementors are <code>WOKeyPathAssociation</code>, which
 * pushes/pulls values into/from the current component in the context,
 * and <code>WOValueAssociation</code>, which just wraps a constant value in
 * the WOAssociation API.
 * <br>
 * But in addition there are associations which evaluate OGNL expressions,
 * which resolve their value as localization keys or which resolve string
 * patterns in a certain context. etc etc
 */
public interface IWOAssociation {

  /* reflection */
  
  /**
   * Returns true if the association always returns the same value. This can be
   * used by dynamic elements to cache the value (and discard the association
   * wrapper).
   * 
   * @return true if the value of the association never changes, false otherwise
   */
  public boolean isValueConstant();
  
  /**
   * Returns true if the association accepts new values. Eg a constant
   * association obviously doesn't accept new values. A KVC association to a
   * target which does not have a <code>set</code> accessor could also return
   * false (but currently does not ...).
   * 
   * @return true if the value of the association can be set, false otherwise
   */
  public boolean isValueSettable();
  
  /**
   * Returns true if the association always returns the same value for the
   * specified cursor (usually a component). This can be used by dynamic
   * elements to cache the value.
   * 
   * @return true if the value of the association does not change
   */
  public boolean isValueConstantInComponent(Object _cursor);
  
  /**
   * Returns true if the association can accept new values for the given cursor
   * (usually a WOComponent). A KVC association to a target which does not have
   * a <code>set</code> accessor could also return false (but currently does
   * not ...).
   * 
   * @return true if the value of the association can be set, false otherwise
   */
  public boolean isValueSettableInComponent(Object _cursor);
  
  
  /* values */
  
  public void setValue(Object _value, Object _cursor);
  public Object valueInComponent(Object _cursor);
  
  
  /* specific values */
  
  public void setBooleanValue(boolean _value, Object _cursor);
  public boolean booleanValueInComponent(Object _cursor);
  
  public void setIntValue(int _value, Object _cursor);
  public int intValueInComponent(Object _cursor);
  
  public void setStringValue(String _value, Object _cursor);
  public String stringValueInComponent(Object _cursor);

}
