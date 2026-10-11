package net.serverplugins.anticheat;

import java.util.Locale;

enum CheckType {
    SPEED,
    FLIGHT,
    REACH;

    String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
