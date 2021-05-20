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
package org.exoplatform.addons.notes.webui.control.action;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.addons.notes.commons.Utils;
import org.exoplatform.addons.notes.service.WikiPageParams;
import org.exoplatform.addons.notes.service.WikiService;
import org.exoplatform.addons.notes.utils.WikiConstants;
import org.exoplatform.addons.notes.webui.UIWikiBreadCrumb;
import org.exoplatform.addons.notes.webui.UIWikiLocationContainer;
import org.exoplatform.addons.notes.webui.UIWikiPortlet;
import org.exoplatform.addons.notes.webui.UIWikiPortlet.PopupLevel;
import org.exoplatform.addons.notes.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.addons.notes.webui.control.filter.DeniedOnWikiHomePageFilter;
import org.exoplatform.addons.notes.webui.control.filter.EditPagesPermissionFilter;
import org.exoplatform.addons.notes.webui.control.filter.IsViewModeFilter;
import org.exoplatform.addons.notes.webui.control.listener.MoreContainerActionListener;
import org.exoplatform.addons.notes.webui.popup.UIWikiMovePageForm;

@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/AbstractActionComponent.gtmpl",
  events = {
    @EventConfig(listeners = MovePageActionComponent.MovePageActionListener.class)
  }
)
public class MovePageActionComponent extends AbstractEventActionComponent {
  
  public static final String                   ACTION  = "MovePage";
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsViewModeFilter(), new DeniedOnWikiHomePageFilter(), new EditPagesPermissionFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  @Override
  public String getActionName() {
    return ACTION;
  }

  @Override
  public boolean isAnchor() {
    return false;
  }
  
  public static class MovePageActionListener extends MoreContainerActionListener<MovePageActionComponent> {
    @Override
    protected void processEvent(Event<MovePageActionComponent> event) throws Exception {      
      ResourceBundle res = event.getRequestContext().getApplicationResourceBundle();
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      UIWikiPortlet uiWikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      WikiPageParams params = Utils.getCurrentWikiPageParams();     
      if (Utils.getCurrentWikiPage().getName().equals(WikiConstants.WIKI_HOME_NAME)) {
        event.getRequestContext()
             .getUIApplication()
             .addMessage(new ApplicationMessage("UIWikiMovePageForm.msg.can-not-move-wikihome", null, ApplicationMessage.WARNING));                
        return;
      }
      UIPopupContainer uiPopupContainer = uiWikiPortlet.getPopupContainer(PopupLevel.L1);
      UIWikiMovePageForm movePageForm = uiPopupContainer.activate(UIWikiMovePageForm.class, 600);
      movePageForm.setDupplicatedPages(null);
      UIWikiLocationContainer locationContainer = movePageForm.findFirstComponentOfType(UIWikiLocationContainer.class);
      UIWikiBreadCrumb currentLocation = locationContainer.getChildById(UIWikiLocationContainer.CURRENT_LOCATION);
      currentLocation.setBreadCumbs(wikiService.getBreadcumb(params.getType(), params.getOwner(), params.getPageName()));
      UIFormInputInfo pageNameInfo = movePageForm.getUIFormInputInfo(UIWikiMovePageForm.PAGENAME_INFO);
      pageNameInfo.setValue(res.getString("UIWikiMovePageForm.msg.you-are-about-move-page")
          +" "+ StringCommonUtils.decodeSpecialCharToHTMLnumber(Utils.getCurrentWikiPage().getTitle()));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
      requireJS.require("SHARED/UITreeExplorer", "UITreeExplorer")
      .addScripts("UITreeExplorer.setMovePage(true); ");
      super.processEvent(event);
    }
  }
}
