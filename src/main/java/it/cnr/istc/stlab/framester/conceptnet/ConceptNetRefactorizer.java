package it.cnr.istc.stlab.framester.conceptnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.istc.stlab.lgu.commons.files.FileUtils;
import it.cnr.istc.stlab.lgu.commons.iterations.ProgressCounter;
import it.cnr.istc.stlab.lgu.commons.rdf.RDFUtils;
import it.cnr.istc.stlab.lgu.commons.tables.RowsIteratorCSV;

public class ConceptNetRefactorizer {

	private static Logger logger = LoggerFactory.getLogger(ConceptNetRefactorizer.class);

	private Configuration c;

	private String isSubGraphOf, wasDerivedFrom, relation, conceptNetWeight, conceptNetIdentifier, assertion;
	private int jsonErrors = 0;
	private OntModel bottomUpSchema = ModelFactory.createOntologyModel();
	private Model subgraphDatasetStructure = ModelFactory.createDefaultModel();
	private Map<String, Long> datasetCount = new HashMap<>();
	private Map<String, StreamWrapper> streamMap = new HashMap<>();

	private ConceptNetRefactorizer() {
		this(Configuration.getConfiguration());
	}

	private ConceptNetRefactorizer(Configuration c) {
		this.c = c;
		isSubGraphOf = c.getMetadataSchemaPrefix() + "isSubGraphOf";
		wasDerivedFrom = c.getMetadataSchemaPrefix() + "wasDerivedFrom";
		relation = c.getConceptNetSchemaPrefix() + "Relation";
		conceptNetWeight = c.getConceptNetSchemaPrefix() + "conceptNetWeight";
		conceptNetIdentifier = c.getConceptNetSchemaPrefix() + "conceptNetIdentifier";
		assertion = c.getConceptNetSchemaPrefix() + "Assertion";
	}

	private void registerStreamWrapper(StreamWrapper sw) {
		streamMap.put(sw.getGraphURI(), sw);
		streamMap.put(sw.getId(), sw);
	}

	private StreamWrapper getMainStream() {
		return streamMap.get(c.getGraph());
	}

	private StreamWrapper getExternalURLsStream() {
		return streamMap.get(c.getExternalURLsNamedGraph());
	}

	private void setMainStream(StreamWrapper stream) {
		streamMap.put(c.getGraph(), stream);
	}

	private void setExternalURLsStream(StreamWrapper stream) {
		streamMap.put(c.getExternalURLsNamedGraph(), stream);
	}

	private StreamWrapper createStreamWrapperByStreamID(String id) throws FileNotFoundException, IOException {
		String filepath = c.getOutFolder() + "/" + id.replace('/', '_') + ".nq.gz";
		new File(filepath).delete();
		GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(new File(filepath)));
		StreamRDF stream = StreamRDFWriter.getWriterStream(gzip, RDFFormat.NQ);
		StreamWrapper sw = new StreamWrapper(filepath, c.getResourcePrefix() + id, id, gzip, stream);
		return sw;
	}

	private StreamWrapper createMainStreamWrapper() throws FileNotFoundException, IOException {
		new File(c.getDumpRDF()).delete();
		GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(new File(c.getDumpRDF())));
		StreamRDF stream = StreamRDFWriter.getWriterStream(gzip, RDFFormat.NQ);
		StreamWrapper swMain = new StreamWrapper(c.getDumpRDF(), c.getGraph(), c.getGraph(), gzip, stream);
		return swMain;
	}

	private StreamWrapper createExternalURLsStreamWrapper() throws FileNotFoundException, IOException {
		new File(c.getExternalURLFilePath()).delete();
		GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(new File(c.getExternalURLFilePath())));
		StreamRDF stream = StreamRDFWriter.getWriterStream(gzip, RDFFormat.NQ);
		StreamWrapper swMain = new StreamWrapper(c.getExternalURLFilePath(), c.getExternalURLsNamedGraph(),
				c.getExternalURLsNamedGraph(), gzip, stream);
		return swMain;
	}

	public void refactorize() throws IOException {

		logger.info("Reading: {}", c.getConceptNetDumpFilePath());

		InputStream fileStreamLC = new FileInputStream(c.getConceptNetDumpFilePath());
		InputStream gzipStreamLC = new GZIPInputStream(fileStreamLC);
		Reader decoderLC = new InputStreamReader(gzipStreamLC);
		long lines = FileUtils.countNumberOfLines(decoderLC);
		logger.info("Number of lines {}", lines);

		int lineErrors = 0;

		InputStream fileStream = new FileInputStream(c.getConceptNetDumpFilePath());
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream);
		BufferedReader buffered = new BufferedReader(decoder);
		ProgressCounter pc = new ProgressCounter(lines);
		RowsIteratorCSV ricsv = new RowsIteratorCSV(buffered, '\t');

		// setting main stream
		setMainStream(createMainStreamWrapper());

		if (c.isExtractExternalURLs()) {
			setExternalURLsStream(createExternalURLsStreamWrapper());
		}

		// iterating over rows
		int lineNumber = 0;
		while (ricsv.hasNext()) {
			try {
				String[] row = (String[]) ricsv.next();
				refactorizeLine(lineNumber, row);
			} catch (ArrayIndexOutOfBoundsException e) {
				logger.error("Error with parsing line {}", lineNumber);
				lineErrors++;
			}
			pc.increase();
			lineNumber++;
		}

		// closing streams
		streamMap.values().forEach(sw -> {
			try {
				sw.getGzip().flush();
				sw.getStream().finish();
				sw.getGzip().finish();
				sw.getGzip().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		// writing bottom up schema
		RDFUtils.writeModelOnNQuadsFile(bottomUpSchema, c.getSchemaBottomUpURI(), c.getSchemaFilePath());

		// writing subgraphDatasetStructure
		RDFUtils.writeModelOnNQuadsFile(subgraphDatasetStructure, c.getSubGraphStructureURI(),
				c.getOutFolder() + "/graphstructure.nq");

		logger.info("JSONErrors {}", jsonErrors);
		logger.info("Line errors {}", lineErrors);

		datasetCount.forEach((dataset, count) -> {
			logger.info("{}\t{}", dataset, count);
		});

		logger.info("End");

	}

	private void refactorizeLine(int line, String[] strings) throws FileNotFoundException, IOException {

		String sng = c.getResourcePrefix() + strings[0].substring(1);
		String cn = c.getConceptNetPrefix() + strings[0].substring(1);
		String p = c.getResourcePrefix() + strings[1].substring(1);
		String s = c.getResourcePrefix() + strings[2].substring(1);
		String o = c.getResourcePrefix() + strings[3].substring(1);

		StreamWrapper datasetStream = getMainStream();

		try {
			JSONObject obj = new JSONObject(strings[4]);

			String dataset = obj.getString("dataset").substring(1);

			if (streamMap.containsKey(dataset)) {
				datasetStream = streamMap.get(dataset);
			} else {
				datasetStream = createStreamWrapperByStreamID(dataset);
				registerStreamWrapper(datasetStream);
			}

			// streaming assertion weight on dataset stream
			datasetStream.quad(new Quad(NodeFactory.createURI(datasetStream.getGraphURI()), new Triple(
					NodeFactory.createURI(sng), NodeFactory.createURI(conceptNetWeight),
					NodeFactory.createLiteral(LiteralLabelFactory.createTypedLiteral(obj.getDouble("weight"))))));

			Long count = datasetCount.get(dataset);
			if (count == null) {
				datasetCount.put(dataset, 1L);
			} else {
				datasetCount.put(dataset, count + 1);
			}

		} catch (org.json.JSONException e) {
			logger.error("Error with parsing JSON at line {}", line);
			jsonErrors++;
		}

		if (strings[1].equals("/r/ExternalURL")) {
			o = strings[3];
			getExternalURLsStream().quad(new Quad(NodeFactory.createURI(c.getExternalURLsNamedGraph()), new Triple(
					NodeFactory.createURI(s), NodeFactory.createURI(OWL.sameAs.getURI()), NodeFactory.createURI(o))));

		}

		// streaming assertion on dataset graph
		datasetStream.quad(new Quad(NodeFactory.createURI(sng),
				new Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))));

		// streaming assertion isSubgGraphOf dataset graph
		datasetStream.quad(
				new Quad(NodeFactory.createURI(datasetStream.getGraphURI()), new Triple(NodeFactory.createURI(sng),
						NodeFactory.createURI(isSubGraphOf), NodeFactory.createURI(datasetStream.getGraphURI()))));

		datasetStream.quad(new Quad(NodeFactory.createURI(datasetStream.getGraphURI()),
				new Triple(NodeFactory.createURI(sng), RDF.type.asNode(), NodeFactory.createURI(assertion))));

		// streaming assertion wasDerivedFrom
		datasetStream.quad(new Quad(NodeFactory.createURI(datasetStream.getGraphURI()), new Triple(
				NodeFactory.createURI(sng), NodeFactory.createURI(wasDerivedFrom), NodeFactory.createURI(cn))));

		// streaming assertion conceptNetIdentifier
		datasetStream.quad(new Quad(NodeFactory.createURI(datasetStream.getGraphURI()),
				new Triple(NodeFactory.createURI(sng), NodeFactory.createURI(conceptNetIdentifier),
						NodeFactory.createLiteral(LiteralLabelFactory.createTypedLiteral(strings[0])))));

		// streaming assertion isSubgGraphOf conceptnet
		if (!datasetStream.getGraphURI().equals(getMainStream().getGraphURI())) {
			subgraphDatasetStructure.add(subgraphDatasetStructure.createResource(datasetStream.getGraphURI()),
					subgraphDatasetStructure.createProperty(isSubGraphOf),
					subgraphDatasetStructure.createResource(getMainStream().getGraphURI()));
		}

		// streaming concept identifier of the subject
		getMainStream().quad(new Quad(NodeFactory.createURI(c.getGraph()),
				new Triple(NodeFactory.createURI(s), NodeFactory.createURI(conceptNetIdentifier),
						NodeFactory.createLiteral(LiteralLabelFactory.createTypedLiteral(strings[2])))));

		// streaming concept identifer of the object
		if (!strings[1].equals("/r/ExternalURL")) {
			LiteralLabelFactory.createTypedLiteral(strings[3]);
			getMainStream().quad(new Quad(NodeFactory.createURI(c.getGraph()),
					new Triple(NodeFactory.createURI(o), NodeFactory.createURI(conceptNetIdentifier),
							NodeFactory.createLiteral(LiteralLabelFactory.createTypedLiteral(strings[3])))));

		}

		// adding relation to bottom up schema
		bottomUpSchema.add(bottomUpSchema.createObjectProperty(p), RDF.type, bottomUpSchema.createResource(relation));

		// adding conceptnet identifier of relation
		bottomUpSchema.add(bottomUpSchema.createObjectProperty(p), bottomUpSchema.createProperty(conceptNetIdentifier),
				strings[1]);

	}

	public static void main(String[] args) throws IOException {
		new ConceptNetRefactorizer().refactorize();
//		Compression.printFirstLinesOfGZipFile(
//				"/Users/lgu/Dropbox/Lavoro/Projects/Framester/f/Framester_v3/endpoint_v3/conceptnet/5.7.0/conceptnet-assertion-5.7.0.nq.gz",
//				100);
	}
}
