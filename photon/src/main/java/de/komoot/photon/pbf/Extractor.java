package de.komoot.photon.pbf;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.graphhopper.reader.OSMElement;
import com.graphhopper.reader.OSMInputFile;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMWay;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.Importer;

@Slf4j
public class Extractor {

	public void doImport(File file, Importer importer) {
		importDocs(extract(file), importer);
	}

	Collection<PhotonDoc> extract(File file) {
		try {
			return toPhotonDocs(nodes(file, ways(file)));
		} catch (Exception ex) {
			throw new IllegalStateException("Error on parse file " + file, ex);
		}
	}

	void importDocs(Collection<PhotonDoc> docs, Importer importer) {

		int i = 0;
		int max=docs.size();

		for (PhotonDoc doc : docs) {
			importer.add(doc);

			if (i % 800 == 0)
				System.out.print("\n" + i + "/" + max + " ");
			else if (i % 10 == 0)
				System.out.print('.');

			i++;
		}
		System.out.println();
	}

	private static Multimap<Long, Entry> ways(File file) throws IOException, XMLStreamException {

		log.info("Extract ways of file " + file);

		Multimap<Long, Entry> ways = ArrayListMultimap.create();
		OSMInputFile i = new OSMInputFile(file).setWorkerThreads(4).open();
		boolean success = false;

		try {
			OSMElement e = null;
			while ((e = i.getNext()) != null) {
				if (e.getType() == OSMElement.WAY) {
					OSMWay w = (OSMWay) e;
					Entry entry = new Entry(w);

					if (!entry.isCandidate()) {
						continue;
					}

					System.out.println(w);

					for (long nodeRefId : w.getNodes().toArray()) {
						ways.put(nodeRefId, entry);
					}
				}
			}
			success = true;
		} finally {
			close(i, !success);
		}

		log.info("Extract ways finished");

		return ways;
	}

	private static Collection<Entry> nodes(File file, Multimap<Long, Entry> nodeRefsAndWays) throws IOException,
			XMLStreamException {

		log.info("Extract nodes of file " + file + " for " + nodeRefsAndWays.size() + " nodes");

		OSMInputFile i = new OSMInputFile(file).setWorkerThreads(4).open();
		boolean success = false;

		try {
			OSMElement e = null;
			while ((e = i.getNext()) != null) {
				if (e.getType() == OSMElement.NODE) {
					OSMNode n = (OSMNode) e;
					for (Entry entry : nodeRefsAndWays.get(n.getId())) {
						entry.addCoord(n.getLon(), n.getLat());
					}
				}
			}
			success = true;
		} finally {
			close(i, !success);
		}

		log.info("Extract nodes finished");

		return new HashSet<>(nodeRefsAndWays.values());
	}

	private static Collection<PhotonDoc> toPhotonDocs(Collection<Entry> entries) {
		final List<PhotonDoc> l = new ArrayList<>();

		for (Entry e : entries) {
			l.add(e.toPhotonDoc());
		}

		return l;
	}

	private static void close(Closeable closeable, boolean failure) throws IOException {
		if (failure) {
			IOUtils.closeQuietly(closeable);
		} else {
			closeable.close();
		}
	}
}
