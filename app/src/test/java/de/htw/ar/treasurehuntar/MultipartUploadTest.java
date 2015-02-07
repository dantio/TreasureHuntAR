package de.htw.ar.treasurehuntar;

import java.io.File;
import java.util.List;

/**
 * Created by Daniil on 07.02.2015.
 */
public class MultipartUploadTest {
    public static void main(String[] argv) {

        MultipartUtility multipart = new MultipartUtility(CachingActivity.POST_CACHE_URL, "UTF-8");

        multipart.addFormField("description", "Treasure-"); // TODO: Voice Recognition Text einfuegen
        multipart.addFormField("latitude", "52.754412");
        multipart.addFormField("longitude", "13.243977");
        multipart.addFormField("altitude", String.valueOf(AbstractArchitectActivity.UNKNOWN_ALTITUDE));

        multipart.addFilePart("Image", new File(imagePath));
        multipart.addFilePart("Audio", new File(audioPath));

        List<String> response = multipart.finish();
    }
}
