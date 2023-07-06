package transpiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.lang.StringBuilder;
import java.util.List;

public abstract class AbstractASTNode
{
    public String type;
    public String value;
    AbstractASTNode parent;
    public List<AbstractASTNode> children;

    public AbstractASTNode()
    {
        this.type = null;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public AbstractASTNode(String type)
    {
        this.type = type;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public AbstractASTNode(String type, String value)
    {
        this.type = type;
        this.value = value;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public AbstractASTNode(String type, String value, AbstractASTNode[] children)
    {
        this.type = type;
        this.value = value;
        this.parent = null;
        setChildren(children);
    }

    public void addChild(AbstractASTNode node)
    {
        node.parent = this;
        this.children.add(node);
    }

    public void setChildren(AbstractASTNode[] children)
    {
        this.children = Arrays.asList(children);
        for (AbstractASTNode child : children) {
            child.parent = this;
        }
    }

    public AbstractASTNode getRoot()
    {
        AbstractASTNode node = this;
        while (this.parent != null) {
            node = this.parent;
        }
        return node;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AbstractASTNode node) {
            return type == node.type
                && value == node.value
                && children.size() == node.children.size();
        } else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "\n", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix)
    {
        buffer.append(prefix);
        buffer.append(value == null ?
                      "(" + type + ")" : "(" + type + ", " + value + ")");
        buffer.append('\n');
        for (Iterator<AbstractASTNode> it = children.iterator(); it.hasNext();) {
            AbstractASTNode next = it.next();
            if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
