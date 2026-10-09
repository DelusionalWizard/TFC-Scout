package com.cooper.terrafirmascout.client;

import net.minecraft.network.chat.Component;

/** All text shown by Scout's screens goes through here so it can be translated with a lang file. */
public final class Lang {
    private Lang() {}
    public static Component t(String key,Object... args) { return Component.translatable(key,args); }
    /** The same text as a plain String, for code that measures or shortens it before display. */
    public static String s(String key,Object... args) { return t(key,args).getString(); }
}
