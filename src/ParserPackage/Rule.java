package ParserPackage;

import java.util.regex.Pattern;

public class Rule {
    private Pattern pattern;
    private String name;

    public Rule(Pattern pattern, String name) {
        this.pattern = pattern;
        this.name = name;
    }

    public Rule(String regex, String name) {
        pattern = Pattern.compile(regex);
        this.name = name;
    }

    public Rule(Pattern pattern) {
        this.pattern = pattern;
    }

    public Rule(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}