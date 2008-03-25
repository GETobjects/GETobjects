/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.getobjects.foundation.csv;

import java.io.Serializable;

/**
 * CSVStrategy
 * 
 * Represents the strategy for a CSV.
 */
public class CSVStrategy implements Cloneable, Serializable {
  private static final long serialVersionUID = 1L;

    private char delimiter;
    private char encapsulator;
    private char commentStart;
    private boolean ignoreLeadingWhitespaces;
    private boolean interpretUnicodeEscapes;
    private boolean ignoreEmptyLines;

    public static char COMMENTS_DISABLED       = (char) 0;

    public static final CSVStrategy DEFAULT_STRATEGY =
      new CSVStrategy(',', '"', COMMENTS_DISABLED, true,  false, true);
    public static final CSVStrategy EXCEL_STRATEGY   =
      new CSVStrategy(',', '"', COMMENTS_DISABLED, false, false, false);
    public static final CSVStrategy TDF_STRATEGY     =
      new CSVStrategy('	', '"', COMMENTS_DISABLED, true,  false, true);


    public CSVStrategy(char _delimiter, char _encapsulator, char _commentStart) {
        this(_delimiter, _encapsulator, _commentStart, true, false, true);
    }
  
    /**
     * Customized CSV strategy setter.
     * 
     * @param delimiter a Char used for value separation
     * @param encapsulator a Char used as value encapsulation marker
     * @param commentStart a Char used for comment identification
     * @param ignoreLeadingWhitespace TRUE when leading whitespaces should be
     *                                ignored
     * @param interpretUnicodeEscapes TRUE when unicode escapes should be 
     *                                interpreted
     * @param ignoreEmptyLines TRUE when the parser should skip emtpy lines
     */
    public CSVStrategy(
        char _delimiter, 
        char _encapsulator, 
        char _commentStart, 
        boolean _ignoreLeadingWhitespace, 
        boolean _interpretUnicodeEscapes,
        boolean _ignoreEmptyLines) 
    {
        setDelimiter(_delimiter);
        setEncapsulator(_encapsulator);
        setCommentStart(_commentStart);
        setIgnoreLeadingWhitespaces(_ignoreLeadingWhitespace);
        setUnicodeEscapeInterpretation(_interpretUnicodeEscapes);
        setIgnoreEmptyLines(_ignoreEmptyLines);
    }

    public void setDelimiter(char _delimiter) { this.delimiter = _delimiter; }
    public char getDelimiter() { return this.delimiter; }

    public void setEncapsulator(char _encapsulator) { this.encapsulator = _encapsulator; }
    public char getEncapsulator() { return this.encapsulator; }

    public void setCommentStart(char _commentStart) { this.commentStart = _commentStart; }
    public char getCommentStart() { return this.commentStart; }
    public boolean isCommentingDisabled() {
      return this.commentStart == COMMENTS_DISABLED;
    }

    public void setIgnoreLeadingWhitespaces(boolean _ignoreLeadingWhitespace) {
      this.ignoreLeadingWhitespaces = _ignoreLeadingWhitespace;
      }
    public boolean getIgnoreLeadingWhitespaces() { return this.ignoreLeadingWhitespaces; }

    public void setUnicodeEscapeInterpretation(boolean _interpretUnicodeEscapes) {
      this.interpretUnicodeEscapes = _interpretUnicodeEscapes;
    }
    public boolean getUnicodeEscapeInterpretation() {
      return this.interpretUnicodeEscapes;
    }

    public void setIgnoreEmptyLines(boolean _ignoreEmptyLines) {
      this.ignoreEmptyLines = _ignoreEmptyLines;
    }
    public boolean getIgnoreEmptyLines() {
      return this.ignoreEmptyLines;
    }

    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);  // impossible
      }
    }
}
