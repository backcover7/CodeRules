import javax.naming.Context;
import javax.naming.InitialContext;

public class test {
    public test(String a) {
        System.out.println(a);
    }

    void y() {
        System.out.println("aaa");
    }

    public static void main(String[] args) {

    }
}

class m extends test {
    public m() {
        super("mm");
    }

    void u() {
        super.y();
    }
}
