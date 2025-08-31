package com.authmat.application.authentication.component;

import com.authmat.application.authentication.token.service.TokenService;
import com.authmat.application.users.UserRepository;
import com.authmat.application.users.model.UserPrincipal;
import com.authmat.application.users.util.UserMapper;
import com.authmat.exception.InternalTypeException;
import com.authmat.tool.exception.UserNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final UserMapper userMapper;


    /**
     * @param request
     * @param response
     * @param authentication
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User;
        if(authentication.getPrincipal() instanceof OAuth2User auth2User) {
            oAuth2User = auth2User;
        }else {
            throw new InternalTypeException("Type mismatch, Expected type: " + OAuth2User.class.getName());
        }

        UserPrincipal user = userRepository
                .findByUsernameOrEmail(oAuth2User.getName())
                .map(userMapper::entityToPrincipal)
                .orElseThrow(() -> new UserNotFoundException(""));

        Set<String> authorities = user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String accessToken = tokenService.generateAccessToken(user.getId().toString(), authorities);
        String refreshToken = tokenService.generateRefreshToken(user.getId().toString());
    }

}
