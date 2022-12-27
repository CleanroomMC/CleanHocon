/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package com.cleanroommc.cleanhocon.internal;

import com.cleanroommc.cleanhocon.ConfigIncluder;
import com.cleanroommc.cleanhocon.ConfigIncluderClasspath;
import com.cleanroommc.cleanhocon.ConfigIncluderFile;
import com.cleanroommc.cleanhocon.ConfigIncluderURL;

interface FullIncluder extends ConfigIncluder, ConfigIncluderFile, ConfigIncluderURL,
            ConfigIncluderClasspath {

}
