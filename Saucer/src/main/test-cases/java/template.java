import javax.naming.Context;
import javax.naming.InitialContext;

public class template {
    void x() {
        Context ctx = new InitialContext();
        ctx.lookup("ss");
    }
}
