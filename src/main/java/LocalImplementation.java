import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.StorageConstraint;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LocalImplementation extends Storage{
    private static LocalImplementation instance = null;
    private static File rootDirectory;

    public static LocalImplementation getInstance() {
        if (instance == null)
            instance = new LocalImplementation();
        return instance;
    }

    @Override
    public void initialiseDirectory(String storageName, String path, int size, int maxFiles, String... bannedExtensions) {
        File directory = new File(path + "\\" + storageName);
        if(directory.exists() && directory.isDirectory() && directory.list().length > 0) throw new FileException("Storage can not be initialized, target directory exists");
        rootDirectory = directory;
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
            System.out.println("Kreirano skladiste na " + directory.getPath());
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

    private String getAbsolutePath(String relative) {
        relative = relative.trim();
        //if(!relative.startsWith("#")) throw new FileException("Invalid path");
        relative = relative.replaceAll("\\\\", "/");
        return relative.replaceFirst("(#/*)", rootDirectory.getAbsolutePath().replaceAll("\\\\", "/") + "/");
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
    }

    @Override
    public void setStorageSize(int i) {

    }

    private boolean checkIfAdditionValid(String path, int add) {
        String s = getAbsolutePath(path);
        File dir = new File(s);
        if(dir.isDirectory()) {
            int noFiles = dir.list().length;
            int allowedFiles;
            path = path.replaceFirst("[/\\\\]$", "");
            allowedFiles = storageConstraint.getMaxNumberOfFiles().get(path);
            if(allowedFiles < 0) return true;
            return (allowedFiles >= noFiles + add);
        }
        throw new RuntimeException("Path " + dir + " not directory");
    }

    // returns false if extension illegal. true if legal
    private boolean checkExtension(String file) {
        String ext = file.substring(file.lastIndexOf("."));
        return (!storageConstraint.getIllegalExtensions().contains(ext));
    }

    @Override
    public void create(String directoryName, String path) {
        create(directoryName, path, -1);
    }

    @Override
    public void create(String directoryName, String path, int i) {
        String s = getAbsolutePath(path + directoryName);
//        if(checkIfAdditionValid(path, 1)) {
            File newDir = new File(s);
            System.out.println(newDir.mkdirs());
            storageConstraint.getMaxNumberOfFiles().put(path +  directoryName, i);
            writeConfiguration();
//        } else try {
//            throw new InvalidConstraintException("Directory full");
//        } catch (InvalidConstraintException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void setMaxFiles(String s, int i) {
        // postaviti u constraint mapi vrednost na i
    }

    @Override
    public void uploadFile(String s, String s1) throws InvalidConstraintException {

    }

    @Override
    public void uploadFiles(String s, String s1, String... strings) throws InvalidConstraintException {

    }

    /**
     * Deletes a provided file. If file is a directory, it will empty it before deletion
     * @implNote this method is dangerous since it recursively deletes content and subdirectories!
     * @param path target to delete
     */
    @Override
    public void delete(String path) {
        String s = getAbsolutePath(path);
        File file = new File(s);
        if(file.exists()) if(recursiveDelete(file)) writeConfiguration();
    }

    private boolean recursiveDelete(File fileOrDirectory) {
        boolean configurationWriteNeeded = false;
        if(fileOrDirectory.isDirectory()) {
            for(File file : fileOrDirectory.listFiles()) {
                recursiveDelete(file);
            }
            storageConstraint.getMaxNumberOfFiles().remove(getRelativePathOfDirectory(fileOrDirectory));
            configurationWriteNeeded = true;
        }
        fileOrDirectory.delete();
        return configurationWriteNeeded;
    }

    private String getRelativePathOfDirectory(File path) {
        String normPath = path.getAbsolutePath().replace(rootDirectory.getAbsolutePath(), "#/").replaceAll("(/\\\\|\\\\/)", "/");
        System.out.println(normPath);
        return normPath;
    }

    @Override
    public void moveFiles(String destination, String... sources) throws InvalidConstraintException, FileNotFoundException {
        String fullPath = getAbsolutePath(destination);
        File destinationFolder = new File(fullPath);

        if(!destinationFolder.exists())
            throw new FileNotFoundException();

        List<String> list = new ArrayList<>();
        for(String source: sources)
            if(checkExtension(source))
                list.add(source);

//        if(!checkIfAdditionValid(destination, list.size())) throw new FileException("preko limita");

        for(String source: list) {
            source = getAbsolutePath(source);
            System.out.println(source);
            File sourceFile = new File(source);

            // Provera da li postoji fajl na prosledjenoj putanji:
            if(!sourceFile.exists()) {
                throw new FileNotFoundException();
            }

            Path result = null;
            try {
                String resultingPath = rootDirectory.getPath() + "/" + destination + "/" + Paths.get(source).getFileName();
                resultingPath = resultingPath.replaceFirst("#[/\\\\]", "");
                System.out.println(resultingPath);
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
    public void createExpanded(String path, String pattern) {
        List<String> list = BraceExpansion.expand(pattern);


    }

    @Override
    public void download(String s, String s1) {

    }

    @Override
    public void rename(String newName, String path) {
        File fileOldName = new File(path);
        File fileNewName = new File(fileOldName.getParentFile().getPath() + "\\" + newName);
//        System.out.println(fileOldName.getPath());
//        System.out.println(fileNewName.getPath());

        System.out.println("promenjeno ime je:" + fileOldName.renameTo(fileNewName));
    }

    @Override
    public long getStorageByteSize() {
        return 0;
    }

    @Override
    public Collection<String> searchFilesInDirectory(String path) {
        String p = getAbsolutePath(path);
        File dir = new File(p);
        List<String> ret = new ArrayList<>();
        if(dir.isDirectory()) {
            ret.addAll(Arrays.asList(dir.list()));
        }
        return ret;
    }

    @Override
    public Collection<String> searchFilesInAllDirectories(String path) {
        return null;
    }

    @Override
    public Collection<String> searchFilesInDirectoryAndBelow(String path) {
        return null;
    }

    @Override
    public Collection<String> searchFilesWithExtension(String path, String extension) {
        return null;
    }

    @Override
    public Collection<String> searchFilesThatContain(String path, String substring) {
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
