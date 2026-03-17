package com.seodashboard.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UrlUtils")
class UrlUtilsTest {

    // ------------------------------------------------------------------ //
    //  normalizeUrl                                                        //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("normalizeUrl")
    class NormalizeUrl {

        @Test
        @DisplayName("removes fragment from URL")
        void removesFragment() {
            assertThat(UrlUtils.normalizeUrl("https://example.com/page#section"))
                    .isEqualTo("https://example.com/page");
        }

        @Test
        @DisplayName("removes trailing slash from URL")
        void removesTrailingSlash() {
            assertThat(UrlUtils.normalizeUrl("https://example.com/about/"))
                    .isEqualTo("https://example.com/about");
        }

        @Test
        @DisplayName("removes both fragment and trailing slash")
        void removesFragmentAndTrailingSlash() {
            // fragment is stripped first, so the trailing slash check applies after
            assertThat(UrlUtils.normalizeUrl("https://example.com/page/#section"))
                    .isEqualTo("https://example.com/page");
        }

        @Test
        @DisplayName("leaves URL unchanged when neither fragment nor trailing slash present")
        void noChangeWhenClean() {
            assertThat(UrlUtils.normalizeUrl("https://example.com/page"))
                    .isEqualTo("https://example.com/page");
        }

        @Test
        @DisplayName("preserves root path '/' (length == 1 guard)")
        void preservesRootSlash() {
            assertThat(UrlUtils.normalizeUrl("/"))
                    .isEqualTo("/");
        }

        @Test
        @DisplayName("preserves query parameters")
        void preservesQueryParams() {
            assertThat(UrlUtils.normalizeUrl("https://example.com/search?q=seo&lang=en"))
                    .isEqualTo("https://example.com/search?q=seo&lang=en");
        }
    }

    // ------------------------------------------------------------------ //
    //  normalizeUrlWithScheme                                              //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("normalizeUrlWithScheme")
    class NormalizeUrlWithScheme {

        @Test
        @DisplayName("prepends https:// when no scheme is present")
        void prependsHttpsWhenNoScheme() {
            assertThat(UrlUtils.normalizeUrlWithScheme("example.com"))
                    .isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("does not alter an http:// URL")
        void leavesHttpSchemeIntact() {
            assertThat(UrlUtils.normalizeUrlWithScheme("http://example.com"))
                    .isEqualTo("http://example.com");
        }

        @Test
        @DisplayName("does not alter an https:// URL")
        void leavesHttpsSchemeIntact() {
            assertThat(UrlUtils.normalizeUrlWithScheme("https://example.com"))
                    .isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("trims leading and trailing whitespace before processing")
        void trimsWhitespace() {
            assertThat(UrlUtils.normalizeUrlWithScheme("  example.com  "))
                    .isEqualTo("https://example.com");
        }
    }

    // ------------------------------------------------------------------ //
    //  extractDomain                                                       //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("extractDomain")
    class ExtractDomain {

        @Test
        @DisplayName("extracts hostname from a normal URL")
        void extractsHostname() {
            assertThat(UrlUtils.extractDomain("https://example.com/page"))
                    .isEqualTo("example.com");
        }

        @Test
        @DisplayName("extracts hostname from a URL that includes a port number")
        void extractsHostnameWithPort() {
            assertThat(UrlUtils.extractDomain("https://example.com:8443/api"))
                    .isEqualTo("example.com");
        }

        @Test
        @DisplayName("extracts hostname from a URL that includes a path")
        void extractsHostnameWithPath() {
            assertThat(UrlUtils.extractDomain("http://sub.example.com/a/b/c?x=1"))
                    .isEqualTo("sub.example.com");
        }

        @Test
        @DisplayName("returns empty string for a malformed URL")
        void returnsEmptyForInvalidUrl() {
            assertThat(UrlUtils.extractDomain("not a valid url"))
                    .isEqualTo("");
        }

        @Test
        @DisplayName("returns empty string for null input")
        void returnsEmptyForNull() {
            assertThat(UrlUtils.extractDomain(null))
                    .isEqualTo("");
        }
    }
}
