package com.authmat.authentication.component.privileged;

import com.authmat.authentication.dto.privileged.KeyRotationRequest;
import com.payme.token.management.secured.SigningKeyManager;
import com.payme.token.model.PublicKeyRecordJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SigningKeyHandler {
    private final SigningKeyManager<PublicKeyRecordJpa> signingKeyManager;

    public void rotateSigningKey(KeyRotationRequest keyRotationRequest){
        // In development. 33 is a placeholder for the id of the person making the request.
        log.info("{} manual key rotation. Reason: {}", 33, keyRotationRequest.reason());
        signingKeyManager.manualKeyRotation();
    }

}
