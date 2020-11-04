package io.github.kimmking.gateway.outbound.okhttp;

import io.github.kimmking.gateway.filter.HttpRequestFilterImpl;
import io.github.kimmking.gateway.outbound.httpclient4.NamedThreadFactory;
import io.github.kimmking.gateway.router.HttpEndpointRouter;
import io.github.kimmking.gateway.router.HttpEndpointRouterImpl;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OkhttpOutboundHandler {

    private CloseableHttpAsyncClient httpclient;
    private ExecutorService proxyService;
    private String backendUrl;

    public OkhttpOutboundHandler(String backendUrl){
        this.backendUrl = backendUrl.endsWith("/")?backendUrl.substring(0,backendUrl.length()-1):backendUrl;
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();//.DiscardPolicy();
        proxyService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"), handler);

        IOReactorConfig ioConfig = IOReactorConfig.custom()
                .setConnectTimeout(1000)
                .setSoTimeout(1000)
                .setIoThreadCount(cores)
                .setRcvBufSize(32 * 1024)
                .build();

        httpclient = HttpAsyncClients.custom().setMaxConnTotal(40)
                .setMaxConnPerRoute(8)
                .setDefaultIOReactorConfig(ioConfig)
                .setKeepAliveStrategy((response,context) -> 6000)
                .build();
        httpclient.start();
    }

    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        final String url = this.backendUrl + fullRequest.uri();
        proxyService.submit(()->fetchGet(fullRequest, ctx, url));
    }

    private void fetchGet(final FullHttpRequest inbound, final ChannelHandlerContext ctx, final String url) {
        //从filter获取请求头
        HttpRequestFilterImpl httpRequestFilter = new HttpRequestFilterImpl();
        httpRequestFilter.filter(inbound,ctx);
        String nio = inbound.headers().get("nio");

        //随机取url放入请求体
        HttpEndpointRouterImpl httpEndpointRouter = new HttpEndpointRouterImpl();
        String route = httpEndpointRouter.route(Arrays.asList("http://127.0.0.1:8803" + inbound.uri(), "http://127.0.0.1:8802" + inbound.uri()));

        OkHttpClient c = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("nio",nio)
                .get() // get post put 等
                .build();
        Call call = c.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            if (response.isSuccessful()) {
                System.out.println("response.code()=="+response.code());
                System.out.println("response.message()=="+response.message());
                System.out.println("res=="+response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    private void handleResponse(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, final HttpResponse endpointResponse) throws Exception {
        FullHttpResponse response = null;
        try {
//            String value = "hello,kimmking";
//            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(value.getBytes("UTF-8")));
//            response.headers().set("Content-Type", "application/json");
//            response.headers().setInt("Content-Length", response.content().readableBytes());


            byte[] body = EntityUtils.toByteArray(endpointResponse.getEntity());
//            System.out.println(new String(body));
//            System.out.println(body.length);

            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
            response.headers().set("Content-Type", "application/json");
            response.headers().setInt("Content-Length", Integer.parseInt(endpointResponse.getFirstHeader("Content-Length").getValue()));

//            for (Header e : endpointResponse.getAllHeaders()) {
//                //response.headers().set(e.getName(),e.getValue());
//                System.out.println(e.getName() + " => " + e.getValue());
//            }

        } catch (Exception e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    //response.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(response);
                }
            }
            ctx.flush();
            //ctx.close();
        }

    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void getRequest(){
        OkHttpClient c = new OkHttpClient();
        Request request = new Request.Builder()
                .url(this.backendUrl)
                .get() // get post put 等
                .build();
        Call call = c.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            if (response.isSuccessful()) {
                System.out.println("response.code()=="+response.code());
                System.out.println("response.message()=="+response.message());
                System.out.println("res=="+response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

}
