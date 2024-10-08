package com.axon.springframework.beans.factory.context.support;

import com.axon.springframework.beans.factory.ConfigurableListableBeanFactory;
import com.axon.springframework.beans.factory.config.BeanFactoryPostProcessor;
import com.axon.springframework.beans.factory.config.BeanPostProcessor;
import com.axon.springframework.beans.factory.context.ApplicationEvent;
import com.axon.springframework.beans.factory.context.ApplicationListener;
import com.axon.springframework.beans.factory.context.ConfigurableApplicationContext;
import com.axon.springframework.beans.factory.context.event.ApplicationEventMulticaster;
import com.axon.springframework.beans.factory.context.event.ContextClosedEvent;
import com.axon.springframework.beans.factory.context.event.ContextRefreshedEvent;
import com.axon.springframework.beans.factory.context.event.SimpleApplicationEventMulticaster;
import com.axon.springframework.core.io.DefaultResourceLoader;

import java.util.Collection;
import java.util.Map;

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {


    private static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";


    private ApplicationEventMulticaster applicationEventMulticaster;

    @Override
    public void refresh() {

        //1. 创建beanFactory,并加载BeanDefinition
        refreshBeanFactory();

        //2.获取beanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        //3.添加ApplicationContextAwareProcessor 类，让继承自ApplicationContextAware接口的bean对象都能感知所属的ApplicationContext
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

        //4. 在bean对象实例化之前，执行BeanFactoryPostProcessor操作
        invokeBeanFactoryPostProcessor(beanFactory);

        //5. BeanPostProcessor需要子啊将bean对象实例化之前注册
        registerBeanPostProcessor(beanFactory);

        //6.初始化事件发布者
        initApplicationEventMulticaster();

        //7.注册监听事件
        registerListeners();

        //8.提前实例化单例bean对象
        beanFactory.preInstantiateSingletons();

        //9.发布容器刷新完成事件
        finishRefresh();

    }


    protected abstract void refreshBeanFactory();


    protected abstract ConfigurableListableBeanFactory getBeanFactory();


    private void invokeBeanFactoryPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanFactoryPostProcessor> beansOfType = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
        for (BeanFactoryPostProcessor beanFactoryPostProcessor : beansOfType.values()) {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
        }

    }

    private void registerBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanPostProcessor> beansOfType = beanFactory.getBeansOfType(BeanPostProcessor.class);
        for (BeanPostProcessor beanPostProcessor : beansOfType.values()) {
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        }

    }


    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public Object getBean(String name) throws InstantiationException, IllegalAccessException {
        return getBeanFactory().getBean(name);
    }

    @Override
    public Object getBean(String name, Object... args) {
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return getBeanFactory().getBean(name, requiredType);
    }

    @Override
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public void close() {
        //容器关闭事件, 注册关闭事件
        publishEvent(new ContextClosedEvent(this));
        //销毁单列对象
        getBeanFactory().destroySingletons();

    }

    // 初始化事件的发布者
    private void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingletonBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster);

    }

    //监听事件
    private void registerListeners() {
        Collection<ApplicationListener> values = getBeansOfType(ApplicationListener.class).values();
        values.forEach(f -> {
            applicationEventMulticaster.addApplicationListener(f);
        });
    }

    // 完成刷新事件
    private void finishRefresh() {
        publishEvent(new ContextRefreshedEvent(this));
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        applicationEventMulticaster.multicastEvent(event);
    }


}
