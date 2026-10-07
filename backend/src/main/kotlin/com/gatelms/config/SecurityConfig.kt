package com.gatelms.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.security.CustomUserDetailsService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import java.net.URI

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val customUserDetailsService: CustomUserDetailsService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(customUserDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun httpFirewall(): org.springframework.security.web.firewall.HttpFirewall {
        val firewall = org.springframework.security.web.firewall.StrictHttpFirewall()
        firewall.setAllowUrlEncodedSlash(true)
        firewall.setAllowUrlEncodedPercent(true)
        return firewall
    }

    @Bean
    fun cookieSerializer(): org.springframework.session.web.http.DefaultCookieSerializer {
        val serializer = org.springframework.session.web.http.DefaultCookieSerializer()
        serializer.setCookieName("GATELMSSESSION")
        serializer.setSameSite("Strict")
        serializer.setUseHttpOnlyCookie(true)
        return serializer
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/api/v1/auth/login")
            }
            .addFilterAfter(CsrfCookieFilter(), org.springframework.security.web.authentication.www.BasicAuthenticationFilter::class.java)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()
                auth.requestMatchers("/actuator/**").hasRole("ADMIN")
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    val problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Unauthorized").apply {
                        title = "Unauthorized"
                        type = URI.create("about:blank")
                    }
                    objectMapper.writeValue(response.writer, problem)
                }
                exceptions.accessDeniedHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    val problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden").apply {
                        title = "Forbidden"
                        type = URI.create("about:blank")
                    }
                    objectMapper.writeValue(response.writer, problem)
                }
            }

        return http.build()
    }
}

class CsrfCookieFilter : org.springframework.web.filter.OncePerRequestFilter() {
    override fun doFilterInternal(
        request: jakarta.servlet.http.HttpServletRequest,
        response: jakarta.servlet.http.HttpServletResponse,
        filterChain: jakarta.servlet.FilterChain
    ) {
        val csrfToken = request.getAttribute(org.springframework.security.web.csrf.CsrfToken::class.java.name) as? org.springframework.security.web.csrf.CsrfToken
        csrfToken?.token // Force evaluation to trigger CookieCsrfTokenRepository to write cookie
        filterChain.doFilter(request, response)
    }
}
