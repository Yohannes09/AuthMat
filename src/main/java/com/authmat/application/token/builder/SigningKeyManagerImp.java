package com.authmat.application.token.builder;

import com.authmat.model.publickey.PublicKeyMetadata;
import com.authmat.model.publickey.PublicKeyMetadataImp;
import com.authmat.application.token.exception.KeyInitializationException;
import com.authmat.application.token.history.PublicKeyHistory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.util.Base64;
import java.util.List;


/**
 * Responsibilities:
 * <ul>
 *     <li>Generates and rotates the key pair used for signing and validating tokens.</li>
 *     <li>Provides access to the currently active signing key and metadata.</li>
 * </ul>
 * <p>
 * The manager is designed to be thread-safe and supports pluggable key formats via {@link KeyFactory}.
 */

@Slf4j
@RequiredArgsConstructor
public class SigningKeyManagerImp implements SigningKeyManager{
    private static final int MINIMUM_KEY_SIZE_BITS = 2048;

    private final PublicKeyHistory publicKeyHistory;
    private final String keyAlgorithm;
    private final String jwtAlgorithm;
    private final int keySize;

    private volatile ActiveKeyPair activeKeyPair;


    /**
     * @return an immutable {@link List} of base64-encoded public keys.
     * @throws KeyInitializationException if no signing key has been initialized.
     */
    public List<PublicKeyMetadata> getPublicKeyHistory(){
        validateKeyIsInitialized();
        return List.copyOf(publicKeyHistory.getKeyHistoryAscending());
    }


    /**
     * @return the active {@link PrivateKey} used for signing.
     * @throws KeyInitializationException if the signing key has not been initialized.
     */
    public PrivateKey getActiveSigningKey(){
        validateKeyIsInitialized();
        return activeKeyPair.privateKey();
    }


    /**
     * @return the current active public key record of type {@code T}.
     * @throws KeyInitializationException if the public key has not been initialized.
     */
    @Override
    public PublicKeyMetadata getCurrentKeyMetaData(){
        validateKeyIsInitialized();
        return activeKeyPair.publicKeyMetaData();
    }


    public synchronized void rotateSigningKey(){
        try {
            this.activeKeyPair = null;

            KeyPair keyPair = generateKeyPair(
                    keySize, keyAlgorithm);

            String encodedPublicKey = Base64
                    .getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            PublicKeyMetadata keyMetaData = new PublicKeyMetadataImp(
                    encodedPublicKey, keyAlgorithm, jwtAlgorithm);

            this.activeKeyPair = new ActiveKeyPair(
                    keyMetaData, keyPair.getPrivate());

            publicKeyHistory.addKey(keyMetaData);

        } catch (Exception e) {
            log.error("Failed to rotate signing key. {}", e.getMessage());
            throw new KeyInitializationException("Failed to rotate signing key. " + e.getMessage());
        }

    }


    private KeyPair generateKeyPair(@NotNull Integer keySizeBits, @NotEmpty String algorithmType) throws NoSuchAlgorithmException {
        if(keySizeBits < MINIMUM_KEY_SIZE_BITS){
            throw new KeyInitializationException("Key size must be at least 2048 bits for security reasons. ");
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithmType);
        keyPairGenerator.initialize(keySizeBits);

        return keyPairGenerator.generateKeyPair();
    }


    private void validateKeyIsInitialized(){
        if(activeKeyPair == null || publicKeyHistory.getKeyHistoryAscending().isEmpty()){
            log.error("ERROR no active Signing Key initialized. ");
            throw new KeyInitializationException("Failed to initialize public key.");
        }
    }


    /**
    * Holds the active signing key pair, including public key metadata and the private key.
    */
    private record ActiveKeyPair(
            PublicKeyMetadata publicKeyMetaData, PrivateKey privateKey
    ) {

    }

}

