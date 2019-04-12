import ParserPackage.Parser;
import ParserPackage.LexingException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        Parser.parse(args[0]);
    }
}