package ParserPackage;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

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
            add(new Rule("(\\d*(\\.\\d+)(e-?\\d+)?)|(\\d+(\\.\\d+)?(e-?\\d+)?)", "VALUE_NUMBER"));
            add(new Rule("('([^'\\\\]|(\\\\.))*')|(\"([^\"\\\\]|(\\\\.))*\")", "VALUE_STRING"));
            add(new Rule("\\`([\\s\\S])*\\`", "VALUE_FUNCTION"));
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

        ArrayList<Value> output = new ArrayList<>();
        ArrayList<Value> operator = new ArrayList<>();

        for(Token token: tokenHolder.getTokens()) {
            if (token.getName().startsWith("VALUE_")) {
                if (token.getName().equals("VALUE_STRING")) {
                    output.add(parseString(token.getValue()));
                } else if (token.getName().equals("VALUE_NUMBER")) {
                    output.add(parseNumber(token.getValue()));
                } else if (token.getName().equals("VALUE_FUNCTION")) {
                    output.add(parseFunction(token.getValue()));
                } else {
                    /* wtf */
                }
            } else if (token.getName().equals("LEFT_PAREN")) {
                if (output.get(output.size() - 1).getType() == JSFunction.class) {
                    Value f = output.remove(output.size() - 1);
                    operator.add(f);
                    output.add(new Value(new Object()));
                    operator.add(new Value(new Object()));
                } else throw new Exception("Unexpected left parenthesis");
            } else if (token.getName().equals("RIGHT_PAREN")) {
                while (operator.get(operator.size() - 1).getType() != Object.class) {
                    output.add(operator.remove(operator.size() - 1));
                }
                if (operator.get(operator.size() - 1).getType() == Object.class) {
                    operator.remove(operator.size() - 1);
                }
            }
        }
        while (operator.size() > 0) {
            output.add(operator.remove(operator.size() - 1));
        }

        System.out.println(output);

        ArrayList<Value> stack = new ArrayList<>();
        for (Value value : output) {
            if (value.getType() == JSFunction.class) {
                ArrayList<Value> args = new ArrayList<>();
                Value val;
                while ((val = stack.remove(stack.size() - 1)).getType() != Object.class) {
                    args.add(val);
                }
                stack.add(((Function<ArrayList<Value>, Value>)value.getValue()).apply(args));
            } else stack.add(value);
        }

        System.out.println(stack);
    }
    private static Value parseString(String s) {
        return new Value(s.substring(1, s.length() - 1).replaceAll("(\\\\)(.)", "$2"));
    }
    private static Value parseNumber(String s) throws ParseException {
        return new Value(NumberFormat.getInstance().parse(s));
    }
    private static Value parseFunction(String s) {
        return new Value(new JSFunction(s));
    }
}

class JSFunction implements Function<ArrayList<Value>, Value> {
    private String s;

    public JSFunction(String s) {
        this.s = s;
    }

    @Override
    public Value apply(ArrayList<Value> values) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        ScriptObjectMirror result = null;
        try {
            result = (ScriptObjectMirror) engine.eval(s.substring(1, s.length() - 1));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return new Value(result.call(null, values.toArray()));
    }
}