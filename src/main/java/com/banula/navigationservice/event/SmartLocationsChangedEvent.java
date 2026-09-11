package com.banula.navigationservice.event;

import org.springframework.context.ApplicationEvent;

/**
 * Raised whenever a smart location is written in this service — an OCPI push, a
 * location pull from a CPO, a dashboard edit, a bulk import or the nightly active
 * state refresh. Consumers mirror the change outwards; see
 * {@link com.banula.navigationservice.listener.SmartLocationsChangedListener}.
 */
public class SmartLocationsChangedEvent extends ApplicationEvent {

    public SmartLocationsChangedEvent(Object source) {
        super(source);
    }
}
