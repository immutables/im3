package io.immutables.build;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

class TargetExecutionPerModule implements TargetExecutor {
  private final ReentrantLock lock = new ReentrantLock();
  private final String module;

  TargetExecutionPerModule(String module) {
    this.module = module;
  }

/*
  @FunctionalInterface
  public interface ResolvedSupplier1<A> {
    void apply(A a);
  }

  @FunctionalInterface
  public interface ResolvedSupplier2<A, B> {
    void apply(A a);
  }

  @FunctionalInterface
  public interface ResolvedSupplier2<A, B> {
    void apply(A a);
  }
*/

  @Override public <T> Target<T> sync(String target, Callable<T> supplier) {
    return new SyncTarget<>(target, supplier);
  }

  @Override public <T> Target<T> async(String target, Callable<T> supplier) {
    return new AsyncTarget<>(target, supplier);
  }

  @Override public <T> Target<T> future(String target, Callable<CompletableFuture<T>> supplier) {
    return new FutureTarget<>(target, supplier);
  }

  @Override public Target<Void> async(String target, Runnable runnable) {
    return async(target, () -> {
      runnable.run();
      return null;
    });
  }

  private final class SyncTarget<T> extends AbstractTarget<T> {
    private final Callable<T> supplier;

    SyncTarget(String name, Callable<T> supplier) {
      super(name);
      this.supplier = supplier;
    }

    @Override CompletableFuture<T> future() {
      try {
        return CompletableFuture.completedFuture(supplier.call());
      } catch (ExecutionException e) {
        return CompletableFuture.failedFuture(e.getCause());
      } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
      }
    }
  }

  private final class FutureTarget<T> extends AbstractTarget<T> {
    private final Callable<CompletableFuture<T>> supplier;

    FutureTarget(String target, Callable<CompletableFuture<T>> supplier) {
      super(target);
      this.supplier = supplier;
    }

    @Override CompletableFuture<T> future() {
      var future = new CompletableFuture<T>();
      Thread.ofVirtual()
          .name(module + ":" + target)
          .start(() -> {
            try {
              // It might be more elegant way to pipe one
              // completable future into the one we return
              // synchronously for the method
              future.complete(supplier.call().get());
            } catch (ExecutionException e) {
              future.completeExceptionally(e.getCause());
            } catch (Throwable e) {
              future.completeExceptionally(e);
            }
          });
      return future;
    }
  }

  private final class AsyncTarget<T> extends AbstractTarget<T> {
    private final Callable<T> supplier;

    AsyncTarget(String target, Callable<T> supplier) {
      super(target);
      this.supplier = supplier;
    }

    @Override CompletableFuture<T> future() {
      var future = new CompletableFuture<T>();
      Thread.ofVirtual()
          .name(module + ":" + target)
          .start(() -> {
            try {
              future.complete(supplier.call());
            } catch (ExecutionException e) {
              future.completeExceptionally(e.getCause());
            } catch (Throwable e) {
              future.completeExceptionally(e);
            }
          });
      return future;
    }
  }

  private abstract class AbstractTarget<T> implements Target<T> {
    final String target;

    private volatile CompletableFuture<T> future;

    AbstractTarget(String target) {
      this.target = target;
    }

    @Override public CompletableFuture<T> asFuture() {
      if (future == null) {
        lock.lock();
        try {
          if (future == null) {
            future = future();
          }
        } finally {
          lock.unlock();
        }
      }
      return future;
    }

    abstract CompletableFuture<T> future();

    @Override public String toString() {
      return getClass().getSimpleName() + "(" + module + ":" + target + ")";
    }
  }
}
