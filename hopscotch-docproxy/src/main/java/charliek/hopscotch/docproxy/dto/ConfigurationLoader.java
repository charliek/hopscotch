package charliek.hopscotch.docproxy.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.util.Set;

public class ConfigurationLoader {

	private static Logger LOG = LoggerFactory.getLogger(ConfigurationLoader.class);

	public static AppConfig loadAppConfig(String fileName) {
		return load(fileName, AppConfig.class, true);
	}

	static private <T> T load(String fileName, Class<T> klass, boolean validate) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		File configFile = new File(fileName);
		T config = null;
		try {
			config = mapper.readValue(configFile, klass);
			if (validate) {
				Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
				Set<ConstraintViolation<T>> violations = validator.validate(config);
				if (violations.size() > 0) {
					LOG.error("Invalid configuration found. Exiting.");
					for (ConstraintViolation v : violations) {
						LOG.error("{} - {}", v.getPropertyPath(), v.getMessage());
					}
					System.exit(1);
				}
			}
		} catch (Exception e) {
			LOG.error("Error loading configuration file {}", fileName, e);
			System.exit(1);
		}
		return config;
	}

}