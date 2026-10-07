package com.cooper.terrafirmascout.tfc;
import java.util.*;
import com.cooper.terrafirmascout.profile.ScoutProfile;
import com.cooper.terrafirmascout.score.*;
import com.cooper.terrafirmascout.search.*;
import com.cooper.terrafirmascout.scanner.DetailedScanner;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
/** Positive findings only. A bounded unsuccessful scan stays INFERRED, never becomes a fabricated absence proof. */
public final class TFCFeatureProbe {
    private final SearchWorldContext context; private final ScoutProfile profile; private final SearchSession session;
    private final int targetBudget; private VeinCatalog catalog=null;
    public TFCFeatureProbe(SearchWorldContext context,ScoutProfile profile,SearchSession session,int targetBudget) {
        this.context=context; this.profile=profile; this.session=session; this.targetBudget=targetBudget;
    }
    public SeedResult verify(SeedCandidate candidate,String fingerprint) throws Exception {
        session.checkpoint();
        try(var world=new ScratchWorld(context,candidate.adapter().seed)) {
            catalog=VeinCatalog.of(world.level.registryAccess(),world.level.getChunkSource().getGenerator().getBiomeSource());
            var spawn=naturalSpawn(world);
            if(spawn==null) return result(candidate,candidate.center(),fingerprint,Criterion.SPAWN,Evidence.failed("No natural safe spawn within bounded native search"));
            var adapter=candidate.adapter();
            var refined=DetailedScanner.scan(adapter,spawn,profile,session,world.level.registryAccess(),world.level.getChunkSource().getGenerator());
            if(refined==null) return result(candidate,spawn,fingerprint,Criterion.CLIMATE,Evidence.failed("The climate at spawn does not fit this starting style"));
            var evidence=new EnumMap<Criterion,Evidence>(Criterion.class); evidence.putAll(refined.evidence());
            var spawnData=TfcCompat.data(world.level,world.level.getChunk(spawn));
            evidence.put(Criterion.SPAWN,verified(0,spawn,1,"The normal TFC spawn point. Trees here: "+spawnData.getForestType().getSerializedName().replace('_',' ')+", "+String.format("%.0f%%",spawnData.getForestDensity()*100)+" tree density"));
            if(profile.specification().enabled()) {
                var match=SpecificationVerifier.verify(profile.specification(),adapter,world,spawn,session);evidence.put(Criterion.SPECIFICATION,match);
                if(match.state()!=VerificationState.VERIFIED)return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
            }
            verifyLandscape(adapter,spawn,evidence);
            if(profile.requiredCriteria().stream().anyMatch(c->evidence.get(c).state()==VerificationState.FAILED))
                return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
            var climate=TfcCompat.data(world.level,world.level.getChunk(spawn));
            float temperature=OverworldClimateModel.getAdjustedAverageTempByElevation(spawn.getY(),climate.getAverageTemp(spawn));
            double rain=climate.getRainfall(spawn);
            if(profile.requires(Criterion.CLIMATE)&&(temperature<profile.temperatureMin()||temperature>profile.temperatureMax()||rain<profile.rainfallMin()||rain>profile.rainfallMax())) {
                evidence.put(Criterion.CLIMATE,Evidence.failed("The yearly climate is outside your limits"));
                return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
            }
            double quality=(CandidateScorer.climate(temperature,profile.temperatureMin(),profile.temperatureIdealMin(),profile.temperatureIdealMax(),profile.temperatureMax())
                +CandidateScorer.climate(rain,profile.rainfallMin(),profile.rainfallIdealMin(),profile.rainfallIdealMax(),profile.rainfallMax()))/2;
            evidence.put(Criterion.CLIMATE,verified(0,spawn,quality,"Yearly average: %.2f C; rainfall: %.2f mm".formatted(temperature,rain)));
            if(profile.requires(Criterion.CROPS)) {
                var fit=CropFit.crops(temperature);
                evidence.put(Criterion.CROPS,fit.enough()?verified(0,spawn,1,fit.describe()):Evidence.failed(fit.describe()));
                if(!fit.enough()) return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
            }
            if(profile.requires(Criterion.CHALLENGE)) {
                int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE; var cp=new ChunkPos(spawn);
                for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) { session.checkpoint();
                    var heights=TfcCompat.data(world.level,world.chunk(cp.x+dx,cp.z+dz)).getRockData().getSurfaceHeight();
                    for(int h:heights) { low=Math.min(low,h); high=Math.max(high,h); }
                }
                int span=high-low; boolean valid=span>=profile.minimumRoughness();
                evidence.put(Criterion.CHALLENGE,valid?verified(0,spawn,1,"Height difference across 48 x 48 blocks: "+span+" blocks"):Evidence.failed("The ground around spawn is not rough enough"));
                if(!valid)return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
            }
            var inspected=new HashMap<Long,ChunkAccess>();
            var pieces=new HashSet<BlockPos>(); int[] copperUnits={0};
            var missed=EnumSet.noneOf(Criterion.class);
            for(var criterion:List.of(Criterion.RIVER,Criterion.LAKE,Criterion.COAST,Criterion.TERRAIN,Criterion.OPEN_GROUND,Criterion.FOREST,Criterion.CLAY,
                Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,Criterion.FLUX,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL,Criterion.KAOLIN)) {
                if(!profile.requires(criterion)) { evidence.put(criterion,Evidence.absent("Not needed for this search")); continue; }
                session.stage="Checking "+criterion.label; session.checkpoint();
                var targets=expandedTargets(refined,criterion,spawn,world);
                if(criterion==Criterion.OPEN_GROUND&&evidence.get(Criterion.TERRAIN).state()==VerificationState.VERIFIED) {
                    var terrain=evidence.get(Criterion.TERRAIN); targets.add(0,new BlockPos(terrain.x(),terrain.y(),terrain.z()));
                }
                int tested=0;
                for(var target:targets) {
                    session.checkpoint(); if(tested++>=targetBudget) break;
                    var cp=new ChunkPos(target); var chunk=inspected.get(cp.toLong());
                    if(chunk==null) {
                        // Complete the decoration halo before inspecting blocks changed across chunk boundaries.
                        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) { session.checkpoint(); world.chunk(cp.x+dx,cp.z+dz); }
                        chunk=world.chunk(cp.x,cp.z); inspected.put(cp.toLong(),chunk);
                    }
                    var found=inspect(world,chunk,criterion,spawn,pieces,copperUnits);
                    if(found!=null) { evidence.put(criterion,found); break; }
                }
                if(evidence.get(criterion).state()!=VerificationState.VERIFIED)
                    evidence.put(criterion,Evidence.absent("Not found in "+Math.min(tested,targetBudget)+" chunks checked; this resource is still unconfirmed"));
                if(evidence.get(criterion).state()!=VerificationState.VERIFIED) {
                    // A bounded miss stays unconfirmed, so this seed can no longer be selected. After a second miss (one miss may still be a close call) stop once the rest
                    // cannot lift it above the best result already shown; otherwise keep going so the best partial stays accurate.
                    missed.add(criterion);
                    if(CandidateScorer.remainingChecksCannotMatter(evidence,profile,missed,session.bestRank()))
                        return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
                }
            }
            var kaolin=evidence.get(Criterion.KAOLIN);
            if(profile.requires(Criterion.CONNECTIVITY)&&kaolin.state()==VerificationState.VERIFIED) {
                var target=new BlockPos(kaolin.x(),kaolin.y(),kaolin.z());
                boolean reachable=ConnectivityScanner.connected(adapter,spawn,target,profile.radius(),session);
                evidence.put(Criterion.CONNECTIVITY,reachable?verified(kaolin.distance(),target,1,"A land route to kaolin exists; you may still need to cross rivers")
                    :Evidence.absent("Could not confirm a land route to kaolin"));
                if(profile.kaolinSameLandmass()&&!reachable) evidence.put(Criterion.KAOLIN,Evidence.absent("Found kaolin, but could not confirm a land route"));
            }
            return new SeedResult(adapter.seed,spawn.getX(),spawn.getY(),spawn.getZ(),fingerprint,profile,evidence);
        }
    }
    private BlockPos naturalSpawn(ScratchWorld world) {
        var ext=(ChunkGeneratorExtension)world.level.getChunkSource().getGenerator();
        var center=new ChunkPos(ext.findSpawnBiome(new XoroshiroRandomSource(world.level.getSeed())));
        // Same spiral and same respawn routine as TFC 3.2.25 ForgeEventHandler.onCreateWorldSpawn.
        int x=0,z=0,xStep=0,zStep=-1;
        for(int tries=0;tries<1024;tries++) { session.checkpoint();
            if(x>-16&&x<=16&&z>-16&&z<=16) {
                var found=PlayerRespawnLogic.getSpawnPosInChunk(world.level,new ChunkPos(center.x+x,center.z+z));
                if(found!=null) return found;
            }
            if(x==z||(x<0&&x==-z)||(x>0&&x==1-z)) { int swap=xStep; xStep=-zStep; zStep=swap; }
            x+=xStep; z+=zStep;
        }
        return null;
    }
    private void verifyLandscape(TFCWorldgenAdapter a,BlockPos spawn,Map<Criterion,Evidence> e) {
        if(!profile.requires(Criterion.MAINLAND)&&!profile.requires(Criterion.LAND_RATIO))return;
        int land=0,total=0,r=profile.landRadius();
        // Count every block column using TFC's exact quart biome lookup, rather than extrapolating a coarse sample.
        for(int dx=-r;dx<=r;dx++) { if((dx&31)==0) session.checkpoint();
            for(int dz=-r;dz<=r;dz++) if((long)dx*dx+(long)dz*dz<=(long)r*r) {
                total++; if(a.land(spawn.getX()+dx,spawn.getZ()+dz)) land++;
            }
        }
        double ratio=(double)land/total;
        e.put(Criterion.LAND_RATIO,ratio>=profile.minimumLand()?verified(r,spawn,1,"%.2f%% non-salty, non-shore biome coverage".formatted(ratio*100)):Evidence.failed("Not enough land nearby"));
        if(!profile.requires(Criterion.MAINLAND))return;
        var point=a.point(spawn.getX(),spawn.getZ());
        String biome=a.biome(spawn.getX(),spawn.getZ()).key().location().getPath();
        boolean extreme=biome.contains("badlands")||biome.contains("mountain")||biome.contains("canyon")||biome.contains("volcano");
        boolean mainland=point.land()&&!point.island()&&!point.mountain()&&!a.biome(spawn.getX(),spawn.getZ()).isVolcanic()&&!extreme&&ratio>=profile.minimumLand();
        if(mainland) mainland=connectedCoverage(a,spawn,r)>=profile.minimumLand();
        e.put(Criterion.MAINLAND,mainland?verified(0,spawn,1,"A connected mainland start, away from extreme terrain"):Evidence.failed("This start is an island, extreme terrain, or a broken-up coast"));
    }
    private double connectedCoverage(TFCWorldgenAdapter a,BlockPos spawn,int radius) {
        int cells=(radius+3)/4,width=cells*2+1,total=0; boolean[] land=new boolean[width*width];
        for(int dx=-cells;dx<=cells;dx++) { session.checkpoint(); for(int dz=-cells;dz<=cells;dz++) {
            if((long)dx*dx+(long)dz*dz>(long)cells*cells) continue; total++;
            land[(dx+cells)*width+dz+cells]=a.land(spawn.getX()+dx*4,spawn.getZ()+dz*4);
        } }
        int start=cells*width+cells; if(!land[start]) return 0;
        var queue=new ArrayDeque<Integer>(); queue.add(start); land[start]=false; int reached=0;
        while(!queue.isEmpty()) { if((reached&2047)==0) session.checkpoint(); int n=queue.remove(); reached++; int x=n/width,z=n%width;
            for(int next:new int[]{x>0?n-width:-1,x+1<width?n+width:-1,z>0?n-1:-1,z+1<width?n+1:-1})
                if(next>=0&&land[next]) { land[next]=false; queue.add(next); }
        }
        return (double)reached/total;
    }
    private List<BlockPos> expandedTargets(SeedCandidate c,Criterion criterion,BlockPos spawn,ScratchWorld world) {
        var chunks=new LinkedHashMap<Long,BlockPos>();
        var points=new ArrayList<>(c.targets().get(criterion));
        var generator=world.level.getChunkSource().getGenerator();
        if(generator instanceof net.dries007.tfc.world.TFCChunkGenerator tfc&&Set.of(Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL).contains(criterion)) {
            var costs=new HashMap<BlockPos,Double>();
            for(var pos:points){session.checkpoint();int height=(int)tfc.createHeightFillerForChunk(new ChunkPos(pos)).sampleHeight(pos.getX(),pos.getZ());
                double cost=Double.POSITIVE_INFINITY;
                for(var hint:c.veinHints().get(criterion).getOrDefault(pos,List.of())){
                    int y=pos.getY()+(hint.projected()?height:0);
                    double gap=criterion==Criterion.STARTER_COPPER?Math.max(0,Math.abs(height-y)-25):Math.max(0,y-height-hint.verticalRadius());
                    boolean host=false;
                    for(int dy:new int[]{0,-hint.verticalRadius(),hint.verticalRadius()})
                        if(hint.hosts().contains(c.adapter().rock(pos.getX(),y+dy,pos.getZ(),height)))host=true;
                    cost=Math.min(cost,gap*10000+(host?0:50000)+DetailedScanner.distance(spawn,pos));
                }
                costs.put(pos,Double.isFinite(cost)?cost:DetailedScanner.distance(spawn,pos));}

            points.sort(Comparator.comparingDouble(costs::get));
        }

        // Sample each distinct native center before spending the budget on a single deposit's halo.
        int halo=Set.of(Criterion.STARTER_COPPER,Criterion.COPPER_VEIN,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL,Criterion.KAOLIN).contains(criterion)?2:0;
        for(int ring=0;ring<=halo;ring++) {
            for(var p:points) {
                var cp=new ChunkPos(p);
                for(int dx=-ring;dx<=ring;dx++) for(int dz=-ring;dz<=ring;dz++) {
                    if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
                    var q=new ChunkPos(cp.x+dx,cp.z+dz); chunks.putIfAbsent(q.toLong(),q.getWorldPosition());
                }
                if(chunks.size()>targetBudget*8)break;
            }
            if(chunks.size()>targetBudget*8)break;
        }
        // Deterministic spiral fallback, e.g. trees and grassy camps missed by coarse sampling.
        var center=new ChunkPos(spawn);
        for(int ring=0;chunks.size()<targetBudget*2&&ring<=32;ring++) for(int dx=-ring;dx<=ring;dx++) for(int dz=-ring;dz<=ring;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=ring) continue;
            var cp=new ChunkPos(center.x+dx,center.z+dz); chunks.putIfAbsent(cp.toLong(),cp.getWorldPosition());
        }
        return new ArrayList<>(chunks.values());
    }
    private Evidence inspect(ScratchWorld w,ChunkAccess chunk,Criterion c,BlockPos spawn,Set<BlockPos> pieces,int[] units) {
        var cp=chunk.getPos();
        int max=c==Criterion.TERRAIN?profile.terrainRadius():c==Criterion.OPEN_GROUND?profile.campRadius():profile.distance(c);
        for(int lx=0;lx<16;lx++) for(int lz=0;lz<16;lz++) {
            int x=cp.getMinBlockX()+lx,z=cp.getMinBlockZ()+lz; double d=Math.hypot((double)x-spawn.getX(),(double)z-spawn.getZ()); if(d>max) continue;
            int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,lx,lz);
            int ground=TfcCompat.data(w.level,chunk).getRockData().getSurfaceHeight()[lz*16+lx];
            if(c==Criterion.TERRAIN||c==Criterion.OPEN_GROUND) {
                if(lx>0||lz>0) continue;
                var patch=buildablePatch(chunk,spawn,c); if(patch!=null) return verified(DetailedScanner.distance(spawn,patch),patch,1,"A dry "+(c==Criterion.TERRAIN?profile.specification().terrainSize():profile.specification().campSize())+"-block square, with clear headroom; grass at least "+Math.round(profile.specification().grassFraction()*100)+"%; height difference at most "+profile.specification().maximumSlope());
                continue;
            }
            int min=Set.of(Criterion.COPPER_VEIN,Criterion.TIN,Criterion.GRAPHITE,Criterion.IRON,Criterion.COAL,Criterion.FLUX).contains(c)?chunk.getMinBuildHeight():Math.max(chunk.getMinBuildHeight(),ground-12);
            for(int y=Math.min(top+1,chunk.getMaxBuildHeight()-1);y>=min;y--) {
                var pos=new BlockPos(x,y,z); var state=chunk.getBlockState(pos); var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
                boolean found=false;
                if(c==Criterion.RIVER||c==Criterion.LAKE||c==Criterion.COAST) found=waterFound(chunk,c,state,id.toString(),x,y,z)&&y>=ground-2;
                if(c==Criterion.FOREST) found=state.is(net.minecraft.tags.BlockTags.LOGS)&&y>=ground;
                if(c==Criterion.CLAY) found=id.getNamespace().equals("tfc")&&(id.getPath().startsWith("clay/")||id.getPath().startsWith("clay_grass/")||id.getPath().startsWith("clay_duff/"));
                if(c==Criterion.KAOLIN) found=id.getNamespace().equals("tfc")&&Set.of("white_kaolin_clay","pink_kaolin_clay","red_kaolin_clay","kaolin_clay_grass").contains(id.getPath());
                if(c==Criterion.FLUX) found=id.getNamespace().equals("tfc")&&(id.getPath().startsWith("rock/raw/")||id.getPath().startsWith("rock/hardened/"))
                    &&Set.of("limestone","dolomite","chalk","marble").contains(id.getPath().substring(id.getPath().lastIndexOf('/')+1));
                // The ore blocks the world's own veins place (TFC's, or a pack's such as GregTech ores), not a fixed list of names.
                if(c==Criterion.COPPER_VEIN||c==Criterion.TIN||c==Criterion.GRAPHITE||c==Criterion.IRON||c==Criterion.COAL) found=catalog.oreBlocks(c).contains(id);
                if(c==Criterion.STARTER_COPPER&&id.getNamespace().equals("tfc")&&Set.of("ore/small_native_copper","ore/small_malachite","ore/small_tetrahedrite").contains(id.getPath())
                    &&y>=ground-2&&state.getFluidState().isEmpty()&&pieces.add(pos)) {
                    var item=new ItemStackInventory(new ItemStack(state.getBlock().asItem()));
                    for(var recipe:context.creation().dataPackResources().getRecipeManager().getAllRecipesFor(TFCRecipeTypes.HEATING.get())) {
                        if(recipe.matches(item,w.level)) {
                            var fluid=recipe.assembleFluid(item);
                            if(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals("tfc:metal/copper")) units[0]+=fluid.getAmount();
                            break;
                        }
                    }
                    if(pieces.size()>=profile.minimumCopperPieces()&&units[0]>=profile.minimumCopperUnits())
                        return verified(d,pos,proximity(c,d),pieces.size()+" loose surface pieces / "+units[0]+" mB of copper with your current recipes");
                }
                if(found) return verified(d,pos,proximity(c,d),"Found "+id);
            }
        }
        return null;
    }
    private static boolean waterFound(ChunkAccess chunk,Criterion c,net.minecraft.world.level.block.state.BlockState state,String id,int x,int y,int z) {
        if(state.getFluidState().isEmpty()||!state.getFluidState().isSource()) return false;
        if(c==Criterion.COAST) return id.equals("tfc:fluid/salt_water");
        if(!id.equals("minecraft:water")&&!id.equals("tfc:fluid/river_water")) return false;
        String biome=chunk.getNoiseBiome(x>>2,y>>2,z>>2).unwrapKey().map(k->k.location().getPath()).orElse("");
        return biome.contains(c==Criterion.RIVER?"river":"lake");
    }
    private BlockPos buildablePatch(ChunkAccess chunk,BlockPos spawn,Criterion c) {
        // WG maps can retain pre-decoration ground under trees. Build a private inspection map with the native predicate.
        // Do not replace the chunk's maps: later native decorations must see the original generation state.
        var decorated=new Heightmap(chunk,Heightmap.Types.OCEAN_FLOOR_WG);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=chunk.getHighestSectionPosition()+15;y>=chunk.getMinBuildHeight();y--){
            var state=chunk.getBlockState(new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z));
            if(Heightmap.Types.OCEAN_FLOOR_WG.isOpaque().test(state)){decorated.update(x,y,z,state);break;}
        }
        int max=c==Criterion.TERRAIN?profile.terrainRadius():profile.campRadius();
        var spec=profile.specification(); int size=c==Criterion.TERRAIN?spec.terrainSize():spec.campSize();
        for(int sx=0;sx<=16-size;sx+=Math.min(4,size))for(int sz=0;sz<=16-size;sz+=Math.min(4,size)) {
            int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE,grass=0;boolean valid=true;
            for(int x=sx;x<sx+size&&valid;x++)for(int z=sz;z<sz+size;z++) {
                int y=decorated.getFirstAvailable(x,z)-1;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
                var state=chunk.getBlockState(pos);var id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();boolean grassy=state.is(TFCTags.Blocks.GRASS);
                boolean firm=grassy||state.is(net.minecraft.tags.BlockTags.DIRT)||id.startsWith("clay/")||id.startsWith("rock/raw/")||id.startsWith("rock/hardened/");
                if(DetailedScanner.distance(spawn,pos)>max||!firm||!state.getFluidState().isEmpty()||!chunk.getBlockState(pos.above()).getFluidState().isEmpty()){valid=false;break;}
                for(int above=1;above<=3;above++){
                    var clearance=chunk.getBlockState(pos.above(above));
                    if(!clearance.getFluidState().isEmpty()||!clearance.isAir()&&!clearance.canBeReplaced()){valid=false;break;}
                }
                if(!valid)break;
                if(grassy)grass++;low=Math.min(low,y);high=Math.max(high,y);
            }
            if(valid&&high-low<=spec.maximumSlope()&&grass>=size*size*spec.grassFraction())return new BlockPos(chunk.getPos().getBlockX(sx+size/2),high+1,chunk.getPos().getBlockZ(sz+size/2));
        }
        return null;
    }
    private double proximity(Criterion c,double d) {
        double ideal=c==Criterion.GRAPHITE?2500:c==Criterion.KAOLIN?3000:profile.distance(c)*0.6;
        return CandidateScorer.proximity(d,ideal,profile.distance(c));
    }
    private static Evidence verified(double d,BlockPos p,double q,String detail) { return new Evidence(VerificationState.VERIFIED,d,p.getX(),p.getY(),p.getZ(),q,detail); }
    private SeedResult result(SeedCandidate c,BlockPos p,String fp,Criterion criterion,Evidence e) {
        var map=new EnumMap<Criterion,Evidence>(Criterion.class); map.putAll(c.evidence()); map.put(criterion,e);
        return new SeedResult(c.adapter().seed,p.getX(),p.getY(),p.getZ(),fp,profile,map);
    }
}
