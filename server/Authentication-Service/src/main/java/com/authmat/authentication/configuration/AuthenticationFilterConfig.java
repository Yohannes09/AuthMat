package com.authmat.authentication.configuration;

import com.payme.internal.security.token.TokenResolver;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilterConfig extends OncePerRequestFilter {
    private static final String AUTH_HEADER = "Authorization";

    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("Request method: {}\turi:{}", request.getMethod(), request.getRequestURI());
        filterChain.doFilter(request, response);
        // This allows pre-flight requests in-case client doesn't attach accessToken
//        if(request.getMethod().equals("OPTIONS")){
//            response.setStatus(HttpServletResponse.SC_OK);
//            return;
//        }
//
//        final String authHeader = request.getHeader(AUTH_HEADER);
//        final String token;
//        final String tokenSubject;
//
//        if(authHeader == null || !authHeader.startsWith("Bearer ")){
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        token = authHeader.substring(7);
//        tokenSubject = TokenResolver.resolveClaim(
//                token,
//
//                Claims::getSubject
//        );tokenSubject != null &&
//        boolean isNotAuthenticated = SecurityContextHolder.getContext().getAuthentication() == null;
//
//        if(isNotAuthenticated){
//            UserDetails fetchedUser = userDetailsService.loadUserByUsername(tokenSubject);
//
//            if(jwtService.isTokenValid(token, fetchedUser)){
//
//                UsernamePasswordAuthenticationToken authToken =
//                        new UsernamePasswordAuthenticationToken(
//                            fetchedUser,
//                            null,
//                            fetchedUser.getAuthorities()
//                        );
//
//                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
//        }

    }

}
