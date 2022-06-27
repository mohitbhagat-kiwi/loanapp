package com.loanapp.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${broker.host}")
    private String brokerHostAndPort;

    @Value("${bankA.host}")
    private String bankAHostAndPort;

    @Value("${bankB.host}")
    private String bankBHostAndPort;

    @Value("${creditBureau.host}")
    private String creditBureauHostAndPort;

    @Value("${evaluationBureau.host}")
    private String evaluationBureauHostAndPort;

    @Bean(destroyMethod = "")  // Avoids node shutdown on rpc disconnect
    public CordaRPCOps brokerProxy(){
        CordaRPCClient brokerClient = new CordaRPCClient(NetworkHostAndPort.parse(brokerHostAndPort),
                new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return brokerClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps bankAProxy(){
        CordaRPCClient bankAClient = new CordaRPCClient(NetworkHostAndPort.parse(bankAHostAndPort),
                new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return bankAClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps bankBProxy(){
        CordaRPCClient bankBClient = new CordaRPCClient(NetworkHostAndPort.parse(bankBHostAndPort),
                new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return bankBClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps creditBureauProxy(){
        CordaRPCClient bankBClient = new CordaRPCClient(NetworkHostAndPort.parse(creditBureauHostAndPort),
                new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return bankBClient.start("user1", "test").getProxy();
    }

    @Bean(destroyMethod = "")
    public CordaRPCOps evaluationBureauProxy(){
        CordaRPCClient bankBClient = new CordaRPCClient(NetworkHostAndPort.parse(evaluationBureauHostAndPort),
                new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return bankBClient.start("user1", "test").getProxy();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
        ObjectMapper mapper =  JacksonSupport.createDefaultMapper(brokerProxy());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**");
            }
        };
    }
}
