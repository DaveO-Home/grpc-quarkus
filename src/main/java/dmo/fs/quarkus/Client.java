package dmo.fs.quarkus;

import handicap.grpc.HandicapData;
import handicap.grpc.HandicapIndexGrpc;
import handicap.grpc.HandicapSetup;
import io.quarkus.grpc.GrpcClient;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/golfer")
public class Client {
    protected static final Logger logger = LoggerFactory.getLogger(Client.class.getName());

    @GrpcClient("handicap")
    HandicapIndexGrpc.HandicapIndexBlockingStub handicapClient;

    @GET
    @Path("/thegolfer")
    public String theGolfer() {
        JsonObject golfer = new JsonObject();
        golfer.put("pin", "test01");
        golfer.put("firstName", "Ace");
        golfer.put("lastName", "Ventura");
        golfer.put("country", "US");
        golfer.put("state", "CA");
        golfer.put("overlap", false);
        golfer.put("public", true);
        golfer.put("lastLogin", java.time.Instant.now().getEpochSecond());
        golfer.put("status", 0);
        golfer.put("course", "");
        golfer.put("tee", 1);
        golfer.put("teeDate", java.time.Instant.now().getEpochSecond());

        JsonObject login = new JsonObject();
        login.put("json", golfer);
        login.put("cmd", 8);
        login.put("message", "Test");

        HandicapData data = handicapClient.getGolfer(HandicapSetup.newBuilder().setJson(login.toBuffer().toString()).build());

        return data.getJson();
    }
}