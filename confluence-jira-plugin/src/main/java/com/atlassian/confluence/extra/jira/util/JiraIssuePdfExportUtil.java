package com.atlassian.confluence.extra.jira.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;


public class JiraIssuePdfExportUtil
{
    
    private static final int PDF_EXPORT_DEFAULT_FONT_SIZE = 8;
    
    private static final Logger log = Logger.getLogger(JiraIssuePdfExportUtil.class);
    
    private JiraIssuePdfExportUtil()
    {
        
    }

    public static void addedHelperDataForPdfExport(Map<String, Object> contextMap, int numberOfColumns)
    {
        if (numberOfColumns > 0)
        {
            // Assign font size for a range columns in JIM table. Default font size(8pt) will apply for JIM table contains from 1 to 11 columns.
            FontRangeHelper.getInstance()
                .setRange(1, 11, 8)
                .setRange(12, 12, 7)
                .setRange(13, 13, 6)
                .setRange(14, 16, 5)
                .setRange(17, 21, 4)
                .setRange(22, 25, 3)
                .setRange(26, 28, 2)
                .setRange(29, Integer.MAX_VALUE - 1, 1);
            contextMap.put("fontSize", FontRangeHelper.getInstance().getFontSize(numberOfColumns) + "pt");
            contextMap.put("statusFontSize", (FontRangeHelper.getInstance().getFontSize(numberOfColumns) -1) + "pt");
        }
    }

    private static class FontRangeHelper
    {

        private Map<Integer[], Integer> internalRangeMap = new HashMap<Integer[], Integer>();
        
        private static FontRangeHelper instance = null;
        
        public static FontRangeHelper getInstance()
        {
            if (null == instance)
            {
                instance = new FontRangeHelper();
            }
            return instance;
        }

        private FontRangeHelper()
        {
            
        }
        public FontRangeHelper setRange(int start, int end, int fontSize)
        {
            Integer[] range = new Integer[2];
            range[0] = start;
            range[1] = end;
            this.internalRangeMap.put(range, fontSize);
            return this;
        }

        public int getFontSize(int numOfColumn)
        {
            for (Entry<Integer[], Integer> entry : internalRangeMap.entrySet())
            {
                Integer[] range = entry.getKey();
                if (numOfColumn >= range[0] && numOfColumn <= range[1])
                {
                    return entry.getValue();
                }
            }
            return PDF_EXPORT_DEFAULT_FONT_SIZE;
        }
    }
}
