package com.atlassian.confluence.plugins.jiracharts.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ChartStatTypeResponse
{

    @SerializedName("stats")
    private List<StatTypeModel> statTypeModels;

    public ChartStatTypeResponse()
    {
    }

    /**
     * @return the statTypeModels
     */
    public List<StatTypeModel> getStatTypeModels()
    {
        return statTypeModels;
    }

    /**
     * @param statTypeModels the statTypeModels to set
     */
    public void setStatTypeModels(List<StatTypeModel> statTypeModels)
    {
        this.statTypeModels = statTypeModels;
    }

}
