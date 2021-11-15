'use strict';

( function() {
  //var btn = ' <v-btn class="btn-primary">test</v-btn>',
  var tpl = new CKEDITOR.template( '<div id={noteChildId}></div>' );

  CKEDITOR.plugins.add('toc', {

    icons: 'toc',

    init: function (editor) {

      var pluginDirectory = this.path;
      editor.addContentsCss(pluginDirectory + 'toc.css');

      var tpl2 = tpl.output( { noteChildId:'noteManuelChildren'} );
      var figure = CKEDITOR.dom.element.createFromHtml( tpl2 );
      editor.insertElement( figure );
      console.warn('olaaaaa outside vue',editor.getData());
      editor.addCommand('ToC', {

        exec: function (editor, childrenList) {
          //editor.insertElement( figure );
          require(['SHARED/vuetify'], function (vuetify) {
            new Vue({
              el: '#noteManuelChildren',
              template: '<v-btn class="btn-primary">test</v-btn>',
              vuetify: new Vuetify(eXo.env.portal.vuetifyPreset),
              data() {
                return {}
              },
              mounted() {
                console.warn('olaaaaa fron vue',editor.document);
              },
              methods: {}
            })
          });

          /*if ( childrenList.length ) {
            var div = editor.document.createElement('div');
            var listChildItems = '<ul class="note-manual-child">';
            for (var j = 0; j < childrenList.length; j++){
              if (childrenList[j].hasChild) {
                listChildItems += '<li class="note-child-item has-child"><a href="'+childrenList[j].id+'" class="noteLink">'+childrenList[j].name+'<a></li>';
              } else {
                listChildItems += '<li class="note-child-item"><a href="'+childrenList[j].id+'" class="noteLink">'+childrenList[j].name+'<a></li>';
              }
            }
            listChildItems += '</ul>';
            div.setHtml(listChildItems);
            editor.insertElement( div );
          }*/
        }
      });
    }

  });
} )();
