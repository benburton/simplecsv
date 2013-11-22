package net.thornydev.simplecsv;

/**
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import org.junit.Before;
import org.junit.Test;
// import org.mockito.Matchers;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;
// import static org.mockito.Matchers.anyInt;
// import static org.mockito.Matchers.notNull;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

public class CsvReaderTest {

  CsvReader csvr;

  /**
   * Setup the test.
   */
  @Before
  public void setUp() throws Exception {
    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
    sb.append("a,b,c").append("\n");   // standard case
    sb.append("a,\"b,b,b\",c").append("\n");  // quoted elements
    sb.append(",,").append("\n"); // empty elements
    sb.append("a,\"PO Box 123,\\nKippax,ACT. 2615.\\nAustralia\",d.\n");
    sb.append("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer\n"); // Test quoted quote chars
    sb.append("\"\"\"\"\"\",\"test\"\n"); // """""","test"  
    sb.append("\"a\\nb\",b,\"\\nd\",e\n");
    csvr = new CsvReader(new StringReader(sb.toString()));
  }


  @Test
  public void test101() throws IOException {
    FileReader fr = new FileReader("src/test/resources/basic.csv");
    CsvParser parser = new CsvParserBuilder().strictQuotes().build();
    csvr = new CsvReader(fr, parser);
    String[] toks = csvr.readNext();
    System.out.println(Arrays.asList(toks));
    toks = csvr.readNext();
    System.out.println(Arrays.asList(toks));

    // TODO: need to do asserts and create better test data file
  }
  
  /**
   * Tests iterating over a reader.
   *
   * @throws IOException if the reader fails.
   */
  @Test
  public void testParseLine() throws IOException {

    // test normal case
    String[] toks = csvr.readNext();
    assertEquals("a", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);

    // test quoted commas
    toks = csvr.readNext();
    assertEquals("a", toks[0]);
    assertEquals("b,b,b", toks[1]);
    assertEquals("c", toks[2]);

    // test empty elements
    toks = csvr.readNext();
    assertEquals(3, toks.length);

    // test multiline quoted
    toks = csvr.readNext();
    assertEquals(3, toks.length);

    // test quoted quote chars
    toks = csvr.readNext();
    assertEquals("Glen \"\"The Man\"\" Smith", toks[0]);

    toks = csvr.readNext();
    assertEquals("\"\"\"\"", toks[0]); 
    assertEquals("test", toks[1]); // make sure we didn't ruin the next field..

    toks = csvr.readNext();
    assertEquals(4, toks.length);

    //test end of stream
    assertNull(csvr.readNext());
  }

  @Test
  public void testParseLineStrictQuote() throws IOException {

    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
    sb.append("a,b,c").append("\n");   // standard case
    sb.append("a,\"b,b,b\",c").append("\n");  // quoted elements
    sb.append(",,").append("\n"); // empty elements
    sb.append("a,\"PO Box 123,\\nKippax,ACT. 2615.\\nAustralia\",d.\n");
    sb.append("\"Glen \\\"The Man\\\" Smith\",Athlete,Developer\n"); // Test quoted quote chars
    sb.append("\"\"\"\"\"\",\"test\"\n"); // """""","test"  representing:  "", test
    sb.append("\"a\\nb\",b,\"\\nd\",e\n");

    CsvParser parser = new CsvParserBuilder().strictQuotes().build();
    System.out.println(parser.parse("\"Glen \\\"The Man\\\" Smith\""));
    csvr = new CsvReader(new StringReader(sb.toString()), parser);

    // test normal case
    String[] toks = csvr.readNext();
    System.out.println(Arrays.asList(toks));
    assertEquals("", toks[0]);
    assertEquals("", toks[1]);
    assertEquals("", toks[2]);

    // test quoted commas
    toks = csvr.readNext();
    assertEquals("", toks[0]);
    assertEquals("b,b,b", toks[1]);
    assertEquals("", toks[2]);

    // test empty elements
    toks = csvr.readNext();
    assertEquals(3, toks.length);

    // test multiline quoted
    toks = csvr.readNext();
    assertEquals(3, toks.length);

    // test quoted quote chars
    toks = csvr.readNext();
    assertEquals("Glen \"The Man\" Smith", toks[0]);

    toks = csvr.readNext();
    assertEquals("", toks[0]);
    assertEquals("test", toks[1]);

    toks = csvr.readNext();
    assertEquals(4, toks.length);
    assertEquals("a\\nb", toks[0]);
    assertEquals("", toks[1]);
    assertEquals("\\nd", toks[2]);
    assertEquals("", toks[3]);

    //test end of stream
    assertNull(csvr.readNext());
  }

//
//  /**
//   * Test parsing to a list.
//   *
//   * @throws IOException if the reader fails.
//   */
//  @Test
//  public void testParseAll() throws IOException {
//    assertEquals(7, csvr.readAll().size());
//  }
//
//  /**
//   * Tests constructors with optional delimiters and optional quote char.
//   *
//   * @throws IOException if the reader fails.
//   */
//  @Test
//  public void testOptionalConstructors() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//    sb.append("a\tb\tc").append("\n");   // tab separated case
//    sb.append("a\t'b\tb\tb'\tc").append("\n");  // single quoted elements
//    csvreader c = new csvreader(new StringReader(sb.toString()), '\t', '\'');
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//    c.close();
//  }
//
//  @Test
//  public void parseQuotedStringWithDefinedSeperator() throws IOException {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//    sb.append("a\tb\tc").append("\n");   // tab separated case
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), '\t');
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    c.close();
//  }
//
//  @Test
//  public void parsePipeDelimitedString() throws IOException {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//    sb.append("[bar]|[baz]").append("\n");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), '|');
//
//    String[] nextLine = c.readNext();
//    assertEquals(2, nextLine.length);
//    assertEquals("[bar]", nextLine[0]);
//    assertEquals("[baz]", nextLine[1]);
//
//    c.close();
//  }
//
//  /**
//   * Tests option to skip the first few lines of a file.
//   *
//   * @throws IOException if bad things happen
//   */
//  @Test
//  public void testSkippingLines() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//    sb.append("Skip this line\t with tab").append("\n");   // should skip this
//    sb.append("And this line too").append("\n");   // and this
//    sb.append("a\t'b\tb\tb'\tc").append("\n");  // single quoted elements
//    csvreader c = new csvreader(new StringReader(sb.toString()), '\t', '\'', 2);
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//
//    c.close();
//  }
//
//
//  /**
//   * Tests option to skip the first few lines of a file.
//   *
//   * @throws IOException if bad things happen
//   */
//  @Test
//  public void testSkippingLinesWithDifferentEscape() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//    sb.append("Skip this line?t with tab").append("\n");   // should skip this
//    sb.append("And this line too").append("\n");   // and this
//    sb.append("a\t'b\tb\tb'\t'c'").append("\n");  // single quoted elements
//    csvreader c = new csvreader(new StringReader(sb.toString()), '\t', '\'', '?', 2);
//
//    String[] nextLine = c.readNext();
//
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals("c", nextLine[2]);
//
//    c.close();
//  }
//
//  /**
//   * Test a normal non quoted line with three elements
//   *
//   * @throws IOException
//   */
//  @Test
//  public void testNormalParsedLine() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,1234567,c").append("\n");// a,1234,c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()));
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals("1234567", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//
//  /**
//   * Same as testADoubleQuoteAsDataElement but I changed the quotechar to a
//   * single quote.
//   *
//   * @throws IOException
//   */
//  @Test
//  public void testASingleQuoteAsDataElement() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,'''',c").append("\n");// a,'',c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), ',', '\'');
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals("''", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//  /**
//   * Same as testADoubleQuoteAsDataElement but I changed the quotechar to a
//   * single quote.  Also the middle field is empty.
//   *
//   * @throws IOException
//   */
//  @Test
//  public void testASingleQuoteAsDataElementWithEmptyField() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,'',c").append("\n");// a,,c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), ',', '\'');
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals(0, nextLine[1].length());
//    assertEquals("", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//  @Test
//  public void testSpacesAtEndOfString() throws IOException {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("\"a\",\"b\",\"c\"   ");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_QUOTE_CHARACTER, true);
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals("b", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//
//  @Test
//  public void testEscapedQuote() throws IOException {
//
//    StringBuffer sb = new StringBuffer();
//
//    sb.append("a,\"123\\\"4567\",c").append("\n");// a,123"4",c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()));
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("123\"4567", nextLine[1]);
//    c.close();
//  }
//
//  @Test
//  public void testEscapedEscape() throws IOException {
//
//    StringBuffer sb = new StringBuffer();
//
//    sb.append("a,\"123\\\\4567\",c").append("\n");// a,123"4",c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()));
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("123\\4567", nextLine[1]);
//    c.close();
//  }
//
//
//  /**
//   * Test a line where one of the elements is two single quotes and the
//   * quote character is the default double quote.  The expected result is two
//   * single quotes.
//   *
//   * @throws IOException
//   */
//  @Test
//  public void testSingleQuoteWhenDoubleQuoteIsQuoteChar() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,'',c").append("\n");// a,'',c
//
//    csvreader c = new csvreader(new StringReader(sb.toString()));
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals(2, nextLine[1].length());
//    assertEquals("''", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//  /**
//   * Test a normal line with three elements and all elements are quoted
//   *
//   * @throws IOException
//   */
//  @Test
//  public void testQuotedParsedLine() throws IOException {
//
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("\"a\",\"1234567\",\"c\"").append("\n"); // "a","1234567","c"
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_QUOTE_CHARACTER, true);
//
//    String[] nextLine = c.readNext();
//    assertEquals(3, nextLine.length);
//
//    assertEquals("a", nextLine[0]);
//    assertEquals(1, nextLine[0].length());
//
//    assertEquals("1234567", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    c.close();
//  }
//
//  @Test
//  public void testIssue2992134OutOfPlaceQuotes() throws IOException {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()));
//
//    String[] nextLine = c.readNext();
//
//    assertEquals("a", nextLine[0]);
//    assertEquals("b", nextLine[1]);
//    assertEquals("c", nextLine[2]);
//    assertEquals("ddd\"eee", nextLine[3]);
//    c.close();
//  }
//
//  //////////
//
//
//  @Test
//  public void testASingleQuoteAsDataElementWithEmptyField2() throws IOException {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("\"\";1").append("\n");// ;1
//    sb.append("\"\";2").append("\n");// ;2
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), ';', '\"');
//
//    String[] nextLine = c.readNext();
//    assertEquals(2, nextLine.length);
//
//    assertEquals(0, nextLine[0].length());
//    assertEquals("1", nextLine[1]);
//
//    nextLine = c.readNext();
//    assertEquals(2, nextLine.length);
//
//    assertEquals("", nextLine[0]);
//    assertEquals(0, nextLine[0].length());
//    assertEquals("2", nextLine[1]);
//
//    c.close();
//  }
//  //////////
//
//  @Test(expected = UnsupportedOperationException.class)
//  public void quoteAndEscapeMustBeDifferent() {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_QUOTE_CHARACTER, CsvParser.DEFAULT_QUOTE_CHARACTER, csvreader.DEFAULT_SKIP_LINES, CsvParser.DEFAULT_STRICT_QUOTES, CsvParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
//  }
//
//  @Test(expected = UnsupportedOperationException.class)
//  public void separatorAndEscapeMustBeDifferent() {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_QUOTE_CHARACTER, CsvParser.DEFAULT_SEPARATOR, csvreader.DEFAULT_SKIP_LINES, CsvParser.DEFAULT_STRICT_QUOTES, CsvParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
//  }
//
//  @Test(expected = UnsupportedOperationException.class)
//  public void separatorAndQuoteMustBeDifferent() {
//    StringBuilder sb = new StringBuilder(CsvParser.INITIAL_READ_SIZE);
//
//    sb.append("a,b,c,ddd\\\"eee\nf,g,h,\"iii,jjj\"");
//
//    csvreader c = new csvreader(new StringReader(sb.toString()), CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_SEPARATOR, CsvParser.DEFAULT_ESCAPE_CHARACTER, csvreader.DEFAULT_SKIP_LINES, CsvParser.DEFAULT_STRICT_QUOTES, CsvParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
//  }
//
//  /**
//   * Tests iterating over a reader.
//   *
//   * @throws IOException if the reader fails.
//   */
//  @Test
//  public void testIteratorFunctionality() throws IOException {
//    String[][] expectedResult = new String[7][];
//    expectedResult[0] = new String[]{"a", "b", "c"};
//    expectedResult[1] = new String[]{"a", "b,b,b", "c"};
//    expectedResult[2] = new String[]{"", "", ""};
//    expectedResult[3] = new String[]{"a", "PO Box 123,\nKippax,ACT. 2615.\nAustralia", "d."};
//    expectedResult[4] = new String[]{"Glen \"The Man\" Smith", "Athlete", "Developer"};
//    expectedResult[5] = new String[]{"\"\"", "test"};
//    expectedResult[6] = new String[]{"a\nb", "b", "\nd", "e"};
//    int idx = 0;
//    for (String[] line : csvr) {
//      String[] expectedLine = expectedResult[idx++];
//      assertArrayEquals(expectedLine, line);
//    }
//  }
//
//  @Test
//  public void canCloseReader() throws IOException {
//    csvr.close();
//  }
//
//  @Test
//  public void canCreateIteratorFromReader() {
//    assertNotNull(csvr.iterator());
//  }
//
//  @Test(expected = RuntimeException.class)
//  public void creatingIteratorForReaderWithNullDataThrowsRuntimeException() throws IOException {
//    Reader mockReader = mock(Reader.class);
//    when(mockReader.read(Matchers.<CharBuffer>any())).thenThrow(new IOException("test io exception"));
//    when(mockReader.read()).thenThrow(new IOException("test io exception"));
//    when(mockReader.read((char[]) notNull())).thenThrow(new IOException("test io exception"));
//    when(mockReader.read((char[]) notNull(), anyInt(), anyInt())).thenThrow(new IOException("test io exception"));
//    csvr = new csvreader(mockReader);
//    Iterator iterator = csvr.iterator();
//  }

}