package transpiler;

public class IntermediateRepresentation
{
    ASTNode ast;

    public IntermediateRepresentation(ASTNode ast)
    {
        this.ast = ast;
    }

    IntermediateRepresentation filterAST()
    {
        return this;
    }

    public static ASTNode generateScalaAST(ASTNode schemeAst)
    {
        IntermediateRepresentation ir = new IntermediateRepresentation(schemeAst);
        return ir.ast;
    }
}
