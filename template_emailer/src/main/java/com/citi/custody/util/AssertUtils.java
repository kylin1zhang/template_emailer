package com.citi.custody.util;

import com.citi.custody.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.Map;

public class AssertUtils extends Assert {
    public static void isTrue(boolean expression, Integer code, String message) {
        if (!expression) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void isTrue(boolean expression, Integer code, String message, Boolean showMsg) {
        if (!expression) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void isFalse(boolean expression, Integer code, String message) {
        if (expression) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void isFalse(boolean expression, Integer code, String message, Boolean showMsg) {
        if (expression) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void isNull(Object object, Integer code, String message) {
        if (object != null) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void isNull(Object object, Integer code, String message, Boolean showMsg) {
        if (object != null) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void isEmpty(@Nullable Map<?, ?> map, Integer code, String message) {
        if (!CollectionUtils.isEmpty(map)) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notNull(Object object, Integer code, String message) {
        if (object == null) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notNull(Object object, Integer code, String message, Boolean showMsg) {
        if (object == null) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void notBlank(String str, Integer code, String message) {
        if (StringUtils.isBlank(str)) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notBlank(String str, Integer code, String message, Boolean showMsg) {
        if (StringUtils.isBlank(str)) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Object[] array, Integer code, String message) {
        if (ObjectUtils.isEmpty(array)) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Object[] array, Integer code, String message, Boolean showMsg) {
        if (ObjectUtils.isEmpty(array)) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Collection<?> collection, Integer code, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Collection<?> collection, Integer code, String message, Boolean showMsg) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Map<?, ?> map, Integer code, String message) {
        if (CollectionUtils.isEmpty(map)) {
            throw new BusinessException(code, message, true, new Object[]{});
        }
    }

    public static void notEmpty(@Nullable Map<?, ?> map, Integer code, String message, Boolean showMsg) {
        if (CollectionUtils.isEmpty(map)) {
            throw new BusinessException(code, message, showMsg, new Object[]{});
        }
    }

    public static void notNull(Object object, BusinessException e) {
        if (object == null) {
            throw new BusinessException(e.getErrorCode(), e.getMessage(), true, new Object[]{});
        }
    }

    public static void isTrue(boolean expression, BusinessException e) {
        if (!expression) {
            throw new BusinessException(e.getErrorCode(), e.getMessage(), true, new Object[]{});
        }
    }
}
