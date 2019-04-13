package ParserPackage;

public class Value {
    private Object value;
    private Class<?> type;

    public Value(Object value) {
        setValue(value);
    }

    public Value() {}

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        this.type = value.getClass();
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
