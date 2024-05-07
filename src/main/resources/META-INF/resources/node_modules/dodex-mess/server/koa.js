const { accessLogger, logger } = require("./js/logger");
const utils = require("./js/utils.js");
global.accessLogger = accessLogger;
global.logger = logger;
const app = require("./js/app");
const port = process.env.PORT || 3087;

app.listen(port, function listening() {
  utils.log("info", `Listening on ${app.address().port}`, __filename);
});
