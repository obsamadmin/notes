/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service;

import java.util.List;
import java.util.Map;

import org.exoplatform.commons.diff.DiffResult;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.plugin.WikiEmotionIconsPlugin;
import org.exoplatform.wiki.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Provides functions for processing database
 * with wikis and pages, including: adding, editing, removing and searching for data.
 *
 * @LevelAPI Provisional
 */
public interface WikiService {

  /**
   * Create a new wiki page in the given wiki, under the given parent page.
   *
   * @param wiki It can be Portal, Group, or User.
   * @param parentPageName Name of the parent wiki page.
   * @param page wiki page object to create.
   * @return The new wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public Page createPage(Wiki wiki, String parentPageName, Page page) throws WikiException;

  /**
   * Creates a new Wiki template.
   *
   * @param wiki Wiki of the template
   * @param template The params object which is used for creating the new Wiki template.
   * @throws WikiException if an error occured if an error occured
   */
  public void createTemplatePage(Wiki wiki, Template template) throws WikiException;

  /**
   * Deletes a wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @param userId
   * @return "True" if deleting the wiki page is successful, or "false" if not.
   * @throws WikiException if an error occured if an error occured
   */
  public boolean deletePage(String wikiType, String wikiOwner, String pageId, String userId) throws WikiException;

  /**
   * Deletes a Wiki template.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param templateName Name of the Wiki template.
   * @throws WikiException if an error occured if an error occured
   */
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException;

  /**
   * Renames a wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageName Old name of the wiki page.
   * @param newName New name of the wiki page.
   * @param newTitle New title of the wiki page.
   * @return "True" if renaming the wiki page is successful, or "false" if not.
   * @throws WikiException if an error occured if an error occured
   */
  public boolean renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws WikiException;

  /**
   * Move a wiki Page
   *
   * @param currentLocationParams The current location of the wiki page.
   * @param newLocationParams The new location of the wiki page.
   * @return "True" if moving the wiki page is successful, or "false" if not.
   * @throws WikiException if an error occured if an error occured
   */
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException;

  /**
   * Gets a list of Wiki permissions based on its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki permissions.
   * @throws WikiException if an error occured if an error occured
   */
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException;

  /**
   * Adds a list of permissions to Wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param permissionEntries The list of permissions.
   * @throws WikiException if an error occured if an error occured
   */
  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException;

  /**
   * Gets a wiki page by its unique name in the wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageName Id of the wiki page.
   * @return The wiki page if the current user has the read permission. Otherwise, it is "null".
   * @throws WikiException if an error occured if an error occured
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException;

  /**
   * Gets a wiki page regardless of the current user's permission.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public Page getPageByRootPermission(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Gets a related page of a wiki page which is specified by a given Id.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The related wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Gets a wiki page or its draft if existing by its Id.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page or its draft.
   * @throws WikiException if an error occured if an error occured
   */
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Gets a wiki page based on its unique id.
   *
   * @param id Unique id of the wiki page.
   * @return The wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public Page getPageById(String id) throws WikiException;

  /**
   * Gets a wiki draft page based on its unique id.
   *
   * @param id Unique id of the wiki page.
   * @return The wiki draft page.
   * @throws WikiException if an error occured if an error occured
   */
  public Page getDraftPageById(String id) throws WikiException;

  /**
   * Get renderd content of a page
   * @param page The wiki page
   * @return The rendered content
   */
  public String getPageRenderedContent(Page page);

  /**
   * Add a link between 2 pages
   * @param param First page
   * @param entity Second page
   */
  public void addPageLink(WikiPageParams param, WikiPageParams entity);

  /**
   * Get parent page of a wiki page
   * @param page Wiki page.
   * @return The list of children pages
   * @throws WikiException if an error occured
   */
  public Page getParentPageOf(Page page) throws WikiException;

  /**
   * Get all the children pages of a wiki page
   * @param page Wiki page.
   * @param userId
   * @param withDrafts if set to true returns the children notes and draft notes
   * @return The list of children pages
   * @throws WikiException if an error occured
   */
  List<Page> getChildrenPageOf(Page page, String userId, boolean withDrafts) throws WikiException;

  /**
   * Gets a Wiki template.
   * @param params The params object which is used for creating the Wiki template.
   * @param templateId Id of the wiki template.
   * @return The wiki template.
   * @throws WikiException if an error occured if an error occured
   */
  public Template getTemplatePage(WikiPageParams params, String templateId) throws WikiException;

  /**
   * Gets a list of data which is used for composing the breadcrumb.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page to which the breadcrumb points.
   * @return The list of data.
   * @throws WikiException if an error occured if an error occured
   */
  public List<BreadcrumbData> getBreadcumb(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Gets parameters of a wiki page based on the data stored in the breadcrumb.
   *
   * @param data The data in the breadcrumb that identifies the wiki page.
   * @return The parameters identifying the wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public WikiPageParams getWikiPageParams(BreadcrumbData data) throws WikiException;

  /**
   * Searches in all wiki pages.
   *
   * @param data The data to search.
   * @return Search results.
   * @throws WikiException if an error occured if an error occured
   */
  public PageList<SearchResult> search(WikiSearchData data) throws WikiException;

  /**
   * Searches in all templates.
   *
   * @param data The data to search.
   * @return Search results.
   * @throws WikiException if an error occured if an error occured
   */
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws WikiException;

  /**
   * Checks if a page and its children are duplicated with ones in the target Wiki or not,
   * then gets a list of duplicated pages if any.
   * 
   * @param parentPage The page to check.
   * @param targetWiki The target Wiki to check.
   * @param resultList The list of duplicated wiki pages.
   * @param userId
   * @return The list of duplicated wiki pages.
   * @throws WikiException if an error occured if an error occured
   */
  public List<Page> getDuplicatePages(Page parentPage, Wiki targetWiki, List<Page> resultList, String userId) throws WikiException;

  /**
   * Gets Id of a default Wiki syntax.
   *
   * @return The Id.
   */
  public String getDefaultWikiSyntaxId();

  /**
   * Gets an interval which specifies the periodical auto-saving for pages in Wiki.
   * 
   * @return The interval. Its default value is 30 seconds.
   */
  public long getSaveDraftSequenceTime();
  
  /**
   * Get the living time of edited page
   * 
   * @return The living time of edited page
   */
  public long getEditPageLivingTime();

  /**
   * Gets attachments of the given page, without loading their content
   * @param page The wiki page
   * @return The attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException;

  /**
   * Gets attachments of the given page,
   * and allow to load their attachment content by setting loadContent to true
   * @param page The wiki page
   * @param loadContent treue if need to load the attachement content
   * @return The attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public default List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    return getAttachmentsOfPage(page);
  }

  /**
   * Get the number of attachment of the given page
   * @param page The wiki page
   * @return The number of attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public int getNbOfAttachmentsOfPage(Page page) throws WikiException;

  /**
   * Get a attachment of a the given page by name, without loading its content
   *
   * @param attachmentName The name of the attachment
   * @param page The wiki page
   * @return Attachment
   * @throws WikiException if an error occured if an error occured
   */
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page) throws WikiException;

  /**
   * Get a attachment of a the given page by name,
   * and allow to load the attachment content by setting loadContent to true
   *
   * @param attachmentName The name of the attachment
   * @param page           The wiki page
   * @param loadContent    true to load the attachment content
   * @return attachement
   * @throws WikiException if an error occured if an error occured
   */
  public default Attachment getAttachmentOfPageByName(String attachmentName, Page page, boolean loadContent) throws WikiException {
    return getAttachmentOfPageByName(attachmentName, page);
  }

  /**
   * Add the given attachment to the given page
   * @param attachment The attachment to add
   * @param page The wiki page
   * @throws WikiException if an error occured if an error occured
   */
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException;

  /**
   * Deletes the given attachment of the given page
   * @param attachmentId Id of the attachment
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException;

  /**
   * Gets a Help wiki page based on a given syntax Id.
   *
   * @param syntaxId Id of the syntax.
   * @param fullContent true to get the full help page content, false to get an excerpt
   * @return The Help wiki page.
   * @throws WikiException if an error occured
   */
  @Deprecated
  public Page getHelpSyntaxPage(String syntaxId, boolean fullContent) throws WikiException;

  /**
   * Gets a map of wiki templates based on a given params object.
   * 
   * @param params The params object which is used for getting the wiki templates.
   * @return The map of wiki templates.
   * @throws WikiException if an error occured
   */
  public Map<String, Template> getTemplates(WikiPageParams params) throws WikiException;

  /**
   * Modifies an existing wiki template.
   *
   * @param template The updated wiki template.
   * @throws WikiException if an error occured
   */
  public void updateTemplate(Template template) throws WikiException;

  /**
   * Checks if a wiki page exists or not.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The returned value is "true" if the page exists, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Gets a list of Wiki default permissions.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki default permissions.
   * @throws WikiException if an error occured
   */
  public List<PermissionEntry> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws WikiException;
  
  /**
   * Registers a component plugin into the Wiki service.
   * @param plugin The component plugin to be registered.
   */
  public void addComponentPlugin(ComponentPlugin plugin);

  /**
   * Adds a Wiki template as plugin.
   *
   * @param templatePlugin The wiki template plugin to be added.
   */
  public void addWikiTemplatePagePlugin(WikiTemplatePagePlugin templatePlugin);

  /**
   * Adds a Wiki emotion icons as plugin.
   *
   * @param plugin The wiki emotion icons plugin to be added.
   */
  public void addEmotionIconsPlugin(WikiEmotionIconsPlugin plugin);

  /**
   * Gets listeners of all wiki pages that are registered into the Wiki service.
   * @return The list of listeners.
   */
  public List<PageWikiListener> getPageListeners();

  /**
   * Gets attachment listeners that are registered into the Wiki service.
   * @return The list of attachment listeners.
   */
  public List<AttachmentWikiListener> getAttachmentListeners();

  /**
   * Adds a related page to the current wiki page.
   *
   * @param orginaryPageParams The params object of the current wiki page.
   * @param relatedPageParams The params object of the related page.
   * @throws WikiException if an error occured
   */
  public void addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException;

  /**
   * Gets a list of related pages based on a given param.
   * @param page The wiki page.
   * @return The list of related pages.
   * @throws WikiException if an error occured
   */
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException;

  /**
   * Removes a related page of the current wiki page.
   * @param orginaryPageParams The params object of the current wiki page.
   * @param relatedPageParams The params object of the related page.
   * @throws WikiException if an error occured
   */
  public void removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException;
  
  /**
   * Creates a draft page for a wiki page which is specified by a given param object.
   * 
   * @param draftPage The draft object
   * @param targetPage The target wiki page.
   * @param revision The revision which is used for creating the draft page. If "null", this will be the last revision.
   * @param clientTime The time of client when the draft page is saved.
   * @return The draft page.
   * @throws WikiException if an error occured if the draft page cannot be created.
   */
  public DraftPage createDraftForExistPage(DraftPage draftPage, Page targetPage, String revision, long clientTime) throws WikiException;
  
  /**
   * Creates a draft page for a new wiki page whose parent is specified by a given param object.
   *
   * @param draftPage The draft object
   * @param parentPage The parent wiki page.
   * @param clientTime The time of client when the draft page is saved.
   * @return The draft page.
   * @throws WikiException if an error occured if the draft page cannot be created.
   */
  public DraftPage createDraftForNewPage(DraftPage draftPage, Page parentPage, long clientTime) throws WikiException;
  
  /**
   * Gets a draft page of a wiki page which is specified by a given param object.
   * 
   * @param page The wiki page.
   * @return The draft page, or "null" if the draft page does not exist.
   * @throws WikiException if an error occured
   */
  public DraftPage getDraftOfPage(Page page) throws WikiException;
  
  /**
   * Gets a draft page by its name.
   * 
   * @param draftName Name of the draft page.
   * @return The draft page, or "null" if it does not exist.
   * @throws WikiException if an error occured
   */
   public DraftPage getDraft(String draftName) throws WikiException;
  
  /**
    * Removes a draft page of a wiki page which is specified by the wiki page param.
    * 
    * @param param The param object of the wiki page param.
    * @throws WikiException if an error occured
    */
  public void removeDraftOfPage(WikiPageParams param) throws WikiException;
  
  /**
   * Removes a draft page by its name.
   * 
   * @param draftName Name of the draft page.
   * @throws WikiException if an error occured
   */
  public void removeDraft(String draftName) throws WikiException;
  
  /**
   * Gets a list of draft pages belonging to a given user.
   * 
   * @param username Name of the user.
   * @return The list of draft pages.
   * @throws WikiException if an error occured
   */
  public List<DraftPage> getDraftsOfUser(String username) throws WikiException;

  /**
   * Check if a draft page is outdated
   * @param draftPage the draft page object
   * @return true if the draft is outdated
   * @throws WikiException if an error occured
   */
  public boolean isDraftOutDated(DraftPage draftPage) throws WikiException;
  
  /**
   * Gets the last created draft of a wiki page.
   * 
   * @return The last draft.
   * @throws WikiException if an error occured
   */
  public DraftPage getLastestDraft() throws WikiException;

  /**
   * Gets the changes between the draft page and the target page
   *
   * @param draftPage the draft page object
   * @return list of changes
   * @throws WikiException if an error occured
   */
  public DiffResult getDraftChanges(DraftPage draftPage) throws WikiException;

  /**
   * Gets a user Wiki. If it does not exist, the new one will be created.
   * 
   * @param username Name of the user.
   * @return The user Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getOrCreateUserWiki(String username) throws WikiException;
 
  /**
   * Gets a space name by a given group Id.
   * 
   * @param groupId The group Id.
   * @return The space name.
   */
  public String getSpaceNameByGroupId(String groupId);
  
  /**
   * Searches for spaces by a given keyword.
   * 
   * @param keyword The keyword to search for spaces.
   * @return The list of spaces matching with the keyword.
   * @throws WikiException if an error occured
   */
  public List<SpaceBean> searchSpaces(String keyword) throws WikiException;
  
  /**
   * Gets a Wiki which is defined by its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @return The Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException;

  /**
   * Gets all wikis of the given type
   * @param wikiType Type of wiki
   * @return Wikis of the given type
   * @throws WikiException if an error occured
   */
  public List<Wiki> getWikisByType(String wikiType) throws WikiException;

  /**
   * Creates a wiki with the given type and owner
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @return Wiki created
   * @throws WikiException if an error occured
   */
  public Wiki createWiki(String wikiType, String owner) throws WikiException;

  /**
   * Gets a Wiki webapp URI.
   * 
   * @return The Wiki webapp URI.
   */
  public String getWikiWebappUri();

  /**
   * Check if the identity has the given permission type on a wiki
   * @param wiki Wiki
   * @param permissionType Permission type to check
   * @param user Identity of the user
   * @return true if the user has the given permission type on the wiki
   * @throws WikiException if an error occured
   */
  boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity user) throws WikiException;

  /**
   * Checks if the given user has the permission on a page
   * @param user the userName
   * @param page the wiki page object
   * @param permissionType permission Type
   * @return true if user has permissions
   * @throws WikiException if an error occured
   */
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException;

  /** 
   * Checks if the current user has the admin permission on a space or not.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the space.
   * @return The returned value is "true" if the current user has the admin permission on the space, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws WikiException;
  
  /**
   * Checks if the current user has the admin permission on a wiki page.
   * 
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the wiki page.
   * @return "True" if the current user has the admin permission on the wiki page, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminPagePermission(String wikiType, String owner) throws WikiException;

  /**
   * Gets a Wiki by its Id.
   * 
   * @param wikiId The Wiki Id.
   * @return The Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getWikiById(String wikiId) throws WikiException;
  
  /**
   * Gets a Wiki name by its Id.
   *
   * @param wikiId The Wiki Id.
   * @return The Wiki name.
   * @throws WikiException if an error occured
   */
  public String getWikiNameById(String wikiId) throws WikiException;

  /**
   * Check if the given user can update the page
   * @param currentPage The page to update
   * @param currentUser The user that needs to update the page
   * @return true if the user can update the page
   * @throws WikiException if an error occured
   */
  public boolean canModifyPagePermission(Page currentPage, String currentUser) throws WikiException;
  
  /**
   * Check if the given user can public or restrict the page
   * @param currentPage page to check
   * @param currentUser user to chck permissions
   * @return true if the current user has EditPage permission or admin page or admin space
   * @throws WikiException if an error occured
   */
  public boolean canPublicAndRetrictPage(Page currentPage, String currentUser) throws WikiException;

  /**
   * Gets all the versions of the given page
   * @param page The wiki page
   * @return All the versions of the page
   * @throws WikiException if an error occured
   */
  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException;

  /**
   * Gets a specific version by name of the given page
   * @param versionName The name of the version
   * @param page The wiki page
   * @return The version of the wiki page
   * @throws WikiException if an error occured
   */
  public PageVersion getVersionOfPageByName(String versionName, Page page) throws WikiException;

  /**
   * Creates a version of a page. This method only tag the current page data as a new version,
   * it does not update the page data
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void createVersionOfPage(Page page) throws WikiException;

  /**
   * Restores a version of a page
   * @param versionName The name of the version to restore
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void restoreVersionOfPage(String versionName, Page page) throws WikiException;

  /**
   * Update the given page. This does not automatically create a new version.
   * If a new version must be created it should be explicitly done by calling createVersionOfPage().
   * The second parameter is the type of update done (title only, content only, both, move, ...).
   * @param page Updated page
   * @param type Type of update
   * @throws WikiException if an error occured
   */
  public void updatePage(Page page, PageUpdateType type) throws WikiException;

  /**
   * Get previous names of a page
   * @param page The wiki page
   * @return List of all the previous names of the page
   * @throws WikiException if an error occured
   */
  public List<String> getPreviousNamesOfPage(Page page) throws WikiException;

  /**
   * Creates a emotion icon
   * @param emotionIcon The emotion icon to add
   * @throws WikiException if an error occured
   */
  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException;

  /**
   * Gets all the emotion icons
   * @return All the emotion icons
   * @throws WikiException if an error occured
   */
  public List<EmotionIcon> getEmotionIcons() throws WikiException;

  /**
   * Gets an emotion icon by name
   * @param name The name of the emotion icon
   * @return The emotion icon
   * @throws WikiException if an error occured
   */
  public EmotionIcon getEmotionIconByName(String name) throws WikiException;

  /**
   * Get all the watchers of a page
   * @param page The wiki page
   * @throws WikiException if an error occured
   *
   * @return liste of watchers
   */
  public List<String> getWatchersOfPage(Page page) throws WikiException;

  /**
   * Add the given user as watcher of the wiki page
   * @param username Username of the user to add as watcher
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void addWatcherToPage(String username, Page page) throws WikiException;

  /**
   * Delete a user as watcher of the given page
   * @param username Username of the user to delete as watcher
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void deleteWatcherOfPage(String username, Page page) throws WikiException;

  /**
   * @return Upload limit for Wiki Attachment
   */
  public int getUploadLimit();

  /**
   * Retrieve the all pages contained in wiki
   * 
   * @param wikiType the wiki type
   * @param wikiOwner the wiki owner
   * @return List of pages
   */
  public List<Page> getPagesOfWiki(String wikiType, String wikiOwner);

}
