package ParserPackage;

public class Variable {
    private String name;
    private Value value;

    public Variable(String name) {
        this.name = name;
    }

    public Variable(String name, Value value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Variable " + name + ": " + value;
    }
}
