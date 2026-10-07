package com.cooper.terrafirmascout.config;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
/** Remembered choices from the search screens. A missing or unreadable file just gives the defaults. */
public final class ScoutPrefs {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public String preset="god",speed="normal";
    public Map<String,Integer> minScore=new LinkedHashMap<>(),radius=new LinkedHashMap<>();
    public int stopAfterMatches,stopAfterMinutes;
    public boolean matchSound=true;
    public static ScoutPrefs load(Path file) {
        try {
            if(Files.isRegularFile(file)) {
                var loaded=JSON.fromJson(Files.readString(file),ScoutPrefs.class);
                if(loaded!=null) { loaded.clean(); return loaded; }
            }
        } catch(Exception ignored) { /* damaged file: start again from defaults */ }
        return new ScoutPrefs();
    }
    public static ScoutPrefs load() { return load(file()); }
    public void save() { save(file()); }
    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            var temporary=Files.createTempFile(file.getParent(),"prefs-",".tmp");
            try { Files.writeString(temporary,JSON.toJson(this)); Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); } finally { Files.deleteIfExists(temporary); }
        } catch(Exception e) { com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.warn("Could not save TerraFirmaScout settings",e); }
    }
    /** Scan workers for a speed setting: Low is gentle on the game, High uses more of the computer. */
    public static int workers(String speed,int configured,int cores) {
        return switch(speed==null?"normal":speed) { case "low"->1; case "high"->Math.max(configured,Math.max(1,Math.min(8,cores-1))); default->configured; };
    }
    private static Path file() { return com.cooper.terrafirmascout.search.ResultHistory.root().resolve("prefs.json"); }
    /** Keeps hand-edited or damaged values inside what the screens accept. */
    private void clean() {
        if(preset==null||preset.isBlank()) preset="god";
        if(!List.of("low","normal","high").contains(speed)) speed="normal";
        if(minScore==null) minScore=new LinkedHashMap<>(); if(radius==null) radius=new LinkedHashMap<>();
        stopAfterMatches=Math.max(0,Math.min(99,stopAfterMatches)); stopAfterMinutes=Math.max(0,Math.min(999,stopAfterMinutes));
        minScore.values().removeIf(v->v==null||v<0||v>100); radius.values().removeIf(v->v==null||v<300||v>12000);
    }
}
