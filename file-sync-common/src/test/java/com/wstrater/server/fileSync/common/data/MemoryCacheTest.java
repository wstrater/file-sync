package com.wstrater.server.fileSync.common.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.wstrater.server.fileSync.common.data.MemoryCache.CacheReader;
import com.wstrater.server.fileSync.common.data.MemoryCache.CacheWriter;

public class MemoryCacheTest {

  private final static String CHARS         = "01456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private final static int    SIZE          = 5;
  private final static long   TIME_TO_PURGE = TimeUnit.SECONDS.toMillis(3L);
  private final static long   TIME_TO_LIVE  = TimeUnit.SECONDS.toMillis(3L);

  private Random              rand          = new Random();

  private String createRandomString(int len) {
    StringBuilder ret = new StringBuilder();

    while (ret.length() < len) {
      ret.append(CHARS.charAt(rand.nextInt(CHARS.length())));
    }

    return ret.toString();
  }

  private List<String> createTestKeys(int size) {
    List<String> ret = new ArrayList<>(size);

    while (ret.size() < size) {
      ret.add(createRandomString(10));
    }

    return ret;
  }

  private Map<String, String> createTestMap(int size) {
    return createTestMap(createTestKeys(size));
  }

  private Map<String, String> createTestMap(List<String> keys) {
    Map<String, String> ret = new HashMap<>();

    for (String key : keys) {
      ret.put(key, createRandomString(25));
    }

    return ret;
  }

  @Test
  public void testReader() throws Exception {
    List<String> keys = createTestKeys(SIZE);
    final Map<String, String> map = createTestMap(keys);

    final AtomicBoolean failRead = new AtomicBoolean(false);

    // Test that the reader works.
    MemoryCache<String, String> cache = new MemoryCache.Builder<String, String>().size(SIZE)
        .reader(new CacheReader<String, String>() {
          @Override
          public String read(String key) {
            if (failRead.get()) {
              fail(String.format("Should not be reading:  %s", key));
            }
            return map.get(key);
          }
        }).build();

    for (String key : keys) {
      String value = cache.get(key);
      assertEquals(String.format("Invalid read value: %s", key), map.get(key), value);
    }

    assertTrue("Cache shouldn't be empty", cache.size() == SIZE);

    failRead.getAndSet(true);

    // Test that the reader did work.
    for (String key : keys) {
      String value = cache.get(key);
      assertEquals(String.format("Invalid cached value: %s", key), map.get(key), value);
    }

  }

  @Test
  public void testSize() throws Exception {
    MemoryCache<String, String> cache = new MemoryCache.Builder<String, String>().size(SIZE).build();

    List<String> keys = createTestKeys(SIZE + 1);
    Map<String, String> map = createTestMap(keys);

    for (String key : keys) {
      String valueIn = map.get(key);
      cache.put(key, valueIn);
      String valueOut = cache.get(key);
      assertNotNull(String.format("Added key not found: %s", key), valueOut);
      assertEquals(String.format("Invalid cached value: %s", key), valueIn, valueOut);
      assertTrue(String.format("Too many cache entries: ", cache.size()), cache.size() <= SIZE);
    }
  }

  @Test
  public void testTimeToPurge() throws Exception {
    // Expires before purge
    MemoryCache<String, String> cache = new MemoryCache.Builder<String, String>().size(SIZE)
        .timeToLive(TIME_TO_PURGE - TimeUnit.SECONDS.toMillis(1L)).timeToPurge(TIME_TO_PURGE).build();

    Map<String, String> map = createTestMap(SIZE);
    for (Entry<String, String> entry : map.entrySet()) {
      cache.put(entry.getKey(), entry.getValue());
    }

    for (Entry<String, String> entry : map.entrySet()) {
      assertEquals(String.format("Invalid cached value: %s", entry.getKey()), entry.getValue(), cache.get(entry.getKey()));
    }

    assertTrue("Cache shouldn't be empty", cache.size() == SIZE);

    cache.purgeExpired();

    assertTrue("Cache shouldn't be purged", cache.size() == SIZE);

    // Sleep after purge up.
    Thread.sleep(TIME_TO_PURGE + TimeUnit.SECONDS.toMillis(1L));

    assertTrue("Cache should be empty", cache.size() == 0);

  }

  @Test
  public void testTimeToLive() throws Exception {
    MemoryCache<String, String> cache = new MemoryCache.Builder<String, String>().size(SIZE).timeToLive(TIME_TO_LIVE).build();

    Map<String, String> map = createTestMap(SIZE);
    for (Entry<String, String> entry : map.entrySet()) {
      cache.put(entry.getKey(), entry.getValue());
    }

    for (Entry<String, String> entry : map.entrySet()) {
      assertEquals(String.format("Invalid cached value: %s", entry.getKey()), entry.getValue(), cache.get(entry.getKey()));
    }

    // Sleep after time to live.
    Thread.sleep(TIME_TO_LIVE + TimeUnit.SECONDS.toMillis(1L));

    assertTrue("Cache shouldn't be empty", cache.size() == SIZE);

    for (Entry<String, String> entry : map.entrySet()) {
      assertNull(String.format("Should have expired: %s", entry.getKey()), cache.get(entry.getKey()));
    }

    assertTrue("Cache should be empty", cache.size() == 0);
  }

  @Test
  public void testWriter() throws Exception {
    Map<String, String> map = createTestMap(SIZE);
    final Map<String, String> written = new HashMap<>();

    final AtomicBoolean dontWrite = new AtomicBoolean(true);

    // Test that values not written are not cached.
    MemoryCache<String, String> cache = new MemoryCache.Builder<String, String>().size(SIZE)
        .writer(new CacheWriter<String, String>() {
          @Override
          public void write(String key, String value) {
            if (dontWrite.get()) {
              throw new IllegalStateException("Failed Write");
            } else {
              written.put(key, value);
            }
          }
        }).build();

    for (Entry<String, String> entry : map.entrySet()) {
      try {
        cache.put(entry.getKey(), entry.getValue());
      } catch (IllegalStateException ee) {
        // Expected
      }
      assertNull(String.format("Shouldn't have written value: %s", entry.getKey()), written.get(entry.getKey()));
      assertNull(String.format("Shouldn't have cached value: %s", entry.getKey()), cache.get(entry.getKey()));
    }

    assertTrue("Cache should be empty", cache.size() == 0);

    dontWrite.getAndSet(false);

    // Test that the writer worked.
    for (Entry<String, String> entry : map.entrySet()) {
      cache.put(entry.getKey(), entry.getValue());
      assertEquals(String.format("Invalid written value: %s", entry.getKey()), map.get(entry.getKey()), written.get(entry.getKey()));
    }
  }

}