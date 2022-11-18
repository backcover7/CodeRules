import config.SpoonConfig;

//import parser.hierarchy.ClassHierachy;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

import java.util.Collection;

public class App {
    public static void main(String[] args) {
        SpoonConfig spoonConfig = new SpoonConfig();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

        String codebase = "src/main/resources/sink1.java";
        CtModel model = spoonConfig.getSpoonModel(codebase);

        Collection<CtType<?>> ctTypes = model.getAllTypes();
        for (CtType<?> ctType:ctTypes) {
//            new ClassHierachy().process(ctType);
        }
    }
}
