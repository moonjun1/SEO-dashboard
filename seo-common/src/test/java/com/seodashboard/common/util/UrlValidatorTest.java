package com.seodashboard.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UrlValidator")
class UrlValidatorTest {

    // ------------------------------------------------------------------ //
    //  Happy Path – valid external URLs                                    //
    //  NOTE: these tests perform real DNS lookups.  Only universally        //
    //  resolvable hostnames are used here to keep the tests deterministic.  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("valid external URLs pass")
    class ValidUrls {

        @Test
        @DisplayName("http://example.com passes")
        void httpExampleCom_passes() {
            assertThatCode(() -> UrlValidator.validateForFetch("http://example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("https://google.com passes")
        void httpsGoogleCom_passes() {
            assertThatCode(() -> UrlValidator.validateForFetch("https://google.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("URL with path and query string passes")
        void urlWithPathAndQuery_passes() {
            assertThatCode(() -> UrlValidator.validateForFetch("http://example.com/path?q=1"))
                    .doesNotThrowAnyException();
        }
    }

    // ------------------------------------------------------------------ //
    //  Scheme validation                                                   //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("scheme validation")
    class SchemeValidation {

        @Test
        @DisplayName("blocks ftp:// scheme")
        void ftpScheme_throwsIllegalArgument() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("ftp://example.com/file.txt"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only HTTP and HTTPS");
        }

        @ParameterizedTest(name = "scheme in: {0}")
        @ValueSource(strings = {"file:///etc/passwd", "ssh://192.168.1.1"})
        @DisplayName("blocks non-HTTP(S) schemes")
        void nonHttpScheme_throwsIllegalArgument(String url) {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(url))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  Null / blank input                                                  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("null and blank input")
    class NullAndBlankInput {

        @Test
        @DisplayName("null URL throws IllegalArgumentException with empty-message")
        void nullUrl_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("URL must not be empty");
        }

        @Test
        @DisplayName("blank URL throws IllegalArgumentException with empty-message")
        void blankUrl_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("URL must not be empty");
        }

        @Test
        @DisplayName("empty string URL throws IllegalArgumentException with empty-message")
        void emptyUrl_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("URL must not be empty");
        }
    }

    // ------------------------------------------------------------------ //
    //  Loopback / special hostnames                                        //
    //  These are blocked BEFORE DNS resolution so no network is needed.    //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("loopback and special hostnames are blocked")
    class LoopbackHosts {

        @Test
        @DisplayName("blocks http://localhost")
        void localhost_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("http://localhost/admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("localhost");
        }

        @Test
        @DisplayName("blocks https://localhost with port")
        void localhostWithPort_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("https://localhost:8080/api"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blocks http://0.0.0.0")
        void anyLocalAddress_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("http://0.0.0.0/"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blocks *.local mDNS hostnames")
        void dotLocalHostname_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("http://mydevice.local/"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  Private IP ranges                                                   //
    //  Numeric IPs resolve immediately (no DNS lookup).                    //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("private IP ranges are blocked")
    class PrivateIpRanges {

        @Test
        @DisplayName("blocks 127.0.0.1 (IPv4 loopback)")
        void loopbackIp_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("http://127.0.0.1/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("private/internal");
        }

        @ParameterizedTest(name = "URL: {0}")
        @ValueSource(strings = {
                "http://192.168.0.1/",
                "http://192.168.1.100/",
                "http://192.168.255.255/"
        })
        @DisplayName("blocks 192.168.x.x (RFC-1918 Class C)")
        void classCPrivateIp_throws(String url) {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(url))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("private/internal");
        }

        @ParameterizedTest(name = "URL: {0}")
        @ValueSource(strings = {
                "http://10.0.0.1/",
                "http://10.10.10.10/",
                "http://10.255.255.255/"
        })
        @DisplayName("blocks 10.x.x.x (RFC-1918 Class A)")
        void classAPrivateIp_throws(String url) {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(url))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("private/internal");
        }

        @Test
        @DisplayName("blocks 169.254.169.254 (cloud metadata endpoint)")
        void cloudMetadataIp_throws() {
            assertThatThrownBy(() -> UrlValidator.validateForFetch("http://169.254.169.254/latest/meta-data/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("private/internal");
        }

        @ParameterizedTest(name = "URL: {0}")
        @ValueSource(strings = {
                "http://169.254.0.1/",
                "http://169.254.100.50/"
        })
        @DisplayName("blocks 169.254.x.x link-local range")
        void linkLocalRange_throws(String url) {
            assertThatThrownBy(() -> UrlValidator.validateForFetch(url))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("private/internal");
        }
    }
}
