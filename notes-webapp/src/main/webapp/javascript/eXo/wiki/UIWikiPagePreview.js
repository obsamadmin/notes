/**
 * Copyright (C) 2010 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */(function(base, uiForm, webuiExt, $) {

function UIWikiPagePreview(){
};

UIWikiPagePreview.prototype.init = function(maskId){
  var me = eXo.wiki.UIWikiPagePreview;
  me.maskId = maskId;
  var maskWorkpace = document.getElementById(maskId);
  if(maskWorkpace){
    var pagePreview = $(maskWorkpace).find('div.UIWikiPagePreview')[0];
    if(pagePreview){
      $(pagePreview).height(($(document).height() - 42) + "px");
      eXo.portal.UIMaskWorkspace.resetPositionEvt();
    }
  }

  $(window).keyup(function(event) {
    if (event.keyCode == 27) {
      var me = eXo.wiki.UIWikiPagePreview;
      var maskWorkpace = document.getElementById(me.maskId);
      if(maskWorkpace){
        var pagePreview = $(maskWorkpace).find('div.UIWikiPagePreview')[0];
        if(pagePreview){
          var closeLink = $(maskWorkpace).find('a.CloseButton')[0];
          $(window).keyup(null);
          document.location = closeLink.href;
        }
      }
      return false;
    }
    return true;
  });
};

eXo.wiki.UIWikiPagePreview = new UIWikiPagePreview();
return eXo.wiki.UIWikiPagePreview;

})(base, uiForm, webuiExt, $);
