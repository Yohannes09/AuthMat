package com.authmat.application.authentication.component;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.token.service.TokenService;
import com.authmat.application.users.User;
import com.authmat.application.users.UserNotFoundException;
import com.authmat.application.users.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    /**
     * @param request
     * @param response
     * @param authentication
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {


        OAuth2User oAuth2User;
        if(authentication.getPrincipal() instanceof OAuth2User auth2User) {
            oAuth2User = auth2User;
        }else {
            throw new RuntimeException();
        }

        User user = userRepository.findByUsernameOrEmail(oAuth2User.getName()).orElseThrow(() -> new UserNotFoundException(""));
        Set<String> permissions = user.getRoles().stream().flatMap(role -> role.getPermissions().stream().map(Permission::getName)).collect(Collectors.toSet());
        String accessToken = tokenService.generateAccessToken(user.getId().toString(), permissions);
    }
}
