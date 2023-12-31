package transpiler.scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import transpiler.ASTNode;

enum ParserState {REPEAT, NEXT, STOP, BACKTRACK}

enum Modifier {PLUS, ASTERISK}

enum TermType {TERMINAL, NONTERMINAL, PATTERN}

class TermMatch
{
    Integer index;
    Object value;

    public TermMatch(Integer index, Object value)
    {
        this.index = index;
        this.value = value;
    }
}

class Term
{
    String value;
    TermType type;
    Modifier modifier;

    public Term()
    {
        this.value = null;
        this.type = null;
        this.modifier = null;
    }

    public Term(String value, TermType type)
    {
        this.value = value;
        this.type = type;
        this.modifier = null;
    }

    public Term(String value, TermType type, Modifier modifier)
    {
        this.value = value;
        this.type = type;
        this.modifier = modifier;
    }
}

class Expr
{
    List<Term> terms;

    public Expr()
    {
        this.terms = new ArrayList<Term>();
    }

    public Expr(Term... terms)
    {
        this.terms = Arrays.asList(terms);
    }

    public Expr(List<Term> terms)
    {
        this.terms = terms;
    }
}

class Rule
{
    List<Expr> exprs;

    public Rule()
    {
        this.exprs = new ArrayList<Expr>();
    }

    public Rule(Expr... exprs)
    {
        this.exprs = Arrays.asList(exprs);
    }

    public Rule(List<Expr> exprs)
    {
        this.exprs = exprs;
    }
}

public class SchemeParser
{
    static Map<String, Rule> DEFAULT_DEFINITIONS =
        buildDefinitions(
               /* Programs and definitions */
               "PROGRAM", nonterminal(terms(term("IMPORT_DECLARATION", "*"),
                                            term("COMMAND_OR_DEFINITION", "+"))),
               "COMMAND_OR_DEFINITION",
               nonterminal(term("DEFINITION"),
                           terms("(", "begin", term("COMMAND_OR_DEFINITION", "+"), ")"),
                           // COMMAND must have lower precedence since, otherwise,
                           // standard language constructs would be interpreted as
                           // user definitions.
                           term("COMMAND")),
               "DEFINITION",
               nonterminal(terms("(", "define", term("IDENTIFIER"), term("EXPRESSION"), ")"),
                           terms("(",
                                 "define",
                                 "(", term("IDENTIFIER"), term("DEF_FORMALS"), ")",
                                 term("BODY"),
                                 ")"),
                           term("SYNTAX_DEFINITION"),
                           terms("(", "define-values", term("FORMALS"), term("BODY"), ")"),
                           terms("(",
                                 "define-record-type",
                                 term("IDENTIFIER"),
                                 term("CONSTRUCTOR"),
                                 term("IDENTIFIER"),
                                 term("FIELD_SPEC", "*"),
                                 ")"),
                           terms("(", "begin", term("DEFINITION", "*"), ")")),
               "DEF_FORMALS",
               // "." can interpreted as an IDENTIFIER, so the rule with "." pair
               // needs to come first.
               nonterminal(terms(term("IDENTIFIER", "*"),
                                 ".",
                                 term("VAR_PARAMETER")),
                           term("IDENTIFIER", "*")),
               "CONSTRUCTOR", nonterminal(terms("(",
                                                term("IDENTIFIER"),
                                                term("FIELD_NAME", "*"),
                                                ")")),
               "FIELD_SPEC", nonterminal(terms(term("FIELD_NAME"), term("ACCESSOR")),
                                         terms("(",
                                               term("FIELD_NAME"),
                                               term("ACCESSOR"),
                                               term("MUTATOR"),
                                               ")")),
               "FIELD_NAME", nonterminal(term("IDENTIFIER")),
               "ACCESSOR", nonterminal(term("IDENTIFIER")),
               "MUTATOR", nonterminal(term("IDENTIFIER")),
               "SYNTAX_DEFINITION",
               nonterminal(terms("(", "define-syntax", term("KEYWORD"), term("TRANSFORMER_SPEC"), ")")),

               /* Libraries */
               "LIBRARY", nonterminal(terms("(",
                                            "define-library",
                                            term("LIBRARY_NAME"),
                                            term("LIBRARY_DECLARATION", "*"),
                                            ")")),
               "LIBRARY_NAME", nonterminal(terms("(", term("LIBRARY_NAME_PART", "+"), ")")),
               "LIBRARY_NAME_PART", nonterminal(term("IDENTIFIER"), term("UINTEGER_10")),
               "LIBRARY_DECLARATION",
               nonterminal(terms("(", "export", term("EXPORT_SPEC", "*"), ")"),
                           term("IMPORT_DECLARATION"),
                           terms("(", "begin", term("COMMAND_OR_DEFINITION", "*"), ")"),
                           term("INCLUDER"),
                           terms("(", "include-library-declarations", term("STRING", "+"), ")"),
                           terms("(", "cond-expand", term("COND_EXPAND_CLAUSE", "+"), ")"),
                           terms("(",
                                 "cond-expand",
                                 term("COND_EXPAND_CLAUSE", "+"),
                                 "(", "else", term("LIBRARY_DECLARATION", "*"), ")",
                                 ")")),
               "IMPORT_DECLARATION", nonterminal(terms("(", "import", term("IMPORT_SET", "+"), ")")),
               "EXPORT_SPEC", nonterminal(term("IDENTIFIER"),
                                          terms("(", "rename", term("IDENTIFIER"), term("IDENTIFIER"), ")")),
               "IMPORT_SET",
               nonterminal(term("LIBRARY_NAME"),
                           terms("(", "only", term("IMPORT_SET"), term("IDENTIFIER", "+"), ")"),
                           terms("(", "except", term("IMPORT_SET"), term("IDENTIFIER", "+"), ")"),
                           terms("(", "prefix", term("IMPORT_SET"), term("IDENTIFIER"), ")"),
                           terms("(",
                                 "rename",
                                 term("IMPORT_SET"),
                                 term("IMPORT_RENAMING", "+"),
                                 ")")),
               "IMPORT_RENAMING", nonterminal(terms("(", term("IDENTIFIER"), term("IDENTIFIER"), ")")),
               "COND_EXPAND_CLAUSE", nonterminal(terms("(",
                                                       term("FEATURE_REQUIREMENT"),
                                                       term("LIBRARY_DECLARATION", "*"),
                                                       ")")),
               "FEATURE_REQUIREMENT", nonterminal(term("IDENTIFIER"),
                                                  term("LIBRARY_NAME"),
                                                  terms("(",
                                                        "and",
                                                        term("FEATURE_REQUIREMENT", "*"),
                                                        ")"),
                                                  terms("(",
                                                        "or",
                                                        term("FEATURE_REQUIREMENT", "*"),
                                                        ")"),
                                                  terms("(", "not", term("FEATURE_REQUIREMENT"), ")")),

               /* External representations */
               "DATUM", nonterminal(term("SIMPLE_DATUM"),
                                    term("COMPOUND_DATUM"),
                                    terms(term("LABEL"), "=", term("DATUM")),
                                    terms(term("LABEL"), "#")),
               "SIMPLE_DATUM", nonterminal(term("BOOLEAN"),
                                           term("NUMBER"),
                                           term("CHARACTER"),
                                           term("STRING"),
                                           term("IDENTIFIER"),
                                           term("BYTEVECTOR")),

               "COMPOUND_DATUM", nonterminal(term("LIST"), term("VECTOR"), term("ABBREVIATION")),
               "LIST", nonterminal(terms("(", term("DATUM", "*"), ")"),
                                   terms("(", term("DATUM", "+"), ".", term("DATUM"), ")")),
               "ABBREVIATION", nonterminal(terms(term("ABBREV_PREFIX"), term("DATUM"))),
               "ABBREV_PREFIX", nonterminal("'", "`", ",", ",@"),
               "VECTOR", nonterminal(terms("#(", term("DATUM", "*"), ")")),
               "LABEL", nonterminal(terms("#", term("UINTEGER_10"))),

               /* Primitive expressions */
               "EXPRESSION", nonterminal(term("IDENTIFIER"),
                                         term("LITERAL"),
                                         term("LAMBDA_EXPRESSION"),
                                         term("CONDITIONAL"),
                                         term("ASSIGNMENT"),
                                         // term("DERIVED_EXPRESSION"),
                                         // term("MACRO_BLOCK"),
                                         term("INCLUDER"),

                                         // The following terms must have lower
                                         // precedence in the rule because,
                                         // otherwise, some of the previous exprs
                                         // could be parsed as one of the following.
                                         term("PROCEDURE_CALL")
                                         // term("MACRO_USE")),
                                         ),
               "LITERAL", nonterminal(term("QUOTATION"), term("SELF_EVALUATING")),
               "SELF_EVALUATING", nonterminal(term("BOOLEAN"),
                                               term("NUMBER"),
                                               term("VECTOR"),
                                               term("CHARACTER"),
                                               term("STRING"),
                                               term("BYTEVECTOR")),
               "QUOTATION", nonterminal(terms("'", term("DATUM")),
                                        terms("(", "quote", term("DATUM"), ")")),
               "PROCEDURE_CALL", nonterminal(terms("(",
                                                   term("OPERATOR"),
                                                   term("OPERAND", "*"),
                                                   ")")),
               "OPERATOR", nonterminal(term("EXPRESSION")),
               "OPERAND", nonterminal(term("EXPRESSION")),
               "LAMBDA_EXPRESSION",
               nonterminal(terms("(", "lambda", term("FORMALS"), term("BODY"), ")")),
               "FORMALS", nonterminal(terms("(", term("IDENTIFIER", "*"), ")"),
                                      terms(term("IDENTIFIER")),
                                      terms("(",
                                            term("IDENTIFIER", "+"),
                                            ".",
                                            term("VAR_PARAMETER"),
                                            ")")),
               "VAR_PARAMETER", nonterminal(term("IDENTIFIER")),
               "BODY", nonterminal(terms(term("DEFINITION", "*"), term("SEQUENCE"))),
               "SEQUENCE", nonterminal(terms(term("COMMAND", "*"), term("EXPRESSION"))),
               "COMMAND", nonterminal(term("EXPRESSION")),

               "CONDITIONAL", nonterminal(terms("(",
                                               "if",
                                               term("TEST"),
                                               term("CONSEQUENT"),
                                               term("ALTERNATE"),
                                                ")")),
               "TEST", nonterminal(term("EXPRESSION")),
               "CONSEQUENT", nonterminal(term("EXPRESSION")),
               "ALTERNATE", nonterminal(term("EXPRESSION"), ""),

               "ASSIGNMENT", nonterminal(terms("(",
                                               "set!",
                                               term("IDENTIFIER"),
                                               term("EXPRESSION"),
                                               ")")),

               "INCLUDER", nonterminal(terms("(", "include", term("STRING", "+"), ")"),
                                       terms("(", "include-ci", term("STRING", "+"), ")")),

               /* Patterns */
               "BOOLEAN", nonterminal(SchemeScanner.BOOLEAN),
               "NUMBER", nonterminal(SchemeScanner.NUMBER),
               "CHARACTER", nonterminal(SchemeScanner.CHARACTER),
               "STRING", nonterminal(SchemeScanner.STRING),
               "IDENTIFIER", nonterminal(SchemeScanner.IDENTIFIER),
               "BYTEVECTOR", nonterminal(SchemeScanner.BYTEVECTOR),
               "UINTEGER_10", nonterminal(SchemeScanner.UINTEGER(10))
               );

    ListIterator<Token> tokenIter;
    Map<String, Rule> definitions;

    public SchemeParser(List<Token> tokenList, Map<String, Rule> definitions)
    {
        this.tokenIter = tokenList.listIterator();
        this.definitions = definitions;
    }

    public SchemeParser(List<Token> tokenList)
    {
        this.tokenIter = tokenList.listIterator();
        this.definitions = DEFAULT_DEFINITIONS;
    }

    static Expr convertToExpr(Object obj)
    {
        Expr expr;
        if (obj instanceof Expr e) {
            expr = e;
        } else if (obj instanceof Term t) {
            expr = new Expr(Arrays.asList(t));
        } else if (obj instanceof String s) {
            expr = new Expr(Arrays.asList(term(s, TermType.TERMINAL, "")));
        } else if (obj instanceof RawPattern p) {
            expr = new Expr(Arrays.asList(term(p.regex, TermType.PATTERN, "")));
        } else if (obj instanceof GroupPattern p) {
            expr = new Expr(Arrays.asList(term(p.regex, TermType.PATTERN, "")));
        } else {
            throw new RuntimeException("Conversion of "
                                       + obj.getClass().getName()
                                       + " Expr is not possible.");
        }
        return expr;
    }

    static Term convertToTerm(Object obj)
    {
        Term term;
        if (obj instanceof Term t) {
            term = t;
        } else if (obj instanceof String s) {
            term = term(s, TermType.TERMINAL, "");
        } else if (obj instanceof RawPattern p) {
            term = term(p.regex, TermType.PATTERN, "");
        } else if (obj instanceof GroupPattern p) {
            term = term(p.regex, TermType.PATTERN, "");
        } else {
            throw new RuntimeException("Conversion of "
                                       + obj.getClass().getName()
                                       + "into Term is not possible.");
        }
        return term;
    }

    static Rule nonterminal(Object... objs)
    {
        List<Expr> exprs = new ArrayList<>();
        for (Object obj : objs) {
            exprs.add(convertToExpr(obj));
        }
        return new Rule(exprs);
    }

    static Expr terms(Object... objs)
    {
        List<Term> terms = new ArrayList<>();
        for (Object obj : objs) {
            terms.add(convertToTerm(obj));
        }
        return new Expr(terms);
    }

    static Term term(String value)
    {
        return term(value, TermType.NONTERMINAL, "");
    }

    static Term term(String value, String modifierSymbol)
    {
        return term(value, TermType.NONTERMINAL, modifierSymbol);
    }

    static Term term(String value, TermType type, String modifierSymbol)
    {
        return new Term(value, type, getModifier(modifierSymbol));
    }

    static Modifier getModifier(String symbol)
    {
        return switch (symbol) {
        case "+" -> Modifier.PLUS;
        case "*" -> Modifier.ASTERISK;
        default -> null;
        };
    }

    static Map<String, Rule> buildDefinitions(Object... objs)
    {
        Map<String, Rule> map = new HashMap<>();
        for (int i = 0; i < objs.length; i += 2) {
            if (objs[i] instanceof String s && objs[i+1] instanceof Rule r) {
                map.put(s, r);
            } else {
                throw new RuntimeException("Expected (String, Rule) pair.");
            }
        }
        return map;
    }

    public ASTNode parse()
    {
        return parse("PROGRAM");
    }

    public ASTNode parse(String rootRule)
    {
        ASTNode ast = parseRule(rootRule);
        if (tokenIter.hasNext()) {
            throw new RuntimeException("Something bad happened.");
        }
        return ast;
    }

    ASTNode parseRule(String ruleName) {
        ASTNode node = null;
        Rule rule = definitions.get(ruleName);
        int curIndex = tokenIter.nextIndex() - 1;

        // Try to match one of the expressions in the rule.
        for (Expr expr : rule.exprs) {
            node = parseExpr(expr);
            if (node != null) {
                node.type = ruleName;
                break;
            }

            // Reset position of cursor if expression doesn't match.
            moveIteratorTo(tokenIter, curIndex);
        }

        return node;
    }

    ASTNode parseExpr(Expr expr)
    {
        ListIterator<Term> termIter = expr.terms.listIterator();
        Term term = termIter.next();
        Map<Term, List<TermMatch>> termMatches = new LinkedHashMap<>();
        termMatches.put(term, new ArrayList<>());

        while (true) {
            TermMatch match = matchTerm(term);
            boolean termMatched = match != null;

            if (termMatched) {
                termMatches.get(term).add(match);
            }

            ParserState currentState = getCurrenState(term,
                                                      termIter,
                                                      termMatched,
                                                      termMatches);

            if (currentState == ParserState.STOP) {
                return buildASTFromTermMatches(termMatches);
            }

            if (currentState == ParserState.BACKTRACK) {
                try {
                    backtrack(termIter, termMatches);
                    currentState = ParserState.NEXT;
                } catch (NoSuchElementException e) {
                    return null;
                }
            }

            if (currentState == ParserState.REPEAT) {
                // Do nothing.
            } else if (currentState == ParserState.NEXT) {
                term = termIter.next();
                termMatches.put(term, new ArrayList<>());
            } else {
                throw new RuntimeException("Unexpected parser state");
            }
        }
    }

    static void moveIteratorTo(ListIterator<?> iter, int index)
    {
        if (iter.nextIndex() <= index + 1) {
            while (iter.nextIndex() != index + 1) {
                iter.next();
            }
        } else {
            while (iter.nextIndex() != index + 1) {
                iter.previous();
            }
        }
    }

    /**
     * Find the last term which matched greedily (i.e. could have matched at least 1
     * less token)
     * If found, removes the last match, and positions the token and term
     * iterators' cursors as they were before the last match.
     * Otherwise, throw NoSuchElementException.
     */
    void backtrack(ListIterator<Term> termIter, Map<Term, List<TermMatch>> termMatches)
    {
        int initialTermIndex = termIter.nextIndex() - 1;
        while (true) {
            Term term;
            try {
                term = termIter.previous();
            } catch (NoSuchElementException e) {
                moveIteratorTo(termIter, initialTermIndex);
                throw e;
            }

            List<TermMatch> matches = termMatches.get(term);

            if (termMatchedGreedily(term, matches.size())) {
                int startOfLastMatch = matches.get(matches.size() - 1).index;
                moveIteratorTo(tokenIter, startOfLastMatch - 1);
                matches.remove(matches.size() - 1);

                // Cursor should always be after the current term. Since this method
                // calls `previous` at least once, wee need to call `next` once to
                // keep the cursor after the element.
                termIter.next();
                break;
            } else {
                // All matches are removed because, if an element is backtracked and
                // can't be matched less times, it's as if it never matched anything.
                matches.clear();
            }
        }
    }

    static boolean termMatchedGreedily(Term term, int matches)
    {
        int minimumMatchesRequired;
        if (term.modifier == Modifier.ASTERISK) {
            minimumMatchesRequired = 0;
        } else if (term.modifier == Modifier.PLUS) {
            minimumMatchesRequired = 1;
        } else {
            minimumMatchesRequired = 1;
        }
        return matches > minimumMatchesRequired;
    }

    ParserState getCurrenState(Term term,
                               ListIterator<Term> termIter,
                               boolean termMatched,
                               Map<Term, List<TermMatch>> termMatches)
    {
        String stateName;
        if (term.modifier == Modifier.PLUS) {
            stateName =
                termMatched ? "REPEAT"
                : termMatches.get(term).size() == 0 ? "BACKTRACK"
                : termIter.hasNext() ? "NEXT" : "STOP";
        } else if (term.modifier == Modifier.ASTERISK) {
            stateName =
                termMatched ? "REPEAT"
                : termIter.hasNext() ? "NEXT" : "STOP";
        } else {
            stateName =
                !termMatched ? "BACKTRACK"
                : termIter.hasNext() ? "NEXT" : "STOP";
        }
        return ParserState.valueOf(stateName);
    }

    static ASTNode buildASTFromTermMatches(Map<Term, List<TermMatch>> termMatches)
    {
        ASTNode node = new ASTNode();
        for (Map.Entry<Term, List<TermMatch>> entry : termMatches.entrySet()) {
            TermType termType = entry.getKey().type;
            for (TermMatch match : entry.getValue()) {
                if (match.value == null) continue;

                if (termType == TermType.TERMINAL
                    && match.value instanceof Token token
                    && token.type == TokenType.IDENTIFIER) {
                    node.value = token.value;
                } else if (termType == TermType.PATTERN
                           && match.value instanceof Token token) {
                    node.value = token.value;
                } else if (termType == TermType.NONTERMINAL
                           && match.value instanceof ASTNode child) {
                    node.addChildren(child);
                }
            }
        }
        return node;
    }

    TermMatch matchTerm(Term term)
    {
        Token token;
        Object match = null;
        Integer matchStart = tokenIter.nextIndex();
        boolean termMatched = false;

        switch (term.type) {
        case TERMINAL:
            if (term.value.equals("")) {
                termMatched = true;
                break;
            }

            if (!tokenIter.hasNext()) break;

            token = tokenIter.next();
            termMatched = term.value.equals(token.value);
            if (termMatched) match = token;
            break;
        case PATTERN:
            if (!tokenIter.hasNext()) break;

            token = tokenIter.next();
            termMatched = patternMatches(term.value, token.value);
            if (termMatched) match = token;
            break;
        case NONTERMINAL:
            match = parseRule(term.value);
            termMatched = match != null;
            break;
        }

        return termMatched ? new TermMatch(matchStart, match) : null;
    }

    static Boolean patternMatches(String pattern, String string)
    {
        return Pattern
            .compile("^" + pattern)
            .matcher(string)
            .find();
    }
}
