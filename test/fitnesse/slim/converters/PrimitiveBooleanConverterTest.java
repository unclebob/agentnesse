package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrimitiveBooleanConverterTest extends BooleanConverterTestSupport {

  public PrimitiveBooleanConverterTest() {
    super(new PrimitiveBooleanConverter());
  }

  @Test
  public void fromString_should_return_false_object_when_value_is_not_defined() {
    String value = "";

    Boolean current = converter.fromString(value);

    assertNotNull(current);
    assertEquals(PrimitiveBooleanConverter.DEFAULT_VALUE, current);
  }
}
