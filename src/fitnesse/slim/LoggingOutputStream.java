// Copyright (C) 2016 by six42@gmx.net, All rights reserved.
// The below code is based on https://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and
package fitnesse.slim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * An OutputStream that writes contents to a Logger upon each call to flush()
 */
class LoggingOutputStream extends ByteArrayOutputStream {
  // PrintStream.println writes the text and its newline in one flush.
  // Drop that trailing separator only. A flush that does not end in the
  // line separator, such as print("Hello"), must keep every character.

  private String lineSeparator;

  private PrintStream logger;
  private String level;

  /**
   * Constructor
   *
   * @param logger
   *          Logger to write to
   * @param level
   *          Level at which to write the log message
   */
  public LoggingOutputStream(PrintStream logger, String level) {
    super();
    this.logger = logger;
    this.level = level;
    this.lineSeparator = System.getProperty("line.separator");
  }

  /**
   * upon flush() write the existing contents of the OutputStream to the logger
   * as a log record.
   *
   * @throws java.io.IOException
   *           in case of error
   */
  @Override
  public void flush() throws IOException {
    String record;
    synchronized (this) {
      super.flush();
      record = this.toString();
      super.reset();
    }
    record = logicalRecord(record, lineSeparator);
    if (record == null) {
      return;
    }
    // Prefix each new line with: newline + level + DOT + ":"
    record = record.replace("\n", "\n" + level
        + SlimPipeSocket.FOLLOWING_LINE_PREFIX);
    // Prefix first line with: level + SPACE + ":"
    logger.println(level + SlimPipeSocket.FIRST_LINE_PREFIX + record);
  }

  /**
   * @return the text to log, or null when this flush is only an empty buffer
   *         or the newline PrintStream.println adds on its own.
   */
  static String logicalRecord(String record, String lineSeparator) {
    if (record.isEmpty() || record.equals(lineSeparator)) {
      return null;
    }
    if (record.endsWith(lineSeparator)) {
      record = record.substring(0, record.length() - lineSeparator.length());
      if (record.isEmpty() || record.equals(lineSeparator)) {
        return null;
      }
    }
    return record;
  }

}
