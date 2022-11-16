package parser.ast;

import config.SpoonConfig;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class LoadSink {
    public void LocateSink(CtModel model) {
        Collection<CtType<?>> classtypes = model.getAllTypes();
        for(CtType<?> classtype : classtypes) {
            Set<CtMethod<?>> methods = classtype.getMethods();

            for (CtMethod<?> method : methods) {

                List<CtExecutableReference<?>> executableReferences = method.getElements(new TypeFilter<>(CtExecutableReference.class));
                for (CtExecutableReference<?> ctExecutableReference : executableReferences) {
                    if (ctExecutableReference.isConstructor()) {
                        Constructor<?> executeActualConstructor = ctExecutableReference.getActualConstructor();
                    } else {
                        Method executeActualMethod = ctExecutableReference.getActualMethod();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SpoonConfig spoonConfig = new SpoonConfig();

        String codebase = "src/main/resources";
        CtModel model = spoonConfig.getSpoonModel(codebase);

        new LoadSink().LocateSink(model);
    }
}
