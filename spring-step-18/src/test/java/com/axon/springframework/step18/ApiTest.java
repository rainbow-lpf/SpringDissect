package com.axon.springframework.step18;

import com.axon.springframework.beans.factory.context.support.ClassPathXmlApplicationContext;
import com.axon.springframework.jdbc.core.JdbcTemplate;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class ApiTest {


    private JdbcTemplate jdbcTemplate;

    @BeforeTest
    public void before() {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        jdbcTemplate = classPathXmlApplicationContext.getBean("jdbcTemplate", JdbcTemplate.class);
    }


    @Test
    public void execute(){
        jdbcTemplate.execute("insert into account (name, balance) values ('184172133','1000') ");
    }


    @Test
    public void queryForListTest() {
        List<Map<String, Object>> allResult = jdbcTemplate.queryForList("select * from account");
        for (Map<String, Object> objectMap : allResult) {
            System.out.println("测试结果：" + objectMap);
        }
    }




}
