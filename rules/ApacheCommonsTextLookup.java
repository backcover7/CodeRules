import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import com.saucer.sast.lang.java.parser.query.FilterHelper;

import java.util.List;

public class ApacheCommonsTextLookup extends FilterHelper<CtInvocation<?>> {
    String category = "sink";
    String kind = "el";
    String rule = "org.apache.commons.text:StringSubstitutor:replace:el";
    boolean isInvocationSink = true;

    @Override
    public boolean matches(CtInvocation invocation) {
        if (invocation.getExecutable().getSimpleName().equals("replace")) {
            CtElement target = getConstantVariable(invocation.getTarget());
            if (target instanceof CtInvocation) {
                CtInvocation<?> targetInvocation = (CtInvocation<?>) target;
                if (targetInvocation.getExecutable().getSimpleName().equals("createInterpolator")) {
                    if (targetInvocation.getTarget() instanceof CtTypeAccess) {
                        CtTypeAccess<?> typeAccess = (CtTypeAccess<?>) targetInvocation.getTarget();
                        return typeAccess.getAccessedType().getQualifiedName().equals("org.apache.commons.text.StringSubstitutor");
                    }
                }
            } else if (target instanceof CtConstructorCallImpl) {
                CtConstructorCall<?> targetConstructor = (CtConstructorCall<?>) target;
                CtExecutableReference<?> executable = targetConstructor.getExecutable();
                if (executable.getType().getQualifiedName().equals("org.apache.commons.text.StringSubstitutor")) {
                    List<CtExpression<?>> arguments = targetConstructor.getArguments();
                    if (arguments.size() == 1) {
                        CtElement argumentElement = getConstantVariable(arguments.get(0));
                        if (argumentElement instanceof CtInvocationImpl<?>) {
                            return ((CtInvocationImpl<?>) argumentElement).getExecutable().getSimpleName().equals("interpolatorStringLookup");
                        }
                    }
                }
            }
        }
        return false;
    }
}