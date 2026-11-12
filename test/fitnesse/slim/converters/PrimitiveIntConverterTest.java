package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.*;

public class PrimitiveIntConverterTest extends IntConverterTestSupport {
  public PrimitiveIntConverterTest() {
    super(new PrimitiveIntConverter());
  }

  @Test
  public void fromString_should_return_default_object_when_value_is_not_defined() {
    String value = "";

    Integer current = converter.fromString(value);

    assertNotNull(current);
    assertEquals(PrimitiveIntConverter.DEFAULT_VALUE, current);
  }
}
