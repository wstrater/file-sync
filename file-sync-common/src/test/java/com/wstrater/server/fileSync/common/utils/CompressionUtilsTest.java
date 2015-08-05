package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import com.wstrater.server.fileSync.common.utils.CompressionUtils.Deflated;
import com.wstrater.server.fileSync.common.utils.CompressionUtils.Inflated;

public class CompressionUtilsTest {

  private final static int DATA_LENGTH = 16384;
  private final static int NUM_TESTS   = 256;

  private Random           rand        = new Random();

  private byte[] generateAlphaNumericData(int length) {
    byte[] ret = new byte[length];

    int max = 'z' - ' ' + 1;

    for (int xx = 0; xx < length; xx++) {
      ret[xx] = (byte) (' ' + rand.nextInt(max));
    }

    return ret;
  }

  private byte[] generateBinaryData(int length) {
    byte[] ret = new byte[length];

    rand.nextBytes(ret);

    return ret;
  }

  private void runDeflatiotionTest(String label, byte[] data) {
    Deflated deflated = CompressionUtils.deflate(data);
    assertNotNull("Missing deflated", deflated);
    assertNotNull("Missing deflated data", deflated.getData());
    assertTrue("Invalid deflated length", deflated.getLength() > 0);

    System.out.println(String.format("Deflated %s: %d/%d - %d%%", label, deflated.getLength(), data.length,
        (int) (deflated.getLength() * 100 / data.length)));

    Inflated inflated = CompressionUtils.inflate(deflated.getData(), data.length);
    assertNotNull("Missing inflated", inflated);
    assertNotNull("Missing inflated data", inflated.getData());
    assertEquals("Invalid inflated length", data.length, inflated.getLength());
    assertTrue("Inflated not equals to data", Compare.equals(data, inflated.getData()));
  }

  private void runZipTest(String label, byte[] data) {
    Deflated deflated = CompressionUtils.zip(data);
    assertNotNull("Missing zipped", deflated);
    assertNotNull("Missing zipped data", deflated.getData());
    assertTrue("Invalid zipped length", deflated.getLength() > 0);

    System.out.println(String.format("Zipped %s: %d/%d - %d%%", label, deflated.getLength(), data.length,
        (int) (deflated.getLength() * 100 / data.length)));

    Inflated inflated = CompressionUtils.unzip(deflated.getData(), data.length);
    assertNotNull("Missing unzipped", inflated);
    assertNotNull("Missing unzipped data", inflated.getData());
    assertEquals("Invalid unzipped length", data.length, inflated.getLength());
    assertTrue("Unzipped not equals to data", Compare.equals(data, inflated.getData()));
  }

  @Test
  public void alphaNumericTest() throws Exception {
    for (int xx = 0; xx < NUM_TESTS; xx++) {
      runDeflatiotionTest(String.format("Alpha %d", xx), generateAlphaNumericData(DATA_LENGTH));
      runZipTest(String.format("Alpha %d", xx), generateAlphaNumericData(DATA_LENGTH));
    }
  }

  @Test
  public void binaryTest() throws Exception {
    for (int xx = 0; xx < NUM_TESTS; xx++) {
      runDeflatiotionTest(String.format("Binary %d", xx), generateBinaryData(DATA_LENGTH));
      runZipTest(String.format("Binary %d", xx), generateBinaryData(DATA_LENGTH));
    }
  }

  @Test
  public void validationTest() throws Exception {
    try {
      CompressionUtils.valdiate(null, 0, 0);
      fail("Expecting exception with out data");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    final int LEN = 5;

    try {
      CompressionUtils.valdiate(new byte[LEN], -1, LEN);
      fail("Expecting exception with negative offset");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    try {
      CompressionUtils.valdiate(new byte[LEN], LEN, LEN);
      fail("Expecting exception with too high offset");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    try {
      CompressionUtils.valdiate(new byte[LEN], 0, -1);
      fail("Expecting exception with negative length");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    try {
      CompressionUtils.valdiate(new byte[LEN], 0, LEN + 1);
      fail("Expecting exception with too long length");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    try {
      CompressionUtils.valdiate(new byte[LEN], 1, LEN);
      fail("Expecting exception with too long length for offset");
    } catch (IndexOutOfBoundsException ee) {
      // Expected.
    }

    CompressionUtils.valdiate(new byte[0], 0, 0);
    CompressionUtils.valdiate(new byte[LEN], 0, LEN);
    final int OFF = 1;
    CompressionUtils.valdiate(new byte[LEN], OFF, LEN - OFF);
  }

}