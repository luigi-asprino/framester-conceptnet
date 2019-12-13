# Framester-ConceptNet

This repository contains:
* A tool (i.e. ConceptNet Refactor) for transforming ConceptNet's CSV [dumps](https://github.com/commonsense/conceptnet5/wiki/Downloads#assertions) in NQUADS.


## ConceptNet Refactor

ConceptNet Refactor is a tool that enables to transform a ConceptNet's CSV [dump](https://github.com/commonsense/conceptnet5/wiki/Downloads#assertions) as follows.


INPUT (CSV FORMAT)

```
<ID_EDGE> <ID_PREDICATE>  <ID_SUBJECT>  <ID_OBJECT> {"dataset": "..", "license": "..", "sources": [..], "weight": <WEIGHT>}
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
${resourcePrefix}<ID_EDGE> <https://w3id.org/framester/metadata/schema/conceptNetWeight> "<WEIGHT>"^^<http://www.w3.org/2001/XMLSchema#double> ${graph} .
```

Content of ${schemaFilePath}
```
${resourcePrefix}<ID_PREDICATE> rdf:type owl:ObjectProperty ${schemaBottomUpURI} .
```

## Software License

* ConceptNet Refactor is distributed under an [Apache License 2.0](https://raw.githubusercontent.com/luigi-asprino/framester-conceptnet/master/Software-LICENSE)

## Data License

* Data generated with ConceptNet Refactor is distributer under Creative Commons Attribution-ShareAlike 4.0 [CC BY SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

## Credits

This work includes data from ConceptNet 5, which was compiled by the
Commonsense Computing Initiative. ConceptNet 5 is freely available under
the Creative Commons Attribution-ShareAlike license (CC BY SA 4.0) from
http://conceptnet.io.

The included data was created by contributors to Commonsense Computing
projects, contributors to Wikimedia projects, DBPedia, OpenCyc, Games
with a Purpose, Princeton University's WordNet, Francis Bond's Open
Multilingual WordNet, and Jim Breen's JMDict.


