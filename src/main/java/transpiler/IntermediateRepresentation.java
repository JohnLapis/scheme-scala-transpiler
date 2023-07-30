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
         "LITERAL", cases("QUOTATION", n("LIST", "$DATUM"),
                          "SELF_EVALUATING", n("LITERAL", "$")),
         "CONDITIONAL", n("CONDITIONAL",
                          n("TEST", "$TEST"),
                          n("SEQUENCE", "$CONSEQUENT"),
                          n("ALTERNATE", "$ALTERNATE")),
         "ASSIGNMENT", n("ASSIGNMENT",
                         n("ID", "$IDENTIFIER"),
                         n("EXPRESSION", "$EXPRESSION")),
         "LAMBDA_EXPRESSION", n("LAMBDA",
                                n("PARAMS", "$FORMALS"),
                                n("EXPR", "$BODY")),
         "PROCEDURE_CALL", n("FUNCTION_CALL",
                             n("NAME", "$OPERATOR"),
                             loop("$OPERAND", n("ARGUMENT", "$")))
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

    static ASTNode loop(String value, ASTNode node)
    {
        return n("loop", value, node);
    }

    static ASTNode n(String type, String value, ASTNode... children)
    {
        return new ASTNode(type, value, children);
    }

    static ASTNode n(String type, ASTNode... children)
    {
        return n(type, null, children);
    }

    /**
     * This method builds a nodes with the structure of conversionNode and data from
     * sourceNode.
     */
    static ASTNode applyConversion(ASTNode sourceNode, ASTNode conversionNode)
    {
        return conversionNode.type.equals("loop") ?
            applyLoopConversion(sourceNode, conversionNode)
            : applySimpleConversion(sourceNode, conversionNode);
    }

    static ASTNode applySimpleConversion(ASTNode sourceNode, ASTNode conversionNode)
    {
        ASTNode node = new ASTNode();

        if (conversionNode.value == null) {
            node.type = conversionNode.type;
        } else if (!conversionNode.value.startsWith("$")) {
            node.type = conversionNode.type;
            node.value = conversionNode.value;
        } else {
            node.type = conversionNode.type;

            ASTNode foundNode = sourceNode.getByPath(conversionNode.value);

            if (foundNode == null) {
                String errorMessage =
                    String.join("\n",
                                "No node matches the path.",
                                "Path: " + conversionNode.value,
                                "Node:\n" + sourceNode.toString());
                throw new RuntimeException(errorMessage);
            }

            node.value = foundNode.value;
            node.setChildren(foundNode.children);
        }

        if (conversionNode.children.size() == 0) {
            // TODO check if found node should be converted so that data is not lost
        }

        for (ASTNode conversionChild : conversionNode.children) {
            ASTNode convertedChild = applyConversion(sourceNode, conversionChild);
            if (convertedChild.type == "loop") {
                node.addChildren(convertedChild.children);
            } else {
                node.addChildren(convertedChild);
            }
        }

        return node;

    }

    static ASTNode applyLoopConversion(ASTNode sourceNode, ASTNode conversionNode)
    {
        ASTNode node = new ASTNode();

        List<ASTNode> foundNodes = sourceNode.getAllByPath(conversionNode.value);

        if (foundNodes.size() == 0) {
            String errorMessage =
                String.join("\n",
                            "No nodes match the path.",
                            "Path: " + conversionNode.value,
                            "Node:\n" + sourceNode.toString());
            throw new RuntimeException(errorMessage);
        }

        node.type = "loop";

        for (ASTNode foundNode : foundNodes) {
            node.addChildren(applyConversion(foundNode,
                                             conversionNode.children.get(0)));
        }

        return node;
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
