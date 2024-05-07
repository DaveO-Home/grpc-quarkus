# grpc-quarkus

Debug Quarkus gRPC.

## Execute Application

1. Separate terminal: execute `..../grpc-quarkus/start.envoy`
2. Execute `..../grpc-quarkus/gradlew quarkusDev`
3. Browse URL `localhost:8089/handicap.html`.
4. Enter pin, first and last name and Click `Login` button.
5. View warning in terminal console.

## Points of Possible Interest 

1. src/main/proto
2. src/main/kotlin/routes/GrpcRoutes
3.

### Kotlin, gRPC Web Application

* This web application can be used to maintain golfer played courses and scores and to calculate a handicap index. The application has many moving parts from the ___envoy___ proxy server to ___kotlin___, ___protobuf___, ___gRPC___, ___jooq___ and __code generator__, ___bootstrap___, ___webpack___, ___esbuild___, ___gradle___, ___java___ and ___javascript___.

  See documentation at: <https://github.com/DaveO-Home/dodex-quarkus/blob/master/handicap/README.md>

## ChangeLog

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the GNU License - see the [LICENSE](LICENSE) file for details
