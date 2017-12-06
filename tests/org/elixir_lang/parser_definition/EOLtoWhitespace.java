package org.elixir_lang.parser_definition;

public class EOLtoWhitespace extends ParsingTestCase {
   public void testCapture() {
      assertParsedAndQuotedCorrectly();
   }

   public void testComparison() {
      assertParsedAndQuotedCorrectly();
   }

   public void testInMatch() {
      assertParsedAndQuotedCorrectly();
   }

   public void testOpeningParentheses() {
      assertParsedAndQuotedCorrectly();
   }

   public void testOr() {
      assertParsedAndQuotedCorrectly();
   }

   public void testPipe() {
      assertParsedAndQuotedCorrectly();
   }

   public void testSemicolon() {
      assertParsedAndQuotedCorrectly();
   }

   public void testWhen() {
      assertParsedAndQuotedCorrectly();
   }

   public void testType() {
      assertParsedAndQuotedCorrectly();
   }

   @Override
   protected String getTestDataPath() {
      return super.getTestDataPath() + "/eol_to_whitespace";
   }
}
