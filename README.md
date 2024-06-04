# grpc-quarkus

Debug Quarkus gRPC.

## Execute Application

**Note:** Kotlin & Kotlin plugin are upgraded to 2.0.0

1. Download `https://github.com/DaveO-Home/grpc-quarkus`
2. cd to `.../grpc-quarkus`
3. execute `./gradlew quarkusDev`

### Java Client

1. Once Quarkus is started, in a browser run `localhost:8089/golfer/thegolfer`
2. Json should be returned with a `"message": "golfer not found"` entry.
3. View warning message in terminal console, 
``` 
  [io.quar.grpc.runt.supp.cont.GrpcRequestContextGrpcInterceptor-46]
    Request context already active when gRPC request started
```

### Javascript Client

1. Download and install Envoy; `https://www.envoyproxy.io/docs/envoy/latest/start/install` 
2. Separate terminal: execute `..../grpc-quarkus/start.envoy`
3. Execute `..../grpc-quarkus/gradlew quarkusDev`
4. Browse URL `localhost:8089/handicap.html`.
5. Enter pin, first and last name and Click `Login` button.
6. View warning in terminal console.

## Points of Possible Interest 

1. src/main/proto
2. src/main/kotlin/handicap/golf/routes/GrpcRoutes.kt
3. src/main/java/dmo/fs/quarkus/Client.java

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the GNU License - see the [LICENSE](LICENSE) file for details
