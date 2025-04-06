package com.example.demo.Util;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component; // So Spring can manage it as a bean
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class IpAddressUtil {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" // Fallback if no headers are found
    };

    // Common localhost IP addresses
    private static final List<String> LOCALHOST_IPS = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1" // IPv6 localhost
    );


    /**
     * Extracts the client's IP address from the HttpServletRequest.
     * It checks common proxy headers first before falling back to request.getRemoteAddr().
     *
     * @param request The incoming HttpServletRequest.
     * @return The determined client IP address, or "Unknown" if it cannot be determined.
     */
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        Pattern ipPattern = Pattern.compile(
                "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$"
        );

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (StringUtils.hasText(ipList) && !"unknown".equalsIgnoreCase(ipList)) {
                if (ipList.toLowerCase().trim().startsWith("unknown")) {
                    continue;
                }

                String[] ips = ipList.split(",");
                for (String ip : ips) {
                    String cleanIp = ip.trim().split(":")[0];
                    if (ipPattern.matcher(cleanIp).matches() &&
                            !isPrivateIp(cleanIp)) {
                        return cleanIp;
                    }
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "Unknown";
    }

    private boolean isPrivateIp(String ip) {
        return ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("127.") ||
                ip.equalsIgnoreCase("0:0:0:0:0:0:0:1");
    }

    /**
     * Checks if the given IP address is a common localhost address.
     *
     * @param ip The IP address string to check.
     * @return true if the IP matches a known localhost address, false otherwise.
     */
    public boolean isLocalhost(String ip) {
        return ip != null && LOCALHOST_IPS.contains(ip);
    }
}
