package io.immutables.build;

import java.util.concurrent.CompletableFuture;

/// Target acting "wrapper" around synchronous or asynchronous operation,
/// usually memoising its result (lazy execution). The underlying mechanisms are used
/// to dispatch the work using fiber threads, provide locking and synchronization of
/// async executions and tracking throughout the build system.
public interface Target<T> {
  CompletableFuture<T> asFuture();

  /// blocking get
  default T get() {
    return asFuture().join();
  }
}
