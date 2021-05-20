package org.exoplatform.addons.notes.service.listener;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.addons.notes.WikiException;
import org.exoplatform.addons.notes.mow.api.Page;
import org.exoplatform.addons.notes.service.PageUpdateType;

/**
 * Listener to trigger actions on page operations
 */
public abstract class PageWikiListener extends BaseComponentPlugin {
  public abstract void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;

  public abstract void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException;

  public abstract void postDeletePage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;
}
