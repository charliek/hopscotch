package charliek.hopscotch.docproxy.config;

import org.hibernate.validator.constraints.NotBlank;

public class GithubRequirement {

	@NotBlank
	String type;

	@NotBlank
	String name;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
