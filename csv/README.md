# Nodes

## Source / Sink CSV
https://github.com/github/codeql/blob/main/java/ql/lib/semmle/code/java/dataflow/ExternalFlow.qll
`namespace: classtype: method: kind`

1. The `namespace` column selects a package.
2. The `classtype` column selects a type within that package.
3. The `method` column optionally selects a specific named member of the type. If the method is constructor then its name is "<init>".
4. The `calltype` column is type of the method, static or instance.
5. The `kind` column is a tag that to determine to which classes the interpreted elements should be added. For example, for sources "remote" indicates a default remote flow source, and for summaries "taint" indicates a default additional taint step and "value" indicates a globally applicable value-preserving step.

## Database
https://dbdiagram.io/