
package com.liyile.medication.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

/**
 * OpenApiConfig 用于配置 OpenAPI/Swagger 的元信息与安全方案。
 *
 * 类职责：
 * - 定义全局的 OpenAPI 信息（标题、版本、描述、服务器地址）。
 * - 定义 JWT Bearer 安全方案，供需要鉴权的接口引用。
 *
 * 命名规范：
 * - 类名采用驼峰命名法。
 * 注释规范：
 * - 每个类需提供用途说明。
 */
@OpenAPIDefinition(
        info = @Info(title = "Medication Backend API", version = "v1", description = "Medication backend REST API"),
        servers = {
                @Server(url = "http://localhost:8080", description = "dev")
        }
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    /**
     * 该配置类无需方法和字段，仅通过注解提供 OpenAPI 元数据与安全方案。
     */
}
