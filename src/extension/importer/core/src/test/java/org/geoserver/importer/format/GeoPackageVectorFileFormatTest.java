/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.File;
import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.GridFormat;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.SpatialFile;
import org.geoserver.platform.GeoServerExtensionsHelper;

public class GeoPackageVectorFileFormatTest extends TestCase {

    private static boolean setUpIsDone = false;

    private GeoPackageVectorFormat geopackageVectorFormat;

    @Override
    protected void setUp() throws Exception {
        geopackageVectorFormat = new GeoPackageVectorFormat();

        if (!setUpIsDone) {
            setUpIsDone = true;
            GeoServerExtensionsHelper.singleton(
                    geopackageVectorFormat.getName(), geopackageVectorFormat, DataFormat.class);
        }
    }

    public void testFileExtensions() throws IOException {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/mixed_geopackage.gpkg");

        List<DataFormat> format = GeoPackageVectorFormat.lookup(file);

        assertNotNull(format);
        assertEquals(2, format.size());

        assertTrue(
                (format.get(0).getClass() == GeoPackageVectorFormat.class)
                        && (format.get(1).getClass() == GridFormat.class));
    }

    public void testListVectorLayers() throws IOException {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/mixed_geopackage.gpkg");

        SpatialFile data = new SpatialFile(file);

        assertTrue(geopackageVectorFormat.canRead(data));

        Catalog catalog = new LocalWorkspaceCatalog(new CatalogImpl());
        List<ImportTask> vectorLayerList = geopackageVectorFormat.list(data, catalog, null);
        assertEquals(2, vectorLayerList.size());

        for (ImportTask task : vectorLayerList) {
            assertTrue(task.getLayer().getName().startsWith("vector_"));
            int fc = geopackageVectorFormat.getFeatureCount(data, task);
            assertEquals(-1, fc);
        }
    }

    public void testIncorrectLayerEntry() throws IOException {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/mixed_geopackage.gpkg");

        SpatialFile data = new SpatialFile(file);

        Catalog catalog = new LocalWorkspaceCatalog(new CatalogImpl());
        List<ImportTask> vectorLayerList = geopackageVectorFormat.list(data, catalog, null);
        assertEquals(2, vectorLayerList.size());
        ImportTask task = vectorLayerList.get(0);
        task.setOriginalLayerName("Invalid layer name");
        int fc = geopackageVectorFormat.getFeatureCount(data, task);
        assertEquals(-1, fc);
    }

    public void testCreateStore() throws IOException {
        // Direct import not supported

        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/mixed_geopackage.gpkg");

        SpatialFile data = new SpatialFile(file);

        Catalog catalog = new LocalWorkspaceCatalog(new CatalogImpl());
        WorkspaceInfo workspace = new WorkspaceInfoImpl();
        workspace.setName("test workspace");

        assertNull(geopackageVectorFormat.createStore(data, workspace, catalog));
    }

    public void testListVectorOnlyLayers() throws IOException {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/vector_geopackage.gpkg");

        List<DataFormat> format = GeoPackageVectorFormat.lookup(file);

        assertNotNull(format);
        assertEquals(2, format.size());

        assertTrue(
                (format.get(0).getClass() == GeoPackageVectorFormat.class)
                        && (format.get(1).getClass() == GridFormat.class));

        SpatialFile data = new SpatialFile(file);

        assertTrue(geopackageVectorFormat.canRead(data));

        Catalog catalog = new LocalWorkspaceCatalog(new CatalogImpl());
        List<ImportTask> vectorLayerList = geopackageVectorFormat.list(data, catalog, null);
        assertEquals(2, vectorLayerList.size());

        for (ImportTask task : vectorLayerList) {
            assertTrue(task.getLayer().getName().startsWith("vector_"));
        }
    }

    public void testListRasterOnlyLayers() throws IOException {
        File file =
                new File(
                        "./src/test/resources/org/geoserver/importer/test-data/geopkg/raster_geopackage.gpkg");

        List<DataFormat> format = GeoPackageVectorFormat.lookup(file);

        assertNotNull(format);
        assertEquals(2, format.size());

        assertTrue(
                (format.get(0).getClass() == GeoPackageVectorFormat.class)
                        && (format.get(1).getClass() == GridFormat.class));

        SpatialFile data = new SpatialFile(file);

        assertTrue(geopackageVectorFormat.canRead(data));

        Catalog catalog = new LocalWorkspaceCatalog(new CatalogImpl());
        List<ImportTask> vectorLayerList = geopackageVectorFormat.list(data, catalog, null);
        assertEquals(0, vectorLayerList.size());
    }
}
