package edu.ucsd.cse110.sharednotes.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NoteRepository {
    private final NoteDao dao;




    private MutableLiveData<Note> noteLiveData;

    private MediatorLiveData<Note> noteData;

    private ScheduledFuture<?> futureUpdate;

    public NoteRepository(NoteDao dao) {
        this.dao = dao;
    }

    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     *
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        var note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = theirNote -> {
            var ourNote = note.getValue();
            if (theirNote == null) return; // do nothing
            if (ourNote == null || ourNote.updatedAt < theirNote.updatedAt) {
                upsertLocal(theirNote);
            }
        };

        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        note.updatedAt = note.updatedAt + 1;
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();
    }

    public void upsertLocal(Note note) {
        //here note.updatedAt = System.currentTimeMillis();
        dao.upsert(note);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // TOD: Implement getRemote!
        // TOD: Set up polling background thread (MutableLiveData?)
        // TOD: Refer to TimerService from https://github.com/DylanLukes/CSE-110-WI23-Demo5-V2.

        // Start by fetching the note from the server _once_ and feeding it into MutableLiveData.
        // Then, set up a background thread that will poll the server every 3 seconds.

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.
        // You don't need to worry about killing background threads.

        NoteAPI noteAPI = new NoteAPI();
        Note note;

        note = noteAPI.getNote(title);

        noteLiveData = new MutableLiveData<>();
        noteLiveData.postValue(note);
        registerUpdateListener(noteAPI, title);

        noteData = new MediatorLiveData<>();
        noteData.addSource(noteLiveData, noteData::postValue);

        return noteData;

        //throw new UnsupportedOperationException("Not implemented yet");
    }

    private void registerUpdateListener(NoteAPI noteAPI, String title) {
        var executor = Executors.newSingleThreadScheduledExecutor();

        futureUpdate = executor.scheduleAtFixedRate(() -> {
            noteLiveData.postValue(noteAPI.getNote(title));
        }, 0, 3000, TimeUnit.MILLISECONDS);

    }

    public void upsertRemote(Note note) {
        // TOD: Implement upsertRemote!
        //throw new UnsupportedOperationException("Not implemented yet");
        ExecutorService backgroundThreadExecutor = Executors.newSingleThreadExecutor();
        Future<?> future;

        NoteAPI noteAPI = new NoteAPI();

        future = backgroundThreadExecutor.submit(() -> {
            noteAPI.putNote(note);
        });
    }
}
