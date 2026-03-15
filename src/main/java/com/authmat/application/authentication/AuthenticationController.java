package com.authmat.application.authentication;

import com.authmat.application.authentication.request.LoginRequest;
import com.authmat.application.authentication.request.RegistrationRequest;
import com.authmat.application.authentication.response.AuthenticationResponse;
import com.authmat.application.authentication.response.RegistrationResponse;
import com.authmat.application.authentication.service.AuthenticationService;
import com.authmat.application.util.UserPrincipalExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("#{environment['AUTH_BASE_ENDPOINT'] ?: '/v1/auth'}")
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserPrincipalExtractor userPrincipalExtractor;


    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user credentials and issue access and refresh tokens. ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully authenticated. ",
                            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - bad credentials"),
                    @ApiResponse(responseCode = "404", description = "Couldn't find user. ")})
    public ResponseEntity<CompletableFuture<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest){
        log.info("New login attempt: {}", loginRequest.usernameOrEmail());
        CompletableFuture<AuthenticationResponse> response = authenticationService.login(loginRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "User registration endpoint",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User successfully registered. ")
            }
    )
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest registrationRequest){
        log.debug("New registration request received. ");
        RegistrationResponse response = authenticationService.register(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/refresh")
    @Operation(
            summary = "Issue new access and refresh tokens. Valid refresh token must come attached to request.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Token refresh successful. ",
                            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
                    )
            }
    )
    public ResponseEntity<CompletableFuture<AuthenticationResponse>> refresh(
            @RequestHeader("Authorization") String authHeader){

        String refreshToken = authHeader.substring(7);

        return ResponseEntity.ok(authenticationService.refresh(refreshToken));
    }


    @PostMapping("/logout")
    @Operation(
            summary = "Terminate the user's authenticated session by black-listing the refresh token. ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refresh successful. ")})
    public ResponseEntity<Void> logout(String refreshToken){

        authenticationService.logout(refreshToken);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
