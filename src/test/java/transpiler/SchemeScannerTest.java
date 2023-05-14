package transpiler;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class SchemeScannerTest
{
    @Test
    public void interpolateStringWithExistingTokenDefinitions ()
    {
        List<String> keys = new ArrayList<>();
        for (String key : SchemeScanner.REGEX_DEFINITIONS.keySet()) {
            keys.add("${" + key + "}");
        }
        String separator = "sep";
        String input = separator + String.join(separator, keys) + separator;
        String expectedString =
            separator
            + String.join(separator, SchemeScanner.REGEX_DEFINITIONS.values())
            + separator;
        assertEquals(expectedString, SchemeScanner.interpolateTokenizationRegex(input));
    }

    @Test
    public void interpolateStringWithNonExistentTokenDefinitions ()
    {
        assertEquals("testzzz", SchemeScanner.interpolateTokenizationRegex("test${whatever}zzz"));
    }
}
