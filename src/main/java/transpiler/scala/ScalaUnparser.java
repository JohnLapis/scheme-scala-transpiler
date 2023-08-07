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
            "EXPRESSION", cases("$LITERAL", "<LITERAL>"),
            "LITERAL", cases("$BOOLEAN", "<BOOLEAN>",
                             "$NUMBER", "<NUMBER>",
                             "$VECTOR", "<VECTOR>",
                             "$CHARACTER", "<CHARACTER>",
                             "$STRING", "<STRING>",
                             "$BYTEVECTOR", "<BYTEVECTOR>"),
            "CONDITIONAL", cases("$ALTERNATE", "if (<TEST>) {<SEQUENCE>} else {<ALTERNATE>}",
                                 "$", "if (<TEST>) {<SEQUENCE>}"),
            "ASSIGNMENT", "<ID> = <EXPRESSION>",
            "DEF_VAR", "var <ID>: <TYPE> = <EXPRESSION>",
            "LAMBDA", "<PARAMS> => <BODY>",
            "PARAMS", "(<loop:PARAM>)"
            );

    // The values of this map serve as separators for metavariables if they are
    // looped.
    static  Map<String, String> SEPARATORS =
        Map.of(
               "PROGRAM", "\n",
               "PARAMS", ", "
            );

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

        while (matcher.find()) {
            String startRawString = string.substring(lastIndex, matcher.start());
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

            blocks.add(new Block(metavariable, type));

            lastIndex = matcher.end();
        }

        String endRawString = string.substring(lastIndex, string.length());
        if (!endRawString.isEmpty()) {
            blocks.add(new Block(endRawString, BlockType.RAW));
        }

        return new Template(blocks);
    }

    static String parseBlock(String block)
    {
        return block.substring(1, block.length() - 1);
    }

    static String getSeparator(ASTNode node)
    {
        return SEPARATORS.getOrDefault(node.type, "");
    }

    static List<String> fillTemplate(Template template, ASTNode node)
    {
        List<String> filledBlocks = new ArrayList<>();

        for (Block block : template.blocks) {
            String filledBlock = "";

            switch (block.type) {
            case RAW:
                filledBlock = block.string;
                break;
            case METAVARIABLE:
                ASTNode foundNode = node.getByPath("$" + block.string);
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
                filledBlock = String.join("", segments);
                break;
            }

            filledBlocks.add(filledBlock);
        }

        return Arrays.asList(String.join("", filledBlocks));
    }

    static String stringify(ASTNode node)
    {
        List<String> blocks = generateCodeBlocks(node);
        if (blocks == null) {
            return  node.value == null ? "" : node.value;
        }
        return  String.join(getSeparator(node), blocks);
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
        codeLines = generateCodeBlocks(ast);
    }

    public static String generateCode(ASTNode ast)
    {
        ScalaUnparser unparser = new ScalaUnparser(ast);
        unparser.generateCodeBlocks();
        return String.join("\n", unparser.codeLines);
    }
}
