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
package org.exoplatform.wiki.webui.control.filter;

import java.util.Map;

import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.commons.WikiConstants;
import org.exoplatform.wiki.webui.WikiMode;

public class IsEditAddModeFilter extends UIExtensionAbstractFilter {

  public IsEditAddModeFilter() {
    this(null);
  }
  
  public IsEditAddModeFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    WikiMode wikiMode = Utils.getWikiMode();
    return (wikiMode == WikiMode.EDITPAGE || wikiMode == WikiMode.ADDPAGE || wikiMode == WikiMode.EDITTEMPLATE || wikiMode == WikiMode.ADDTEMPLATE);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {

    
  }

}
