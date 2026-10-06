package com.cooper.terrafirmascout.search;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.neoforged.fml.loading.FMLPaths;
/** History is separate from exports: public reports hide resource coordinates by default. */
public final class ResultHistory {
    private static final Gson JSON=new GsonBuilder().registerTypeAdapter(Evidence.class,new EvidenceJson()).serializeNulls().setPrettyPrinting().create();
    private static Path root(){return FMLPaths.GAMEDIR.get().resolve("terrafirmascout");}
    public static Path save(SeedResult result) throws Exception {
        if(!result.selectable(result.fingerprint()))throw new IllegalArgumentException("Only qualifying verified results can be saved");
        var dir=root().resolve("history");Files.createDirectories(dir);
        var path=dir.resolve(result.seed()+"-"+Integer.toHexString(result.profile().hashCode())+"-"+result.fingerprint().substring(0,12)+".json");
        var temporary=Files.createTempFile(dir,"result-",".tmp");
        try{Files.writeString(temporary,JSON.toJson(result));Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
        return path;
    }
    public static List<SeedResult> load() {
        var dir=root().resolve("history");var results=new ArrayList<SeedResult>();
        if(!Files.isDirectory(dir))return results;
        try(var paths=Files.list(dir)){
            var files=paths.filter(p->p.getFileName().toString().endsWith(".json")).sorted((a,b)->{try{return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));}catch(Exception e){return 0;}}).limit(100).toList();
            for(var p:files)try{var result=JSON.fromJson(Files.readString(p),SeedResult.class);if(result.selectable(result.fingerprint()))results.add(result);}catch(Exception ignored){}
        }catch(Exception e){com.cooper.terrafirmascout.TerraFirmaScout.LOGGER.warn("Unable to read seed history",e);}
        return List.copyOf(results);
    }
    public static String encode(SeedResult result){return JSON.toJson(result);}
    public static SeedResult decode(String json){return JSON.fromJson(json,SeedResult.class);}
    public static JsonObject report(SeedResult result,boolean reveal) {
        var report=JSON.toJsonTree(result).getAsJsonObject();report.addProperty("formatVersion",1);report.addProperty("status",result.status());report.addProperty("score",result.score());
        if(!reveal){report.remove("spawnX");report.remove("spawnY");report.remove("spawnZ");
            report.getAsJsonObject("evidence").entrySet().forEach(e->{var v=e.getValue().getAsJsonObject();v.remove("x");v.remove("y");v.remove("z");});}
        return report;
    }
    public static Path export(SeedResult result,boolean reveal) throws Exception {
        if(!result.selectable(result.fingerprint()))throw new IllegalArgumentException("Result is not fully verified");
        var dir=root().resolve("exports");Files.createDirectories(dir);var path=dir.resolve("seed-"+result.seed()+(reveal?"-locations":"")+".json");
        Files.writeString(path,JSON.toJson(report(result,reveal)));return path;
    }
}
