/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.webui;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.control.UIWikiToolBar;

@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiApplicationControlArea.gtmpl"
)
public class UIWikiApplicationControlArea extends UIContainer {
  public UIWikiApplicationControlArea() throws Exception{
    addChild(UIWikiSearchBox.class, null, null);
    addChild(UIWikiToolBar.class, null, null);
    UIWikiBreadCrumb uiWikiBreadCrumb = addChild(UIWikiBreadCrumb.class, null, null);
    uiWikiBreadCrumb.setAllowChooseSpace(true);
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    UIWikiBreadCrumb wikiBreadCrumb = findFirstComponentOfType(UIWikiBreadCrumb.class);
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    String currentActionLabel = getCurrentActionLabel();
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    UISpacesSwitcher uiWikiSpaceSwitcher = wikiBreadCrumb.getChildById(UIWikiBreadCrumb.SPACE_SWITCHER);
    String currentSpaceName = Utils.getCurrentSpaceName();
    if (!StringUtils.isEmpty(currentSpaceName)) {
      uiWikiSpaceSwitcher.setCurrentSpaceName(Utils.upperFirstCharacter(currentSpaceName));
    }
    wikiBreadCrumb.setBreadCumbs(wikiService.getBreadcumb(params.getType(), params.getOwner(), params.getPageName()));
    wikiBreadCrumb.setActionLabel(currentActionLabel);
    super.processRender(context);
  }
  
  private  String getCurrentActionLabel() {
    UIWikiPortlet wikiPortlet= this.getAncestorOfType(UIWikiPortlet.class);
    switch (wikiPortlet.getWikiMode()) {
    case EDITPAGE:
      return "UIWikiPortlet.label.Edit-Page";
    case ADDPAGE:
      return "UIWikiPortlet.label.Add-Page";
    case ADVANCEDSEARCH:
      return "UIWikiPortlet.label.Advanced-Search";
    case SHOWHISTORY:
      return "UIWikiPortlet.label.Show-History";
    case VIEWREVISION:
      return "UIWikiPortlet.label.View-Version";
    case EDITTEMPLATE:
      return "UIWikiPortlet.label.Edit-Template";
    case ADDTEMPLATE:
      return "UIWikiPortlet.label.Add-Template";
    case COMPAREREVISION:
      return "UIWikiPortlet.label.Compare-Version";
    case SPACESETTING:
      return "UIWikiPortlet.label.Setting";
    case PAGEINFO:
      return "UIWikiPageInfo.label.Page-Info";
    default:
      return "";
    }
  }
}
