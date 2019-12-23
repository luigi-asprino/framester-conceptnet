package it.cnr.istc.stlab.framester.conceptnet;

import java.util.zip.GZIPOutputStream;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

public class StreamWrapper implements StreamRDF {

	private String filePath;
	private String graphURI;
	private String id;
	private GZIPOutputStream gzip;
	private StreamRDF stream;

	public StreamWrapper(String filePath, String graphURI, String id, GZIPOutputStream gzip, StreamRDF stream) {
		super();
		this.filePath = filePath;
		this.graphURI = graphURI;
		this.id = id;
		this.gzip = gzip;
		this.stream = stream;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getGraphURI() {
		return graphURI;
	}

	public void setGraphURI(String graphURI) {
		this.graphURI = graphURI;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public GZIPOutputStream getGzip() {
		return gzip;
	}

	public void setGzip(GZIPOutputStream gzip) {
		this.gzip = gzip;
	}

	public StreamRDF getStream() {
		return stream;
	}

	public void setStream(StreamRDF stream) {
		this.stream = stream;
	}

	@Override
	public void start() {
		stream.start();
	}

	@Override
	public void triple(Triple triple) {
		stream.triple(triple);
	}

	@Override
	public void quad(Quad quad) {
		stream.quad(quad);
	}

	@Override
	public void base(String base) {
		stream.base(base);
	}

	@Override
	public void prefix(String prefix, String iri) {
		stream.prefix(prefix, iri);
	}

	@Override
	public void finish() {
		stream.finish();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((graphURI == null) ? 0 : graphURI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StreamWrapper other = (StreamWrapper) obj;
		if (graphURI == null) {
			if (other.graphURI != null)
				return false;
		} else if (!graphURI.equals(other.graphURI))
			return false;
		return true;
	}

}
