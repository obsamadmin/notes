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

function UIWikiPageVersionsList(){
};

UIWikiPageVersionsList.prototype.init = function(formId) {
  var me = eXo.wiki.UIWikiPageVersionsList;
  me.selectedCheckbox = new Array();
  var versionListForm = document.getElementById(formId);
  var inputs = $(versionListForm).find('input');
  me.compareButton = $(versionListForm).find('button.refreshModeTarget')[0];
  $(this.compareButton).click(function(evt) {
    if (me.selectedCheckbox.length == 2) {
      eXo.wiki.UIWikiAjaxRequest.makeNewHash("#CompareRevision");
    }
  });

  var ln = inputs.length;
  var countCheckBox = 0;
  for ( var i = 0; i < ln; i++) {
    var input = inputs[i];
    if (input.type == "checkbox") {
      input.checked = false;
      $(input).click(me.onCheck);
      countCheckBox++;
    }
  }
  $(me.compareButton).attr('class', 'btn  disableButton refreshModeTarget');
  $(me.compareButton).attr('disabled', true);
};

UIWikiPageVersionsList.prototype.onCheck = function(evt) {
  var me = eXo.wiki.UIWikiPageVersionsList;
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  
  if (target.checked == true) {
    me.selectedCheckbox.push(target);
    if (me.selectedCheckbox.length > 2) {
      var popCheckbox = me.selectedCheckbox.shift();
      popCheckbox.checked = false;
    }
  } else {
    me.selectedCheckbox.remove(target);
  }
  
  if (me.selectedCheckbox.length == 2) {
    $(me.compareButton).attr('class','btn btn-primary refreshModeTarget');
	$(me.compareButton).attr('disabled', false);
  } else {
    $(me.compareButton).attr('class','btn  disableButton refreshModeTarget');
	$(me.compareButton).attr('disabled', true);
  }
};

eXo.wiki.UIWikiPageVersionsList = new UIWikiPageVersionsList();
return eXo.wiki.UIWikiPageVersionsList;

})(base, uiForm, webuiExt, $);
