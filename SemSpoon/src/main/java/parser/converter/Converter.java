package parser.converter;

public class Converter {
    // NAMESPACE:CLASSTYPE:METHOD:KIND
    // If one column is wrapped by brackets then it's regex
    // METHOD might be empty

    final static String NAMESPACE = "namespace";
    final static String CLASSTYPE = "classtype";
    final static String METHOD = "method";
    final static String KIND = "kind";

    final static int NAMESPACEINDEX = 0;
    final static int CLASSTYPEINDEX = 1;
    final static int METHODINDEX = 2;
    final static int KINDINDEX = 4;

    public static void main(String[] args) {
        new CodeQL().process();
        new Findsecbugs().process();
    }
}
