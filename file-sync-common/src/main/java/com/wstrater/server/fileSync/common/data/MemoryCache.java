package com.wstrater.server.fileSync.common.data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;

/**
 * Create a simple memory cache that supports a size limit and a time to live.
 * <p/>
 * The cache can be created with a timeToPurge. This will create a thread that sleeps timeToPurge
 * milliseconds and then perform a {@link #purgeExpired()}.
 * 
 * @author wstrater
 *
 * @param <K>
 * @param <T>
 */
public class MemoryCache<K, T> {

  public final static int            DEFAULT_SIZE = 50;
  public final static long           NON_EXPIRING = -1L;
  public final static long           NON_PURGING  = -1L;

  private final static AtomicInteger threadCount  = new AtomicInteger();

  private LRUMap                     cache;
  private Lock                       lock         = new ReentrantLock();
  private CacheReader<K, T>          reader;
  private int                        size         = DEFAULT_SIZE;
  private long                       timeToPurge  = NON_PURGING;
  private long                       timeToLive   = NON_EXPIRING;
  private CacheWriter<K, T>          writer;

  private MemoryCache() {}

  /**
   * Get a value from the cache. If it does not exist, call the {@link CacheReader}. The value
   * returned by the {@link CacheReader} is added to the cache.
   * 
   * @param key
   * @param reader
   * @return
   */
  public T get(K key) {
    T ret = null;

    lock.lock();
    try {
      ret = getCache(key);
      if (ret == null && reader != null) {
        ret = reader.read(key);
        if (ret != null) {
          putCache(key, ret);
        }
      }
    } finally {
      lock.unlock();
    }

    return ret;
  }

  /**
   * Get a value from the cache if it exists and has not expired. Expired elements are removed.
   * 
   * @param key
   * @return
   */
  private T getCache(K key) {
    T ret = null;

    lock.lock();
    try {
      @SuppressWarnings("unchecked")
      CacheValue value = (CacheValue) cache.get(key);
      if (value != null) {
        if (value.isExpired()) {
          cache.remove(key);
        } else {
          ret = (T) value.getValue();
        }
      }
    } finally {
      lock.unlock();
    }

    return ret;
  }

  /**
   * Purge any expired cached value.
   */
  @SuppressWarnings("unchecked")
  public void purgeExpired() {
    lock.lock();
    try {
      MapIterator iter = cache.mapIterator();
      while (iter.hasNext()) {
        iter.next();
        CacheValue value = (CacheValue) iter.getValue();
        if (value != null && value.isExpired()) {
          iter.remove();
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Use the {@link CacheWriter} to save the value before updating cache.
   * 
   * @param key
   * @param value
   * @param writer
   * @return The previously cached value
   */
  public T put(K key, T value) {
    T ret = null;

    if (writer == null) {
      ret = putCache(key, value);
    } else {
      lock.lock();
      try {
        writer.write(key, value);
        ret = putCache(key, value);
      } finally {
        lock.unlock();
      }
    }

    return ret;
  }

  /**
   * Add a value to the cache. The least recently used will be removed to keep the cache within it's
   * size limit.
   * 
   * @param key
   * @param value
   * @return The previously cached value
   */
  @SuppressWarnings("unchecked")
  private T putCache(K key, T value) {
    T ret = null;

    lock.lock();
    try {
      ret = (T) cache.put(key, new CacheValue(value));
    } finally {
      lock.unlock();
    }

    return ret;
  }

  public T remove(K key) {
    T ret = null;

    lock.lock();
    try {
      @SuppressWarnings("unchecked")
      CacheValue value = (CacheValue) cache.remove(key);
      if (value != null && !value.isExpired()) {
        ret = (T) value.getValue();
      }
    } finally {
      lock.unlock();
    }

    return ret;
  }

  /**
   * Added for testing. No real need to know the size of the cache. It either has the cached value
   * or it doesn't.
   * 
   * @return
   */
  int size() {
    int ret = 0;

    lock.lock();
    try {
      ret = cache.size();
    } finally {
      lock.unlock();
    }

    return ret;
  }

  /**
   * Build an immutable instance of {@link MemoryCache}.
   * 
   * @author wstrater
   *
   * @param <K>
   * @param <T>
   */
  public static class Builder<K, T> {

    private MemoryCache<K, T> built;

    public Builder() {
      built = new MemoryCache<K, T>();
    }

    public MemoryCache<K, T> build() {
      built.cache = new LRUMap(built.size);

      if (built.timeToPurge > 0L) {
        Thread thread = new Thread(new Runnable() {

          @Override
          public void run() {
            boolean running = true;
            while (running) {
              try {
                Thread.sleep(built.timeToPurge);
              } catch (InterruptedException ee) {
                // running = false;
              }
              built.purgeExpired();
            }

          }
        });
        thread.setName(String.format("cache-purge-%d", threadCount.incrementAndGet()));
        thread.setDaemon(true);
        thread.start();
      }

      return built;
    }

    public Builder<K, T> reader(CacheReader<K, T> reader) {
      built.reader = reader;
      return this;
    }

    public Builder<K, T> size(int size) {
      built.size = size;
      return this;
    }

    public Builder<K, T> timeToLive(long timeToLive) {
      built.timeToLive = timeToLive;
      return this;
    }

    public Builder<K, T> timeToPurge(long timeToPurge) {
      built.timeToPurge = timeToPurge;
      return this;
    }

    public Builder<K, T> writer(CacheWriter<K, T> writer) {
      built.writer = writer;
      return this;
    }

  }

  /**
   * Provide an interface for populating the cache within the get lock. Allows implementing a self
   * populating cache. This will block other gets and puts but will keep everything synchronized.
   * 
   * @author wstrater
   *
   * @param <K>
   * @param <T>
   */
  public static interface CacheReader<K, T> {

    public T read(K key);

  }

  /**
   * Keep track of lastAccessed for a cachced value.
   * 
   * @author wstrater
   *
   */
  private class CacheValue {

    private long lastAccessed;
    private T    value;

    public CacheValue(T value) {
      this.value = value;
      lastAccessed = System.currentTimeMillis();
    }

    public boolean isExpired() {
      return timeToLive > 0L && lastAccessed + timeToLive < System.currentTimeMillis();
    }

    public T getValue() {
      lastAccessed = System.currentTimeMillis();
      return value;
    }

    @Override
    public String toString() {
      return value == null ? null : String.valueOf(value);
    }

  }

  /**
   * Provide an interface for saving cached values within the put lock. Allows implementing a write
   * through cache. This will block other gets and puts but will keep everything synchronized.
   * 
   * @author wstrater
   *
   * @param <K>
   * @param <T>
   */
  public static interface CacheWriter<K, T> {

    /**
     * Assumes value is written so throw a {@link RuntimeException} if not.
     * 
     * @param key
     * @param value
     */
    public void write(K key, T value);

  }

}