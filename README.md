# Framester-ConceptNet


## ConceptNet Refactor

This repository contains:
* A script that serialises a ConceptNet's CSV [dump](https://github.com/commonsense/conceptnet5/wiki/Downloads#assertions) as follows.

INPUT (CSV FORMAT)

```
<ID_EDGE> <ID_PREDICATE>  <ID_SUBJECT>  <ID_OBJECT> {"dataset": "..", "license": "..", "sources": [..], "weight": 1.0}
```

N.B. With notation ${variable} we indicate a variable in the [configuration file](https://github.com/luigi-asprino/framester-conceptnet/blob/master/src/main/resources/properties.properties).

The output is split into two files ${conceptNetDumpFilePath} and ${schemaFilePath}:
* ${conceptNetDumpFilePath} contains the ConcentNet's assertions and a subset of metadata associated with them. ${conceptNetDumpFilePath}  file is serialized in NQUADS format and compressed with gzip.
* ${schemaFilePath} contains ConceptNet's schema derived with a bottom-up approach. ${schemaFilePath} is serialized in NQUADS format. 

Content of ${conceptNetDumpFilePath} 
```
${resourcePrefix}<ID_SUBJECT> ${resourcePrefix}<ID_PREDICATE> ${resourcePrefix}<ID_OBJECT> ${resourcePrefix}<ID_EDGE> .
${resourcePrefix}<ID_EDGE> <https://w3id.org/framester/metadata/schema/isSubGraphOf> ${graph} ${graph} .
${resourcePrefix}<ID_EDGE> <https://w3id.org/framester/metadata/schema/wasDerivedFrom> ${conceptNetPrefix}<ID_EDGE> ${graph} .
```

Content of ${schemaFilePath}
```
${resourcePrefix}<ID_PREDICATE> rdf:type owl:ObjectProperty ${schemaBottomUpURI} .
```


