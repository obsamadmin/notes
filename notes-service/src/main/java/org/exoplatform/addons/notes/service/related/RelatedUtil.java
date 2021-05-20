package org.exoplatform.addons.notes.service.related;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.addons.notes.mow.api.Page;
import org.exoplatform.addons.notes.service.WikiPageParams;
import org.exoplatform.addons.notes.tree.utils.TreeUtils;
import org.exoplatform.addons.notes.utils.Utils;

public final class RelatedUtil {
  private RelatedUtil() {}
  
  public static List<JsonRelatedData> pageToJson(List<Page> pages) {
    List<JsonRelatedData> jsonObjs = new ArrayList<>();
    for (Page page : pages) {
      String name = page.getName();
      String title = page.getTitle();
      String path = TreeUtils.getPathFromPageParams(Utils.getWikiPageParams(page));
      JsonRelatedData dataObj = new JsonRelatedData(name, title, path);
      jsonObjs.add(dataObj);
    }
    return jsonObjs;
  }
  
  /**
   * convert wiki page info to path string. <br>
   * The format: [wiki type]/[wiki owner]/[page id]
   * @param params
   * @return
   */
  public static String getPath(WikiPageParams params) {
    StringBuilder sb = new StringBuilder();
    if (params.getType() != null) {
      sb.append(params.getType());
    }
    if (params.getOwner() != null) {
      sb.append("/").append(Utils.validateWikiOwner(params.getType(), params.getOwner()));
    }
    if (params.getPageName() != null) {
      sb.append("/").append(params.getPageName());
    }
    return sb.toString();
  }
  
  /**
   * get wiki page params from the path made by {@link #getPath(WikiPageParams)} 
   * @param path made by {@link #getPath(WikiPageParams)}
   * @throws Exception if an error occurs.
   */
  public static WikiPageParams getPageParams(String path) throws Exception {
    return TreeUtils.getPageParamsFromPath(path);
  }
}
