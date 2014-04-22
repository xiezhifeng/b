package com.atlassian.confluence.plugins.jiracharts.model;

import java.io.Serializable;
import java.util.List;

public class TwoDimensionalChart implements Serializable
{
    private String xHeading;

    private String yHeading;

    private Row firstRow;

    private List<Row> rows;

    public static class Row
    {
        private List<Cell> cells;
    }

    public static class Cell
    {
        private String markup;

        private String[] classes;
    }
}
