package transpiler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemeScanner
{
    public static List<String> tokenize(String code)
    {
        return new ArrayList<String>();
    }

    public static SchemeAST parse(List<String> tokens)
    {
        return new SchemeAST();
    }

    public static IntermediateAST convertSchemeToIntermediateAST(SchemeAST ast)
    {
        return new IntermediateAST();
    }
}
