package charliek.hopscotch.docproxy.config;

import com.google.common.base.Preconditions;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;

public class AppConfig {

	@NotNull
	@Valid
	private Map<String, S3Host> hosts;

	public AppConfig init() throws Exception {
		hosts.forEach((k, v) -> v.init());
		return this;
	}

	public Optional<S3Host> getConfiguredHost(String host) {
		Preconditions.checkNotNull(host, "Host header is required");
		return Optional.ofNullable(hosts.get(host));
	}

	public Map<String, S3Host> getHosts() {
		return hosts;
	}

	public void setHosts(Map<String, S3Host> hosts) {
		this.hosts = hosts;
	}
}
