package io.promoengine.exception;

import io.promoengine.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuleCompilationException.class)
    public ResponseEntity<ErrorResponse> handleRuleCompilation(RuleCompilationException ex) {
        log.warn("DRL compilation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.builder().errorCode(14).message(ex.getMessage()).build());
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        log.warn("Tenant not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder().errorCode(50).message(ex.getMessage()).build());
    }

    @ExceptionHandler(EnrichmentException.class)
    public ResponseEntity<ErrorResponse> handleEnrichment(EnrichmentException ex) {
        log.error("Enrichment error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorCode(99).message("Enrichment service error").build());
    }

    @ExceptionHandler(EngineInitException.class)
    public ResponseEntity<ErrorResponse> handleEngineInit(EngineInitException ex) {
        log.error("Engine init error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorCode(41).message("Engine initialization error").build());
    }

    @ExceptionHandler(RuleSetException.class)
    public ResponseEntity<ErrorResponse> handleRuleSet(RuleSetException ex) {
        log.warn("Rule set error: {}", ex.getMessage());
        int httpStatus = ex.getErrorCode() == 14 ? 422 : 400;
        return ResponseEntity.status(httpStatus)
                .body(ErrorResponse.builder().errorCode(ex.getErrorCode()).message(ex.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder().errorCode(0).message(message).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().errorCode(99).message("Internal server error").build());
    }
}
