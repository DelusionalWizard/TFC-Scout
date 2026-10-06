package com.cooper.terrafirmascout.profile;
import java.util.*;
import com.cooper.terrafirmascout.score.Criterion;
public enum SeedQuality {
    SUPER_HARD("Super Hard","super_hard"), HARD("Hard","hard"), AVERAGE("Average","average"), GOOD("Good","good"), GOD("God","god");
    public final String label,id;
    SeedQuality(String label,String id) { this.label=label;this.id=id; }
    public boolean challenging() { return this==HARD||this==SUPER_HARD; }
    public Set<Criterion> required() {
        var set=EnumSet.of(Criterion.SPAWN,Criterion.CLIMATE,Criterion.FRESHWATER,Criterion.FOREST,Criterion.CLAY,
            Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,Criterion.TERRAIN,Criterion.OPEN_GROUND);
        if(!challenging()) set.addAll(EnumSet.of(Criterion.MAINLAND,Criterion.LAND_RATIO,Criterion.FLUX,Criterion.TIN));
        if(this==GOOD||this==GOD) set.addAll(EnumSet.of(Criterion.GRAPHITE,Criterion.KAOLIN,Criterion.CONNECTIVITY));
        if(challenging()) set.add(Criterion.CHALLENGE);
        return Collections.unmodifiableSet(set);
    }
    public static SeedQuality fromName(String name) {
        for(var q:values()) if(q.label.equalsIgnoreCase(name)||q.id.equalsIgnoreCase(name)) return q;
        return name.equalsIgnoreCase("balanced")||name.equals("Balanced God Seed")?GOOD:GOD;
    }
}
