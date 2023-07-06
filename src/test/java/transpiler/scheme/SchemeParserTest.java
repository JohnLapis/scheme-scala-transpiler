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
    @Test
    public void someTest()
    {
    }

    static void compareASTNodes(ASTNode node1, ASTNode node2)
    {
        assertTrue(node1.equals(node2));
        int childrenCount = node1.children.size();
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
}
