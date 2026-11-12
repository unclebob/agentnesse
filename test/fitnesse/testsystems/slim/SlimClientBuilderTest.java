package fitnesse.testsystems.slim;

import fitnesse.testsystems.ClassPath;
import fitnesse.testsystems.CommandRunner;
import fitnesse.testsystems.Descriptor;
import fitnesse.testsystems.MockCommandRunner;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlimClientBuilderTest {

  @Before
  public void setUp() throws Exception {
    // Enforce the test runner here, to make sure we're talking to the right system
    SlimClientBuilder.clearSlimPortOffset();
  }

  @Test
  public void standardPortWillNotRotate() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn(null);
    for (int i = 0; i < 15; i++) {
      SlimClientBuilder clientBuilder = new SlimClientBuilder(descriptor);
      assertEquals(1, clientBuilder.getSlimPort());
    }
  }

  @Test
  public void portStartsAtSlimPortVariableAndRotates() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("9000");
    for (int i = 0; i < 15; i++) {
      SlimClientBuilder clientBuilder = new SlimClientBuilder(descriptor);
      assertEquals(9000 + (i % 10), clientBuilder.getSlimPort());
    }
  }
  @Test
  public void portStartsAtSlimPortEnvironmentVariable() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("slim.port")).thenReturn("9000");
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("1313");
    for (int i = 0; i < 15; i++) {
      SlimClientBuilder clientBuilder = new SlimClientBuilder(descriptor);
      assertEquals(9000 + (i % 10), clientBuilder.getSlimPort());
    }
  }

  @Test
  public void badSlimPortVariableDefaults() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("BOB");
    for (int i = 0; i < 15; i++)
      assertEquals(1, new SlimClientBuilder(descriptor).getSlimPort());
  }

  @Test
  public void slimPortPoolSizeCanBeModified() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("9000");
    when(descriptor.getVariable("slim.pool.size")).thenReturn("20");
    for (int i = 0; i < 25; i++)
      assertEquals(9000 + (i % 20),
          new SlimClientBuilder(descriptor).getSlimPort());
  }

  @Test
  public void slimHostDefaultsTolocalhost() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    assertEquals("localhost", new SlimClientBuilder(descriptor).determineSlimHost());
  }

  @Test
  public void slimHostVariableSetsTheHost() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_HOST")).thenReturn("somehost");
    assertEquals("somehost", new SlimClientBuilder(descriptor).determineSlimHost());
  }

  @Test
  public void slimVersionVariableSetsRequiredVersion() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_VERSION")).thenReturn("0.0");
    assertEquals(0.0, new SlimClientBuilder(descriptor).getSlimVersion(), 0.000001);
  }

  @Test
  public void slimHostVariableSetsTheHostEnvironmentVariable() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("slim.host")).thenReturn("somehost");
    when(descriptor.getVariable("SLIM_HOST")).thenReturn("notThisHost");
    assertEquals("somehost", new SlimClientBuilder(descriptor).determineSlimHost());
  }

  @Test
  public void slimDefaultTimeoutIs10Seconds() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("slim.debug.timeout")).thenReturn("30");
    assertEquals(10, new SlimClientBuilder(descriptor).determineTimeout());
  }

  @Test
  public void manualStartConnectsToARunningSlimService() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("9001");
    when(descriptor.getVariable("MANUALLY_START_TEST_RUNNER")).thenReturn("true");
    CommandRunner runner = new SlimClientBuilder(descriptor).determineCommandRunner();
    assertTrue(runner instanceof MockCommandRunner);
  }

  @Test
  public void manualStartOnDebugUsesTheDebugVariable() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.isDebug()).thenReturn(true);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("9002");
    when(descriptor.getVariable("manually.start.test.runner.on.debug")).thenReturn("true");
    CommandRunner runner = new SlimClientBuilder(descriptor).determineCommandRunner();
    assertTrue(runner instanceof MockCommandRunner);
  }

  @Test
  public void manualStartIsOffWhenTheVariableIsNotTrue() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.getVariable("SLIM_PORT")).thenReturn("9003");
    when(descriptor.getVariable("MANUALLY_START_TEST_RUNNER")).thenReturn("false");
    when(descriptor.getClassPath()).thenReturn(new ClassPath(Collections.singletonList("."), System.getProperty("path.separator")));
    CommandRunner runner = new SlimClientBuilder(descriptor).determineCommandRunner();
    assertFalse(runner instanceof MockCommandRunner);
  }

  @Test
  public void slimDebugTimeoutIsUsedWhenExecutingWithDebugMode() throws Exception {
    Descriptor descriptor = mock(Descriptor.class);
    when(descriptor.isDebug()).thenReturn(true);
    when(descriptor.getVariable("slim.debug.timeout")).thenReturn("30");
    assertEquals(30, new SlimClientBuilder(descriptor).determineTimeout());
  }

}
