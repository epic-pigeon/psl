package ParserPackage;


import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    public static TokenHolder lex(String code, Collection<Rule> rules, Rule toSkip) throws LexingException {
        int position = 0;
        Collection<Token> tokens = new Collection<>();
        rules.add(new Rule("\\/([^/]|(\\\\.))*\\/", "__LEXER_SETTINGS"));

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
                                rules = parseLexerSettings(matcher.group(), rules);
                                position += matcher.group().length();
                            } else {
                                tokens.add(new Token(rule.getName(), matcher.group(), position));
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
    private static Collection<Rule> parseLexerSettings(String s, Collection<Rule> oldRules) throws LexingException {
        Collection<Rule> newRules = new Collection<>();
        for (Rule rule: oldRules) newRules.add(new Rule(rule.getPatterns(), rule.getName()));
        Rule toSkip = new Rule("\\s+");
        Collection<Rule> rules = new Collection<>(
                new Rule("define", "COMMAND_DEFINE"),
                new Rule("redefine", "COMMAND_REDEFINE"),
                new Rule("add", "COMMAND_ADD"),
                new Rule("delete", "COMMAND_DELETE"),
                new Rule("print", "COMMAND_PRINT"),
                new Rule("[A-Z0-9_]+", "VALUE_IDENTIFIER"),
                new Rule("\\`[^\\`]*\\`", "VALUE_REGEX")
        );
        TokenHolder tokens = lex(s.substring(1, s.length() - 1), rules, toSkip);
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
                    rule.setPatterns(new Collection<>());
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
                    newRules.remove(rule);
                } else if (token.getName().equals("COMMAND_PRINT")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    System.out.println(rule);
                }
            }
        }
        return newRules;
    }

    private static Pattern parseRegex(String s) {
        s = s.replaceAll("(\\\\)(.)", "$2");
        return Pattern.compile(s.substring(1, s.length() - 1));
    }
}

class ContinueException extends Exception {
    ContinueException() {
        super("Continuation exception, if you see it, then something fucked up");
    }
}