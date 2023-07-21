package transpiler.scheme;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import transpiler.ASTNode;
import org.junit.Test;
import org.junit.Ignore;
import java.util.List;
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
    public void parseProcedures()
    {
        String code = """
(define fact
  (lambda (n)
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
                        n("VARIABLE",
                          n("IDENTIFIER", "n"))),
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

    static void compareASTNodes(ASTNode node1, ASTNode node2)
    {
        assertTrue(node1.equals(node2));
        int childrenCount = node1.children.size();
        assertEquals(node1.children.size(), node2.children.size());
        for (int i = 0; i < childrenCount; i++) {
            compareASTNodes(node1.children.get(i), node2.children.get(i));
        }
    }

    static ASTNode n(String type, ASTNode... children)
    {
        return new ASTNode(type, null, children);
    }

    static ASTNode n(String type, String value, ASTNode... children)
    {
        return new ASTNode(type, value, children);
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
