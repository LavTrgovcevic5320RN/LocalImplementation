import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

public class LocalImplementation extends Storage{
    private static LocalImplementation instance = null;
    private static File rootDirectory;
    private boolean bulkMode = false;

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
        if (size >= 0)
            storageConstraint.setByteSizeQuota(size);

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
        }else if(!dir.exists() && bulkMode){
            path = path.replaceAll("[/\\\\]+$", "");
            create(path.substring(Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"))+1),
                    path.substring(0, Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"))+1));
            return true;
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
            if(!newDir.exists()) newDir.mkdirs();
            else System.err.println("Directory exists!");
            storageConstraint.getMaxNumberOfFiles().put(path +  directoryName, i);
            if(!bulkMode) writeConfiguration();
        } else throw new InvalidConstraintException("Directory full");
    }

    @Override
    public void createExpanded(String path, String pattern) {
        bulkMode = true;
        if(checkIfAdditionValid(path, BraceExpansion.getTopLevelDirectoryCount(pattern)))
            for(String s : BraceExpansion.expand(pattern)){
                String full = path + s;
                full = full.replaceAll("\\\\+", "/");
                full = full.replaceAll("/+", "/");
                int index = Math.max(full.lastIndexOf("/"), full.lastIndexOf("\\"));
                String actualName = full.substring(index +1);
                String actualPath = full.substring(0, index +1);
                create(actualName, actualPath);
            }
        else throw new InvalidConstraintException("Too many files!");
        writeConfiguration();
        bulkMode = false;
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
//                System.out.println(resultingPath);
                result = Files.move(Paths.get(source), Paths.get(resultingPath), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(result == null) System.out.println("Unsuccessful operation");
        }
    }

    @Override
    public void download(String destination, String...sources) {
        List<String> list = new ArrayList<>();
        for(String source: sources)
            if(checkExtension(source))
                list.add(source);

        for(String source : list) {
            String sourcePath = getAbsolutePath(source);
            File sourceFolder = new File(sourcePath);
            sourceFolder.mkdirs();

            String destinationPath = destination + source.replaceFirst("#", "");
            File destinationFolder = new File(destinationPath);
            if (!sourceFolder.exists())
                throw new FileException(String.format("File selected for move on path %s does not exist", sourceFolder.getAbsolutePath()));

            try {
                FileUtils.copyDirectory(sourceFolder, destinationFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        //todo postaviti vrednost u mapi za folder na putanji s na i pa ispisati konfiguraciju
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
        //todo utvrditi sta ovde treba da se uradi
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
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow("#");
        final String finalSubstring = substring.toLowerCase();
        return allFiles.stream().filter(fileMetaData -> fileMetaData.getName().toLowerCase().contains(finalSubstring)).collect(Collectors.toList());
    }

    @Override
    public boolean searchIfFilesExist(String s, String... strings) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(s);
        Collection<String> names = new HashSet<>();
        for(FileMetaData f : allFiles) names.add(f.getName());
        return names.containsAll(Arrays.asList(strings));
    }

    @Override
    public Collection<String> searchFile(final String s) {
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow("#");
        Collection<FileMetaData> matching = allFiles.stream().filter(fileMetaData -> fileMetaData.getName().equalsIgnoreCase(s)).collect(Collectors.toList());
        Collection<String> paths = new HashSet<>();
        for (FileMetaData f : matching)
            paths.add(f.getFullPath().substring(0, Math.max(f.getFullPath().lastIndexOf('\\'), f.getFullPath().lastIndexOf('/'))));
        return paths;
    }

    @Override
    public Collection<FileMetaData> searchByNameSorted(String path, boolean ascending) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(path);
        Sorter s = new Sorter(ascending);
        return s.applySorter(allFiles);
    }

    @Override
    public Collection<FileMetaData> searchByDirectoryDateRange(Date startDate, Date endDate, DateType sortDateType, String path) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(path);
        Filter f = new Filter(startDate, endDate, sortDateType);
        return f.applyFilter(allFiles);
    }
}
