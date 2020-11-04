package io.github.kimmking.gateway.router;

import java.util.List;
import java.util.Random;

public class HttpEndpointRouterImpl implements HttpEndpointRouter {

    @Override
    public String route(List<String> endpoints) {
        Random random = new Random();
        int i = random.nextInt(endpoints.size());
        return endpoints.get(i);
    }
}
