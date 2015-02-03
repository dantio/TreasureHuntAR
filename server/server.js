var express = require('express'), // REST-App
    app = express(),
    bodyParser = require('body-parser'),
    multer = require('multer'),
    fs = require('fs'),
    TargetsAPI = require("./TargetsAPI.js"),
    sqlite3 = require('sqlite3').verbose(),
    db = new  sqlite3.Database('treasureHuntAR.db');

var API_TOKEN = "8baa3a2cac6df74b3a0154a062b8b1e5",
    API_VERSION = 2,
    LIMIT = 1000000000;

var API = new TargetsAPI(API_TOKEN, API_VERSION);
var IMAGE_URL = 'http://ericwuendisch.de/restnode/server/uploads/'; //remember the last /

/*db.serialize(function () {
    db.run("CREATE TABLE cache ("
    + "id INTEGER PRIMARY KEY,"
    + "description text NOT NULL,"
    + "picture character varying(512) NOT NULL,"
    + "latitude double precision NOT NULL,"
    + "longitude double precision NOT NULL,"
    + "altitude double precision NOT NULL,"
    + "target text NULL);");
});*/

app.use(bodyParser.json({limit: LIMIT})); // for parsing application/json
app.use(bodyParser.urlencoded({extended: true, limit: LIMIT})); // for parsing application/x-www-form-urlencoded
app.use(multer({limit: LIMIT, size: LIMIT})); // for parsing multipart/form-data
var host = null;
var port = null;

var server = app.listen(9999, function () {
    host = server.address().address;
    port = server.address().port;
    console.log('Server is listening at http://%s:%s', host, port)
});
var computeTargetImage = function (id, picture, callback) {
    var IMAGE = IMAGE_URL + "" +picture;
    console.log(IMAGE);
    API.convert(IMAGE, function (err, url) {
        if (err) {
            console.log("Error: " + err);
            callback(false);
        } else {
            var q = db.prepare('UPDATE cache SET target = "' + url + '" WHERE id = ' + id);
            q.run(function(err){
                if (err) {
                    callback(false);
                } else {
                    callback(true);
                }
            });
        }
    });
};

var hash = function(len)
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < len; i++ ){
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
};

app.post('/cache64', function (req, res) {

            var picture = hash(32).toString();
            var newPath = __dirname + "/uploads/" +picture+".jpg";
            fs.writeFile(newPath, new Buffer(req.body.file, "base64"), function(err) {

                if (err) {
                    fs.unlinkSync(newPath);
                    //res.send(404).send("Nope");
                } else {

                    var description = req.body.description.toString();

                    var latitude = req.body.latitude.replace(',', '.');
                    var longitude = req.body.longitude.replace(',', '.');
                    var altitude = req.body.altitude.replace(',', '.');

                    var q = db.prepare('INSERT INTO cache (description, picture, latitude, longitude, altitude) VALUES ("' + description + '","' + picture + '.jpg",' + latitude + ',' + longitude + ',' + altitude + ')');
                        q.run(function(err){
                            if (err) throw err;
                            computeTargetImage(this.lastID, picture+".jpg", function (state) {
                                if(state){
                                    //res.send(200).send("Cool");
                                    res.send("ok");
                                }else{
                                    res.send("nope");
                                    //res.send(404).send("Nope");
                                }
                            });
                        });
                }
    });
});


app.post('/cache', function (req, res) {

    fs.readFile(req.files.thumbnail.path, function (err, data) {
        if (err) {
            //res.send(404).send("Nope");
        } else {
            var newPath = __dirname + "/uploads/" + req.files.thumbnail.originalname;
            fs.writeFile(newPath, data, function (err) {

                if (err) {
                    fs.unlinkSync(newPath);
                    //res.send(404).send("Nope");
                } else {

                    var description = req.body.description.toString();
                    var picture = req.files.thumbnail.originalname.toString();
                    console.log(picture);
                    var latitude = req.body.latitude.replace(',', '.');
                    var longitude = req.body.longitude.replace(',', '.');
                    var altitude = req.body.altitude.replace(',', '.');

                    var q = db.prepare('INSERT INTO cache (description, picture, latitude, longitude, altitude) VALUES ("' + description + '","' + picture + '",' + latitude + ',' + longitude + ',' + altitude + ')');
                    q.run(function(err){
                        if (err) throw err;
                        computeTargetImage(this.lastID, picture, function (state) {
                            if(state){
                                //res.send(200).send("Cool");
                                res.send("ok");
                            }else{
                                res.send("nope");
                                //res.send(404).send("Nope");
                            }
                        });

                    });
                }
            });
        }
    });
});

app.get('/cache/:id', function (req, res) {
    db.all('SELECT * FROM cache WHERE id = ' + req.params.id, function(err, rows) {
        if (rows.length == 0) {
            res.send(404).send("Nope");
        } else {
            res.send(rows);
        }
    });
});

app.get('/area/:lat/:lon/:radius', function (req, res) {

    var latitude = req.params.lat.replace(',', '.');
    var longitude = req.params.lon.replace(',', '.');
    var radius = req.params.radius.replace(',', '.');

    var lat_min = latitude - radius;
    var lat_max = latitude + radius;
    var lon_min = longitude - radius;
    var lon_max = longitude + radius;

    db.all('SELECT * FROM cache WHERE latitude > '+lat_min+' AND latitude < '+lat_max+' AND longitude > '+lon_min+' AND longitude < '+lon_max, function(err, rows) {
        if (rows.length == 0) {
            res.send(404).send("Nope");
        } else {
            res.send(rows);
        }
    });
});


app.get('/caches', function (req, res) {
    db.all('SELECT * FROM cache', function(err, rows) {
        if (rows.length == 0) {
            res.send(404).send("Nope");
        } else {
            res.send(rows);
        }
    });
});
