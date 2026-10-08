package com.cooper.terrafirmascout.tfc;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.fml.ModList;
/** Conservative fingerprint: all mod archives and all supplied datapack bytes, not just guessed worldgen mods. */
public final class WorldgenFingerprint {
    public static String compute(SearchWorldContext c) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        add(digest,"scout-v1"); add(digest,SharedConstants.getCurrentVersion().getName()); add(digest,c.preset());
        add(digest,c.creation().options().generateStructures()+":"+c.creation().options().generateBonusChest());
        add(digest,c.creation().dataConfiguration().toString());
        var ops=RegistryOps.create(JsonOps.INSTANCE,c.creation().worldgenLoadContext());
        for(var entry:c.creation().selectedDimensions().bake(c.creation().datapackDimensions()).dimensions().entrySet().stream()
            .sorted(Comparator.comparing(e->e.getKey().location().toString())).toList()) {
            add(digest,entry.getKey().location().toString());
            add(digest,ChunkGenerator.CODEC.encodeStart(ops,entry.getValue().generator()).getOrThrow().toString());
        }
        var files=new TreeSet<Path>(Comparator.comparing(Path::toString));
        for(var mod:ModList.get().getMods().stream().sorted(Comparator.comparing(m->m.getModId())).toList()) {
            add(digest,mod.getModId()+":"+mod.getVersion()); files.add(mod.getOwningFile().getFile().getFilePath());
        }
        for(var path:files) hashPath(digest,path);
        if(c.dataPacks()!=null&&Files.exists(c.dataPacks())) hashPath(digest,c.dataPacks());
        // TFC common/server configuration can affect climate or resource semantics; hash conservatively.
        var config=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        if(Files.isDirectory(config)) try(var stream=Files.list(config)) {
            for(var path:stream.filter(p->p.getFileName().toString().startsWith("tfc-")).sorted().toList()) hashPath(digest,path);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void add(MessageDigest d,String s) { d.update(s.getBytes(StandardCharsets.UTF_8)); d.update((byte)0); }
    private static void hashPath(MessageDigest d,Path p) throws IOException {
        if(Files.isDirectory(p)) try(var stream=Files.walk(p)) {
            for(var f:stream.filter(Files::isRegularFile).sorted(Comparator.comparing(f->p.relativize(f).toString())).toList()) {
                add(d,p.relativize(f).toString()); hashFile(d,f);
            }
        } else { add(d,p.getFileName().toString()); hashFile(d,p); }
    }
    /** Digests of files already read this session, by path, size and modified time, so a second search in a big modpack does not read every jar again. */
    private static final java.util.concurrent.ConcurrentHashMap<String,byte[]> FILE_DIGESTS=new java.util.concurrent.ConcurrentHashMap<>();
    private static void hashFile(MessageDigest d,Path p) throws IOException {
        var attributes=Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class);
        String key=p.toAbsolutePath()+"|"+attributes.size()+"|"+attributes.lastModifiedTime().toMillis();
        byte[] digest=FILE_DIGESTS.get(key);
        if(digest==null) {
            MessageDigest inner; try { inner=MessageDigest.getInstance("SHA-256"); } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
            try(var in=Files.newInputStream(p)) { byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) inner.update(b,0,n); }
            digest=inner.digest(); FILE_DIGESTS.put(key,digest);
        }
        d.update(digest); d.update((byte)0);
    }
}
