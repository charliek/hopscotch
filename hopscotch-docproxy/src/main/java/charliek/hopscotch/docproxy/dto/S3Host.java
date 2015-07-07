package charliek.hopscotch.docproxy.dto;

import charliek.hopscotch.docproxy.services.EncryptionService;
import com.google.common.collect.Lists;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class S3Host implements GithubCredentials {
	@NotBlank
	private String clientId;

	@NotBlank
	private String clientSecret;

	@NotBlank
	private String bucket;

	@NotBlank
	private String passphrase;

	@NotBlank
	private String salt;

	@Valid
	@NotNull
	private List<GithubRequirement> required = Lists.newArrayList();

	private EncryptionService encryptionService;

	public void init() {
		encryptionService = new EncryptionService(passphrase, salt);
	}

	public EncryptionService getEncryptionService() {
		return encryptionService;
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

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public List<GithubRequirement> getRequired() {
		return required;
	}

	public void setRequired(List<GithubRequirement> required) {
		this.required = required;
	}
}
