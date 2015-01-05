var TreasureHuntAR = {
    treasuresGeoObjects: [],

    /**
     * Called in HuntingActivity
     * @param poiData data from Server
     */
    hunting: function (poiDataServer) {
        var poiData = poiDataServer || [];

        // sizes & distances are far away from real values! used these scalings to be able to show within user range
        var sizeFactor = 0.5;
        var sizeTreasure = 300;

        var treasureSize = (((109 * sizeTreasure) / sizeTreasure) * 0.3) * sizeFactor;

        // every object in space has a name, location and a circle (drawable)
        var treasureImg = new AR.ImageResource("img/treasure.png");
        var indicatorImg = new AR.ImageResource("img/indi.png");

        // Example treasure
        var treasureExample = {
            id: poiData.length,
            name: "My First Treasure",
            altitude: AR.CONST.UNKNOWN_ALTITUDE,
            location: new AR.RelativeLocation(null, 25000, 0, 5000),
            image: new AR.ImageDrawable(treasureImg, treasureSize),
            description: "My first Treasure"
        };

        // poiData.push(treasureExample);

        // create geo objects

        for (var i = 0; i < poiData.length; i++) {
            var location = new AR.GeoLocation(
                parseFloat(poiData[i].latitude),
                parseFloat(poiData[i].longitude),
                parseFloat(poiData[i].altitude) );

            // show name of object below
            var label = new AR.Label(poiData[i].name, 3, {
                offsetY: -treasureSize / 2,
                verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP,
                opacity: 0.9,
                zOrder: 1,
                style: {
                    textColor: '#FFFFFF',
                    backgroundColor: '#00000005'
                }
            });

            var image = new AR.ImageDrawable(new AR.ImageResource(poiData[i].res), treasureSize);

            // Create objects in AR

            this.treasuresGeoObjects[i] = new AR.GeoObject(
                location, {
                    drawables: {
                        cam: [image, label]
                    },

                    onClick: this.treasureClicked(poiData[i])
                });
        }

        // Add indicator to first treasure
        var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });

        this.treasuresGeoObjects[0].drawables.addIndicatorDrawable(imageDrawable);
        //AR.radar.container = document.getElementById("radarContainer");
    },

    treasureClicked: function (treasure) {
        return function () {
            document.getElementById("info").setAttribute("class", "info");
            document.getElementById("name").innerHTML = treasure.name;
            document.getElementById("info").setAttribute("class", "infoVisible");
        };
    }
};