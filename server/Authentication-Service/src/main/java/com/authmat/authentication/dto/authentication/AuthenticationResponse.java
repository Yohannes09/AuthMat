package com.authmat.authentication.dto.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response provided by the server containing the user's ID and username/email")
public record AuthenticationResponse(
        @Schema(description = "Short-lived token used to access protected resources. ")
        String accessToken,

        @Schema(description = "Long-lived token used to fetch a new access token. ")
        String refreshToken,

        @Schema(description = "ID of the authenticated user. ")
        Long userId,

        @Schema
        String usernameOrEmail
){
        public AuthenticationResponse trimmed(){
                return AuthenticationResponse.builder()
                        .userId(this.userId)
                        .usernameOrEmail(this.usernameOrEmail)
                        .build();
        }

}
