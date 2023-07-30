package transpiler;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.function.Function;


public class IntermediateRepresentation
{
    record Conversion(Map<String, ASTNode> cases) {}

    class ConvertSchemeAST implements Function<ASTNode, Void>
    {
        /**
         * Return true if function should be applied in children of the give
         * node else false.
         */
        public Void apply(ASTNode node)
        {
            if (nodeWasSieved(node)) return null;

            node.status = "SIEVED";

            Map<String, ASTNode> conversionCases = getConversions(node);

            if (conversionCases == null) return null;

            ASTNode conversionNode = null;
            ASTNode sourceNode = null;
            for (Map.Entry<String, ASTNode> entry : conversionCases.entrySet()) {
                String nodePath = entry.getKey();

                if (nodePath.equals("")) {
                    sourceNode = node;
                    conversionNode = entry.getValue();
                    break;
                }

                ASTNode matchingNode = node.get(nodePath);
                if (matchingNode != null) {
                    sourceNode = matchingNode;
                    conversionNode = entry.getValue();
                    break;
                }
            }

            if (sourceNode == null || conversionNode == null) return null;

            ASTNode convertedNode = applyConversion(sourceNode, conversionNode);
            node.value = convertedNode.value;
            node.type = convertedNode.type;
            node.setChildren(convertedNode.children);

            return null;
        }
    }

    static List<String> NODE_STATUSES = Arrays.asList("SIEVED");
    static Map<String, Map<String, ASTNode>> NODE_CONVERSIONS =
        buildConversions
        (
         "LITERAL", cases("QUOTATION", n("LIST", "DATUM"),
                          "SELF_EVALUATING", n("LITERAL", "")),
         "CONDITIONAL", n("CONDITIONAL",
                          n("TEST", "TEST"),
                          n("SEQUENCE", "CONSEQUENT"),
                          n("ALTERNATE", "ALTERNATE")),
         "ASSIGNMENT", n("ASSIGNMENT",
                         n("ID", "IDENTIFIER"),
                         n("EXPRESSION", "EXPRESSION")),
         "LAMBDA_EXPRESSION", n("LAMBDA",
                                n("PARAMS", "FORMALS"),
                                n("EXPR", "BODY")),
         "PROCEDURE_CALL", n("FUNCTION_CALL",
                             n("NAME", "OPERATOR"),
                             n("ARGUMENT", "OPERAND"))
         );

    ASTNode ast;

    public IntermediateRepresentation(ASTNode ast)
    {
        this.ast = ast;
    }

    static boolean nodeWasSieved(ASTNode node)
    {
        return node.status != null
            && NODE_STATUSES.indexOf(node.status) > NODE_STATUSES.indexOf("SIEVED");
    }

    static Map<String, ASTNode> getConversions(ASTNode node)
    {
        return NODE_CONVERSIONS.get(node.type);
    }

    static Map<String, Map<String, ASTNode>> buildConversions(Object... objs)
    {
        Map<String, Map<String, ASTNode>> conversions = new HashMap<>();
        for (int i = 0; i < objs.length; i += 2) {
            if (objs[i] instanceof String s
                && objs[i + 1] instanceof Conversion conversion) {
                conversions.put(s, conversion.cases);
            } else if (objs[i] instanceof String s
                       && objs[i + 1] instanceof ASTNode conversionNode) {
                conversions.put(s, Map.of("", conversionNode));
            } else {
                throw new RuntimeException("Expected (String, Conversion | ASTNode) pair.");
            }
        }
        return conversions;
    }

    static Conversion cases(Object... objs)
    {
        Map<String, ASTNode> cases = new HashMap<>();
        for (int i = 0; i < objs.length; i += 2) {
            if (objs[i] instanceof String prefix
                && objs[i + 1] instanceof ASTNode conversionNode) {
                cases.put(prefix, conversionNode);
            } else {
                throw new RuntimeException("Expected (String, ASTNode) pair.");
            }
        }
        return new Conversion(cases);
    }

    static ASTNode n(String scalaType, String nodePath, ASTNode... children)
    {
        return new ASTNode(scalaType, nodePath, children);
    }

    static ASTNode n(String scalaType, ASTNode... children)
    {
        return n(scalaType, null, children);
    }

    static ASTNode applyConversion(ASTNode sourceNode, ASTNode conversionNode)
    {
        List<ASTNode> nodes = applyConversion(sourceNode, conversionNode, "");
        if (nodes.size() != 1) {
            throw new RuntimeException("Expected 1 node.");
        }
        return nodes.get(0);
    }


    /**
     * This method builds a nodes with the structure of conversionNode and data from
     * sourceNode. The nodePath represents the current depth in sourceNode.
     */
    static List<ASTNode> applyConversion(ASTNode sourceNode,
                                         ASTNode conversionNode,
                                         String nodePath)
    {
        List<ASTNode> nodes = new ArrayList<>();

        if (conversionNode.value == null) {
            nodes.add(new ASTNode(conversionNode.type));
        } else {
            nodePath = conversionNode.value;

            List<ASTNode> foundNodes = sourceNode.getAllByPath(nodePath);

            if (foundNodes.size() == 0) {
                String errorMessage =
                    String.join("\n",
                                "Path not found in node.",
                                "Path: " + nodePath,
                                "Node:\n" + sourceNode.toString());
                throw new RuntimeException(errorMessage);
            }

            for (ASTNode foundNode : foundNodes) {
                ASTNode node = new ASTNode(conversionNode.type);
                node.value = foundNode.value;
                node.setChildren(foundNode.children);
                nodes.add(node);
            }
        }

        if (nodes.size() == 1) {
            for (ASTNode conversionChild : conversionNode.children) {
                nodes.get(0).addChildren(applyConversion(sourceNode,
                                                         conversionChild,
                                                         nodePath));
            }
        } else if (conversionNode.children.size() > 0) {
            throw new RuntimeException("Conversion nodes which match multiple siblings, should not have children. Instead, a new node conversion should be created.");
        }

        return nodes;
    }

    static Boolean patternMatches(String pattern, String string) {
        return Pattern
                .compile("^" + pattern)
                .matcher(string)
                .find();
    }

    void sieveAST()
    {
        ast.apply(new ConvertSchemeAST());
    }

    void addTypes()
    {
        // where appropriate, add types to values c/ all types will be ~Sch~
    }

    public static ASTNode generateScalaAST(ASTNode schemeAst)
    {
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        return ir.ast;
    }
}
