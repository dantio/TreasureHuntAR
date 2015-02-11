var express = require('express'), // REST-App
    app = express(),
    bodyParser = require('body-parser'),
    multer = require('multer'),
    fs = require('fs'),
    TargetsAPI = require("./TargetsAPI.js"),
    sqlite3 = require('sqlite3').verbose(),
    db = new sqlite3.Database('treasureHuntAR.db'),
    ace = require('audio-convert-export');

var API_TOKEN = "8baa3a2cac6df74b3a0154a062b8b1e5",
    API_VERSION = 2,
    LIMIT = 1000000000;

var API = new TargetsAPI(API_TOKEN, API_VERSION);
var IMAGE_URL = 'http://ericwuendisch.de/restnode/server/uploads/'; //remember the last /

db.serialize(function () {
    db.run("CREATE TABLE IF NOT EXISTS cache ("
    + "id INTEGER PRIMARY KEY,"
    + "description text NOT NULL,"
    + "picture character varying(512) NOT NULL,"
    + "audio character varying(512) NOT NULL,"
    + "latitude double precision NOT NULL,"
    + "longitude double precision NOT NULL,"
    + "altitude double precision NOT NULL,"
    + "target text NULL);");
});

app.use(bodyParser.json({limit: LIMIT})); // for parsing application/json
app.use(bodyParser.urlencoded({extended: true, limit: LIMIT})); // for parsing application/x-www-form-urlencoded
app.use(multer({
    limit: LIMIT, size: LIMIT, dest: './uploads/'
})); // for parsing multipart/form-data

var server = app.listen(9999, function () {
    var host = server.address().address;
    var port = server.address().port;
    console.log('Server is listening at http://%s:%s', host, port)
});
var computeTargetImage = function (id, picture, callback) {
    var IMAGE = IMAGE_URL + "" + picture;
    console.log(IMAGE);
    API.convert([IMAGE], function (err, url) {
        if (err) {
            console.log("Error: " + err);
            callback(false);
        } else {
            var q = db.prepare('UPDATE cache SET target = "' + url + '" WHERE id = ' + id);
            q.run(function (err) {
                if (err) {
                    callback(false);
                } else {
                    callback(true);
                }
            });
        }
    });
};

var convertAudio = function (name){
    ace.exec({
        inputDir: './uploads',
        inputFormat: '3gp',
        outputDir: './uploads',
        outputFormat: 'mp3',
        ingnoreFileNamesContain: [],
        createArtistAlbumDirs: false,
        copyWithoutMetadata: true,
        bitrate: '256k',
	overwrite: true,
        log: {
            inputFormat: false
        }
    });


    ace.on('done', function () {
        console.log("file " + name + " successfully converted to mp3");
        fs.unlinkSync('./uploads/'+ name);
    });
}

app.post('/cache', function (req, res) {
   // audio and image
   if(req.files.length < 2) {
	console.error("we need two files");
   	res.send(404);
	return;
   }

    var data = {
        $description: req.body.description.toString(),
        $picture: req.files.image.name.toString(),
        $audio: req.files.audio.name.toString().replace('.3gp', '.mp3'),
	    $latitude: req.body.latitude.replace(',', '.'),
        $longitude: req.body.longitude.replace(',', '.'),
        $altitude: req.body.altitude.replace(',', '.')
    };


    var q = db.prepare('INSERT INTO cache (description, picture, audio, latitude, longitude, altitude)'
    + ' VALUES ($description, $picture, $audio, $latitude, $longitude, $altitude)', data);

    q.run(function (err) {
        if (err) throw err;
        var lastId = this.lastID;
        computeTargetImage(lastId, data.$picture, function (state) {
            if (state) {
		convertAudio(req.files.audio.name.toString());
                res.send(200);
            } else {
                // Delte from DB
                db.run('DELETE FROM cache WHERE id = ?', lastId);
                // Remove from FS
                fs.unlinkSync('./uploads/' + req.files.audio.name.toString());
                fs.unlinkSync('./uploads/' + data.$picture);
                res.send(404);
            }
        });
    });

});

app.get('/cache/:id', function (req, res) {
    db.all('SELECT * FROM cache WHERE id = ' + req.params.id, function (err, rows) {
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

    db.all('SELECT * FROM cache WHERE latitude > ' + lat_min + ' AND latitude < ' + lat_max + ' AND longitude > ' + lon_min + ' AND longitude < ' + lon_max, function (err, rows) {
        if (rows.length == 0) {
            res.send(404).send("Nope");
        } else {
            res.send(rows);
        }
    });
});


app.get('/caches', function (req, res) {
    db.all('SELECT * FROM cache', function (err, rows) {
        if (rows.length == 0) {
            res.send(404).send("Nope");
        } else {
            res.send(rows);
        }
    });
});
