package photorawlab.app.edit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import photorawlab.adapters.LensfunCatalog;
import photorawlab.domain.edit.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

class LensPanelTest {
    @Test
    void delayedAutomaticLookupCannotReplaceNextPhotosPinnedProfile() throws Exception {
        var catalog = new LensfunCatalog();
        var profile = catalog.search("Sony", "28-60", "ILCE-7C", false).getFirst();
        var next =
                new LensSettings(
                        LensSettings.Mode.AUTO, profile, true, true, true, 42, 5.6, 1, false);
        List<Runnable> queue = new ArrayList<>();
        AtomicReference<LensSettings> changed = new AtomicReference<>();
        LensPanel[] panel = new LensPanel[1];
        Runnable[] stale = new Runnable[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    panel[0] = new LensPanel(queue::add, changed::set);
                    panel[0].catalog(catalog);
                    queue.clear();
                    panel[0].show(LensSettings.DEFAULT, "ILCE-7M3", "FE 28-70mm f/3.5-5.6 OSS");
                    stale[0] = queue.getFirst();
                    panel[0].show(next, "ILCE-7C", "FE 28-60mm F4-5.6");
                });
        stale[0].run();
        SwingUtilities.invokeAndWait(
                () ->
                        assertNull(
                                changed.get(),
                                "A stale lookup must not save the previous lens on the next"
                                    + " photo"));
    }
}
