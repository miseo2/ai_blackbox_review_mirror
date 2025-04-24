package ccrate.s3_test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // REST API이므로 CSRF 보호 비활성화
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll()  // 모든 요청 허용
            );
            
        return http.build();
    }
} 