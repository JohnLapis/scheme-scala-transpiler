package transpiler;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.function.Function;

class NodePathNotMatched extends RuntimeException
{
    public NodePathNotMatched(String path, ASTNode node)
    {
        super(String.join("\n",
                          "No nodes match the path.",
                          "Path: " + path,
                          "Node:",
                          node.toString()));
    }
}

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
            for (Map.Entry<String, ASTNode> entry : conversionCases.entrySet()) {
                if (node.getByPath(entry.getKey()) != null) {
                    conversionNode = entry.getValue();
                    break;
                }
            }

            if (conversionNode == null) return null;

            ASTNode convertedNode = applyConversion(node, conversionNode);
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
         "LITERAL", cases("$QUOTATION", n("LIST", "$QUOTATION.DATUM"),
                          "$SELF_EVALUATING", n("LITERAL", "$SELF_EVALUATING")),
         "CONDITIONAL", n("CONDITIONAL",
                          n("TEST", "$TEST"),
                          n("SEQUENCE", "$CONSEQUENT"),
                          n("ALTERNATE", "$ALTERNATE")),
         "ASSIGNMENT", n("ASSIGNMENT",
                         n("ID", "$IDENTIFIER"),
                         n("EXPRESSION", "$EXPRESSION")),
         "LAMBDA_EXPRESSION", n("LAMBDA",
                                n("PARAMS", "FORMALS"),
                                n("BODY", "$BODY")),
         "DEFINITION:define",
         cases("$EXPRESSION", n("DEF_VAR",
                                n("ID", "$IDENTIFIER"),
                                n("EXPRESSION", "$EXPRESSION")),
               "$DEF_FORMALS", n("DEF_VAR",
                                 n("ID", "$IDENTIFIER"),
                                 n("EXPRESSION",
                                   n("LAMBDA",
                                     n("PARAMS", "$DEF_FORMALS"),
                                     n("BODY", "$BODY"))))),
         "DEF_FORMALS", n("FORMALS", "$"),
         "FORMALS",
         cases("$VAR_PARAMETER", n("PARAMS",
                                   loop("$IDENTIFIER",
                                        n("PARAM", n("ID", "$"), n("TYPE", "Sch"))),
                                   n("PARAM", "$VAR_PARAMETER")),
               "$", n("PARAMS",
                      loop("$IDENTIFIER",
                           n("PARAM", n("ID", "$"), n("TYPE", "Sch"))))),
         "VAR_PARAMETER", n("PARAM",
                            n("ID", "$IDENTIFIER"),
                            n("TYPE", "Sch*")),
         "PROCEDURE_CALL", n("FUNCTION_CALL",
                             n("NAME", "$OPERATOR"),
                             loop("$OPERAND", n("ARGUMENT", "$")))
         );

    ASTNode ast;
    ConvertSchemeAST convertSchemeAST;

    public IntermediateRepresentation(ASTNode ast)
    {
        this.ast = ast;
        this.convertSchemeAST = new ConvertSchemeAST();

    }

    static boolean nodeWasSieved(ASTNode node)
    {
        return node.status != null
            && NODE_STATUSES.indexOf(node.status) > NODE_STATUSES.indexOf("SIEVED");
    }

    static Map<String, ASTNode> getConversions(ASTNode node)
    {
        Map<String, ASTNode> conversion = NODE_CONVERSIONS.get(node.type);
        if (conversion != null) return conversion;
        conversion = NODE_CONVERSIONS.get(node.type + ":" + node.value);
        return conversion;
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
                conversions.put(s, Map.of("$", conversionNode));
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
                throw new NodePathNotMatched(conversionNode.value, sourceNode);
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
            throw new NodePathNotMatched(conversionNode.value, sourceNode);
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
        ast.apply(convertSchemeAST);
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
