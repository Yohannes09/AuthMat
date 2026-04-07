package com.authmat.application.exception;

import com.authmat.application.authentication.exception.DuplicateCredentialException;
import com.authmat.application.authorization.exception.PermissionNotFoundException;
import com.authmat.application.authorization.exception.RoleNotFoundException;
import com.authmat.application.user.exception.CredentialUpdateException;
import com.authmat.tool.exception.UserNotFoundException;
import io.lettuce.core.RedisConnectionException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnkownServiceIdentityException.class)
    public ResponseEntity<ErrorResponse> handleUnkownServiceIdentityException(
            UnkownServiceIdentityException ex, HttpServletRequest request) {

        log.warn("Service Token Request from an unknown identity on {}:{}",
                request.getRequestURI(), ex.getMessage());

        return generateErrorResponse(
                "Forbidden",
                HttpStatus.FORBIDDEN,
                request);
    }


    @ExceptionHandler({RoleNotFoundException.class, PermissionNotFoundException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleInternalExceptions(
            Exception exception, HttpServletRequest request){

        return generateErrorResponse(
                "Oops, something went wrong. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest servletRequest){
        log.error("Authentication failed: {}", exception.getMessage());

        String message = switch (exception){
            case DisabledException disabledException-> "Account is disabled.";
            case LockedException lockedException -> "Account is locked.";
            case BadCredentialsException badCredentialsException -> "Invalid username or password.";
            case AccountExpiredException accountExpiredException -> "Account has expired.";
            case CredentialsExpiredException credentialsExpiredException -> "Your password has expired.";
            default -> "Authentication failed.";
        };

        return generateErrorResponse(
                message,
                HttpStatus.UNAUTHORIZED,
                servletRequest);
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(
            UserNotFoundException ex, HttpServletRequest request){
        log.warn("Resource not found on {}: ", request.getRequestURI(), ex);

        return generateErrorResponse(
                "User not found.",
                HttpStatus.NOT_FOUND,
                request);
    }


    @ExceptionHandler({DuplicateCredentialException.class, CredentialUpdateException.class})
    public ResponseEntity<ErrorResponse> handleDuplicateCredentialException(
            RuntimeException ex, HttpServletRequest request){
        log.warn("Bad request on {}: ", request.getRequestURI(), ex);

        String message = switch (ex){
            case DuplicateCredentialException e-> "Credential already exists.";
            case CredentialUpdateException e-> "Credential does not meet requirements.";
            default -> "Bad Request";
        };

        return generateErrorResponse(
                message,
                HttpStatus.BAD_REQUEST,
                request);
    }


    @ExceptionHandler(RedisConnectionException.class)
    public ResponseEntity<ErrorResponse> handleRedisConnectionException(
            DuplicateCredentialException exception, HttpServletRequest request){

        log.error("Redis connection error on {}: ", request.getRequestURI(), exception);

        return generateErrorResponse(
                "Service temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE,
                request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request){

        Map<String,String> validationErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                        ));

        return generateErrorResponse(
                "Validation error.",
                HttpStatus.BAD_REQUEST,
                request,
                validationErrors);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex, HttpServletRequest request){
        log.error("Unhandled exception on {}:", request.getRequestURI(), ex);

        return generateErrorResponse(
                "Something went wrong.  Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    protected ResponseEntity<ErrorResponse> generateErrorResponse(
            String message,
            HttpStatus status,
            HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                message,
                status.value(),
                request.getRequestURI(),
                null);

        return ResponseEntity.status(status).body(body);
    }

    protected ResponseEntity<ErrorResponse> generateErrorResponse(
            String message,
            HttpStatus status,
            HttpServletRequest request,
            Map<String, String> validationErrors) {

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                message,
                status.value(),
                request.getRequestURI(),
                validationErrors);

        return ResponseEntity.status(status).body(body);
    }

}
