package org.exoplatform.addons.notes.webui;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.addons.notes.commons.Utils;
import org.exoplatform.addons.notes.mow.api.Page;
import org.exoplatform.addons.notes.mow.api.PageVersion;
import org.exoplatform.addons.notes.service.WikiPageParams;
import org.exoplatform.addons.notes.service.WikiService;
import org.exoplatform.addons.notes.tree.TreeNode;
import org.exoplatform.addons.notes.tree.utils.TreeUtils;
import org.exoplatform.addons.notes.utils.VersionNameComparatorDesc;
import org.exoplatform.addons.notes.webui.control.UIRelatedPagesContainer;
import org.exoplatform.addons.notes.webui.control.action.ShowHistoryActionListener;
import org.exoplatform.addons.notes.webui.control.action.ViewRevisionActionListener;
import org.exoplatform.addons.notes.webui.core.UIWikiContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ComponentConfig(
  template = "app:/templates/wiki/webui/UIWikiPageInfo.gtmpl",
  events = {
    @EventConfig(listeners = ViewRevisionActionListener.class),
    @EventConfig(listeners = ShowHistoryActionListener.class)
  }
)
public class UIWikiPageInfo extends UIWikiContainer {
  private static final Log log                     = ExoLogger.getLogger(UIWikiPageInfo.class);

  private static final int NUMBER_OF_SHOWN_CHANGES = 5;

  private WikiService wikiService;

  public UIWikiPageInfo() throws Exception {
    super();

    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.PAGEINFO });
    addChild(UIRelatedPagesContainer.class, null, null);
  }

  protected List<PageVersion> getVersionList(Page page) {
    List<PageVersion> versions = null;
    try {
      versions = wikiService.getVersionsOfPage(page);
      Collections.sort(versions, new VersionNameComparatorDesc());
      return versions.subList(0,
                              versions.size() > NUMBER_OF_SHOWN_CHANGES ? NUMBER_OF_SHOWN_CHANGES : versions.size());
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn(String.format("getting version list of page %s failed", page.getName()), e);
      }
    }
    return versions;
  }

  protected String getPageLink(Page page) throws Exception {
    WikiPageParams params = org.exoplatform.addons.notes.utils.Utils.getWikiPageParams(page);
    return Utils.getURLFromParams(params);
  }

  protected String renderHierarchy() throws Exception {
    StringBuilder treeSb = new StringBuilder();
    StringBuilder initSb = new StringBuilder();
    TreeNode node = TreeUtils.getTreeNode(Utils.getCurrentWikiPageParams());
    String treeRestURI = Utils.getCurrentRestURL().concat("/wiki/tree/children/");
    String redirectURI = getAncestorOfType(UIWikiPortlet.class).getRedirectURL();
    String baseURL = getBaseUrl();

    initSb.append("?")
            .append(TreeNode.PATH)
            .append("=")
            .append(node.getPath())
            .append("&")
            .append(TreeNode.DEPTH)
            .append("=1");
    treeSb.append("<div class=\"uiTreeExplorer PageTreeMacro\">")
            .append("  <div>")
            .append("    <input class=\"ChildrenURL\" title=\"hidden\" type=\"hidden\" value=\"").append(treeRestURI).append("\" />")
            .append("    <input class=\"InitParams\" title=\"hidden\" type=\"hidden\" value=\"").append(initSb.toString()).append("\" />")
            .append("    <input class=\"BaseURL\" title=\"hidden\" type=\"hidden\" value=\"").append(baseURL).append("\" />")
            .append("    <a class=\"SelectNode\" style=\"display:none\" href=\"").append(redirectURI).append("\" ></a>")
            .append("  </div>")
            .append("</div>");
    return treeSb.toString();
  }

  protected String getBaseUrl() throws Exception {
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    params.setPageName(null);
    return Utils.getURLFromParams(params);
  }
  
  protected Page getCurrentPage() throws Exception {
    return Utils.getCurrentWikiPage();
  }

  protected Page getCurrentParentPage() throws Exception {
    return wikiService.getParentPageOf(getCurrentPage());
  }
}
