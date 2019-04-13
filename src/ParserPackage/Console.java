package ParserPackage;

public class Console {
    public void log(Object... args) {
        System.out.println(new Collection<>(args).join());
    }
}
