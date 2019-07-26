/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Supports reading GeoPackage Vector layer features from a file with ".geopkg" extension
 *
 * @author Robert Ward - SciSys
 */
public class GeoPackageVectorFormat extends VectorFormat {

    static Logger LOG = Logging.getLogger(GeoPackageVectorFormat.class);

    private static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    @Override
    public FeatureReader read(ImportData data, ImportTask item) throws IOException {

        if ((data != null) && (item != null)) {
            File sourceFile = ((SpatialFile) data).allFiles().get(0);
            GeoPackage geoPkg = new GeoPackage(sourceFile, null, null, true);

            return geoPkg.reader(geoPkg.feature(item.getOriginalLayerName()), Filter.INCLUDE, null);
        }

        return null;
    }

    @Override
    public void dispose(FeatureReader reader, ImportTask item) throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask item) throws IOException {
        // we don't have a fast way to get the count
        // instead of parsing through the entire file
        return -1;
    }

    @Override
    public String getName() {
        return "GeoPackage Vector";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        if (data instanceof FileData) {
            return ((FileData) data).getFile().getAbsolutePath().endsWith("gpkg");
        }
        return false;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // direct import not supported
        return null;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        List<ImportTask> taskList = new ArrayList<ImportTask>();

        if ((data != null) && (catalog != null)) {

            CatalogFactory factory = catalog.getFactory();
            CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);

            File f = ((SpatialFile) data).allFiles().get(0);

            GeoPackage geoPkg = new GeoPackage(f, null, null, false);

            for (FeatureEntry entry : geoPkg.features()) {
                // get the composite feature type
                SimpleFeatureType featureType =
                        geoPkg.reader(geoPkg.feature(entry.getTableName()), Filter.INCLUDE, null)
                                .getFeatureType();
                LOG.log(Level.FINE, featureType.toString());

                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(featureType);
                tb.setName(entry.getTableName());
                featureType = tb.buildFeatureType();

                // create the feature type
                FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
                ft.setName(entry.getTableName());
                ft.setNativeName(ft.getName());

                List<AttributeTypeInfo> attributes = ft.getAttributes();
                for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
                    AttributeTypeInfo att = factory.createAttribute();
                    att.setName(ad.getLocalName());
                    att.setBinding(ad.getType().getBinding());
                    attributes.add(att);
                }

                // crs
                CoordinateReferenceSystem crs = null;
                if (featureType != null && featureType.getCoordinateReferenceSystem() != null) {
                    crs = featureType.getCoordinateReferenceSystem();
                }
                try {
                    crs = crs != null ? crs : CRS.decode("EPSG:4326");
                } catch (Exception e) {
                    throw new IOException(e);
                }

                ft.setNativeCRS(crs);

                String srs = String.format("EPSG:%d", entry.getSrid());

                ft.setSRS(srs);

                // bounds
                ft.setNativeBoundingBox(EMPTY_BOUNDS);
                ft.setLatLonBoundingBox(EMPTY_BOUNDS);
                ft.getMetadata().put("recalculate-bounds", Boolean.TRUE);

                LayerInfo layer = catalogBuilder.buildLayer((ResourceInfo) ft);

                ImportTask task = new ImportTask(data);
                task.setLayer(layer);

                task.getMetadata().put(FeatureType.class, featureType);

                taskList.add(task);
            }
        }
        return taskList;
    }
}
