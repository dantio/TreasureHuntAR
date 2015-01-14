var TreasureHuntAR = {
    magnifiersGeoObjects: {},
    magnifierInVision: null,

    /**
     * Called in HuntingActivity
     * @param poiData data from Server
     */
    hunting: function (poiDataServer) {
        var poiData = poiDataServer || [];

        // sizes & distances are far away from real values! used these scalings to be able to show within user range
        var magnifierSize = 2;

        // every object in space has a name, location and a circle (drawable)
        var indicatorImg = new AR.ImageResource("img/indi.png");


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

            var image = new AR.ImageDrawable(new AR.ImageResource(poiData[i].res), magnifierSize);

            var poi = this.magnifiersGeoObjects[poiData[i].id] = {};
            poi.poiData = poiData[i];
            poi.geoObject = new AR.GeoObject(
                location, {
                    drawables: {
                        cam: [image]
                    },
                    onEnterFieldOfVision: TreasureHuntAR.inVision(poi),
                    onExitFieldOfVision: TreasureHuntAR.exitVision(poi)
                });
        }
        // Add indicator to first Magnifier
        //var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
        //    verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        //});

        //this.magnifiersGeoObjects[0].drawables.addIndicatorDrawable(imageDrawable);
        //AR.radar.container = document.getElementById("radarContainer");
        //AR.radar.enabled = true;
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
    }
    ,

    exitVision: function (poi) {
        return function () {
            if (TreasureHuntAR.magnifierInVision != null && TreasureHuntAR.magnifierInVision == poi) {
                TreasureHuntAR.magnifierInVision = null;
                TreasureHuntAR.hideDetails();
            }
        };
    }
    ,

    showDetails: function (poi) {
        document.getElementById("name").innerHTML = poi.poiData.name;
        document.getElementById("info").setAttribute("class", "infoVisible");
        document.getElementById("distance").innerHTML = poi.geoObject.locations[0].distanceToUser();
    }
    ,

    hideDetails: function () {
        document.getElementById("info").setAttribute("class", "info");
    }
};