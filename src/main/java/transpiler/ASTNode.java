package transpiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.lang.StringBuilder;
import java.util.List;
import java.util.function.Function;

public class ASTNode
{
    public String status;
    public String type;
    public String value;
    ASTNode parent;
    public List<ASTNode> children;

    public ASTNode()
    {
        this.status = null;
        this.type = null;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type)
    {
        this.status = null;
        this.type = type;
        this.value = null;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type, String value)
    {
        this.status = null;
        this.type = type;
        this.value = value;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(String type, String value, ASTNode[] children)
    {
        this.status = null;
        this.type = type;
        this.value = value;
        this.parent = null;
        setChildren(children);
    }

    public void addChildren(List<ASTNode> nodes)
    {
        addChildren(nodes.toArray(new ASTNode[0]));
    }

    public void addChildren(ASTNode... nodes)
    {
        for (ASTNode node : nodes) {
            node.parent = this;
            this.children.add(node);
        }
    }

    public void setChildren(ASTNode[] children)
    {
        setChildren(Arrays.asList(children));
    }

    public void setChildren(List<ASTNode> children)
    {
        this.children = children;
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

    public boolean isLeaf()
    {
        return children.size() == 0;
    }

    public List<ASTNode> getAllByPath(String path)
    {
        return getAll(splitPath(path));
    }

    public List<ASTNode> getAll(String... types)
    {
        if (types.length == 0) return Arrays.asList(this);

        List<ASTNode> all = new ArrayList<>();
        for (ASTNode child : children) {
            if (!child.type.equals(types[0])) continue;

            if (types.length == 1) {
                all.add(child);
            } else {
                all.addAll(child.getAll(Arrays.copyOfRange(types, 1, types.length)));
            }
        }
        return all;
    }

    public boolean hasByPath(String path)
    {
        return getByPath(path) != null;
    }

    public ASTNode getByPath(String path)
    {
        return get(splitPath(path));
    }

    ASTNode get(String... types)
    {
        if (types.length == 0) return this;

        for (ASTNode child : children) {
            if (!child.type.equals(types[0])) continue;

            return types.length == 1 ?
                child : child.get(Arrays.copyOfRange(types, 1, types.length));
        }

        return null;
    }

    public static String[] splitPath(String path)
    {
        if (!path.startsWith("$")) {
            throw new RuntimeException("Expected \"$\" at the beginning of path");
        }
        path = path.substring(1);
        return path.equals("") ? new String[0] : path.split("\\.");
    }

    public static String joinPaths(String... paths)
    {
        List<String> pathSegments = new ArrayList<>();
        for (String path : paths) {
            if (!path.equals("")) {
                pathSegments.add(path);
            }
        }
        return String.join(".", pathSegments);
    }


    public void apply(Function<ASTNode, ?> function)
    {
        apply(this, function);
    }

    /**
     * This function traverses the tree in a depth-first order.
     */
    public static void apply(ASTNode node, Function<ASTNode, ?> function)
    {
        function.apply(node);
        for (ASTNode child : node.children) {
            apply(child, function);
        }
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
