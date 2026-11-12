package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrimitiveDoubleConverterTest extends DoubleConverterTestSupport {
  public PrimitiveDoubleConverterTest() {
    super(new PrimitiveDoubleConverter());
  }

  @Test
  public void fromString_should_return_default_object_when_value_is_not_defined() {
    String value = "";

    Double current = converter.fromString(value);

    assertNotNull(current);
    assertEquals(PrimitiveDoubleConverter.DEFAULT_VALUE, current);
  }
}
