package openclaw.security.ssrf;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default SSRF policy implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultSsrfPolicy implements SsrfPolicy {

    // Private IP ranges
    private static final Set<String> PRIVATE_IP_RANGES = Set.of(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "0.0.0.0/8",
            "fc00::/7",
            "fe80::/10",
            "::1/128"
    );

    // Blocked hostnames
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "localhost.localdomain",
            "ip6-localhost",
            "ip6-loopback"
    );

    // Allowed schemes
    private static final Set<String> ALLOWED_SCHEMES = Set.of(
            "http",
            "https"
    );

    // Blocked schemes
    private static final Set<String> BLOCKED_SCHEMES = Set.of(
            "file",
            "ftp",
            "gopher",
            "jar",
            "netdoc"
    );

    // Blocked ports
    private static final Set<Integer> BLOCKED_PORTS = Set.of(
            22,   // SSH
            23,   // Telnet
            25,   // SMTP
            110,  // POP3
            143,  // IMAP
            445,  // SMB
            3306, // MySQL
            3389, // RDP
            5432, // PostgreSQL
            6379, // Redis
            9200, // Elasticsearch
            27017 // MongoDB
    );

    private final Set<String> allowlist;
    private final Set<String> blocklist;

    public DefaultSsrfPolicy() {
        this(Set.of(), Set.of());
    }

    public DefaultSsrfPolicy(Set<String> allowlist, Set<String> blocklist) {
        this.allowlist = allowlist;
        this.blocklist = blocklist;
    }

    @Override
    public SsrfValidationResult validate(String url) {
        try {
            URI uri = new URI(url);
            return validate(uri);
        } catch (Exception e) {
            return SsrfValidationResult.blocked("Invalid URL: " + e.getMessage());
        }
    }

    @Override
    public SsrfValidationResult validate(URI uri) {
        // Check scheme
        String scheme = uri.getScheme();
        if (scheme == null) {
            return SsrfValidationResult.blocked("Missing URL scheme");
        }

        scheme = scheme.toLowerCase();

        if (BLOCKED_SCHEMES.contains(scheme)) {
            return SsrfValidationResult.blocked("Blocked scheme: " + scheme);
        }

        if (!ALLOWED_SCHEMES.contains(scheme)) {
            return SsrfValidationResult.blocked("Unsupported scheme: " + scheme);
        }

        // Check allowlist
        String host = uri.getHost();
        if (host == null) {
            return SsrfValidationResult.blocked("Missing hostname");
        }

        host = host.toLowerCase();

        // Check explicit allowlist
        if (allowlist.contains(host)) {
            return SsrfValidationResult.allowed();
        }

        // Check blocklist
        if (blocklist.contains(host)) {
            return SsrfValidationResult.blocked("Hostname in blocklist: " + host);
        }

        // Check blocked hostnames
        if (BLOCKED_HOSTNAMES.contains(host)) {
            return SsrfValidationResult.blocked(
                    "Blocked hostname: " + host,
                    RiskLevel.CRITICAL
            );
        }

        // Check port
        int port = uri.getPort();
        if (port == -1) {
            port = scheme.equals("https") ? 443 : 80;
        }

        if (BLOCKED_PORTS.contains(port)) {
            return SsrfValidationResult.blocked("Blocked port: " + port);
        }

        // Check IP address
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                String ip = address.getHostAddress();

                if (isPrivateIp(ip)) {
                    return SsrfValidationResult.blocked(
                            "Private IP address: " + ip,
                            RiskLevel.CRITICAL
                    );
                }
            }
        } catch (UnknownHostException e) {
            // Hostname resolution failed, but we'll allow it
            // The actual HTTP client will fail if it's invalid
        }

        return SsrfValidationResult.allowed();
    }

    @Override
    public boolean isIpBlocked(String ip) {
        return isPrivateIp(ip);
    }

    @Override
    public boolean isHostnameBlocked(String hostname) {
        return BLOCKED_HOSTNAMES.contains(hostname.toLowerCase());
    }

    private boolean isPrivateIp(String ip) {
        // Check for IPv4 private ranges
        if (ip.startsWith("10.") ||
            ip.startsWith("127.") ||
            ip.startsWith("0.") ||
            ip.startsWith("169.254.")) {
            return true;
        }

        // Check 172.16.0.0/12
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int second = Integer.parseInt(parts[1]);
                if (second >= 16 && second <= 31) {
                    return true;
                }
            }
        }

        // Check 192.168.0.0/16
        if (ip.startsWith("192.168.")) {
            return true;
        }

        // Check IPv6
        if (ip.startsWith("fc") || ip.startsWith("fd") ||
            ip.startsWith("fe80:") || ip.equals("::1")) {
            return true;
        }

        return false;
    }
}
