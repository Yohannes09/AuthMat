package com.authmat.application.authentication.response;

import com.authmat.application.token.model.AccessToken;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response provided by the server containing the user's ID and username/email")
public record AuthenticationResponse(
        @Schema(description = "Short-lived token used to access protected resources. ")
        AccessToken accessToken,

        @Schema(description = "Long-lived token used to fetch a new access token. ")
        String refreshToken
){
}
