package com.authmat.application.authentication;

import com.authmat.application.authentication.dto.AuthenticationResponse;
import com.authmat.application.authentication.dto.LoginRequest;
import com.authmat.application.authentication.dto.RegistrationRequest;
import com.authmat.application.authentication.models.UserPrincipal;
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

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500"})
@RequestMapping("${endpoints.auth.base:/auth/v1}")
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
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest loginRequest){
        log.info("New login attempt: {}", loginRequest.usernameOrEmail());
        AuthenticationResponse response = authenticationService.login(loginRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Create a new user. ",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User successfully registered. ")})
    public ResponseEntity<String> register(
            @Valid @RequestBody RegistrationRequest registrationRequest){
        log.info("New registration request received. ");
        authenticationService.register(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registration success.");
    }


    @PostMapping("/refresh")
    @Operation(
            summary = "Issue new access and refresh tokens. Valid refresh token must come attached to request.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Token refresh successful. ",
                            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class)))})
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestHeader("Authorization") String authHeader){

        Optional<String> refreshToken = Optional.of(authHeader.substring(7))
                        .filter(token -> token.startsWith("bearer "));

        Optional<UserPrincipal> userPrincipal = userPrincipalExtractor.extract();

        if(refreshToken.isEmpty() || userPrincipal.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthenticationResponse response = authenticationService.refresh(userPrincipal.get());
        return ResponseEntity.ok(response);
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
