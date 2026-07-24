package com.project.aspect;

import com.project.annotation.SystemAuditLog;
import com.project.entity.mysql.AuditLog;
import com.project.repository.mysql.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.project.annotation.SystemAuditLog)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SystemAuditLog auditLogAnnotation = method.getAnnotation(SystemAuditLog.class);

        String module = auditLogAnnotation.module();
        String action = auditLogAnnotation.action();
        String actionType = auditLogAnnotation.actionType();

        Map<String, Object> requestParams = getRequestParams(joinPoint);
        String requestParamsJson = "";
        try {
            requestParamsJson = objectMapper.writeValueAsString(requestParams);
        } catch (Exception e) {
            requestParamsJson = requestParams.toString();
        }

        String ipAddress = getClientIpAddress();
        Long userId = getCurrentUserId();
        String username = getCurrentUsername();

        Object result = null;
        String resultJson = "";
        String errorMessage = "";
        boolean success = true;

        try {
            result = joinPoint.proceed();
            try {
                resultJson = objectMapper.writeValueAsString(result);
                if (resultJson.length() > 2000) {
                    resultJson = resultJson.substring(0, 2000) + "...(truncated)";
                }
            } catch (Exception e) {
                resultJson = String.valueOf(result);
            }
            return result;
        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            saveAuditLogAsync(module, action, actionType, userId, username, requestParamsJson, resultJson, errorMessage, ipAddress, duration, success);
        }
    }

    private Map<String, Object> getRequestParams(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        if (paramNames != null && paramValues != null) {
            for (int i = 0; i < paramNames.length; i++) {
                Object value = paramValues[i];
                if (value != null && !isSensitiveType(value.getClass())) {
                    try {
                        String jsonValue = objectMapper.writeValueAsString(value);
                        if (jsonValue.length() > 1000) {
                            params.put(paramNames[i], "[Large Object]");
                        } else {
                            params.put(paramNames[i], value);
                        }
                    } catch (Exception e) {
                        params.put(paramNames[i], value.toString());
                    }
                } else {
                    params.put(paramNames[i], "[Sensitive/Stream]");
                }
            }
        }
        return params;
    }

    private boolean isSensitiveType(Class<?> clazz) {
        String className = clazz.getName();
        return className.contains("HttpServletRequest")
                || className.contains("HttpServletResponse")
                || className.contains("MultipartFile")
                || className.contains("InputStream")
                || className.contains("OutputStream");
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception e) {
            logger.error("获取客户端IP失败", e);
        }
        return "unknown";
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return getUserIdFromUsername(userDetails.getUsername());
            }
        } catch (Exception e) {
            logger.error("获取当前用户ID失败", e);
        }
        return null;
    }

    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return userDetails.getUsername();
            }
        } catch (Exception e) {
            logger.error("获取当前用户名失败", e);
        }
        return null;
    }

    private Long getUserIdFromUsername(String username) {
        return null;
    }

    private void saveAuditLogAsync(String module, String action, String actionType, Long userId, String username,
                                   String requestParams, String result, String errorMessage, String ipAddress,
                                   long duration, boolean success) {
        CompletableFuture.runAsync(() -> {
            try {
                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setUsername(username);
                auditLog.setModule(module);
                auditLog.setAction(action);
                auditLog.setActionType(actionType);
                auditLog.setRequestParams(requestParams);
                auditLog.setResult(success ? result : ("ERROR: " + errorMessage));
                auditLog.setIpAddress(ipAddress);
                auditLog.setDuration(duration);
                auditLog.setDetail(action);

                auditLogRepository.save(auditLog);
                logger.debug("审计日志保存成功: module={}, action={}, userId={}", module, action, userId);
            } catch (Exception e) {
                logger.error("保存审计日志失败", e);
            }
        });
    }
}