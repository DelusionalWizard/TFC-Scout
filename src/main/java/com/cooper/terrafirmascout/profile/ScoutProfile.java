package com.cooper.terrafirmascout.profile;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
public record ScoutProfile(String name,int minScore,int radius,double temperatureMin,double temperatureIdealMin,
    double temperatureIdealMax,double temperatureMax,double rainfallMin,double rainfallIdealMin,
    double rainfallIdealMax,double rainfallMax,double minimumLand,int landRadius,int terrainRadius,
    int campRadius,int minimumCopperUnits,int minimumCopperPieces,boolean kaolinSameLandmass,Map<Criterion,Integer> distances,int minimumRoughness,WorldSpecification specification,Set<Criterion> skipped,Set<Criterion> extra) {
    /** Without skipped requirements: the 20-argument form. */
    public ScoutProfile(String name,int minScore,int radius,double temperatureMin,double temperatureIdealMin,double temperatureIdealMax,double temperatureMax,
        double rainfallMin,double rainfallIdealMin,double rainfallIdealMax,double rainfallMax,double minimumLand,int landRadius,int terrainRadius,int campRadius,
        int minimumCopperUnits,int minimumCopperPieces,boolean kaolinSameLandmass,Map<Criterion,Integer> distances,int minimumRoughness,WorldSpecification specification) {
        this(name,minScore,radius,temperatureMin,temperatureIdealMin,temperatureIdealMax,temperatureMax,rainfallMin,rainfallIdealMin,rainfallIdealMax,rainfallMax,
            minimumLand,landRadius,terrainRadius,campRadius,minimumCopperUnits,minimumCopperPieces,kaolinSameLandmass,distances,minimumRoughness,specification,Set.of(),Set.of());
    }
    public ScoutProfile(String name,int minScore,int radius,double temperatureMin,double temperatureIdealMin,double temperatureIdealMax,double temperatureMax,
        double rainfallMin,double rainfallIdealMin,double rainfallIdealMax,double rainfallMax,double minimumLand,int landRadius,int terrainRadius,int campRadius,
        int minimumCopperUnits,int minimumCopperPieces,boolean kaolinSameLandmass,Map<Criterion,Integer> distances,int minimumRoughness) {
        this(name,minScore,radius,temperatureMin,temperatureIdealMin,temperatureIdealMax,temperatureMax,rainfallMin,rainfallIdealMin,rainfallIdealMax,rainfallMax,
            minimumLand,landRadius,terrainRadius,campRadius,minimumCopperUnits,minimumCopperPieces,kaolinSameLandmass,distances,minimumRoughness,WorldSpecification.none());
    }
    public ScoutProfile withScoreAndRadius(int score,int searchRadius) {
        return new ScoutProfile(name,score,searchRadius,temperatureMin,temperatureIdealMin,temperatureIdealMax,temperatureMax,rainfallMin,rainfallIdealMin,rainfallIdealMax,rainfallMax,
            minimumLand,landRadius,terrainRadius,campRadius,minimumCopperUnits,minimumCopperPieces,kaolinSameLandmass,distances,minimumRoughness,specification,skipped,extra);
    }
    /** The same profile also requiring these resources (used for resources a particular pack makes essential). */
    public ScoutProfile withExtra(Set<Criterion> more) {
        var all=new HashSet<>(extra); all.addAll(more);
        return new ScoutProfile(name,minScore,radius,temperatureMin,temperatureIdealMin,temperatureIdealMax,temperatureMax,rainfallMin,rainfallIdealMin,rainfallIdealMax,rainfallMax,
            minimumLand,landRadius,terrainRadius,campRadius,minimumCopperUnits,minimumCopperPieces,kaolinSameLandmass,distances,minimumRoughness,specification,skipped,all);
    }
    /** The same profile with these requirements switched off. */
    public ScoutProfile withSkipped(Set<Criterion> more) {
        var all=new HashSet<>(skipped); all.addAll(more);
        return new ScoutProfile(name,minScore,radius,temperatureMin,temperatureIdealMin,temperatureIdealMax,temperatureMax,rainfallMin,rainfallIdealMin,rainfallIdealMax,rainfallMax,
            minimumLand,landRadius,terrainRadius,campRadius,minimumCopperUnits,minimumCopperPieces,kaolinSameLandmass,distances,minimumRoughness,specification,all,extra);
    }
    public ScoutProfile {
        distances=Map.copyOf(distances); specification=Objects.requireNonNull(specification); skipped=skipped==null?Set.of():Set.copyOf(skipped); extra=extra==null?Set.of():Set.copyOf(extra);
        if(minScore<(!specification.enabled()&&SeedQuality.fromName(name)==SeedQuality.GOD?90:0)||minScore>100||radius<300||radius>12000||minimumLand<(specification.enabled()?0:SeedQuality.fromName(name)==SeedQuality.GOD?0.7:0.25)||minimumLand>1||minimumRoughness<0
            ||landRadius<(specification.enabled()?16:1000)||terrainRadius<16||campRadius<16||minimumCopperUnits<(specification.enabled()?1:100)||minimumCopperPieces<(specification.enabled()?1:10)
            ||!(temperatureMin<=temperatureIdealMin&&temperatureIdealMin<=temperatureIdealMax&&temperatureIdealMax<=temperatureMax)
            ||!(rainfallMin<=rainfallIdealMin&&rainfallIdealMin<=rainfallIdealMax&&rainfallIdealMax<=rainfallMax))
            throw new IllegalArgumentException("These settings do not fit together. Check the limits and ranges.");
        if(specification.enabled()&&((specification.requirements().contains(Criterion.TERRAIN)&&terrainRadius>radius)
            ||(specification.requirements().contains(Criterion.OPEN_GROUND)&&campRadius>radius)
            ||((specification.requirements().contains(Criterion.LAND_RATIO)||specification.requirements().contains(Criterion.MAINLAND))&&landRadius>radius)))
            throw new IllegalArgumentException("Your building, camp or land check is larger than the search area.");
        for(var c:List.of(Criterion.FRESHWATER,Criterion.FOREST,Criterion.CLAY,Criterion.STARTER_COPPER,
            Criterion.COPPER_VEIN,Criterion.FLUX,Criterion.TIN,Criterion.GRAPHITE,Criterion.KAOLIN))
            if(!distances.containsKey(c)||distances.get(c)<1||(!specification.enabled()||specification.requirements().contains(c))&&distances.get(c)>radius) throw new IllegalArgumentException("Check the distance for "+c);
        // Iron and coal came later; saved settings from older versions have no distance for them, which means the whole search area.
        for(var c:List.of(Criterion.IRON,Criterion.COAL))
            if(distances.containsKey(c)&&(distances.get(c)<1||(!specification.enabled()||specification.requirements().contains(c))&&distances.get(c)>radius)) throw new IllegalArgumentException("Check the distance for "+c);
    }
    public int distance(Criterion c) { return distances.getOrDefault(c,radius); }
    public SeedQuality quality() { return SeedQuality.fromName(name); }
    public Set<Criterion> requiredCriteria() {
        Set<Criterion> base=specification.enabled()?specification.requiredCriteria():quality().required();
        if(skipped.isEmpty()&&extra.isEmpty()) return base;
        var kept=new HashSet<>(base); kept.addAll(extra); kept.removeAll(skipped); return Collections.unmodifiableSet(kept);
    }
    public boolean requires(Criterion c) { return requiredCriteria().contains(c); }
    public int analysisRadius() {
        int value=Math.max(128,distances.entrySet().stream().filter(e->requires(e.getKey())).mapToInt(Map.Entry::getValue).max().orElse(128));
        if(requires(Criterion.TERRAIN))value=Math.max(value,terrainRadius);
        if(requires(Criterion.OPEN_GROUND))value=Math.max(value,campRadius);
        if(requires(Criterion.LAND_RATIO)||requires(Criterion.MAINLAND))value=Math.max(value,landRadius);
        if(specification.enabled()&&!specification.nearbyBiomes().isEmpty())value=Math.max(value,specification.biomeRadius());
        return value;
    }

    public String description() {
        if(specification.enabled())return "Your wishlist: "+specification.requirements().size()+" things to check; nearby biomes: "+(specification.allBiomes()?"ALL":"ANY")+" within "+specification.biomeRadius()+" blocks.";
        return switch(quality()){case GOD->"Everything close by on a mainland start, with graphite and kaolin in reach.";case GOOD->"The same list as Dream Start, with a little more travel allowed.";
            case AVERAGE->"Solid basics: copper, tin, flux, clay, trees and water. Late extras can wait.";case HARD->"A cold, rugged start. The essentials are there, but you may have to travel.";case SUPER_HARD->"Freezing weather and very rough ground, with the essentials within reach.";};
    }
    /** Name to show a player: saved seeds from older versions carry the old preset names. */
    public String displayName() {
        if(specification.enabled()) return "Your wishlist";
        return name.equalsIgnoreCase("custom")?"Custom":quality().label;
    }
    public static ScoutProfile beginner() { return preset(SeedQuality.GOD); }
    public static ScoutProfile balanced() { return preset(SeedQuality.GOOD); }
    public static ScoutProfile preset(SeedQuality quality) {
        var d=new EnumMap<Criterion,Integer>(Criterion.class);
        var keys=List.of(Criterion.FRESHWATER,Criterion.FOREST,Criterion.CLAY,Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,
            Criterion.FLUX,Criterion.TIN,Criterion.GRAPHITE,Criterion.KAOLIN,Criterion.IRON,Criterion.COAL);
        int[] values=switch(quality) {
            case GOD->new int[]{160,350,300,750,1000,1200,2000,4000,4000,3000,2500};
            case GOOD->new int[]{250,500,500,1000,1500,2000,3000,5000,6000,4000,3500};
            case AVERAGE->new int[]{400,800,800,1500,2500,3500,4500,7000,9000,6000,5000};
            case HARD->new int[]{800,1500,2000,3000,3500,6000,7000,10000,12000,9000,8000};
            case SUPER_HARD->new int[]{1500,2500,3500,5000,6000,8000,10000,12000,12000,12000,12000};
        };
        for(int i=0;i<keys.size();i++) d.put(keys.get(i),values[i]);
        return switch(quality) {
            case GOD->new ScoutProfile("Dream Start",95,4000,7,10,15,17,220,250,350,400,0.7,1000,250,160,100,10,true,d,0);
            case GOOD->new ScoutProfile("Easy Start",90,6000,4,8,18,22,175,225,350,450,0.65,1000,350,250,100,10,true,d,0);
            case AVERAGE->new ScoutProfile("Fair Start",85,9000,0,5,20,26,125,175,400,475,0.50,1000,500,350,100,10,true,d,0);
            case HARD->new ScoutProfile("Rugged Start",80,12000,-8,-2,4,6,75,125,220,275,0.35,1000,1000,600,100,10,true,d,0);
            case SUPER_HARD->new ScoutProfile("Wilderness Start",80,12000,-20,-10,-3,0,50,75,200,300,0.25,1000,1500,1000,100,10,true,d,12);
        };
    }
}
