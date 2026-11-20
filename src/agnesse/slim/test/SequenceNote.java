package agnesse.slim.test;

/** A note the sequence shrinks one character at a time until the shrink cap. */
public class SequenceNote {
  public String note(String text) {
    return text.length() >= 48 ? "no" : "yes";
  }
}
