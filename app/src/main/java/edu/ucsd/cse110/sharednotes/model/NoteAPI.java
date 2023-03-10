package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    // TOD: Implement the API using OkHttp!
    // TOD: - getNote (maybe getNoteAsync)
    // TOD: - putNote (don't need putNotAsync, probably)
    // TOD: Read the docs: https://square.github.io/okhttp/
    // TOD: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            System.out.println(body);
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }

    @WorkerThread
    public Note getNote(String title){
        Note newNote;
        String newTitle;
        String newMessage;
        String newVersion;
        String[] bodyLines;
        long actualVersion;

        String encodedTitle = title.replace(" ", "%20");
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();

            if (body.equals("{\"detail\":\"Note not found.\"}")) {
                Log.i("EMPTY", "no note in server");
                return null;
            }

            bodyLines = body.split("[,]", 0);
            newTitle = bodyLines[0];
            newMessage = bodyLines[1];
            newVersion = bodyLines[2];

            newTitle = newTitle.substring(10, newTitle.length() - 1);
            newMessage = newMessage.substring(11, newMessage.length() - 1);
            newVersion = newVersion.substring(10, newVersion.length() - 1);

            actualVersion = Long.parseLong(newVersion);

            newNote = new Note(newTitle, newMessage, actualVersion);

            Log.i("ECHO", newTitle + ", " + newMessage + ", " + actualVersion);
            return newNote;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @WorkerThread
    public void putNote(Note note) {
        String bodyString = "{\"content\":\"" + note.content + "\",\"version\":" + note.updatedAt + "}";
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(bodyString, JSON);
        String title = note.title;
        String encodedTitle = title.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .method("PUT", requestBody)
                .build();

        try (var response = client.newCall(request).execute()) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}
