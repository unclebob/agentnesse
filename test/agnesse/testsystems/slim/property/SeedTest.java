package agnesse.testsystems.slim.property;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

public class SeedTest {
  @Test
  public void valueIsTheLongItWasGiven() {
    assertEquals(42L, new Seed(42L).value());
    assertEquals(-7L, new Seed(-7L).value());
  }

  @Test
  public void sameSeedOpensTheSameStreamFromTheStart() {
    Seed seed = new Seed(99L);
    Random first = seed.random();
    Random second = seed.random();
    assertNotSame(first, second);
    assertEquals(first.nextInt(), second.nextInt());
    assertEquals(first.nextInt(), second.nextInt());
  }

  @Test
  public void oneSeedDoesNotAdvanceAnother() {
    Random first = new Seed(5L).random();
    int opened = first.nextInt();
    Random second = new Seed(5L).random();
    first.nextInt();
    assertEquals(opened, second.nextInt());
  }

  @Test
  public void differentSeedsOpenDifferentStreams() {
    assertNotEquals(new Seed(1L).random().nextInt(), new Seed(2L).random().nextInt());
  }
}
