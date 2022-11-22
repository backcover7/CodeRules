import javax.naming.Context;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JxltEngine;
import java.io.Writer;

public class test {
    void x(JxltEngine.Template template, JexlContext var1, Writer var2) {
        template.evaluate(var1, var2);
    }


}
