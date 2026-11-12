package fitnesse.slim.converters;

import fitnesse.slim.SlimError;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LongConverterTest extends LongConverterTestSupport {

  public LongConverterTest() {
    super(new LongConverter());
  }

  @Test
  public void fromString_should_return_null_object_when_value_is_not_defined() {
    String value = "";

    Long current = converter.fromString(value);

    assertNull(current);
  }
}

abstract class LongConverterTestSupport extends AbstractConverterTest<Long, LongConverter> {

  protected LongConverterTestSupport(LongConverter longConverter) {
    super(longConverter);
  }

  /*
   * TO STRING
   */
  @Test
  public void toString_should_return_string_which_contains_the_long_when_value_is_a_long() {
    Long value = 31415962l;

    String current = converter.toString(value);

    assertEquals(value.toString(), current);
  }

  /*
   * FROM STRING
   */
  @Test
  public void fromString_should_return_the_long_when_value_is_a_long() {
    String value = "31415962";

    Long current = converter.fromString(value);

    assertEquals(Long.valueOf(value), current);
  }

  @Test
  public void fromString_should_return_throw_Exception_when_value_is_not_a_long() {
    String value = "foo";
    String errorMessage = "no error";

    try {
      converter.fromString(value);
    } catch (SlimError e) {
      errorMessage = e.getMessage();
    }
    assertEquals("message:<<Can't convert foo to long.>>", errorMessage);
  }
}
