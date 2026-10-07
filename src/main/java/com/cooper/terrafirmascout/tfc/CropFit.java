package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.dries007.tfc.util.climate.ClimateRange;
/** Whether the climate at spawn suits TFC's crops, read from the loaded climate ranges (so addon crops count too). */
public final class CropFit {
    public static final double FARMLAND_MIN=30,FARMLAND_MAX=80;
    public record Fit(int suitable,int total,List<String> names) {
        public int needed() { return Math.min(10,(total+1)/2); }
        public boolean enough() { return total>0&&suitable>=needed(); }
        public String describe() {
            if(total==0) return "No crop climate data is loaded";
            String list=names.size()>6?String.join(", ",names.subList(0,6))+", ...":String.join(", ",names);
            return suitable+" of "+total+" crops fit the yearly climate at spawn (at least "+needed()+" wanted)"+(names.isEmpty()?"":": "+list);
        }
    }
    /** A crop fits when the yearly average temperature is inside its range and the rain alone does not make the soil wetter than it allows. */
    public static Fit crops(float temperature,float driestRainHydration) {
        var names=new ArrayList<String>(); int total=0;
        for(var entry:ClimateRange.MANAGER.getElements().entrySet()) {
            var path=entry.getKey().getPath(); if(!path.startsWith("crop/")) continue; total++;
            var range=entry.getValue();
            if(temperature>=range.getMinTemperature(true)&&temperature<=range.getMaxTemperature(true)&&driestRainHydration<=range.getMaxHydration(true)) names.add(path.substring(5).replace('_',' '));
        }
        Collections.sort(names); return new Fit(names.size(),total,names);
    }
}
