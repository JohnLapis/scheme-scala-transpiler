package transpiler.scala;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import transpiler.ASTNode;

public class ScalaUnparser
{
    record Cases(Map<String, Template> cases) {}

    record Block(String string, BlockType type) {}

    record Template(List<Block> blocks) {}

    enum BlockType {RAW, METAVARIABLE, LOOPED_METAVARIABLE}

    static String LOOP_PREFIX = "loop:";

    static Map<String, Cases> TEMPLATES =
        buildTemplates(
            "PROGRAM", "<loop:*>",
            "LITERAL", cases("$BOOLEAN", "<BOOLEAN>",
                             "$NUMBER", "<NUMBER>",
                             "$VECTOR", "<VECTOR>",
                             "$CHARACTER", "<CHARACTER>",
                             "$STRING", "<STRING>",
                             "$BYTEVECTOR", "<BYTEVECTOR>"),
            "CONDITIONAL", cases("$ALTERNATE", "if (<TEST>) {<SEQUENCE>} else {<ALTERNATE>}",
                                 "$", "if (<TEST>) {<SEQUENCE>}"),
            "FUNCTION_CALL", "<NAME>(<loop:ARGUMENT>)",
            "ASSIGNMENT", "<ID> = <EXPRESSION>",
            "DEF_VAR", "var <ID>: <TYPE> = <EXPRESSION>",
            "LAMBDA", "<PARAMS> => <BODY>",
            "PARAMS", "(<loop:PARAM>)"
            );

    // The values of this map serve as separators for metavariables if they are
    // looped.
    static Map<String, String> SEPARATORS =
        Map.of(
               "PROGRAM", "\n",
               "PARAMS", ", ",
               "ARGUMENT", ", "
            );
    static String DEFAULT_SEPARATOR = "";

    static List<String> INFIX_FUNCTIONS = Arrays.asList("+", "*", "-", "/", "%");

    ASTNode ast;
    List<String> codeLines;

    public ScalaUnparser(ASTNode ast)
    {
        this.ast = ast;
        this.codeLines = new ArrayList<>();
    }

    static Cases cases(Object... objs)
    {
        Map<String, Template> cases = new LinkedHashMap<>();
        for (int i = 0; i < objs.length; i += 2) {
            if (objs[i] instanceof String condition
                    && objs[i + 1] instanceof String string) {
                cases.put(condition, parseTemplateString(string));
            } else {
                throw new RuntimeException("Expected (String, String) pair.");
            }
        }
        return new Cases(cases);
    }

    static Map<String, Cases> buildTemplates(Object... objs)
    {
        Map<String, Cases> map = new HashMap<>();
        for (int i = 0; i < objs.length; i += 2) {
            String key;
            Cases value;
            if (objs[i] instanceof String s) {
                key = s;
            } else {
                throw new RuntimeException("Expected (String, Cases | String) pair.");
            }

            if (objs[i + 1] instanceof Cases cases) {
                value = cases;
            } else if (objs[i + 1] instanceof String templateString) {
                value = cases("$", templateString);
            } else {
                throw new RuntimeException("Expected (String, Cases | String) pair.");
            }

            map.put(key, value);
        }
        return map;
    }

    static Template parseTemplateString(String string)
    {
        List<Block> blocks = new ArrayList<>();
        Matcher matcher = Pattern.compile("<[^>]+>").matcher(string);
        int lastIndex = 0;

        System.out.println(string);
        while (matcher.find()) {
            String startRawString = string.substring(lastIndex, matcher.start());
            System.out.println("start: " + startRawString);
            if (!startRawString.isEmpty()) {
                blocks.add(new Block(startRawString, BlockType.RAW));
            }

            // We skip the "<" and ">" around the metavariable
            String metaexpr = string.substring(matcher.start() + 1,
                                               matcher.end() - 1);
            String metavariable;
            BlockType type;

            if (metaexpr.startsWith(LOOP_PREFIX)) {
                metavariable = metaexpr.substring(LOOP_PREFIX.length());
                type = BlockType.LOOPED_METAVARIABLE;
            } else {
                metavariable = metaexpr;
                type = BlockType.METAVARIABLE;
            }

            System.out.println("meta: " + metavariable);

            blocks.add(new Block(metavariable, type));

            lastIndex = matcher.end();
        }

        String endRawString = string.substring(lastIndex, string.length());
        System.out.println("end: " + endRawString);
        if (!endRawString.isEmpty()) {
            blocks.add(new Block(endRawString, BlockType.RAW));
        }
        System.out.println("==========================================");

        return new Template(blocks);
    }

    static String parseBlock(String block)
    {
        return block.substring(1, block.length() - 1);
    }

    static String getSeparator(List<ASTNode> nodes)
    {
        return getSeparator(nodes.isEmpty() ? null : nodes.get(0));
    }

    static String getSeparator(ASTNode node)
    {
        return SEPARATORS.getOrDefault(node.type, DEFAULT_SEPARATOR);
    }

    static boolean hasInfixOperator(ASTNode node)
    {
        return INFIX_FUNCTIONS.contains(node.getByPath("$NAME.EXPRESSION.IDENTIFIER").value);
    }

    static String unparseFunctionCallWithInfixOperator(ASTNode functionCall)
    {
        String operatorName = functionCall.getByPath("$NAME.EXPRESSION.IDENTIFIER").value;
        List<ASTNode> argumentNodes = functionCall.getAllByPath("$ARGUMENT");
        List<String> arguments = new ArrayList<>();

        for (ASTNode node : argumentNodes) {
            // Parentheses need to be added to guarantee the preservation of the
            // order of operations.
            arguments.add("(" + stringify(node) + ")");
        }

        return String.join(" " + operatorName + " ", arguments);
    }

    static List<String> fillTemplate(Template template, ASTNode node)
    {
        if (node.type == "FUNCTION_CALL" && hasInfixOperator(node)) {
            return Arrays.asList(unparseFunctionCallWithInfixOperator(node));
        }

        System.out.println(node.type);
        List<String> filledBlocks = new ArrayList<>();

        for (Block block : template.blocks) {
            System.out.println(block);
            String filledBlock = "";

            switch (block.type) {
            case RAW:
                filledBlock = block.string;
                break;
            case METAVARIABLE:
                ASTNode foundNode = node.getByPath("$" + block.string);
                System.out.println(foundNode);
                filledBlock = foundNode == null ?
                    "" : stringify(foundNode);
                break;
            case LOOPED_METAVARIABLE:
                List<String> segments = new ArrayList<>();
                List<ASTNode> foundNodes = block.string.equals("*") ?
                    node.children : node.getAllByPath("$" + block.string);
                for (ASTNode _node : foundNodes) {
                    segments.add(stringify(_node));
                }
                filledBlock = String.join(getSeparator(foundNodes), segments);
                break;
            }

            filledBlocks.add(filledBlock);
        }

        return Arrays.asList(String.join("", filledBlocks));
    }

    static String stringify(ASTNode node)
    {
        List<String> blocks = generateCodeBlocks(node);
        if (blocks != null) {
            return String.join(getSeparator(node), blocks);
        }

        // If a node has no value, then its children possess the semantics. If it has
        // no templates (since `blocks == null`), then it means it's a simple node
        // and has a single child.
        return node.value == null ? stringify(node.children.get(0)) : node.value;
    }

    static List<String> generateCodeBlocks(ASTNode node)
    {
        Cases templateCases = TEMPLATES.get(node.type);

        if (templateCases == null) return null;

        Template template = null;
        for (Map.Entry<String, Template> entry : templateCases.cases.entrySet()) {
            if (node.hasByPath(entry.getKey())) {
                template = entry.getValue();
                break;
            }
        }

        if (template == null) return null;

        return fillTemplate(template, node);
    }

    void generateCodeBlocks()
    {
        System.out.println(ast);
        codeLines = generateCodeBlocks(ast);
    }

    public static String generateCode(ASTNode ast)
    {
        ScalaUnparser unparser = new ScalaUnparser(ast);
        unparser.generateCodeBlocks();
        return String.join("\n", unparser.codeLines);
    }
}
