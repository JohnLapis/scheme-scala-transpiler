package transpiler.scheme;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
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
            node("PROGRAM",
                 node("COMMAND_OR_DEFINITION",
                      node("COMMAND",
                           node("EXPRESSION",
                                node("PROCEDURE_CALL",
                                     node("OPERATOR",
                                          node("EXPRESSION",
                                               node("IDENTIFIER", "+"))),
                                     node("OPERAND",
                                          node("EXPRESSION",
                                               node("LITERAL",
                                                    node("SELF_EVALUATING",
                                                         node("NUMBER", "5"))))),
                                     node("OPERAND",
                                          node("EXPRESSION",
                                               node("LITERAL",
                                                    node("SELF_EVALUATING",
                                                         node("NUMBER", "4"))))))))));
        List<Token> tokenList = tokens("DELIMITER", "(",
                                       "IDENTIFIER", "+",
                                       "NUMBER", "5",
                                       "NUMBER", "4",
                                       "DELIMITER", ")");
        SchemeParser parser = new SchemeParser(tokenList);
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
        int childrenCount = node1.children == null ? 0 : node1.children.size();
        assertEquals(childrenCount, node2.children == null ? 0 : node2.children.size());
        for (int i = 0; i < childrenCount; i++) {
            compareASTNodes(node1.children.get(i), node2.children.get(i));
        }
    }

    static ASTNode node(String type, ASTNode... children)
    {
        return new ASTNode(type, null, children);
    }

    static ASTNode node(String type, String value, ASTNode... children)
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
