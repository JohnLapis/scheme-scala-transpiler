package transpiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemeScanner
{
    record GroupPattern(String regex) {}
    record RawPattern(String regex) {}

    static GroupPattern LINE_ENDING = or("\n", "\r", "\r\n");
    static GroupPattern INTRALINE_WHITESPACE = or(" ", "\t");
    static GroupPattern WHITESPACE = or(INTRALINE_WHITESPACE, LINE_ENDING);
    static GroupPattern DELIMITER = or(WHITESPACE, "|", "(", ")", "\"", ";");
    static RawPattern COMMENT_TEXT = raw("((?!(#\\||\\|#)).)+");
    // static String COMMENT_CONT = and( NESTED_COMMENT, COMMENT_TEXT);
    static RawPattern COMMENT_CONT = COMMENT_TEXT;
    static GroupPattern NESTED_COMMENT = and("#|",
                                             COMMENT_TEXT,
                                             raw("(" + COMMENT_CONT.regex + ")*"),
                                             "|#");
    static GroupPattern COMMENT = or(and(";", raw(".*?"), LINE_ENDING),
                                     // and("#;", INTERTOKEN_SPACE, DATUM)
                                     NESTED_COMMENT);
    static GroupPattern DIRECTIVE = or("#!fold-case", "#!no-fold-case");
    static GroupPattern ATMOSPHERE = or(WHITESPACE, COMMENT, DIRECTIVE);
    static RawPattern INTERTOKEN_SPACE = raw(ATMOSPHERE.regex + "*");

    static GroupPattern SIGN = or("", "+", "-");
    static GroupPattern INFNAN = or("+inf.0", "-inf.0", "+nan.0", "-nan.0");
    static String EXPONENT_MARKER = "e";
    static GroupPattern SUFFIX = or("", and(EXPONENT_MARKER, SIGN, raw(DIGIT(10).regex + "+")));
    static GroupPattern EXACTNESS = or("", "#e", "#i");

    static GroupPattern RADIX(int n)
    {
        return switch (n) {
        case 2 -> new GroupPattern("#b");
        case 8 -> new GroupPattern("#o");
        case 10 -> or("", "#d");
        case 16 -> new GroupPattern("#x");
        default -> RADIX(10);
        };
    }

    static GroupPattern DIGIT(int n)
    {
        return switch (n) {
        case 2 -> or("0", "1");
        case 8 -> or("0", "1", "2", "3", "4", "5", "6", "7");
        case 10 -> or("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        case 16 -> or(DIGIT(10), raw("[a-f]"));
        default -> DIGIT(10);
        };
    }

    static GroupPattern PREFIX(int n)
    {
        return or(and(RADIX(n), EXACTNESS), and(EXACTNESS, RADIX(n)));
    }

    static RawPattern UINTEGER(int n)
    {
        return raw(DIGIT(n).regex + "+");
    }

    static GroupPattern DECIMAL_10 = or(and(UINTEGER(10), SUFFIX),
                                        and(".", raw(DIGIT(10).regex + "+"), SUFFIX),
                                        and(raw(DIGIT(10).regex + "+"),
                                            ".",
                                            raw(DIGIT(10).regex + "*"),
                                            SUFFIX));

    static GroupPattern UREAL(int n)
    {
        GroupPattern pattern;
        if (n == 10) {
            pattern = or(UINTEGER(n),
                         and(UINTEGER(n), "/", UINTEGER(n)),
                         DECIMAL_10);
        } else {
            pattern = or(UINTEGER(n),
                         and(UINTEGER(n), "/", UINTEGER(n)));
        }
        return pattern;
    }

    static GroupPattern REAL(int n)
    {
        return or(and(SIGN, UREAL(n)), INFNAN);
    }

    static GroupPattern COMPLEX(int n)
    {
        return or(REAL(n),
                  and(REAL(n), "@", REAL(n)),
                  and(REAL(n), "+", UREAL(n), "i"),
                  and(REAL(n), "-", UREAL(n), "i"),
                  and(REAL(n), "+", "i"),
                  and(REAL(n), "-", "i"),
                  and(REAL(n), INFNAN, "i"),
                  and("+", UREAL(n), "i"),
                  and("-", UREAL(n), "i"),
                  and(INFNAN, "i"),
                  and("+", "i"));
    }

    static GroupPattern NUMBER_(int n)
    {
        return and(PREFIX(n), COMPLEX(n));
    }

    static GroupPattern NUMBER = or(NUMBER_(2), NUMBER_(8), NUMBER_(10), NUMBER_(16));

    static GroupPattern BOOLEAN = or("#t", "#f", "#true", "#false");

    static RawPattern LETTER = raw("[a-zA-Z]");
    static GroupPattern SPECIAL_INITIAL = or("!", "$", "%", "&", "*", "/", ":", "<", "=", ">", "?", "^", "_", "~");
    static GroupPattern INITIAL = or(LETTER, SPECIAL_INITIAL);
    static GroupPattern EXPLICIT_SIGN = or("+", "-");
    static GroupPattern SPECIAL_SUBSEQUENT = or(EXPLICIT_SIGN, ".", "@");
    static GroupPattern SUBSEQUENT = or(INITIAL, DIGIT(10), SPECIAL_SUBSEQUENT);
    static RawPattern HEX_SCALAR_VALUE = raw(DIGIT(16).regex + "+");
    static GroupPattern INLINE_HEX_ESCAPE = and("\\\\x", HEX_SCALAR_VALUE, ";");
    static GroupPattern MNEMONIC_ESCAPE = or("\\\\a", "\\\\b", "\\\\t", "\\\\n", "\\\\r");
    static GroupPattern SYMBOL_ELEMENT = or(raw("[^|\\\\]"),
                                            INLINE_HEX_ESCAPE,
                                            MNEMONIC_ESCAPE,
                                            "\\\\|");
    static GroupPattern SIGN_SUBSEQUENT = or(INITIAL, EXPLICIT_SIGN, "@");
    static GroupPattern DOT_SUBSEQUENT = or(SIGN_SUBSEQUENT, ".");
    static GroupPattern PECULIAR_IDENTIFIER = or(EXPLICIT_SIGN,
                                                 and(EXPLICIT_SIGN, SIGN_SUBSEQUENT, raw(SUBSEQUENT.regex + "*")),
                                                 and(EXPLICIT_SIGN, ".", DOT_SUBSEQUENT, raw(SUBSEQUENT.regex + "*")),
                                                 and(".", DOT_SUBSEQUENT, raw(SUBSEQUENT.regex + "*")));
    static GroupPattern IDENTIFIER = or(and(INITIAL, raw(SUBSEQUENT.regex + "*")),
                                        and("|", raw(SYMBOL_ELEMENT.regex + "*"), "|"),
                                              PECULIAR_IDENTIFIER);

    static GroupPattern CHARACTER_NAME = or("alarm", "backspace", "delete", "escape", "newline", "null", "return", "space", "tab");
    static GroupPattern CHARACTER = or(raw("#\\\\."),
                                       and("#\\\\", CHARACTER_NAME),
                                             and("#\\\\x", HEX_SCALAR_VALUE));

    static GroupPattern STRING_ELEMENT = or(raw("[^\"\\\\]"),
                                            MNEMONIC_ESCAPE,
                                            "\\\\\"",
                                            "\\\\\\\\",
                                            and("\\\\",
                                                raw(INTRALINE_WHITESPACE.regex + "*"),
                                                LINE_ENDING,
                                                raw(INTRALINE_WHITESPACE.regex + "*")),
                                            INLINE_HEX_ESCAPE);
    static GroupPattern STRING = and("\"", raw(STRING_ELEMENT.regex + "*"), "\"");

    static RawPattern BYTE = raw("(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])"); // TODO incomplete.
    static GroupPattern BYTEVECTOR = and("#u8(", raw(BYTE.regex + "*"), ")");

    static List<String> TOKEN_DEFINITIONS =
        Arrays.asList(
                      IDENTIFIER.regex,
                      BOOLEAN.regex,
                      NUMBER.regex,
                      CHARACTER.regex,
                      STRING.regex,
                      or("(", ")", "#(", "#u8(", "'", "`", ",", ",@", ".").regex
                      );

    static GroupPattern TOKEN = or(IDENTIFIER,
                                   BOOLEAN,
                                   NUMBER,
                                   CHARACTER,
                                   STRING,
                                   "(", ")", "#(", "#u8(", "'", "`", ",", ",@", ".");

    static Map<String, String> REGEX_DEFINITIONS =
        Map.of("TOKEN", TOKEN.regex,
               "INTERTOKEN_SPACE", INTERTOKEN_SPACE.regex);

    static String getRegexStr(Object obj)
    {
        String str;
        if (obj instanceof String s) {
            str = escape(s);
        } else if (obj instanceof RawPattern r) {
            str = r.regex;
        } else if (obj instanceof GroupPattern g) {
            str = g.regex;
        } else {
            throw new RuntimeException("Expected a String, RawPattern, or GroupPattern.");
        }
        return str;
    }

    static List<String> getRegexStr(Object[] regexes)
    {
        List<String> regexStrs = new ArrayList<>();
        for (Object regex : regexes) {
            regexStrs.add(getRegexStr(regex));
        }
        return regexStrs;
    }

    static RawPattern raw(String str)
    {
        return new RawPattern(str);
    }

    static String escape(String str)
    {
        return "\\Q" + str + "\\E";
    }

    static GroupPattern and(Object... regexes)
    {
        return new GroupPattern("(" + String.join("", getRegexStr(regexes)) + ")");
    }

    static GroupPattern or(Object... regexes)
    {
        return new GroupPattern("(" + String.join("|", getRegexStr(regexes)) + ")");
    }

    public static List<String> tokenize(String code)
    {
        int curPos = 0;
        int codeLength = code.length();
        List<String> tokens = new ArrayList<>();

        while (curPos < codeLength) {
            curPos = skipIntertokenSpace(code, curPos);
            String token = matchToken(code, curPos);
            if (token.length() == 0) {
                break;
            }
            tokens.add(token);
            curPos += token.length();
        }

        if (curPos != codeLength) {
            throw new RuntimeException("No valid token was found at position " + curPos + ".");
        }

        return tokens;
    }

    static public int skipIntertokenSpace(String code, int startPos)
    {
        Matcher matcher = Pattern.compile("^" + INTERTOKEN_SPACE.regex)
            .matcher(code.substring(startPos));
        return startPos + (matcher.find() ? matcher.end() : 0);
    }

    static public String matchToken(String code, int startPos)
    {
        for (String definition : TOKEN_DEFINITIONS) {
            Matcher matcher = Pattern.compile("^" + definition)
                    .matcher(code.substring(startPos));
            if (!matcher.find()) continue;
            if (matcher.group() == "") {
                // throw some error
            }
            return matcher.group();
        }

        return "";
    }

    /**
     * Substrings of the form "${VAR}" will be replaced with the value corresponding
     * to the key VAR in REGEX_DEFINITIONS if present.
     */
    static String interpolateTokenizationRegex(String string)
    {
        Matcher matcher = Pattern.compile("\\$\\{\\w+\\}").matcher(string);
        List<String> regexParts = new ArrayList<>();
        int lastIndex = 0;
        while (matcher.find()) {
            regexParts.add(string.substring(lastIndex, matcher.start()));
            // We skip the matched "${" and "}".
            String definitionValue = REGEX_DEFINITIONS.get(string.substring(matcher.start() + 2,
                                                                            matcher.end() - 1));
            if (definitionValue != null) {
                regexParts.add(definitionValue);
            }
            lastIndex = matcher.end();
        }
        regexParts.add(string.substring(lastIndex, string.length()));
        return String.join("", regexParts);
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
