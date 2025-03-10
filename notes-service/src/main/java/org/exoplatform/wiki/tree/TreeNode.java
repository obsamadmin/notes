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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.utils.Utils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;


public class TreeNode { 

  protected String           name;

  protected String           id;

  protected String           path;
  
  protected boolean          isRetricted     = false;

  protected boolean          hasChild;

  protected TreeNodeType     nodeType;

  protected List<TreeNode>   children        = new ArrayList<TreeNode>();

  final static public String STACK_PARAMS    = "stackParams";
  
  final static public String PATH            = "path";
  
  final static public String WITH_DRAFTS            = "withDrafts";
  
  public static final String SELECTED_PAGE   = "selectedPage";

  /** The current path that's used in move page */
  final static public String CURRENT_PATH    = "page";
  
  public static final String CURRENT_PAGE    = "currentPage";

  public static final String CAN_EDIT        = "canEdit";
  
  public static final String SHOW_EXCERPT    = "excerpt";

  public static final String SHOW_DESCENDANT = "showDes";

  public static final String CHILDREN_NUMBER = "childrenNumber";

  public static final String DEPTH           = "depth";
  
  public enum TREETYPE {
    ALL, CHILDREN
  }

  protected boolean          isSelected      = false;
  
  public TreeNode() {    
  }

  public TreeNode(String name, TreeNodeType nodeType) {    
    this.name = name;
    this.nodeType = nodeType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TreeNodeType getNodeType() {
    return nodeType;
  }

  public void setNodeType(TreeNodeType nodeType) {
    this.nodeType = nodeType;
  }

  public boolean isHasChild() {
    return hasChild;
  }

  public void setHasChild(boolean hasChild) {
    this.hasChild = hasChild;
  }

  public List<TreeNode> getChildren() {
    return children;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public void setChildren(List<TreeNode> children) {
    this.children = children;
  }

  public String getPath() {
    return path;
  }
  
  public boolean isRetricted() {
    return isRetricted;
  }

  public void setRetricted(boolean isRetricted) {
    this.isRetricted = isRetricted;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((children == null) ? 0 : children.hashCode());
    result = prime * result + (hasChild ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((nodeType == null) ? 0 : nodeType.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TreeNode other = (TreeNode) obj;
    if (children == null) {
      if (other.children != null)
        return false;
    } else if (!children.equals(other.children))
      return false;
    if (hasChild != other.hasChild)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (nodeType == null) {
      if (other.nodeType != null)
        return false;
    } else if (!nodeType.equals(other.nodeType))
      return false;
    if (path == null) {
      if (other.path != null)
        return false;
    } else if (!path.equals(other.path))
      return false;
    return true;
  } 
  
  public void pushDescendants(HashMap<String, Object> context, String userId) throws Exception {
    addChildren(context, userId);
    pushChildren(context, userId);
  }
  
  protected void addChildren(HashMap<String, Object> context, String userId) throws Exception {
  }
  
  protected int getNumberOfChildren(HashMap<String, Object> context, int size) {
    String childNumCdt = (String) context.get(CHILDREN_NUMBER);
    int childrenNUm = (childNumCdt == null || StringUtils.EMPTY.equals(childNumCdt)) ? -1
                                                                                    : Integer.valueOf(childNumCdt);
    if (childrenNUm < 0 || childrenNUm > size) {
      childrenNUm = size;
    }
    //Only apply for the first level
    if (context.containsKey(CHILDREN_NUMBER))
      context.remove(CHILDREN_NUMBER);

    return childrenNUm;
  }
  
  private void pushChildren(HashMap<String, Object> context, String userId) throws Exception {
    Deque<WikiPageParams> paramsStk = (Deque<WikiPageParams>) context.get(STACK_PARAMS);
    if (paramsStk == null) {
      pushChild(context, userId);
    } else {
      if (paramsStk.isEmpty()) {
        this.isSelected = true;
      } else {
        WikiPageParams params = new WikiPageParams();
        params = paramsStk.pop();
        context.put(STACK_PARAMS, paramsStk);
        if (this instanceof RootTreeNode) {
          SpaceTreeNode spaceNode = new SpaceTreeNode(params.getType());
          pushChild(spaceNode, context, userId);
        } else if (this instanceof SpaceTreeNode) {
          Wiki wiki = (Wiki) Utils.getObjectFromParams(params);
          WikiTreeNode wikiNode = new WikiTreeNode(wiki);
          pushChild(wikiNode, context, userId);
        } else if (this instanceof WikiTreeNode) {
          pushChild(context, userId);
        } else if (this instanceof WikiHomeTreeNode || this instanceof PageTreeNode) {
          Page page = (Page) Utils.getObjectFromParams(params);
          PageTreeNode pageNode = new PageTreeNode(page);
          pushChild(pageNode, context, userId);
        }
      }
    }
  }
  
  private void pushChild(TreeNode child, HashMap<String, Object> context, String userId) throws Exception {
    Boolean showDesCdt = (Boolean) context.get(SHOW_DESCENDANT);

    String depthCdt = (String) context.get(DEPTH);
    boolean showDes = (showDesCdt == null) ? true : showDesCdt;

    int depth = (depthCdt == null || StringUtils.EMPTY.equals(depthCdt)) ? -1 : Integer.valueOf(depthCdt);
    --depth;
    TreeNode temp = new TreeNode();
    if (showDes) {
      if (depth != 0) {
        context.put(DEPTH, String.valueOf(depth));
        for (int i = 0; i < children.size(); i++) {
          temp = children.get(i);
          if (child == null) {
            temp.pushDescendants(context, userId);
          } else if (child.equals(temp)) {
            temp.pushDescendants(context, userId);
            return;
          }
        }
      }
    }
  }

  private void pushChild(HashMap<String, Object> context, String userId) throws Exception {
    pushChild(null, context, userId);
  }

  public String buildPath() {
    return null;
  }
}
