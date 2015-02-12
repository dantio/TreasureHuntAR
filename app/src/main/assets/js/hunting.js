var TreasureHuntAR = {
    currentLocation: null,
    loaded: false,
    magnifiers: {}, // { id, poiData, geoObject }
    magnifiersGeoObjects: [], // array of geoObject
    magnifierInVision: null,
    magnifierSize: 2,
    huntingMode: false,
    radius: 30, // 30m
    tracker: null,
    modelTreasure: null, // 3d treasur model

    onLocationChanged: function (latitude, longitude, altitude, accuracy) {
        TreasureHuntAR.currentLocation = new AR.GeoLocation(latitude, longitude, altitude);
        console.log("Set location");
    },
    /**
     * Called in HuntingActivity
     * @param poiDataServer data from Server
     */
    hunting: function (poiDataServer) {
        var poiData = poiDataServer || [];
        var image = new AR.ImageDrawable(new AR.ImageResource('img/magnifier.png'), this.magnifierSize);

        this.modelTreasure = new AR.Model("treasure.wt3", {
            scale: {
                x: 0.003,
                y: 0.003,
                z: 0.003
            },
            translate: {
                y: -0.05
            },
            rotate: {
                tilt: -45
            }
        });

        // create geo objects
        for (var i = 0; i < poiData.length; i++) {
            var location = new AR.GeoLocation(
                parseFloat(poiData[i].latitude),
                parseFloat(poiData[i].longitude),
                parseFloat(poiData[i].altitude));

            var label = new AR.Label("distance", 1, {
                offsetY: -this.magnifierSize / 4,
                verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP,
                opacity: 0.9,
                enabled: false,
                zOrder: 1,
                style: {
                    textColor: '#FFFFFF',
                    backgroundColor: '#00000005'
                }
            });


            var poi = this.magnifiers[poiData[i].id] = {};
            poi.poiData = poiData[i];
            poi.geoObject = new AR.GeoObject(
                location, {
                    drawables: {
                        cam: [image, label]
                    },
                    onEnterFieldOfVision: TreasureHuntAR.inVision(poi),
                    onExitFieldOfVision: TreasureHuntAR.exitVision(poi)
                });

            this.magnifiersGeoObjects.push(poi.geoObject);
        }

        //AR.radar.container = document.getElementById("radarContainer");
        //AR.radar.enabled = true;
    },

    restartHunting: function () {
        delete TreasureHuntAR.magnifierInVision;
        TreasureHuntAR.magnifierInVision = null;
    },

    /**
     * User tapped and want to hunt a treasure
     */
    startHuntingMagnifier: function () {
        // no magnifier
        if (this.huntingMode || TreasureHuntAR.magnifierInVision == null) {
            return;
        }

        // disable all magnifier
        for (var i = 0, l = TreasureHuntAR.magnifiersGeoObjects.length; i < l; i++) {
            TreasureHuntAR.magnifiersGeoObjects[i].enabled = false;
        }

        // TODO change image?
        // enable magnifier
        // we are hunting now!!
        TreasureHuntAR.magnifierInVision.geoObject.enabled = true;
        this.huntingMode = true;

        // Add indicator to our magnifier
        var indicatorImg = new AR.ImageResource("img/indi.png");
        var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });

        TreasureHuntAR.magnifierInVision.geoObject.drawables.addIndicatorDrawable(imageDrawable);
        document.location = "architectsdk://startHuntingMagnifier";

        console.log("set action area");
        var actionRange = new AR.ActionRange(TreasureHuntAR.magnifierInVision.geoObject.locations[0], TreasureHuntAR.radius, {
                onEnter: TreasureHuntAR.inActionRange,
                onExit: TreasureHuntAR.stopHuntingTreasure
            }
        );

        console.log(TreasureHuntAR.currentLocation);

        if (TreasureHuntAR.currentLocation != null && actionRange.isInArea(TreasureHuntAR.currentLocation)) {
            console.log("in Area");
            TreasureHuntAR.inActionRange();
        }
        console.log("check action area");
    },

    /**
     * User swiped down want to stop
     */
    stopHuntingMagnifier: function () {
        if (!TreasureHuntAR.huntingMode) {
            return;
        }

        // enable all magnifier
        for (var i = 0, l = TreasureHuntAR.magnifiersGeoObjects.length; i < l; i++) {
            TreasureHuntAR.magnifiersGeoObjects[i].enabled = true;
        }

        TreasureHuntAR.magnifierInVision.geoObject.drawables.removeIndicatorDrawable();
        TreasureHuntAR.huntingMode = false;
    },


    inVision: function (poi) {
        return function () {
            if (!TreasureHuntAR.huntingMode) {
                if (TreasureHuntAR.magnifierInVision != null &&
                    TreasureHuntAR.magnifierInVision.geoObject.enabled) {
                    // FIXME "Uncaught TypeError: Cannot read property '0' of undefined"
                    var userDistance1 = TreasureHuntAR.magnifierInVision.geoObject.locations[0].distanceToUser();
                    var userDistance2 = poi.geoObject.locations[0].distanceToUser();
                    if (userDistance2 < userDistance1) {
                        TreasureHuntAR.hideDetails(TreasureHuntAR.magnifierInVision);
                        TreasureHuntAR.showDetails(poi);
                        TreasureHuntAR.magnifierInVision = poi;
                    }
                } else {
                    TreasureHuntAR.showDetails(poi);
                    TreasureHuntAR.magnifierInVision = poi;
                }
            }
        };
    },

    exitVision: function (poi) {
        return function () {
            if (!TreasureHuntAR.huntingMode) {
                if (TreasureHuntAR.magnifierInVision != null && TreasureHuntAR.magnifierInVision == poi) {
                    TreasureHuntAR.magnifierInVision = null;
                    TreasureHuntAR.hideDetails(poi);
                }
            }
        };
    },

    showDetails: function (poi) {
        var label = poi.geoObject.drawables.cam[1];
        label.enabled = true;
        label.text = Math.round(poi.geoObject.locations[0].distanceToUser()) + " meter";
    },

    hideDetails: function (poi) {
        var label = poi.geoObject.drawables.cam[1];
        label.enabled = false;
    },

    /**
     * If user is in action range of magnifier, start hunting treasure
     */
    inActionRange: function () {
        console.log("in Action Range");
        // Disable all sensors in "IR-only" Worlds to save performance.
        AR.context.services.sensors = false;
        document.location = "architectsdk://startHuntingTreasure";
        // Hide magnifier
        TreasureHuntAR.magnifierInVision.geoObject.enabled = false;

        console.log("Tracker: " + TreasureHuntAR.magnifierInVision.poiData.target);
        TreasureHuntAR.tracker = new AR.Tracker(TreasureHuntAR.magnifierInVision.poiData.target, {
            onLoaded: function () {
                console.log("Loaded tracker");
            }
        });

        // Similar to 2D content the 3D model is added to the drawables.cam property of an AR.Trackable2DObject.
        // don't change '*' !
        var thisDoc = document;
        TreasureHuntAR.tracker.enabled = true;
        var trackable = new AR.Trackable2DObject(TreasureHuntAR.tracker, "*", {
            drawables: {
                cam: [TreasureHuntAR.modelTreasure]
            },
            onEnterFieldOfVision: function () {
                console.log("see treasure");
                thisDoc.location = "architectsdk://foundTreasure"; // ?id=" + TreasureHuntAR.magnifierInVision.poiData.id;
            }
        });

        console.log("Picture" + TreasureHuntAR.magnifierInVision.poiData.picture);

        document.getElementById('hintImage').src = TreasureHuntAR.magnifierInVision.poiData.picture;
        document.getElementById('hintImageWrapper').style.display = 'block';


    },

    stopHuntingTreasure: function () {
        // enable all magnifier
        AR.context.services.sensors = true;
        TreasureHuntAR.magnifierInVision.geoObject.enabled = true;
        TreasureHuntAR.tracker.enabled = false;
        document.getElementById('hintImageWrapper').style.display = 'none';
        TreasureHuntAR.huntingMode = false;
        TreasureHuntAR.stopHuntingMagnifier();
    },

    playAudio: function () {
        console.log("Play Audio: " + TreasureHuntAR.magnifierInVision.poiData.audio);

        if (!TreasureHuntAR.magnifierInVision.poiData.sound) {
            TreasureHuntAR.magnifierInVision.poiData.sound = new AR.Sound(TreasureHuntAR.magnifierInVision.poiData.audio, {
                onLoaded: function () {
                    console.log("audio play");
                    TreasureHuntAR.magnifierInVision.poiData.sound.play();
                },
                onError: function () {
                    console.log("audio error");
                }
            });

            TreasureHuntAR.magnifierInVision.poiData.sound.load()
        }

        TreasureHuntAR.magnifierInVision.poiData.sound.play();
    }
};

AR.context.onLocationChanged = TreasureHuntAR.onLocationChanged;