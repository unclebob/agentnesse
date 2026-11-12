package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrimitiveLongConverterTest extends LongConverterTestSupport {
  public PrimitiveLongConverterTest() {
    super(new PrimitiveLongConverter());
  }

  @Test
  public void fromString_should_return_default_object_when_value_is_not_defined() {
    String value = "";

    Long current = converter.fromString(value);

    assertNotNull(current);
    assertEquals(PrimitiveLongConverter.DEFAULT_VALUE, current);
  }
}
