/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import com.teradata.presto.monitor.service.category.IntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@Category(IntegrationTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PrestoMonitorApp.class)
@WebAppConfiguration
public class IntegrationTestBase
{
    @Autowired
    protected RestTemplate restTemplate;

    protected MockMvc mvc;

    protected MockRestServiceServer restServiceServer;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Before
    public void setUp()
            throws Exception
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Before
    public void initializeRestServiceServer()
    {
        restServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @After
    public void verifyRestServiceServer()
    {
        restServiceServer.verify();
    }

    @Test
    public void testContextNotNull()
    {
        // this dummy test is needed to make surefire happy
        assertThat(webApplicationContext).isNotNull();
    }

    protected void withinTransaction(Runnable runnable)
    {
        new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            runnable.run();
            return new Object();
        });
    }

    protected RequestMatcher matchAll(RequestMatcher... matchers)
    {
        return request -> {
            for (RequestMatcher matcher : matchers) {
                matcher.match(request);
            }
        };
    }
}
