package ParserPackage;

import java.util.ArrayList;

public class LexingException extends Exception {
    private ArrayList<Token> tokens;
    private int position;

    public LexingException(ArrayList<Token> tokens, int position) {
        super("Lexing exception on position " + position);
        this.tokens = tokens;
        this.position = position;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
