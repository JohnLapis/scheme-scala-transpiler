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

    @Ignore("TODO")
    @Test
    public void parseProcedures()
    {
    }

    @Ignore("TODO")
    @Test
    public void parseConditionals()
    {
    }

    @Ignore("TODO")
    @Test
    public void parseAssignments()
    {
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
