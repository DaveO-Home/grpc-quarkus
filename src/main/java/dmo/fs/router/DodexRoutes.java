package dmo.fs.router;

import dmo.fs.quarkus.Server;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import golf.handicap.routes.GrpcRoutes;
import golf.handicap.routes.HandicapRoutes;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.CommandLineArguments;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.mutiny.core.Promise;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServerResponse;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.CorsHandler;
import io.vertx.mutiny.ext.web.handler.FaviconHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import io.vertx.mutiny.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

@Unremovable
@ApplicationScoped
public class DodexRoutes {
  @Inject
  Vertx vertx;

  protected static final Logger logger = LoggerFactory.getLogger(DodexRoutes.class.getName());
  protected static HandicapRoutes routesHandicap;
  protected final StaticHandler staticHandler = StaticHandler.create();
  protected final boolean isProduction = !ProfileManager.getLaunchMode().isDevOrTest();
  protected TcpEventBusBridge bridge;
  protected final io.vertx.core.Promise<Void> handicapPromise = io.vertx.core.Promise.promise();
  @Inject
  @CommandLineArguments
  String[] args;

  void onStart(@Observes StartupEvent event) {
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");
    String startupMessage = "In Production";
    startupMessage = "dev".equals(DodexUtil.getEnv()) ? "In Development" : startupMessage;

    logger.info("{}{}{}", ColorUtilConstants.BLUE_BOLD_BRIGHT, startupMessage, ColorUtilConstants.RESET);
    logger.info(String.format("%sDodex Server on Quarkus started%s", ColorUtilConstants.BLUE_BOLD_BRIGHT,
      ColorUtilConstants.RESET));
  }

  void onStop(@Observes ShutdownEvent event) {
    if (logger.isInfoEnabled()) {
      logger.info(String.format("%sStopping Quarkus%s", ColorUtilConstants.BLUE_BOLD_BRIGHT,
        ColorUtilConstants.RESET));
    }
    if (bridge != null) {
      bridge.close();
    }
  }

  // /* Just a way to gracefully shutdown the dev server */
  @Route(regex = "/dev[/]?|/dev/.*\\.html", methods = HttpMethod.GET)
  void dev(RoutingExchange ex) {
    io.vertx.core.http.HttpServerResponse response = ex.response();
    response.putHeader("content-type", "text/html");

    if (isProduction) {
      response.setStatusCode(404).end("not found");
    } else {
      Quarkus.asyncExit();
      response.end("<div><strong>Exited</strong></dev>");
    }
  }

  @Route(regex = "/test[/]?|/test/.*\\.html", methods = HttpMethod.GET)
  void test(RoutingExchange ex) {
    io.vertx.core.http.HttpServerResponse response = ex.response();
    response.putHeader("content-type", "text/html");

    if (isProduction) {
      response.setStatusCode(404).end("not found");
    } else {
      int length = ex.context().request().path().length();
      String path = ex.context().request().path();
      String file = length < 7 ? "test/index.html" : path.substring(1);

      response.sendFile(file);
    }
  }

  // dodex conflicts with websocket endpoint "/dodex" so using ddex
  @Route(regex = "/ddex[/]?|/ddex/.*\\.html", methods = HttpMethod.GET)
  public void prod(RoutingExchange ex) {
    io.vertx.core.http.HttpServerResponse response = ex.response(); // routingContext.response();
    response.putHeader("content-type", "text/html");

    if (isProduction) {
      int length = ex.context().request().path().length();
      String path = ex.context().request().path();
      String file = length < 7 ? "dodex/index.html" : path.substring(1).replace("ddex", "dodex");

      response.sendFile(file);
    } else {
      response.setStatusCode(404).end("<h3>Not found-try production mode</h3>");
    }
  }

//  @Route(regex = "/monitor[/]?|/monitor/.*\\.html", methods = HttpMethod.GET)
//  void monitor(RoutingExchange ex) {
//    io.vertx.core.http.HttpServerResponse response = ex.response();
//    response.putHeader("content-type", "text/html");
//
//    int length = ex.context().request().path().length();
//    String path = ex.context().request().path();
//    String file = length < 10 ? "monitor/index.html" : path.substring(1);
//
//    response.sendFile(file);
//  }

  // static content and Spa Routes
  public void init(@Observes Router router) {
    String value = Server.isProduction ? "prod" : "dev";
    FaviconHandler faviconHandler = FaviconHandler.create(vertx);

    if (isProduction) {
      DodexUtil.setEnv("prod");
      staticHandler.setCachingEnabled(true);
    } else {
      DodexUtil.setEnv(value);
      staticHandler.setCachingEnabled(false);
    }
        /*
            This will trap routing errors - but not the cause?
         */
//        router.route().failureHandler(ctx -> {
//            if (logger.isInfoEnabled()) {
//                logger.error(String.format("%sFAILURE in static route: %d%s", ColorUtilConstants.RED_BOLD_BRIGHT,
//                        ctx.statusCode(), ColorUtilConstants.RESET));
//                ctx.request().body().onItem().invoke(result ->
//                        logger.info("Result: "+result)).subscribeAsCompletionStage();
//            }
//            ctx.next();
//        });

    String readme = "/dist_test/react-fusebox";
    if (isProduction) {
      readme = "/dist/react-fusebox";
    }
//    router.route(readme + "/README.md").produces("text/markdown").handler(ctx -> {
//      HttpServerResponse response = ctx.response();
//      String acceptableContentType = ctx.getAcceptableContentType();
//      response.putHeader("content-type", acceptableContentType);
//      response.sendFile("dist_test/README.md").subscribeAsCompletionStage().isDone();
//    });

    router.route("/*").handler(StaticHandler.create())
      .produces("text/plain")
      .produces("text/html")
      .produces("text/markdown")
      .produces("image/*")
      .handler(staticHandler)
    ;
    router.route().handler(TimeoutHandler.create(2000));

    if ("dev".equals(DodexUtil.getEnv())) {
      router.route().handler(CorsHandler.create(/* Need ports 8089 & 9876 */)
        .allowedMethod(io.vertx.core.http.HttpMethod.GET));
    }

    if (routesHandicap == null) {
      routesHandicap = new GrpcRoutes(DodexUtil.getVertx(), router);
      routesHandicap.getVertxRouter(handicapPromise);
    }

    Server.getServerPromise().future().onItem().invoke(httpServer -> {
      try {
        setDodexRoute(/*httpServer,*/ router);
      } catch (InterruptedException | IOException | SQLException e) {
        e.printStackTrace();
      }
    }).subscribeAsCompletionStage().isDone();

    router.route().handler(faviconHandler);
  }

  public void setDodexRoute(/*HttpServer server,*/ Router router) throws InterruptedException, IOException, SQLException {
    DodexUtil du = new DodexUtil();
    String defaultDbName = du.getDefaultDb();
    Promise<Router> routerPromise = Promise.promise();

    logger.info("{}{}{}{}{}", ColorUtilConstants.PURPLE_BOLD_BRIGHT, "Using ", defaultDbName, " database",
      ColorUtilConstants.RESET);
    Server.setDefaultDbName(defaultDbName);

//    if (defaultDbName.equals("h2")) {
      handicapPromise.future().onSuccess(r -> {
        routerPromise.complete(router);
      });
//    }
    Server.setRoutesPromise(routerPromise);
  }

  public TcpEventBusBridge getBridge() {
    return bridge;
  }
}