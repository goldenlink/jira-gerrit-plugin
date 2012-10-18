package com.meetme.plugins.jira.gerrit.projectpanel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.JDKDSASigner.stdDSA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanel;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanelModuleDescriptor;
import com.atlassian.jira.plugin.projectpanel.impl.AbstractProjectTabPanel;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.util.OutlookDate;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.meetme.plugins.jira.gerrit.tabpanel.GerritReviewIssueAction;
import com.meetme.plugins.jira.gerrit.tabpanel.GerritReviewsTabPanel;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

public class GerritReviewsProjectPanel extends AbstractProjectTabPanel
{
  private static final Logger log = LoggerFactory.getLogger(GerritReviewsProjectPanel.class);

  private final UserManager userManager;
  private final ApplicationProperties applicationProperties;
  private final GerritConfiguration configuration;
  private final IssueReviewsManager reviewsManager;
  private final DateTimeFormatter dateTimeFormatter;
  private final I18nResolver i18n;
  private static final Calendar cal = Calendar.getInstance();
  
  public GerritReviewsProjectPanel(JiraAuthenticationContext jiraAuthenticationContext,
      UserManager userManager, ApplicationProperties applicationProperties, GerritConfiguration configuration,
      IssueReviewsManager reviewsManager, I18nResolver i18n, DateTimeFormatter dateTimeFormatter)
  {
    super(jiraAuthenticationContext);
    this.userManager = userManager;
    this.applicationProperties = applicationProperties;
    this.configuration = configuration;
    this.dateTimeFormatter = dateTimeFormatter.forLoggedInUser();
    this.reviewsManager = reviewsManager;
    this.i18n = i18n;
  }
  
  @Override
  public boolean showPanel(BrowseContext context)
  {
    return true;
  }
  
  @Override
  public Map<String,Object> createVelocityParams(BrowseContext context)
  {
    Map<String, Object> ctx = super.createVelocityParams(context);
    List<GerritChange> openedReviews, mergedReviews, abandonnedReviews;
    ctx.put("test", "Test of context.");
    try
    {
      openedReviews = reviewsManager.getReviewsForProject(context.getProject().getKey().toLowerCase());
      mergedReviews = reviewsManager.getMergedReviews(context.getProject().getKey().toLowerCase());
      abandonnedReviews = reviewsManager.getAbandonnedReviews(context.getProject().getKey().toLowerCase());
    }
    catch (GerritQueryException e)
    {
      e.printStackTrace();
      ctx.put("Error", e.getMessage());
      return ctx;
    } 
    
    if(openedReviews.isEmpty())
    {
      ctx.put("size",0);
    } else {
      ctx.put("size",openedReviews.size());
      ctx.put("changes", openedReviews);
    }
    ctx.put("merged", mergedReviews);
    ctx.put("abandoned", abandonnedReviews);
    
    int[] mergeFreq = new int[52];
    
    Date changeDate = null;
    int week;
    for (GerritChange change : mergedReviews)
    {
      changeDate = change.getLastUpdated();
      cal.setTime(changeDate);
      week = cal.get(Calendar.WEEK_OF_YEAR);
      mergeFreq[week]++;
    }
    StringBuffer stb = new StringBuffer();
    stb.append("[['Week','Merges']");
    for (int i = 0; i < mergeFreq.length; i++){
      stb.append(",['" + i + "',"+mergeFreq[i]+"]");
    }
    stb.append("]");
    ctx.put("mergeFrequency", stb.toString());
    return ctx;
    
  }

}
