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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${config.rpc.host}")
    private String host;
    // The RPC port of the node we are connecting to.
    @Value("${config.rpc.username}")
    private String username;
    // The username for logging into the RPC client.
    @Value("${config.rpc.password}")
    private String password;
    // The password for logging into the RPC client.
    @Value("${config.rpc.port}")
    private int rpcPort;

    @Bean(destroyMethod = "")  // Avoids node shutdown on rpc disconnect
    public CordaRPCOps defProxy(){
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient partyAClient = new CordaRPCClient(rpcAddress,new CordaRPCClientConfiguration(Duration.ofMinutes(3),10));
        return partyAClient.start("user1", "test").getProxy();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
        ObjectMapper mapper =  JacksonSupport.createDefaultMapper(defProxy());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;
    }
}
