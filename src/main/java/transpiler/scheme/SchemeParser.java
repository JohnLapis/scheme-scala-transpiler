package transpiler.scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Modifier {PLUS, ASTERISK}

enum TermType {TERMINAL, NONTERMINAL, PATTERN}

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

    public Rule(List<Expr> exprs)
    {
        this.exprs = exprs;
    }
}

public class SchemeParser
{
    static Map<String, Rule> DEFINITIONS =
        buildDefinitions(
               /* Programs and definitions */
               "PROGRAM", nonterminal(terms(term("IMPORT_DECLARATION", "+"),
                                            term("COMMAND_OR_DEFINITION", "+"))),
               "COMMAND_OR_DEFINITION",
               nonterminal(term("COMMAND"),
                           term("DEFINITION"),
                           terms("(", "begin", term("COMMAND_OR_DEFINITION", "+"), ")")),
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
               nonterminal(term("IDENTIFIER", "*"),
                           terms(term("IDENTIFIER", "*"),
                                 ".",
                                 term("IDENTIFIER"))),
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
                                 // TODO term(terms("(", term("IDENTIFIER"), term("IDENTIFIER"), ")"), "+"),
                                 ")")),
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
               "FORMALS", nonterminal(terms("(", term("VARIABLE", "*"), ")"),
                                      terms(term("VARIABLE")),
                                      terms("(",
                                            term("VARIABLE", "+"),
                                            ".",
                                            term("VARIABLE"),
                                            ")")),
               "VARIABLE", nonterminal(term("IDENTIFIER")),
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

    ListIterator<Token> iterator;

    public SchemeParser(List<Token> tokenList)
    {
        this.iterator = tokenList.listIterator();
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
        ASTNode ast = parseRule("PROGRAM");
        if (iterator.hasNext()) {
            throw new RuntimeException("Something bad happened.");
        }
        return ast;
    }

    ASTNode parseExpr(Expr expr)
    {
        ASTNode node = new ASTNode();
        Iterator<Term> termIter = expr.terms.iterator();
        Term term = termIter.next();
        int matches = 0;
        while (true) {
            Token token = iterator.next();
            boolean tokenMatched = false;
            switch (term.type) {
            case TERMINAL:
                if (term.value == token.value) {
                    tokenMatched = true;
                    if (token.type == TokenType.IDENTIFIER) {
                        node.value = token.value;
                    }
                }
                break;
            case PATTERN:
                if (patternMatches(term.value, token.value)) {
                    tokenMatched = true;
                    node.addChild(new ASTNode(token.value));
                }
                break;
            case NONTERMINAL:
                ASTNode ruleNode = parseRule(term.value);
                if (ruleNode != null) {
                    tokenMatched = true;
                    node.addChild(ruleNode);
                }
                break;
            }

            if (tokenMatched) {
                if (term.modifier == Modifier.PLUS
                    || term.modifier == Modifier.ASTERISK) {
                    matches++;
                } else if (termIter.hasNext()) {
                    term = termIter.next();
                    matches = 0;
                } else {
                    break;
                }
            } else if (term.modifier == Modifier.ASTERISK && matches >= 0
                       || term.modifier == Modifier.PLUS && matches >= 1) {
                if (termIter.hasNext()) {
                    term = termIter.next();
                    matches = 0;
                } else {
                    break;
                }
            } else {
                return null;
            }
        }
        return node;
    }

    ASTNode parseRule(String ruleName)
    {
        ASTNode node = null;
        Rule rule = DEFINITIONS.get(ruleName);
        int curIndex = iterator.nextIndex() - 1;

        for (Expr expr : rule.exprs) {
            node = parseExpr(expr);
            if (node != null) {
                node.value = ruleName;
                break;
            }

            backtrackTo(curIndex);
        }

        return node;
    }

    void backtrackTo(int index)
    {
        while (iterator.nextIndex() != index + 1) {
            iterator.previous();
        }
    }

    Boolean patternMatches(String pattern, String string)
    {
        Matcher matcher = Pattern.compile("^" + pattern).matcher(string);
        return matcher.find();
    }
}
