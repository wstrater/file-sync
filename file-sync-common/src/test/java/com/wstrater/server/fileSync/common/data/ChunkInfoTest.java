package com.wstrater.server.fileSync.common.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChunkInfoTest {

  @Test
  public void testOneFlag() throws Exception {
    ChunkInfo chunkInfo = new ChunkInfo();
    chunkInfo.setNumChunks(ChunkInfo.MAX_NUM_CHUNKS);

    for (int xx = 0; xx < chunkInfo.getNumChunks(); xx++) {
      chunkInfo.setFlag(xx);
      for (int yy = 0; yy < chunkInfo.getNumChunks(); yy++) {
        if (xx == yy) {
          assertTrue(String.format("flag %d is not checked", yy), chunkInfo.isFlag(yy));
        } else {
          assertFalse(String.format("flag %d is not checked", yy), chunkInfo.isFlag(yy));
        }
      }
      chunkInfo.clearFlag(xx);
    }
  }

  @Test
  public void testAllButOneFlag() throws Exception {
    ChunkInfo chunkInfo = new ChunkInfo();
    chunkInfo.setNumChunks(ChunkInfo.MAX_NUM_CHUNKS);

    for (int xx = 0; xx < chunkInfo.getNumChunks(); xx++) {
      chunkInfo.setFlag(xx);
    }

    for (int xx = 0; xx < chunkInfo.getNumChunks(); xx++) {
      chunkInfo.clearFlag(xx);
      for (int yy = 0; yy < chunkInfo.getNumChunks(); yy++) {
        if (xx == yy) {
          assertFalse(String.format("flag %d is not checked", yy), chunkInfo.isFlag(yy));
        } else {
          assertTrue(String.format("flag %d is not checked", yy), chunkInfo.isFlag(yy));
        }
      }
      chunkInfo.setFlag(xx);
    }
  }

}