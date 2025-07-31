package com.authmat.application.authentication.component;

import com.payme.internal.security.constant.TokenRecipient;
import com.payme.internal.security.constant.TokenType;
import com.payme.internal.security.model.TokenSubject;
import com.payme.token.management.TokenConfigurationProperties;
import com.payme.token.management.secured.TokenFactory;
import com.payme.token.model.PublicKeyRecordJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TokenProvider {
    private final TokenFactory<PublicKeyRecordJpa> tokenFactory;
    private final TokenConfigurationProperties tokenConfigurationProperties;

    public String generateAccessToken(TokenSubject tokenSubject, TokenRecipient tokenRecipient){
        return tokenFactory.generateNewToken(
                tokenSubject,
                TokenType.ACCESS.name(),
                tokenRecipient.name(),
                tokenConfigurationProperties.getTemplates().get("short-lived").getValidityMinutes()
        );
    }

    public String generateRefreshToken(TokenSubject tokenSubject, TokenRecipient tokenRecipient){
        return tokenFactory.generateNewToken(
                tokenSubject,
                TokenType.REFRESH.name(),
                tokenRecipient.name(),
                tokenConfigurationProperties.getTemplates().get("long-lived").getValidityMinutes()
        );
    }

}
