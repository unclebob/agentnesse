// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertHasRegexp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StreamReaderTest {
  private ByteArrayOutputStream written;
  private StreamReader reader;

  @Before
  public void setUp() {
    written = new ByteArrayOutputStream();
  }

  @After
  public void tearDown() throws Exception {
    if (reader != null)
      reader.close();
  }

  private void write(String value) throws Exception {
    written.write(value.getBytes());
  }

  private StreamReader openReader() {
    reader = new StreamReader(new ByteArrayInputStream(written.toByteArray()));
    return reader;
  }

  @Test
  public void testReadLine() throws Exception {
    write("a line\r\n");
    assertEquals("a line", openReader().readLine());
  }

  @Test
  public void testReadLineBytes() throws Exception {
    write("a line\r\n");
    assertEquals("a line", new String(openReader().readLineBytes()));
  }

  @Test
  public void testBufferCanGrow() throws Exception {
    for (int i = 0; i < 1001; i++)
      write(i + ",");
    write("\r\n");

    assertHasRegexp("1000", openReader().readLine());
  }

  @Test
  public void testReadNumberOfBytesAsString() throws Exception {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      buffer.append("*");
      write("*");
    }

    assertEquals(buffer.toString(), openReader().read(100));
  }

  @Test
  public void testReadNumberOfBytes() throws Exception {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      buffer.append("*");
      write("*");
    }

    assertEquals(buffer.toString(), new String(openReader().readBytes(100)));
  }

  @Test
  public void testReadNumberOfBytesWithClosedInput() throws Exception {
    for (int i = 0; i < 50; i++)
      write("*");
    StreamReader reader = openReader();

    byte[] bytes = reader.readBytes(100);

    assertEquals("bytes consumed", 50, reader.numberOfBytesConsumed());
    assertEquals("bytes returned", 50, bytes.length);
  }

  @Test
  public void testReadingZeroBytes() throws Exception {
    assertEquals("", openReader().read(0));
  }

  @Test
  public void testReadUpTo() throws Exception {
    checkReadUpTo("--boundary", "some bytes--boundary", "some bytes");
  }

  @Test
  public void testReadUpToNonEnd() throws Exception {
    checkReadUpTo("--bound", "some bytes--boundary", "some bytes");
  }

  @Test
  public void testReadBytesUpTo() throws Exception {
    write("some bytes--boundary");
    assertEquals("some bytes", new String(openReader().readBytesUpTo("--boundary")));
  }

  @Test
  public void testReadUpTo2() throws Exception {
    checkReadUpTo("--bob", "----bob\r\n", "--");
  }

  @Test
  public void testReadUpTo3() throws Exception {
    checkReadUpTo("12345", "112123123412345", "1121231234");
  }

  private void checkReadUpTo(String boundary, String input, String expected) throws Exception {
    write(input);
    assertEquals(expected, openReader().readUpTo(boundary));
  }

  @Test
  public void testCopyBytesUpTo() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    write("some bytes--boundary");
    openReader().copyBytesUpTo("--boundary", outputStream);
    assertEquals("some bytes", outputStream.toString());
  }

  @Test
  public void testEofReadCount() throws Exception {
    write("abcdefghijklmnopqrstuvwxyz");
    StreamReader reader = openReader();
    assertFalse(reader.isEof());
    reader.read(10);
    assertFalse(reader.isEof());
    reader.read(16);
    assertFalse(reader.isEof());
    reader.read(1);
    assertTrue(reader.isEof());
  }

  @Test
  public void testEofReadLine() throws Exception {
    write("one line\ntwo lines\nthree lines");
    StreamReader reader = openReader();
    assertFalse(reader.isEof());
    reader.readLine();
    assertFalse(reader.isEof());
    reader.readLine();
    assertFalse(reader.isEof());
    reader.readLine();
    assertTrue(reader.isEof());
  }

  @Test
  public void testEofReadUpTo() throws Exception {
    write("mark one, mark two, the end");
    StreamReader reader = openReader();
    assertFalse(reader.isEof());
    reader.readUpTo("one");
    assertFalse(reader.isEof());
    reader.readUpTo("two");
    assertFalse(reader.isEof());
    reader.readUpTo("three");
    assertTrue(reader.isEof());
  }

  @Test
  public void testBytesConsumed() throws Exception {
    write("One line\r\n12345abc-boundary");
    StreamReader reader = openReader();
    assertEquals(0, reader.numberOfBytesConsumed());

    reader.readLine();
    assertEquals(10, reader.numberOfBytesConsumed());

    reader.read(5);
    assertEquals(15, reader.numberOfBytesConsumed());

    reader.readUpTo("-boundary");
    assertEquals(27, reader.numberOfBytesConsumed());
  }

  @Test
  public void testEarlyClosingStream() throws Exception {
    assertEquals("", openReader().read(10));
  }
}
