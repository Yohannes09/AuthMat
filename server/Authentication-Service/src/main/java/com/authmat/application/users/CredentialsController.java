package com.authmat.application.users;

import com.authmat.application.users.service.UserCredentialsService;
import com.authmat.application.users.dto.EmailUpdateRequest;
import com.authmat.application.users.dto.PasswordUpdateRequest;
import com.authmat.application.users.dto.UsernameUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${endpoints.credentials.base:/api/credentials}")
@RequiredArgsConstructor
@Slf4j(topic = "CREDENTIALS_CONTROLLER")
public class CredentialsController {
    private final UserCredentialsService credentialsService;


    @PostMapping("${endpoints.credentials.username:/username}")
    @Operation(
            description = "Update username endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated username successfully."),
                    @ApiResponse(responseCode = "400", description = "New username provided failed validation. ")
            }
    )
    public ResponseEntity<Void> updateUsername(@Valid @RequestBody UsernameUpdateRequest updateRequest){
        credentialsService.updateUsername(updateRequest);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("${endpoints.credentials.email:/email}")
    @Operation(
            description = "Update email endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated email successfully."),
                    @ApiResponse(responseCode = "400", description = "New email provided failed validation. ")
            }
    )
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody EmailUpdateRequest updateRequest){
        credentialsService.updateEmail(updateRequest);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("${endpoints.credentials.password:/password}")
    @Operation(
            description = "Update password endpoint.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User updated password successfully."),
                    @ApiResponse(responseCode = "400", description = "New password provided failed validation. ")
            }
    )
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest updateRequest){
        credentialsService.updatePassword(updateRequest);
        return ResponseEntity.noContent().build();
    }


}
