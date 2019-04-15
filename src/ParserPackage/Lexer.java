package ParserPackage;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    public static TokenHolder lex(String code, Collection<Rule> rules, Rule toSkip) throws Exception {
        int position = 0;
        Collection<Token> tokens = new Collection<>();
        rules.add(new Rule("\\/\\<([^\\/\\<\\>]|(\\\\.))*\\>\\/", "__LEXER_SETTINGS"));


        while (position < code.length()) {
            for (Pattern pattern: toSkip.getPatterns()) {
                Matcher skipMatcher = pattern.matcher(code.substring(position));
                if (skipMatcher.find() && skipMatcher.start() == 0) {
                    position += skipMatcher.end();
                    break;
                }
            }
            if (position >= code.length()) break;

            try {
                for (Rule rule: rules) {
                    for (Pattern pattern: rule.getPatterns()) {
                        Matcher matcher = pattern.matcher(code.substring(position));
                        if (matcher.find() && matcher.start() == 0) {
                            if (rule.getName().equals("__LEXER_SETTINGS")) {
                                tokens.add(new Token(rule.getName(), matcher.group(), position, matcher.group().split("\\r?\\r").length - 1));
                                    rules = parseLexerSettings(
                                            matcher.group().substring(2, matcher.group().length() - 2).replaceAll("(\\\\)([/<>])", "$2"), rules
                                    );
                                position += matcher.group().length();
                            } else {
                                tokens.add(new Token(rule.getName(), matcher.group(), position, matcher.group().split("\\r?\\r").length - 1));
                                position += matcher.group().length();
                            }
                            throw new ContinueException();
                        }
                    }
                }
            } catch (ContinueException ignored) {
                continue;
            }
            throw new LexingException(tokens, position);
        }

        return new TokenHolder(tokens);
    }
    private static Collection<Rule> parseLexerSettings(String s, Collection<Rule> oldRules) throws Exception {
        Collection<Rule> newRules = new Collection<>();
        for (Rule rule: oldRules) newRules.add(new Rule(rule.getPatterns(), rule.getName()));
        Rule toSkip = new Rule("\\s+");
        Collection<Rule> rules = new Collection<>(
                new Rule("define", "COMMAND_DEFINE"),
                new Rule("redefine", "COMMAND_REDEFINE"),
                new Rule("add", "COMMAND_ADD"),
                new Rule("delete", "COMMAND_DELETE"),
                new Rule("print", "COMMAND_PRINT"),
                new Rule("load", "COMMAND_LOAD"),
                new Rule("[A-Z0-9_]+", "VALUE_IDENTIFIER"),
                new Rule("\\`([^`\\\\]|(\\\\[`\\\\]))*\\`", "VALUE_REGEX"),
                new Rule("\\'[^']*\\'", "VALUE_STRING")
        );
        TokenHolder tokens = lex(s, rules, toSkip);
        Iterator<Token> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            Token token = iterator.next();
            if (token.getName().startsWith("COMMAND_")) {
                if (token.getName().equals("COMMAND_DEFINE")) {
                    Token name = iterator.next();
                    assert name.getName().equals("VALUE_IDENTIFIER");
                    newRules.add(new Rule(new Collection<>(), name.getValue()));
                } else if (token.getName().equals("COMMAND_REDEFINE")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    if (rule != null)
                        rule.setPatterns(new Collection<>());
                    else
                        newRules.add(new Rule(new Collection<>(), ruleToken.getValue()));
                } else if (token.getName().equals("COMMAND_ADD")) {
                    Token ruleToken = iterator.next();
                    Token regexToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    assert regexToken.getName().equals("VALUE_REGEX");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    rule.addPattern(parseRegex(regexToken.getValue()));
                } else if (token.getName().equals("COMMAND_DELETE")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    if (rule != null) newRules.remove(rule);
                } else if (token.getName().equals("COMMAND_PRINT")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    System.out.println(rule);
                } else if (token.getName().equals("COMMAND_LOAD")) {
                    Token fileToken = iterator.next();
                    assert fileToken.getName().equals("VALUE_STRING");
                    String filename = parseString(fileToken.getValue());
                    File file = new File(filename);
                    if (!Parser.getFileExtension(file).equalsIgnoreCase("pslcfg")) {
                        if (Parser.getFileExtension(file).equals("")) {
                            file = new File(filename + ".pslcfg");
                            if (Parser.getFileExtension(file).equals("")) throw new Exception("Wrong file: '" + filename + "'");
                        } else throw new Exception("Wrong file extension: " + file.getAbsolutePath());
                    }
                    byte[] data = new byte[(int) file.length()];
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        fis.read(data);
                        fis.close();
                    } catch (IOException ignored) {}
                    String code = new String(data, StandardCharsets.UTF_8);
                    try {
                        newRules = Lexer.parseLexerSettings(code, newRules);
                    } catch (LexingException e) {
                        throw new LexingException(tokens.getTokens(), fileToken.getPosition());
                    }
                }
            }
        }
        return newRules;
    }

    private static Pattern parseRegex(String s) {
        s = s.replaceAll("(\\\\)([`\\\\])", "$2");
        return Pattern.compile(s.substring(1, s.length() - 1));
    }
    private static String parseString(String s) {
        return s.substring(1, s.length() - 1);
    }
}

class ContinueException extends Exception {
    ContinueException() {
        super("Continuation exception, if you see it, then something fucked up");
    }
}