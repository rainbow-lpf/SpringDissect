package com.axon.springframework.tx.transaction.annoation;

import java.lang.annotation.*;

/**
 * 事务注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

    Class<? extends Throwable>[] rollbackFor() default {};

}
