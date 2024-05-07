# doDex-input, Front-End content input for the dodex widget

## Getting Started

Include with dodex page.

1. Add to your html document using defaults.

```html
   <!-- Dodex css file includes css for the 'input' module -->
    <link rel="stylesheet" href="(location)/dodex.min.css">
    <!-- JSONEditor css for editing private content in JSON files -->
    <link href="(location)/jsoneditor/dist/jsoneditor.min.css" rel="stylesheet" type="text/css">
  <style>
   /* Size and location of JSONEditor window */
   .editor {
      width: 80%; height: 400px; position: fixed; bottom: 0; left: 0;
    }
  </style>
</head>


    <body>
      <div class="dodex--open">
        <img src="(location)/dodex/images/dodex_g.ico">
      </div>
      <!-- container for the frontend JSON editor - will be positioned based on .editor class -->
      <div id="jsoneditor" class="editor"></div>
      <!-- Note; dodex-input will handle the implementation - see the input popup -->
      <script src="(location)/jsoneditor/dist/jsoneditor.min.js"></script>

      <script src="(location)/dodex.min.js" type="text/javascript"></script>
      <script src="(location)/dodex-input.min.js" type="text/javascript"></script>
      <script src="(location)/dodex-mess.min.js" type="text/javascript"></script>
      <script>
            doDex.init({
               input: doDexInput, // "private: none" is default so no popup input form
               mess: doDexMess    // defaults to "server: localhost:3087" for websockets
            });
      </script>
    </body>
 ```

2. Modifying defaults by adding inline javascript to page.

```html
    <script>
         var dodex = window.doDex; // global variable
         var input = window.doDexInput; // global variable
         var mess = window.doDexMess; // global variable

         /* Card content can be customized - returns an object
           This content is used for cards A-Z and static card 27
         */
         dodex.setContentFile("(location)/content.js");

         // Change size and or position - returns a Promise
         dodex.init({
            width:375,
            height: 200,
            top: "100px",
            left: "50%",
            input: input,        // required if using frontend content load
            private: "partial",  // frontend load of private content, "none", "full", "partial"(only cards 28-52) - default none
            replace: true,       // append to or replace default content - default false(append only)
            mess: mess,          // requireed if using messaging client.
            server: "localhost:3087"  // default demo server - see node_modules/dodex-mess/server
            })

         // Add up to 24 additional cards. Card # must start at 28.
         .then(function () {
            for (var i = 0; i < 3; i++) {
              dodex.addCard({/*see custom content section*/});
            }
            /* Auto display of widget */
            dodex.openDodex();
         });
    </script>
```

3. Loading as a module.

* Using es6 syntax

```javascript
       import dodex from "dodex";
       import input from "dodex-input";
```

* Using commonjs syntax

```javascript
       var dodex = require("dodex").default;
       var input = require("dodex-input").default;
```

* Changing default behavior in an application module

```javascript

       import dodex from "dodex";
       import input from "dodex-input";
       /* This content is used for cards A-Z and static card 27 */
       dodex.setContentFile("<location>/content.js");

       dodex.init({
          width: 375,
          height: 200,
          left: "50%",
          top: "100px",
          input: input,
          private: "partial",
          replace: false
       })
       .then(function () {
           /* Add up to 24 additional cards. */
           for(var i = 0; i < 1; i++) {
               dodex.addCard(content);
           }
           /* Auto display of widget */
           dodex.openDodex();
      });

      var content = {
          cards: {
             card28: {     // Notice, card # starts at 28
               tab: "Me1", // Maximum 3 characters
               front: {
                  content: `<h1>My Personal Stuff</h1>`
               },
               back: {
                  content: `<h1>More personal Stuff</h1>`
               }
          }}}

```

4. Adding content to the dodex cards(content can be from a javascript or JSON file).

      Use the following as a javascript template. You only need to include cards with content. See node_modules/dodex/data for examples.

   __Note;__ The window scoped "dodexContent" is required to load content at initialization. This allows content without using a module.(dodex.setContentFile). The additional card content as well as content loaded from the front-end Input module, use plain objects.

```javascript

      dodexContent = {
          cards: {
             card1: {
               tab: "A",
               front: {
                  content: ""
               },
               back: {
                  content: ""
               }
             },
             card2: {
                tab: "B",
                front: {
                   content: ""
                },
                back: {
                   content: ""
                }
             },
             card3: {
                tab: "C",
                front: {
                   content: `<h1>Best's Contact Form</h1><a href="#!contact"><i class="fa fa-fw fa-phone"></i>Contact</a>`
                },
                back: {
                   content: `<h1>Lorem Ipsum</h1><a href="https://www.yahoo.com" target="_">Yahoo</a>`
                }
             },
             card27: {
                tab: "",
                front: {
                   content: ""
                },
                back: {
                   content: `<h1 style="font-size: 14px;">
                      <svg height="18" width="17" style="font-family: 'Open Sans', sans-serif;">
                      <text x="3" y="18" fill="#059">O</text><text x="0" y="15" fill="#059">D</text></svg> doDex</h1>`
                }
             }
          }
        }
```

### Operation

1. Clicking on the dodex icon will toggle the widget's visibility.
1. Click a tab to page to desired card.
1. Click the face or back of a card to flip current cards.
1. Enter a dial with mouse and with mouse down slowly move up or down to flip cards.
1. Double-Click on bottom static card or dials to popup the front-end load file form.
1. Double-Click again to close or click close button.
1. As of Dodex-Input 1.3.0 the following are supported:
   1. JSON files can now use arrays as content to make JSON more readable.
      * For example, you can code;
         1. ```"back": {"content": ["back", "of", "card"]}, "front": {"content": ["front", "of", "card"]}```
   2. JSON files can be edited and saved from the frontend dodex-input dialog form.
      * This feature is optional, to implement you must add the following to the web page;
         1. The jsoneditor css style link ```<link href="(location)/jsoneditor/dist/jsoneditor.min.css" rel="stylesheet" type="text/css">```. see; <https://github.com/josdejong/jsoneditor>
         2. The css class ```<style>.editor { width: 80%; height: 400px; position: fixed; bottom: 0; left: 0; }</style>``` and this can be customized.
         3. The jsoneditor javascript file; ```<script src="(location)/jsoneditor/dist/jsoneditor.min.js"></script>```.
   3. Personal Data can now be removed from the frontend dodex-input dialog form. An edited or new content file can then be loaded.

__Note;__ Firefox works best by only clicking the tabs.

### Prerequisites

Browser must support the "indexedDB" storage feature. To clear content from the "indexedDB", execute from your browser's dev-tools console; `indexedDB.deleteDatabase("dodex")`.

### Installing

1. `npm install dodex --save` or download from <https://github.com/DaveO-Home/dodex>.
1. `npm install dodex-input --save` or download from <https://github.com/DaveO-Home/dodex-input>.
1. `npm install jsoneditor --save` if using the frontend json editor.
1. Optionally copy `node_modules/dodex/` javascript, css and images to appropriate directories; If using a bundler like browserify, you may only need to copy the content.js(or create your own) and images.  
__Note;__ Content can also be loaded from a `JSON` file.

Here's an example of dodex loaded in a `bootstrap` environment (view on GitHub <https://github.com/DaveO-Home/dodex>).

![dodex](./images/dodex.png?raw=true)

## Deployment

See getting started.

## Test

1. Open in any browser.
1. Load `node_modules/dodex-input/test/index.html`

## Built With

* [SASS](https://sass-lang.com/) - css build
* [Javascript](https://www.javascript.com//) - language
* [Rollup](https://rollupjs.org/) - bundler

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
