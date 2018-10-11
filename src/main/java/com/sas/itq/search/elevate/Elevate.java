//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.10.13 at 08:58:34 AM EDT 
//


package com.sas.itq.search.elevate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="query" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="doc" type="{}docType"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="query" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "query"
})
@XmlRootElement(name = "elevate")
public class Elevate {

    /**
     * Map of Query elevation spreadsheets
     */
    public static final Map<String, Integer> QUERY_ELEVATION_COLUMNS;
    public static final String QUERY = "query";
    public static final String ID = "id";

    static {
        Map<String, Integer> quMap = new HashMap<>();
        quMap.put("query", 0);
        quMap.put("id", 1);
        QUERY_ELEVATION_COLUMNS = Collections.unmodifiableMap(quMap);
    }

    protected List<Query> query;

    /**
     * Gets the value of the query property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the query property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getQuery().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Query }
     */
    public List<Query> getQuery() {
        if (query == null) {
            query = new ArrayList<Query>();
        }
        return this.query;
    }


    /**
     * <p>Java class for anonymous complex type.
     * <p>
     * <p>The following schema fragment specifies the expected content contained within this class.
     * <p>
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="doc" type="{}docType"/>
     *       &lt;/sequence>
     *       &lt;attribute name="query" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "query")
    public static class Query {

        @XmlElement(name = "doc", required = true)
        protected List<DocType> doc;
        @XmlAttribute(name = "text", required = true)
        protected String text;

        /**
         * Gets the value of the doc property.
         *
         * @return possible object is
         * {@link DocType }
         */
        public List<DocType> getDocs() {
            if (doc == null) {
                doc = new ArrayList<>();
            }
            return doc;
        }

        /**
         * Sets the value of the doc property.
         *
         * @param value allowed object is
         *              {@link DocType }
         */
        public void setDoc(List<DocType> value) {
            this.doc = value;
        }

        public void addDoc(DocType value) {
            this.getDocs().add(value);
        }

        /**
         * Gets the value of the query property.
         *
         * @return possible object is
         * {@link String }
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the value of the query property.
         *
         * @param value allowed object is
         *              {@link String }
         */
        public void setText(String value) {
            this.text = value;
        }

    }

}
