package ParserPackage;

import java.util.ArrayList;
import java.util.Iterator;

public class TokenHolder {
    private ArrayList<Token> tokens;

    public TokenHolder(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[\n    ");
        for (int i = 0; i < tokens.size(); i++) {
            if (i != 0) stringBuilder.append(",\n    ");
            stringBuilder.append(tokens.get(i).toString());
        }
        stringBuilder.append("\n]");
        return stringBuilder.toString();
    }

    public Iterator<Token> iterator() {
        return tokens.iterator();
    }
}
