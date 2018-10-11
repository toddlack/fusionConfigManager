package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Calendar;

/**
 * Represent a ROLE in fusion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements IdentifiableString {
    private String name;
    private String id;
    private String desc;
    private Calendar createdAt;
    Permission[] permissions;
    String[] uiPermissions;

    public Role() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Calendar getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Calendar createdAt) {
        this.createdAt = createdAt;
    }

    public Permission[] getPermissions() {
        return permissions;
    }

    public void setPermissions(Permission[] permissions) {
        this.permissions = permissions;
    }

    public String[] getUiPermissions() {
        return uiPermissions;
    }

    public void setUiPermissions(String[] uiPermissions) {
        this.uiPermissions = uiPermissions;
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name.
     * Roles use the id string
     */
    @Override
    public String getPathSegmentName() {
        return getId();
    }

    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder(getName());
        if (sb.indexOf("role") < 1) {
            sb.append("-role");
        }
        sb.append(JSON);
        return sb.toString();
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName());
        return sb.toString();
    }

}
