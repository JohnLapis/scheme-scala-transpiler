package transpiler.scheme;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class SchemeScannerTest
{
    @Test
    public void certainTokensShouldBeNumbers()
    {
        List<String> tokenValues = Arrays.asList("+i", "-i", "+inf.0", "-inf.0", "+nan.0", "-nan.0");
        for (String tokenValue : tokenValues) {
            List<Token> expectedTokens =
                Arrays.asList(new Token(TokenType.NUMBER, tokenValue));
            List<Token> outputTokens = SchemeScanner.tokenize(tokenValue);
            compareLists(expectedTokens, outputTokens);
        }

        String input = "(" + String.join(", ", tokenValues) + ")";
        for (Token token : SchemeScanner.tokenize(input)) {
            TokenType expectedType;
            if (token.value == "(" || token.value == "," || token.value == ")") {
                expectedType = TokenType.DELIMITER;
            } else {
                expectedType = TokenType.NUMBER;
            }
            assertEquals(expectedType, token.type);
        }
    }

    @Ignore("TODO")
    @Test
    public void directivesMustBeFollowedByDelimiter()
    {
        assertTrue(false);
    }


    @Test
    public void schemeCodeShouldBeTokenized ()
    {
        List<String> expectedValue = Arrays.asList("(", "+", "1", "(", "+", "2", "3", ")", ")");
        assertEquals(expectedValue,
                     getTokensValues(SchemeScanner.tokenize("(+ 1 (+ 2 3))"))) ;
    }

    static List<String> getTokensValues(List<Token> tokens)
    {
        List<String> values = new ArrayList<>();
        for (Token token : tokens) {
            values.add(token.value);
        }
        return values;
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
