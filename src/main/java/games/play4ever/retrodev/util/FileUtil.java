package games.play4ever.retrodev.util;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * File handling / I/O utility. Could be replaced by Apache IO commons etc.,
 * but I prefer to avoid external dependencies.
 *
 * @author Marcel Schoen
 */
public class FileUtil {

    /**
     * Copies a directory, and all its contents, including subdirectories.
     *
     * @param sourceDirectory The source directory to copy.
     * @param targetDirectory The target directory to create.
     */
    public static void copyDirectory(File sourceDirectory, File targetDirectory) {
        if (sourceDirectory.isDirectory()) {
            targetDirectory.mkdirs();
            for (File entry : sourceDirectory.listFiles()) {
                if (entry.isDirectory()) {
                    copyDirectory(entry, new File(targetDirectory, entry.getName()));
                } else {
                    copyFileTo(entry, new File(targetDirectory, entry.getName()));
                }
            }
        }
    }

    /**
     * Copies a file.
     *
     * @param source The source file to copy.
     * @param target The target file.
     */
    public static void copyFileTo(File source, File target) {
        try {
            InputStream tosImg = new FileInputStream(source);
            byte[] buffer = new byte[4096];
            FileOutputStream out = new FileOutputStream(target);
            int len;
            while ((len = tosImg.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.close();
            System.out.println(">> Copied file " + source.getAbsolutePath() + " to: " + target.getAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to copy file '" + source.getAbsolutePath()
                    + "' to '" + target.getAbsolutePath() + "': " + ex, ex);
        }
    }

    /**
     * Unpacks the given Zip input stream into the given working directory.
     *
     * @param tempDir
     * @param zipFile
     * @throws IOException
     */
    public static void unpackZipXX(File tempDir, InputStream zipFile) throws IOException {
        File destDir = tempDir;
        System.out.println("> Unpack Zip file to: " + tempDir.getAbsolutePath());
        byte[] buffer = new byte[4096];
        ZipInputStream zis = new ZipInputStream(zipFile);
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            System.out.println(">>> zip entry: " + zipEntry.getName());
            File newFile = extractFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        System.out.println(">> Unpacked zip into directory " + destDir.getAbsolutePath());
    }

    public static void unpackZip(File tempDir, InputStream zipFileIn) {
        File destDir = tempDir;
        System.out.println("> Unpack Zip file to: " + tempDir.getAbsolutePath());
        ZipInputStream zipIs = null;
        ZipEntry zEntry = null;
        try {
            zipIs = new ZipInputStream(zipFileIn);
            while ((zEntry = zipIs.getNextEntry()) != null) {
                System.out.println("> entry: " + zEntry.getName() + " / " + zEntry.isDirectory());
                if (!zEntry.isDirectory()) {
                    try {
                        byte[] tmp = new byte[4 * 1024];
                        FileOutputStream fos = null;
                        File outFile = new File(destDir, zEntry.getName());
                        System.out.println("Extracting file to " + outFile.getAbsolutePath());
                        fos = new FileOutputStream(outFile);
                        int size = 0;
                        while ((size = zipIs.read(tmp)) != -1) {
                            fos.write(tmp, 0, size);
                        }
                        fos.flush();
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            zipIs.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Extracts an entry from the Hatari emulator zip archive.
     *
     * @param destinationDir The target build directory.
     * @param zipEntry       The entry to extract.
     * @return The file reference to the extracted file.
     * @throws IOException If the extraction failed.
     */
    public static File extractFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Recursive deletion of directory with all contents.
     *
     * @param file The directory to delete.
     */
    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }
}
