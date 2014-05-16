package com.atlassian.confluence.plugins.jiracharts.model;

import java.util.List;

public class TwoDimensionalChartModel implements JiraHtmlChartModel
{
    private String xHeading;

    private String yHeading;

    private boolean showTotals;

    private int totalRows;

    private Row firstRow;

    private List<Row> rows;

    public static class Row
    {
        private List<Cell> cells;

        public List<Cell> getCells()
        {
            return cells;
        }

        public void setCells(List<Cell> cells)
        {
            this.cells = cells;
        }
    }

    public static class Cell
    {
        private String markup;

        private String[] classes;

        public String getMarkup()
        {
            return markup;
        }

        public void setMarkup(String markup)
        {
            this.markup = markup;
        }

        public String[] getClasses()
        {
            return classes;
        }

        public String getCssClass()
        {
            String cssClass = "";
            if (classes == null || classes.length == 0) return cssClass;

            for (String className :  classes)
            {
                cssClass += className + " ";
            }
            return cssClass;
        }

        public void setClasses(String[] classes)
        {
            this.classes = classes;
        }
    }

    public String getxHeading()
    {
        return xHeading;
    }

    public void setxHeading(String xHeading)
    {
        this.xHeading = xHeading;
    }

    public String getyHeading()
    {
        return yHeading;
    }

    public void setyHeading(String yHeading)
    {
        this.yHeading = yHeading;
    }

    public Row getFirstRow()
    {
        return firstRow;
    }

    public void setFirstRow(Row firstRow)
    {
        this.firstRow = firstRow;
    }

    public List<Row> getRows()
    {
        return rows;
    }

    public void setRows(List<Row> rows)
    {
        this.rows = rows;
    }

    public boolean isShowTotals()
    {
        return showTotals;
    }

    public void setShowTotals(boolean showTotals)
    {
        this.showTotals = showTotals;
    }

    public int getTotalRows()
    {
        return totalRows;
    }

    public void setTotalRows(int totalRows)
    {
        this.totalRows = totalRows;
    }
}
