package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 9/28/2018
 * Time: 9:01 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FusionLink implements IdentifiableString {

    public static final String DEPENDS_ON="dependsOn";
    public static final String SUPPORTS="supports";
    public static final String IS_PART_OF="isPartOf";
    public static final String HAS_PART="hasPart";
    public static final String RELATES_TO="relatesTo";
    private static final String AMPER = "&";

    String subject;
    String object;
    String linkType;

    public FusionLink() {
    }

    public FusionLink(String subject, String object, String linkType) {
        this.subject = subject;
        this.object = object;
        this.linkType = linkType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    @Override
    public String generateFileName() {
        StringBuilder sb=new StringBuilder();

        if (subject!=null ) {
            sb.append(parseValue(subject));
        }
        if (linkType!=null) {
            if (sb.length()>1) {
                sb.append("-");
            }
            sb.append(parseValue(linkType));
        }
        if (object!=null) {
            if (sb.length()>1) {
                sb.append("-");
            }
            sb.append(parseValue(object));
        }

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
        StringBuilder sb = new StringBuilder();
        if (subject!=null) {
            sb.append("subject=").append(subject);
        }
        if (object!=null) {
            if (sb.length()>7) {
                sb.append(AMPER); //ampersand
            }
            sb.append("object=").append(subject);
        }
        if (linkType!=null) {
            if (sb.length()>7) {
                sb.append(AMPER); //ampersand
            }
            sb.append("linkType=").append(linkType);
        }
        //Add a question mark to the front

//        return new StringBuilder("?").append(sb).toString();
        return sb.toString();
    }

    /**
     * Values can be type:name or relationship. If it has a colon, use the second value. Otherwise use the fisrt and only value
     * @param val
     * @return
     */
    String parseValue(String val) {
        if (val == null) {
            return "";
        }
        String[] valArray = val.split(":");
        if (valArray.length <2 ) {
            return valArray[0];
        }
        return valArray[1];
    }
}
