package ParserPackage;

import java.util.function.BiFunction;

public class BinaryOperator {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BinaryOperator(String name, BiFunction<Value, Value, Value> action) {
        this.name = name;
        this.action = action;
    }

    private String name;

    public BiFunction<Value, Value, Value> getAction() {
        return action;
    }

    public void setAction(BiFunction<Value, Value, Value> action) {
        this.action = action;
    }

    private BiFunction<Value, Value, Value> action;

    public Value eval(Value a, Value b) {
        return action.apply(a, b);
    };
}
