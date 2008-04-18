/*
  Copyright (C) 2008 Helge Hess

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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.eocontrol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * EOExpression
 * <p>
 * EOExpression is an extension of the EOQualifier idea. While EOQualifier's
 * can be used in-memory, they are usually representations of SQL boolean
 * expressions.
 * That is, they are used as in-memory representations of things which can be
 * used in SQL WHERE statements.
 * <p>
 * Example:<pre>
 *   lastname LIKE 'Duck*' AND status != 'archived'</pre>
 * The feature is that you can use keypathes in the qualifier keys. Eg
 * <code>customer.address.city = 'Magdeburg'</code>.
 * When this is converted to SQL by EOSQLExpression, EOSQLExpression will
 * automatically generated the proper joins etc.
 * <p>
 * Back to EOExpression. EOExpression objects are objects which can return
 * arbirary values, not just booleans. Consider this SQL:<pre>
 *   SELECT amount / 1.19 FROM journal WHERE amount + 1000 > 2000;</pre>
 * This is hard to do with just qualifiers, but its perfectly supported by
 * modern SQL databases.
 * <p>
 * Now the most important EOExpression is EOKey. EOKey is a 'column reference',
 * can be either a direct key like 'lastname' or a keypath like 'address.city'.
 * 
 * <p>
 * Inline evaluation. Just like EOQualifierEvaluation provides inline
 * evaluation of qualifiers, EOExpressionEvaluation provides inline
 * evaluation of expressions. Only objects which implement that interface
 * can be evaluated in the appserver.
 * 
 * <p>
 * Note: EOExpression like EOQualifier objects are by definition immutable.
 * Hence clone() just returns this.
 * 
 * <p>
 * Summary: EOExpression streamlines the idea of an EOQualifier into a generic
 * EOExpression which can have multiple value results.
 */
public class EOExpression extends NSObject implements Cloneable {
  protected static final Log log = LogFactory.getLog("EOExpression");

  /* Cloneable */
  
  @Override
  protected Object clone() throws CloneNotSupportedException {
    /* EOExpressions are immutable */
    return this;
  }
}
