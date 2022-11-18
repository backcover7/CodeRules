import com.sun.jndi.url.rmi.rmiURLContext;
import org.apache.maven.shared.utils.xml.XmlStreamWriter;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class test {
    public void x(OutputStream outputStream) throws NamingException {
        new XmlStreamWriter(outputStream);
        new rmiURLContext().lookup("aa");
    }
}
