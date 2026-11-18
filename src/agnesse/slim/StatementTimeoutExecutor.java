package agnesse.slim;

import java.util.concurrent.*;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class StatementTimeoutExecutor implements StatementExecutorInterface {
  private final StatementExecutorInterface inner;
  private final long timeoutMillis;
  private final String timeoutLabel;
  private final ExecutorService service;

  private StatementTimeoutExecutor(StatementExecutorInterface inner, long timeoutMillis, String timeoutLabel, ExecutorService service) {
    this.inner = inner;
    this.timeoutMillis = timeoutMillis;
    this.timeoutLabel = timeoutLabel;
    this.service = service;
  }

  public static StatementExecutorInterface decorate(StatementExecutorInterface inner, Integer timeoutSeconds) {
    return decorate(inner, timeoutSeconds, newSingleThreadExecutor());
  }

  public static StatementExecutorInterface decorate(StatementExecutorInterface inner, Integer timeoutSeconds, ExecutorService service) {
    return decorate(inner, timeoutSeconds * 1000L, timeoutSeconds.toString(), service);
  }

  public static StatementExecutorInterface decorate(StatementExecutorInterface inner, long timeoutMillis, String timeoutLabel, ExecutorService service) {
    return new StatementTimeoutExecutor(inner, timeoutMillis, timeoutLabel, service);
  }

  @Override
  public void assign(final String name, final Object value) {
    inner.assign(name, value);
  }

  @Override public Object getSymbol(String symbolName) { return inner.getSymbol(symbolName); }

  @Override
  public Object getSymbolObject(String symbolName) {
    return inner.getSymbolObject(symbolName);
  }

  @Override public Object getInstance(String instanceName) { return inner.getInstance(instanceName); }

  @Override
  public boolean stopHasBeenRequested() {
    return inner.stopHasBeenRequested();
  }

  @Override
  public void reset() {
    inner.reset();
  }

  @Override
  public void setInstance(String actorInstanceName, Object actor) {
    inner.setInstance(actorInstanceName, actor);
  }

  @Override public void addPath(String path) throws SlimException { inner.addPath(path); }

  @Override
  public void create(final String instanceName, final String className, final Object... constructorArgs) throws SlimException {
    Future<?> submit = submit(() -> {
      inner.create(instanceName, className, constructorArgs);
      return null;
    });
    getWithTimeout(submit);
  }

  @Override
  public Object callAndAssign(final String symbolName, final String instanceName, final String methodsName, final Object... arguments) throws SlimException {
    Future<Object> submit = submit(() -> inner.callAndAssign(symbolName, instanceName, methodsName, arguments));
    return getWithTimeout(submit);
  }

  @Override
  public Object call(final String instanceName, final String methodName, final Object... arguments) throws SlimException {
    Future<Object> submit = submit(() -> inner.call(instanceName, methodName, arguments));
    return getWithTimeout(submit);
  }

  private <T> Future<T> submit(Callable<T> task) {
    return service.submit(() -> {
      Thread.interrupted();
      return task.call();
    });
  }

  private <T> T getWithTimeout(Future<T> submit) throws SlimException {
    try {
      return submit.get(timeoutMillis, MILLISECONDS);
    } catch (TimeoutException e) {
      submit.cancel(true);
      throw new SlimException(timeoutLabel, SlimServer.TIMED_OUT, true);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SlimError("Statement execution was interrupted", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SlimException) {
        throw (SlimException) cause;
      } else {
        throw new SlimException(e.getCause());
      }
    }
  }
}
