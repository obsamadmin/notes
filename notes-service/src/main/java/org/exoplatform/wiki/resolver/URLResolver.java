package org.exoplatform.wiki.resolver;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wiki.service.WikiPageParams;

public class URLResolver extends Resolver{
  private OrganizationService orgSerivce ;
  public URLResolver(OrganizationService orgSerivce) throws Exception {
    this.orgSerivce = orgSerivce ;
  }

  @Override
  public WikiPageParams extractPageParams(String requestURL, SiteKey siteKey, UserNode portalUserNode) throws Exception {
    UserPortalConfigService configService = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                                                                                         .getComponentInstanceOfType(UserPortalConfigService.class);
    WikiPageParams params = new WikiPageParams() ;
    String wikiPageName;
    if (portalUserNode == null) {
      wikiPageName = "wiki";
    } else {
      wikiPageName = portalUserNode.getURI();
    }
    String uri = extractURI(requestURL, wikiPageName) ; 
    if(uri.indexOf("/") > 0) {
      String[] array = uri.split("/") ;      
      if(array[0].equals(PortalConfig.USER_TYPE)) {
        params.setType(PortalConfig.USER_TYPE)  ;
        if(array.length >= 3) {
          params.setOwner(array[1]);
          StringBuilder pageId = new StringBuilder();
          for(int i=2; i< array.length; i++){
            pageId.append(array[i]);
          }
          params.setPageName(pageId.toString());
          
        }else if(array.length == 2) {
          params.setOwner(array[1]);
          params.setPageName(WikiPageParams.WIKI_HOME);
        }        
      }else if(array[0].equals(PortalConfig.GROUP_TYPE)) {
        params.setType(PortalConfig.GROUP_TYPE)  ;
        String groupId = uri.substring(uri.indexOf("/")) ;
        
        if(orgSerivce.getGroupHandler().findGroupById(groupId) != null) {
          params.setOwner(groupId) ;
          params.setPageName(WikiPageParams.WIKI_HOME) ;
        }else {
          if(groupId.substring(1).indexOf("/") > 0) {
            String pageId = groupId.substring(groupId.lastIndexOf("/")+ 1) ;
            String owner = groupId.substring(0, groupId.lastIndexOf("/")) ;
            params.setOwner(owner) ;
            if(pageId != null && pageId.length() > 0) params.setPageName(pageId) ;
            else params.setPageName(WikiPageParams.WIKI_HOME) ;
          }else {
            params.setOwner(groupId) ;
            params.setPageName(WikiPageParams.WIKI_HOME) ;
          }
        }
      } else if (array[0].equals(PortalConfig.PORTAL_TYPE)) {
        params.setType(PortalConfig.PORTAL_TYPE);
        params.setOwner(array[1]);
        if (array.length >= 3) {
          params.setPageName(array[2]);
        } else {
          params.setPageName(WikiPageParams.WIKI_HOME);
        }
      }
    } else {
      if (siteKey == null && portalUserNode != null) {
        siteKey = portalUserNode.getPageRef().getSite();
      }
      if (siteKey != null) {
        params.setType(siteKey.getTypeName());
        params.setOwner(siteKey.getName());
        if (uri.length() > 0) {
          params.setPageName(uri);
        } else {
          params.setPageName(WikiPageParams.WIKI_HOME);
        }
      }
    }
    params.setPageName(TitleResolver.getId(params.getPageName(), true));
    return params;
  }

  private String extractURI(String url, String wikiPageName) throws Exception{
    String uri = StringUtils.EMPTY;
    String sign1 = "/" + wikiPageName + "/";
    String sign2 = "/" + wikiPageName;
    if(url.lastIndexOf(sign1) < 0){
      if(url.lastIndexOf(sign2) > 0) {
        uri = url.substring(url.lastIndexOf(sign2) + sign2.length()) ;
      }      
    } else{
      uri = url.substring(url.lastIndexOf(sign1) + sign1.length()) ;
    }
    
    if(uri != null && uri.length() > 0 && (uri.lastIndexOf("/") + 1) == uri.length()) 
      uri = uri.substring(0, uri.lastIndexOf("/")) ;
    return uri ;
  }

}
