/**
 * Copyright 2017 cloudchain. All Rights Reserved.
 *
 * server.js
 * crest
 */

var fs = require("fs"),
  mongodb = require("mongodb"),
  express = module.exports.express = require("express"),
  bodyParser = module.exports.bodyParser =  require('body-parser'),
  multer = module.exports.multer = require('multer'),
  fs = module.exports.fs = require('fs'),     
  cors = require('cors'),
  app = module.exports.app = express();

var DEBUGPREFIX = "DEBUG: ";

var config = {
  "db": {
    "db":"cloudchaindb",
    "port": 27017,
    "host": "localhost"
  },
  "server": {
    "port": 3500,
    "address": "0.0.0.0"
  },
  "flavor": "mongodb",
  "debug": false,
  "upload": "uploads/",
  "uploadserver": {
    "port": 3000,
    "address": "0.0.0.0"
  }
};

var debug = module.exports.debug = function (str) {
  if (config.debug) {
    console.log(DEBUGPREFIX + str);
  }
};

try {
  config = JSON.parse(fs.readFileSync(process.cwd() + "/config.json"));
} catch (e) {
  debug("No config.json file found. Fall back to default config.");
}

module.exports.config = config;


app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cors());

var http = module.exports.http  = require('http').Server(app);

 
var router = module.exports.server = express.Router(); 
app.use('/', router);

var upload = module.exports.upload = multer({ dest: config.upload });

var io = module.exports.io = require('socket.io')(http);

require('./lib/rest');
require('./lib/upload');
require('./lib/download');
 
http.listen(config.server.port, function () {
  console.log("Server listening at %s", config.server.port);
});

