package agnesse.slim.test;

/** A record that can change while open, and only through an audited adjustment once locked. */
public class AuditedRecord {
  private String phase = "open";
  private int value;
  private boolean audited;

  public String set(String amount) {
    if (!"open".equals(phase)) {
      return "refused";
    }
    value = Integer.parseInt(amount);
    return "ok";
  }

  public String lock() {
    if (!"open".equals(phase)) {
      return "refused";
    }
    phase = "locked";
    return "ok";
  }

  public String adjust(String amount, String note) {
    if (!"locked".equals(phase) || note == null || note.isBlank()) {
      return "refused";
    }
    value += Integer.parseInt(amount);
    audited = true;
    return "ok";
  }

  public String tweak(String amount) {
    return "refused";
  }

  public String value() {
    return Integer.toString(value);
  }

  public String phase() {
    return phase;
  }

  public boolean audited() {
    return audited;
  }

  protected String change(String amount) {
    value += Integer.parseInt(amount);
    return "ok";
  }
}
