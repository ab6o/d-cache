package com.github.httpcache.dynamodb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Utility for generating cache keys from HTTP requests.
 */
public class CacheKeyGenerator {
    
    private CacheKeyGenerator() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Generates a simple cache key from the URL.
     *
     * @param url The URL
     * @return The cache key
     */
    public static String generateSimpleKey(String url) {
        return url;
    }
    
    /**
     * Generates a cache key from the URL with tenant and country code.
     *
     * @param url The URL
     * @param tenant The tenant identifier, or null if not used
     * @param countryCode The country code, or null if not used
     * @return The cache key
     */
    public static String generateKeyWithTenantAndCountry(String url, String tenant, String countryCode) {
        StringBuilder key = new StringBuilder();
        
        if (tenant != null) {
            key.append("tenant:").append(tenant).append(":");
        }
        
        if (countryCode != null) {
            key.append("country:").append(countryCode).append(":");
        }
        
        key.append(url);
        return key.toString();
    }
    
    /**
     * Generates a cache key from the URL and headers.
     *
     * @param url The URL
     * @param headers The headers
     * @return The cache key
     */
    public static String generateKeyWithHeaders(String url, Map<String, String> headers) {
        // Sort headers by key to ensure consistent key generation
        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        
        StringBuilder key = new StringBuilder(url);
        key.append("?");
        
        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            key.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        
        return key.toString();
    }
    
    /**
     * Generates a cache key from the URL, headers, tenant, and country code.
     *
     * @param url The URL
     * @param headers The headers
     * @param tenant The tenant identifier, or null if not used
     * @param countryCode The country code, or null if not used
     * @return The cache key
     */
    public static String generateKeyWithHeadersAndTenant(String url, Map<String, String> headers, 
                                                         String tenant, String countryCode) {
        // Sort headers by key to ensure consistent key generation
        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        
        StringBuilder key = new StringBuilder();
        
        if (tenant != null) {
            key.append("tenant:").append(tenant).append(":");
        }
        
        if (countryCode != null) {
            key.append("country:").append(countryCode).append(":");
        }
        
        key.append(url).append("?");
        
        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            key.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        
        return key.toString();
    }
    
    /**
     * Generates a cache key from the URL, method, and headers.
     *
     * @param url The URL
     * @param method The HTTP method
     * @param headers The headers
     * @return The cache key
     */
    public static String generateKeyWithMethod(String url, String method, Map<String, String> headers) {
        // Sort headers by key to ensure consistent key generation
        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        
        StringBuilder key = new StringBuilder(method);
        key.append(":").append(url).append("?");
        
        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            key.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        
        return key.toString();
    }
    
    /**
     * Generates a cache key from the URL, method, headers, tenant, and country code.
     *
     * @param url The URL
     * @param method The HTTP method
     * @param headers The headers
     * @param tenant The tenant identifier, or null if not used
     * @param countryCode The country code, or null if not used
     * @return The cache key
     */
    public static String generateKeyWithMethodAndTenant(String url, String method, Map<String, String> headers,
                                                       String tenant, String countryCode) {
        // Sort headers by key to ensure consistent key generation
        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        
        StringBuilder key = new StringBuilder();
        
        if (tenant != null) {
            key.append("tenant:").append(tenant).append(":");
        }
        
        if (countryCode != null) {
            key.append("country:").append(countryCode).append(":");
        }
        
        key.append(method).append(":").append(url).append("?");
        
        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            key.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        
        return key.toString();
    }
    
    /**
     * Generates a cache key from the URL and query parameters.
     *
     * @param baseUrl The base URL without query parameters
     * @param queryParams The query parameters
     * @return The cache key
     */
    public static String generateKeyWithQueryParams(String baseUrl, Map<String, String> queryParams) {
        // Sort query parameters by key for consistency
        String paramString = queryParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        
        return baseUrl + (paramString.isEmpty() ? "" : "?" + paramString);
    }
    
    /**
     * Generates a hashed cache key from a full key.
     *
     * @param fullKey The full key to hash
     * @return The hashed cache key
     */
    public static String generateHashedKey(String fullKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fullKey.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate hashed cache key", e);
        }
    }
    
    /**
     * Parses query parameters from a URL.
     *
     * @param url The URL
     * @return A map of query parameters
     */
    public static Map<String, String> parseQueryParams(String url) {
        Map<String, String> params = new TreeMap<>();
        
        int queryStartPos = url.indexOf('?');
        if (queryStartPos == -1 || queryStartPos == url.length() - 1) {
            return params;
        }
        
        String queryString = url.substring(queryStartPos + 1);
        String[] pairs = queryString.split("&");
        
        for (String pair : pairs) {
            int equalsPos = pair.indexOf('=');
            if (equalsPos == -1) {
                // Handle param without value
                params.put(pair, "");
            } else {
                String key = pair.substring(0, equalsPos);
                String value = equalsPos < pair.length() - 1 ? pair.substring(equalsPos + 1) : "";
                params.put(key, value);
            }
        }
        
        return params;
    }
} 