package com.atlassian.confluence.plugins.jiracharts.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.annotations.SerializedName;

@XmlRootElement
public class StatTypeModel
{

    @SerializedName("value")
    @XmlElement
    private String value;

    @SerializedName("label")
    @XmlElement
    private String label;

    public StatTypeModel()
    {
    }
    
    public StatTypeModel(String value, String label)
    {
        this.value = value;
        this.label = label;
    }

    /**
     * @return the value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

}
