package com.axon.springframework.aop;

import com.axon.springframework.utils.ClassUtils;

public class TargetSource {

    private final Object target;

    public Object getTarget() {
        return target;
    }

    public TargetSource(Object target) {
        this.target = target;
    }



    public Class<?>[] getTargetClass() {
        Class<?> aClass = this.target.getClass();
        aClass = ClassUtils.isCglibProxyClass(aClass) ? aClass.getSuperclass() : aClass;
        return aClass.getInterfaces();
    }

}
