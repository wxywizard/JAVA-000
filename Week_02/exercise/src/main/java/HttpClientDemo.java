import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class HttpClientDemo {
    public static void main(String[] args) {
        getRequest();
    }

    private static void getRequest(){
        OkHttpClient c = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:8803")
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
                response.close(); // 别忘记关闭
            }
        }
    }
}

