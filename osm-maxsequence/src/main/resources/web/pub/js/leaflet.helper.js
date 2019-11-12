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

function isCommonName(name) {
    return (name === "DE:205" || name === "DE:206" || name === "DE:274" || name === "city_limit");
}