var app = require('express')();
var bodyParser = require('body-parser');
var multer = require('multer');
var pg = require('pg');
var async = require('async');
var fs = require('fs');
var TargetsAPI = require("./TargetsAPI.js");

var CONNECTION_STRING = "postgres://XXX:XXX@XXX.de/dbname";
var STATUS = {status: false};
var API_TOKEN = "XXX";
var API_VERSION = 2;

var API = new TargetsAPI(API_TOKEN, API_VERSION);

var IMAGE_URL = 'http://ericwuendisch.de/restnode/uploads/'; //remember the last /

app.use(bodyParser.json()); // for parsing application/json
app.use(bodyParser.urlencoded({ extended: true })); // for parsing application/x-www-form-urlencoded
app.use(multer()); // for parsing multipart/form-data


var server = app.listen(9999, function () {
    var host = server.address().address;
    var port = server.address().port;
    console.log('Server is listening at http://%s:%s', host, port)
});

var computeTargetImage = function (id, picture, callback) {
    var IMAGE = [IMAGE_URL + picture];
    API.convert(IMAGE, function (err, url) {
        if (err) {
            console.log("Error: " + err);
        } else {
            query('UPDATE cache SET target = "' + url + '" WHERE id = ' + id, null, function (result) {
                if (result.length == 0) {
                    callback(false);
                } else {
                    callback(true);
                }
            });
        }
    });
};

app.post('/cache', function (req, res) {
    fs.readFile(req.files.thumbnail.path, function (err, data) {
        if (err) {
            res.send(status);
        } else {
            var newPath = __dirname + "/uploads/" + req.files.thumbnail.originalname;
            fs.writeFile(newPath, data, function (err) {

                if (err) {
                    deleteFile(newPath);
                    res.send(STATUS);
                } else {

                    var description = req.body.description.toString();
                    var picture = req.files.thumbnail.originalname.toString();
                    var latitude = req.body.latitude.replace(',', '.');
                    var longitude = req.body.longitude.replace(',', '.');
                    var altitude = req.body.altitude.replace(',', '.');

                    query('INSERT INTO cache (description, picture, latitude, longitude, altitude) VALUES (\'' + description + '\',\'' + picture + '\',' + latitude + ',' + longitude + ',' + altitude + ') returning id', newPath, function (result) {
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
    query('SELECT * FROM cache WHERE id = ' + req.params.id, null, function (result) {
        if (result.length == 0) {
            res.send(STATUS);
        } else {
            res.send(result);
        }
    });
});

app.get('/caches', function (req, res) {
    query('SELECT * FROM cache', null, function (result) {
        if (result.length == 0) {
            res.send(STATUS);
        } else {
            res.send(result);
        }
    });
});


var deleteFile = function (filename) {
    if (filename != null) {
        fs.unlinkSync(filename);
    }
};

var query = function (query, filename, callback) {
    async.series({
            result: function (callback) {
                pg.connect(CONNECTION_STRING, function (err, client, done) {
                    if (err) {
                        deleteFile(filename);
                        return console.error('error fetching client from pool', err);
                    }
                    client.query('BEGIN', function (err, result) {
                        client.query(query, function (err, result) {
                            done();
                            if (err) {
                                client.query('ROLLBACK', function () {
                                    done();
                                    deleteFile(filename);
                                    return console.error('error running query', err);
                                });
                            } else {
                                client.query('COMMIT');
                                callback(null, result);
                            }
                        });
                    });
                });
            }
        },
        function (err, results) {
            console.log(results.result.rows);
            callback(results.result.rows);
        });
};

