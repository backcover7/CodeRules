This looks like a Markdown file.Beautify Now!No, thanks.
# Rules

## JAVA Native Deserialization
### URLDNS
```sql
match (source:Method) where source.NAME="readObject"
match (sink:Method) where sink.IS_SINK=true and sink.VUL="SSRF"
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 7) yield path
return * limit 10
```

### Click1 or Beanutils1
```sql
match (source:Method) where source.IS_GADGETSOURCE=true and source.NAME="compare"
match (sink:Method) where sink.IS_SINK=true and sink.VUL="CODE"
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 6) yield path
return * limit 10
```

### c3p0
```sql
match (source:Method) where (source.NAME="readObject" and source.SUB_SIGNATURE="void readObject(java.io.ObjectInputStream)") or (source.NAME="readExternal" and source.SUB_SIGNATURE="void readExternal(java.io.ObjectInput)")
match (sink:Method) where sink.IS_SINK=true and sink.VUL="JNDI"
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 3) yield path
return path
```

### clojure
```sql
match (source:Method) where source.NAME="hashCode" and source.CLASSNAME=~"clojure.*"
match (sink:Method) where sink.IS_SINK=true and sink.VUL="CODE" and sink.NAME="invoke"
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 6) yield path
return path
```

## Marshalsec
### JdbcRowSetImpl
```sql
match (source:Method) where source.IS_SETTER=true
match (sink:Method) where sink.IS_SINK=true and sink.VUL="JNDI"
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 3) yield path
return *
```

## Notes
### gadget sources
```sql
match (source:Method)<-[:ALIAS]-(g:Method {IS_GADGETSOURCE:true}) where source.CLASSNAME=~"org.eclipse.jetty.*" and not(source.CLASSNAME contains "test")
```

### readObject sources
```sql
match (source:Method) where ((source.NAME="readObject" and source.SUB_SIGNATURE="void readObject(java.io.ObjectInputStream)") or (source.NAME="readExternal" and source.SUB_SIGNATURE="void readExternal(java.io.ObjectInput)"))
and source.CLASSNAME=~"org.eclipse.jetty.*" and not(source.CLASSNAME contains "test")
```

### marshalsec sources
```sql
match (source:Method) where source.IS_SETTER=true or source.IS_GETTER=true or source.NAME="<init>"
and source.CLASSNAME=~"org.eclipse.jetty.*" and not(source.CLASSNAME contains "test")
```

### web sources
```sql
match (source:Method) where source.IS_SOURCE=true or source.IS_ENDPOINT=true
```

### sink
```sql
match (sink:Method)-[:CALL]->(s:Method) where s.IS_SINK=true and sink.CLASSNAME=~"org.eclipse.jetty.*" and not(sink.CLASSNAME contains "test")
```

### path
```sql
call apoc.algo.allSimplePaths(sink, source, "<CALL|ALIAS", 3) yield path
where all(n in nodes(path) where n.CLASSNAME=~"org.eclipse.jetty.*" and not (n.CLASSNAME contains "test")) and none(n in nodes(path) where n.CLASSNAME in [org.blacklist])
```
