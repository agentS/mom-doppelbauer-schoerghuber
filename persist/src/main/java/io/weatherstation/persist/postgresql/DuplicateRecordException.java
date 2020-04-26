package io.weatherstation.persist.postgresql;

public class DuplicateRecordException extends RuntimeException {
	public DuplicateRecordException() {
	}

	public DuplicateRecordException(String message) {
		super(message);
	}

	public DuplicateRecordException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateRecordException(Throwable cause) {
		super(cause);
	}
}
