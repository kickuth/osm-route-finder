// POI colours
const poiColors = ['#f00', '#0f0', '#00f', '#ff0', '#f0f', '#0ff', '#000', '#fff', '#f70'];

const icon205 = L.icon({  // Vorfahrt gewähren
    iconUrl: 'img/signs/DE205.png',
    iconSize: [12, 11], // size of the icon
    iconAnchor: [6, 5], // point of the icon which will correspond to marker's location
});

const icon206 = L.icon({  // Stop
    iconUrl: 'img/signs/DE206.png',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
});

const icon250 = L.icon({  // Verbot für Fahrzeuge aller Art
    iconUrl: 'img/signs/DE250.png',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
});

const icon274 = L.icon({  // Max speed
    iconUrl: 'img/signs/DE274.png',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
});

const icon357 = L.icon({  // Sackgasse
    iconUrl: 'img/signs/DE357.png',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
});

const icon449 = L.icon({  // Vorwegweiser auf Autobahnen
    iconUrl: 'img/signs/DE449.png',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
});

const iconcity = L.icon({  // Ortstafel / 310
    iconUrl: 'img/signs/city_limit.png',
    iconSize: [12, 8],
    iconAnchor: [6, 4],
});

// letter icons
const letterIcons = new Array(26);
for (let i=0; i < 26; i++){
    letterIcons[i] = L.icon({
        iconUrl: 'img/letters/' + String.fromCharCode(i+97) + '.png',  // a = 97
        iconSize: [16, 16],
        iconAnchor: [8, 8],
    });
}


function getMarker(name, latlon) {
    let icon;
    switch(name) {
        case "city_limit":
            icon = iconcity;
            break;
        case "DE:205":
            icon = icon205;
            break;
        case "DE:206":
            icon = icon206;
            break;
        case "DE:250":
            icon = icon250;
            break;
        case "DE:274":
            icon = icon274;
            break;
        case "DE:357":
            icon = icon357;
            break;
        case "DE:449":
            icon = icon449;
            break;
        default:
            if (name.length === 1) {
                icon = letterIcons[name.charCodeAt(0)-65];  // A = 65
                break;
            }
            return L.circleMarker(latlon, {
                radius: 6,
                fillColor: poiColors[name.charCodeAt(name.length - 1) % poiColors.length],
                color: '#000',
                weight: 1,
                opacity: 0.5,
                fillOpacity: 0.7
            });
    }
    return L.marker(latlon, {icon: icon});
}

function isCommonName(name) {  // TODO
    return (name === "city_limit" || name === "DE:205" || name === "DE:206" || name === "DE:274" || name.length === 1);
}

// popup for computed paths
function pathPopup(layer) {
    const res = layer.feature.geometry;
    return ('score: ' + res.score + ' (' + res.uBound + ' upper bound), length: ' + res.length
        + '<br>TODO common classes here.');  // TODO
}

// popup for POI markers
function poiPopup(feature) {
    const poiName = feature.properties.name;
    const poiID = feature.properties.id;
    return (
        poiName + " (id " + poiID + ")<br>" +
        "<input type='button' value='set as source' onclick='document.getElementById(\x22source\x22).value=" + poiID +"'><br>" +
        "<input type='button' value='set as sink' onclick='document.getElementById(\x22sink\x22).value=" + poiID + "'>"
    );
}

function drawEllipse(feature, event) {
    const latlngs = feature.coordinates;
    const lat1 = latlngs[0][1];
    const lng1 = latlngs[0][0];
    const lat2 = latlngs[latlngs.length - 1][1];
    const lng2 = latlngs[latlngs.length - 1][0];

    // compute ellipse center and rotation ## TODO calc center correctly for lat/lon point pairs
    const center = [(lat1 + lat2) / 2, (lng1 + lng2) / 2];
    const angle = getAngle([lat1, lng1], [lat2, lng2]);

    // compute ellipse width and height
    const minLength = feature.shortestpathdist;
    const maxLength = minLength * document.getElementById("max_dist_factor").value;  // (= ellipseHeight)
    const ellipseWidth = Math.sqrt(Math.pow(maxLength, 2) - Math.pow(minLength, 2)) / 2;

    const pathColor = event.target.options.color;

    L.ellipse(center, [ellipseWidth / 2, maxLength / 2], angle, {
        color: pathColor,
        fillColor: pathColor,
        fillOpacity: 0.0625,
        interactive: false
    }).addTo(map);
}

// as computed in http://makinacorpus.github.io/Leaflet.GeometryUtil/
function getAngle(latlng1, latlng2) {
    const rad = Math.PI / 180,
        lat1 = latlng1[0] * rad,
        lat2 = latlng2[0] * rad,
        lon1 = latlng1[1] * rad,
        lon2 = latlng2[1] * rad,
        y = Math.sin(lon2 - lon1) * Math.cos(lat2),
        x = Math.cos(lat1) * Math.sin(lat2) -
            Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
    const bearing = ((Math.atan2(y, x) * 180 / Math.PI) + 360) % 360;
    return bearing >= 180 ? bearing-360 : bearing;
}
