package com.cleanroommc.cleanhocon.internal;

import com.cleanroommc.cleanhocon.ConfigMergeable;
import com.cleanroommc.cleanhocon.ConfigValue;

interface MergeableValue extends ConfigMergeable {
    // converts a Config to its root object and a ConfigValue to itself
    ConfigValue toFallbackValue();
}
