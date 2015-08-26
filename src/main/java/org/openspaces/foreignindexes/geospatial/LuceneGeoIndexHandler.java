package org.openspaces.foreignindexes.geospatial;

import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.j_spaces.core.cache.foreignIndexes.*;
import com.j_spaces.core.geospatial.shapes.*;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.shape.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Created by yechielf
 * @since 11.0
 */
public class LuceneGeoIndexHandler extends ForeignIndexesHandler {
    private static final Logger logger = Logger.getLogger(LuceneGeoIndexHandler.class.getName());

    public final static Map<String, SpatialOperation> spatialOperationMap = new HashMap<String, SpatialOperation>();
    static{
        spatialOperationMap.put("WITHIN", SpatialOperation.IsWithin);
        spatialOperationMap.put("CONTAINS", SpatialOperation.Contains);
        spatialOperationMap.put("DISJOINT", SpatialOperation.IsDisjointTo);
        spatialOperationMap.put("INTERSECTS", SpatialOperation.Intersects);
    }

    private final String mainDirectory = System.getProperty("com.gs.foreignindex.lucene.work", System.getProperty("user.home") + "/lucenework/");
    static final String GSUID = "GSUID";
    static final String GSVERSION = "GSVERSION";

    static final int MAX_RESULTS = 10000;
    private final ConcurrentMap<Object, IIndexableServerEntry> _uidToEntry;

    private LuceneHolder luceneEntryHolder;


    private SpatialContext spatialContext = JtsSpatialContext.GEO;
    private int maxLevels = 11;//results in sub-meter precision for geohash
    private SpatialPrefixTree grid = new GeohashPrefixTree(spatialContext, maxLevels);

    public class LuceneHolder {
        private Directory _directory;
        private IndexWriter _indexWriter;

        public LuceneHolder(Directory _directory, IndexWriter _indexWriter) {
            this._directory = _directory;
            this._indexWriter = _indexWriter;
        }

        public IndexWriter getIndexWriter() {
            return _indexWriter;
        }

        public Directory getDirectory() {
            return _directory;
        }
    }


    public LuceneGeoIndexHandler() {
        _uidToEntry = new ConcurrentHashMap<Object, IIndexableServerEntry>();
    }

    private LuceneHolder createLuceneHolder(String path) throws IOException {
        MMapDirectory directory = new MMapDirectory(Paths.get(path));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig wc = new IndexWriterConfig(analyzer);
        wc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, wc);
        return new LuceneHolder(directory, iwriter);
    }

    @Override
    public void initialize(String className) throws Exception {
        super.initialize(className);

        luceneEntryHolder = createLuceneHolder(mainDirectory + "/" + className + "/entries");
        ForeignIndexesHandler.addHandler("geospatial", className, this);

    }


    @Override
    public void insertEntry(IIndexableServerEntry entry) throws Exception {
        _uidToEntry.put(entry.getUid(), entry);
        //construct a document and add all fixed  properties
        Document doc = new Document();
        Map<String, SpaceIndex> indexes = entry.getSpaceTypeDescriptor().getIndexes();
        for (String index : indexes.keySet()) {
            if (indexes.get(index).getIndexType().equals(SpaceIndexType.GEOSPATIAL)) {
                Object val = entry.getPropertyValue(index);
                if (val != null) {//???????????  does lucene handle nulls ?????????
                    if (val instanceof com.j_spaces.core.geospatial.shapes.Shape) {
                        com.j_spaces.core.geospatial.shapes.Shape gigaShape = (com.j_spaces.core.geospatial.shapes.Shape) val;
                        com.spatial4j.core.shape.Shape shape = toSpatial4j(gigaShape);
                        Field[] fields = createStrategyByFieldName(indexes.get(index).getName()).createIndexableFields(shape);
                        for (Field field : fields)
                            doc.add(field);
                    }
                }
            }
        }

        //cater for uid & version
        //noinspection deprecation
        doc.add(new Field(GSUID, (String) entry.getUid(), Field.Store.YES,
                Field.Index.NOT_ANALYZED));

        FieldType gsVersionFieldType = new FieldType();
        gsVersionFieldType.setStored(true);
        gsVersionFieldType.setIndexOptions(IndexOptions.NONE);
        gsVersionFieldType.setNumericType(FieldType.NumericType.INT);
        doc.add(new IntField(GSVERSION, entry.getVersion(), gsVersionFieldType));


        luceneEntryHolder.getIndexWriter().addDocument(doc);
        luceneEntryHolder.getIndexWriter().commit();

    }

    @Override
    public void removeEntry(IIndexableServerEntry entry) throws Exception {
        luceneEntryHolder.getIndexWriter().deleteDocuments(new Term(GSUID, (String) entry.getUid()));
        luceneEntryHolder.getIndexWriter().commit();
        _uidToEntry.remove( entry.getUid());
    }

    @Override
    public void replaceEntry(IIndexableServerEntry newEentry, IIndexableServerEntry originalEentry) {
        //TBD
    }


    @Override
    public void insertTemplate(IIndexableServerTemplate template) throws Exception {
    }

    @Override
    public void removeTemplate(IIndexableServerTemplate template) throws Exception {
    }

    @Override
    public ForeignQueryTemplatesResultIterator queryTemplates(String className, IIndexableServerEntry entry) throws Exception {
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void close() throws Exception {
        luceneEntryHolder.getIndexWriter().close();
    }


    public com.spatial4j.core.shape.Shape toSpatial4j(com.j_spaces.core.geospatial.shapes.Shape gigaShape) {
        if (gigaShape instanceof com.j_spaces.core.geospatial.shapes.Rectangle) {
            return convertRectangle((com.j_spaces.core.geospatial.shapes.Rectangle) gigaShape);
        } else if (gigaShape instanceof com.j_spaces.core.geospatial.shapes.Circle) {
            return convertCircle((com.j_spaces.core.geospatial.shapes.Circle) gigaShape);
        } else if (gigaShape instanceof com.j_spaces.core.geospatial.shapes.Point) {
            return convertPoint((com.j_spaces.core.geospatial.shapes.Point) gigaShape);
        } else if (gigaShape instanceof Polygon) {
            return convertPolygon((Polygon) gigaShape);
        } else {
            throw new RuntimeException("Unknown shape [" + gigaShape.getClass().getName() + "]");
        }
    }

    private com.spatial4j.core.shape.Shape convertCircle(com.j_spaces.core.geospatial.shapes.Circle circle) {
        return spatialContext.makeCircle(circle.getPoint().getX(), circle.getPoint().getY(), circle.getRadius());
    }

    private com.spatial4j.core.shape.Shape convertRectangle(com.j_spaces.core.geospatial.shapes.Rectangle rectangle) {
        return spatialContext.makeRectangle(rectangle.getMinX(), rectangle.getMaxX(),
                rectangle.getMinY(), rectangle.getMaxY());
    }

    private com.spatial4j.core.shape.Shape convertPoint(com.j_spaces.core.geospatial.shapes.Point point) {
        return spatialContext.makePoint(point.getX(), point.getY());
    }

    private com.spatial4j.core.shape.Shape convertPolygon(Polygon polygon) {
        try {
            String coordinates = "";
            for (int i=0; i<polygon.getCoordinates().size(); i++) {
                coordinates += (int)polygon.getCoordinates().get(i).getX() + " " + (int)polygon.getCoordinates().get(i).getY() +",";
            }
            coordinates += (int)polygon.getCoordinates().get(0).getX() + " " + (int)polygon.getCoordinates().get(0).getY();
            return spatialContext.readShapeFromWkt("POLYGON (("+coordinates+"))");
            //return poly;
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private SpatialStrategy createStrategyByFieldName(String fieldName) {
        return new RecursivePrefixTreeStrategy(grid, fieldName);
    }

    //@todo create param object
    public boolean applyOperationFilter(String relation, Object actual, Object matchedAgainst) {
        if (!(actual instanceof com.j_spaces.core.geospatial.shapes.Shape) || !(matchedAgainst instanceof com.j_spaces.core.geospatial.shapes.Shape)) {
            logger.warning("Relation " + relation + " can be applied only for geometrical shapes, instead given: " + actual + " and " + matchedAgainst);
            return false;
        } else {
            SpatialRelation spatialRelation = SpatialRelation.valueOf(relation.toUpperCase());
            if(spatialRelation == null){
                logger.warning("Relation " + relation + " not found, known relations are: " + Arrays.asList(SpatialRelation.values()));
                return false;
            }
            com.spatial4j.core.shape.Shape actualShape = toSpatial4j((com.j_spaces.core.geospatial.shapes.Shape) actual);
            com.spatial4j.core.shape.Shape matchedAgainstShape = toSpatial4j((com.j_spaces.core.geospatial.shapes.Shape) matchedAgainst);
            return actualShape.relate(matchedAgainstShape) == spatialRelation;
        }
    }

    //@todo create param object
    @Override
    public ForeignQueryEntriesResultIterator scanIndex(String typeName, String path, String namespace, String relation, Object subject) throws Exception {
        SpatialRelation spatialRelation = SpatialRelation.valueOf(relation.toUpperCase());
        if(spatialRelation == null){
            logger.warning("Relation " + relation + " not found, known relations for " + namespace + " are: " + Arrays.asList(SpatialRelation.values()));
            return null;
        }
        if (!(subject instanceof com.j_spaces.core.geospatial.shapes.Shape)) {
            logger.warning("Relation " + relation + " can be applied only for geometrical shapes, instead given: " + subject);
            return null;
        }
        com.spatial4j.core.shape.Shape subjectShape = toSpatial4j((com.j_spaces.core.geospatial.shapes.Shape) subject);
        SpatialArgs args = new SpatialArgs(spatialOperationMap.get(spatialRelation.name()), subjectShape);
        DirectoryReader dr = DirectoryReader.open(luceneEntryHolder.getDirectory());
        IndexSearcher is = new IndexSearcher(dr);
        Query q = createStrategyByFieldName(path).makeQuery(args);
        ScoreDoc[] scores = is.search(q, MAX_RESULTS).scoreDocs;
        return new LuceneIterator(scores, is, _uidToEntry, dr);
    }

}