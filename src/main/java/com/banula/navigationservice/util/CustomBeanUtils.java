package com.banula.navigationservice.util;

import org.springframework.beans.BeanUtils;

public class CustomBeanUtils {

    public static void copyProperties(Object origin, Object destination) {
        BeanUtils.copyProperties(origin, destination);
    }
}
