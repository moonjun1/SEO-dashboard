package com.seodashboard.common.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Validates URLs to prevent SSRF attacks by blocking internal/private IP ranges.
 */
public final class UrlValidator {

    private UrlValidator() {
    }

    /**
     * Validates that a URL is safe for server-side fetching.
     * Blocks private IPs, loopback, link-local, and cloud metadata endpoints.
     *
     * @throws IllegalArgumentException if the URL is unsafe
     */
    public static void validateForFetch(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Only HTTP and HTTPS schemes are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }

        String hostLower = host.toLowerCase();
        if (hostLower.equals("localhost") || hostLower.equals("0.0.0.0") || hostLower.endsWith(".local")) {
            throw new IllegalArgumentException("Requests to localhost are not allowed");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrReserved(addr)) {
                    throw new IllegalArgumentException(
                            "Requests to private/internal IP addresses are not allowed: " + host
                    );
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    private static boolean isPrivateOrReserved(InetAddress addr) {
        return addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isAnyLocalAddress()
                || isCloudMetadataRange(addr);
    }

    /**
     * Blocks AWS/GCP/Azure metadata IP range 169.254.169.254
     */
    private static boolean isCloudMetadataRange(InetAddress addr) {
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            return (bytes[0] & 0xFF) == 169 && (bytes[1] & 0xFF) == 254
                    && (bytes[2] & 0xFF) == 169 && (bytes[3] & 0xFF) == 254;
        }
        return false;
    }
}
