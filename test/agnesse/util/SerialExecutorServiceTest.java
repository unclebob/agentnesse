package agnesse.util;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SerialExecutorServiceTest {
  private final SerialExecutorService service = new SerialExecutorService();

  @Test
  public void tasksRunImmediatelyOnTheCallingThread() throws Exception {
    Future<String> done = service.submit(() -> "ok");
    assertEquals("ok", done.get());
    assertEquals("ok", done.get(1, TimeUnit.MILLISECONDS));
    assertTrue(done.isDone());
    assertTrue(!done.isCancelled());
    assertTrue(!done.cancel(true));

    Future<String> failed = service.submit(() -> {
      throw new IllegalStateException("no");
    });
    try {
      failed.get();
      fail("expected the task failure");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalStateException);
    }

    assertEquals("ran", service.submit(() -> { }, "ran").get());
    assertNull(service.submit(() -> { }).get());
    service.execute(() -> { });
  }

  @Test
  public void theServiceIsAlreadyFinishedAndRefusesBulkCalls() throws Exception {
    service.shutdown();
    assertTrue(service.isShutdown());
    assertTrue(service.isTerminated());
    assertTrue(service.awaitTermination(1, TimeUnit.SECONDS));
    assertTrue(service.shutdownNow().isEmpty());
    assertUnsupported(() -> service.invokeAll(List.of()));
    assertUnsupported(() -> service.invokeAll(List.of(), 1, TimeUnit.SECONDS));
    assertUnsupported(() -> service.invokeAny(List.of()));
    assertUnsupported(() -> service.invokeAny(List.of(), 1, TimeUnit.SECONDS));
  }

  private static void assertUnsupported(Call call) throws Exception {
    try {
      call.run();
      fail("expected the bulk call to be refused");
    } catch (UnsupportedOperationException expected) {
      assertTrue(true);
    }
  }

  private interface Call {
    void run() throws Exception;
  }
}
