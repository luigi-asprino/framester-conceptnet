package it.cnr.istc.stlab.framester.conceptnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.istc.stlab.lgu.commons.files.FileUtils;
import it.cnr.istc.stlab.lgu.commons.iterations.ProgressCounter;
import it.cnr.istc.stlab.lgu.commons.tables.RowsIteratorCSV;

public class ConceptNetRefactorizer {

	private static Logger logger = LoggerFactory.getLogger(ConceptNetRefactorizer.class);

	private static void parseCN() throws IOException {

		String isSubGraphOf = "https://w3id.org/framester/metadata/schema/isSubGraphOf";
		String wasDerivedFrom = "https://w3id.org/framester/metadata/schema/wasDerivedFrom";
		String conceptNetWeight = "https://w3id.org/framester/conceptnet/schema/conceptNetWeight";
		String conceptNetIdentifier = "https://w3id.org/framester/conceptnet/schema/conceptNetIdentifier";

		Configuration c = Configuration.getConfiguration();
		logger.info("Reading: {}", c.getConceptNetDumpFilePath());

		InputStream fileStreamLC = new FileInputStream(c.getConceptNetDumpFilePath());
		InputStream gzipStreamLC = new GZIPInputStream(fileStreamLC);
		Reader decoderLC = new InputStreamReader(gzipStreamLC);
		long lines = FileUtils.countNumberOfLines(decoderLC);
		logger.info("Number of lines {}", lines);

		InputStream fileStream = new FileInputStream(c.getConceptNetDumpFilePath());
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream);
		BufferedReader buffered = new BufferedReader(decoder);
		ProgressCounter pc = new ProgressCounter(lines);
		RowsIteratorCSV ricsv = new RowsIteratorCSV(buffered, '\t');
		new File(c.getDumpRDF()).delete();
		GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(new File(c.getDumpRDF())));
		StreamRDF stream = StreamRDFWriter.getWriterStream(gzip, RDFFormat.NQ);

		stream.prefix("fcn570", c.getConceptNetPrefix());
		stream.prefix("fcn-schema", "https://w3id.org/framester/conceptnet/schema/");
		stream.prefix("f-meta", "https://w3id.org/framester/metadata/schema/");

		OntModel schema = ModelFactory.createOntologyModel();

		int line = 0;
		while (ricsv.hasNext()) {

			try {

				String[] strings = (String[]) ricsv.next();
				String sng = c.getResourcePrefix() + strings[0].substring(1);
				String cn = c.getConceptNetPrefix() + strings[0].substring(1);
				String p = c.getResourcePrefix() + strings[1].substring(1);
				String s = c.getResourcePrefix() + strings[2].substring(1);
				String o = c.getResourcePrefix() + strings[3].substring(1);
				
				if (!strings[1].equals("/r/ExternalURL")) {
					o = strings[3];
				}

				// streaming assertion
				stream.quad(new Quad(NodeFactory.createURI(sng),
						new Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))));

				// streaming assertion isSubgGraphOf conceptnet
				stream.quad(new Quad(NodeFactory.createURI(c.getGraph()), new Triple(NodeFactory.createURI(sng),
						NodeFactory.createURI(isSubGraphOf), NodeFactory.createURI(c.getGraph()))));

				// streaming assertion wasDerivedFrom
				stream.quad(new Quad(NodeFactory.createURI(c.getGraph()), new Triple(NodeFactory.createURI(sng),
						NodeFactory.createURI(wasDerivedFrom), NodeFactory.createURI(cn))));

				// streaming assertion conceptNetIdentifier
				stream.quad(new Quad(NodeFactory.createURI(c.getGraph()), new Triple(NodeFactory.createURI(sng),
						NodeFactory.createURI(conceptNetIdentifier), NodeFactory.createURI(strings[0]))));

				// streaming concept identifier of the subject
				stream.quad(new Quad(NodeFactory.createURI(c.getGraph()), new Triple(NodeFactory.createURI(s),
						NodeFactory.createURI(conceptNetIdentifier), NodeFactory.createURI(strings[2]))));

				// streaming concept identifer of the object
				stream.quad(new Quad(NodeFactory.createURI(c.getGraph()), new Triple(NodeFactory.createURI(o),
						NodeFactory.createURI(conceptNetIdentifier), NodeFactory.createURI(strings[3]))));

				// adding relation to bottom up schema
				schema.add(schema.createObjectProperty(p), RDF.type, OWL.ObjectProperty);

				// adding conceptnet identifier of relation
				schema.add(schema.createObjectProperty(p), schema.createProperty(conceptNetIdentifier),
						OWL.ObjectProperty);

				try {
					JSONObject obj = new JSONObject(strings[4]);
					stream.quad(new Quad(NodeFactory.createURI(c.getGraph()),
							new Triple(NodeFactory.createURI(sng), NodeFactory.createURI(conceptNetWeight), NodeFactory
									.createLiteral(LiteralLabelFactory.createTypedLiteral(obj.getDouble("weight"))))));
				} catch (org.json.JSONException e) {
					logger.error("Error with parsing JSON at line {}", line);
				}

			} catch (ArrayIndexOutOfBoundsException e) {
				logger.error("Error with parsing line {}", line);
			}

			pc.increase();
			line++;

		}
		gzip.flush();
		stream.finish();
		gzip.finish();
		gzip.close();

		DatasetGraph d = DatasetGraphFactory.create();
		d.begin(ReadWrite.WRITE);
		d.addGraph(NodeFactory.createURI(c.getSchemaBottomUpURI()), schema.getGraph());
		d.commit();
		d.end();
		d.close();
		RDFDataMgr.write(new FileOutputStream(new File(c.getSchemaFilePath())), d, RDFFormat.NQ);

	}

	public static void main(String[] args) throws IOException {
		parseCN();
	}
}
