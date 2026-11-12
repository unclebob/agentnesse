package fitnesse.slim.converters;

import org.junit.Test;

import fitnesse.slim.Converter;
import static org.junit.Assert.*;

public abstract class AbstractConverterTest<T, C extends Converter<T>> {
  protected final C converter;

  public AbstractConverterTest(C converter) {
    this.converter = converter;
  }

  /*
   * TO STRING
   */
  @Test
  public void toString_should_return_null_string_when_value_is_not_defined() {
    T value = null;

    String current = converter.toString(value);

    assertEquals(Converter.NULL_VALUE, current);
  }
}
