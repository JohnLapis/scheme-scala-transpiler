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
    public void sieve_addTypes_command()
    {
        ASTNode expectedAst =
            n("PROGRAM",
              n("COMMAND",
                n("EXPRESSION")));
        ASTNode schemeAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION",
                n("COMMAND",
                  n("EXPRESSION"))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_definition()
    {
        ASTNode expectedAst =
            n("PROGRAM",
              n("DEFINITION",
                n("EXPRESSION")));
        ASTNode schemeAst = n("PROGRAM",
                n("COMMAND_OR_DEFINITION",
                        n("DEFINITION",
                          n("EXPRESSION"))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_block()
    {
        ASTNode expectedAst =
            n("PROGRAM",
              n("BLOCK",
                n("COMMAND",
                  n("EXPRESSION")),
                n("DEFINITION",
                  n("EXPRESSION")),
                n("COMMAND",
                  n("EXPRESSION"))));
        ASTNode schemeAst =
            n("PROGRAM",
              n("COMMAND_OR_DEFINITION", "begin",
                n("COMMAND_OR_DEFINITION",
                  n("COMMAND",
                    n("EXPRESSION"))),
                n("COMMAND_OR_DEFINITION",
                  n("DEFINITION",
                    n("EXPRESSION"))),
                n("COMMAND_OR_DEFINITION",
                  n("COMMAND",
                    n("EXPRESSION")))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_variableDefinition()
    {
        ASTNode expectedAst =
            n("DEF_VAR",
              n("ID", "n"),
              n("EXPRESSION",
                n("LITERAL",
                  n("NUMBER", "1"))),
              n("TYPE", "Sch"));
        ASTNode schemeAst =
            n("DEFINITION", "define",
              n("IDENTIFIER", "n"),
              n("EXPRESSION",
                n("LITERAL",
                  n("SELF_EVALUATING",
                    n("NUMBER", "1")))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         ir.addTypes();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_procedureCalls()
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
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_procedure_Syntax1()
    {
        ASTNode expectedAst =
            n("DEF_VAR",
              n("ID", "fact"),
              n("EXPRESSION",
                n("LAMBDA",
                  n("PARAMS",
                    n("PARAM",
                      n("ID", "a"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "b"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "c"),
                      n("TYPE", "Sch"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("FUNCTION_CALL",
                          n("NAME",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))),
              n("TYPE", "Sch"));
        ASTNode schemeAst =
            n("DEFINITION", "define",
              n("IDENTIFIER", "fact"),
              n("EXPRESSION",
                n("LAMBDA_EXPRESSION", "lambda",
                  n("FORMALS",
                    n("IDENTIFIER", "a"),
                    n("IDENTIFIER", "b"),
                    n("IDENTIFIER", "c")),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         ir.addTypes();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_procedure_Syntax2()
    {
        ASTNode expectedAst =
            n("DEF_VAR",
              n("ID", "fact"),
              n("EXPRESSION",
                n("LAMBDA",
                  n("PARAMS",
                    n("PARAM",
                      n("ID", "a"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "b"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "c"),
                      n("TYPE", "Sch"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("FUNCTION_CALL",
                          n("NAME",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))),
              n("TYPE", "Sch"));
        ASTNode schemeAst =
            n("DEFINITION", "define",
              n("IDENTIFIER", "fact"),
              n("DEF_FORMALS",
                n("IDENTIFIER", "a"),
                n("IDENTIFIER", "b"),
                n("IDENTIFIER", "c")),
              n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         ir.addTypes();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_procedureWithDottedVariables_Syntax1()
    {
        ASTNode expectedAst =
            n("DEF_VAR",
              n("ID", "fact"),
              n("EXPRESSION",
                n("LAMBDA",
                  n("PARAMS",
                    n("PARAM",
                      n("ID", "a"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "b"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "c"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "d"),
                      n("TYPE", "Sch*"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("FUNCTION_CALL",
                          n("NAME",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))),
              n("TYPE", "Sch"));
        ASTNode schemeAst =
            n("DEFINITION", "define",
              n("IDENTIFIER", "fact"),
              n("EXPRESSION",
                n("LAMBDA_EXPRESSION", "lambda",
                  n("FORMALS",
                    n("IDENTIFIER", "a"),
                    n("IDENTIFIER", "b"),
                    n("IDENTIFIER", "c"),
                    n("VAR_PARAMETER",
                      n("IDENTIFIER", "d"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         ir.addTypes();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_procedureWithDottedVariables_Syntax2()
    {
        ASTNode expectedAst =
            n("DEF_VAR",
              n("ID", "fact"),
              n("EXPRESSION",
                n("LAMBDA",
                  n("PARAMS",
                    n("PARAM",
                      n("ID", "a"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "b"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "c"),
                      n("TYPE", "Sch")),
                    n("PARAM",
                      n("ID", "d"),
                      n("TYPE", "Sch*"))),
                  n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("FUNCTION_CALL",
                          n("NAME",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("ARGUMENT",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))))),
              n("TYPE", "Sch"));
        ASTNode schemeAst =
            n("DEFINITION", "define",
              n("IDENTIFIER", "fact"),
              n("DEF_FORMALS",
                n("IDENTIFIER", "a"),
                n("IDENTIFIER", "b"),
                n("IDENTIFIER", "c"),
                n("VAR_PARAMETER",
                  n("IDENTIFIER", "d"))),
              n("BODY",
                    n("SEQUENCE",
                      n("EXPRESSION",
                        n("PROCEDURE_CALL",
                          n("OPERATOR",
                            n("EXPRESSION",
                              n("IDENTIFIER", "+"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "a"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "b"))),
                          n("OPERAND",
                            n("EXPRESSION",
                              n("IDENTIFIER", "c"))))))));
         IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
         ir.sieveAST();
         ir.addTypes();
         compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_conditionalsWithAlternate()
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
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_conditionalsWithoutAlternate()
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
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }

    @Test
    public void sieve_addTypes_assignments()
    {
        ASTNode expectedAst =
            n("EXPRESSION",
              n("ASSIGNMENT",
                n("ID", "nIsZero"),
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
                          n("NUMBER", "0"))))))));
        ASTNode schemeAst =
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
                            n("NUMBER", "0")))))))));
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        ir.sieveAST();
        ir.addTypes();
        compareASTNodes(expectedAst, ir.ast);
    }
}
