package com.authmat.application.security.ingress;

import com.authmat.application.authorization.dto.PermissionDto;
import com.authmat.application.security.exception.NoVerifierForKidException;
import com.authmat.application.security.principal.SecurityContextPrincipal;
import com.authmat.application.security.principal.ServiceContextPrincipal;
import com.authmat.application.security.principal.UserContextPrincipal;
import com.authmat.application.security.properties.PublicPathsProperties;
import com.authmat.application.security.registry.VerifierRegistry;
import com.authmat.application.token.constant.TokenType;
import com.authmat.application.token.TokenService;
import com.authmat.application.user.UserDto;
import com.authmat.application.user.exception.UserNotFoundException;
import com.authmat.application.user.repository.UserCache;
import com.authmat.validation.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JwtUtil RESOLVER = new JwtUtil();

    private final PublicPathsProperties publicPathsProperties;
    private final UserCache userCache;
    private final VerifierRegistry verifierRegistry;
    private final MeterRegistry meterRegistry;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(
            PublicPathsProperties publicPathsProperties,
            UserCache userCache,
            VerifierRegistry verifierRegistry,
            MeterRegistry meterRegistry,
            TokenService tokenService) {
        this.publicPathsProperties = publicPathsProperties;
        this.userCache = userCache;
        this.verifierRegistry = verifierRegistry;
        this.meterRegistry = meterRegistry;
        this.tokenService = tokenService;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicPathsProperties.publicPaths().containsValue(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String rawToken = extractBearerToken(request);

        if(rawToken == null){
            rejectUnauthorized(response, "No JWT provided");
            return;
        }

        try {
            String kid = extractKid(rawToken);
            if(kid == null) throw new MalformedJwtException("Token is missing kid");

            Key verifyingKey = verifierRegistry.get(kid);
            if(verifyingKey == null) throw new NoVerifierForKidException("A key could not be found for kid: " + kid);

            Claims claims = RESOLVER.resolveClaims(rawToken, verifyingKey);
            if(RESOLVER.isTokenExpired(rawToken, verifyingKey)){
                rejectUnauthorized(response, "Token expired");
            }
            if (claims.getId() == null || tokenService.isBlacklisted(claims.getId())) {
                rejectUnauthorized(response, "Token has been blacklisted");
            }
            buildPrincipalPopulateContext(claims);
            filterChain.doFilter(request, response);
        } catch (MalformedJwtException | NoVerifierForKidException e) {
            rejectUnauthorized(response, e.getMessage());
        } catch (JwtException e) {
            rejectUnauthorized(response, "Invalid signature");
        }finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private String extractKid(String rawToken){
        int firstDot = rawToken.indexOf('.');
        if(firstDot == -1) throw new MalformedJwtException("Invalid token");

        try {
            byte[] decodedHeader = Base64.getUrlDecoder().decode(rawToken.substring(0, firstDot));
            JsonNode node = MAPPER.readTree(decodedHeader);

            return (node.has("kid")) ?
                    node.get("kid").asText() : null;
        } catch (IOException e) {
            throw new MalformedJwtException("Invalid token");
        }
    }

    public void buildPrincipalPopulateContext(Claims claims){
        String typeClaim = (String) claims.getOrDefault("type", "");
        TokenType tokenType = Arrays.stream(TokenType.values())
                .filter(type -> type.name().equalsIgnoreCase(typeClaim.trim()))
                .findFirst()
                .orElseThrow(()-> new MalformedJwtException("Type field missing or token principal is unknown"));

        String subject =  claims.getSubject();

        SecurityContextPrincipal principal = switch (tokenType){
            case SERVICE -> new ServiceContextPrincipal(subject, List.of("ROLE_SERVICE"));
            case ACCESS -> handleUserContextPrincipal(subject);
            default -> throw new MalformedJwtException("Unknown token principal");
        };

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UserContextPrincipal handleUserContextPrincipal(String identifier){
        UserDto user = userCache
                .findByExternalId(identifier)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Set<String> authorities = user.roles().stream()
                .flatMap(role -> Stream.concat(
                        Stream.of("ROLE_" + role.name()),
                        role.permissions().stream().map(PermissionDto::name)
                ))
                .collect(Collectors.toSet());
        return new UserContextPrincipal(identifier, authorities);
    }

    // todo: probably not using some of this correctly, tbd
    private void rejectUnauthorized(HttpServletResponse response, String reason) throws IOException {
        meterRegistry.counter("jwt.unauthorized", "reason", reason).increment();
        response.setContentType("application/json");
        String responseMessage = """
                {
                    "error" " : "Unauthorized"
                }
                """;

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, responseMessage);
    }

}
