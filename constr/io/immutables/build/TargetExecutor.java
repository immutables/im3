package io.immutables.build;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public interface TargetExecutor {
  /// Sync execution will not use any new (fiber) threads, but will computed by first calling
  /// thread on that thread
  <T> Target<T> sync(String target, Callable<T> supplier);

  /// Will use new (fiber) thread to execute callable once
  <T> Target<T> async(String target, Callable<T> supplier);

  /// Will use new (fiber) thread to execute runnable once
  Target<Void> async(String target, Runnable runnable);

  /// Callable is invoked synchronously, but returns completable future,
  /// which is computed asynchronously, oftentimes that future is a composition
  /// of futures coming from result of other targets.
  <T> Target<T> future(String target, Callable<CompletableFuture<T>> supplier);
}
