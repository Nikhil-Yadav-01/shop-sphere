package com.rudraksha.shopsphere.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.routes")
public class GatewayRouteProperties {

    private List<RouteConfig> routes;

    public List<RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteConfig> routes) {
        this.routes = routes;
    }

    public static class RouteConfig {
        private String name;
        private String path;
        private String uri;
        private boolean authenticated;
        private String[] roles;
        private boolean stripPrefix;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public String[] getRoles() {
            return roles;
        }

        public void setRoles(String[] roles) {
            this.roles = roles;
        }

        public boolean isStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(boolean stripPrefix) {
            this.stripPrefix = stripPrefix;
        }
    }
}
