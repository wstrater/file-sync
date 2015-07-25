package com.wstrater.server.fileSync.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.FileInfo;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.exceptions.DeleteNotAllowedException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.MissingBaseDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.MissingPathException;
import com.wstrater.server.fileSync.common.exceptions.MissingRequestException;
import com.wstrater.server.fileSync.common.exceptions.NotValidDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.WriteNotAllowedException;

public abstract class DirectoryUtils {

  protected final static Logger logger = LoggerFactory.getLogger(DirectoryUtils.class);

  private static File           baseDir;

  public static DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request) {
    DirectoryDeleteResponse ret = new DirectoryDeleteResponse();

    ret.setRequest(request);

    File dir = validateDirectoryDeleteRequest(request);

    logger.info(String.format("DeleteDiretory: %s, Files: %b, Recursive: %b", dir.getAbsolutePath(), request.isFiles(),
        request.isRecursive()));

    boolean isBaseDir = dir.equals(request.getBaseDir());

    ret.setSuccess(deleteDirectoryContents(dir, !isBaseDir, request.isFiles(), request.isRecursive()));

    logger.info(String.format("DeleteDiretory: %s, Success: %b", dir.getAbsolutePath(), ret.isSuccess()));

    return ret;
  }

  /**
   * Recursively delete files and directories.
   * 
   * @param dir
   * @param deleteDir This is set to false for the first call if dir is the baseDir.
   * @param deleteFiles
   * @param recursive
   */
  private static boolean deleteDirectoryContents(File dir, boolean deleteDir, boolean deleteFiles, boolean recursive) {
    boolean ret = true;

    if (dir != null && dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            if (recursive) {
              ret = deleteDirectoryContents(file, true, deleteFiles, recursive);
            } else {
              ret = false;
            }
          } else {
            if (deleteFiles) {
              file.delete();
            } else {
              ret = false;
            }
          }
        }
      }
      if (deleteDir) {
        dir.delete();
        ret = !dir.exists();
      }
    }

    return ret;
  }

  public static DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request) {
    DirectoryMakeResponse ret = new DirectoryMakeResponse();

    ret.setRequest(request);

    File dir = validateDirectoryMakeRequest(request);

    logger.info(String.format("MakeDiretory: %s", dir.getAbsolutePath()));

    dir.mkdirs();

    ret.setSuccess(dir.isDirectory());

    logger.info(String.format("MakeDiretory: %s, Success: %b", dir.getAbsolutePath(), ret.isSuccess()));

    return ret;
  }

  public static File getBaseDir() {
    return baseDir;
  }

  /**
   * Determine if the file is contained within the baseDir. Both need to resolve to a canonical file
   * before testing.
   * 
   * @param baseDir
   * @param file
   * @return
   */
  public static boolean isChild(File baseDir, File file) {
    boolean ret = false;

    try {
      File base = baseDir.getCanonicalFile();
      File child = file.getCanonicalFile();

      File parent = child;
      while (parent != null) {
        if (base.equals(parent)) {
          ret = true;
          break;
        }
        parent = parent.getParentFile();
      }
    } catch (IOException ee) {
      ret = false;
    }

    return ret;
  }

  public static DirectoryListResponse listDirectory(DirectoryListRequest request) {
    DirectoryListResponse ret = new DirectoryListResponse();

    ret.setRequest(request);

    File dir = validateDirectoryListRequest(request);

    logger.info(String.format("ListDiretory: %s, HiddenDirs: %b, HiddenFiles: %b, Recursive: %b", dir.getAbsolutePath(),
        request.isHiddenDirectories(), request.isHiddenFiles(), request.isRecursive()));

    ret.setDirectoryInfo(listDirectory(dir, request.isRecursive(), request.isHiddenDirectories(), request.isHiddenFiles()));

    ret.setSuccess(ret.getDirectoryInfo() != null);

    logger.info(String.format("ListDiretory: %s, Success: %b", dir.getAbsolutePath(), ret.isSuccess()));

    return ret;
  }

  /**
   * Create a {@see DirectoryInfo} structure for the path within the base dir.
   * 
   * @param baseDir
   * @param path
   * @param recursive
   * @param hiddenDirectories
   * @param hiddenFiles
   * @return
   */
  public static DirectoryInfo listDirectory(File baseDir, String path, boolean recursive, boolean hiddenDirectories,
      boolean hiddenFiles) {
    DirectoryInfo ret = null;

    if (baseDir != null && baseDir.isDirectory() && path != null) {
      File dir = new File(baseDir, path);
      ret = listDirectory(dir, recursive, hiddenDirectories, hiddenFiles);
    }

    return ret;
  }

  /**
   * Create a {@see DirectoryInfo} structure for the dir.
   * 
   * @param dir
   * @param recursive
   * @param hiddenDirectories
   * @param hiddenFiles
   * @return
   */
  public static DirectoryInfo listDirectory(File dir, boolean recursive, boolean hiddenDirectories, boolean hiddenFiles) {
    DirectoryInfo ret = null;

    if (dir != null && dir.isDirectory()) {
      dir = FileUtils.canonicalFile(dir);
      ret = new DirectoryInfo();
      ret.setName(".");
      listDirectoryContents(ret, dir, recursive, hiddenDirectories, hiddenFiles);
    }

    return ret;
  }

  /**
   * Recursively generate a {@see DirectoryInfo} structure.
   * 
   * @param parent
   * @param dir
   * @param recursive
   * @param hiddenDirectories
   * @param hiddenFiles
   */
  private static void listDirectoryContents(DirectoryInfo parent, File dir, boolean recursive, boolean hiddenDirectories,
      boolean hiddenFiles) {
    if (parent != null && dir != null && dir.isDirectory()) {
      IndexFile indexFile = IndexManager.loadIndex(dir);
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            if (hiddenDirectories || !file.isHidden()) {
              DirectoryInfo directoryInfo = new DirectoryInfo();
              directoryInfo.setName(file.getName());
              parent.addDirectory(directoryInfo);
              if (recursive) {
                listDirectoryContents(directoryInfo, file, recursive, hiddenDirectories, hiddenFiles);
              }
            }
          } else {
            if ((hiddenFiles || !file.isHidden()) && !Compare.equals(IndexFile.INDEX_FILE_NAME, file.getName())) {
              FileInfo fileInfo = new FileInfo();
              fileInfo.setName(file.getName());
              fileInfo.setLastModified(file.lastModified());
              fileInfo.setLength(file.length());
              IndexInfo indexInfo = indexFile.getIndexInfo(file.getName());
              if (indexInfo != null) {
                fileInfo.setChunkInfo(indexInfo.getChunkInfo());
                fileInfo.setHash(indexInfo.getHash());
                fileInfo.setHashType(indexInfo.getHashType());
              }
              parent.addFile(fileInfo);
            }
          }
        }
      }
    }
  }

  public static void setBaseDir(File baseDir) {
    DirectoryUtils.baseDir = FileUtils.canonicalFile(baseDir);
    logger.info(String.format("Setting baseDir: %s", DirectoryUtils.baseDir.getAbsolutePath()));
  }

  @Deprecated
  public boolean updateIndexInfoX(Map<String, IndexInfo> indexInfos, DirectoryInfo directoryInfo) {
    boolean ret = false;

    if (indexInfos != null && directoryInfo != null) {
      if (directoryInfo.getFiles() != null) {
        for (FileInfo fileInfo : directoryInfo.getFiles()) {
          IndexInfo indexInfo = indexInfos.get(fileInfo.getName());
          if (indexInfo == null) {
            indexInfo = new IndexInfo();
            indexInfo.setName(fileInfo.getName());
            indexInfo.setLastModified(fileInfo.getLastModified());
            indexInfo.setLength(fileInfo.getLength());
            indexInfo.setChunkInfo(ChunkUtils.newInstance(fileInfo.getLength(), ChunkUtils.getBlockSize()));
          } else {
            updateIndexInfoX(indexInfo, fileInfo);
          }
        }
      }
    }

    return ret;
  }

  @Deprecated
  public boolean updateIndexInfoX(IndexInfo indexInfo, FileInfo fileInfo) {
    boolean ret = false;

    if (indexInfo != null && fileInfo != null) {
      if (!Compare.equals(indexInfo.getLastModified(), fileInfo.getLastModified())
          || !Compare.equals(indexInfo.getLength(), fileInfo.getLength())) {
        indexInfo.setLastModified(fileInfo.getLastModified());
        indexInfo.setLength(fileInfo.getLength());
        indexInfo.setHashType(null);
        indexInfo.setHash(null);
      }
    }

    return ret;
  }

  private static void validateDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) {
      throw new NotValidDirectoryException(String.format("Not a valid directory '%s'", dir));
    }

  }

  private static File validateDirectoryDeleteRequest(DirectoryDeleteRequest request) {
    File ret = null;

    if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getPath() == null) {
      throw new MissingPathException("Missing path");
    } else if (FileUtils.getPermissions() == null || !FileUtils.getPermissions().isLocalDelete()) {
      throw new DeleteNotAllowedException(String.format("Delete not allowed: %s", request.getPath()));
    }

    ret = FileUtils.canonicalFile(new File(request.getBaseDir(), request.getPath()));

    validateDirectory(ret);
    validateFileLocation(request.getBaseDir(), ret);

    return ret;
  }

  private static File validateDirectoryListRequest(DirectoryListRequest request) {
    File ret = null;

    if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getPath() == null) {
      throw new MissingPathException("Missing path");
    }

    ret = FileUtils.canonicalFile(new File(request.getBaseDir(), request.getPath()));

    validateDirectory(ret);
    validateFileLocation(request.getBaseDir(), ret);

    return ret;
  }

  private static File validateDirectoryMakeRequest(DirectoryMakeRequest request) {
    File ret = null;

    if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getPath() == null) {
      throw new MissingPathException("Missing path");
    } else if (FileUtils.getPermissions() == null || !FileUtils.getPermissions().isLocalWrite()) {
      throw new WriteNotAllowedException(String.format("Write not allowed: %s", request.getPath()));
    }

    ret = FileUtils.canonicalFile(new File(request.getBaseDir(), request.getPath()));

    validateFileLocation(request.getBaseDir(), ret);

    return ret;
  }

  private static void validateFileLocation(File baseDir, File file) {
    if (baseDir == null || file == null || !DirectoryUtils.isChild(baseDir, file)) {
      throw new InvalidFileLocationException(String.format("'%s' not within '%s'", file, baseDir));
    }
  }

}