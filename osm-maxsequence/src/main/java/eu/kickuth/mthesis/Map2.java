// Copyright 2018 Sebastian Kuerten
//
// This file is part of osm4j.
//
// osm4j is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// osm4j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with osm4j. If not, see <http://www.gnu.org/licenses/>.

package eu.kickuth.mthesis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.topobyte.osm4j.pbf.seq.PbfIterator;
import org.wololo.geojson.Feature;
import org.wololo.jts2geojson.GeoJSONWriter;

import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.vividsolutions.jts.geom.Geometry;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.geometry.RegionBuilder;
import de.topobyte.osm4j.geometry.RegionBuilderResult;

public class Map2
{

    public static void main(String[] args)
            throws IOException, EntityNotFoundException
    {
        OsmIterator iterator = readData();
        InMemoryMapDataSet data = MapDataSetLoader.read(iterator, false, false,
                true);

        TLongObjectMap<OsmWay> ways = data.getWays();

        OsmWay way = ways.valueCollection().iterator().next();

//        for (OsmWay way : ways.valueCollection()) {
//
//        }

        RegionBuilder rb = new RegionBuilder();
        RegionBuilderResult result = rb.build(way, data);
        Geometry polygon = result.getMultiPolygon();

        Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
        Map<String, Object> properties = new HashMap<>();
        for (String key : tags.keySet()) {
            properties.put(key, tags.get(key));
        }

        GeoJSONWriter writer = new GeoJSONWriter();
        org.wololo.geojson.Geometry g = writer.write(polygon);
        Feature feature = new Feature(g, properties);

        String json = feature.toString();

        BufferedWriter jsonWriter = new BufferedWriter(new FileWriter("/home/todd/Desktop/map.geojson"));
        jsonWriter.write(json);

        jsonWriter.close();
    }

    private static OsmIterator readData()
    {
        // Open dump file as stream
        InputStream input = null;
        try
        {
            input = ClassLoader.getSystemClassLoader().getResource("./osm_data/tue.osm.pbf").openStream();
        } catch (NullPointerException e)
        {
            System.out.println("Failed to read map dump!");
            System.exit(1);
        } catch (IOException e)
        {
            System.out.println("Failed to locate map dump!");
            System.exit(1);
        }

        // Return a reader for PBF data
        return new PbfIterator(input, true);
    }

}