package agnesse.slim.converters;

import agnesse.slim.Converter;
import agnesse.slim.SlimError;
import agnesse.util.StringUtils;

import java.util.function.Function;

public abstract class ConverterBase<T> implements Converter<T> {
  @Override
  public String toString(T o) {
    return o == null ? NULL_VALUE : getString(o);
  }

  protected String getString(T o) {
    return o.toString();
  }

  @Override
  public T fromString(String arg) {
    return StringUtils.isBlank(arg) ? null : getObject(arg);
  }

  protected abstract T getObject(String arg);

  protected static <T> T parsed(String arg, String label, Function<String, T> parse) {
    try {
      return parse.apply(arg);
    } catch (NumberFormatException e) {
      throw new SlimError(String.format("message:<<Can't convert %s to %s.>>", arg, label), e);
    }
  }
}
