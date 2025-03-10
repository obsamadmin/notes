/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.wiki.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.entity.IdentityEntity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.Relations;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.image.ResizeImageService;
import org.exoplatform.wiki.service.related.JsonRelatedData;
import org.exoplatform.wiki.service.related.RelatedUtil;
import org.exoplatform.wiki.service.rest.model.Attachment;
import org.exoplatform.wiki.service.rest.model.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TitleSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.WikiTreeNode;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.NoteConstants;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Class;
import java.lang.Object;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

/**
 * Wiki REST service
 */
@SuppressWarnings("deprecation")
@Path("/wiki")
public class WikiRestServiceImpl implements ResourceContainer {

  private final WikiService      wikiService;

  private final NoteService noteService;

  private final ResourceBundleService resourceBundleService;

  private static Log             log = ExoLogger.getLogger("wiki:WikiRestService");

  private final CacheControl     cc;
  
  private ObjectFactory objectFactory = new ObjectFactory();
  
  public WikiRestServiceImpl(WikiService wikiService, NoteService noteService, ResourceBundleService resourceBundleService) {
    this.wikiService = wikiService;
    this.noteService = noteService;
    this.resourceBundleService = resourceBundleService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  /**
   * Return the wiki page content as html or wiki syntax.
   * @param text contain the data as html
   * @return the instance of javax.ws.rs.core.Response
   *
   * @LevelAPI Experimental
   */
  @POST
  @Path("/content/")
  public Response getWikiPageContent(@FormParam("text") String text) {
    try {
      String outputText = HTMLSanitizer.sanitize(text);

      return Response.ok(outputText, MediaType.TEXT_HTML).cacheControl(cc).build();
    } catch (Exception e) {
      log.error("Error while converting wiki page content: " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Upload an attachment to a wiki page
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Is the pageId used by the system
   * @return the instance of javax.ws.rs.core.Response
   */
  @POST
  @Path("/upload/{wikiType}/{wikiOwner:.+}/{pageId}/")
  @RolesAllowed("users")
  public Response upload(@PathParam("wikiType") String wikiType,
                         @PathParam("wikiOwner") String wikiOwner,
                         @PathParam("pageId") String pageId) {
    EnvironmentContext env = EnvironmentContext.getCurrent();
    HttpServletRequest req = (HttpServletRequest) env.get(HttpServletRequest.class);
    boolean isMultipart = FileUploadBase.isMultipartContent(req);
    if (isMultipart) {
      DiskFileUpload upload = new DiskFileUpload();
      // Parse the request
      try {
        String attachmentName = null;

        List<FileItem> items = upload.parseRequest(req);
        for (FileItem fileItem : items) {
          InputStream inputStream = fileItem.getInputStream();
          byte[] imageBytes;
          if (inputStream != null) {
            imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
          } else {
            imageBytes = null;
          }
          String fileName = Utils.normalizeUploadedFilename(fileItem.getName());
          if (fileName != null) {
            // It's necessary because IE posts full path of uploaded files
            fileName = FilenameUtils.getName(fileName);
          }
          String mimeType = new MimeTypeResolver().getMimeType(StringUtils.lowerCase(fileName));

          WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
          org.exoplatform.wiki.mow.api.Page page = wikiService.getExsitedOrNewDraftPageById(wikiType, wikiOwner, pageId);
          org.exoplatform.wiki.mow.api.Attachment attachment = new org.exoplatform.wiki.mow.api.Attachment();
          attachment.setName(fileName);
          if (fileName.lastIndexOf(".") > 0) {
            attachment.setTitle(fileName.substring(0, fileName.lastIndexOf(".")));
          }
          attachment.setMimeType(mimeType);
          attachment.setContent(imageBytes);
          ConversationState conversationState = ConversationState.getCurrent();
          if (conversationState != null && conversationState.getIdentity() != null) {
            attachment.setCreator(conversationState.getIdentity().getUserId());
          }
          wikiService.addAttachmentToPage(attachment, page);

          attachmentName = attachment.getName();
        }

        StringBuilder responseBody = new StringBuilder("{\"default\":\"")
                .append(Utils.getDefaultRestBaseURI())
                .append("/wiki/attachments/")
                .append(wikiType)
                .append("/")
                .append(Utils.SPACE)
                .append("/")
                .append(wikiOwner)
                .append("/")
                .append(Utils.PAGE)
                .append("/")
                .append(pageId)
                .append("/")
                .append(attachmentName)
                .append("\"}");

        return Response.ok(responseBody.toString()).build();
      } catch (IllegalArgumentException e) {
        log.error("Special characters are not allowed in the name of an attachment.");
        return Response.status(HTTPStatus.BAD_REQUEST).entity(e.getMessage()).build();
      } catch (Exception e) {
        log.error(e.getMessage());
        return Response.status(HTTPStatus.BAD_REQUEST).entity(e.getMessage()).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("The request must be a multipart request").build();
    }
  }

  /**
   * Display the current tree of a wiki based on is path
   * @param type It can be a Portal, Group, User type of wiki
   * @param path Contains the path of the wiki page
   * @param canEdit true if user can edit
   * @param currentPath Contains the path of the current wiki page
   * @param showExcerpt Boolean to display or not the excerpt
   * @param depth Defined the depth of the children we want to display
   * @return List of descendants including the page itself.
   */
  @GET
  @Path("/tree/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTreeData(@PathParam("type") String type,
                              @QueryParam(TreeNode.PATH) String path,
                              @QueryParam(TreeNode.CURRENT_PATH) String currentPath,
                              @QueryParam(TreeNode.CAN_EDIT) Boolean canEdit,
                              @QueryParam(TreeNode.SHOW_EXCERPT) Boolean showExcerpt,
                              @QueryParam(TreeNode.DEPTH) String depth) {
    try {
      List<JsonNodeData> responseData = new ArrayList<JsonNodeData>();
      HashMap<String, Object> context = new HashMap<String, Object>();
      context.put(TreeNode.CAN_EDIT, canEdit);
      if (currentPath != null){
        currentPath = URLDecoder.decode(currentPath, "utf-8");
        context.put(TreeNode.CURRENT_PATH, currentPath);
        WikiPageParams currentPageParam = TreeUtils.getPageParamsFromPath(currentPath);
        org.exoplatform.wiki.mow.api.Page currentPage = wikiService.getPageOfWikiByName(currentPageParam.getType(), currentPageParam.getOwner(), currentPageParam.getPageName());
        context.put(TreeNode.CURRENT_PAGE, currentPage);
      }

      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);

      // Put select page to context
      path = URLDecoder.decode(path, "utf-8");
      context.put(TreeNode.PATH, path);
      WikiPageParams pageParam = TreeUtils.getPageParamsFromPath(path);
      org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(pageParam.getType(), pageParam.getOwner(), pageParam.getPageName());
      if (page == null) {
        log.warn("User [{}] can not get wiki path [{}]. Home is used instead",
                 ConversationState.getCurrent().getIdentity().getUserId(), path);
        page = wikiService.getPageOfWikiByName(pageParam.getType(), pageParam.getOwner(), pageParam.WIKI_HOME);
        if(page == null) {
          ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.wiki.WikiPortlet", request.getLocale());
          String errorMessage = "";
          if(resourceBundle != null) {
            errorMessage = resourceBundle.getString("UIWikiMovePageForm.msg.no-permission-at-wiki-destination");
          }
          return Response.serverError().entity("{ \"message\": \"" + errorMessage + "\"}").cacheControl(cc).build();
        }
      }
      
      context.put(TreeNode.SELECTED_PAGE, page);
      
      context.put(TreeNode.SHOW_EXCERPT, showExcerpt);
      if (type.equalsIgnoreCase(TREETYPE.ALL.toString())) {
        Deque<WikiPageParams> stk = Utils.getStackParams(page);
        context.put(TreeNode.STACK_PARAMS, stk);
        responseData = getJsonTree(pageParam, context, ConversationState.getCurrent().getIdentity().getUserId());
      } else if (type.equalsIgnoreCase(TREETYPE.CHILDREN.toString())) {
        // Get children only
        if (depth == null)
          depth = "1";
        context.put(TreeNode.DEPTH, depth);
        responseData = getJsonDescendants(pageParam, context, ConversationState.getCurrent().getIdentity().getUserId());
      }

      encodeWikiTree(responseData, request.getLocale());
      return Response.ok(new BeanToJsons(responseData), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      log.error("Failed for get tree data by rest service - Cause : " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Return the related pages of a Wiki page
   * @param path Contains the path of the wiki page
   * @return List of related pages
   */
  @GET
  @Path("/related/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getRelated(@QueryParam(TreeNode.PATH) String path) {
    if (path == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    try {
      WikiPageParams params = TreeUtils.getPageParamsFromPath(path);
      org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(params.getType(), params.getOwner(), params.getPageName());
      if (page != null) {
        List<org.exoplatform.wiki.mow.api.Page> relatedPages = wikiService.getRelatedPagesOfPage(page);
        List<JsonRelatedData> relatedData = RelatedUtil.pageToJson(relatedPages);
        return Response.ok(new BeanToJsons<>(relatedData)).cacheControl(cc).build();
      }
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error(String.format("can not get related pages of [%s]", path), e);
      return Response.serverError().cacheControl(cc).build();
    }
  }

  /**
   * Return a list of wiki based on their type.
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param start Not used
   * @param number Not used
   * @return List of wikis by type
   */
  @GET
  @Path("/{wikiType}/spaces")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Spaces getSpaces(@Context UriInfo uriInfo,
                          @PathParam("wikiType") String wikiType,
                          @QueryParam("start") Integer start,
                          @QueryParam("number") Integer number) {
    Spaces spaces = objectFactory.createSpaces();
    List<String> spaceNames = new ArrayList<>();
    try {
      List<Wiki> wikis = wikiService.getWikisByType(wikiType);
      for (Wiki wiki : wikis) {
        spaceNames.add(wiki.getOwner());
      }
      for (String spaceName : spaceNames) {
        org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(wikiType, spaceName, NoteConstants.NOTE_HOME_NAME);
        spaces.getSpaces().add(createSpace(objectFactory, uriInfo.getBaseUri(), wikiType, spaceName, page));
      }
    } catch(WikiException e) {
      log.error("Cannot get spaces of wiki type " + wikiType + " - Cause : " + e.getMessage(), e);
    }
    return spaces;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List getLastAccessedSpace(String userId, String appId, int offset, int limit) throws Exception {
    List spaces = new ArrayList();
    Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
    Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);
    spaces = (List) spaceServiceClass.getDeclaredMethod("getLastAccessedSpace", String.class, String.class, Integer.class, Integer.class)
      .invoke(spaceService, userId, appId, new Integer(offset), new Integer(limit));
    return spaces;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked"})
  private <T> T getValueFromSpace(Object space, String getterMethod, Class<T> propertyClass) throws Exception {
    Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
    T propertyValue = (T) spaceClass.getMethod(getterMethod).invoke(space);
    return propertyValue;
  }

  /**
   * Return a list of last visited spaces by the user.
   * @param uriInfo Uri of the wiki
   * @param offset The offset to search
   * @param limit Limit number to search
   * @return List of spaces
   */
  @GET
  @Path("/lastVisited/spaces")
  @Produces("application/xml")
  @SuppressWarnings("rawtypes")
  @RolesAllowed("users")
  public Spaces getLastVisitedSpaces(@Context UriInfo uriInfo,
                                     @QueryParam("offset") Integer offset,
                                     @QueryParam("limit") Integer limit) {
    Spaces spaces = objectFactory.createSpaces();
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      List lastVisitedSpaces = getLastAccessedSpace(currentUser, "Wiki", offset, limit);
      for (Object space : lastVisitedSpaces) {
        String groupId = getValueFromSpace(space, "getGroupId", String.class);
        String displayName = getValueFromSpace(space, "getDisplayName", String.class);
        Wiki wiki = wikiService.getWikiByTypeAndOwner(WikiType.GROUP.toString(), groupId);
        org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), NoteConstants.NOTE_HOME_NAME);
        spaces.getSpaces().add(createSpace(objectFactory, uriInfo.getBaseUri(), wiki.getType(), displayName, page));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return spaces;
  }

  /**
   * Return the space based on the uri
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return Space related to the uri
   */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Space getSpace(@Context UriInfo uriInfo,
                        @PathParam("wikiType") String wikiType,
                        @PathParam("wikiOwner") String wikiOwner) {
    org.exoplatform.wiki.mow.api.Page page;
    try {
      page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, NoteConstants.NOTE_HOME_NAME);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return objectFactory.createSpace();
    }
    return createSpace(objectFactory, uriInfo.getBaseUri(), wikiType, wikiOwner, page);
  }

  /**
   * Return a list of pages related to the space and uri
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param start Not used
   * @param number Not used
   * @param parentFilterExpression parent Filter Expression
   * @return List of pages
   */
  @GET
  @Path("/{userId}/{wikiType}/spaces/{wikiOwner:.+}/pages")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Pages getPages(@Context UriInfo uriInfo,
                        @PathParam("userId") String userId,
                        @PathParam("wikiType") String wikiType,
                        @PathParam("wikiOwner") String wikiOwner,
                        @QueryParam("start") Integer start,
                        @QueryParam("number") Integer number,
                        @QueryParam("parentId") String parentFilterExpression) {
    Pages pages = objectFactory.createPages();
    org.exoplatform.wiki.mow.api.Page page;
    boolean isWikiHome = true;
    try {
      String parentId = NoteConstants.NOTE_HOME_NAME;
      if (parentFilterExpression != null && parentFilterExpression.length() > 0
          && !parentFilterExpression.startsWith("^(?!")) {
        parentId = parentFilterExpression;
        if (parentId.indexOf(".") >= 0) {
          parentId = parentId.substring(parentId.indexOf(".") + 1);
        }
        isWikiHome = false;
      }
      page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, parentId);
      if (isWikiHome) {
        pages.getPageSummaries().add(createPageSummary(objectFactory, uriInfo.getBaseUri(), page, userId));
      } else {
        List<org.exoplatform.wiki.mow.api.Page> childrenPages = wikiService.getChildrenPageOf(page, ConversationState.getCurrent().getIdentity().getUserId(), false);
        for (org.exoplatform.wiki.mow.api.Page childPage : childrenPages) {
          pages.getPageSummaries().add(createPageSummary(objectFactory,
                                                       uriInfo.getBaseUri(),
                                                       childPage, userId));
        }
      }
    } catch (Exception e) {
      log.error("Can't get children pages of:" + parentFilterExpression, e);
    }

    return pages;
  }

  /**
   * Return a wiki page based on is uri and id
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @param userId
   * @return A wiki page
   */
  @GET
  @Path("/{userId}/{wikiType}/spaces/{wikiOwner:.+}/pages/{pageId}")
  @Produces("application/xml")
  @RolesAllowed("users")
  public org.exoplatform.wiki.service.rest.model.Page getPage(@Context UriInfo uriInfo,
                                                              @PathParam("wikiType") String wikiType,
                                                              @PathParam("wikiOwner") String wikiOwner,
                                                              @PathParam("pageId") String pageId,
                                                              @PathParam("userId") String userId) {
    org.exoplatform.wiki.mow.api.Page page;
    try {
      page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (page != null) {
        return createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), page, userId);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return objectFactory.createPage();
  }

    /**
     * Return a list of attachments attached to a wiki page
     * @param uriInfo Uri of the wiki
     * @param wikiType It can be a Portal, Group, User type of wiki
     * @param wikiOwner Is the owner of the wiki
     * @param pageId Id of the wiki page
     * @param start Not used
     * @param number Not used
     * @return List of attachments
     */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/pages/{pageId}/attachments")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Attachments getAttachments(@Context UriInfo uriInfo,
                                    @PathParam("wikiType") String wikiType,
                                    @PathParam("wikiOwner") String wikiOwner,
                                    @PathParam("pageId") String pageId,
                                    @QueryParam("start") Integer start,
                                    @QueryParam("number") Integer number) {
    Attachments attachments = objectFactory.createAttachments();
    org.exoplatform.wiki.mow.api.Page page;
    try {
      page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      List<org.exoplatform.wiki.mow.api.Attachment> pageAttachments = wikiService.getAttachmentsOfPage(page);
      for (org.exoplatform.wiki.mow.api.Attachment pageAttachment : pageAttachments) {
        attachments.getAttachments().add(createAttachment(objectFactory, uriInfo.getBaseUri(), pageAttachment, page, "attachment", "attachment"));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return attachments;
  }

  /**
   * Return a list of title based on a searched words.
   * @param uriInfo uriInfo
   * @param keyword Word to search
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return List of title
   * @throws Exception if an error occured
   */
  @GET
  @Path("contextsearch/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response searchData(@Context UriInfo uriInfo,
                             @QueryParam("keyword") String keyword,
                             @QueryParam("limit") int limit,
                             @QueryParam("wikiType") String wikiType,
                             @QueryParam("wikiOwner") String wikiOwner,
                             @QueryParam("favorites") boolean favorites) throws Exception {
    limit = limit > 0 ? limit : RestUtils.getLimit(uriInfo);
    try {

      keyword = keyword.toLowerCase();
      Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      WikiSearchData data = new WikiSearchData(keyword, currentIdentity.getUserId());
      data.setLimit(limit);
      data.setFavorites(favorites);
      List<SearchResult> results = wikiService.search(data).getAll();
      List<TitleSearchResult> titleSearchResults = new ArrayList<>();
      for (SearchResult searchResult : results) {
        org.exoplatform.wiki.mow.api.Page page = noteService.getNoteOfNoteBookByName(searchResult.getWikiType(),
                                                                                     searchResult.getWikiOwner(),
                                                                                     searchResult.getPageName(),
                                                                                     currentIdentity);
        if (page != null) {
          page.setUrl(Utils.getPageUrl(page));
          if (SearchResultType.ATTACHMENT.equals(searchResult.getType())) {
            org.exoplatform.wiki.mow.api.Attachment attachment =
                                                               wikiService.getAttachmentOfPageByName(searchResult.getAttachmentName(),
                                                                                                     page);
            TitleSearchResult titleSearchResult = new TitleSearchResult();
            titleSearchResult.setTitle(attachment.getName());
            titleSearchResult.setId(page.getId());
            titleSearchResult.setActivityId(page.getActivityId());
            titleSearchResult.setType(searchResult.getType());
            titleSearchResult.setUrl(attachment.getDownloadURL());
            titleSearchResult.setMetadatas(page.getMetadatas());
            titleSearchResults.add(titleSearchResult);
          } else if (searchResult.getPoster() != null) {
            IdentityEntity posterIdentity = EntityBuilder.buildEntityIdentity(searchResult.getPoster(), uriInfo.getPath(), "all");
            IdentityEntity wikiOwnerIdentity =
                                             searchResult.getWikiOwnerIdentity() != null ? EntityBuilder.buildEntityIdentity(searchResult.getWikiOwnerIdentity(),
                                                                                                                             uriInfo.getPath(),
                                                                                                                             "all")
                                                                                         : null;
            TitleSearchResult titleSearchResult = new TitleSearchResult();
            titleSearchResult.setTitle(searchResult.getTitle());
            titleSearchResult.setId(page.getId());
            titleSearchResult.setActivityId(page.getActivityId());
            titleSearchResult.setPoster(posterIdentity);
            titleSearchResult.setWikiOwner(wikiOwnerIdentity);
            titleSearchResult.setExcerpt(searchResult.getExcerpt());
            titleSearchResult.setCreatedDate(searchResult.getCreatedDate().getTimeInMillis());
            titleSearchResult.setType(searchResult.getType());
            titleSearchResult.setUrl(page.getUrl());
            titleSearchResult.setMetadatas(page.getMetadatas());
            titleSearchResults.add(titleSearchResult);
          }
        } else {
          log.warn("Cannot get page of search result " + searchResult.getWikiType() + ":" + searchResult.getWikiOwner() + ":"
              + searchResult.getPageName());
        }
      }
      return Response.ok(new BeanToJsons(titleSearchResults), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Return an image attach to the wiki page and keep the size ratio of it.
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @param imageId Id of the image attached to the wiki page
   * @param width expected width of the image, it will keep the ratio
   * @return The response with the image
   * @deprecated use /attachments/{wikiType}/space/{wikiOwner:.+}/page/{pageId}/{attachmentId} instead
   */
  @GET
  @Path("/images/{wikiType}/space/{wikiOwner:.+}/page/{pageId}/{imageId}")
  public Response getImage(@Context UriInfo uriInfo,
                           @PathParam("wikiType") String wikiType,
                           @PathParam("wikiOwner") String wikiOwner,
                           @PathParam("pageId") String pageId,
                           @PathParam("imageId") String imageId,
                           @QueryParam("width") Integer width) {
    return getAttachment(uriInfo, wikiType, wikiOwner, pageId, imageId, width);
  }

  /**
   * Return an attchment attached to the wiki page.
   * In case of an image, the width can be specified (the size ratio is kept).
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @param attachmentId Id of the attachment of the wiki page
   * @param width in case of an image, expected width of the image, it will keep the ratio
   * @return The response with the attachment
   */
  @GET
  @Path("/attachments/{wikiType}/space/{wikiOwner:.+}/page/{pageId}/{attachmentId}")
  public Response getAttachment(@Context UriInfo uriInfo,
                           @PathParam("wikiType") String wikiType,
                           @PathParam("wikiOwner") String wikiOwner,
                           @PathParam("pageId") String pageId,
                           @PathParam("attachmentId") String attachmentId,
                           @QueryParam("width") Integer width) {
    InputStream result;
    try {
      org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (page == null) {
        page = wikiService.getRelatedPage(wikiType, wikiOwner, pageId);
      }
      if (page == null) {
        return Response.status(HTTPStatus.NOT_FOUND).entity("There is no resource matching to request path " + uriInfo.getPath()).type(MediaType.TEXT_PLAIN).build();
      }
      org.exoplatform.wiki.mow.api.Attachment attachment = wikiService.getAttachmentOfPageByName(attachmentId, page, true);

      if (attachment == null) {
        return Response.status(HTTPStatus.NOT_FOUND).entity("There is no resource matching to request path " + uriInfo.getPath()).type(MediaType.TEXT_PLAIN).build();
      }
      
      if (attachment.getContent() == null) {
        EnvironmentContext env = EnvironmentContext.getCurrent();
        HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);
        ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(Utils.WIKI_RESOUCE_BUNDLE_NAME, request.getLocale());
        String message = resourceBundle.getString("WikiRestService.message.attachment.notAvailable");
        message = message.replace("{0}", attachment.getName());
        return Response.status(HTTPStatus.NOT_FOUND).entity(message).type(MediaType.TEXT_PLAIN).build();
  
      }

      ByteArrayInputStream bis = new ByteArrayInputStream(attachment.getContent());
      if (width != null) {
        ResizeImageService resizeImgService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ResizeImageService.class);
        result = resizeImgService.resizeImageByWidth(attachmentId, bis, width);
      } else {
        result = bis;
      }
      return Response.ok(result).header("Content-Disposition", "attachment; filename=" + attachment.getName()).cacheControl(cc).build();
    } catch (Exception e) {
      log.error(String.format("Can't get attachment name: %s of page %s", attachmentId, pageId), e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  public Space createSpace(ObjectFactory objectFactory,
                           URI baseUri,
                           String wikiName,
                           String spaceName,
                           org.exoplatform.wiki.mow.api.Page home) {
    Space space = objectFactory.createSpace();
    space.setId(String.format("%s:%s", wikiName, spaceName));
    space.setWiki(wikiName);
    space.setName(spaceName);
    if (home != null) {
      space.setHome("home");
      space.setXwikiRelativeUrl("home");
      space.setXwikiAbsoluteUrl("home");
    }

    String pagesUri = UriBuilder.fromUri(baseUri)
                                .path("/wiki/{wikiName}/spaces/{spaceName}/pages")
                                .build(wikiName, spaceName)
                                .toString();
    Link pagesLink = objectFactory.createLink();
    pagesLink.setHref(pagesUri);
    pagesLink.setRel(Relations.PAGES);
    space.getLinks().add(pagesLink);

    if (home != null) {
      String homeUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                                 .build(wikiName, spaceName, home.getName())
                                 .toString();
      Link homeLink = objectFactory.createLink();
      homeLink.setHref(homeUri);
      homeLink.setRel(Relations.HOME);
      space.getLinks().add(homeLink);
    }

    String searchUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/search")
                                 .build(wikiName, spaceName)
                                 .toString();
    Link searchLink = objectFactory.createLink();
    searchLink.setHref(searchUri);
    searchLink.setRel(Relations.SEARCH);
    space.getLinks().add(searchLink);

    return space;

  }

  public org.exoplatform.wiki.service.rest.model.Page createPage(ObjectFactory objectFactory,
                                                                 URI baseUri,
                                                                 URI self,
                                                                 Page doc, String userId) throws Exception {
    org.exoplatform.wiki.service.rest.model.Page page = objectFactory.createPage();
    fillPageSummary(page, objectFactory, baseUri, doc, userId);

    page.setVersion("current");
    page.setMajorVersion(1);
    page.setMinorVersion(0);
    page.setLanguage(doc.getSyntax());
    page.setCreator(doc.getOwner());

    GregorianCalendar calendar = new GregorianCalendar();
    page.setCreated(calendar);

    page.setModifier(doc.getAuthor());

    calendar = new GregorianCalendar();
    calendar.setTime(doc.getUpdatedDate());
    page.setModified(calendar);

    page.setContent(doc.getContent());

    if (self != null) {
      Link pageLink = objectFactory.createLink();
      pageLink.setHref(self.toString());
      pageLink.setRel(Relations.SELF);
      page.getLinks().add(pageLink);
    }
    return page;
  }

  public PageSummary createPageSummary(ObjectFactory objectFactory, URI baseUri, Page page, String userId) throws IllegalArgumentException, UriBuilderException, Exception {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
    PageSummary pageSummary = objectFactory.createPageSummary();
    fillPageSummary(pageSummary, objectFactory, baseUri, page, userId);
    String wikiName = wiki.getType();
    String spaceName = wiki.getOwner();
    String pageUri = UriBuilder.fromUri(baseUri)
                               .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                               .build(wikiName, spaceName, page.getName())
                               .toString();
    Link pageLink = objectFactory.createLink();
    pageLink.setHref(pageUri);
    pageLink.setRel(Relations.PAGE);
    pageSummary.getLinks().add(pageLink);

    return pageSummary;
  }
  
  public Attachment createAttachment(ObjectFactory objectFactory,
                                     URI baseUri,
                                     org.exoplatform.wiki.mow.api.Attachment pageAttachment,
                                     org.exoplatform.wiki.mow.api.Page page,
                                     String xwikiRelativeUrl,
                                     String xwikiAbsoluteUrl) throws Exception {
    Attachment attachment = objectFactory.createAttachment();

    fillAttachment(attachment, objectFactory, baseUri, pageAttachment, page, xwikiRelativeUrl, xwikiAbsoluteUrl);

    String attachmentUri = UriBuilder.fromUri(baseUri)
                                     .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments/{attachmentName}")
            .build(page.getWikiType(), page.getWikiOwner(), page.getName(), pageAttachment.getName())
                                     .toString();
    Link attachmentLink = objectFactory.createLink();
    attachmentLink.setHref(attachmentUri);
    attachmentLink.setRel(Relations.ATTACHMENT_DATA);
    attachment.getLinks().add(attachmentLink);

    return attachment;
  }  
 
  private List<JsonNodeData> getJsonTree(WikiPageParams params, HashMap<String, Object> context, String userId) throws Exception {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(params.getType(), params.getOwner());
    WikiTreeNode wikiNode = new WikiTreeNode(wiki);
    wikiNode.pushDescendants(context, userId);
    return TreeUtils.tranformToJson(wikiNode, context);
  }

  private List<JsonNodeData> getJsonDescendants(WikiPageParams params,
                                                HashMap<String, Object> context, String userId) throws Exception {
    TreeNode treeNode = TreeUtils.getDescendants(params, context, userId);
    return TreeUtils.tranformToJson(treeNode, context);
  }

  private void fillPageSummary(PageSummary pageSummary,
                               ObjectFactory objectFactory,
                               URI baseUri,
                               Page page, String userId) throws IllegalArgumentException, UriBuilderException, Exception {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
    pageSummary.setWiki(wiki.getType());
    pageSummary.setFullName(page.getTitle());
    pageSummary.setId(wiki.getType() + ":" + wiki.getOwner() + "." + page.getName());
    pageSummary.setSpace(wiki.getOwner());
    pageSummary.setName(page.getName());
    pageSummary.setTitle(HTMLSanitizer.sanitize(page.getTitle()));
    pageSummary.setTranslations(objectFactory.createTranslations());
    pageSummary.setSyntax(page.getSyntax());

    org.exoplatform.wiki.mow.api.Page parent = wikiService.getParentPageOf(page);
    // parentId must not be set if the parent document does not exist.
    if (parent != null) {
      pageSummary.setParent(parent.getName());
      pageSummary.setParentId(parent.getName());
    } else {
      pageSummary.setParent("");
      pageSummary.setParentId("");
    }

    String spaceUri = UriBuilder.fromUri(baseUri)
                                .path("/wiki/{wikiName}/spaces/{spaceName}")
                                .build(wiki.getType(), wiki.getOwner())
                                .toString();
    Link spaceLink = objectFactory.createLink();
    spaceLink.setHref(spaceUri);
    spaceLink.setRel(Relations.SPACE);
    pageSummary.getLinks().add(spaceLink);

    if (parent != null) {
      String parentUri = UriBuilder.fromUri(baseUri)
                                   .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                                   .build(wiki.getType(),
                                           wiki.getOwner(),
                                          parent.getName())
                                   .toString();
      Link parentLink = objectFactory.createLink();
      parentLink.setHref(parentUri);
      parentLink.setRel(Relations.PARENT);
      pageSummary.getLinks().add(parentLink);
    }

    if (!wikiService.getChildrenPageOf(page, userId, false).isEmpty()) {
      String pageChildrenUri = UriBuilder.fromUri(baseUri)
                                         .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/children")
                                         .build(wiki.getType(),
                                                 wiki.getOwner(),
                                                 page.getName())
                                         .toString();
      Link pageChildrenLink = objectFactory.createLink();
      pageChildrenLink.setHref(pageChildrenUri);
      pageChildrenLink.setRel(Relations.CHILDREN);
      pageSummary.getLinks().add(pageChildrenLink);
    }

    List<org.exoplatform.wiki.mow.api.Attachment> attachments = wikiService.getAttachmentsOfPage(page);
    if (!attachments.isEmpty()) {
      String attachmentsUri;
      attachmentsUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments")
                                 .build(wiki.getType(),
                                         wiki.getOwner(),
                                         page.getName())
                                 .toString();

      Link attachmentsLink = objectFactory.createLink();
      attachmentsLink.setHref(attachmentsUri);
      attachmentsLink.setRel(Relations.ATTACHMENTS);
      pageSummary.getLinks().add(attachmentsLink);
    }

  }
  
  private void fillAttachment(Attachment attachment,
                              ObjectFactory objectFactory,
                              URI baseUri,
                              org.exoplatform.wiki.mow.api.Attachment pageAttachment,
                              org.exoplatform.wiki.mow.api.Page page,
                              String xwikiRelativeUrl,
                              String xwikiAbsoluteUrl) throws Exception {
    attachment.setId(String.format("%s@%s", page.getName(), pageAttachment.getName()));
    attachment.setName(pageAttachment.getName());
    attachment.setSize((int) pageAttachment.getWeightInBytes());
    attachment.setVersion("current");
    attachment.setPageId(page.getName());
    attachment.setPageVersion("current");
    attachment.setMimeType(pageAttachment.getMimeType());
    attachment.setAuthor(pageAttachment.getCreator());

    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(pageAttachment.getCreatedDate().getTime());
    attachment.setDate(calendar);

    attachment.setXwikiRelativeUrl(xwikiRelativeUrl);
    attachment.setXwikiAbsoluteUrl(xwikiAbsoluteUrl);

    String pageUri = UriBuilder.fromUri(baseUri)
                               .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                               .build(page.getWikiType(), page.getWikiOwner(), page.getName())
                               .toString();
    Link pageLink = objectFactory.createLink();
    pageLink.setHref(pageUri);
    pageLink.setRel(Relations.PAGE);
    attachment.getLinks().add(pageLink);
  }

  @GET
  @Path("/spaces/accessibleSpaces/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response searchAccessibleSpaces(@QueryParam("keyword") String keyword) {
    try {
      List<SpaceBean> spaceBeans = wikiService.searchSpaces(keyword);
      return Response.ok(new BeanToJsons(spaceBeans), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      if (log.isWarnEnabled()) {
        log.warn("An exception happens when searchAccessibleSpaces", ex);
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  /**
   * Save draft title and content for a page specified by the given page params
   * 
   * @param wikiType type of wiki to save draft
   * @param wikiOwner owner of wiki to save draft
   * @param rawPageId name of page to save draft in encoded format
   * @param pageRevision the target revision of target page
   * @param lastDraftName name of the draft page of last saved draft request
   * @param isNewPage The draft for new page or not
   * @param title draft title
   * @param content draft content
   * @param clientTime client Time
   * @param isMarkup content is markup or html. True if is markup.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is performed successfully
   *                          with status HTTPStatus.INTERNAL_ERROR if there is any unknown error in the saving process
   */                          
  @POST
  @Path("/saveDraft/")
  @RolesAllowed("users")
  public Response saveDraft(@QueryParam("wikiType") String wikiType,
                            @QueryParam("wikiOwner") String wikiOwner,
                            @QueryParam("pageId") String rawPageId,
                            @QueryParam("pageRevision") String pageRevision,
                            @QueryParam("lastDraftName") String lastDraftName,
                            @QueryParam("isNewPage") boolean isNewPage,
                            @QueryParam("clientTime") long clientTime,
                            @FormParam("title") String title,
                            @FormParam("content") String content,
                            @FormParam("isMarkup") String isMarkup) {
    String pageId = null;
    try {
      if ("__anonim".equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
        return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
      } 
      pageId = URLDecoder.decode(rawPageId,"utf-8");
      WikiPageParams param = new WikiPageParams(wikiType, wikiOwner, pageId);
      org.exoplatform.wiki.mow.api.Page page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (StringUtils.isEmpty(pageId) || (page == null)) {
        throw new IllegalArgumentException("Can not find the target page");
      }

      DraftPage draftPage = null;
      if (!isNewPage) {
        draftPage = wikiService.getDraftOfPage(page);
        if ((draftPage != null) && !draftPage.getName().equals(lastDraftName)) {
          draftPage = null;
        }
      } else {
        if (!StringUtils.isEmpty(lastDraftName)) {
          draftPage = wikiService.getDraft(lastDraftName);
        }
      }
      
      // If draft page is not exist then create draft page
      if (draftPage == null) {
        DraftPage newDraftPage = new DraftPage();
        newDraftPage.setTitle(title);
        newDraftPage.setContent(content);
        // if create draft for exist page, we need synchronized when create draft 
        if (!isNewPage) {
          draftPage = wikiService.createDraftForExistPage(newDraftPage, page, pageRevision, clientTime);
        } else {
          draftPage = wikiService.createDraftForNewPage(newDraftPage, page, clientTime);
        }
      } else {
        // Store page content and page title in draft
        if ("".equals(title)) {
          draftPage.setTitle(draftPage.getName());
        } else {
          draftPage.setTitle(title);
        }
        draftPage.setContent(content);
        wikiService.updatePage(draftPage, null);
      }

      // Log the editting time for current user
      Utils.logEditPageTime(param, Utils.getCurrentUser(), System.currentTimeMillis(), draftPage.getName(), isNewPage);
      
      // Notify to client that saved draft success
      return Response.ok(new DraftData(draftPage.getName()), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (UnsupportedEncodingException uee) {
        log.warn("Cannot decode page name");
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    } 
    catch (Exception ex) {
      if(StringUtils.isEmpty(pageId)) pageId = rawPageId;
      log.warn(String.format("Failed to perform auto save wiki page %s:%s:%s", wikiType,wikiOwner,pageId), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  /**
   * Remove the draft
   * 
   * @param draftName The name of draft to remove
   * @return Status.OK if remove draft success
   *         HTTPStatus.INTERNAL_ERROR if there's error occur when remove draft
   */
  @GET
  @Path("/removeDraft/")
  @RolesAllowed("users")
  public Response removeDraft(@QueryParam("draftName") String draftName) {
    if (StringUtils.isEmpty(draftName)) {
      return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
    }
    
    try {
      wikiService.removeDraft(draftName);
      return Response.ok().cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Return an emotion icon
   * @param uriInfo Uri of the wiki
   * @param name Name of the emotion icon
   * @return The response with the emotion icon
   */
  @GET
  @Path("/emoticons/{name}")
  public Response getEmotionIcon(@Context UriInfo uriInfo,
                                @PathParam("name") String name) {
    try {
      EmotionIcon emotionIcon = wikiService.getEmotionIconByName(name);
      if (emotionIcon == null) {
        return Response.status(HTTPStatus.NOT_FOUND).build();
      }

      ByteArrayInputStream emotionIconImage = new ByteArrayInputStream(emotionIcon.getImage());

      return Response.ok(emotionIconImage).cacheControl(cc).build();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Can't get emotion icon: %s", name), e);
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  private void encodeWikiTree(List<JsonNodeData> responseData, Locale locale) throws Exception {
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(Utils.WIKI_RESOUCE_BUNDLE_NAME, locale);
    String untitledLabel = "";
    if (resourceBundle == null) {
      // May happen in Tests
      log.warn("Cannot find resource bundle '{}'", Utils.WIKI_RESOUCE_BUNDLE_NAME);
    } else {
      untitledLabel = resourceBundle.getString("Page.Untitled");
    }

    for (JsonNodeData data : responseData) {
      if (StringUtils.isBlank(data.getName())) {
        data.setName(untitledLabel);
      }
      if (CollectionUtils.isNotEmpty(data.getChildren())) {
        encodeWikiTree(data.getChildren(), locale);
      }
    }
  }
}
