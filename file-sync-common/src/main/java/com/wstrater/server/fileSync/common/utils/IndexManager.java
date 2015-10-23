package com.wstrater.server.fileSync.common.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.wstrater.server.fileSync.common.data.ChunkInfo;
import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.FileInfo;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.data.MemoryCache;
import com.wstrater.server.fileSync.common.data.MemoryCache.CacheReader;
import com.wstrater.server.fileSync.common.data.MemoryCache.CacheWriter;

/**
 * This class manages the {@link IndexFile} written to each directory.
 * <p/>
 * <strong>This implementation uses a locking cache in memory but does not block concurrent updates
 * on the file system.</strong> This should not be an issue since only the client would be updating
 * these files and there should only be one client running at one time.
 * 
 * @author wstrater
 *
 */
public class IndexManager {

  private final static int             CACHE_SIZE      = 25;
  private final static long            CACHE_LIFE      = TimeUnit.MINUTES.toMillis(5L);

  public final static String           CHUNK_SEPARATOR = ",";
  public final static String           INDEX_SEPARATOR = "|";

  private MemoryCache<File, IndexFile> cache           = null;

  /**
   * Setup the singleton instance. This is a self-populating and write through cache.
   */
  private IndexManager() {
    cache = new MemoryCache.Builder<File, IndexFile>().size(CACHE_SIZE).timeToLive(CACHE_LIFE).reader(new IndexFileReader())
        .writer(new IndexFileWriter()).build();
  }

  protected static IndexManager getInstance() {
    return Builder.getInstance();
  }

  /**
   * Delete one {@link IndexInfo} from a {@link IndexFile}. Update is saved to disk.
   * 
   * @param dir
   * @param indexInfo
   */
  public static void deleteIndexItem(File dir, IndexInfo indexInfo) {
    getInstance().deleteIndexItemToCache(dir, indexInfo);
  }

  /**
   * Load the exising {@link IndexFile}, delete the {@link IndexInfo} and save the {@link IndexInfo}
   * .
   * 
   * @param dir
   * @param indexInfo
   */
  private void deleteIndexItemToCache(File dir, IndexInfo indexInfo) {
    if (dir != null && indexInfo != null) {
      IndexFile indexFile = loadIndexFromCache(dir);
      if (indexFile != null) {
        indexFile.removeIndexInfo(indexInfo);
        saveIndexToCache(indexFile);
      }
    }
  }

  /**
   * Load the {@see IndexFile} for a directory. A new one is created if it does not exist.
   * 
   * @param dir
   * @return
   */
  public static IndexFile loadIndex(File dir) {
    return getInstance().loadIndexFromCache(dir);
  }

  /**
   * Load an {@link IndeFile} from cache. It is loaded from disk if needed.
   * 
   * @param dir
   * @return
   */
  private IndexFile loadIndexFromCache(File dir) {
    IndexFile ret = null;

    if (dir != null) {
      ret = cache.get(dir);
    }

    return ret;
  }

  /**
   * Save a {@see IndexFile} to a directory.
   * 
   * @param indexFile
   */
  public static void saveIndex(IndexFile indexFile) {
    getInstance().saveIndexToCache(indexFile);
  }

  /**
   * Save a {@link IndeFile} to cache and disk.
   * 
   * @param indexFile
   */
  private void saveIndexToCache(IndexFile indexFile) {
    if (indexFile != null) {
      cache.put(indexFile.getDirectory(), indexFile);
    }
  }

  /**
   * Update one {@link IndexInfo} for a {@link IndexFile}. Update is saved to disk.
   * 
   * @param dir
   * @param indexInfo
   */
  public static void saveIndexItem(File dir, IndexInfo indexInfo) {
    getInstance().saveIndexItemToCache(dir, indexInfo);
  }

  /**
   * Load the exising {@link IndexFile}, update it with the {@link IndexInfo} and save it.
   * 
   * @param dir
   * @param indexInfo
   */
  private void saveIndexItemToCache(File dir, IndexInfo indexInfo) {
    if (dir != null && indexInfo != null) {
      IndexFile indexFile = loadIndexFromCache(dir);
      if (indexFile != null) {
        indexFile.putIndexInfo(indexInfo);
        saveIndexToCache(indexFile);
      }
    }
  }

  /**
   * Update a {@see IndexFile} with the contents of a {@see DirectoryInfo}.
   * 
   * @param indexFile
   * @param directoryInfo
   */
  public static void updateIndexInfo(IndexFile indexFile, DirectoryInfo directoryInfo) {
    if (indexFile != null && directoryInfo != null) {
      if (directoryInfo.getFiles() != null) {
        for (FileInfo fileInfo : directoryInfo.getFiles()) {
          IndexInfo indexInfo = indexFile.getIndexInfo(fileInfo.getName());
          if (indexInfo == null) {
            indexInfo = new IndexInfo();
            indexInfo.setName(fileInfo.getName());
            indexInfo.setAccess(fileInfo.getAccess());
            indexInfo.setLastModified(fileInfo.getLastModified());
            indexInfo.setLength(fileInfo.getLength());
            indexInfo.setChunkInfo(ChunkUtils.newInstance(fileInfo.getLength(), ChunkUtils.getBlockSize()));
            indexFile.putIndexInfo(indexInfo);
          } else {
            updateIndexInfo(indexInfo, fileInfo);
          }
        }
      }
    }
  }

  /**
   * Update the contents of a {@see IndexInfo} with a {@see FileInfo}.
   * 
   * @param indexInfo
   * @param fileInfo
   */
  public static void updateIndexInfo(IndexInfo indexInfo, FileInfo fileInfo) {
    if (indexInfo != null && fileInfo != null) {
      if (!Compare.equals(indexInfo.getLastModified(), fileInfo.getLastModified())
          || !Compare.equals(indexInfo.getLength(), fileInfo.getLength())
          || !Compare.equals(indexInfo.getAccess(), fileInfo.getAccess())) {
        indexInfo.setAccess(fileInfo.getAccess());
        indexInfo.setLastModified(fileInfo.getLastModified());
        indexInfo.setLength(fileInfo.getLength());
        indexInfo.setHashType(null);
        indexInfo.setHash(null);
      }
    }
  }

  /**
   * Singleton builder.
   * 
   * @author wstrater
   *
   */
  private static class Builder {

    private static IndexManager indexManager = new IndexManager();

    public static IndexManager getInstance() {
      return indexManager;
    }

  }

  /**
   * This is the self-populating reader for the cache and should only be called from the cache.
   * 
   * @author wstrater
   *
   */
  private class IndexFileReader implements CacheReader<File, IndexFile> {

    private ChunkInfo parseChunkInfo(String text) {
      ChunkInfo ret = null;

      if (text != null) {
        String[] nodes = text.split(String.format("[%s]", CHUNK_SEPARATOR));
        if (nodes != null && nodes.length >= 4) {
          ret = new ChunkInfo();
          int index = 0;
          ret.setBlockSize(parseInt(nodes[index++], 0));
          ret.setChunkSize(parseInt(nodes[index++], 0));
          ret.setNumChunks(parseInt(nodes[index++], 0));
          ret.setFlags(Long.parseLong(nodes[index++], 16));
        }
      }

      return ret;
    }

    private IndexInfo parseIndexInfo(String text) {
      IndexInfo ret = null;

      if (text != null) {
        String[] nodes = text.split(String.format("[%s]", INDEX_SEPARATOR));
        if (nodes != null && nodes.length >= 6) {
          ret = new IndexInfo();
          int index = 0;
          ret.setName(nodes[index++]);
          ret.setLastModified(parseLong(nodes[index++]));
          ret.setLength(parseLong(nodes[index++]));
          ret.setHashType(nodes[index++]);
          ret.setHash(nodes[index++]);
          ret.setChunkInfo(parseChunkInfo(nodes[index++]));
        }
      }

      return ret;
    }

    private Integer parseInt(String text) {
      return parseInt(text, null);
    }

    private Integer parseInt(String text, Integer def) {
      Integer ret = def;

      if (Compare.isNotBlank(text)) {
        try {
          ret = Integer.parseInt(text);
        } catch (NumberFormatException ee) {
        }
      }

      return ret;
    }

    private Long parseLong(String text) {
      return parseLong(text, null);
    }

    private Long parseLong(String text, Long def) {
      Long ret = def;

      if (Compare.isNotBlank(text)) {
        try {
          ret = Long.parseLong(text);
        } catch (NumberFormatException ee) {
        }
      }

      return ret;
    }

    @Override
    public IndexFile read(File dir) {
      IndexFile ret = new IndexFile();

      ret.setDirectory(dir);

      if (dir != null && dir.isDirectory()) {
        File file = new File(ret.getDirectory(), IndexFile.INDEX_FILE_NAME);
        if (file.canRead()) {
          try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try {
              String line;
              while ((line = reader.readLine()) != null) {
                IndexInfo indexInfo = parseIndexInfo(line);
                if (indexInfo != null) {
                  ret.putIndexInfo(indexInfo);
                }
              }
            } finally {
              reader.close();
            }
          } catch (IOException ee) {
            ChunkUtils.logger.error(String.format("Error loading index: %s", file.getAbsolutePath()), ee);
          }
        }
      }

      return ret;
    }
  }

  /**
   * This is the write through implementation for the cache and should only be called from the
   * cache.
   * 
   * @author wstrater
   *
   */
  private class IndexFileWriter implements CacheWriter<File, IndexFile> {

    private Object formatChunkInfo(ChunkInfo chunkInfo) {
      String ret = null;

      if (chunkInfo != null) {
        StringBuilder buf = new StringBuilder();

        buf.append(chunkInfo.getBlockSize());
        buf.append(CHUNK_SEPARATOR);
        buf.append(chunkInfo.getChunkSize());
        buf.append(CHUNK_SEPARATOR);
        buf.append(chunkInfo.getNumChunks());
        buf.append(CHUNK_SEPARATOR);
        buf.append(Long.toHexString(chunkInfo.getFlag()));

        ret = buf.toString();
      }

      return ret;
    }

    private String formatIndexInfo(IndexInfo indexInfo) {
      String ret = null;

      if (indexInfo != null) {
        StringBuilder buf = new StringBuilder();

        if (indexInfo.getName() != null) {
          buf.append(indexInfo.getName());
        }
        buf.append(INDEX_SEPARATOR);
        if (indexInfo.getLastModified() != null) {
          buf.append(indexInfo.getLastModified());
        }
        buf.append(INDEX_SEPARATOR);
        if (indexInfo.getLength() != null) {
          buf.append(indexInfo.getLength());
        }
        buf.append(INDEX_SEPARATOR);
        if (indexInfo.getHashType() != null) {
          buf.append(indexInfo.getHashType());
        }
        buf.append(INDEX_SEPARATOR);
        if (indexInfo.getHash() != null) {
          buf.append(indexInfo.getHash());
        }
        buf.append(INDEX_SEPARATOR);
        if (indexInfo.getChunkInfo() != null) {
          buf.append(formatChunkInfo(indexInfo.getChunkInfo()));
        }

        ret = buf.toString();
      }

      return ret;
    }

    @Override
    public void write(File dir, IndexFile indexFile) {
      if (indexFile != null && indexFile.getDirectory() != null && indexFile.getDirectory().isDirectory()) {
        File file = new File(indexFile.getDirectory(), IndexFile.INDEX_FILE_NAME);
        if (!file.exists() || file.canWrite()) {
          try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            try {
              for (Entry<String, IndexInfo> entry : indexFile.getIndexInfos().entrySet()) {
                writer.write(formatIndexInfo(entry.getValue()));
                writer.newLine();
              }
            } finally {
              writer.close();
            }
          } catch (IOException ee) {
            ChunkUtils.logger.error(String.format("Error writing index: %s", file.getAbsolutePath()), ee);
          }
        }
      }
    }

  }

}