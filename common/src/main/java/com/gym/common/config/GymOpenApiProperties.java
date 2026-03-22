package com.gym.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gym.openapi")
public class GymOpenApiProperties {

    private String title = "Gym Platform API";
    private String description = "Gym Platform Microservice";
    private String version = "1.0.0";
    private String contactEmail = "support@gym-platform.com";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}
