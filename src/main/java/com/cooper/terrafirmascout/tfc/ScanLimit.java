package com.cooper.terrafirmascout.tfc;
import java.util.function.BooleanSupplier;
/**
 * Lets a Scout scan worker abandon one seed whose TFC region generation (river building) runs far too long, or stop at once on cancel.
 * Only threads that opened a scope are ever affected; the check is a single volatile read when no search is running, and it never
 * changes what TFC generates, because an abandoned seed's generators are thrown away.
 */
public final class ScanLimit {
    /** Thrown inside TFC's river builder on an abandoning scan thread. Carries no stack trace. */
    public static final class Abandoned extends RuntimeException {
        Abandoned() { super("Scout scan abandoned",null,false,false); }
    }
    private static final class Scope { long start; final long limit; final BooleanSupplier cancelled; Scope(long limit,BooleanSupplier cancelled) { this.limit=limit; this.cancelled=cancelled; start=System.nanoTime(); } }
    private static volatile int open;
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private ScanLimit() {}
    public static synchronized void begin(long limitNanos,BooleanSupplier cancelled) { CURRENT.set(new Scope(limitNanos,cancelled)); open++; }
    public static synchronized void end() { if(CURRENT.get()!=null) { CURRENT.remove(); open--; } }
    /** A paused scan should not count the pause against its time. */
    public static void restart() { var s=CURRENT.get(); if(s!=null) s.start=System.nanoTime(); }
    /** Called from TFC's river builder. */
    public static void check() {
        if(open==0) return;
        var s=CURRENT.get();
        if(s!=null&&(s.cancelled.getAsBoolean()||System.nanoTime()-s.start>s.limit)) throw new Abandoned();
    }
}
