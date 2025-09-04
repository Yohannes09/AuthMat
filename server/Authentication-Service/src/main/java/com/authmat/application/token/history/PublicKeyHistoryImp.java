package com.authmat.application.token.history;

import com.authmat.model.publickey.PublicKeyMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Default in-memory implementation of the {@link PublicKeyHistory} interface.
 *
 * <p><strong>Note:</strong> This implementation does not persist key history and will
 * reset on application restart.
 */
//@Component
@Validated
@RequiredArgsConstructor
public class PublicKeyHistoryImp implements PublicKeyHistory{
    private final Integer maxKeysTraced;
    private final Deque<PublicKeyMetadata> keyHistory;


    /**
     * Adds a new {@link PublicKeyMetadata} to the key history queue.
     * <p>
     * If the queue has reached its configured maximum size, the oldest
     * key is automatically evicted to make room for the new one.
     * <p>
     * The most recent key is always placed at the end of the queue.
     *
     * @param publicKeyMetaData the public key metadata to store
     */
    @Override
    public void addKey(PublicKeyMetadata publicKeyMetaData) {
        if(keyHistory.size() >= maxKeysTraced){
            keyHistory.pollFirst();
        }

        keyHistory.addLast(publicKeyMetaData);
    }


    @Override
    public void addKeys(List<PublicKeyMetadata> publicKeyMetaData) {
        publicKeyMetaData.forEach(this::addKey);
    }


    /**
     * Retrieves a reverse-chronological list of all retained public key records.
     *
     * @return a sorted list of {@link PublicKeyMetadata} entries, newest first
     */
    @Override
    public List<PublicKeyMetadata> getKeyHistoryAscending() {
        return keyHistory.stream()
                .sorted(Comparator.comparing(PublicKeyMetadata::getCreatedAt).reversed())
                .toList();
    }

}
