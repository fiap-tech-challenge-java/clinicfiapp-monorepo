package br.com.fiap.clinic.history.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class HistoryAccessDeniedException extends RuntimeException {
    public HistoryAccessDeniedException(String message) {
        super(message);
    }
}
