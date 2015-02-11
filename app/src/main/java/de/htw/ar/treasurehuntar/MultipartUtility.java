package de.htw.ar.treasurehuntar;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Daniil Tomilow
 */
public class MultipartUtility {

    private final String delimiter = "--";
    // creates a unique boundary based on time stamp
    private final String boundary = "----MPGL" + System.currentTimeMillis();

    private static final byte[] LINE_END = "\r\n".getBytes();
    private HttpURLConnection con;
    private String charset;
    private OutputStream os;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public MultipartUtility(String requestURL, String charset)
            throws IOException {
        this.charset = charset;

        con = (HttpURLConnection) (new URL(requestURL)).openConnection();
        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.connect();
        os = con.getOutputStream();
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) throws IOException {
        os.write((delimiter + boundary).getBytes());
        os.write(LINE_END);
        os.write("Content-Type: text/plain".getBytes());
        os.write(LINE_END);
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes());
        os.write(LINE_END);
        os.write(LINE_END);
        os.write(value.getBytes());
        os.write(LINE_END);
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        os.write((delimiter + boundary).getBytes());
        os.write(LINE_END);
        os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").getBytes());
        os.write(LINE_END);
        os.write(("Content-Type: application/octet-stream").getBytes());
        os.write(LINE_END);
        os.write(("Content-Transfer-Encoding: binary").getBytes());
        os.write(LINE_END);
        os.write(LINE_END);

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }

        inputStream.close();

        os.write(LINE_END);
    }

    public void finishMultipart() throws Exception {
        os.write((delimiter + boundary + delimiter).getBytes());
        os.write(LINE_END);
    }

    public String getResponse() throws Exception {
        finishMultipart();

        InputStream is = con.getInputStream();
        byte[] bytes = new byte[1024];
        StringBuilder builder = new StringBuilder();

        int numRead;
        while ((numRead = is.read(bytes)) != -1) {
            builder.append(new String(bytes, 0, numRead));
        }

        con.disconnect();

        return builder.toString();
    }
}