package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrimitiveCharConverterTest extends CharConverterTestSupport {

  public PrimitiveCharConverterTest() {
    super(new PrimitiveCharConverter());
  }

  @Test
  public void fromString_should_return_default_object_when_value_is_not_defined() {
    String value = "";

    Character current = converter.fromString(value);

    assertNotNull(current);
    assertEquals(PrimitiveCharConverter.DEFAULT_VALUE, current);
  }
}
