package checkers.error;


import checkers.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static checkers.error.CheckersErrorCode.UNKNOWN_ERROR;


@RestControllerAdvice
public class ErrorHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);
    
    
    @ExceptionHandler(CheckersException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResponse handleCheckersException(CheckersException e) {
        LOGGER.debug("Caught a CheckersException", e);
        
        CheckersErrorCode errorCode = e.getErrorCode();
        
        return new ErrorResponse(
                e.getErrorCode().name(),
                e.getReason()
        );
    }
    
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResponse handleUnknownException(Exception e) {
        LOGGER.debug("Caught an unknown error", e);
        
        return new ErrorResponse(
                UNKNOWN_ERROR.name(),
                null
        );
    }
}
