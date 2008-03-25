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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * ExtendedBufferedReader
 *
 * A special reader decorater which supports more
 * sophisticated access to the underlying reader object.
 * 
 * In particular the reader supports a look-ahead option,
 * which allows you to see the next char returned by
 * next().
 * Furthermore the skip-method supports skipping until
 * (but excluding) a given char. Similar functionality
 * is supported by the reader as well.
 * 
 */
public class ExtendedBufferedReader extends BufferedReader  {

  
  /** the end of stream symbol */
  public static final int END_OF_STREAM = -1;
  /** undefined state for the lookahead char */
  public static final int UNDEFINED = -2;
  
  /** the lookahead chars */
  private int lookaheadChar = UNDEFINED;
  /** the last char returned */
  private int lastChar = UNDEFINED;
  /** the line counter */
  private int lineCounter = 0;
  private StringBuilder line = new StringBuilder(128);
  
  /**
   * Created extended buffered reader using default buffer-size
   *
   */
  public ExtendedBufferedReader(Reader r) {
    super(r);
    /* note uh: do not fetch the first char here,
     *          because this might block the method!
     */
  }
    
  /**
   * Create extended buffered reader using the given buffer-size
   */
  public ExtendedBufferedReader(Reader r, int bufSize) {
    super(r, bufSize);
    /* note uh: do not fetch the first char here,
     *          because this might block the method!
     */
  }
  
  /**
   * Reads the next char from the input stream.
   * @return the next char or END_OF_STREAM if end of stream has been reached.
   */
  public int read() throws IOException {
    // initalize the lookahead
    if (this.lookaheadChar == UNDEFINED) {
      this.lookaheadChar = super.read();
    }
    this.lastChar = this.lookaheadChar;
    if (super.ready()) {
      this.lookaheadChar = super.read();
    } else {
      this.lookaheadChar = UNDEFINED;
    }
    if (this.lastChar == '\n') {
      this.lineCounter++;
    } 
    return this.lastChar;
  }
  
  /**
   * Returns the last read character again.
   * 
   * @return the last read char or UNDEFINED
   */
  public int readAgain() {
    return this.lastChar;  
  }
  
  /**
   * Non-blocking reading of len chars into buffer buf starting
   * at bufferposition off.
   * 
   * performs an iteratative read on the underlying stream
   * as long as the following conditions hold:
   *   - less than len chars have been read
   *   - end of stream has not been reached
   *   - next read is not blocking
   * 
   * @return nof chars actually read or END_OF_STREAM
   */
  public int read(char[] buf, int off, int len) throws IOException {
    // do not claim if len == 0
    if (len == 0) {
      return 0;
    } 
    
    // init lookahead, but do not block !!
    if (this.lookaheadChar == UNDEFINED) {
        if (ready()) {
          this.lookaheadChar = super.read();
        } else {
          return -1;
        }
    }
    // 'first read of underlying stream'
    if (this.lookaheadChar == -1) {
      return -1;
    }
    // continue until the lookaheadChar would block
    int cOff = off;
    while (len > 0 && ready()) {
      if (this.lookaheadChar == -1) {
        // eof stream reached, do not continue
        return cOff - off;
      }
      
      buf[cOff++] = (char) this.lookaheadChar;
      if (this.lookaheadChar == '\n') {
        this.lineCounter++;
      } 
      this.lastChar = this.lookaheadChar;
      this.lookaheadChar = super.read();
      len--;
    }
    return cOff - off;
  }
 
 /**
  * Reads all characters up to (but not including) the given character.
  * 
  * @param c the character to read up to
  * @return the string up to the character <code>c</code>
  * @throws IOException
  */
 public String readUntil(char c) throws IOException {
   if (this.lookaheadChar == UNDEFINED) {
     this.lookaheadChar = super.read();
   }
   this.line.setLength(0); // reuse
   while (this.lookaheadChar != c && this.lookaheadChar != END_OF_STREAM) {
     this.line.append((char) this.lookaheadChar);
     if (this.lookaheadChar == '\n') {
       this.lineCounter++;
     } 
     this.lastChar = this.lookaheadChar;
     this.lookaheadChar = super.read();
   }
   return this.line.toString();    
 }
 
 /**
  * @return A String containing the contents of the line, not 
  *         including any line-termination characters, or null 
  *         if the end of the stream has been reached
  */
  public String readLine() throws IOException {
    
    if (this.lookaheadChar == UNDEFINED) {
      this.lookaheadChar = super.read(); 
    }
    
    this.line.setLength(0); //reuse
    
    // return null if end of stream has been reached
    if (this.lookaheadChar == END_OF_STREAM) {
      return null;
    }
    // do we have a line termination already
    char laChar = (char) this.lookaheadChar;
    if (laChar == '\n' || laChar == '\r') {
      this.lastChar = this.lookaheadChar;
      this.lookaheadChar = super.read();
      // ignore '\r\n' as well
      if ((char) this.lookaheadChar == '\n') {
        this.lastChar = this.lookaheadChar;
        this.lookaheadChar = super.read();
      }
      this.lineCounter++;
      return this.line.toString();
    }
    
    // create the rest-of-line return and update the lookahead
    this.line.append(laChar);
    String restOfLine = super.readLine(); // TODO involves copying
    this.lastChar = this.lookaheadChar;
    this.lookaheadChar = super.read();
    if (restOfLine != null) {
      this.line.append(restOfLine);
    }
    this.lineCounter++;
    return this.line.toString();
  }
  
  /**
   * Skips char in the stream
   * 
   * ATTENTION: invalidates the line-counter !!!!!
   * 
   * @return nof skiped chars
   */
  public long skip(long n) throws IllegalArgumentException, IOException  {
    
    if (this.lookaheadChar == UNDEFINED) {
      this.lookaheadChar = super.read();   
    }
    
    // illegal argument
    if (n < 0) {
      throw new IllegalArgumentException("negative argument not supported");  
    }
    
    // no skipping
    if (n == 0 || this.lookaheadChar == END_OF_STREAM) {
      return 0;
    } 
    
    // skip and reread the lookahead-char
    long skiped = 0;
    if (n > 1) {
      skiped = super.skip(n - 1);
    }
    this.lookaheadChar = super.read();
    // fixme uh: we should check the skiped sequence for line-terminations...
    this.lineCounter = Integer.MIN_VALUE;
    return skiped + 1;
  }
  
  /**
   * Skips all chars in the input until (but excluding) the given char
   * 
   * @param c
   * @return
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public long skipUntil(char c) throws IllegalArgumentException, IOException {
    if (this.lookaheadChar == UNDEFINED) {
      this.lookaheadChar = super.read();   
    }
    long counter = 0;
    while (this.lookaheadChar != c && this.lookaheadChar != END_OF_STREAM) {
      if (this.lookaheadChar == '\n') {
        this.lineCounter++;
      } 
      this.lookaheadChar = super.read();
      counter++;
    }
    return counter;
  }
  
  /**
   * Returns the next char in the stream without consuming it.
   * 
   * Remember the next char read by read(..) will always be
   * identical to lookAhead().
   * 
   * @return the next char (without consuming it) or END_OF_STREAM
   */
  public int lookAhead() throws IOException {
    if (this.lookaheadChar == UNDEFINED) {
      this.lookaheadChar = super.read();
    }
    return this.lookaheadChar;
  }
  
  
  /**
   * Returns the nof line read
   * ATTENTION: the skip-method does invalidate the line-number counter
   * 
   * @return the current-line-number (or -1)
   */ 
  public int getLineNumber() {
    if (this.lineCounter > -1) {
      return this.lineCounter;
    }
    return -1;
  }
  public boolean markSupported() {
    /* note uh: marking is not supported, cause we cannot
     *          see into the future...
     */
    return false;
  }
  
}
