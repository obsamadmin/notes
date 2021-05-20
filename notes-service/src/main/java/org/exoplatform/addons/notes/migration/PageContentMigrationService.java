package org.exoplatform.addons.notes.migration;

import org.exoplatform.addons.notes.mow.api.Page;

public interface PageContentMigrationService {

  void migratePage(Page page) throws Exception;

}
