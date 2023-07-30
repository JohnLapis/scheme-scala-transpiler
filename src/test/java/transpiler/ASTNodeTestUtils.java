package transpiler;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ASTNodeTestUtils
{
    public static void compareASTNodes(ASTNode node1, ASTNode node2)
    {
        try {
            _compareASTNodes(node1, node2);
        } catch (Throwable t) {
            System.out.println("Expected AST node:");
            System.out.println(node1);
            System.out.println("Received AST node:");
            System.out.println(node2);
            throw t;
        }

    }

    private static void _compareASTNodes(ASTNode node1, ASTNode node2)
    {
        assertTrue(node1.equals(node2));
        int childrenCount = node1.children.size();
        assertEquals(node1.children.size(), node2.children.size());
        for (int i = 0; i < childrenCount; i++) {
            _compareASTNodes(node1.children.get(i), node2.children.get(i));
        }
    }

    public static ASTNode n(String type, ASTNode... children)
    {
        return new ASTNode(type, null, children);
    }

    public static ASTNode n(String type, String value, ASTNode... children)
    {
        return new ASTNode(type, value, children);
    }
}
