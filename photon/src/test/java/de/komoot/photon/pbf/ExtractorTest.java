package de.komoot.photon.pbf;

import java.io.File;

import org.junit.Test;

public class ExtractorTest {

	@Test
	public void test() {
		Extractor e = new Extractor();
		e.extract(new File("src/test/resources/berlin-latest.osm.pbf"));
	}

}
