/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@WebAppConfiguration
public class IntegrationTestBase
{

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected MockMvc mvc;

    @Before
    public void setUp()
            throws Exception
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    protected void withinTransaction(Runnable runnable)
    {
        new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            runnable.run();
            return new Object();
        });
    }
}
