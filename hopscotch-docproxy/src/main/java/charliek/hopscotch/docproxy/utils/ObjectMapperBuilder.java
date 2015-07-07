package charliek.hopscotch.docproxy.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperBuilder {

	private ObjectMapperBuilder() {
		// only static
	}

	public static ObjectMapper build() {
		return new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}
}
