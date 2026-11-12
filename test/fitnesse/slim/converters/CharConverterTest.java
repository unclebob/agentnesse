package fitnesse.slim.converters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CharConverterTest extends CharConverterTestSupport {

  public CharConverterTest() {
    super(new CharConverter());
  }

  @Test
  public void fromString_should_return_null_object_when_value_is_not_defined() {
    String value = "";

    Character current = converter.fromString(value);

    assertNull(current);
  }
}

abstract class CharConverterTestSupport extends AbstractConverterTest<Character, CharConverter> {

  protected CharConverterTestSupport(CharConverter charConverter) {
    super(charConverter);
  }

  /*
   * TO STRING
   */
  @Test
  public void toString_should_return_string_which_contains_the_char_when_value_is_a_char() {
    Character value = Character.valueOf('a');

    String current = converter.toString(value);

    assertEquals(value.toString(), current);
  }

  /*
   * FROM STRING
   */
  @Test
  public void fromString_should_return_the_char_when_value_is_a_char() {
    String value = "a";

    Character current = converter.fromString(value);

    assertEquals(Character.valueOf('a'), current);
  }
}
