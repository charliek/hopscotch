package charliek.hopscotch.docproxy.dto;

import org.hibernate.validator.constraints.NotBlank;

public class S3Host implements GithubCredentials {

	@NotBlank
	private String host;

	@NotBlank
	private String clientId;

	@NotBlank
	private String clientSecret;

	@NotBlank
	private String bucket;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
}
