import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.StorageConstraint;

import java.io.*;
import java.nio.file.*;
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
        create(directoryName, path, -1);
    }

    private boolean checkIfAdditionValid(String path, int add) {
        String s = path.replaceAll(".*#/*", "");
        File dir = new File(rootDirectory, s);
        if(dir.isDirectory()) {
            int noFiles = dir.list().length;
            int allowedFiles;
            allowedFiles = storageConstraint.getMaxNumberOfFiles().get(path);
            if(allowedFiles < 0) return true;
            return (allowedFiles >= noFiles + add);
        }
        throw new RuntimeException("Path not directory");
    }

    // returns false if extension illegal. true if legal
    private boolean checkExtension(String file) {
        String ext = file.substring(file.lastIndexOf("."));
        return (!storageConstraint.getIllegalExtensions().contains(ext));
    }

    @Override
    public void create(String directoryName, String path, int i) {
        String s = path.replaceAll(".*#/*", "") + directoryName;
        if(checkIfAdditionValid(s, 1)) {
            File newDir = new File(rootDirectory, s);
            System.out.println(newDir.mkdirs());
            storageConstraint.getMaxNumberOfFiles().put(path + "\\" + directoryName, i);
            writeConfiguration();
        } else try {
            throw new InvalidConstraintException("Directory full");
        } catch (InvalidConstraintException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setMaxFiles(String s, int i) {
        // postaviti u constraint mapi vrednost na i
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
    public void delete(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();
        for(File file : files){
            System.out.println(file.getName());
            file.delete();
        }
    }

    @Override
    public void moveFile(String destination, String... sources) throws InvalidConstraintException, FileNotFoundException {
        String fullPath = rootDirectory + "/" + destination;
        File destinationFolder = new File(fullPath);

        if(!destinationFolder.exists())
            throw new FileNotFoundException();

        if(storageConstraint.getMaxNumberOfFiles().containsKey("fullpath")){
            int numberOfFiles = destinationFolder.listFiles().length;
            if(numberOfFiles + sources.length > storageConstraint.getMaxNumberOfFiles().get(fullPath))
                throw new FileException("preko limita");
        }

        for(String source: sources) {

            source = rootDirectory.getPath() + "/" + source;
            System.out.println(source);
            File sourceFile = new File(source);

            // Provera da li postoji fajl na prosledjenoj putanji:
            if(!sourceFile.exists()) {
                throw new FileNotFoundException();
            }

            Path result = null;

            try {
                String resultingPath = rootDirectory.getPath() + "/" + destination + "/" + Paths.get(source).getFileName();
                result = Files.move(Paths.get(source), Paths.get(resultingPath), StandardCopyOption.REPLACE_EXISTING);
            } catch (NoSuchFileException e1) {
                e1.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(result == null)
                try {
                    throw new Exception("Operacija se nije izvrsila");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }


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
        String p = s.replaceAll(".*#/*", "");
        File dir = new File(rootDirectory, p);
        if(dir.isDirectory()) {
           return Arrays.asList(dir.list());
        }
        return new ArrayList<>();
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
