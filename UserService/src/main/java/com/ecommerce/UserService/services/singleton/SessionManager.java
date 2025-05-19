package com.ecommerce.UserService.services.singleton;

import com.ecommerce.UserService.models.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SessionManager {

    private static SessionManager instance;

    private final Map<String, UserSession> activeSessionsByToken = new HashMap<>();
    private final Map<Long, String> userIdToToken = new HashMap<>();

    private RedisTemplate<String, UserSession> redisTemplate;

    private SessionManager() {
        System.out.println("⚙️ [SessionManager] Singleton instance created");
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
            System.out.println("🚀 [getInstance] New SessionManager instance initialized");
        }
        return instance;
    }

    @Autowired
    public void initRedisTemplate(RedisTemplate<String, UserSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
        getInstance().setRedisTemplate(redisTemplate);
        System.out.println("🔗 [initRedisTemplate] RedisTemplate wired into singleton");
    }

    private void setRedisTemplate(RedisTemplate<String, UserSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
        System.out.println("📦 [setRedisTemplate] RedisTemplate assigned");
    }

    public void addSession(String token, UserSession userSession) {
        System.out.println("➕ [addSession] Adding new session: token = " + token + ", userId = " + userSession.getUserId());
        activeSessionsByToken.put(token, userSession);
        userIdToToken.put(userSession.getUserId(), token);
        System.out.println("🗃️ [addSession] userIdToToken map updated: " + userIdToToken);
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(token, userSession, Duration.ofHours(6));
            System.out.println("💾 [addSession] Session cached in Redis for 6h");
        } else {
            System.out.println("❗ [addSession] RedisTemplate is null — skipping Redis store");
        }
    }

    public void invalidateToken(String token) {
        System.out.println("🚫 [invalidateToken] Invalidating token: " + token);
        UserSession session = activeSessionsByToken.remove(token);
        if (session != null) {
            System.out.println("🧽 [invalidateToken] Removed from activeSessionsByToken: userId = " + session.getUserId());
            userIdToToken.remove(session.getUserId());
            System.out.println("🧹 [invalidateToken] Cleaned up userIdToToken");
        } else {
            System.out.println("⚠️ [invalidateToken] No session found for token: " + token);
        }

        if (redisTemplate != null) {
            redisTemplate.delete(token);
            System.out.println("🗑️ [invalidateToken] Token deleted from Redis");
        } else {
            System.out.println("❗ [invalidateToken] RedisTemplate is null — skipping Redis delete");
        }
    }

    public boolean isTokenValid(String token) {
        System.out.println("🕵️‍♀️ [isTokenValid] Checking token validity: " + token);

        if (!activeSessionsByToken.containsKey(token)) {
            System.out.println("🔍 [isTokenValid] Not in memory");
            if (redisTemplate == null || redisTemplate.opsForValue().get(token) == null) {
                System.out.println("❌ [isTokenValid] Token not in Redis either — INVALID");
                return false;
            } else {
                System.out.println("✅ [isTokenValid] Token found in Redis");
            }
        } else {
            if (redisTemplate != null && redisTemplate.opsForValue().get(token) == null) {
                System.out.println("⚠️ [isTokenValid] Token in memory but expired in Redis — cleaning up");
                UserSession staleSession = activeSessionsByToken.remove(token);
                if (staleSession != null) {
                    userIdToToken.remove(staleSession.getUserId());
                    System.out.println("🧽 [isTokenValid] Cleaned stale session from userIdToToken");
                }
                return false;
            } else {
                System.out.println("✅ [isTokenValid] Token is valid (memory + Redis OK)");
            }
        }

        return true;
    }

    public UserSession getSession(String token) {
        System.out.println("📥 [getSession] Fetching session for token: " + token);
        UserSession session = activeSessionsByToken.get(token);

        if (session == null) {
            System.out.println("🔍 [getSession] Not found in memory — checking Redis");
            if (redisTemplate != null) {
                session = redisTemplate.opsForValue().get(token);
                if (session != null) {
                    activeSessionsByToken.put(token, session);
                    userIdToToken.put(session.getUserId(), token);
                    System.out.println("📦 [getSession] Session rehydrated from Redis: " + session);
                } else {
                    System.out.println("❌ [getSession] Token not found in Redis either");
                    activeSessionsByToken.remove(token); // safety
                }
            } else {
                System.out.println("❗ [getSession] RedisTemplate is null");
            }
        } else {
            System.out.println("✅ [getSession] Found in memory: " + session);
        }

        return session;
    }

    public UserSession getSessionByUserId(Long userId) {
        System.out.println("🔑 [getSessionByUserId] Getting session for userId: " + userId);
        String token = userIdToToken.get(userId);

        if (token != null) {
            System.out.println("✅ [getSessionByUserId] Found token in memory: " + token);
            return activeSessionsByToken.get(token);
        }

        System.out.println("⚠️ [getSessionByUserId] Token not in memory, trying Redis lookup...");

        if (redisTemplate != null) {
            // 🔥 Brute force search: check Redis for all sessions and match by userId
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null) {
                for (String key : keys) {
                    UserSession session = redisTemplate.opsForValue().get(key);
                    if (session != null && session.getUserId().equals(userId)) {
                        System.out.println("✅ [getSessionByUserId] Found session in Redis with key: " + key);
                        // Update maps for next time
                        userIdToToken.put(userId, key);
                        activeSessionsByToken.put(key, session);
                        return session;
                    }
                }
            }
        }

        System.out.println("❌ [getSessionByUserId] No token found for userId: " + userId);
        return null;
    }

    public void removeSessionByUserId(Long userId) {
        try {
            System.out.println("🚨 [removeSessionByUserId] Requested session removal for userId: " + userId);

            UserSession userSession = getSessionByUserId(userId);
            if (userSession == null) {
                System.out.println("❌ [removeSessionByUserId] No active session found for userId: " + userId);
                return;
            }

            String token = userSession.getToken();
            System.out.println("🔐 [removeSessionByUserId] Found session with token: " + token);

            // Remove from Redis
            if (redisTemplate != null) {
                redisTemplate.delete(token);
                System.out.println("🗑️ [removeSessionByUserId] Deleted token from Redis cache");
            } else {
                System.out.println("⚠️ [removeSessionByUserId] RedisTemplate is null — skipped Redis deletion");
            }

            // Remove from in-memory maps
            activeSessionsByToken.remove(token);
            userIdToToken.remove(userId);
            System.out.println("🧹 [removeSessionByUserId] Removed session from activeSessionsByToken and userIdToToken");
        } catch (Exception e) {
            System.err.println("💥 [removeSessionByUserId] Error while removing session for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


}