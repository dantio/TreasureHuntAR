var TreasureHuntAR = {
    magnifiersGeoObjects: [],

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

            // show name of object below
            var label = new AR.Label(location.distanceToUser(), 1, {
                offsetY: -magnifierSize / 2,
                verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP,
                opacity: 0.9,
                zOrder: 1,
                style: {
                    textColor: '#FFFFFF',
                    backgroundColor: '#00000005'
                }
            });

            var image = new AR.ImageDrawable(new AR.ImageResource(poiData[i].res), magnifierSize);

            // Create objects in AR
            this.magnifiersGeoObjects[i] = new AR.GeoObject(
                location, {
                    drawables: {
                        cam: [image, label]
                    },

                    onClick: TreasureHuntAR.magnifierClicked(poiData[i])
                });
        }

        // Add indicator to first Magnifier
        var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });

        this.magnifiersGeoObjects[0].drawables.addIndicatorDrawable(imageDrawable);
        AR.radar.container = document.getElementById("radarContainer");
        AR.radar.enabled = true;
    },

    magnifierClicked: function (magnifier) {
        return function () {
            document.getElementById("name").innerHTML = magnifier.name;
            document.getElementById("info").setAttribute("class", "infoVisible");
        };
    }
};