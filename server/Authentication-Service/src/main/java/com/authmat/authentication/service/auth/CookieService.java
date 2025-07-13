package com.authmat.authentication.service.auth;

import com.authmat.authentication.constant.Endpoints;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CookieService {
    // TO-DO proper CSRF protection

    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String ACCESS_TOKEN = "access_token";


    public void setTokenCookies(String refreshToken, String accessToken, HttpServletResponse response){
        addCookieToResponse(
                response, ACCESS_TOKEN, accessToken, Endpoints.Auth.TOKEN_REFRESH, 7 * 24 * 60 * 60
        );

        addCookieToResponse(
                response, REFRESH_TOKEN, refreshToken, Endpoints.Auth.TOKEN_REFRESH, 7 * 24 * 60 * 60
        );

    }


    public void clearRefreshTokenCookie(HttpServletResponse response){
        addCookieToResponse(
                response, "","", Endpoints.Auth.TOKEN_REFRESH, 0
        );
    }


    public String extractCookie(HttpServletRequest request, String cookie){
        return Arrays.stream(request.getCookies())
                .filter(c -> cookie.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse("");
    }


    private void addCookieToResponse(
            HttpServletResponse response, String cookieName, String cookieValue, String path, int maxAge
    ){
        Cookie cookie = new Cookie(cookieName, cookieValue);

        cookie.setHttpOnly(true);
        //cookie.setSecure(true); for https
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        response.addCookie(cookie);
    }

}
