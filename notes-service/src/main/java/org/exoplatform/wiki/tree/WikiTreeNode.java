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
package org.exoplatform.wiki.tree;

import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.utils.TreeUtils;

import java.util.HashMap;

public class WikiTreeNode extends TreeNode {
  private Wiki wiki;

  public WikiTreeNode(Wiki wiki) throws Exception {
    super(wiki.getOwner(), TreeNodeType.WIKI);
    this.wiki = wiki;
    this.path = buildPath();
    this.hasChild = true;
  }

  public WikiHomeTreeNode getWikiHomeTreeNode() {
    return (WikiHomeTreeNode) children.get(0);
  }

  @Override
  protected void addChildren(HashMap<String, Object> context, String userId) throws Exception {

    this.children.add(new WikiHomeTreeNode(wiki.getWikiHome()));
    super.addChildren(context, userId);
  }

  public Wiki getWiki() {
    return wiki;
  }

  public void setWiki(Wiki wiki) {
    this.wiki = wiki;
  }
  
  @Override
  public String buildPath() { 
    WikiPageParams params = new WikiPageParams(wiki.getType(), wiki.getOwner(), null);
    return TreeUtils.getPathFromPageParams(params);
  }
}
