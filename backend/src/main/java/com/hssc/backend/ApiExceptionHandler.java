package com.hssc.backend;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorResponse(Instant timestamp,int status,String error,String message,Map<String,String> fields) {}
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e){return error(e.getStatus(),e.getMessage(),Map.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e){
        Map<String,String> fields=new TreeMap<>();e.getBindingResult().getFieldErrors().forEach(f->fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST,"Request validation failed",fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> malformed(Exception e){return error(HttpStatus.BAD_REQUEST,"Request body or path parameter is invalid",Map.of());}
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException e){return error(HttpStatus.CONFLICT,"A record with one or more of these unique values already exists",Map.of());}
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> forbidden(Exception e){return error(HttpStatus.FORBIDDEN,"Access denied",Map.of());}
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> upload(MaxUploadSizeExceededException e){return error(HttpStatus.PAYLOAD_TOO_LARGE,"Uploaded file exceeds the 5 MB limit",Map.of());}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e){return error(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error",Map.of());}
    private ResponseEntity<ErrorResponse> error(HttpStatus status,String message,Map<String,String> fields){
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(),status.value(),status.getReasonPhrase(),message,fields));
    }
}
