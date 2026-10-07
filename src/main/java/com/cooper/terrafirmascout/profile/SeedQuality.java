package com.cooper.terrafirmascout.profile;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
public enum SeedQuality {
    SUPER_HARD("Wilderness Start","super_hard"), HARD("Rugged Start","hard"), AVERAGE("Fair Start","average"), GOOD("Easy Start","good"), GOD("Dream Start","god");
    /** Names used before 0.2.5. Saved seeds and reports written by older versions still carry them. */
    private static final Map<String,SeedQuality> LEGACY=Map.of("god",GOD,"good",GOOD,"average",AVERAGE,"hard",HARD,"super hard",SUPER_HARD,"balanced",GOOD,"balanced god seed",GOOD);
    public final String label,id;
    SeedQuality(String label,String id) { this.label=label;this.id=id; }
    public boolean challenging() { return this==HARD||this==SUPER_HARD; }
    public Set<Criterion> required() {
        var set=EnumSet.of(Criterion.SPAWN,Criterion.CLIMATE,Criterion.FOREST,Criterion.CLAY,
            Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,Criterion.TERRAIN,Criterion.OPEN_GROUND);
        if(!challenging()) set.addAll(EnumSet.of(Criterion.MAINLAND,Criterion.LAND_RATIO,Criterion.FLUX,Criterion.TIN));
        if(this==GOOD||this==GOD) set.addAll(EnumSet.of(Criterion.GRAPHITE,Criterion.KAOLIN,Criterion.CONNECTIVITY));
        if(challenging()) set.add(Criterion.CHALLENGE);
        return Collections.unmodifiableSet(set);
    }
    public static SeedQuality fromName(String name) {
        for(var q:values()) if(q.label.equalsIgnoreCase(name)||q.id.equalsIgnoreCase(name)) return q;
        var legacy=LEGACY.get(name.toLowerCase(java.util.Locale.ROOT));
        return legacy!=null?legacy:GOD;
    }
}
