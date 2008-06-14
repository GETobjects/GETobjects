/*
 * Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of Go.
 * 
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UData
 * <p>
 * Byte array related utility functions.
 */
public class UData extends NSObject {
  protected static Log log = LogFactory.getLog("UData");

  private UData() { } /* do not allow construction */
  
  /**
   * This method reads the whole contents of the given stream into a byte array.
   * After the content is read, the stream is closed.
   * 
   * @param _in - an InputStream to read the content from
   * @return a byte[] array containing the stream contents or null on error
   */
  public static byte[] loadContentFromStream(InputStream _in) {
    if (_in == null) return null;
    
    // TBD: is this necessary? Probably not, we already copy with a bufer
    BufferedInputStream in;
    in = (_in instanceof BufferedInputStream)
      ? (BufferedInputStream)_in
      : new BufferedInputStream(_in);
    
    // TODO: there must be a smarter way to do this ;-)
    byte[] results = new byte[0];
    try {
      int    didRead;
      byte[] buf = new byte[4096];
      
      while ((didRead = in.read(buf)) > 0) {
        byte[] nre = new byte[results.length + didRead];
        System.arraycopy(results, 0 /* start of source */,
                         nre,     0 /* start in dest   */,
                         results.length);
        System.arraycopy(buf, 0              /* start of source */,
                         nre, results.length /* start in dest   */,
                         didRead);
        
        results = nre;
      }
    }
    catch (IOException e) {
      log.warn("failed to read data from stream: " + _in, e);
      return null;
    }
    finally {
      try {
        _in.close();
      }
      catch (IOException e) {
        log.warn("could not close input stream", e);
      }
    }
    
    return results;
  }

  /**
   * This method reads the whole contents of the given object into a byte array.
   * After the content is read, the stream is closed.
   * <p>
   * The object can be:
   * <ul>
   *   <li>an InputStream
   *   <li>a File object
   *   <li>a String treated as a path name
   *   <li>a URL object
   * </ul>
   * 
   * @param _o - an InputStream/File/URL/path to read the content from
   * @return a byte[] array containing the stream contents or null on error
   */
  public static byte[] loadContentFromSource(Object _o) {
    if (_o == null)
      return null;
    
    if (_o instanceof InputStream)
      return loadContentFromStream((InputStream)_o);
    
    if (_o instanceof File) {
      try {
        /* Note: stream is closed by load function */
        FileInputStream is = new FileInputStream((File)_o);
        return loadContentFromStream(is);
      }
      catch (FileNotFoundException e) {
        log.info("could not open file: " + _o);
        return null;
      }
    }
    
    if (_o instanceof String) {
      try {
        /* Note: stream is closed by load function */
        FileInputStream is = new FileInputStream((String)_o);
        return loadContentFromStream(is);
      }
      catch (FileNotFoundException e) {
        log.info("could not open file: " + _o);
        return null;
      }
    }
    
    if (_o instanceof URL) {
      try {
        /* Note: stream is closed by load function */
        return loadContentFromStream(((URL)_o).openStream());
      }
      catch (IOException e) {
        log.info("could not open URL: " + _o, e);
        return null;
      }
    }
    
    log.error("don't know how to load data from object: " + _o.getClass());
    return null;
  }
  
  
  /* writing files */
  
  public static Exception writeToStream(byte[] _data, OutputStream _out) {
    if (_data == null)
      return new NSException("got no data to write ...");
    if (_out == null)
      return new NSException("got no stream to write data to ...");
    
    try {
      _out.write(_data, 0, _data.length);
      _out.flush();
    }
    catch (IOException e) {
      return e;
    }
    
    return null /* everything went fine */;
  }

  public static Exception writeToFile
    (byte[] _data, File _file, boolean _atomically)
  {
    if (_file == null)
      return new NSException("got no File to write data to ...");
    if (_file.isDirectory())
      return new NSException("target File is a directory ...");
    
    OutputStream os;
    File tmpFile = null;
    
    /* create tmpfile and open output stream */
    
    if (_atomically) {
      try {
        // TBD: this gives Prefix string too short
        tmpFile = File.createTempFile(".", ".atomicwrite", _file.getParentFile());
      }
      catch (IOException e) {
        return e;
      }
      
      try {
        os = new FileOutputStream(_file);
      }
      catch (FileNotFoundException e) {
        tmpFile.delete();
        return e;
      }
    }
    else {
      try {
        os = new FileOutputStream(_file);
      }
      catch (FileNotFoundException e) {
        return e;
      }
    }
    
    /* write data to stream */
    
    Exception error = writeToStream(_data, os);
    
    try {
      if (os != null) os.close();
    }
    catch (IOException e) {
      log.warn("could not close output stream: " + os, e);
      os = null;
    }
    
    if (error != null) {
      if (tmpFile != null) tmpFile.delete();
      return error;
    }
    
    if (tmpFile == null) {
      /* OK, we did a non atomic write and everything went fine. Great! */
      return null;
    }
    
    /* move file on atomic writes, depending on the host OS this is not really
     * atomic
     */
      
    /* first attempt to move the file "over" the old one */

    if (tmpFile.renameTo(_file))
      return null; /* rename went fine */

    /* If this didn't work out, check whether the target exists and
     * delete it prior moving the tmpfile over it */
    
    if (_file.exists()) {
      if (!_file.delete()) {
        // TBD: should we attempt to copy in this situation?
        tmpFile.delete();
        return new NSException("Could not delete target file");
      }

      if (tmpFile.renameTo(_file))
        return null; /* rename went fine */

      // TBD: should we attempt to copy in this situation?
      return new NSException("Could not move temporary data file!");
    }
    
    return new NSException("Could not move temporary data file!");
  }
  
  public static Exception writeToFile
    (byte[] _data, String _path, boolean _atomically)
  {
    if (_data == null)
      return new NSException("got no data to write ...");
    if (_path == null)
      return new NSException("got no path to write data to ...");
    
    return writeToFile(_data, new File(_path), _atomically);
  }

  
  /* hashing */
  
  /**
   * Calculates an MD5 hash over the given byte array and returns it as a
   * String containing hex digits (eg 0FAADE...).
   * 
   * @param _p - a byte array
   * @return a String containing the MD5 hash, or null on error
   */
  public static String md5HashForData(byte[] _p) {
    if (_p == null) return null;

    String pwdhash = UString.hexStringFromData(md5DataHashForData(_p));
    if (pwdhash == null || pwdhash.length() == 0) {
      log.error("could not compute the MD5 hash of a given byte array.");
      return null;
    }
    return pwdhash;
  }
  
  /**
   * Calculates an MD5 hash over the given byte array and returns it as a
   * byte array.
   * 
   * @param _p - a byte array
   * @return the MD5 hash, or null on error
   */
  public static byte[] md5DataHashForData(byte[] _p) {
    if (_p == null) return null;
    
    try {
      // TODO: cache digest in thread local variable?
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(_p);
      byte[] res = md5.digest();
      md5.reset(); // TBD: superflous?
      return res;
    }
    catch (NoSuchAlgorithmException e) {
      log.error("did not find MD5 hash generator", e);
      return null;
    }    
  }

  
  /* Base64 */

  /**
   * Decode the given String as a BASE64 byte array.
   * <p>
   * Reverse is UString.stringByEncodingBase64(byte[]).
   * 
   * @param _src - the String containing the BASE64.
   * @return the decoded byte array, or null if decoding failed
   */
  public static byte[] dataByDecodingBase64(String _src) {
    if (_src == null)
      return null;
    
    try {
      // TBD: use non-private mechanism?!
      return new sun.misc.BASE64Decoder().decodeBuffer(_src);
    }
    catch (IOException e) {
      log.error("could not decode base64 string", e);
    }
    return null;
  }
  
  
  /* encryption/decryption */

  /**
   * Encrypts the given data with the given password as MD5 in the given
   * algorithm (defaults to AES).
   * The password can be prefixed with the hashing algorithm, eg:
   *   {md5}abcdef
   * to hash the password as MD5. MD5 is the default if the {} prefix is
   * missing.
   * 
   * @param _data     - data to be encrypted, may not be null
   * @param _password - will be converted to a hash
   * @param _algo     - algorithm to use for encryption (eg AES or DES)
   * @return the encrypted array of bytes, or null if something failed
   */
  public static byte[] encrypt(byte[] _data, String _password, String _algo) {
    if (_password == null || _data == null || _password.length() == 0)
      return null;

    byte[] hash;
    
    int idx;
    if (_password.charAt(0) == '{' && (idx = _password.indexOf('}')) > 1) {
      String hashAlgo = _password.substring(1, idx);
      
      // TBD: use generic mechanism
      if (hashAlgo.equalsIgnoreCase("md5"))
        hash = UData.md5DataHashForData(UString.getBytes(_password, null));
      else {
        log.error("unsupported password hash algorithm: " + hashAlgo);
        return null;
      }
    }
    else
      hash = UData.md5DataHashForData(UString.getBytes(_password, null));
    
    return UData.encrypt(_data, hash, _algo);
  }
  
  /**
   * Decrypts the given data with the given password as MD5 in the given
   * algorithm (defaults to AES).
   * The password can be prefixed with the hashing algorithm, eg:
   *   {md5}abcdef
   * to hash the password as MD5. MD5 is the default if the {} prefix is
   * missing.
   * 
   * @param _data     - data to be decrypted, may not be null
   * @param _password - will be converted to a hash
   * @param _algo     - algorithm to use for encryption (eg AES or DES)
   * @return the encrypted array of bytes, or null if something failed
   */
  public static byte[] decrypt(byte[] _data, String _password, String _algo) {
    if (_password == null || _data == null || _password.length() == 0)
      return null;

    byte[] hash;
    
    int idx;
    if (_password.charAt(0) == '{' && (idx = _password.indexOf('}')) > 1) {
      String hashAlgo = _password.substring(1, idx);
      
      // TBD: use generic mechanism
      if (hashAlgo.equalsIgnoreCase("md5"))
        hash = UData.md5DataHashForData(UString.getBytes(_password, null));
      else {
        log.error("unsupported password hash algorithm: " + hashAlgo);
        return null;
      }
    }
    else
      hash = UData.md5DataHashForData(UString.getBytes(_password, null));
    
    return UData.decrypt(_data, hash, _algo);
  }
  
  /**
   * Encrypts the given data with the given key in the given algorithm
   * (defaults to AES).
   * 
   * @param _data - data to be encrypted, may not be null
   * @param _key  - key used to encrypt
   * @param _algo - algorithm to use for encryption (eg AES or DES)
   * @return the encrypted array of bytes, or null if something failed
   */
  public static byte[] encrypt(byte[] _data, byte[] _key, String _algo) {
    if (_key == null || _data == null)
      return null;
    
    if (_algo == null)
      _algo = "AES";
    
    SecretKey key = new SecretKeySpec(_key, _algo);
    
    /* encrypt */
    
    Cipher enc = null;
    try {
      enc = Cipher.getInstance(_algo);
    }
    catch (NoSuchAlgorithmException e) {
      log.error("no AES available", e);
      return null;
    }
    catch (NoSuchPaddingException e) {
      log.error("AES padding error", e);
      return null;
    }
    
    try {
      enc.init(Cipher.ENCRYPT_MODE, key);
    }
    catch (InvalidKeyException e) {
      log.error("invalid AES key (#" + _key.length + " bytes)", e);
      return null;
    }
    
    try {
      return enc.doFinal(_data);
    }
    catch (IllegalBlockSizeException e) {
      log.error("illegal block size when encrypting data", e);
      return null;
    }
    catch (BadPaddingException e) {
      log.error("bad padding while encrypting data", e);
      return null;
    }
  }

  /**
   * Decrypts the given data with the given key in the given algorithm
   * (defaults to AES).
   * 
   * @param _data - data to be decrypted, may not be null
   * @param _key  - key used to encrypt
   * @param _algo - algorithm to use for encryption (eg AES or DES)
   * @return the encrypted array of bytes, or null if something failed
   */
  public static byte[] decrypt(byte[] _data, byte[] _key, String _algo) {
    if (_key == null || _data == null)
      return null;
    
    if (_algo == null)
      _algo = "AES";
    
    SecretKey key = new SecretKeySpec(_key, _algo);
    
    /* encrypt */
    
    Cipher enc = null;
    try {
      enc = Cipher.getInstance(_algo);
    }
    catch (NoSuchAlgorithmException e) {
      log.error("no AES available", e);
      return null;
    }
    catch (NoSuchPaddingException e) {
      log.error("AES padding error", e);
      return null;
    }
    
    try {
      enc.init(Cipher.DECRYPT_MODE, key);
    }
    catch (InvalidKeyException e) {
      log.error("invalid AES key (#" + _key.length + " bytes)", e);
      return null;
    }
    
    try {
      return enc.doFinal(_data);
    }
    catch (IllegalBlockSizeException e) {
      log.error("illegal block size when decrypting data", e);
      return null;
    }
    catch (BadPaddingException e) {
      log.error("bad padding while decrypting data", e);
      return null;
    }
  }
}
