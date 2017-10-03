var MongoClient = require("mongodb").MongoClient,
    BSON = require("mongodb").BSONPure,
    ObjectID = require('mongodb').ObjectID,
    server = module.parent.exports.server,
    config = module.parent.exports.config,
    debug = module.parent.exports.debug,
    upload = module.parent.exports.upload,
    express = module.parent.exports.express,
    fs = module.parent.exports.fs,
    util = require("./util").util;

debug("upload.js is loaded");

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

  function insertRecord(coll, rec, callback){
    MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
      db.collection(coll, function (err, collection) {
        collection.insert(rec, function (err, docs) {        
          callback(err == undefined);        
          db.close();
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
* File Upload
*/
server.post('/upload', upload.single('file'), function (req, res) {
   if(req.body.email != undefined){
       var user = {"email":req.body.email, "password":req.body.password};
    checkUserLogin(user, function(loginResult){
        var path = '';
        var originalFileName = '';
        originalFileName = req.file.originalname;
        path = req.file.path;    
        if(loginResult != null){
            var rec = {};
            var date = new Date();
            rec._id = new ObjectID();
            rec.name = originalFileName;
            rec.uploadfilepath = path;
            rec.type = req.file.mimetype;
            rec.size = req.file.size;
            rec.lastModifiedDate = req.file.lastModifiedDate != undefined ? req.file.lastModifiedDate : date;            
            rec.lastModified =  req.file.lastModifiedDate != undefined ? req.file.lastModifiedDate.milliseconds : date.milliseconds;
            rec.mail = req.body.email;
            rec.nodeid = parseInt(req.body.nodeid);
            console.log(rec);
            insertRecord("files",rec,function(result){
                if(result){                    
                    res.json(200, { "ok": true, path: path });
                }else{
                    fs.unlinkSync(path);
                    res.json(500, {"error":"internal error."});        
                }
            });            
        }else{
            fs.unlinkSync(path);            
            res.json(500, {"error":"login is fail"});
        }
    });
   }else{
       res.json(500, {"error":"login is fail"});
   }
    
});

