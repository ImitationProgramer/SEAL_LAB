package com.seal.seal_lab.infra.config;

import com.seal.seal_lab.infra.security.CustomLogoutSuccessHandler;
import com.seal.seal_lab.infra.security.LoginFailureHandler;
import com.seal.seal_lab.infra.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 우리가 만든 3가지 핵심 보안 핸들러 주입
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/signup", "/about/**", "/publications/**", "/projects/**", "/gallery/**", "/contact/**", "/related/**").permitAll()
                        .requestMatchers("/css/**", "/images/**", "/js/**", "/favicon.ico", "/uploads/**").permitAll()

                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                        .anyRequest().permitAll()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        // 로그인 성공 시: 신뢰 점수 로그 및 위치 동기화
                        .successHandler(loginSuccessHandler)
                        // 로그인 실패 시: 공격 감지 로그 및 에러 처리
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // 로그아웃 성공 시: 안전한 퇴장 로그 기록
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}