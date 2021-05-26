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
package org.exoplatform.wiki.service.rest;

import io.swagger.annotations.ApiParam;
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
import org.exoplatform.social.rest.entity.IdentityEntity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.Relations;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.image.ResizeImageService;
import org.exoplatform.wiki.service.impl.BeanToJsons;
import org.exoplatform.wiki.service.impl.DraftData;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.related.JsonRelatedData;
import org.exoplatform.wiki.service.related.RelatedUtil;
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
import org.exoplatform.wiki.utils.WikiConstants;
import org.exoplatform.wiki.utils.WikiHTMLSanitizer;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.lang.Object;
import java.lang.Class;

/**
 * Notes REST service
 */
@SuppressWarnings("deprecation")
@Path("/notes")
public class NotesRestService implements ResourceContainer {

  private final WikiService           wikiService;

  private final ResourceBundleService resourceBundleService;

  private static Log                  log = ExoLogger.getLogger("wiki:WikiRestService");

  private final CacheControl          cc;

  public NotesRestService(WikiService wikiService, ResourceBundleService resourceBundleService) {
    this.wikiService = wikiService;
    this.resourceBundleService = resourceBundleService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  @GET
  @Path("/pages/{wikiType}/{wikiOwner:.+}/{pageId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getPage(@Context UriInfo uriInfo,
                          @PathParam("wikiType") String wikiType,
                          @PathParam("wikiOwner") String wikiOwner,
                          @PathParam("pageId") String pageId) {
    try {
      Page page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (page == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      page.setContent(WikiHTMLSanitizer.markupSanitize(page.getContent()));
      return Response.ok(page).build();
    } catch (Exception e) {
      log.error("Can't get page", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  /**
   * Create new Wiki page
   * 
   * @param page WIKI PAGE.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is
   *         performed successfully with status HTTPStatus.INTERNAL_ERROR if there
   *         is any unknown error in the saving process
   */
  @POST
  @Path("/page")
  @RolesAllowed("users")
  public Response createPage(@ApiParam(value = "task object to be updated", required = true) Page page) {
    if (page == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      /* TODO: check wiki permissions */
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
      if (wiki == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String syntaxId = wikiService.getDefaultWikiSyntaxId();
      String currentUser = identity.getUserId();
      page.setAuthor(currentUser);
      page.setOwner(currentUser);
      page.setSyntax(syntaxId);
      page.setName(TitleResolver.getId(page.getTitle(), false));
      page.setUrl("");
      Page createdPage = wikiService.createPage(wiki, page.getParentPageName(), page);
      return Response.ok(createdPage, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      log.warn(String.format("Failed to perform auto save wiki page %s:%s:%s",
                             page.getWikiType(),
                             page.getWikiOwner(),
                             page.getId()),
               ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Update Wiki page
   *
   * @param page WIKI PAGE.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is
   *         performed successfully with status HTTPStatus.INTERNAL_ERROR if there
   *         is any unknown error in the saving process
   */
  @PUT
  @Path("/pages/{wikiType}/{wikiOwner:.+}/{pageId}")
  @RolesAllowed("users")
  public Response updatePage(@ApiParam(value = "task object to be updated", required = true)
                             @Context UriInfo uriInfo,
                             @PathParam("wikiType") String wikiType,
                             @PathParam("wikiOwner") String wikiOwner,
                             @PathParam("pageId") String pageId,
                             Page page) {
    if (page == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page page_ = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (page_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      if(!wikiService.hasPermissionOnPage(page_,PermissionType.EDITPAGE,identity)){
        return Response.status(Response.Status.FORBIDDEN).build();
      }

      if(!page_.getTitle().equals(page.getTitle())) {
        String newPageName = TitleResolver.getId(page.getTitle(), false);
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(page.getName())
                && !page.getName().equals(newPageName)) {
          wikiService.renamePage(wikiType, wikiOwner, page_.getName(), newPageName, page.getTitle());
          page_.setName(newPageName);
        }
        page_.setTitle(page.getTitle());
        wikiService.updatePage(page_, PageUpdateType.EDIT_PAGE_TITLE);
        wikiService.createVersionOfPage(page_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams pageParams = new WikiPageParams(wikiType,wikiOwner,newPageName);
          wikiService.removeDraftOfPage(pageParams);
        }
      }
      else if(!page_.getContent().equals(page.getContent())) {
        page_.setContent(page.getContent());
        wikiService.updatePage(page_, PageUpdateType.EDIT_PAGE_CONTENT);
        wikiService.createVersionOfPage(page_);
      }
      else if(!page_.getTitle().equals(page.getTitle())&&!page_.getContent().equals(page.getContent())) {
        String newPageName = TitleResolver.getId(page.getTitle(), false);
        page_.setTitle(page.getTitle());
        page_.setContent(page.getContent());
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(page.getName())
                && !page.getName().equals(newPageName)) {
          wikiService.renamePage(wikiType, wikiOwner, page_.getName(), newPageName, page.getTitle());
          page_.setName(newPageName);
        }
        wikiService.updatePage(page_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE);
        wikiService.createVersionOfPage(page_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams pageParams = new WikiPageParams(wikiType,wikiOwner,newPageName);
          wikiService.removeDraftOfPage(pageParams);
        }
      }
      return Response.ok().build();
    } catch (Exception ex) {
      log.warn(String.format("Failed to perform update wiki page %s:%s:%s",
                             page.getWikiType(),
                             page.getWikiOwner(),
                             page.getId()),
               ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }


  /**
   * Display the current tree of a wiki based on is path
   * @param type It can be a Portal, Group, User type of wiki
   * @param path Contains the path of the wiki page
   * @param currentPath Contains the path of the current wiki page
   * @param showExcerpt Boolean to display or not the excerpt
   * @param depth Defined the depth of the children we want to display
   * @return List of descendants including the page itself.
   */
  @GET
  @Path("/tree/{type}")
  @RolesAllowed("users")
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
        log.warn("User [{}] can not get wiki path [{}]. Wiki Home is used instead",
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
        Stack<WikiPageParams> stk = Utils.getStackParams(page);
        context.put(TreeNode.STACK_PARAMS, stk);
        responseData = getJsonTree(pageParam, context);
      } else if (type.equalsIgnoreCase(TREETYPE.CHILDREN.toString())) {
        // Get children only
        if (depth == null)
          depth = "1";
        context.put(TreeNode.DEPTH, depth);
        responseData = getJsonDescendants(pageParam, context);
      }

      encodeWikiTree(responseData, request.getLocale());
      return Response.ok(new BeanToJsons(responseData), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      log.error("Failed for get tree data by rest service - Cause : " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }


  private List<JsonNodeData> getJsonTree(WikiPageParams params,HashMap<String, Object> context) throws Exception {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(params.getType(), params.getOwner());
    WikiTreeNode wikiNode = new WikiTreeNode(wiki);
    wikiNode.pushDescendants(context);
    return TreeUtils.tranformToJson(wikiNode, context);
  }

  private List<JsonNodeData> getJsonDescendants(WikiPageParams params,
                                                HashMap<String, Object> context) throws Exception {
    TreeNode treeNode = TreeUtils.getDescendants(params, context);
    return TreeUtils.tranformToJson(treeNode, context);
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
      if (CollectionUtils.isNotEmpty(data.children)) {
        encodeWikiTree(data.children, locale);
      }
    }
  }

  private boolean hasEditPermission(List<PermissionEntry> permissions, String currentUser){
    if (permissions != null) {
      for (PermissionEntry permissionEntry : permissions) {
        if(permissionEntry.getId().equals(currentUser)) {
          for(Permission permission : permissionEntry.getPermissions()) {
            if(permission.getPermissionType().equals(PermissionType.EDITPAGE) && permission.isAllowed()) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }


}
