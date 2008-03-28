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

package org.getobjects.eocontrol;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.EORawSQLValue;
import org.getobjects.foundation.NSObject;

/**
 * EOQualifierParser
 * <p>
 * Parses EOQualifier objects from a char buffer. Qualifiers look like a
 * SQL WHERE statement, but some special rules apply.
 * 
 * <p>
 * <h4>EOKeyValueQualifier</h4>
 * Example:
 * <pre>lastname like 'h*'</pre>
 * 
 * 
 * <p>
 * <h4>Comparison Operations</h4>
 * <ul>
 *   <li>=
 *   <li>!=
 *   <li>&lt;
 *   <li>&gt;
 *   <li>=&lt; / &lt;= 
 *   <li>=&gt; / &gt;=
 *   <li>&lt;&gt; / &gt;&lt; 
 *   <li>LIKE
 *   <li>IN
 *   <li>IS NULL / IS NOT NULL
 *   <li>custom identifiers, eg: 'hasPrefix:'
 *   <li>Note: you can use formats, eg: ("lastname %@ 'Duck'", "LIKE")
 * </ul>
 * 
 * 
 * <p>
 * <h4>Constants</h4>
 * <ul>
 *   <li>numbers - 12345
 *   <li>strings - 'hello world' or "hello world"
 *   <li>boolean - true/false/YES/NO
 *   <li>null    - NULL / null (no support for nil!)
 *   <li>casts   - start with a '(', eg (Date)'2007-09-21'
 * </ul>
 * 
 * 
 * <p>
 * <h4>EOQualifier Bindings</h4>
 * Bindings are used to fill values into the qualifier at a later time. Each
 * binding has a name which can be used multiple times in a single qualifier.
 * The binding is represented as a EOQualifierVariable object once it got
 * parsed.
 * <br>
 * Example:
 * <pre>lastname = $lastname AND firstname = $firstname</pre>
 * Matching Java code:
 * <pre>
 *   EOQualifier q = EOQualifier.qualifierWithQualifierFormat
 *      ("lastname = $lastname AND firstname = $firstname");
 *   q = q.qualifierWithBindings(this);
 * </pre>
 * The <code>qualifierWithBindings</code> method will ask 'this' for the
 * 'lastname' and 'firstname' keys using KVC.
 * 
 * 
 * <p>
 * <h4>Patterns</h4>
 * You can embed patterns in a qualifier format, eg:
 * <pre>lastname like %@</pre>
 * The pattern is resolved during format parsing, in the above case the
 * matching Java code would look like:
 * <pre>
 *   EOQualifier q = EOQualifier.qualifierWithQualifierFormat
 *      ("lastname like %@", "Duck");
 * </pre>
 * (usually the argument will be some instance variable, eg one which is filled
 * from a search field).
 * <br />
 * There is no strict rule when to use Patterns and when to using Bindings.
 * Usually bindings are more convenient to map to control elements (because
 * the bindings dictionary can be filled conveniently using KVC).
 * <ul>
 *   <li>%@ - use given value as-is
 *   <li>%s - convert value to String
 *   <li>%i / %d - convert value to Integer
 *   <li>%f - convert value to Double
 *   <li>%K - a key, this will result in a EOKeyComparisonQualifier
 *   <li>%% - to escape %
 * </ul>
 * 
 * 
 * <p>
 * <h4>True/False Qualifiers</h4>
 * Those are sometimes useful in rules, they always match or fail:
 * <pre> *true*
 * *false*</pre>
 * 
 * 
 * <p>
 * <h4>SQL Qualifiers</h4>
 * To embed SQL in your qualifiers, you can use the <code>SQL[]</code>
 * construct, eg:
 * <pre>lastname = 'Duck' AND SQL[ EXISTS (SELECT 1 FROM permissions) ]</pre>
 * A SQL qualifier can even include bindings. The qualifier is represented as
 * a EOSQLQualifier object at runtime, which in turn is a sequence of 'parts'.
 * Those parts are either EOQualifierVariable's or EORawSQLValue's (those are
 * output as-is by EOSQLExpression).
 * 
 */
public class EOQualifierParser extends NSObject {
  protected static final Log log = LogFactory.getLog("EOQualifierParser");
  
  protected static final Object[] emptyObjectArray = {};

  /* input */
  protected char[]   string;
  protected Object[] args;
  
  /* processing status */
  protected int      idx;
  protected int      currentArgument;
  
  /* constructors */
  
  public EOQualifierParser(char[] _content, Object[] _args) {
    this.string = _content;
    this.args   = _args;
    
    if (this.args == null) this.args = emptyObjectArray;
    this.currentArgument = 0;
  }
  
  /* main entry */

  public EOQualifier parseQualifier() {
    if (!this.skipSpaces()) return null; /* EOF */
    return this.parseCompoundQualifier();
  }
  
  public void reset() {
    this.string = null;
    this.args   = null;
    this.idx    = -1;
    this.currentArgument = -1;
  }
  
  /* parsing */

  protected EOQualifier parseOneQualifier() {
    if (!this.skipSpaces()) return null; /* EOF */
    
    /* sub-qualifiers in parenthesis */
    
    if (this.match('('))
      return this.parseCompoundQualifierInParenthesis();
    
    /* NOT qualifier */
    
    if (this.match(TOK_NOT))
      return this.parseNotQualifier();
    
    /* raw SQL qualifier */

    if (this.match(TOK_SQL))
      return this.parseRawSQLQualifier();
    
    /* special constant qualifiers */
    
    if (this.consumeIfMatch(TOK_STAR_TRUE))
      return EOBooleanQualifier.trueQualifier;
    
    if (this.consumeIfMatch(TOK_STAR_FALSE))
      return EOBooleanQualifier.falseQualifier;
    
    return this.parseKeyBasedQualifier();
  }
  
  protected String nextNonNullStringArgument(String _pat) {
    if (this.currentArgument >= this.args.length) {
      this.addError("more format patterns than arguments");
      return null;
    }
    Object arg = this.args[this.currentArgument];
    this.currentArgument++; /* consume */
    
    /* process format spec */
    
    switch (_pat.charAt(1)) {
      case 'K': case 's': case 'i': case 'd': case 'f': case '@':
        return arg.toString();

      case '%':
        this.addError("not yet supported: %%");
        return null;
      default:
        this.addError("unknown string format specification: " + _pat);
        return null;        
    }
  }
  
  protected EOQualifier parseKeyBasedQualifier() {
    // TODO: we need to improve and consolidate the argument handling, but hey,
    //       it works ;-)
    //       Maybe we want to move it to the identifier parsing?
    
    /* some identifier or keyword */
    
    String id = this.parseIdentifier(false /* break on all break chars */);
    if (id == null) {
      /* found no ID, error */
      return null;
    }

    /* process formats */
    if (id.length() > 1 && id.startsWith("%")) {
      // the id itself is a format, eg: "%@ LIKE 'Hello*'"
      if ((id = this.nextNonNullStringArgument(id)) == null)
        return null;
    }
    

    if (!this.skipSpaces()) {
      /* ok, it was just the ID. We treat this as a boolean kvqualifier,
       * eg: "isArchived"
       */
      return new EOKeyValueQualifier(id, Boolean.TRUE);
    }
    
    /* check whether the qualifier is closed, that is, whether we are bool */
    
    if (this.match(TOK_AND) || this.match(TOK_OR) || this.match(')')) {
      /* ok, it was just the ID. We treat this as a boolean kvqualifier,
       * eg: "isArchived AND code > 10"
       *     "(code > 10 AND isArchived) AND is New"
       */
      return new EOKeyValueQualifier(id, Boolean.TRUE);
    }
    
    /* OK, now we check for operations */
    
    String operation = this.parseOperation();
    if (operation == null) {
      /* ok, it was just the ID and some spaces, no operation. We treat this as
       * a boolean kvqualifier, eg: "isArchived  "
       */
      return new EOKeyValueQualifier(id, Boolean.TRUE);
    }

    /* process formats */
    if (operation.length() > 1 && operation.startsWith("%")) {
      // the operation is a pattern, eg: "value %@ 5", "<" 
      if ((operation = this.nextNonNullStringArgument(operation)) == null)
        return null;
    }
    
    /* check for IS NULL and IS NOT NULL */
    
    if ("IS".equals(operation)) {
      int saveIdx = this.idx;
      
      if (this.skipSpaces()) {
        if (this.consumeIfMatch(TOK_NOT)) {
          if (this.skipSpaces()) {
            if (this.consumeIfMatch(TOK_NULL))
              return new EONotQualifier(new EOKeyValueQualifier(id, null));
          }
        }
        else if (this.consumeIfMatch(TOK_NULL))
          return new EOKeyValueQualifier(id, null);
      }
      
      /* did not match, restore pointer */
      this.idx = saveIdx;
    }
    
    /* and finally the right hand side (either id or value) */

    if (!this.skipSpaces()) {
      this.addError("expected value/id after identifier and operation (op=" +
                    operation + ", id=" + id + ")");
      return null; /* EOF */
    }
    
    /* process variables ($name) */

    if (this.match('$')) {
      this.idx++; // consume $
      
      String var = this.parseIdentifier(false /* break on all break chars */);
      if (var == null) {
        this.addError("expected variable identifier after '$'?!");
        return null; /* EOF */      
      }
      
      return new EOKeyValueQualifier
        (id, operation, new EOQualifierVariable(var));
    }
    
    /* process value arguments */
    
    if (this.match('%')) {
      /* Note: we do not support %%, and we do not support arbitrary
       *       strings, like "col_%K" or something like this
       */
      this.idx++; // consume %
      
      char fspec = this.string[this.idx];
      this.idx++; // consume format spec char
      
      /* retrieve argument */
      
      if (this.currentArgument >= this.args.length) {
        this.addError("more format patterns than arguments");
        return null;
      }
      Object arg = this.args[this.currentArgument];
      this.currentArgument++; /* consume */
      
      /* convert argument */
      
      switch (fspec) {
        case '@':
          return new EOKeyValueQualifier(id, operation, arg);
        
        case 's':
          if (arg != null && !(arg instanceof String))
            arg = arg.toString();
          return new EOKeyValueQualifier(id, operation, arg);
        
        case 'd': case 'i':
          if (arg != null && !(arg instanceof Integer))
            arg = Integer.valueOf(arg.toString());
          return new EOKeyValueQualifier(id, operation, arg);
        
        case 'f':
          if (arg != null && !(arg instanceof Double))
            arg = Double.valueOf(arg.toString());
          return new EOKeyValueQualifier(id, operation, arg);
        
        case 'K':
          if (arg != null && !(arg instanceof String))
            arg = arg.toString();
          return new EOKeyComparisonQualifier(id, operation, (String)arg);
        
        case '%':
          this.addError("not yet supported: %%");
          return null;
        default:
          this.addError("unknown format specification: %" + fspec);
          return null;
      }
    }
    
    /* process constants */

    if (this.matchConstant()) {
      /* EOKeyValueQualifier */
      Object v = this.parseConstant();
      if (log.isDebugEnabled())
        log.debug("parsed constant: " + v);
      
      return new EOKeyValueQualifier(id, operation, v);
    }
    
    /* process identifiers */
    
    String rhs = this.parseIdentifier(false /* break on all break chars */);
    if (rhs == null) {
      this.addError("expected value/id after identifier and operation?!");
      return null; /* EOF */      
    }
    
    if (log.isDebugEnabled())
      log.debug("not a constant: " + rhs);
    
    return new EOKeyComparisonQualifier(id, operation, rhs);
  }
  
  protected EOQualifier parseNotQualifier() {
    if (!this.consumeIfMatch(TOK_NOT))
      return null; /* not a NOT qualifier */
    
    if (!this.skipSpaces()) {
      this.addError("missing qualifier after NOT!");
      return null; /* ERROR */
    }
    
    EOQualifier q = this.parseOneQualifier();
    if (q == null) return null; /* parsing failed */
    
    return new EONotQualifier(q);    
  }
  
  protected EOQualifier parseCompoundQualifierInParenthesis() {
    if (!this.consumeIfMatch('('))
      return null; /* not in parenthesis */
    
    if (!this.skipSpaces()) {
      this.addError("missing closing parenthesis!");
      return null; /* ERROR */
    }
    
    /* parse qualifier */
    EOQualifier q = this.parseCompoundQualifier();
    if (q == null) return null; /* parsing failed */
    
    this.skipSpaces();
    if (!this.consumeIfMatch(')')) /* be tolerant and keep the qualifier */
      this.addError("missing closing parenthesis!");
    
    return q;
  }
  
  protected EOQualifier buildCompoundQualifier
    (final String _operation, final List<EOQualifier> _qualifiers)
  {
    if (_qualifiers        == null) return null;
    if (_qualifiers.size() == 0)    return null;
    
    if (_qualifiers.size() == 1)
      return _qualifiers.get(0);
    
    if (STOK_AND.equals(_operation))
      return new EOAndQualifier(_qualifiers);
    if (STOK_OR.equals(_operation))
      return new EOOrQualifier(_qualifiers);
    
    /* Note: we could make this extensible */
    this.addError("unknown compound operator: " + _operation);
    return null;
  }
  
  protected EOQualifier parseCompoundQualifier() {
    List<EOQualifier> qualifiers = null;
    String lastCompoundOperator = null;
    
    while (this.idx < this.string.length) {
      EOQualifier q = this.parseOneQualifier();
      if (q == null) return null; /* parse error */
      
      if (qualifiers == null)
        qualifiers = new ArrayList<EOQualifier>(4);
      qualifiers.add(q);
      
      if (!this.skipSpaces()) break; /* expected EOF */
      
      /* check whether a closing paren is up front */
      
      if (this.match(')')) /* stop processing */
        break;
      
      /* now check for AND or OR */
      String compoundOperator =
        this.parseIdentifier(false /* break on all break chars */);
      if (compoundOperator == null) {
        this.addError("could not parse compound operator, index: " + this.idx);
        break;
      }
      
      /* process formats */
      if (compoundOperator.length() > 1 && compoundOperator.startsWith("%")) {
        compoundOperator = this.nextNonNullStringArgument(compoundOperator);
        if (compoundOperator == null)
          return null;
      }
      
      if (!this.skipSpaces()) {
        this.addError("expected another qualifier after compound operator " +
                      "(op='" + compoundOperator + "')");
        break;
      }
      
      if (lastCompoundOperator != null && 
          !compoundOperator.equals(lastCompoundOperator)) {
        /* operation changed, for example:
         *   a AND b AND c OR d OR e AND f
         * will be parsed as:
         *   ((a AND b AND c) OR d OR e) AND f
         */
        
        q = this.buildCompoundQualifier(lastCompoundOperator, qualifiers);
        qualifiers = new ArrayList<EOQualifier>(4);
        qualifiers.add(q);
      }
      lastCompoundOperator = compoundOperator;
    }
    
    return this.buildCompoundQualifier(lastCompoundOperator, qualifiers);
  }
  
  /**
   * parse something like this:
   *   <pre>SQL[select abc WHERE date_id = $dateId]</pre>
   * into:
   *   <pre>"select abc WHERE date_id ="</pre>
   * <p>
   * Note that the SQL strings are converted into EORawSQLValue objects so
   * that they do not get quoted as SQL strings during SQL generation.
   */
  protected EOQualifier parseRawSQLQualifier() {
    if (!this.consumeIfMatch(TOK_SQL))
      return null; /* does not start with SQL[ */
    
    List<Object> parts = new ArrayList<Object>(16);
    StringBuilder sql = new StringBuilder();
    
    for (int i = this.idx; i < this.string.length; i++) {
      if (this.string[i] == ']') {
        this.idx = i + 1; /* consume ] */
        break;
      }
      else if (this.string[i] == '$') {
        if (sql.length() != 0) {
          parts.add(new EORawSQLValue(sql.toString()));
          sql.setLength(0); /* reset char buffer */
        }
        
        this.idx = i + 1; /* skip '$' */
        String varName = this.parseIdentifier(false /* break on everything */);
        i = this.idx;
        i--; /* will get bumped by next loop iteration */
        
        parts.add(new EOQualifierVariable(varName));
      }
      else {
        /* regular char */
        sql.append(this.string[i]);
      }
    }
    if (sql.length() != 0)
      parts.add(new EORawSQLValue(sql.toString()));
    
    return new EOSQLQualifier(parts);
  }
  
  /**
   * Parse an identifier. Identifiers do not start with numbers or spaces, they
   * are at least on char long.
   * 
   * @param _onlyBreakOnSpace - read ID until a space is encountered
   * @return String containing the ID or null if not could be found
   */
  protected String parseIdentifier(boolean _onlyBreakOnSpace) {
    if (this.idx >= this.string.length)
      return null; /* EOF */
    
    if (_isDigit(this.string[this.idx]))
      return null; /* identifiers never start with a digit */
    if (_isSpace(this.string[this.idx]))
      return null; /* nor with a space */
    
    /* we are extremely tolerant here, everything is allowed as long as it
     * starts w/o a digit. processing ends at the next space.
     */
    int i;
    for (i = this.idx + 1; i < this.string.length; i++) {
      //System.err.println("CHECK: " + i + ": '" + this.string[i] + "'");
      
      if (_onlyBreakOnSpace) {
        if (_isSpace(this.string[i]))
          break;
      }
      else if (_isIdBreakChar(this.string[i])) /* Note: this includes spaces */
        break;
      
      // System.err.println("  no break");
    }
    
    int len = i - this.idx;
    /* Note: len==0 cannot happen, catched above */
    
    String id = new String(this.string, this.idx, len);
    
    this.idx += len; /* consume */
    return id;
  }
  
  /**
   * Parses those qualifier operations:<pre>
   *   =   =
   *   &lt;   &lt;
   *   &gt;   &gt;
   *   =&gt;  =&gt;
   *   =&lt;  =&lt;
   *   != !=
   *   &lt;=  =&lt;
   *   &lt&gt;  &lt&gt;
   *   &gt;=  =&gt;
   *   &gt;&lt;  &lt&gt;</pre>
   * If none matches, parseIdentifier is called.
   * 
   * <p>
   * @return the operation or identifier
   */
  protected String parseOperation() {
    if (this.idx + 2 /* allow for some space */ >= this.string.length)
      return null; /* EOF */
    
    if (this.string[this.idx] == '=') {
      this.idx++;
      if (this.string[this.idx] == '>') {
        this.idx++;
        return "=>";
      }
      if (this.string[this.idx] == '<') {
        this.idx++;
        return "=<";
      }
      return "=";
    }
    
    if (this.string[this.idx] == '!' && this.string[this.idx + 1] == '=') {
      this.idx += 2;
      return "!=";
    }
    
    if (this.string[this.idx] == '<') {
      this.idx++;
      if (this.string[this.idx] == '=') {
        this.idx++;
        return "=<";
      }
      if (this.string[this.idx] == '>') {
        this.idx++;
        return "<>";
      }
      return "<";
    }
    
    if (this.string[this.idx] == '>') {
      this.idx++;
      if (this.string[this.idx] == '=') {
        this.idx++;
        return "=>";
      }
      if (this.string[this.idx] == '<') {
        this.idx++;
        return "<>";
      }
      return ">";
    }
    
    // TODO: better an own parser? hm, yes.
    // the following stuff parses things like hasPrefix:
    
    return this.parseIdentifier(true /* break only on space */);
  }
  
  protected boolean matchConstant() {
    if (this.idx >= this.string.length)
      return false;
    
    if (this.string[this.idx] == '(') {
      if (!this.skipSpaces()) return false;
      return true; // no further checks for: ID ')'
    }
    
    if (_isDigit(this.string[this.idx]))
      return true;
    if (this.string[this.idx] == '\'')
      return true;
    if (this.string[this.idx] == '"') // TODO: would be an ID in SQL syntax
      return true;
    
    if (this.match(TOK_TRUE))  return true;
    if (this.match(TOK_FALSE)) return true;
    if (this.match(TOK_NULL))  return true;
    if (this.match(TOK_null))  return true;
    if (this.match(TOK_YES))   return true;
    if (this.match(TOK_NO))    return true;
    
    return false;
  }
  
  protected boolean matchCast() {
    if (this.idx >= this.string.length)
      return false;
    
    if (this.string[this.idx] == '(') {
      if (!this.skipSpaces()) return false;
      return true; // no further checks for: ID ')'
    }
    
    return false;
  }
  
  protected String parseCast() {
    if (this.string[this.idx] != '(')
      return null;
    
    if (!this.skipSpaces()) {
      this.addError("expected class cast identifier after parenthesis!");
      return null;
    }
    
    String castClass = this.parseIdentifier(false /* on all */);
    if (castClass == null) {
      this.addError("expected class cast identifier after parenthesis!");
      return null;        
    }
    if (!this.skipSpaces())
      this.addError("expected closing parenthesis after class cast!");
    else if (!this.consumeIfMatch(')'))
      this.addError("expected closing parenthesis after class cast!");
    
    return castClass;
  }
  
  /**
   * This parses:
   * <ul>
   *   <li>single quoted strings
   *   <li>double quoted strings
   *   <li>numbers
   *   <li>true/false, YES/NO
   *   <li>null/NULL
   * </ul>
   * The constant can be prefixed with a cast, eg:<pre>
   *   (int)"383"</pre>
   * But the casts are not resolved yet ...
   * 
   * <p>
   * @return the value of the constant
   */
  protected Object parseConstant() {
    String castClass = this.parseCast();
    Object v = null;
    
    if (this.string[this.idx] == '\'')
      v = this.parseQuotedString();
    else if (this.string[this.idx] == '"') // TODO: could be a SQL id
      v = this.parseQuotedString();
    else if (_isDigit(this.string[this.idx]))
      v = this.parseNumber();
    else if (this.consumeIfMatch(TOK_TRUE) || this.consumeIfMatch(TOK_YES))
      v = Boolean.TRUE;
    else if (this.consumeIfMatch(TOK_FALSE) || this.consumeIfMatch(TOK_NO))
      v = Boolean.FALSE;
    else if (this.consumeIfMatch(TOK_NULL))
      return null; // do not apply casts for null
    else if (this.consumeIfMatch(TOK_null))
      return null; // do not apply casts for null
    else
      return null; // hm, can't distinguish between this and null => match..
    
    if (castClass != null) {
      // TODO: handle casts, eg (Date)'2006-06-10'
      log.error("not handling cast to '" + castClass + "', value: "  + v);
    }
    return v;
  }
  
  protected String parseQuotedString() {
    char quoteChar = this.string[this.idx];
    
    /* a quoted string */
    int pos      = this.idx + 1;  /* skip quote */
    int ilen     = 0;
    int startPos = pos;
    boolean containsEscaped = false;
    
    /* loop until closing quote */
    while ((this.string[pos] != quoteChar) && (pos < this.string.length)) {
      if (this.string[pos] == '\\') {
        containsEscaped = true;
        pos++; /* skip following char */
        if (pos == this.string.length) {
          this.addError("escape in quoted string not finished!");
          return null;
        }
      }
      pos++;
      ilen++;
    }
    
    if (pos == this.string.length) { /* syntax error, quote not closed */
      this.idx = pos;
      this.addError("quoted string not closed (expected '" + quoteChar + "')");
      return null;
    }
    
    pos++;          /* skip closing quote */
    this.idx = pos; /* store pointer */
    pos = 0;
    
    if (ilen == 0) /* empty string */
      return "";
    
    if (containsEscaped) {
      // TODO: implement unescaping in quoted strings
      System.err.println("ERROR: unescaping not implemented!");
    }    
    return new String(this.string, startPos, ilen);
  }
  
  protected Number parseNumber() {
    if (this.idx >= this.string.length)
      return null; /* EOF */
    
    if (!_isDigit(this.string[this.idx]) || this.string[this.idx] == '-')
      return null; /* numbers must start with a digit */
    if (_isSpace(this.string[this.idx]))
      return null; /* but not with a space */
    
    /* we are extremely tolerant here, almost everything is allowed ... */
    int i;
    for (i = this.idx + 1; i < this.string.length; i++) {
      if (_isIdBreakChar(this.string[i]) || this.string[i] == ')')
        break;
    }
    
    int len = i - this.idx;
    /* Note: len==0 cannot happen, catched above */
    
    String numstr = new String(this.string, this.idx, len);
    this.idx += len; /* consume */

    try {
      if (numstr.indexOf('.') == -1)
        return Integer.parseInt(numstr);
      
      return Double.parseDouble(numstr);
    }
    catch (NumberFormatException e) {
      this.addError("failed to parse number: '" + numstr + "'");
      return null;
    }
  }
  
  protected void addError(String _reason) {
    // TODO: generate some exception
    log.error(_reason);
  }
  
  /* core parsing */
  
  protected static boolean _isDigit(char _c) {
    switch (_c) {
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        return true;
      default:
        return false;
    }
  }
  protected static boolean _isSpace(char _c) {
    switch (_c) {
      case ' ': case '\t': case '\n': case '\r':
        return true;
      default:
        return false;
    }
  }
  protected static boolean _isIdBreakChar(char _c) {
    switch (_c) {
      case ' ': case '\t': case '\n': case '\r':
      case '<': case '>': case '=':
      case '*': case '/': case '+': case '-':
      case '(': case ')': case ']':
      case '!': /* eg NSFileName!="index.html" */
        return true;
      default:
        return false;
    }
  }

  protected boolean skipSpaces() {
    while (this.idx < this.string.length) {
      if (!_isSpace(this.string[this.idx]))
        return true;
      
      this.idx++;
    }
    return this.idx < this.string.length;
  }
  
  protected final int la(int _i) {
    _i += this.idx;
    return (_i < this.string.length) ? this.string[_i] : -1;
  }
  
  protected final boolean match(char[] _tok) {
    if (this.idx + _tok.length > this.string.length)
      return false; /* not enough space */
    
    for (int i = 0; i < _tok.length; i++) {
      if (_tok[i] != this.string[this.idx + i])
        return false;
    }
    return true;
  }
  
  /**
   * Returns true if the current parsing position matches the char. This does
   * NOT consume the char (use consumeIfMatch for that).
   * Example:<pre>
   *   if (this.match(')')) return ...</pre>
   * 
   * @param _c
   * @return
   */
  protected final boolean match(final char _c) {
    if (this.idx >= this.string.length)
      return false; /* not enough space */
    
    return this.string[this.idx] == _c;
  }
  
  protected final boolean consumeIfMatch(char[] _tok) {
    if (!this.match(_tok))
      return false;
    
    this.idx += _tok.length;
    return true;
  }

  protected final boolean consumeIfMatch(char _c) {
    if (!this.match(_c))
      return false;
    
    this.idx++;
    return true;
  }
  
  /* tokens */
  
  protected static final String STOK_AND = "AND";
  protected static final String STOK_OR  = "OR";
  
  protected static final char[] TOK_NOT   = { 'N', 'O', 'T' };
  protected static final char[] TOK_NULL  = { 'N', 'U', 'L', 'L' };
  protected static final char[] TOK_null  = { 'n', 'u', 'l', 'l' };
  protected static final char[] TOK_TRUE  = { 't', 'r', 'u', 'e' };
  protected static final char[] TOK_FALSE = { 'f', 'a', 'l', 's', 'e' };
  protected static final char[] TOK_YES   = { 'Y', 'E', 'S' };
  protected static final char[] TOK_NO    = { 'N', 'O' };
  protected static final char[] TOK_SQL   = { 'S', 'Q', 'L', '[' };
  protected static final char[] TOK_AND   = { 'A', 'N', 'D' };
  protected static final char[] TOK_OR    = { 'O', 'R' };
  
  protected static final char[] TOK_STAR_TRUE = {
    '*', 't', 'r', 'u', 'e', '*'
  };
  protected static final char[] TOK_STAR_FALSE = {
    '*', 'f', 'a', 'l', 's', 'e', '*'
  };
}
