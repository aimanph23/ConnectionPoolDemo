package com.example.connectionpool.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    /**
     * Home endpoint that provides a comprehensive overview of all available endpoints
     * 
     * Example: GET http://localhost:8080/home (HTML view - user-friendly)
     * Example: GET http://localhost:8080/home?format=json (JSON view)
     */
    @GetMapping("/home")
    public ResponseEntity<?> home(@RequestParam(defaultValue = "html") String format) {
        log.info("Serving home endpoint with application overview, format: {}", format);
        
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(generateJsonResponse());
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(generateHtmlResponse());
        }
    }
    
    private Map<String, Object> generateJsonResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "Connection Pool Demo");
        response.put("description", "A Spring Boot application demonstrating database connection pool management with HikariCP");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        response.put("baseUrl", "http://localhost:8080");
        
        // Product Management Endpoints
        Map<String, Object> productEndpoints = new LinkedHashMap<>();
        productEndpoints.put("basePath", "/api/products");
        productEndpoints.put("description", "Product management with connection pool testing");
        
        List<Map<String, String>> productOperations = Arrays.asList(
            createEndpointInfo("POST", "/api/products/{id}/process", "Process product (DB + External API + Update)", "Main endpoint for testing connection pool behavior"),
            createEndpointInfo("POST", "/api/products", "Create new product", "Body: ProductRequest with name, description, price, stockQuantity"),
            createEndpointInfo("GET", "/api/products", "Get all products", "Returns list of all products"),
            createEndpointInfo("GET", "/api/products/{id}", "Get product by ID", "Includes mock API call with configurable delay"),
            createEndpointInfo("GET", "/api/products/v2/{id}", "Get product by ID (Async)", "Non-blocking version that releases DB connection immediately"),
            createEndpointInfo("DELETE", "/api/products/{id}", "Delete product", "Removes product by ID"),
            createEndpointInfo("GET", "/api/products/health", "Product API health check", "Returns API status")
        );
        productEndpoints.put("operations", productOperations);
        
        // Customer Management Endpoints (API 101 Integration)
        Map<String, Object> customerEndpoints = new LinkedHashMap<>();
        customerEndpoints.put("basePath", "/api/customers");
        customerEndpoints.put("description", "Customer management via Postman API 101 collection integration");
        
        List<Map<String, String>> customerOperations = Arrays.asList(
            createEndpointInfo("GET", "/api/customers", "Get all customers", "Retrieves customers from Postman Echo API"),
            createEndpointInfo("GET", "/api/customers/{id}", "Get customer by ID", "Retrieves specific customer from Postman Echo API"),
            createEndpointInfo("POST", "/api/customers", "Create new customer", "Body: Customer with name, email, phone, address"),
            createEndpointInfo("PUT", "/api/customers/{id}", "Update customer", "Updates customer information"),
            createEndpointInfo("DELETE", "/api/customers/{id}", "Delete customer", "Removes customer from Postman Echo API"),
            createEndpointInfo("GET", "/api/customers/async", "Get all customers (Async)", "Asynchronous version of get all customers"),
            createEndpointInfo("GET", "/api/customers/async/{id}", "Get customer by ID (Async)", "Asynchronous version of get customer by ID"),
            createEndpointInfo("POST", "/api/customers/async", "Create customer (Async)", "Asynchronous version of create customer"),
            createEndpointInfo("GET", "/api/customers/health", "Customer API health check", "Returns API status")
        );
        customerEndpoints.put("operations", customerOperations);
        
        // Monitoring Endpoints
        Map<String, Object> monitoringEndpoints = new LinkedHashMap<>();
        monitoringEndpoints.put("basePath", "/api/monitoring");
        monitoringEndpoints.put("description", "Real-time connection pool monitoring and metrics");
        
        List<Map<String, String>> monitoringOperations = Arrays.asList(
            createEndpointInfo("GET", "/api/monitoring/hikari", "Get HikariCP metrics", "Returns current connection pool statistics"),
            createEndpointInfo("GET", "/api/monitoring/hikari/details", "Get detailed HikariCP info", "Returns comprehensive pool configuration and status"),
            createEndpointInfo("GET", "/api/monitoring/hikari/status", "Get pool status", "Returns simple pool status information"),
            createEndpointInfo("GET", "/api/monitoring/hikari/stream", "Real-time metrics stream", "Server-Sent Events stream for live monitoring"),
            createEndpointInfo("GET", "/api/monitoring/health", "Monitoring API health check", "Returns API status")
        );
        monitoringEndpoints.put("operations", monitoringOperations);
        
        // Thread Pool Monitoring Endpoints
        Map<String, Object> threadPoolEndpoints = new LinkedHashMap<>();
        threadPoolEndpoints.put("basePath", "/api/threadpool");
        threadPoolEndpoints.put("description", "Real-time thread pool monitoring and metrics");
        
        List<Map<String, String>> threadPoolOperations = Arrays.asList(
            createEndpointInfo("GET", "/api/threadpool/metrics", "Get thread pool metrics", "Returns current thread pool statistics"),
            createEndpointInfo("GET", "/api/threadpool/details", "Get detailed thread pool info", "Returns comprehensive pool configuration and status"),
            createEndpointInfo("GET", "/api/threadpool/status", "Get thread pool status", "Returns simple thread pool status information"),
            createEndpointInfo("GET", "/api/threadpool/stream", "Real-time thread pool stream", "Server-Sent Events stream for live monitoring"),
            createEndpointInfo("GET", "/api/threadpool/health", "Thread pool API health check", "Returns API status")
        );
        threadPoolEndpoints.put("operations", threadPoolOperations);
        
        // Dashboard Endpoints
        Map<String, Object> dashboardEndpoints = new LinkedHashMap<>();
        dashboardEndpoints.put("basePath", "/dashboard");
        dashboardEndpoints.put("description", "Web-based monitoring dashboards");
        
        List<Map<String, String>> dashboardOperations = Arrays.asList(
            createEndpointInfo("GET", "/dashboard/hikari", "HikariCP Dashboard", "Real-time web dashboard for connection pool monitoring"),
            createEndpointInfo("GET", "/dashboard/threadpool", "Thread Pool Dashboard", "Real-time web dashboard for thread pool monitoring")
        );
        dashboardEndpoints.put("operations", dashboardOperations);
        
        // Application Information
        Map<String, Object> appInfo = new LinkedHashMap<>();
        appInfo.put("database", "H2 In-Memory Database");
        appInfo.put("connectionPool", "HikariCP");
        appInfo.put("maxPoolSize", "10");
        appInfo.put("minIdle", "5");
        appInfo.put("externalApis", Arrays.asList(
            "JSONPlaceholder API (https://jsonplaceholder.typicode.com/posts/1)",
            "Postman Echo API (https://postman-echo.com)",
            "Postman Echo Delay API (https://postman-echo.com/delay/{seconds})"
        ));
        appInfo.put("features", Arrays.asList(
            "Connection Pool Monitoring",
            "Async vs Sync API Processing",
            "External API Integration",
            "Real-time Metrics Dashboard",
            "Postman API 101 Collection Integration"
        ));



        // Quick Start Guide
        Map<String, Object> quickStart = new LinkedHashMap<>();
        quickStart.put("1", "Start the application: mvn spring-boot:run");
        quickStart.put("2", "Access the home endpoint: GET http://localhost:8080/home");
        quickStart.put("3", "Monitor connection pool: GET http://localhost:8080/api/monitoring/hikari");
        quickStart.put("4", "View real-time dashboard: http://localhost:8080/dashboard/hikari");
        quickStart.put("5", "Test product processing: POST http://localhost:8080/api/products/1/process");
        quickStart.put("6", "Test async processing: GET http://localhost:8080/api/products/v2/1");
        quickStart.put("7", "Test customer API: GET http://localhost:8080/api/customers");
        
        // Tomcat Monitoring Endpoints
        Map<String, Object> tomcatEndpoints = new LinkedHashMap<>();
        Map<String, String> tomcatOperations = new LinkedHashMap<>();
        tomcatOperations.put("GET /api/tomcat/metrics", "Get Tomcat thread pool metrics");
        tomcatOperations.put("GET /api/tomcat/details", "Get detailed Tomcat information");
        tomcatOperations.put("GET /api/tomcat/status", "Get Tomcat status");
        tomcatOperations.put("GET /api/tomcat/stream", "Real-time Tomcat metrics stream (SSE)");
        tomcatOperations.put("GET /api/tomcat/health", "Health check");
        tomcatEndpoints.put("operations", tomcatOperations);
        
        // Assemble response
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("products", productEndpoints);
        endpoints.put("customers", customerEndpoints);
        endpoints.put("monitoring", monitoringEndpoints);
        endpoints.put("threadpool", threadPoolEndpoints);
        endpoints.put("tomcat", tomcatEndpoints);
        endpoints.put("dashboard", dashboardEndpoints);
        response.put("endpoints", endpoints);
        response.put("applicationInfo", appInfo);
        response.put("quickStart", quickStart);
        
        return response;
    }
    
    private String generateHtmlResponse() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Connection Pool Demo - API Overview</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; line-height: 1.6; }\n");
        html.append("        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); overflow: hidden; }\n");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; }\n");
        html.append("        .header h1 { font-size: 2.5em; margin-bottom: 10px; }\n");
        html.append("        .header p { font-size: 1.2em; opacity: 0.9; }\n");
        html.append("        .header .meta { margin-top: 20px; font-size: 0.9em; opacity: 0.8; }\n");
        html.append("        .content { padding: 40px; }\n");
        html.append("        .section { margin-bottom: 40px; }\n");
        html.append("        .section-title { font-size: 1.8em; color: #667eea; margin-bottom: 20px; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n");
        html.append("        .endpoint-category { background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n");
        html.append("        .category-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }\n");
        html.append("        .category-title { font-size: 1.4em; color: #333; font-weight: bold; }\n");
        html.append("        .category-path { font-family: 'Courier New', monospace; color: #667eea; background: white; padding: 5px 10px; border-radius: 5px; font-size: 0.9em; }\n");
        html.append("        .category-desc { color: #666; margin-bottom: 15px; font-style: italic; }\n");
        html.append("        .endpoint { background: white; border-left: 4px solid #667eea; padding: 15px; margin-bottom: 10px; border-radius: 5px; transition: all 0.3s; }\n");
        html.append("        .endpoint:hover { box-shadow: 0 4px 8px rgba(0,0,0,0.1); transform: translateX(5px); }\n");
        html.append("        .endpoint-line { display: flex; align-items: center; margin-bottom: 5px; }\n");
        html.append("        .method { display: inline-block; padding: 5px 12px; border-radius: 5px; font-weight: bold; font-size: 0.85em; margin-right: 15px; min-width: 70px; text-align: center; }\n");
        html.append("        .method.get { background: #28a745; color: white; }\n");
        html.append("        .method.post { background: #007bff; color: white; }\n");
        html.append("        .method.put { background: #ffc107; color: #333; }\n");
        html.append("        .method.delete { background: #dc3545; color: white; }\n");
        html.append("        .path { font-family: 'Courier New', monospace; color: #333; font-size: 0.95em; font-weight: 500; }\n");
        html.append("        .path-link { font-family: 'Courier New', monospace; color: #667eea; font-size: 0.95em; font-weight: 500; text-decoration: none; transition: color 0.3s; }\n");
        html.append("        .path-link:hover { color: #764ba2; text-decoration: underline; }\n");
        html.append("        .example { display: block; font-size: 0.85em; color: #888; margin-left: 85px; margin-top: 3px; }\n");
        html.append("        .example a { color: #667eea; text-decoration: none; }\n");
        html.append("        .example a:hover { text-decoration: underline; }\n");
        html.append("        .description { color: #666; font-size: 0.95em; margin-left: 85px; }\n");
        html.append("        .details { color: #999; font-size: 0.85em; margin-left: 85px; margin-top: 5px; }\n");
        html.append("        .info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }\n");
        html.append("        .info-card { background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #764ba2; }\n");
        html.append("        .info-card h3 { color: #764ba2; margin-bottom: 10px; }\n");
        html.append("        .info-card ul { list-style: none; }\n");
        html.append("        .info-card li { padding: 5px 0; color: #555; }\n");
        html.append("        .info-card li:before { content: \"✓ \"; color: #28a745; font-weight: bold; }\n");
        html.append("        .quick-start { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); padding: 30px; border-radius: 8px; color: #333; }\n");
        html.append("        .quick-start h2 { margin-bottom: 20px; }\n");
        html.append("        .quick-start ol { margin-left: 20px; }\n");
        html.append("        .quick-start li { margin: 10px 0; font-size: 1.05em; }\n");
        html.append("        .quick-start code { background: rgba(255,255,255,0.8); padding: 2px 8px; border-radius: 3px; font-family: 'Courier New', monospace; }\n");
        html.append("        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; border-top: 1px solid #dee2e6; }\n");
        html.append("        .badge { display: inline-block; padding: 5px 10px; background: #667eea; color: white; border-radius: 20px; font-size: 0.8em; margin-left: 10px; }\n");
        html.append("        @media (max-width: 768px) { .header h1 { font-size: 1.8em; } .section-title { font-size: 1.4em; } .info-grid { grid-template-columns: 1fr; } }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        
        // Header
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>🚀 Connection Pool Demo</h1>\n");
        html.append("            <p>A Spring Boot application demonstrating database connection pool management with HikariCP</p>\n");
        html.append("            <div class=\"meta\">Version 1.0.0 | ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</div>\n");
        html.append("        </div>\n");
        
        html.append("        <div class=\"content\">\n");
        html.append("            </div>\n");

        // Dashboard
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">🎨 Dashboards</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Web Dashboards<span class=\"badge\">Visual</span></div>\n");
        html.append("                        <div class=\"category-path\">/dashboard</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Web-based monitoring dashboards</div>\n");

        addEndpoint(html, "GET", "/dashboard/hikari", "HikariCP Dashboard", "Real-time web dashboard for connection pool monitoring");
        addEndpoint(html, "GET", "/dashboard/threadpool", "Thread Pool Dashboard", "Real-time web dashboard for thread pool monitoring");
        addEndpoint(html, "GET", "/dashboard/tomcat", "Tomcat Dashboard", "Real-time web dashboard for Tomcat thread pool monitoring");
        addEndpoint(html, "GET", "/dashboard/jvm", "JVM Dashboard", "Real-time CPU & Memory monitoring dashboard");

        html.append("                </div>\n");
        // Quick Start
        html.append("            <div class=\"section\">\n");
        html.append("                <div class=\"quick-start\">\n");
        html.append("                    <h2>🎯 Quick Start Guide</h2>\n");
        html.append("                    <ol>\n");
        html.append("                        <li>Start the application: <code>mvn spring-boot:run</code></li>\n");
        html.append("                        <li>Access this page: <code>GET http://localhost:8080/home</code></li>\n");
        html.append("                        <li>Monitor connection pool: <code>GET http://localhost:8080/api/monitoring/hikari</code></li>\n");
        html.append("                        <li>View real-time dashboard: <a href=\"http://localhost:8080/dashboard/hikari\" style=\"color: #667eea; font-weight: bold;\">http://localhost:8080/dashboard/hikari</a></li>\n");
        html.append("                        <li>Test product processing: <code>POST http://localhost:8080/api/products/1/process</code></li>\n");
        html.append("                        <li>Test async processing: <code>GET http://localhost:8080/api/products/v2/1</code></li>\n");
        html.append("                        <li>Test customer API: <code>GET http://localhost:8080/api/customers</code></li>\n");
        html.append("                    </ol>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // Product Endpoints
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">📦 Product Management API</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Products<span class=\"badge\">Connection Pool Testing</span></div>\n");
        html.append("                        <div class=\"category-path\">/api/products</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Product management with connection pool testing capabilities</div>\n");
        
        addEndpoint(html, "POST", "/api/products/{id}/process", "Process product (DB + External API + Update)", "Main endpoint for testing connection pool behavior");
        addEndpoint(html, "POST", "/api/products", "Create new product", "Body: ProductRequest with name, description, price, stockQuantity");
        addEndpoint(html, "GET", "/api/products", "Get all products", "Returns list of all products");
        addEndpoint(html, "GET", "/api/products/{id}", "Get product by ID", "Includes mock API call with configurable delay");
        addEndpoint(html, "GET", "/api/products/v2/{id}", "Get product by ID (Async)", "Non-blocking version that releases DB connection immediately");
        addEndpoint(html, "DELETE", "/api/products/{id}", "Delete product", "Removes product by ID");
        addEndpoint(html, "GET", "/api/products/health", "Health check", "Returns API status");
        
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // Customer Endpoints
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">👥 Customer Management API</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Customers<span class=\"badge\">Postman API 101</span></div>\n");
        html.append("                        <div class=\"category-path\">/api/customers</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Customer management via Postman API 101 collection integration</div>\n");
        
        addEndpoint(html, "GET", "/api/customers", "Get all customers", "Retrieves customers from Postman Echo API");
        addEndpoint(html, "GET", "/api/customers/{id}", "Get customer by ID", "Retrieves specific customer from Postman Echo API");
        addEndpoint(html, "POST", "/api/customers", "Create new customer", "Body: Customer with name, email, phone, address");
        addEndpoint(html, "PUT", "/api/customers/{id}", "Update customer", "Updates customer information");
        addEndpoint(html, "DELETE", "/api/customers/{id}", "Delete customer", "Removes customer from Postman Echo API");
        addEndpoint(html, "GET", "/api/customers/async", "Get all customers (Async)", "Asynchronous version");
        addEndpoint(html, "GET", "/api/customers/async/{id}", "Get customer by ID (Async)", "Asynchronous version");
        addEndpoint(html, "POST", "/api/customers/async", "Create customer (Async)", "Asynchronous version");
        addEndpoint(html, "GET", "/api/customers/health", "Health check", "Returns API status");
        
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // Monitoring Endpoints
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">📊 Monitoring & Metrics API</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Monitoring<span class=\"badge\">Real-time</span></div>\n");
        html.append("                        <div class=\"category-path\">/api/monitoring</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Real-time connection pool monitoring and metrics</div>\n");
        
        addEndpoint(html, "GET", "/api/monitoring/hikari", "Get HikariCP metrics", "Returns current connection pool statistics");
        addEndpoint(html, "GET", "/api/monitoring/hikari/details", "Get detailed HikariCP info", "Returns comprehensive pool configuration and status");
        addEndpoint(html, "GET", "/api/monitoring/hikari/status", "Get pool status", "Returns simple pool status information");
        addEndpoint(html, "GET", "/api/monitoring/hikari/stream", "Real-time metrics stream", "Server-Sent Events stream for live monitoring");
        addEndpoint(html, "GET", "/api/monitoring/health", "Health check", "Returns API status");
        
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // Thread Pool Monitoring Endpoints
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">🧵 Thread Pool Monitoring API</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Thread Pool<span class=\"badge\">Real-time</span></div>\n");
        html.append("                        <div class=\"category-path\">/api/threadpool</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Real-time thread pool monitoring and metrics</div>\n");
        
        addEndpoint(html, "GET", "/api/threadpool/metrics", "Get thread pool metrics", "Returns current thread pool statistics");
        addEndpoint(html, "GET", "/api/threadpool/details", "Get detailed thread pool info", "Returns comprehensive pool configuration and status");
        addEndpoint(html, "GET", "/api/threadpool/status", "Get thread pool status", "Returns simple thread pool status information");
        addEndpoint(html, "GET", "/api/threadpool/stream", "Real-time thread pool stream", "Server-Sent Events stream for live monitoring");
        addEndpoint(html, "GET", "/api/threadpool/health", "Health check", "Returns API status");
        
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        // Tomcat Monitoring Endpoints
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">🚀 Tomcat Thread Pool Monitoring API</h2>\n");
        html.append("                <div class=\"endpoint-category\">\n");
        html.append("                    <div class=\"category-header\">\n");
        html.append("                        <div class=\"category-title\">Tomcat Pool<span class=\"badge\">Real-time</span></div>\n");
        html.append("                        <div class=\"category-path\">/api/tomcat</div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"category-desc\">Real-time Tomcat HTTP thread pool monitoring and metrics</div>\n");
        
        addEndpoint(html, "GET", "/api/tomcat/metrics", "Get Tomcat metrics", "Returns current Tomcat thread pool statistics");
        addEndpoint(html, "GET", "/api/tomcat/details", "Get detailed Tomcat info", "Returns comprehensive Tomcat configuration and status");
        addEndpoint(html, "GET", "/api/tomcat/status", "Get Tomcat status", "Returns simple Tomcat status information");
        addEndpoint(html, "GET", "/api/tomcat/stream", "Real-time Tomcat stream", "Server-Sent Events stream for live monitoring");
        addEndpoint(html, "GET", "/api/tomcat/health", "Health check", "Returns API status");
        
        html.append("                </div>\n");

        html.append("            </div>\n");
        
        // Application Info
        html.append("            <div class=\"section\">\n");
        html.append("                <h2 class=\"section-title\">ℹ️ Application Information</h2>\n");
        html.append("                <div class=\"info-grid\">\n");
        html.append("                    <div class=\"info-card\">\n");
        html.append("                        <h3>Configuration</h3>\n");
        html.append("                        <ul>\n");
        html.append("                            <li>Database: H2 In-Memory</li>\n");
        html.append("                            <li>Connection Pool: HikariCP</li>\n");
        html.append("                            <li>Max Pool Size: 10</li>\n");
        html.append("                            <li>Min Idle: 5</li>\n");
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"info-card\">\n");
        html.append("                        <h3>External APIs</h3>\n");
        html.append("                        <ul>\n");
        html.append("                            <li>JSONPlaceholder API</li>\n");
        html.append("                            <li>Postman Echo API</li>\n");
        html.append("                            <li>Postman Echo Delay API</li>\n");
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"info-card\">\n");
        html.append("                        <h3>Key Features</h3>\n");
        html.append("                        <ul>\n");
        html.append("                            <li>Connection Pool Monitoring</li>\n");
        html.append("                            <li>Async vs Sync Processing</li>\n");
        html.append("                            <li>External API Integration</li>\n");
        html.append("                            <li>Real-time Dashboards</li>\n");
        html.append("                            <li>API 101 Integration</li>\n");
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        
        html.append("        </div>\n");
        
        // Footer
        html.append("        <div class=\"footer\">\n");
        html.append("            <p>📄 View as JSON: <a href=\"/home?format=json\" style=\"color: #667eea; font-weight: bold;\">/home?format=json</a></p>\n");
        html.append("            <p style=\"margin-top: 10px;\">Built with Spring Boot & HikariCP | Connection Pool Demo v1.0.0</p>\n");
        html.append("        </div>\n");
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    private void addEndpoint(StringBuilder html, String method, String path, String description, String details) {
        html.append("                    <div class=\"endpoint\">\n");
        html.append("                        <div class=\"endpoint-line\">\n");
        html.append("                            <span class=\"method ").append(method.toLowerCase()).append("\">").append(method).append("</span>\n");
        
        // Make GET endpoints clickable
        if ("GET".equalsIgnoreCase(method) && !path.contains("{")) {
            html.append("                            <a href=\"").append(path).append("\" class=\"path-link\" target=\"_blank\">").append(path).append("</a>\n");
        } else if ("GET".equalsIgnoreCase(method)) {
            // For GET endpoints with parameters, show example
            String examplePath = path.replace("{id}", "1");
            html.append("                            <a href=\"").append(examplePath).append("\" class=\"path-link\" target=\"_blank\">").append(path).append("</a>\n");
            html.append("                            <span class=\"example\">Example: <a href=\"").append(examplePath).append("\" target=\"_blank\">").append(examplePath).append("</a></span>\n");
        } else {
            html.append("                            <span class=\"path\">").append(path).append("</span>\n");
        }
        
        html.append("                        </div>\n");
        html.append("                        <div class=\"description\">").append(description).append("</div>\n");
        html.append("                        <div class=\"details\">").append(details).append("</div>\n");
        html.append("                    </div>\n");
    }
    
    private Map<String, String> createEndpointInfo(String method, String path, String description, String details) {
        Map<String, String> endpoint = new LinkedHashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        endpoint.put("details", details);
        return endpoint;
    }
}
