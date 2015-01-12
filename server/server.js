var express = require('express'), // REST-App
    app = express(),
    bodyParser = require('body-parser'),
    multer = require('multer'),
    async = require('async'),
    fs = require('fs'),
    TargetsAPI = require("./TargetsAPI.js"),
    sqlite3 = require('sqlite3').verbose(),
    db = new sqlite3.Database('treasureHuntAR.db');

var STATUS = {status: false},
    API_TOKEN = "XXX",
    API_VERSION = 2;

var API = new TargetsAPI(API_TOKEN, API_VERSION);
var IMAGE_URL = 'http://ericwuendisch.de/restnode/uploads/'; //remember the last /

db.serialize(function () {
    db.run("CREATE TABLE cache ("
    + "id INTEGER PRIMARY KEY,"
    + "description text NOT NULL,"
    + "picture character varying(512) NOT NULL,"
    + "latitude double precision NOT NULL,"
    + "longitude double precision NOT NULL,"
    + "altitude double precision NOT NULL,"
    + "target text NOT NULL);");
});

app.use(bodyParser.json()); // for parsing application/json
app.use(bodyParser.urlencoded({extended: true})); // for parsing application/x-www-form-urlencoded
app.use(multer()); // for parsing multipart/form-data


var server = app.listen(9999, function () {
    var host = server.address().address;
    var port = server.address().port;
    console.log('Server is listening at http://%s:%s', host, port)
});

app.post('/cache', function (req, res) {
    var computeTargetImage = function (id, picture, callback) {
        var IMAGE = [IMAGE_URL + picture];
        API.convert(IMAGE, function (err, url) {
            if (err) {
                console.log("Error: " + err);
            } else {
                db.run('UPDATE cache SET target = "' + url + '" WHERE id = ' + id, null, function (result) {
                    if (result.length == 0) {
                        callback(false);
                    } else {
                        callback(true);
                    }
                });
            }
        });
    };

    fs.readFile(req.files.thumbnail.path, function (err, data) {
        if (err) {
            res.send(status);
        } else {
            var newPath = __dirname + "/uploads/" + req.files.thumbnail.originalname;
            fs.writeFile(newPath, data, function (err) {

                if (err) {
                    // delete file
                    if (filename != null) {
                        fs.unlinkSync(filename);
                    }
                    res.send(STATUS);
                } else {

                    var description = req.body.description.toString();
                    var picture = req.files.thumbnail.originalname.toString();
                    var latitude = req.body.latitude.replace(',', '.');
                    var longitude = req.body.longitude.replace(',', '.');
                    var altitude = req.body.altitude.replace(',', '.');

                    db.run('INSERT INTO cache (description, picture, latitude, longitude, altitude) VALUES (\'' + description + '\',\'' + picture + '\',' + latitude + ',' + longitude + ',' + altitude + ') returning id', newPath, function (result) {
                        computeTargetImage(result.id, picture, function (state) {
                            res.send({status: state});
                        });
                    });
                }
            });
        }
    });
});

app.get('/cache/:id', function (req, res) {
    db.get('SELECT * FROM cache WHERE id = ' + req.params.id, null, function (result) {
        if (result.length == 0) {
            res.send(STATUS);
        } else {
            res.send(result);
        }
    });
});

app.get('/caches', function (req, res) {
    db.get('SELECT * FROM cache', null, function (result) {
        if (result.length == 0) {
            res.send(STATUS);
        } else {
            res.send(result);
        }
    });
});
