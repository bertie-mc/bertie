package io.github.bertie_mc.emi.integration.oritech;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * Oritech ships its own EMI plugin, so its machines are already covered. The one thing no recipe can
 * point at is Crude Oil: it is placed by worldgen as oil springs rather than produced, which left the
 * fluid and its bucket looking unobtainable.
 */
public final class OritechEmiModule {

    private OritechEmiModule() {}

    public static void register(EmiRegistry reg) {
        InfoPages.page(
                reg,
                "oritech/crude_oil",
                List.of("oritech:still_oil_bucket"),
                "Crude Oil is not crafted. It forms as oil springs during world",
                "generation, in the Overworld and more often in deserts.",
                "Find a spring and pump it out.");
    }
}
