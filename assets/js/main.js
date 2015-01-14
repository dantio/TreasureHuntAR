var TreasureHuntAR = {
    magnifiers: {}, // { id, poiData, geoObject }
    magnifiersGeoObjects: [], // array of geoObject
    magnifierInVision: null,
    magnifierSize: 2,

    /**
     * Called in HuntingActivity
     * @param poiData data from Server
     */
    hunting: function (poiDataServer) {
        var poiData = poiDataServer || [];

        // create geo objects
        for (var i = 0; i < poiData.length; i++) {
            var location = new AR.GeoLocation(
                parseFloat(poiData[i].latitude),
                parseFloat(poiData[i].longitude),
                parseFloat(poiData[i].altitude));

            //var actionRange2 = new AR.ActionRange(location1, 500, {
            //    onEnter : function() {
            //        actionRange2.enabled = false; //an ActionArea which can only be entered once
            //    }
            //});

            var image = new AR.ImageDrawable(new AR.ImageResource(poiData[i].res), this.magnifierSize);

            var poi = this.magnifiers[poiData[i].id] = {};
            poi.poiData = poiData[i];
            poi.geoObject = new AR.GeoObject(
                location, {
                    drawables: {
                        cam: [image]
                    },
                    onEnterFieldOfVision: TreasureHuntAR.inVision(poi),
                    onExitFieldOfVision: TreasureHuntAR.exitVision(poi)
                });

            this.magnifiersGeoObjects.push(poi.geoObject);
        }

        //AR.radar.container = document.getElementById("radarContainer");
        //AR.radar.enabled = true;
    },

    // User tapped and want to hunt a treasure
    startHunting: function () {
        // no magnifier
        if (this.magnifierInVision == null) {
            return;
        }

        // disable all magnifier
        for(var i = 0, l = this.magnifiersGeoObjects.length; i < l; i++) {
            this.this.magnifiersGeoObjects[i].enabled = false;
        }

        // TODO change image?
        // enable magnifier
        this.magnifierInVision.geoObject.enabled = true;

        // Add indicator to our magnifier
        var indicatorImg = new AR.ImageResource("img/indi.png");
        var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });
        this.magnifierInVision.geoObject.drawables.addIndicatorDrawable(imageDrawable);

    },

    inVision: function (poi) {
        return function () {
            if (TreasureHuntAR.magnifierInVision != null) {
                var userDistance1 = TreasureHuntAR.magnifierInVision.locations[0].distanceToUser();
                var userDistance2 = poi.geoObject.locations[0].distanceToUser();
                if (userDistance2 < userDistance1) {
                    TreasureHuntAR.showDetails(poi);
                    TreasureHuntAR.magnifierInVision = poi;
                }
            } else {
                TreasureHuntAR.showDetails(poi);
                TreasureHuntAR.magnifierInVision = poi;
            }
        };
    },

    exitVision: function (poi) {
        return function () {
            if (TreasureHuntAR.magnifierInVision != null && TreasureHuntAR.magnifierInVision == poi) {
                TreasureHuntAR.magnifierInVision = null;
                TreasureHuntAR.hideDetails();
            }
        };
    },

    showDetails: function (poi) {
        document.getElementById("name").innerHTML = poi.poiData.name;
        document.getElementById("info").setAttribute("class", "infoVisible");
        document.getElementById("distance").innerHTML = poi.geoObject.locations[0].distanceToUser();
    },

    hideDetails: function () {
        document.getElementById("info").setAttribute("class", "info");
    }
};