package com.wstrater.server.fileSync.common.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.MissingBaseDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.MissingIdException;
import com.wstrater.server.fileSync.common.exceptions.MissingPathException;
import com.wstrater.server.fileSync.common.exceptions.MissingRequestException;
import com.wstrater.server.fileSync.common.hash.HashEvent.EventType;
import com.wstrater.server.fileSync.common.utils.Base64Utils;
import com.wstrater.server.fileSync.common.utils.ChunkUtils;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.IndexManager;

public abstract class HashProcessor {

  public final static String               DEFAULT_HASH_TYPE = "SHA1";
  public final static int                  MAX_HISTORY       = 25;

  protected final static Logger            logger            = LoggerFactory.getLogger(HashProcessor.class);

  private transient static ExecutorService executor;
  private final static List<HashStatus>    hashHistory       = new ArrayList<>(MAX_HISTORY);
  private static String                    hashType          = DEFAULT_HASH_TYPE;
  private final static Object              mutex             = new Object();

  /**
   * {@see ExecutorService#awaitTermination(long, TimeUnit)}
   * 
   * @param timeout
   * @param timeUnit
   * @return
   * @throws InterruptedException
   */
  public static boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
    boolean ret = false;

    if (executor != null) {
      ret = executor.awaitTermination(timeout, timeUnit);
    }

    return ret;
  }

  private static void fireEvent(HashRequest request, HashEvent event) {
    if (event.getEvent() == EventType.StartingDirectory || event.getEvent() == EventType.Done) {
      logger.info(String.format("Firing Event %s for request %s", event, request.getId()));
    } else {
      logger.debug(String.format("Firing Event %s for request %s", event, request.getId()));
    }

    /*
     * if (request.getListener() != null) { request.getListener().fireEvent(event); }
     */
  }

  /**
   * Retrieve a {@link HashRequest} from history if it is still there.
   * 
   * @param id
   * @return
   */
  public static HashStatus getHashStatus(String id) {
    HashStatus ret = null;

    if (id != null) {
      synchronized (mutex) {
        for (HashStatus status : hashHistory) {
          if (Compare.equals(id, status.getRequest().getId())) {
            ret = status;
            break;
          }
        }
      }
    }

    return ret;
  }

  /**
   * Get the hash type used by default.
   * 
   * @return
   */
  public static String getHashType() {
    return hashType;
  }

  public static HashResponse hashDirectory(HashRequest request) {
    HashResponse ret = new HashResponse();

    ret.setRequest(request);

    File dir = validateHashRequest(request);

    Future<HashStatus> future = queueRequest(request);

    ret.setQueued(future != null);

    return ret;
  }

  /**
   * Hash the files in a directory and update the {@see IndexFile}.
   * 
   * @param request
   * @param dir
   * @param directoryInfo
   * @throws NoSuchAlgorithmException
   */
  private static void hashDirectory(HashRequest request, File dir, DirectoryInfo directoryInfo) throws NoSuchAlgorithmException {
    if (request != null && directoryInfo != null && dir != null && dir.isDirectory()) {
      logger.info(String.format("Hashing Directory: %s with %d directories and %d files", dir.getAbsolutePath(), directoryInfo
          .getDirectories().size(), directoryInfo.getFiles().size()));

      fireEvent(request, new HashEvent(EventType.StartingDirectory, FileUtils.canonicalFile(dir), 0, request.getId()));

      String hashType = Compare.isBlank(request.getHashType()) ? DEFAULT_HASH_TYPE : request.getHashType();
      MessageDigest digester = MessageDigest.getInstance(hashType);

      IndexFile indexFile = IndexManager.loadIndex(dir);
      try {
        IndexManager.updateIndexInfo(indexFile, directoryInfo);

        int count = indexFile.getIndexInfos().size();
        int index = 0;
        for (IndexInfo indexInfo : indexFile.getIndexInfos().values()) {
          File file = FileUtils.canonicalFile(new File(indexFile.getDirectory(), indexInfo.getName()));
          if (file.canRead()) {
            long length = file.length();
            boolean hashFile = request.isReHashExisting() || Compare.isBlank(indexInfo.getHash());
            if (!hashFile) {
              // Check for modified file.
              hashFile = length != indexInfo.getLength() || file.lastModified() != indexInfo.getLastModified();
            }
            if (hashFile) {
              fireEvent(request,
                  HashEvent.newProgress(EventType.StartingFile, FileUtils.canonicalFile(file), index++, count, request.getId()));

              long progress = 0L;
              byte[] buf = new byte[ChunkUtils.getBlockSize()];
              int len = 0;
              try {
                digester.reset();
                InputStream in = new FileInputStream(file);
                try {
                  while ((len = in.read(buf)) >= 0) {
                    fireEvent(
                        request,
                        HashEvent.newProgress(EventType.HashingFile, FileUtils.canonicalFile(file), progress, length,
                            request.getId()));

                    progress += len;
                    if (progress > length) {
                      throw new IOException("Trying to hash file that is growing");
                    }
                    digester.update(buf, 0, len);
                  }
                } finally {
                  in.close();
                }

                byte[] digest = digester.digest();
                indexInfo.setHash(Base64Utils.encodeAsString(digest));
                indexInfo.setHashType(hashType);
                IndexManager.saveIndexItem(indexFile.getDirectory(), indexInfo);
              } catch (IOException ee) {
                fireEvent(request,
                    new HashEvent(EventType.HashingError, String.format("%s: %s", ee.getMessage(), file.getAbsolutePath()), 0,
                        request.getId()));
              }

              fireEvent(request, new HashEvent(EventType.FinishedFile, FileUtils.canonicalFile(file), 100, request.getId()));
            }
          }
        }
      } finally {
        IndexManager.saveIndex(indexFile);
      }

      fireEvent(request, new HashEvent(EventType.FinishedDirectory, FileUtils.canonicalFile(dir), 100, request.getId()));
    }
  }

  /**
   * {@see ExecutorService#isShutdown()}
   * 
   * @return
   */
  public static boolean isShutdown() {
    return executor == null || executor.isShutdown();
  }

  /**
   * {@see ExecutorService#isTerminated()}
   * 
   * @return
   */
  public static boolean isTerminated() {
    return executor == null || executor.isTerminated();
  }

  /**
   * Process a directory for a request. Can be recursive.
   * 
   * @param request
   * @param path
   * @throws NoSuchAlgorithmException
   */
  private static void processDirectory(HashRequest request, String path) throws NoSuchAlgorithmException {
    if (request != null && path != null) {
      File dir = FileUtils.canonicalFile(new File(request.getBaseDir(), path));
      if (dir.isDirectory()) {
        DirectoryInfo directoryInfo = DirectoryUtils.listDirectory(dir, false, request.isHiddenDirectories(),
            request.isHiddenFiles());
        if (directoryInfo != null) {
          hashDirectory(request, dir, directoryInfo);
          if (request.isRecursive()) {
            for (DirectoryInfo childInfo : directoryInfo.getDirectories()) {
              processDirectory(request, String.format("%s%s%s", path, File.separator, childInfo.getName()));
            }
          }
        }
      }
    }
  }

  /**
   * Process a request. Called by the executor.
   * 
   * @param request
   * @return
   * @throws NoSuchAlgorithmException
   */
  private static HashStatus processRequest(HashStatus status) throws NoSuchAlgorithmException {
    if (status != null && status.getRequest() != null) {
      status.setDone(false);
      status.setStarted(true);

      fireEvent(status.getRequest(), new HashEvent(EventType.Starting, "Starting", 0, status.getRequest().getId()));

      try {
        processDirectory(status.getRequest(), status.getRequest().getPath());
      } catch (NoSuchAlgorithmException ee) {
        status.setFailureMessage(ee.toString());
        status.setFailed(true);
        fireEvent(status.getRequest(), new HashEvent(EventType.Failed, "Failed", 0, status.getRequest().getId()));
        throw ee;
      }

      fireEvent(status.getRequest(), new HashEvent(EventType.Done, "Finished", 100, status.getRequest().getId()));

      status.setDone(true);
    }

    return status;
  }

  /**
   * Queue up a request to be processed by a single threaded executor. The {@link Future} value is
   * the supplied {@link HashRequest}. This is just to have an object to wait for.
   * 
   * @param request
   * @return
   */
  public static Future<HashStatus> queueRequest(HashRequest request) {
    Future<HashStatus> ret = null;

    if (executor == null) {
      synchronized (mutex) {
        if (executor == null) {
          executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));
        }
      }
    }

    final HashStatus status = new HashStatus();
    status.setRequest(request);

    synchronized (mutex) {
      // Keep some history.
      while (hashHistory.size() >= MAX_HISTORY) {
        hashHistory.remove(0);
      }
      hashHistory.add(status);
    }

    logger.info(String.format("Queuing Hash Request: %s", request));

    ret = executor.submit(new Callable<HashStatus>() {

      @Override
      public HashStatus call() throws Exception {
        return processRequest(status);
      }
    });

    return ret;
  }

  /**
   * Set the hash type used by default.
   * 
   * @param hashType
   */
  public static void setHashType(String hashType) {
    HashProcessor.hashType = hashType;
    logger.info(String.format("Setting hashType: %s", HashProcessor.hashType));
  }

  /**
   * {@see ExecutorService#shutdown()()}
   * 
   * @return
   */
  public static boolean shutdown() {
    boolean ret = false;

    if (executor != null) {
      executor.shutdown();
      ret = true;
    }

    return ret;
  }

  /**
   * {@see ExecutorService#shutdownNow()}
   * 
   * @return
   */
  public static boolean shutdownNow() {
    boolean ret = false;

    if (executor != null) {
      executor.shutdownNow();
      ret = true;
    }

    return ret;
  }

  private static File validateHashRequest(HashRequest request) {
    File ret = null;

    if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (Compare.isBlank(request.getPath())) {
      throw new MissingPathException("Missing path");
    } else if (Compare.isBlank(request.getId())) {
      throw new MissingIdException("Missing id");
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