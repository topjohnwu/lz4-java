package net.jpountz.xxhash;

/*
 * Copyright 2020 Adrien Grand and the lz4-java contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Random;

/**
 * Entry point to get {@link XXHash32} and {@link StreamingXXHash32} instances.
 * <p>
 * All methods from this class are very costly, so you should get an instance
 * once, and then reuse it whenever possible. This is typically done by storing
 * a {@link XXHashFactory} instance in a static field.
 */
public final class XXHashFactory {

  private static XXHashFactory INSTANCE;

  /**
   * Returns a {@link XXHashFactory} that returns {@link XXHash32} instances that
   *  are written with Java's official API.
   *
   * @return a {@link XXHashFactory} that returns {@link XXHash32} instances that
   *  are written with Java's official API.
   */
  public static XXHashFactory fastestInstance() {
    if (INSTANCE == null) {
      INSTANCE = new XXHashFactory();
    }
    return INSTANCE;
  }

  private final XXHash32 hash32;
  private final XXHash64 hash64;
  private final StreamingXXHash32.Factory streamingHash32Factory;
  private final StreamingXXHash64.Factory streamingHash64Factory;

  private XXHashFactory() throws SecurityException, IllegalArgumentException {
    hash32 = new XXHash32JavaSafe();
    streamingHash32Factory = new StreamingXXHash32JavaSafe.Factory();
    hash64 = new XXHash64JavaSafe();
    streamingHash64Factory = new StreamingXXHash64JavaSafe.Factory();

    // make sure it can run
    final byte[] bytes = new byte[100];
    final Random random = new Random();
    random.nextBytes(bytes);
    final int seed = random.nextInt();

    final int h1 = hash32.hash(bytes, 0, bytes.length, seed);
    final StreamingXXHash32 streamingHash32 = newStreamingHash32(seed);
    streamingHash32.update(bytes, 0, bytes.length);
    final int h2 = streamingHash32.getValue();
    final long h3 = hash64.hash(bytes, 0, bytes.length, seed);
    final StreamingXXHash64 streamingHash64 = newStreamingHash64(seed);
    streamingHash64.update(bytes, 0, bytes.length);
    final long h4 = streamingHash64.getValue();
    if (h1 != h2) {
      throw new AssertionError();
    }
    if (h3 != h4) {
      throw new AssertionError();
    }
  }

  /**
   * Returns a {@link XXHash32} instance.
   *
   * @return a {@link XXHash32} instance.
   */
  public XXHash32 hash32() {
    return hash32;
  }

  /**
   * Returns a {@link XXHash64} instance.
   *
   * @return a {@link XXHash64} instance.
   */
  public XXHash64 hash64() {
    return hash64;
  }

  /**
   * Return a new {@link StreamingXXHash32} instance.
   *
   * @param seed the seed to use
   * @return a {@link StreamingXXHash32} instance
   */
  public StreamingXXHash32 newStreamingHash32(int seed) {
    return streamingHash32Factory.newStreamingHash(seed);
  }

  /**
   * Return a new {@link StreamingXXHash64} instance.
   *
   * @param seed the seed to use
   * @return a {@link StreamingXXHash64} instance
   */
  public StreamingXXHash64 newStreamingHash64(long seed) {
    return streamingHash64Factory.newStreamingHash(seed);
  }

}
