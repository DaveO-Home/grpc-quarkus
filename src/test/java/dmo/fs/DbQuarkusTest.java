package dmo.fs;

import dmo.fs.db.wsnext.h2.DbH2Next;
import io.quarkus.test.junit.QuarkusTest;

import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.mutiny.jdbcclient.JDBCPool;
import org.junit.jupiter.api.Disabled;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Promise;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;
import io.vertx.sqlclient.PoolOptions;

@QuarkusTest
public class DbQuarkusTest  extends DbH2Next {
	private static final Logger logger = LoggerFactory.getLogger(DbQuarkusTest.class.getName());
    @Disabled("Disabled until VertxExtension works with reactivex")
    // @Test
    public void testTestEndpoint() {
        given()
          .when().get("/test")
          .then()
             .statusCode(200).body(containsString("dodex--open"))
            ;
    }

    public Promise<Pool> databaseSetup() {

		Promise<Pool> promise = Promise.promise();
		JDBCPool pool = getConfiguredPool();

        pool.getConnection().flatMap(conn -> {
			conn.query(CHECKUSERSQL).execute().flatMap(row -> {
				RowIterator<Row> ri = row.iterator();
				String val = null;
				while (ri.hasNext()) {
					val = ri.next().getString(0);
				}
				if (val == null) {
					final String usersSql = getCreateTable("USERS");
					conn.query(usersSql).execute().onFailure().invoke(error -> {
						logger.error("{}Users Table Error1: {}{}", ColorUtilConstants.RED, error,
								ColorUtilConstants.RESET);
					}).onItem().invoke(c -> {
						if (c.next().rowCount() > 0) {
							logger.info("{}Users Table Added.{}", ColorUtilConstants.BLUE_BOLD_BRIGHT,
									ColorUtilConstants.RESET);
						}
					}).subscribeAsCompletionStage().complete(null);
				}
				return Uni.createFrom().item(conn);
			}).onFailure().invoke(error -> {
				logger.error("{}Users Table Error2: {}{}", ColorUtilConstants.RED, error, ColorUtilConstants.RESET);
			}).subscribeAsCompletionStage().complete(null);
			return Uni.createFrom().item(conn);
		}).flatMap(conn -> {
			promise.complete(pool);
			conn.close().onFailure().invoke(err -> err.printStackTrace()).subscribeAsCompletionStage().complete(null);
			return Uni.createFrom().item(pool);
		}).subscribeAsCompletionStage().complete(null);

		return promise;
	}

	private static JDBCPool getConfiguredPool() {

		PoolOptions poolOptions =
			new PoolOptions().setMaxSize(Runtime.getRuntime().availableProcessors() * 5);

		JDBCConnectOptions connectOptions = new JDBCConnectOptions()
			.setJdbcUrl("jdbc:h2:" + "file:../../unit_testh2.db")
			.setUser("sa")
			.setPassword("sa")
			.setIdleTimeout(1)
		// .setCachePreparedStatements(true)
		;

		Vertx vertx = Vertx.vertx();
		return JDBCPool.pool(vertx, connectOptions, poolOptions);
	}

	@Override
	public <T> T getPool() {
		return null;
	}

	@Override
	public MessageUser createMessageUser() {
		return null;
	}
}