package ParserPackage;

public class Environment {
    private Collection<Variable> variables;
    private Collection<BinaryOperator> binaryOperators;

    public Environment(Collection<Variable> variables, Collection<BinaryOperator> binaryOperators) {
        this.variables = variables;
        this.binaryOperators = binaryOperators;
    }

    public Collection<Variable> getVariables() {
        return variables;
    }

    public void setVariables(Collection<Variable> variables) {
        this.variables = variables;
    }

    public Collection<BinaryOperator> getBinaryOperators() {
        return binaryOperators;
    }

    public void setBinaryOperators(Collection<BinaryOperator> binaryOperators) {
        this.binaryOperators = binaryOperators;
    }

    public void addBinaryOperator(BinaryOperator binaryOperator) {
        binaryOperators.add(binaryOperator);
    }

    public void addVariable(Variable variable) {
        variables.add(variable);
    }
}
