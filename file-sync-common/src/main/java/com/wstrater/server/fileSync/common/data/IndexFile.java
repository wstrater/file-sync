package com.wstrater.server.fileSync.common.data;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Keeps track of the last know state of all the files within a directory.
 * 
 * @author wstrater
 *
 */
public class IndexFile {

  public final static String     INDEX_FILE_NAME = ".indexInfo.dat";

  private File                   directory;
  private Map<String, IndexInfo> indexInfos;

  public File getDirectory() {
    return directory;
  }

  public IndexInfo getIndexInfo(String name) {
    IndexInfo ret = null;

    if (name != null) {
      ret = getIndexInfos().get(name);
    }

    return ret;
  }

  public Map<String, IndexInfo> getIndexInfos() {
    if (indexInfos == null) {
      return Collections.emptyMap();
    } else {
      return Collections.unmodifiableMap(indexInfos);
    }
  }

  public void putIndexInfo(IndexInfo indexInfo) {
    if (indexInfo != null && indexInfo.getName() != null && !INDEX_FILE_NAME.equals(indexInfo.getName())) {
      if (indexInfos == null) {
        indexInfos = new TreeMap<>();
      }
      indexInfos.put(indexInfo.getName(), indexInfo);
    }
  }

  /**
   * @param indexInfo
   * @return Returns the previous value.
   */
  public IndexInfo removeIndexInfo(IndexInfo indexInfo) {
    IndexInfo ret = null;

    if (indexInfo != null) {
      ret = removeIndexInfo(indexInfo.getName());
    }

    return ret;
  }

  /**
   * @param name
   * @return Returns the previous value for the name.
   */
  public IndexInfo removeIndexInfo(String name) {
    IndexInfo ret = null;

    if (indexInfos != null && name != null) {
      ret = indexInfos.remove(name);
    }

    return ret;
  }

  public void setDirectory(File directory) {
    if (directory == null) {
      this.directory = directory;
    } else {
      try {
        this.directory = directory.getCanonicalFile();
      } catch (IOException e) {
        this.directory = directory.getAbsoluteFile();
      }
    }
  }

  public void setIndexInfos(Map<String, IndexInfo> indexInfos) {
    this.indexInfos = indexInfos;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("IndexFile [");
    if (directory != null)
      builder.append("directory=").append(directory).append(", ");
    if (indexInfos != null)
      builder.append("indexInfos=").append(indexInfos);
    builder.append("]");

    return builder.toString();
  }

}