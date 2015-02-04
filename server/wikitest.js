var express = require('express'), // REST-App
    app = express(),
    bodyParser = require('body-parser'),
    fs = require('fs'),
    TargetsAPI = require("./TargetsAPI.js");

var API_TOKEN = "e166a98596adb76ca46da0a1060bf3ca",
    API_VERSION = 2;

var API = new TargetsAPI(API_TOKEN, API_VERSION);

var computeTargetImage = function (id, picture, callback) {
    var IMAGE = [picture];
    API.convert(IMAGE, function (err, url) {
        if (err) {
            callback(err);
        } else {
           callback(url);
        }
    });
};

computeTargetImage(99, "http://cdn.akamai.steamstatic.com/steam/apps/271590/capsule_616x353.jpg", function (state) {
    console.log(state);
});