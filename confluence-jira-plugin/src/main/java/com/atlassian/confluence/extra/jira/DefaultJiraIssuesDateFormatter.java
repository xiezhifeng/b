package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DefaultJiraIssuesDateFormatter implements JiraIssuesDateFormatter {

    private static final Logger log = Logger.getLogger(DefaultJiraIssuesDateFormatter.class);

    private static final String MAIL_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final String STATIC_MODE_DATE_FORMAT = "MMM dd, yyyy";     //Format to be rendered in static JIRA issues view

    public String formatDate(Locale userLocale, String dateString)
    {
        String date = reformatDateInUserLocale(dateString, userLocale, STATIC_MODE_DATE_FORMAT);
        if (StringUtils.isEmpty(date))
        {
            try
            {
                date = reformatDateInDefaultLocale(dateString, userLocale, STATIC_MODE_DATE_FORMAT);
            }
            catch (ParseException pe)
            {
                log.debug(dateString + " cannot be parsed ", pe);
                return dateString;
            }
        }
        return StringUtils.isEmpty(date) ? dateString : date;
    }

    public String reformatDateInUserLocale(String value, Locale userLocale, String dateFormat)
    {
        if (value == null) {
            return "";
        }
        try
        {
            DateFormat mailFormatDate = new SimpleDateFormat(MAIL_DATE_FORMAT, userLocale);
            DateFormat dateValueFormat = new SimpleDateFormat(dateFormat, userLocale);
            return dateValueFormat.format(mailFormatDate.parse(value));
        }
        catch (ParseException pe)
        {
            log.debug(value + " is not in user locale " + userLocale, pe);
            return null;
        }
    }

    public String reformatDateInDefaultLocale(String value, Locale userLocale, String dateFormat) throws ParseException
    {
        Date date = GeneralUtil.convertMailFormatDate(value);
        return date == null? null : new SimpleDateFormat(dateFormat, userLocale).format(GeneralUtil.convertMailFormatDate(value));
    }

}
