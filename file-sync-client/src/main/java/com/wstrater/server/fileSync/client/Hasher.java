package com.wstrater.server.fileSync.client;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.exceptions.ErrorHashingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.NotValidDirectoryException;
import com.wstrater.server.fileSync.common.file.HashRequester;
import com.wstrater.server.fileSync.common.file.HashRequesterLocalImpl;
import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This class is used to process the <code>hash</code> command line option. It can submit requests
 * remotely and/or locally. The main thread waits for the local process to finish before returning.
 * 
 * @author wstrater
 *
 */
public class Hasher {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String         hashType;
  private boolean        hiddenDirectories;
  private boolean        hiddenFiles;
  private File           localBaseDir;
  private boolean        recursive;
  private boolean        rehash;
  private File           remoteBaseDir;
  private RemoteClient   remoteClient;
  private HashRequester  remoteHasher;

  private Hasher() {}

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Set up the local and remote workers.
   * 
   * @param remoteClient Missing for unit testing.
   */
  private void init(RemoteClient remoteClient) {
    if (remoteBaseDir != null && remoteClient == null) {
      remoteHasher = new HashRequesterLocalImpl();
    } else {
      remoteHasher = new HashRequesterRemoteImpl(remoteClient);
    }
  }

  /**
   * Submit the hash requests.
   * 
   * @param hash
   * @param path
   */
  public void hash(SyncEnum hash, String path) {
    if (hash == null) {
      throw new IllegalStateException(String.format("%s missing %s", getClass().getSimpleName(), SyncEnum.class.getSimpleName()));
    } else if (Compare.isBlank(path)) {
      throw new IllegalStateException(String.format("%s missing path", getClass().getSimpleName()));
    }

    File dir = FileUtils.canonicalFile(new File(localBaseDir, path));
    if (!dir.isDirectory()) {
      throw new NotValidDirectoryException(String.format("Not a valid directory '%s'", dir.getAbsolutePath()));
    } else if (!DirectoryUtils.isChild(localBaseDir, dir)) {
      throw new InvalidFileLocationException(String.format("Not a valid directory '%s'", dir.getAbsolutePath()));
    }

    if (hash.syncLocalToRemote()) {
      HashRequest request = new HashRequest();
      request.setBaseDir(remoteBaseDir);
      request.setPath(path);
      request.setHashType(hashType);
      request.setHiddenDirectories(hiddenDirectories);
      request.setHiddenFiles(hiddenFiles);
      request.setRecursive(recursive);
      request.setReHashExisting(rehash);
      remoteHasher.hashDirectory(request);
    }

    if (hash.syncRemoteToLocal()) {
      HashRequest request = new HashRequest();
      request.setBaseDir(remoteBaseDir);
      request.setPath(path);
      request.setHashType(hashType);
      request.setHiddenDirectories(hiddenDirectories);
      request.setHiddenFiles(hiddenFiles);
      request.setRecursive(recursive);
      request.setReHashExisting(rehash);
      Future<HashStatus> future = HashProcessor.queueRequest(request);
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new ErrorHashingDirectoryException(String.format("Error hashing local directory: %s", path));
      }
    }
  }

  /**
   * Allow building a {@link Hasher} with a flexible list of arguments that are easily readable.
   * 
   * @author wstrater
   *
   */
  public static class Builder {

    private Hasher built = new Hasher();

    public Hasher build() {
      if (built.remoteClient == null && built.remoteBaseDir == null) {
        throw new IllegalStateException(String.format("% is missing a %s", getClass().getSimpleName(),
            RemoteClient.class.getSimpleName()));
      } else if (built.localBaseDir == null) {
        throw new IllegalStateException(String.format("%s missing localBaseDir", getClass().getSimpleName()));
      }

      built.init(built.remoteClient);

      return built;
    }

    public Builder hashType(String hashType) {
      built.hashType = hashType;
      return this;
    }

    public Builder hiddenDirectories(boolean hiddenDirectories) {
      built.hiddenDirectories = hiddenDirectories;
      return this;
    }

    public Builder hiddenFiles(boolean hiddenFiles) {
      built.hiddenFiles = hiddenFiles;
      return this;
    }

    public Builder localBaseDir(File localBaseDir) {
      built.localBaseDir = localBaseDir;
      return this;
    }

    public Builder recursive(boolean recursive) {
      built.recursive = recursive;
      return this;
    }

    public Builder rehash(boolean rehash) {
      built.rehash = rehash;
      return this;
    }

    /**
     * Allow for a local "remote" for testing or a real remote. This test relies on the remote using
     * correct base directory. Reset the remoteClient.
     * 
     * @param remoteBaseDir
     * @return
     */
    Builder remoteBaseDir(File remoteBaseDir) {
      built.remoteBaseDir = remoteBaseDir;
      built.remoteClient = null;
      return this;
    }

    /**
     * This is required for accessing a remote file system. Resets the remoteBaseDir used for
     * testing.
     * 
     * @param remoteClient
     * @return
     */
    public Builder remoteClient(RemoteClient remoteClient) {
      built.remoteClient = remoteClient;
      built.remoteBaseDir = null;
      return this;
    }

  }

}