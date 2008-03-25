package org.getobjects.jsapp;

import java.io.File;

/**
 * JSCachedDirectory
 * <p>
 * This caches the contents of a directory.
 */
public class JSCachedDirectory extends JSCachedObjectFile {

  public JSCachedDirectory(File _dir, String _filename) {
    super(_dir, _filename);
  }
  public JSCachedDirectory(File _file) {
    super(_file);
    
  }

  @Override
  public Object loadContent(File _file) {
    if (_file == null || !_file.isDirectory())
      return null;
    
    /* It doesn't make sense to record timestamps etc of contained files since
     * the dir lastModified won't get updated on changes on those.
     */
    return _file.listFiles();
  }
  
  @Override
  public Object parseObject(String _path, Object _content) {
    return _content; /* File[] */
  }

}
