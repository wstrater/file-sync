package com.wstrater.server.fileSync.client;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.ChunkInfo;
import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.exceptions.ErrorListingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.InvalidBlockHashException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.NotValidDirectoryException;
import com.wstrater.server.fileSync.common.file.BlockReader;
import com.wstrater.server.fileSync.common.file.BlockReaderLocalImpl;
import com.wstrater.server.fileSync.common.file.BlockWriter;
import com.wstrater.server.fileSync.common.file.BlockWriterLocalAsRemoteImpl;
import com.wstrater.server.fileSync.common.file.BlockWriterLocalImpl;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalAsRemoteImpl;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalImpl;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.ActionEnum;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.IndexManager;

/**
 * This class is used to process the <code>sync</code> command line option. It uses a
 * {@link DirectoryLister} to list the directories and {@link PlanMapper} to produce a plan. Once a
 * plan is generated, it steps through the plan and executes the {@link ActionEnum} for the
 * {@link PlanItem}.
 * 
 * @author wstrater
 *
 */
public class Syncer {

  protected final Logger  logger = LoggerFactory.getLogger(getClass());

  private File            localBaseDir;
  private DirectoryLister localLister;
  private BlockReader     localReader;
  private BlockWriter     localWriter;
  private boolean         hiddenDirectories;
  private boolean         hiddenFiles;
  private FilePermissions permissions;
  private boolean         recursive;
  private File            remoteBaseDir;
  private RemoteClient    remoteClient;
  private DirectoryLister remoteLister;
  private BlockReader     remoteReader;
  private BlockWriter     remoteWriter;

  private Syncer() {}

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Delete a directory from either file systems.
   * 
   * @param lister
   * @param baseDir
   * @param path
   */
  private void deleteDirectory(DirectoryLister lister, File baseDir, String path) {
    logger.info(String.format("Deleting Directory %s from %s", path, baseDir));

    DirectoryDeleteRequest request = new DirectoryDeleteRequest();
    request.setBaseDir(baseDir);
    request.setPath(path);
    request.setFiles(true);
    request.setRecursive(true);
    DirectoryDeleteResponse response = lister.deleteDirectory(request);
  }

  /**
   * Delete a file from either file system.
   * 
   * @param writer
   * @param baseDir
   * @param path
   * @param indexInfo
   * @param action
   */
  private void deleteFile(BlockWriter writer, File baseDir, String path, IndexInfo indexInfo, ActionEnum action) {
    logger.info(String.format("Deleting File %s in %s from %s", indexInfo.getName(), path, baseDir));

    indexInfo.getChunkInfo().setAction(action);
    writeLocalIndex(localBaseDir, indexInfo);

    DeleteRequest request = new DeleteRequest();
    request.setBaseDir(baseDir);
    request.setFileName(newPath(path, indexInfo.getName()));
    DeleteResponse response = writer.deleteFile(request);

    indexInfo.getChunkInfo().setAction(ActionEnum.Done);
    deleteLocalIndex(localBaseDir, indexInfo);
  }

  /**
   * Delete the {@link IndexFile} from the local file system. This is used to keep track of the
   * progress locally.
   * 
   * @param baseDir
   * @param indexInfo
   */
  private void deleteLocalIndex(File baseDir, IndexInfo indexInfo) {
    IndexManager.deleteIndexItem(baseDir, indexInfo);
  }

  private DirectoryInfo getDirectoryInfo(DirectoryLister lister, File baseDir, String path) {
    DirectoryInfo ret = null;

    DirectoryListRequest request = new DirectoryListRequest();
    request.setBaseDir(baseDir);
    request.setPath(path);
    request.setHiddenDirectories(hiddenDirectories);
    request.setHiddenFiles(hiddenFiles);
    request.setRecursive(false);

    try {
      DirectoryListResponse response = lister.listDirectory(request);
      if (response != null && response.isSuccess()) {
        ret = response.getDirectoryInfo();
      } else {
        throw new ErrorListingDirectoryException(String.format("Unable to list directory: %s", request));
      }
    } catch (NotValidDirectoryException ee) {
      // It does not exist so create an empty structure.
      ret = new DirectoryInfo();
    }

    return ret;
  }

  /**
   * Set up the local and remote workers.
   * 
   * @param remoteClient Missing for unit testing.
   */
  private void init(RemoteClient remoteClient) {
    localLister = new DirectoryListerLocalImpl();
    localReader = new BlockReaderLocalImpl();
    localWriter = new BlockWriterLocalImpl();

    if (remoteBaseDir != null && remoteClient == null) {
      // This is for unit testing only. Both remote and local are on the local file system.
      remoteReader = new BlockReaderLocalImpl();
      // These are special implementations for swapping local and remote permissions while unit
      // testing.
      remoteLister = new DirectoryListerLocalAsRemoteImpl();
      remoteWriter = new BlockWriterLocalAsRemoteImpl();
    } else {
      remoteLister = new DirectoryListerRemoteImpl(remoteClient);
      remoteReader = new BlockReaderRemoteImpl(remoteClient);
      remoteWriter = new BlockWriterRemoteImpl(remoteClient);
    }
  }

  /**
   * Create a new directory on either file system.
   * 
   * @param lister
   * @param baseDir
   * @param path
   */
  private void makeDirectory(DirectoryLister lister, File baseDir, String path) {
    logger.info(String.format("Making Directory %s in %s", path, baseDir));

    DirectoryMakeRequest request = new DirectoryMakeRequest();
    request.setBaseDir(baseDir);
    request.setPath(path);
    DirectoryMakeResponse response = lister.makeDirectory(request);
  }

  /**
   * Create a new path as the process recursively steps through the sub-directories.
   * 
   * @param path
   * @param name
   * @return
   */
  private String newPath(String path, String name) {
    return String.format("%s/%s", path, name);
  }

  /**
   * Generate the plans and sync the local and remote file systems.
   * 
   * @param sync
   * @param path
   */
  public void sync(SyncEnum sync, String path) {
    if (sync == null) {
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

    syncContents(sync, path);
  }

  private void syncContents(SyncEnum sync, String path) {
    File dir = FileUtils.canonicalFile(new File(localBaseDir, path));
    if (!DirectoryUtils.isChild(localBaseDir, dir)) {
      throw new InvalidFileLocationException(String.format("Invalid directory '%s'", dir.getAbsolutePath()));
    }

    DirectoryInfo localDirectory = getDirectoryInfo(localLister, localBaseDir, path);
    logger.debug(String.format("Local Directory: %s", localDirectory));
    IndexFile localIndex = IndexManager.loadIndex(dir);
    IndexManager.updateIndexInfo(localIndex, localDirectory);

    DirectoryInfo remoteDirectory = getDirectoryInfo(remoteLister, remoteBaseDir, path);
    logger.debug(String.format("Remote Directory: %s", remoteDirectory));
    IndexFile remoteIndex = new IndexFile();
    IndexManager.updateIndexInfo(remoteIndex, remoteDirectory);

    PlanMapper planMap = new PlanMapper(sync, localDirectory, localIndex, remoteDirectory, remoteIndex, recursive, permissions);
    logger.debug(String.format("path: %s, planMap: %s", path, planMap.toString()));

    /*
     * if (permissions.isLocalWrite()) { makeDirectory(localLister, localBaseDir, path); } if
     * (permissions.isRemoteWrite()) { makeDirectory(remoteLister, remoteBaseDir, path); }
     */

    for (PlanItem planItem : planMap.getPlanItems()) {
      switch (planItem.getAction()) {
        case DeleteFileFromRemote: {
          deleteFile(remoteWriter, remoteBaseDir, path, (IndexInfo) planItem.getRemote(), planItem.getAction());
          break;
        }
        case CopyFileToRemote: {
          syncFile(localReader, localBaseDir, remoteWriter, remoteBaseDir, path, (IndexInfo) planItem.getLocal(),
              planItem.getAction());
          break;
        }
        case DeleteFileFromLocal: {
          deleteFile(localWriter, localBaseDir, path, (IndexInfo) planItem.getLocal(), planItem.getAction());
          break;
        }
        case CopyFileToLocal: {
          syncFile(remoteReader, remoteBaseDir, localWriter, localBaseDir, path, (IndexInfo) planItem.getRemote(),
              planItem.getAction());
          break;
        }
        case DeleteDirFromRemote: {
          deleteDirectory(remoteLister, remoteBaseDir, newPath(path, planItem.getRemote().getName()));
          break;
        }
        case SyncLocalDirToRemote: {
          if (recursive) {
            syncContents(sync, newPath(path, planItem.getLocal().getName()));
          }
          break;
        }
        case DeleteDirFromLocal: {
          deleteDirectory(localLister, localBaseDir, newPath(path, planItem.getLocal().getName()));
          break;
        }
        case SyncRemoteDirToLocal: {
          if (recursive) {
            syncContents(sync, newPath(path, planItem.getRemote().getName()));
          }
          break;
        }
        default: {
          break;
        }
      }
    }
  }

  /**
   * @param reader
   * @param readBaseDir Used for reading. Could be local or remote.
   * @param writer
   * @param writeBaseDir Used for writing. Could be local or remote.
   * @param path
   * @param indexInfo
   * @param action
   */
  private void syncFile(BlockReader reader, File readBaseDir, BlockWriter writer, File writeBaseDir, String path,
      IndexInfo indexInfo, ActionEnum action) {
    String fileName = newPath(path, indexInfo.getName());

    logger.info(String.format("Syncing File %s in %s from %s to %s", indexInfo.getName(), path, readBaseDir, writeBaseDir));

    ReadRequest readRequest = new ReadRequest();
    readRequest.setBaseDir(readBaseDir);
    readRequest.setFileName(fileName);

    WriteRequest writeRequest = new WriteRequest();
    writeRequest.setBaseDir(writeBaseDir);
    writeRequest.setFileName(fileName);
    writeRequest.setEof(false);
    // In the past until done writing.
    writeRequest.setTimeStamp(indexInfo.getLastModified() - TimeUnit.DAYS.toMillis(2L));

    ChunkInfo localChunk = indexInfo.getChunkInfo();
    localChunk.setAction(action);
    writeLocalIndex(localBaseDir, indexInfo);

    readRequest.setBlockSize(localChunk.getBlockSize());
    for (int chunkIndex = 0; chunkIndex < localChunk.getNumChunks(); chunkIndex++) {
      if (!localChunk.isFlag(chunkIndex)) {
        long offset = (long) chunkIndex * localChunk.getChunkSize() * localChunk.getBlockSize();
        for (int blockIndex = 0; blockIndex < localChunk.getChunkSize(); blockIndex++) {
          readRequest.setOffset(offset);
          ReadResponse readResponse = reader.readBlock(readRequest);

          writeRequest.setOffset(offset);
          writeRequest.setData(readResponse.getData());
          writeRequest.setLength(readResponse.getLength());
          WriteResponse writeResponse = writer.writeBlock(writeRequest);

          if (readResponse.getCrc32() != writeResponse.getCrc32()) {
            throw new InvalidBlockHashException(String.format("Block hash after write of %s/%s at offset %d did not match read.",
                path, indexInfo.getName(), offset));
          }

          offset += localChunk.getBlockSize();
        }

        localChunk.setFlag(chunkIndex);
        writeLocalIndex(localBaseDir, indexInfo);
      }
    }

    writeRequest.setOffset(indexInfo.getLength());
    writeRequest.setData(new byte[0]);
    writeRequest.setLength(0);
    writeRequest.setEof(true);
    writeRequest.setTimeStamp(indexInfo.getLastModified());
    WriteResponse writeResponse = writer.writeBlock(writeRequest);

    localChunk.setAction(ActionEnum.Done); // Skip ??
    writeLocalIndex(localBaseDir, indexInfo);
  }

  private void writeLocalIndex(File baseDir, IndexInfo indexInfo) {
    IndexManager.saveIndexItem(baseDir, indexInfo);
  }

  /**
   * Allow building a {@link Syncer} with a flexible list of arguments that are easily readable.
   * 
   * @author wstrater
   *
   */
  public static class Builder {

    private Syncer built = new Syncer();

    public Syncer build() {
      if (built.remoteClient == null && built.remoteBaseDir == null) {
        throw new IllegalStateException(String.format("% is missing a %s", getClass().getSimpleName(),
            RemoteClient.class.getSimpleName()));
      } else if (built.localBaseDir == null) {
        throw new IllegalStateException(String.format("%s missing localBaseDir", getClass().getSimpleName()));
      } else if (built.permissions == null) {
        throw new IllegalStateException(String.format("%s missing a %s", getClass().getSimpleName(),
            FilePermissions.class.getSimpleName()));
      }

      built.init(built.remoteClient);

      return built;
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

    public Builder permissions(FilePermissions permissions) {
      built.permissions = permissions;
      return this;
    }

    public Builder recursive(boolean recursive) {
      built.recursive = recursive;
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