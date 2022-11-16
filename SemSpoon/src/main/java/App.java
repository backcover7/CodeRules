import config.SpoonConfig;

import spoon.reflect.CtModel;

public class App {
    public static void main(String[] args) {
        SpoonConfig spoonConfig = new SpoonConfig();

//        String codebase = Paths.get(FileUtils.Expanduser("~/Documents/CodeRules/SemSpoon")).toAbsolutePath().toString();
//        CtModel model = spoonConfig.getSpoonMavenLauncher(codebase);

        String codebase = "src/main/resources/ResponseEntity.java";
        CtModel model = spoonConfig.getSpoonModel(codebase);
    }
}
