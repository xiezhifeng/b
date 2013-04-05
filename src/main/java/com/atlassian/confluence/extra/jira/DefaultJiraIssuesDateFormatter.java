package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DefaultJiraIssuesDateFormatter implements JiraIssuesDateFormatter {

    private static final Logger log = Logger.getLogger(JsonFlexigridResponseGenerator.class);

    private static final String mailDateFormat = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final String STATIC_MODE_DATE_FORMAT = "MMM dd, yyyy";

    public String formatDate(Locale userLocale, String dateString)
    {
        String date = convertDateInUserLocale(dateString, userLocale, STATIC_MODE_DATE_FORMAT);
        if (StringUtils.isEmpty(date))
        {
            try
            {
                date = convertDateInDefaultLocale(dateString, userLocale, STATIC_MODE_DATE_FORMAT);
            }
            catch (ParseException pe)
            {
                log.debug(dateString + " cannot be parsed ", pe);
                return dateString;
            }
        }
        return StringUtils.isEmpty(date) ? dateString : date;
    }

    public String convertDateInUserLocale(String value, Locale userLocale, String dateFormat)
    {
        try
        {
            DateFormat dateValueFormat = new SimpleDateFormat(dateFormat, userLocale);
            DateFormat mailFormatDate = new SimpleDateFormat(mailDateFormat, userLocale);
            return dateValueFormat.format(mailFormatDate.parse(value));
        }
        catch (ParseException pe)
        {
            log.debug(value + " is not in user locale " + userLocale, pe);
            return null;
        }
    }

    public String convertDateInDefaultLocale(String value, Locale userLocale, String dateFormat) throws ParseException
    {
        DateFormat dateValueFormat = new SimpleDateFormat(dateFormat, userLocale);
        Date date = GeneralUtil.convertMailFormatDate(value);
        return date == null? null : dateValueFormat.format(GeneralUtil.convertMailFormatDate(value));
    }

}
