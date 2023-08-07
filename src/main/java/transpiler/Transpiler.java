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
import transpiler.scheme.SchemeScanner;
import transpiler.scala.ScalaUnparser;

public class Transpiler {
    public static void main(String[] args) throws IOException, ParseException, Exception {
        if (args.length != 1) {
            System.out.println("Wrong number of parameters provided.");
            throw new Exception();
        }

        String inputFilename = args[0];
        Path path = Paths.get(inputFilename);
        String schemeCode = String.join("\n", Files.readAllLines(path));
        String scalaCode = convertSchemeToScalaCode(schemeCode);
        try (PrintWriter outputStream = new PrintWriter("output.scala")) {
            outputStream.println(scalaCode);
        }
    }

    public static String convertSchemeToScalaCode(String schemeCode)
    {
        ASTNode schemeAst = SchemeScanner.generateAST(schemeCode);
        ASTNode scalaAst = IntermediateRepresentation.generateScalaAST(schemeAst);
        String scalaCode = ScalaUnparser.generateCode(scalaAst);
        return scalaCode;
    }
}
