package tvhbrowser;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class TvHBrowserSettingsTest {

    private TvHBrowser browser;

    @Before
    public void setUp() {
        browser = new TvHBrowser();
    }

    @Test
    public void returnsDefaultsForMissingKeys() {
        assertEquals("x", browser.getSetting("missing", "x"));
        assertEquals(9981, browser.getSetting("missing", 9981));
        assertTrue(browser.getSetting("missing", true));
    }

    @Test
    public void invalidNumbersFallBackToDefault() {
        browser.setSetting("port", "not-a-number");
        assertEquals(9981, browser.getSetting("port", 9981));
        browser.setSetting("port", "");
        assertEquals(9981, browser.getSetting("port", 9981));
    }

    @Test
    public void readsStoredValues() {
        browser.setSetting("port", 443);
        browser.setSetting("https", true);
        browser.setSetting("fqdn", "tv.example");
        assertEquals(443, browser.getSetting("port", 1));
        assertTrue(browser.getSetting("https", false));
        assertEquals("tv.example", browser.getSetting("fqdn", ""));
    }

    @Test
    public void numbersWithSurroundingWhitespaceAreAccepted() {
        browser.setSetting("timeout", " 750 ");
        assertEquals(750, browser.getSetting("timeout", 500));
    }

    @Test
    public void loadedSettingsReplaceTheCurrentOnes() {
        Properties props = new Properties();
        props.setProperty("fqdn", "loaded");
        browser.loadSettings(props);
        assertEquals("loaded", browser.getSetting("fqdn", ""));
        assertSame(props, browser.storeSettings());
    }

    @Test
    public void translationsComeFromTheBundleWithSubstitutedArguments() {
        String text = TvHBrowser.getTranslation("chooseSimilar", "fallback", "Tatort");
        assertTrue(text, text.contains("Tatort"));
        assertFalse(text, text.contains("{0}"));
        assertFalse(text, text.startsWith("[TvHBrowser."));
    }

    @Test
    public void everyTranslationKeyUsedInTheCodeExistsInTheBundle() {
        for (String key : new String[] { "epgLoading", "chooseTitle", "upcomingRecordings", "finishedRecordings",
                "untitled" }) {
            String text = TvHBrowser.getTranslation(key, "fallback");
            assertFalse(key + " -> " + text, text.startsWith("[TvHBrowser."));
        }
    }

    @Test
    public void settingsAreNotSharedBetweenInstances() {
        browser.setSetting("fqdn", "first");
        assertEquals("default", new TvHBrowser().getSetting("fqdn", "default"));
    }
}
