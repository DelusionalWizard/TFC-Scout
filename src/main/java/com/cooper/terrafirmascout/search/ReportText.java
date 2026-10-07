package com.cooper.terrafirmascout.search;
import java.util.Locale;
import com.cooper.terrafirmascout.score.*;
/** Plain-text report of a result, for pasting into chat. Locations are left out unless the player has chosen to reveal them. */
public final class ReportText {
    private ReportText() {}
    public static String bucket(double d) { int step=d<1000?100:500; int blocks=Math.max(step,(int)Math.ceil(d/step)*step); return blocks>=1000?String.format(Locale.ROOT,"%.1f km",blocks/1000.0):blocks+" m"; }
    public static String text(SeedResult r,boolean reveal) {
        var sb=new StringBuilder();
        sb.append("TerraFirmaScout seed report\n");
        sb.append("Seed: ").append(r.seed()).append('\n');
        sb.append("Style: ").append(r.profile().displayName()).append(" - match ").append(r.score()).append("% - ").append(r.status()).append('\n');
        if(reveal) sb.append("Start: X=").append(r.spawnX()).append(" Z=").append(r.spawnZ()).append('\n');
        sb.append("Checks:\n");
        for(var c:Criterion.values()) {
            if(!r.profile().requires(c)) continue;
            var e=r.evidence().get(c);
            var state=e==null?"Not checked":switch(e.state()){case VERIFIED->"Confirmed";case INFERRED->"Not confirmed";case FAILED->"Does not match";};
            sb.append(" - ").append(c.label).append(": ").append(state);
            if(e!=null&&e.state()==VerificationState.VERIFIED&&Double.isFinite(e.distance())&&e.distance()>0) sb.append(" within ").append(bucket(e.distance()));
            if(reveal&&e!=null&&e.state()==VerificationState.VERIFIED) sb.append(" (X=").append(e.x()).append(" Y=").append(e.y()).append(" Z=").append(e.z()).append(')');
            sb.append('\n');
        }
        sb.append("Found with TerraFirmaScout for TerraFirmaCraft 4.2.11 (Minecraft 1.21.1). The seed only matches with the same world settings.\n");
        return sb.toString();
    }
}
