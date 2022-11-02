import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.FileMetaData;
import storage.StorageConstraint;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

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
            for(int i = 0 ; i < bannedExtensions.length; i++) bannedExtensions[i] = bannedExtensions[i].toLowerCase();
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
        if(!relative.startsWith("#")) throw new FileException("Invalid path");
        relative = relative.replaceAll("\\\\", "/");
        return relative.replaceFirst("(#/*)", rootDirectory.getAbsolutePath().replaceAll("\\\\", "/") + "/");
    }

    @Override
    public void openDirectory(String s) {
        File directory = new File(s);
        rootDirectory = directory;
        //System.out.println(directory.getPath());
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(directory, "directory.conf"))))  {
            storageConstraint = (StorageConstraint) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println(storageConstraint);
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
        String ext = file.substring(file.lastIndexOf(".")+1).toLowerCase();
        return (!storageConstraint.getIllegalExtensions().contains(ext));
    }

    @Override
    public void create(String directoryName, String path) {
        create(directoryName, path, -1);
    }

    @Override
    public void create(String directoryName, String path, int i) {
        String s = getAbsolutePath(path + directoryName);
        if(checkIfAdditionValid(path, 1)) {
            File newDir = new File(s);
            System.out.println(newDir.mkdirs());
            storageConstraint.getMaxNumberOfFiles().put(path +  directoryName, i);
            writeConfiguration();
        } else throw new InvalidConstraintException("Directory full");
    }

    private long getSubSize(File directory) {
        long sum = 0;
        for(File sub: directory.listFiles()) {
            if(sub.isDirectory()) sum += getSubSize(sub);
            else {
                try {
                    sum += Files.size(Paths.get(sub.getAbsolutePath()));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        return sum;
    }

    private boolean checkForSpace(long additional) {
        return getStorageByteSize() + additional <= storageConstraint.getByteSizeQuota();
    }

    @Override
    public void uploadFiles(String destination, String... sources) throws InvalidConstraintException {
        if(destination.matches("[/\\\\]$")) destination += "/";
        File dest = new File(getAbsolutePath(destination));
        if(!dest.exists() || !dest.isDirectory()) throw new FileException("Destination is not an existing directory");
        if(!checkIfAdditionValid(destination, sources.length)) throw new FileException("Destination full");
        long size = 0;
        for(String s : sources) {
            File source = new File(s);
            dest = new File(dest, source.getName());
            if(!source.exists()) throw new FileException("Source does not exist");
            if(!checkExtension(source.getName())) throw new FileException("Source has illegal extension");
            try {
                size += Files.size(Paths.get(source.getAbsolutePath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if(checkForSpace(size)) for(String s : sources) {
            try {
                Path result = Files.move(Paths.get(s), Paths.get(dest.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                if(!result.toFile().exists()) System.err.println("Upload failed");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
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
//        System.out.println(normPath);
        return normPath;
    }

    @Override
    public void moveFiles(String destination, String... sources) throws InvalidConstraintException {
        String fullPath = getAbsolutePath(destination);
        File destinationFolder = new File(fullPath);

        if(!destinationFolder.exists()) throw new FileException("Destination directory does not exist");

        List<String> list = new ArrayList<>();
        for(String source: sources)
            if(checkExtension(source))
                list.add(source);

        if(!checkIfAdditionValid(destination, list.size())) throw new InvalidConstraintException("Too many files in target directory");

        for(String source: list) {
            source = getAbsolutePath(source);
            File sourceFile = new File(source);

            // Provera da li postoji fajl na prosledjenoj putanji:
            if(!sourceFile.exists()) throw new FileException(String.format("File selected for move on path %s does not exist", sourceFile.getAbsolutePath()));

            Path result = null;
            try {
                String resultingPath = rootDirectory.getPath() + "/" + destination + "/" + Paths.get(source).getFileName();
                resultingPath = resultingPath.replaceFirst("#[/\\\\]", "");
                System.out.println(resultingPath);
                result = Files.move(Paths.get(source), Paths.get(resultingPath), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(result == null) System.out.println("Unsuccessful operation");
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
        System.out.println("promenjeno ime je:" + fileOldName.renameTo(fileNewName));
    }

    @Override
    public long getStorageByteSize() {
        return getSubSize(rootDirectory);
    }

    @Override
    public void setSizeQuota(long i) {
        if(storageConstraint.getByteSizeQuota() <= i || getStorageByteSize() <= i) storageConstraint.setByteSizeQuota(i);
        else throw new InvalidConstraintException("New storage constraint smaller than current size");
        writeConfiguration();
    }

    @Override
    public void setMaxFiles(String s, int i) {
        // postaviti u constraint mapi vrednost na i
    }

    @Override
    public Collection<FileMetaData> searchFilesInDirectory(String path) {
        String p = getAbsolutePath(path);
        File dir = new File(p);
        List<FileMetaData> ret = new ArrayList<>();
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            for(File file : files) {
                ret.add(readMetadata(file));
            }
        } else ret.add(readMetadata(dir));
        return ret;
    }

    private FileMetaData readMetadata(File f) {
        try {
            BasicFileAttributes bfa = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            return new FileMetaData(f.getName(), getRelativePathOfDirectory(f), Date.from(bfa.lastModifiedTime().toInstant()), Date.from(bfa.lastAccessTime().toInstant()),
                    Date.from(bfa.creationTime().toInstant()), Files.size(f.toPath()),bfa.isDirectory() ? FileMetaData.Type.DIRECTORY : FileMetaData.Type.FILE);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesInAllDirectories(String path) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesInDirectoryAndBelow(String path) {
        File dirFile = new File(getAbsolutePath(path));
        if(!dirFile.isDirectory()) throw new FileException("File is not directory");
        Collection<FileMetaData> ret = new ArrayList<>();
        for(File f : dirFile.listFiles()) {
            if(f.isDirectory()) ret.addAll(searchFilesInDirectoryAndBelow(getRelativePathOfDirectory(f)));
            else ret.add(readMetadata(f));
        }
        return ret;
    }

    @Override
    public Collection<FileMetaData> searchFilesWithExtension(String path, String extension) {
        extension = extension.trim();
        extension = extension.toLowerCase();
        if(!extension.matches("\\.?[\\w\\d.]+")) throw new RuntimeException(String.format("Extension \"%s\" is not valid", extension));
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow("#");
        final String finalExtension = extension;
        return allFiles.stream().filter(fileMetaData -> fileMetaData.getName().toLowerCase().endsWith(finalExtension)).collect(Collectors.toList());
    }

    @Override
    public Collection<FileMetaData> searchFilesThatContain(String path, String substring) {
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
    public Collection<FileMetaData> searchByNameSorted(String s, Boolean aBoolean) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchByDirectoryDateRange(Date date, Date date1, String s) {
        return null;
    }
}
