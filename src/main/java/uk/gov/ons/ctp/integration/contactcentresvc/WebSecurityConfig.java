package uk.gov.ons.ctp.integration.contactcentresvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${spring.security.user.name}")
  String username;

  @Value("${spring.security.user.password}")
  String password;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/info")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .csrf()
        .disable()
        .httpBasic();
  }

  // USE OF THE PASSWORD ENCRYPTION COMMENTED OUT WHILE I TRY AND WORK OUT WHY IT IS NOT WORKING
  // WHEN
  // CONFIGURED THROUGH SECRETS IN GCP
  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
        //        .passwordEncoder(passwordEncoder())
        .withUser(username)
        .password("{noop}" + password)
        .roles("USER");
  }

  //  @Bean
  //  public PasswordEncoder passwordEncoder() {
  //    return new BCryptPasswordEncoder();
  //  }
}
