package com.wstrater.server.fileSync.common.hash;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;

public class HashProcessorTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void testHashing() throws Exception {
    DirectoryUtils.setBaseDir(new File(System.getProperty("user.dir"), "."));

    HashRequest request = new HashRequest();
    request.setRecursive(false);
    request.setBaseDir(DirectoryUtils.getBaseDir());
    request.setHashType(HashProcessor.getHashType());
    request.setHiddenDirectories(!request.isRecursive());
    request.setHiddenFiles(!request.isRecursive());
    request.setPath(".");
    request.setReHashExisting(!request.isRecursive());

    HashProcessor.queueRequest(request);

    HashProcessor.shutdown();
    HashProcessor.awaitTermination(1, TimeUnit.HOURS);
  }

}