package com.bugstack.springframework.test.step19;

import com.alibaba.druid.pool.DruidDataSource;
import com.bugstack.springframework.aop.AdvisedSupport;
import com.bugstack.springframework.aop.TargetSource;
import com.bugstack.springframework.aop.aspectj.AspectJExpressionPointcut;
import com.bugstack.springframework.aop.framework.Cglib2AopProxy;
import com.bugstack.springframework.beans.factory.context.support.ClassPathXmlApplicationContext;
import com.bugstack.springframework.jdbc.core.JdbcTemplate;
import com.bugstack.springframework.jdbc.datasource.DataSourceTransactionManager;
import com.bugstack.springframework.tx.transaction.annoation.AnnotationTransactionAttributeSource;
import com.bugstack.springframework.tx.transaction.interceptor.TransactionInterceptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.sql.DataSource;

/**
 * 创建事务的核心逻辑流程
 * 前提：事务主要是通过代理去执行的
 * 1.声明式的注解 @Transtional
 * 2.切面获取带有  @Transtional的方法
 * 3.在执行目标方法前，会获取到一个数据库链接存放到 ThreadLocal中， 在ThreadLoalMap中key，和value ,key是dataSource, value是Contenction
 * 4.每次执行sql前，都会获取一个数据库链接， 获取时都从当前线程中的TreadLocal中去获取，多条sql语句。
 * 5.执行时遇到异常，则回滚，否则提交完成。
 * <p>
 * chatGpt总结：
 * 你的事务创建流程的理解基本准确，但可以稍微补充和调整一些细节来更清晰地描述 Spring 声明式事务的核心逻辑。这里是调整后的详细说明：
 * <p>
 * 1. 声明式的注解 @Transactional：
 * <p>
 * •	通过 @Transactional 注解，开发者声明了哪些方法应该被事务管理。
 * •	Spring AOP 拦截所有被 @Transactional 注解标注的方法，通过事务管理器来处理事务逻辑。
 * <p>
 * 2. 切面获取带有 @Transactional 的方法：
 * <p>
 * •	Spring AOP 使用事务拦截器（TransactionInterceptor）来处理方法调用前后的事务逻辑。切面会检测方法是否有 @Transactional 注解，并应用事务逻辑。
 * •	通过代理模式（CGLIB 或 JDK 动态代理）拦截方法调用。
 * <p>
 * 3. 在执行目标方法前，会获取到一个数据库连接，存放到 ThreadLocal 中：
 * <p>
 * •	在方法执行之前，Spring 会检查事务的上下文（TransactionContext）。如果没有现有事务，Spring 会从数据源（DataSource）获取数据库连接，并将其存放到 ThreadLocal 中，确保在同一线程中的操作使用相同的数据库连接。
 * •	ThreadLocal 用于保存当前线程的数据库连接（Connection）。这意味着在同一事务范围内，同一线程会复用这个数据库连接。
 * <p>
 * 4. 每次执行 SQL 前，从当前线程的 ThreadLocal 中获取数据库连接：
 * <p>
 * •	当方法中执行 SQL 操作时，Spring 会通过 JDBC Template 或 ORM 框架获取数据库连接。
 * •	如果是事务环境，连接会从 ThreadLocal 中取出，而不是每次都从连接池中重新获取。这样确保了在事务中，多条 SQL 语句使用的是同一个连接。
 * <p>
 * 5. 遇到异常则回滚，否则提交：
 * <p>
 * •	在事务方法执行时，Spring 会监控方法中的异常情况。
 * •	如果捕捉到 RuntimeException 或 Error（或通过 rollbackFor 配置的异常），Spring 将标记事务为回滚状态。
 * •	在方法执行结束后，Spring 会根据事务的状态决定是提交还是回滚。
 * •	如果事务状态是正常的，Spring 会提交事务，释放连接。
 * •	如果事务状态是回滚的，Spring 会回滚事务，释放连接。
 * <p>
 * 进一步补充：
 * <p>
 * 1.	事务的传播行为：
 * •	当一个事务方法调用另一个事务方法时，Spring 会根据 @Transactional 注解中定义的传播行为（如 REQUIRED、REQUIRES_NEW 等）来决定是否创建新事务或加入现有事务。
 * 2.	事务隔离级别：
 * •	@Transactional 注解中可以定义事务的隔离级别，控制并发事务的可见性和相互干扰。
 * 3.	事务超时：
 * •	事务可以设置超时时间，防止长时间未提交或回滚的事务阻塞资源。
 * 4.	只读事务：
 * •	通过 @Transactional(readOnly = true) 声明事务为只读，Spring 可以进行优化，防止修改操作。
 * <p>
 * 小结：
 * <p>
 * 你的理解是正确的，但补充了 Spring 框架处理事务时一些更具体的细节，如事务传播、异常处理、隔离级别等。Spring 通过 ThreadLocal 来管理事务的生命周期，并确保在一个事务中使用相同的数据库连接，最终根据执行结果决定提交还是回滚。
 */
public class ApiTest {

    private JdbcTemplate jdbcTemplate;

    private JdbcService jdbcService;

    private DataSource dataSource;

    @BeforeTest
    public void init() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        jdbcService = applicationContext.getBean(JdbcService.class);
        dataSource = applicationContext.getBean(DruidDataSource.class);
        jdbcTemplate = applicationContext.getBean(JdbcTemplate.class);
    }

    /**
     * 核心方法逻辑在  TransactionAspectSupport-> invokeWithinTransaction 方法中， 请查看这个方法的逻辑。容易理解整个事务的流程
     */
    @Test
    public void test() {

        AnnotationTransactionAttributeSource annotationTransactionAttributeSource = new AnnotationTransactionAttributeSource();
        annotationTransactionAttributeSource.findTransactionAttribute(jdbcService.getClass());

        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);

        //事务的切面， 实际的代理类去操作
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(dataSourceTransactionManager, annotationTransactionAttributeSource);

        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTargetSource(new TargetSource(jdbcService));
        advisedSupport.setInterceptor(transactionInterceptor);

        advisedSupport.setMethodMatcher(new AspectJExpressionPointcut("execution(* com.bugstack.springframework.test.step19.JdbcService.*(..))"));
        //创建代理对象
        JdbcService proxy_cglib = (JdbcService) new Cglib2AopProxy(advisedSupport).getProxy();
        proxy_cglib.saveData(jdbcTemplate);

    }

}
