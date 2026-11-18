package agnesse.slim.converters;

public class PrimitiveBooleanConverter extends BooleanConverter {
  public static final Boolean DEFAULT_VALUE = Boolean.FALSE;

  @Override public Boolean fromString(String arg) { Boolean parsed = super.fromString(arg); return parsed != null ? parsed : DEFAULT_VALUE; }
}
