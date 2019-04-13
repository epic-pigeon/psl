import ParserPackage.Collection;
import ParserPackage.Parser;
import ParserPackage.LexingException;
import ParserPackage.Value;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        long t = System.nanoTime();
        Collection<Value> result = Parser.parse(args[0]);
        System.out.println(result);
        System.out.println("Runtime: " + (double)(System.nanoTime() - t) / 1000000000);
    }
}