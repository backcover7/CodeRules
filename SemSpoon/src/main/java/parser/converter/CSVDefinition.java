package parser.converter;

public class CSVDefinition {
    // NAMESPACE:CLASSTYPE:METHOD:KIND
    // If one column is wrapped by brackets then it's regex
    // METHOD might be empty

    public final static String NAMESPACE = "namespace";
    public final static String CLASSTYPE = "classtype";
    public final static String METHOD = "method";
    public final static String KIND = "kind";

    public final static String ANNOTATION = "ANNOTATION";

    public final static int NAMESPACEINDEX = 0;
    public final static int CLASSTYPEINDEX = 1;
    public final static int METHODINDEX = 2;
    public final static int KINDINDEX = 3;

    public static void main(String[] args) {
        new CodeQL().process();
        new Findsecbugs().process();
    }
}
