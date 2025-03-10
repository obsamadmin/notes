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
package org.exoplatform.wiki.webui.form;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.input.UIUploadInput;

@ComponentConfig(
   template = "app:/templates/wiki/webui/form/UIWikiFormUploadInput.gtmpl", 
   events = {
    @EventConfig(listeners = UIUploadInput.CreateUploadIdActionListener.class),
    @EventConfig(listeners = UIUploadInput.RemoveUploadIdActionListener.class) })
public class UIWikiFormUploadInput extends UIUploadInput {

  final static public String UPLOAD_ACTION = "UploadAttachment" ; 
  
  /**
   * The auto upload feature
   */
  private boolean isAutoUpload = false;
  
  public boolean isAutoUpload() {
    return isAutoUpload;
  }

  public void setAutoUpload(boolean isAutoUpload) {
    this.isAutoUpload = isAutoUpload;
  }
  
  public UIWikiFormUploadInput(String name, String bindingExpression) {
    super(name, bindingExpression);
    setComponentConfig(UIWikiFormUploadInput.class, null);
  }
  
  public UIWikiFormUploadInput(String name, String bindingExpression, int limit) {
    super(name, bindingExpression, limit);
    setComponentConfig(UIWikiFormUploadInput.class, null);
  }
  
  public String getPostUploadActionLink() throws Exception {
    UIComponent uiParent = getParent();
    if(uiParent != null){
      return uiParent.event(UPLOAD_ACTION, getUploadIds()[0]);
    } else {
      return "";
    }
  }
}
