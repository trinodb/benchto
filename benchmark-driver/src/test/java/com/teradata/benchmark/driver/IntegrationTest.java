package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.jdbc.MultipleDataSourcesAutoConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DriverApp.class, TestConfig.class, MultipleDataSourcesAutoConfiguration.class})
@org.springframework.boot.test.IntegrationTest({"executionSequenceId=BEN_SEQ_ID", "runs=2"})
public abstract class IntegrationTest
{
    @Autowired
    protected RestTemplate restTemplate;

    protected MockRestServiceServer restServiceServer;

    @Before
    public void initializeRestServiceServer()
    {
        restServiceServer = MockRestServiceServer.createServer(restTemplate);
    }
}
