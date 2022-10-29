import exceptions.InvalidConstraintException;
import storage.StorageConstraint;

import java.io.*;
import java.util.*;

public class LocalImplementation extends Storage{
    File rootDirectory;
    @Override
    public void initialiseDirectory(String storageName, String path, int size, int maxFiles, String... bannedExtensions) {
        File directory = new File(path + "\\" + storageName);
        rootDirectory = directory;
        System.out.println(directory.getPath());
        storageConstraint = new StorageConstraint();
        if (size >= 0) {
            storageConstraint.setByteSizeQuota(size);
        }

        storageConstraint.getMaxNumberOfFiles().put("#", maxFiles >= 0 ? maxFiles : -1);


        if (bannedExtensions.length > 0) {
            storageConstraint.getIllegalExtensions().addAll(Arrays.asList(bannedExtensions));
            System.out.println(storageConstraint.getIllegalExtensions());
        }
        if(!directory.mkdirs()) {
            System.err.println("MKDIR FAILED!");
        } else {
            writeConfiguration();
        }
//
//            if (storageName.contains(".")) {
//                String ekstenzija = storageName.substring(storageName.indexOf(".") + 1);
//                for (String s : rootFolder.getIllegalExtensions()) {
////                System.out.println(s);
//                    if (s.equals(ekstenzija))
//                        try {
//                            throw new Exception();
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                }
//            }
//
//            if (!rootFolder.getIllegalExtensions().isEmpty()) {
//                for (String s : rootFolder.getIllegalExtensions())
//                    if (storageName.endsWith(s))
//                        throw new FileException("greska sa fajlom");
//            }
//        System.out.println(directory.isDirectory());
//        System.out.println(directory.getName());
    }

    private void writeConfiguration() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(rootDirectory, "directory.conf"))))  {
            oos.writeObject(storageConstraint);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openDirectory(String s) {
        File directory = new File(s);
        rootDirectory = directory;
        System.out.println(directory.getPath());
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(directory, "directory.conf"))))  {
            storageConstraint = (StorageConstraint) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println(storageConstraint);
        System.out.println(storageConstraint.getByteSizeQuota());
        System.out.println(storageConstraint.getMaxNumberOfFiles());
        System.out.println(storageConstraint.getIllegalExtensions());
    }

    @Override
    public void setStorageSize(int i) {

    }

    @Override
    public void create(String directoryName, String path) {
        String s = path.replaceAll(".*#/*", "") + directoryName;
        File newDir = new File(rootDirectory, s);
        System.out.println(newDir.mkdirs());
        storageConstraint.getMaxNumberOfFiles().put(path+directoryName, -1);
        writeConfiguration();
    }

    @Override
    public void create(String s, String s1, int i) {

    }

    @Override
    public void setMaxFiles(String s, int i) {

    }

    @Override
    public void create(String s, String s1, String s2) {

    }

    @Override
    public void uploadFile(String s, String s1) throws InvalidConstraintException {

    }

    @Override
    public void uploadFiles(String s, String s1, String... strings) throws InvalidConstraintException {

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
