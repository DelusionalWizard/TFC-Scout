package com.cooper.terrafirmascout.tfc;
import java.util.concurrent.*;
/** A scratch world owns its threads; terminating them releases native per-thread caches. */
public final class ScratchExecutors {
    private static final ThreadLocal<ExecutorService> CURRENT=new ThreadLocal<>();
    private ScratchExecutors() {}
    public static ExecutorService currentOrGameExecutor() {
        var executor=CURRENT.get();
        if(executor!=null) return executor;
        CURRENT.remove(); return net.minecraft.Util.backgroundExecutor();
    }
    public static ExecutorService create() {
        return new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(),r->{
            var thread=new Thread(r,"TerraFirmaScout chunk generator");thread.setDaemon(true);thread.setContextClassLoader(ScratchExecutors.class.getClassLoader());return thread;
        }) {
            @Override protected void beforeExecute(Thread thread,Runnable task) { super.beforeExecute(thread,task); CURRENT.set(this); }
            @Override protected void afterExecute(Runnable task,Throwable failure) { try { CURRENT.remove(); } finally { super.afterExecute(task,failure); } }
        };
    }
    public static void close(ExecutorService executor) {
        executor.shutdown();
        boolean interrupted=false;
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
            while(!executor.isTerminated()&&System.nanoTime()<deadline) {
                try { executor.awaitTermination(200,TimeUnit.MILLISECONDS); }
                catch(InterruptedException e) { interrupted=true; executor.shutdownNow(); }
            }
            if(!executor.isTerminated()) { executor.shutdownNow(); throw new IllegalStateException("Scratch generation threads did not stop"); }
        } finally { if(interrupted) Thread.currentThread().interrupt(); }
    }
}
