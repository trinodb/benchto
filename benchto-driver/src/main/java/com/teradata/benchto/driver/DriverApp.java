/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.benchto.driver;

import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.ExecutionDriver;
import freemarker.template.TemplateException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkState;

@Configuration
@EnableRetry
@EnableAutoConfiguration(exclude = {
        FreeMarkerAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@ComponentScan(basePackages = "com.teradata.benchto")
public class DriverApp
{

    private static final Logger LOG = LoggerFactory.getLogger(DriverApp.class);

    public static void main(String[] args)
            throws Exception
    {
        CommandLine commandLine = processArguments(args);

        SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(DriverApp.class)
                .web(false)
                .registerShutdownHook(false)
                .properties();
        if (commandLine.hasOption("profile")) {
            applicationBuilder.profiles(commandLine.getOptionValue("profile"));
        }

        try (ConfigurableApplicationContext ctx = applicationBuilder.run()) {
            ExecutionDriver executionDriver = ctx.getBean(ExecutionDriver.class);
            Thread.currentThread().setName("main");
            executionDriver.execute();
        }
        catch (Throwable e) {
            logException(e);
            System.exit(1);
        }
    }

    private static CommandLine processArguments(String[] args)
            throws ParseException
    {
        DefaultParser defaultParser = new DefaultParser();
        Options options = createOptions();
        CommandLine commandLine = defaultParser.parse(options, args);
        if (commandLine.hasOption("h")) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("Benchto driver", options);
            System.exit(0);
        }
        exposeArgumentsAsPropertiesForSpring(commandLine);
        checkState(commandLine.getArgList().isEmpty(), "Added extra non used arguments: %s", commandLine.getArgList());
        return commandLine;
    }

    private static void exposeArgumentsAsPropertiesForSpring(CommandLine commandLine)
    {
        for (Option option : commandLine.getOptions()) {
            System.setProperty(option.getLongOpt(), option.getValue());
        }
    }

    private static Options createOptions()
            throws ParseException
    {
        Options options = new Options();
        addOption(options, "sql", "DIR", "sql queries directory", "sql");
        addOption(options, "benchmarks", "DIR", "benchmark descriptors directory", "benchmarks");
        addOption(options, "activeBenchmarks", "BENCHMARK_NAME,...", "list of active benchmarks", "all benchmarks");
        addOption(options, "activeVariables", "VARIABLE_NAME=VARIABLE_VALUE,...", "list of active variables", "no filtering by variables");
        addOption(options, "executionSequenceId", "SEQUENCE_ID", "sequence id of benchmark execution", "generated");
        addOption(options, "timeLimit", "DURATION", "amount of time while benchmarks will be executed", "unlimited");
        addOption(options, "profile", "PROFILE", "configuration profile", "none");
        addOption(options, "frequencyCheckEnabled", "boolean", "if set no fresh benchmark will be executed", "true");
        options.addOption("h", "help", false, "Display help message.");
        return options;
    }

    private static void addOption(Options options, String longOption, String arg, String description, String defaultValue)
    {
        options.addOption(Option.builder()
                .longOpt(longOption)
                .hasArg()
                .desc(String.format("%s - %s (default: %s).", arg, description, defaultValue))
                .build());
    }

    private static void logException(Throwable e)
    {
        LOG.error("Benchmark execution failed: {}", e.getMessage(), e);
        if (e instanceof FailedBenchmarkExecutionException) {
            FailedBenchmarkExecutionException failedBenchmarkExecutionException = (FailedBenchmarkExecutionException) e;
            for (BenchmarkExecutionResult failedBenchmarkResult : failedBenchmarkExecutionException.getFailedBenchmarkResults()) {
                LOG.error("--------------------------------------------------------------------------");
                LOG.error("Failed benchmark: {}", failedBenchmarkResult.getBenchmark().getUniqueName());
                for (Exception failureCause : failedBenchmarkResult.getFailureCauses()) {
                    LOG.error("Cause: {}", failureCause.getMessage(), failureCause);
                }
            }
            LOG.error("Total benchmarks failed {} out of {}",
                    failedBenchmarkExecutionException.getFailedBenchmarkResults().size(),
                    failedBenchmarkExecutionException.getBenchmarksCount());
        }
    }

    @Bean
    public RestTemplate restTemplate()
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    @Bean
    public ThreadPoolTaskExecutor defaultTaskExecutor()
    {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(300);

        return taskExecutor;
    }

    @Bean
    public FreeMarkerConfigurationFactoryBean freemarkerConfiguration()
            throws IOException, TemplateException
    {
        FreeMarkerConfigurationFactoryBean factory = new FreeMarkerConfigurationFactoryBean();
        factory.setDefaultEncoding("UTF-8");
        return factory;
    }
}
