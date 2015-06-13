package de.komoot.photon.pbf;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;

import com.graphhopper.reader.OSMWay;
import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;

import de.komoot.photon.PhotonDoc;

@Getter
@ToString
@EqualsAndHashCode
class Entry {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

	private final long osmId;
	private final CountryCode country;
	private final String postalcode;
	private final String city;
	private final String street;
	private final String houseNumber;
	private final boolean context;
	private final Collection<Coordinate> coords = new HashSet<>();

	Entry(OSMWay w) {
		osmId = w.getId();

		if (w.hasTag("addr:country")) {
			country = CountryCode.valueOf(w.getTag("addr:country"));
		} else {
			country = null;
		}

		postalcode = StringUtils.trimToNull(w.getTag("addr:postcode"));
		city = StringUtils.trimToNull(w.getTag("addr:city"));
		street = StringUtils.trimToNull(w.getTag("addr:street"));
		houseNumber = StringUtils.trimToNull(w.getTag("addr:housenumber"));
		
		boolean isContext = w.hasTag("boundary");
		isContext |= (isContext ? true : w.hasTag("landuse"));
		isContext |= (isContext ? true : w.hasTag("place"));
		context = isContext;
	}

	void addCoord(double lon, double lat) {
		coords.add(new Coordinate(lon, lat));
	}

	boolean isCandidate() {
		return StringUtils.isNotBlank(postalcode) && StringUtils.isNotBlank(city) && StringUtils.isNotBlank(street)
				&& StringUtils.isNotBlank(houseNumber) && context;
	}

	PhotonDoc toPhotonDoc() {

		MultiPoint coords = getCoords();

		Map<String, String> name = Collections.singletonMap("name", city);
		String osmType = "W";
		String tagKey = null;
		String tagValue = null;
		Envelope bbox = coords.getEnvelopeInternal();
		Map<String, String> extratags = null;
		long parentPlaceId = 0;
		double importance = 1;
		long linkedPlaceId = 0;
		int rankSearch = 0;

		PhotonDoc doc = new PhotonDoc(osmId, osmType, osmId, tagKey, tagValue, name, houseNumber, extratags, bbox,
				parentPlaceId, importance, country, coords.getCentroid(), linkedPlaceId, rankSearch);

		doc.setPostcode(postalcode);
		doc.setCity(Collections.singletonMap("name:de", city));
		doc.setStreet(Collections.singletonMap("name:de", street));

		return doc;
	}

	private MultiPoint getCoords() {
		return GEOMETRY_FACTORY.createMultiPoint(coords.toArray(new Coordinate[coords.size()]));
	}
}