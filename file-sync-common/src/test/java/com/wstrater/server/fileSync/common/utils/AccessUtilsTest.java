package com.wstrater.server.fileSync.common.utils;

import static com.wstrater.server.fileSync.common.utils.AccessUtils.access;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wstrater.server.fileSync.common.utils.AccessUtils.Access;

/**
 * @author wstrater
 *
 */
public class AccessUtilsTest {

  private static byte getMask(int index) {
    if (index < 0 || index >= AccessUtils.NUM_FLAGS) {
      throw new IndexOutOfBoundsException(String.format("Invalid index %d. Max is %d", index, Byte.SIZE));
    }
    return (byte) (1 << index);
  }

  @Test
  public void testAllButOneFlag() throws Exception {
    Access access = access();
    for (int xx = 0; xx < AccessUtils.NUM_FLAGS; xx++) {
      access.setFlag(getMask(xx));
    }

    for (int xx = 0; xx < AccessUtils.NUM_FLAGS; xx++) {
      access.clearFlag(getMask(xx));
      for (int yy = 0; yy < AccessUtils.NUM_FLAGS; yy++) {
        if (xx == yy) {
          assertFalse(String.format("Access %d is checked", yy), access.isFlag(getMask(yy)));
        } else {
          assertTrue(String.format("Access %d is not checked", yy), access.isFlag(getMask(yy)));
        }
      }
      access.setFlag(getMask(xx));
    }
  }

  @Test
  public void testCan() throws Exception {
    assertTrue("Delete dir not set", AccessUtils.canDeleteDir(access().existsDir().deleteDir().get()));
    assertTrue("Exists dir not set", AccessUtils.isExistsDir(access().existsDir().get()));
    assertTrue("Read dir not set", AccessUtils.canReadDir(access().existsDir().readDir().get()));
    assertTrue("Write dir not set", AccessUtils.canWriteDir(access().existsDir().writeDir().get()));

    assertTrue("Delete file not set", AccessUtils.canDeleteFile(access().existsDir().deleteDir().existsFile().deleteFile().get()));
    assertFalse("Delete file set without dir", AccessUtils.canDeleteFile(access().existsFile().deleteFile().get()));
    assertFalse("Delete file set without dir delete", AccessUtils.canDeleteFile(access().existsDir().existsFile().deleteFile().get()));
    assertTrue("Exists file not set", AccessUtils.isExistsFile(access().existsFile().get()));
    assertTrue("Read file not set", AccessUtils.canReadFile(access().existsDir().readDir().existsFile().readFile().get()));
    assertFalse("Read file set without dir", AccessUtils.canReadFile(access().existsFile().readFile().get()));
    assertTrue("Write file not set without file", AccessUtils.canWriteFile(access().existsDir().writeDir().get()));
    assertTrue("Write file not set", AccessUtils.canWriteFile(access().existsDir().writeDir().existsFile().writeFile().get()));
    assertFalse("Write file set without dir", AccessUtils.canWriteFile(access().existsFile().writeFile().get()));
  }

  @Test
  public void testDuplicates() throws Exception {
    for (int xx = 0; xx < AccessUtils.FLAGS.length - 1; xx++) {
      for (int yy = xx + 1; yy < AccessUtils.FLAGS.length; yy++) {
        assertTrue(
            String.format("Flags over lap: %d-%s %d-%s", xx, Access.toString(AccessUtils.FLAGS[xx]), yy,
                Access.toString(AccessUtils.FLAGS[yy])), (byte) (AccessUtils.FLAGS[xx] & AccessUtils.FLAGS[yy]) == 0);
      }
    }
  }

  @Test
  public void testOneFlag() throws Exception {
    Access access = access();
    for (int xx = 0; xx < AccessUtils.NUM_FLAGS; xx++) {
      access.setFlag(getMask(xx));
      for (int yy = 0; yy < AccessUtils.NUM_FLAGS; yy++) {
        if (xx == yy) {
          assertTrue(String.format("Access %d is not checked", yy), access.isFlag(getMask(yy)));
        } else {
          assertFalse(String.format("Access %d is checked", yy), access.isFlag(getMask(yy)));
        }
      }
      access.clearFlag(getMask(xx));
    }
  }

}