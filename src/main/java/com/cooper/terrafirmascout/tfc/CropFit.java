package com.cooper.terrafirmascout.tfc;
import java.util.*;
import net.dries007.tfc.util.climate.ClimateRange;
/** Whether the climate at spawn suits TFC's crops, read from the loaded climate ranges (so addon crops count too). */
public final class CropFit {
    public record Fit(int suitable,int total,List<String> names) {
        public int needed() { return Math.min(10,(total+1)/2); }
        public boolean enough() { return total>0&&suitable>=needed(); }
        public String describe() {
            if(total==0) return "No crop climate data is loaded";
            String list=names.size()>6?String.join(", ",names.subList(0,6))+", ...":String.join(", ",names);
            return suitable+" of "+total+" crops fit the yearly temperature at spawn (at least "+needed()+" wanted)"+(names.isEmpty()?"":": "+list);
        }
    }
    /** TFC 3.2 farmland moisture comes only from nearby water, so only the yearly average temperature is compared with each crop's range. */
    public static Fit crops(float temperature) {
        var names=new ArrayList<String>(); int total=0;
        for(var entry:ClimateRange.MANAGER.getValues()) {
            var range=entry.get(); var path=range.getId().getPath(); if(!path.startsWith("crop/")) continue; total++;
            if(temperature>=range.getMinTemperature(true)&&temperature<=range.getMaxTemperature(true)) names.add(path.substring(5).replace('_',' '));
        }
        Collections.sort(names); return new Fit(names.size(),total,names);
    }
}
