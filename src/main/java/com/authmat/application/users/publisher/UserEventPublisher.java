package com.authmat.application.users.publisher;

import com.authmat.events.NewUserEvent;

public interface UserEventPublisher {
    void userCreatedEvent(NewUserEvent userEvent);

    // TODO create event type for user deletion
    void userDeletedEvent();
}
