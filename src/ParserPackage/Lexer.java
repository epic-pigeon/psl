package ParserPackage;


import java.util.regex.Matcher;

public class Lexer {
    public static TokenHolder lex(String code, Collection<Rule> rules, Rule toSkip) throws LexingException {
        int position = 0;
        Collection<Token> tokens = new Collection<>();

        while (position < code.length()) {
            Matcher skipMatcher = toSkip.getPattern().matcher(code.substring(position));
            if (skipMatcher.find() && skipMatcher.start() == 0) {
                position += skipMatcher.end();
            }

            if (position >= code.length()) break;

            try {
                for (Rule rule: rules) {
                    Matcher matcher = rule.getPattern().matcher(code.substring(position));
                    if (matcher.find() && matcher.start() == 0) {
                        tokens.add(new Token(rule.getName(), matcher.group(), position));
                        position += matcher.group().length();
                        throw new ContinueException();
                    }
                }
            } catch (ContinueException ignored) {
                continue;
            }
            throw new LexingException(tokens, position);
        }

        return new TokenHolder(tokens);
    }
}

class ContinueException extends Exception {
    ContinueException() {
        super("Continuation exception, if you see it, then something fucked up");
    }
}