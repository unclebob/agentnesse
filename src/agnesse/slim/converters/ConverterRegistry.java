package agnesse.slim.converters;

import agnesse.slim.Converter;
import agnesse.slim.converters.beans.JavaBeansConverterGateway;

import java.lang.reflect.ParameterizedType;
import java.util.*;

public class ConverterRegistry {

  private static final Map<Class<?>, Converter<?>> converters = new HashMap<>();
  private static Converter<Object> defaultConverter = new DefaultConverter();

  static {
    addStandardConverters();
  }

  private ConverterRegistry() {
  }

  public static void resetToStandardConverters() {
    converters.clear();
    addStandardConverters();
  }

  protected static void addStandardConverters() {
    addConverter(void.class, new VoidConverter());

    addConverter(String.class, new StringConverter());
    addConverter(Date.class, new DateConverter());

    addConverter(Double.class, new DoubleConverter());
    addConverter(double.class, new PrimitiveDoubleConverter());

    addConverter(Long.class, new LongConverter());
    addConverter(long.class, new PrimitiveLongConverter());

    addConverter(Integer.class, new IntConverter());
    addConverter(int.class, new PrimitiveIntConverter());

    addConverter(Character.class, new CharConverter());
    addConverter(char.class, new PrimitiveCharConverter());

    addConverter(Boolean.class, new BooleanConverter());
    addConverter(boolean.class, new PrimitiveBooleanConverter());

    addConverter(Map.class, new MapConverter());
  }

  public static <T> Converter<T> getConverterForClass(Class<T> clazz) {
    return getConverterForClass(clazz, null);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> Converter<T> getConverterForClass(Class<? extends T> clazz, ParameterizedType typedClazz) {
    Converter<T> registered = converterForRegisteredClass(clazz);
    if (registered != null) return registered;
    Converter<T> fromInterface = getConverterForInterface(clazz);
    if (fromInterface != null) return fromInterface;
    Converter<T> beans = JavaBeansConverterGateway.getConverter(clazz);
    if (beans != null) return beans;
    return converterForShape(clazz, typedClazz);
  }

  @SuppressWarnings("unchecked")
  private static <T> Converter<T> converterForRegisteredClass(Class<?> clazz) {
    if (converters.containsKey(clazz)) return (Converter<T>) converters.get(clazz);
    return converterForSuperclass(clazz);
  }

  @SuppressWarnings("unchecked")
  private static <T> Converter<T> converterForSuperclass(Class<?> clazz) {
    Class<?> superclass = clazz.getSuperclass();
    while (superclass != null && !Object.class.equals(superclass)) {
      if (converters.containsKey(superclass)) return (Converter<T>) converters.get(superclass);
      superclass = superclass.getSuperclass();
    }
    return null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <T> Converter<T> converterForShape(Class<?> clazz, ParameterizedType typedClazz) {
    if (clazz.isEnum()) return new GenericEnumConverter(clazz);
    if (clazz.isArray()) return arrayConverter(clazz);
    if (Collection.class.isAssignableFrom(clazz)) return collectionConverter(clazz, typedClazz);
    return (Converter<T>) converters.get(Object.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> Converter<T> arrayConverter(Class<?> clazz) {
    Class<?> componentType = clazz.getComponentType();
    return new GenericArrayConverter(componentType, getConverterForClassOrDefaultConverter(componentType));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <T> Converter<T> collectionConverter(Class<?> clazz, ParameterizedType typedClazz) {
    Class<?> componentType = typedClazz != null ? (Class<?>) typedClazz.getActualTypeArguments()[0] : String.class;
    return new GenericCollectionConverter(clazz, getConverterForClassOrDefaultConverter(componentType));
  }

  protected static <T> Converter<T> getConverterForInterface(Class<?> clazz) {
    List<Class<?>> parents = new ArrayList<>();
    Converter<T> direct = registeredInterface(clazz.getInterfaces(), parents);
    if (direct != null) return direct;
    Converter<T> parent = parentInterface(parents);
    if (parent != null) return parent;
    return interfaceOnSuperclass(clazz);
  }

  @SuppressWarnings("unchecked")
  private static <T> Converter<T> registeredInterface(Class<?>[] interfaces, List<Class<?>> parents) {
    for (Class<?> interf : interfaces) {
      parents.addAll(Arrays.asList(interf.getInterfaces()));
      if (converters.containsKey(interf)) return (Converter<T>) converters.get(interf);
    }
    return null;
  }

  private static <T> Converter<T> parentInterface(List<Class<?>> parents) {
    for (Class<?> parent : parents) {
      Converter<T> converter = getConverterForInterface(parent);
      if (converter != null) return converter;
    }
    return null;
  }

  private static <T> Converter<T> interfaceOnSuperclass(Class<?> clazz) {
    Class<?> superclass = clazz.getSuperclass();
    while (superclass != null && !Object.class.equals(superclass)) {
      Converter<T> converter = getConverterForInterface(superclass);
      if (converter != null) return converter;
      superclass = superclass.getSuperclass();
    }
    return null;
  }

  public static <T> void addConverter(Class<? extends T> clazz, Converter<T> converter) {
    converters.put(clazz, converter);
  }

  public static void removeConverter(Class<?> clazz) {
    converters.remove(clazz);
  }

  public static Map<Class<?>, Converter<?>> getConverters() {
    return Collections.unmodifiableMap(converters);
  }

  /*
   * PRIVATE
   */
  public static Converter<?> getConverterForClassOrDefaultConverter(Class<?> clazz) {
    Converter<?> converter = getConverterForClass(clazz);
    if (converter == null) {
      converter = defaultConverter;
    }
    return converter;
  }
}
