# doDex-mess, Front-End messaging client for Dodex

## Getting Started

Include with dodex page.

1. Add to your html document using defaults.

```html
   <!-- Dodex css file includes css for the 'input' module -->
    <link rel="stylesheet" href="(location)/dodex.min.css">
    <body>
      <div class="dodex--open">
        <img src="(location)/dodex/images/dodex_g.ico">
      </div>
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
            replace: true        // append to or replace default content - default false(append only)
            mess: mess,          // messaging client for dodex
            server: "localhost:3087" // default koa demo server - see node_modules/dodex-mess/server
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
       import mess from "dodex-mess";
```

* Using commonjs syntax

```javascript
       var dodex = require("dodex").default;
       var input = require("dodex-input").default;
       var mess = require("dodex-mess").default;
```

* Changing default behavior in an application module

```javascript

       import dodex from "dodex";
       import input from "dodex-input";
       import mess from "dodex-mess";
       /* This content is used for cards A-Z and static card 27 */
       dodex.setContentFile("<location>/content.js");

       dodex.init({
          width: 375,
          height: 200,
          left: "50%",
          top: "100px",
          input: input,
          private: "partial",
          replace: false,
          mess: mess,
          server: "localhost:3087"
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
1. Ctrl+Double-Click on bottom static card or dials to popup the client messaging form.
1. On initial access, the user-id input form will popup. Enter your handle for messaging.
1. Click on "more button" to view options.
1. Enter a message and click "send", assuming that he demo server has been started.

__Note;__ Dodex messaging supports both broadcast and private messages. The default database on the backend is "Sqlite3", no further configuation is necessay. However, the backend can be configured with any database supported by "knex". Postgres has also been tested with dodex-messaging. The database maintains the registered users and any undelivered private messages. Broadcast messages will only be delivered to active online users.

### Prerequisites

Browser must support the "indexedDB" storage feature. To clear content from the "indexedDB", execute from your browser's dev-tools console; `indexedDB.deleteDatabase("comm")`.

### Installing

1. `npm install dodex --save` or download from <https://github.com/DaveO-Home/dodex>.
1. `npm install dodex-input --save` or download from <https://github.com/DaveO-Home/dodex-input>.
1. `npm install dodex-mess --save` or download from <https://github.com/DaveO-Home/dodex-mess>.
1. Optionally copy `node_modules/dodex/`, `node_modules/dodex-input/` and `node_modules/dodex-mess/` javascript, css and images to appropriate directories; If using a bundler like browserify, you may only need to copy the content.js(or create your own) and images.
1. The Demo Koa server.
   1. Copy "node_modules/dodex-mess/server" to an appropriate directory.
   1. `cd` to the directory and execute `npm ci` || `npm install`.
   1. Execute `npm start`.
1. In lieu of Koa you can use the Java/rxJava asynchronous server ```dodex-vertx```.

## Deployment

See getting started.

## Test

1. Make sure the demo Koa server is running.
1. Test Dodex by entering the URL `localhost:3087/test/index.html` in a browser.
1. Ctrl+Double-Click a dial or bottom card to popup the messaging client.
1. To test the messaging, open up the URL in a different browser.
1. Enter a message and click send to test.

## Use same credentials across browsers and devices

1. In the master browser open dodex-mess(ctrl+double-click bottom static card).
1. Click the more button(...).
1. Select `Grab Credentials`.
1. Paste into a file or favorite internet application(email, text-message etc.).
1. Open dodex-mess in another browser or a browser on other devices.
1. Select `Change Handle`.
1. Paste master credentials into the `Enter your chat handle` input popup.
1. Click `Save` button.

The secondary browsers will now have the same credentials as your master browser and only one `handle` will show when sending private messages.  The first browser logged into will recieve the private message.

## Built With

* [SASS](https://sass-lang.com/) - css build
* [Javascript](https://www.javascript.com//) - language
* [Rollup](https://rollupjs.org/) - bundler

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
