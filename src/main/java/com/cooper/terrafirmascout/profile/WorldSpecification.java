package com.cooper.terrafirmascout.profile;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
/** Player choices constrain seed selection; they never change world generation. */
public record WorldSpecification(boolean enabled,Set<Criterion> requirements,Set<String> nearbyBiomes,
    boolean allBiomes,int biomeRadius,Set<String> spawnBiomes,Set<String> spawnRocks,Set<String> forestTypes,
    int densityMin,int densityMax,int elevationMin,int elevationMax,int terrainSize,int campSize,
    int maximumSlope,double grassFraction) {
    public WorldSpecification {
        requirements=requirements.stream().filter(c->c!=Criterion.FRESHWATER).collect(java.util.stream.Collectors.toUnmodifiableSet()); nearbyBiomes=Set.copyOf(nearbyBiomes); spawnBiomes=Set.copyOf(spawnBiomes);
        spawnRocks=Set.copyOf(spawnRocks); forestTypes=Set.copyOf(forestTypes);
        if(biomeRadius<16||biomeRadius>12000||densityMin<0||densityMax>4||densityMin>densityMax
            ||elevationMin>elevationMax||elevationMin< -64||elevationMax>320||terrainSize<4||terrainSize>16
            ||campSize<4||campSize>16||maximumSlope<0||maximumSlope>32||!Double.isFinite(grassFraction)||grassFraction<0||grassFraction>1)
            throw new IllegalArgumentException("Check the limits in your wishlist");
        if(requirements.contains(Criterion.CONNECTIVITY)&&!requirements.contains(Criterion.KAOLIN))
            throw new IllegalArgumentException("To ask for a land route to kaolin, tick Kaolin too.");
    }
    public static WorldSpecification none() {
        return new WorldSpecification(false,Set.of(),Set.of(),false,1000,Set.of(),Set.of(),Set.of(),0,4,-64,320,16,8,3,0.5);
    }
    public Set<Criterion> requiredCriteria() {
        var set=EnumSet.of(Criterion.SPAWN,Criterion.SPECIFICATION); set.addAll(requirements); return Collections.unmodifiableSet(set);
    }
    public boolean biomesMatch(Set<String> found) { return nearbyBiomes.isEmpty()||(allBiomes?found.containsAll(nearbyBiomes):nearbyBiomes.stream().anyMatch(found::contains)); }
    public boolean spawnMatches(String biome,String rock,String forest,int density,int height) {
        return (spawnBiomes.isEmpty()||spawnBiomes.contains(biome))&&(spawnRocks.isEmpty()||spawnRocks.contains(rock))
            &&(forestTypes.isEmpty()||forestTypes.contains(forest))&&density>=densityMin&&density<=densityMax&&height>=elevationMin&&height<=elevationMax;
    }
}
