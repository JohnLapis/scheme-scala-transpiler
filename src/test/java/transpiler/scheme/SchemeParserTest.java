package transpiler.scheme;

import transpiler.ASTNode;
import static transpiler.ASTNodeTestUtils.compareASTNodes;
import static transpiler.ASTNodeTestUtils.n;
import org.junit.Test;
import org.junit.Ignore;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class SchemeParserTest
{
    @Ignore("TODO")
    @Test
    public void parseVariables()
    {
        // This should test variables which are outside any expression.
    }

    @Test
    public void parseProcedureCalls()
    {
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION",
                    n("PROCEDURE_CALL",
                      n("OPERATOR",
                        n("EXPRESSION",
                          n("IDENTIFIER", "+"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "1"))))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("PROCEDURE_CALL",
                            n("OPERATOR",
                              n("EXPRESSION",
                                n("IDENTIFIER", "*"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("LITERAL",
                                  n("SELF_EVALUATING",
                                    n("NUMBER", "2"))))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("LITERAL",
                                  n("SELF_EVALUATING",
                                    n("NUMBER", "3")))))))))))));
        String code = "(+ 1 (* 2 3))";
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseProcedureWith1Variable_Syntax1()
    {
        String code = """
(define fact
  (lambda n
    (if (= n 0)
        1
      (* n (fact (- n 1))))))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("EXPRESSION",
                    n("LAMBDA_EXPRESSION", "lambda",
                      n("FORMALS",
                        n("IDENTIFIER", "n")),
                      n("BODY",
                        n("SEQUENCE",
                          n("EXPRESSION",
                            n("CONDITIONAL", "if",
                              n("TEST",
                                n("EXPRESSION",
                                  n("PROCEDURE_CALL",
                                    n("OPERATOR",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "="))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "n"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("LITERAL",
                                          n("SELF_EVALUATING",
                                            n("NUMBER", "0")))))))),
                              n("CONSEQUENT",
                                n("EXPRESSION",
                                  n("LITERAL",
                                    n("SELF_EVALUATING",
                                      n("NUMBER", "1"))))),
                              n("ALTERNATE",
                                n("EXPRESSION",
                                  n("PROCEDURE_CALL",
                                    n("OPERATOR",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "*"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "n"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("PROCEDURE_CALL",
                                          n("OPERATOR",
                                            n("EXPRESSION",
                                              n("IDENTIFIER", "fact"))),
                                          n("OPERAND",
                                            n("EXPRESSION",
                                              n("PROCEDURE_CALL",
                                                n("OPERATOR",
                                                  n("EXPRESSION",
                                                    n("IDENTIFIER", "-"))),
                                                n("OPERAND",
                                                  n("EXPRESSION",
                                                    n("IDENTIFIER", "n"))),
                                                n("OPERAND",
                                                  n("EXPRESSION",
                                                    n("LITERAL",
                                                      n("SELF_EVALUATING",
                                                        n("NUMBER", "1")))))))))))))))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseProcedureWith3Variables_Syntax1()
    {
        String code = """
(define fact
  (lambda (a b c)
    (+ a b c)))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("EXPRESSION",
                    n("LAMBDA_EXPRESSION", "lambda",
                      n("FORMALS",
                        n("IDENTIFIER", "a"),
                        n("IDENTIFIER", "b"),
                        n("IDENTIFIER", "c")),
                      n("BODY",
                        n("SEQUENCE",
                          n("EXPRESSION",
                            n("PROCEDURE_CALL",
                              n("OPERATOR",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "+"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "a"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "b"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "c"))))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseProcedureWith3Variables_Syntax2()
    {
        String code = """
(define (fact a b c)
  (+ a b c))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("DEF_FORMALS",
                    n("IDENTIFIER", "a"),
                    n("IDENTIFIER", "b"),
                    n("IDENTIFIER", "c")),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseProcedureWithDottedVariables_Syntax1()
    {
        String code = """
(define fact
  (lambda (a b . c)
    (+ a b c)))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("EXPRESSION",
                    n("LAMBDA_EXPRESSION", "lambda",
                      n("FORMALS",
                        n("IDENTIFIER", "a"),
                        n("IDENTIFIER", "b"),
                        n("VAR_PARAMETER",
                          n("IDENTIFIER", "c"))),
                      n("BODY",
                        n("SEQUENCE",
                          n("EXPRESSION",
                            n("PROCEDURE_CALL",
                              n("OPERATOR",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "+"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "a"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "b"))),
                              n("OPERAND",
                                n("EXPRESSION",
                                  n("IDENTIFIER", "c"))))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseProcedureWithDottedVariables_Syntax2()
    {
        String code = """
(define (fact a b . c)
  (+ a b c))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("DEF_FORMALS",
                    n("IDENTIFIER", "a"),
                    n("IDENTIFIER", "b"),
                    n("VAR_PARAMETER",
                      n("IDENTIFIER", "c"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseConditionalsWithAlternate()
    {
        String code = """
    (if (= n 0)
        1   (* n (fact (- n 1))))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION",
                    n("CONDITIONAL", "if",
                      n("TEST",
                        n("EXPRESSION",
                          n("PROCEDURE_CALL",
                            n("OPERATOR",
                              n("EXPRESSION",
                                n("IDENTIFIER", "="))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("IDENTIFIER", "n"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("LITERAL",
                                  n("SELF_EVALUATING",
                                    n("NUMBER", "0")))))))),
                      n("CONSEQUENT",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "1"))))),
                      n("ALTERNATE",
                        n("EXPRESSION",
                          n("PROCEDURE_CALL",
                            n("OPERATOR",
                              n("EXPRESSION",
                                n("IDENTIFIER", "*"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("IDENTIFIER", "n"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("PROCEDURE_CALL",
                                  n("OPERATOR",
                                    n("EXPRESSION",
                                      n("IDENTIFIER", "fact"))),
                                  n("OPERAND",
                                    n("EXPRESSION",
                                      n("PROCEDURE_CALL",
                                        n("OPERATOR",
                                          n("EXPRESSION",
                                            n("IDENTIFIER", "-"))),
                                        n("OPERAND",
                                          n("EXPRESSION",
                                            n("IDENTIFIER", "n"))),
                                        n("OPERAND",
                                          n("EXPRESSION",
                                            n("LITERAL",
                                              n("SELF_EVALUATING",
                                                n("NUMBER", "1")))))))))))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseConditionalsWithoutAlternate()
    {
        String code = """
    (if (= n 0) 1)
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION",
                    n("CONDITIONAL", "if",
                      n("TEST",
                        n("EXPRESSION",
                          n("PROCEDURE_CALL",
                            n("OPERATOR",
                              n("EXPRESSION",
                                n("IDENTIFIER", "="))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("IDENTIFIER", "n"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("LITERAL",
                                  n("SELF_EVALUATING",
                                    n("NUMBER", "0")))))))),
                      n("CONSEQUENT",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "1"))))),
                      n("ALTERNATE"))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void parseAssignments()
    {
        String code = """
    (set! nIsZero (= n 0))
            """;
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION",
                    n("ASSIGNMENT", "set!",
                      n("IDENTIFIER", "nIsZero"),
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "="))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "n"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("LITERAL",
                                n("SELF_EVALUATING",
                                n("NUMBER", "0"))))))))))));
        SchemeParser parser = new SchemeParser(SchemeScanner.tokenize(code));
        compareASTNodes(expectedAst, parser.parse());
    }

    @Test
    public void backtrackExprWithOneModifier()
    {
        /**
         * Input: "aaa"
         * The parser states will evolve as following:
         * - R1 matches "aaa", backtrack
         * - R1 matches "aa", R2 matches "a"
         */
        Map<String, Rule> definitions =
            SchemeParser.buildDefinitions
            (
             "R", new Rule(new Expr(new Term("R1",
                                             TermType.NONTERMINAL,
                                             Modifier.ASTERISK),
                                    new Term("R2", TermType.NONTERMINAL))),
             "R1", new Rule(new Expr(new Term("a", TermType.TERMINAL))),
             "R2", new Rule(new Expr(new Term("a", TermType.TERMINAL)))
             );
        SchemeParser parser = new SchemeParser(tokens("IDENTIFIER", "a",
                                                      "IDENTIFIER", "a",
                                                      "IDENTIFIER", "a"),
                                               definitions);
        ASTNode expectedAst = n("R", n("R1", "a"), n("R1", "a"), n("R2", "a"));
        compareASTNodes(expectedAst, parser.parse("R"));
    }

    @Test
    public void backtrackExprWithTwoModifiers()
    {
        /**
         * Input: "aaaaa"
         * The parser states will evolve as following:
         * - R1 matches "aaaaa"
         * - R1 matches "aaaa", R2 matches "a"
         * - R1 matches "aaa", R2 matches "aa"
         * - R1 matches "aaa", R2 matches "a", R3 matches "a"
        */

        Map<String, Rule> definitions =
            SchemeParser.buildDefinitions
            (
             "R", new Rule(new Expr(new Term("R1",
                                             TermType.NONTERMINAL,
                                             Modifier.ASTERISK),
                                    new Term("R2",
                                             TermType.NONTERMINAL,
                                             Modifier.PLUS),
                                    new Term("R3", TermType.NONTERMINAL))),
             "R1", new Rule(new Expr(new Term("a", TermType.TERMINAL))),
             "R2", new Rule(new Expr(new Term("a", TermType.TERMINAL))),
             "R3", new Rule(new Expr(new Term("a", TermType.TERMINAL)))
             );
        SchemeParser parser = new SchemeParser(tokens("IDENTIFIER", "a",
                                                      "IDENTIFIER", "a",
                                                      "IDENTIFIER", "a",
                                                      "IDENTIFIER", "a",
                                                      "IDENTIFIER", "a"),
                                               definitions);
        ASTNode expectedAst = n("R",
                                n("R1", "a"),
                                n("R1", "a"),
                                n("R1", "a"),
                                n("R2", "a"),
                                n("R3", "a"));
        compareASTNodes(expectedAst, parser.parse("R"));
    }

    static List<Token> tokens(Object... objs)
    {
        List<Token> tokenList = new ArrayList<>();
        for (int i = 0; i < objs.length; i += 2) {
            if (objs[i] instanceof String t && objs[i + 1] instanceof String v) {
                tokenList.add(new Token(TokenType.valueOf(t), v));
            } else {
                throw new RuntimeException("Expected (TokenType, String) pair.");
            }
        }
        return tokenList;
    }
}
