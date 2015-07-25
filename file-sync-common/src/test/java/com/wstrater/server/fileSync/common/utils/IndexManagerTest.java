package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;

public class IndexManagerTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private Random         rand   = new Random();

  @Test
  public void testListDirectory() throws Exception {
    File baseDir = new File(System.getProperty("user.dir"));
    logger.info(String.format("baseDir: %s", baseDir.getAbsolutePath()));

    DirectoryInfo directoryInfo = DirectoryUtils.listDirectory(baseDir, ".", false, false, true);
    assertNotNull("Missing directoryInfo", directoryInfo);
    logger.debug(String.format("directoryInfo: %s", directoryInfo));

    // Create a new one instead of loading an existing one.
    IndexFile indexFile = new IndexFile();
    indexFile.setDirectory(baseDir);
    IndexManager.updateIndexInfo(indexFile, directoryInfo);
    assertNotNull("Missing updated indexFile", indexFile);
    logger.debug(String.format("indexFile: %s", indexFile));
    assertEquals("Number of indexInfos does not match number of files in directory", directoryInfo.getFiles().size(), indexFile
        .getIndexInfos().size());

    if (!indexFile.getIndexInfos().isEmpty()) {
      // Create a new directory so don't mess with an existing file.
      File dir = new File(baseDir, Integer.toString(100000 + rand.nextInt(899999)));
      dir.mkdirs();
      try {
        indexFile.setDirectory(dir);

        File file = new File(dir, IndexFile.INDEX_FILE_NAME);
        IndexManager.saveIndex(indexFile);
        try {
          assertTrue(String.format("File should exist: %s", file.getAbsolutePath()), file.exists());

          Set<String> names = indexFile.getIndexInfos().keySet();
          String name = names.iterator().next();
          IndexInfo original = indexFile.getIndexInfo(name);
          assertNotNull("Missing original indexInfo", original);
          Long length = original.getLength() << 1;
          original.setLength(length);
          IndexManager.saveIndexItem(indexFile.getDirectory(), original);

          indexFile = IndexManager.loadIndex(dir);
          assertNotNull("Missing loaded indexFile", indexFile);
          IndexInfo loaded = indexFile.getIndexInfo(name);
          assertNotNull("Missing loaded indexInfo", loaded);
          assertEquals("Length does not match", length, loaded.getLength());
        } finally {
          file.delete();
        }
      } finally {
        dir.delete();
      }
    }
  }

}