package com.github.httpcache.dynamodb.annotation;

import com.github.httpcache.dynamodb.HttpCache;
import com.github.httpcache.dynamodb.HttpCacheConfig;
import com.github.httpcache.dynamodb.HttpRequest;
import com.github.httpcache.dynamodb.HttpResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Aspect that handles the {@link Cacheable} and {@link CacheEvict} annotations.
 */
@Aspect
@Component
public class CacheableAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheableAspect.class);
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    
    private final HttpCache httpCache;
    
    @Autowired
    public CacheableAspect(HttpCache httpCache) {
        this.httpCache = httpCache;
    }
    
    /**
     * Around advice for methods annotated with {@link Cacheable}.
     */
    @Around("@annotation(cacheable)")
    public Object cacheableAdvice(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        // Check if we should bypass the cache
        if (cacheable.bypass() || !evaluateCondition(cacheable.condition(), paramNames, args)) {
            return joinPoint.proceed();
        }
        
        // Generate cache key
        String cacheKey = generateCacheKey(cacheable, method, paramNames, args);
        
        try {
            // Determine if we should use headers in the key based on the cache key strategy
            boolean includeHeaders = cacheable.keyStrategy() == CacheKeyStrategyType.WITH_HEADERS || 
                                    cacheable.keyStrategy() == CacheKeyStrategyType.WITH_METHOD;
            
            // Extract HTTP URL from args if present
            String url = extractUrl(args);
            if (url == null) {
                LOGGER.warn("Could not extract URL from method arguments, using method name as URL");
                url = method.getDeclaringClass().getName() + "." + method.getName();
            }
            
            // Prepare request options
            Consumer<HttpRequest.Builder> options = builder -> {
                if (cacheable.ttl() > 0) {
                    builder.ttl(cacheable.ttl());
                }
                
                // Add headers if specified
                if (cacheable.includeHeaders().length > 0) {
                    Map<String, String> headers = extractHeaders(args);
                    for (String headerName : cacheable.includeHeaders()) {
                        String headerValue = headers.get(headerName);
                        if (headerValue != null) {
                            builder.header(headerName, headerValue);
                        }
                    }
                }
            };
            
            // Try to fetch from cache
            HttpResponse response = httpCache.fetch(url, options);
            
            if (response.isFromCache()) {
                LOGGER.debug("Cache hit for key: {}", cacheKey);
                return convertResponseToReturnType(response, method.getReturnType());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to fetch from cache, proceeding with method execution", e);
        }
        
        // Cache miss or error, proceed with method execution
        Object result = joinPoint.proceed();
        
        // Cache the result if it's an HttpResponse
        if (result instanceof HttpResponse) {
            LOGGER.debug("Caching result for key: {}", cacheKey);
            // The result is already cached by httpCache.fetch() so we don't need to do anything
        }
        
        return result;
    }
    
    /**
     * Around advice for methods annotated with {@link CacheEvict}.
     */
    @Around("@annotation(cacheEvict)")
    public Object cacheEvictAdvice(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        // Check condition before execution if needed
        if (cacheEvict.beforeInvocation() && !evaluateCondition(cacheEvict.condition(), paramNames, args)) {
            return joinPoint.proceed();
        }
        
        // Evict cache before method execution if requested
        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, method, paramNames, args);
        }
        
        // Execute the method
        Object result = joinPoint.proceed();
        
        // Check condition after execution if needed
        if (!cacheEvict.beforeInvocation()) {
            // Add the result to the context for condition evaluation
            Map<String, Object> contextMap = createContextMap(paramNames, args);
            contextMap.put("result", result);
            
            if (!StringUtils.hasText(cacheEvict.condition()) || 
                evaluateExpression(cacheEvict.condition(), contextMap, Boolean.class)) {
                evictCache(cacheEvict, method, paramNames, args);
            }
        }
        
        return result;
    }
    
    private void evictCache(CacheEvict cacheEvict, Method method, String[] paramNames, Object[] args) {
        try {
            if (cacheEvict.allEntries()) {
                LOGGER.debug("Evicting all cache entries");
                httpCache.invalidateAll();
            } else {
                String cacheKey = generateCacheKey(cacheEvict.key(), method, paramNames, args, cacheEvict.includeParams(), cacheEvict.excludeParams());
                String url = extractUrl(args);
                if (url == null) {
                    url = cacheKey;
                }
                LOGGER.debug("Evicting cache entry with key: {}", cacheKey);
                httpCache.invalidate(url);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to evict cache", e);
        }
    }
    
    private String generateCacheKey(Cacheable cacheable, Method method, String[] paramNames, Object[] args) {
        return generateCacheKey(cacheable.key(), method, paramNames, args, cacheable.includeParams(), cacheable.excludeParams());
    }
    
    private String generateCacheKey(String key, Method method, String[] paramNames, Object[] args, 
                                   boolean includeParams, String[] excludeParams) {
        // If a specific key is provided, use it (evaluate if it's an expression)
        if (StringUtils.hasText(key)) {
            Map<String, Object> contextMap = createContextMap(paramNames, args);
            return evaluateExpression(key, contextMap, String.class);
        }
        
        // Otherwise, generate a key based on method and parameters
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getDeclaringClass().getName()).append(".").append(method.getName());
        
        if (includeParams && args.length > 0) {
            keyBuilder.append("(");
            
            // Add parameters to the key
            for (int i = 0; i < args.length; i++) {
                String paramName = paramNames[i];
                
                // Skip excluded parameters
                if (Arrays.asList(excludeParams).contains(paramName)) {
                    continue;
                }
                
                if (i > 0) {
                    keyBuilder.append(",");
                }
                
                // Add parameter value to key
                keyBuilder.append(paramName).append("=");
                if (args[i] == null) {
                    keyBuilder.append("null");
                } else {
                    keyBuilder.append(args[i].toString());
                }
            }
            
            keyBuilder.append(")");
        }
        
        return keyBuilder.toString();
    }
    
    private boolean evaluateCondition(String condition, String[] paramNames, Object[] args) {
        if (!StringUtils.hasText(condition)) {
            return true; // No condition means always apply
        }
        
        Map<String, Object> contextMap = createContextMap(paramNames, args);
        return evaluateExpression(condition, contextMap, Boolean.class);
    }
    
    private <T> T evaluateExpression(String expressionString, Map<String, Object> contextMap, Class<T> resultType) {
        try {
            Expression expression = EXPRESSION_PARSER.parseExpression(expressionString);
            EvaluationContext context = new StandardEvaluationContext();
            
            // Add variables to the context
            contextMap.forEach((name, value) -> {
                if (context instanceof StandardEvaluationContext) {
                    ((StandardEvaluationContext) context).setVariable(name, value);
                }
            });
            
            return expression.getValue(context, resultType);
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate expression: {}", expressionString, e);
            return resultType.equals(Boolean.class) ? resultType.cast(false) : null;
        }
    }
    
    private Map<String, Object> createContextMap(String[] paramNames, Object[] args) {
        Map<String, Object> contextMap = new HashMap<>();
        
        // Add all method parameters to the context
        IntStream.range(0, args.length)
                .forEach(i -> contextMap.put(paramNames[i], args[i]));
        
        return contextMap;
    }
    
    private String extractUrl(Object[] args) {
        // Try to find a String that looks like a URL in the arguments
        for (Object arg : args) {
            if (arg instanceof String && ((String) arg).startsWith("http")) {
                return (String) arg;
            }
        }
        return null;
    }
    
    private Map<String, String> extractHeaders(Object[] args) {
        // Try to find a Map<String, String> in the arguments that might be headers
        for (Object arg : args) {
            if (arg instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) arg;
                return map;
            }
        }
        return new HashMap<>();
    }
    
    private Object convertResponseToReturnType(HttpResponse response, Class<?> returnType) {
        // If the method returns HttpResponse, return it directly
        if (HttpResponse.class.isAssignableFrom(returnType)) {
            return response;
        }
        
        // If it returns a String, convert the response body to String
        if (String.class.isAssignableFrom(returnType)) {
            return new String(response.getBody());
        }
        
        // If it returns a byte array, return the response body
        if (byte[].class.isAssignableFrom(returnType)) {
            return response.getBody();
        }
        
        // Otherwise, try to deserialize the JSON response
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.getBody(), returnType);
        } catch (Exception e) {
            LOGGER.warn("Failed to convert response to return type: {}", returnType.getName(), e);
            return null;
        }
    }
} 