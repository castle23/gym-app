package com.gym.common.security;

import java.util.Optional;
import java.util.Collections;
import java.util.Set;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    public static void setContext(UserContext context) {
        if (context == null) {
            contextHolder.remove();
        } else {
            contextHolder.set(context);
        }
    }

    public static UserContext getContext() {
        return contextHolder.get();
    }

    public static Optional<UserContext> getContextOptional() {
        return Optional.ofNullable(contextHolder.get());
    }

    public static String getUserId() {
        UserContext context = contextHolder.get();
        return context != null ? context.getUserId() : null;
    }

    public static Set<String> getRoles() {
        UserContext context = contextHolder.get();
        return context != null ? context.getRoles() : Collections.emptySet();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
