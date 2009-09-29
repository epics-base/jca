package gov.aps.jca;

/**
 * CA status exception.
 */
public class CAStatusException extends CAException {

 private CAStatus status;
	
  public CAStatusException( CAStatus status ) {
    super();
    this.status = status;
  }
  public CAStatusException( CAStatus status, String msg ) {
    super( msg );
    this.status = status;
  }
  public CAStatusException( CAStatus status, String msg, Throwable cause ) {
    super( msg, cause );
    this.status = status;
  }
  public CAStatusException( CAStatus status, Throwable cause ) {
    super( cause );
    this.status = status;
  }
  public CAStatus getStatus() {
	  return status;
  }
}