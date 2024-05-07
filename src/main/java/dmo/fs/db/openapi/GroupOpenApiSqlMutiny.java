package dmo.fs.db.openapi;

import dmo.fs.db.MessageUser;
import dmo.fs.db.wsnext.DbConfiguration;
import dmo.fs.db.wsnext.DbDefinitionBase;
import dmo.fs.db.wsnext.DodexDatabase;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.SingleHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.sqlclient.*;
import jakarta.enterprise.context.Dependent;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.jooq.impl.DSL.*;

//@SessionScoped
@Dependent
public class GroupOpenApiSqlMutiny {
  protected final static Logger logger = LoggerFactory.getLogger(GroupOpenApiSqlMutiny.class.getName());

  protected static String GETGROUPBYNAME;
  protected static String GETADDGROUP;
  protected static String GETMARIAADDGROUP;
  protected static String GETADDMEMBER;
  protected static String GETDELETEGROUP;
  protected static String GETDELETEGROUPBYID;
  protected static String GETDELETEMEMBERS;
  protected static String GETDELETEMEMBER;
  protected static String GETMEMBERSBYGROUP;
  protected static String GETUSERBYNAMESQLITE;
  protected static DSLContext create;
  protected static Pool pool;
  protected static boolean qmark = true;
  protected final static DateTimeFormatter formatter =
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

  public static void buildSql() {
    GETGROUPBYNAME = qmark ? setupGroupByName().replaceAll("\\$\\d", "?") : setupGroupByName();
    GETADDGROUP = qmark ? setupAddGroup().replaceAll("\\$\\d", "?") : setupAddGroup();
    GETMARIAADDGROUP =
      qmark ? setupMariaAddGroup().replaceAll("\\$\\d", "?") : setupMariaAddGroup();
    GETADDMEMBER = qmark ? setupAddMember().replaceAll("\\$\\d", "?") : setupAddMember();
    GETDELETEGROUP = qmark ? setupDeleteGroup().replaceAll("\\$\\d", "?") : setupDeleteGroup();
    GETDELETEGROUPBYID = qmark ? setupDeleteGroupById().replaceAll("\\$\\d", "?") : setupDeleteGroupById();
    GETDELETEMEMBERS = qmark ? setupDeleteMembers().replaceAll("\\$\\d", "?") : setupDeleteMembers();
    GETDELETEMEMBER = qmark ? setupDeleteMember().replaceAll("\\$\\d", "?") : setupDeleteMember();
    GETMEMBERSBYGROUP = qmark ? setupMembersByGroup().replaceAll("\\$\\d", "?") : setupMembersByGroup();
    GETUSERBYNAMESQLITE = setupUsersByNameSqlite();
  }

  protected static String setupGroupByName() {
    return create.renderNamedParams(
      select(field("ID"), field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
        .from(table("groups")).where(field("NAME").eq("$")));
  }

  public String getGroupByName() {
    return GETGROUPBYNAME;
  }

  protected static String setupAddGroup() {
    return create.renderNamedParams(insertInto(table("groups"))
      .columns(field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
      .values("$", "$", "$", "$").returning(field("ID")));
  }

  public String getAddGroup() {
    return GETADDGROUP;
  }

  protected static String setupMariaAddGroup() {
    return create.renderNamedParams(insertInto(table("groups"))
      .columns(field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
      .values("$", "$", "$", "$"));
  }

  public String getMariaAddGroup() {
    return GETMARIAADDGROUP;
  }

  protected static String setupAddMember() {
//        org.jooq.Record record = create.selectFrom((table("member")).where(field("GROUP_ID").eq("$").and(field("USER_ID").ne("$")))).fetchOne();
    return create.renderNamedParams(
      insertInto(table("member")).columns(field("GROUP_ID"), field("USER_ID")).values("$", "$"));
  }

  public String getAddMember() {
    return GETADDMEMBER;
  }

  protected static String setupDeleteGroup() {

    return create.renderNamedParams(deleteFrom(table("groups")).where(field("NAME").eq("$1")).returning(field("ID")));
  }

  public String getDeleteGroup() {
    return GETDELETEGROUP;
  }

  protected static String setupDeleteGroupById() {
    return create.renderNamedParams(deleteFrom(table("groups")).where(field("ID").eq("$1")).returning(field("ID")));
  }

  public String getDeleteGroupById() {
    return GETDELETEGROUPBYID;
  }

  protected static String setupDeleteMembers() {
    return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1"))); // .returning(field("ID")));
  }

  public String getDeleteMembers() {
    return GETDELETEMEMBERS;
  }

  protected static String setupDeleteMember() {
    return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1").and(field("USER_ID").eq("$2")))); // .returning(field("ID")));
  }

  public String getDeleteMember() {
    return GETDELETEMEMBER;
  }

  protected static String setupMembersByGroup() {
    return create.renderNamedParams(select(field("USER_ID"), field("users.NAME"), field("GROUP_ID"))
      .from(table("groups")).join(table("member"))
      .on(field("groups.ID").eq(field("GROUP_ID")).and(field("groups.NAME").eq("$")))
      .join(table("users")).on(field("users.ID").eq(field("USER_ID")))
      .whereExists(select(field("ID"))
        .from(table("users")).join(table("member"))
        .on(field("users.ID").eq(field("USER_ID")).and(field("users.NAME").eq("$")))
      ));
  }

  public String getMembersByGroup() {
    return GETMEMBERSBYGROUP;
  }

  protected static String setup() {
    return create.renderNamedParams(
      select(field("ID"), field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
        .from(table("groups")).where(field("NAME").eq("$")));
  }

  protected static String setupUsersByNameSqlite() {
    return create.render(select(field("*")).from(table("users").where(field("NAME").in("?"))));
  }

  public String getupUsersByNameSqlite() {
    return GETUSERBYNAMESQLITE;
  }

  public Future<JsonObject> addGroupAndMembers(JsonObject addGroupJson)
    throws InterruptedException, SQLException, IOException {
    final Promise<JsonObject> promise = Promise.promise();
    final DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    final MessageUser messageUser = dodexDatabase.createMessageUser();
    final Map<String, String> selected = DodexUtil.commandMessage(addGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));
    String ownerKey = addGroupJson.getString("ownerKey");

    if (ownerKey != null) {
      messageUser.setId(Long.valueOf(ownerKey));
    }

    addGroup(addGroupJson).onSuccess(groupJson -> {
      String entry0 = selectedUsers.get(0);

      if (groupJson.getInteger("status") == 0 &&
        entry0 != null && !"".equals(entry0)) {
        try {
          addMembers(selectedUsers, groupJson).onSuccess(promise::complete).onFailure(err -> {
            logger.error("Add group/member err1: " + err.getMessage());
            addGroupJson.put("status", -1);
            addGroupJson.put("errorMessage", err.getMessage());
            promise.complete(addGroupJson);
          });
        } catch (InterruptedException | SQLException | IOException err) {
          err.printStackTrace();
          addGroupJson.put("status", -1);
          addGroupJson.put("errorMessage", err.getMessage());
        }
      } else {
        promise.complete(addGroupJson);
      }
    }).onFailure(err -> {
      logger.error("Add group/member err2: " + err.getMessage());
      promise.complete(addGroupJson);
    });

    return promise.future();
  }

  protected Future<JsonObject> addGroup(JsonObject addGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    Timestamp current = new Timestamp(new Date().getTime());
    OffsetDateTime time = OffsetDateTime.now();

    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();
    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));

    dodexDatabase.selectUser(messageUser).future()
      .subscribeAsCompletionStage().thenComposeAsync(userData -> {
        addGroupJson.put("ownerKey", userData.getId());

        pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getGroupByName())
          .rxExecute(Tuple.of(addGroupJson.getString("groupName"))).doOnSuccess(rows -> {
            if (rows.size() == 1) {
              Row row = rows.iterator().next();
              addGroupJson.put("id", row.getInteger(0));
            }
          })
          .doOnError(Throwable::printStackTrace)
          .doAfterSuccess(result -> {
            if (addGroupJson.getInteger("id") == null) {
              Tuple parameters = Tuple.of(addGroupJson.getString("groupName"),
                addGroupJson.getInteger("ownerKey"), current, current);
              String sql = getAddGroup();

              conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                for (Row row : rows) {
                  addGroupJson.put("id", row.getLong(0));
                }

                LocalDate localDate = LocalDate.now();
                LocalTime localTime = LocalTime.of(LocalTime.now().getHour(),
                  LocalTime.now().getMinute(), LocalTime.now().getSecond());
                ZonedDateTime zonedDateTime =
                  ZonedDateTime.of(localDate, localTime, ZoneId.systemDefault());
                String openApiDate = zonedDateTime.format(formatter);

                addGroupJson.put("created", openApiDate);
                addGroupJson.put("status", 0);
                conn.close();
                promise.complete(addGroupJson);
              }).subscribe(rows -> {
              }, err -> {
                logger.error(String.format("%sError Adding group: %s%s", ColorUtilConstants.RED,
                  err, ColorUtilConstants.RESET));
                errData(err, promise, addGroupJson);
                if (err != null && err.getMessage() != null) {
                  conn.close();
                }
                if (addGroupJson.getInteger("id") == null) {
                  addGroupJson.put("id", -1);
                }
              });
            } else {
              promise.complete(addGroupJson);
            }
          }).subscribe(rows -> {
          }, err -> {
            logger.error(String.format("%sError Adding group: %s%s", ColorUtilConstants.RED,
              err, ColorUtilConstants.RESET));
            errData(err, promise, addGroupJson);
            if (err != null && err.getMessage() != null) {
              conn.close();
            }
            if (addGroupJson.getInteger("id") == null) {
              addGroupJson.put("id", -1);
            }
          })).subscribe();
        return null;
      });
    return promise.future();
  }

  protected Future<JsonObject> addMembers(List<String> selectedUsers, JsonObject addGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();

    checkOnGroupOwner(addGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        List<String> allUsers = new ArrayList<>();
        allUsers.add(addGroupJson.getString("groupOwner"));
        allUsers.addAll(selectedUsers);

        checkOnMembers(allUsers, addGroupJson).onSuccess(newUsers -> {
          if (!newUsers.isEmpty()) {
            pool.getConnection().doOnSuccess(connection -> {
              List<Tuple> userList = new ArrayList<>();
              StringBuilder sql = new StringBuilder();
              StringBuilder stringBuilder = new StringBuilder();

              for (String name : newUsers) {
                stringBuilder.append("'").append(name).append("'");
                if (!newUsers.get(newUsers.size() - 1).equals(name)) {
                  stringBuilder.append(",");
                }
              }

              // jdbc(sqlite3,h2) client 'select' does not work well with executeBatch
              Single<RowSet<Row>> query;
              // jooq generates "cast" for parameter(h2), easier to just remove
              sql.append(getupUsersByNameSqlite().replace("cast(", "").replace(" as varchar)", ""));
              query = connection.preparedQuery(sql.toString().replace("?", stringBuilder.toString())).execute();

              query.doOnSuccess(result -> {
                List<Tuple> list = new ArrayList<>();
                for (RowSet<Row> rows = result; rows != null; rows = rows.next()) {
                  for (Row row : rows) {
                    list.add(Tuple.of(addGroupJson.getInteger("id"), row.getInteger(0)));
                  }
                }

                connection.rxBegin().doOnSuccess(tx -> connection
                  .preparedQuery(getAddMember()).executeBatch(list)
                  .doOnSuccess(res -> {
                    int rows = 0;
                    for (RowSet<Row> s = res; s != null; s = s.next()) {
                      if (s.rowCount() != 0) {
                        rows += s.rowCount();
                      }
                    }
                    addGroupJson.put("errorMessage", "Members Added: " + rows);
                    tx.rxCommit().doFinally(() ->
                      connection.rxClose().doFinally(() -> promise.complete(addGroupJson)).subscribe()
                    ).subscribe();

                  }).subscribe(v -> {
                  }, err -> {
                    errData(err, promise, addGroupJson);
                    if (err != null && err.getMessage() != null) {
                      // committing because some of the batch inserts may have succeeded
                      tx.rxCommit().doFinally(() ->
                        connection.rxClose().subscribe()
                      ).subscribe();
                    }
                  })).subscribe(v -> {
                }, err -> {
                  errData(err, promise, addGroupJson);
                  if (err != null && err.getMessage() != null) {
                    connection.close();
                  }
                });
              }).subscribe(result -> {
                List<Tuple> list = new ArrayList<>();
                for (RowSet<Row> rows = result; rows != null; rows = rows.next()) {
                  if (rows.iterator().hasNext()) {
                    Row row = rows.iterator().next();
                    list.add(Tuple.of(addGroupJson.getInteger("id"), row.getInteger(0)));
                  }
                }
              }, err -> {
                errData(err, promise, addGroupJson);
                if (err != null && err.getMessage() != null) {
                  connection.close();
                }
              });
            }).subscribe(v -> {
            }, Throwable::printStackTrace);
          } else {
            addGroupJson.put("errorMessage", "Some member(s) already added");
            promise.complete(addGroupJson);
          }
        }).onFailure(Throwable::printStackTrace);
      } else {
        addGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(addGroupJson);
      }
    }).onFailure(Throwable::printStackTrace);
    return promise.future();
  }

  public Future<JsonObject> deleteGroupOrMembers(JsonObject deleteGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();
    Map<String, String> selected = DodexUtil.commandMessage(deleteGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    messageUser.setName(deleteGroupJson.getString("groupOwner"));
    messageUser.setPassword(deleteGroupJson.getString("ownerId"));
    String ownerKey = deleteGroupJson.getString("ownerKey");

    if (ownerKey != null) {
      messageUser.setId(Long.valueOf(ownerKey));
    }

    String entry0 = selectedUsers.get(0);

    if (deleteGroupJson.getInteger("status") == 0 &&
      "".equals(entry0)) {
      try {
        deleteGroup(deleteGroupJson)
          .onSuccess(deleteGroupObject -> promise.complete(deleteGroupJson))
          .onFailure(err -> errData(err, promise, deleteGroupJson));
      } catch (InterruptedException | SQLException | IOException err) {
        errData(err, promise, deleteGroupJson);
      }
    } else if (deleteGroupJson.getInteger("status") == 0) {
      try {
        deleteMembers(selectedUsers, deleteGroupJson)
          .onSuccess(promise::complete)
          .onFailure(err -> {
            errData(err, promise, deleteGroupJson);
          });
      } catch (InterruptedException | SQLException | IOException err) {
        errData(err, promise, deleteGroupJson);
      }
    } else {
      promise.complete(deleteGroupJson);
    }

    return promise.future();
  }

  protected Future<JsonObject> deleteGroup(JsonObject deleteGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    checkOnGroupOwner(deleteGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        pool.rxGetConnection().doOnSuccess(connection -> connection.preparedQuery(getGroupByName())
          .rxExecute(Tuple.of(deleteGroupJson.getString("groupName")))
          .doOnSuccess(rows -> {
            Integer id = 0;
            for (Row row : rows) {
              id = row.getInteger(0);
            }
            deleteGroupJson.put("id", id);
            Tuple parameters = Tuple.of(id);

            connection.rxBegin()
              .doOnSuccess(tx -> connection
                .preparedQuery(getDeleteMembers())
                .rxExecute(parameters)
                .doOnSuccess(r -> {
                  int deletedMembers = 0;
                  for (RowSet<Row> s = r; s != null; s = s.next()) {
                    if (s.rowCount() != 0) {
                      deletedMembers += s.rowCount();
                    }
                  }
                  if (deletedMembers > 0) {
                    deleteGroupJson.put("errorMessage", deletedMembers + " members with ");
                  } else {
                    parameters.clear();
                    parameters.addInteger(0);
                  }
                  connection
                    .preparedQuery(getDeleteGroupById())
                    .rxExecute(parameters)
                    .doFinally(() -> {
                      deleteGroupJson.put("status", 0);
                      deleteGroupJson.put("errorMessage", deleteGroupJson.getString("errorMessage") + "group deleted");
                      tx.rxCommit().doFinally(() ->
                        connection.rxClose().doFinally(() -> promise.complete(deleteGroupJson)).subscribe()
                      ).subscribe();
                    }).subscribe(v -> {
                    }, err -> {
                      errData(err, promise, deleteGroupJson);
                      if (err != null && err.getMessage() != null) {
                        connection.close();
                      }
                    });
                }).doOnError(err -> {
                  errData(err, promise, deleteGroupJson);
                }).subscribe())
              .subscribe(v -> {
              }, err -> {
                errData(err, promise, deleteGroupJson);
                if (err != null && err.getMessage() != null) {
                  connection.close();
                }
              });
          }).subscribe(v -> {
          }, err -> {
            errData(err, promise, deleteGroupJson);
          })
        ).subscribe(v -> {
        }, err -> {
          errData(err, promise, deleteGroupJson);
        });
      } else {
        deleteGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(deleteGroupJson);
      }
    });
    return promise.future();
  }

  protected Future<JsonObject> deleteMembers(List<String> selectedUsers, JsonObject deleteGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();

    checkOnGroupOwner(deleteGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        pool.rxGetConnection()
          .doOnSuccess(connection -> connection.preparedQuery(getGroupByName())
            .rxExecute(Tuple.of(deleteGroupJson.getString("groupName")))
            .doOnSuccess(rows -> {
              Integer id = 0;
              for (Row row : rows) {
                id = row.getInteger(0);
              }
              deleteGroupJson.put("id", id);
              connection.rxBegin().doOnSuccess(tx -> { //connection
                StringBuilder sql = new StringBuilder();
                StringBuilder stringBuilder = new StringBuilder();

                List<Tuple> userList = new ArrayList<>();
                for (String name : selectedUsers) {
                  stringBuilder.append("'").append(name).append("',");
                }

                Single<RowSet<Row>> query;
                stringBuilder.append("''"); //.append(deleteGroupJson.getString("groupOwner")).append("'");
//                                    sql.append(getupUsersByNameSqlite().replace("?", stringBuilder.toString()));
                sql.append(getupUsersByNameSqlite().replace("cast(", "").replace(" as varchar)", ""));
                query = connection.preparedQuery(sql.toString().replace("?", stringBuilder.toString())).execute();

                query.doOnSuccess(result -> {
                  // Sqlite3(jdbc client) 'select' does not work well with executeBatch
                  List<Tuple> list = new ArrayList<>();
                  for (RowSet<Row> rows2 = result; rows2 != null; rows2 = rows2.next()) {
                    for (Row row : rows2) {
                      list.add(Tuple.of(deleteGroupJson.getInteger("id"), row.getInteger(0)));
                    }
                  }
                  connection.preparedQuery(getDeleteMember()).executeBatch(list)
                    .doOnSuccess(res -> {
                      int rows3 = 0;
                      for (RowSet<Row> s = res; s != null; s = s.next()) {
                        if (s.rowCount() != 0) {
                          rows3 += s.rowCount();
                        }
                      }
                      deleteGroupJson.put("errorMessage", "Members Deleted: " + rows3);
                      tx.rxCommit().doFinally(() ->
                        connection.rxClose().doFinally(() -> promise.complete(deleteGroupJson)).subscribe()
                      ).subscribe();

                    }).subscribe(v -> {
                    }, err -> {
                      errData(err, promise, deleteGroupJson);
                      if (err != null && err.getMessage() != null) {
                        // committing because some of the batch deletes may have succeeded
                        tx.rxCommit().doFinally(() ->
                          connection.rxClose().subscribe()
                        ).subscribe();
                      }
                    });

                }).subscribe(v -> {
                }, Throwable::printStackTrace);
              }).subscribe(v -> {
              }, Throwable::printStackTrace); // .subscribe(v -> {}, Throwable::printStackTrace));
            }).subscribe(v -> {
            }, Throwable::printStackTrace))
          .subscribe(v -> {
          }, Throwable::printStackTrace);
      } else {
        deleteGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(deleteGroupJson);
      }
    });

    return promise.future();
  }

  public Future<JsonObject> getMembersList(JsonObject getGroupJson)
    throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();

    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();

    messageUser.setName(getGroupJson.getString("groupOwner"));
    messageUser.setPassword(getGroupJson.getString("ownerId"));

    try {
      dodexDatabase.selectUser(messageUser).future()
        .subscribeAsCompletionStage().thenComposeAsync(userData -> {
          JsonArray members = new JsonArray();
          getGroupJson.put("ownerKey", userData.getId());
          Tuple parameters = Tuple.of(getGroupJson.getString("groupName"),
            getGroupJson.getString("groupOwner"));
          pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getMembersByGroup())
            .rxExecute(parameters).doOnSuccess(rows -> {
              if (rows.size() > 0) {
                for (Row row : rows) {
                  if (!row.getString(1).equals(getGroupJson.getString("groupOwner"))) {
                    members.add(new JsonObject().put("name", row.getString(1)));
                  } else {
                    getGroupJson.put("id", row.getInteger(2));
                  }
                }
                getGroupJson.put("members", members.encode());
                promise.complete(getGroupJson);
              } else {
                getGroupJson.put("errorMessage", "Group not found: " + getGroupJson.getString("groupName"));
                getGroupJson.put("id", 0);
                promise.complete(getGroupJson);
              }
            })
            .doOnError(Throwable::printStackTrace)
            .subscribe(o -> {
            }, Throwable::printStackTrace)).subscribe(o -> {
          }, Throwable::printStackTrace);
          return null;
        });
    } catch (InterruptedException | SQLException e) {
      throw new RuntimeException(e);
    }

    return promise.future();
  }

  protected Future<JsonObject> checkOnGroupOwner(JsonObject groupJson) throws InterruptedException {
    Promise<JsonObject> waitFor = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getGroupByName())
        .rxExecute(Tuple.of(groupJson.getString("groupName")))
        .doOnSuccess(rows -> {
          if (rows.size() == 1) {
            Row row = rows.iterator().next();
            groupJson.put("id", row.getInteger(0));
            groupJson.put("groupOwnerId", row.getInteger(2));
          }

          conn.preparedQuery(DbDefinitionBase.getUserByName())
            .rxExecute(Tuple.of(groupJson.getString("groupOwner")))
            .doOnSuccess(rows2 -> {
              if (rows2.size() == 1) {
                Row row = rows2.iterator().next();
                groupJson.put("checkGroupOwnerId", row.getInteger(0));
                groupJson.put("checkGroupOwner", row.getString(1));
              }
              conn.close();
            })
            .doOnError(err -> {
              errData(err, waitFor, groupJson);
              conn.close();
            })
            .doFinally(() -> {
              JsonObject config = Vertx.currentContext().config();
              boolean isCheckForOwner =
                config.getBoolean("dodex.groups.checkForOwner") != null &&
                  config.getBoolean("dodex.groups.checkForOwner");
              groupJson.put("checkForOwner", isCheckForOwner);
              groupJson.put("isValidForOperation", groupJson.getInteger("status") != -1 &&
                !isCheckForOwner || groupJson.getInteger("checkGroupOwnerId") == groupJson.getInteger("groupOwnerId"));
              if (!groupJson.getBoolean("isValidForOperation")) {
                groupJson.put("errorMessage", "Contact owner for group administration");
              }
              waitFor.complete(groupJson);
            }).subscribe();
        })
        .doOnError(err -> {
          errData(err, waitFor, groupJson);
          conn.close();
        }).subscribe())
      .doOnError(err -> {
        errData(err, waitFor, groupJson);
      }).subscribe();

    return waitFor.future();
  }

  protected Future<List<String>> checkOnMembers(List<String> selectedList, JsonObject addGroupJson) {
    Promise<List<String>> waitFor = Promise.promise();
    List<String> newSelected = new ArrayList<>();
    Single<SqlConnection> connResult = pool.rxGetConnection();

    for (String user : selectedList) {
      Single.just(user).subscribe(SingleHelper.toObserver(userName -> {
        Tuple parameters = Tuple.of(addGroupJson.getString("groupName"), userName.result());

        connResult.flatMap(conn -> {
            conn.preparedQuery(getMembersByGroup()).rxExecute(parameters)
              .doOnSuccess(rows -> {
                if (rows.size() == 0) {
                  newSelected.add(userName.result());
                }
                if (userName.result().equals(selectedList.get(selectedList.size() - 1))) {
                  waitFor.complete(newSelected);
                  conn.close();
                }
              }).doOnError(Throwable::printStackTrace)
              .subscribe();
            return Single.just(conn);
          }).doOnError(Throwable::printStackTrace)
          .subscribe();
      }));
    }

    return waitFor.future();
  }

  protected void errData(Throwable err, Promise<JsonObject> promise, JsonObject groupJson) {
    if (err != null && err.getMessage() != null) {
      if (!err.getMessage().contains("batch execution")) {
        err.printStackTrace();
        groupJson.put("errorMessage", err.getMessage());
      } else {
        groupJson.put("errorMessage", err.getMessage() + " -- some actions may have succeeded.");
        logger.error(err.getMessage());
      }
      groupJson.put("status", -1);
      promise.tryComplete(groupJson);
    }
  }

  public static void setCreate(DSLContext create) {
    GroupOpenApiSqlHandicap.create = create;
  }

  public static void setPool(Pool pool) {
    GroupOpenApiSqlHandicap.pool = pool;
  }

  public static void setQmark(boolean qmark) {
    GroupOpenApiSqlHandicap.qmark = qmark;
  }

}
