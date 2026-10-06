package com.cooper.terrafirmascout.search;
import java.lang.reflect.Type;
import com.google.gson.*;
import com.cooper.terrafirmascout.score.VerificationState;
/** Incomplete distance is JSON null, not the invalid JSON literal Infinity. */
final class EvidenceJson implements JsonSerializer<Evidence>,JsonDeserializer<Evidence> {
    public JsonElement serialize(Evidence e,Type type,JsonSerializationContext context){
        var j=new JsonObject();j.addProperty("state",e.state().name());
        if(Double.isFinite(e.distance()))j.addProperty("distance",e.distance());else j.add("distance",JsonNull.INSTANCE);
        j.addProperty("x",e.x());j.addProperty("y",e.y());j.addProperty("z",e.z());j.addProperty("quality",e.quality());j.addProperty("detail",e.detail());return j;
    }
    public Evidence deserialize(JsonElement json,Type type,JsonDeserializationContext context){
        var j=json.getAsJsonObject();return new Evidence(VerificationState.valueOf(j.get("state").getAsString()),j.get("distance").isJsonNull()?Double.POSITIVE_INFINITY:j.get("distance").getAsDouble(),
            j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt(),j.get("quality").getAsDouble(),j.get("detail").getAsString());
    }
}
