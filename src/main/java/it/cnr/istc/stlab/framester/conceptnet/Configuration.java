package it.cnr.istc.stlab.framester.conceptnet;

import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Configuration {

	private String conceptNetDumpFilePath, resourcePrefix, graph, dumpRDF, conceptNetPrefix, schemaFilePath,schemaBottomUpURI;
	private static Configuration instance;
	private static final String CONF_FILE = "config.properties";

	private Configuration(String configFile) {
		Configurations configs = new Configurations();
		try {
			org.apache.commons.configuration2.Configuration config = configs.properties(configFile);
			conceptNetDumpFilePath = config.getString("conceptNetDumpFilePath");
			resourcePrefix = config.getString("resourcePrefix");
			graph = config.getString("graph");
			dumpRDF = config.getString("dumpRDF");
			conceptNetPrefix = config.getString("conceptNetPrefix");
			schemaFilePath = config.getString("schemaFilePath");
			schemaBottomUpURI=config.getString("schemaBottomUpURI");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static Configuration getConfiguration(String configFile) {
		if (instance == null) {
			instance = new Configuration(configFile);
		}
		return instance;
	}

	public static Configuration getConfiguration() {
		return getConfiguration(CONF_FILE);
	}

	public String getConceptNetDumpFilePath() {
		return conceptNetDumpFilePath;
	}

	public String getResourcePrefix() {
		return resourcePrefix;
	}

	public String getGraph() {
		return graph;
	}

	public String getDumpRDF() {
		return dumpRDF;
	}

	public String getConceptNetPrefix() {
		return conceptNetPrefix;
	}

	public String getSchemaFilePath() {
		return schemaFilePath;
	}

	public String getSchemaBottomUpURI() {
		return schemaBottomUpURI;
	}
	
	

}
