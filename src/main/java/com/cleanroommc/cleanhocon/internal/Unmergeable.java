/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package com.cleanroommc.cleanhocon.internal;

import java.util.Collection;

/**
 * Interface that tags a ConfigValue that is not mergeable until after
 * substitutions are resolved. Basically these are special ConfigValue that
 * never appear in a resolved tree, like {@link ConfigSubstitution} and
 * {@link ConfigDelayedMerge}.
 */
interface Unmergeable {
    Collection<? extends AbstractConfigValue> unmergedValues();
}