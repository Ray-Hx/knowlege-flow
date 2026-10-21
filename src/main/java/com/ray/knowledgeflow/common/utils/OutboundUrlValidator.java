package com.ray.knowledgeflow.common.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 出站 HTTP URL 安全边界。阻止抓取器访问本机、内网、链路本地和元数据地址。
 */
public final class OutboundUrlValidator {

    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private OutboundUrlValidator() {
    }

    public static URI requirePublicHttpUrl(String rawUrl) {
        final URI uri;
        try {
            uri = URI.create(rawUrl).normalize();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("URL 格式无效");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!HTTP_SCHEMES.contains(scheme) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("仅支持有效的 HTTP/HTTPS URL");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL 不允许包含用户凭据");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            throw new IllegalArgumentException("不允许访问本机或内网地址");
        }

        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("URL 主机无法解析");
        }
        if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(OutboundUrlValidator::isNonPublicAddress)) {
            throw new IllegalArgumentException("不允许访问本机或内网地址");
        }
        return uri;
    }

    public static URI requireHost(String rawUrl, boolean httpsOnly, Set<String> allowedHosts) {
        URI uri = requirePublicHttpUrl(rawUrl);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(host::equals);
        if (!allowed || (httpsOnly && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("URL 不属于允许的服务域名");
        }
        return uri;
    }

    private static boolean isNonPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            // 0.0.0.0/8 以及运营商级 NAT 100.64.0.0/10 不应被服务端抓取器访问。
            return first == 0 || (first == 100 && second >= 64 && second <= 127);
        }
        // IPv6 ULA fc00::/7。
        return bytes.length == 16 && ((bytes[0] & 0xfe) == 0xfc);
    }
}
