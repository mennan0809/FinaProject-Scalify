package com.ecommerce.UserService.services.singleton;

import com.ecommerce.UserService.models.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private static SessionManager instance;

    private final Map<String, UserSession> activeSessionsByToken = new ConcurrentHashMap<>();
    private final Map<Long, String> userIdToToken = new ConcurrentHashMap<>();

    private RedisTemplate<String, UserSession> redisTemplate;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    @Autowired
    public void initRedisTemplate(RedisTemplate<String, UserSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
        getInstance().setRedisTemplate(redisTemplate);
    }

    private void setRedisTemplate(RedisTemplate<String, UserSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addSession(String token, UserSession userSession) {
        activeSessionsByToken.put(token, userSession);
        userIdToToken.put(userSession.getUserId(), token);
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(token, userSession, Duration.ofHours(6));
        }
    }

    public void invalidateToken(String token) {
        UserSession session = activeSessionsByToken.remove(token);
        if (session != null) {
            userIdToToken.remove(session.getUserId());
        }

        if (redisTemplate != null) {
            redisTemplate.delete(token);
        }
    }

    public boolean isTokenValid(String token) {

        if (!activeSessionsByToken.containsKey(token)) {
            if (redisTemplate == null || redisTemplate.opsForValue().get(token) == null) {
                return false;
            }
        } else {
            if (redisTemplate != null && redisTemplate.opsForValue().get(token) == null) {
                UserSession staleSession = activeSessionsByToken.remove(token);
                if (staleSession != null) {
                    userIdToToken.remove(staleSession.getUserId());
                }
                return false;
            }
        }

        return true;
    }

    public UserSession getSession(String token) {
        UserSession session = activeSessionsByToken.get(token);

        if (session == null) {
            if (redisTemplate != null) {
                session = redisTemplate.opsForValue().get(token);
                if (session != null) {
                    activeSessionsByToken.put(token, session);
                    userIdToToken.put(session.getUserId(), token);
                } else {
                    activeSessionsByToken.remove(token); // safety
                }
            } else {
            }
        } else {
        }

        return session;
    }

    public UserSession getSessionByUserId(Long userId) {
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null) {
                for (String key : keys) {
                    UserSession session = redisTemplate.opsForValue().get(key);
                    if (session != null && session.getUserId().equals(userId)) {
                        userIdToToken.put(userId, key);
                        activeSessionsByToken.put(key, session);
                        return session;
                    }
                }
            }
        }
        return null;
    }

    public void removeSessionByUserId(Long userId) {
        try {
            UserSession userSession = getSessionByUserId(userId);

            if (userSession == null) {
                return;
            }

            String token = userSession.getToken();

            if (redisTemplate != null) {
                redisTemplate.delete(token);
            }

            invalidateToken(token);
            // Remove from in-memory maps
            activeSessionsByToken.remove(token);
            userIdToToken.remove(userId);
        } catch (Exception e) {
            System.err.println("💥 [removeSessionByUserId] Error while removing session for userId " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


}