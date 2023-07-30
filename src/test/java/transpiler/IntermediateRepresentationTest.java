package transpiler;

import static transpiler.ASTNodeTestUtils.compareASTNodes;
import static transpiler.ASTNodeTestUtils.n;
import org.junit.Test;
import org.junit.Ignore;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class IntermediateRepresentationTest
{
    @Test
    public void sieveProcedureCalls()
    {
        ASTNode expectedAst =
            n("EXPRESSION",
              n("FUNCTION_CALL",
                n("NAME",
                  n("EXPRESSION",
                    n("IDENTIFIER", "+"))),
                n("ARGUMENT",
                  n("EXPRESSION",
                    n("LITERAL",
                      n("NUMBER", "1")))),
                n("ARGUMENT",
                  n("EXPRESSION",
                    n("FUNCTION_CALL",
                      n("NAME",
                        n("EXPRESSION",
                          n("IDENTIFIER", "*"))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("NUMBER", "2")))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("NUMBER", "3")))))))));

        ASTNode schemeAst =
            n("EXPRESSION",
              n("PROCEDURE_CALL",
                n("OPERATOR",
                  n("EXPRESSION",
                    n("IDENTIFIER", "+"))),
                n("OPERAND",
                  n("EXPRESSION",
                    n("LITERAL",
                      n("SELF_EVALUATING",
                        n("NUMBER", "1"))))),
                n("OPERAND",
                  n("EXPRESSION",
                    n("PROCEDURE_CALL",
                      n("OPERATOR",
                        n("EXPRESSION",
                          n("IDENTIFIER", "*"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "2"))))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "3"))))))))));

        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Ignore("TODO")
    @Test
    public void sieveProcedures()
    {
        ASTNode expectedAst = null;
        ASTNode schemeAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("DEFINITION", "define",
                  n("IDENTIFIER", "fact"),
                  n("EXPRESSION",
                    n("LAMBDA_EXPRESSION", "lambda",
                      n("FORMALS",
                        n("VARIABLE",
                          n("IDENTIFIER", "n"))),
                      n("BODY",
                        n("SEQUENCE",
                          n("EXPRESSION",
                            n("CONDITIONAL", "if",
                              n("TEST",
                                n("EXPRESSION",
                                  n("PROCEDURE_CALL",
                                    n("OPERATOR",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "="))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "n"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("LITERAL",
                                          n("SELF_EVALUATING",
                                            n("NUMBER", "0")))))))),
                              n("CONSEQUENT",
                                n("EXPRESSION",
                                  n("LITERAL",
                                    n("SELF_EVALUATING",
                                      n("NUMBER", "1"))))),
                              n("ALTERNATE",
                                n("EXPRESSION",
                                  n("PROCEDURE_CALL",
                                    n("OPERATOR",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "*"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("IDENTIFIER", "n"))),
                                    n("OPERAND",
                                      n("EXPRESSION",
                                        n("PROCEDURE_CALL",
                                          n("OPERATOR",
                                            n("EXPRESSION",
                                              n("IDENTIFIER", "fact"))),
                                          n("OPERAND",
                                            n("EXPRESSION",
                                              n("PROCEDURE_CALL",
                                                n("OPERATOR",
                                                  n("EXPRESSION",
                                                    n("IDENTIFIER", "-"))),
                                                n("OPERAND",
                                                  n("EXPRESSION",
                                                    n("IDENTIFIER", "n"))),
                                                n("OPERAND",
                                                  n("EXPRESSION",
                                                    n("LITERAL",
                                                      n("SELF_EVALUATING",
                                                        n("NUMBER", "1")))))))))))))))))))))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieveConditionalsWithAlternate()
    {
        ASTNode expectedAst =
            n("EXPRESSION",
              n("CONDITIONAL",
                n("TEST",
                  n("EXPRESSION",
                    n("FUNCTION_CALL",
                      n("NAME",
                        n("EXPRESSION",
                          n("IDENTIFIER", "="))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("LITERAL",
                              n("NUMBER", "0"))))))),
                n("SEQUENCE",
                  n("EXPRESSION",
                    n("LITERAL",
                        n("NUMBER", "1")))),
                n("ALTERNATE",
                  n("EXPRESSION",
                    n("FUNCTION_CALL",
                      n("NAME",
                        n("EXPRESSION",
                          n("IDENTIFIER", "*"))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("FUNCTION_CALL",
                            n("NAME",
                              n("EXPRESSION",
                                n("IDENTIFIER", "fact"))),
                            n("ARGUMENT",
                              n("EXPRESSION",
                                n("FUNCTION_CALL",
                                  n("NAME",
                                    n("EXPRESSION",
                                      n("IDENTIFIER", "-"))),
                                  n("ARGUMENT",
                                    n("EXPRESSION",
                                      n("IDENTIFIER", "n"))),
                                  n("ARGUMENT",
                                    n("EXPRESSION",
                                      n("LITERAL",
                                          n("NUMBER", "1")))))))))))))));
        ASTNode schemeAst =
            n("EXPRESSION",
              n("CONDITIONAL", "if",
                n("TEST",
                  n("EXPRESSION",
                    n("PROCEDURE_CALL",
                      n("OPERATOR",
                        n("EXPRESSION",
                          n("IDENTIFIER", "="))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "0")))))))),
                n("CONSEQUENT",
                  n("EXPRESSION",
                    n("LITERAL",
                      n("SELF_EVALUATING",
                        n("NUMBER", "1"))))),
                n("ALTERNATE",
                  n("EXPRESSION",
                    n("PROCEDURE_CALL",
                      n("OPERATOR",
                        n("EXPRESSION",
                          n("IDENTIFIER", "*"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("PROCEDURE_CALL",
                            n("OPERATOR",
                              n("EXPRESSION",
                                n("IDENTIFIER", "fact"))),
                            n("OPERAND",
                              n("EXPRESSION",
                                n("PROCEDURE_CALL",
                                  n("OPERATOR",
                                    n("EXPRESSION",
                                      n("IDENTIFIER", "-"))),
                                  n("OPERAND",
                                    n("EXPRESSION",
                                      n("IDENTIFIER", "n"))),
                                  n("OPERAND",
                                    n("EXPRESSION",
                                      n("LITERAL",
                                        n("SELF_EVALUATING",
                                          n("NUMBER", "1"))))))))))))))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieveConditionalsWithoutAlternate()
    {
        ASTNode expectedAst =
            n("EXPRESSION",
              n("CONDITIONAL",
                n("TEST",
                  n("EXPRESSION",
                    n("FUNCTION_CALL",
                      n("NAME",
                        n("EXPRESSION",
                          n("IDENTIFIER", "="))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("ARGUMENT",
                        n("EXPRESSION",
                          n("LITERAL",
                              n("NUMBER", "0"))))))),
                n("SEQUENCE",
                  n("EXPRESSION",
                    n("LITERAL",
                        n("NUMBER", "1")))),
                n("ALTERNATE")));
        ASTNode schemeAst =
            n("EXPRESSION",
              n("CONDITIONAL", "if",
                n("TEST",
                  n("EXPRESSION",
                    n("PROCEDURE_CALL",
                      n("OPERATOR",
                        n("EXPRESSION",
                          n("IDENTIFIER", "="))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("IDENTIFIER", "n"))),
                      n("OPERAND",
                        n("EXPRESSION",
                          n("LITERAL",
                            n("SELF_EVALUATING",
                              n("NUMBER", "0")))))))),
                n("CONSEQUENT",
                  n("EXPRESSION",
                    n("LITERAL",
                      n("SELF_EVALUATING",
                        n("NUMBER", "1"))))),
                n("ALTERNATE")));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Ignore("TODO")
    @Test
    public void sieveAssignments()
    {
        ASTNode expectedAst = null;
        ASTNode schemeAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION",
                    n("ASSIGNMENT", "set!",
                      n("IDENTIFIER", "nIsZero"),
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "="))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "n"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("LITERAL",
                                n("SELF_EVALUATING",
                                n("NUMBER", "0"))))))))))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        compareASTNodes(expectedAst, ir.ast);
    }
}
