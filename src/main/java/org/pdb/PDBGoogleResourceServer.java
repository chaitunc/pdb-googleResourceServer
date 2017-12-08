package org.pdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableResourceServer
@ComponentScan
@EnableDiscoveryClient
public class PDBGoogleResourceServer extends ResourceServerConfigurerAdapter {

	private static final String PDB_GOOGLE_SECRET = "pdb.auth.client.google.secret";
	private static final String PDB_AUTH_CHECKTOKEN_URI = "pdb.auth.client.check_token.url";
	@Autowired
	Environment env;

	public static void main(String[] args) {
		SpringApplication.run(PDBGoogleResourceServer.class, args);
	}

	@Bean
	public LoadBalancerInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient,
			LoadBalancerRequestFactory requestFactory) {
		return new LoadBalancerInterceptor(loadBalancerClient, requestFactory);
	}

	@Bean
	public RestOperations restTemplate(LoadBalancerClient loadBalancerClient,
			LoadBalancerRequestFactory requestFactory) {
		RestTemplate restTemplate = new RestTemplate();
		((RestTemplate) restTemplate).setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			// Ignore 400
			public void handleError(ClientHttpResponse response) throws IOException {
				if (response.getRawStatusCode() != 400) {
					super.handleError(response);
				}
			}
		});

		List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
		list.add(ribbonInterceptor(loadBalancerClient, requestFactory));
		restTemplate.setInterceptors(list);
		return restTemplate;
	}

	@Primary
	@Bean
	public RemoteTokenServices tokenService(OAuth2ProtectedResourceDetails resource, OAuth2ClientContext context,
			LoadBalancerClient loadBalancerClient, LoadBalancerRequestFactory requestFactory) {
		RemoteTokenServices tokenService = new RemoteTokenServices();
		tokenService.setCheckTokenEndpointUrl(env.getRequiredProperty(PDB_AUTH_CHECKTOKEN_URI));
		tokenService.setClientId("PDB-GOOGLE");
		tokenService.setClientSecret(env.getRequiredProperty(PDB_GOOGLE_SECRET));
		tokenService.setRestTemplate(restTemplate(loadBalancerClient, requestFactory));
		return tokenService;
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/").permitAll().anyRequest().hasAuthority("ROLE_GOOGLE").anyRequest()
				.authenticated();
	}

}
