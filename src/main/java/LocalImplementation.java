import exceptions.InvalidConstraintException;

import java.util.Collection;
import java.util.Date;

public class LocalImplementation implements Storage{

    @Override
    public void initialiseStorage(String s, String s1) {

    }

    @Override
    public void setStorageSize(int i) {

    }

    @Override
    public void setMaxNumberOfFiles(int i) {

    }

    @Override
    public void createDirectory(String s, String s1) {

    }

    @Override
    public void createDirectory(String s, String s1, String s2) {

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
