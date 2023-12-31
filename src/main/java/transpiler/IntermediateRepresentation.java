package transpiler;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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

    class ConvertLiterals implements Function<ASTNode, Void>
    {
        public Void apply(ASTNode node) {
            if (nodeWasLiteralConverted(node)) return null;

            node.status = "LITERAL_CONVERTED";

            if (node.type.equals("BOOLEAN")) {
                convertBooleanLiteral(node);
            } else if (node.type.equals("NUMBER")) {
                convertNumberLiteral(node);
            }

            return null;
        }
    }

    class ConvertProcedures implements Function<ASTNode, Void>
    {
        public Void apply(ASTNode node)
        {
            if (nodeWasProcedureConverted(node)) return null;

            node.status = "PROCEDURE_CONVERTED";

            if (!node.type.equals("FUNCTION_CALL")) return null;

            ASTNode functionId = node.getByPath("$NAME.EXPRESSION.IDENTIFIER");

            if (STANDARD_SCHEME_PROCEDURES.containsKey(functionId.value)) {
                functionId.value = STANDARD_SCHEME_PROCEDURES.get(functionId.value);
            }

            return null;
        }
    }

    class TypeSchemeAST implements Function<ASTNode, Void>
    {
        public Void apply(ASTNode node)
        {
            if (nodeWasTyped(node)) return null;


            if (TYPED_NODE_TYPES.contains(node.type) && !node.hasByPath("$TYPE")) {
                node.addChildren(new ASTNode("TYPE", "Sch"));
            }

            node.status = "TYPED";
            return null;
        }
    }

    class ConvertSchemeAST implements Function<ASTNode, Void>
    {
        /**
         * Return true if function should be applied in children of the give
         * node else false.
         */
        public Void apply(ASTNode node)
        {
            if (nodeWasSieved(node)) return null;

            Map<String, ASTNode> conversionCases = getConversions(node);

            if (conversionCases == null) {
                node.status = "SIEVED";
                return null;
            }

            ASTNode conversionNode = null;
            for (Map.Entry<String, ASTNode> entry : conversionCases.entrySet()) {
                if (node.hasByPath(entry.getKey())) {
                    conversionNode = entry.getValue();
                    break;
                }
            }

            if (conversionNode == null) {
                node.status = "SIEVED";
                return null;
            }

            ASTNode convertedNode = applyConversion(node, conversionNode);
            node.value = convertedNode.value;
            node.setChildren(convertedNode.children);

            // "*" indicates that the conversion should be recursed on the new node.
            if (convertedNode.type.startsWith("*")) {
                node.type = convertedNode.type.substring(1);
                apply(node);
            } else {
                node.type = convertedNode.type;
            }

            for (ASTNode child : node.children) {
                if (child.type.startsWith("*")) {
                    child.type = child.type.substring(1);
                    child.status = null;
                    apply(child);
                }
            }

            node.status = "SIEVED";
            return null;
        }
    }

    static List<String> NODE_STATUSES =
        Arrays.asList("SIEVED", "TYPED", "PROCEDURE_CONVERTED", "LITERAL_CONVERTED");

    /**
     * Each conversion consists of a key of format "<TYPE>[:<VALUE>]" and cases. A
     * case consists of a path and a conversion node. The first case to have its
     * path matched by the source node will have its conversion node applied onto
     * the source node.
     */
    static Map<String, Map<String, ASTNode>> NODE_CONVERSIONS =
        buildConversions
        (
         "COMMAND_OR_DEFINITION",
         cases("$DEFINITION", n("*DEFINITION", "$DEFINITION"),
               "$COMMAND", n("*COMMAND", "$COMMAND"),
               "$", n("BLOCK",
                      loop("$COMMAND_OR_DEFINITION",
                           n("*COMMAND_OR_DEFINITION", "$")))),
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
                                n("PARAMS", "$FORMALS"),
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
         "DEF_FORMALS", n("*FORMALS", "$"),
         "FORMALS",
         cases("$VAR_PARAMETER", n("PARAMS",
                                   loop("$IDENTIFIER",
                                        n("PARAM", n("ID", "$"))),
                                   n("PARAM", "$VAR_PARAMETER")),
               "$", n("PARAMS",
                      loop("$IDENTIFIER",
                           n("PARAM", n("ID", "$"))))),
         "VAR_PARAMETER", n("PARAM",
                            n("ID", "$IDENTIFIER"),
                            n("TYPE", "Sch*")),
         "PROCEDURE_CALL", n("FUNCTION_CALL",
                             n("NAME", "$OPERATOR"),
                             loop("$OPERAND", n("ARGUMENT", "$")))
         );

    static List<String> TYPED_NODE_TYPES = Arrays.asList("PARAM", "DEF_VAR");

    static Map<String, String> STANDARD_SCHEME_PROCEDURES =
        Map.of(
               // Equivalence predicate
               "eqv?", "isEqv",
               "eq?", "isEq",
               "equal?", "isEqual",

               // Numbers
               "=", "numsEq",

               // Booleans
               "not", "!",
               "boolean?", "isBoolean"
               );

    ASTNode ast;
    ConvertSchemeAST convertSchemeAST;

    public IntermediateRepresentation(ASTNode ast)
    {
        this.ast = ast;
        this.convertSchemeAST = new ConvertSchemeAST();

    }

    static boolean nodeWasLiteralConverted(ASTNode node)
    {
        return node.status != null
                && NODE_STATUSES.indexOf(node.status) >= NODE_STATUSES.indexOf("LITERAL_CONVERTED");
    }

    static boolean nodeWasProcedureConverted(ASTNode node)
    {
        return node.status != null
            && NODE_STATUSES.indexOf(node.status) >= NODE_STATUSES.indexOf("PROCEDURE_CONVERTED");
    }

    static boolean nodeWasTyped(ASTNode node)
    {
        return node.status != null
            && NODE_STATUSES.indexOf(node.status) >= NODE_STATUSES.indexOf("TYPED");
    }

    static boolean nodeWasSieved(ASTNode node)
    {
        return node.status != null
            && NODE_STATUSES.indexOf(node.status) >= NODE_STATUSES.indexOf("SIEVED");
    }

    static boolean conversionExists(String conversionName)
    {
        String key = conversionName.startsWith("$") ?
            conversionName.substring(1) : conversionName;
        return NODE_CONVERSIONS.containsKey(key);
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
        Map<String, ASTNode> cases = new LinkedHashMap<>();
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
    ASTNode applyConversion(ASTNode sourceNode, ASTNode conversionNode)
    {
        return conversionNode.type.equals("loop") ?
            applyLoopConversion(sourceNode, conversionNode)
            : applySimpleConversion(sourceNode, conversionNode);
    }

    ASTNode applySimpleConversion(ASTNode sourceNode, ASTNode conversionNode)
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

            if (conversionNode.isLeaf() && conversionExists(conversionNode.value)) {
                foundNode.apply(convertSchemeAST);
            }

            node.value = foundNode.value;
            node.setChildren(foundNode.children);
        }

        for (ASTNode conversionChild : conversionNode.children) {
            ASTNode convertedChild = applyConversion(sourceNode, conversionChild);
            if (convertedChild.type == "loop") {
                node.addChildren(convertedChild.children);
            } else {
                node.addChildren(convertedChild);
            }
        }

        node.status = "SIEVED";

        return node;
    }

    ASTNode applyLoopConversion(ASTNode sourceNode, ASTNode conversionNode)
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

    static void convertBooleanLiteral(ASTNode node)
    {
        if (node.value.equals("#t") || node.value.equals("#true")) {
            node.value = "true";
        } else if (node.value.equals("#f") || node.value.equals("#false")) {
            node.value = "false";
        }
    }

    static String changeIntegerBase(String value, int radix)
    {
        List<Character> digits = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f');

        // Double number = (Double) 0;
        int number = 0;

        for (int i = 0; i < value.length(); i++) {
            number += digits.indexOf(value.charAt(i)) * Math.pow(radix, i);
        }

        return ((Integer) number).toString();
    }

    static String convertIntegerLiteral(String value)
    {
        int radix;
        if (value.contains("#b")) {
            radix = 2;
        } else if (value.contains("#x")) {
            radix = 6;
        } else if (value.contains("#o")) {
            radix = 8;
        } else {
            radix = 10;
        }
        return radix == 10 ? value : changeIntegerBase(value, radix);
    }

    static String convertRealLiteral(String value)
    {
        if (value.contains("/")) {
            String[] numbers = value.split("/");
            value =
                !numbers[1].equals("0") ?
                "Rational("
                + convertIntegerLiteral(numbers[0])
                + ", "
                + convertIntegerLiteral(numbers[1])
                + ")"
                : (numbers[0].contains("-") ?
                   "Dobule.NegativeInfinity" : "Dobule.PositiveInfinity");
        } else if (value.equals("+inf.0")) {
            value = "Double.PositiveInfinity";
        } else if (value.equals("-inf.0")) {
            value = "Double.NegativeInfinity";
        } else if (value.contains("nan.0")) {
            value = "Double.NaN";
        }
        return value;
    }

    static String convertNumberLiteral(String value)
    {
        value = value.replace("#i", "").replace("#e", "").replace("#d", "");

        if (value.contains("@")) {
            String[] numbers = value.split("@");
            value =
                "Polar("
                + convertRealLiteral(numbers[0])
                + ", "
                + convertRealLiteral(numbers[1])
                + ")";
        } else if (value.contains("i")) {
            Matcher matcher = Pattern.compile("(.*?)([\\+-][^\\+-]*i)$").matcher(value);
            value =
                "Complex("
                + convertRealLiteral(matcher.group(0).length() == 0 ?
                                     "0" : matcher.group(0))
                + ", "
                + convertRealLiteral(matcher.group(1))
                + ")";
        } else {
            value = "Complex(" + convertRealLiteral(value) + ", 0)";
        }

        return value;
    }

    static void convertNumberLiteral(ASTNode node)
    {
        node.value = convertNumberLiteral(node.value);
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
        ast.apply(new TypeSchemeAST());
    }

    void convertProcedures()
    {
        ast.apply(new ConvertProcedures());
    }

    void convertLiterals()
    {
        ast.apply(new ConvertLiterals());
    }

    public static ASTNode generateScalaAST(ASTNode schemeAst)
    {
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        ir.convertProcedures();
        ir.convertLiterals();
        return ir.ast;
    }
}
