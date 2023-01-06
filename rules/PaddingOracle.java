import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtElement;
import com.saucer.sast.lang.java.parser.query.FilterHelper;

import java.util.List;

public class PaddingOracle extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "crypto";
    String rule = "javax.crypto:Cipher:getInstance:crypto";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        if (invocation.getExecutable().getSimpleName().equals("getInstance")) {
            CtElement target = getConstantVariable(invocation.getTarget());
            if (target instanceof CtTypeAccess) {
                CtTypeAccess<?> typeAccess = (CtTypeAccess<?>) target;
                if (typeAccess.getAccessedType().getQualifiedName().equals("javax.crypto.Cipher")) {
                    List<CtExpression<?>> arguments = invocation.getArguments();
                    CtElement transformation = getConstantVariable(arguments.get(0));
                    if (transformation instanceof CtLiteral) {
                        CtLiteral<?> transformationLiteral = (CtLiteral<?>) transformation;
                        return transformationLiteral.getValue().toString().contains("CBC");
                    }
                }
            }
        }
        return false;
    }
}
