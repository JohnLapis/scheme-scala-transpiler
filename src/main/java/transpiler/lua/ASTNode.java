package transpiler.lua;

import transpiler.AbstractASTNode;
import java.util.List;

public class ASTNode extends AbstractASTNode
{
    ASTNode parent;
    public List<ASTNode> children;

    public ASTNode()
    {
        super();
    }

    public ASTNode(String type)
    {
        super(type);
    }

    public ASTNode(String type, String value)
    {
        super(type, value);
    }

    public ASTNode(String type, String value, ASTNode[] children)
    {
        super(type, value, children);
    }
}
