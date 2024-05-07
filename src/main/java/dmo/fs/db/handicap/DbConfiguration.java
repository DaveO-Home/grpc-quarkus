package dmo.fs.db.handicap;

import dmo.fs.db.handicap.utils.DodexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DbConfiguration {
    static Logger logger = LoggerFactory.getLogger(DbConfiguration.class.getName());
    protected static final Map<String, String> map = new ConcurrentHashMap<>();
    protected static Properties properties = new Properties();

    protected static Boolean isUsingH2 = false;
    protected static String defaultDb = "h2";
    protected static final DodexUtil dodexUtil = new DodexUtil();
    protected static HandicapDatabase handicapDatabase;

    protected enum DbTypes {
        H2("h2");

        final String db;

        DbTypes(String db) {
            this.db = db;
        }
    };

    public static boolean isUsingH2() {
        return isUsingH2;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb() throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();

        if(/*defaultDb.equals(DbTypes.H2.db) &&*/ handicapDatabase == null) {
            handicapDatabase = new HandicapDatabaseH2();
            isUsingH2 = true;
        }

        return (T) handicapDatabase;
    }

    /*
        Used for JOOQ Generate - set to false so DSL is not executed
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Boolean isCreateTables) throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb().toLowerCase();

       if(defaultDb.equals(DbTypes.H2.db) && isCreateTables) {
            handicapDatabase = new HandicapDatabaseH2(isCreateTables);
            isUsingH2 = true;
        }

        return (T) handicapDatabase;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultDb(Map<String, String> overrideMap, Properties overrideProps)
            throws InterruptedException, IOException, SQLException {
        defaultDb = dodexUtil.getDefaultDb();

        if(defaultDb.equals(DbTypes.H2.db) && handicapDatabase == null) {
            handicapDatabase = new HandicapDatabaseH2(overrideMap, overrideProps);
            isUsingH2 = true;
        }
        return (T) handicapDatabase;
    }

    public static void configureDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && !overrideProps.isEmpty()) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);
    }

    public static void configureTestDefaults(Map<String, String>overrideMap, Properties overrideProps) {
        if(overrideProps != null && !overrideProps.isEmpty()) {
            properties = overrideProps;
        }
        mapMerge(map, overrideMap);

    }

    public static void mapMerge(Map<String,String> map1, Map<String, String> map2) {
        map2.forEach((key, value) -> map1
            .merge( key, value, (v1, v2) -> v2));  // let duplicate key in map2 win
    }

}