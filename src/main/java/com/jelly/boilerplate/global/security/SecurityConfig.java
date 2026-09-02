package com.jelly.boilerplate.global.security;

import com.jelly.boilerplate.global.jwt.JwtAuthenticationFilter;
import com.jelly.boilerplate.global.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize("hasRole('ADMIN')") 등을 메서드에 붙일 수 있게 함
@EnableConfigurationProperties(JwtProperties.class) // jwt.* → JwtProperties 바인딩 + 빈 등록
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            //JWT 기반 REST API를 위한 CSRF 보호 기능 끔
            //토큰은 헤더로 보내고 브라우저가 자동으로 실어주지 않으므로 CSRF 공격 자체가 성립하지 않는다.
            .csrf(csrf -> csrf.disable())
            //어떤 출처/메서드/헤더를 허용할지에 대한 구체적 규칙
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            //JWT를 사용하여 서버가 세션을 만들지 않도록 명시 (STATELESS)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            //HTTP 요청별 인가(권한) 규칙
            .authorizeHttpRequests(auth -> auth
                //로그인/토큰 재발급 등 인증 API는 공개
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Swagger UI/OpenAPI 문서 자체는 공개 (문서를 보는 것과, 문서 안에서 "Authorize"로
                // 토큰 넣고 보호된 API를 실제 호출하는 건 별개)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // 나머지는 유효한 JWT 필요
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                //인증 안 됨(토큰 없음 등) -> 401
                .authenticationEntryPoint(authenticationEntryPoint)
                //인증은 됐으나 권한 부족 -> 403
                .accessDeniedHandler(accessDeniedHandler)
            )
            //아이디/비번 폼 로그인 필터 앞에 JWT 필터를 끼워 넣는다
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 비밀번호 해시. 회원가입 시 encode, 로그인 시 matches 로 검증 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 프론트 개발 서버 포트가 바뀔 수 있어서 자주 쓰는 포트를 열어둠.
        config.setAllowedOrigins(List.of(
            //로컬 프론트 서버
            "http://localhost:3000",
            //로컬 프론트 서버를 2개 돌릴 때 예비용 (주로 AI가 프론트 서버 돌릴 때)
            "http://localhost:3001"
        ));

        //허용하는 HTTP 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        //클라이언트가 요청에 담아 보낼 수 있는 헤더
        //지금은 전체를 열었지만 운영에서는 "Authorization", "Content-Type" 정도만 여는 게 좋다.
        config.setAllowedHeaders(List.of("*"));

        //클라이언트(JS)가 응답에서 읽을 수 있는 헤더
        config.setExposedHeaders(List.of("Authorization"));

        //cross-origin 요청에 쿠키/인증정보를 실어 보내는 것을 허용
        //(true 이면 allowedOrigins 에 "*" 사용 불가 → 위처럼 도메인을 나열해야 함)
        config.setAllowCredentials(true);

        //URL 경로별로 서로 다른 CORS 규칙을 매핑할 수 있는 구현체
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //모든 경로(/**)에 위 config 적용
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
