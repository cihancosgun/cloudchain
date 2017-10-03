var MongoClient = require("mongodb").MongoClient,
    BSON = require("mongodb").BSONPure,
    ObjectID = require('mongodb').ObjectID,
    server = module.parent.exports.server,
    config = module.parent.exports.config,
    debug = module.parent.exports.debug,
    upload = module.parent.exports.upload,
    express = module.parent.exports.express,
    util = require("./util").util;

debug("download.js is loaded");

function findOneRecord(coll, query, callback){
    MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
      db.collection(coll, function (err, collection) {      
        collection.findOne(query, function(err, result) {
          if (err) callback(null);
          db.close();
          return callback(result);
        });
      });//collection
    });//connect
  }

  function checkUserLogin(user,callback){  
      if(user == undefined){
          callback(null);
      }else{
        findOneRecord("users",user,function(result){
            if(result != null){
              callback(result);
            }else{
              callback(null);
            }
          });
      }
  }

/**
* File Download
*/
server.get('/download', function (req, res) {
     
    if(req.query.email != undefined){        
        var email = req.query.email;
        var password =  req.query.password;
        var user = {"email":email, "password":password};            
        checkUserLogin(user, function(loginResult){
            if(loginResult){                
                if (req.query.file != undefined) {
                    var file = __dirname + '/../' + req.query.file;
                    var fileQuery = {"mail":user.email, uploadfilepath: req.query.file };
                    if(findOneRecord("files",fileQuery, function(result){
                        if(result != null){
                            res.download(file, result.name);
                        }else{
                            res.json(500, { "ok": false });        
                        }
                    }));                                        
                } else {
                    res.json(500, { "ok": false });
                }
            }
            else{
                res.json(500, {"error":"login is fail"});
            }
        });
    }else{
        res.json(500, { "ok": false });
    }
});