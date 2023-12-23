package info.cemu.Cemu;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FileUtil {
    private static final String PATH_SEPARATOR_ENCODED = "%2F";
    private static final String PATH_SEPARATOR_DECODED = "/";
    private static final String COLON_ENCODED = "%3A";
    private static final String MODE = "r";

    private static CemuApplication cemuApplication;

    public static void setCemuApplication(CemuApplication cemuApplication) {
        FileUtil.cemuApplication = cemuApplication;
    }

    private static String toCppPath(Uri uri) {
        String uriPath = uri.toString();
        int delimiterPos = uriPath.lastIndexOf(COLON_ENCODED);
        return uriPath.substring(0, delimiterPos) + uriPath.substring(delimiterPos).replace(PATH_SEPARATOR_ENCODED, PATH_SEPARATOR_DECODED);
    }

    private static Uri fromCppPath(String cppPath) {
        int delimiterPos = cppPath.lastIndexOf(COLON_ENCODED);
        return Uri.parse(cppPath.substring(0, delimiterPos) + cppPath.substring(delimiterPos).replace(PATH_SEPARATOR_DECODED, PATH_SEPARATOR_ENCODED));
    }

    public static int openContentUri(String uri) {
        try {
            if (!exists(uri)) {
                return -1;
            }
            ParcelFileDescriptor parcelFileDescriptor = cemuApplication.getApplicationContext().getContentResolver().openFileDescriptor(fromCppPath(uri), MODE);
            if (parcelFileDescriptor != null) {
                int fd = parcelFileDescriptor.detachFd();
                parcelFileDescriptor.close();
                return fd;
            }
        } catch (Exception e) {
            Log.e("FileUtil", "Cannot open content uri, error: " + e.getMessage());
        }
        return -1;
    }

    public static String[] listFiles(String uri) {
        ArrayList<String> files = new ArrayList<>();
        Uri directoryUri = fromCppPath(uri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri));
        try (Cursor cursor = cemuApplication.getApplicationContext().getContentResolver().query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String documentId = cursor.getString(0);
                Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId);
                files.add(toCppPath(documentUri));
            }
        } catch (Exception e) {
            Log.e("FileUtil", "Cannot list files: " + e.getMessage());
        }
        String[] filesArray = new String[files.size()];
        filesArray = files.toArray(filesArray);
        return filesArray;
    }

    public static boolean isDirectory(String uri) {
        String mimeType = cemuApplication.getApplicationContext().getContentResolver().getType(fromCppPath(uri));
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    public static boolean isFile(String uri) {
        return !isDirectory(uri);
    }

    public static boolean exists(String uri) {
        try (Cursor cursor = cemuApplication.getApplicationContext().getContentResolver().query(fromCppPath(uri), null, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.e("FileUtil", "Failed checking if file exists: " + e.getMessage());
            return false;
        }
    }
}
