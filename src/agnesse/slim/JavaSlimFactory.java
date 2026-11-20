package agnesse.slim;

import agnesse.slim.fixtureInteraction.DefaultInteraction;
import agnesse.slim.fixtureInteraction.FixtureInteraction;

import java.util.concurrent.Executors;

public class JavaSlimFactory extends SlimFactory {

  private final NameTranslator identityTranslator = new NameTranslatorIdentity();
  private final Long timeoutMillis;
  private final String timeoutLabel;
  private final boolean verbose;
  private final boolean share;
  private final FixtureInteraction interaction;
  private SlimExecutionContext shared;

  private JavaSlimFactory(FixtureInteraction interaction, Long timeoutMillis, String timeoutLabel, boolean verbose, boolean share) {
    this.interaction = interaction;
    this.timeoutMillis = timeoutMillis;
    this.timeoutLabel = timeoutLabel;
    this.verbose = verbose;
    this.share = share;
  }

  @Override
  public StatementExecutorInterface getStatementExecutor() {
    StatementExecutorInterface statementExecutor = new StatementExecutor(execution());
    if (timeoutMillis != null) {
      statementExecutor = StatementTimeoutExecutor.decorate(statementExecutor, timeoutMillis, timeoutLabel, Executors.newSingleThreadExecutor());
    }
    return statementExecutor;
  }

  @Override
  public NameTranslator getMethodNameTranslator() {
    return getIdentityTranslator();
  }

  @Override
  public boolean isVerbose() {
    return verbose;
  }

  private NameTranslator getIdentityTranslator() {
    return identityTranslator;
  }

  private SlimExecutionContext execution() {
    if (!share) {
      return new SlimExecutionContext(interaction);
    }
    return sharedExecution();
  }

  private synchronized SlimExecutionContext sharedExecution() {
    if (shared == null) {
      shared = new SlimExecutionContext(interaction);
    }
    return shared;
  }

  // Called from main. Daemon mode keeps one fixture visible on every connection.
  public static SlimFactory createJavaSlimFactory(SlimService.Options options) {
    return new JavaSlimFactory(options.interaction, options.statementTimeoutMillis, options.statementTimeoutLabel,
        options.verbose, options.daemon);
  }

  public static SlimFactory createJavaSlimFactory(FixtureInteraction interaction, Integer timeout, boolean verbose) {
    return new JavaSlimFactory(interaction, SlimService.millisFor(timeout), SlimService.labelFor(timeout), verbose, false);
  }

  public static SlimFactory createJavaSlimFactory(FixtureInteraction interaction, long timeoutMillis, String timeoutLabel, boolean verbose) {
    return new JavaSlimFactory(interaction, timeoutMillis, timeoutLabel, verbose, false);
  }

  public static FixtureInteraction createInteraction(String interactionClassName) {
    return createInteraction(interactionClassName, ClassLoader.getSystemClassLoader());
  }

  @SuppressWarnings("unchecked")
  public static FixtureInteraction createInteraction(String interactionClassName, ClassLoader classLoader) {
    if (interactionClassName == null) {
      return new DefaultInteraction();
    }
    try {
      return ((Class<FixtureInteraction>) classLoader.loadClass(interactionClassName)).newInstance();
    } catch (Exception e) {
      throw new SlimError(e);
    }
  }
}
