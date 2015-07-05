package charliek.hopscotch.docproxy.dto;

import com.google.common.base.Preconditions;
import io.netty.handler.codec.http.HttpRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public class AppConfig {

	@NotNull
	@Valid
	private List<S3Host> hosts;

	public List<S3Host> getHosts() {
		return hosts;
	}

	public Optional<S3Host> getConfiguredHost(String host) {
		Preconditions.checkNotNull(host, "Host header is required");
		return hosts
			.stream()
			.filter(s3Host -> host.equals(s3Host.getHost()))
			.findFirst();
	}
}
