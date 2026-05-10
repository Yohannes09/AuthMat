package com.authmat.application.util;

import com.github.f4b6a3.uuid.UuidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.util.UUID;


public class UuidV7Generator implements IdentifierGenerator {
    @Override
    public UUID generate(SharedSessionContractImplementor session, Object obj) {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
