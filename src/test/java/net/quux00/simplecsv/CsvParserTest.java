package net.quux00.simplecsv;

import static net.quux00.simplecsv.CsvParser.DEFAULT_QUOTE_CHAR;
import static net.quux00.simplecsv.CsvParser.DEFAULT_SEPARATOR;
import static net.quux00.simplecsv.CsvParser.NULL_CHARACTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import net.quux00.simplecsv.CsvParser;
import net.quux00.simplecsv.CsvParserBuilder;

import org.junit.Before;
import org.junit.Test;

public class CsvParserTest {

  static String longEntry1 = "aaaaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbbbbbbbbbb cccccccccccccccccccccccccccccccccccccc ddddddddddddddddddddddddddddd efg 123456789012345678901234567890";
  static String longEntry2 = "Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of \"de Finibus Bonorum et Malorum\" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, \"Lorem ipsum dolor sit amet..\", comes from a line in section 1.10.32.";
  static String longLine = String.format("%s,, \"%s\"", longEntry1, longEntry2);
  
  CsvParser parser = null;
  
  @Before
  public void setUp() {
    parser = new CsvParser();
  }
  
  /* --------------------------------- */  
  /* ---[ Tests parser invariants ]--- */
  /* --------------------------------- */  

  @Test(expected = UnsupportedOperationException.class)
  public void quoteAndEscapeCannotBeTheSameViaParserCtor() {
    new CsvParser(DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHAR, DEFAULT_QUOTE_CHAR,
        false, false, false, false, true, false);
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void quoteAndEscapeCannotBeTheSameViaBuilder() {
    new CsvParserBuilder().quoteChar(DEFAULT_QUOTE_CHAR).escapeChar(DEFAULT_QUOTE_CHAR).build();
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void separatorCharacterCannotBeNull() {
    new CsvParserBuilder().separator(NULL_CHARACTER).build();
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void separatorAndEscapeCannotBeTheSame() {
    new CsvParserBuilder().separator(DEFAULT_SEPARATOR).escapeChar(DEFAULT_SEPARATOR).build();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void separatorAndQuoteCannotBeTheSame() {
    new CsvParserBuilder().separator(DEFAULT_SEPARATOR).quoteChar(DEFAULT_SEPARATOR).build();
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void nullQuoteCharAndAlwaysQuoteOutputAreIncompatible() {
    new CsvParserBuilder().quoteChar(CsvParser.NULL_CHARACTER).alwaysQuoteOutput(true).build();
  }

  /* -------------------------------------------- */  
  /* ---[ Tests with Default Parser Settings ]--- */
  /* -------------------------------------------- */
  
  @Test
  public void testParseLine() {
    String toks[] = parser.parseLine("This, is, a, test.");
    assertEquals(4, toks.length);
    assertEquals("This", toks[0]);
    assertEquals(" is", toks[1]);
    assertEquals(" a", toks[2]);
    assertEquals(" test.", toks[3]);
  }
  
  
  @Test
  public void parseSimpleQuotedString() {
    String[] toks = parser.parseLine("\"a\",\"b\",\"c\"");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void parseSimpleQuotedStringWithSpaces() {
    String[] toks = parser.parseLine(" \"a\" , \"b\" , \"c\" ");
    assertEquals(3, toks.length);
    assertEquals(" a ", toks[0]);
    assertEquals(" b ", toks[1]);
    assertEquals(" c ", toks[2]);
  }
  
  @Test
  public void testParsedLineWithInternalQuota() {
    String[] toks = parser.parseLine("a,123\"4\"567,c");
    assertEquals(3, toks.length);
    assertEquals("123\"4\"567", toks[1]);
  }
  
  @Test
  public void parseQuotedStringWithCommas() {
    String[] toks = parser.parseLine("a,\"b,b,b\",c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b,b,b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void parseEmptyElements() {
    String[] toks = parser.parseLine(",,");
    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals("", toks[1]);
    assertEquals("", toks[2]);
  }

  
  // RFC4180 examples from: https://en.wikipedia.org/wiki/Comma-separated_values
  // This shows how simplecsv rejects the "quotes as escape chars" philosophy of that RFC
  @Test
  public void testRFC4180Examples() {
    String text = "1997,Ford,E350,\"Super, \"\"luxurious\"\" truck\"";
    String[] toks = parser.parseLine(text);

    assertEquals(4, toks.length);
    assertEquals("1997", toks[0]);
    assertEquals("Ford", toks[1]);
    assertEquals("E350", toks[2]);
    assertEquals("Super, \"\"luxurious\"\" truck", toks[3]);
  }
  
  @Test
  public void testEscapedDoubleQuoteAsDataElement() {
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = parser.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("test", toks[0]);
    assertEquals("this,test,is,good", toks[1]);
    assertEquals("\\\"test\\\"", toks[2]);
    assertEquals("\\\"quote\\\"", toks[3]);
  }

  @Test
  public void parseMultipleQuotes() {
    String[] toks = parser.parseLine("\"\"\"\"\"\",\"test\"\n"); // """""","test"  representing:  "", test
    assertEquals(2, toks.length);
    assertEquals("\"\"\"\"", toks[0]);  // check the tricky situation
    assertEquals("test\n", toks[1]);    // make sure we didn't ruin the next field..
  }
  
  @Test
  public void parseTrickyString() {
    String[] toks = parser.parseLine("\"a\nb\",b,\"\nd\",e\n");
    assertEquals(4, toks.length);
    assertEquals("a\nb", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("\nd", toks[2]);
    assertEquals("e\n", toks[3]);
  }
  
  @Test
  public void testAMultiLineInsideQuotes() {
    String text = "Small test,\"This is a test across \ntwo lines.\"";
    String[] toks = parser.parseLine(text);
    assertEquals(2, toks.length);
    assertEquals("Small test", toks[0]);
    assertEquals("This is a test across \ntwo lines.", toks[1]);
  }

  /**
   * Test issue 2726363
   * <p/>
   * Data given:
   * <p/>
   * "804503689","London",""London""shop","address","116.453182","39.918884"
   * "453074125","NewYork","brief","address"","121.514683","31.228511"
   */
  @Test
  public void testIssue2726363() {
    String[] toks = parser.parseLine("\"804503689\",\"London\",\"\"London\"shop\",\"address\",\"116.453182\",\"39.918884\"");

    assertEquals(6, toks.length);
    assertEquals("804503689", toks[0]);
    assertEquals("London", toks[1]);
    assertEquals("\"London\"shop", toks[2]);
    assertEquals("address", toks[3]);
    assertEquals("116.453182", toks[4]);
    assertEquals("39.918884", toks[5]);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void anExceptionThrownifStringEndsInsideAQuotedString() {
    parser.parseLine("This,is a \"bad line to parse.");
  }
  
  @Test
  public void returnNullWhenNullPassedIn() {
    String[] toks = parser.parseLine(null);
    assertNull(toks);
  }
  
  @Test
  public void testInternalQuotes() {
    String[] toks = parser.parseLine("a , \"b\",1000");
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals(" b", toks[1]);
    assertEquals("1000", toks[2]);
  } 
  
  @Test
  public void testInternalQuotedQuotes() {
    String[] toks = parser.parseLine("a , \"\\\"\",1000");  // a, "\"",1000
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals(" \\\"", toks[1]);
    assertEquals("1000", toks[2]);
  } 
  
  
  @Test
  public void testADoubleQuoteAsDataElement() {
    String[] toks = parser.parseLine("a,\"\"\"\",c");  // a,"""",c

    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\"\"", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testIssueThorny1a() {
    String[] toks = parser.parseLine("a , \"\",1000");  // a, "",1000
    
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals(" ", toks[1]);
    assertEquals("1000", toks[2]);
  }
  
  @Test
  public void testIssueThorny1b() {
    String[] toks = parser.parseLine("a , \"\" ,1000");  // a, "" ,1000
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals("  ", toks[1]);
    assertEquals("1000", toks[2]);
  }
  
  @Test
  public void testIssueThorny1c() {
    String[] toks = parser.parseLine("a ,Mike \"The Situation\" Sorrentino,1000");
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals("Mike \"The Situation\" Sorrentino", toks[1]);
    assertEquals("1000", toks[2]);
  }
  
  @Test
  public void testIssueThorny1d() {
    String[] toks = parser.parseLine("a,\" \"hello\" \",c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals(" \"hello\" ", toks[1]);
    assertEquals("c", toks[2]);
    
    CsvParser p = new CsvParserBuilder().quoteChar('\'').build();
    toks = p.parseLine("a,' 'hello' ',c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals(" 'hello' ", toks[1]);
    assertEquals("c", toks[2]);

    p = new CsvParserBuilder().quoteChar('\'').trimWhitespace(true).build();
    toks = p.parseLine("a,' 'hello' ',c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("'hello'", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void whitespaceBeforeEscape() {
    String[] toks = parser.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
    assertEquals("this", toks[0]);
    assertEquals(" is", toks[1]);
    assertEquals("a test", toks[2]);
  }
  
  @Test
  public void testFourSingleQuotes() {
    String[] toks = parser.parseLine("a,'\'\'', c ");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("''''", toks[1]);
    assertEquals(4, toks[1].length());
    assertEquals(" c ", toks[2]);
  }

  
  @Test
  public void testLongTokens() {
    String[] toks = parser.parseLine(longLine);
    assertEquals(3, toks.length);
    assertEquals(longEntry1, toks[0]);
    assertEquals("", toks[1]);
    assertEquals(" " + longEntry2, toks[2]);
  }
  
  
  /* -------------------------------------------- */  
  /* ---[ Alternative Delims and Quote Chars ]--- */
  /* -------------------------------------------- */
  
  @Test
  public void testParseLinePipeDelimited() {
    CsvParser p = new CsvParserBuilder().separator('|').build();
    String toks[] = p.parseLine("This|is|a|test.");
    assertEquals(4, toks.length);
    assertEquals("This", toks[0]);
    assertEquals("is", toks[1]);
    assertEquals("a", toks[2]);
    assertEquals("test.", toks[3]);
  }
  
  
  @Test
  public void parseQuotedStringWithDefinedSeperator() {
    CsvParser p = new CsvParserBuilder().separator(':').build();

    String[] toks = p.parseLine("a:\"b:b:b\":c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b:b:b", toks[1]);
    assertEquals("c", toks[2]);
  }

  
  @Test
  public void parseQuotedStringWithDefinedSeparatorAndQuote() {
    CsvParser p = new CsvParserBuilder().separator(':').quoteChar('\'').build();

    String[] toks = p.parseLine("a:'b:b:b':c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b:b:b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  
  @Test  // issue from the old opencsv sourceforge project
  public void testIssue2859181() {
    CsvParser p = new CsvParserBuilder().separator(';').build();
    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("field1", toks[0]);
    assertEquals("\\=field2", toks[1]);
    assertEquals("\"\"field3\"\"", toks[2]);
  }
  
  @Test    // https://sourceforge.net/p/opencsv/bugs/93/
  public void testIssueSfBugs93() {
    CsvParser p = new CsvParserBuilder().separator(';').build();

    String[] toks = p.parseLine("\"\";1");
    assertEquals(2, toks.length);
    assertEquals("", toks[0]);
    assertEquals("1", toks[1]);

    toks = p.parseLine("\"\";2");
    assertEquals(2, toks.length);
    assertEquals("", toks[0]);
    assertEquals("2", toks[1]);
  }
 
  @Test
  public void testFourSingleQuotesWithSingleQuoteAsQuoteChar() {
    CsvParser p = new CsvParserBuilder().
      quoteChar('\'').
      build();

    String[] toks = p.parseLine("a,'\'\'',c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\'\'", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  /**
   * This is an interesting issue where the data does not use quotes but IS using a 
   * quote within the field as a inch symbol.  So we want to keep that quote as part 
   * of the field and not as the start or end of a field.
   *
   * Test data is as follows.
   *
   * RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16", ALUMINUM, DESIGN 1
   * RPO;2012;P; ; ; ;SDZ;ACCESSORY WHEEL - 17" - ALLOY - DESIGN 1
   */
  @Test
  public void testIssue3314579() {
    // difference from OpenCSV: cleaner soln is to set quotechar to NULL_CHAR
    CsvParser p = new CsvParserBuilder().
        separator(';').
        quoteChar(CsvParser.NULL_CHARACTER).
        allowUnbalancedQuotes(true).
        build();
    String testString = "RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1";

    String[] toks = p.parseLine(testString);
    assertEquals(8, toks.length);
    assertEquals("RPO", toks[0]);
    assertEquals("2012", toks[1]);
    assertEquals("P", toks[2]);
    assertEquals(" ", toks[3]);
    assertEquals(" ", toks[4]);
    assertEquals(" ", toks[5]);
    assertEquals("SDX", toks[6]);
    assertEquals("ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1", toks[7]);

    // you don't need "allowUnbalancedQuotes" if you set quotechar to be the NULL_CHAR
    p = new CsvParserBuilder().
        separator(';').
        quoteChar(CsvParser.NULL_CHARACTER).
        build();
    testString = "RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1";

    toks = p.parseLine(testString);
    assertEquals(8, toks.length);
    assertEquals("RPO", toks[0]);
    assertEquals("2012", toks[1]);
    assertEquals("P", toks[2]);
    assertEquals(" ", toks[3]);
    assertEquals(" ", toks[4]);
    assertEquals(" ", toks[5]);
    assertEquals("SDX", toks[6]);
    assertEquals("ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1", toks[7]);    
    
    
    // this combination doesn't make any sense, but ensure it doesn't throw
    // NPEs or otherwise freak out
    p = new CsvParserBuilder().
        separator(';').
        quoteChar(CsvParser.NULL_CHARACTER).
        retainOuterQuotes(true).
        build();
    testString = "RPO;2012;P; ; ; ;SDX;ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1";

    toks = p.parseLine(testString);
    assertEquals(8, toks.length);
    assertEquals("RPO", toks[0]);
    assertEquals("2012", toks[1]);
    assertEquals("P", toks[2]);
    assertEquals(" ", toks[3]);
    assertEquals(" ", toks[4]);
    assertEquals(" ", toks[5]);
    assertEquals("SDX", toks[6]);
    assertEquals("ACCESSORY WHEEL, 16\", ALUMINUM, DESIGN 1", toks[7]);    
  }
  
  /**
   * Test issue 2263439 where an escaped quote was causing the parse to fail.
   */
  @Test
  public void testIssue2263439() {
    CsvParser p = new CsvParserBuilder().
        quoteChar('\'').
        build();

    String text = "865,0,'AmeriKKKa\\'s_Most_Wanted','',294,0,0,0.734338696798625,'20081002052147',242429208,18448";
    String[] toks = p.parseLine(text);

    assertEquals(11, toks.length);
    assertEquals("865", toks[0]);
    assertEquals("0", toks[1]);
    assertEquals("AmeriKKKa\\'s_Most_Wanted", toks[2]);
    assertEquals("", toks[3]);
    assertEquals("18448", toks[10]);
  }

  // https://sourceforge.net/p/opencsv/bugs/100/
  @Test
  public void testOpenCsvIssue100() {
    CsvParser p = new CsvParserBuilder().
        escapeChar(NULL_CHARACTER).
        retainOuterQuotes(true).
        trimWhitespace(true).
        build();

    String text = "\\x0\"\", two, \"three,four\"";
    String[] toks = p.parseLine(text);

    assertEquals(3, toks.length);
    assertEquals("\\x0\"\"", toks[0]);
    assertEquals("two", toks[1]);
    assertEquals("\"three,four\"", toks[2]);
  }
  
  
  /* ------------------------------- */    
  /* ---[ Strict Quotes Setting ]--- */
  /* ------------------------------- */  

  @Test
  public void parseSimpleQuotedStringWithSpacesWithstrictQuotes() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();
    
    String[] toks = p.parseLine(" \"a\" , \"b\" , \"c\" ");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testStrictQuoteSimple() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();
    String testString = "\"a\",\"b\",\"c\"";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testStrictQuoteWithSpacesAndTabs() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).retainOuterQuotes(true).build();

    String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }

  @Test
  public void spacesAtEndOfQuotedStringDoNotCountIfStrictQuotesIsTrue() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();
    String[] toks = p.parseLine("\"Line with\", \"spaces at end\"  ");
    assertEquals(2, toks.length);
    assertEquals("Line with", toks[0]);
    assertEquals("spaces at end", toks[1]);
  }
  
  @Test
  public void testStrictQuoteWithGarbage() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();
    String testString = "abc',!@#\",\\\"\"   xyz,";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals(",\\\"", toks[1]);
    assertEquals("", toks[2]);
  }
  
  @Test
  public void testWhitespaceBeforeEscapeWithStrictQuotes() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();

    String[] toks = p.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
    assertEquals("this", toks[0]);
    assertEquals("is", toks[1]);
    assertEquals("a test", toks[2]);
  }

  @Test
  public void testSomeFieldsWithoutQuotesWithStrictQuotes() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).build();

    String[] toks = p.parseLine("this, \"is\",\"a test\" xyz");
    assertEquals("", toks[0]);
    assertEquals("is", toks[1]);
    assertEquals("a test", toks[2]);
  }

  
  /* ----------------------------------------- */  
  /* ---[ Allow Unbalanced Quotes Setting ]--- */
  /* ----------------------------------------- */  

  @Test
  public void parseSimpleQuotedStringAllowUnbalancedQuotes() {
    CsvParser p = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    String[] toks = p.parseLine("\"\"a\"\",\"b\",\"c\"");
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testADoubleQuoteAsDataElementWithAllowUnbalancedQuotes() {
    CsvParser p = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    String[] toks = p.parseLine("a,\"\"\"\",c");  // a,"""",c

    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\"\"", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testIssueThorny1WithAllowUnbalancedQuotes() {
    CsvParser p = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    String[] toks = p.parseLine("a , \"\",1000");
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals(" ", toks[1]);
    assertEquals("1000", toks[2]);
  }
  
  
  @Test
  public void whitespaceBeforeEscapeWithAllowUnbalancedQuotes() {
    CsvParser p = new CsvParserBuilder().allowUnbalancedQuotes(true).build();

    String[] toks = p.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
    assertEquals("this", toks[0]);
    assertEquals(" is", toks[1]);
    assertEquals("a test", toks[2]);
  }
  
  
  

  /* ------------------------------------- */  
  /* ---[ Retain Outer Quotes Setting ]--- */
  /* ------------------------------------- */  
  
  @Test
  public void testParseLineWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String toks[] = p.parseLine("This, is,\"a\", test.");
    assertEquals(4, toks.length);
    assertEquals("This", toks[0]);
    assertEquals(" is", toks[1]);
    assertEquals("\"a\"", toks[2]);
    assertEquals(" test.", toks[3]);
  }
  
  @Test
  public void parseQuotedStringWithCommasWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String[] toks = p.parseLine("a,\"b,b,b\",c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\"b,b,b\"", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  
  @Test
  public void parseQuotedStringWithDefinedSeperatorWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().separator(':').
        retainOuterQuotes(true).build();

    String[] toks = p.parseLine("a:\"b:b:b\":c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\"b:b:b\"", toks[1]);
    assertEquals("c", toks[2]);
  }

  @Test
  public void parseQuotedStringWithDefinedSeperatorAndQuoteWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().separator(':').
        quoteChar('\'').
        retainOuterQuotes(true).
        build();

    String[] toks = p.parseLine("a:'b:b:b':c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("'b:b:b'", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void parseEmptyElementsWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String[] toks = p.parseLine(",,");
    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals("", toks[1]);
    assertEquals("", toks[2]);
  }
  
  @Test
  public void testADoubleQuoteAsDataElementWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();

    String[] toks = p.parseLine("a,\"\"\"\",c");// a,"""",c

    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("\"\"\"\"", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testEscapedDoubleQuoteAsDataElementWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();

    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); // "test","this,test,is,good","\"test\",\"quote\""

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\\\"test\\\"\"", toks[2]);
    assertEquals("\"\\\"quote\\\"\"", toks[3]);
  }

  
  @Test
  public void testEscapedDoubleQuoteAsDataElementPipeDelimitedWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().separator('|').retainOuterQuotes(true).build();
    //                            "test"|"this|test|is|good"|"\"test\"|\"quote\""
    String[] toks = p.parseLine("\"test\"|\"this,test,is,good\"|\"\\\"test\\\"\"|\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\\\"test\\\"\"", toks[2]);
    assertEquals("\"\\\"quote\\\"\"", toks[3]);
  }

  @Test
  public void parseMultipleQuotesWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String[] toks = p.parseLine("\"\"\"\"\"\",\"test\"\n"); // """""","test"  representing:  "", test
    assertEquals("\"\"\"\"\"\"", toks[0]); // check the tricky situation
    assertEquals("\"test\"\n", toks[1]);   // make sure we didn't ruin the next field..
    assertEquals(2, toks.length);
  }
  
  @Test
  public void parseTrickyStringWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String[] toks = p.parseLine("\"a\nb\",b,\"\nd\",e\n");
    assertEquals(4, toks.length);
    assertEquals("\"a\nb\"", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("\"\nd\"", toks[2]);
    assertEquals("e\n", toks[3]);
  }

  @Test
  public void testAMultiLineInsideQuotesWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();

    String text = "Small test,\"This is a test across \ntwo lines.\"";
    String[] toks = p.parseLine(text);
    assertEquals(2, toks.length);
    assertEquals("Small test", toks[0]);
    assertEquals("\"This is a test across \ntwo lines.\"", toks[1]);
  }
  
  @Test
  public void testStrictQuoteSimpleWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).strictQuotes(true).build();
    String testString = "\"a\",\"b\",\"c\"";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  @Test
  public void testIssue2859181WithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().
        separator(';').
        retainOuterQuotes(true).
        build();

    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("field1", toks[0]);
    assertEquals("\\=field2", toks[1]);
    assertEquals("\"\"\"field3\"\"\"", toks[2]);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void anExceptionThrownifStringEndsInsideAQuotedStringWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    p.parseLine("This,is a \"bad line to parse.");
  }
  
  @Test
  public void testIssueThorny1WithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().
        retainOuterQuotes(true).
        build();
    String[] toks = p.parseLine("a , \"\",1000");
    assertEquals(3, toks.length);
    assertEquals("a ", toks[0]);
    assertEquals(" \"\"", toks[1]);
    assertEquals("1000", toks[2]);
  }
    
  @Test
  public void whitespaceBeforeEscapeWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();

    String[] toks = p.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
    assertEquals("\"this\"", toks[0]);
    assertEquals(" \"is\"", toks[1]);
    assertEquals("\"a test\"", toks[2]);
  }
  
  @Test
  public void testFourSingleQuotesWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().
      separator(',').
      retainOuterQuotes(true).
      build();
    String[] toks = p.parseLine("a,'\'\'',c");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("''''", toks[1]);
    assertEquals("c", toks[2]);
  }

  @Test
  public void testLongTokensRetainOuterQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();

    String[] toks = p.parseLine(longLine);
    assertEquals(3, toks.length);
    assertEquals(longEntry1, toks[0]);
    assertEquals("", toks[1]);
    assertEquals(" \"" + longEntry2 + "\"", toks[2]);
  }

  
  /* ----------------------------------- */  
  /* ---[ RetainEscapeChars = false ]--- */
  /* ----------------------------------- */  

  @Test
  public void testEscapedDoubleQuoteAsDataElementWithRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().retainEscapeChars(false).build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("test", toks[0]);
    assertEquals("this,test,is,good", toks[1]);
    assertEquals("\"test\"", toks[2]);
    assertEquals("\"quote\"", toks[3]);
  }
  
  @Test  // issue from the old opencsv sourceforge project
  public void testIssue2859181WithRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().separator(';').retainEscapeChars(false).build();
    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("field1", toks[0]);
    assertEquals("=field2", toks[1]);
    assertEquals("\"\"field3\"\"", toks[2]);
  }
  
  
  @Test
  public void testEscapesBeforeNewLinesEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().retainEscapeChars(false).build();
    String[] toks = p.parseLine("\"a\\nb\",b,\"\\nd\",e\n");
    
    assertEquals(4, toks.length);
    assertEquals("a\nb", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("\nd", toks[2]);
    assertEquals("e\n", toks[3]);
  }


  
  /* ---------------------------------- */  
  /* ---[ AlwaysQuoteOutput = true ]--- */
  /* ---------------------------------- */  
  
  @Test
  public void testParseLineAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    
    String toks[] = p.parseLine("This, is, a, test.");
    assertEquals(4, toks.length);
    assertEquals("\"This\"", toks[0]);
    assertEquals("\" is\"", toks[1]);
    assertEquals("\" a\"", toks[2]);
    assertEquals("\" test.\"", toks[3]);
  }
  
  @Test
  public void testParseLineAlwaysQuoteOutputWithTrimSpaces() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        trimWhitespace(true).
        build();
    
    String toks[] = p.parseLine("This, is, a, test.");
    assertEquals(4, toks.length);
    assertEquals("\"This\"", toks[0]);
    assertEquals("\"is\"", toks[1]);
    assertEquals("\"a\"", toks[2]);
    assertEquals("\"test.\"", toks[3]);
  }
  
  @Test
  public void parseQuotedStringWithCommasAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();

    String[] toks = p.parseLine("a,\"b,b,b\",c,, ,");
    assertEquals(6, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b,b,b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
    assertEquals("", toks[3]);
    assertEquals("\" \"", toks[4]);
    assertEquals("", toks[5]);
  }
  
  @Test
  public void parseQuotedStringWithCommasAndWhitespaceAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();

    String[] toks = p.parseLine(" a ,   \"b,b,b\",c ,, ,");
    assertEquals(6, toks.length);
    assertEquals("\" a \"", toks[0]);
    assertEquals("   \"b,b,b\"", toks[1]);
    assertEquals("\"c \"", toks[2]);
    assertEquals("", toks[3]);
    assertEquals("\" \"", toks[4]);
    assertEquals("", toks[5]);
  }
  
  @Test
  public void parseQuotedStringWithCommasAndWhitespaceAlwaysQuoteOutputAndTrimWhitespace() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        trimWhitespace(true).
        build();

    String[] toks = p.parseLine(" a ,   \"b,b,b\",c ,, ,");
    assertEquals(6, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b,b,b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
    assertEquals("", toks[3]);
    assertEquals("\" \"", toks[4]);
    assertEquals("", toks[5]);
  }
  
  @Test
  public void testEscapedDoubleQuoteAsDataElementAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\\\"test\\\"\"", toks[2]);
    assertEquals("\"\\\"quote\\\"\"", toks[3]);
  }
  
  @Test
  public void testEscapedDoubleQuoteAsDataElementAlwaysQuoteOutputWithTrimWhitespace() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        trimWhitespace(true).
        build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\\\"test\\\"\"", toks[2]);
    assertEquals("\"\\\"quote\\\"\"", toks[3]);
  }
  
  
  @Test
  public void testEscapedDoubleQuoteAsDataElementAlwaysQuoteOutputRetainOuterQuotes() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        retainOuterQuotes(true).
        build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\\\"test\\\"\"", toks[2]);
    assertEquals("\"\\\"quote\\\"\"", toks[3]);
  }

  @Test
  public void testEscapedDoubleQuoteAsDataElementAlwaysQuoteOutputRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        retainEscapeChars(false).
        build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\"test\"\"", toks[2]);
    assertEquals("\"\"quote\"\"", toks[3]);
  }

  @Test
  public void testEscapedDoubleQuoteAsDataElementAlwaysQuoteOutputAndStrictOutputAndRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        retainEscapeChars(false).
        strictQuotes(true).
        build();
    //                                        "test","this,test,is,good","\"test\",\"quote\""
    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); 

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\"test\"\"", toks[2]);
    assertEquals("\"\"quote\"\"", toks[3]);
  }
  
  
  @Test
  public void testIssue2859181WithAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().
        separator(';').
        alwaysQuoteOutput(true).
        build();

    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("\"field1\"", toks[0]);
    assertEquals("\"\\=field2\"", toks[1]);
    assertEquals("\"\"\"field3\"\"\"", toks[2]);
  }

  
  @Test
  public void testIssue2859181WithAlwaysQuoteOutputAndStrictQuotes() {
    CsvParser p = new CsvParserBuilder().
        separator(';').
        alwaysQuoteOutput(true).
        strictQuotes(true).
        build();

    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals("", toks[1]);
    assertEquals("\"field3\"", toks[2]);
  }
  
  
  @Test
  public void testIssueThorny1dAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    String[] toks = p.parseLine("a,\" \"hello\" \",c");
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\" \"hello\" \"", toks[1]);
    assertEquals("\"c\"", toks[2]);
    
    p = new CsvParserBuilder().quoteChar('\'').alwaysQuoteOutput(true).build();
    toks = p.parseLine("a,' 'hello' ',c");
    assertEquals(3, toks.length);
    assertEquals("'a'", toks[0]);
    assertEquals("' 'hello' '", toks[1]);
    assertEquals("'c'", toks[2]);

    p = new CsvParserBuilder().
        quoteChar('\'').
        trimWhitespace(true).
        alwaysQuoteOutput(true).
        build();
    toks = p.parseLine("a,' 'hello' ',c");
    assertEquals(3, toks.length);
    assertEquals("'a'", toks[0]);
    assertEquals("' 'hello' '", toks[1]);
    assertEquals("'c'", toks[2]);
  }
  
  
  @Test
  public void testADoubleQuoteAsDataElementAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    
    String[] toks = p.parseLine("a,\"\"\"\",c");  // a,"""",c

    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"\"\"\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }

  @Test
  public void testIssue2263439AlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().
        quoteChar('\'').
        alwaysQuoteOutput(true).
        build();

    String text = "865,0,'AmeriKKKa\\'s_Most_Wanted','',294,0,0,0.734338696798625,'20081002052147',242429208,18448";
    String[] toks = p.parseLine(text);

    assertEquals(11, toks.length);
    assertEquals("'865'", toks[0]);
    assertEquals("'0'", toks[1]);
    assertEquals("'AmeriKKKa\\'s_Most_Wanted'", toks[2]);
    assertEquals("''", toks[3]);
    assertEquals("'18448'", toks[10]);
  }
  
  @Test
  public void testIssue2726363AlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        build();

    String[] toks = p.parseLine("\"804503689\",\"London\",\"\"London\"shop\",\"address\",\"116.453182\",\"39.918884\"");

    assertEquals(6, toks.length);
    assertEquals("\"804503689\"", toks[0]);
    assertEquals("\"London\"", toks[1]);
    assertEquals("\"\"London\"shop\"", toks[2]);
    assertEquals("\"address\"", toks[3]);
    assertEquals("\"116.453182\"", toks[4]);
    assertEquals("\"39.918884\"", toks[5]);
  }
  
  
  @Test    // https://sourceforge.net/p/opencsv/bugs/93/
  public void testIssueSfBugs93AlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().separator(';').alwaysQuoteOutput(true).build();

    String[] toks = p.parseLine("\"\";1");
    assertEquals(2, toks.length);
    assertEquals("\"\"", toks[0]);
    assertEquals("\"1\"", toks[1]);

    toks = p.parseLine("\"\";2");
    assertEquals(2, toks.length);
    assertEquals("\"\"", toks[0]);
    assertEquals("\"2\"", toks[1]);
  }
  
  @Test
  public void testWithSpacesAndTabsAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();

    String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals(" \t      \"a\"", toks[0]);
    assertEquals("\"b\"      \t       ", toks[1]);
    assertEquals("   \"c\"   ", toks[2]);
  }

  @Test
  public void testWithSpacesAndTabsAlwaysQuoteOutputWithTrimWhitespace() {
    CsvParser p = new CsvParserBuilder().
        trimWhitespace(true).
        alwaysQuoteOutput(true).
        build();

    String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);

    // should get the same result with retainQuotes also thrown in
    p = new CsvParserBuilder().
        trimWhitespace(true).
        alwaysQuoteOutput(true).
        retainOuterQuotes(true).
        build();

    toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  
  @Test
  public void testWithSpacesAndTabsAlwaysQuoteOutputRetainOuterQuotes() {
    CsvParser p = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        retainOuterQuotes(true).
        build();

    String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals(" \t      \"a\"", toks[0]);
    assertEquals("\"b\"      \t       ", toks[1]);
    assertEquals("   \"c\"   ", toks[2]);
  }

  @Test
  public void testStrictQuoteWithSpacesAndTabsAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).alwaysQuoteOutput(true).build();

    String testString = " \t      \"a\",\"b\"      \t       ,   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  @Test
  public void testStrictQuoteWithEverythingToggled() {
    CsvParser p = new CsvParserBuilder().
        separator(':').
        strictQuotes(true).
        alwaysQuoteOutput(true).
        trimWhitespace(true).
        allowUnbalancedQuotes(true).
        retainEscapeChars(false).
        build();

    String testString = " \t      \"a\":\"b\"      \\\t       :   \"c\"   ";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  @Test
  public void testLongTokensAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).build();

    String[] toks = p.parseLine(longLine);
    assertEquals(3, toks.length);
    assertEquals("\"" + longEntry1 + "\"", toks[0]);
    assertEquals("", toks[1]);
    assertEquals(" \"" + longEntry2 + "\"", toks[2]);
  }
  
  
  /* ------------------------------------- */  
  /* ---[ Various Mixed Mode Settings ]--- */
  /* ------------------------------------- */  
  
  @Test
  public void testCrLfAsWhiteSpace() {
    CsvParser p = new CsvParserBuilder().
        trimWhitespace(true).
        build();

    String[] toks = p.parseLine("a\n,\r\nb\n,c\r");
    assertEquals(3, toks.length);
    assertEquals("a", toks[0]);
    assertEquals("b", toks[1]);
    assertEquals("c", toks[2]);
  }
  
  @Test
  public void testCrLfAsWhiteSpaceAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().
        trimWhitespace(true).
        alwaysQuoteOutput(true).
        build();

    String[] toks = p.parseLine("a\n,\r\nb\n,c\r");
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  @Test
  public void testIssue2859181WithRetainQuotesAndRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().
        separator(';').
        retainOuterQuotes(true).
        retainEscapeChars(false).
        build();

    String[] toks = p.parseLine("field1;\\=field2;\"\"\"field3\"\"\""); // field1;\=field2;"""field3"""

    assertEquals(3, toks.length);
    assertEquals("field1", toks[0]);
    assertEquals("=field2", toks[1]);
    assertEquals("\"\"\"field3\"\"\"", toks[2]);
  }
  
  @Test
  public void testEscapedDoubleQuoteAsDataElementWithRetainQuotesAndRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).retainEscapeChars(false).build();

    String[] toks = p.parseLine("\"test\",\"this,test,is,good\",\"\\\"test\\\"\",\"\\\"quote\\\"\""); // "test","this,test,is,good","\"test\",\"quote\""

    assertEquals(4, toks.length);
    assertEquals("\"test\"", toks[0]);
    assertEquals("\"this,test,is,good\"", toks[1]);
    assertEquals("\"\"test\"\"", toks[2]);
    assertEquals("\"\"quote\"\"", toks[3]);
  }
  
  @Test
  public void parseSimpleQuotedStringWithSpacesWithRetainQuotesAndStrictQuotes() {
    CsvParser p = new CsvParserBuilder().
        strictQuotes(true).
        retainOuterQuotes(true).
        build();

    String[] toks = p.parseLine(" \"a\" , \"b\" , \"c\" ");
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  
  @Test
  public void parseSimpleQuotedStringWithSpacesPipeDelimitedWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().
        separator('|').
        strictQuotes(true).
        retainOuterQuotes(true).
        build();

    String[] toks = p.parseLine(" \"a\" | \"b\" | \"c\" ");
    assertEquals(3, toks.length);
    assertEquals("\"a\"", toks[0]);
    assertEquals("\"b\"", toks[1]);
    assertEquals("\"c\"", toks[2]);
  }
  
  @Test
  public void testParsedLineWithInternalQuotaWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().retainOuterQuotes(true).build();
    String[] toks = p.parseLine("a,123\"4\"567,c");

    assertEquals(3, toks.length);
    assertEquals("123\"4\"567", toks[1]);
  }
  
  
  // An opencsv issue: // https://issues.sonatype.org/browse/OSSRH-6159
  // The simplecsv code base leaves the trailing space if "trimWhiteSpace=true"
  // is not invoked, so the behavior is different from original opencsv
  @Test  
  public void testTrailingSpace() {
    // trailing space
    String[] toks = parser.parseLine("\"1\" ,\"2\"");
    assertEquals(2, toks.length);
    assertEquals("1 ", toks[0]);
    assertEquals("2", toks[1]);

    CsvParser p = new CsvParserBuilder().
        trimWhitespace(true).
        build();
    toks = p.parseLine("\"1\" ,\"2\"");
    assertEquals(2, toks.length);
    assertEquals("1", toks[0]);
    assertEquals("2", toks[1]);

    p = new CsvParserBuilder().
        retainOuterQuotes(true).
        trimWhitespace(true).
        build();
    toks = p.parseLine("\"1\" ,\"2\"");
    assertEquals(2, toks.length);
    assertEquals("\"1\"", toks[0]);
    assertEquals("\"2\"", toks[1]);
  }

  
  @Test
  public void spacesAtEndOfQuotedStringDoNotCountIfStrictQuotesIsTrueWithRetainQuotes() {
    CsvParser p = new CsvParserBuilder().
        strictQuotes(true).
        retainOuterQuotes(true).
        build();
    String[] toks = p.parseLine("\"Line with\", \"spaces at end\"  ");
    assertEquals(2, toks.length);
    assertEquals("\"Line with\"", toks[0]);
    assertEquals("\"spaces at end\"", toks[1]);
  }
  
  @Test
  public void testStrictQuoteWithGarbageWithRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().strictQuotes(true).retainEscapeChars(false).build();
    String testString = "abc',!@#\",\\\"\"   xyz,";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals(",\"", toks[1]);
    assertEquals("", toks[2]);
  }
  
  @Test
  public void testStrictQuoteWithGarbageWithRetainEscapeCharsFalseAlwaysQuoteChars() {
    CsvParser p = new CsvParserBuilder().
        strictQuotes(true).
        retainEscapeChars(false).
        alwaysQuoteOutput(true).
        build();
    String testString = "abc',!@#\",\\\"\"   xyz,";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("", toks[0]);
    assertEquals("\",\"\"", toks[1]);
    assertEquals("", toks[2]);
  }
  
  @Test
  public void testWithGarbageWithRetainEscapeCharsFalseAndAlwaysQuoteOutput() {
    CsvParser p = new CsvParserBuilder().alwaysQuoteOutput(true).retainEscapeChars(false).build();
    String testString = "abc',!@#\",\\\"\"   xyz,";

    String[] toks = p.parseLine(testString);
    assertEquals(3, toks.length);
    assertEquals("\"abc'\"", toks[0]);
    assertEquals("\"!@#\",\"\"   xyz\"", toks[1]);
    assertEquals("", toks[2]);
  }
  
  @Test
  public void whitespaceBeforeEscapeWithAllowUnbalancedQuotesWithRetainEscapeCharsFalse() {
    CsvParser p = new CsvParserBuilder().
        allowUnbalancedQuotes(true).
        retainEscapeChars(false).
        build();

    String[] toks = p.parseLine("\"this\", \"is\",\"a test\""); //"this", "is","a test"
    assertEquals("this", toks[0]);
    assertEquals(" is", toks[1]);
    assertEquals("a test", toks[2]);
  }
  
  
  /* ---[ Table Examples in simplecsv documentation ]--- */
 
  @Test
  public void testDocTableExample1() {
    String text = "\"abc\"d\"efg\",1,\"2\", w\"x\"y\"\"z ";
    String[] toks = null;
    CsvParser p1 = new CsvParserBuilder().strictQuotes(true).build();
    CsvParser p2 = new CsvParserBuilder().retainOuterQuotes(true).build();
    CsvParser p3 = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    CsvParser p4 = new CsvParserBuilder().strictQuotes(true).retainOuterQuotes(true).build();
    CsvParser p5 = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    
    // default mode
    toks = parser.parseLine(text);  // [abc"d"efg, 1, 2,  w"x"y""z ]  // CORRECT
    String asList = Arrays.asList(toks).toString();
    String exp = "[abc\"d\"efg, 1, 2,  w\"x\"y\"\"z ]";
    assertEquals(exp, asList);
    
    toks = p1.parseLine(text);      // [abc"d"efg, 1, 2,  w"x"y""z ]  // WRONG=>[abcefg, , 2, x]
    asList = Arrays.asList(toks).toString();
    exp = "[abcefg, , 2, x]";
    assertEquals(exp, asList);
    
    toks = p2.parseLine(text);      // ["abc"d"efg", 1, "2",  w"x"y""z ]
    asList = Arrays.asList(toks).toString();
    exp = "[\"abc\"d\"efg\", 1, \"2\",  w\"x\"y\"\"z ]";
    assertEquals(exp, asList);

    toks = p3.parseLine(text);      // [abcdefg, 1, 2,  wxyz ]  // WRONG
    asList = Arrays.asList(toks).toString();
    exp = "[abc\"d\"efg, 1, 2,  w\"x\"y\"\"z ]";
    assertEquals(exp, asList);
    
    toks = p4.parseLine(text);      // ["abc"d"efg", 1, "2",  w"x"y""z ]
    asList = Arrays.asList(toks).toString();
    exp = "[\"abcefg\", , \"2\", \"x\"]";
    assertEquals(exp, asList);
    
    toks = p5.parseLine(text);      // ["abc"d"efg", 1, "2",  w"x"y""z ]
    asList = Arrays.asList(toks).toString();
    exp = "[\"abc\"d\"efg\", \"1\", \"2\", \" w\"x\"y\"\"z \"]";
    assertEquals(exp, asList);
  }
  
  
  @Test
  public void testDocTableExample2() {
    String text = "1,\"abc\\\"d\\\"efg\"";
    String[] toks = null;
    CsvParser p1 = new CsvParserBuilder().strictQuotes(true).build();
    CsvParser p2 = new CsvParserBuilder().retainOuterQuotes(true).build();
    CsvParser p3 = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    CsvParser p4 = new CsvParserBuilder().strictQuotes(true).retainOuterQuotes(true).build();
    CsvParser p5 = new CsvParserBuilder().alwaysQuoteOutput(true).build();
    CsvParser p6 = new CsvParserBuilder().strictQuotes(true).alwaysQuoteOutput(true).build();
        
    // default mode
    toks = parser.parseLine(text);
    String asList = Arrays.asList(toks).toString();
    String exp = "[1, abc\\\"d\\\"efg]";
    assertEquals(exp, asList);
    
    toks = p1.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, abc\\\"d\\\"efg]";
    assertEquals(exp, asList);

    toks = p2.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1, \"abc\\\"d\\\"efg\"]";
    assertEquals(exp, asList);

    toks = p3.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1, abc\\\"d\\\"efg]";
    assertEquals(exp, asList);

    toks = p4.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, \"abc\\\"d\\\"efg\"]";
    assertEquals(exp, asList);
    
    toks = p5.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[\"1\", \"abc\\\"d\\\"efg\"]";
    assertEquals(exp, asList);
    
    toks = p6.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, \"abc\\\"d\\\"efg\"]";
    assertEquals(exp, asList);
  }

  @Test
  public void testDocTableExample3() {
    String text = "1, \"abc\"def\"";
    String[] toks = null;
    CsvParser p1 = new CsvParserBuilder().strictQuotes(true).build();
    CsvParser p2 = new CsvParserBuilder().retainOuterQuotes(true).build();
    CsvParser p3 = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    CsvParser p4 = new CsvParserBuilder().retainOuterQuotes(true).allowUnbalancedQuotes(true).build();
    CsvParser p5 = new CsvParserBuilder().
        strictQuotes(true).
        retainOuterQuotes(true).
        allowUnbalancedQuotes(true).
        build();
    CsvParser p6 = new CsvParserBuilder().
        strictQuotes(true).
        alwaysQuoteOutput(true).
        allowUnbalancedQuotes(true).
        build();
    CsvParser p7 = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        allowUnbalancedQuotes(true).
        build();
    CsvParser p8 = new CsvParserBuilder().
        alwaysQuoteOutput(true).
        allowUnbalancedQuotes(true).
        trimWhitespace(true).
        build();
    
    // default mode
    boolean parseError = false;
    try {
      parser.parseLine(text);
    } catch (IllegalArgumentException e) {
      parseError = true;
    } finally {
      assertTrue(parseError);
      parseError = false;
    }
    
    try {
      p1.parseLine(text);
    } catch (IllegalArgumentException e) {
      parseError = true;
    } finally {
      assertTrue(parseError);
      parseError = false;
    }

    try {
      p2.parseLine(text);
    } catch (IllegalArgumentException e) {
      parseError = true;
    } finally {
      assertTrue(parseError);
      parseError = false;
    }
    
    toks = p3.parseLine(text);
    String asList = Arrays.asList(toks).toString();
    String exp = "[1,  abc\"def]";
    assertEquals(exp, asList);

    toks = p4.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1,  \"abc\"def\"]";
    assertEquals(exp, asList);

    toks = p5.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, \"abc\"]";
    assertEquals(exp, asList);
    
    toks = p6.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, \"abc\"]";
    assertEquals(exp, asList);
    
    toks = p7.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[\"1\",  \"abc\"def\"]";
    assertEquals(exp, asList);

    toks = p8.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[\"1\", \"abc\"def\"]";
    assertEquals(exp, asList);
  }
  
  @Test
  public void testDocTableExample4() {
    String text = "1,\"abc\\\"def\"";
    String[] toks = null;
    CsvParser p1 = new CsvParserBuilder().strictQuotes(true).build();
    CsvParser p2 = new CsvParserBuilder().retainOuterQuotes(true).build();
    CsvParser p3 = new CsvParserBuilder().allowUnbalancedQuotes(true).build();
    CsvParser p4 = new CsvParserBuilder().retainOuterQuotes(true).allowUnbalancedQuotes(true).build();
    CsvParser p5 = new CsvParserBuilder().
        strictQuotes(true).
        retainOuterQuotes(true).
        allowUnbalancedQuotes(true).
        build();
    
    // default mode
    toks = parser.parseLine(text);
    String asList = Arrays.asList(toks).toString();
    String exp = "[1, abc\\\"def]";
    assertEquals(exp, asList);
    
    toks = p1.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, abc\\\"def]";
    assertEquals(exp, asList);

    toks = p2.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1, \"abc\\\"def\"]";
    assertEquals(exp, asList);
    
    toks = p3.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1, abc\\\"def]";
    assertEquals(exp, asList);

    toks = p4.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[1, \"abc\\\"def\"]";
    assertEquals(exp, asList);

    toks = p5.parseLine(text);
    asList = Arrays.asList(toks).toString();
    exp = "[, \"abc\\\"def\"]";
    assertEquals(exp, asList);
  }
}
