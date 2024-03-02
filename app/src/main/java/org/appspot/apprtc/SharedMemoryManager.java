package org.appspot.apprtc;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SharedMemoryManager {
    private String TAG = "ShardMemoryManager";
    private MappedByteBuffer mappedByteBuffer;
    private FileChannel fileChannel;
    private String filePath;

    private Context context; // Add a context field

    // Modify the constructor to accept a Context
    public SharedMemoryManager(Context context) {
        this.context = context;
        String fileName = "shared_memory_file.txt";
        // Use the context to construct the file path
        this.filePath = new File(context.getFilesDir(), fileName).getAbsolutePath();
    }

    public void setupSharedMemory() {
        try {
            RandomAccessFile sharedFile = new RandomAccessFile(filePath, "rw");
            fileChannel = sharedFile.getChannel();
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024); // Size of 1KB for example
            Log.i(TAG, "mmap in " + filePath);
        } catch (Exception e) {
            Log.i(TAG, "mmap failed " + e);
            e.printStackTrace();
        }
    }

    public void writeToJsonFile(String filePath, Object dataObject) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(dataObject);

        try (RandomAccessFile file = new RandomAccessFile(filePath, "rw");
             FileChannel channel = file.getChannel()) {

            byte[] bytes = jsonString.getBytes();
            mappedByteBuffer.clear();
            mappedByteBuffer .put(bytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFromJsonFile() {
        try {
            // Rewind the buffer to read from the beginning
            mappedByteBuffer.rewind();

            // Assuming the JSON data is not larger than the buffer size, read it into a byte array
            byte[] bytes = new byte[mappedByteBuffer.limit()];
            mappedByteBuffer.get(bytes);

            // Convert the byte array into a string
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void close() {
        try {
            fileChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


