package io.github.bertie_mc.fletching;

/** State implemented on the upstream projectile without replacing its registry identity. */
public interface ArrowRuntime {
    FlightState bertie$flight();

    void bertie$grounded(boolean value);
}
