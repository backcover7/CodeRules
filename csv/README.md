# Nodes

## Method CSV
Namespace:Type$InternalType:TypeModifiers:Superclass:Interface:Method:MethodModifiers:ArgumentSize:ArgumentTypes:Return:Input:Output:Signature:Kind:Node
1. `Namespace`: Package name of the Java class.
2. `Type`: Classname.
3. `InternalType`: Classname of internal Java class.
4. `TypeModifiers`: Modifiers of Java class.
5. `Superclass`: One Java class can only extend another one Java class. If a class has no explicit extends then it extends from java.lang.Object.
6. `Interface`: One Java class can implement several interfaces.
7. `Method`: Method name.
8. `MethodModifiers`: Modifier of the method.
9. `ArgumentSize`: Count of arguments of the method.
10. `ArgumentTypes`: Types of the arguments of the method.
11. `Return`: Type of the return value of the method. If the method is constructor then it's empty.
12. `Kind`: Kind of the procedure of this method
13. `Category`: If the class or method comes from JDK.

Examples:
java.io:File:public:java.lang.Object:java.io.Serializable,java.lang.Comparable:File:public:1:java.lang.String::0:-1:io:taint

#### Description
12. If one column is empty it means this column is not suitable for currenty entry.
13. If InternalType column is not empty, then columns from 6 to 13 are all info about the internal class instead of namespace.type
14. The Extends, Implements and ArgumentTypes should all be full type name except for class under java.lang.
15. If column value are multiple, they will be separated by comma(,).
16. Method CSV nodes could only be generated based on Class CSV nodes.

## Source / Sink CSV
https://github.com/github/codeql/blob/main/misc/scripts/models-as-data/generate_flow_model.py
Reuse but did a little changes on the CSV node flow in codeql (java/ql/lib/semmle/code/java/dataflow/ExternalFlow.qll)
`namespace: type: name: calltype: return: kind: category`

1. The `namespace` column selects a package.
2. The `type` column selects a type within that package.
3. The `name` column optionally selects a specific named member of the type. If the method is constructor then its name is "<init>".
5. The `calltype` column is type of the method, static or instance.
6. The `return` column is type of the return value of the method. If the method is constructor then it's empty.
7. The `kind` column is a tag that can be referenced from QL to determine to which classes the interpreted elements should be added. For example, for sources "remote" indicates a default remote flow source, and for summaries "taint" indicates a default additional taint step and "value" indicates a globally applicable value-preserving step.
8. The `category` column is a tag if the class or method comes from JDK.

## Database
https://dbdiagram.io/

Table classtype {
  class_id        int
  namespace       varchar
  classtype       varchar
  type_modifer    varchar
  superclass      varchar
  interface       varchar
  category        varchar
}

Table methods {
  class_id        int
  method_id       int
  methodname      varchar
  method_modifier varchar
  argument_size   varchar
  argument_type   varchar
  return_type     varchar
  kind            varchar
  category        varchar
}

## Alias
extends, implements, override

## Return type
Recursively find return type
select from methods where return_val == "javax.script.ScriptEngine";