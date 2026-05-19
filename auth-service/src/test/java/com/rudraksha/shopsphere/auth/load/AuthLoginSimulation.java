package com.rudraksha.shopsphere.auth.load;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class AuthLoginSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8081") // Target the auth-service directly on port 8081
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder scn = scenario("Auth Service Load Test - Login Flow")
            .exec(http("Login Request")
                    .post("/auth/login")
                    .body(StringBody("{\"email\":\"user@example.com\",\"password\":\"SecurePassword123!\"}"))
                    .check(status().is(200))
            );

    public AuthLoginSimulation() {
        this.setUp(
                scn.injectOpen(
                        nothingFor(2),             // 2 seconds warm-up pause
                        rampUsers(10).during(5),    // Ramp 10 concurrent logins over 5 seconds
                        constantUsersPerSec(5).during(10) // Maintain 5 requests/sec for 10 seconds
                )
        ).protocols(httpProtocol);
    }
}
