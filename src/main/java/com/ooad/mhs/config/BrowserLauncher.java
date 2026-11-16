package com.ooad.mhs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Opens the home page in the default browser once the app is ready.
 *
 * Used by the double-click VCogniRule.exe, which starts the app with
 * {@code vcognirule.open-browser=true}. It is off by default, so running the app
 * from the IDE, from Maven or on Railway never opens a browser.
 */
@Component
@ConditionalOnProperty(name = "vcognirule.open-browser", havingValue = "true")
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private final int port;

    public BrowserLauncher(@Value("${server.port:8080}") int port) {
        this.port = port;
    }

    /** @return the address of the home page, e.g. http://localhost:8080/ */
    public static String homeUrl(int port) {
        return "http://localhost:" + port + "/";
    }

    /** @return the command that opens a web address with the operating system's default browser */
    static List<String> openCommand(String osName, String url) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.contains("win")) return List.of("rundll32", "url.dll,FileProtocolHandler", url);
        if (os.contains("mac")) return List.of("open", url);
        return List.of("xdg-open", url);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openHomePage() {
        String url = homeUrl(port);
        log.info("VCogniRule is running at {} (close this window to stop it)", url);
        try {
            new ProcessBuilder(openCommand(System.getProperty("os.name", ""), url)).start();
        } catch (IOException e) {
            log.warn("Could not open the browser automatically; open {} yourself.", url);
        }
    }
}
