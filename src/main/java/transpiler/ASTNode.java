package transpiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.lang.StringBuilder;
import java.util.List;

public class ASTNode
{
    public String type;
    public String value;
    ASTNode parent;
    public List<ASTNode> children;

    public ASTNode()
    {
        this.type = null;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type)
    {
        this.type = type;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type, String value)
    {
        this.type = type;
        this.value = value;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type, String value, ASTNode[] children)
    {
        this.type = type;
        this.value = value;
        this.parent = null;
        setChildren(children);
    }

    public void addChild(ASTNode node)
    {
        node.parent = this;
        this.children.add(node);
    }

    public void setChildren(ASTNode[] children)
    {
        this.children = Arrays.asList(children);
        for (ASTNode child : children) {
            child.parent = this;
        }
    }

    public ASTNode getRoot()
    {
        ASTNode node = this;
        while (this.parent != null) {
            node = this.parent;
        }
        return node;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ASTNode node) {
            boolean typesAreEqual = type == null ?
                node.type == null : type.equals(node.type);
            boolean valuesAreEqual = value == null ?
                node.value == null : value.equals(node.value);
            return typesAreEqual
                && valuesAreEqual
                && children.size() == node.children.size();
        } else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix)
    {
        buffer.append(prefix);
        buffer.append(value == null ?
                      "(" + type + ")" : "(" + type + ", " + value + ")");
        buffer.append('\n');
        for (Iterator<ASTNode> it = children.iterator(); it.hasNext();) {
            ASTNode next = it.next();
            if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
