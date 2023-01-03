package com.cleanroommc.cleanhocon;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public interface ConfigSorter {

    Map getMapBacking();

    Set getSetBacking();

    Comparator<String> getRendererSorter();

}
