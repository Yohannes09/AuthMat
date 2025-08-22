package com.authmat.application.token.history;

import com.authmat.application.token.model.PublicKeyMetaData;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
@Component
@Validated
@RequiredArgsConstructor
public class PublicKeyHistoryImp implements PublicKeyHistory{
    private final Integer maxKeysTraced;
    private final Deque<PublicKeyMetaData> keyHistory;


    /**
     * Adds a new {@link PublicKeyMetaData} to the key history queue.
     * <p>
     * If the queue has reached its configured maximum size, the oldest
     * key is automatically evicted to make room for the new one.
     * <p>
     * The most recent key is always placed at the end of the queue.
     *
     * @param publicKeyMetaData the public key metadata to store
     */
    @Override
    public void addKey(@NotNull PublicKeyMetaData publicKeyMetaData) {
        if(keyHistory.size() >= maxKeysTraced){
            keyHistory.pollFirst();
        }

        keyHistory.addLast(publicKeyMetaData);
    }


    @Override
    public void addKeys(@NotNull @NotEmpty List<@NotEmpty PublicKeyMetaData> publicKeyMetaData) {
        publicKeyMetaData.forEach(this::addKey);
    }


    /**
     * Retrieves a reverse-chronological list of all retained public key records.
     *
     * @return a sorted list of {@link PublicKeyMetaData} entries, newest first
     */
    @Override
    public List<PublicKeyMetaData> getKeyHistoryAscending() {
        return keyHistory.stream()
                .sorted(Comparator.comparing(PublicKeyMetaData::getCreatedAt).reversed())
                .toList();
    }

}
