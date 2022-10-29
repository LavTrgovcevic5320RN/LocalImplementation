import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.FileMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class LocalImplementation implements Storage{

    FileMetaData rootFolder = null;


    @Override
    public void initialiseDirectory(String storageName, String path, int size, int maxFiles, String...st ) {
        File directory = new File(path + "\\" + storageName);
        System.out.println(directory.getPath());
        if (size != 0) {
            rootFolder = new FileMetaData(storageName, path, new Date(), new Date(), 1024, FileMetaData.Type.DIRECTORY);
        } else {
            rootFolder = new FileMetaData(storageName, path, new Date(), new Date(), size, FileMetaData.Type.DIRECTORY);
        }

        if (maxFiles != 0) {
            rootFolder.setMaxNumberOfFiles(maxFiles);
        }

        if (st.length > 0) {
            for (String s : st) {
                ArrayList<String> arrayList = (ArrayList<String>) rootFolder.getIllegalExtensions();
                arrayList.add(s);
                rootFolder.setIllegalExtensions(arrayList);
            }

            if (storageName.contains(".")) {
                String ekstenzija = storageName.substring(storageName.indexOf(".") + 1);
                for (String s : rootFolder.getIllegalExtensions()) {
//                System.out.println(s);
                    if (s.equals(ekstenzija))
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                }
            }

            if (!rootFolder.getIllegalExtensions().isEmpty()) {
                for (String s : rootFolder.getIllegalExtensions())
                    if (storageName.endsWith(s))
                        throw new FileException("greska sa fajlom");
            }
            directory.mkdir();
//        System.out.println(directory.isDirectory());
//        System.out.println(directory.getName());

        }
    }

    @Override
    public void setStorageSize(int i) {

    }

    @Override
    public void setMaxNumberOfFiles(int i) {

    }

    @Override
    public void create(String storageName, String path) {
        File root = new File(path + "/" + storageName);
        rootFolder = new FileMetaData(storageName, path, new Date(), new Date(), 0 , FileMetaData.Type.DIRECTORY);
        root.mkdirs();

        System.out.println(root.getPath() + " \n " + root.getName());
        rootFolder.setByteSize(10);

         
    }

    @Override
    public void create(String s, String s1, String s2) {

    }

    @Override
    public void uploadFiles(String s) throws InvalidConstraintException {

    }

    @Override
    public void delete(String s) {

    }

    @Override
    public void moveFile(String s, String s1, String s2) throws InvalidConstraintException {

    }

    @Override
    public void moveFiles(Collection<String> collection, String s, String s1) throws InvalidConstraintException {

    }

    @Override
    public void download(String s, String s1) {

    }

    @Override
    public void rename(String s, String s1) {

    }

    @Override
    public long getStorageByteSize() {
        return 0;
    }

    @Override
    public Collection<String> searchFilesInDirectory(String s) {
        return null;
    }

    @Override
    public Collection<String> searchFilesInAllDirectories(String s) {
        return null;
    }

    @Override
    public Collection<String> searchFilesInDirectoryAndBelow(String s) {
        return null;
    }

    @Override
    public Collection<String> searchFilesWithExtension(String s) {
        return null;
    }

    @Override
    public Collection<String> searchFilesThatContain(String s) {
        return null;
    }

    @Override
    public boolean searchIfFilesExist(String s, String... strings) {
        return false;
    }

    @Override
    public String searchFile(String s) {
        return null;
    }

    @Override
    public Date getCreationDate(String s) {
        return null;
    }

    @Override
    public Date getModificationDate(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByNameSorted(String s, Boolean aBoolean) {
        return null;
    }

    @Override
    public Collection<String> searchByDirectoryDateRange(Date date, Date date1, String s) {
        return null;
    }
}
