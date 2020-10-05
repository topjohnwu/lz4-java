package net.jpountz.lz4;

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

import java.util.Arrays;

import static net.jpountz.lz4.LZ4Constants.DEFAULT_COMPRESSION_LEVEL;
import static net.jpountz.lz4.LZ4Constants.MAX_COMPRESSION_LEVEL;

/**
 * Entry point for the LZ4 API.
 * <p>
 * All methods from this class are very costly, so you should get an instance
 * once, and then reuse it whenever possible. This is typically done by storing
 * a {@link LZ4Factory} instance in a static field.
 */
public final class LZ4Factory {

  private static LZ4Factory INSTANCE;

  /**
   * Returns a {@link LZ4Factory} instance that returns compressors and
   * decompressors that are written with Java's official API.
   *
   * @return a {@link LZ4Factory} instance that returns compressors and
   * decompressors that are written with Java's official API.
   */
  public static LZ4Factory getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new LZ4Factory();
    }
    return INSTANCE;
  }

  private final LZ4Compressor fastCompressor;
  private final LZ4Compressor highCompressor;
  private final LZ4FastDecompressor fastDecompressor;
  private final LZ4SafeDecompressor safeDecompressor;
  private final LZ4Compressor[] highCompressors = new LZ4Compressor[MAX_COMPRESSION_LEVEL+1];

  private LZ4Factory() throws SecurityException, IllegalArgumentException {
    fastCompressor = new LZ4JavaSafeCompressor();
    highCompressor = new LZ4HCJavaSafeCompressor();
    fastDecompressor = new LZ4JavaSafeFastDecompressor();
    safeDecompressor = new LZ4JavaSafeSafeDecompressor();
    highCompressors[DEFAULT_COMPRESSION_LEVEL] = highCompressor;
    for (int level = 1; level <= MAX_COMPRESSION_LEVEL; level++) {
      if(level == DEFAULT_COMPRESSION_LEVEL) continue;
      highCompressors[level] = new LZ4HCJavaSafeCompressor(level);
    }

    // quickly test that everything works as expected
    final byte[] original = new byte[] {'a','b','c','d',' ',' ',' ',' ',' ',' ','a','b','c','d','e','f','g','h','i','j'};
    for (LZ4Compressor compressor : Arrays.asList(fastCompressor, highCompressor)) {
      final int maxCompressedLength = compressor.maxCompressedLength(original.length);
      final byte[] compressed = new byte[maxCompressedLength];
      final int compressedLength = compressor.compress(original, 0, original.length, compressed, 0, maxCompressedLength);
      final byte[] restored = new byte[original.length];
      fastDecompressor.decompress(compressed, 0, restored, 0, original.length);
      if (!Arrays.equals(original, restored)) {
        throw new AssertionError();
      }
      Arrays.fill(restored, (byte) 0);
      final int decompressedLength = safeDecompressor.decompress(compressed, 0, compressedLength, restored, 0);
      if (decompressedLength != original.length || !Arrays.equals(original, restored)) {
        throw new AssertionError();
      }
    }

  }

  /**
   * Returns a blazing fast {@link LZ4Compressor}.
   *
   * @return a blazing fast {@link LZ4Compressor}
   */
  public LZ4Compressor fastCompressor() {
    return fastCompressor;
  }

  /**
   * Returns a {@link LZ4Compressor} which requires more memory than
   * {@link #fastCompressor()} and is slower but compresses more efficiently.
   *
   * @return a {@link LZ4Compressor} which requires more memory than
   * {@link #fastCompressor()} and is slower but compresses more efficiently.
   */
  public LZ4Compressor highCompressor() {
    return highCompressor;
  }

  /**
   * Returns a {@link LZ4Compressor} which requires more memory than
   * {@link #fastCompressor()} and is slower but compresses more efficiently.
   * The compression level can be customized.
   * <p>For current implementations, the following is true about compression level:<ol>
   *   <li>It should be in range [1, 17]</li>
   *   <li>A compression level higher than 17 would be treated as 17.</li>
   *   <li>A compression level lower than 1 would be treated as 9.</li>
   * </ol>
   * Note that compression levels from different implementations
   * (native, unsafe Java, and safe Java) cannot be compared with one another.
   * Specifically, the native implementation of a high compression level
   * is not necessarily faster than the safe/unsafe Java implementation
   * of the same compression level.
   *
   * @param compressionLevel the compression level between [1, 17]; the higher the level, the higher the compression ratio
   * @return a {@link LZ4Compressor} which requires more memory than
   * {@link #fastCompressor()} and is slower but compresses more efficiently.
   */
  public LZ4Compressor highCompressor(int compressionLevel) {
    if(compressionLevel > MAX_COMPRESSION_LEVEL) {
      compressionLevel = MAX_COMPRESSION_LEVEL;
    } else if (compressionLevel < 1) {
      compressionLevel = DEFAULT_COMPRESSION_LEVEL;
    }
    return highCompressors[compressionLevel];
  }

  /**
   * Returns a {@link LZ4FastDecompressor} instance.
   *
   * @return a {@link LZ4FastDecompressor} instance
   *
   */
  public LZ4FastDecompressor fastDecompressor() {
    return fastDecompressor;
  }

  /**
   * Returns a {@link LZ4SafeDecompressor} instance.
   *
   * @return a {@link LZ4SafeDecompressor} instance
   */
  public LZ4SafeDecompressor safeDecompressor() {
    return safeDecompressor;
  }

}
