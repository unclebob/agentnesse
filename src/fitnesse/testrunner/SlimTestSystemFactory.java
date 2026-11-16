package fitnesse.testrunner;

import fitnesse.testsystems.Descriptor;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemFactory;
import fitnesse.testsystems.slim.HtmlSlimTestSystem;
import fitnesse.testsystems.slim.InProcessSlimClientBuilder;
import fitnesse.testsystems.slim.SlimClientBuilder;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

public final class SlimTestSystemFactory implements TestSystemFactory {
  private final SlimTableFactory tables;
  private final ClassLoader classLoader;

  public SlimTestSystemFactory(SlimTableFactory tables, ClassLoader classLoader) {
    this.tables = tables;
    this.classLoader = classLoader;
  }

  @Override
  public TestSystem create(Descriptor descriptor) {
    if (descriptor.runInProcess()) {
      return inProcess(descriptor);
    }
    return external(descriptor);
  }

  private TestSystem external(Descriptor descriptor) {
    SlimClientBuilder clientBuilder = new SlimClientBuilder(descriptor);
    return new HtmlSlimTestSystem(clientBuilder.getTestSystemName(), clientBuilder.build(), tables.copy());
  }

  private TestSystem inProcess(Descriptor descriptor) {
    InProcessSlimClientBuilder clientBuilder = new InProcessSlimClientBuilder(descriptor, classLoader);
    return new HtmlSlimTestSystem(clientBuilder.getTestSystemName(), clientBuilder.build(), tables.copy());
  }
}
