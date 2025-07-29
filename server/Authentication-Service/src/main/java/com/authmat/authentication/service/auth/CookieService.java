package com.authmat.authentication.service.auth;

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
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    public void setTokenCookies(
            String refreshToken, String accessToken, HttpServletResponse response
    ){

        addCookieToResponse(
                response, ACCESS_TOKEN, accessToken, COOKIE_MAX_AGE
        );

        addCookieToResponse(
                response, REFRESH_TOKEN, refreshToken, COOKIE_MAX_AGE
        );

    }

      // WHAT IS THIS?????
//    public void clearRefreshTokenCookie(HttpServletResponse response){
//        addCookieToResponse(
//                response, "","", Endpoints.Auth.TOKEN_REFRESH, 0
//        );
//    }


    public String extractCookie(HttpServletRequest request, String cookie){
        return Arrays.stream(request.getCookies())
                .filter(c -> cookie.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse("");
    }


    private void addCookieToResponse(
            HttpServletResponse response, String cookieName, String cookieValue, int maxAge//String path,
    ){
        Cookie cookie = new Cookie(cookieName, cookieValue);

        cookie.setHttpOnly(true);
        //cookie.setSecure(true); for https
        //cookie.setPath(path);
        cookie.setMaxAge(maxAge);

        response.addCookie(cookie);
    }

}
