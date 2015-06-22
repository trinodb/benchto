/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DriverApp.class, TestConfig.class})
@org.springframework.boot.test.IntegrationTest({"executionSequenceId=BEN_SEQ_ID"})
public abstract class IntegrationTest
{
    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected ApplicationContext context;

    protected MockRestServiceServer restServiceServer;

    @Before
    public void resetMocks()
    {
        for (String name : context.getBeanDefinitionNames()) {
            Object bean = context.getBean(name);
            if (new MockUtil().isMock(bean)) {
                Mockito.reset(bean);
            }
        }
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
}
