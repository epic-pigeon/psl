package ParserPackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Parser {
    public static void parse(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String code = new String(data, StandardCharsets.UTF_8);
        ArrayList<Rule> rules = new ArrayList<Rule>(){{
            add(new Rule(";", "SEMICOLON"));
            add(new Rule("//", "LINE_COMMENT"));
            add(new Rule("\\(", "LEFT_PAREN"));
            add(new Rule("\\)", "RIGHT_PAREN"));
            add(new Rule("(\\d*(\\.\\d+)(e-?\\d+)?)|(\\d+(\\.\\d+)?(e-?\\d+)?)", "NUMBER"));
            add(new Rule("('([^'\\\\]|(\\\\.))*')|(\"([^\"\\\\]|(\\\\.))*\")", "STRING"));
            add(new Rule("[a-zA-Z_$][a-zA-Z0-9_$]+", "IDENTIFIER"));
            add(new Rule("\\r?\\n", "NEWLINE"));
        }};
        Rule toSkip = new Rule("[^\\S\\n]+");
        TokenHolder tokenHolder = null;
        try {
            tokenHolder = Lexer.lex(code, rules, toSkip);
        } catch (LexingException e) {
            ArrayList<Token> tokens = e.getTokens();
            Token lastNewLine = null;
            int k = 0;
            for (Token token: tokens) if ("NEWLINE".equals(token.getName())) {
                k++;
                lastNewLine = token;
            }
            int relativePosition;
            if (k == 0) {
                relativePosition = e.getPosition();
            } else {
                relativePosition = e.getPosition() - lastNewLine.getPosition() - 1;
            }
            throw new Exception("Unexpected token in " + file.getAbsolutePath() + ":" + (k + 1) + ":" + relativePosition + " (absolute position: " + e.getPosition() + ")");
        }
        System.out.println(tokenHolder);

    }
}
