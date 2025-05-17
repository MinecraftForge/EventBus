/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

final class LogMarkers {
    private LogMarkers() {}

    static final Marker EVENTBUS = MarkerManager.getMarker("EVENTBUS");
}
