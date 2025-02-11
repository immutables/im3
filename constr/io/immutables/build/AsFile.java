package io.immutables.build;

import io.immutables.meta.Null;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.google.common.hash.HashCode;

public interface AsFile {

  Path toPath();

  /// Filename, exclude any directories. Includes any extension etc.
  String filename();

  interface Text extends AsFile, Hashed {
    String getContent();
  }

  interface Binary extends AsFile, Hashed {
    byte[] getBytes();
  }

  static Text text(Path file) {
    return new Text() {
      private final ReentrantLock lock = new ReentrantLock();
      private @Null String content;
      private @Null HashCode hash;

      @Override public Path toPath() {
        return file;
      }

      @Override public String filename() {
        return file.getFileName().toString();
      }

      @Override public String getContent() {
        lockInterruptibly(lock);
        try {
          if (content == null) {
            // this defaults to UTF-8
            content = Files.readString(file);
          }
          return content;
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        } finally {
          lock.unlock();
        }
      }

      @Override public HashCode hash() {
        // TODO maybe better to populate hash while reading content
        lockInterruptibly(lock);
        try {
          if (hash == null) {
            hash = Function.hashString(getContent(), StandardCharsets.UTF_8);
          }
          return hash;
        } finally {
          lock.unlock();
        }
      }
    };
  }

  static Text text(
//      Category category,
      String filename,
      String content) {
    throw new UnsupportedOperationException();
  }

  static Binary binary(Path file) {
    return new Binary() {
      private final ReentrantLock lock = new ReentrantLock();
      private @Null byte[] bytes;
      private @Null HashCode hash;

      @Override public Path toPath() {
        return file;
      }

      @Override public String filename() {
        return file.getFileName().toString();
      }

      @Override public byte[] getBytes() {
        lockInterruptibly(lock);
        try {
          if (bytes == null) {
            bytes = Files.readAllBytes(file);
          }
          return bytes;
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        } finally {
          lock.unlock();
        }
      }

      @Override public HashCode hash() {
        // TODO maybe better to populate hash while reading content
        lockInterruptibly(lock);
        try {
          if (hash == null) {
            hash = Function.hashBytes(getBytes());
          }
          return hash;
        } finally {
          lock.unlock();
        }
      }
    };
  }

  static Binary binary(
//      Category category,
      String filename,
      byte[] bytes) {
    throw new UnsupportedOperationException();
  }

  private static void lockInterruptibly(Lock lock) {
    try {
      // do we need such a fancy-ness of using interruptible lock,
      // supposedly to be able to cancel execution while waiting
      // for a long file read
      lock.lockInterruptibly();
    } catch (InterruptedException ex) {
      throw new UncheckedIOException(
          "interrupted while waiting for lock", new IOException());
    }
  }
}
