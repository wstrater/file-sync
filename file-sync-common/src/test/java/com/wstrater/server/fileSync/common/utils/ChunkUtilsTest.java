package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.wstrater.server.fileSync.common.data.ChunkInfo;

public class ChunkUtilsTest {

  protected void newChunkInfoTest(long fileSize, int blockSize) {
    assertTrue("Negative fileSize", fileSize >= 0);
    ChunkInfo chunkInfo = ChunkUtils.newInstance(fileSize, blockSize);
    assertNotNull("Missing chunkInfo", chunkInfo);
    System.out.println(String.format("%d -> %s", fileSize, chunkInfo));
    assertEquals("Block sizes don't match", blockSize, chunkInfo.getBlockSize());
    assertTrue(String.format("Negative getChunkSize: %d", chunkInfo.getChunkSize()), chunkInfo.getChunkSize() >= 0);
    assertTrue(String.format("Negative getNumChunks: %d", chunkInfo.getNumChunks()), chunkInfo.getNumChunks() >= 0);
    long maxSize = (long) chunkInfo.getBlockSize() * chunkInfo.getChunkSize() * chunkInfo.getNumChunks();
    assertTrue(String.format("%d * %d * %d = %d < %d", chunkInfo.getBlockSize(), chunkInfo.getChunkSize(),
        chunkInfo.getNumChunks(), maxSize, fileSize), maxSize > fileSize);
  }

  @Test
  public void testNewChunkInfo() {
    Random rand = new Random();

    int blockSize = 8096;
    for (int xx = 0; xx < 10000; xx++) {
      long fileSize = Math.abs(rand.nextInt());
      newChunkInfoTest(fileSize, blockSize);
    }
    
    long fileSize = 0L;
    for (int xx = 0; xx < 10000; xx++) {
      fileSize += rand.nextInt(blockSize >> 1);
      newChunkInfoTest(fileSize, blockSize);
    }
  }

}