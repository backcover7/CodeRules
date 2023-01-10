import com.saucer.sast.lang.java.parser.filter.FilterHelper;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;

import java.util.List;

public class PaddingOracle extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "crypto";
    String rule = "javax.crypto:Cipher:getInstance:crypto";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        if (getSimpleName(invocation).equals("getInstance")
                && getQualifiedType(invocation.getTarget()).equals("javax.crypto.Cipher")) {
            List<CtExpression<?>> arguments = invocation.getArguments();
            return getElementStringValue(getArgument(arguments, 0)).contains("CBC");
        }
        return false;
    }
}