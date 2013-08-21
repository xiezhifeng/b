package com.atlassian.confluence.extra.jira;

import java.text.ParseException;
import java.util.Locale;

public interface JiraIssuesDateFormatter {

    String formatDate(Locale userLocale, String dateString);

    /**
     * Reformat the date sent from JIRA to the format displayed in JIRA issues macro in user's locale
     *
     * @param value
     * Date in string. Expected in format EEE, d MMM yyyy HH:mm:ss Z
     * @param userLocale
     * User locale
     * @param dateFormat
     * The desired output format. In dynamic rendered mode, the value is dd/MMM/yy. In static rendered mode, the value is MMM dd, yyyy
     *
     * @return
     * Date in string with the desired format
     */
    String reformatDateInUserLocale(String value, Locale userLocale, String dateFormat);

    /**
     * Reformat the date sent from JIRA to the format displayed in JIRA issues macro in default locale
     *
     * @param value
     * Date in string. Expected in format EEE, d MMM yyyy HH:mm:ss Z
     * @param userLocale
     * User locale
     * @param dateFormat
     * The desired output format. In dynamic rendered mode, the value is dd/MMM/yy. In static rendered mode, the value is MMM dd, yyyy
     *
     * @return
     * Date in string with the desired format
     */
    String reformatDateInDefaultLocale(String value, Locale userLocale, String dateFormat) throws ParseException;

}
