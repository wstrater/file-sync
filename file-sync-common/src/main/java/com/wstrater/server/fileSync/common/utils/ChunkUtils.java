package com.wstrater.server.fileSync.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.ChunkInfo;

/**
 * Creates a new instance of {@link ChunkInfo} based on the file size and block size. There are a
 * maximum of {@link Long#SIZE} chunks. This allows bit flags for each chunk.
 * 
 * @author wstrater
 *
 */
public class ChunkUtils {

  public final static int       DEFAULT_BLOCK_SIZE = 1024 * 32;

  protected final static Logger logger             = LoggerFactory.getLogger(ChunkUtils.class);

  private static int            blockSize          = DEFAULT_BLOCK_SIZE;

  /**
   * Get the default block size.
   * 
   * @return
   */
  public static int getBlockSize() {
    return ChunkUtils.blockSize;
  }

  /**
   * Create a new {@see ChunkInfo} based on the fileSize and blockSize.
   * 
   * @param fileSize
   * @param blockSize
   * @return
   */
  public static ChunkInfo newInstance(long fileSize, int blockSize) {
    ChunkInfo ret = new ChunkInfo();

    ret.setBlockSize(blockSize);

    if (fileSize > 0) {
      int numBlocks = ((int) fileSize / blockSize) + 1;
      int chunkSize = ((int) numBlocks / ChunkInfo.MAX_NUM_CHUNKS) + 1;
      int numChunks = (int) Math.ceil((double) numBlocks / chunkSize);
      ret.setChunkSize(chunkSize);
      ret.setNumChunks(numChunks);
    } else {
      ret.setChunkSize(0);
      ret.setNumChunks(0);
    }

    return ret;
  }

  /**
   * Set the default block size.
   * 
   * @param blockSize
   */
  public static void setBlockSize(int blockSize) {
    ChunkUtils.blockSize = blockSize;
  }

}