package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;

public class DirectoryUtilsTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void testSubDirectory() throws Exception {
    File baseDir = new File(System.getProperty("user.dir"));
    baseDir = FileUtils.canonicalFile(new File("."));

    assertTrue("Current dir is not sub-directory", DirectoryUtils.isChild(baseDir, new File(".")));
    assertFalse("Parent dir is sub-directory", DirectoryUtils.isChild(baseDir, new File("..")));
    assertFalse("Root dir is sub-directory", DirectoryUtils.isChild(baseDir, new File("/")));
    assertFalse("Root sub-dir is sub-directory", DirectoryUtils.isChild(baseDir, new File("/tmp")));
    assertTrue("File is not sub-directory", DirectoryUtils.isChild(baseDir, new File("temp.dat")));
    assertTrue("Child is not sub-directory", DirectoryUtils.isChild(baseDir, new File("subDir/../temp.dat")));
    assertTrue("Relative child is not sub-directory", DirectoryUtils.isChild(baseDir, new File("subDir/temp.dat")));
    assertTrue("Relative child of parent is not sub-directory",
        DirectoryUtils.isChild(baseDir, new File(String.format("../%s/temp.dat", baseDir.getName()))));
    assertTrue(
        "Relative grandchild of grandparent is not sub-directory",
        DirectoryUtils.isChild(baseDir,new File(
            String.format("../../%s/%s/temp.dat", baseDir.getParentFile().getName(), baseDir.getName()))));
  }

  @Test
  public void testListDirectory() throws Exception {
    DirectoryUtils.setBaseDir(new File(System.getProperty("user.dir"), ".."));

    DirectoryInfo directoryInfo = DirectoryUtils.listDirectory(DirectoryUtils.getBaseDir(), ".", true, false, false);
    assertNotNull("Missing directoryInfo", directoryInfo);
    logger.debug(String.format("directoryInfo: %s", directoryInfo));
    assertNotNull("Missing directoryInfo.getDirectories", directoryInfo.getDirectories());
    assertFalse("Empty directoryInfo.getDirectories", directoryInfo.getDirectories().isEmpty());
    assertNotNull("Missing directoryInfo.getFiles", directoryInfo.getFiles());
    assertFalse("Missing directoryInfo.getFiles", directoryInfo.getFiles().isEmpty());

    directoryInfo = DirectoryUtils.listDirectory(DirectoryUtils.getBaseDir(), ".", false, true, true);
    assertNotNull("Missing directoryInfo", directoryInfo);
    logger.debug(String.format("directoryInfo: %s", directoryInfo));
    assertNotNull("Found directoryInfo.getDirectories", directoryInfo.getDirectories());
    assertNotNull("Missing directoryInfo.getFiles", directoryInfo.getFiles());
    assertFalse("Missing directoryInfo.getFiles", directoryInfo.getFiles().isEmpty());
  }

}