package config;

import soot.G;
import soot.Main;
import soot.Scene;
import soot.options.Options;
import utils.ClasspathUtils;

import java.io.File;

public class SootConfiguration {
    public static void initSootOption(){
        String output = String.join(File.separator, System.getProperty("user.dir"), "temp");
        G.reset();

        Options.v().set_verbose(true); // 打印详细信息

        Options.v().set_prepend_classpath(true); // 优先载入soot classpath
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true); // 记录文件行数
        Options.v().set_src_prec(Options.src_prec_class); // 优先处理class格式
        Options.v().set_output_dir(output); // 设置IR Jimple的输出目录
        Options.v().set_output_format(Options.output_format_jimple); // 输出Jimple格式
//        Options.v().set_validate(true);
//        Options.v().set_ignore_classpath_errors(true); // Ignores invalid entries on the Soot classpath.
        Options.v().set_whole_program(true);// 目前开启过程间分析不会进行实质上的过程间分析，开启当前flag只是为了解决依赖缺失的问题
        Options.v().set_no_writeout_body_releasing(true); // 当输出内容后不释放获取的body数据
//        Options.v().set_no_bodies_for_excluded(true);
//        Options.v().set_omit_excepting_unit_edges(true);
        // 设置自定义的package
//        PhaseOptions.v().setPhaseOption("cg","on");
    }
}
