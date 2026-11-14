package fitnesse.junit;

import fitnesse.ConfigurationParameter;
import fitnesse.ContextConfigurator;
import fitnesse.FitNesseContext;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testrunner.SuiteContentsFinder;
import fitnesse.testrunner.run.TestRun;
import fitnesse.testsystems.ConsoleExecutionLogListener;
import fitnesse.testsystems.TestExecutionException;
import fitnesse.testsystems.TestSummary;
import fitnesse.wiki.WikiPage;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fitnesse.junit.JUnitHelper.createTestRunner;
import static fitnesse.junit.JUnitHelper.getSuiteRootPage;
import static fitnesse.junit.JUnitHelper.msgAtLeastOneTest;
import static org.junit.Assert.assertTrue;

public class FitNesseRunner extends ParentRunner<WikiPage> {
  /**
   * The <code>Suite</code> annotation specifies the name of the Fitnesse suite
   * (or single page) to be run, e.g.: MySuite.MySubSuite
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Suite {

    String value() default "";
    String systemProperty() default "";
  }
  /**
   * The <code>SuiteFilter</code> annotation specifies the suite filter of the Fitnesse suite
   * to be run, e.g.: fasttests
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface SuiteFilter {

    String value() default "";
    String systemProperty() default "";
    boolean andStrategy() default false;
  }
  /**
   * The <code>ExcludeSuiteFilter</code> annotation specifies a filter for excluding tests from the Fitnesse suite
   * to be run, e.g.: slowtests
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface ExcludeSuiteFilter {

    String value();
    String systemProperty() default "";
  }
  /**
   * The <code>FitnesseDir</code> annotation specifies the absolute or relative
   * path to the directory in which FitNesseRoot can be found. You can either specify
   * <ul>
   * <li>a relative or absolute path directly, e.g.: <code>@FitnesseDir("/parentOfFitNesseRoot")</code>,
   * or you can specify
   * <li>a system property the content of which will be taken as base dir and
   * optionally give a path extension, e.g.:
   * <code>@FitnesseDir(systemProperty = "fitnesse.root.dir.parent")</code></li>
   * </ul>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface FitnesseDir {

    String value() default "";
    String systemProperty() default "";
    String fitNesseRoot() default ContextConfigurator.DEFAULT_ROOT;
  }
  /**
   * The <code>OutputDir</code> annotation specifies where the html reports of
   * run suites and tests will be found after running them. You can either specify
   * <ul>
   * <li>a relative or absolute path directly, e.g.: <code>@OutputDir("/tmp/trinidad-results")</code>,
   * or you can specify
   * <li>a system property the content of which will be taken as base dir and
   * optionally give a path extension, e.g.:
   * <code>@OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "trinidad-results")</code></li>
   * </ul>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface OutputDir {

    String value() default "";
    String systemProperty() default "";

    String pathExtension() default "";

  }
  /**
   * The <code>Port</code> annotation specifies the port used by the FitNesse
   * server. Default is the standard FitNesse port.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Deprecated
  public @interface Port {

    int value() default 0;
    String systemProperty() default "";

  }
  private Class<?> suiteClass;
  private String suiteName;
  private String outputDir;
  private String suiteFilter;
  private boolean suiteFilterAndStrategy;
  private String excludeSuiteFilter;
  private boolean debugMode;
  private FitNesseContext context;
  private DescriptionFactory descriptionFactory;
  private TestRun testRun;
  private List<WikiPage> children;

  public FitNesseRunner(Class<?> suiteClass) throws InitializationError {
    super(suiteClass);
    descriptionFactory = new DescriptionFactory();
  }

  @Override
  protected void collectInitializationErrors(List<Throwable> errors) {
    // called by superclass' constructor
    super.collectInitializationErrors(errors);

    this.suiteClass = getTestClass().getJavaClass();
    readSuiteIdentity(errors);
    readRunOptions(errors);
  }

  private void readSuiteIdentity(List<Throwable> errors) {
    try {
      this.suiteName = getSuiteName(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      this.outputDir = getOutputDir(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      this.suiteFilter = getSuiteFilter(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      this.suiteFilterAndStrategy = getSuiteFilterAndStrategy(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
  }

  private void readRunOptions(List<Throwable> errors) {
    try {
      this.excludeSuiteFilter = getExcludeSuiteFilter(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
    try {
      this.context = createContext(suiteClass);
    } catch (Exception e) {
      errors.add(e);
    }
  }

  protected FitNesseContext createContext(Class<?> suiteClass) throws Exception {

    return initContextConfigurator().makeFitNesseContext();
  }

  protected String getSuiteName(Class<?> klass) throws InitializationError {
    Suite suiteAnnotation = klass.getAnnotation(Suite.class);
    if (suiteAnnotation == null) {
      throw new InitializationError("There must be a @Suite annotation");
    }
    return requiredSetting(suiteAnnotation.value(), suiteAnnotation.systemProperty(),
      "In annotation @Suite you have to specify either 'value' or 'systemProperty'");
  }

  protected String getOutputDir(Class<?> klass) throws InitializationError {
    OutputDir outputDirAnnotation = klass.getAnnotation(OutputDir.class);
    if (outputDirAnnotation == null) {
      throw new InitializationError("There must be a @OutputDir annotation");
    }
    return requiredSetting(outputDirAnnotation.value(), outputDirAnnotation.systemProperty(),
      "In annotation @OutputDir you have to specify either 'value' or 'systemProperty'",
      property -> new File(System.getProperty(property), outputDirAnnotation.pathExtension()).getAbsolutePath());
  }

  protected String getSuiteFilter(Class<?> klass) throws Exception {
    return optionalFilter(klass.getAnnotation(SuiteFilter.class), SuiteFilter::value, SuiteFilter::systemProperty, "@SuiteFilter");
  }

  protected boolean getSuiteFilterAndStrategy(Class<?> klass) throws Exception {
    SuiteFilter suiteFilterAnnotation = klass.getAnnotation(SuiteFilter.class);
    if (suiteFilterAnnotation == null) {
      return false;
    }
    return suiteFilterAnnotation.andStrategy();
  }

  protected String getExcludeSuiteFilter(Class<?> klass) throws Exception {
    return optionalFilter(klass.getAnnotation(ExcludeSuiteFilter.class), ExcludeSuiteFilter::value, ExcludeSuiteFilter::systemProperty, "@ExcludeSuiteFilter");
  }

  protected String getFitNesseDir(Class<?> klass)
          throws InitializationError {
    FitnesseDir fitnesseDirAnnotation = klass.getAnnotation(FitnesseDir.class);
    if (fitnesseDirAnnotation == null) {
      throw new InitializationError("There must be a @FitnesseDir annotation");
    }
    return requiredSetting(fitnesseDirAnnotation.value(), fitnesseDirAnnotation.systemProperty(),
      "In annotation @FitnesseDir you have to specify either 'value' or 'systemProperty'",
      property -> new File(System.getProperty(property)).getAbsolutePath());
  }

  private <A> String optionalFilter(A annotation, java.util.function.Function<A, String> value,
                                   java.util.function.Function<A, String> property, String name) throws Exception {
    if (annotation == null) {
      return null;
    }
    return requiredSetting(value.apply(annotation), property.apply(annotation),
      "In annotation " + name + " you have to specify either 'value' or 'systemProperty'");
  }

  private String requiredSetting(String value, String systemProperty, String message) throws InitializationError {
    return requiredSetting(value, systemProperty, message, System::getProperty);
  }

  private String requiredSetting(String value, String systemProperty, String message,
                                 java.util.function.Function<String, String> fromProperty) throws InitializationError {
    if (!"".equals(value)) {
      return value;
    }
    if (!"".equals(systemProperty)) {
      return fromProperty.apply(systemProperty);
    }
    throw new InitializationError(message);
  }

  protected String getFitNesseRoot(Class<?> klass) {
    FitnesseDir fitnesseDirAnnotation = klass.getAnnotation(FitnesseDir.class);
    return fitnesseDirAnnotation.fitNesseRoot();
  }

  public int getPort(Class<?> klass) {
    Port portAnnotation = klass.getAnnotation(Port.class);
    if (null == portAnnotation) {
      return 0;
    }
    int lport = portAnnotation.value();
    if (!"".equals(portAnnotation.systemProperty())) {
      lport = Integer.getInteger(portAnnotation.systemProperty(), lport);
    }
    return lport;
  }

  protected File getConfigFile(String rootPath) {
    return new File(rootPath, ContextConfigurator.DEFAULT_CONFIG_FILE);
  }

  @Override
  protected Description describeChild(WikiPage child) {
    return getDescriptionFactory().createDescription(suiteClass, child);
  }

  protected TestRun getTestRun() {
    if (this.testRun == null) {
      List<WikiPage> allChildren = initChildren();
      this.testRun = createTestRun(allChildren);
    }
    return this.testRun;
  }

  @Override
  protected List<WikiPage> getChildren() {
    if (this.children == null) {
      this.children = getTestRun().getPages();
    }
    return this.children;
  }

  @Override
  public void run(final RunNotifier notifier) {
    TestRun run = getTestRun();
    runPages(run, notifier);
  }

  @Override
  protected void runChild(WikiPage page, RunNotifier notifier) {
    runPages(listOf(page), notifier);
  }

  protected void runPages(List<WikiPage> pages, final RunNotifier notifier) {
    TestRun run = createTestRun(pages);
    runPages(run, notifier);
  }

  protected void runPages(TestRun run, final RunNotifier notifier) {
    MultipleTestsRunner testRunner = createTestRunner(run, context, debugMode);
    addTestSystemListeners(notifier, testRunner, suiteClass, getDescriptionFactory());
    addExecutionLogListener(notifier, testRunner, suiteClass);
    try {
      executeTests(testRunner);
    } catch (AssertionError | Exception e) {
      Description description = getDescriptionFactory().createSuiteDescription(suiteClass);
      notifier.fireTestFailure(new Failure(description, e));
    }
  }

  protected TestRun createTestRun(List<WikiPage> pages) {
    return JUnitHelper.createTestRun(context, pages);
  }

  protected void addTestSystemListeners(RunNotifier notifier, MultipleTestsRunner testRunner, Class<?> suiteClass,
                                        DescriptionFactory descriptionFactory) {
    testRunner.addTestSystemListener(new JUnitRunNotifierResultsListener(notifier, suiteClass, descriptionFactory));
  }

  protected void addExecutionLogListener(RunNotifier notifier, MultipleTestsRunner testRunner, Class<?> suiteClass) {
    testRunner.addExecutionLogListener(new ConsoleExecutionLogListener());
  }

  protected List<WikiPage> initChildren() {
    WikiPage suiteRoot = getSuiteRootPage(suiteName, context, Collections.emptyMap());
    if (suiteRoot == null) {
      throw new IllegalArgumentException("No page " + this.suiteName);
    }
    List<WikiPage> children;
    if (suiteRoot.getData().hasAttribute("Suite")) {
      SuiteContentsFinder contentsFinder = new SuiteContentsFinder(suiteRoot, getSuiteFilter(), context.getRootPage());
      return contentsFinder.getAllPagesToRunForThisSuite();
    } else {
      children = Collections.singletonList(suiteRoot);
    }
    return children;
  }

  private fitnesse.testrunner.SuiteFilter getSuiteFilter() {
    return new fitnesse.testrunner.SuiteFilter(getOrSuiteFilter(), excludeSuiteFilter, getAndSuiteFilter(), null);
  }

  private String getOrSuiteFilter() {
    return suiteFilterAndStrategy ? null : suiteFilter;
  }

  private String getAndSuiteFilter() {
    return suiteFilterAndStrategy ? suiteFilter : null;
  }

  protected ContextConfigurator initContextConfigurator() throws InitializationError {
    String rootPath = getFitNesseDir(suiteClass);
    String fitNesseRoot = getFitNesseRoot(suiteClass);
    int port = getPort(suiteClass);
    File configFile = getConfigFile(rootPath);

    ContextConfigurator configurator = ContextConfigurator.systemDefaults();
    boolean configRequired = false;
    try {
      fitnesse.AgentNesseConfig.apply(configurator, configFile, configRequired);
    } catch (IllegalArgumentException e) {
      throw new InitializationError(e.getMessage());
    }
    return configurator
      .updatedWith(System.getProperties())
      .updatedWith(ConfigurationParameter.makeProperties(
            ConfigurationParameter.PORT, port,
            ConfigurationParameter.ROOT_PATH, rootPath,
            ConfigurationParameter.ROOT_DIRECTORY, fitNesseRoot));
  }

  private void executeTests(MultipleTestsRunner testRunner) throws IOException, TestExecutionException {
    fitnesse.results.JsonResultsFormatter results = new fitnesse.results.JsonResultsFormatter(new StringBuilder(), false, null);
    testRunner.addTestSystemListener(results);
    testRunner.addExecutionLogListener(results);
    testRunner.executeTestPages();
    results.finish();
    fitnesse.results.JunitXmlWriter.write(results.document(), new File(outputDir, suiteName + ".xml").toPath());
    TestSummary summary = fitnesse.results.RunDocument.countsOf(
      results.document().getJSONObject("run").getJSONObject("counts"));
    assertTrue(msgAtLeastOneTest(suiteName, summary), summary.getRight() > 0 || summary.getWrong() > 0 || summary.getExceptions() > 0);
  }

  private List<WikiPage> listOf(WikiPage page) {
    List<WikiPage> list = new ArrayList<>(1);
    list.add(page);
    return list;
  }

  public DescriptionFactory getDescriptionFactory() {
    return descriptionFactory;
  }

  public void setDescriptionFactory(DescriptionFactory descriptionFactory) {
    this.descriptionFactory = descriptionFactory;
  }
}
