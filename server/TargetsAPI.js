/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
 *
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * @author Wikitude
 *
 */
var https = require('https');

// the API host
var API_HOST = "api.wikitude.com";
// the API path
var API_PATH = "/targets/wtc-url";
// Your API key
var apiToken = "zMouy3qO4qG5b5/BT1xREMi21mMEf5MuS1dAKivyliDEfTC8zyTNNTLua/Iin5Pm9MtvqzyoikCS1+DwGb1c3s4NDZ1/auvwMT/oiadv/1mq8pQNL30EBg3VA28beiaghZneOsuVwEppKpY1+JsILQZYLKVw+cwC94lvp4K81j9TYWx0ZWRfX0Hp94Iut/ZB6fhtR/IYiohoXlcvGncpnOULCnogtoa04ILZVFF73yE/H57LiFtv29irwOM19LvPWGdI1SgJxMhmJcLHJM4HZ2+670MLexVI65WNhYG4Hruc5GlROaRpADBSf10Y+5OKAr+MWIkzk8D+EPMZK54jzRfDGwxH6hRJaT4KMmtl1C4tPkY7ItZkhxzC56Qy3XEMwo4LJirMTrq+G2JDZC/DvHfKfr3qSHSgDXs4Mx1+wTsXBPo4hXKKxWnON9567EgBSkWF/v8hjaHKgxfhI3y2cRqVk/ljsMIz9gyLjneDTej232cop6yqSgxl0TW+P+BQTEiUqqqWsFW7f91T4sOpYyT9heo7wEHC6rE3K3ovnW0m3AlNCfGeeQjKx1vf484TZ96CRbnY2E0Herurhqn6IJ6s0UPBiU83PtVRrc7srcwa/ulhSr/NU12fmOaBawLDY+HaujDeQpFlm7R5VkNW4PB2WOdPrJTV+mC8afq5iFaScCdkwXE8oIjflVZtalQ9";
// The version of the API we will use
var apiVersion = 2;

/**
 * Takes an Array of Image URLs and converts them into the JSON object the Wikitude Cloud Targets API requires when
 * using a POST request.
 *
 * @param imageUrls
 *            The imageUrls that point to the targets.
 * @return A JSON String that the Wikitude Cloud Targets API requires.
 */
function buildPayLoad (images) {
    // the array of targets that we'll populate
    var targetsArray = [];
    // for each image in the array, create an object the API understands
    for (var i = 0; i < images.length; i++) {
        targetsArray.push({
            url : images[i]
        });
    }
    // return the wrapper
    return {
        targets : targetsArray
    };
}

/**
 * Send the POST request to the Wikitude Cloud Targets API.
 *
 * @param payload
 *            the JSON object that will be posted into the body
 * @param callback
 *            the callback triggered when the request completes or fails. Needs to comply with the common (err, data)
 *            signature.
 */
function sendHttpRequest (payload, callback) {

    // The configuration of the request
    var post_options = {
        // We mainly use HTTPS connections to encrypt the data that is sent across the net. The rejectUnauthorized
        // property set to false avoids that the HTTPS request fails when the certificate authority is not in the
        // certificate store. If you do not want to ignore unauthorized HTTPS connections, you need to add the HTTPS
        // certificate of the api.wikitude.com server to the certificate store and make it accessible in Node.js.
        // Otherwise, you need to use a http connection instead.
        rejectUnauthorized : false,
        hostname : API_HOST,
        path : API_PATH,
        method : 'POST',
        headers : {
            'Content-Type' : 'application/json',
            'X-Version' : apiVersion,
            'X-Api-Token' : apiToken
        }
    };

    // Create the request
    var request = https.request(post_options, function (res) {
        res.setEncoding('utf8');
        // check for the status of the response
        if (res.statusCode !== 200) {
            // call was unsuccessful, callback with the error
            callback("Error: Status code " + res.statusCode);
        } else {
            // when we receive data, we call the callback function with err as null, and the wtc URL
            res.on('data', function (responseBody) {
                callback(null, JSON.parse(responseBody).wtc.url);
            });
        }
    });

    // On error, we call the callback with the error parameter and the error data
    request.on('error', function (e) {
        callback(e.message, e);
    });

    // post the data
    request.end(JSON.stringify(payload));
}

/**
 * Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
 *
 * @param token
 *            The token to use when connecting to the endpoint
 * @param version
 *            The version of the API we will use
 */
module.exports = function (token, version) {

    // save the configured values
    apiToken = token;
    apiVersion = version;

    /**
     * @method convert
     * @description The only entry point that triggers the conversion pipeline.
     * @param images
     *            The array of URLs pointing to the images
     * @param callback
     *            the callback triggered when the request completes or fails. Needs to comply with the common (err,
     *            data) signature.
     */
    this.convert = function (images, callback) {
        // construct the payload
        var payload = buildPayLoad(images);
        // send the request
        sendHttpRequest(payload, callback);
    };
};