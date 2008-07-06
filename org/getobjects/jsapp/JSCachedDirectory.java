package org.getobjects.jsapp;

import java.io.File;

/**
 * JSCachedDirectory
 * <p>
 * This caches the contents of a directory. If the timestamp or size of the
 * directory changes, the contents are reloaded.
 */
public class JSCachedDirectory extends JSCachedObjectFile {

  public JSCachedDirectory(final File _dir, final String _filename) {
    super(_dir, _filename);
  }
  public JSCachedDirectory(final File _file) {
    super(_file);
    
  }

  @Override
  public Object loadContent(final File _file) {
    if (_file == null || !_file.isDirectory())
      return null;
    
    /* It does not make sense to record timestamps etc of contained files since
     * the dir lastModified won't get updated on changes on those.
     */
    return _file.listFiles();
  }
  
  @Override
  public Object parseObject(final String _path, final Object _content) {
    return _content; /* File[] */
  }

}
