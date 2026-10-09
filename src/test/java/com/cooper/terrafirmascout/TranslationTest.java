package com.cooper.terrafirmascout;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import com.google.gson.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Keeps the language files honest so translators can add a language without touching the code. */
class TranslationTest {
    private static final Path LANG=Path.of("src/main/resources/assets/terrafirmascout/lang");
    private static final Path SOURCE=Path.of("src/main/java");
    private static final Pattern KEY=Pattern.compile("\"(terrafirmascout\\.(?:common|history|note|options|results|world|spec|custom|toast|title)[a-z0-9_.]*)\"");

    private static Map<String,String> read(Path file) throws IOException {
        var map=new TreeMap<String,String>();
        for(var e:JsonParser.parseString(Files.readString(file)).getAsJsonObject().entrySet()) map.put(e.getKey(),e.getValue().getAsString());
        return map;
    }
    @Test void everyKeyUsedInCodeExistsInEnglish() throws IOException {
        var en=read(LANG.resolve("en_us.json")); var used=new TreeSet<String>();
        try(Stream<Path> files=Files.walk(SOURCE)) {
            for(var f:files.filter(p->p.toString().endsWith(".java")).toList()) { var m=KEY.matcher(Files.readString(f)); while(m.find()) used.add(m.group(1)); }
        }
        assertFalse(used.isEmpty());
        for(var key:used) assertTrue(en.containsKey(key),"Missing in en_us.json: "+key);
    }
    @Test void otherLanguagesOnlyUseKnownKeysAndKeepTheFormatSlots() throws IOException {
        var en=read(LANG.resolve("en_us.json"));
        try(Stream<Path> files=Files.list(LANG)) {
            for(var f:files.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("en_us.json")).toList()) {
                for(var e:read(f).entrySet()) {
                    assertTrue(en.containsKey(e.getKey()),f.getFileName()+" has an unknown key "+e.getKey());
                    assertEquals(slots(en.get(e.getKey())),slots(e.getValue()),f.getFileName()+" changes the %s slots of "+e.getKey());
                }
            }
        }
    }
    private static int slots(String text) { var m=Pattern.compile("%s").matcher(text.replace("%%","")); int n=0; while(m.find()) n++; return n; }
}
