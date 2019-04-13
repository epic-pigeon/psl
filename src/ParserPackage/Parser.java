package ParserPackage;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.CollationElementIterator;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Function;

public class Parser {
    public static Environment defaultEnvironment = new Environment(
            new Collection<>(
                    new Variable("print",
                            new Value(
                                    new PSLFunction(args -> {
                                        System.out.println("printed: " + args);
                                        return null;
                                    })
                            )
                    ),
                    new Variable("random",
                            new Value(
                                    new PSLFunction(args -> {
                                        switch (args.size()) {
                                            case 0:
                                                return new Value(Math.random());
                                            case 1:
                                                double arg = ((Number)args.get(0).getValue()).doubleValue();
                                                return new Value(Math.random() * arg);
                                            default:
                                                double first = ((Number)args.get(0).getValue()).doubleValue();
                                                double second = ((Number)args.get(1).getValue()).doubleValue();
                                                return new Value(first + Math.random() * (second - first));
                                        }
                                    })
                            )
                    )
            ),
            new Collection<>()
    );
    public static Collection<Value> parse(String filename, Environment environment) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String code = new String(data, StandardCharsets.UTF_8);
        Collection<Rule> rules = new Collection<Rule>(){{
            add(new Rule(";", "SEMICOLON"));
            add(new Rule(",", "COMMA"));
            add(new Rule("//", "LINE_COMMENT"));
            add(new Rule("\\(", "LEFT_PAREN"));
            add(new Rule("\\)", "RIGHT_PAREN"));
            add(new Rule("(\\d*(\\.\\d+)(e-?\\d+)?)|(\\d+(\\.\\d+)?(e-?\\d+)?)", "VALUE_NUMBER"));
            add(new Rule("VALUE_STRING",
                    new Collection<>(
                            "'([^'\\\\\\n]|(\\\\.))*'",
                            "\"([^\"\\\\\\n]|(\\\\.))*\")"
                    )
            ));
            add(new Rule("\\`([^\\`]|(\\\\.))*\\`", "VALUE_JS"));
            add(new Rule("[a-zA-Z_$][a-zA-Z0-9_$]*", "IDENTIFIER"));
            add(new Rule("\\r?\\n", "NEWLINE"));
        }};
        Rule toSkip = new Rule("[^\\S\\n]+");
        TokenHolder tokenHolder;
        try {
            tokenHolder = Lexer.lex(code, rules, toSkip);
            System.out.println(tokenHolder);
        } catch (LexingException e) {
            Collection<Token> tokens = e.getTokens();
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
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < relativePosition; i++) spaces.append(" ");
            throw new Exception("Unexpected token in " + file.getAbsolutePath() + ":" + (k + 1) + ":" + relativePosition + " (absolute position: " + e.getPosition() + "):\n" +
                                code.split("\\r?\\n")[k] + "\n" +
                                spaces.toString() + "^");
        }

        Collection<Value> output = new Collection<>();
        Collection<Value> operator = new Collection<>();

        for(Token token: tokenHolder.getTokens()) {
            if (token.getName().startsWith("VALUE_")) {
                if (token.getName().equals("VALUE_STRING")) {
                    output.add(parseString(token.getValue()));
                } else if (token.getName().equals("VALUE_NUMBER")) {
                    output.add(parseNumber(token.getValue()));
                } else if (token.getName().equals("VALUE_JS")) {
                    Value value = parseJSValue(token.getValue());
                    if (!(value instanceof JSValue)) output.add(new Value(new DenyCall()));
                    output.add(value);
                } else {
                    /* wtf */
                }
            } else if (token.getName().equals("LEFT_PAREN")) {
                operator.add(new Value(new ApproveCall()));
                if (Function.class.isAssignableFrom(output.get(output.size() - 1).getType())) {
                    Value f = output.remove(output.size() - 1);
                    output.remove(output.size() - 1);
                    operator.add(f);
                    output.add(new Value(new ApproveCall()));
                }
            } else if (token.getName().equals("RIGHT_PAREN")) {
                while (operator.get(operator.size() - 1).getType() != ApproveCall.class) {
                    output.add(operator.remove(operator.size() - 1));
                }
                if (operator.get(operator.size() - 1).getType() == ApproveCall.class) {
                    operator.remove(operator.size() - 1);
                }
            } else if (token.getName().equals("IDENTIFIER")) {
                Variable variable = environment.getVariables().findFirst(variable1 -> variable1.getName().equals(token.getValue()));
                if (variable != null) {
                    Value value = variable.getValue();
                    if (Function.class.isAssignableFrom(value.getType())) output.add(new Value(new DenyCall()));
                    output.add(value);
                } else {
                    output.add(new Value(new Identifier(token.getValue())));
                }
            }
        }
        while (operator.size() > 0) {
            output.add(operator.remove(operator.size() - 1));
        }

        Collection<Value> stack = new Collection<>();
        for (Value value : output) {
            if (Function.class.isAssignableFrom(value.getType())) {
                Collection<Value> args = new Collection<>();
                Value val;
                while (!ArgumentEnd.class.isAssignableFrom((val = stack.remove(stack.size() - 1)).getType())) {
                    args.add(val);
                }
                if (val.getType() == ApproveCall.class)
                    stack.add(((Function<Collection<Value>, Value>) value.getValue()).apply(args));
                else stack.add(value);
            } else stack.add(value);
        }

        return stack;
    }
    public static Collection<Value> parse(String filename) throws Exception {
        return parse(filename, defaultEnvironment);
    }
    private static Value parseString(String s) {
        return new Value(s.substring(1, s.length() - 1).replaceAll("(\\\\)(.)", "$2"));
    }
    private static Value parseNumber(String s) throws ParseException {
        String[] parts = s.split("e");
        Double number = Double.parseDouble(parts[0]);
        try {
            long exponent = Long.parseLong(parts[1]);
            number *= Math.pow(10, exponent);
        } catch (IndexOutOfBoundsException ignored){}
        return new Value((Number) number);
    }
    private static Value parseFunction(String s) {
        return new Value(new JSFunction(s));
    }
    private static Value parseJSValue(String s) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        Object result = null;
        try {
            result = engine.eval(s.substring(1, s.length() - 1));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        try {
            if (((ScriptObjectMirror) result).isFunction()) {
                return parseFunction(s);
            } else return new JSValue(s);
        } catch (ClassCastException ignored) {
            return new JSValue(s);
        }
    }
}

class JSFunction implements Function<Object, Value> {
    private String s;

    public JSFunction(String s) {
        this.s = s;
    }

    @Override
    public Value apply(Object values) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        ScriptObjectMirror result = null;
        try {
            result = (ScriptObjectMirror) engine.eval(s.substring(1, s.length() - 1));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        Object[] args = new Object[]{values};
        try {
            args = ((Collection<Value>) values).map(value -> value.getType().cast(value.getValue())).reverse().toArray();
        } catch (ClassCastException ignored){}
        return new Value(
                result.call(null, args)
        );
    }
}

class PSLFunction implements Function<Object, Value> {
    private Function<Collection<Value>, Value> action;

    public PSLFunction(Function<Collection<Value>, Value> action) {
        this.action = action;
    }

    @Override
    public Value apply(Object o) {
        if (o instanceof Collection) {
            return action.apply(((Collection<Value>) o).reverse());
        } else return action.apply(new Collection<>(new Value(o)));
    }
}

class JSValue extends Value {
    public JSValue(Object object) {
        if (object.getClass() == String.class) {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            Object result = null;
            String s = object.toString();
            try {
                result = engine.eval(s.substring(1, s.length() - 1));
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            setValue(result);
        } else {
            setValue(object);
        }
    }
}

class Identifier {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

class ArgumentEnd {}
class ApproveCall extends ArgumentEnd {}
class DenyCall extends ArgumentEnd {}