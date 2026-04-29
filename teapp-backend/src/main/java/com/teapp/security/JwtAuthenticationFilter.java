package com.teapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta cada petición HTTP, extrae y valida el Bearer token,
 * y establece la autenticación en el SecurityContext si es válido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Intercepta la petición, extrae el Bearer token del header Authorization,
     * lo valida y, si es correcto, setea la autenticación en el SecurityContext.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadenaFiltros) throws ServletException, IOException {
        String token = extraerTokenDePeticion(peticion);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            UserDetails detallesUsuario = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(detallesUsuario, null, detallesUsuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            log.debug("Usuario autenticado via JWT: {}", email);
        }

        cadenaFiltros.doFilter(peticion, respuesta);
    }

    /** Extrae el token del header Authorization eliminando el prefijo "Bearer ". */
    private String extraerTokenDePeticion(HttpServletRequest peticion) {
        String bearerToken = peticion.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
