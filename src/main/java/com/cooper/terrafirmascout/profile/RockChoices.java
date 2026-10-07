package com.cooper.terrafirmascout.profile;
import java.util.*;
/** The rocks a player can ask for under spawn. TFC registers each raw rock beside its slab, stairs and wall blocks, which are never the surface rock. */
public final class RockChoices {
    private static final String PREFIX="rock/raw/";
    private static final List<String> VARIANT_SUFFIXES=List.of("_slab","_stairs","_wall");
    private RockChoices() {}
    /**
     * @param blockPaths block ids without the namespace, for example {@code rock/raw/andesite}; other paths are ignored
     * @return rock names (for example {@code andesite}), sorted. A name is dropped only when it is another rock's name plus a variant
     *         suffix, so real rocks (including ones added by other mods) are kept.
     */
    public static List<String> spawnRocks(Collection<String> blockPaths) {
        var names=new TreeSet<String>();
        for(var path:blockPaths) if(path.startsWith(PREFIX)&&path.length()>PREFIX.length()) names.add(path.substring(PREFIX.length()));
        return names.stream().filter(name->VARIANT_SUFFIXES.stream().noneMatch(suffix->name.endsWith(suffix)&&names.contains(name.substring(0,name.length()-suffix.length())))).toList();
    }
}
