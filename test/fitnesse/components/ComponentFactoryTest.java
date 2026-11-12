package fitnesse.components;

import java.util.Properties;

import fitnesse.components.ComponentInstantiationException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComponentFactoryTest {

  @Test
  public void shouldInstantiateComponentsOnce() {
    ComponentFactory componentFactory = new ComponentFactory(new Properties());

    Object component1 = componentFactory.createComponent("SomePropertyName", TheComponent.class);
    Object component2 = componentFactory.createComponent("SomePropertyName", TheComponent.class);

    assertSame(component1, component2);
  }

  @Test
  public void missingTypeWithNoDefaultIsAbsent() {
    assertNull(new ComponentFactory(new Properties()).createComponent("missing", null));
  }

  @Test
  public void namedClassIsLoadedOnce() {
    Properties properties = new Properties();
    properties.setProperty("widget", TheComponent.class.getName());
    ComponentFactory factory = new ComponentFactory(properties);

    assertSame(factory.createComponent("widget", null), factory.createComponent("widget", null));
  }

  @Test
  public void unknownClassNameIsAnError() {
    Properties properties = new Properties();
    properties.setProperty("widget", "no.such.Class");
    try {
      new ComponentFactory(properties).createComponent("widget", TheComponent.class);
      fail("expected a missing class to be reported");
    } catch (ComponentInstantiationException expected) {
      assertTrue(expected.getMessage().contains("no.such.Class"));
    }
  }

  @Test
  public void classIsBuiltFromPropertiesOrTheFactory() {
    ComponentFactory factory = new ComponentFactory(new Properties());
    assertTrue(factory.createComponent(TakesProperties.class) instanceof TakesProperties);
    assertTrue(factory.createComponent(TakesFactory.class) instanceof TakesFactory);
    try {
      factory.createComponent(NoConstructor.class);
      fail("expected a class with no usable constructor to be reported");
    } catch (ComponentInstantiationException expected) {
      assertTrue(expected.getMessage().contains(NoConstructor.class.getName()));
    }
  }

  public static class TheComponent {

  }

  public static class TakesProperties {
    public TakesProperties(Properties properties) {
    }
  }

  public static class TakesFactory {
    public TakesFactory(ComponentFactory factory) {
    }
  }

  public static class NoConstructor {
    private NoConstructor(String ignored) {
    }
  }
}