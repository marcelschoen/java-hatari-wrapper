package games.play4ever.retrodev.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    public void zipDir(String dirName, String nameZipFile) throws IOException {
        ZipOutputStream zip = null;
        FileOutputStream fW = null;
        fW = new FileOutputStream(nameZipFile);
        zip = new ZipOutputStream(fW);
        addFolderToZip("", dirName, zip);
        zip.close();
        fW.close();
    }

    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws IOException {
        File folder = new File(srcFolder);
        System.out.println("> folder: " + srcFolder + " / files: " + folder.list().length);
        if (folder.list().length == 0) {
            addFileToZip(path, srcFolder, zip, true);
        } else {
            System.out.println("> Path: " + path);
            for (String fileName : folder.list()) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip, false);
                } else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip, false);
                }
            }
        }
    }

    private void addFileToZip(String path, String srcFile, ZipOutputStream zip, boolean flag) throws IOException {
        File folder = new File(srcFile);
        if (flag) {
//            zip.putNextEntry(new ZipEntry(path + "/" +folder.getName() + "/"));
        } else {
            if (folder.isDirectory()) {
//                addFolderToZip(path, srcFile, zip);
            } else {
                byte[] buf = new byte[1024];
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                System.out.println("> path: " + path);
                zip.putNextEntry(new ZipEntry(folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }
}