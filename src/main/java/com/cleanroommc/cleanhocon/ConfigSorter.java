package com.cleanroommc.cleanhocon;

import java.util.Comparator;
import java.util.Map;

public interface ConfigSorter {

    Map getMapBacking();

    Comparator<String> getRendererSorter();

}
