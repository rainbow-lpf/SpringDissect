package com.axon.springframework.test;

import com.axon.springframework.bean.step7.impl.UserServiceImpl;
import com.axon.springframework.beans.factory.config.BeanPostProcessor;

public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if ("userServiceImpl".equals(beanName)) {
            System.out.println("我是初始化前操作");
            UserServiceImpl userService = (UserServiceImpl) bean;
            userService.setAddress("北京天上人间");
            return userService;

        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("userServiceImpl".equals(beanName)) {
            System.out.println("我是初始后操作");
        }
        return bean;
    }
}
