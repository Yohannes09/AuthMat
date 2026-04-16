package com.authmat.application.security.ingress;

import com.authmat.application.security.properties.PublicPathsProperties;
import com.authmat.application.security.properties.ServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class MtlsEnforcementFilter extends OncePerRequestFilter {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final ServiceProperties properties;
    private final PublicPathsProperties publicPathsProperties;

    public MtlsEnforcementFilter(ServiceProperties properties, PublicPathsProperties publicPathsProperties) {
        this.properties = properties;
        this.publicPathsProperties = publicPathsProperties;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return publicPathsProperties.publicPaths().containsValue(path);
    }


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        X509Certificate[] certs =
                (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if(certs==null || certs.length==0) {
            AUDIT.warn("No X509 certificate found in request");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client certificate required");
            return;
        }

        X509Certificate cert = certs[0];

        try {
            cert.checkValidity();
        }catch (CertificateException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client certificate required");
            return;
        }

        String spiffeId = extractSpiffeIdFromSan(cert);

        if(spiffeId==null || !properties.serviceNames().contains(spiffeId)) {
            log.warn("Invalid spiffe id {}", spiffeId);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Untrusted spiffeId");
            return;
        }

        request.setAttribute("mtls.client.spiffe",  spiffeId);
        filterChain.doFilter(request,response);
    }

    private String extractSpiffeIdFromSan(X509Certificate cert) {
        try {
            final int sanUriCode = 6;

            Collection<List<?>> sans =  cert.getSubjectAlternativeNames();
            if(sans == null) return null;

            return sans.stream()
                    .filter(san -> Integer.valueOf(sanUriCode).equals(san.get(0)))
                    .map(san -> (String) san.get(1))
                    .filter(uri -> uri.startsWith("spiffe://"))
                    .findFirst()
                    .orElse(null);

        } catch (CertificateParsingException e) {
            return null;
        }
    }

}
