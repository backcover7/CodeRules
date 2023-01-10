Saucer is a SAST tool which is based on Spoon (https://spoon.gforge.inria.fr/) and it is able to find out particular node elements based on csv and filter rules.

## Document
https://www.sfdc.co/Saucer

## Guide
java -Xmx6g -jar Saucer-0.1-all.jar --help
Usage: <main class> [-hmV] [-c=<csv>] [-d=<dependency>] [-o=<output>]
                    [-r=<rule>] <codebase>
Scan Java codebase to find security threats.
      <codebase>          The path of target codebase.
  -c, --csv=<csv>         The path of security analysis csv rules.
  -d, --dependency=<dependency>
                          The path of dependency jar files of target codebase.
                          * Default is None.
  -h, --help              Show this help message and exit.
  -m, --maven             Specify if the target is built by Maven
  -o, --output=<output>   The path of output report.
                          * Default is current directory.
  -r, --rule=<rule>       The path of security analysis filter rules.
  -V, --version           Print version information and exit.

## Rule Sources
1. https://github.com/momosecurity/momo-code-sec-inspector-java/tree/2018.3/src/main/java/com/immomo/momosec/lang
2. https://portal.securecodewarrior.com/#/home
3. https://twitter.com/search?q=(from%3ASonarSource%2C%20OR%20from%3Aintigriti)&src=typed_query
4. gradle in CSVResolver
5. https://github.com/elttam/semgrep-rules
6. thread vulnerability