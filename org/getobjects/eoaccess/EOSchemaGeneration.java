/*
  Copyright (C) 2006 Helge Hess

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

import java.util.List;
import java.util.Map;

public interface EOSchemaGeneration {
  
  /* constants */
  
  public static final String DropTablesKey   = "DropTables";
  public static final String CreateTablesKey = "CreateTables";

  /* tables */
  
  public List<EOSQLExpression> schemaCreationStatementsForEntities
    (List<EOEntity> _entities, Map<String, Object> options);

  public List<EOSQLExpression> createTableStatementsForEntityGroup
    (List<EOEntity> _group);
  public List<EOSQLExpression> dropTableStatementsForEntityGroup
    (List<EOEntity> _group);
  
  public List<EOSQLExpression> createTableStatementsForEntityGroups
    (List<List<EOEntity>> _groups);
  public List<EOSQLExpression> dropTableStatementsForEntityGroups
    (List<List<EOEntity>> _groups);
}
