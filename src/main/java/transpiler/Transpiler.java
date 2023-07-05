package transpiler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.List;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import transpiler.SchemeScanner.Token;

public class Transpiler {
    public static void main(String[] args) throws IOException, ParseException, Exception {
        if (args.length != 1) {
            System.out.println("Wrong number of parameters provided.");
            throw new Exception();
        }

        String inputFilename = args[0];
        Path path = Paths.get(inputFilename);
        String schemeCode = String.join("\n", Files.readAllLines(path));
        String luaCode = convertSchemeToLuaCode(schemeCode);
        try (PrintWriter outputStream = new PrintWriter("output.lua")) {
            outputStream.println(luaCode);
        }
    }

    public static String convertSchemeToLuaCode(String code)
    {
        List<Token> schemeCodeTokens = SchemeScanner.tokenize(code);
        SchemeAST schemeAST = SchemeScanner.parse(schemeCodeTokens);
        IntermediateAST intermediateAST =
            SchemeScanner.convertSchemeToIntermediateAST(schemeAST);
        LuaAST luaAST = LuaScanner.convertIntermediateToLuaAST(intermediateAST);
        return LuaScanner.generate(luaAST);
    }
}
