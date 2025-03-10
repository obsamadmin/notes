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
package org.exoplatform.wiki.webui.control.action;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.webui.EditMode;
import org.exoplatform.wiki.webui.UIWikiPageEditForm;
import org.exoplatform.wiki.webui.UIWikiPageTitleControlArea;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.UIWikiRichTextArea;
import org.exoplatform.wiki.webui.WikiMode;
import org.exoplatform.wiki.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.wiki.webui.control.filter.EditPagesPermissionFilter;
import org.exoplatform.wiki.webui.control.filter.IsViewModeFilter;
import org.exoplatform.wiki.webui.control.listener.UIPageToolBarActionListener;

@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/EditPageActionComponent.gtmpl",
  events = {
    @EventConfig(listeners = EditPageActionComponent.EditPageActionListener.class)
  }
)
public class EditPageActionComponent extends AbstractEventActionComponent {
  
  public static final String                   ACTION  = "EditPage";

  public static final String                   SECTION = "section";

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsViewModeFilter(), new EditPagesPermissionFilter() });

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
    return true;
  }
  
  public static class EditPageActionListener extends UIPageToolBarActionListener<EditPageActionComponent> {
    @Override
    protected void processEvent(Event<EditPageActionComponent> event) throws Exception {
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      String sectionIndex = event.getRequestContext().getRequestParameter(SECTION);
      if (sectionIndex == null || sectionIndex.length() == 0) {
        wikiPortlet.changeEditMode(EditMode.ALL);
      } else {
        wikiPortlet.changeEditMode(EditMode.SECTION);
        wikiPortlet.setSectionIndex(sectionIndex);
      }
      UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
      UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class).getUIStringInput();
      UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
      UIFormStringInput commentInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_COMMENT);
      
      Page page = Utils.getCurrentWikiPage();
      String title = page.getTitle();
      String content = page.getContent();
      titleInput.setEditable(true);
      if (wikiPortlet.getEditMode() == EditMode.SECTION) {
        titleInput.setEditable(false);
      }
      titleInput.setValue(title);
      pageEditForm.setTitle(title) ;
      markupInput.setValue(content);
      commentInput.setValue("");
      commentInput.setRendered(true);
      wikiPortlet.changeMode(WikiMode.EDITPAGE);
      UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
      if (wikiRichTextArea.isRendered()) {
        Utils.feedDataForWYSIWYGEditor(pageEditForm, null);
      }      
      pageEditForm.setInitDraftName(StringUtils.EMPTY);
      super.processEvent(event);
    }
  }
}
