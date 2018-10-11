package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Calendar;

/**
 * User object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements IdentifiableString {
    private String id;
    private String realmName;
    private String username;
    private String[] roleNames;
    private Permission[] permissions;
    private Calendar createdAt;
    private Calendar updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String[] roleNames) {
        this.roleNames = roleNames;
    }

    public Permission[] getPermissions() {
        return permissions;
    }

    public void setPermissions(Permission[] permissions) {
        this.permissions = permissions;
    }

    public Calendar getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Calendar createdAt) {
        this.createdAt = createdAt;
    }

    public Calendar getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Calendar updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder(getUsername());
        return sb.toString();
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName());
        return sb.toString();
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        if (!getRealmName().equals(user.getRealmName())) return false;
        return getUsername().equals(user.getUsername());
    }

    @Override
    public int hashCode() {
        int result = getRealmName().hashCode();
        result = 31 * result + getUsername().hashCode();
        return result;
    }
}
