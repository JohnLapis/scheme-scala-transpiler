package transpiler.scheme;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class SchemeScannerTest
{
    @Test
    public void certainTokensShouldBeNumbers()
    {
        List<String> tokenValues = Arrays.asList("+i", "-i", "+inf.0", "-inf.0", "+nan.0", "-nan.0");

        // Test values individually.
        for (String tokenValue : tokenValues) {
            List<Token> expectedTokens =
                Arrays.asList(new Token(TokenType.NUMBER, tokenValue));
            List<Token> outputTokens = SchemeScanner.tokenize(tokenValue);
            compareLists(expectedTokens, outputTokens);
        }

        // Test values in a list.
        List<Token> expectedTokens = new ArrayList<>();
        expectedTokens.add(new Token(TokenType.DELIMITER, "("));
        for (String tokenValue : tokenValues) {
            expectedTokens.add(new Token(TokenType.NUMBER, tokenValue));
        }
        expectedTokens.add(new Token(TokenType.DELIMITER, ")"));
        String inputCode = "(" + String.join(" ", tokenValues) + ")";
        compareLists(expectedTokens, SchemeScanner.tokenize(inputCode));
    }

    @Ignore("TODO")
    @Test
    public void directivesMustBeFollowedByDelimiter()
    {
        assertTrue(false);
    }


    @Test
    public void schemeCodeShouldBeTokenized()
    {
        List<Token> expectedTokens =
            Arrays.asList(new Token(TokenType.DELIMITER, "("),
                          new Token(TokenType.IDENTIFIER, "+"),
                          new Token(TokenType.NUMBER, "1"),
                          new Token(TokenType.DELIMITER, "("),
                          new Token(TokenType.IDENTIFIER, "+"),
                          new Token(TokenType.NUMBER, "2"),
                          new Token(TokenType.NUMBER, "3"),
                          new Token(TokenType.DELIMITER, ")"),
                          new Token(TokenType.DELIMITER, ")"));
        compareLists(expectedTokens, SchemeScanner.tokenize("(+ 1 (+ 2 3))"));
    }

    static void compareLists(List<?> list1, List<?> list2)
    {
        int size = list1.size();
        assertEquals(size, list2.size());
        for (int i = 0; i < size; i++) {
            assertEquals(list1.get(i), list2.get(i));
        }
    }
}
