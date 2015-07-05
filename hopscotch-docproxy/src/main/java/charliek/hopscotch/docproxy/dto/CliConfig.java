package charliek.hopscotch.docproxy.dto;

import com.beust.jcommander.Parameter;

public class CliConfig {

	@Parameter(names = "-config", required = true, description = "The location of the yaml configuration file")
	private String configLocation;

	@Parameter(names = "-port", required = false, description = "The port to run on")
	private int port = 9999;

	public int getPort() {
		return port;
	}

	public String getConfigLocation() {
		return configLocation;
	}
}
