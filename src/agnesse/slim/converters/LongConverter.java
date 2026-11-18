package agnesse.slim.converters;

public class LongConverter extends ConverterBase<Long> {

  @Override protected Long getObject(String arg) { return parsed(arg, "long", Long::valueOf); }
}
