package com.authmat.application.users;

import com.authmat.application.users.dto.EmailUpdateRequest;
import com.authmat.application.users.dto.PasswordUpdateRequest;
import com.authmat.application.users.dto.UsernameUpdateRequest;
import com.authmat.application.util.UserPrincipalExtractor;
import com.authmat.tool.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("${endpoints.credentials.base:/v1/users}")
@RequiredArgsConstructor
@Slf4j(topic = "CREDENTIALS_CONTROLLER")
public class UserServiceController {
    private final UserService userService;
    private final UserPrincipalExtractor principalExtractor;


    @PostMapping("/username")
    @Operation(
            description = "Update username endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated username successfully."),
                    @ApiResponse(responseCode = "400", description = "New username provided failed validation. ")})
    public ResponseEntity<?> updateUsername(@Valid @RequestBody UsernameUpdateRequest updateRequest, HttpServletRequest request){
        return principalExtractor.extract()
                .map(principal -> {
                    boolean isUpdated = userService.updateUsername(updateRequest, principal.getId());
                    return isUpdated ?
                            ResponseEntity.noContent().build() :
                            ResponseEntity.status(HttpStatus.CONFLICT).build();
                }).orElse(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED.value())));
    }


    @PostMapping("/email")
    @Operation(
            description = "Update email endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated email successfully."),
                    @ApiResponse(responseCode = "400", description = "New email provided failed validation. ")})
    public ResponseEntity<?> updateEmail(
            @Valid @RequestBody EmailUpdateRequest updateRequest, HttpServletRequest request){

        return principalExtractor.extract()
                .map(principal -> {
                    boolean isUpdated = userService.updateEmail(updateRequest, principal.getId());
                    return isUpdated ?
                            ResponseEntity.noContent().build() :
                            ResponseEntity.status(HttpStatus.CONFLICT).build();
                }).orElse(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED.value())));
    }

    @PostMapping("/password")
    @Operation(
            description = "Update password endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated password successfully."),
                    @ApiResponse(responseCode = "400", description = "New password provided failed validation. ")})
    public ResponseEntity<?> updatePassword(@Valid @RequestBody PasswordUpdateRequest updateRequest, HttpServletRequest request){
        return principalExtractor.extract()
                .map(principal -> {
                    boolean isUpdated = userService.updatePassword(updateRequest, principal.getId());
                    return isUpdated ?
                            ResponseEntity.noContent().build() :
                            ResponseEntity.status(HttpStatus.CONFLICT).build();
                }).orElse(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED.value())));
    }


    private ErrorResponse buildErrorResponse(HttpServletRequest request, int statusCode){
        return ErrorResponse.builder()
                .message("Must be signed in.")
                .requestPath(request.getRequestURI())
                .errorTimestamp(LocalDateTime.now())
                .statusCode(statusCode)
                .build();
    }

}
