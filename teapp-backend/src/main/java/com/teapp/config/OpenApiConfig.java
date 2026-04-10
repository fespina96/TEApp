package com.teapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger UI para la documentación del API REST.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "TEApp API",
        version = "1.0",
        description = "API REST para la agenda semanal de niños con autismo. " +
                      "Permite a los padres gestionar perfiles de sus hijos y sus rutinas semanales.",
        contact = @Contact(name = "TEApp Team")
    ),
    servers = @Server(url = "http://localhost:8080", description = "Servidor de desarrollo")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenido del endpoint /api/v1/auth/login"
)
public class OpenApiConfig {
}
