package charliek.hopscotch.docproxy.exceptions;

public class HopscotchException extends RuntimeException {
	public HopscotchException() {
	}

	public HopscotchException(String message) {
		super(message);
	}

	public HopscotchException(String message, Throwable cause) {
		super(message, cause);
	}

	public HopscotchException(Throwable cause) {
		super(cause);
	}

	public HopscotchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
