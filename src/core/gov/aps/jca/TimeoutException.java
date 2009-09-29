package gov.aps.jca;

public class TimeoutException extends Exception {

  public TimeoutException() {
  }

  public TimeoutException(String message) {
    super(message);
  }

  public TimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  public TimeoutException(Throwable cause) {
    super(cause);
  }
}