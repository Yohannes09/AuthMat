package com.authmat.application.security.oauth2;

import com.authmat.application.authentication.service.AuthenticationService;
import com.authmat.application.security.exception.InvalidPrincipalException;
import com.authmat.application.user.repository.UserCache;
import com.authmat.application.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletionException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserCache userCache;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        AsyncContext async = request.startAsync();
        async.start(() -> {
            if(!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
                throw new InvalidPrincipalException("Wrong type of principal. Received: " + authentication.getPrincipal().getClass());
            }

            authenticationService
                    .generateAuthenticationResponse(oAuth2User.getName())
                    .thenAccept(authResponse -> {
                        try {
                            HttpServletResponse asyncResponse = (HttpServletResponse) async.getResponse();

                            asyncResponse.setContentType("application/json");
                            asyncResponse.setStatus(HttpServletResponse.SC_OK);

                            new ObjectMapper().writeValue(asyncResponse.getOutputStream(), authResponse);
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    })
                    .exceptionally(e -> {
                        HttpServletResponse asyncResponse =
                        (HttpServletResponse) async.getResponse();

                        asyncResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        async.complete();
                        return null;
                    })
                    .thenRun(async::complete);

            request.getSession().invalidate();
        });

    }

}
