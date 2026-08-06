package io.github.bertie_mc.alexscavesworldgenfix.logic;

/**
 * Recognises the one biome-decoration failure this mod is allowed to absorb.
 *
 * <p>Alex's Caves redirects <i>every</i> {@code List.get(int)} in
 * {@code ChunkGenerator#applyBiomeDecoration} through a clamp that is meant to survive a stale
 * feature index:
 *
 * <pre>
 * if (index &lt; 0 || index &gt;= list.size()) {
 *     int safeIndex = Math.max(0, Math.min(index, list.size() - 1));
 *     return list.get(safeIndex);
 * }
 * </pre>
 *
 * <p>On an <b>empty</b> list that reads {@code max(0, min(index, -1))}, which is {@code 0}, and
 * {@code get(0)} throws {@code IndexOutOfBoundsException: Index 0 out of bounds for length 0} - the
 * exact crash the clamp was added in 2.0.9 to prevent. Vanilla wraps the decoration loop in
 * {@code catch (Exception)} and rethrows a {@code ReportedException}, which kills the worldgen
 * thread: already-loaded chunks keep rendering and nothing new ever generates.
 *
 * <p>Upstream issue: <a href="https://github.com/Raguto/AlexsCaves-1.21.1/issues/172">Raguto/
 * AlexsCaves-1.21.1#172</a>, open against 2.0.10 - the newest build - with no fix.
 *
 * <p>Pure predicate logic, no Minecraft types, so the decision is unit-testable on its own.
 */
public final class BiomeDecorationFailure {

    /**
     * Alex's Caves' redirect handler.
     *
     * <p>Matched by CONTAINMENT, not equality: Mixin renames the handler when it merges it into
     * {@code ChunkGenerator}, and the observed runtime name carries a per-build hash -
     * {@code redirect$dfj000$alexscaves$ac_clampBiomeDecorationIndex}.
     */
    public static final String CLAMP_HANDLER = "ac_clampBiomeDecorationIndex";

    /** Cause chains here are two deep; the bound only stops a self-referential one from hanging. */
    private static final int MAX_CAUSE_DEPTH = 16;

    private BiomeDecorationFailure() {}

    /**
     * True only when {@code thrown} carries Alex's Caves' empty-list clamp failure.
     *
     * <p>Deliberately narrow on both axes. The throwable must be an
     * {@link IndexOutOfBoundsException} <i>and</i> some frame of its own stack must be the clamp
     * handler. Anything else - another mod's worldgen bug, a genuine vanilla fault - is not ours to
     * swallow and has to keep crashing, or this mod turns every worldgen defect into silent
     * missing terrain.
     *
     * <p>The chain is walked because vanilla rewraps the failure as a {@code ReportedException}
     * before it leaves {@code applyBiomeDecoration}.
     */
    public static boolean isAlexsCavesClamp(Throwable thrown) {
        Throwable current = thrown;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof IndexOutOfBoundsException && threwInsideClamp(current)) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                return false;
            }
            current = cause;
        }
        return false;
    }

    private static boolean threwInsideClamp(Throwable thrown) {
        for (StackTraceElement frame : thrown.getStackTrace()) {
            if (frame.getMethodName().contains(CLAMP_HANDLER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether occurrence number {@code failureCount} should be written to the log.
     *
     * <p>A player crossing virgin terrain can trip this on chunk after chunk, so the log has to
     * stay readable while still making it obvious the fix is carrying the world: the first three,
     * then every hundredth.
     */
    public static boolean shouldReport(long failureCount) {
        if (failureCount <= 0L) {
            return false;
        }
        return failureCount <= 3L || failureCount % 100L == 0L;
    }
}
