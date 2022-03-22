package checkers.error;


import lombok.Getter;


@Getter
public class CheckersException extends Exception {
    private final CheckersErrorCode errorCode;
    private final String reason;
    
    
    public CheckersException(CheckersErrorCode errorCode, String reason, Throwable cause) {
        super(errorCode.toString() + ": " + reason, cause);
        this.errorCode = errorCode;
        this.reason = reason;
    }
    
    
    public CheckersException(CheckersErrorCode errorCode, String reason) {
        super(errorCode.toString() + ": " + reason);
        this.errorCode = errorCode;
        this.reason = reason;
    }
    
    
    public CheckersException(CheckersErrorCode errorCode, Throwable cause) {
        super(errorCode.toString(), cause);
        this.errorCode = errorCode;
        this.reason = null;
    }
    
    
    public CheckersException(CheckersErrorCode errorCode) {
        super(errorCode.toString());
        this.errorCode = errorCode;
        reason = null;
    }
}
