package com.wstrater.server.fileSync.common.data;

import com.wstrater.server.fileSync.common.utils.Constants.ActionEnum;

/**
 * The {@see ChunkInfo} is used to keep track of processing a file. Progress is maintained by chunks
 * but a chunk may consist of mulitple blocks. The actual work is done by blocks.
 * 
 * @author wstrater
 *
 */
public class ChunkInfo {

  public final static char FLAG_INIT      = '.';
  public final static int  MAX_NUM_CHUNKS = Long.SIZE;

  private ActionEnum       action = ActionEnum.Skip;
  private int              blockSize;
  private int              chunkSize;
  private long             flags;
  private int              numChunks;

  public void clearFlag(int index) {
    flags &= ~getMask(index);
  }

  public ActionEnum getAction() {
    return action;
  }

  public int getBlockSize() {
    return blockSize;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public boolean isFlag(int index) {
    long mask = getMask(index);
    return (flags & mask) == mask;
  }

  public long getFlag() {
    return flags;
  }

  private long getMask(int index) {
    if (index < 0 || index >= numChunks) {
      throw new IndexOutOfBoundsException(String.format("Invalid index %d. Max is %d", index, numChunks));
    }
    return 1L << index;
  }

  public int getNumChunks() {
    return numChunks;
  }

  public void setAction(ActionEnum action) {
    this.action = action;
  }

  public void setBlockSize(int blockSize) {
    this.blockSize = blockSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public void setFlag(int index) {
    flags |= getMask(index);
  }

  public void setFlags(long flags) {
    this.flags = flags;
  }

  public void setNumChunks(int numChunks) {
    this.numChunks = numChunks;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("ChunkInfo [");
    if (action != null)
      builder.append("action=").append(action).append(", ");
    builder.append("blockSize=").append(blockSize).append(", chunkSize=").append(chunkSize).append(", numChunks=")
        .append(numChunks).append(", flags=").append(Long.toBinaryString(flags));
    builder.append("]");

    return builder.toString();
  }

}