package golf.handicap.db

import dmo.fs.db.handicap.DbConfiguration
import dmo.fs.utils.ColorUtilConstants
import golf.handicap.*
import golf.handicap.generated.tables.references.COURSE
import golf.handicap.generated.tables.references.GOLFER
import golf.handicap.generated.tables.references.RATINGS
import golf.handicap.generated.tables.references.SCORES
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.Promise
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import org.jooq.*
import org.jooq.impl.*
import org.jooq.impl.DSL.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.*
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.Year
import java.util.*
import java.util.logging.Logger

class PopulateGolferScores : SqlConstants() {
    private val teeDate = SimpleDateFormat("yyyy-MM-dd")
    private val teeYear = SimpleDateFormat("yyyy")
    private var beginDate: String? = null
    private var endDate: String? = null
    private var maxRows = 20
    private var gettingData = false
    private var beginGolfDate: java.util.Date? = null
    private var endGolfDate: java.util.Date? = null
    private var overlapYears = false

    companion object {
        private val LOGGER = Logger.getLogger(PopulateGolferScores::class.java.name)

        //
        private val regEx = "\\$\\d".toRegex()

        @Throws(SQLException::class)
        @JvmStatic
        fun buildSql() {
            GETSETUSEDUPDATE =
                if (qmark) setupSetUsedUpdate().replace(regEx, "?")
                else setupSetUsedUpdate().replace("\"", "")
            val dialect = create!!.dsl().dialect().toString()
            if ("SQLITE" == dialect || "DEFAULT" == dialect) {
                GETSETUSEDSQLITEUPDATE =
                    if (qmark) setupSqliteSetUsedUpdate().replace(regEx, "?")
                    else setupSqliteSetUsedUpdate()
            }
            GETRESETUSEDUPDATE =
                if (qmark) setupResetUsedUpdate().replace(regEx, "?").replace("\"", "")
                else setupResetUsedUpdate().replace("\"", "")
            GETRESETUSEDSQLITEUPDATE =
                if (qmark) setupSqliteResetUsedUpdate().replace(regEx, "?").replace("\"", "")
                else setupSqliteResetUsedUpdate()
            GETHANDICAPUPDATE =
                if (qmark) setupHandicapUpdate().replace(regEx, "?").replace("\"", "")
                else setupHandicapUpdate().replace("\"", "")
            GETHANDICAPSQLITEUPDATE =
                if (qmark) setupSqliteHandicapUpdate().replace(regEx, "?").replace("\"", "")
                else setupSqliteHandicapUpdate()
            GETSCORESUPDATE =
                if (qmark) setupScoreUpdate().replace(regEx, "?").replace("\"", "")
                else setupScoreUpdate().replace("\"", "")
            GETSCORESSQLITEUPDATE =
                if (qmark) setupSqliteScoreUpdate().replace(regEx, "?").replace("\"", "")
                else setupSqliteScoreUpdate().replace("\"", "")
            GETGOLFERDATA =
                if (qmark) setupGetGolferData().replace(regEx, "?").replace("\"", "")
                else setupGetGolferData().replace("\"", "")
            GETGOLFERPUBLICDATA =
                if (qmark) setupGetPublicGolferData().replace(regEx, "?").replace("\"", "")
                else setupGetPublicGolferData().replace("\"", "")
            GETREMOVESCORE =
                if (qmark) setupRemoveScore().replace(regEx, "?").replace("\"", "")
                else setupRemoveScore().replace("\"", "")
            GETREMOVESCORESUB =
                if (qmark) setupRemoveScoreSub().replace(regEx, "?").replace("\"", "")
                else setupRemoveScoreSub().replace("\"", "")
            GETLASTSCORE =
                if (qmark) setupGetLastScore().replace(regEx, "?").replace("\"", "")
                else setupGetLastScore().replace("\"", "")
            GETGOLFERSCORES =
                if (qmark) setupGetGolferScores().replace(regEx, "?").replace("\"", "")
                else setupGetGolferScores().replace("\"", "")
        }

        @JvmStatic
        private fun setupSetUsedUpdate(): String {
            return create!!.renderNamedParams(
                update(SCORES)
                    .set(SCORES.USED, "*")
                    .where(SCORES.PIN.eq("$").and(SCORES.COURSE_SEQ.eq(0)).and(SCORES.TEE_TIME.eq("$")))
            )
        }

        @JvmStatic
        private fun setupSqliteSetUsedUpdate(): String {
            return create!!.renderNamedParams(
                update(table("SCORES"))
                    .set(field("USED"), '*')
                    .where(
                        (field("PIN")
                            .eq("$")
                            .and(field("COURSE_SEQ").eq("$"))
                            .and(field("TEE_TIME").eq("$")))
                    )
            )
        }

        @JvmStatic
        private fun setupResetUsedUpdate(): String {
            return create!!.renderNamedParams(
                update(SCORES).setNull(SCORES.USED).where(SCORES.PIN.eq("$").and(SCORES.USED.eq("$")))
            )
        }

        @JvmStatic
        private fun setupSqliteResetUsedUpdate(): String {
            return create!!.renderNamedParams(
                update(table("SCORES"))
                    .setNull(field("USED"))
                    .where((field("PIN").eq("$").and(field("USED").eq("$"))))
            )
        }

        @JvmStatic
        private fun setupHandicapUpdate(): String {
            return create!!.renderNamedParams(
                update(GOLFER).set(GOLFER.HANDICAP, 0.0f).where(GOLFER.PIN.eq("$"))
            )
        }

        @JvmStatic
        private fun setupSqliteHandicapUpdate(): String {
            return create!!.renderNamedParams(
                update(table("golfer")).set(field("HANDICAP"), "$").where((field("PIN").eq("$")))
            )
        }

        @JvmStatic
        private fun setupScoreUpdate(): String {
            return create!!.renderNamedParams(
                update(SCORES)
                    .set(SCORES.HANDICAP, 0.0f)
                    .set(SCORES.NET_SCORE, 0.0f)
                    .where(SCORES.COURSE_SEQ.eq(0).and(SCORES.PIN.eq("$")).and(SCORES.TEE_TIME.eq("$")))
            )
        }

        @JvmStatic
        private fun setupSqliteScoreUpdate(): String {
            return create!!.renderNamedParams(
                update(table("scores"))
                    .set(field("HANDICAP"), "$")
                    .set(field("NET_SCORE"), "$")
                    .where(
                        field("COURSE_SEQ")
                            .eq("$")
                            .and(field("PIN").eq("$").and(field("TEE_TIME").eq("$")))
                    )
            )
        }

        @JvmStatic
        private fun setupGetGolferData(): String {
            // val json: String = create!!.selectFrom(GOLFER, RATINGS, COURSE,
            // SCORES).where().fetch().formatJSON();
            return create!!.renderNamedParams(
                select(
                    COURSE.COURSE_SEQ,
                    COURSE.COURSE_NAME,
                    SCORES.PIN,
                    SCORES.GROSS_SCORE,
                    SCORES.NET_SCORE,
                    SCORES.ADJUSTED_SCORE,
                    SCORES.HANDICAP,
                    SCORES.COURSE_TEES,
                    SCORES.TEE_TIME,
                    SCORES.USED
                )
                    .from(GOLFER, RATINGS, COURSE, SCORES)
                    .where(
                        (GOLFER.PIN.eq(SCORES.PIN))
                            .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                            .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                            .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                            .and(GOLFER.PIN.eq("$"))
                            .and(SCORES.TEE_TIME.between("$").and("$"))
                    )
                    .orderBy(SCORES.TEE_TIME.desc())
            )
        }

        @JvmStatic
        private fun setupGetPublicGolferData(): String {

            return create!!.renderNamedParams(
                select(
                    COURSE.COURSE_SEQ,
                    COURSE.COURSE_NAME,
                    SCORES.PIN,
                    SCORES.GROSS_SCORE,
                    SCORES.NET_SCORE,
                    SCORES.ADJUSTED_SCORE,
                    SCORES.HANDICAP,
                    SCORES.COURSE_TEES,
                    SCORES.TEE_TIME,
                    SCORES.USED
                )
                    .from(GOLFER, RATINGS, COURSE, SCORES)
                    .where(
                        (GOLFER.PIN.eq(SCORES.PIN))
                            .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                            .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                            .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                            .and(GOLFER.FIRST_NAME.eq("$"))
                            .and(GOLFER.LAST_NAME.eq("$"))
                            .and(SCORES.TEE_TIME.between("$").and("$"))
                    )
                    .orderBy(SCORES.TEE_TIME.desc())
            )
        }

        @JvmStatic
        private fun setupRemoveScore(): String {
            return create!!.renderNamedParams(
                delete(SCORES)
                    .where(SCORES.PIN.eq("$").and(SCORES.COURSE_SEQ.eq(0)).and(SCORES.TEE_TIME.eq("$")))
            )
        }

        @JvmStatic
        private fun setupRemoveScoreSub(): String {
            return create!!.renderNamedParams(
                delete(SCORES)
                    .where(
                        (SCORES.PIN.eq("$")).and(
                            SCORES.TEE_TIME.eq(
                                select(max(SCORES.TEE_TIME)).from(SCORES).where(SCORES.PIN.eq("$"))
                            )
                        )
                    )
            )
        }

        @JvmStatic
        private fun setupGetLastScore(): String {
            return create!!.renderNamedParams(
                select(SCORES.USED, SCORES.COURSE_SEQ, SCORES.TEE_TIME)
                    .from(SCORES)
                    .where(
                        (SCORES.PIN.eq("$")).and(
                            SCORES.TEE_TIME.eq(
                                select(max(SCORES.TEE_TIME)).from(SCORES).where(SCORES.PIN.eq("$"))
                            )
                        )
                    )
            )
        }

        @JvmStatic
        private fun setupGetGolferScores(): String {
            return create!!.renderNamedParams(
                select(
                    GOLFER.PIN,
                    RATINGS.TEE_RATING,
                    RATINGS.TEE_SLOPE,
                    SCORES.ADJUSTED_SCORE,
                    SCORES.TEE_TIME,
                    COURSE.COURSE_SEQ,
                    RATINGS.TEE_PAR
                )
                    .from(GOLFER, RATINGS, COURSE, SCORES)
                    .where(
                        (GOLFER.PIN.eq(SCORES.PIN))
                            .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                            .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                            .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                            .and(GOLFER.PIN.eq("$"))
                            .and(SCORES.TEE_TIME.between("$").and("$"))
                    )
                    .orderBy(SCORES.TEE_TIME.desc())
            )
        }
    }

    private fun setPeriod(year: String) {
        beginDate = "$year-01-01"
        endDate = (year.toInt() + 1).toString() + "-01-01"
        beginGolfDate = beginDate?.let { teeDate.parse(it, ParsePosition(0)) }
        endGolfDate = endDate?.let { teeDate.parse(it, ParsePosition(0)) }
    }

    fun useCurrentYearOnly(thisYearOnly: Boolean) {
        overlapYears = thisYearOnly
    }

    @Throws(SQLException::class, InterruptedException::class)
    fun setGolferHandicap(golfer: Golfer): Uni<Int> {
        val handicapPromise: Promise<Int> = Promise.promise()
        var rowsUpdated = 0
        var sql: String?

        pool!!.withTransaction { conn ->
            var parameters: Tuple = Tuple.tuple()
            parameters.addFloat(golfer.handicap)
            parameters.addString(golfer.pin)

            sql = GETHANDICAPUPDATE

            val cf = conn.preparedQuery(sql)
                .execute(parameters)
                .onItem().invoke { rows ->
                    rowsUpdated += rows.rowCount()

                    val score = golfer.score
                    if (score == null) {
                        conn.close()
                        handicapPromise.complete(rowsUpdated)
                    } else {
                        parameters = Tuple.tuple()
                        parameters.addFloat(golfer.handicap)
                        parameters.addFloat(score.netScore)
                        parameters.addInteger(score.course!!.key)
                        parameters.addString(golfer.pin)
                        parameters.addString(score.teeTime)

                        sql = GETSCORESUPDATE

                        conn.preparedQuery(sql)
                            .execute(parameters)
                            .onItem().invoke { rows2 -> rowsUpdated += rows2.rowCount() }
                            .onFailure().invoke { err ->
                                LOGGER.severe(
                                    String.format(
                                        "%sError Updating Scores - %s%s",
                                        ColorUtilConstants.RED,
                                        err.message,
                                        ColorUtilConstants.RESET
                                    )
                                )
                            }
                            .onTermination().invoke { ->
                                conn.close()
                                handicapPromise.complete(rowsUpdated)
                            }.subscribeAsCompletionStage()
                    }
                }
                .onFailure().invoke { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Updating Handicap - %s%s",
                            ColorUtilConstants.RED,
                            err.message,
                            ColorUtilConstants.RESET
                        )
                    )

                    handicapPromise.complete(0)
                }.subscribeAsCompletionStage()

            Uni.createFrom().item(cf)
        }.subscribeAsCompletionStage()

        return handicapPromise.future()
    }

    @Throws(Exception::class)
    fun getGolferScores(golfer: Golfer, rows: Int): Uni<Map<String, Any?>>? {
        val oldRows = this.maxRows
        val golferScores: Promise<Map<String, Any?>>? = Promise.promise()
        this.maxRows = rows
        gettingData = true
        getGolferScores(golfer)!!.onItem().invoke { data ->
            gettingData = false
            this.maxRows = oldRows
            golferScores!!.complete(data)
        }.subscribeAsCompletionStage()
        return golferScores!!.future()
    }

    @Throws(Exception::class)
    fun getGolferScores(golfer: Golfer): Uni<Map<String, Any?>>? {
        val scoresPromise: Promise<Map<String, Any?>> = Promise.promise()
        val golferPin = golfer.pin
        val previousYear = Year.now().value - 1
        overlapYears = golfer.overlap

        beginDate = if (overlapYears) "01-01-$previousYear" else beginDate
        var sql: String?

        pool!!
            .connection
            .onItem().invoke { conn ->
                val parameters: Tuple = Tuple.tuple()
                val maximumRows = " limit $maxRows"

                sql = if (gettingData) {
                    if (golferPin.isNullOrEmpty()) {
                        parameters.addString(golfer.firstName)
                        parameters.addString(golfer.lastName)
                        GETGOLFERPUBLICDATA + maximumRows
                    } else {
                        parameters.addString(golferPin)
                        GETGOLFERDATA + maximumRows
                    }
                } else {
                    parameters.addString(golferPin)
                    GETGOLFERSCORES + maximumRows
                }
                parameters.addString(beginDate)
                parameters.addString(teeDate.format(endGolfDate))
                val tableMap: MutableMap<String, Any> = HashMap()
                conn.preparedQuery(sql)
                    .execute(parameters)
                    .onItem().invoke { rows ->
                        val columns = rows.columnsNames().size
                        var y: Int
                        val tableArray = JsonArray()

                        for (row in rows) {
                            y = 0
                            val rowObject = JsonObject()
                            while (y < columns) {
                                var name = row.getColumnName(y)

                                if ("NET_SCORE" == name || "HANDICAP" == name) {
                                    rowObject.put(
                                        name,
                                        BigDecimal(row.getValue(y++).toString()).setScale(1, RoundingMode.UP)
                                    )
                                } else if ("TEE_TIME" == name && gettingData) {
                                    rowObject.put(name, row.getValue(y++).toString().substring(0, 10))
                                } else {
                                    rowObject.put(name, row.getValue(y++))
                                }
                            }
                            tableArray.add(rowObject)
                        }

                        if (tableArray.size() != 0) {
                            tableMap["array"] = tableArray
                        }
                    }
                    .onFailure().invoke { err ->
                        LOGGER.severe(
                            String.format(
                                "%sError Getting Score(s) Data - %s%s \n%s",
                                ColorUtilConstants.RED,
                                err.message,
                                ColorUtilConstants.RESET,
                                err.stackTraceToString()
                            )
                        )
                    }
                    .onTermination().invoke { ->
                        scoresPromise.complete(tableMap)
                    }
                    .subscribeAsCompletionStage()
            }
            .subscribeAsCompletionStage()

        return scoresPromise.future()
    }

    @Throws(Exception::class)
    fun setUsed(conn: SqlConnection, pin: String?, course: Int, teeTime: String): Uni<Int> {
        val usedPromise: Promise<Int> = Promise.promise()
        var count = 0

        val parameters: Tuple = Tuple.tuple()
        parameters.addString("*")
        parameters.addString(pin)
        parameters.addInteger(course)
        parameters.addString(teeTime)

        val sql: String? = GETSETUSEDUPDATE?.uppercase()

        conn.preparedQuery(sql)
            .execute(parameters)
            .onItem().invoke { rows ->
                count += rows.rowCount()
            }
            .onFailure().invoke { err ->
                LOGGER.warning(
                    String.format(
                        "Used Parameters/Sql: %s %s -- %s",
                        parameters.deepToString(),
                        sql, err.message
                    )
                )
            }.onTermination().invoke { ->
                usedPromise.complete(count)
            }
            .subscribeAsCompletionStage()

        return usedPromise.future()
    }

    @Throws(Exception::class)
    fun clearUsed(connection: SqlConnection, pin: String?): Uni<Int> {
        val usedPromise: Promise<Int> = Promise.promise()
        var count = 0

        val parameters: Tuple = Tuple.tuple()
        parameters.addString(null)

        val sql: String? = GETRESETUSEDUPDATE
        parameters.addString(pin)
        parameters.addString("*")

        connection.preparedQuery(sql)
            .execute(parameters)
            .onItem().invoke { rows ->
                count += rows.rowCount()
            }
            .onFailure().invoke { err ->
                LOGGER.severe(
                    String.format(
                        "%sError Cleaning Used - %s%s",
                        ColorUtilConstants.RED,
                        err.message,
                        ColorUtilConstants.RESET
                    )
                )
            }.onTermination().invoke { ->
                usedPromise.complete(count)
            }.subscribeAsCompletionStage()

        return usedPromise.future()
    }

    @Throws(Exception::class)
    fun removeLastScore(golferPIN: String?): Uni<String> {
        val usedPromise: Promise<String> = Promise.promise()
        var count = 0

        pool!!.withTransaction { conn ->
            val parameters: Tuple = Tuple.tuple()
            parameters.addString(golferPIN)
            parameters.addString(golferPIN)
            var used: String? = ""

            val cf = conn.preparedQuery(GETLASTSCORE)
                .execute(parameters)
                .onItem().invoke { lastRows ->
                    for (row in lastRows) {
                        used = row.getString(0)
                    }
                    if (used == null) {
                        used = "N"
                    }

                    conn.preparedQuery(GETREMOVESCORESUB)
                        .execute(parameters)
                        .onItem().invoke { rows ->
                            count += rows.rowCount()
                        }
                        .onFailure().invoke { err ->
                            LOGGER.severe(
                                String.format(
                                    "%sError removing last - %s%s",
                                    ColorUtilConstants.RED,
                                    err.message,
                                    ColorUtilConstants.RESET
                                )
                            )
                        }.onTermination().invoke { ->
                            usedPromise.complete(used)
                            conn.close()
                        }
                        .subscribeAsCompletionStage()
                }
                .onFailure().invoke { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError getting Used - %s%s",
                            ColorUtilConstants.RED,
                            err.message,
                            ColorUtilConstants.RESET
                        )
                    )
                    usedPromise.complete(used)
                    conn.close()
                }.subscribeAsCompletionStage()
            Uni.createFrom().item(cf)
        }
            .subscribeAsCompletionStage()

        return usedPromise.future()
    }

    fun getSqlPool(): Pool? {
        return pool
    }

    init {
        setPeriod(teeYear.format(java.util.Date()))
    }
}
