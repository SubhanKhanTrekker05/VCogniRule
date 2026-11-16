package com.ooad.mhs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrowserLauncher (used by VCogniRule.exe)")
class BrowserLauncherTest {

    @Test
    @DisplayName("Home page address uses the server port")
    void homeUrl() {
        assertThat(BrowserLauncher.homeUrl(8080)).isEqualTo("http://localhost:8080/");
        assertThat(BrowserLauncher.homeUrl(9090)).isEqualTo("http://localhost:9090/");
    }

    @Test
    @DisplayName("Right open command for Windows, macOS and Linux")
    void openCommand() {
        String url = "http://localhost:8080/";
        assertThat(BrowserLauncher.openCommand("Windows 10", url))
            .containsExactly("rundll32", "url.dll,FileProtocolHandler", url);
        assertThat(BrowserLauncher.openCommand("Mac OS X", url)).containsExactly("open", url);
        assertThat(BrowserLauncher.openCommand("Linux", url)).containsExactly("xdg-open", url);
    }
}
