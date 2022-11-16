import javax.naming.InitialContext;
import javax.naming.NamingException;

public class test {
    public static void main(String[] args) throws NamingException {
        new InitialContext().lookup("aa");
    }
}
