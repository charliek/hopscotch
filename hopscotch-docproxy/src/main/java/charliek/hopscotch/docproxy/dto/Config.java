package charliek.hopscotch.docproxy.dto;

import com.beust.jcommander.Parameter;

public class Config {

	@Parameter(names = "-bucket", required = false, description = "The s3 bucket to proxy")
	private String bucket = "charliek-repodocs";

	@Parameter(names = "-port", required = false, description = "The port to run on")
	private int port = 9999;

	public int getPort() {
		return port;
	}

	public String getBucket() {
		return bucket;
	}
}
