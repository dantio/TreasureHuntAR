var TreasureHuntAR = {
    treasuresInfo: null,

    hunting: function() {
        var distanceFactor = 580.2;

        /* null means: use relative to user, sun is NORTH to the user */
        var locationTreasure = new AR.RelativeLocation(null, 25000, 0, 5000);

        /* sizes & distances are far away from real values! used these scalings to be able to show within user range */
        var sizeFactor = 0.5;
        var sizeEarth = 12.8 * 25;

        /* every object in space has a name, location and a circle (drawable) */
        var treasureImg = new AR.ImageResource("img/treasure.png");
        var indicatorImg = new AR.ImageResource("img/indi.png");

        var treasureSize = (((109 * sizeEarth) / sizeEarth) * 0.3) * sizeFactor;

        var treasure = {
            name: "First Treasure",
            distance: 0,
            location: locationTreasure,
            imgDrawable: new AR.ImageDrawable(treasureImg, treasureSize),
            size: treasureSize,
            description: "My first Treasure",
            mass: "2&nbsp;10<sup>30</sup>&nbsp;kg",
            diameter: "1,392,684&nbsp;km"
        };

        // put sun, planets (and pluto) in an array
        this.treasuresInfo = [treasure];

        // create helper array to create goeObjects out of given information
        var treasuresGeoObjects = [];
        for (var i = 0; i < this.treasuresInfo.length; i++) {

            /* show name of object below*/
            var label = new AR.Label(this.treasuresInfo[i].name, 3, {
                offsetY: -this.treasuresInfo[i].size / 2,
                verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP,
                opacity: 0.9,
                zOrder: 1,
                style: {
                    textColor: '#FFFFFF',
                    backgroundColor: '#00000005'
                }
            });

            // drawable in cam of object -> image and label
            var drawables = [];
            drawables[0] = this.treasuresInfo[i].imgDrawable;
            drawables[1] = label;

            /* Create objects in AR*/
            treasuresGeoObjects[i] = new AR.GeoObject(this.treasuresInfo[i].location, {
                drawables: {
                    cam: drawables
                },
                onClick: this.treasureClicked(this.treasuresInfo[i])
            });
            // ??
            if (i > 0) {
                this.animate(this.treasuresInfo[i]);
            } else {
                var sunHackAnim = new AR.PropertyAnimation(this.treasuresInfo[i].location, 'northing', 10000, 10000, 1000, {
                    type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_SINE
                });
                sunHackAnim.start(-1);
            }
        }

        // Add indicator to sun
        var imageDrawable = new AR.ImageDrawable(indicatorImg, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });
        treasuresGeoObjects[0].drawables.addIndicatorDrawable(imageDrawable);
    },

    animate: function(treasure) {
        var relLocation = treasure.location;
        var distance = treasure.distance;
        var roundingTime = distance * 2 * 2;

        var northSouthAnimation1 = new AR.PropertyAnimation(relLocation, 'northing', distance * 1, distance * 0, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_SINE
        });
        var eastWestAnimation1 = new AR.PropertyAnimation(relLocation, 'easting', distance * 0, distance * 1, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_OUT_SINE
        });

        var northSouthAnimation2 = new AR.PropertyAnimation(relLocation, 'northing', distance * 0, distance * -1, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_OUT_SINE
        });
        var eastWestAnimation2 = new AR.PropertyAnimation(relLocation, 'easting', distance * 1, distance * 0, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_SINE
        });

        var northSouthAnimation3 = new AR.PropertyAnimation(relLocation, 'northing', distance * -1, distance * 0, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_SINE
        });
        var eastWestAnimation3 = new AR.PropertyAnimation(relLocation, 'easting', distance * 0, distance * -1, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_OUT_SINE
        });

        var northSouthAnimation4 = new AR.PropertyAnimation(relLocation, 'northing', distance * 0, distance * 1, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_OUT_SINE
        });
        var eastWestAnimation4 = new AR.PropertyAnimation(relLocation, 'easting', distance * -1, distance * 0, roundingTime / 4, {
            type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_SINE
        });

        var q1 = new AR.AnimationGroup(AR.CONST.ANIMATION_GROUP_TYPE.PARALLEL, [northSouthAnimation1, eastWestAnimation1]);
        var q2 = new AR.AnimationGroup(AR.CONST.ANIMATION_GROUP_TYPE.PARALLEL, [northSouthAnimation2, eastWestAnimation2]);
        var q3 = new AR.AnimationGroup(AR.CONST.ANIMATION_GROUP_TYPE.PARALLEL, [northSouthAnimation3, eastWestAnimation3]);
        var q4 = new AR.AnimationGroup(AR.CONST.ANIMATION_GROUP_TYPE.PARALLEL, [northSouthAnimation4, eastWestAnimation4]);

        var cicularAnimationGroup = new AR.AnimationGroup(AR.CONST.ANIMATION_GROUP_TYPE.SEQUENTIAL, [q4, q1, q2, q3]);

        cicularAnimationGroup.start(-1);
    },

    treasureClicked: function(planet) {
        return function() {
            document.getElementById("info").setAttribute("class", "info");
            document.getElementById("name").innerHTML = planet.name;
            document.getElementById("mass").innerHTML = planet.mass;
            document.getElementById("diameter").innerHTML = planet.diameter;
            document.getElementById("info").setAttribute("class", "infoVisible");
        };
    }
};