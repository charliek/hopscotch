package charliek.hopscotch.docproxy.github.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AuthCookie {
	private final String userName;
	private final List<String> orgNames;
	private final String token;

	@JsonCreator
	public AuthCookie(
		@JsonProperty("userName") String userName,
		@JsonProperty("orgNames") List<String> orgNames,
		@JsonProperty("token") String token) {
		this.userName = userName;
		this.orgNames = orgNames;
		this.token = token;
	}

	public String getUserName() {
		return userName;
	}

	public List<String> getOrgNames() {
		return orgNames;
	}

	public String getToken() {
		return token;
	}
}
