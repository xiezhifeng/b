package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import it.webdriver.com.atlassian.confluence.pageobjects.ApplinkSetupSuggestionDialog;

import org.json.JSONException;
import org.junit.Test;

import com.atlassian.confluence.it.Space;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;

public class NoApplinkTest extends AbstractJIMTest {

	
	@Override
	public void setup() throws Exception 
	{
		super.setup();
		removeAllAppLink();
	}
	
	@Override
	protected void setupTrustedAppLink() throws IOException, JSONException {
		// don't setup anything
	}
	
	@Test
    public void testOpenJIMDialogWithAdminAccount()
    {
		assertTrue("Couldn't open setup applink suggestion dialog", openSetupAssistantDialog().isSetConnectionButtonVisible());
    }

	@Test
	public void testOpenJIMDialogWithNonAdminAccount()
	{
		super.product.loginAndCreatePage(User.TEST, Space.TEST);
		assertTrue("Couldn't open contact admin to create jira applink dialog", openSetupAssistantDialog().isContactAdminButtonVisible());
	}

	private ApplinkSetupSuggestionDialog openSetupAssistantDialog() 
	{
	    MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
	    macroBrowserDialog.searchForFirst("embed jira issues").select();
	    return product.getPageBinder().bind(ApplinkSetupSuggestionDialog.class);
	}
}
