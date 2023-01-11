Saucer is a SAST tool which is based on Spoon (https://spoon.gforge.inria.fr/) and it is able to find out particular node elements based on csv and filter rules.

## Document
https://www.sfdc.co/Saucer

## Guide
❯ java -jar Saucer-0.1-all.jar --help
 ________  ________  ___  ___  ________  _______   ________          ___
|\   ____\|\   __  \|\  \|\  \|\   ____\|\  ___ \ |\   __  \        |\  \
\ \  \___|\ \  \|\  \ \  \\\  \ \  \___|\ \   __/|\ \  \|\  \       \ \  \
 \ \_____  \ \   __  \ \  \\\  \ \  \    \ \  \_|/_\ \   _  _\       \ \  \
  \|____|\  \ \  \ \  \ \  \\\  \ \  \____\ \  \_|\ \ \  \\  \|       \ \__\
    ____\_\  \ \__\ \__\ \_______\ \_______\ \_______\ \__\\ _\        \|__|
   |\_________\|__|\|__|\|_______|\|_______|\|_______|\|__|\|__|           ___
   \|_________|                                                           |\__\
                                                                          \|__|
                                    @Author: kang.hou@salesforce.com
Usage: <main class> [-hmV] [-d=<dependency>] [-o=<output>] <codebase>
Security Analysis on Plain Java.
      <codebase>          The path of target codebase.
  -d, --dependency=<dependency>
                          The path of dependency jar files of target codebase.
                          * Default is None.
  -h, --help              Show this help message and exit.
  -m, --maven             Specify if the target is built by Maven
  -o, --output=<output>   The path of output report.
                          * Default is current directory.
  -V, --version           Print version information and exit.

### Examples
java -Xmx6g -jar Saucer-0.1-all.jar -o ~/Downloads test-cases/java/test.java
java -Xmx6g -jar Saucer-0.1-all.jar -m Commons-BeanUtils/

## Rule Sources
1. https://github.com/momosecurity/momo-code-sec-inspector-java/tree/2018.3/src/main/java/com/immomo/momosec/lang
2. https://portal.securecodewarrior.com/#/home
3. https://twitter.com/search?q=(from%3ASonarSource%2C%20OR%20from%3Aintigriti)&src=typed_query
4. gradle in CSVResolver
5. https://github.com/elttam/semgrep-rules
6. Thread vulnerability