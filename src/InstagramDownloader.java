import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Objects;

static File file = new File("resources");
static ArrayList<String> ids = new ArrayList<>();
static ArrayList<String> urls = new ArrayList<>();
JsonArray postIds = new JsonArray();
JsonArray postIds2 = new JsonArray();
String next25Posts = "";
Boolean isNext = true;

public void main(String[] args) throws IOException {
    // change userId
    long userId = 0;

    // change token
    String token =
            "";
    String first25Posts =
            STR."https://graph.instagram.com/v20.0/\{userId}/media?fields=id,caption,media_url,media_type,timestamp&access_token=\{token}";

    // purge assets folder for fresh directory
    purgeDirectory(file);

    // get first 25 instagram post ids, returns "data" JsonArray
    getPostIds(first25Posts);

    // get next 25 instagram post ids and adds them to postId JsonArray
    getNextPostIds(next25Posts);

    // adds carousel media ids to ids ArrayList
    sortIds(postIds);
    //System.out.println(ids);

    // gets carousel post media
    getCarouselPosts(ids, token);

    // loop through urls and save images
    arrayImageDownloader();
}

public void purgeDirectory(File dir) {
    for (File file : Objects.requireNonNull(dir.listFiles())) {
        if (file.isDirectory())
            purgeDirectory(file);
        file.delete();
    }
}

public static void arrayImageDownloader() throws IOException {
    file.mkdir();
    for (int i = 0; i < urls.size(); i++) {
        String url = urls.get(i);
        String destination = STR."resources//\{i}.jpg";
        saveImage(url, destination);
        System.out.println(i);
    }
    System.out.println("done");
}

public static void saveImage(String imageUrl, String destinationFile) throws IOException {
    URL url = new URL(imageUrl.replace("\"", ""));
    InputStream is = url.openStream();
    OutputStream os = new FileOutputStream(destinationFile);

    byte[] b = new byte[2048];
    int length;

    while ((length = is.read(b)) != -1) {
        os.write(b, 0, length);
    }

    is.close();
    os.close();
}

public void getCarouselPosts(ArrayList<String> carouselIds, String token) throws IOException {
    for (String postId : carouselIds) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STR."https://graph.instagram.com/\{postId.substring(1, postId.length() - 1)}/children?fields=id,media_url&access_token=\{token}"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        assert response != null;
        JsonObject carouselData = new Gson().fromJson(response.body(), JsonObject.class);

        // returns multiple arrays containing "id" and "media_url" keys
        // loops through arrays and add "media_url" to urls ArrayList
        JsonArray temp = carouselData.get("data").getAsJsonArray();
        for (int i = 0; i < temp.size(); i++) {
            urls.add(String.valueOf(temp.get(i).getAsJsonObject().get("media_url")));
        }
    }
}


public void sortIds(JsonArray idArray) {
    for (int i = 0; i < idArray.size(); i++) {
        ids.add(String.valueOf(idArray.get(i).getAsJsonObject().get("id")));
    }
}

public void getNextPostIds(String url) {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = null;
    try {
        response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
    assert response != null;
    JsonObject postData = new Gson().fromJson(response.body(), JsonObject.class);
    // full page of posts == postIds.size() = 25
    postIds2 = postData.get("data").getAsJsonArray();
    for (int i = 0; i < postIds2.size(); i++) {
        postIds.add(postIds2.get(i));
    }

    Boolean isNextTrue = postData.get("paging").getAsJsonObject().keySet().contains("next");
    if (isNext == isNextTrue) {
        next25Posts = postData.get("paging").getAsJsonObject().get("next").toString().replace("\"", "");
        getNextPostIds(next25Posts);
    } else {
        isNext = false;
    }
}

public void getPostIds(String url) {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = null;
    try {
        response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
    }
    assert response != null;
    JsonObject postData = new Gson().fromJson(response.body(), JsonObject.class);
    // postIds.size() = 25
    postIds = (JsonArray) postData.get("data");

    // url string to get the next 25 posts
    next25Posts = postData.get("paging").getAsJsonObject().get("next").toString().replace("\"", "");
}
