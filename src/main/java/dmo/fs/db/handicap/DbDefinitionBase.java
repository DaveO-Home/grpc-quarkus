
package dmo.fs.db.handicap;

import dmo.fs.db.openapi.GroupOpenApiSqlHandicap;
import dmo.fs.db.handicap.utils.DodexUtil;
import golf.handicap.db.PopulateCourse;
import golf.handicap.db.PopulateGolfer;
import golf.handicap.db.PopulateGolferScores;
import golf.handicap.db.PopulateScore;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.jdbcclient.JDBCPool;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public abstract class DbDefinitionBase {
  protected final static Logger logger = LoggerFactory.getLogger(DbDefinitionBase.class.getName());

  protected static DSLContext create;

  protected Boolean isTimestamp;
  protected Vertx vertx;
  protected static Pool pool;
  protected static JDBCConnectOptions jdbcConnectOptions;
  protected static PoolOptions poolOptions;
  protected static boolean qmark = true;

  public static <T> void setupSql(T pool4) throws IOException, SQLException {
    // Non-Blocking Drivers
   if (pool4 instanceof JDBCPool) {
      pool = (Pool) pool4;
    }

    if(logger.isDebugEnabled()) {
      logger.debug("Pool for H2 Database: " + pool);
    }

    Settings settings = new Settings().withRenderNamedParamPrefix("$"); // making compatible with Vertx4/Postgres
    create = DSL.using(DodexUtil.getSqlDialect(), settings);

    /* @TODO: convert GroupOpenApiSql to mutiny */
    if (pool4 instanceof JDBCPool) {
      io.vertx.rxjava3.sqlclient.Pool poolRx =
              io.vertx.rxjava3.jdbcclient.JDBCPool.pool(io.vertx.rxjava3.core.Vertx.vertx(), jdbcConnectOptions, poolOptions);
      GroupOpenApiSqlHandicap.setPool(poolRx);
    }
    GroupOpenApiSqlHandicap.setCreate(create);
    GroupOpenApiSqlHandicap.setQmark(qmark);
    GroupOpenApiSqlHandicap.buildSql();

    PopulateGolfer.setQMark(qmark);
    PopulateGolfer.setSqlPool(pool);
    PopulateGolfer.setDslContext(create);
    PopulateGolfer.buildSql();
    PopulateCourse.buildSql();
    PopulateScore.buildSql();
    PopulateGolferScores.buildSql();
  }

  public void setIsTimestamp(Boolean isTimestamp) {
    this.isTimestamp = isTimestamp;
  }

  public boolean getisTimestamp() {
    return this.isTimestamp;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  public static DSLContext getCreate() {
    return create;
  }

  public void setJDBCConnectOptions(JDBCConnectOptions jdbcConnectOptions) {
    DbDefinitionBase.jdbcConnectOptions = jdbcConnectOptions;
  }

  public void setPoolOptions(PoolOptions poolOptions) {
    DbDefinitionBase.poolOptions = poolOptions;
  }
}
