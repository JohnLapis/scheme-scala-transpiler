package transpiler.scheme;

public class Token
{
    public String value;
    public TokenType type;

    public Token(TokenType type, String value)
    {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Token token) {
            return type == token.type && value == token.value;
        } else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return "(" + type + ", " + value + ")";
    }
}
