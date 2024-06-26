/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.bootstrap.plugin;

import lombok.Getter;

/**
 * Represents an event generated by a plugin. It contains details about the event type, the source of the event,
 * an optional message, and an optional throwable if an error occurred.
 */
public class PluginEvent {

    // The source of the event
    private final Object owner;

    // The type of the event
    @Getter
    private final EventType type;

    // An optional message associated with the event
    @Getter
    private final String message;

    // An optional Throwable associated with the event, typically used for error events
    @Getter
    private final Throwable throwable;

    /**
     * Constructs a new PluginEvent without an associated Throwable.
     *
     * @param owner   the source of the event
     * @param type    the type of the event
     * @param message an optional message describing the event
     */
    public PluginEvent(Object owner, EventType type, String message) {
        this(owner, type, message, null);
    }

    /**
     * Constructs a new PluginEvent.
     *
     * @param owner     the source of the event
     * @param type      the type of the event
     * @param message   an optional message describing the event
     * @param throwable an optional Throwable associated with the event
     */
    public PluginEvent(Object owner, EventType type, String message, Throwable throwable) {
        this.owner = owner;
        this.type = type;
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * Retrieves the owner of this event with the expected type.
     *
     * @param <T> the expected type of the event owner
     * @return the owner of this event cast to the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getOwner() {
        return (T) owner;
    }

    /**
     * Enum representing the possible types of plugin events.
     */
    public enum EventType {
        /**
         * Indicates a successful operation related to the plugin
         */
        SUCCESS,
        /**
         * Indicates a failed operation related to the plugin
         */
        FAIL,
        /**
         * Indicates the plugin is being uninstalled
         */
        UNINSTALL
    }
}

