package org.exoplatform.wiki.utils;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.diff.DiffResult;
import org.exoplatform.commons.diff.DiffService;
import org.exoplatform.commons.dlp.processor.DlpOperationProcessor;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceApplication;
import org.exoplatform.social.core.space.SpaceTemplate;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.space.spi.SpaceTemplateService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.WikiPageHistory;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
  public static final String                               SLASH                            = "SLASH";

  public static final String                               DOT                              = "DOT";

  public static final String                               SPACE                            = "space";

  public static final String                               PAGE                             = "page";

  public static final String                               NOTE_LINK                        = "class=\"noteLink\" href=\"//-";

  public static final String                               ANONYM_IDENTITY                  = "__anonim";

  private static final Log                                 log_                             = ExoLogger.getLogger(Utils.class);

  public static final String                               COMPARE_REVISION                 = "CompareRevision";

  public static final String                               VER_NAME                         = "verName";

  final private static String                              MIMETYPE_TEXTHTML                = "text/html";

  private static final Map<String, Map<String, WikiPageHistory>> editPageLogs               = new HashMap<>();

  public static final String                               WIKI_RESOUCE_BUNDLE_NAME         = "locale.wiki.service.WikiService";

  private static final String                              ILLEGAL_SEARCH_CHARACTERS        = "\\!^()+{}[]:-\"";

  private static final String                              ILLEGAL_NAME_CHARACTERS          = "*|\":[]/',^<>";

  public static final String                               SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE = "_A_A_";

  public static final String                               NOTES_METADATA_OBJECT_TYPE       = "notes";

  public static final String                               WIKI_APP_ID                      = "ks-wiki:spaces";

  public static final String                               PAGE_ID_KEY                      = "page_id";

  public static final String                               PAGE_TYPE_KEY                    = "page_type";

  public static final String                               PAGE_OWNER_KEY                   = "page_owner";

  public static String normalizeUploadedFilename(String name) {
    name = name.replace("%22", "\"");  // Fix the bug in Chrome which a double quotes is encoded to %22
    name = name.replace("\\\"", "\"");  // Fix the bug in Firefox which a double quotes is escaped to \\"

    name = Utils.escapeIllegalCharacterInName(name);
    return name;
  }

  public static String escapeIllegalCharacterInQuery(String query) {
    String ret = query;
    if (ret != null) {
      for (char c : ILLEGAL_SEARCH_CHARACTERS.toCharArray()) {
        ret = ret.replace(c + "", "\\" + c);
      }
      ret = ret.replace("'", "''");
    }
    return ret;
  }
  
  public static String escapeIllegalCharacterInName(String name) {
    if (name == null) return null;
    else if (".".equals(name)) return "_";
    else {
      int first = name.indexOf('.');
      int last = name.lastIndexOf('.');
      //if only 1 dot character
      if (first != -1 && first == last && ( first == 0 || last == name.length() - 1)) {
        name = name.replace('.', '_');
      } 
      for (char c : ILLEGAL_NAME_CHARACTERS.toCharArray())
        name = name.replace(c, '_');
      return name;
    }
  }
  
  public static String getPortalName() {
    return PortalContainer.getCurrentPortalContainerName();
  }
  
  /**
   * Get resource bundle from given resource file
   *
   * @param key key
   * @param cl ClassLoader to load resource file
   * @return The value of key in resource bundle
   */
  public static String getWikiResourceBundle(String key, ClassLoader cl) {
    Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
    ResourceBundle resourceBundle = ResourceBundle.getBundle(WIKI_RESOUCE_BUNDLE_NAME, locale,cl);
    return resourceBundle.getString(key);
  }
  
  /**
   * Log the edit page action of user
   * 
   * @param pageParams The page that has been editing
   * @param username The name of user that editing wiki page
   * @param updateTime The time that this page is edited
   * @param draftName The name of draft for this edit
   * @param isNewPage Is the wiki page a draft or not
   */
  public static void logEditPageTime(WikiPageParams pageParams, String username, long updateTime, String draftName, boolean isNewPage) {
    String pageId = pageParams.getPageName();
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage == null) {
      logByPage = new HashMap<String, WikiPageHistory>();
      editPageLogs.put(pageId, logByPage);
    }
    WikiPageHistory logByUsername = logByPage.get(username);
    if (logByUsername == null) {
      logByUsername = new WikiPageHistory(pageParams, username, draftName, isNewPage);
      logByPage.put(username, logByUsername);
    }
    logByUsername.setEditTime(updateTime);
  }
  
  /**
   * removes the log of user editing page.
   * @param pageParams wiki page params
   * @param user current userName
   */
  public static void removeLogEditPage(WikiPageParams pageParams, String user) {
    String pageId = pageParams.getPageName();
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage != null) {
      logByPage.remove(user);
    }
  }

  /**
   * get user identity.
   * @param userId current userName
   *
   * @return the full name of the user
   */
  public static String getIdentityUser( String userId) {
    IdentityManager identityManager = ExoContainerContext.getService(IdentityManager.class);
    Identity userIdentity =  identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false);
   return userIdentity.getProfile().getFullName();
  }
  
  /**
   * Get the list of user that're editing the wiki page
   * 
   * @param pageId The id of wiki page
   * @return The list of user that're editing this wiki page 
   */
  public static List<String> getListOfUserEditingPage(String pageId) {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    List<String> edittingUsers = new ArrayList<String>();
    List<String> outdateEdittingUser = new ArrayList<String>();
    String currentUser = getCurrentUser();
    
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage != null) {
      // Find all the user that editting this page
      for (String username : logByPage.keySet()) {
        WikiPageHistory log = logByPage.get(username);
        if (System.currentTimeMillis() - log.getEditTime() < wikiService.getEditPageLivingTime()) {
          if (!username.equals(currentUser) && !log.isNewPage()) {
            edittingUsers.add(username);
          }
        } else {
          outdateEdittingUser.add(username);
        }
      }
      
      // Remove all outdate editting user
      for (String username : outdateEdittingUser) {
        logByPage.remove(username);
      }
    }
    return edittingUsers;
  }
  
  /**
   * Get the permalink of current wiki page <br>
   *
   * With the current page param:
   * <ul>
   *   <li>type = "group"</li>
   *   <li>owner = "spaces/test_space"</li>
   *   <li>pageId = "test_page"</li>
   * </ul>
   * <br>
   *
   *  The permalink will be:
   * <ul>
   *   <li>http://int.exoplatform.org/portal/intranet/wiki/group/spaces/test_space/test_page</li>
   * </ul>
   * <br>
   *
   * @param params the wiki oage parms
   * @param hasDowmainUrl if page has domain url
   * @return The permalink of current wiki page
   * @throws Exception if error occured
   */
  public static String getPermanlink(WikiPageParams params, boolean hasDowmainUrl) throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    
    // get wiki webapp name
    String wikiWebappUri = wikiService.getWikiWebappUri();
    
    // Create permalink
    StringBuilder sb = new StringBuilder(wikiWebappUri);
    sb.append("/");
    if (!params.getType().equalsIgnoreCase(WikiType.PORTAL.toString())) {
      sb.append(params.getType().toLowerCase());
      sb.append("/");
      sb.append(org.exoplatform.wiki.utils.Utils.validateWikiOwner(params.getType(), params.getOwner()));
      sb.append("/");
    }
    
    if (params.getPageName() != null) {
      sb.append(params.getPageName());
    }
    
    if (hasDowmainUrl) {
      return getDomainUrl() + fillPortalName(sb.toString());
    }
    return fillPortalName(sb.toString());
  }

  public static String getPageNameForAddingPage() {
    return Utils.getPageNameForAddingPage(null);
  }

  public static String getPageNameForAddingPage(String sessionId) {
    if(sessionId == null || sessionId.isEmpty()) {
      sessionId = StringUtils.EMPTY;
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      if(portalRequestContext != null) {
        HttpServletRequest request = portalRequestContext.getRequest();
        if(request != null && request.getSession(false) != null) {
          sessionId = request.getSession(false).getId();
        }
      }

    }
    String username = Utils.getCurrentUser();
    return username + SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE + sessionId;
  }
  
  private static String getDomainUrl() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder domainUrl = new StringBuilder();
    domainUrl.append(portalRequestContext.getRequest().getScheme());
    domainUrl.append("://");
    domainUrl.append(portalRequestContext.getRequest().getServerName());
    int port = portalRequestContext.getRequest().getServerPort();
    if (port != 80) {
      domainUrl.append(":");
      domainUrl.append(port);
    }
    return domainUrl.toString();
  }
  
  private static String fillPortalName(String url) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), url);
    return nodeURL.setResource(resource).toString(); 
  }

  /**
   * Get the editting log of wiki page
   * 
   * @param pageId The id of wiki page to get log
   * @return The editting log of wiki pgae
   */
  public static Map<String, WikiPageHistory> getLogOfPage(String pageId) {
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage == null) {
      logByPage = new HashMap<String, WikiPageHistory>();
    }
    return logByPage;
  }
  
  /**
   * Validate {@code wikiOwner} depending on {@code wikiType}. <br>
   * If wikiType is {@link PortalConfig#GROUP_TYPE}, {@code wikiOwner} is checked to removed slashes at the begin and the end point of it.
   * @param wikiType the wiki type
   * @param wikiOwner the wiki owner
   * @return wikiOwner after validated.
   */ 
  public static String validateWikiOwner(String wikiType, String wikiOwner){
    if(wikiType != null && wikiType.equals(PortalConfig.GROUP_TYPE) && StringUtils.isNotEmpty(wikiOwner)) {
      if(wikiOwner.startsWith("/")){
        wikiOwner = wikiOwner.substring(1,wikiOwner.length());
      }
      if(wikiOwner.endsWith("/")){
        wikiOwner = wikiOwner.substring(0,wikiOwner.length()-1);
      }
    }
    return wikiOwner;
  }
  
  public static String getDefaultRestBaseURI() {
    StringBuilder sb = new StringBuilder();
    sb.append("/");
    sb.append(PortalContainer.getCurrentPortalContainerName());
    sb.append("/");
    sb.append(PortalContainer.getCurrentRestContextName());
    return sb.toString();
  }
  
  public static String getDocumentURL(WikiContext wikiContext) {
    if (wikiContext.getPortalURL() == null && wikiContext.getPortletURI() == null) {
      return wikiContext.getPageName();
    }
    StringBuilder sb = new StringBuilder();
    sb.append(wikiContext.getPortalURL());
    sb.append(wikiContext.getPortletURI());
    sb.append("/");
    if (!PortalConfig.PORTAL_TYPE.equalsIgnoreCase(wikiContext.getType())) {
      sb.append(wikiContext.getType().toLowerCase());
      sb.append("/");
      sb.append(Utils.validateWikiOwner(wikiContext.getType(), wikiContext.getOwner()));
      sb.append("/");
    }
    sb.append(wikiContext.getPageName());
    return sb.toString();
  }
  
  public static String getCurrentUser() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null; 
  }
  
  public static boolean isDescendantPage(Page page, Page parentPage) throws WikiException {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    // if page and parentPage are the same page, it is considered as a descendant
    if(page.getWikiType().equals(parentPage.getWikiType()) && page.getWikiOwner().equals(parentPage.getWikiOwner())
            && page.getName().equals(parentPage.getName())) {
      return true;
    }
    Page parentOfPage = wikiService.getParentPageOf(page);
    // we reach the Wiki root
    if(parentOfPage == null) {
      return false;
    }
    // if the parent of the given page is the same than the parentPage, page is a descendant of parentPage
    if(parentOfPage.getWikiType().equals(parentPage.getWikiType()) && parentOfPage.getWikiOwner().equals(parentPage.getWikiOwner())
            && parentOfPage.getName().equals(parentPage.getName())) {
      return true;
    } else {
      // otherwise we continue to go up in the page tree
      return isDescendantPage(parentOfPage, parentPage);
    }
  }
  
  public static Object getObjectFromParams(WikiPageParams param) throws WikiException {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    String wikiType = param.getType();
    String wikiOwner = param.getOwner();
    String wikiPageId = param.getPageName();

    if (wikiOwner != null && wikiPageId != null) {
      if (!wikiPageId.equals(NoteConstants.NOTE_HOME_NAME)) {
        // Object is a page
        Page expandPage = wikiService.getPageByRootPermission(wikiType, wikiOwner, wikiPageId);
        return expandPage;
      } else {
        // Object is a Home page
        Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner);
        if(wiki != null) {
          Page wikiHome = wiki.getWikiHome();
          return wikiHome;
        } else {
          return null;
        }
      }
    } else if (wikiOwner != null) {
      // Object is a wiki
      Wiki wiki =  wikiService.getWikiByTypeAndOwner(wikiType.toUpperCase(), wikiOwner);
      return wiki;
    } else if (wikiType != null) {
      // Object is a space
      return wikiType;
    } else {
      return null;
    }
  }
  
  public static Deque<WikiPageParams> getStackParams(Page page) throws WikiException {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    Deque<WikiPageParams> stack = new ArrayDeque<>();
    Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
    if (wiki != null) {
      while (page != null) {
        stack.push(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName()));
        page = wikiService.getParentPageOf(page);
      }      
    }
    return stack;
  }
  
  
  public static WikiPageParams getWikiPageParams(Page page) {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    try {
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
      String wikiType = wiki.getType();
      WikiPageParams params = new WikiPageParams(wikiType, wiki.getOwner(), page.getName());
      return params;
    } catch(Exception e) {
      log_.error("Cannot build wiki page params from wiki page " + page.getWikiType() + ":" + page.getWikiOwner()
              + ":" + page.getName() + " - Cause : " + e.getMessage(), e);
      return null;
    }
  }
  
  public static String getWikiOnChangeContent(Page page)
          throws WikiException, DifferentiationFailedException {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WikiService wikiService = container.getComponentInstanceOfType(WikiService.class);
    DiffService diffService = container.getComponentInstanceOfType(DiffService.class);
    
    // Get differences
    String currentVersionContent = page.getContent() != null ? new String(page.getContent()) : StringUtils.EMPTY;
    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    String previousVersionContent = StringUtils.EMPTY;
    if(versions != null && !versions.isEmpty()) {
      PageVersion previousVersion = versions.get(0);
      previousVersionContent = previousVersion.getContent();
    }
    DiffResult diffResult = diffService.getDifferencesAsHTML(previousVersionContent,
                                                             currentVersionContent,
                                                             false);
    
    if (diffResult.getChanges() == 0) {
      diffResult.setDiffHTML("No changes, new revision is created.");
    } 

    StringBuilder sbt = new StringBuilder();
    sbt.append("<html>")
        .append("  <body>")
            .append(insertStyle(diffResult.getDiffHTML()))
            .append("  </body>")
            .append("</html>");
    return sbt.toString();
  }
  
  private static boolean isEnabledUser(String userName) throws WikiException {
    OrganizationService orgService = ExoContainerContext.getService(OrganizationService.class);
    try {
      return orgService.getUserHandler().findUserByName(userName) != null;
    } catch (Exception e) {
      throw new WikiException("Cannot check if user " + userName + " is enabled", e);
    }
  }
  
  public static String getEmailUser(String userName) throws WikiException {
    OrganizationService organizationService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(OrganizationService.class);
    User user;
    try {
      user = organizationService.getUserHandler().findUserByName(userName);
      String email = user.getEmail();
      return email;
    } catch (Exception e) {
      throw new WikiException("Cannot get email of user " + userName, e);
    }
  }
  
  public static HashMap<String, IDType> getACLForAdmins() {
    HashMap<String, IDType> permissionMap = new HashMap<String, IDType>();
    UserACL userACL = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
    permissionMap.put(userACL.getSuperUser(), IDType.USER);
    for (String group : userACL.getPortalCreatorGroups()) {
      if (!StringUtils.isEmpty(group)) {
        permissionMap.put(group, IDType.MEMBERSHIP);
      }
    }
    return permissionMap;
  }

  private static String makeNotificationSender(String from) {
    InternetAddress addr = null;
    if (from == null) return null;
    try {
      addr = new InternetAddress(from);
    } catch (AddressException e) {
      if (log_.isDebugEnabled()) { log_.debug("value of 'from' field in message made by forum notification feature is not in format of mail address", e); }
      return null;
    }
    Properties props = new Properties(System.getProperties());
    String mailAddr = props.getProperty("gatein.email.smtp.from");
    if (mailAddr == null || mailAddr.length() == 0) mailAddr = props.getProperty("mail.from");
    if (mailAddr != null) {
      try {
        String companyName = NotificationPluginUtils.getBrandingPortalName();
        InternetAddress serMailAddr = new InternetAddress(mailAddr);
        addr.setAddress(serMailAddr.getAddress());
        return companyName + "<" + addr.toUnicodeString() + ">";
      } catch (AddressException e) {
        if (log_.isDebugEnabled()) { log_.debug("value of 'gatein.email.smtp.from' or 'mail.from' in configuration file is not in format of mail address", e); }
        return null;
      }
    } else {
      return null;
    }
  }
  

  private static String insertStyle(String rawHTML) {
    String result = rawHTML;
    result = result.replaceAll("class=\"diffaddword\"", "style=\"background: #b5ffbf;\"");
    result = result.replaceAll("<span class=\"diffremoveword\">",
                               "<span style=\" background: #ffd8da;text-decoration: line-through;\">");
    result = result.replaceAll("<pre class=\"diffremoveword\">",
                               "<pre style=\" background: #ffd8da;\">");
    return result;
  }
  
  /*
   * get URL to public on social activity
   */
  public static String getURL(String url, String verName){
    StringBuffer strBuffer = new StringBuffer();
    strBuffer.append(url).append("?").append(WikiContext.ACTION).append("=").append(COMPARE_REVISION).append("&").append(VER_NAME).append("=").append(verName);
    return strBuffer.toString();
  }
  
  public static long countSearchResult(WikiSearchData data) throws Exception {
    data.setOffset(0);
    data.setLimit(Integer.MAX_VALUE);
    WikiService wikiservice = (WikiService) PortalContainer.getComponent(WikiService.class);
    PageList<SearchResult> results = wikiservice.search(data);
    return results.getAll().size();

  }
  
  public static String getAttachmentCssClass(Attachment attachment, String append) throws Exception {
    Class<?> dmsMimeTypeResolverClass = Class.forName("org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver");
    Object dmsMimeTypeResolverObject =
        dmsMimeTypeResolverClass.getDeclaredMethod("getInstance", null).invoke(null, null);
    Object mimeType = dmsMimeTypeResolverClass
      .getMethod("getMimeType", new Class[] { String.class})
      .invoke(dmsMimeTypeResolverObject, new Object[]{new String(attachment.getFullTitle().toLowerCase())});

    StringBuilder cssClass = new StringBuilder();
    cssClass.append(append);
    cssClass.append("FileDefault");
    cssClass.append(" ");
    cssClass.append(append);
    cssClass.append("nt_file");
    cssClass.append(" ");
    cssClass.append(append);
    cssClass.append(((String)mimeType).replaceAll("/|\\.", ""));
    return cssClass.toString();
  }

  public static boolean isDlpFeatureEnabled() {
    ExoFeatureService featureService = CommonsUtils.getService(ExoFeatureService.class);
    return featureService.isActiveFeature(DlpOperationProcessor.DLP_FEATURE);
  }

  /**
   * gets rest context name
   * @return rest context name
   */
  public static String getRestContextName() {
    return PortalContainer.getCurrentRestContextName();
  }

  public static String getPageUrl(Page page){
    String appName= page.getAppName();
    if(StringUtils.isEmpty(appName)){
      appName = getWikiAppNameInSpace(page.getWikiOwner());
    }
    String spaceUri = getSpacesURI(page);
    StringBuilder spaceUrl = new StringBuilder("/portal");
    spaceUrl.append(spaceUri);
    spaceUrl.append("/");
    spaceUrl.append(appName);
    spaceUrl.append("/");
    if (!StringUtils.isEmpty(page.getId())) {
      spaceUrl.append(page.getId());
    }
    return spaceUrl.toString();
  }

  public static String getSpacesURI(Page page) {
    try {
    QualifiedName REQUEST_HANDLER = QualifiedName.create("gtn", "handler");
    QualifiedName REQUEST_SITE_TYPE = QualifiedName.create("gtn", "sitetype");
    QualifiedName REQUEST_SITE_NAME = QualifiedName.create("gtn", "sitename");
    QualifiedName PATH = QualifiedName.create("gtn", "path");
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    WebAppController webAppController = CommonsUtils.getService(WebAppController.class);
    Router router = webAppController.getRouter();
      Space space = spaceService.getSpaceByGroupId(page.getWikiOwner());
      if(space==null){
        return "";
      }
      Map<QualifiedName, String> qualifiedName = new HashedMap();
      qualifiedName.put(REQUEST_HANDLER, "portal");
      qualifiedName.put(REQUEST_SITE_TYPE, "group");

        StringBuilder urlBuilder = new StringBuilder();
        qualifiedName.put(REQUEST_SITE_NAME, space.getGroupId());
        qualifiedName.put(PATH, space.getPrettyName());
        router.render(qualifiedName, new URIWriter(urlBuilder));
        return(urlBuilder.toString());

    } catch (Exception e) {
      return "";
    }
  }
  public static String getWikiAppNameInSpace(String spaceId) {
    try {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceByGroupId(spaceId);
      SpaceTemplateService spaceTemplateService = CommonsUtils.getService(SpaceTemplateService.class);
      SpaceTemplate spaceTemplate = spaceTemplateService.getSpaceTemplateByName(space.getTemplate());
      List<SpaceApplication> spaceTemplateApplications = spaceTemplate.getSpaceApplicationList();
      if (spaceTemplateApplications != null) {
        for (SpaceApplication spaceApplication : spaceTemplateApplications) {
          if ("WikiPortlet".equals(spaceApplication.getPortletName())) {
            return spaceApplication.getUri();
          }
        }
      }
    } catch (Exception e) {
      log_.warn("Cannot get Wiki App anme");
    }
    return "notes";
  }


  public static List<String> unzip(String zipFilePath, String folderPath) throws IOException {
    List<String> files = new ArrayList<>();
    File destDir = new File(folderPath);
    if (!destDir.exists()) {
      destDir.mkdir();
    }
    try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))){
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        String filePath = folderPath + File.separator + entry.getName();
        if (!entry.isDirectory()) {
          extractFile(zipIn, filePath);
          files.add(filePath);
        } else {
          File dir = new File(filePath);
          dir.mkdirs();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    }
    return files;
  }


  public static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    try(BufferedOutputStream bos =  new BufferedOutputStream(new FileOutputStream(filePath));) {
      byte[] bytesIn = new byte[4096];
      int read = 0;
      while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }
  }

  public static String html2text(String html) {
    Document doc = Jsoup.parse(html);
    return doc.text();
  }

}
