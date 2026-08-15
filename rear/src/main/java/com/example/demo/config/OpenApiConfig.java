package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置
 *
 * 【访问地址】
 * - 文档页面：http://localhost:8080/doc.html
 * - OpenAPI JSON：http://localhost:8080/v3/api-docs
 *
 * 【功能说明】
 * - 配置 API 文档的基本信息（标题、版本、描述、联系方式）
 * - 配置 JWT 认证方案，支持在文档页面调试需鉴权的接口
 * - 配置分组，按业务模块组织接口
 */
@Configuration
public class OpenApiConfig {

    /**
     * 全局 OpenAPI 配置
     *
     * 【JWT 调试说明】
     * 在 Knife4j 页面右上角点击"文档管理" → "全局参数"或点击"Authorize"按钮，
     * 填入：Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 之后调试所有接口都会自动携带 Authorization 头
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("影院管理系统 API 文档")
                        .version("1.0.0")
                        .description("""
                                ## 影院管理系统后端接口文档
                                
                                基于 Spring Boot 3 + Knife4j (OpenAPI 3) 自动生成。
                                
                                ### 接口分组说明
                                - **认证管理**：登录、注册、Token 刷新
                                - **影院/影厅管理**：影院和影厅的 CRUD
                                - **电影管理**：电影的查询、搜索、分类
                                - **管理员后台**：管理员专用的全模块管理接口
                                - **订单与支付**：订单创建、支付、取消、退票
                                - **用户功能**：收藏、评论、浏览记录
                                
                                ### 认证方式
                                除公开接口外，其他接口需在请求头携带：
                                ```
                                Authorization: Bearer {accessToken}
                                ```
                                """)
                        .contact(new Contact()
                                .name("影院管理系统")
                                .email("admin@cinema.com"))
                        .license(new License()
                                .name("MIT License")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入登录后获取的 accessToken，格式：Bearer {token}")));
    }
}