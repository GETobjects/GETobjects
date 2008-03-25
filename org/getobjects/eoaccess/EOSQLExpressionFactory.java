/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.eoaccess;

import java.util.Map;

import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;

/**
 * EOSQLExpressionFactory
 * <p>
 * The expression factory is exposed by the EOAdaptor. Most adaptors subclass
 * this object to add support for their own SQL syntax. For example MySQL uses
 * backticks for quoting schema identifiers while PostgreSQL uses double quotes.
 * <p>
 * Such differences are covered by subclasses of EOSQLExpression which generate
 * the actual SQL. This factory constructs the specific EOSQLExpression objects.
 * <p>
 * Usually the methods of this class are called by EOAdaptorChannel to
 * build SQL for non-raw fetches and changes.
 * But you can use EOSQLExpression in your own code if you want to perform
 * raw SQL but still want to generate database independend SQL.
 * However its recommended to use EOFetchSpecifications with an SQL hint
 * instead, eg:<pre>
 *   &lt;fetch name="selectCount"&gt;
 *     &lt;sql&gt;SELECT * FROM MyTable&lt;/sql&gt;
 *   &lt;/fetch&gt;</pre>
 */
public class EOSQLExpressionFactory extends NSObject {

  protected EOAdaptor adaptor;
  protected Class     expressionClass;
  
  public EOSQLExpressionFactory(EOAdaptor _adaptor) {
    this.adaptor = _adaptor;
    
    if (this.adaptor != null)
      this.expressionClass = this.adaptor.expressionClass();
  }
  
  /* accessors */
  
  public EOAdaptor adaptor() {
    return this.adaptor;
  }
  
  /* factory */
  
  public EOSQLExpression createExpression(EOEntity _e) {
    if (this.expressionClass != null) {
      return (EOSQLExpression)NSJavaRuntime.NSAllocateObject
               (this.expressionClass, EOEntity.class, _e);
    }
    return new EOSQLExpression(_e);
  }
  
  public EOSQLExpression expressionForString(String _sql) {
    if (_sql == null || _sql.length() == 0) return null;
    EOSQLExpression e = this.createExpression(null /* entity */);
    e.setStatement(_sql);
    return e;
  }
  
  public EOSQLExpression deleteStatementWithQualifier
    (EOQualifier _qualifier, EOEntity _entity)
  {
    EOSQLExpression e = this.createExpression(_entity);
    e.prepareDeleteExpressionForQualifier(_qualifier);
    return e;
  }
  
  public EOSQLExpression insertStatementForRow
    (Map<String, Object> _row, EOEntity _entity)
  {
    EOSQLExpression e = this.createExpression(_entity);
    e.prepareInsertExpressionWithRow(_row);
    return e;
  }

  public EOSQLExpression updateStatementForRow
    (Map<String, Object> _row, EOQualifier _qualifier, EOEntity _entity)
  {
    EOSQLExpression e = this.createExpression(_entity);
    e.prepareUpdateExpressionWithRow(_row, _qualifier);
    return e;
  }
  
  public EOSQLExpression selectExpressionForAttributes
    (EOAttribute[] _attrs, boolean _lock, EOFetchSpecification _f, EOEntity _e)
  {
    /* This is called by the EOAdaptorChannel to construct the SQL required to
     * execute a fetch. At this point no SQL has been processed and the SQL
     * hints are still in the fetch specification.
     */
    
    /*
     * Let the adaptor construct a new, database specific expression object. 
     */
    EOSQLExpression e = this.createExpression(_e);
    
    /*
     * Note: Despite the name 'prepare' this already constructs the SQL inside
     *       the expression. It also processes the raw SQL hints in the fetch
     *       specification.
     *       
     *       Bindings are resolved *before* this step. Don't be confused by the
     *       difference between EOFetchSpecification/EOQualifier binding
     *       variables and SQL bindings used in the SQL.
     */
    e.prepareSelectExpressionWithAttributes(_attrs, _lock, _f);
    return e;
  }
  
  /* description */

  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.adaptor != null) _d.append(" adaptor=" + this.adaptor);
  }
}
