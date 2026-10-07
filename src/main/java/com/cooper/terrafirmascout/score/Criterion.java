package com.cooper.terrafirmascout.score;
public enum Criterion {
    SPAWN("Spawn point",0), MAINLAND("Mainland start",0), LAND_RATIO("Land nearby",0),
    FRESHWATER("Freshwater (retired)",0), RIVER("River",0), LAKE("Lake",0), COAST("Coast or ocean",0), CROPS("Crops",0), FARMLAND("Farmland moisture",0),
    CLIMATE("Climate",13), TERRAIN("Building spot",10),
    FOREST("Trees",10), CLAY("Clay",9), OPEN_GROUND("Camp spot",0),
    STARTER_COPPER("Loose copper",15), COPPER_VEIN("Copper vein",7), FLUX("Flux",7),
    TIN("Tin",9), DIVERSITY("Rock variety",5), GRAPHITE("Graphite",5), IRON("Iron",0), COAL("Coal",0),
    KAOLIN("Kaolin",7), CONNECTIVITY("Land route to kaolin",3), CHALLENGE("Rugged terrain",0), SPECIFICATION("Your wishlist",0);
    public final String label; public final int weight;
    Criterion(String label,int weight) { this.label=label; this.weight=weight; }
    public boolean hard() { return this != DIVERSITY&&this!=FRESHWATER&&this!=CHALLENGE&&this!=SPECIFICATION; }
}
