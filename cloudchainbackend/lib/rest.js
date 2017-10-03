/**
 * Copyright 2017 cloudchain. All Rights Reserved.
 *
 * rest.js
 * cloudchain
 */

var MongoClient = require("mongodb").MongoClient,
  BSON = require("mongodb").BSONPure,
  ObjectID = require('mongodb').ObjectID,
  server = module.parent.exports.server,
  config = module.parent.exports.config,
  debug = module.parent.exports.debug,
  upload = module.parent.exports.upload,
  express = module.parent.exports.express,
  http = module.parent.exports.http,
  fs = module.parent.exports.fs,
  io = module.parent.exports.io,
  util = require("./util").util;

debug("rest.js is loaded");


//socket io begin

server.get('/', function(req, res){
  res.json(200, {"msg":"hello world"});
});

io.on('connection', function(socket){
  console.log('a user connected');
  socket.on('disconnect', function(){
    console.log('user disconnected');
  });

  socket.on('chat message', function(msg){
    console.log('message: ' + msg);
    io.emit('chat message', msg);
  });

  socket.on('joinroom', function(newroom){
		if(socket.room != undefined){
      socket.leave(socket.room);
    }    
    socket.join(newroom);		
    debug("a user join this room : ".concat(newroom));
	});
});
   

//socket io end


/// mongo functions begin

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

function updateRecordIncludeSet(coll, query, rec, callback){
  MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
    db.collection(coll, function (err, collection) {
      collection.updateMany(query,{$set: rec}, function (err, docs) {
        callback(!err);        
        db.close();
      });
    });//collection
  });//connect
}

function deleteAllRecords(coll, query, callback){
  MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
    db.collection(coll, function (err, collection) {      
      collection.deleteMany(query,function(err, result) {
        if (err) callback(null);
        db.close();
        return callback(result);
      });
    });//collection
  });//connect
}

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

function getCountOfRecords(coll, query, callback){
  MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
    db.collection(coll, function (err, collection) {      
      collection.count(query, function(err, result) {
        if (err) callback(null);
        db.close();
        return callback(result);
      });
    });//collection
  });//connect
}

function findAllRecords(coll, query, project,sort, callback){
  MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
    db.collection(coll, function (err, collection) {      
      collection.find(query, project).sort(sort).toArray(function(err, result) {
        if (err) callback(null);
        db.close();
        return callback(result);
      });
    });//collection
  });//connect
}

function findRecords(coll, query, project,sort,limit,skip, callback){
  MongoClient.connect(util.connectionURL(config.db.db, config), function (err, db) {
    db.collection(coll, function (err, collection) {
      if(err){
        debug(err.message);
      }
      collection.find(query, project).sort(sort).limit(limit).skip(skip).toArray(function(err, result) {        
        if (err) callback(null);
        db.close();
        return callback(result);
      });
    });//collection
  });//connect
}

/// mongo functions end

function checkUserExists(email, callback){
  var query = {"email":email};    
  findOneRecord("users", query, function(result){
    callback(result != null);
  });
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


server.get('/cloudchain/getNewID', function(req, res, next) {
  debug("/cloudchain/getNewID recieved");
  res.json({ "_id": new ObjectID().toString() });
});

server.get('/cloudchain/userexists/:email', function(req,res){
  debug("/cloudchain/userexists recieved");
  checkUserExists(req.params.email, function(result){
    res.json(200,{"ok" :result});
  });
});

server.get('/cloudchain/getuser/:email/:password', function(req,res){
  debug("/cloudchain/getuser recieved");
  var query = {"email":req.params.email, "password":req.params.password};
  checkUserLogin(query, function(result){
    if(result != null){
      res.json(200, result);
    }else{
      res.json(500, {"ok":false});
    }
  });
});

server.post('/cloudchain/getnode', function(req,res){
  debug("/cloudchain/getnode recieved");
  var user = req.body;
  checkUserLogin(user, function(loginResult){
    if(loginResult){
      var query = {"email":user.email};
      findAllRecords("nodes",query, {},{},function(result){
        if(result != null){
          res.json(200, result);
        }else{
          res.json(500, {"error":"record not found."});
        }
      });
    }else{
      res.json(500, {"error":"login is fail"});
    }
  });
});

server.post('/cloudchain/getfiles/:nodeid', function(req,res){
  debug("/cloudchain/getfiles recieved");
  var user = req.body;
  checkUserLogin(user, function(loginResult){
    if(loginResult){
      var query = {"mail":user.email, "nodeid":parseInt( req.params.nodeid)};
      findAllRecords("files",query, {},{},function(result,err){      
        if(result != null){
          res.json(200, result);
        }else{
          res.json(500, {"error":"record not found."});
        }
      });
    }else{
      res.json(500, {"error":"login is fail"});
    }
  });
});

server.post('/cloudchain/getfiles/:nodeid/:skip/:limit', function(req,res){
  debug("/cloudchain/getfiles skip and limit recieved");
  var user = req.body;
  checkUserLogin(user, function(loginResult){
    if(loginResult){
      var query = {"mail":user.email, "nodeid":parseInt(req.params.nodeid)};      
      findRecords("files",query, {}, {}, parseInt( req.params.limit),parseInt( req.params.skip), function(result,err){      
        if(result != null){
          debug(JSON.stringify(result));
          res.json(200, result);
        }else{
          res.json(500, {"error":"record not found."});
        }
      });
    }else{
      res.json(500, {"error":"login is fail"});
    }
  });
});

server.post('/cloudchain/getfilescount/:nodeid', function(req,res){
  debug("/cloudchain/getfilescount recieved");
  var user = req.body;
  checkUserLogin(user, function(loginResult){
    if(loginResult){
      var query = {"mail":user.email, "nodeid":parseInt( req.params.nodeid)};
      getCountOfRecords("files", query, function(result){
        if(result != null){
          res.json(200, {"count":result});
        }else{
          res.json(200, {"count":0});
        }
      });
    }else{
      res.json(500, {"error":"login is fail"});
    }
  });
});

server.post('/cloudchain/createuser',function(req,res){
  debug("/cloudchain/createuser recieved");
  var rec = req.body;
  rec._id = new ObjectID();
  if(rec.email && rec.password){
    checkUserExists(rec.email,function(result){
      if(result){
        res.json(500, {"error":"this email is using"});
      }else{
       insertRecord("users",rec, function(val){
        var recNode = {};
        recNode._id = new ObjectID();
        recNode.email = rec.email;
        recNode.nodes = [{"id":1,"name":"/", "type":"folder","children":[]}];
        insertRecord("nodes",recNode,function(resultt){
            if(resultt){
              res.json(200,{ "ok": val });
            }
          });        
       });
      }
    });
  }else{
    res.json(500, {"error":"email and password not found"});
  }   
});
 
server.post('/cloudchain/setnodes',function(req,res){
  debug("/cloudchain/setnodes recieved");
  var params =  req.body;    
   if(params.email != undefined && params.password != undefined){
    var user = {"email":params.email, "password":params.password};
    checkUserLogin(user,function(result){
      if(result == null){
        res.json(500, {"error":" login fail."});
      }else{
        var query = {"email":user.email};
        updateRecordIncludeSet("nodes",query, {"nodes":params.nodes} , function(result){
          io.to(params.email).emit('nodeupdate',"nodeupdate");
          res.json(200,{ "ok":result });
        });
      }
    });
  }else{
    res.json(500, {"error":" login fail."});
  }   
});

server.post('/cloudchain/deletenodefiles',function(req,res){
  debug("/cloudchain/deletenodefiles recieved");
  var params =  req.body;    
   if(params.email != undefined && params.password != undefined){
    var user = {"email":params.email, "password":params.password};
    checkUserLogin(user,function(result){
      if(result == null){
        res.json(500, {"error":" login fail."});
      }else{
        query = {"mail":user.email, "nodeid":parseInt(params.nodeid)};
        findAllRecords("files",query,{},{},function(results){
          if(results!= null){
            for(var i=0; i<results.length;i++){
              var file = results[i];
              try{
              fs.unlinkSync(file.uploadfilepath);            
              }catch(ex){}
            }            
          }                    
          debug(JSON.stringify(query));
          deleteAllRecords("files",query,function(result){            
            res.json(200,{"ok":true});
          });
        });
      }
    });
  }else{
    res.json(500, {"error":" login fail."});
  }   
});

server.post('/cloudchain/deletefile',function(req,res){
  debug("/cloudchain/deletefile recieved");
  var params =  req.body;    
   if(params.email != undefined && params.password != undefined){
    var user = {"email":params.email, "password":params.password};
    checkUserLogin(user,function(result){
      if(result == null){
        res.json(500, {"error":" login fail."});
      }else{
        query = {"mail":user.email, "_id": new ObjectID(params.file_id)};
        findOneRecord("files",query,function(result){
          if(result!=null){              
              fs.unlinkSync(result.uploadfilepath);
              deleteAllRecords("files",query, function(result){
                io.to(params.email).emit('updatefiles',"updatefiles");
                res.json(200,{"ok":result});
              });
            }else{
              res.json(500,{"error":"file not found"});
            }
        });
      }
    });
  }else{
    res.json(500, {"error":" login fail."});
  }   
});

server.post('/cloudchain/renamefile',function(req,res){
  debug("/cloudchain/renamefile recieved");
  var params =  req.body;    
   if(params.email != undefined && params.password != undefined){
    var user = {"email":params.email, "password":params.password};
    checkUserLogin(user,function(result){
      if(result == null){
        res.json(500, {"error":" login fail."});
      }else{
        query = {"mail":user.email, "_id": new ObjectID(params.file_id)};
        var rec = {"name":params.file_name};
        findOneRecord("files",query,function(result){
          if(result!=null){           
            updateRecordIncludeSet("files", query, rec, function(result){
              io.to(params.email).emit('updatefiles','updatefiles');
              res.json(200,{"ok":result});
            });
          }else{
            res.json(500,{"error":"file not found"});
          }
        });        
      }
    });
  }else{
    res.json(500, {"error":" login fail."});
  }   
});