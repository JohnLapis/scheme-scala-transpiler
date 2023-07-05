package transpiler;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Arrays;

import org.junit.Test;

public class SchemeScannerTest
{
    @Test
    public void schemeCodeShouldBeTokenized ()
    {
        List<String> expectedValue = Arrays.asList("(", "+", "1", "(", "+", "2", "3", ")", ")");
        assertEquals(expectedValue, SchemeScanner.tokenize("(+ 1 (+ 2 3))"));
    }
}
