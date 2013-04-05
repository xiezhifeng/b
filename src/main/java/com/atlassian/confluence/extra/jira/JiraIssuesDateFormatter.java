package com.atlassian.confluence.extra.jira;

import java.text.ParseException;
import java.util.Locale;

public interface JiraIssuesDateFormatter {

    String formatDate(Locale userLocale, String dateString);

    String convertDateInUserLocale(String value, Locale userLocale, String dateFormat);

    String convertDateInDefaultLocale(String value, Locale userLocale, String dateFormat) throws ParseException;

}
