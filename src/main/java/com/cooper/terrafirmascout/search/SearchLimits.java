package com.cooper.terrafirmascout.search;
/** Per-search choices made on the Options screen. Zero means "no limit" or "use the configured default". */
public record SearchLimits(int workers,int stopAfterMatches,int stopAfterMinutes) {
    public static final SearchLimits DEFAULT=new SearchLimits(0,0,0);
    public SearchLimits { workers=Math.max(0,workers); stopAfterMatches=Math.max(0,stopAfterMatches); stopAfterMinutes=Math.max(0,stopAfterMinutes); }
}
