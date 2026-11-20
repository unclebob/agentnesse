package agnesse.slim.test;

/** A locked record that changes without an audit. */
public class AuditedLeak extends AuditedRecord {
  @Override
  public String tweak(String amount) {
    return change(amount);
  }
}
