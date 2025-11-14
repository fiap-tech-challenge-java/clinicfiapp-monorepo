package br.com.fiap.clinic.history.exception;

public class HistoryAccessDeniedException extends RuntimeException {
    public HistoryAccessDeniedException(String message) {
        super(message);
    }
}
