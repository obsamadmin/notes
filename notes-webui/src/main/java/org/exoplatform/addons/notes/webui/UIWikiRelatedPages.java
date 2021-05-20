package org.exoplatform.addons.notes.webui;

import java.util.Arrays;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.addons.notes.webui.core.UIWikiContainer;

@ComponentConfig(
                 lifecycle = Lifecycle.class,
                 template = "app:/templates/wiki/webui/UIWikiRelatedPages.gtmpl"
               )
public class UIWikiRelatedPages extends UIWikiContainer {
  public UIWikiRelatedPages() {
    super();
    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.EDITPAGE,
        WikiMode.VIEWREVISION, WikiMode.SHOWHISTORY, WikiMode.PAGEINFO });
  }
}
