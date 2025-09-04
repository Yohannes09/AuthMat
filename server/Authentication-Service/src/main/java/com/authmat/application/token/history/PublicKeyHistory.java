package com.authmat.application.token.history;

import com.authmat.model.publickey.PublicKeyMetadata;

import java.util.List;

/**
 * Contract for maintaining a history of public keys used for token verification.
 * <p>
 * Implementations of this interface allow tracking of previously active public keys,
 * enabling support for key rotation without invalidating tokens signed with older keys.
 * <p>
 * The key history typically includes the currently active public key as well as a
 * limited number of past keys. This is crucial in distributed systems where tokens
 * may remain valid during a grace period after a key rotation.
 *
 * <p><strong>Implementations:</strong> The backing store may be in-memory, persistent,
 * or distributed depending on the application's needs and security posture.
 *
 * @see com.authmat.model.publickey.PublicKeyMetadata
 */
public interface PublicKeyHistory {
    void addKey(PublicKeyMetadata publicKeyMetaData);
    void addKeys(List<PublicKeyMetadata> publicKeyMetaData);
    List<PublicKeyMetadata> getKeyHistoryAscending();
}
