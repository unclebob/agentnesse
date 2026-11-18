package agnesse.testsystems.slim;

import agnesse.slim.JavaSlimFactory;
import agnesse.slim.SlimServer;
import agnesse.slim.SlimService;
import agnesse.slim.fixtureInteraction.FixtureInteraction;
import agnesse.testsystems.ClientBuilder;
import agnesse.testsystems.Descriptor;

/**
 * In-process version, mainly for testing AgNesse itself.
 */
public class InProcessSlimClientBuilder extends ClientBuilder<SlimClient> {

  private ClassLoader classLoader;

  public InProcessSlimClientBuilder(Descriptor descriptor, ClassLoader classLoader) {
    super(descriptor);
    this.classLoader = classLoader;
  }

  @Override
  public SlimClient build() {
    final SlimService.Options options = SlimService.parseCommandLine(getSlimFlags());
    FixtureInteraction interaction = options != null ? options.interaction : JavaSlimFactory.createInteraction(null, classLoader);

    SlimServer slimServer = options != null && options.statementTimeoutMillis != null
        ? createSlimServer(interaction, options.statementTimeoutMillis, options.statementTimeoutLabel, isDebug())
        : createSlimServer(interaction, options != null ? options.statementTimeout : null, isDebug());
    return new InProcessSlimClient(getTestSystemName(), slimServer, getExecutionLogListener(), classLoader);
  }

  @Override
  protected String defaultTestRunner() {
    return "in-process";
  }

  protected SlimServer createSlimServer(FixtureInteraction interaction, Integer timeout, boolean verbose) {
    return JavaSlimFactory.createJavaSlimFactory(interaction, timeout, verbose).getSlimServer();
  }

  protected SlimServer createSlimServer(FixtureInteraction interaction, long timeoutMillis, String label, boolean verbose) {
    return JavaSlimFactory.createJavaSlimFactory(interaction, timeoutMillis, label, verbose).getSlimServer();
  }

  protected String[] getSlimFlags() { return slimFlags(); }

}
