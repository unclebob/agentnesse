package agnesse.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runners.model.InitializationError;

/**
 * Use agnesse.junit.AgNesseRunner.
 * @deprecated Use agnesse.junit.AgNesseRunner.
 */
@Deprecated
public class AgNesseSuite extends AgNesseRunner {
  /**
   * The <code>Name</code> annotation specifies the name of the AgNesse suite
   * to be run, e.g.: MySuite.MySubSuite
   * @deprecated Use AgNesseRunner.Suite
   */
  @Deprecated
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface Name {

    String value();
  }

  public AgNesseSuite(Class<?> suiteClass) throws InitializationError {
    super(suiteClass);
  }

  @Override
  protected String getSuiteName(Class<?> klass) throws InitializationError {
    Name nameAnnotation = klass.getAnnotation(Name.class);
    if (nameAnnotation == null) {
      throw new InitializationError("There must be a @Name annotation");
    }
    return nameAnnotation.value();
  }
}
