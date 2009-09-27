package org.getobjects.appserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class WOProjectDirectoryResourceManager extends WOClassResourceManager {

  protected File              base;
  protected Map<String, File> fileMap;

  public WOProjectDirectoryResourceManager(String _basePath, Class _clazz, boolean caching) {
    super(_clazz, caching);
    this.base = new File(_basePath);
    if (!this.base.exists()) {
      log.error("WOProjectDirectoryResourceManager initialized for " +
                "non-existing directory '" + _basePath + "'");
    }
    if (caching)
      this.fileMap = new HashMap<String, File>(32);
  }

  /* internal resources */

  @Override
  public URL urlForResourceNamed(String _name, String[] _ls) {
    if (log.isDebugEnabled()) {
      log.debug("getting URL for resource: " + _name);
    }

    File file = null;

    if (_ls != null) {
      for (String lang : _ls) {
        file = this.existingFileForResourceNamed(_name, lang);
        if (file != null)
          break;
      }
    }
    if (file == null)
      file = this.existingFileForResourceNamed(_name, null);
    if (file == null)
      return null;
    try {
      return file.toURI().toURL();
    }
    catch (MalformedURLException e) {
      return null;
    }
  }

  @Override
  public InputStream inputStreamForResourceNamed(String _name, String[] _ls) {
    if (log.isDebugEnabled()) {
      log.debug("getting inputStream for resource: " + _name);
    }

    File file = null;

    if (_ls != null) {
      for (String lang : _ls) {
        file = this.existingFileForResourceNamed(_name, lang);
        if (file != null)
          break;
      }
    }
    if (file == null)
      file = this.existingFileForResourceNamed(_name, null);
    if (file == null)
      return null;
    try {
      return new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  protected String pathComponentForResourceNamed(String _name, String _lang) {
    if (_lang == null) return _name;
    return _lang + ".lproj" + File.separator + _name;
  }

  protected File cachedFileForResourceNamed(String _name, String _lang) {
    if (this.fileMap == null) return null;
    return this.fileMap.get(this.pathComponentForResourceNamed(_name, _lang));
  }

  protected File existingFileForResourceNamed(String _name, String _lang) {
    File file = this.cachedFileForResourceNamed(_name, _lang);
    if (file != null) return file;

    String path = this.pathComponentForResourceNamed(_name, _lang);
    file = new File(this.base, path);
    if (file.exists()) {
      if (this.fileMap != null)
        this.fileMap.put(path, file);
      return file;
    }
    return null;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" baseDir=" + this.base.getAbsolutePath());
  }
}
